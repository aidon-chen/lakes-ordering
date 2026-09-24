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
    name: 'siam-shop',
    http:{
      //baseUrl: 'http://localhost:9411/siam-server', // 本地联调
      //cashierBaseUrl: 'http://127.0.0.1:5174' // 本地收银台

      //生产：云托管 bar-mvp 公网域名（2026-09-18 部署接入）
      baseUrl: 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com/siam-server', // production
      baseUrl4Test: 'https://bar-mvp-315579-12-1488393615.sh.run.tcloudbase.com/siam-server', // production_test
      developmentBaseUrl: 'http://localhost:9411/siam-server', // development（本地联调：本地后端 9411）
      cashierBaseUrl: '/cashier' // 收银台静态托管子目录（同域相对路径，如改独立域名则填完整 URL）
    },
    oss:{
      // 云端：阿里云 OSS 公网域名（线上图片真实存放处，生产/测试构建用）
      domain: 'https://lakes-public.oss-cn-chengdu.aliyuncs.com/',
      // 本地联调（npm run dev）：后端「本地存储端点」
      //   = 本地启动器 local-run/scripts/start-backend.sh 注入的 --aliyun.oss.localStorageDir
      //     目录，经 context-path /siam-server + /upload/ 暴露
      //   库里的图片字段是相对路径（data/images/...），两种模式同构、只换前缀；
      //   本地目录里没有的图（存量商品图）由后端自动回源 OSS，所以本地开发也能正常显示。
      developmentDomain: 'http://localhost:9411/siam-server/upload/'
    }
  }
}