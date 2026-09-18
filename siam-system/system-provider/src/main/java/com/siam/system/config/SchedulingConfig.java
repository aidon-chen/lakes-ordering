package com.siam.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度总开关：@EnableScheduling 从启动类迁移至此。
 * mvp profile（云托管最小副本 0 / 演示场景）不启动任何 @Scheduled 任务——
 * 避免额度消耗，且规避「常驻任务被自动缩容误判」；涉钱 4 条任务正式版恢复。
 * 见 04-deploy-cost/mvp-migration/06
 */
@Configuration
@EnableScheduling
@Profile("!mvp")
public class SchedulingConfig {
}
