/*
DishRepository.java
功能：
菜品数据访问层。
继承 JpaRepository<Dish, Long>，自动获得 findAll()、findById()、save()、deleteById() 等方法。
自定义方法：findByNameContaining(String name) 实现按菜品名称模糊查询。
 */
package com.wxz.food_ordering_web.repo;

import com.wxz.food_ordering_web.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByMerchantId(Long merchantId);
    List<Dish> findByNameContaining(String name);
}