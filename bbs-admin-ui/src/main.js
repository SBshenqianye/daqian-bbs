import Vue from 'vue';
import App from './App.vue';
import router from './router';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css'; // 默认主题
// import './assets/css/theme-green/index.css'; // 浅绿色主题
import './assets/css/icon.css';
import 'babel-polyfill';
import './assets/tailwind.css';
import {postRequest,putRequest,getRequest,getRequestUrl,deleteRequest,uploadFile,downloadFile} from "@/api/api";
import {installErrorHandler} from "./utils/errorHandler";
import adminStore from "./utils/adminStore";

//插件形式使用请求
Vue.prototype.postRequest = postRequest;
Vue.prototype.putRequest = putRequest;
Vue.prototype.getRequest = getRequest;
Vue.prototype.getRequestUrl = getRequestUrl;
Vue.prototype.deleteRequest = deleteRequest;
Vue.prototype.uploadFile = uploadFile;
Vue.prototype.downloadFile = downloadFile;


Vue.use(ElementUI, {
    size: 'small'
});

// 初始化管理员状态 Store
adminStore.init();

// 管理端账号角色：userType 2=管理员，3=超级管理员，4=版主
function hasAdminRole() {
    const role = adminStore.getRole();
    return role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'MODERATOR';
}

//使用钩子函数对路由进行权限跳转
router.beforeEach((to, from, next) => {
    document.title = `大千智荟创新创意交流论坛`;
    const isLoggedIn = adminStore.isLoggedIn();
    const publicPaths = ['/login'];
    if (!isLoggedIn && !publicPaths.includes(to.path)) {
        next('/login');
    } else if (to.meta.permission) {
        // 角色不符：清理无权限登录态；角色正确但缺权限点：跳 403
        if (!hasAdminRole()) {
            adminStore.logout();
            next('/login');
        } else if (!adminStore.hasPermission(to.meta.permission)) {
            next('/403');
        } else {
            next();
        }
    } else {
        next();
    }
});

// 全局错误处理 + 自动恢复（必须在 new Vue() 之前安装）
installErrorHandler(router);

new Vue({
    router,
    render: h => h(App)
}).$mount('#app');
