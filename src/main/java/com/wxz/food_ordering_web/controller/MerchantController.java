/*
MerchantController.java
功能：
商家管理控制器。
包含5个方法：
list() 获取所有商家列表，
newForm() 跳转新增商家页面，
save() 保存新增商家，
editForm() 跳转编辑商家页面，
delete() 删除商家。
使用 MerchantRepository 进行数据库操作，表单中需要传递 username 和 password 字段。
*/
package com.wxz.food_ordering_web.controller;

import com.wxz.food_ordering_web.entity.Merchant;
import com.wxz.food_ordering_web.repo.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/merchants")
public class MerchantController {

    @Autowired
    private MerchantRepository merchantRepository;

    // 商家列表
    @GetMapping
    public String list(Model model) {
        List<Merchant> merchants = merchantRepository.findAll();
        model.addAttribute("merchants", merchants);
        return "merchant_list";
    }

    // 新增页面
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("merchant", new Merchant());
        return "merchant_form";
    }

    // 保存新增
    @PostMapping
    public String save(@ModelAttribute Merchant merchant) {
        merchantRepository.save(merchant);
        return "redirect:/merchants";
    }

    // 编辑页面
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Merchant merchant = merchantRepository.findById(id).orElseThrow();
        model.addAttribute("merchant", merchant);
        return "merchant_form";
    }

    // 删除
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        merchantRepository.deleteById(id);
        return "redirect:/merchants";
    }
}