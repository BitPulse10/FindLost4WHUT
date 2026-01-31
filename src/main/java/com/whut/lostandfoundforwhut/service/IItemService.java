package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.model.dto.PageQueryDTO;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品服务接口
 */
public interface IItemService {
    /**
     * 添加物品
     * 
     * @param itemDTO 物品DTO
     * @param userId  用户ID
     * @return 物品实体
     */
    Item addItem(ItemDTO itemDTO, Long userId);

    /**
     * 更新物品
     * 
     * @param itemId  物品ID
     * @param itemDTO 物品DTO
     * @param userId  用户ID
     * @return 更新后的物品实体
     */
    Item updateItem(Long itemId, ItemDTO itemDTO, Long userId);

    /**
     * 获取物品详情
     * 
     * @param itemId 物品ID
     * @return 物品实体
     */
    Item getItemById(Long itemId);

    /**
     * 筛选物品
     * 
     * @param pageQueryDTO 分页查询参数
     * @param type         物品类型（可选）
     * @param status       物品状态（可选）
     * @param keyword      关键词搜索（可选）
     * @return 分页结果
     */
    PageResultVO<Item> filterItems(PageQueryDTO pageQueryDTO, Integer type, Integer status, String keyword);

    /**
     * 下架物品
     * 
     * @param itemId 物品ID
     * @param userId 用户ID
     * @return 是否下架成功
     */
    boolean takeDownItem(Long itemId, Long userId);
}