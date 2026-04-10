declare namespace API {
  interface Response<T = any> {
    code: number
    message: string
    data: T
    traceId: string
  }

  interface PageResult<T = any> {
    items: T[]
    total: number
    page: number
    pageSize: number
    totalPages: number
  }

  interface PageParams {
    page?: number
    pageSize?: number
  }
}

export type { API }
