/*const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
    transpileDependencies: true,
    lintOnSave:false
})*/

// 后端地址，可通过 .env 中 DEV_BACKEND_URL 覆盖，默认 127.0.0.1:9083
const DEV_BACKEND_URL = process.env.DEV_BACKEND_URL || 'http://127.0.0.1:9083'

module.exports = {
    publicPath: process.env.NODE_ENV === "development" ? "/" : "/bbs-user/",
    pages: {
        index: {
            entry: 'src/main.js',
        }
    },
    lintOnSave: false, // 关闭语法检查

    // 开启代理服务器(通过9081转发给后端)，使用 vue-cli 实现
    devServer: {
        port: 9081,
        proxy: {
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
