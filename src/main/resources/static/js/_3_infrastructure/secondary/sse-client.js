/* _3_infrastructure/secondary/sse-client.js — live cadastro updates over SSE.
 *
 * EventSource cannot send the Authorization header, so we parse the stream
 * manually via fetch + ReadableStream. Updates window.CBD in place and
 * broadcasts `cbd:change` via the application event bus.
 */
(function () {
  const STREAM_PATH = '/v1/sse/stream';

  function keyOf(type) {
    if (type === 'CATEGORY')                       return 'categories';
    if (type === 'ACCOUNT' || type === 'CREDITCARD') return 'accounts';
    if (type === 'TAG')                            return 'tags';
    // Centro de custo é fixo (somente leitura): sem canal de eventos.
    return null;
  }

  function applyUpsert(type, payload) {
    const key = keyOf(type);
    if (!key || !payload || payload.id == null) return false;
    window.CBD = window.CBD || {};
    const list = Array.isArray(window.CBD[key]) ? window.CBD[key].slice() : [];
    let normalized = payload;
    if (type === 'CATEGORY') {
      normalized = Object.assign({}, payload, {
        name: payload.name || payload.description,
        parentId: payload.parentId !== undefined
          ? payload.parentId
          : (payload.parent ? payload.parent.id : null),
      });
    } else if (type === 'ACCOUNT' || type === 'CREDITCARD') {
      normalized = Object.assign({}, payload, {
        type: payload.type ? String(payload.type).toUpperCase() : 'CHECKING',
      });
    }
    const idx = list.findIndex(function (x) { return String(x.id) === String(normalized.id); });
    if (idx >= 0) list[idx] = normalized;
    else list.push(normalized);
    window.CBD[key] = list;
    return true;
  }

  function applyDelete(type, id) {
    const key = keyOf(type);
    if (!key || id == null) return false;
    window.CBD = window.CBD || {};
    const list = Array.isArray(window.CBD[key]) ? window.CBD[key] : [];
    window.CBD[key] = list.filter(function (x) { return String(x.id) !== String(id); });
    return true;
  }

  function parseFrames(buf) {
    const out = [];
    let i;
    while ((i = buf.indexOf('\n\n')) >= 0 || (i = buf.indexOf('\r\n\r\n')) >= 0) {
      const sep = buf.substr(i, 2) === '\n\n' ? 2 : 4;
      const frame = buf.substring(0, i);
      buf = buf.substring(i + sep);
      let event = 'message';
      let data = '';
      const lines = frame.split(/\r?\n/);
      for (let k = 0; k < lines.length; k++) {
        const ln = lines[k];
        if (!ln || ln.charAt(0) === ':') continue;
        const colon = ln.indexOf(':');
        const field = colon >= 0 ? ln.substring(0, colon) : ln;
        let value = colon >= 0 ? ln.substring(colon + 1) : '';
        if (value.charAt(0) === ' ') value = value.substring(1);
        if (field === 'event') event = value;
        else if (field === 'data') data += (data ? '\n' : '') + value;
      }
      out.push({ event: event, data: data });
    }
    return { frames: out, rest: buf };
  }

  function create(opts) {
    const baseUrl  = (opts && opts.baseUrl) || '/api';
    const auth     = opts.authStore;
    const bus      = opts.bus;

    let ctrl = null;
    let connecting = false;
    let backoff = 1000;

    function broadcast(detail) { if (bus) bus.emit('cbd:change', detail); }

    function handleEvent(name, dataRaw) {
      if (name === 'INITIALIZE') return;
      let data;
      try { data = JSON.parse(dataRaw); } catch (e) { return; }
      if (name === 'UPSERT') {
        if (applyUpsert(data.type, data.payload)) {
          broadcast({ type: data.type, action: 'upsert', payload: data.payload });
        }
      } else if (name === 'DELETE') {
        if (applyDelete(data.type, data.id)) {
          broadcast({ type: data.type, action: 'delete', id: data.id });
        }
      }
    }

    async function connect() {
      if (connecting) return;
      const t = auth.get();
      if (!t) return;

      connecting = true;
      ctrl = new AbortController();

      try {
        const res = await fetch(baseUrl + STREAM_PATH, {
          method: 'GET',
          headers: {
            'X-Access-Token': t,
            'Accept': 'text/event-stream',
          },
          signal: ctrl.signal,
          cache: 'no-store',
        });

        if (!res.ok || !res.body) {
          if (res.status === 401) {
            auth.clear();
            connecting = false;
            return;
          }
          throw new Error('SSE HTTP ' + res.status);
        }

        backoff = 1000;
        const reader = res.body.getReader();
        const dec = new TextDecoder('utf-8');
        let buf = '';

        while (true) {
          const { value, done } = await reader.read();
          if (done) break;
          buf += dec.decode(value, { stream: true });
          const parsed = parseFrames(buf);
          buf = parsed.rest;
          for (let i = 0; i < parsed.frames.length; i++) {
            handleEvent(parsed.frames[i].event, parsed.frames[i].data);
          }
        }
      } catch (e) {
        if (ctrl && ctrl.signal.aborted) { connecting = false; return; }
      }

      connecting = false;
      if (!auth.get()) return;
      setTimeout(connect, backoff);
      backoff = Math.min(backoff * 2, 30000);
    }

    function disconnect() {
      if (ctrl) { try { ctrl.abort(); } catch (e) { /* noop */ } }
      ctrl = null;
      connecting = false;
    }

    return { connect: connect, disconnect: disconnect };
  }

  window.Infra = window.Infra || {};
  window.Infra.SSEClient = { create: create };
})();
