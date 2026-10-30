/*
WebConfig.java
功能：
Web配置类。
实现 WebMvcConfigurer 接口，重写 addInterceptors() 方法。
功能：将 LoginInterceptor 注册到拦截器链中，设置拦截路径为 /**（所有路径），排除路径为 /login、/css/**、/js/**（登录页和静态资源）。
 */

package com.wxz.food_ordering_web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/css/**", "/js/**");
    }
}
