/** Regras puras de evolução por categoria. Agregação client-side: transações agrupadas em buckets
 * (mês ou ano), roll-up pelas subcategorias até as raízes, com poda de linhas vazias. Regime
 * caixa: compras de cartão são redatadas pelo vencimento da fatura via Invoice.dueDate. */
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import * as Category from '@/core/kernel/_0_domain/category.ts';
import * as Format from '@/core/kernel/_0_domain/format.ts';
import { dueDate } from '@/core/kernel/_0_domain/invoice.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as Transaction from '@/core/kernel/_0_domain/transaction.ts';

export type Interval = 'month' | 'year';

export interface Bucket {
  key: string; // 'yyyy-MM' ou 'yyyy'
  label: string; // 'Mar/26' ou '2026'
}

export interface EvolutionTx {
  date?: string | null;
  amount?: number | string | null;
  type?: string | null;
  status?: string | null;
  categoryId?: string | null;
  cardId?: string | null;
}

export interface EvolutionRow {
  id: string; // categoryId | 'none:EXPENSE' | 'group:INCOME' | 'group:EXPENSE'
  label: string;
  level: 0 | 1 | 2; // 0 grupo natureza · 1 macro · 2 sub
  nature: 'INCOME' | 'EXPENSE';
  parentId: string | null; // null no nível 0
  hasChildren: boolean;
  values: number[]; // índice por bucket
  total: number;
  average: number; // total / buckets.length
}

export interface EvolutionMatrix {
  buckets: Bucket[];
  rows: EvolutionRow[];
  incomeByBucket: number[];
  expenseByBucket: number[];
  resultByBucket: number[];
}

function pad2(n: number): string {
  return n < 10 ? '0' + n : '' + n;
}

export function buildBuckets(interval: Interval, start: Period.Period, size: number): Bucket[] {
  const buckets: Bucket[] = [];
  let p = start;
  for (let i = 0; i < size; i++) {
    if (interval === 'month') {
      buckets.push({
        key: p.year + '-' + pad2(p.month),
        label: Format.monthLabel(p.month, p.year).substring(0, 3) + '/' + String(p.year).substring(2),
      });
      p = Period.shift(p, 1);
    } else {
      buckets.push({
        key: String(p.year),
        label: String(p.year),
      });
      p = Period.shift(p, 12);
    }
  }
  return buckets;
}

export function bucketKeyOf(interval: Interval, dateStr: string): string {
  const m = /^(\d{4})-(\d{2})/.exec(dateStr);
  if (!m) return '';
  const y = +m[1]!;
  const mo = +m[2]!;
  return interval === 'month' ? y + '-' + pad2(mo) : String(y);
}

export function cardAccountIndex(accounts: readonly Account[]): Record<string, Account> {
  const index: Record<string, Account> = {};
  accounts.forEach((a) => {
    (a.cards || []).forEach((c) => {
      index[String(c.id)] = a;
    });
  });
  return index;
}

export function effectiveDate(tx: EvolutionTx, index: Record<string, Account>): string {
  if (tx.cardId != null && tx.date) {
    const account = index[String(tx.cardId)];
    if (account) {
      return dueDate(account, tx.date);
    }
  }
  return tx.date || '';
}

export function shareOf(value: number, incomeOfBucket: number): number | null {
  if (incomeOfBucket <= 0 || value === 0) return null;
  return Math.abs(value) / incomeOfBucket * 100;
}

