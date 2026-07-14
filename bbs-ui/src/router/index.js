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

    ]
})
export default router

