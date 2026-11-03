export type ApiFieldErrors = Record<string, string | string[]>;

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** Error envelope expected from the Spring Boot API. */
export interface ApiErrorResponse {
  code?: string;
  message?: string;
  errors?: ApiFieldErrors;
  path?: string;
  timestamp?: string;
}

export type ApiErrorKind = 'api' | 'network' | 'unknown';

/** Infrastructure-level error. Feature-specific error codes remain available in `data`. */
export class ApiClientError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  readonly code?: string;
  readonly data?: ApiErrorResponse;
  readonly retryAfterSeconds?: number;

  constructor(options: {
    message: string;
    kind: ApiErrorKind;
    status?: number;
    code?: string;
    data?: ApiErrorResponse;
    retryAfterSeconds?: number;
    cause?: unknown;
  }) {
    super(options.message, { cause: options.cause });
    this.name = 'ApiClientError';
    this.kind = options.kind;
    this.status = options.status;
    this.code = options.code;
    this.data = options.data;
    this.retryAfterSeconds = options.retryAfterSeconds;
  }
}
