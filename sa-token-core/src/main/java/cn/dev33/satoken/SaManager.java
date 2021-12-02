package cn.dev33.satoken;

import java.util.HashMap;
import java.util.Map;

import cn.dev33.satoken.action.SaTokenAction;
import cn.dev33.satoken.action.SaTokenActionDefaultImpl;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.config.SaTokenConfigFactory;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextDefaultImpl;
import cn.dev33.satoken.context.second.SaTokenSecondContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.listener.SaTokenListenerDefaultImpl;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpInterfaceDefaultImpl;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.temp.SaTempDefaultImpl;
import cn.dev33.satoken.temp.SaTempInterface;
import cn.dev33.satoken.util.SaFoxUtil;

/**
 * 管理 Sa-Token 所有全局组件  
 * @author kong
 *
 */
@SuppressWarnings("deprecation")
public class SaManager {

	/**
	 * 配置文件 Bean 
	 */
	public volatile static SaTokenConfig config;	
	public static void setConfig(SaTokenConfig config) {
		SaManager.config = config;
		if(config.getIsPrint()) {
			SaFoxUtil.printSaToken();
		}
		// 调用一次StpUtil中的方法，保证其可以尽早的初始化 StpLogic 
		StpUtil.getLoginType();
	}
	public static SaTokenConfig getConfig() {
		if (config == null) {
			synchronized (SaManager.class) {
				if (config == null) {
					setConfig(SaTokenConfigFactory.createConfig());
				}
			}
		}
		return config;
	}
	
	/**
	 * 持久化 Bean 
	 */
	private volatile static SaTokenDao saTokenDao;
	public static void setSaTokenDao(SaTokenDao saTokenDao) {
		if((SaManager.saTokenDao instanceof SaTokenDaoDefaultImpl)) {
			((SaTokenDaoDefaultImpl)SaManager.saTokenDao).endRefreshThread();
		}
		SaManager.saTokenDao = saTokenDao;
	}
	public static SaTokenDao getSaTokenDao() {
		if (saTokenDao == null) {
			synchronized (SaManager.class) {
				if (saTokenDao == null) {
					setSaTokenDao(new SaTokenDaoDefaultImpl());
				}
			}
		}
		return saTokenDao;
	}
	
	/**
	 * 权限认证 Bean 
	 */
	private volatile static StpInterface stpInterface;
	public static void setStpInterface(StpInterface stpInterface) {
		SaManager.stpInterface = stpInterface;
	}
	public static StpInterface getStpInterface() {
		if (stpInterface == null) {
			synchronized (SaManager.class) {
				if (stpInterface == null) {
					setStpInterface(new StpInterfaceDefaultImpl());
				}
			}
		}
		return stpInterface;
	}
	
	/**
	 * 框架行为 Bean 
	 */
	private volatile static SaTokenAction saTokenAction;
	public static void setSaTokenAction(SaTokenAction saTokenAction) {
		SaManager.saTokenAction = saTokenAction;
	}
	public static SaTokenAction getSaTokenAction() {
		if (saTokenAction == null) {
			synchronized (SaManager.class) {
				if (saTokenAction == null) {
					setSaTokenAction(new SaTokenActionDefaultImpl());
				}
			}
		}
		return saTokenAction;
	}
	
	/**
	 * 上下文Context Bean  
	 */
	private volatile static SaTokenContext saTokenContext;
	public static void setSaTokenContext(SaTokenContext saTokenContext) {
		SaManager.saTokenContext = saTokenContext;
	}
	public static SaTokenContext getSaTokenContext() {
		return saTokenContext;
	}
	
	/**
	 * 二级Context 
	 */
	private volatile static SaTokenSecondContext saTokenSecondContext;
	public static SaTokenSecondContext getSaTokenSecondContext() {
		return saTokenSecondContext;
	}
	public static void setSaTokenSecondContext(SaTokenSecondContext saTokenSecondContext) {
		SaManager.saTokenSecondContext = saTokenSecondContext;
	}
	
	/**
	 * 获取一个可用的SaTokenContext 
	 * @return / 
	 */
	public static SaTokenContext getSaTokenContextOrSecond() {
		
		// s1. 一级Context可用时返回一级Context
		if(saTokenContext != null) {
			if(saTokenSecondContext == null || saTokenContext.isValid()) {
				// 因为 isValid 是一个耗时操作，所以此处假定：二级Context为null的情况下无需验证一级Context有效性 
				// 这样可以提升6倍左右的上下文获取速度 
				return saTokenContext;
			}
		}
		
		// s2. 一级Context不可用时判断二级Context是否可用 
		if(saTokenSecondContext != null && saTokenSecondContext.isValid()) {
			return saTokenSecondContext;
		}
		
		// s3. 都不行，就返回默认的 Context 
		return SaTokenContextDefaultImpl.defaultContext; 
	}

	/**
	 * 侦听器 Bean  
	 */
	private volatile static SaTokenListener saTokenListener;
	public static void setSaTokenListener(SaTokenListener saTokenListener) {
		SaManager.saTokenListener = saTokenListener;
	}
	public static SaTokenListener getSaTokenListener() {
		if (saTokenListener == null) {
			synchronized (SaManager.class) {
				if (saTokenListener == null) {
					setSaTokenListener(new SaTokenListenerDefaultImpl());
				}
			}
		}
		return saTokenListener;
	}

	/**
	 * 临时令牌验证模块 Bean  
	 */
	private volatile static SaTempInterface saTemp;
	public static void setSaTemp(SaTempInterface saTemp) {
		SaManager.saTemp = saTemp;
	}
	public static SaTempInterface getSaTemp() {
		if (saTemp == null) {
			synchronized (SaManager.class) {
				if (saTemp == null) {
					setSaTemp(new SaTempDefaultImpl());
				}
			}
		}
		return saTemp;
	}
	
	/**
	 * StpLogic集合, 记录框架所有成功初始化的StpLogic 
	 */
	public static Map<String, StpLogic> stpLogicMap = new HashMap<String, StpLogic>();
	
	/**
	 * 向集合中 put 一个 StpLogic 
	 * @param stpLogic StpLogic
	 */
	public static void putStpLogic(StpLogic stpLogic) {
		stpLogicMap.put(stpLogic.getLoginType(), stpLogic);
	}

	/**
	 * 根据 LoginType 获取对应的StpLogic，如果不存在则抛出异常 
	 * @param loginType 对应的账号类型 
	 * @return 对应的StpLogic
	 */
	public static StpLogic getStpLogic(String loginType) {
		// 如果type为空则返回框架内置的 
		if(loginType == null || loginType.isEmpty()) {
			return StpUtil.stpLogic;
		}
		
		// 从SaManager中获取 
		StpLogic stpLogic = stpLogicMap.get(loginType);
		if(stpLogic == null) {
			/*
			 * 此时有两种情况会造成 StpLogic == null 
			 * 1. loginType拼写错误，请改正 （建议使用常量） 
			 * 2. 自定义StpUtil尚未初始化（静态类中的属性至少一次调用后才会初始化），解决方法两种
			 * 		(1) 从main方法里调用一次
			 * 		(2) 在自定义StpUtil类加上类似 @Component 的注解让容器启动时扫描到自动初始化 
			 */
			throw new SaTokenException("未能获取对应StpLogic，type="+ loginType);
		}
		
		// 返回 
		return stpLogic;
	}
	
	
}
