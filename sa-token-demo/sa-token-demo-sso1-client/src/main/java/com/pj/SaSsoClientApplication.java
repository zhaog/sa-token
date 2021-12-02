package com.pj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSO模式一，Client端 Demo 
 * @author kong
 *
 */
@SpringBootApplication
public class SaSsoClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaSsoClientApplication.class, args);
		System.out.println("\nSa-Token SSO模式一 Client端启动成功");
	}
	
}