
// 引入 axios
import axios from "axios";
//引入 element-ui 信息
import {Message} from "element-ui";
import router from "@/router";
import {getToken, removeToken, removeUser} from "@/utils/auth";


//请求拦截器
axios.interceptors.request.use(config=>{
    var token = getToken()
    if(token){
        config.headers['Authorization'] = token;
    }
    return config;
},error=>{
    console.log(error);
    return Promise.reject(error)
})

//响应拦截器
axios.interceptors.response.use(success=>{
    //业务逻辑错误
    console.log("success",success)
    if(success.status && success.status === 200){
        if(success.data.code === 500 || success.data.code === 403){
            Message({
                type: 'warning',
                message: success.data.message,
                offset:54
            })
            return;
        }
        if(success.data.code === 401){
            removeToken()
            removeUser()
            const currentPath = router.currentRoute.fullPath
            router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login');
            return;
        }
        if(success.data.message){
            const url = (success.config && success.config.url) || ''
            const noSuccessTip = url.indexOf('listDictByType') !== -1 || url.indexOf('saOrgTree') !== -1 || url.indexOf('login') !== -1
            if (!noSuccessTip) {
                Message({
                    type: 'success',
                    message: success.data.message,
                    offset:54
                })
            }
        }

    }
    return success.data;

},error => {
    const status = error.response && error.response.status;
    if(status === 504 || status === 404){
        Message({
            type: 'error',
            message: '服务器错误',
            offset:54
        })

    }else if(status === 403){
        Message({
            type: 'error',
            message: '权限不足，请联系管理员！',
            offset:54
        })

    }else if(status === 401){
        const currentPath = router.currentRoute.fullPath
        router.replace(currentPath !== '/login' ? `/login?redirect=${encodeURIComponent(currentPath)}` : '/login');
    }else{
        if(error.response.data.message){
            Message({
                type: 'error',
                message: error.response.data.message,
                offset:54
            })
        }else{
            Message({
                type: 'error',
                message: '未知错误！',
                offset:54
            })
        }
    }

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
        url:`${process.env.VUE_APP_BBS_API}${url}`,
        data:params
    })
}

//传送json格式的delete请求
export const deleteRequest=(url,params)=>{
    return axios({
        method:'delete',
        url:`${process.env.VUE_APP_BBS_API}${url}`,
        data:params
    })
}
