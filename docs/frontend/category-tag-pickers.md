# Padrão de Seleção de Categoria/Tag

## 1. Fonte de dados
cache (`App.CacheStore.categories()` / `.tags()`), hydrated by `registry-bootstrap.js`, kept alive by SSE (`sse-client.js`). No request when opening a picker.

## 2. Elegibilidade de categoria
`window.flatCategories(natureFilter, excludeRoots, keepId)` in `_3_infrastructure/primary/format.js:113-127`:
- `excludeRoots: true` — only subcategory is assignable
- nature filter by transaction type (INCOME/EXPENSE; transfer has no category)
- `Domain.Category.isEffectivelyActive` — deactivating macro hides children
- `keepId` — mandatory escape hatch in every edit screen
- label always `labelChain` = "Pai / Filho", sorted with `localeCompare('pt-BR')`

## 3. Widget de categoria
native `<select>` hidden (source of truth, read via `.val()` on submit) paired with `window.searchSelectHtml(items, selected, key, { pairedSelectId })`. Rules: programmatic mutation requires `window.refreshSearchSelect(id)`; delegated listener `.search-dropdown-row[data-dd-value]` in `ui.js` syncs the pair on click.

## 4. Placeholder
`<option value="" selected>Selecione</option>` explicit when no valid selection. `categoryPickerHtml`'s
default behavior only shows this option when nothing valid is selected (mandatory fields — the option
disappears once a real value is picked). Optional fields where "none" is a legitimate ongoing choice
(a nomenclature rule with no category, a list filter) pass `alwaysPlaceholder: true` to keep it
selectable even after a real value is already chosen — otherwise there'd be no way back to empty
through the picker itself.

## 5. Widget de tag
`window.tagsDropdownHtml(tagIds, key, opts)`: `<details>` + checkbox multi-selection, count label ("Nenhuma tag" / "N tags"), live search. State lives in caller's JS array, never re-read from DOM; after mutating, call `window.refreshTagsDropdownLabel($checkbox)`. One skin only (`.search-dropdown` — the former compact/`matchSelect`-less variant was removed once every call site had migrated); `opts.floating`/`opts.compact` still apply, same as `searchSelectHtml`.

## 6. Quick-create
"+ Nova categoria" / "+ Nova tag" open nested modal; on return, insert option/row by hand (`refreshSearchSelect` / `appendTagRow`) instead of waiting for SSE.

## 7. Ancoragem do painel
`.search-dropdown-panel` is `position:absolute`; inside container with `overflow:scroll`, use `floating: true` option.

## 8. Exceções deliberadas
pickers that are NOT "choose the category/tag of a transaction" are out of scope: parent-category selector (`categories.js`, roots only), and MOVE targets of `linkedDeleteDialog` (`categories.js` / `tags.js`), which are generic `<select>` from dialog.

## 9. Matriz de conformidade
table of 8 usage points, final state (2026-08-11 — all migrated except the deliberate §8 exception):

| Ponto | Arquivo | Categoria | Tag | Quick-create | Placeholder | keepId | excludeRoots | Natureza | Floating |
|---|---|---|---|---|---|---|---|---|---|
| Edição de lançamento | `create-edit.js` | `categoryPickerHtml` | `tagsDropdownHtml` | ✓ categoria + tag | ✓ "Selecione" | ✓ | ✓ | tipo do tx | — |
| Importação (fatura) | `import-statement.js` | `categoryPickerHtml` | `tagsDropdownHtml(floating,compact)` | ✓ cabeçalho | ✓ "Selecione" | ✓ via keepId | ✓ | tipo da linha | ✓ |
| Importação (extrato) | `import-statement.js` | `categoryPickerHtml` | `tagsDropdownHtml(floating,compact)` | ✓ cabeçalho | ✓ "Selecione" | ✓ via keepId | ✓ | tipo da linha | ✓ |
| Contas a pagar | `accounts-payable.js` | `categoryPickerHtml` | — | — | ✓ "Selecione" | ✓ | ✓ | A Pagar/Receber | — |
| Regras de nomenclatura | `import-rules.js` | `categoryPickerHtml` | — | — | ✓ "— Nenhuma —" (`alwaysPlaceholder`) | ✓ | ✓ | ambas | — |
| Orçamento | `budget.js` | `categoryPickerHtml` | — | — | ✓ "— Selecione —" | ✓ | ✓ | EXPENSE | — |
| Filtro (Lançamentos) | `transactions.js` | `categoryPickerHtml` | — | — | ✓ "Todas" (`alwaysPlaceholder`) | — | ✓ | ambas | — |
| Seletor categoria-pai | `categories.js` | `<select>` genérico (exceção §8) | — | — | — | — | — | — | — |
