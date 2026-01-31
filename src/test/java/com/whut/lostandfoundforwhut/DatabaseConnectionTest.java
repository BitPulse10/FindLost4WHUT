package com.whut.lostandfoundforwhut;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 数据库连接测试类
 */
@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试数据库连接是否成功
     */
    @Test
    void testDatabaseConnection() throws SQLException {
        assertNotNull(dataSource, "数据源不应为空");

        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(5), "数据库连接应该有效");
            System.out.println("数据库连接成功！");
            System.out.println("数据库URL: " + connection.getMetaData().getURL());
            System.out.println("数据库驱动: " + connection.getMetaData().getDriverName());
            System.out.println("数据库用户名: " + connection.getMetaData().getUserName());
        }
    }

    /**
     * 测试通过JdbcTemplate执行简单查询
     */
    @Test
    void testJdbcTemplate() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result, "查询结果应该是1");
        System.out.println("JdbcTemplate 查询成功！");
    }

    /**
     * 测试数据库连接池配置
     */
    @Test
    void testConnectionPool() throws SQLException {
        try (Connection conn1 = dataSource.getConnection();
                Connection conn2 = dataSource.getConnection();
                Connection conn3 = dataSource.getConnection()) {

            assertTrue(conn1.isValid(5));
            assertTrue(conn2.isValid(5));
            assertTrue(conn3.isValid(5));

            // 验证连接不是同一个实例（连接池应该提供不同的连接）
            assertNotSame(conn1, conn2);
            assertNotSame(conn2, conn3);

            System.out.println("连接池工作正常！");
        }
    }
}