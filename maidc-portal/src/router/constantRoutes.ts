import BasicLayout from '@/layouts/BasicLayout.vue'

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
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue'),
      },
      // ===== 专病管理（CRS 试点）页面原型，仅设计评审用，不进菜单，实现后删除 =====
      {
        path: 'proto/crs/detail',
        name: 'ProtoCrsDetail',
        meta: { title: '[原型] 专病详情重构' },
        component: () => import('@/views/prototype/crs/CrsDiseaseDetail.vue'),
      },
      {
        path: 'proto/crs/workbench',
        name: 'ProtoCrsWorkbench',
        meta: { title: '[原型] 随访工作台' },
        component: () => import('@/views/prototype/crs/CrsWorkbench.vue'),
      },
      {
        path: 'proto/crs/scales',
        name: 'ProtoCrsScales',
        meta: { title: '[原型] 量表管理' },
        component: () => import('@/views/prototype/crs/CrsScales.vue'),
      },
      {
        path: 'proto/crs/scale-designer',
        name: 'ProtoCrsScaleDesigner',
        meta: { title: '[原型] 量表设计器' },
        component: () => import('@/views/prototype/crs/ScaleDesigner.vue'),
      },
      {
        path: 'proto/crs/archive/:fid',
        name: 'ProtoCrsArchive',
        meta: { title: '[原型] 随访档案' },
        component: () => import('@/views/prototype/crs/CrsArchiveDetail.vue'),
      },
      // ===== 首页工作台 v2 原型，仅设计评审用，不进菜单，实现后删除 =====
      {
        path: 'proto/workspace',
        name: 'ProtoWorkspaceV2',
        meta: { title: '[原型] 首页工作台v2' },
        component: () => import('@/views/prototype/workspace/WorkspaceV2.vue'),
      },
    ],
  },
]
