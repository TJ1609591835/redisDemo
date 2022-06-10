package com.info.redis.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: DistributeLock
 * Package: com.info.redis.ann
 * created By admin
 * Description:
 *
 * @date: 2022/4/10 16:04
 * @author: admin
 * @email: 1609591835@qq.com
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributeLock {
    /**
     * @author 锁的key前缀
     */
    String lockKeyPrefix() default "";
    /**
     * @author 业务code
     */
    String businessCode() default "";
    /**
     * @author 获取锁的等待时间(毫秒)
     */
    int waitTime() default 0;
    /**
     * @author 释放的时间(毫秒)
     */
    int leaseTime() default 0;
}
