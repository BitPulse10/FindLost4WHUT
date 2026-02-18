package com.whut.lostandfoundforwhut.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.vo.ImageSearchVO;
import com.whut.lostandfoundforwhut.service.IImageSearchService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/image-search")
@RequiredArgsConstructor
@Tag(name = "图搜临时图片管理", description = "图搜临时图片相关接口")
public class ImageSearchController {
    private final IImageSearchService imageSearchService;

    /**
     * 上传多个图搜临时图片
     * @param entity 图搜临时图片文件列表
     * @return 图搜临时图片实体ID列表
     */
    @PostMapping("/upload")
    public Result<List<ImageSearchVO>> postMethodName(@RequestBody List<MultipartFile> files) {
        try {
        List<ImageSearchVO> imageSearchIds = imageSearchService.uploadImageSearchs(files);
            return Result.success(imageSearchIds);
        } catch (Exception e) {
            String code = e instanceof AppException ? ((AppException) e).getCode() : ResponseCode.UN_ERROR.getCode();
            return Result.fail(code, e.getMessage());
        }
    }
    
    /**
     * 根据图搜临时图片ID删除图搜临时图片
     * @param ids 图搜临时图片ID列表
     * @return 删除结果
     */
    @DeleteMapping("/images")
    public Result<Void> deleteImageSearchsByIds(@RequestBody List<Long> ids) {
        try {
            imageSearchService.deleteImageSearchsByIds(ids);
            return Result.success(null);
        } catch (Exception e) {
            String code = e instanceof AppException ? ((AppException) e).getCode() : ResponseCode.UN_ERROR.getCode();
            return Result.fail(code, e.getMessage());
        }
    }

    /**
     * 根据图搜临时图片ID查询图搜临时图片的URL
     * @param id 图搜临时图片ID
     * @return 图搜临时图片的URL
     */
    @GetMapping("/url/{id}")
    public Result<String> getImageUrlsByItemId(@PathVariable("id") Long id) {
        try {
            String url = imageSearchService.getUrl(id);
            if (url != null) {
                return Result.success(url);
            } else {
                return Result.fail(ResponseCode.RESOURCE_NOT_FOUND.getCode(), "图片不存在");
            }
        } catch (Exception e) {
            String code = e instanceof AppException ? ((AppException) e).getCode() : ResponseCode.UN_ERROR.getCode();
            return Result.fail(code, e.getMessage());
        }
    }
}
