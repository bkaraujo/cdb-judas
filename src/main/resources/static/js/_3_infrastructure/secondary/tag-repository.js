/* _3_infrastructure/secondary/tag-repository.js — HTTP adapter for /tags. */
(function () {
  function create(http) {
    return {
      list:   function ()        { return http.user.get('/tags'); },
      create: function (data)    { return http.user.post('/tags', data); },
      update: function (id, d)   { return http.user.patch('/tags/' + id, d); },
      remove: function (id)      { return http.user.delete('/tags/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.TagRepository = { create: create };
})();
