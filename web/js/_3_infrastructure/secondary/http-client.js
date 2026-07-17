/* _3_infrastructure/secondary/http-client.js — token-rotating HTTP client.
 * Requests are serialized through a queue so the rotating token stays consistent. */
(function () {
  const TOKEN_HEADER = 'X-Access-Token';

  function p2(n) { return n < 10 ? '0' + n : '' + n; }
  function p5(n) {
    if (n < 10)    return '0000' + n;
    if (n < 100)   return '000' + n;
    if (n < 1000)  return '00' + n;
    if (n < 10000) return '0' + n;
    return '' + n;
  }

  function reqId() {
    const d = new Date();
    let v = d.getMilliseconds() * 100;
    if (typeof performance !== 'undefined') v += (performance.now() % 1) * 100 | 0;
    return '' + d.getFullYear() + p2(d.getMonth() + 1) + p2(d.getDate()) +
      p2(d.getHours()) + p2(d.getMinutes()) + p2(d.getSeconds()) +
      p5(v);
  }

  /* Reversed from the deletion contract: 409 LINKED_TRANSACTIONS -> pick a strategy -> retry the
   * DELETE with ?strategy=MOVE|DELETE|DETACH (+targetId for MOVE). */
  function deletionQuery(opts) {
    opts = opts || {};
    const params = [];
    if (opts.strategy) params.push('strategy=' + encodeURIComponent(opts.strategy));
    if (opts.targetId) params.push('targetId=' + encodeURIComponent(opts.targetId));
    return params.length ? ('?' + params.join('&')) : '';
  }

  function create(opts) {
    const baseUrl = (opts && opts.baseUrl) || '/api';
    let onUnauthorized = function () {};
    let queue = Promise.resolve();

    function enqueue(fn) {
      const next = queue.then(fn, fn);
      queue = next.catch(function () {});
      return next;
    }

    function doRequest(method, path, body) {
      const auth = window.Infra.AuthStore;
      const headers = { 'X-request-id': reqId() };
      const token = auth.get();
      if (token) headers[TOKEN_HEADER] = token;
      if (body)  headers['Content-Type'] = 'application/json';

      return fetch(baseUrl + path, {
        method: method,
        headers: headers,
        body: body ? JSON.stringify(body) : undefined,
      }).then(function (res) {
        const nextToken = res.headers.get(TOKEN_HEADER);
        if (nextToken) auth.set(nextToken);

        if (!res.ok) {
          if (res.status === 401) {
            auth.clear();
            try { onUnauthorized(); } catch (e) { /* noop */ }
          }
          return res.text().then(function (txt) {
            let code = null, detail = txt, count = null;
            try {
              const json = JSON.parse(txt);
              code = json.code || null;
              detail = json.detail || json.message || txt;
              count = typeof json.count === 'number' ? json.count : null;
            } catch (e) { /* non-JSON body */ }
            const err = new Error(detail || ('HTTP ' + res.status));
            err.status = res.status;
            if (code) err.code = code;
            if (count !== null) err.count = count;
            throw err;
          });
        }
        if (res.status === 204) return null;
        return res.json();
      });
    }

    function request(method, path, body) {
      return enqueue(function () { return doRequest(method, path, body); });
    }

    /* Multipart upload. Shares the serialized queue + rotating token of doRequest, but sends a
     * FormData body (the browser sets the multipart Content-Type/boundary) and surfaces the
     * server's ProblemDetail `code` on the rejection so callers can map error UX. */
    function doUpload(method, path, formData) {
      const auth = window.Infra.AuthStore;
      const headers = { 'X-request-id': reqId() };
      const token = auth.get();
      if (token) headers[TOKEN_HEADER] = token;

      return fetch(baseUrl + path, {
        method: method,
        headers: headers,
        body: formData,
      }).then(function (res) {
        const nextToken = res.headers.get(TOKEN_HEADER);
        if (nextToken) auth.set(nextToken);

        if (!res.ok) {
          if (res.status === 401) {
            auth.clear();
            try { onUnauthorized(); } catch (e) { /* noop */ }
          }
          return res.text().then(function (txt) {
            let code = null, detail = txt, count = null;
            try {
              const json = JSON.parse(txt);
              code = json.code || null;
              detail = json.detail || json.message || txt;
              count = typeof json.count === 'number' ? json.count : null;
            } catch (e) { /* non-JSON body */ }
            const err = new Error(detail || ('HTTP ' + res.status));
            err.status = res.status;
            if (code) err.code = code;
            if (count !== null) err.count = count;
            throw err;
          });
        }
        if (res.status === 204) return null;
        return res.json();
      });
    }

    /* Prepends the authenticated user's id, building /api/{uuid}<path>. This is the default for all
     * data routes after the cutover; global routes (cost-center) opt out via http.global.*. */
    function withUser(path) {
      const uid = window.Infra.AuthStore.userId();
      return (uid ? '/' + uid : '') + path;
    }

    return {
      baseUrl: baseUrl,
      get:    function (p)    { return request('GET',    withUser(p)); },
      post:   function (p, b) { return request('POST',   withUser(p), b); },
      put:    function (p, b) { return request('PUT',    withUser(p), b); },
      patch:  function (p, b) { return request('PATCH',  withUser(p), b); },
      delete: function (p)    { return request('DELETE', withUser(p)); },
      upload: function (p, fd) { return enqueue(function () { return doUpload('POST', withUser(p), fd); }); },
      // Global (no user prefix) — only for system-wide routes such as /cost-center.
      global: {
        get:    function (p)    { return request('GET',    p); },
        post:   function (p, b) { return request('POST',   p, b); },
        put:    function (p, b) { return request('PUT',    p, b); },
        patch:  function (p, b) { return request('PATCH',  p, b); },
        delete: function (p)    { return request('DELETE', p); },
      },
      setUnauthorizedHandler: function (fn) { onUnauthorized = fn || function () {}; },
    };
  }

  window.Infra = window.Infra || {};
  window.Infra.HttpClient = { create: create, deletionQuery: deletionQuery };
})();
