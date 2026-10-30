/*
HomeController.java
功能：
首页控制器。最简单的控制器，只有一个方法 index() 返回 index.html 页面。
首页会根据Session中的角色动态显示不同的功能卡片。
*/
package com.wxz.food_ordering_web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}