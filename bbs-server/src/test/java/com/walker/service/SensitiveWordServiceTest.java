package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.SensitiveWordMapper;
import com.walker.pojo.SensitiveWord;
import com.walker.service.impl.SensitiveWordServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensitiveWordServiceTest {

    @InjectMocks
    private SensitiveWordServiceImpl sensitiveWordService;

    @Mock
    private SensitiveWordMapper sensitiveWordMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = sensitiveWordService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(sensitiveWordService, sensitiveWordMapper);
    }

    @Test
    @DisplayName("新增敏感词 → 已存在 → 返回错误")
    void addSensitiveWord_duplicate_returnsError() {
        when(sensitiveWordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ResultBean result = sensitiveWordService.addSensitiveWord("敏感词");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("新增敏感词 → 正常添加成功")
    void addSensitiveWord_new_succeeds() {
        when(sensitiveWordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sensitiveWordMapper.insert(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.addSensitiveWord("新敏感词");
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("批量导入 → 空列表 → 返回错误")
    void batchAdd_emptyList_returnsError() {
        ResultBean result = sensitiveWordService.batchAdd(Collections.emptyList());
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("批量导入 → null列表 → 返回错误")
    void batchAdd_nullList_returnsError() {
        ResultBean result = sensitiveWordService.batchAdd(null);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("删除敏感词 → 成功")
    void delSensitiveWord_succeeds() {
        when(sensitiveWordMapper.deleteById(1)).thenReturn(1);
        when(sensitiveWordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        ResultBean result = sensitiveWordService.delSensitiveWord(1);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("删除敏感词 → 失败")
    void delSensitiveWord_fails() {
        when(sensitiveWordMapper.deleteById(999)).thenReturn(0);
        ResultBean result = sensitiveWordService.delSensitiveWord(999);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("获取敏感词列表 → 返回结果")
    void getList_returnsList() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1);
        word.setKeyword("敏感");
        when(sensitiveWordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(word));

        List<SensitiveWord> result = sensitiveWordService.getList();
        assertEquals(1, result.size());
    }
}
