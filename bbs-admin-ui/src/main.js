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
import * as echarts from 'echarts'
import {installErrorHandler} from "./utils/errorHandler";
import adminStore from "./utils/adminStore";


Vue.prototype.$echarts = echarts

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

//使用钩子函数对路由进行权限跳转
router.beforeEach((to, from, next) => {
    document.title = `大千智荟创新创意交流论坛`;
    const isLoggedIn = adminStore.isLoggedIn();
    const publicPaths = ['/login'];
    if (!isLoggedIn && !publicPaths.includes(to.path)) {
        next('/login');
    } else if (to.meta.permission) {
        // 如果是管理员权限则可进入
        const admin = adminStore.state.admin;
        const role = admin ? admin.username : null;
        role === 'admin' ? next() : next('/403');
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
