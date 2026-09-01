/**
 * API请求模块 - 使用共享拦截器
 */
import { setupInterceptors, createApiMethods } from '../../../shared/api-interceptor'
import router from '@/router'

// 设置拦截器
setupInterceptors({
  getToken: () => window.sessionStorage.getItem('tokenStr'),
  onUnauthorized: () => {
    Message({
      type: 'error',
      message: '登录已过期，请重新登录！',
      showClose: true,
    })
    const currentPath = router.currentRoute.fullPath
    router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login')
  },
  noSuccessTipUrls: [], // 管理后台所有POST操作都显示成功提示
  noSuccessTipForGet: false // GET请求也显示成功提示（如果后端返回message）
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