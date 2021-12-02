package cn.dev33.satoken.action;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckBasic;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaCheckSafe;
import cn.dev33.satoken.basic.SaBasicUtil;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.util.SaFoxUtil;
import cn.dev33.satoken.util.SaTokenConsts;

/**
 * <h1> v1.27+ 此接口已废弃，目前版本暂时向下兼容，请及时更换为 SaStrategy </h1>
 * <p> Sa-Token 逻辑代理接口 [默认实现类]  </p> 
 * @author kong
 *
 */
@Deprecated
public class SaTokenActionDefaultImpl implements SaTokenAction {

	/**
	 * 创建一个Token 
	 */
	@Override
	public String createToken(Object loginId, String loginType) {
		// 根据配置的tokenStyle生成不同风格的token 
		String tokenStyle = SaManager.getConfig().getTokenStyle();
		// uuid 
		if(SaTokenConsts.TOKEN_STYLE_UUID.equals(tokenStyle)) {
			return UUID.randomUUID().toString();
		}
		// 简单uuid (不带下划线)
		if(SaTokenConsts.TOKEN_STYLE_SIMPLE_UUID.equals(tokenStyle)) {
			return UUID.randomUUID().toString().replaceAll("-", "");
		}
		// 32位随机字符串
		if(SaTokenConsts.TOKEN_STYLE_RANDOM_32.equals(tokenStyle)) {
			return SaFoxUtil.getRandomString(32);
		}
		// 64位随机字符串
		if(SaTokenConsts.TOKEN_STYLE_RANDOM_64.equals(tokenStyle)) {
			return SaFoxUtil.getRandomString(64);
		}
		// 128位随机字符串
		if(SaTokenConsts.TOKEN_STYLE_RANDOM_128.equals(tokenStyle)) {
			return SaFoxUtil.getRandomString(128);
		}
		// tik风格 (2_14_16)
		if(SaTokenConsts.TOKEN_STYLE_TIK.equals(tokenStyle)) {
			return SaFoxUtil.getRandomString(2) + "_" + SaFoxUtil.getRandomString(14) + "_" + SaFoxUtil.getRandomString(16) + "__";
		}
		// 默认，还是uuid 
		return UUID.randomUUID().toString();
	}

	/**
	 * 创建一个Session 
	 */
	@Override
	public SaSession createSession(String sessionId) {
		return new SaSession(sessionId);
	}

	/**
	 * 判断：集合中是否包含指定元素（模糊匹配） 
	 */
	@Override
	public boolean hasElement(List<String> list, String element) {
		
		// 空集合直接返回false
		if(list == null || list.size() == 0) {
			return false;
		}

		// 先尝试一下简单匹配，如果可以匹配成功则无需继续模糊匹配 
		if (list.contains(element)) {
			return true;
		}
		
		// 开始模糊匹配 
		for (String patt : list) {
			if(SaFoxUtil.vagueMatch(patt, element)) {
				return true;
			}
		}
		
		// 走出for循环说明没有一个元素可以匹配成功 
		return false;
	}

	/**
	 * 对一个Method对象进行注解检查（注解鉴权内部实现） 
	 */
	@Override
	public void checkMethodAnnotation(Method method) {

		// 先校验 Method 所属 Class 上的注解 
		validateAnnotation(method.getDeclaringClass());
		
		// 再校验 Method 上的注解  
		validateAnnotation(method);
	}

	/**
	 * 从指定元素校验注解 
	 * @param target see note 
	 */
	public void validateAnnotation(AnnotatedElement target) {
		
		// 校验 @SaCheckLogin 注解 
		SaCheckLogin checkLogin = (SaCheckLogin) SaStrategy.me.getAnnotation.apply(target, SaCheckLogin.class);
		if(checkLogin != null) {
			SaManager.getStpLogic(checkLogin.type()).checkByAnnotation(checkLogin);
		}
		
		// 校验 @SaCheckRole 注解 
		SaCheckRole checkRole = (SaCheckRole) SaStrategy.me.getAnnotation.apply(target, SaCheckRole.class);
		if(checkRole != null) {
			SaManager.getStpLogic(checkRole.type()).checkByAnnotation(checkRole);
		}
		
		// 校验 @SaCheckPermission 注解
		SaCheckPermission checkPermission = (SaCheckPermission) SaStrategy.me.getAnnotation.apply(target, SaCheckPermission.class);
		if(checkPermission != null) {
			SaManager.getStpLogic(checkPermission.type()).checkByAnnotation(checkPermission);
		}

		// 校验 @SaCheckSafe 注解
		SaCheckSafe checkSafe = (SaCheckSafe) SaStrategy.me.getAnnotation.apply(target, SaCheckSafe.class);
		if(checkSafe != null) {
			SaManager.getStpLogic(checkSafe.type()).checkByAnnotation(checkSafe);
		}
		
		// 校验 @SaCheckBasic 注解
		SaCheckBasic checkBasic = (SaCheckBasic) SaStrategy.me.getAnnotation.apply(target, SaCheckBasic.class);
		if(checkBasic != null) {
			SaBasicUtil.check(checkBasic.realm(), checkBasic.account());
		}
		
	}
	
}
