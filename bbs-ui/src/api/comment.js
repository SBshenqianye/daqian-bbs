import { postRequest } from './api'

export const getCommentReply =(articleId)=>{
    return postRequest(`/common/comment/getCommentReply/${articleId}`)
}
