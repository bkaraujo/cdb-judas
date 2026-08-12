#!/usr/bin/env node
/* tools/check-slices.js — heurística leve (regex, não AST) que espelha, no frontend, a regra
 * ArchUnit `feature_slices_must_not_depend_on_sibling_slices` do backend: uma fatia (web/feature/<slice>.js,
 * um arquivo por fatia) não pode usar um global window.* definido por OUTRA fatia, a menos que o
 * global venha de um arquivo <slice>.api.js (mecanismo público explícito, equivalente ao FNNNApi
 * do backend) ou de web/core/kernel/** (kernel — todas podem depender dele).
 * web/core/composition-root/** é isento (é o único lugar autorizado a conhecer duas fatias ao
 * mesmo tempo, equivalente ao f999).
 *
 * web/pages/** e os barrels legados por camada (web/core/_1_domain.js etc) ficam de fora da
 * varredura — ainda não migrados para feature/, isentos até entrarem lá
 * (ver .claude/frontend-refactor.md).
 *
 * Roda manual antes de cada commit de fase: `node web/tools/check-slices.js`.
 * Falsos positivos/negativos existem (string literal, comentário) — não é um parser real.
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..'); // web/
const SCAN_ROOTS = ['core/kernel', 'feature', 'core/composition-root'].map((p) => path.join(ROOT, p));

// application(_1) não pode acessar infrastructure(_2) — mesma regra do backend, exceções nomeadas
// nos mesmos moldes do ArchUnit (LoginResource/SelfResource): casos pré-existentes, documentados.
// Só se aplica ao kernel (que ainda mantém _0_domain/_1_application/_2_infrastructure separados) —
// as fatias em feature/ são um arquivo só por design (domain+application+infra juntos).
const LAYERING_EXCEPTIONS = new Set([
  'core/kernel/_1_application/period-service.js:Infra.Storage',
  'core/kernel/_1_application/preferences-service.js:Infra.AuthStore',
  // money.js é uma facade deliberada sobre format.js (comentário de topo do próprio arquivo,
  // pré-existente à reorganização em fatias) — não é acoplamento introduzido agora.
  'core/kernel/_0_domain/money.js:fmt',
  'core/kernel/_0_domain/money.js:fmtShort',
  'core/kernel/_0_domain/money.js:parseCurrency',
  'core/kernel/_0_domain/money.js:valueColor',
]);

function walk(dir, out) {
  if (!fs.existsSync(dir)) return out;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith('.js')) out.push(full);
  }
  return out;
}

function sliceOf(relPath) {
  if (relPath.startsWith('core/kernel/')) return 'kernel';
  if (relPath.startsWith('core/composition-root/')) return 'composition-root';
  // feature/<slice>.js ou feature/<slice>.api.js — um arquivo flat por fatia, sem subpasta.
  const m = /^feature\/([^/]+?)(?:\.api)?\.js$/.exec(relPath);
  return m ? 'feature:' + m[1] : null;
}

function layerOf(relPath) {
  if (relPath.includes('/_2_infrastructure/')) return 'infrastructure';
  if (relPath.includes('/_1_application/')) return 'application';
  if (relPath.includes('/_0_domain/')) return 'domain';
  return 'root'; // kernel.barrel.js, composition-root.barrel.js, feature/<slice>.js flat, *.api.js
}

// Browser globals, não símbolos de fatia — `window.location.hash = ...`, `window.history.*` etc
// aparecem em múltiplos arquivos independentes e não são ownership real de ninguém.
const BROWSER_GLOBALS = new Set(['location', 'history', 'document', 'navigator', 'console', 'localStorage', 'sessionStorage']);

const scanned = [];
for (const root of SCAN_ROOTS) walk(root, scanned);

const relFiles = scanned.map((f) => ({
  abs: f,
  rel: path.relative(ROOT, f).split(path.sep).join('/'),
}));

// ── Pass 1: collect definitions (symbol -> owner) ──────────────────────────
const DEF_RE = /window\.([A-Za-z_$][\w$]*)(\.[A-Za-z_$][\w$]*)?\s*=(?!=)/g;
const defs = new Map(); // symbol -> { rel, slice, isApi }

for (const f of relFiles) {
  const src = fs.readFileSync(f.abs, 'utf8');
  const lines = src.split('\n');
  const slice = sliceOf(f.rel);
  const isApi = /\.api\.js$/.test(f.rel);
  lines.forEach((line, idx) => {
    let m;
    DEF_RE.lastIndex = 0;
    while ((m = DEF_RE.exec(line))) {
      const top = m[1];
      const sub = m[2] || '';
      const symbol = top + sub;
      if (BROWSER_GLOBALS.has(top)) continue;
      // Namespace guard: `window.App = window.App || {};` — not a real ownership claim.
      const guardRe = new RegExp('window\\.' + top.replace(/\$/g, '\\$') + '\\s*=\\s*window\\.' + top.replace(/\$/g, '\\$') + '\\s*\\|\\|');
      if (guardRe.test(line)) continue;
      if (!defs.has(symbol)) {
        defs.set(symbol, { rel: f.rel, slice, isApi, line: idx + 1 });
      }
    }
  });
}

// ── Pass 2: find cross-slice uses ───────────────────────────────────────────
const USE_RE = /window\.([A-Za-z_$][\w$]*)(\.[A-Za-z_$][\w$]*)?/g;
const violations = [];

for (const f of relFiles) {
  const src = fs.readFileSync(f.abs, 'utf8');
  const lines = src.split('\n');
  const fileSlice = sliceOf(f.rel);
  const fileLayer = layerOf(f.rel);

  lines.forEach((line, idx) => {
    let m;
    USE_RE.lastIndex = 0;
    while ((m = USE_RE.exec(line))) {
      const top = m[1];
      const sub = m[2] || '';
      const symbol = top + sub;
      if (BROWSER_GLOBALS.has(top)) continue;
      const owner = defs.get(symbol) || defs.get(top); // fall back to top-level owner
      if (!owner) continue;
      if (owner.rel === f.rel) continue; // own file

      // Rule 1: cross-slice access outside kernel/composition-root, unless via *.api.js.
      if (
        owner.slice &&
        fileSlice &&
        owner.slice !== fileSlice &&
        owner.slice !== 'kernel' &&
        fileSlice !== 'composition-root' &&
        !owner.isApi
      ) {
        violations.push(
          `${f.rel}:${idx + 1} — fatia "${fileSlice}" usa global "${symbol}" de fatia "${owner.slice}" ` +
          `(definido em ${owner.rel}:${owner.line}) sem passar por kernel/*.api.js/composition-root`
        );
      }

      // Rule 2: _0_domain/_1_application não acessa _2_infrastructure (kernel — fatias em
      // feature/ são um arquivo só, sem separação de camada física a validar aqui).
      if ((fileLayer === 'domain' || fileLayer === 'application') && layerOf(owner.rel) === 'infrastructure') {
        const key = f.rel + ':' + symbol;
        if (!LAYERING_EXCEPTIONS.has(key)) {
          violations.push(
            `${f.rel}:${idx + 1} — camada "${fileLayer}" usa global "${symbol}" de infrastructure ` +
            `(definido em ${owner.rel}:${owner.line}) — application/domain não deve acessar infrastructure`
          );
        }
      }
    }
  });
}

if (violations.length) {
  console.error(`check-slices: ${violations.length} violação(ões) encontrada(s):\n`);
  violations.forEach((v) => console.error('  ' + v));
  process.exit(1);
} else {
  console.log('check-slices: nenhuma violação encontrada (' + relFiles.length + ' arquivo(s) verificado(s)).');
  process.exit(0);
}
