package com.siam.package_common.config;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 会在启动期建立连接；云托管环境无 Redis，必须按需装配。
 * 仅当 siam.redis.enabled=true 时创建（见 04-deploy-cost/mvp-migration/02）。
 */
@Configuration
@ConditionalOnProperty(name = "siam.redis.enabled", havingValue = "true")
public class RedissonConfig {

//    @Value("${spring.redis.sentinel.nodes}")
//    private String nodes;
//
//    @Value("${spring.redis.sentinel.master}")
//    private String masterName;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    @Value("${spring.redis.password}")
    private String password;

    @Bean()
    public RedissonClient redissonClient() {
        //1、创建配置
        String address = "redis://" + host + ":" + port;
        Config config = new Config();
        config.useSingleServer().setAddress(address);
        // 1.1 如果password不为空，则set密码
        if (StringUtils.isNotEmpty(password)) {
            config.useSingleServer().setPassword(password);
        }

        //2、根据配置创建出RedissonClient实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}