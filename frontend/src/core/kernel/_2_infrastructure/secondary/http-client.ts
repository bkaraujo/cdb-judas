import { ApiError } from '../../../../api/problem.ts';
import type { AuthStore } from './auth-store.ts';

const TOKEN_HEADER = 'X-Access-Token';

function p2(n: number): string {
  return n < 10 ? '0' + n : '' + n;
}
function p5(n: number): string {
  if (n < 10) return '0000' + n;
  if (n < 100) return '000' + n;
  if (n < 1000) return '00' + n;
  if (n < 10000) return '0' + n;
  return '' + n;
}

export function reqId(): string {
  const d = new Date();
  let v = d.getMilliseconds() * 100;
  if (typeof performance !== 'undefined') v += ((performance.now() % 1) * 100) | 0;
  return (
    '' + d.getFullYear() + p2(d.getMonth() + 1) + p2(d.getDate()) +
    p2(d.getHours()) + p2(d.getMinutes()) + p2(d.getSeconds()) + p5(v)
  );
}

export interface DeletionQueryOptions {
  strategy?: 'MOVE' | 'DELETE' | 'DETACH';
  targetId?: string;
}

/** Reverso do contrato de exclusão: 409 LINKED_TRANSACTIONS -> escolhe estratégia -> refaz o
 * DELETE com ?strategy=MOVE|DELETE|DETACH (+targetId para MOVE). */
export function deletionQuery(opts?: DeletionQueryOptions): string {
  const params: string[] = [];
  if (opts?.strategy) params.push('strategy=' + encodeURIComponent(opts.strategy));
  if (opts?.targetId) params.push('targetId=' + encodeURIComponent(opts.targetId));
  return params.length ? '?' + params.join('&') : '';
}

export interface HttpClient {
  baseUrl: string;
  get<T>(path: string): Promise<T | null>;
  post<T>(path: string, body?: unknown): Promise<T | null>;
  put<T>(path: string, body?: unknown): Promise<T | null>;
  patch<T>(path: string, body?: unknown): Promise<T | null>;
  delete<T>(path: string): Promise<T | null>;
  upload<T>(path: string, formData: FormData): Promise<T | null>;
  global: {
    get<T>(path: string): Promise<T | null>;
    post<T>(path: string, body?: unknown): Promise<T | null>;
    put<T>(path: string, body?: unknown): Promise<T | null>;
    patch<T>(path: string, body?: unknown): Promise<T | null>;
    delete<T>(path: string): Promise<T | null>;
  };
  setUnauthorizedHandler(fn: () => void): void;
  /** Exposto SÓ para o handshake do SSE (sse-client). Sem isto o `fetch` do stream lê o token
   * fora da fila e pode sair com o mesmo valor que uma requisição já em voo está consumindo — o
   * servidor rotaciona, o token do stream morre e volta 401 (pattern 004 em web/CLAUDE.md). O SSE
   * deve enfileirar APENAS o fetch: ele resolve quando chegam os headers, não quando o corpo
   * termina — enfileirar a leitura do corpo travaria a fila para sempre. */
  enqueue<T>(fn: () => Promise<T>): Promise<T>;
}

export interface HttpClientOptions {
  authStore: AuthStore;
  baseUrl?: string;
}

