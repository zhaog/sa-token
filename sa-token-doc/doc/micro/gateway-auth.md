# 微服务 - 网关统一鉴权

微服务架构下的鉴权一般分为两种：
1. 每个服务各自鉴权
2. 网关统一鉴权 

方案一和传统单体鉴权差别不大，不再过多赘述，本篇介绍方案二的整合步骤：

--- 



### 1、引入依赖 

首先，根据 [依赖引入说明](/micro/import-intro) 引入正确的依赖，以`[SpringCloud Gateway]`为例：

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
注：Redis包是必须的，因为我们需要和各个服务通过Redis来同步数据 

### 2、实现鉴权接口
``` java
/**
 * 自定义权限验证接口扩展 
 */
@Component   
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的权限列表 
        return ...;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的角色列表
        return ...;
    }

}

```
关于数据的获取，建议以下方案三选一：
1. 在网关处集成ORM框架，直接从数据库查询数据
2. 先从Redis中获取数据，获取不到时走ORM框架查询数据库 
3. 先从Redis中获取缓存数据，获取不到时走RPC调用子服务 (专门的权限数据提供服务) 获取


### 3、注册全局过滤器 
然后在网关处注册全局过滤器进行鉴权操作 

``` java
/**
 * [Sa-Token 权限认证] 配置类 
 * @author kong
 */
@Configuration
public class SaTokenConfigure {
	// 注册 Sa-Token全局过滤器 
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
			// 拦截地址 
			.addInclude("/**")
			// 开放地址 
			.addExclude("/favicon.ico")
			// 鉴权方法：每次访问进入 
			.setAuth(r -> {
				// 登录验证 -- 拦截所有路由，并排除/user/doLogin 用于开放登录 
				SaRouter.match("/**", "/user/doLogin", () -> StpUtil.checkLogin());
				
				// 权限认证 -- 不同模块, 校验不同权限 
				SaRouter.match("/user/**", () -> StpUtil.checkPermission("user"));
				SaRouter.match("/admin/**", () -> StpUtil.checkPermission("admin"));
				SaRouter.match("/goods/**", () -> StpUtil.checkPermission("goods"));
				SaRouter.match("/orders/**", () -> StpUtil.checkPermission("orders"));
				
				// ... 
			})
			// 异常处理方法：每次setAuth函数出现异常时进入 
			.setError(e -> {
				return SaResult.error(e.getMessage());
			})
			;
    }
}
```

详细操作参考：[路由拦截鉴权](/use/route-check)






