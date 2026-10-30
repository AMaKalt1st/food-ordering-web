/*
DishController.java
功能：
菜品管理控制器。
包含6个方法：
list() 获取所有菜品列表，
searchByMerchant() 按商家ID搜索菜品，
newForm() 跳转新增菜品页面，
save() 保存新增或编辑的菜品，
editForm() 跳转编辑菜品页面，
delete() 删除菜品。
涉及与 DishRepository 和 MerchantRepository 的交互。
*/
package com.wxz.food_ordering_web.controller;

import com.wxz.food_ordering_web.entity.Dish;
import com.wxz.food_ordering_web.repo.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/dishes")
public class DishController {

    @Autowired
    private DishRepository dishRepository;

    // 菜品列表
    @GetMapping
    public String list(Model model) {
        List<Dish> dishes = dishRepository.findAll();
        model.addAttribute("dishes", dishes);
        return "dish_list";
    }

    // 按商家查询
    @GetMapping("/search")
    public String search(@RequestParam(required = false) Long merchantId, Model model) {
        List<Dish> dishes;
        if (merchantId != null) {
            dishes = dishRepository.findByMerchantId(merchantId);
        } else {
            dishes = dishRepository.findAll();
        }
        model.addAttribute("dishes", dishes);
        model.addAttribute("merchantId", merchantId);
        return "dish_list";
    }

    // 新增页面
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dish", new Dish());
        return "dish_form";
    }

    // 保存新增
    @PostMapping
    public String save(@ModelAttribute Dish dish) {
        dishRepository.save(dish);
        return "redirect:/dishes";
    }

    // 编辑页面
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("无效菜品ID: " + id));
        model.addAttribute("dish", dish);
        return "dish_form";
    }

    // 删除
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        dishRepository.deleteById(id);
        return "redirect:/dishes";
    }
}

