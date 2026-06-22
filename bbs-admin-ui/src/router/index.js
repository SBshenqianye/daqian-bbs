import Vue from 'vue';
import Router from 'vue-router';
import * as path from 'path';

Vue.use(Router);

export default new Router({
    base: process.env.NODE_ENV === "development" ? "" : "/bbs-admin",
    routes: [
        {
            path: '/',
            redirect: '/dashboard'
        },
        {
            path: '/',
            component: () => import('../components/common/Home.vue'),
            meta: {
                title: '自述文件'
            },
            children: [
                {
                    path: '/dashboard',
                    component: () => import('../pages/Dashboard.vue'),
                    meta: { title: '系统首页' }
                },
                {
                    path: '/user',
                    component: () => import('../pages/UserPage.vue'),
                    meta: { title: '用户管理' }
                },
                {
                    path: '/article',
                    component: () => import('../pages/ArticlePage.vue'),
                    meta: { title: '帖子管理' },
                },
                {
                    path: '/articleLable',
                    component: () => import('../pages/ArticleLablePage.vue'),
                    meta: { title: '标签管理' }
                },
                {
                    path: '/dict',
                    component: () => import('../pages/DictPage.vue'),
                    meta: { title: '配置管理' }
                },
                {
                    path: '/article/id',
                    component: () => import('../pages/ArticleDetailsPage.vue'),
                    meta: { title: '文章详情', noCache: false },
                },
                {
                    path: '/community',
                    component: () => import('../pages/CommunityPage.vue'),
                    meta: { title: '社区管理' }
                },
                {
                    path: '/points',
                    component: () => import('../pages/BBSPoints.vue'),
                    meta: { title: '积分排名' }
                },
                {
                    path: '/points/detail',
                    component: () => import('../pages/BBSPointsDetail.vue'),
                    meta: { title: '积分排名详情' }
                },
                {
                    path: '/unitManage',
                    component: () => import('../pages/BBSUnitManage.vue'),
                    meta: { title: '单位管理' }
                },
                {
                    path: '/sensitiveWord',
                    component: () => import('../pages/BBSSensitiveWord.vue'),
                    meta: { title: '敏感词管理' }
                },
                {
                    path: '/404',
                    component: () => import('../pages/404.vue'),
                    meta: { title: '404' }
                },
                {
                    path: '/403',
                    component: () => import('../pages/403.vue'),
                    meta: { title: '403' }
                },
            ]
        },
        {
            path: '/login',
            component: () => import('../pages/Login.vue'),
            meta: { title: '管理员登录', auth: false }
        },
        {
            path: '*',
            redirect: '/404'
        }
    ]
});
