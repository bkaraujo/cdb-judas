/**
 * Live cadastro updates over SSE.
 *
 * EventSource cannot send the Authorization header, so we parse the stream manually via fetch +
 * ReadableStream. Aplica upsert/delete via a porta `SseCachePort` (implementada por `cache-store`,
 * Fase 4 — é lá que mora a normalização de categoria/conta, não mais duplicada aqui) e transmite
 * `cbd:change` via o barramento de eventos da aplicação (`event-bus.ts`, Fase 4).
 */
import type { CbdEntityKind, SseCachePort } from '@/core/kernel/_1_application/cache-store.ts';
import type { CbdChangeDetail, EventBus } from '@/core/kernel/_1_application/event-bus.ts';
import type { AuthStore } from '@/core/kernel/_2_infrastructure/secondary/auth-store.ts';

const HANDSHAKE_TIMEOUT_MS = 10000;

type CbdEventType = 'CATEGORY' | 'ACCOUNT' | 'TAG';

const KIND_BY_EVENT_TYPE: Record<CbdEventType, CbdEntityKind> = {
  CATEGORY: 'categories',
  ACCOUNT: 'accounts',
  TAG: 'tags',
};

export interface SseClient {
  connect(): void;
  disconnect(): void;
}

export interface SseClientOptions {
  baseUrl?: string;
  authStore: AuthStore;
  bus?: EventBus;
  cache: SseCachePort;
  enqueue?: <T>(fn: () => Promise<T>) => Promise<T>;
}

// Stream lives under the user namespace: /api/{uuid}/stream.
function streamPath(uid: string): string {
  return '/' + uid + '/stream';
}

interface SseFrame {
  event: string;
  data: string;
}

function parseFrames(buf: string): { frames: SseFrame[]; rest: string } {
  const out: SseFrame[] = [];
  let i: number;
  while ((i = buf.indexOf('\n\n')) >= 0 || (i = buf.indexOf('\r\n\r\n')) >= 0) {
    const sep = buf.substr(i, 2) === '\n\n' ? 2 : 4;
    const frame = buf.substring(0, i);
    buf = buf.substring(i + sep);
    let event = 'message';
    let data = '';
    const lines = frame.split(/\r?\n/);
    for (const ln of lines) {
      if (!ln || ln.charAt(0) === ':') continue;
      const colon = ln.indexOf(':');
      const field = colon >= 0 ? ln.substring(0, colon) : ln;
      let value = colon >= 0 ? ln.substring(colon + 1) : '';
      if (value.charAt(0) === ' ') value = value.substring(1);
      if (field === 'event') event = value;
      else if (field === 'data') data += (data ? '\n' : '') + value;
    }
    out.push({ event, data });
  }
  return { frames: out, rest: buf };
}

interface WireEvent {
  type: CbdEventType;
  payload?: Record<string, unknown> & { id?: unknown };
  id?: string;
}

export function createSseClient(opts: SseClientOptions): SseClient {
  const baseUrl = opts.baseUrl || '/api';
  const auth = opts.authStore;
  const bus = opts.bus;
  const cache = opts.cache;
  // Fila do http-client: o handshake do stream entra nela para não pegar um token que outra
  // requisição já está consumindo. Só o fetch é enfileirado (resolve nos headers); a leitura do
  // corpo roda fora, senão o stream — que nunca termina — travaria a fila.
  const enqueue = opts.enqueue || (<T,>(fn: () => Promise<T>) => Promise.resolve().then(fn));

  let ctrl: AbortController | null = null;
  let connecting = false;
  let backoff = 1000;

  function broadcast(detail: CbdChangeDetail): void {
    if (bus) bus.emit('cbd:change', detail);
  }

  function handleEvent(name: string, dataRaw: string): void {
    // INITIALIZE chega como a string "Connected", não JSON — o early-return preserva isso.
    if (name === 'INITIALIZE') return;
    let data: WireEvent;
    try {
      data = JSON.parse(dataRaw) as WireEvent;
    } catch {
      return;
    }
    const kind = KIND_BY_EVENT_TYPE[data.type];
    if (!kind) return;
    if (name === 'UPSERT') {
      if (data.payload && data.payload.id != null && cache.upsert(kind, data.payload)) {
        broadcast({ type: data.type, action: 'upsert', payload: data.payload });
      }
    } else if (name === 'DELETE') {
      if (data.id != null && cache.remove(kind, data.id)) {
        broadcast({ type: data.type, action: 'delete', id: data.id });
      }
    }
  }

  async function connect(): Promise<void> {
    if (connecting) return;
    if (!auth.get() || !auth.userId()) return;

    connecting = true;
    ctrl = new AbortController();
    const signal = ctrl.signal;

    try {
      // Token lido DENTRO da fila: só aqui é garantido que nenhuma outra requisição está em voo
      // com ele. `uid` idem, para não montar a URL com um usuário já deslogado.
      const res = await enqueue(function (): Promise<Response | null> {
        const t = auth.get();
        const uid = auth.userId();
        if (!t || !uid) return Promise.resolve(null);
        const handshake = fetch(baseUrl + streamPath(uid), {
          method: 'GET',
          headers: {
            'X-Access-Token': t,
            Accept: 'text/event-stream',
          },
          signal,
          cache: 'no-store',
        });
        // Teto para a fila: se o servidor segurar a conexão sem mandar headers, a fila (e com ela
        // toda a UI) ficaria travada. O fetch continua vivo — só deixa de bloquear a fila.
        return Promise.race([
          handshake,
          new Promise<Response>((_, reject) => {
            setTimeout(() => reject(new Error('SSE handshake timeout')), HANDSHAKE_TIMEOUT_MS);
          }),
        ]);
      });

      if (!res) {
        connecting = false;
        return;
      }

      if (!res.ok || !res.body) {
        // 401 aqui NÃO derruba a sessão. O token é rotativo (pattern 004 em web/CLAUDE.md):
        // qualquer requisição concorrente invalida o que este stream usou, então um 401 no canal
        // SSE nunca é prova de que a sessão morreu — limpar aqui deslogava o usuário no meio de
        // uma navegação. Quem decide isso é o 401 do http-client, autoridade única; aqui só se
        // tenta de novo com backoff, e `auth.get()` vazio (linha do retry) encerra sozinho.
        throw new Error('SSE HTTP ' + res.status);
      }

      backoff = 1000;
      const reader = res.body.getReader();
      const dec = new TextDecoder('utf-8');
      let buf = '';

      for (;;) {
        const { value, done } = await reader.read();
        if (done) break;
        buf += dec.decode(value, { stream: true });
        const parsed = parseFrames(buf);
        buf = parsed.rest;
        for (const frame of parsed.frames) {
          handleEvent(frame.event, frame.data);
        }
      }
    } catch {
      if (signal.aborted) {
        connecting = false;
        return;
      }
    }

    connecting = false;
    if (!auth.get()) return;
    setTimeout(connect, backoff);
    backoff = Math.min(backoff * 2, 30000);
  }

  function disconnect(): void {
    if (ctrl) {
      try {
        ctrl.abort();
      } catch {
        /* noop */
      }
    }
    ctrl = null;
    connecting = false;
  }

  return { connect, disconnect };
}
