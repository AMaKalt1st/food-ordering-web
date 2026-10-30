/*
MerchantRepository.java
功能：
商家数据访问层。
继承 JpaRepository<Merchant, Long>。
自定义方法：findByNameContaining(String name) 实现按商家名称模糊查询，findByUsername(String username) 用于商家登录验证。
 */
package com.wxz.food_ordering_web.repo;

import com.wxz.food_ordering_web.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    List<Merchant> findByNameContaining(String name);
    Merchant findByUsername(String username);
}