package com.chess2.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceTimingAspect {
    private static final Logger log=LoggerFactory.getLogger(ServiceTimingAspect.class);
    @Around("execution(public * com.chess2.service..*(..))") public Object measure(ProceedingJoinPoint joinPoint) throws Throwable { long start=System.nanoTime(); try { return joinPoint.proceed(); } finally { log.info("service={} elapsedMs={}",joinPoint.getSignature().toShortString(),(System.nanoTime()-start)/1_000_000); } }
}
