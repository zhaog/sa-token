# 临时Token令牌认证  

---

### 适用场景 

在部分业务场景，我们需要一种临时授权的能力，即：一个token的有效期并不需要像登录有效期那样需要[七天、三十天]，而是仅仅需要 [五分钟、半小时]

举个比较明显的例子：超链接邀请机制 

比如说你在一个游戏中创建一个公会 `(id=10014)`，现在你想邀请你的好朋友加入这个公会，在你点击 **`[邀请]`** 按钮时，系统为你生成一个连接: 

``` xml
http://xxx.com/apply?id=10014
```

接着，你的好朋友点击这个链接，加入了你的工会

那么，系统是如何识别这个链接对应的工会是10014呢？很明显，我们可以观察出，这个链接的尾部有个id参数值为10014，这便是系统识别的关键

此时你可能眉头一紧，就这么简单？那我如果手动更改一下尾部的参数改成10015，然后我再一点，岂不是就可以偷偷加入别人的工会了？

你想的没错，如果这个游戏的架构设计者采用上述方案完成功能的话，这个邀请机制就轻松的被你攻破了

但是很明显，正常的商业项目一般不会拉跨到这种地步，比较常见的方案是，对这个公会id做一个token映射，最终你看到链接一般是这样的：

``` xml
http://xxx.com/apply?token=oEwQBnglXDoGraSJdGaLooPZnGrk
```

后面那一串字母是乱打出来的，目的是为了突出它的随机性，即：使用一个随机的token来代替明文显示真正的数据

在用户点击这个链接之后，服务器便可根据这个token解析出真正公会id (10014) ，至于伪造？全是随机的你怎么伪造？你又不知道10015会随机出一个什么样的Token 

而且为了安全性，这个token的有效期一般不会太长，给你预留五分钟、半小时的时间足够你点击它即可


### 相关API 

**[sa-token-temp临时认证模块]** 已内嵌到核心包，无需引入其它依赖即可使用

``` java
// 根据 value 创建一个 token 
String token = SaTempUtil.createToken("10014", 200);

// 解析 token 获取 value，并转换为指定类型 
String value = SaTempUtil.parseToken(token, String.class);

// 获取指定 token 的剩余有效期，单位：秒 
SaTempUtil.getTimeout(token);

// 删除指定 token
SaTempUtil.deleteToken(token);
```


### 集成jwt
提到 [临时Token认证]，你是不是想到一个专门干这件事的框架？对，就是JWT！

**[sa-token-temp]** 模块允许以JWT作为逻辑内核完成工作，你只需要引入以下依赖，所有上层API保持不变

``` xml
<dependency>
	<groupId>cn.dev33</groupId>
	<artifactId>sa-token-temp-jwt</artifactId>
	<version>${sa.top.version}</version>
</dependency>
```

并在配置文件中配置上jwt秘钥 **`(必填!)`**
``` java
sa-token: 
	# sa-token-temp-jwt 模块的秘钥 （随便乱摁几个字母就行了） 
	jwt-secret-key: JfdDSgfCmPsDfmsAaQwnXk
```
