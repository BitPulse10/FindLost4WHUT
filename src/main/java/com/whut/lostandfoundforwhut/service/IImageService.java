package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片服务接口
 */
public interface IImageService {

    /**
     * 上传单个图片
     * @param file 图片文件
     * @return 图片上传响应DTO
     */
    Image uploadImage(MultipartFile file);

    /**
     * 上传多个图片
     * @param files 图片文件列表
     * @return 图片上传响应DTO列表
     */
    List<Image> uploadImages(List<MultipartFile> files);

    /**
     * 根据 ID 查询图片
     * @param id 图片ID
     * @return 图片响应DTO
     */
    Image getImageById(Long id);
}
