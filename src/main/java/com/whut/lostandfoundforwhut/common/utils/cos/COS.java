package com.whut.lostandfoundforwhut.common.utils.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.exception.MultiObjectDeleteException.DeleteError;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.DeleteObjectsRequest.KeyVersion;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsResult;
import com.qcloud.cos.model.DeleteObjectsResult.DeletedObject;
import com.qcloud.cos.region.Region;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

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
    // private TransferManager transferManager;

    @PostConstruct
    public void init() {
        // 初始化 COS 客户端
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        cosClient = new COSClient(cred, clientConfig);
        // // 初始化 TransferManager
        // ExecutorService threadPool = Executors.newFixedThreadPool(16);
        // transferManager = new TransferManager(cosClient, threadPool);
    }

    /**
     * 关闭资源
     */
    @PreDestroy
    public void shutdown() {
        // // 关闭 TransferManager
        // try {
        //     Optional.ofNullable(transferManager).ifPresent(tm -> tm.shutdownNow(false));
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        // 关闭 COS 客户端
        try {
            Optional.ofNullable(cosClient).ifPresent(c -> c.shutdown());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置对象为公共读权限
     * @param key COS 存储路径
     */
    public void setObjectPublicRead(String key) {
        cosClient.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
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
    public String getObjectUrl(String key) {
        return cosClient.getObjectUrl(bucketName, key).toString();
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

    /**
     * 批量删除对象
     * @param keys COS 存储路径列表
     * @return 删除错误列表
     */
    public List<DeleteError> batchDeleteObject(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        List<KeyVersion> keyVersions = new ArrayList<>();
        for (String key : keys) {
            keyVersions.add(new KeyVersion(key));
        }

        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
        deleteObjectsRequest.setKeys(keyVersions);

        try {
            DeleteObjectsResult deleteObjectsResult = cosClient.deleteObjects(deleteObjectsRequest);
            List<DeletedObject> deleteObjectResultArray = deleteObjectsResult.getDeletedObjects();
            return new ArrayList<>();
        } catch (MultiObjectDeleteException mde) {
            List<DeleteError> deleteErrors = mde.getErrors();
            return deleteErrors;
        }
    }

    /**
     * 检查 ObjectKey 是否存在
     * @param key COS 存储路径
     * @return 如果存在返回 true，否则返回 false
     */
    public boolean hasObject(String key) {
        return cosClient.doesObjectExist(bucketName, key);
    }

    public String getBucketName() { return bucketName; }
    public COSClient getCosClient() { return cosClient; }
}
