import { usePermissionStore } from '@/stores/permission'

export function usePermission() {
  const permissionStore = usePermissionStore()

  function hasPermission(code: string): boolean {
    return permissionStore.hasPermission(code)
  }

  return { hasPermission }
}
