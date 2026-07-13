// 引入共享的请求方法（自带拦截器：自动附加 token、统一错误处理）
import { postRequest } from '@/api/api'

export const getArticleById =(articleId)=>{
    return postRequest(`/common/article/getArticleById/articleId/${articleId}`)
}

export const getUserinfoById =(id)=>{
    return postRequest(`/common/user/getUserinfoById/${id}`)
}

// 根据文章ID获取文章附件列表
export const getArticleFileByArticleId = (articleId) => {
    return postRequest(`/common/getArticleFileByArticleId/${articleId}`)
}
