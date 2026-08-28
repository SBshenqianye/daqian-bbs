// 后端地址，可通过 .env 中 DEV_BACKEND_URL 覆盖，默认 127.0.0.1:9083
const DEV_BACKEND_URL = process.env.DEV_BACKEND_URL || 'http://127.0.0.1:9083'
// 用户前端 devserver 地址（跨应用相对跳转代理目标，如"前往论坛"→ /bbs-user/）
const DEV_USER_URL = process.env.DEV_USER_URL || 'http://127.0.0.1:9081'
// URL 路由结构对齐生产 nginx：管理前端统一挂在 /bbs-admin/ 下（dev 与 prod 一致）
const PUBLIC_PATH = '/bbs-admin/'

module.exports = {
    publicPath: PUBLIC_PATH,
    pages: {
        index: {
            entry: 'src/main.js',
        }
    },
    lintOnSave: false, //关闭语法检查

    //开启代理服务器(端口9082)，使用vue-cli 实现
    devServer: {
        port: 9082,
        proxy: {
            // 用户前端代理：/bbs-user/... 在本端口即可达，对齐生产同域行为
            '/bbs-user': {
                target: DEV_USER_URL,
                changeOrigin: true,
            },
            [process.env.VUE_APP_BBS_API]: {
                target: process.env.VUE_APP_BBS_BASE_API + '/bbs-server',
                changeOrigin: true,
                pathRewrite: {
                    ['^' + process.env.VUE_APP_BBS_API]: ''
                }
            },

        },
        // /bbs-server/ 开头的请求转发到后端（用于头像、附件等静态资源）
        // 注意：本项目 admin-ui 的 webpack-dev-server 为 v3，自定义逻辑需用 before(app)，
        // 不支持 v4 的 setupMiddlewares（会报 options additional properties 校验错误）
        before(app) {
            // 老习惯地址 / 直接 302 到 /bbs-admin/（302 保留 #hash，深链不丢）
            app.get('/', (req, res) => res.redirect(PUBLIC_PATH));
            const proxy = require('http-proxy-middleware');
            app.use(
                '/bbs-server/',
                proxy({
                    target: DEV_BACKEND_URL,
                    changeOrigin: true,
                })
            );
        },
    }
}
