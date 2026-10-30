/*
UserController.java
功能：
用户管理控制器。
包含5个方法：
list() 获取所有用户列表，
newForm() 跳转新增用户页面，
save() 保存新增或编辑的用户，
editForm() 跳转编辑用户页面，
delete() 删除用户。
使用 UserRepository 进行数据库操作，表单中需要传递 role 字段。
*/
package com.wxz.food_ordering_web.controller;

import com.wxz.food_ordering_web.entity.User;
import com.wxz.food_ordering_web.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 用户列表
    @GetMapping
    public String list(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "user_list";
    }

    // 新增页面
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("user", new User());
        return "user_form";
    }

    // 保存新增
    @PostMapping
    public String save(@ModelAttribute User user) {
        userRepository.save(user);
        return "redirect:/users";
    }

    // 编辑页面
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        model.addAttribute("user", user);
        return "user_form";
    }

    // 删除
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/users";
    }
}