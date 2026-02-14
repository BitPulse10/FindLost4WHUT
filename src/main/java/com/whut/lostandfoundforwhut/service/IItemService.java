package com.whut.lostandfoundforwhut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.dto.ItemFilterDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import java.util.List;

/**
 * @author Qoder
 * @date 2026/01/31
 * @description 物品服务接口
 */
public interface IItemService extends IService<Item> {

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
     * 筛选物品
     *
     * @param ItemFilterDTO 筛选参数（包含分页和筛选条件）
     * @return 分页结果
     */
    PageResultVO<Item> filterItems(ItemFilterDTO itemFilterDTO);

    /**
     * 下架物品
     *
     * @param itemId 物品ID
     * @param userId 用户ID
     * @return 是否下架成功
     */
    boolean takeDownItem(Long itemId, Long userId);

    /***
     * 根据状态筛选物品
     * 
     * @param itemIds 物品ID列表
     * @param status  状态
     * @return 符合状态的物品列表
     */
    List<Item> filterItemsByStatus(List<Long> itemIds, String status);

    /**
     * 搜索相似物品
     *
     * @param query      查询文本
     * @param imageIds   图片ID列表
     * @param maxResults 最大返回结果数
     * @return 相似的物品列表
     */
    List<Item> searchSimilarItems(String query, List<Long> imageIds, int maxResults);
}
