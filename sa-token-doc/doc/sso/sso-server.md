# 搭建统一认证中心 SSO-Server  

在开始SSO三种模式的对接之前，我们必须先搭建一个 SSO-Server 认证中心 

> 搭建示例在官方仓库的 `/sa-token-demo/sa-token-demo-sso-server/`，如遇到难点可结合源码进行测试学习，demo里有制作好的登录页面 

--- 

### 1、添加依赖 
创建 SpringBoot 项目 `sa-token-demo-sso-server`，引入依赖：

``` xml
<!-- Sa-Token 权限认证, 在线文档：http://sa-token.dev33.cn/ -->
<dependency>
	<groupId>cn.dev33</groupId>
	<artifactId>sa-token-spring-boot-starter</artifactId>
	<version>${sa.top.version}</version>
</dependency>

<!-- Sa-Token 整合 Redis (使用 jackson 序列化方式) -->
<dependency>
	<groupId>cn.dev33</groupId>
	<artifactId>sa-token-dao-redis-jackson</artifactId>
	<version>${sa.top.version}</version>
</dependency>
<dependency>
	<groupId>org.apache.commons</groupId>
	<artifactId>commons-pool2</artifactId>
</dependency>

<!-- 视图引擎（在前后端不分离模式下提供视图支持） -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Http请求工具（在模式三的单点注销功能下用到，如不需要可以注释掉） -->
<dependency>
	 <groupId>com.ejlchina</groupId>
	 <artifactId>okhttps</artifactId>
	 <version>3.1.1</version>
</dependency>
```

除了 **sa-token-spring-boot-starter** 以外，其它包都是可选的：
- 在SSO模式三时 Redis 相关包是可选的  
- 在前后端分离模式下可以删除 thymeleaf 相关包
- 在不需要SSO模式三单点注销的情况下可以删除 http 工具包 

建议先完整测试三种模式之后再对pom依赖进行酌情删减。


### 2、开放认证接口  
新建 `SsoServerController`，用于对外开放接口：

``` java
/**
 * Sa-Token-SSO Server端 Controller 
 */
@RestController
public class SsoServerController {

	/*
	 * SSO-Server端：处理所有SSO相关请求 (下面的章节我们会详细列出开放的接口) 
	 */
	@RequestMapping("/sso/*")
	public Object ssoRequest() {
		return SaSsoHandle.serverRequest();
	}
	
	/**
	 * 配置SSO相关参数 
	 */
	@Autowired
	private void configSso(SaTokenConfig cfg) {
		// 配置：未登录时返回的View 
		cfg.sso.setNotLoginView(() -> {
			String msg = "当前会话在SSO-Server端尚未登录，请先访问"
					+ "<a href='/sso/doLogin?name=sa&pwd=123456' target='_blank'> doLogin登录 </a>"
					+ "进行登录之后，刷新页面开始授权";
			return msg;
		});
		
		// 配置：登录处理函数 
		cfg.sso.setDoLoginHandle((name, pwd) -> {
			// 此处仅做模拟登录，真实环境应该查询数据进行登录 
			if("sa".equals(name) && "123456".equals(pwd)) {
				StpUtil.login(10001);
				return SaResult.ok("登录成功！").setData(StpUtil.getTokenValue());
			}
			return SaResult.error("登录失败！");
		});
		
		// 配置 Http 请求处理器 （在模式三的单点注销功能下用到，如不需要可以注释掉） 
		cfg.sso.setSendHttp(url -> {
			return OkHttps.sync(url).get().getBody().toString();
		});
	}
	
}
```

注：在`setDoLoginHandle`函数里如果要获取name, pwd以外的参数，可通过`SaHolder.getRequest().getParam("xxx")`来获取 

全局异常处理：
``` java
@RestControllerAdvice
public class GlobalExceptionHandler {
	// 全局异常拦截 
	@ExceptionHandler
	public SaResult handlerException(Exception e) {
		e.printStackTrace(); 
		return SaResult.error(e.getMessage());
	}
}
```


### 3、application.yml配置
``` yml
# 端口
server:
    port: 9000

# Sa-Token 配置
sa-token: 
    # -------------- SSO-模式一相关配置  (非模式一不需要配置) 
    # cookie: 
        # 配置Cookie作用域 
        # domain: stp.com 
        
    # ------- SSO-模式二相关配置 
    sso: 
        # Ticket有效期 (单位: 秒)，默认五分钟 
        ticket-timeout: 300
        # 所有允许的授权回调地址
        allow-url: "*"
        # 是否打开单点注销功能
        is-slo: true
        
        # ------- SSO-模式三相关配置 （下面的配置在SSO模式三并且 is-slo=true 时打开） -------
        # 是否打开模式三 
        isHttp: true
        # 接口调用秘钥（用于SSO模式三的单点注销功能）
        secretkey: kQwIOrYvnXmSDkwEiFngrKidMcdrgKor
        # ---- 除了以上配置项，你还需要为 Sa-Token 配置http请求处理器（文档有步骤说明） 
        
spring: 
    # Redis配置 （SSO模式一和模式二使用Redis来同步会话）
    redis:
        # Redis数据库索引（默认为0）
        database: 1
        # Redis服务器地址
        host: 127.0.0.1
        # Redis服务器连接端口
        port: 6379
        # Redis服务器连接密码（默认为空）
        password: 
```

