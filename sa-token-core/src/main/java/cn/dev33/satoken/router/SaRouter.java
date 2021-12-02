package cn.dev33.satoken.router;

import java.util.List;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.BackResultException;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.fun.SaFunction;
import cn.dev33.satoken.fun.SaParamFunction;
import cn.dev33.satoken.fun.SaParamRetFunction;

/**
 * 路由匹配操作工具类 
 * @author kong
 *
 */
public class SaRouter {

	// -------------------- 路由匹配相关 -------------------- 
	
	/**
	 * 路由匹配
	 * @param pattern 路由匹配符 
	 * @param path 被匹配的路由  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(String pattern, String path) {
		return SaManager.getSaTokenContextOrSecond().matchPath(pattern, path);
	}

	/**
	 * 路由匹配   
	 * @param patterns 路由匹配符集合 
	 * @param path 被匹配的路由  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(List<String> patterns, String path) {
		if(patterns == null) {
			return false;
		}
		for (String pattern : patterns) {
			if(isMatch(pattern, path)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 路由匹配   
	 * @param patterns 路由匹配符数组  
	 * @param path 被匹配的路由  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(String[] patterns, String path) {
		if(patterns == null) {
			return false;
		}
		for (String pattern : patterns) {
			if(isMatch(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Http请求方法匹配 
	 * @param methods Http请求方法断言数组  
	 * @param methodString Http请求方法
	 * @return 是否匹配成功 
	 */
	public static boolean isMatch(SaHttpMethod[] methods, String methodString) {
		if(methods == null) {
			return false;
		}
		for (SaHttpMethod method : methods) {
			if(method == SaHttpMethod.ALL || (method != null && method.toString().equalsIgnoreCase(methodString))) {
				return true;
			}
		}
		return false;
	}
	
	// ------ 使用当前URI匹配 
	
	/**
	 * 路由匹配 (使用当前URI) 
	 * @param pattern 路由匹配符 
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrURI(String pattern) {
		return isMatch(pattern, SaHolder.getRequest().getRequestPath());
	}

	/**
	 * 路由匹配 (使用当前URI) 
	 * @param patterns 路由匹配符集合 
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrURI(List<String> patterns) {
		return isMatch(patterns, SaHolder.getRequest().getRequestPath());
	}

	/**
	 * 路由匹配 (使用当前URI) 
	 * @param patterns 路由匹配符数组 
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrURI(String[] patterns) {
		return isMatch(patterns, SaHolder.getRequest().getRequestPath());
	}

	/**
	 * Http请求方法匹配 (使用当前请求方式) 
	 * @param methods Http请求方法断言数组  
	 * @return 是否匹配成功 
	 */
	public static boolean isMatchCurrMethod(SaHttpMethod[] methods) {
		return isMatch(methods, SaHolder.getRequest().getMethod());
	}
	

	// -------------------- 开始匹配 -------------------- 
	
	/**
	 * 初始化一个SaRouterStaff，开始匹配
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff newMatch() {
		return new SaRouterStaff();
	}

	// ----------------- path匹配 
	
	/**
	 * 路由匹配 
	 * @param patterns 路由匹配符集合
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff match(String... patterns) {
		return new SaRouterStaff().match(patterns);
	}

	/**
	 * 路由匹配排除 
	 * @param patterns 路由匹配符排除数组  
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff notMatch(String... patterns) {
		return new SaRouterStaff().notMatch(patterns);
	}

	/**
	 * 路由匹配 
	 * @param patterns 路由匹配符集合 
	 * @return 对象自身 
	 */
	public static SaRouterStaff match(List<String> patterns) {
		return new SaRouterStaff().match(patterns);
	}

	/**
	 * 路由匹配排除 
	 * @param patterns 路由匹配符排除集合 
	 * @return 对象自身 
	 */
	public static SaRouterStaff notMatch(List<String> patterns) {
		return new SaRouterStaff().notMatch(patterns);
	}

	// ----------------- Method匹配 
	
