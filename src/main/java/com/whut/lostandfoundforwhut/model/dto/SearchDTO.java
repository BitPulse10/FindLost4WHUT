package com.whut.lostandfoundforwhut.model.dto;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDTO implements Serializable {

    private String query;
    private List<Long> imageIds;
    private Integer maxResults;

    /**
     * 生成 Redis 键
     * 使用 MD5 哈希确保相同参数生成相同的键
     */
    public String toRedisKey() {
        try {
            // 标准化参数
            String normalizedQuery = (query == null || query.isEmpty()) ? "" : query.trim();
            String imageIdsStr = (imageIds == null || imageIds.isEmpty())
                    ? ""
                    : imageIds.stream()
                            .sorted() // 排序确保顺序一致
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
            int normalizedMaxResults = (maxResults == null) ? 10 : maxResults;

            // 组合所有参数
            String combined = String.format("q=%s&imgs=%s&max=%d",
                    normalizedQuery, imageIdsStr, normalizedMaxResults);

            // 计算 MD5 哈希
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(combined.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return "similar:search:" + hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成 Redis 键失败", e);
        }
    }
}