// 引入 axios
import axios from "axios";
//引入 element-ui 信息
import {Message} from "element-ui";
import router from "@/router";


//请求拦截器
axios.interceptors.request.use(config=>{
    if(window.sessionStorage.getItem('tokenStr')){
        config.headers['Authorization'] = window.sessionStorage.getItem('tokenStr');
    }
    return config;
},error=>{
    console.log(error);
})

// 构建错误信息，开发环境下附带后端详情方便 debug
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

//响应拦截器
axios.interceptors.response.use(success=>{
    //业务逻辑错误
    if(success.status && success.status == 200){
        if( success.data.code == 401 || success.data.code == 403){
            return;
        }
    }
    return success.data;

},error => {
    // 优先显示后端返回的错误信息，方便 debug；没有则兜底通用文案
    const resData = error.response && error.response.data
    const status = error.response && error.response.status
    const resMsg = buildErrorMessage(resData)
    // 始终将完整错误打印到控制台供排查
    console.error('[API Error]', status, resData || error.message)
    if(status == 504 || status == 404){
        Message({
            type: 'error',
            message: resMsg || '服务器错误',
            showClose: true,
        })
    }else if(status == 403){
        Message({
            type: 'error',
            message: resMsg || '权限不足，请联系管理员！',
            showClose: true,
        })
    }else if(status == 401){
        Message({
            type: 'error',
            message: resMsg || '尚未登录，请登录！',
            showClose: true,
        })
        const currentPath = router.currentRoute.fullPath
        router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login');
    }else{
        Message({
            type: 'error',
            message: resMsg || '未知错误！',
            showClose: true,
        })
    }
    return;
})


//传送json格式的post请求
export const postRequest =(url,params)=>{
    return axios({
        method:'post',
        url: `${process.env.VUE_APP_BBS_API}${url}`,
        data: params
    })
}

//传送json格式的put请求
export const putRequest=(url,params)=>{
    return axios({
        method:'put',
        url:`${process.env.VUE_APP_BBS_API}${url}`,
        data:params
    })
}

//传送json格式的get请求
export const getRequest=(url,params)=>{
    return axios({
        method:'get',
        url:`${process.env.VUE_APP_BBS_API}${url}/${params}`,
    })
}

// 无路径参数的 get 请求（如获取组织树）
export const getRequestUrl = (url) => {
    return axios({
        method: 'get',
        url: `${process.env.VUE_APP_BBS_API}${url}`
    })
}

//传送json格式的delete请求
export const deleteRequest=(url,params)=>{
    return axios({
        method:'delete',
        url:`${process.env.VUE_APP_BBS_API}${url}/${params}`,
        //data:params
    })
}

// 上传文件（multipart/form-data）
export const uploadFile = (url, formData) => {
    return axios({
        method: 'post',
        url: `${process.env.VUE_APP_BBS_API}${url}`,
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}
