import Vue from 'vue'
import App from './App.vue'
//引入 router
import VueRouter from 'vue-router';
import router from "@/router";
//import store from "@/store";

// 引入 Tailwind CSS 全局样式
import './assets/tailwind.css'

//按需引入element-ui
import {
    MessageBox,
    Tooltip,
} from 'element-ui';

// 引入公共组件
import BBSUserBadge from '@/components/BBSUserBadge'
Vue.component('bbs-user-badge', BBSUserBadge)

//引入 封装好的请求
import {postRequest} from "@/api/api";
import {putRequest} from "@/api/api";
import {getRequest} from "@/api/api";
import {deleteRequest} from "@/api/api";
//引入 身份认证工具
import {getToken, getUser} from "@/utils/auth";



//关闭生产提示
Vue.config.productionTip = false

Vue.use(VueRouter)

Vue.component(Tooltip.name,Tooltip)


Vue.prototype.$confirm = MessageBox.confirm;

//插件形式使用请求
Vue.prototype.postRequest = postRequest;
Vue.prototype.putRequest = putRequest;
Vue.prototype.getRequest = getRequest;
Vue.prototype.deleteRequest = deleteRequest;

// 需要登录才能访问的路径（未登录时直接跳转登录页）
const authPaths = ['/write', '/userinfo', '/stat'];
const needAuth = (path) => authPaths.some(p => path === p || path.startsWith(p + '/'));

router.beforeEach(((to, from, next) => {
    var hasToken = getToken();
    if (needAuth(to.path) && !hasToken) {
        next({ path: '/login', query: { redirect: to.fullPath } });
        return;
    }

    // 首次登录强制改密：除改密页和登录页外，isFirstLogin==1 时一律拦截
    if (hasToken && to.path !== '/change-password' && to.path !== '/login') {
        try {
            var user = getUser();
            if (user && user.isFirstLogin === 1) {
                next({ path: '/change-password', query: { redirect: to.fullPath } });
                return;
            }
        } catch(e) {}
    }

    next();
}))


new Vue({
    el:'#app',
    render: h => h(App),
    router:router,

    beforeCreate() {
        //安装全局事件总线
        Vue.prototype.$bus = this
    },

})

