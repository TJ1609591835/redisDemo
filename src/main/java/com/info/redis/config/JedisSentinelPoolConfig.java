package com.info.redis.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassName: JedisSentinelPool
 * Package: com.info.redis.config
 * created By admin
 * Description:
 *
 * @date: 2022/2/28 17:23
 * @author: admin
 * @email: 1609591835@qq.com
 */
public class JedisSentinelPoolConfig {
    private static JedisSentinelPool jedisSentinelPool = null;

    public static Jedis getJedisFromSentinel() {
        if (null == jedisSentinelPool) {
            Set<String> sentinelSet = new HashSet<>();
            sentinelSet.add("192.168.11.103:26379");

            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            // 最大可用连接数
            jedisPoolConfig.setMaxTotal(10);
            // 最大闲置连接数
            jedisPoolConfig.setMaxIdle(5);
            // 最小闲置连接数
            jedisPoolConfig.setMinIdle(5);
            // 连接耗尽是否等待
            jedisPoolConfig.setBlockWhenExhausted(true);
            // 等待时间
            jedisPoolConfig.setMaxWaitMillis(2000);
            // 取连接的时候进行一下测试 ping - pong
            jedisPoolConfig.setTestOnBorrow(true);

            jedisSentinelPool = new JedisSentinelPool("mymaster", sentinelSet, jedisPoolConfig);
            return jedisSentinelPool.getResource();
        }
        return null;
    }
}
