/* _2_application/event-bus.js — thin wrapper over $(document) trigger/on. */
(function () {
  function emit(name, detail) {
    try { $(document).trigger(name, [detail]); } catch (e) { /* noop */ }
  }
  function on(name, cb)  { $(document).on(name, cb); }
  function off(name, cb) { $(document).off(name, cb); }

  window.App = window.App || {};
  window.App.EventBus = { emit: emit, on: on, off: off };
})();
