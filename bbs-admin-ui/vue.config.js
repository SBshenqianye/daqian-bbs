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

    //开启代理服务器(通过8082转发给8083)，使用vue-cli 实现
    devServer: {
        port: 8082,
        proxy: {
            [process.env.VUE_APP_BBS_API]: {
                target: process.env.VUE_APP_BBS_BASE_API + '/bbs-server',
                changeOrigin: true,
                pathRewrite: {
                    ['^' + process.env.VUE_APP_BBS_API]: ''
                }
            },

        },
    }
}
