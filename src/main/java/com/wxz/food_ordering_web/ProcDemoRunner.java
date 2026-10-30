package com.wxz.food_ordering_web;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Component
public class ProcDemoRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    // 两趟跑：减少 DROP/CREATE 引入的抖动
    private boolean noIndexPrepared = false;   // 是否已准备“无索引状态”（DROP过）
    private boolean withIndexPrepared = false; // 是否已准备“有索引状态”（CREATE过）

    public ProcDemoRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean procDemo = true;
        for (String a : args) {
            if ("--procDemo=true".equals(a)) procDemo = true;
        }
        if (!procDemo) return;

        System.out.println("==== 存储过程调用演示开始（菜单式 + 可取消输入 + audit_log + 索引两趟跑 + CSV记录） ====");

        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             Scanner sc = new Scanner(System.in)) {

            while (true) {
                System.out.println("\n请选择操作：");
                System.out.println("1) 查询订单详情（sp_get_order_detail）");
                System.out.println("2) 下单插入（sp_create_order_single_item）");
                System.out.println("3) 更新订单状态（sp_update_order_status）");
                System.out.println("4) 删除地址（sp_delete_address）");
                System.out.println("5) 查看触发器日志（audit_log）");
                System.out.println("6a) 索引实验-无索引测速（DROP一次后多次测 + 写CSV）");
                System.out.println("6b) 索引实验-有索引测速（CREATE一次后多次测 + 写CSV）");
                System.out.println("0) 退出");
                System.out.print("请输入选项：");

                String choice = sc.nextLine().trim();
                switch (choice) {
                    case "1" -> handleQueryOrderDetail(conn, sc);
                    case "2" -> handleCreateOrder(conn, sc);
                    case "3" -> handleUpdateOrderStatus(conn, sc);
                    case "4" -> handleDeleteAddress(conn, sc);
                    case "5" -> handleShowAuditLog(sc);
                    case "6a" -> handleIndexBenchNoIndex(sc);
                    case "6b" -> handleIndexBenchWithIndex(sc);
                    case "0" -> {
                        System.out.println("==== 已退出菜单 ====");
                        System.out.println("==== 存储过程调用演示结束 ====");
                        return;
                    }
                    default -> System.out.println("无效选项，请重新输入。");
                }
            }
        }
    }

    // ===================== 5) 查看触发器日志 =====================

    private void handleShowAuditLog(Scanner sc) {
        try {
            Integer limit = askIntOrCancel(sc, "查看最近多少条日志？(1-200，q/quit/0 取消)：", 1, 200);
            if (limit == null) {
                System.out.println("[CANCEL] 已取消查看日志，返回主菜单。");
                return;
            }

            System.out.println("\n最近审计日志（audit_log）：");
            printRows(jdbcTemplate.queryForList(
                    "SELECT log_id, event_time, event_type, table_name, ref_id, detail " +
                            "FROM audit_log ORDER BY log_id DESC LIMIT " + limit));

        } catch (Exception e) {
            printException("查看 audit_log", e);
        }
    }

    // ===================== 6a) 索引实验：无索引测速（两趟跑：先把 NO_INDEX 全部测完） =====================

    private void handleIndexBenchNoIndex(Scanner sc) {
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
            Long paid = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE status='paid'", Long.class);
            System.out.println("\n[IndexBench-NO] 当前 orders 总行数=" + total + "；status='paid' 行数=" + paid);

            Integer warmup = askIntOrCancel(sc, "请输入热身次数 warmup（建议 10，q/quit/0 取消）：", 1, 100000);
            if (warmup == null) {
                System.out.println("[CANCEL] 已取消无索引测速，返回主菜单。");
                return;
            }

            Integer loops = askIntOrCancel(sc, "请输入计时次数 loops（建议 200~2000，q/quit/0 取消）：", 1, 200000);
            if (loops == null) {
                System.out.println("[CANCEL] 已取消无索引测速，返回主菜单。");
                return;
            }

            final String sql = "SELECT COUNT(*) FROM orders WHERE status='paid'";

            // 仅第一次进入无索引模式时 DROP
            if (!noIndexPrepared) {
                System.out.println("\n[IndexBench-NO] 准备无索引状态：DROP idx_orders_status（仅执行一次）");
                dropIndexIfExists("orders", "idx_orders_status");
                noIndexPrepared = true;
                withIndexPrepared = false; // 已删除索引
            } else {
                System.out.println("\n[IndexBench-NO] 已处于无索引状态（本次不再DROP）");
            }

            // warmup
            for (int i = 0; i < warmup; i++) jdbcTemplate.queryForObject(sql, Long.class);

            long t1 = System.nanoTime();
            for (int i = 0; i < loops; i++) jdbcTemplate.queryForObject(sql, Long.class);
            long t2 = System.nanoTime();

            double avgMs = (t2 - t1) / 1_000_000.0 / loops;
            System.out.printf("[IndexBench-NO] 无索引 平均耗时：%.4f ms/query（loops=%d）%n", avgMs, loops);

            appendIndexBenchCsv("NO_INDEX", total, paid, warmup, loops, avgMs);
            System.out.println("[IndexBench-NO] 已写入 CSV：output/index_bench_two_pass.csv");

            System.out.println("\n[建议截图] 无索引 EXPLAIN：");
            System.out.println("  EXPLAIN " + sql + ";");

        } catch (Exception e) {
            printException("索引实验-无索引测速", e);
        }
    }

    // ===================== 6b) 索引实验：有索引测速（两趟跑：再把 WITH_INDEX 全部测完） =====================

    private void handleIndexBenchWithIndex(Scanner sc) {
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
            Long paid = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE status='paid'", Long.class);
            System.out.println("\n[IndexBench-WITH] 当前 orders 总行数=" + total + "；status='paid' 行数=" + paid);

            Integer warmup = askIntOrCancel(sc, "请输入热身次数 warmup（建议 10，q/quit/0 取消）：", 1, 100000);
            if (warmup == null) {
                System.out.println("[CANCEL] 已取消有索引测速，返回主菜单。");
                return;
            }

            Integer loops = askIntOrCancel(sc, "请输入计时次数 loops（建议 200~2000，q/quit/0 取消）：", 1, 200000);
            if (loops == null) {
                System.out.println("[CANCEL] 已取消有索引测速，返回主菜单。");
                return;
            }

            final String sql = "SELECT COUNT(*) FROM orders WHERE status='paid'";

            // 仅第一次进入有索引模式时 CREATE（先判断是否存在，避免重复建索引报错）
            if (!withIndexPrepared) {
                System.out.println("\n[IndexBench-WITH] 准备有索引状态：CREATE idx_orders_status（仅执行一次）");

                // 如果不存在则创建；存在就不动
                if (!indexExists("orders", "idx_orders_status")) {
                    jdbcTemplate.execute("CREATE INDEX idx_orders_status ON orders(status)");
                    System.out.println("[IndexBench-WITH] 已创建索引：idx_orders_status");
                } else {
                    System.out.println("[IndexBench-WITH] 索引已存在：idx_orders_status（无需创建）");
                }

                withIndexPrepared = true;
                noIndexPrepared = false;
            } else {
                System.out.println("\n[IndexBench-WITH] 已处于有索引状态（本次不再CREATE）");
            }

            // warmup
            for (int i = 0; i < warmup; i++) jdbcTemplate.queryForObject(sql, Long.class);

            long t1 = System.nanoTime();
            for (int i = 0; i < loops; i++) jdbcTemplate.queryForObject(sql, Long.class);
            long t2 = System.nanoTime();

            double avgMs = (t2 - t1) / 1_000_000.0 / loops;
            System.out.printf("[IndexBench-WITH] 有索引 平均耗时：%.4f ms/query（loops=%d）%n", avgMs, loops);

            appendIndexBenchCsv("WITH_INDEX", total, paid, warmup, loops, avgMs);
            System.out.println("[IndexBench-WITH] 已写入 CSV：output/index_bench_two_pass.csv");

            System.out.println("\n[建议截图] 有索引 EXPLAIN：");
            System.out.println("  EXPLAIN " + sql + ";");

        } catch (Exception e) {
            printException("索引实验-有索引测速", e);
        }
    }

    // ===================== 索引工具函数 =====================

    private boolean indexExists(String tableName, String indexName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class, tableName, indexName);
        return cnt != null && cnt > 0;
    }

    private void dropIndexIfExists(String tableName, String indexName) {
        if (indexExists(tableName, indexName)) {
            jdbcTemplate.execute("DROP INDEX " + indexName + " ON " + tableName);
            System.out.println("[IndexBench] 已删除索引：" + indexName);
        } else {
            System.out.println("[IndexBench] 索引不存在，无需删除：" + indexName);
        }
    }

    private void appendIndexBenchCsv(String indexState,
                                     long totalRows,
                                     long paidRows,
                                     int warmup,
                                     int loops,
                                     double avgMs) throws IOException {

        Path dir = Path.of("output");
        Files.createDirectories(dir);

        Path csv = dir.resolve("index_bench_two_pass.csv");
        boolean needHeader = !Files.exists(csv) || Files.size(csv) == 0;

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (BufferedWriter w = Files.newBufferedWriter(
                csv,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        )) {
            if (needHeader) {
                w.write("timestamp,index_state,total_rows,paid_rows,warmup,loops,avg_ms");
                w.newLine();
            }

            w.write(String.format("%s,%s,%d,%d,%d,%d,%.6f",
                    ts, indexState, totalRows, paidRows, warmup, loops, avgMs));
            w.newLine();
        }
    }

    // ===================== 1) 查询订单详情 =====================

    private void handleQueryOrderDetail(Connection conn, Scanner sc) {
        try {
            System.out.println("\n最近订单（orders）：");
            printRows(jdbcTemplate.queryForList(
                    "SELECT order_id, user_id, merchant_id, status, total_amount, created_at " +
                            "FROM orders ORDER BY order_id DESC LIMIT 10"));

            Long orderId = askExistingLongOrCancel(sc, jdbcTemplate,
                    "请输入要查询的 order_id（q/quit/0 取消）：",
                    "SELECT 1 FROM orders WHERE order_id=?");

            if (orderId == null) {
                System.out.println("[CANCEL] 已取消查询，返回主菜单。");
                return;
            }

            callSpGetOrderDetail(conn, orderId);
        } catch (Exception e) {
            printException("查询订单详情", e);
        }
    }

    // ===================== 2) 下单插入 =====================

    private void handleCreateOrder(Connection conn, Scanner sc) {
        try {
            // 选择 user + address（循环直到 user 有地址；任意步骤可取消）
            Long userId;
            Long addressId;

            while (true) {
                System.out.println("\n可用用户（user）：");
                List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                        "SELECT user_id, nickname, phone FROM user ORDER BY user_id LIMIT 50");
                printRows(userRows);

                if (userRows.isEmpty()) {
                    System.out.println("【错误】user 表无数据：请先插入至少一条 user。");
                    return;
                }

                userId = askExistingLongOrCancel(sc, jdbcTemplate,
                        "请输入 user_id（q/quit/0 取消）：",
                        "SELECT 1 FROM user WHERE user_id=?");

                if (userId == null) {
                    System.out.println("[CANCEL] 已取消下单，返回主菜单。");
                    return;
                }

                System.out.println("\n该用户可用地址（address）：");
                List<Map<String, Object>> addrRows = jdbcTemplate.queryForList(
                        "SELECT address_id, receiver_name, receiver_phone, region, detail " +
                                "FROM address WHERE user_id=? ORDER BY address_id", userId);
                printRows(addrRows);

                if (addrRows.isEmpty()) {
                    System.out.println("该用户没有地址，请重新选择 user_id（或先给该用户新增地址）。");
                    continue;
                }

                addressId = askExistingLongOrCancel(sc, jdbcTemplate,
                        "请输入 address_id（必须属于该 user，q/quit/0 取消）：",
                        "SELECT 1 FROM address WHERE address_id=? AND user_id=?",
                        userId);

                if (addressId == null) {
                    System.out.println("[CANCEL] 已取消下单，返回主菜单。");
                    return;
                }

                break;
            }

            // 选择 merchant + dish（循环直到 merchant 有上架 dish；任意步骤可取消）
            Long merchantId;
            Long dishId;

            while (true) {
                System.out.println("\n可用商家（merchant）：");
                List<Map<String, Object>> merchantRows = jdbcTemplate.queryForList(
                        "SELECT merchant_id, name, phone, rating, status FROM merchant ORDER BY merchant_id LIMIT 50");
                printRows(merchantRows);

                if (merchantRows.isEmpty()) {
                    System.out.println("【错误】merchant 表无数据：请先插入至少一条 merchant。");
                    return;
                }

                merchantId = askExistingLongOrCancel(sc, jdbcTemplate,
                        "请输入 merchant_id（q/quit/0 取消）：",
                        "SELECT 1 FROM merchant WHERE merchant_id=?");

                if (merchantId == null) {
                    System.out.println("[CANCEL] 已取消下单，返回主菜单。");
                    return;
                }

                System.out.println("\n该商家上架菜品（dish.on_sale=1）：");
                List<Map<String, Object>> dishRows = jdbcTemplate.queryForList(
                        "SELECT dish_id, name, price, on_sale FROM dish " +
                                "WHERE merchant_id=? AND on_sale=1 ORDER BY dish_id", merchantId);
                printRows(dishRows);

                if (dishRows.isEmpty()) {
                    System.out.println("该商家当前没有上架菜品（on_sale=1），请重新选择 merchant_id（或先上架菜品）。                    ");
                    continue;
                }

                dishId = askExistingLongOrCancel(sc, jdbcTemplate,
                        "请输入 dish_id（必须属于该商家且上架，q/quit/0 取消）：",
                        "SELECT 1 FROM dish WHERE dish_id=? AND merchant_id=? AND on_sale=1",
                        merchantId);

                if (dishId == null) {
                    System.out.println("[CANCEL] 已取消下单，返回主菜单。");
                    return;
                }

                break;
            }

            Integer qty = askIntOrCancel(sc, "请输入 qty（>0，q/quit/0 取消）：", 1, 999);
            if (qty == null) {
                System.out.println("[CANCEL] 已取消下单，返回主菜单。");
                return;
            }

            String payMethod = askPayMethodOrCancel(sc);
            if (payMethod == null) {
                System.out.println("[CANCEL] 已取消下单，返回主菜单。");
                return;
            }

            long newOrderId;
            BigDecimal totalAmount;

            try (CallableStatement cs = conn.prepareCall("{CALL sp_create_order_single_item(?,?,?,?,?,?)}")) {
                cs.setLong(1, userId);
                cs.setLong(2, merchantId);
                cs.setLong(3, addressId);
                cs.setLong(4, dishId);
                cs.setInt(5, qty);
                cs.setString(6, payMethod);

                cs.execute();
                try (ResultSet rs = cs.getResultSet()) {
                    rs.next();
                    newOrderId = rs.getLong("new_order_id");
                    totalAmount = rs.getBigDecimal("total_amount");
                }
            }

            System.out.println("\n[INSERT PROC] 下单成功：new_order_id=" + newOrderId + ", total_amount=" + totalAmount);

            System.out.print("是否立即查询该订单详情？(y/n)：");
            String yn = sc.nextLine().trim();
            if (yn.equalsIgnoreCase("y")) {
                callSpGetOrderDetail(conn, newOrderId);
            }

        } catch (Exception e) {
            printException("下单插入", e);
        }
    }

    // ===================== 3) 更新订单状态 =====================

    private void handleUpdateOrderStatus(Connection conn, Scanner sc) {
        try {
            System.out.println("\n最近订单（orders）：");
            printRows(jdbcTemplate.queryForList(
                    "SELECT order_id, status, total_amount, created_at FROM orders ORDER BY order_id DESC LIMIT 10"));

            Long orderId = askExistingLongOrCancel(sc, jdbcTemplate,
                    "请输入要更新的 order_id（q/quit/0 取消）：",
                    "SELECT 1 FROM orders WHERE order_id=?");

            if (orderId == null) {
                System.out.println("[CANCEL] 已取消更新，返回主菜单。");
                return;
            }

            String newStatus = askOrderStatusOrCancel(sc);
            if (newStatus == null) {
                System.out.println("[CANCEL] 已取消更新，返回主菜单。");
                return;
            }

            try (CallableStatement cs = conn.prepareCall("{CALL sp_update_order_status(?,?)}")) {
                cs.setLong(1, orderId);
                cs.setString(2, newStatus);
                cs.execute();
            }

            System.out.println("[UPDATE PROC] order " + orderId + " -> " + newStatus);
            printRows(jdbcTemplate.queryForList(
                    "SELECT order_id, status, paid_at FROM orders WHERE order_id=?", orderId));

        } catch (Exception e) {
            printException("更新订单状态", e);
        }
    }

    // ===================== 4) 删除地址 =====================

    private void handleDeleteAddress(Connection conn, Scanner sc) {
        try {
            System.out.println("\n可用用户（user）：");
            printRows(jdbcTemplate.queryForList(
                    "SELECT user_id, nickname, phone FROM user ORDER BY user_id LIMIT 50"));

            Long userId = askExistingLongOrCancel(sc, jdbcTemplate,
                    "请输入 user_id（q/quit/0 取消）：",
                    "SELECT 1 FROM user WHERE user_id=?");

            if (userId == null) {
                System.out.println("[CANCEL] 已取消删除，返回主菜单。");
                return;
            }

            System.out.println("\n该用户地址（address）：");
            printRows(jdbcTemplate.queryForList(
                    "SELECT address_id, receiver_name, receiver_phone, region, detail " +
                            "FROM address WHERE user_id=? ORDER BY address_id", userId));

            Long addressId = askExistingLongOrCancel(sc, jdbcTemplate,
                    "请输入要删除的 address_id（必须属于该 user，q/quit/0 取消）：",
                    "SELECT 1 FROM address WHERE address_id=? AND user_id=?",
                    userId);

            if (addressId == null) {
                System.out.println("[CANCEL] 已取消删除，返回主菜单。");
                return;
            }

            try (CallableStatement cs = conn.prepareCall("{CALL sp_delete_address(?,?)}")) {
                cs.setLong(1, addressId);
                cs.setLong(2, userId);
                cs.execute();
            }

            System.out.println("[DELETE PROC] address_id=" + addressId + " 已删除");
            System.out.println("删除后该用户地址：");
            printRows(jdbcTemplate.queryForList(
                    "SELECT address_id, receiver_name, receiver_phone, region, detail " +
                            "FROM address WHERE user_id=? ORDER BY address_id", userId));

        } catch (Exception e) {
            printException("删除地址", e);
        }
    }

    // ===================== 存储过程公共调用 =====================

    private void callSpGetOrderDetail(Connection conn, long orderId) throws SQLException {
        try (CallableStatement cs = conn.prepareCall("{CALL sp_get_order_detail(?)}")) {
            cs.setLong(1, orderId);
            boolean hasRs = cs.execute();
            int idx = 0;

            while (hasRs) {
                idx++;
                try (ResultSet rs = cs.getResultSet()) {
                    System.out.println("[QUERY PROC] ResultSet #" + idx);
                    ResultSetMetaData md = rs.getMetaData();
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            System.out.print(md.getColumnLabel(i) + "=" + rs.getString(i) + "  ");
                        }
                        System.out.println();
                    }
                    if (!any) System.out.println("(无记录)");
                }
                hasRs = cs.getMoreResults();
            }
        }
    }

    // ===================== helpers（统一可取消输入） =====================

    private static boolean isCancel(String s) {
        return s == null || s.isBlank()
                || s.equalsIgnoreCase("q")
                || s.equalsIgnoreCase("quit")
                || s.equals("0");
    }

    private static void printRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("(无数据)");
            return;
        }
        for (Map<String, Object> r : rows) {
            System.out.println(r);
        }
    }

    private static Long askExistingLongOrCancel(Scanner sc, JdbcTemplate jt,
                                                String prompt, String existsSql, Object... extraParams) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (isCancel(s)) return null;

            try {
                long id = Long.parseLong(s);

                Object[] params = new Object[1 + (extraParams == null ? 0 : extraParams.length)];
                params[0] = id;
                if (extraParams != null) System.arraycopy(extraParams, 0, params, 1, extraParams.length);

                List<Map<String, Object>> ok = jt.queryForList(existsSql, params);
                if (ok != null && !ok.isEmpty()) return id;

                System.out.println("不存在或不符合条件，请重新输入（q/quit/0 取消）。");
            } catch (NumberFormatException e) {
                System.out.println("请输入数字（q/quit/0 取消）。");
            }
        }
    }

    private static Integer askIntOrCancel(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (isCancel(s)) return null;

            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    System.out.println("输入不合法：范围应为 " + min + " ~ " + max + "（q/quit/0 取消）。");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("请输入整数（q/quit/0 取消）。");
            }
        }
    }

    private static String askPayMethodOrCancel(Scanner sc) {
        while (true) {
            System.out.print("请输入 pay_method（alipay/wechat，q/quit/0 取消）：");
            String m = sc.nextLine().trim().toLowerCase();
            if (isCancel(m)) return null;
            if (m.equals("alipay") || m.equals("wechat")) return m;
            System.out.println("输入不合法，只能是 alipay 或 wechat（q/quit/0 取消）。");
        }
    }

    private static String askOrderStatusOrCancel(Scanner sc) {
        while (true) {
            System.out.print("请输入新状态（created/paid/accepted/done/cancelled，q/quit/0 取消）：");
            String st = sc.nextLine().trim().toLowerCase();
            if (isCancel(st)) return null;

            if (st.equals("created") || st.equals("paid") || st.equals("accepted") || st.equals("done") || st.equals("cancelled")) {
                return st;
            }
            System.out.println("输入不合法（q/quit/0 取消）。");
        }
    }

    private static void printException(String action, Exception e) {
        System.out.println("【操作失败】" + action);
        if (e instanceof SQLException se) {
            System.out.println("SQLState=" + se.getSQLState() + ", ErrorCode=" + se.getErrorCode());
            System.out.println("Message=" + se.getMessage());
        } else {
            System.out.println("Message=" + e.getMessage());
        }
    }
}