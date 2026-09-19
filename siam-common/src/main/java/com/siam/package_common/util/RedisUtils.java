package com.siam.package_common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存工具：Redis 可用时走 Redis；不可用（云托管无 Redis，siam.redis.enabled!=true）时
 * 自动退化为进程内缓存（单实例语义，重启即失——对开关/菜单缓存等场景语义可接受）。
 * 见 04-deploy-cost/mvp-migration/02
 */
@Component
public class RedisUtils {

    /**mvp/无Redis环境时为 null，各方法自动走进程内兜底*/
    @Autowired(required = false)
    private RedisTemplate redisTemplate;

    /**进程内兜底存储：key -> value*/
    private final Map<String, Object> localStore = new ConcurrentHashMap<String, Object>();

    /**进程内兜底过期时间戳：key -> expireAtMillis*/
    private final Map<String, Long> localExpireAt = new ConcurrentHashMap<String, Long>();

    /**Redis 可用性探测结果：null=尚未探测，true=可用，false=不可用（走进程内兜底）*/
    private volatile Boolean redisAvailableCache = null;

    /**
     * ⚠️ 不能只判 redisTemplate != null。
     * mvp profile（云托管）没有 Redis，但也没有配置 spring.redis.host，Spring Boot 仍会
     * 自动装配 RedisTemplate（默认 localhost:6379），于是这里恒为 true —— 进程内兜底永远
     * 不生效，每次调用都抛 RedisConnectionFailureException，接口统一返回「系统异常，请稍后重试」。
     * 典型受害接口：/pointsMall/order/createOrderToken、/wxPay/toPay4Applet、/smsLog/sendMobileCode。
     * 改为首次使用时真实 PING 一次再定档：本地 profile 有 Redis 时行为不变。
     */
    private boolean redisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        if (redisAvailableCache == null) {
            synchronized (this) {
                if (redisAvailableCache == null) {
                    try {
                        redisTemplate.execute(new RedisCallback() {
                            @Override
                            public Object doInRedis(RedisConnection connection) {
                                connection.ping();
                                return null;
                            }
                        });
                        redisAvailableCache = Boolean.TRUE;
                    } catch (Exception e) {
                        System.out.println("Redis 不可用，自动降级为进程内缓存：" + e.getMessage());
                        redisAvailableCache = Boolean.FALSE;
                    }
                }
            }
        }
        return redisAvailableCache.booleanValue();
    }

    private void cleanIfExpired(String key) {
        Long expireAt = localExpireAt.get(key);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            localStore.remove(key);
            localExpireAt.remove(key);
        }
    }

    /**
     * 指定缓存过期时间
     *
     * @param key
     * @param time 时间(秒)
     * @return
     */
    public void expire(String key, long time){
        if (redisAvailable()) {
            redisTemplate.expire(key, time, TimeUnit.SECONDS);
            return;
        }
        Long expireAt = localExpireAt.get(key);
        if (expireAt != null) {
            localExpireAt.put(key, System.currentTimeMillis() + time * 1000L);
        }
    }

    /**
     * 根据key 获取过期时间
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key){
        if (redisAvailable()) {
            return redisTemplate.getExpire(key);
        }
        cleanIfExpired(key);
        Long expireAt = localExpireAt.get(key);
        if (expireAt == null) {
            return 0L;
        }
        long leftSeconds = (expireAt - System.currentTimeMillis()) / 1000L;
        return leftSeconds > 0 ? leftSeconds : 0L;
    }

    /**
     * 判断key是否存在
     * @return
     */
    public boolean hasKey(String key){
        if (redisAvailable()) {
            return redisTemplate.hasKey(key);
        }
        cleanIfExpired(key);
        return localStore.containsKey(key);
    }

    /**
     * 删除缓存
     * @return
     */
    public void delete(String... key){
        if(key!=null && key.length>0){
            if (redisAvailable()) {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
                return;
            }
            for (String k : key) {
                localStore.remove(k);
                localExpireAt.remove(k);
            }
        }
    }

    /**
     * 根据key 获取缓存数据
     * @return
     */
    public Object get(String key){
        if (redisAvailable()) {
            return redisTemplate.opsForValue().get(key);
        }
        cleanIfExpired(key);
        return localStore.get(key);
    }

    /**
     * 添加缓存数据
     * @return
     */
    public void set(String key, Object value){
        if (redisAvailable()) {
            redisTemplate.opsForValue().set(key, value);
            return;
        }
        localStore.put(key, value);
        localExpireAt.remove(key);
    }

    /**
     * 添加缓存数据并设置过期时间
     *
     * @param time 时间小于等于0，将设置无限期
     * @return
     */
    public void set(String key, Object value, long time){
        if (redisAvailable()) {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            return;
        }
        localStore.put(key, value);
        if (time > 0) {
            localExpireAt.put(key, System.currentTimeMillis() + time * 1000L);
        } else {
            localExpireAt.remove(key);
        }
    }

    /**
     * 检查redis是否存活
     * @param url 服务器地址
     * @param port 端口
     * @param password redis的密码
     * @return
     */
    public Integer getRedisIsOk(String url, int port, String password) {
        int result = 0;
        try {
            //连接本地Redis服务
            Jedis jedis = new Jedis(url, port);
            jedis.auth(password);//密码
            String ping = jedis.ping();
            if (ping.equalsIgnoreCase("PONG")) {
                System.out.println("redis缓存有效！" + ping);
                result = 0;
            }
            jedis.close(); // 释放连接资源
        } catch (Exception e) {
            System.out.println("redis缓存失败！");
            result = 1;
        }
        return result;
    }
}
