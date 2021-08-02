# 路由拦截鉴权
--- 

假设我们有如下需求：
> 项目中所有接口均需要登录验证，只有'登录接口'本身对外开放

我们怎么实现呢？给每个接口加上鉴权注解？手写全局拦截器？似乎都不是非常方便。<br/>
在这个需求中我们真正需要的是一种基于路由拦截的鉴权模式, 那么在Sa-Token怎么实现路由拦截鉴权呢？



### 1、注册路由拦截器
以`SpringBoot2.0`为例, 新建配置类`SaTokenConfigure.java`
``` java 
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
	// 注册拦截器
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册Sa-Token的路由拦截器，并排除登录接口或其他可匿名访问的接口地址 (与注解拦截器无关)
		registry.addInterceptor(new SaRouteInterceptor()).addPathPatterns("/**").excludePathPatterns("/user/doLogin"); 
	}
}
```
以上代码，我们注册了一个登录验证拦截器，并且排除了`/user/doLogin`接口用来开放登录（除了`/user/doLogin`以外的所有接口都需要登录才能访问） <br>
那么我们如何进行权限认证拦截呢，且往下看


### 2、自定义权限验证规则
你可以使用函数式编程自定义验证规则

``` java 
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册路由拦截器，自定义验证规则 
		registry.addInterceptor(new SaRouteInterceptor((req, res, handler)->{
			// 根据路由划分模块，不同模块不同鉴权 
			SaRouter.match("/user/**", () -> StpUtil.checkPermission("user"));
			SaRouter.match("/admin/**", () -> StpUtil.checkPermission("admin"));
			SaRouter.match("/goods/**", () -> StpUtil.checkPermission("goods"));
			SaRouter.match("/orders/**", () -> StpUtil.checkPermission("orders"));
			SaRouter.match("/notice/**", () -> StpUtil.checkPermission("notice"));
			SaRouter.match("/comment/**", () -> StpUtil.checkPermission("comment"));
		})).addPathPatterns("/**");
	}
}
```

### 3、完整示例
所有用法示例：

``` java 
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
	// 注册Sa-Token的拦截器
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册路由拦截器，自定义验证规则 
		registry.addInterceptor(new SaRouteInterceptor((req, res, handler) -> {
			
			// 登录验证 -- 拦截所有路由，并排除/user/doLogin 用于开放登录 
			SaRouter.match("/**", "/user/doLogin", () -> StpUtil.checkLogin());
			
			// 登录验证 -- 排除多个路径
			SaRouter.match(Arrays.asList("/**"), Arrays.asList("/user/doLogin", "/user/reg"), () -> StpUtil.checkLogin());
						
			// 角色认证 -- 拦截以 admin 开头的路由，必须具备[admin]角色或者[super-admin]角色才可以通过认证 
			SaRouter.match("/admin/**", () -> StpUtil.checkRoleOr("admin", "super-admin"));
			
			// 权限认证 -- 不同模块, 校验不同权限 
			SaRouter.match("/user/**", () -> StpUtil.checkPermission("user"));
			SaRouter.match("/admin/**", () -> StpUtil.checkPermission("admin"));
			SaRouter.match("/goods/**", () -> StpUtil.checkPermission("goods"));
			SaRouter.match("/orders/**", () -> StpUtil.checkPermission("orders"));
			SaRouter.match("/notice/**", () -> StpUtil.checkPermission("notice"));
			SaRouter.match("/comment/**", () -> StpUtil.checkPermission("comment"));
			
			// 匹配 restful 风格路由 
			SaRouter.match("/article/get/{id}", () -> StpUtil.checkPermission("article"));
			
            // 检查请求方式 
			SaRouter.match("/notice/**", () -> {
				if(req.getMethod().equals(HttpMethod.GET.toString())) {
					StpUtil.checkPermission("notice");
				}
			});
			
			// 提前退出 (执行SaRouter.stop()后会直接退出匹配链)
			SaRouter.match("/test/back", () -> SaRouter.stop());
			
			// 在多账号模式下，可以使用任意StpUtil进行校验
			SaRouter.match("/user/**", () -> StpUserUtil.checkLogin());
			
		})).addPathPatterns("/**");
	}
}
```


### 4、提前退出匹配链条 
使用 `SaRouter.stop()` 可以提前退出匹配链，例：

``` java
// 原写法
registry.addInterceptor(SaRouteInterceptor.createPermissionVal("user")).addPathPatterns("/user/**");

// 改为以下方式，效果同上 
registry.addInterceptor(new SaRouteInterceptor((req, res, handler) -> {
			SaRouter.match("/**", () -> System.out.println("进入1"));
			SaRouter.match("/**", () -> {System.out.println("进入2"); SaRouter.stop();});
			SaRouter.match("/**", () -> System.out.println("进入3"));
})).addPathPatterns("/**");
```
如上示例，代码运行至第2条匹配链时，会在stop函数处提前退出整个匹配函数，从而忽略掉剩余的所有match匹配 

除了`stop()`函数，`SaRouter`还提供了 `SaRouter.back()` 函数，用于：停止匹配，结束执行，直接向前端返回结果
``` java
SaRouter.match("/user/back", () -> SaRouter.back("执行back函数后将停止匹配，也不会进入Controller，而是直接将此参数作为返回值输出到前端"));
```






<!-- 
### 注意事项
在`v1.14`及以前版本下，路由拦截器提供了封装式写法，该方法代码比较冗余，在`v1.15`版本已移除，替代方案如下：

``` java
// 原写法
registry.addInterceptor(SaRouteInterceptor.createPermissionVal("user")).addPathPatterns("/user/**");

// 改为以下方式，效果同上 
registry.addInterceptor(new SaRouteInterceptor((request, response, handler) -> {
	SaRouter.match("/user/**", () -> StpUtil.checkPermission("user"));
})).addPathPatterns("/**");
```
-->
		
		