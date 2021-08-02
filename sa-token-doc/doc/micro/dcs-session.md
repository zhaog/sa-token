# 微服务 - 分布式Session会话

--- 

### 需求场景 

微服务架构下的第一个难题便是数据同步，单机版的`Session`在分布式环境下一般不能正常工作，为此我们需要对框架做一些特定的处理。

首先我们要明白，分布式环境下为什么`Session`会失效？因为用户在一个节点对会话做出的更改无法实时同步到其它的节点，
这就导致一个很严重的问题：如果用户在节点一上已经登录成功，那么当下一次的请求落在节点二上时，对节点二来讲，此用户仍然是未登录状态。

### 解决方案 

要怎么解决这个问题呢？目前的主流方案有四种：
1. **Session同步**：只要一个节点的数据发生了改变，就强制同步到其它所有节点 
2. **Session粘滞**：通过一定的算法，保证一个用户的所有请求都稳定的落在一个节点之上，对这个用户来讲，就好像还是在访问一个单机版的服务
3. **建立会话中心**：将Session存储在专业的缓存中间件上，使每个节点都变成了无状态服务，例如：`Redis`
4. **颁发无状态token**：放弃Session机制，将用户数据直接写入到令牌本身上，使会话数据做到令牌自解释，例如：`jwt`


### 方案选择

该如何选择一个合适的方案？
- 方案一：性能消耗太大，不太考虑
- 方案二：需要从网关处动手，与框架无关
- 方案三：Sa-Token 整合`Redis`非常简单，详见章节：[集成 Redis](/up/integ-redis)
- 方案四：详见官方仓库中 Sa-Token 整合`jwt`的示例

由于`jwt`模式不在服务端存储数据，对于比较复杂的业务可能会功能受限，因此更加推荐使用方案三

``` xml
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
详细参考：[集成 Redis](/up/integ-redis)









