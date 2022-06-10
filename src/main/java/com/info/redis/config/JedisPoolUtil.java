package com.info.redis.config;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * ClassName: JedisPoolUtil
 * Package: com.info.redis.config
 * created By admin
 * Description:
 *
 * @date: 2022/2/24 10:55
 * @author: admin
 * @email: 1609591835@qq.com
 */
public class JedisPoolUtil {
    private static volatile JedisPool jedisPool = null;

    private JedisPoolUtil() {

    }

    public static JedisPool getJedisPoolInstance() {
        if (null == jedisPool) {
            synchronized (JedisPoolUtil.class) {
                if (null == jedisPool) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    // 最大连接数
                    poolConfig.setMaxTotal(200);
                    // 最大空闲连接
                    poolConfig.setMaxIdle(32);
                    // 最大等待毫秒数(100 * 1000ms) = 1min + 40s
                    poolConfig.setMaxWaitMillis(100*1000);
                    //
                    poolConfig.setBlockWhenExhausted(true);
                    // ping PONG
                    poolConfig.setTestOnBorrow(true);

                    jedisPool = new JedisPool(poolConfig, "192.168.6.128", 6379, 6000);
                }
            }
        }
        return jedisPool;
    }

    // 这个地方的returnResource方法会报错
//    public static void release(JedisPool jedisPool, Jedis jedis) {
//        if (null != jedis) {
//            jedisPool.returnResource(jedis);
//        }
//    }
}
