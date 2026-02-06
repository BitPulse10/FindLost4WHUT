package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * 图片服务接口
 */
public interface IImageService {
    /**
     * @description 获取图片上传目录
     * @return 图片上传目录
     */
    String getUploadDir();

    /**
     * 上传多个图片
     * @param files 图片文件列表
     * @return 图片实体列表
     */
    List<Image> uploadImages(List<MultipartFile> files);

    /**
     * 从图片中提取标签
     * @param file 图片文件
     * @return 标签列表
     */
    List<String> getTabs(MultipartFile file);

    /**
     * 根据 ID 查询图片
     * @param id 图片ID
     * @return 图片响应DTO
     */
    Image getImageById(Long id);

    /**
     * 根据 ID 查询图片文件
     * @param id 图片ID
     * @return 图片文件
     */
    File getImageFileById(Long id);

    /**
     * 删除图片和关联的文件
     * @param imageIds 图片ID列表
     */
    void deleteImagesAndFiles(List<Long> imageIds);
}
