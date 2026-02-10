package com.whut.lostandfoundforwhut.common.utils.cos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qcloud.cos.model.ciModel.image.ImageLabelRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.Label;

@Component
public class ContentRecognizer {
    private final COS cos;

    @Autowired
    public ContentRecognizer(COS cos) { this.cos = cos; }

    public List<Label> getLabels(String objectKey, int minConfidence) {
        // 内容识别标签请求
        ImageLabelRequest request = new ImageLabelRequest();
        request.setBucketName(cos.getBucketName());
        request.setObjectKey(objectKey);
        // 内容识别标签响应
        ImageLabelResponse response = cos.getCosClient().getImageLabel(request);
        // 内容识别标签结果
        List<Label> labels = response.getRecognitionResult();
        return labels.stream()
            .filter(label -> Integer.parseInt(label.getConfidence()) >= minConfidence)
            .toList();
    }

    public List<String> getNames(String objectKey, int minConfidence) {
        return getLabels(objectKey, minConfidence)
            .stream()
            .map(Label::getName)
            .toList();
    }

    public List<String> getCategoriesAndNames(String objectKey, int minConfidence) {
        List<Label> labels = getLabels(objectKey, minConfidence);
        List<String> categoriesAndNames = new ArrayList<>();
        for (Label label : labels) {
            categoriesAndNames.add(label.getFirstCategory());
            categoriesAndNames.add(label.getSecondCategory());
            categoriesAndNames.add(label.getName());
        }
        return categoriesAndNames.stream().distinct().toList();
    }
}
