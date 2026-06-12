import Vue from 'vue';
import Router from 'vue-router';
import * as path from 'path';

Vue.use(Router);

export default new Router({
    base: process.env.NODE_ENV === "development" ? "" : "/bbs-admin",
    routes: [
        {
            path: '/',
            redirect: '/stitch-dashboard'
        },
        {
            path: '/',
            component: () => import('../components/common/StitchHome.vue'),
            meta: {
                title: '自述文件'
            },
            children: [
                {
                    path: '/stitch-dashboard',
                    component: () => import('../pages/StitchDashboard.vue'),
                    meta: { title: '系统首页' }
                },
                {
                    path: '/stitch-user',
                    component: () => import('../pages/StitchUser.vue'),
                    meta: { title: '用户管理' }
                },
                {
                    path: '/stitch-article',
                    component: () => import('../pages/StitchArticle.vue'),
                    meta: { title: '帖子管理' },
                },
                {
                    path: '/stitch-article-label',
                    component: () => import('../pages/StitchArticleLabel.vue'),
                    meta: { title: '标签管理' }
                },
                {
                    path: '/stitch-dict',
                    component: () => import('../pages/StitchDict.vue'),
                    meta: { title: '配置管理' }
                },
                {
                    path: '/stitch-article-details',
                    component: () => import('../pages/StitchArticleDetails.vue'),
                    meta: { title: '文章详情', noCache: false },
                },
                {
                    path: '/stitch-community',
                    component: () => import('../pages/StitchCommunity.vue'),
                    meta: { title: '社区管理' }
                },
                {
                    path: '/stitch-points',
                    component: () => import('../pages/StitchPoints.vue'),
                    meta: { title: '积分排名' }
                },
                {
                    path: '/stitch-points-detail',
                    component: () => import('../pages/StitchPointsDetail.vue'),
                    meta: { title: '积分排名详情' }
                },
                {
                    path: '/stitch-unit-manage',
                    component: () => import('../pages/StitchUnitManage.vue'),
                    meta: { title: '单位管理' }
                },
                {
                    path: '/stitch-sensitive-word',
                    component: () => import('../pages/StitchSensitiveWord.vue'),
                    meta: { title: '敏感词管理' }
                },
                {
                    path: '/stitch-404',
                    component: () => import('../pages/Stitch404.vue'),
                    meta: { title: '404' }
                },
                {
                    path: '/stitch-403',
                    component: () => import('../pages/Stitch403.vue'),
                    meta: { title: '403' }
                },
            ]
        },
        {
            path: '/stitch-login',
            component: () => import('../pages/StitchLogin.vue'),
            meta: { title: '管理员登录', auth: false }
        },
        {
            path: '*',
            redirect: '/stitch-404'
        }
    ]
});
