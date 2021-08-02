package cn.dev33.satoken.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.util.SaTokenConsts;

/**
 * sa-token 基于 Spring Aop 的注解鉴权
 * 
 * @author kong
 */
@Aspect
@Component
@Order(SaTokenConsts.ASSEMBLY_ORDER)
public class SaCheckAspect {
	
	/**
	 * 构建
	 */
	public SaCheckAspect() {
	}

	/**
	 * 定义AOP签名 (切入所有使用sa-token鉴权注解的方法)
	 */
	public static final String POINTCUT_SIGN = 
			"@within(cn.dev33.satoken.annotation.SaCheckLogin) || @annotation(cn.dev33.satoken.annotation.SaCheckLogin) || "
			+ "@within(cn.dev33.satoken.annotation.SaCheckRole) || @annotation(cn.dev33.satoken.annotation.SaCheckRole) || "
			+ "@within(cn.dev33.satoken.annotation.SaCheckPermission) || @annotation(cn.dev33.satoken.annotation.SaCheckPermission) || "
			+ "@within(cn.dev33.satoken.annotation.SaCheckSafe) || @annotation(cn.dev33.satoken.annotation.SaCheckSafe)";

	/**
	 * 声明AOP签名
	 */
	@Pointcut(POINTCUT_SIGN)
	public void pointcut() {
	}

	/**
	 * 环绕切入
	 * 
	 * @param joinPoint 切面对象
	 * @return 底层方法执行后的返回值
	 * @throws Throwable 底层方法抛出的异常
	 */
	@Around("pointcut()")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		
		// 注解鉴权
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		SaManager.getSaTokenAction().checkMethodAnnotation(signature.getMethod());

		try {
			// 执行原有逻辑
			Object obj = joinPoint.proceed();
			return obj;
		} catch (Throwable e) {
			throw e;
		}
	}

}
