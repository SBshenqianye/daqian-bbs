import Vue from 'vue';
import App from './App.vue';
import router from './router';
import ElementUI from 'element-ui';
import { MessageBox } from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css'; // 默认主题
// import './assets/css/theme-green/index.css'; // 浅绿色主题
import './assets/css/icon.css';
import 'babel-polyfill';
import './assets/tailwind.css';
import './assets/css/overlay.css';
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

// 管理端访问资格：由权限组授予 admin:access 决定
function hasAdminRole() {
    return adminStore.hasPermission('admin:access');
}

// 启动时刷新一次会话权限，避免 sessionStorage 里过期的权限点把所有页面误判成 403
let sessionSyncPromise = null
let syncedAdminId = null
let lastSessionSyncAt = 0
function syncAdminSession() {
    const current = adminStore.state.admin
    if (current && current.id && syncedAdminId === current.id && Date.now() - lastSessionSyncAt < 30000) {
        return Promise.resolve()
    }
    if (sessionSyncPromise) return sessionSyncPromise
    sessionSyncPromise = getRequestUrl('/common/user/info')
        .then(resp => {
            if (resp && resp.id) {
                adminStore.updateAdmin(resp)
                syncedAdminId = resp.id
                lastSessionSyncAt = Date.now()
            }
        })
        .catch(() => {})
        .then(() => {
            sessionSyncPromise = null
        })
    return sessionSyncPromise
}

// 路由权限不足：弹窗提示并留在当前页，避免整页跳到 403
let routeDeniedVisible = false
function showRouteDenied(title) {
    if (routeDeniedVisible) return
    routeDeniedVisible = true
    const done = () => { routeDeniedVisible = false }
    MessageBox.alert(`当前账号没有访问「${title || '该页面'}」的权限。`, '没有权限', {
        type: 'warning',
        confirmButtonText: '知道了',
        callback: done
    }).catch(done)
}

//使用钩子函数对路由进行权限跳转
router.beforeEach(async (to, from, next) => {
    document.title = `大千智荟创新创意交流论坛`;
    if (adminStore.isLoggedIn() && to.path !== '/login') {
        await syncAdminSession();
    }
    const isLoggedIn = adminStore.isLoggedIn();
    const publicPaths = ['/login'];
    if (isLoggedIn && adminStore.needsPasswordChange() && to.path !== '/change-password') {
        // 首次登录（或密码被重置）必须先改密，禁止绕过前端进入其他页面
        next('/change-password');
    } else if (!isLoggedIn && !publicPaths.includes(to.path)) {
        next('/login');
    } else if (to.meta.permission) {
        // 角色不符：清理无权限登录态；角色正确但缺权限点：弹窗提示
        if (!hasAdminRole()) {
            adminStore.logout();
            next('/login');
        } else if (!adminStore.hasPermission(to.meta.permission)) {
            showRouteDenied(to.meta.title);
            if (from && from.matched && from.matched.length > 0) {
                next(false);
            } else if (to.path !== '/dashboard') {
                next('/dashboard');
            } else {
                next(false);
            }
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
