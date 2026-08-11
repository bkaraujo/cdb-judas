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
`<option value="" selected>Selecione</option>` explicit when no valid selection.

## 5. Widget de tag
`window.tagsDropdownHtml(tagIds, key, { matchSelect: true })`: `<details>` + checkbox multi-selection, count label ("Nenhuma tag" / "N tags"), live search. State lives in caller's JS array, never re-read from DOM; after mutating, call `window.refreshTagsDropdownLabel($checkbox)`.

## 6. Quick-create
"+ Nova categoria" / "+ Nova tag" open nested modal; on return, insert option/row by hand (`refreshSearchSelect` / `appendTagRow`) instead of waiting for SSE.

## 7. Ancoragem do painel
`.search-dropdown-panel` is `position:absolute`; inside container with `overflow:scroll`, use `floating: true` option.

## 8. Exceções deliberadas
pickers that are NOT "choose the category/tag of a transaction" are out of scope: parent-category selector (`categories.js`, roots only), and MOVE targets of `linkedDeleteDialog` (`categories.js` / `tags.js`), which are generic `<select>` from dialog.

## 9. Matriz de conformidade
table of 8 usage points with their state after this refactoring:

| Ponto | Arquivo | Categoria | Tag | Quick-create | Placeholder | keepId | excludeRoots | Natureza | Floating |
|---|---|---|---|---|---|---|---|---|---|
| Edição de lançamento | `create-edit.js` | `categoryPickerHtml` | `tagsDropdownHtml(matchSelect)` | ✓ categoria + tag | ✓ "Selecione" | ✓ | ✓ | tipo do tx | — |
| Importação (fatura) | `import-statement.js` | `searchSelectHtml` na mão ⧗ | `tagsDropdownHtml(matchSelect,floating)` | ✓ cabeçalho | ✓ "Selecione" | ✓ via keepId | ✓ | tipo da linha | ✓ |
| Importação (extrato) | `import-statement.js` | `searchSelectHtml` na mão ⧗ | `tagsDropdownHtml(matchSelect,floating)` | ✓ cabeçalho | ✓ "Selecione" | ✓ via keepId | ✓ | tipo da linha | ✓ |
| Contas a pagar | `accounts-payable.js` | `<select>` genérico ⧗ | — | — | ✓ "Selecione" | ✓ | ✓ | A Pagar/Receber | — |
| Regras de nomenclatura | `import-rules.js` | `<select>` genérico ⧗ | — | — | ✓ "— Nenhuma —" | ✓ | ✓ | ambas | — |
| Orçamento | `budget.js` | `<select>` genérico ⧗ | — | — | ✓ "— Selecione —" | ✓ | ✓ | EXPENSE | — |
| Filtro (Lançamentos) | `transactions.js` | `<select>` genérico ⧗ | — | — | ✓ "Todas" | — | ✓ | ambas | — |
| Seletor categoria-pai | `categories.js` | `<select>` genérico (exceção §8) | — | — | — | — | — | — | — |

⧗ = ainda não migrado para `categoryPickerHtml`; a migração é incremental, um ponto por vez.
