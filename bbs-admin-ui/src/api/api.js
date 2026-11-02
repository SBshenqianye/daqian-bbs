/**
 * API请求模块 - 使用共享拦截器
 */
import { setupInterceptors, createApiMethods } from '../../../shared/api-interceptor'
import router from '@/router'
import adminStore from '@/utils/adminStore'
import { requestBadgeRefresh, shouldRefreshBadges } from '@/utils/badgeRefresh'

// 判断当前会话是否具备管理端账号角色（ADMIN / SUPER_ADMIN / MODERATOR）
function hasAdminRole() {
  const role = adminStore.getRole()
  return role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'MODERATOR'
}

// 设置拦截器
setupInterceptors({
  getToken: () => window.sessionStorage.getItem('tokenStr'),
  onUnauthorized: () => {
    Message({
      type: 'error',
      message: '登录已过期，请重新登录！',
      showClose: true,
    })
    adminStore.logout()
    const currentPath = router.currentRoute.fullPath
    router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login')
  },
  // 403：非管理端账号（如误用普通用户 Token）清理登录态；合法管理员仅跳 403，避免误退出
  onForbidden: () => {
    if (!hasAdminRole()) {
      adminStore.logout()
      const currentPath = router.currentRoute.fullPath
      router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login')
      return true
    }
    if (router.currentRoute.path !== '/403') {
      const from = router.currentRoute.fullPath
      router.replace({ path: '/403', query: { from } })
    }
    return true
  },
  // 待办类操作成功后刷新侧栏红点（#9：处理完待办数字不更新）
  onResponseSuccess: (data, config) => {
    if (shouldRefreshBadges(config)) requestBadgeRefresh()
  }
})

// 创建API方法并导出
const apiMethods = createApiMethods(process.env.VUE_APP_BBS_API, {
  appendParamsToUrl: true // GET和DELETE请求将params追加到URL路径
})

export const {
  postRequest,
  putRequest,
  getRequest,
  deleteRequest,
  getRequestUrl,
  uploadFile,
  downloadFile
} = apiMethods

// 需要导入Message
import { Message } from 'element-ui'