/**
 * Espelha `br.cdb.core.web.error.ProblemDetail` (RFC 7807) e o que
 * `web/core/kernel/_2_infrastructure/secondary/http-client.js:69-77` já lê do corpo de erro.
 */
export class ApiError extends Error {
  status: number;
  code?: string;
  count?: number;

  constructor(message: string, status: number, code?: string, count?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    if (code !== undefined) this.code = code;
    if (count !== undefined) this.count = count;
  }
}
