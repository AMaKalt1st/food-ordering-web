/*
LoginController.java
功能：
登录/退出控制器。负责用户登录验证、角色识别、Session管理。
包含三个方法：loginPage() 显示登录页面，login() 处理登录请求（根据角色分别查询user表或merchant表进行验证），logout() 清除Session并退出登录。
关键技术点：
通过 @RequestParam 接收表单提交的用户名、密码、角色三个参数，验证成功后将 userId、username、role 存入Session。
*/
package com.wxz.food_ordering_web.controller;

import com.wxz.food_ordering_web.entity.User;
import com.wxz.food_ordering_web.entity.Merchant;
import com.wxz.food_ordering_web.repo.UserRepository;
import com.wxz.food_ordering_web.repo.MerchantRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    // 登录页面
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 处理登录
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String role,
                        HttpSession session,
                        Model model) {

        if ("user".equals(role) || "admin".equals(role)) {
            // 用户登录
            User user = userRepository.findByNickname(username);
            if (user != null && "123456".equals(password)) {
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("username", user.getNickname());
                session.setAttribute("role", user.getRole());
                return "redirect:/";
            }
        } else if ("merchant".equals(role)) {
            // 商家登录
            Merchant merchant = merchantRepository.findByUsername(username);
            if (merchant != null && "123456".equals(password)) {
                session.setAttribute("merchantId", merchant.getMerchantId());
                session.setAttribute("username", merchant.getName());
                session.setAttribute("role", "merchant");
                return "redirect:/";
            }
        }

        model.addAttribute("error", "用户名或密码错误");
        return "login";
    }

    // 退出登录
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
