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

//响应拦截器
axios.interceptors.response.use(success=>{
    //业务逻辑错误
    if(success.status && success.status == 200){
        if( success.data.code == 401 || success.data.code == 403){
            /*Message({
                type: 'error',
                message: success.data.message,
                //offset:50
            })*/
            return;
        }
        if(success.data.message){
            /*Message({
                type: 'success',
                message: success.data.message,
                //offset:50
            })*/
        }
    }
    return success.data;

},error => {
    // 优先显示后端返回的错误信息，方便 debug；没有则兜底通用文案
    const resMsg = error.response && error.response.data && error.response.data.message
    const status = error.response && error.response.status
    if(status == 504 || status == 404){
        Message({
            type: 'error',
            message: resMsg || '服务器错误',
        })
    }else if(status == 403){
        Message({
            type: 'error',
            message: resMsg || '权限不足，请联系管理员！',
        })
    }else if(status == 401){
        Message({
            type: 'error',
            message: resMsg || '尚未登录，请登录！',
        })
        router.replace('/');
    }else{
        Message({
            type: 'error',
            message: resMsg || '未知错误！',
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
