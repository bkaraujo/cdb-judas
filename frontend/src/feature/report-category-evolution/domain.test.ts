import { describe, expect, it } from 'vitest';
import type { Account } from '@/core/kernel/_0_domain/account.ts';
import * as Period from '@/core/kernel/_0_domain/period.ts';
import * as Domain from '@/feature/report-category-evolution/domain.ts';
import type { Category } from '@/core/kernel/_0_domain/category.ts';

describe('feature:report-category-evolution — domain', () => {
  it('buildBuckets mensal gera 3 períodos corretos', () => {
    const buckets = Domain.buildBuckets('month', Period.create(11, 2025), 3);
    expect(buckets).toHaveLength(3);
    expect(buckets[0]!.key).toBe('2025-11');
    expect(buckets[1]!.key).toBe('2025-12');
    expect(buckets[2]!.key).toBe('2026-01');
  });

  it('buildBuckets anual gera 3 períodos corretos', () => {
    const buckets = Domain.buildBuckets('year', Period.create(6, 2024), 3);
    expect(buckets).toHaveLength(3);
    expect(buckets[0]!.key).toBe('2024');
    expect(buckets[1]!.key).toBe('2025');
    expect(buckets[2]!.key).toBe('2026');
  });

  it('bucketKeyOf extrai chave correta para month', () => {
    expect(Domain.bucketKeyOf('month', '2026-03-15')).toBe('2026-03');
    expect(Domain.bucketKeyOf('month', '2025-12-01')).toBe('2025-12');
  });

  it('bucketKeyOf extrai chave correta para year', () => {
    expect(Domain.bucketKeyOf('year', '2026-03-15')).toBe('2026');
    expect(Domain.bucketKeyOf('year', '2025-12-01')).toBe('2025');
  });

  it('cardAccountIndex mapeia cardId para Account', () => {
    const accounts: Account[] = [
      {
        id: 'acc1',
        name: 'Acc1',
        type: 'CHECKING',
        balance: 0,
        currentBalance: 0,
        color: null,
        active: true,
        creditLimit: 0,
        overdraftLimit: 0,
        closingDay: null,
        dueDay: null,
        cards: [
          { id: 'card1', last4: '1111', accountId: 'acc1', active: true },
          { id: 'card2', last4: '2222', accountId: 'acc1', active: true },
        ],
      },
      {
        id: 'acc2',
        name: 'Acc2',
        type: 'CHECKING',
        balance: 0,
        currentBalance: 0,
        color: null,
        active: true,
        creditLimit: 0,
        overdraftLimit: 0,
        closingDay: null,
        dueDay: null,
        cards: [
          { id: 'card3', last4: '3333', accountId: 'acc2', active: true },
        ],
      },
    ];

    const index = Domain.cardAccountIndex(accounts);
    expect(index['card1']).toBe(accounts[0]);
    expect(index['card2']).toBe(accounts[0]);
    expect(index['card3']).toBe(accounts[1]);
  });

  it('effectiveDate retorna data original sem cartão', () => {
    const tx: Domain.EvolutionTx = { date: '2026-03-15', cardId: null };
    const date = Domain.effectiveDate(tx, {});
    expect(date).toBe('2026-03-15');
  });

  it('shareOf retorna nulo quando income <= 0 ou value === 0', () => {
    expect(Domain.shareOf(100, 0)).toBeNull();
    expect(Domain.shareOf(0, 100)).toBeNull();
    expect(Domain.shareOf(100, -50)).toBeNull();
  });

  it('shareOf calcula percentual corretamente', () => {
    const result = Domain.shareOf(-67572.13, 85906.07);
    expect(result).toBeCloseTo(78.66, 1);
  });

  it('buildMatrix filtra status cancelled', () => {
    const txs: Domain.EvolutionTx[] = [
      { date: '2026-03-01', amount: 100, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
      { date: '2026-03-02', amount: 50, type: 'expense', status: 'cancelled', categoryId: 'cat1' },
    ];
    const buckets = Domain.buildBuckets('month', Period.create(3, 2026), 1);
    const categories: Category[] = [
      { id: 'cat1', name: 'Cat 1', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
    ];
    const accounts: Account[] = [];

    const matrix = Domain.buildMatrix(txs, categories, accounts, buckets, 'month');
    const catRow = matrix.rows.find((r) => r.id === 'cat1');
    expect(catRow?.total).toBe(-100); // Só a não-cancelled
  });

  it('buildMatrix computa valores assinados corretamente', () => {
    const txs: Domain.EvolutionTx[] = [
      { date: '2026-03-01', amount: 100, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
      { date: '2026-03-01', amount: 50, type: 'income', status: 'confirmed', categoryId: 'cat2' },
    ];
    const buckets = Domain.buildBuckets('month', Period.create(3, 2026), 1);
    const categories: Category[] = [
      { id: 'cat1', name: 'Cat 1', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
      { id: 'cat2', name: 'Cat 2', nature: 'INCOME', parentId: null, isSystem: false, active: true },
    ];
    const accounts: Account[] = [];

    const matrix = Domain.buildMatrix(txs, categories, accounts, buckets, 'month');
    const expRow = matrix.rows.find((r) => r.id === 'cat1');
    const incRow = matrix.rows.find((r) => r.id === 'cat2');
    expect(expRow?.total).toBe(-100);
    expect(incRow?.total).toBe(50);
  });

  it('buildMatrix cria linha none:<nature> para categoria nula', () => {
    const txs: Domain.EvolutionTx[] = [
      { date: '2026-03-01', amount: 100, type: 'expense', status: 'confirmed', categoryId: null },
    ];
    const buckets = Domain.buildBuckets('month', Period.create(3, 2026), 1);
    const categories: Category[] = [];
    const accounts: Account[] = [];

    const matrix = Domain.buildMatrix(txs, categories, accounts, buckets, 'month');
    const noneRow = matrix.rows.find((r) => r.id === 'none:EXPENSE');
    expect(noneRow).toBeDefined();
    expect(noneRow?.label).toBe('Sem categoria');
    expect(noneRow?.level).toBe(2);
  });

  it('buildMatrix inclui categoria ativa mesmo sem transação no período', () => {
    const txs: Domain.EvolutionTx[] = [
      { date: '2026-03-01', amount: 100, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
    ];
    const buckets = Domain.buildBuckets('month', Period.create(3, 2026), 1);
    const categories: Category[] = [
      { id: 'cat1', name: 'Cat 1', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
      { id: 'cat2', name: 'Cat 2', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
      { id: 'cat3', name: 'Cat 3 (inativa)', nature: 'EXPENSE', parentId: null, isSystem: false, active: false },
    ];
    const accounts: Account[] = [];

    const matrix = Domain.buildMatrix(txs, categories, accounts, buckets, 'month');
    const cat2Row = matrix.rows.find((r) => r.id === 'cat2');
    expect(cat2Row).toBeDefined();
    expect(cat2Row?.total).toBe(0);
    expect(matrix.rows.find((r) => r.id === 'cat3')).toBeUndefined();
  });

  it('buildMatrix calcula average como total / buckets.length', () => {
    const txs: Domain.EvolutionTx[] = [
      { date: '2026-03-01', amount: 60, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
      { date: '2026-04-01', amount: 120, type: 'expense', status: 'confirmed', categoryId: 'cat1' },
    ];
    const buckets = Domain.buildBuckets('month', Period.create(3, 2026), 2);
    const categories: Category[] = [
      { id: 'cat1', name: 'Cat 1', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
    ];
    const accounts: Account[] = [];

    const matrix = Domain.buildMatrix(txs, categories, accounts, buckets, 'month');
    const catRow = matrix.rows.find((r) => r.id === 'cat1');
    expect(catRow?.total).toBe(-180);
    expect(catRow?.average).toBe(-90);
  });

  it('buildMatrix computa incomeByBucket e expenseByBucket', () => {
    const txs: Domain.EvolutionTx[] = [
      { date: '2026-03-01', amount: 100, type: 'income', status: 'confirmed', categoryId: 'cat1' },
      { date: '2026-03-01', amount: 50, type: 'expense', status: 'confirmed', categoryId: 'cat2' },
    ];
    const buckets = Domain.buildBuckets('month', Period.create(3, 2026), 1);
    const categories: Category[] = [
      { id: 'cat1', name: 'Cat 1', nature: 'INCOME', parentId: null, isSystem: false, active: true },
      { id: 'cat2', name: 'Cat 2', nature: 'EXPENSE', parentId: null, isSystem: false, active: true },
    ];
    const accounts: Account[] = [];

    const matrix = Domain.buildMatrix(txs, categories, accounts, buckets, 'month');
    expect(matrix.incomeByBucket[0]).toBe(100);
    expect(matrix.expenseByBucket[0]).toBe(-50);
    expect(matrix.resultByBucket[0]).toBe(50);
  });
});
