package com.info.redis.controller;

import com.info.redis.config.JedisPoolUtil;
import com.info.redis.config.RedisConfigByScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Random;

/**
 * ClassName: TestController
 * Package: com.info.redis.controller
 * created By admin
 * Description:
 *
 * @date: 2022/2/23 15:18
 * @author: admin
 * @email: 1609591835@qq.com
 */
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    public StringRedisTemplate redisTemplate;

    @RequestMapping("/test")
    public String test() {
        return "成功";
    }

    @RequestMapping("/testRedis")
    public String testRedis() {
        redisTemplate.opsForValue().set("taojunKey", "taojun");
        String test = redisTemplate.opsForValue().get("taojunKey");
        return test;
    }

    /**
     * 这个本质是使用事务, 但是多个人或者多并发的情况下
     * 比如1000个请求 100个并发  可能先到的 因为redis的乐观锁 导致先到的没抢到，后到的反而抢到了
     * 比如100个库存 上述请求 请求大于库存 但是实际只有几个抢到了  其他人没抢到 有结余
     */
    @RequestMapping("/miaosha")
    public void miaosha(HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        String userId = new Random().nextInt(50000)+"";
        String prodId = request.getParameter("prodid");

        // 调用抢单方法
        boolean isSuccess = TestController.doSecKill(userId, prodId);
        response.getWriter().print(isSuccess);
    }

    /**
     * 虽然这个解决库存的问题，但不是很理解怎么引入这个的
     * 本质是: 抛弃事务, 采用 LUA 脚本的方式进行处理
     */
    @RequestMapping("/miaosha2")
    public void miaosha2(HttpServletRequest request,
                         HttpServletResponse response) throws Exception {
        String userid = new Random().nextInt(50000)+"";
        String prodid = request.getParameter("prodid");

        boolean isSuccess = RedisConfigByScript.doSecKill(userid, prodid);
        response.getWriter().print(isSuccess);
    }

    /**
     * 核心抢票方法
     *  1.uid和prodid非空判断
     *  2.连接redis
     *  3.拼接key
     *      3.1 库存key
     *      3.2 秒杀成功用户key
     *  4.获取库存, 如果库存null, 秒杀还没有开始
     *  5.判断用户是否重复秒杀操作
     *  6.判断如果商品数量, 库存数量小于1, 秒杀结束
     *  7.秒杀过程
     *      7.1 库存-1
     *      7.2 把秒杀成功的用户添加清单里面
     */
    public static boolean doSecKill(String uid, String prodid) throws Exception {
        // 1.uid和prodid非空判断
        if (null == uid || null == prodid) {return false;}

        // 2.连接redis
//        Jedis jedis = new Jedis("192.168.6.128", 6379);
        //  2.1 通过线程池去获取
        JedisPool jedisPoolInstance = JedisPoolUtil.getJedisPoolInstance();
        Jedis jedis = jedisPoolInstance.getResource();

        // 3.拼接key
        //  3.1 库存key
        String kcKey = "sk:" + prodid + ":qt";
        //  3.2 秒杀成功用户key
        String userKey = "sk:" + prodid + ":user";

        // 监视key
        jedis.watch(kcKey);

        // 4.获取库存, 如果库存null, 秒杀还没有开始
        String kc = jedis.get(kcKey);
        if (null == kc) {
            System.out.println("秒杀还没有开始, 请等待");
            jedis.close();
            return false;
        }

        // 5.判断用户是否重复秒杀操作
        if(jedis.sismember(userKey, uid)) {
            System.out.println("已经秒杀成功, 不能重复秒杀");
            jedis.close();
            return false;
        }

        // 6.判断如果商品数量, 库存数量小于1, 秒杀结束
        if (Integer.parseInt(kc)<=0) {
            System.out.println("秒杀已经结束了");
            jedis.close();
            return false;
        }

        // 7.秒杀过程

        // 事务操作
        Transaction multi = jedis.multi();

        multi.decr(kcKey);
        multi.sadd(userKey, uid);

        List<Object> results = multi.exec();
        if (null == results || 0 == results.size()) {
            System.out.println("秒杀失败……");
            jedis.close();
            return false;
        }

        //  7.1 库存-1
//        jedis.decr(kcKey);
        //  7.2 把秒杀成功的用户添加清单里面
//        jedis.sadd(userKey, uid);
        System.out.println("秒杀成功了……");
        jedis.close();
        return true;
    }
}
