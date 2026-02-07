package com.whut.lostandfoundforwhut.common.utils.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@SpringBootTest
class ImageProcessorTest {
    @Autowired
    private ImageProcessor imageProcessor;

    private File testFile;
    private String testDir = "src\\test\\java\\com\\whut\\lostandfoundforwhut\\common\\utils\\cos\\images\\";
    private String testKey = "test-image.png";

    @BeforeEach
    public void setUp() {
        // 指向测试图片文件
        testFile = new File(testDir, "test.png");
    }

    @Test
    void testDownloadAndProcessImage() {
        // 创建下载目标文件
        File downloadFile = new File(testDir, "downloaded-test.png");
        
        // 调用下载方法
        imageProcessor.downloadAndProcessImage(testKey, downloadFile);
        
        // 验证文件是否下载成功
        assert downloadFile.exists() : "下载文件不存在";
        assert downloadFile.length() > 0 : "下载文件为空";
    }
}