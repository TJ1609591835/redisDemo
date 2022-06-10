package com.info.redis.task;

import com.info.redis.ann.DistributeLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ClassName: SetNxExTask
 * Package: com.info.redis.task
 * created By admin
 * Description:
 *
 * @date: 2022/4/10 16:02
 * @author: admin
 * @email: 1609591835@qq.com
 */
@Component
public class SetNxExTask {
    private final static Logger logger = LoggerFactory.getLogger(SetNxExTask.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 定时任务
     * 0 15 10 ? * *" 每天上午10:15触发
     */
//    @Scheduled(cron = "0 43 16 ? * *")
    @Scheduled(cron = "*/30 * * * * ?")
    @DistributeLock(lockKeyPrefix = "taojunTask", businessCode = "taojunYeWuCode", waitTime = 10, leaseTime = 1000*1)
    public void testTask() {
        logger.info("定时任务开始");
        String countTaojunTask = redisTemplate.opsForValue().get("countTaojunTask");
        int nowCount = Integer.valueOf(countTaojunTask) + 1;
        redisTemplate.opsForValue().set("countTaojunTask", nowCount+"");
        logger.info("定时任务结束");
    }
}
