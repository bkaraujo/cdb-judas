/* _1_domain/tag.js — Tag entity. Pure. */
(function () {
  function normalize(raw) {
    if (!raw) return null;
    return {
      id:    raw.id,
      name:  raw.name || '',
      color: raw.color || null,
    };
  }

  function hasColor(t) { return !!(t && t.color); }

  /* Resolve ids → tags do catálogo, na ordem do catálogo. Id órfão é descartado: apagar uma tag
     com strategy=DETACH desfaz o vínculo no servidor, mas o `tagIds` já carregado numa tela aberta
     continua citando o id até a próxima busca — resolver contra o catálogo é o que impede um
     indicador de tags apontando para nada. */
  function resolve(tagIds, catalog) {
    if (!Array.isArray(tagIds) || tagIds.length === 0) return [];
    const wanted = {};
    tagIds.forEach(function (id) { wanted[String(id)] = true; });
    return (catalog || []).filter(function (t) { return t && wanted[String(t.id)]; });
  }

  window.Domain = window.Domain || {};
  window.Domain.Tag = {
    normalize: normalize,
    hasColor: hasColor,
    resolve: resolve,
  };
})();
