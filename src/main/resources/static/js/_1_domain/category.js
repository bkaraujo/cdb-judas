/* _1_domain/category.js — Category entity + hierarchy rules. Pure. */
(function () {
  const NATURE = { EXPENSE: 'EXPENSE', REVENUE: 'REVENUE' };

  function normalize(raw) {
    if (!raw) return null;
    return {
      id:        raw.id,
      name:      raw.name || raw.description || '',
      nature:    String(raw.nature || NATURE.EXPENSE).toUpperCase(),
      parentId:  raw.parentId !== undefined
                   ? raw.parentId
                   : (raw.parent ? raw.parent.id : null),
      isSystem:  !!raw.isSystem,
    };
  }

  function isExpense(c) { return !!c && c.nature === NATURE.EXPENSE; }
  function isRevenue(c) { return !!c && c.nature === NATURE.REVENUE; }
  function isRoot(c)    { return !!c && c.parentId == null; }

  function byId(list, id) {
    if (id == null) return null;
    for (let i = 0; i < (list || []).length; i++) {
      if (String(list[i].id) === String(id)) return list[i];
    }
    return null;
  }

  function filterByNature(list, nature) {
    return (list || []).filter(function (c) { return c.nature === nature; });
  }

  function rootsByNature(list, nature) {
    return filterByNature(list, nature).filter(isRoot);
  }

  function childrenOf(list, parentId) {
    if (parentId == null) return [];
    return (list || []).filter(function (c) {
      return c.parentId != null && String(c.parentId) === String(parentId);
    });
  }

  /* Parent → child label, e.g. "Alimentação · Restaurante". */
  function labelChain(list, id, separator) {
    const sep = separator || ' · ';
    const c = byId(list, id);
    if (!c) return '';
    if (c.parentId == null) return c.name;
    const parent = byId(list, c.parentId);
    return (parent ? parent.name + sep : '') + c.name;
  }

  /* For a parent picker on create/edit: roots of same nature, excluding self. */
  function eligibleParents(list, nature, excludeId) {
    return rootsByNature(list, nature).filter(function (c) {
      return String(c.id) !== String(excludeId);
    });
  }

  window.Domain = window.Domain || {};
  window.Domain.Category = {
    NATURE: NATURE,
    normalize: normalize,
    isExpense: isExpense,
    isRevenue: isRevenue,
    isRoot: isRoot,
    byId: byId,
    filterByNature: filterByNature,
    rootsByNature: rootsByNature,
    childrenOf: childrenOf,
    labelChain: labelChain,
    eligibleParents: eligibleParents,
  };
})();
