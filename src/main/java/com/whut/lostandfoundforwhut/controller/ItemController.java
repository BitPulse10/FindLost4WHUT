package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemFilterDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IImageService;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import com.whut.lostandfoundforwhut.service.IUserService;
import com.whut.lostandfoundforwhut.mapper.ItemImageMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品控制器
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "物品管理", description = "物品相关接口")
@Slf4j
public class ItemController {

    private final IItemService itemService;
    private final JwtUtil jwtUtil;
    private final IUserService userService;
    private final IImageService imageService;
    private final ItemImageMapper itemImageMapper;

    @PostMapping("/add-item")
    @Operation(summary = "添加物品", description = "添加新的挂失或招领物品")
    public Result<Item> addItem(
            @Parameter(description = "Bearer token", required = true) @RequestHeader(value = "Authorization") String authorization,
            @RequestBody ItemDTO itemDTO) {
        try {
            Long userId = resolveUserIdFromToken(authorization);
            Item item = itemService.addItem(itemDTO, userId);
            System.out.println("成功创建物品，ID：" + item.getId());

            return Result.success(item);
        } catch (Exception e) {
            // 捕获异常后，删除上传的图片
            // if (itemDTO.getImageId() != null) {
            // List<Long> imageIds = Arrays.asList(itemDTO.getImageId());
            // imageService.deleteImagesAndFiles(imageIds);
            // }

            // 处理业务异常
            if (e instanceof AppException) {
                AppException appException = (AppException) e;
                System.out.println("添加物品时发生业务异常：" + e.getMessage());
                return Result.fail(appException.getCode(), appException.getInfo());
            } else {
                System.out.println("添加物品时发生未知异常：" + e.getMessage());
                e.printStackTrace();
                return Result.fail(ResponseCode.UN_ERROR.getCode(), "添加物品失败：" + e.getMessage());
            }
        }
    }

    @PutMapping("/update-item")
    @Operation(summary = "更新物品", description = "通过查询参数更新物品信息")
    public Result<Item> updateItemByQuery(
            @Parameter(description = "Item ID", required = true) @RequestParam Long itemId,
            @Parameter(description = "Bearer token", required = true) @RequestHeader(value = "Authorization") String authorization,
            @RequestBody ItemDTO itemDTO) {
        try {
            Long userId = resolveUserIdFromToken(authorization);
            Item updatedItem = itemService.updateItem(itemId, itemDTO, userId);

            return Result.success(updatedItem);
        } catch (Exception e) {
            // 捕获异常后，删除要删除的图片
            String updateImageIdStr = itemDTO.getImageId();
            Long updateImageId = null;
            if (updateImageIdStr != null && !updateImageIdStr.isEmpty()) {
                try {
                    updateImageId = Long.parseLong(updateImageIdStr);
                } catch (NumberFormatException ex) {
                    log.warn("图片ID格式错误: {}", updateImageIdStr);
                }
            }
            List<Long> oldImageIds = Optional.ofNullable(itemImageMapper.getImageIdsByItemId(itemId))
                    .orElse(new ArrayList<>()); // 获取旧图片实体列表
            List<Long> deleteImageIds = new ArrayList<>();

            // 如果有旧图片但新图片ID不同，则删除旧图片
            if (!oldImageIds.isEmpty() && (updateImageId == null || !oldImageIds.contains(updateImageId))) {
                deleteImageIds.addAll(oldImageIds);
            }
            if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
                imageService.deleteImagesAndFiles(deleteImageIds);
            }

            // 处理业务异常
            if (e instanceof AppException) {
                AppException appException = (AppException) e;
                System.out.println("更新物品时发生业务异常：" + e.getMessage());
                return Result.fail(appException.getCode(), appException.getInfo());
            } else {
                System.out.println("更新物品时发生未知异常：" + e.getMessage());
                e.printStackTrace();
                return Result.fail(ResponseCode.UN_ERROR.getCode(), "更新物品失败：" + e.getMessage());
            }
        }
    }

    @PutMapping("/take-down")
    @Operation(summary = "下架物品", description = "通过查询参数下架物品")
    public Result<Boolean> takeDownItemByQuery(
            @Parameter(description = "Item ID", required = true) @RequestParam Long itemId,
            @Parameter(description = "Bearer token", required = true) @RequestHeader(value = "Authorization") String authorization) {
        try {
            Long userId = resolveUserIdFromToken(authorization);
            boolean success = itemService.takeDownItem(itemId, userId);

            return Result.success(success);
        } catch (AppException e) {
            System.out.println("下架物品时发生业务异常：" + e.getMessage());
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            System.out.println("下架物品时发生未知异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "下架物品失败：" + e.getMessage());
        }
    }

    @PostMapping("/filter")
    @Operation(summary = "筛选物品", description = "按类型、状态、标签或时间段筛选物品")
    public Result<PageResultVO<Item>> filterItems(@RequestBody ItemFilterDTO itemFilterDTO) {
        try {
            PageResultVO<Item> result = itemService.filterItems(itemFilterDTO);
            return Result.success(result);
        } catch (AppException e) {
            System.out.println("筛选物品时发生业务异常：" + e.getMessage());
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            System.out.println("筛选物品时发生未知异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "筛选物品失败：" + e.getMessage());
        }
    }

    @GetMapping("/{ItemId}")
    @Operation(summary = "获取物品", description = "通过物品ID获取物品信息")
    public Result<Item> getItemById(
            @Parameter(description = "Item ID", required = true) @PathVariable Long ItemId) {
        try {
            Item item = itemService.getItemById(ItemId);
            return Result.success(item);
        } catch (AppException e) {
            System.out.println("获取物品时发生业务异常：" + e.getMessage());
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            System.out.println("获取物品时发生未知异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "获取物品失败：" + e.getMessage());
        }
    }

    @GetMapping("/search-similar")
    @Operation(summary = "搜索相似物品", description = "在向量数据库中搜索与查询文本相似的物品")
    public Result<List<Item>> searchSimilarItems(
            @Parameter(description = "查询文本", required = true) @RequestParam String query,
            @Parameter(description = "返回结果数量", required = false, example = "5") @RequestParam(defaultValue = "5") int maxResults) {
        try {
            List<Item> results = itemService.searchSimilarItems(query, maxResults);
            log.info("搜索相似物品完成，查询：{}，返回结果数量：{}", query, results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索相似物品失败，查询：{}", query, e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "搜索相似物品失败：" + e.getMessage());
        }
    }

    private Long resolveUserIdFromToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        String token = authorization.substring(7);
        String email = jwtUtil.getEmail(token);
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        // resolve user id by email via service
        return userService.getUserIdByEmail(email);
    }
}