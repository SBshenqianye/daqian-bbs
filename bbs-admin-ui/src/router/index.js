import Vue from 'vue';
import Router from 'vue-router';

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
                    meta: { title: '系统首页', permission: 'dashboard:read' }
                },
                {
                    path: '/user',
                    component: () => import('../pages/UserPage.vue'),
                    meta: { title: '用户管理', permission: 'user:read' }
                },
                {
                    path: '/article',
                    component: () => import('../pages/ArticlePage.vue'),
                    meta: { title: '帖子管理', permission: 'article:read' },
                },
                {
                    path: '/articleLable',
                    component: () => import('../pages/ArticleLablePage.vue'),
                    meta: { title: '标签管理', permission: 'config:read' }
                },
                {
                    path: '/dict',
                    component: () => import('../pages/ConfigCenterPage.vue'),
                    meta: { title: '配置管理', permission: 'config:read' }
                },
                {
                    path: '/systemConfig',
                    redirect: '/dict'
                },
                {
                    path: '/article/id',
                    component: () => import('../pages/ArticleDetailsPage.vue'),
                    meta: { title: '文章详情', noCache: false, permission: 'article:read' },
                },
                {
                    path: '/community',
                    component: () => import('../pages/CommunityPage.vue'),
                    meta: { title: '社区管理', permission: 'article:read' }
                },
                {
                    path: '/points',
                    component: () => import('../pages/BBSPoints.vue'),
                    meta: { title: '积分排名', permission: 'points:read' }
                },
                {
                    path: '/points/detail',
                    component: () => import('../pages/BBSPointsDetail.vue'),
                    meta: { title: '积分排名详情', permission: 'points:read' }
                },
                {
                    path: '/points/config',
                    redirect: '/unitManage'
                },
                {
                    path: '/unitManage',
                    component: () => import('../pages/BBSUnitManage.vue'),
                    meta: { title: '单位管理', permission: 'config:read' }
                },
                {
                    path: '/sensitiveWord',
                    component: () => import('../pages/BBSSensitiveWord.vue'),
                    meta: { title: '敏感词管理', permission: 'config:read' }
                },
                {
                    path: '/violation',
                    component: () => import('../pages/ViolationPage.vue'),
                    meta: { title: '违规管理', permission: 'article:review' }
                },
                {
                    path: '/report',
                    component: () => import('../pages/ReportPage.vue'),
                    meta: { title: '举报管理', permission: 'report:review' }
                },
                {
                    path: '/appeal',
                    component: () => import('../pages/AppealPage.vue'),
                    meta: { title: '申诉管理', permission: 'appeal:review' }
                },
                {
                    path: '/moderator',
                    component: () => import('../pages/ModeratorPage.vue'),
                    meta: { title: '版主管理', permission: 'moderator:manage' }
                },
                {
                    path: '/moderator-complaint',
                    component: () => import('../pages/ModeratorComplaintPage.vue'),
                    meta: { title: '版主投诉管理', permission: 'moderator:complaint:review' }
                },
                {
                    path: '/approve-adopt',
                    component: () => import('../pages/ApproveAdoptPage.vue'),
                    meta: { title: '采纳审批', permission: 'adopt:review' }
                },
                {
                    path: '/featured-recommendation',
                    component: () => import('../pages/FeaturedRecommendationPage.vue'),
                    meta: { title: '精华帖审批', permission: 'featured:review' }
                },
                {
                    path: '/permission-matrix',
                    component: () => import('../pages/PermissionMatrixPage.vue'),
                    meta: { title: '权限矩阵', permission: 'permission:manage' }
                },
                {
                    path: '/permission-groups',
                    component: () => import('../pages/PermissionGroupPage.vue'),
                    meta: { title: '权限管理', permission: 'permission:manage' }
                },
                {
                    path: '/audit',
                    component: () => import('../pages/AuditLogPage.vue'),
                    meta: { title: '审计日志', permission: 'audit:read' }
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
            path: '/change-password',
            component: () => import('../pages/ChangePasswordPage.vue'),
            meta: { title: '修改初始密码', auth: false }
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
