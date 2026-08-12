/* feature/categories.api.js — contrato público da fatia categories (equivalente ao FNNNApi do
 * backend). Único arquivo que outra fatia pode referenciar. Consumidor: dashboard (refresh do
 * painel de despesas por categoria ao mudar cadastro). */
(function () {
  window.CategoriesApi = {
    onChange: function (cb) { return window.App.CategoryService.onChange(cb); },
  };
})();
