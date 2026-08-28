// 后端地址，可通过 .env 中 DEV_BACKEND_URL 覆盖，默认 127.0.0.1:9083
const DEV_BACKEND_URL = process.env.DEV_BACKEND_URL || 'http://127.0.0.1:9083'
// 管理前端 devserver 地址（跨应用相对跳转代理目标，如通知"待审批"→ /bbs-admin/#/approve-adopt）
const DEV_ADMIN_URL = process.env.DEV_ADMIN_URL || 'http://127.0.0.1:9082'
// URL 路由结构对齐生产 nginx：用户前端统一挂在 /bbs-user/ 下（dev 与 prod 一致，
// 使 /bbs-admin 等跨应用相对链接在所有环境行为相同）
const PUBLIC_PATH = '/bbs-user/'

module.exports = {
    publicPath: PUBLIC_PATH,
    pages: {
        index: {
            entry: 'src/main.js',
        }
    },
    lintOnSave: false, // 关闭语法检查

    // 开启代理服务器(端口9081)，使用 vue-cli 实现
    devServer: {
        port: 9081,
        // 禁用启发式缓存：dev 响应默认无 Cache-Control 头，浏览器会按 ETag 做启发式缓存，
        // 导致改代码后 F5 仍显示旧版（需 Ctrl+F5）。这里强制 no-cache。
        headers: {
            'Cache-Control': 'no-cache',
        },
        proxy: {
            // 管理前端代理：/bbs-admin/... 在本端口即可达，对齐生产同域行为
            '/bbs-admin': {
                target: DEV_ADMIN_URL,
                changeOrigin: true,
            },
            [process.env.VUE_APP_BBS_API]: {
                target: DEV_BACKEND_URL + '/bbs-server',
                changeOrigin: true,
                pathRewrite: {
                    ['^' + process.env.VUE_APP_BBS_API]: ''
                }
            },
        },
        // /bbs-server/ 开头的请求用 setupMiddlewares 手动转发
        setupMiddlewares(middlewares, devServer) {
            if (!devServer) throw new Error('webpack-dev-server is not defined');
            const { createProxyMiddleware } = require('http-proxy-middleware');
            // 老习惯地址 / 直接 302 到 /bbs-user/（302 保留 #hash，深链不丢）
            devServer.app.get('/', (req, res) => res.redirect(PUBLIC_PATH));
            devServer.app.use(
                '/bbs-server/',
                createProxyMiddleware({
                    target: DEV_BACKEND_URL,
                    changeOrigin: true,
                })
            );
            return middlewares;
        },
    }
}
