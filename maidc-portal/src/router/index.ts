import { createRouter, createWebHistory } from 'vue-router'
import { constantRoutes } from './constantRoutes'
import { setupGuards } from './guards'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

setupGuards(router)
export default router
