package com.whut.lostandfoundforwhut.common.utils.cos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
public class COSTest {
    @Autowired
    private COS cos;

    private File testFile;
    private String testDir = "src\\test\\java\\com\\whut\\lostandfoundforwhut\\common\\utils\\cos\\images\\";
    private String testKey = "test-image.png";

    @BeforeEach
    public void setUp() {
        // 指向测试图片文件
        testFile = new File(testDir + "test.png");
    }

    @Test
    public void testUploadFile() {
        try {
            cos.uploadFile(testFile, testKey);
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "上传测试失败: " + e.getMessage();
        }
    }

    @Test
    public void testDownloadFile() {
        // 创建下载目标文件
        File downloadFile = new File(testDir + "downloaded-test.png");
        // downloadFile.deleteOnExit();
        
        // 调用下载方法
        cos.downloadFile(testKey, downloadFile);
        
        // 验证文件是否下载成功
        assert downloadFile.exists() : "下载文件不存在";
        assert downloadFile.length() > 0 : "下载文件为空";
    }

    @Test
    public void testGetObjectURL() {
        try {
            String url = cos.getObjectURL(testKey);
            System.out.println("获取到的URL: " + url);
            assert url != null : "获取URL失败";
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "删除测试失败: " + e.getMessage();
        }
    }

    @Test
    public void testDeleteObject() {
        try {
            cos.deleteObject(testKey);
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "删除测试失败: " + e.getMessage();
        }
    }
}
