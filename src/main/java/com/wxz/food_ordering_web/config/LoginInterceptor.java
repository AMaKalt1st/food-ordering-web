/*
LoginInterceptor.java
功能：
登录权限拦截器。
实现 HandlerInterceptor 接口，重写 preHandle() 方法。
核心功能：在每次请求前检查Session中是否存在 role 属性，如果没有则重定向到登录页面；如果存在则根据角色校验URL访问权限。
关键技术点：merchant 角色只能访问菜品相关URL，user 角色不能访问管理类URL，admin 角色可以访问所有URL。对登录页面和静态资源路径进行放行。
*/
package com.wxz.food_ordering_web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession();
        String role = (String) session.getAttribute("role");
        String uri = request.getRequestURI();

        // 登录页面和静态资源不需要拦截
        if (uri.equals("/login") || uri.startsWith("/css") || uri.startsWith("/js")) {
            return true;
        }

        // 未登录则跳转到登录页
        if (role == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 商家只能访问菜品和首页
        if ("merchant".equals(role)) {
            if (uri.startsWith("/merchants") || uri.startsWith("/users") ||
                    uri.startsWith("/audit-logs") || uri.startsWith("/orders")) {
                response.sendRedirect("/");
                return false;
            }
            return true;
        }

        // 普通用户可以访问菜品、订单、下单
        if ("user".equals(role)) {
            if (uri.startsWith("/merchants") || uri.startsWith("/users") ||
                    uri.startsWith("/audit-logs") || uri.startsWith("/addresses")) {
                response.sendRedirect("/");
                return false;
            }
            return true;
        }

        // 管理员可以访问所有
        return true;
    }
}