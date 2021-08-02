# 微服务 - 内部服务外网隔离 

--- 


### 一、需求场景  

我们的子服务一般不能通过外网直接访问，必须通过网关转发才是一个合法的请求，这种子服务与外网的隔离一般分为两种：

1. 物理隔离：子服务部署在指定的内网环境中，只有网关对外网开放 
2. 逻辑隔离：子服务与网关同时暴露在外网，但是子服务会有一个权限拦截层保证只接受网关发送来的请求，绕过网关直接访问子服务会被提示：无效请求 

这种鉴权需求牵扯到两个环节：**`网关转发鉴权`**、**`服务内部调用鉴权`**

Sa-Token提供两种解决方案：
1. 使用 OAuth2.0 模式的凭证式，将 Client-Token 用作各个服务的身份凭证进行权限校验
2. 使用 Id-Token 模块提供的身份校验能力，完成服务间的权限认证

本篇主要讲解方案二 `Id-Token` 模块的整合步骤，其鉴权流程与 OAuth2.0 类似，不过使用方式上更加简洁（希望使用方案一的同学可参考Sa-OAuth2模块，此处不再赘述）

### 二、网关转发鉴权 

##### 1、引入依赖

在网关处引入的依赖为（此处以 SpringCloud Gateway 为例）：
``` xml
<!-- Sa-Token 权限认证（Reactor响应式集成）, 在线文档：http://sa-token.dev33.cn/ -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-reactor-spring-boot-starter</artifactId>
    <version>${sa.top.version}</version>
</dependency>
<!-- Sa-Token 整合 Redis (使用jackson序列化方式) -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>${sa.top.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

在子服务引入的依赖为：
``` xml
<!-- Sa-Token 权限认证, 在线文档：http://sa-token.dev33.cn/ -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>${sa.top.version}</version>
</dependency>
<!-- Sa-Token 整合 Redis (使用jackson序列化方式) -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>${sa.top.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

##### 2、网关处添加Id-Token

为网关添加全局过滤器：
``` java
/**
 * 全局过滤器，为请求添加 Id-Token 
 */
@Component
public class ForwardAuthFilter implements GlobalFilter {
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest newRequest = exchange
				.getRequest()
				.mutate()
				// 为请求追加 Id-Token 参数 
				.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken())
				.build();
        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
	}
}
```
此过滤器会为 Request 请求头追加 `Id-Token` 参数，这个参数会被转发到子服务 


##### 3、在子服务里校验参数 

在子服务添加过滤器校验参数 
``` java
/**
 * Sa-Token 权限认证 配置类 
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
	// 注册 Sa-Token 全局过滤器 
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
        		.addInclude("/**")
        		.addExclude("/favicon.ico")
        		.setAuth(r -> {
        			// 校验 Id-Token 身份凭证 	—— 以下两句代码可简化为：SaIdUtil.checkCurrentRequestToken(); 
        			String token = SaHolder.getRequest().getHeader(SaIdUtil.ID_TOKEN);
        			SaIdUtil.checkToken(token);
        		})
        		.setError(e -> {
        			return SaResult.error(e.getMessage());
        		})
        		;
    }
}
```

启动网关与子服务，访问测试：

> 如果通过网关转发，可以正常访问，直接访问子服务会提示：`无效Id-Token：xxx`


### 三、服务内部调用鉴权 

有时候我们需要在一个服务调用另一个服务的接口，这也是需要添加`Id-Token`作为身份凭证的

在服务里添加 Id-Token 流程与网关类似，我们以RPC框架 `Feign` 为例：

##### 1、首先在调用方添加 FeignInterceptor
``` java
/**
 * feign拦截器, 在feign请求发出之前，加入一些操作 
 */
@Component
public class FeignInterceptor implements RequestInterceptor {
	// 为 Feign 的 RCP调用 添加请求头Id-Token 
	@Override
	public void apply(RequestTemplate requestTemplate) {
		requestTemplate.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
	}
}
```

##### 2、在调用接口里使用此 Interceptor 
``` java
/**
 * 服务调用 
 */
@FeignClient(
		name = "sp-home", 				// 服务名称 
		configuration = FeignInterceptor.class,		// 请求拦截器 （关键代码）
		fallbackFactory = SpCfgInterfaceFallback.class	// 服务降级处理 
		)	
public interface SpCfgInterface {

	// 获取server端指定配置信息 
	@RequestMapping("/SpConfig/getConfig")
	public String getConfig(@RequestParam("key")String key);
	
}
```

被调用方的代码无需更改（按照网关转发鉴权处的代码注册全局过滤器），保持启动测试即可 


### 四、Id-Token 模块详解 

Id-Token —— 专门解决身份凭证问题的一个模块，它的作用不仅局限于微服务调用场景

基本使用流程为：服务调用方获取Token，提交到请求中，被调用方取出Token进行校验：Token一致则校验通过，否则拒绝服务

首先我们预览一下此模块的相关API：
``` java
// 获取当前Id-Token
SaIdUtil.getToken();

// 判断一个Id-Token是否有效
SaIdUtil.isValid(token);

// 校验一个Id-Token是否有效 (如果无效则抛出异常)
SaIdUtil.checkToken(token);

// 校验当前Request提供的Id-Token是否有效 (如果无效则抛出异常)
SaIdUtil.checkCurrentRequestToken();

// 刷新一次Id-Token (注意集群环境中不要多个服务重复调用) 
SaIdUtil.refreshToken();

// 在 Request 上储存 Id-Token 时建议使用的key
SaIdUtil.ID_TOKEN;
```

##### 1、疑问：这个Token保存在什么地方？有没有泄露的风险？Token为永久有效还是临时有效？
Id-Token 默认随 Sa-Token 数据一起保存在Redis中，理论上不会存在泄露的风险，每个Token默认有效期只有一天

##### 2、如何主动刷新Id-Token，例如：五分钟、两小时刷新一次？
Id-Token 刷新间隔越短，其安全性越高，每个Token的默认有效期为一天，在一天后再次获取会自动产生一个新的Token

!> 需要注意的一点是：Id-Token默认的自刷新机制，并不能做到高并发可用，多个服务一起触发Token刷新可能会造成毫秒级的短暂服务失效，其只能适用于 项目开发阶段 或 低并发业务场景 

因此在微服务架构下，我们需要有专门的机制主动刷新Id-Token，保证其高可用

例如，我们可以专门起一个服务，使用定时任务来刷新Id-Token 
``` java
/**
 * Id-Token，定时刷新
 */
@Configuration
public class SaIdTokenRefreshTask {
	// 从 0 分钟开始 每隔 5 分钟执行一次 Id-Token  
	@Scheduled(cron = "0 0/5 * * * ? ")
	public void refreshToken(){
		SaIdUtil.refreshToken();
	}
}
```

以上的cron表达式刷新间隔可以配置为`五分钟`、`十分钟` 或 `两小时`，只要低于Id-Token的有效期（默认为一天）即可。

##### 3、如果网关携带token转发的请求在落到子服务的节点上时，恰好刷新了token，导致鉴权未通过怎么办？
Id-Token 模块在设计时，充分考虑到了这一点，在每次刷新 Token 时，旧 Token 会被作为次级 Token 存储起来，
只要网关携带的 Token 符合新旧 Token 其一即可通过认证，直至下一次刷新，新 Token 再次作为次级 Token 将此替换掉 