注意点：`allow-url`为了方便测试配置为`*`，线上生产环境一定要配置为详细URL地址 （之后的章节我们会详细阐述此配置项）


### 4、创建启动类
``` java 
@SpringBootApplication
public class SaSsoServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(SaSsoServerApplication.class, args);
		System.out.println("\n------ Sa-Token-SSO 认证中心启动成功");
	}
}
```

启动项目，不出意外的情况下我们将看到如下输出：

![sso-server-start](https://oss.dev33.cn/sa-token/doc/sso/sso-server-start.png 's-w-sh')

访问统一授权地址：
- [http://localhost:9000/sso/auth](http://localhost:9000/sso/auth)

![sso-server-init-login.png](https://oss.dev33.cn/sa-token/doc/sso/sso-server-init-login.png 's-w-sh')

可以看到这个页面目前非常简陋，这是因为我们以上的代码示例，主要目标是为了带大家从零搭建一个可用的SSO认证服务端，所以就对一些不太必要的步骤做了简化。

大家可以下载运行一下官方仓库里的示例`/sa-token-demo/sa-token-demo-sso-server/`，里面有制作好的登录页面：

![sso-server-init-login2.png](https://oss.dev33.cn/sa-token/doc/sso/sso-server-init-login2.png 's-w-sh')

默认账号密码为：`sa / 123456`，先别着急点击登录，因为我们还没有搭建对应的 Client 端项目，
真实项目中我们是不会直接从浏览器访问 `/sso/auth` 授权地址的，我们需要在 Client 端点击登录按钮重定向而来。

现在我们先来看看除了 `/sso/auth` 统一授权地址，这个 SSO-Server 认证中心还开放了哪些API。

### 5、API 列表

如果你仅仅使用 Sa-Token 搭建 SSO-Server 端，而 Client 端使用其它框架的话，那么下面的 API 列表将给你的对接步骤做一份参考。

如果你在 Client 端也用到了 Sa-Token 框架，那么你可以选择跳过本小节，Sa-Token 对 Client 端也提供了相应的封装，你可以直接开始学习：[SSO模式一 共享Cookie同步会话](/sso/sso-type1)


#### 5.1、单点登录授权地址
``` url
http://{host}:{port}/sso/auth
```

接收参数：

| 参数			| 是否必填	| 说明																|
| :--------		| :--------	| :--------															|
| redirect		| 是		| 登录成功后的重定向地址，一般填写 location.href（从哪来回哪去）							|
| mode			| 否		| 授权模式，取值 [simple, ticket]，simple=登录后直接重定向，ticket=带着ticket参数重定向，默认值为ticket			|

访问接口后有两种情况：
- 情况一：当前会话在 SSO 认证中心未登录，会进入登录页开始登录。
- 情况二：当前会话在 SSO 认证中心已登录，会被重定向至 `redirect` 地址，并携带 `ticket` 参数。


#### 5.2、RestAPI 登录接口
``` url
http://{host}:{port}/sso/doLogin
```

接收参数：

| 参数			| 是否必填	| 说明													|
| :--------		| :--------	| :--------												|
| name			| 是		| 用户名  |
| pwd			| 是		| 密码	 |

此接口属于 RestAPI (使用ajax访问)，会进入后端配置的 `setDoLoginHandle` 函数中，另外需要注意：
此接口并非只能携带 name、pwd 参数，因为你可以在 setDoLoginHandle 函数里通过 `SaHolder.getRequest().getParam("xxx")` 来获取其它参数。 


#### 5.3、Ticket 校验接口
此接口仅配置模式三 `(isHttp=true)` 时打开 

``` url
http://{host}:{port}/sso/checkTicket
```

接收参数：

| 参数			| 是否必填	| 说明													|
| :--------		| :--------	| :--------												|
| ticket		| 是		| 在步骤 5.1 中授权重定向时的 ticket 参数 						|
| ssoLogoutCall	| 否		| 单点注销时的回调通知地址，只在SSO模式三单点注销时需要携带此参数|

返回值场景：
- 返回空，代表校验失败。
- 返回具体的 loginId，例如10001，代表校验成功，值为此 ticket 码代表的用户id。


#### 5.4、单点注销接口
``` url
http://{host}:{port}/sso/logout         
```

接受参数：

| 参数			| 是否必填	| 说明													|
| :--------		| :--------	| :--------												|
| loginId		| 否		| 要注销的账号id			 						|
| secretkey		| 否		| 接口通信秘钥									|
| back			| 否		| 注销成功后的重定向地址							|


此接口有两种调用方式

##### 方式一：在 Client 的前端页面引导用户直接跳转，并带有 back 参数 
例如：`http://{host}:{port}/sso/logout?back=xxx`，代表用户注销成功后返回back地址 

##### 方式二：在 Client 的后端通过 http 工具来调用
例如：`http://{host}:{port}/sso/logout?loginId={value}&secretkey={value}`，代表注销 账号=loginId 的账号，返回json数据结果，形如：

``` js
{
    "code": 200,    // 200表示请求成功，非200标识请求失败
    "msg": "单点注销成功",
    "data": null
}
```

<br>

SSO 认证中心只有这四个接口，接下来让我一起来看一下 Client 端的对接流程：[SSO模式一 共享Cookie同步会话](/sso/sso-type1) 











