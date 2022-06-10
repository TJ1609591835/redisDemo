package com.info.redis.config;


import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

/**
 * ClassName: JedisCluster
 * Package: com.info.redis.config
 * created By admin
 * Description:
 *
 * @date: 2022/3/30 22:44
 * @author: admin
 * @email: 1609591835@qq.com
 */
public class JedisClusterTest {
    public static void main(String[] args) {
        HostAndPort hostAndPort = new HostAndPort("192.168.6.128", 6379);
        JedisCluster jedisCluster = new JedisCluster(hostAndPort);
        try {
            jedisCluster.set("k1", "v1");
            System.out.println(jedisCluster.get("k1"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        jedisCluster.close();
    }
}
