/* _3_infrastructure/secondary/category-repository.js — HTTP adapter for /categories. */
(function () {
  function create(http) {
    return {
      list:   function ()        { return http.user.get('/categories'); },
      create: function (data)    { return http.user.post('/categories', data); },
      update: function (id, d)   { return http.user.patch('/categories/' + id, d); },
      remove: function (id)      { return http.user.delete('/categories/' + id); },
    };
  }
  window.Infra = window.Infra || {};
  window.Infra.CategoryRepository = { create: create };
})();
