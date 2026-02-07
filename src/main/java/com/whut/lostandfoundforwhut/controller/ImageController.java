package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.service.IImageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private IImageService imageService;

    /**
     * 上传多张图片
     * @param files 图片文件列表，文件类型仅支持为jpg、png、gif
     * @return 图片实体列表
     */
    @PostMapping("/upload")
    public Result<List<Image>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<Image> images = imageService.uploadImages(files);
            return Result.success(images);
        } catch (Exception e) {
            String code = e instanceof AppException ? ((AppException) e).getCode() : ResponseCode.UN_ERROR.getCode();
            return Result.fail(code, e.getMessage());
        }
    }

    /**
     * 识别图片中的标签
     * @param file 图片文件，文件类型仅支持为jpg、png、gif
     * @return 图片中的标签列表
     */
    @PostMapping("/recognize/tabs")
    public Result<List<String>> getTabs(@RequestParam("file") MultipartFile file) {
        try {
            List<String> tabs = imageService.getTabs(file);
            return Result.success(tabs);
        } catch (Exception e) {
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }

    /**
     * 根据图片ID查询图片信息
     * @param id 图片ID
     * @return 图片实体
     */
    @GetMapping("/{id}")
    public Result<Image> getImageById(@PathVariable Long id) {
        try {
            Image image = imageService.getImageById(id);
            if (image != null) {
                return Result.success(image);
            } else {
                return Result.fail(ResponseCode.RESOURCE_NOT_FOUND.getCode(), "图片不存在");
            }
        } catch (AppException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "查询失败: "+e.getMessage());
        }
    }
}
