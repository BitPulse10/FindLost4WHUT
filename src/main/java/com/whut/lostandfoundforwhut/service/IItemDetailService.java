package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.vo.ItemDetailVO;

public interface IItemDetailService {
    /**
     * 获取二级面板展示所需的物品聚合详情。
     *
     * @param itemId 物品ID
     * @return 物品详情视图对象
     */
    ItemDetailVO getItemDetailById(Long itemId);
}
