package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.item.ItemStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.ItemMapper;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.ItemDTO;
import com.whut.lostandfoundforwhut.model.entity.Item;
import com.whut.lostandfoundforwhut.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void addItem_throwsWhenUserMissing() {
        when(userMapper.selectById(1L)).thenReturn(null);
        AppException ex = assertThrows(AppException.class, () -> itemService.addItem(new ItemDTO(), 1L));
        assertEquals(ResponseCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updateItem_throwsWhenUserMismatch() {
        Item existing = new Item();
        existing.setUserId(1L);
        when(itemMapper.selectById(10L)).thenReturn(existing);
        AppException ex = assertThrows(AppException.class, () -> itemService.updateItem(10L, new ItemDTO(), 2L));
        assertEquals(ResponseCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    void takeDownItem_throwsWhenAlreadyClosed() {
        Item existing = new Item();
        existing.setUserId(1L);
        existing.setStatus(ItemStatus.CLOSED.getCode());
        when(itemMapper.selectById(20L)).thenReturn(existing);
        AppException ex = assertThrows(AppException.class, () -> itemService.takeDownItem(20L, 1L));
        assertEquals(ResponseCode.ITEM_STATUS_INVALID.getCode(), ex.getCode());
    }
}
