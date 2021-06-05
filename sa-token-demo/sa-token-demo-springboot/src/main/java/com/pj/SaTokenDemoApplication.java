package com.pj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import cn.dev33.satoken.SaManager;

/**
 * Sa-Token整合SpringBoot 示例 
 * @author kong
 *
 */
@SpringBootApplication
public class SaTokenDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaTokenDemoApplication.class, args);
		System.out.println("\n启动成功：sa-token配置如下：" + SaManager.getConfig());
	}
	
}