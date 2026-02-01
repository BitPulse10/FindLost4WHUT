package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.ItemImageAddDTO;
import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.service.IItemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item-images")
public class ItemImageController {

    @Autowired
    private IItemImageService itemImageService;

    @GetMapping("/item/{itemId}")
    public Result<List<Image>> getImagesByItemId(@PathVariable Long itemId) {
        try {
            List<Image> images = itemImageService.getImagesByItemId(itemId);
            return Result.success(images);
        } catch (AppException e){
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "查询失败: "+e.getMessage());
        }
    }

    @PostMapping("/add")
    public Result<Boolean> saveItemImages(@RequestBody ItemImageAddDTO itemImageAddDTO) {
        try {
            boolean success = itemImageService.saveItemImages(itemImageAddDTO);
            if (success) {
                return Result.success(true);
            } else {
                return Result.fail(ResponseCode.UN_ERROR.getCode(), "保存失败");
            }
        } catch (AppException e){
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }
}
