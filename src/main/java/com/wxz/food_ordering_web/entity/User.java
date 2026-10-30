/*
User.java
功能：
用户实体类。
映射 user 表，包含字段：
userId（主键，自增）、nickname（昵称）、phone（手机号）、role（角色）、createdAt（创建时间，默认当前时间）。
使用 @Entity、@Table(name = "user")、@Id、@GeneratedValue 等JPA注解。
关键设计：createdAt 设置默认值 LocalDateTime.now()，防止插入时报非空约束错误。
*/
package com.wxz.food_ordering_web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 20)
    private String role;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();  // 加个默认值

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}