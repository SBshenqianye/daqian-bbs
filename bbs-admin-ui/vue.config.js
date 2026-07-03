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
            // 文件直接走 /files/ 代理（后端 normalizeFieldUrl/normalizeEmbeddedUrls 处理后的路径）
            '/files/': {
                target: process.env.VUE_APP_BBS_BASE_API + '/bbs-server',
                changeOrigin: true,
            },
        },
        // /bbs-server/ 开头的请求手动转发（后端 normalize 后返回的路径已含 context-path）
        before(app, server) {
            const proxy = require('http-proxy-middleware');
            app.use(
                '/bbs-server/',
                proxy({
                    target: process.env.VUE_APP_BBS_BASE_API,
                    changeOrigin: true,
                })
            );
        },
    }
}
