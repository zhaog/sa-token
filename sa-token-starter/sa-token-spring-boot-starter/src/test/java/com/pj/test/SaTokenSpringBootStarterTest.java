package com.pj.test;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import cn.dev33.satoken.exception.DisableLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.temp.SaTempUtil;
import cn.dev33.satoken.util.SaTokenConsts;

/**
 * Sa-Token 单元测试 
 * 
 * <p> 注解详解参考： https://www.cnblogs.com/flypig666/p/11505277.html
 * @author Auster 
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartUpApplication.class)
public class SaTokenSpringBootStarterTest {

	// 开始 
	@BeforeClass
    public static void beforeClass() {
    	System.out.println("\n\n------------------------ Test star ...");
    }

	// 结束 
    @AfterClass
    public static void afterClass() {
    	System.out.println("\n\n------------------------ Test end ... \n");
    }

    // 测试：登录 & 注销 
    @Test
    public void testDoLogin() {
    	// 登录
    	StpUtil.login(10001);
    	Assert.assertTrue(StpUtil.isLogin());	
    	Assert.assertNotNull(StpUtil.getTokenValue());	// token不为null
    	Assert.assertEquals(StpUtil.getLoginIdAsLong(), 10001);	// loginId=10001 
    	Assert.assertEquals(StpUtil.getLoginDevice(), SaTokenConsts.DEFAULT_LOGIN_DEVICE);	// 登录设备 
    	
    	// 注销
    	StpUtil.logout();
    	Assert.assertFalse(StpUtil.isLogin());
    	// Assert.assertNull(StpUtil.getTokenValue());
    	Assert.assertNull(StpUtil.getLoginIdDefaultNull()); 
    }

    // 测试：权限认证 
    @Test
    public void testCheckPermission() {
    	StpUtil.login(10001);
    	
    	// 权限认证 
    	Assert.assertTrue(StpUtil.hasPermission("user-add"));
    	Assert.assertTrue(StpUtil.hasPermission("user-list"));
    	Assert.assertTrue(StpUtil.hasPermission("user"));
    	Assert.assertTrue(StpUtil.hasPermission("art-add"));
    	Assert.assertFalse(StpUtil.hasPermission("get-user"));
    }

    // 测试：角色认证
    @Test
    public void testCheckRole() {
    	StpUtil.login(10001);
    	
    	// 角色认证 
    	Assert.assertTrue(StpUtil.hasRole("admin")); 
    	Assert.assertFalse(StpUtil.hasRole("teacher")); 
    }
	
    // 测试：踢人下线 
    @Test
    public void testKickOut() {
    	
    	// 根据token踢人 
    	StpUtil.login(10001); 
    	Assert.assertTrue(StpUtil.isLogin());	
    	StpUtil.logoutByTokenValue(StpUtil.getTokenValue()); 
    	Assert.assertFalse(StpUtil.isLogin()); 

    	// 根据账号id踢人
    	StpUtil.login(10001); 
    	Assert.assertTrue(StpUtil.isLogin());	
    	StpUtil.logoutByLoginId(10001);
    	Assert.assertFalse(StpUtil.isLogin()); 
    }

    // 测试：账号封禁 
    @Test(expected = DisableLoginException.class)
    public void testDisable() {
    	
    	// 封号 
    	StpUtil.disable(10007, 200);
    	Assert.assertTrue(StpUtil.isDisable(10007));
    	
    	// 解封  
    	StpUtil.untieDisable(10007);
    	Assert.assertFalse(StpUtil.isDisable(10007));
    	
    	// 封号后登陆 (会抛出 DisableLoginException 异常)
    	StpUtil.disable(10007, 200); 
    	StpUtil.login(10007);  
    }

    // 测试：Session会话 
    @Test
    public void testSession() {
    	StpUtil.login(10001);
    	
    	// Session 应该存在 
    	Assert.assertNotNull(StpUtil.getSession(false));
    	
    	// 存取值 
    	SaSession session = StpUtil.getSession();
    	session.set("name", "zhang");
    	session.set("age", "18");
    	Assert.assertEquals(session.get("name"), "zhang");
    	Assert.assertEquals(session.getInt("age"), 18);
    	Assert.assertEquals((int)session.getModel("age", int.class), 18);
    	Assert.assertEquals((int)session.get("age", 20), 18);
    	Assert.assertEquals((int)session.get("name2", 20), 20);
    	Assert.assertEquals((int)session.get("name2", () -> 30), 30);
    	
    }
    
    // 测试：身份切换 
    @Test
    public void testSwitch() {
    	// 登录
    	StpUtil.login(10001);
    	Assert.assertFalse(StpUtil.isSwitch());
    	Assert.assertEquals(StpUtil.getLoginIdAsLong(), 10001);
    	
    	// 开始身份切换 
    	StpUtil.switchTo(10044);
    	Assert.assertTrue(StpUtil.isSwitch());
    	Assert.assertEquals(StpUtil.getLoginIdAsLong(), 10044);
    	
    	// 结束切换 
    	StpUtil.endSwitch(); 
    	Assert.assertFalse(StpUtil.isSwitch());
    	Assert.assertEquals(StpUtil.getLoginIdAsLong(), 10001);
    }
    
    // 测试：会话管理
    @Test
    public void testSearchTokenValue() {
    	// 登录
    	StpUtil.login(10001);
    	StpUtil.login(10002);
    	StpUtil.login(10003);
    	StpUtil.login(10004);
    	StpUtil.login(10005);
    	
    	// 查询 
    	List<String> list = StpUtil.searchTokenValue("", 0, 10);
    	Assert.assertTrue(list.size() >= 5);
    }
    
    // 测试：临时验证模块
    @Test
    public void testSaTemp() {
    	// 生成token 
    	String token = SaTempUtil.createToken("group-1014", 200);
    	Assert.assertNotNull(token);
    	
    	// 解析token  
    	String value = SaTempUtil.parseToken(token, String.class);
    	Assert.assertEquals(value, "group-1014"); 
    	
    	// 过期时间 
    	long timeout = SaTempUtil.getTimeout(token);
    	Assert.assertTrue(timeout > 195);
    }

    // 测试：二级认证
    @Test
    public void testSafe() throws InterruptedException {
    	// 登录 
    	StpUtil.login(10001);
    	Assert.assertFalse(StpUtil.isSafe());
    	
    	// 开启二级认证 
    	StpUtil.openSafe(2);
    	Assert.assertTrue(StpUtil.isSafe()); 
    	Assert.assertTrue(StpUtil.getSafeTime() > 0); 
    	
    	// 自然结束 
    	Thread.sleep(2500);
    	Assert.assertFalse(StpUtil.isSafe());
    	
    	// 手动结束
    	StpUtil.openSafe(2);
    	StpUtil.closeSafe();
    	Assert.assertFalse(StpUtil.isSafe());
    }
    
}
