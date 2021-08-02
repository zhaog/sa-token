package cn.dev33.satoken.action;

import java.lang.reflect.Method;
import java.util.List;

import cn.dev33.satoken.session.SaSession;

/**
 * Sa-Token 逻辑代理接口 
 * <p>此接口将会代理框架内部的一些关键性逻辑，方便开发者进行按需重写</p> 
 * @author kong
 *
 */
public interface SaTokenAction {

	/**
	 * 创建一个Token 
	 * @param loginId 账号id 
	 * @param loginType 账号类型 
	 * @return token
	 */
	public String createToken(Object loginId, String loginType); 
	
	/**
	 * 创建一个Session 
	 * @param sessionId Session的Id
	 * @return 创建后的Session 
	 */
	public SaSession createSession(String sessionId); 
	
	/**
	 * 判断：集合中是否包含指定元素（模糊匹配） 
	 * @param list 集合
	 * @param element 元素
	 * @return 是否包含
	 */
	public boolean hasElement(List<String> list, String element);

	/**
	 * 对一个Method对象进行注解检查（注解鉴权内部实现） 
	 * @param method Method对象
	 */
	public void checkMethodAnnotation(Method method);
	
}
