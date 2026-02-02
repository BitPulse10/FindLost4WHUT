package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemFilter;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import com.whut.lostandfoundforwhut.service.IUserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品控制器
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "物品管理", description = "物品相关接口")
public class ItemController {

    private final IItemService itemService;
    private final JwtUtil jwtUtil;
    private final IUserService userService;

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
            System.out.println("添加物品时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
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
            System.out.println("更新物品时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
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
        } catch (Exception e) {
            System.out.println("下架物品时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }

    @PostMapping("/filter")
    @Operation(summary = "筛选物品", description = "按类型、状态、标签或时间段筛选物品")
    public Result<PageResultVO<Item>> filterItems(@RequestBody ItemFilter ItemFilterDTO) {
        try {
            PageResultVO<Item> result = itemService.filterItems(ItemFilterDTO);
            return Result.success(result);
        } catch (Exception e) {
            System.out.println("筛选物品时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }

    @GetMapping("/{ItemId}")
    @Operation(summary = "获取物品", description = "通过物品ID获取物品信息")
    public Result<Item> getItemById(
            @Parameter(description = "Item ID", required = true) @PathVariable Long ItemId) {
        try {
            return Result.success(itemService.getItemById(ItemId));
        } catch (Exception e) {
            System.out.println("获取物品时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
        }
    }

    @GetMapping("/search-similar")
    @Operation(summary = "搜索相似物品", description = "通过向量搜索查找相似的物品")
    public Result<List<Item>> searchSimilarItems(
            @Parameter(description = "查询文本", required = true) @RequestParam String query,
            @Parameter(description = "最大返回结果数", required = false) @RequestParam(defaultValue = "10") int maxResults,
            @Parameter(description = "状态筛选", required = false) @RequestParam(required = false) Integer statusFilter) {
        try {
            List<Item> similarItems = itemService.searchSimilarItems(query, maxResults, statusFilter);
            return Result.success(similarItems);
        } catch (Exception e) {
            System.out.println("搜索相似物品时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), e.getMessage());
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