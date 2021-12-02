package cn.dev33.satoken.context.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.util.SaFoxUtil;

/**
 * Cookie Model  
 * @author kong
 *
 */
public class SaCookie {
	
	/**
	 * 写入响应头时使用的key 
	 */
	public static final String HEADER_NAME = "Set-Cookie";

	/**
	 * 名称 
	 */
    private String name;
    
    /**
     * 值 
     */
    private String value;

    /**
     * 有效时长 （单位：秒），-1代表为临时Cookie 浏览器关闭后自动删除 
     */
    private int maxAge = -1; 
    
    /**
     * 域
     */
    private String domain; 

    /**
     * 路径 
     */
    private String path;

    /**
     * 是否只在 https 协议下有效 
     */
    private Boolean secure = false; 
    
    /**
     * 是否禁止 js 操作 Cookie 
     */
    private Boolean httpOnly = false; 
    
    /**
     * 第三方限制级别（Strict=完全禁止，Lax=部分允许，None=不限制）
     */
	private String sameSite;

	
	/**
	 * 构造一个 
	 */
	public SaCookie() {
	}
	
	/**
	 * 构造一个 
	 * @param name 名字
	 * @param value 值 
	 */
	public SaCookie(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	

	/**
	 * @return 名称 
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name 名称 
	 * @return 对象自身 
	 */
	public SaCookie setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return 值 
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value 值 
	 * @return 对象自身 
	 */
	public SaCookie setValue(String value) {
		this.value = value;
		return this;
	}

	/**
	 * @return 有效时长 （单位：秒），-1代表为临时Cookie 浏览器关闭后自动删除 
	 */
	public int getMaxAge() {
		return maxAge;
	}

	/**
	 * @param maxAge 有效时长 （单位：秒），-1代表为临时Cookie 浏览器关闭后自动删除 
	 * @return 对象自身 
	 */
	public SaCookie setMaxAge(int maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	/**
	 * @return 域 
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @param domain 域 
	 * @return 对象自身 
	 */
	public SaCookie setDomain(String domain) {
		this.domain = domain;
		return this;
	}

	/**
	 * @return 路径 
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path 路径 
	 * @return 对象自身 
	 */
	public SaCookie setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * @return 是否只在 https 协议下有效 
	 */
	public Boolean getSecure() {
		return secure;
	}

	/**
	 * @param secure 是否只在 https 协议下有效 
	 * @return 对象自身 
	 */
	public SaCookie setSecure(Boolean secure) {
		this.secure = secure;
		return this;
	}

	/**
	 * @return 是否禁止 js 操作 Cookie 
	 */
	public Boolean getHttpOnly() {
		return httpOnly;
	}

	/**
	 * @param httpOnly 是否禁止 js 操作 Cookie 
	 * @return 对象自身 
	 */
	public SaCookie setHttpOnly(Boolean httpOnly) {
		this.httpOnly = httpOnly;
		return this;
	}

	/**
	 * @return 第三方限制级别（Strict=完全禁止，Lax=部分允许，None=不限制）
	 */
	public String getSameSite() {
		return sameSite;
	}

	/**
	 * @param sameSite 第三方限制级别（Strict=完全禁止，Lax=部分允许，None=不限制）
	 * @return 对象自身 
	 */
	public SaCookie setSameSite(String sameSite) {
		this.sameSite = sameSite;
		return this;
	}


	// toString 
	@Override
	public String toString() {
		return "SaCookie [name=" + name + ", value=" + value + ", maxAge=" + maxAge  + ", domain=" + domain + ", path=" + path 
				+ ", secure=" + secure + ", httpOnly=" + httpOnly + ", sameSite="
				+ sameSite + "]";
	}

	/**
	 * 构建一下 
	 */
	public void builde() {
		if(path == null) {
			path = "/";
		}
	}
	
	/**
	 * 转换为响应头 Set-Cookie 参数需要的值
	 * @return / 
	 */
	public String toHeaderValue() {
		this.builde();
		
		if(SaFoxUtil.isEmpty(name)) {
			throw new SaTokenException("name不能为空");
		}
		if(value != null && value.indexOf(";") > -1) {
			throw new SaTokenException("无效Value：" + value);
		}

		// Set-Cookie: name=value; Max-Age=100000; Expires=Tue, 05-Oct-2021 20:28:17 GMT; Domain=localhost; Path=/; Secure; HttpOnly; SameSite=Lax 
		
		StringBuffer sb = new StringBuffer();
		sb.append(name + "=" + value);
		
		if(maxAge >= 0) {
			 sb.append("; Max-Age=" + maxAge);
			 String expires;
			 if(maxAge == 0) {
				 expires = Instant.EPOCH.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);
			 } else {
				 expires = OffsetDateTime.now().plusSeconds(maxAge).format(DateTimeFormatter.RFC_1123_DATE_TIME);
			 }
			 sb.append("; Expires=" + expires);
		}
		if(SaFoxUtil.isEmpty(domain) == false) {
			sb.append("; Domain=" + domain);
		}
		if(SaFoxUtil.isEmpty(path) == false) {
			sb.append("; Path=" + path);
		}
		if(secure) {
			sb.append("; Secure");
		}
		if(httpOnly) {
			sb.append("; HttpOnly");
		}
		if(SaFoxUtil.isEmpty(sameSite) == false) {
			sb.append("; sameSite=" + sameSite);
		}
		
		return sb.toString();
	}
	
}
