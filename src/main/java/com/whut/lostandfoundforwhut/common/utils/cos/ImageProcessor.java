package com.whut.lostandfoundforwhut.common.utils.cos;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;

@Component
public class ImageProcessor {
    private final COS cos;

    @Autowired
    public ImageProcessor(COS cos) { this.cos = cos; }

    /**
     * @description 下载时压缩图片
     * @param key COS 存储路径
     * @param localFile 本地文件
     */
    public void downloadAndProcessImage(String key, File localFile) {
        // 创建云上处理请求
        GetObjectRequest request = new GetObjectRequest(cos.getBucketName(), key);
        
        // 极智压缩规则
        String compress = "imageSlim";
        request.putCustomQueryParameter(compress, null);

        // 下载处理后的图片
        COSClient client = cos.getCosClient();
        client.getObject(request, localFile);
    }
}