export function buildMatrix(
  txs: readonly EvolutionTx[],
  categories: readonly Category.Category[],
  accounts: readonly Account[],
  buckets: Bucket[],
  interval: Interval,
): EvolutionMatrix {
  const bucketKeys = buckets.map((b) => b.key);
  const index = cardAccountIndex(accounts);

  // Mapa: linhaId → EvolutionRow (mutable durante construção)
  const rows: Map<string, EvolutionRow> = new Map();

  // Totalizadores
  const incomeByBucket = buckets.map(() => 0);
  const expenseByBucket = buckets.map(() => 0);

  // Processa cada transação
  txs.forEach((tx) => {
    // Rejeita cancelled
    if (tx.status === 'cancelled') return;

    // Rejeita transferência
    if (Category.isTransferCategory(categories, tx.categoryId)) return;

    // Data efetiva
    let effectiveDateStr = tx.date || '';
    if (tx.cardId != null && tx.date) {
      const account = index[String(tx.cardId)];
      if (account) {
        effectiveDateStr = dueDate(account, tx.date);
      }
    }

    // Bucket
    const bucketKey = bucketKeyOf(interval, effectiveDateStr);
    const bucketIdx = bucketKeys.indexOf(bucketKey);
    if (bucketIdx < 0 || bucketIdx >= buckets.length) return;

    // Valor assinado
    const amt = +(tx.amount ?? 0) || 0;
    const signed = Transaction.isExpense(tx) ? -Math.abs(amt) : +Math.abs(amt);

    // Natureza
    const nature = Transaction.isExpense(tx) ? 'EXPENSE' : 'INCOME';

    // Categoria: se houver, usa a folha; senão linha sintética 'none:<nature>'
    let categoryId = tx.categoryId;
    const cat = categoryId ? Category.byId(categories, categoryId) : null;

    if (!categoryId || !cat) {
      categoryId = 'none:' + nature;
    }

    // Agrupa naquela linha
    addToRow(categoryId, signed, nature, bucketIdx, rows, categories, buckets);

    // Roll-up: ancestrais
    let parent = cat ? Category.byId(categories, cat.parentId) : null;
    while (parent) {
      addToRow(parent.id, signed, nature, bucketIdx, rows, categories, buckets);
      parent = Category.byId(categories, parent.parentId);
    }

    // Roll-up: grupo de natureza
    addToRow('group:' + nature, signed, nature, bucketIdx, rows, categories, buckets);

    // Atualiza totalizadores
    if (bucketIdx >= 0 && bucketIdx < buckets.length) {
      if (signed >= 0) {
        incomeByBucket[bucketIdx]! += signed;
      } else {
        expenseByBucket[bucketIdx]! += signed;
      }
    }
  });

  // Garante linha p/ toda categoria ativa, mesmo sem transação no período
  categories.forEach((cat) => {
    if (!cat.active) return;
    if (Category.isTransferCategory(categories, cat.id)) return;
    const nature = cat.nature === 'INCOME' ? 'INCOME' : 'EXPENSE';
    addToRow(cat.id, 0, nature, 0, rows, categories, buckets);
  });

  // Poda: descarta só linhas sintéticas ('none:*') vazias; categorias reais sempre aparecem
  const allRows = Array.from(rows.values());
  const nonEmptyIds = new Set<string>();

  function markNonEmpty(rowId: string) {
    if (nonEmptyIds.has(rowId)) return;
    nonEmptyIds.add(rowId);
    const row = rows.get(rowId);
    if (row && row.parentId) {
      markNonEmpty(row.parentId);
    }
  }

  allRows.forEach((row) => {
    const isSynthetic = row.id.startsWith('none:');
    if (!isSynthetic || row.values.some((v) => v !== 0)) {
      markNonEmpty(row.id);
    }
  });

  // Grupos de natureza aparecem sempre
  nonEmptyIds.add('group:INCOME');
  nonEmptyIds.add('group:EXPENSE');

  // Filtra linhas visíveis
  const visibleRows = allRows.filter((row) => nonEmptyIds.has(row.id));

  // Ordena hierarquicamente: grupo → macro (alfabético) → suas subs (alfabético)
  const result: EvolutionRow[] = [];
  const byId = new Map(visibleRows.map((r) => [r.id, r] as const));
  const childrenOf = new Map<string, EvolutionRow[]>();
  visibleRows.forEach((r) => {
    if (r.parentId == null) return;
    const list = childrenOf.get(r.parentId) ?? [];
    list.push(r);
    childrenOf.set(r.parentId, list);
  });
  childrenOf.forEach((list) => list.sort((a, b) => a.label.localeCompare(b.label, 'pt-BR')));

  function appendWithChildren(row: EvolutionRow) {
    result.push(row);
    (childrenOf.get(row.id) ?? []).forEach(appendWithChildren);
  }

  (['INCOME', 'EXPENSE'] as const).forEach((nature) => {
    const group = byId.get('group:' + nature);
    if (group) appendWithChildren(group);
  });

  // Computa average para cada linha
  result.forEach((row) => {
    row.average = row.total / buckets.length;
  });

  // Computa resultados
  const resultByBucket = buckets.map((_, i) => (incomeByBucket[i] ?? 0) + (expenseByBucket[i] ?? 0));

  return {
    buckets,
    rows: result,
    incomeByBucket,
    expenseByBucket,
    resultByBucket,
  };
}

// Helper: adiciona/atualiza valor numa linha
function addToRow(
  rowId: string,
  signed: number,
  nature: 'INCOME' | 'EXPENSE',
  bucketIdx: number,
  rows: Map<string, EvolutionRow>,
  categories: readonly Category.Category[],
  buckets: Bucket[],
) {
  let row = rows.get(rowId);
  if (!row) {
    const isGroup = rowId.startsWith('group:');
    const isNone = rowId.startsWith('none:');
    const cat = !isGroup && !isNone ? Category.byId(categories, rowId) : null;

    let level: 0 | 1 | 2 = 1;
    let label = '';
    let parentId: string | null = null;
    let hasChildren = false;

    if (isGroup) {
      level = 0;
      label = nature === 'INCOME' ? 'Receitas' : 'Despesas';
      parentId = null;
      hasChildren = true;
    } else if (isNone) {
      level = 2;
      label = 'Sem categoria';
      parentId = 'group:' + nature;
      hasChildren = false;
    } else if (cat) {
      if (cat.parentId == null) {
        level = 1;
        hasChildren = Category.childrenOf(categories, cat.id).length > 0;
      } else {
        level = 2;
        hasChildren = false;
      }
      label = cat.name;
      parentId = cat.parentId || ('group:' + nature);
    }

    row = {
      id: rowId,
      label,
      level,
      nature,
      parentId,
      hasChildren,
      values: buckets.map(() => 0),
      total: 0,
      average: 0,
    };
    rows.set(rowId, row);
  }

  const val = row.values[bucketIdx];
  if (val != null) {
    row.values[bucketIdx] = val + signed;
    row.total += signed;
  }
}
