package com.whut.lostandfoundforwhut.common.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnvVariableTest {

    @Autowired
    private Environment environment;

    @BeforeAll
    static void loadEnvFile() {
        // 手动加载 .env 文件
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    System.setProperty(key, value);
                    System.out.println("Loaded env variable: " + key + " = " + value);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }
    }

    @Test
    void testCosEnvVariablesLoaded() {
        // 测试腾讯云 COS 相关环境变量是否正确加载
        String secretId = System.getProperty("TENCENT_COS_SECRET_ID");
        String secretKey = System.getProperty("TENCENT_COS_SECRET_KEY");
        String bucketName = System.getProperty("TENCENT_COS_BUCKET_NAME");
        String region = System.getProperty("TENCENT_COS_REGION");

        // 验证环境变量是否存在
        assertNotNull(secretId, "TENCENT_COS_SECRET_ID 环境变量未加载");
        assertNotNull(secretKey, "TENCENT_COS_SECRET_KEY 环境变量未加载");
        assertNotNull(bucketName, "TENCENT_COS_BUCKET_NAME 环境变量未加载");
        assertNotNull(region, "TENCENT_COS_REGION 环境变量未加载");

        // 验证环境变量值是否非空
        assertFalse(secretId.isEmpty(), "TENCENT_COS_SECRET_ID 环境变量值为空");
        assertFalse(secretKey.isEmpty(), "TENCENT_COS_SECRET_KEY 环境变量值为空");
        assertFalse(bucketName.isEmpty(), "TENCENT_COS_BUCKET_NAME 环境变量值为空");
        assertFalse(region.isEmpty(), "TENCENT_COS_REGION 环境变量值为空");

        // 打印环境变量值，用于调试
        System.out.println("TENCENT_COS_SECRET_ID: " + secretId);
        System.out.println("TENCENT_COS_SECRET_KEY: " + secretKey);
        System.out.println("TENCENT_COS_BUCKET_NAME: " + bucketName);
        System.out.println("TENCENT_COS_REGION: " + region);
    }

    @Test
    void testEnvVariablesFromEnvironment() {
        // 测试通过 Environment 对象读取环境变量
        String secretId = environment.getProperty("TENCENT_COS_SECRET_ID");
        String secretKey = environment.getProperty("TENCENT_COS_SECRET_KEY");
        String bucketName = environment.getProperty("TENCENT_COS_BUCKET_NAME");
        String region = environment.getProperty("TENCENT_COS_REGION");

        // 验证环境变量是否存在
        assertNotNull(secretId, "TENCENT_COS_SECRET_ID 环境变量未加载到 Environment");
        assertNotNull(secretKey, "TENCENT_COS_SECRET_KEY 环境变量未加载到 Environment");
        assertNotNull(bucketName, "TENCENT_COS_BUCKET_NAME 环境变量未加载到 Environment");
        assertNotNull(region, "TENCENT_COS_REGION 环境变量未加载到 Environment");

        // 打印环境变量值，用于调试
        System.out.println("\n从 Environment 读取的环境变量：");
        System.out.println("TENCENT_COS_SECRET_ID: " + secretId);
        System.out.println("TENCENT_COS_SECRET_KEY: " + secretKey);
        System.out.println("TENCENT_COS_BUCKET_NAME: " + bucketName);
        System.out.println("TENCENT_COS_REGION: " + region);
    }
}
