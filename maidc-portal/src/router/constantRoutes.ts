import AppLayout from '@/layout/AppLayout.vue'

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: { title: '登录' },
  },
  { path: '/403', name: '403', component: () => import('@/views/error/403.vue') },
  { path: '/404', name: '404', component: () => import('@/views/error/404.vue') },
  { path: '/500', name: '500', component: () => import('@/views/error/500.vue') },
  {
    path: '/',
    name: 'Root',
    component: AppLayout,
    redirect: '/dashboard/workspace',
    children: [
      {
        path: 'redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue'),
      },
    ],
  },
]
