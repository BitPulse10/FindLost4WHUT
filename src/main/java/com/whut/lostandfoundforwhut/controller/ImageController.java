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

    @PostMapping("/upload")
    public Result<Image> upload(@RequestParam("file") MultipartFile file) {
        try {
            Image image = imageService.uploadImage(file);
            return Result.success(image);
        } catch (AppException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }

    @PostMapping("/upload/multiple")
    public Result<List<Image>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<Image> responses = imageService.uploadImages(files);
            return Result.success(responses);
        } catch (AppException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }

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
