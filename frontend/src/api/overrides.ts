import type { UUID } from './types.ts';

/**
 * Três pontos onde a geração a partir do OpenAPI está errada ou incompleta — escritos à mão.
 * Ver `.claude/plan.md`, Fase 1.
 */

// 1. Transaction.Status/Type vão na rede em lowercase (JsonStorageConfig, Jackson custom), mas o
// enum Java é uppercase e é dele que o SmallRye deriva o schema gerado. `planed` é o typo real.
export type TransactionType = 'income' | 'expense' | 'transfer';
export type TransactionStatus = 'pending' | 'scheduled' | 'confirmed' | 'planed' | 'balance' | 'cancelled';

// 2. CategoryResponse.nature e AccountResponse.type SÃO uppercase de verdade — a inconsistência
// entre entidades é real e não é uniformizada aqui (o gerado já cobre esses dois campos).

// 3. POST /accounts/transactions/import/preview é `Object` no Java (RequestMapper.toResponseBody
// faz switch); na prática é união discriminada por `documentType`
// (br-application/.../f007/_2_infrastructure/web/response/{ImportPreviewResponse,BankStatementPreviewResponse}.java).
export interface ImportPreviewResponse {
  documentType: 'CREDIT_CARD_INVOICE';
  issuer: string;
  last4s: string[];
  rows: ImportPreviewRow[];
  candidateCards: ImportPreviewCardOption[];
  closingPeriod: string | null;
}

export interface ImportPreviewRow {
  last4: string;
  date: string;
  originalDate: string;
  description: string;
  amount: number;
  installmentNumber: number | null;
  installmentTotal: number | null;
  groupId: UUID | null;
  status: TransactionStatus;
  type: TransactionType;
  duplicate: boolean;
  closed: boolean;
  categoryId: UUID | null;
  costCenterId: UUID | null;
  suggestedCardId: UUID | null;
}

export interface ImportPreviewCardOption {
  id: UUID;
  name: string;
  last4: string | null;
}

export interface BankStatementPreviewResponse {
  documentType: 'BANK_STATEMENT';
  issuer: string;
  candidateAccounts: BankStatementAccountOption[];
  selectedAccountId: UUID | null;
  rows: BankStatementPreviewRow[];
  closingPeriod: string | null;
}

export interface BankStatementPreviewRow {
  date: string;
  description: string;
  amount: number;
  type: TransactionType;
  state: 'NEW' | 'DUPLICATE' | 'RECONCILE';
  closed: boolean;
  categoryId: UUID | null;
  costCenterId: UUID | null;
  reconcileDescription: string | null;
}

export interface BankStatementAccountOption {
  id: UUID;
  name: string;
}

export type ImportPreview = ImportPreviewResponse | BankStatementPreviewResponse;
