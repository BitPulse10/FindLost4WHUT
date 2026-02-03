package com.whut.lostandfoundforwhut.common.utils.cos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qcloud.cos.model.ciModel.auditing.AuditingJobsDetail;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingInputObject;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.BatchImageJobDetail;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingResponse;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class ContentReviewer {
    private final COS cos;

    @Autowired
    public ContentReviewer(COS cos) { this.cos = cos; }

    /**
     * @description 审核文本
     * @param text 文本
     * @return 审核错误描述，为空字符串则审核通过
     */
    public String reviewText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 构建文本审核请求
        TextAuditingRequest request = new TextAuditingRequest();
        request.setBucketName(cos.getBucketName());

        // 对文本进行 Base64 编码
        String encodedText = Base64.getEncoder().encodeToString(text.getBytes());
        request.getInput().setContent(encodedText);

        // 调用文本审核接口
        TextAuditingResponse response = cos.getCosClient().createAuditingTextJobs(request);
        AuditingJobsDetail jobDetail = response.getJobsDetail();

        if (!jobDetail.getState().equals("Success")) {
            return "审核失败: " + jobDetail.getMessage();
        } 

        return getMessage(jobDetail.getResult(), jobDetail.getLabel());
    }   

    /**
     * @description 审核图片 URL
     * @param imageURL 图片 URL
     * @return 审核错误描述，为空字符串则审核通过
     */
    public String reviewImageURL(String imageURL) {
        // 构建图片审核请求
        ImageAuditingRequest request = new ImageAuditingRequest();
        request.setBucketName(cos.getBucketName());
        request.setDetectUrl(imageURL);

        // 调用图片审核接口
        ImageAuditingResponse response = cos.getCosClient().imageAuditing(request);

        if(!response.getState().equals("Success")) {
            return "审核失败: " + response.getMessage();
        }
        return getMessage(response.getResult(), response.getLabel());
    }

    /**
     * @description 批量审核图片 URL
     * @param imageURLs 图片 URL 列表
     * @return 审核错误描述列表，为空字符串则审核通过
     */
    public List<String> batchReviewImageURL(List<String> imageURLs) {
        // 构建批量图片审核请求
        BatchImageAuditingRequest request = new BatchImageAuditingRequest();
        request.setBucketName(cos.getBucketName());
        
        // 添加请求内容
        List<BatchImageAuditingInputObject> inputList = request.getInputList();
        for (int i = 0; i < imageURLs.size(); i++) {
            BatchImageAuditingInputObject input = new BatchImageAuditingInputObject();
            input.setUrl(imageURLs.get(i));
            input.setDataId("image_" + i);
            inputList.add(input);
        }

        // 设置审核类型
        request.getConf().setDetectType("all");
        
        // 调用批量图片审核接口
        BatchImageAuditingResponse response = cos.getCosClient().batchImageAuditing(request);
        List<BatchImageJobDetail> jobList = response.getJobList();

        List<String> messages = new ArrayList<>();
        for (BatchImageJobDetail jobDetail : jobList) {
            if (!jobDetail.getState().equals("Success")) {
                messages.add("审核失败: " + jobDetail.getMessage());
            } else {
                messages.add(getMessage(jobDetail.getResult(), jobDetail.getLabel()));
            }
        }
        return messages;
    }

    /**
     * @description 通过对象键审核图片
     * @param objectKey 对象键
     * @return 审核错误描述，为空字符串则审核通过
     */
    public String reviewImageKey(String objectKey) {
        // 构建图片审核请求
        ImageAuditingRequest request = new ImageAuditingRequest();
        request.setBucketName(cos.getBucketName());
        request.setObjectKey(objectKey);

        // 调用图片审核接口
        ImageAuditingResponse response = cos.getCosClient().imageAuditing(request);

        if(!response.getState().equals("Success")) {
            return "审核失败: " + response.getMessage();
        }
        return getMessage(response.getResult(), response.getLabel());
    }

     /**
     * @description 批量审核图片对象键
     * @param objectKeys 图片对象键列表
     * @return 审核错误描述列表，为空字符串则审核通过
     */
    public List<String> batchReviewImageKey(List<String> objectKeys) {
        // 构建批量图片审核请求
        BatchImageAuditingRequest request = new BatchImageAuditingRequest();
        request.setBucketName(cos.getBucketName());
        
        // 添加请求内容
        List<BatchImageAuditingInputObject> inputList = request.getInputList();
        for (int i = 0; i < objectKeys.size(); i++) {
            BatchImageAuditingInputObject input = new BatchImageAuditingInputObject();
            input.setObject(objectKeys.get(i));
            input.setDataId("image_" + i);
            inputList.add(input);
        }
        // 设置审核类型
        request.getConf().setDetectType("all");
        
        // 调用批量图片审核接口
        BatchImageAuditingResponse response = cos.getCosClient().batchImageAuditing(request);
        List<BatchImageJobDetail> jobList = response.getJobList();

        List<String> messages = new ArrayList<>();
        for (BatchImageJobDetail jobDetail : jobList) {
            if (!jobDetail.getState().equals("Success")) {
                messages.add("审核失败: " + jobDetail.getMessage());
            } else {
                messages.add(getMessage(jobDetail.getResult(), jobDetail.getLabel()));
            }
        }
        return messages;
    }

    private String getMessage(String result, String label) {
        StringBuilder messageBuilder = new StringBuilder();
        switch (result) {
            case "0" -> { return ""; }
            case "1" -> { messageBuilder.append("包含"); }
            case "2" -> { messageBuilder.append("疑似"); }
        }
        
        switch (label) {
            case "Normal" -> { return ""; }
            case "Porn" -> { messageBuilder.append("色情"); }
            case "Ads" -> { messageBuilder.append("广告"); }
            case "Illegal" -> { messageBuilder.append("违法"); }
            case "Abuse" -> { messageBuilder.append("谩骂"); }
            case "Politics" -> { messageBuilder.append("政治"); }
            case "Terrorism" -> { messageBuilder.append("暴恐"); }
        }

        messageBuilder.append("内容");
        return messageBuilder.toString();
    }
}