package com.siam.system.config;

import com.siam.package_common.converter.DateConverter;
import com.siam.system.interceptor.AdminInterceptor;
import com.siam.system.interceptor.MemberInterceptor;
import com.siam.system.interceptor.MerchantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * 拦截器配置类
 **/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private MemberInterceptor memberInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Autowired
    private MerchantInterceptor merchantInterceptor;

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    /** 本地联调图片存储目录（仅 application-local.yml 配置）。留空则整个 /upload/** 都不注册 ⇒ 云端行为零变化 */
    @Value("${aliyun.oss.localStorageDir:}")
    private String localStorageDir;

    /** 回源用：本地目录里没有的图（如存量商品图）转发到 OSS */
    @Value("${aliyun.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${aliyun.oss.bucketName:}")
    private String ossBucketName;

    /**
     * 重写添加拦截器方法并添加配置拦截器
     *
     * @param registry
     **/
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册管理员拦截器
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/rest/admin/**")
                .excludePathPatterns("/rest/admin/login")
                .excludePathPatterns("/rest/admin/loginByMobile")
                .excludePathPatterns("/rest/admin/forgetPassword/step1")
                .excludePathPatterns("/rest/admin/forgetPassword/step2");

        //注册用户拦截器
        registry.addInterceptor(memberInterceptor)
                .addPathPatterns("/rest/member/**")
                .excludePathPatterns("/rest/member/login")
                .excludePathPatterns("/rest/member/loginByMobile")
                /*.excludePathPatterns("/rest/member/logout")*/
                .excludePathPatterns("/rest/member/register")
                .excludePathPatterns("/rest/member/verification/login")
                .excludePathPatterns("/rest/member/weChat/login")
                .excludePathPatterns("/rest/member/wxPay/notify")
                .excludePathPatterns("/rest/member/wxPay/refundSuccessNotify");

        //商户拦截器
        registry.addInterceptor(merchantInterceptor)
//                .addPathPatterns("/api-member/rest/member/**")
                .addPathPatterns("/*/rest/merchant/**")
                .excludePathPatterns("/rest/merchant/login")
                .excludePathPatterns("/rest/merchant/loginByMobile")
                .excludePathPatterns("/rest/merchant/logout")
                .excludePathPatterns("/rest/merchant/register")
                .excludePathPatterns("/rest/merchant/registerByMobile")
                .excludePathPatterns("/rest/merchant/verification/login")
                .excludePathPatterns("/rest/merchant/forgetPassword/step1");
    }

    /**
     * 改用过滤器CorsFilter 来配置跨域，由于Filter的位置是在Interceptor之前的，问题得到解决
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 设置允许跨域请求的域名
        config.addAllowedOrigin("*");
        // 是否允许证书(这个不开启，请求会报错的)
        config.setAllowCredentials(true);
        // 设置允许的方法
        config.addAllowedMethod("*");
        // 允许任何头
        config.addAllowedHeader("*");
        config.addExposedHeader("token");
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        return new CorsFilter(configSource);
    }

    /**
     * 该方法已废弃
     * @Deprecated
     *
     * 浏览器跨域处理（只会在spring初始化的时候加载一次）
     * 注意：这种跨域问题的处理方式 对于拦截器不放行的请求无效
     *
     * @param registry
     **/
    /*@Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders(HttpHeaders.SET_COOKIE).maxAge(3600L);
    }*/

    /**
     * 配置全局日期转换器
     */
    @PostConstruct
    public void initEditableAvlidation() {
        ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) handlerAdapter.getWebBindingInitializer();
        if(initializer.getConversionService()!=null) {
            GenericConversionService genericConversionService = (GenericConversionService)initializer.getConversionService();
            genericConversionService.addConverter(new DateConverter());//添加自定义的类型转换器
        }
    }

    /**
     * 本地联调图片访问：GET /upload/**
     *
     * 只有在 aliyun.oss.localStorageDir 配置了非空值时才注册（云端不注册，行为与改动前完全一致）。
     * 解析顺序：
     *   ① 本地存储目录里有该文件 → 直接返回（本地新上传的图）
     *   ② 本地没有 → 回源 OSS（存量商品图只存在于 OSS，且 bucket 对象级公开读，无需签名）
     **/
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = localStorageDir == null ? "" : localStorageDir.trim();
        if (dir.isEmpty()) {
            return;
        }
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        final String ossPrefix = buildOssPrefix();

        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + dir)
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource local = location.createRelative(resourcePath);
                        if (local.exists() && local.isReadable()) {
                            return local;
                        }
                        if (ossPrefix == null) {
                            return null;
                        }
                        UrlResource remote = new UrlResource(ossPrefix + resourcePath);
                        return remote.exists() ? remote : null;
                    }
                });
    }

    /**
     * 拼出 OSS 公开访问前缀：https://{bucket}.{endpoint 主机}/
     **/
    private String buildOssPrefix() {
        String endpoint = ossEndpoint == null ? "" : ossEndpoint.trim();
        String bucket = ossBucketName == null ? "" : ossBucketName.trim();
        if (endpoint.isEmpty() || bucket.isEmpty()) {
            return null;
        }
        String host = endpoint.replaceFirst("^https?://", "");
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return "https://" + bucket + "." + host + "/";
    }
}