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
            redirect: '/stitch-forum'
        },

        // ===== Stitch Design System Pages (lazy-loaded) =====
        {
            name: 'stitchForum',
            path: '/stitch-forum',
            component: () => import('@/views/StitchForum.vue'),
            meta: { auth: false }
        },
        {
            name: 'stitchLogin',
            path: '/stitch-login',
            component: () => import('@/views/StitchLogin.vue'),
            meta: { auth: false }
        },
        {
            name: 'stitchPoints',
            path: '/stitch-points',
            component: () => import('@/views/StitchPoints.vue'),
            meta: { auth: false }
        },
        {
            name: 'stitchWrite',
            path: '/stitch-write',
            component: () => import('@/views/StitchWrite.vue'),
        },
        {
            name: 'stitchUserinfo',
            path: '/stitch-userinfo',
            component: () => import('@/views/StitchUserinfo.vue'),
        },
        {
            name: 'stitchStat',
            path: '/stitch-stat',
            component: () => import('@/views/StitchStat.vue'),
        },
        {
            name: 'stitchArticleDetails',
            path: '/stitch-article-details/articleId/:articleId',
            component: () => import('@/views/StitchArticleDetails.vue'),
            meta: { auth: false }
        },

    ]
})
export default router

