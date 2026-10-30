/*
UserRepository.java
功能：
用户数据访问层。
继承 JpaRepository<User, Long>。
自定义方法：findByNicknameContaining(String nickname) 实现按昵称模糊查询，findByNickname(String nickname) 用于用户登录验证。
 */
package com.wxz.food_ordering_web.repo;

import com.wxz.food_ordering_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByNicknameContaining(String nickname);
    User findByNickname(String nickname);
}