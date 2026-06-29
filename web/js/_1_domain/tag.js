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

  window.Domain = window.Domain || {};
  window.Domain.Tag = {
    normalize: normalize,
    hasColor: hasColor,
  };
})();
