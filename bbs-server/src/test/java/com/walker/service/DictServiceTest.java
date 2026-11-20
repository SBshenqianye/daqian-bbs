package com.walker.service;

import com.walker.mapper.DictMapper;
import com.walker.pojo.Dict;
import com.walker.service.impl.DictServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictServiceTest {

    @InjectMocks
    private DictServiceImpl dictService;

    @Mock
    private DictMapper dictMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = dictService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(dictService, dictMapper);
    }

    @Test
    @DisplayName("保存字典 → 设置创建时间并保存")
    void saveDict_setsCreateTime() {
        Dict dict = new Dict();
        dict.setDictKey("post");
        dict.setDictValue("3");
        when(dictMapper.insert(any(Dict.class))).thenReturn(1);

        boolean result = dictService.saveDict(dict);
        assertTrue(result);
        assertNotNull(dict.getCreateTime());
    }

    @Test
    @DisplayName("更新字典 → 设置更新时间并更新")
    void updateDict_setsUpdateTime() {
        Dict dict = new Dict();
        dict.setId(1);
        dict.setDictKey("post");
        dict.setDictValue("5");
        when(dictMapper.updateById(any(Dict.class))).thenReturn(1);

        boolean result = dictService.updateDict(dict);
        assertTrue(result);
        assertNotNull(dict.getUpdateTime());
    }

    @Test
    @DisplayName("按key查询值 → 返回结果")
    void getValueByKey_returnsValue() {
        when(dictMapper.selectValueByKey("post")).thenReturn("3");
        assertEquals("3", dictService.getValueByKey("post"));
    }

    @Test
    @DisplayName("按key查询值 → 不存在 → 返回null")
    void getValueByKey_notFound_returnsNull() {
        when(dictMapper.selectValueByKey("nonexistent")).thenReturn(null);
        assertNull(dictService.getValueByKey("nonexistent"));
    }

    @Test
    @Disabled("MyBatis-Plus SqlHelper requires Spring context for removeById")
    @DisplayName("删除字典 → 成功")
    void removeDictById_succeeds() {
        when(dictMapper.deleteById(1)).thenReturn(1);
        assertTrue(dictService.removeDictById(1));
    }

    @Test
    @Disabled("MyBatis-Plus SqlHelper requires Spring context for removeById")
    @DisplayName("删除字典 → 失败")
    void removeDictById_fails() {
        when(dictMapper.deleteById(999)).thenReturn(0);
        assertFalse(dictService.removeDictById(999));
    }
}
