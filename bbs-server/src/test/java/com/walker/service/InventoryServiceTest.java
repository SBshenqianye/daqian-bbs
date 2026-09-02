package com.walker.service;

import com.walker.mapper.InventoryMapper;
import com.walker.pojo.Inventory;
import com.walker.service.impl.InventoryServiceImpl;
import com.walker.vo.param.InventoryParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Mock
    private InventoryMapper inventoryMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = inventoryService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(inventoryService, inventoryMapper);
    }

    // ==================== getAllInventory ====================

    @Test
    @DisplayName("查询库存 → 无筛选条件 → 返回全部")
    void getAllInventory_noFilter() {
        InventoryParam param = new InventoryParam();
        when(inventoryMapper.selectList(null)).thenReturn(Collections.emptyList());

        List<Inventory> result = inventoryService.getAllInventory(param);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("查询库存 → 按地区筛选 → 返回匹配结果")
    void getAllInventory_withArea() {
        InventoryParam param = new InventoryParam();
        param.setArea("内江");
        Inventory inv = new Inventory();
        inv.setArea("内江");
        when(inventoryMapper.selectList(any())).thenReturn(Arrays.asList(inv));

        List<Inventory> result = inventoryService.getAllInventory(param);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("查询库存 → 按关键词筛选")
    void getAllInventory_withKeywords() {
        InventoryParam param = new InventoryParam();
        param.setKeywords("测试");
        when(inventoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Inventory> result = inventoryService.getAllInventory(param);

        assertNotNull(result);
    }

    @Test
    @DisplayName("查询库存 → 多个筛选条件")
    void getAllInventory_multipleFilters() {
        InventoryParam param = new InventoryParam();
        param.setArea("内江");
        param.setType("类型");
        when(inventoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Inventory> result = inventoryService.getAllInventory(param);

        assertNotNull(result);
    }

    // ==================== getInventoryById ====================

    @Test
    @DisplayName("按ID查询库存 → 存在 → 返回记录")
    void getInventoryById_found() {
        Inventory inv = new Inventory();
        inv.setId(1);
        when(inventoryMapper.selectById(1)).thenReturn(inv);

        Inventory result = inventoryService.getInventoryById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("按ID查询库存 → 不存在 → 返回null")
    void getInventoryById_notFound() {
        when(inventoryMapper.selectById(999)).thenReturn(null);

        Inventory result = inventoryService.getInventoryById(999);

        assertNull(result);
    }
}
