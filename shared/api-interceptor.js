/**
 * API拦截器共享模块
 * 用于统一bbs-ui和bbs-admin-ui的axios拦截器逻辑
 */
import axios from 'axios'
import { Message } from 'element-ui'

/**
 * 构建错误信息，开发环境下附带后端详情方便 debug
 * @param {Object} resData 后端返回的错误数据
 * @returns {string} 错误信息
 */
function buildErrorMessage(resData) {
  if (!resData) return ''
  let msg = resData.message || ''
  // 后端额外返回 detail / traceId 时，在开发环境展示（方便定位问题）
  if (process.env.NODE_ENV !== 'production') {
    const detail = resData.detail || resData.traceId
    if (detail) {
      msg = msg ? `${msg}（${detail}）` : detail
    }
  }
  return msg
}

/**
 * 设置axios拦截器
 * @param {Object} options 配置选项
 * @param {Function} options.getToken 获取token的函数
 * @param {Function} options.onUnauthorized 401时的处理函数（如跳转登录）
 * @param {Function} options.onForbidden 403时的处理函数
 */
export function setupInterceptors({
  getToken = () => null,
  onUnauthorized = () => {},
  onForbidden = () => {}
} = {}) {
  // 请求拦截器
  axios.interceptors.request.use(
    config => {
      const token = getToken()
      if (token) {
        config.headers['Authorization'] = token
      }
      return config
    },
    error => {
      console.error('[Request Error]', error)
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  axios.interceptors.response.use(
    success => {
      // 业务逻辑错误
      if (success.status && success.status === 200) {
        if (success.data.code === 500) {
          Message({
            type: 'warning',
            message: buildErrorMessage(success.data),
            showClose: true,
            offset: 54
          })
          return
        }
        if (success.data.code === 401) {
          onUnauthorized()
          return
        }
        if (success.data.code === 403) {
          // 业务级403：仅提示，不跳登录（可配置）
          Message({
            type: 'error',
            message: buildErrorMessage(success.data) || '权限不足，请联系管理员！',
            showClose: true,
            offset: 54
          })
          return
        }
        // 注意：成功提示已移至 shared/feedback.js 的 handleResponse 中统一处理
        // 拦截器不再自动弹出成功 Toast，避免与组件的 Toast 重复
      }
      return success.data
    },
    error => {
      // 优先显示后端返回的错误信息，方便 debug；没有则兜底通用文案
      const resData = error.response && error.response.data
      const status = error.response && error.response.status
      const resMsg = buildErrorMessage(resData)
      // 始终将完整错误打印到控制台供排查
      console.error('[API Error]', status, resData || error.message)
      
      if (status === 504 || status === 404) {
        Message({
          type: 'error',
          message: resMsg || '服务器错误',
          showClose: true,
          offset: 54
        })
      } else if (status === 403) {
        Message({
          type: 'error',
          message: resMsg || '权限不足，请联系管理员！',
          showClose: true,
          offset: 54
        })
      } else if (status === 401) {
        onUnauthorized()
      } else {
        Message({
          type: 'error',
          message: resMsg || '未知错误！',
          showClose: true,
          offset: 54
        })
      }
      return
    }
  )
}

/**
 * 创建通用的请求方法
 * @param {string} baseURL API基础路径
 * @param {Object} options 配置选项
 * @param {boolean} options.appendParamsToUrl 是否将params追加到URL路径（如 /api/user/123）
 * @returns {Object} 包含postRequest、putRequest、getRequest、deleteRequest等方法
 */
export function createApiMethods(baseURL, options = {}) {
  const { appendParamsToUrl = false } = options
  
  const request = (method, url, params) => {
    const config = {
      method,
      url: `${baseURL}${url}`
    }
    
    const methodLower = method.toLowerCase()
    // appendParamsToUrl 仅对 GET/DELETE 生效（路径参数如 /user/123）
    // POST/PUT 的 params 始终放在请求体中
    if (appendParamsToUrl && params !== undefined && (methodLower === 'get' || methodLower === 'delete')) {
      config.url = `${config.url}/${params}`
    } else {
      config.data = params
    }
    
    return axios(config)
  }

  return {
    postRequest: (url, params) => request('post', url, params),
    putRequest: (url, params) => request('put', url, params),
    getRequest: (url, params) => request('get', url, params),
    deleteRequest: (url, params) => request('delete', url, params),
    
    // 无路径参数的get请求
    getRequestUrl: (url) => {
      return axios({
        method: 'get',
        url: `${baseURL}${url}`
      })
    },
    
    // 上传文件
    uploadFile: (url, formData) => {
      return axios({
        method: 'post',
        url: `${baseURL}${url}`,
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    },
    
    // 下载文件
    downloadFile: (url, params) => {
      const query = params
        ? '?' + Object.entries(params)
            .filter(([, v]) => v !== undefined && v !== null && v !== '')
            .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
            .join('&')
        : ''
      return axios({
        method: 'get',
        url: `${baseURL}${url}${query}`,
        responseType: 'blob'
      })
    }
  }
}