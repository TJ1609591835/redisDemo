package com.info.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;


/**
 * ClassName: RedisConfigByScript
 * Package: com.info.redis.config
 * created By admin
 * Description:
 *
 * @date: 2022/2/24 14:49
 * @author: admin
 * @email: 1609591835@qq.com
 */
public class RedisConfigByScript {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfigByScript.class);

    public static void main(String[] args) {
        JedisPool jedispool = JedisPoolUtil.getJedisPoolInstance();
        Jedis jedis = jedispool.getResource();
        System.out.println(jedis.ping());
        Set<HostAndPort> set = new HashSet<>();
        doSecKill("201", "sk:0101");
    }

    static String secKillScript = "local userid=KEYS[1];\r\n" +
            "local prodid=KEYS[2];\r\n" +
            "local qtkey='sk:'..prodid..\":qt\";\r\n" +
            "local usersKey='sk:'..prodid..\":user\";\r\n" +
            "local userExists=redis.call(\"sismember\",usersKey,userid);\r\n" +
            "if tonumber(userExists)==1 then \r\n" +
            "   return 2;\r\n" +
            "end\r\n" +
            "local num= redis.call(\"get\",qtkey);\r\n" +
            "if tonumber(num)<=0 then \r\n" +
            "   return 0;\r\n" +
            "else \r\n" +
            "   redis.call(\"decr\",qtkey);\r\n" +
            "   redis.call(\"sadd\",usersKey,userid);\r\n" +
            "end\r\n" +
            "return 1";

    static String secKillScript2 =
            "local userExists=redis.call(\"sismember\",\"{sk}:0101:user\",userid);\r\n" +
            " return 1";

    public static boolean doSecKill(String userid, String prodid) {
        JedisPool jedisPool = JedisPoolUtil.getJedisPoolInstance();
        Jedis jedis = jedisPool.getResource();

        String sha1 = jedis.scriptLoad(secKillScript);
        Object result = jedis.evalsha(sha1, 2, userid, prodid);

        String reString = String.valueOf(result);
        if ("0".equals(reString)) {
            System.err.println("已抢空！！");
        } else if ("1".equals(reString)) {
            System.err.println("抢购成功！！！！");
        } else if ("2".equals(reString)) {
            System.err.println("该用户已抢过！！");
        } else {
            System.err.println("抢购异常！！");
        }
        jedis.close();
        return true;
    }
}
