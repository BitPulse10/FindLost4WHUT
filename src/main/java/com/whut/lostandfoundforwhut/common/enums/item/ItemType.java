package com.whut.lostandfoundforwhut.common.enums.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 物品类型
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ItemType {
    LOST(0, "挂失"),
    FOUND(1, "招领"),
    CARD(2, "卡证");

    private Integer code;
    private String desc;

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (ItemType itemType : values()) {
            if (itemType.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
