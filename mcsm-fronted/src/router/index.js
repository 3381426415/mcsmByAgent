import { createRouter, createWebHistory } from 'vue-router'
import login from '../pages/user/login.vue'
import register from '../pages/user/register.vue'
import userPanel from '@/pages/user/userPanel.vue'
import request from '@/utils/request'
import adminPanel from '@/pages/admin/adminPanel.vue'
import baned from '@/components/baned.vue'


const routes = [
  {
    path: '/',
    name: 'login',
    component: login
  },
  {
    path: '/register',
    name: 'register',
    component: register
  },
  {
    path: '/setup',
    name: 'setup',
    component: () => import('@/pages/setup.vue')
  },
   {
    path: '/baned',
    name: 'baned',
    component: baned
  },
  {
    path: '/userPanel',
    name: 'userPanel',
    meta: { requiresAuth: true },
    component: userPanel,
    redirect: '/userPanel/accountSetting',
    children: [
      {
        path: 'accountSetting',
        component: () => import('@/pages/user/user-menu/accountSetting.vue')
      },
      {
        path: 'myPackage',
        component: () => import('@/pages/user/user-menu/myPackage.vue')
      },
      {
        path: 'personalTradeRecord',
        component: () => import('@/pages/user/user-menu/personalTradeRecord.vue')
      },
      {
        path: 'tradeMarket',
        component: () => import('@/pages/user/user-menu/tradeMarket.vue')
      }
    ]
  },
   {
    path: '/adminPanel',
    name: 'adminPanel',
    component: adminPanel,
    redirect: '/adminPanel/serverM',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'marketM',
        component: () => import('@/pages/admin/admin-menu/marketM.vue')
      },
      {
        path: 'offlinePlayerM',
        component: () => import('@/pages/admin/admin-menu/offlinePlayerM.vue')
      },
      {
        path: 'onlinePlayerM',
        component: () => import('@/pages/admin/admin-menu/onlinePlayerM.vue')
      },
      {
        path: 'serverM',
        component: () => import('@/pages/admin/admin-menu/serverM.vue')
      },
      {
        path: 'serverM/:serverId',
        component: () => import('@/pages/admin/admin-menu/serverM/serverDetail.vue')
      },
      {
        path: 'pluginM',
        component: () => import('@/pages/admin/admin-menu/pluginM.vue'),
      },
      {
        path: 'announcementM',
        component: () => import('@/pages/admin/admin-menu/announcementM.vue'),
      },
      {
        path: 'logM',
        component: () => import('@/pages/admin/admin-menu/logM.vue')
      },
      {
        path: 'frpM',
        component: () => import('@/pages/admin/admin-menu/frpM.vue')
      },
      {
        path: 'userM',
        component: () => import('@/pages/admin/admin-menu/userM.vue'),
        redirect: '/adminPanel/userM/accountM',
        children : [
         {
          path : 'accountM',
          component : ()=> import('@/pages/admin/admin-menu/userM/accountM.vue')
         },
         {
          path : 'roleM',
          component : ()=> import('@/pages/admin/admin-menu/userM/roleM.vue')
         }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  // 0. 安装向导页面直接放行
  if (to.name === 'setup') {
    return next();
  }

  // 1. 检查安装状态
  try {
    const setupRes = await request.get('/api/setup/status', { silent: true });
    if (setupRes.data && !setupRes.data.setupComplete) {
      return next({ name: 'setup' });
    }
  } catch (e) {
    // 接口不可用时放行，避免阻塞
  }

  const token = localStorage.getItem('token');
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

  // 2. 访问受限页面且没 Token -> 拦住
  if (requiresAuth && !token) {
    return next({ name: 'login' });
  }

  // 3. 已登录状态下想回登录页 -> 重定向到对应面板
  if (to.name === 'login' && token) {
    try {
        await request.get("/api/verify");

        const permissions = JSON.parse(
            localStorage.getItem("user_permissions") || "[]"
        );

        // 基于权限码判断跳转
        if (permissions.some(p => p.startsWith('admin:'))) {
            return next({ name: 'adminPanel' });
        } else {
            return next({ name: 'userPanel' });
        }
    } catch (error) {
        return next();
    }
}

  // 4. 其他情况放行
  next();
});

export default router;