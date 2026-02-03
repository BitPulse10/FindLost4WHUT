package com.whut.lostandfoundforwhut.common.utils.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.region.Region;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.Date;

@Component
public class COS {
    @Value("${tencent.cos.secret-id}")
    private String secretId;
    @Value("${tencent.cos.secret-key}")
    private String secretKey;
    @Value("${tencent.cos.bucket-name}")
    private String bucketName;
    @Value("${tencent.cos.region}")
    private String region;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        cosClient = new COSClient(cred, clientConfig);
    }

    /**
     * 上传本地文件
     * @param localFile 本地文件
     * @param key COS 存储路径
     */
    public void uploadFile(File localFile, String key) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
    }

    /**
     * 获取对象访问URL
     * @param key COS 存储路径
     * @return 访问URL
     */
    public String getObjectURL(String key) {
        Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000);
        URL url = cosClient.generatePresignedUrl(bucketName, key, expiration);
        return url.toString();
    }

    /**
     * 下载文件到本地
     * @param key COS 存储路径
     * @param localFile 本地文件
     */
    public void downloadFile(String key, File localFile) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        cosClient.getObject(getObjectRequest, localFile);
    }

    /**
     * 删除文件
     * @param key COS 存储路径
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(bucketName, key);
    }

    public String getBucketName() { return bucketName; }
    public COSClient getCosClient() { return cosClient; }
}
