/*
AuditLogController.java
功能：
审计日志控制器。
最简单的控制器之一，只有一个方法 list() 查询 audit_log 表获取所有操作日志，按 log_id 倒序排列，显示最近100条记录。
使用 JdbcTemplate 进行查询。
*/
package com.wxz.food_ordering_web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/audit-logs")
public class AuditLogController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public String list(Model model) {
        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT log_id, event_time, event_type, table_name, ref_id, detail " +
                        "FROM audit_log ORDER BY log_id DESC LIMIT 100");
        model.addAttribute("logs", logs);
        return "audit_log_list";
    }
}
