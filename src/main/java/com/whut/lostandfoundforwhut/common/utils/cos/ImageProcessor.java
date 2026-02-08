package com.whut.lostandfoundforwhut.common.utils.cos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qcloud.cos.model.ciModel.common.ImageProcessRequest;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;

@Component
public class ImageProcessor {
    private final COS cos;

    @Autowired
    public ImageProcessor(COS cos) { this.cos = cos; }

    /**
     * @description 压缩图片
     * @param key COS 存储对象键
     */
    public void processimage(String key) {
        String bucketName = cos.getBucketName();
        String sourceKey = key;
        String targetKey;
        int lastSlashIndex = key.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            targetKey = key.substring(lastSlashIndex + 1);
        } else {
            targetKey = key;
        }
        // 创建云上处理请求
        ImageProcessRequest imageReq = new ImageProcessRequest(bucketName, sourceKey);
        // 配置图片处理参数
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        // 设置处理规则
        List<PicOperations.Rule> ruleList = new ArrayList<>();
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(bucketName);
        rule.setFileId(targetKey);
        // 极智压缩规则
        rule.setRule("imageSlim");
        ruleList.add(rule);
        picOperations.setRules(ruleList);
        imageReq.setPicOperations(picOperations);
        // 执行云上数据处理
        CIUploadResult ciUploadResult = cos.getCosClient().processImage(imageReq);
    }

    // /**
    //  * @description 下载时压缩图片
    //  * @param key COS 存储路径
    //  * @param localFile 本地文件
    //  */
    // public void downloadAndProcessImage(String key, File localFile) {
    //     // 创建云上处理请求
    //     GetObjectRequest request = new GetObjectRequest(cos.getBucketName(), key);
        
    //     // 极智压缩规则
    //     String compress = "imageSlim";
    //     request.putCustomQueryParameter(compress, null);

    //     // 下载处理后的图片
    //     COSClient client = cos.getCosClient();
    //     client.getObject(request, localFile);
    // }
}
