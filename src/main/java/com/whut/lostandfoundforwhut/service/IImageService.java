package com.whut.lostandfoundforwhut.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片服务接口
 */
public interface IImageService {
    /**
     * 上传多个图片
     * @param files 图片文件列表
     * @return 图片实体ID列表
     */
    List<Long> uploadImages(List<MultipartFile> files);

    /**
     * 从图片中提取标签
     * @param file 图片文件
     * @return 标签列表
     */
    List<String> getTabs(MultipartFile file);

    /**
     * 获取图片URL
     * @param imageId 图片ID
     * @return 图片URL
     */
    String getUrlById(Long imageId);

    /**
     * 根据物品ID获取所有图片ID
     * @param itemId 物品ID
     * @return 图片ID列表
     */
    List<Long> getImageIdsByItemId(Long itemId);

    /**
     * 根据物品ID获取所有图片URL
     * @param itemId 物品ID
     * @return 图片URL列表
     */
    List<String> getUrlsByItemId(Long itemId);

    /**
     * 根据 ID 删除图片
     * @param imageIds 图片ID列表
     */
    void deleteImagesByIds(List<Long> imageIds);
}