	/**
	 * Http请求方式匹配 (Enum) 
	 * @param methods Http请求方法断言数组  
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff match(SaHttpMethod... methods) {
		return new SaRouterStaff().match(methods);
	}

	/**
	 * Http请求方法匹配排除 (Enum) 
	 * @param methods Http请求方法断言排除数组  
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff notMatch(SaHttpMethod... methods) {
		return new SaRouterStaff().notMatch(methods);
	}

	/**
	 * Http请求方法匹配 (String)  
	 * @param methods Http请求方法断言数组  
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff matchMethod(String... methods) {
		return new SaRouterStaff().matchMethod(methods);
	}

	/**
	 * Http请求方法匹配排除 (String) 
	 * @param methods Http请求方法断言排除数组  
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff notMatchMethod(String... methods) {
		return new SaRouterStaff().notMatchMethod(methods);
	}
	
	// ----------------- 条件匹配 

	/**
	 * 根据 boolean 值进行匹配 
	 * @param flag boolean值 
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff match(boolean flag) {
		return new SaRouterStaff().match(flag);
	}

	/**
	 * 根据 boolean 值进行匹配排除 
	 * @param flag boolean值 
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff notMatch(boolean flag) {
		return new SaRouterStaff().notMatch(flag);
	}
	
	/**
	 * 根据自定义方法进行匹配 (lazy) 
	 * @param fun 自定义方法
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff match(SaParamRetFunction<Object, Boolean> fun) {
		return new SaRouterStaff().match(fun);
	}

	/**
	 * 根据自定义方法进行匹配排除 (lazy) 
	 * @param fun 自定义排除方法
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff notMatch(SaParamRetFunction<Object, Boolean> fun) {
		return new SaRouterStaff().notMatch(fun);
	}

	
	// -------------------- 直接指定check函数 -------------------- 
	
	/**
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符
	 * @param fun 要执行的校验方法 
	 * @return /
	 */
	public static SaRouterStaff match(String pattern, SaFunction fun) {
		return new SaRouterStaff().match(pattern, fun);
	}

	/**
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符
	 * @param fun 要执行的校验方法 
	 * @return /
	 */
	public static SaRouterStaff match(String pattern, SaParamFunction<SaRouterStaff> fun) {
		return new SaRouterStaff().match(pattern, fun);
	}

	/**
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符 
	 * @param excludePattern 要排除的路由匹配符 
	 * @param fun 要执行的方法 
	 * @return /
	 */
	public static SaRouterStaff match(String pattern, String excludePattern, SaFunction fun) {
		return new SaRouterStaff().match(pattern, excludePattern, fun);
	}

	/**
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param pattern 路由匹配符 
	 * @param excludePattern 要排除的路由匹配符 
	 * @param fun 要执行的方法 
	 * @return /
	 */
	public static SaRouterStaff match(String pattern, String excludePattern, SaParamFunction<SaRouterStaff> fun) {
		return new SaRouterStaff().match(pattern, excludePattern, fun);
	}

	
	// -------------------- 提前退出 -------------------- 
	
	/**
	 * 停止匹配，跳出函数 (在多个匹配链中一次性跳出Auth函数) 
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff stop() {
		throw new StopMatchException();
	}

	/**
	 * 停止匹配，结束执行，向前端返回结果 
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff back() {
		throw new BackResultException("");
	}
	
	/**
	 * 停止匹配，结束执行，向前端返回结果 
	 * @param result 要输出的结果 
	 * @return SaRouterStaff
	 */
	public static SaRouterStaff back(Object result) {
		throw new BackResultException(result);
	}


	// -------------------- 历史API兼容 -------------------- 
	
	/**
	 * <h1>本函数设计已过时，请更换为：SaRouter.match(path...).ckeck(fun) </h1>
	 * 路由匹配，如果匹配成功则执行认证函数 
	 * @param patterns 路由匹配符集合
	 * @param function 要执行的方法 
	 */
	@Deprecated
	public static void match(List<String> patterns, SaFunction function) {
		if(isMatchCurrURI(patterns)) {
			function.run();
		}
	}

	/**
	 * <h1>本函数设计已过时，请更换为：SaRouter.match(path...).notMatch(path...).ckeck(fun) </h1>
	 * 路由匹配 (并指定排除匹配符)，如果匹配成功则执行认证函数 
	 * @param patterns 路由匹配符集合
	 * @param excludePatterns 要排除的路由匹配符集合
	 * @param function 要执行的方法 
	 */
	@Deprecated
	public static void match(List<String> patterns, List<String> excludePatterns, SaFunction function) {
		if(isMatchCurrURI(patterns)) {
			if(isMatchCurrURI(excludePatterns) == false) {
				function.run();
			}
		}
	}

}
