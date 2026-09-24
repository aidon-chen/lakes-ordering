// see http://vuejs-templates.github.io/webpack for documentation.
var path = require('path')

module.exports = {
  build: {
    env: require('./prod.env'),
    index: path.resolve(__dirname, '../dist/index.html'),
    assetsRoot: path.resolve(__dirname, '../dist'),
    assetsSubDirectory: 'static',
    assetsPublicPath: './',
    productionSourceMap: false,
    // Gzip off by default as many popular static hosts such as
    // Surge or Netlify already gzip all static assets for you.
    // Before setting to `true`, make sure to:
    // npm install --save-dev compression-webpack-plugin
    productionGzip: false,
    productionGzipExtensions: ['js', 'css'],
    // Run the build command with an extra argument to
    // View the bundle analyzer report after build finishes:
    // `npm run build --report`
    // Set to `true` or `false` to always turn it on or off
    bundleAnalyzerReport: process.env.npm_config_report
  },
  appConfig: {
    rootPath: '',
    name: 'siam-admin',
    http:{
      // baseUrl: 'http://localhost:9411/siam-server', // 本地联调

      //生产：云托管 bar-mvp 公网域名（2026-09-18 部署接入）
      baseUrl: 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com/siam-server', // production
      baseUrl4Test: 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com/siam-server', // production_test
      developmentBaseUrl: 'http://localhost:9411/siam-server' // development（本地联调：本地后端 9411）
    },
    oss:{
      domain: 'https://siam-hangzhou.oss-cn-hangzhou.aliyuncs.com/'
    }
  }
}