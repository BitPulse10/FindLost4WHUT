package com.whut.lostandfoundforwhut.service.impl;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.image.ImageValidator;
import com.whut.lostandfoundforwhut.mapper.ImageMapper;
import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.service.IImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageService implements IImageService {

    @Value("${app.upload.image.dir}")
    private String uploadDir;

    @Autowired
    private ImageMapper imageMapper;

    @Override
    public Image uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ResponseCode.RESOURCE_NOT_FOUND.getCode(), "文件不能为空");
        }
        return uploadSingleFile(file);
    }

    @Override
    public List<Image> uploadImages(List<MultipartFile> files) {
        List<Image> responses = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return responses;
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            Image image = uploadSingleFile(file);
            responses.add(image);
        }

        return responses;
    }

    @Override
    public Image getImageById(Long id) {
        return imageMapper.selectById(id);
    }

    /**
     * 上传单个文件的私有方法
     * @param file 图片文件
     * @return 上传响应DTO
     */
    private Image uploadSingleFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文件不能为空");
        }

        // 验证文件是否为图片
        String errorMessage = ImageValidator.validateImageFile(file);
        if (errorMessage != null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), errorMessage);
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = java.util.UUID.randomUUID().toString() + extension;

            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            // 保存图片
            File filePath = new File(uploadDirFile, uniqueFilename);
            file.transferTo(filePath.toPath());

            // 保存数据库信息
            Image image = new Image();
            image.setUrl(uniqueFilename);
            imageMapper.insert(image);

            return image;
        } catch (IOException e) {
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }
}
