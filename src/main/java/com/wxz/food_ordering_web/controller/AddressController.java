/*
AddressController.java
功能：
地址管理控制器。
包含5个方法：
list() 获取所有地址列表（关联用户名称），
newForm() 跳转新增地址页面，
save() 保存新增地址，
editForm() 跳转编辑地址页面，
update() 保存编辑后的地址，
delete() 删除地址。
使用 JdbcTemplate 进行数据库操作，通过SQL关联查询获取用户名称。
*/
package com.wxz.food_ordering_web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/addresses")
public class AddressController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 地址列表
    @GetMapping
    public String list(Model model) {
        List<Map<String, Object>> addresses = jdbcTemplate.queryForList(
                "SELECT a.address_id, a.user_id, u.nickname AS user_name, " +
                        "a.receiver_name, a.receiver_phone, a.region, a.detail, a.is_default " +
                        "FROM address a JOIN user u ON a.user_id = u.user_id " +
                        "ORDER BY a.address_id DESC");
        model.addAttribute("addresses", addresses);
        return "address_list";
    }

    // 新增页面
    @GetMapping("/new")
    public String newForm(Model model) {
        // 获取用户列表供选择
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT user_id, nickname FROM user ORDER BY user_id");
        model.addAttribute("users", users);
        return "address_form";
    }

    // 保存新增
    @PostMapping
    public String save(@RequestParam Long userId,
                       @RequestParam String receiverName,
                       @RequestParam String receiverPhone,
                       @RequestParam String region,
                       @RequestParam String detail,
                       @RequestParam(defaultValue = "0") int isDefault) {
        jdbcTemplate.update(
                "INSERT INTO address (user_id, receiver_name, receiver_phone, region, detail, is_default) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                userId, receiverName, receiverPhone, region, detail, isDefault);
        return "redirect:/addresses";
    }

    // 编辑页面
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        List<Map<String, Object>> address = jdbcTemplate.queryForList(
                "SELECT * FROM address WHERE address_id = ?", id);
        if (!address.isEmpty()) {
            model.addAttribute("address", address.get(0));
        }
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT user_id, nickname FROM user ORDER BY user_id");
        model.addAttribute("users", users);
        return "address_form";
    }

    // 保存编辑
    @PostMapping("/update")
    public String update(@RequestParam Long addressId,
                         @RequestParam Long userId,
                         @RequestParam String receiverName,
                         @RequestParam String receiverPhone,
                         @RequestParam String region,
                         @RequestParam String detail,
                         @RequestParam(defaultValue = "0") int isDefault) {
        jdbcTemplate.update(
                "UPDATE address SET user_id=?, receiver_name=?, receiver_phone=?, " +
                        "region=?, detail=?, is_default=? WHERE address_id=?",
                userId, receiverName, receiverPhone, region, detail, isDefault, addressId);
        return "redirect:/addresses";
    }

    // 删除
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM address WHERE address_id = ?", id);
        return "redirect:/addresses";
    }
}
