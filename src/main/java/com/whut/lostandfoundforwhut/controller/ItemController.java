package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemFilterDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.vo.ItemDetailVO;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.IImageService;
import com.whut.lostandfoundforwhut.service.IItemDetailService;
import com.whut.lostandfoundforwhut.service.IItemService;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.whut.lostandfoundforwhut.service.IUserService;
import org.springframework.web.bind.annotation.*;

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
@Slf4j
public class ItemController {

    private final IItemService itemService;
    private final IUserService userService;
    private final IImageService imageService;
    private final IItemDetailService itemDetailService;

    @PostMapping("/add-item")
    @Operation(summary = "添加物品", description = "添加新的挂失或招领物品 ，返回物品ID")
    public Result<Item> addItem(@RequestBody ItemDTO itemDTO) {
        try {
            Long userId = userService.getCurrentUserId();
            Item item = itemService.addItem(itemDTO, userId);
            System.out.println("成功创建物品，ID：" + item.getId());

            return Result.success(item);
        } catch (Exception e) {
            // 捕获异常后，删除上传的图片
            if (itemDTO.getImageIds() != null) {
                List<Long> imageIds = itemDTO.getImageIds();
                imageService.deleteImagesByIds(imageIds);
            }

            // 处理异常
            if (e instanceof AppException) {
                AppException appException = (AppException) e;
                System.out.println("添加物品时发生业务异常：" + e.getMessage() + "，错误码：" + appException.getCode());
                return Result.fail(appException.getCode(), appException.getInfo());
            } else {
                System.out.println("添加物品时发生未知异常：" + e.getMessage());
                e.printStackTrace();
                return Result.fail(ResponseCode.UN_ERROR.getCode(), "添加物品失败：" + e.getMessage());
            }
        }
    }

    @PutMapping("/update-item")
    @Operation(summary = "更新物品", description = "通过查询参数更新物品淇℃伅")
    public Result<Item> updateItemByQuery(
            @Parameter(description = "Item ID", required = true) @RequestParam Long itemId,
            @RequestBody ItemDTO itemDTO) {
        try {
            Long userId = userService.getCurrentUserId();
            Item updatedItem = itemService.updateItem(itemId, itemDTO, userId);

            return Result.success(updatedItem);
        } catch (Exception e) {
            if (e instanceof AppException) {
                AppException appException = (AppException) e;
                System.out.println("更新物品时发生业务异常：" + e.getMessage() + "，错误码：" + appException.getCode());
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
            @Parameter(description = "Item ID", required = true) @RequestParam Long itemId) {
        try {
            Long userId = userService.getCurrentUserId();
            boolean success = itemService.takeDownItem(itemId, userId);

            return Result.success(success);
        } catch (AppException e) {
            System.out.println("下架物品时发生业务异常：" + e.getMessage() + "，错误码：" + e.getCode());
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
            System.out.println("筛选物品时发生业务异常：" + e.getMessage() + "，错误码：" + e.getCode());
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            System.out.println("筛选物品时发生未知异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "筛选物品失败：" + e.getMessage());
        }
    }

    @GetMapping("/me")
    @Operation(summary = "查询我的物品", description = "查询当前登录用户发布的物品，支持按类型、关键词筛选与分页")
    public Result<PageResultVO<Item>> listMyItems(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页数量，最大100") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "物品类型：0-挂失，1-招领，2-卡证") @RequestParam(required = false) Integer type,
            @Parameter(description = "关键词（匹配描述和地点）") @RequestParam(required = false) String keyword) {
        try {
            Long userId = userService.getCurrentUserId();
            PageResultVO<Item> result = itemService.listMyItems(userId, pageNo, pageSize, type, keyword);
            return Result.success(result);
        } catch (AppException e) {
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("查询我的物品失败，pageNo={}, pageSize={}, type={}, keyword={}", pageNo, pageSize, type, keyword, e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "查询我的物品失败：" + e.getMessage());
        }
    }

    @GetMapping("/{itemId}/detail")
    @Operation(summary = "获取物品详情", description = "返回物品图片、标签、发现时间、发现地点、描述等聚合信息")
    public Result<ItemDetailVO> getItemDetailById(
            @Parameter(description = "Item ID", required = true) @PathVariable Long itemId) {
        try {
            ItemDetailVO detail = itemDetailService.getItemDetailById(itemId);
            return Result.success(detail);
        } catch (AppException e) {
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("获取物品详情失败，itemId={}", itemId, e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "获取物品详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{ItemId:\\d+}")
    @Operation(summary = "获取物品", description = "通过物品ID获取物品信息")
    public Result<Item> getItemById(
            @Parameter(description = "Item ID", required = true) @PathVariable Long ItemId) {
        Item item = itemService.getById(ItemId);
        if (item == null) {
            return Result.fail(ResponseCode.ITEM_NOT_FOUND);
        }
        return Result.success(item);
    }

    @DeleteMapping("/images")
    @Operation(summary = "删除图片", description = "根据图片ID列表删除图片")
    public Result<Boolean> deleteImages(
            @Parameter(description = "图片ID列表", required = true) @RequestBody List<Long> imageIds) {
        try {
            // TODO(上线前删除): 该接口当前仅用于联调测试，存在越权删除风险，生产发布前必须移除。
            log.warn("测试接口 deleteImages 被调用，请在生产发布前删除该接口");
            imageService.deleteImagesByIds(imageIds);
            return Result.success(true);
        } catch (AppException e) {
            log.warn("删除图片时发生业务异常：{}", e.getMessage());
            return Result.fail(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("删除图片时发生未知异常：" + e.getMessage());
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "删除图片失败：" + e.getMessage());
        }
    }

    @GetMapping("/search-similar")
    @Operation(summary = "搜索相似物品", description = "在向量数据库中搜索与查询文本相似的物品")
    public Result<List<Item>> searchSimilarItems(
            @Parameter(description = "查询文本", required = false) @RequestParam(required = false) String query,
            @Parameter(description = "返回结果数量", required = false, example = "5") @RequestParam(defaultValue = "5") int maxResults,
            @Parameter(description = "图片ID", required = false) @RequestParam(required = false) List<Long> imageIds) {
        try {
            List<Item> results = itemService.searchSimilarItems(query, imageIds, maxResults);
            log.info("搜索相似物品完成，查询：{}，返回结果数量：{}", query, results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索相似物品时发生未知异常：" + e.getMessage());
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "搜索相似物品失败：" + e.getMessage());
        }
    }
}
