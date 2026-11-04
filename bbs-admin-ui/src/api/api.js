/**
 * API请求模块 - 使用共享拦截器
 */
import { setupInterceptors, createApiMethods } from '../../../shared/api-interceptor'
import router from '@/router'
import adminStore from '@/utils/adminStore'
import { requestBadgeRefresh, shouldRefreshBadges } from '@/utils/badgeRefresh'

// 判断当前会话是否具备管理端访问资格（由权限组授予 admin:access 决定）
function hasAdminRole() {
  return adminStore.hasPermission('admin:access')
}

// 403 统一弹窗提示：避免整页跳转打断当前操作
let forbiddenDialogVisible = false
function showForbiddenDialog(message) {
  if (forbiddenDialogVisible) return
  forbiddenDialogVisible = true
  const done = () => { forbiddenDialogVisible = false }
  MessageBox.alert(message || '当前账号没有执行该操作的权限，请联系管理员。', '没有权限', {
    type: 'warning',
    confirmButtonText: '知道了',
    callback: done
  }).catch(done)
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
  // 403：非管理端账号（如误用普通用户 Token）清理登录态；合法管理员仅弹窗提示
  onForbidden: (data) => {
    const message = data && data.message ? String(data.message) : ''
    // AdminAccessFilter 明确返回“无权访问管理后台”：说明当前会话已失去 admin:access，
    // 立即退出，避免后台轮询/并发请求反复弹“没有权限”。
    const lostAdminAccess = message.indexOf('无权访问管理后台') !== -1
    if (lostAdminAccess || !hasAdminRole()) {
      if (lostAdminAccess) {
        Message({
          type: 'warning',
          message: '管理端权限已失效，请重新登录',
          showClose: true
        })
      }
      adminStore.logout()
      const currentPath = router.currentRoute.fullPath
      router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login')
      return true
    }
    // 首次登录/密码被重置：后端会拒绝管理接口，统一引导去改密
    if (adminStore.needsPasswordChange()) {
      if (router.currentRoute.path !== '/change-password') {
        router.replace('/change-password')
      }
      return true
    }
    showForbiddenDialog(message || undefined)
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
import { Message, MessageBox } from 'element-ui'