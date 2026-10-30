/*
FoodOrderingWebApplication.java
功能：
Spring Boot启动类。
包含 main() 方法，使用 @SpringBootApplication 注解标记为Spring Boot应用入口。
功能：
启动内嵌Tomcat服务器、自动配置Spring容器、扫描组件（Controller、Repository、Entity等）、加载配置文件。
执行 SpringApplication.run(FoodOrderingWebApplication.class, args) 完成应用启动。
 */
package com.wxz.food_ordering_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FoodOrderingWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodOrderingWebApplication.class, args);
	}

}
