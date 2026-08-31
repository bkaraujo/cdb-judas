import type {components} from '@/api/schema.d.ts';

export type AccountRequest = components['schemas']['AccountRequest'];
export type AccountResponse = components['schemas']['AccountResponse'];
export type BalanceResponse = components['schemas']['BalanceResponse'];
export type Card = components['schemas']['Card'];
export type CardRequest = components['schemas']['CardRequest'];
export type CardResponse = components['schemas']['CardResponse'];
export type CardStatusRequest = components['schemas']['CardStatusRequest'];
export type CategoryResponse = components['schemas']['CategoryResponse'];
export type ClosingRequest = components['schemas']['ClosingRequest'];
export type ClosingResponse = components['schemas']['ClosingResponse'];
export type CreateRequest = components['schemas']['CreateRequest'];
export type HistoricalResult = components['schemas']['HistoricalResult'];
export type ImportRule = components['schemas']['ImportRule'];
export type ImportRuleRequest = components['schemas']['ImportRuleRequest'];
export type LocalDate = components['schemas']['LocalDate'];
export type LocalDateTime = components['schemas']['LocalDateTime'];
export type LoginRequest = components['schemas']['LoginRequest'];
export type MonthlyResult = components['schemas']['MonthlyResult'];
// PatchStatusRequest: ver api/overrides.ts — mesmo caso de TransactionRequest (status lowercase).
export type Preferences = components['schemas']['Preferences'];
export type PreferencesRequest = components['schemas']['PreferencesRequest'];
export type Row = components['schemas']['Row'];
export type SelfResponse = components['schemas']['SelfResponse'];
export type StatementConfirmRequest = components['schemas']['StatementConfirmRequest'];
export type Tag = components['schemas']['Tag'];
export type TagRequest = components['schemas']['TagRequest'];
// TransactionRequest/TransactionResponse: ver api/overrides.ts — status/type vão na rede em
// lowercase, o gerado (uppercase) está errado; corrigidos lá para não formar ciclo de import.
export type TransferCategoryResponse = components['schemas']['TransferCategoryResponse'];
export type TransferRequest = components['schemas']['TransferRequest'];
export type UpdateMeRequest = components['schemas']['UpdateMeRequest'];
export type UpdateRequest = components['schemas']['UpdateRequest'];
export type UUID = components['schemas']['UUID'];
export type YearMonth = components['schemas']['YearMonth'];
