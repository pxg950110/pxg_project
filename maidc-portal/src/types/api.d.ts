declare namespace API {
  interface Response<T = any> {
    code: number
    message: string
    data: T
    traceId: string
  }

  interface PageResult<T = any> {
    content: T[]
    totalElements: number
    totalPages: number
    number: number
    size: number
    first: boolean
    last: boolean
    empty: boolean
    // Legacy fields for backward compatibility
    items?: T[]
    total?: number
    page?: number
    pageSize?: number
  }

  interface PageParams {
    page?: number
    pageSize?: number
  }
}

export type { API }
