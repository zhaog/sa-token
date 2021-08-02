# 权限认证
--- 


### 核心思想

所谓权限认证，认证的核心就是一个账号是否拥有一个权限码 <br/>
有，就让你通过。没有？那么禁止访问!

再往底了说，就是每个账号都会拥有一个权限码集合，我来校验这个集合中是否包含指定的权限码 <br/>
例如：当前账号拥有权限码集合：`["user-add", "user-delete", "user-get"]`，这时候我来校验权限 `"user-update"`，则其结果就是：**验证失败，禁止访问** <br/>

所以现在问题的核心就是: 
1. 如何获取一个账号所拥有的的权限码集合
2. 本次操作需要验证的权限码是哪个 

### 获取当前账号权限码集合
因为每个项目的需求不同，其权限设计也千变万化，因此【获取当前账号权限码集合】这一操作不可能内置到框架中，
所以 Sa-Token 将此操作以接口的方式暴露给你，以方便的你根据自己的业务逻辑进行重写

你需要做的就是新建一个类，实现`StpInterface`接口，例如以下代码：

``` java 
package com.pj.satoken;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import cn.dev33.satoken.stp.StpInterface;

/**
 * 自定义权限验证接口扩展 
 */
@Component	// 保证此类被SpringBoot扫描，完成Sa-Token的自定义权限验证扩展 
public class StpInterfaceImpl implements StpInterface {

	/**
	 * 返回一个账号所拥有的权限码集合 
	 */
	@Override
	public List<String> getPermissionList(Object loginId, String loginType) {
		// 本list仅做模拟，实际项目中要根据具体业务逻辑来查询权限
		List<String> list = new ArrayList<String>();	
		list.add("101");
		list.add("user-add");
		list.add("user-delete");
		list.add("user-update");
		list.add("user-get");
		list.add("article-get");
		return list;
	}

	/**
	 * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
	 */
	@Override
	public List<String> getRoleList(Object loginId, String loginType) {
		// 本list仅做模拟，实际项目中要根据具体业务逻辑来查询角色
		List<String> list = new ArrayList<String>();	
		list.add("admin");
		list.add("super-admin");
		return list;
	}

}
```

