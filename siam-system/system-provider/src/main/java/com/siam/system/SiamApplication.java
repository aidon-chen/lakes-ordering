package com.siam.system;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 调度开关已迁移至 config/SchedulingConfig.java（@Profile("!mvp") 控制启停，
 * mvp/云托管环境不启动 @Scheduled；见 04-deploy-cost/mvp-migration/06）
 */
@SpringBootApplication(scanBasePackages = "com.siam", exclude = {RocketMQAutoConfiguration.class})
@MapperScan(basePackages = {"com.siam.**.mapper"})
public class SiamApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SiamApplication.class, args);
    }
}