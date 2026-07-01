/*const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
    transpileDependencies: true,
    lintOnSave:false
})*/

module.exports = {
    publicPath: process.env.NODE_ENV === "development" ? "/" : "/bbs-user/",
    pages: {
        index: {
            entry: 'src/main.js',
        }
    },
    lintOnSave: false, //关闭语法检查

    //开启代理服务器(通过9081转发给9083)，使用vue-cli 实现
    devServer: {
        port: 9081,
        proxy: {
            [process.env.VUE_APP_BBS_API]: {
                target: 'http://127.0.0.1:9083/bbs-server',
                changeOrigin: true,
                pathRewrite: {
                    ['^' + process.env.VUE_APP_BBS_API]: ''
                }
            },
            // 文件统一走 /files/ proxy
            '/files/': {
                target: 'http://127.0.0.1:9083/bbs-server',
                changeOrigin: true,
            },
        },
    }
}