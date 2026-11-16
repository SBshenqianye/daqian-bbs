import VueRouter from 'vue-router';


//解决vue路由重复导航错误
//获取原型对象上的push函数
const originalPush = VueRouter.prototype.push
//修改原型对象中的push方法
VueRouter.prototype.push = function push(location) {
    return originalPush.call(this, location).catch(err => err)
}

//创建一个路由器
const router = new VueRouter({
    //mode: 'history',
    routes:[
        {
            name:'index',
            path:'/',
            redirect: '/forum'
        },

        // ===== BBS Forum Pages =====
        {
            name: 'BBSForum',
            path: '/forum',
            component: () => import('@/views/BBSForum.vue'),
            meta: { auth: false }
        },
        {
            name: 'BBSLogin',
            path: '/login',
            component: () => import('@/views/BBSLogin.vue'),
            meta: { auth: false }
        },
        {
            name: 'BBSPoints',
            path: '/points',
            component: () => import('@/views/BBSPoints.vue'),
            meta: { auth: false }
        },
        {
            name: 'BBSArticleWrite',
            path: '/write',
            component: () => import('@/views/BBSArticleWrite.vue'),
        },
        {
            name: 'BBSUserinfo',
            path: '/userinfo',
            component: () => import('@/views/BBSUserinfo.vue'),
        },
        {
            name: 'BBSPublicProfile',
            path: '/user/:userId',
            component: () => import('@/views/BBSPublicProfile.vue'),
            meta: { auth: false }
        },
        {
            name: 'BBSStat',
            path: '/stat',
            component: () => import('@/views/BBSStat.vue'),
        },
        {
            name: 'BBSArticleDetails',
            path: '/articleDetails/articleId/:articleId',
            component: () => import('@/views/BBSArticleDetails.vue'),
            meta: { auth: false }
        },
        {
            name: 'BBSChangePassword',
            path: '/change-password',
            component: () => import('@/views/BBSChangePassword.vue'),
            meta: { auth: true }
        },
        {
            name: 'BBSFeaturedArticles',
            path: '/featured',
            component: () => import('@/views/BBSFeaturedArticles.vue'),
            meta: { auth: false }
        },
        // 以下子页面已迁移为「个人中心」内嵌 Tab，保留旧地址做重定向，避免旧链接 404
        {
            path: '/my-replies',
            redirect: to => ({ path: '/userinfo', query: { tab: 'replies', ...to.query } })
        },
        {
            path: '/my-favorites',
            redirect: { path: '/userinfo', query: { tab: 'favorites' } }
        },
        {
            path: '/my-points-log',
            redirect: to => ({ path: '/userinfo', query: { tab: 'points', ...to.query } })
        },
        {
            path: '/my-violations',
            redirect: { path: '/userinfo', query: { tab: 'violations' } }
        },
        {
            path: '/my-appeals',
            redirect: to => ({ path: '/userinfo', query: { tab: 'appeals', ...to.query } })
        },
        {
            path: '/my-reports',
            redirect: { path: '/userinfo', query: { tab: 'reports' } }
        },
        {
            path: '/moderator-complaint',
            redirect: { path: '/userinfo', query: { tab: 'complaint' } }
        },
        {
            name: 'BBSNotifications',
            path: '/notifications',
            component: () => import('@/views/BBSNotifications.vue'),
            meta: { auth: true }
        },

    ]
})
export default router

