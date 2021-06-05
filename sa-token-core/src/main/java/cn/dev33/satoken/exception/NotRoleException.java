package cn.dev33.satoken.exception;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 没有指定角色标识，抛出的异常 
 * 
 * @author kong
 *
 */
public class NotRoleException extends SaTokenException {

	/**
	 * 序列化版本号 
	 */
	private static final long serialVersionUID = 8243974276159004739L;

	/** 角色标识 */
	private String role;

	/**
	 * @return 获得角色标识
	 */
	public String getRole() {
		return role;
	}

	/**
	 * loginKey
	 */
	private String loginKey;

	/**
	 * 获得loginKey
	 * 
	 * @return loginKey
	 */
	public String getLoginKey() {
		return loginKey;
	}

	public NotRoleException(String role) {
		this(role, StpUtil.stpLogic.loginKey);
	}

	public NotRoleException(String role, String loginKey) {
		super("无此角色：" + role);
		this.role = role;
		this.loginKey = loginKey;
	}

}
