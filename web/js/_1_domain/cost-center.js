/* _1_domain/cost-center.js — CostCenter entity. Pure. */
(function () {
  function normalize(raw) {
    if (!raw) return null;
    return {
      id:          raw.id,
      name:        raw.name || '',
      description: raw.description || '',
      color:       raw.color || null,
    };
  }

  /* Show secondary line only when description differs from the name. */
  function displaySubtitle(c) {
    if (!c) return '';
    if (c.name && c.description && c.name !== c.description) return c.description;
    return '';
  }

  window.Domain = window.Domain || {};
  window.Domain.CostCenter = {
    normalize: normalize,
    displaySubtitle: displaySubtitle,
  };
})();
