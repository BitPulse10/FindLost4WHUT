package com.whut.lostandfoundforwhut.common.utils.cos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qcloud.cos.model.ciModel.auditing.AuditingJobsDetail;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingResponse;

import java.util.Base64;

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

        StringBuilder messageBuilder = new StringBuilder();
        switch (jobDetail.getResult()) {
            case "0" -> { return ""; }
            case "1" -> { messageBuilder.append("包含"); }
            case "2" -> { messageBuilder.append("疑似"); }
        }
        
        switch (jobDetail.getLabel()) {
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