export function createHttpClient(opts: HttpClientOptions): HttpClient {
  const baseUrl = opts.baseUrl || '/api';
  const auth = opts.authStore;
  let onUnauthorized: () => void = () => {};
  let queue: Promise<unknown> = Promise.resolve();

  function enqueue<T>(fn: () => Promise<T>): Promise<T> {
    const next = queue.then(fn, fn) as Promise<T>;
    queue = next.catch(() => {});
    return next;
  }

  async function readError(res: Response): Promise<never> {
    const txt = await res.text();
    let code: string | undefined;
    let detail = txt;
    let count: number | undefined;
    try {
      const json = JSON.parse(txt);
      code = json.code || undefined;
      detail = json.detail || json.message || txt;
      count = typeof json.count === 'number' ? json.count : undefined;
    } catch {
      /* non-JSON body */
    }
    throw new ApiError(detail || 'HTTP ' + res.status, res.status, code, count);
  }

  /* `auth.get() === token` on 401 guards against a stale-request race: mountChrome() fires an
   * unauthenticated GET (e.g. sidebar version check) before login, queued same as everything
   * else; if it resolves 401 *after* a subsequent successful login, an unconditional auth.clear()
   * would wipe the freshly-set session. Comparing against the token this specific request was
   * sent with skips the clear once something fresher has already taken over. */
  async function handleResponse<T>(res: Response, token: string | null): Promise<T | null> {
    const nextToken = res.headers.get(TOKEN_HEADER);
    if (nextToken) auth.set(nextToken);

    if (!res.ok) {
      if (res.status === 401 && auth.get() === token) {
        auth.clear();
        try {
          onUnauthorized();
        } catch {
          /* noop */
        }
      }
      return readError(res);
    }
    if (res.status === 204) return null;
    return (await res.json()) as T;
  }

  async function doRequest<T>(method: string, path: string, body?: unknown): Promise<T | null> {
    const headers: Record<string, string> = { 'X-request-id': reqId() };
    const token = auth.get();
    if (token) headers[TOKEN_HEADER] = token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const res = await fetch(baseUrl + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    return handleResponse<T>(res, token);
  }

  function request<T>(method: string, path: string, body?: unknown): Promise<T | null> {
    return enqueue(() => doRequest<T>(method, path, body));
  }

  /* Multipart upload. Shares the serialized queue + rotating token of doRequest, but sends a
   * FormData body (the browser sets the multipart Content-Type/boundary) and surfaces the
   * server's ProblemDetail `code` on the rejection so callers can map error UX. */
  async function doUpload<T>(method: string, path: string, formData: FormData): Promise<T | null> {
    const headers: Record<string, string> = { 'X-request-id': reqId() };
    const token = auth.get();
    if (token) headers[TOKEN_HEADER] = token;

    const res = await fetch(baseUrl + path, { method, headers, body: formData });
    return handleResponse<T>(res, token);
  }

  /* Prepends the authenticated user's id, building /api/{uuid}<path>. This is the default for all
   * data routes after the cutover; global routes (cost-center) opt out via http.global.*. Returns
   * null instead of silently falling back to an unprefixed path — callers (scoped() below) turn
   * that into a rejected promise, so a missing uid surfaces as a caught error, not a confusing
   * 404 on the wrong URL. */
  function withUser(path: string): string | null {
    const uid = auth.userId();
    return uid ? '/' + uid + path : null;
  }

  function scoped<T>(method: string, path: string, body?: unknown): Promise<T | null> {
    const p = withUser(path);
    if (!p) return Promise.reject(new Error('Sem uid de usuário autenticado para ' + path));
    return request<T>(method, p, body);
  }

  return {
    baseUrl,
    get: <T>(p: string) => scoped<T>('GET', p),
    post: <T>(p: string, b?: unknown) => scoped<T>('POST', p, b),
    put: <T>(p: string, b?: unknown) => scoped<T>('PUT', p, b),
    patch: <T>(p: string, b?: unknown) => scoped<T>('PATCH', p, b),
    delete: <T>(p: string) => scoped<T>('DELETE', p),
    upload: <T>(p: string, fd: FormData) => {
      const up = withUser(p);
      if (!up) return Promise.reject(new Error('Sem uid de usuário autenticado para ' + p));
      return enqueue(() => doUpload<T>('POST', up, fd));
    },
    // Global (no user prefix) — only for system-wide routes such as /cost-center.
    global: {
      get: <T>(p: string) => request<T>('GET', p),
      post: <T>(p: string, b?: unknown) => request<T>('POST', p, b),
      put: <T>(p: string, b?: unknown) => request<T>('PUT', p, b),
      patch: <T>(p: string, b?: unknown) => request<T>('PATCH', p, b),
      delete: <T>(p: string) => request<T>('DELETE', p),
    },
    setUnauthorizedHandler: (fn) => {
      onUnauthorized = fn || (() => {});
    },
    enqueue,
  };
}
