package com.info.redis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: RedisNxExTestController
 * Package: com.info.redis.config
 * created By admin
 * Description:
 *
 * @date: 2022/4/9 15:12
 * @author: admin
 * @email: 1609591835@qq.com
 */
@RestController
@RequestMapping("/redisTest")
public class RedisNxExTestController {
    @Autowired
//    private RedisTemplate redisTemplate;
    private StringRedisTemplate redisTemplate;

    /**
     * 分布式的锁代码
     */
    @GetMapping("/testLock")
    public void testLock() {
        System.out.println(redisTemplate);
        String uuid = UUID.randomUUID().toString();
        // 1. 读取锁, setnx ex
        // 注: 这个地方相当于无线时长 所以用到了 del
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111");
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        // 2. 获取锁成功, 查询 num 的值
        if (lock) {
            Object value = redisTemplate.opsForValue().get("num");
            // 2.1 判断 num 为空 return
            if (StringUtils.isEmpty(value)) {
                return;
            }

            // 2.2 有值就转成成 int
            int num = Integer.parseInt(value+"");
//            // 2.3 把 redis 的 num 加1
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 判断比较 uuid 值是否一样
            String lockUuid = (String) redisTemplate.opsForValue().get("lock");
            if (uuid.equals(lockUuid)) {
                // 2.4 释放锁, del
                redisTemplate.delete("lock");
            }
        } else {
            // 3. 获取锁失败, 每隔 0.1s 再获取
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * lua脚本
     */
    @GetMapping("/testLockLua")
    public void testLockLua() {
        // 1. 声明一个 uuid, 将做为一个 value 放入我们的 key 所对应的值中
        String uuid = UUID.randomUUID().toString();
        // 2. 定义一个锁: lua脚本可以使用同一把锁, 来实现删除！
        String skuId = "25"; // 访问 skuId 为 25号的商品 100008348542
        String lockKey = "lock:" + skuId; // 锁住的是每个商品的数据

        // 3. 获取锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 3, TimeUnit.SECONDS);

        // 第一种: lock 与过期时间中间不写任何的代码。
        // redisTemplate.expire("lock", 10, TimeUnit.SECONDS); // 设置过期时间
        // 如果 true
        if (lock) {
            // 执行的业务逻辑开始
            // 获取缓存中的num数据
            Object value = redisTemplate.opsForValue().get("num");
            // 如果是空直接返回
            if (StringUtils.isEmpty(value)) {
                return;
            }
            // 不是空 如果说在这出现了异常！ 那么delete 就删除失败！也就是锁永远存在！
            int num = Integer.parseInt(value + "");
            // 使num 每次+1放入缓存
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
            /* 使用lua脚本来锁 */
            // 定义lua脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            // 使用 redis执行 lua脚本
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            // 设置一下返回值的类型 为 Long
            // 因为删除判断的时候, 返回的0, 给其封装为数据类型。如果不封装默认返回String 类型,
            // 那么返回字符串与0 会有发生错误。
            redisScript.setResultType(Long.class);
            // 第一个要是script 脚本, 第二个需要判断的 key , 第三个就是 key 所对应的值
            redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
        } else {
            // 其他线程等待
            try {
                // 睡眠
                Thread.sleep(1000);
                // 睡醒了之后, 调用方法。
                testLockLua();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
