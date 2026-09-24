import { ref, reactive } from 'vue'
import type { ApiResponse, PageResult } from '@/utils/request'

export function useTable<T>(
  fetchFn: (params: Record<string, any>) => Promise<{ data: ApiResponse<PageResult<T>> }>,
) {
  const tableData = ref<T[]>([]) as any
  const loading = ref(false)
  const searchParams = ref<Record<string, any>>({})
  const pagination = reactive({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条`,
  })

  async function fetchData(extra?: { page?: number; pageSize?: number }) {
    loading.value = true
    try {
      const params: Record<string, any> = {
        page: extra?.page ?? pagination.current,
        page_size: extra?.pageSize ?? pagination.pageSize,
        ...searchParams.value,
      }
      const res = await fetchFn(params)
      tableData.value = res.data.data.items
      pagination.total = res.data.data.total ?? 0
      pagination.current = res.data.data.page ?? 1
    } finally {
      loading.value = false
    }
  }

  function setSearchParams(params: Record<string, any>) {
    searchParams.value = { ...params }
    pagination.current = 1
  }

  function handleTableChange(pag: any) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    fetchData({ page: pag.current, pageSize: pag.pageSize })
  }

  return { tableData, loading, pagination, fetchData, handleTableChange, setSearchParams }
}
