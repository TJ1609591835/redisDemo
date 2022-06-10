package com.info.redis.aspect;

import com.alibaba.fastjson.JSONObject;
import com.info.redis.ann.DistributeLock;
import com.info.redis.util.ServerEnvUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: DistributeLockAspect
 * Package: com.info.redis.aspect
 * created By admin
 * Description:
 *
 * @date: 2022/4/10 16:13
 * @author: admin
 * @email: 1609591835@qq.com
 */
@Aspect
@Component
public class DistributeLockAspect {
    private final static Logger logger = LoggerFactory.getLogger(DistributeLockAspect.class);

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.info.redis.ann.DistributeLock)")
    public Object interceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        JSONObject responseMsg = new JSONObject();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);
        String lockKey = getLockKeyName(distributeLock,joinPoint);
        RLock rLock = redissonClient.getLock(lockKey);
        boolean b = getLock(rLock, distributeLock);
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        String remark = className + "." + methodName;
        try {
            // 为了拿端口
            String localPort = ServerEnvUtil.getLocalPort();
            if(b) {
                remark += "lockKey为: " + lockKey + " 端口为" + localPort + "服务，抢到锁，开始执行";
                logger.info("lockKey为："+lockKey+" 端口为" + localPort + "服务，抢到锁，开始执行");
                return joinPoint.proceed();
            } else {
                remark += "lockKey为："+lockKey+" 端口为" + localPort + "服务，未抢到锁，未执行";
                responseMsg.put("code", "500");
                responseMsg.put("errMsg", "服务器繁忙,请稍后再试");
                return responseMsg;
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseMsg.put("code", "500");
            responseMsg.put("errMsg", "服务器繁忙,请稍后再试");
            return responseMsg;
        } finally {
            System.out.println("结果为:" + remark);
        }
    }

    private String getLockKeyName(DistributeLock distributeLock, JoinPoint joinPoint) {
        String lockKeyPrefix = distributeLock.lockKeyPrefix();
        String businessCode = distributeLock.businessCode();
        if(StringUtils.isNotBlank(lockKeyPrefix)&&StringUtils.isNotBlank(businessCode)){
            return lockKeyPrefix+":"+businessCode;
        }else if(StringUtils.isNotBlank(lockKeyPrefix)){
            return lockKeyPrefix;
        }else if(StringUtils.isNotBlank(businessCode)){
            return businessCode;
        }else {
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            return className+"."+methodName;
        }
    }
    private boolean getLock(RLock rLock, DistributeLock distributeLock) throws InterruptedException {
        int waitTime = distributeLock.waitTime();
        int leaseTime = distributeLock.leaseTime();
        boolean b = false;
        if(waitTime > 0 && leaseTime > 0){
            b = rLock.tryLock(waitTime, leaseTime, TimeUnit.MICROSECONDS);
        }else if(leaseTime > 0){
            b = rLock.tryLock(0, leaseTime, TimeUnit.MILLISECONDS);
        }else if(waitTime > 0){
            b = rLock.tryLock(waitTime, TimeUnit.MILLISECONDS);
        }else {
            // 加锁
            b = rLock.tryLock();
        }
        return b;
    }
}