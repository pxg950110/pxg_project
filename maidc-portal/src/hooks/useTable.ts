import { ref, reactive } from 'vue'
import type { ApiResponse, PageResult } from '@/utils/request'

export function useTable<T>(
  fetchFn: (params: { page: number; pageSize: number }) => Promise<{ data: ApiResponse<PageResult<T>> }>,
) {
  const tableData = ref<T[]>([]) as any
  const loading = ref(false)
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
      const params = {
        page: extra?.page ?? pagination.current,
        pageSize: extra?.pageSize ?? pagination.pageSize,
      }
      const res = await fetchFn(params)
      tableData.value = res.data.data.items
      pagination.total = res.data.data.total
      pagination.current = res.data.data.page
    } finally {
      loading.value = false
    }
  }

  function handleTableChange(pag: any) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    fetchData({ page: pag.current, pageSize: pag.pageSize })
  }

  return { tableData, loading, pagination, fetchData, handleTableChange }
}
