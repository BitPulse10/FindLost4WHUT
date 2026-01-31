package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.PageQueryDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品控制器
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "物品管理", description = "物品相关操作接口")
public class ItemController {

    private final IItemService itemService;
    private final JwtUtil jwtUtil;

    @PostMapping("/add-item")
    @Operation(summary = "添加物品", description = "添加新的挂失或招领物品")
    public Result<Item> addItem(@RequestBody ItemDTO itemDTO) {
        System.out.println("itemDTO: " + itemDTO);
        // 通过邮箱获取用户ID
        // Long userId = getUserIdByEmail(email);
        long userId = 123;
        // 验证用户是否存在
        // User user = userMapper.selectById(userId);
        // if (user == null) {
        // throw new RuntimeException("用户不存在");
        // }
        Item item = itemService.addItem(itemDTO, userId);

        return Result.success(item);
    }

    @PutMapping("/update-item")
    @Operation(summary = "更新物品", description = "通过查询参数更新物品信息")
    public Result<Item> updateItemByQuery(@RequestParam Long itemId, @RequestBody ItemDTO itemDTO) {
        long userId = 123; // 临时使用固定用户ID
        System.out.println("itemDTO: " + itemDTO);
        Item updatedItem = itemService.updateItem(itemId, itemDTO, userId);
        return Result.success(updatedItem);
    }

    @PutMapping("/take-down")
    @Operation(summary = "下架物品", description = "通过查询参数下架物品")
    public Result<Boolean> takeDownItemByQuery(@RequestParam Long itemId) {
        long userId = 123; // 临时使用固定用户ID
        boolean success = itemService.takeDownItem(itemId, userId);
        return Result.success(success);
    }

    @GetMapping("/filter")
    @Operation(summary = "筛选物品", description = "支持按类型、状态、关键词筛选，可单独一个条件也可以多个条件组合")
    public Result<PageResultVO<Item>> filterItems(
            PageQueryDTO pageQueryDTO,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageResultVO<Item> result = itemService.filterItems(pageQueryDTO, type, status, keyword);
        return Result.success(result);
    }
}