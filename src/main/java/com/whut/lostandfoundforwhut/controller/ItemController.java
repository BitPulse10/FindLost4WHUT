package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.PageQueryDTO;
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
            @Parameter(description = "Bearer token", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ItemDTO itemDTO) {
        Long userId = resolveUserId(authorization, itemDTO.getEmail());
        Item item = itemService.addItem(itemDTO, userId);
        return Result.success(item);
    }

    @PutMapping("/update-item")
    @Operation(summary = "更新物品", description = "通过查询参数更新物品信息")
    public Result<Item> updateItemByQuery(
            @Parameter(description = "Item ID", required = true)
            @RequestParam Long itemId,
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader(value = "Authorization") String authorization,
            @RequestBody ItemDTO itemDTO) {
        Long userId = resolveUserId(authorization, itemDTO.getEmail());
        Item updatedItem = itemService.updateItem(itemId, itemDTO, userId);
        return Result.success(updatedItem);
    }

    @PutMapping("/take-down")
    @Operation(summary = "下架物品", description = "通过查询参数下架物品")
    public Result<Boolean> takeDownItemByQuery(
            @Parameter(description = "Item ID", required = true)
            @RequestParam Long itemId,
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader(value = "Authorization") String authorization) {
        Long userId = resolveUserId(authorization, null);
        boolean success = itemService.takeDownItem(itemId, userId);
        return Result.success(success);
    }

    @GetMapping("/filter")
    @Operation(summary = "筛选物品", description = "按类型、状态或关键字筛选物品")
    public Result<PageResultVO<Item>> filterItems(
            PageQueryDTO pageQueryDTO,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageResultVO<Item> result = itemService.filterItems(pageQueryDTO, type, status, keyword);
        return Result.success(result);
    }

    private Long resolveUserId(String authorization, String emailFallback) {
        String email = null;
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            email = jwtUtil.getEmail(token);
        }
        if (!StringUtils.hasText(email)) {
            email = emailFallback;
        }
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        // resolve user id by email via service
        return userService.getUserIdByEmail(email);
    }
}

