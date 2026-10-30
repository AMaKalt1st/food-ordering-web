/*
OrderController.java
功能：
订单管理控制器。
包含5个方法：
list() 获取订单列表（支持正序/倒序排序），
detail() 调用存储过程 sp_get_order_detail 获取订单完整信息，
updateStatus() 调用存储过程 sp_update_order_status 更新订单状态，
newForm() 跳转新增下单页面，createOrder() 调用存储过程 sp_create_order_single_item 创建订单。
关键技术点：
使用 JdbcTemplate 调用存储过程、使用 @RequestParam(defaultValue = "desc") 实现排序参数接收。
*/
package com.wxz.food_ordering_web.controller;

import com.wxz.food_ordering_web.entity.Order;
import com.wxz.food_ordering_web.repo.OrderRepository;
import com.wxz.food_ordering_web.repo.UserRepository;
import com.wxz.food_ordering_web.repo.MerchantRepository;
import com.wxz.food_ordering_web.repo.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private DishRepository dishRepository;

    // 订单列表
    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "desc") String sort) {
        Sort sorting;
        if ("asc".equals(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "orderId");
        } else {
            sorting = Sort.by(Sort.Direction.DESC, "orderId");
        }
        List<Order> orders = orderRepository.findAll(sorting);
        model.addAttribute("orders", orders);
        model.addAttribute("currentSort", sort);
        return "order_list";
    }

    // 订单详情
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        // 调用存储过程获取订单主信息
        List<Map<String, Object>> orderInfo = jdbcTemplate.queryForList(
                "CALL sp_get_order_detail(?)", id);

        if (!orderInfo.isEmpty()) {
            model.addAttribute("orderInfo", orderInfo.get(0));
        }

        // 获取订单明细
        List<Map<String, Object>> orderItems = jdbcTemplate.queryForList(
                "SELECT d.name AS dish_name, oi.qty, oi.deal_price AS unit_price, oi.subtotal " +
                        "FROM order_item oi JOIN dish d ON oi.dish_id = d.dish_id " +
                        "WHERE oi.order_id = ?", id);
        model.addAttribute("orderItems", orderItems);

        model.addAttribute("orderId", id);
        return "order_detail";
    }

    // 更新订单状态
    @GetMapping("/updateStatus")
    public String updateStatus(@RequestParam Long orderId,
                               @RequestParam String status) {
        jdbcTemplate.update("CALL sp_update_order_status(?, ?)", orderId, status);
        return "redirect:/orders";
    }

    // 新增下单页面
    @GetMapping("/new")
    public String newOrder(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("merchants", merchantRepository.findAll());
        model.addAttribute("dishes", dishRepository.findAll());
        // 获取地址
        List<Map<String, Object>> addresses = jdbcTemplate.queryForList(
                "SELECT address_id, receiver_name, detail FROM address");
        model.addAttribute("addresses", addresses);
        return "order_new";
    }

    // 提交下单
    @PostMapping("/create")
    public String createOrder(@RequestParam Long userId,
                              @RequestParam Long merchantId,
                              @RequestParam Long addressId,
                              @RequestParam Long dishId,
                              @RequestParam int qty,
                              @RequestParam String payMethod) {
        // pay_method 需要转为英文，因为存储过程是 ENUM('alipay','wechat')
        String payMethodEN;
        if ("微信支付".equals(payMethod)) {
            payMethodEN = "wechat";
        } else if ("支付宝".equals(payMethod)) {
            payMethodEN = "alipay";
        } else {
            payMethodEN = "wechat"; // 默认
        }

        jdbcTemplate.update(
                "CALL sp_create_order_single_item(?, ?, ?, ?, ?, ?)",
                userId, merchantId, addressId, dishId, qty, payMethodEN);
        return "redirect:/orders";
    }
}