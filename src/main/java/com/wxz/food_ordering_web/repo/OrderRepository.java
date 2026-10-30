/*
OrderRepository.java
功能：
订单数据访问层。
继承 JpaRepository<Order, Long>。
内置的 findAll(Sort sort) 方法用于实现排序功能。
自定义方法：findByUserId(Long userId)、findByMerchantId(Long merchantId)、findByStatus(String status) 可根据条件筛选订单。
 */
package com.wxz.food_ordering_web.repo;

import com.wxz.food_ordering_web.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByMerchantId(Long merchantId);
    List<Order> findByStatus(String status);
}