可参考代码：[码云：StpInterfaceImpl.java](https://gitee.com/dromara/sa-token/blob/master/sa-token-demo/sa-token-demo-springboot/src/main/java/com/pj/satoken/StpInterfaceImpl.java)

<!-- todo: 缓存逻辑 -->



### 权限认证
然后就可以用以下api来鉴权了

``` java
// 当前账号是否含有指定权限, 返回true或false 
StpUtil.hasPermission("user-update");		

// 当前账号是否含有指定权限, 如果验证未通过，则抛出异常: NotPermissionException 
StpUtil.checkPermission("user-update");		

// 当前账号是否含有指定权限 [指定多个，必须全部验证通过] 
StpUtil.checkPermissionAnd("user-update", "user-delete");		

// 当前账号是否含有指定权限 [指定多个，只要其一验证通过即可] 
StpUtil.checkPermissionOr("user-update", "user-delete");		
```

扩展：`NotPermissionException` 对象可通过 `getLoginType()` 方法获取具体是哪个 `StpLogic` 抛出的异常


### 角色认证
在Sa-Token中，角色和权限可以独立验证

``` java
// 当前账号是否含有指定角色标识, 返回true或false 
StpUtil.hasRole("super-admin");		

// 当前账号是否含有指定角色标识, 如果验证未通过，则抛出异常: NotRoleException 
StpUtil.checkRole("super-admin");		

// 当前账号是否含有指定角色标识 [指定多个，必须全部验证通过] 
StpUtil.checkRoleAnd("super-admin", "shop-admin");		

// 当前账号是否含有指定角色标识 [指定多个，只要其一验证通过即可] 
StpUtil.checkRoleOr("super-admin", "shop-admin");		
```

扩展：`NotRoleException` 对象可通过 `getLoginType()` 方法获取具体是哪个 `StpLogic` 抛出的异常



### 拦截全局异常
有同学要问，鉴权失败，抛出异常，然后呢？要把异常显示给用户看吗？**当然不可以！** <br>
你可以创建一个全局异常拦截器，统一返回给前端的格式，参考：[码云：GlobalException.java](https://gitee.com/dromara/sa-token/blob/master/sa-token-demo/sa-token-demo-springboot/src/main/java/com/pj/test/GlobalException.java)


### 权限通配符
Sa-Token允许你根据通配符指定泛权限，例如当一个账号拥有`user*`的权限时，`user-add`、`user-delete`、`user-update`都将匹配通过

``` java
// 当拥有 user* 权限时
StpUtil.hasPermission("user-add");        // true
StpUtil.hasPermission("user-update");     // true
StpUtil.hasPermission("art-add");         // false

// 当拥有 *-delete 权限时
StpUtil.hasPermission("user-add");        // false
StpUtil.hasPermission("user-delete");     // true
StpUtil.hasPermission("art-delete");      // true

// 当拥有 *.js 权限时
StpUtil.hasPermission("index.js");        // true
StpUtil.hasPermission("index.css");       // false
StpUtil.hasPermission("index.html");      // false
```

上帝权限：当一个账号拥有 `"*"` 权限时，他可以验证通过任何权限码 (角色认证同理)


### 如何把权限精确搭到按钮级？
权限精确到按钮级的意思就是指：**权限范围可以控制到页面上的每一个按钮是否显示**

思路：如此精确的范围控制只依赖后端已经难以完成，此时需要前端进行一定的逻辑判断

1. 在登录时，把当前账号拥有的所有权限码一次性返回给前端
2. 前端将权限码集合保存在`localStorage`或其它全局状态管理对象中
3. 在需要权限控制的按钮上，使用js进行逻辑判断，例如在`vue`框架中我们可以使用如下写法：
``` js
<button v-if="arr.indexOf('user:delete') > -1">删除按钮</button>
```
其中：`arr`是当前用户拥有的权限码数组，`user:delete`是显示按钮需要拥有的权限码，`删除按钮`是用户拥有权限码才可以看到的内容


注意：以上写法只为提供一个参考示例，不同框架有不同写法，开发者可根据项目技术栈灵活封装进行调用


### 前端有了鉴权后端还需要鉴权吗？
**需要！**

前端的鉴权只是一个辅助功能，对于专业人员这些限制都是可以轻松绕过的，为保证服务器安全，无论前端是否进行了权限校验，后端接口都需要对会话请求再次进行权限校验！


### 将权限数据放在缓存里
前面我们讲解了如何通过`StpInterface`接口注入权限数据，框架默认是不提供缓存能力的，如果你想减小数据库的访问压力，则需要将权限数据放到缓存中

参考示例：
``` java
/**
 * 返回一个账号所拥有的权限码集合 
 */
@Override
public List<String> getPermissionList(Object loginId, String loginType) {
	
	// 1. 获取这个账号所属角色id 
	long roleId = StpUtil.getSessionByLoginId(loginId).get("Role_Id", () -> {
		return ...;	 // 从数据库查询这个账号所属的角色id 
	});
	
	// 2. 获取这个角色id拥有的权限列表  
	SaSession roleSession = SaSessionCustomUtil.getSessionById("role-" + roleId);
	List<String> list = roleSession.get("Permission_List", () -> {
		return ...;  // 从数据库查询这个角色id拥有的权限列表 
	});
	
	// 3. 返回
	return list;
}
```
以上仅为代码示例，角色列表步骤同理 

##### 疑问：为什么不直接缓存 `[账号id->权限列表]`的关系，而是 `[账号id -> 角色id -> 权限列表]`？

<!-- ``` java
// 在一个账号登录时写入其权限数据
RedisUtil.setValue("账号id", <权限列表>);

// 然后在`StpInterface`接口中，如下方式获取
List<String> list = RedisUtil.getValue("账号id");
``` -->

答：`[账号id->权限列表]`的缓存方式虽然更加直接粗暴，却有一个严重的问题：

- 通常我们系统的权限架构是RBAC模型：权限与用户没有直接的关系，而是：用户拥有指定的角色，角色再拥有指定的权限
- 而这种'拥有关系'是动态的，是可以随时修改的，一旦我们修改了它们的对应关系，便要同步修改或清除对应的缓存数据 

现在假设如下业务场景：我们系统中有十万个账号属于同一个角色，当我们变动这个角色的权限时，难道我们要同时清除这十万个账号的缓存信息吗？
这显然是一个不合理的操作，同一时间缓存大量清除容易引起Redis的缓存雪崩

而当我们采用 `[账号id -> 角色id -> 权限列表]` 的缓存模型时，则只需要清除或修改 `[角色id -> 权限列表]` 一条缓存即可 

一言以蔽之：权限的缓存模型需要跟着权限模型走，角色缓存亦然 


