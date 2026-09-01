/**
 * API请求模块 - 使用共享拦截器
 */
import { setupInterceptors, createApiMethods } from '../../../shared/api-interceptor'
import router from '@/router'
import { getToken, removeToken, removeUser } from '@/utils/auth'

// 设置拦截器
setupInterceptors({
  getToken: () => getToken(),
  onUnauthorized: () => {
    removeToken()
    removeUser()
    const currentPath = router.currentRoute.fullPath
    router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login')
  }
})

// 创建API方法并导出
const apiMethods = createApiMethods(process.env.VUE_APP_BBS_API)
export const { postRequest, putRequest, getRequest, deleteRequest } = apiMethods