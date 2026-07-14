/*
module.exports = {
    baseUrl: './',
    assetsDir: 'static',
    productionSourceMap: false,
     devServer: {
         proxy: {
             '/api':{
                 target:'http://jsonplaceholder.typicode.com',
                 changeOrigin:true,
                 pathRewrite:{
                     '/api':''
                 }
             }
         }
     }
}
*/

// 后端地址，可通过 .env 中 DEV_BACKEND_URL 覆盖，默认 127.0.0.1:9083
const DEV_BACKEND_URL = process.env.DEV_BACKEND_URL || 'http://127.0.0.1:9083'

module.exports = {
    publicPath: process.env.NODE_ENV === "development" ? "/" : "/bbs-admin/",
    pages: {
        index: {
            entry: 'src/main.js',
        }
    },
    lintOnSave: false, //关闭语法检查

    //开启代理服务器(通过9082转发给9083)，使用vue-cli 实现
    devServer: {
        port: 9082,
        proxy: {
            [process.env.VUE_APP_BBS_API]: {
                target: process.env.VUE_APP_BBS_BASE_API + '/bbs-server',
                changeOrigin: true,
                pathRewrite: {
                    ['^' + process.env.VUE_APP_BBS_API]: ''
                }
            },

        },
        // /bbs-server/ 开头的文件请求直接转发到后端（用于头像、附件等静态资源）
        before(app) {
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
