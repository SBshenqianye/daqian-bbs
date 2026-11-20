package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.SensitiveWordMapper;
import com.walker.pojo.SensitiveWord;
import com.walker.service.impl.SensitiveWordServiceImpl;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        // 静态词表状态，测试后清空，避免影响其他用例
        SensitiveWordUtil.updateSensitiveWords(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
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

    @Test
    @DisplayName("关闭遮蔽 → 成功并刷新缓存")
    void updateMaskEnabled_disable_succeeds() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1);
        word.setKeyword("看看");
        word.setMaskEnabled(1);
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.updateMaskEnabled(1, 0);

        assertEquals(200, result.getCode());
        assertEquals(0, word.getMaskEnabled());
    }

    @Test
    @DisplayName("遮蔽开关 → 参数非法 → 返回错误")
    void updateMaskEnabled_invalidParam_returnsError() {
        assertEquals(500, sensitiveWordService.updateMaskEnabled(null, 1).getCode());
        assertEquals(500, sensitiveWordService.updateMaskEnabled(1, 2).getCode());
        assertEquals(500, sensitiveWordService.updateMaskEnabled(1, null).getCode());
    }

    @Test
    @DisplayName("遮蔽开关 → 词不存在 → 返回错误")
    void updateMaskEnabled_notFound_returnsError() {
        when(sensitiveWordMapper.selectById(999)).thenReturn(null);
        assertEquals(500, sensitiveWordService.updateMaskEnabled(999, 1).getCode());
    }

    @Test
    @DisplayName("修改分类 → 改为垃圾词 → 自动关闭打码")
    void updateCategory_toSpam_disablesMask() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1);
        word.setKeyword("看看");
        word.setMaskEnabled(1);
        word.setCategory("sensitive");
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.updateCategory(1, "spam");

        assertEquals(200, result.getCode());
        assertEquals("spam", word.getCategory());
        assertEquals(0, word.getMaskEnabled());
    }

    @Test
    @DisplayName("修改分类 → 改回严禁词 → 默认打码")
    void updateCategory_toSensitive_enablesMask() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1);
        word.setKeyword("赌博");
        word.setMaskEnabled(0);
        word.setCategory("spam");
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.updateCategory(1, "sensitive");

        assertEquals(200, result.getCode());
        assertEquals("sensitive", word.getCategory());
        assertEquals(1, word.getMaskEnabled());
    }

    @Test
    @DisplayName("修改分类 → 非法值 → 返回错误")
    void updateCategory_invalid_returnsError() {
        assertEquals(500, sensitiveWordService.updateCategory(1, "other").getCode());
    }

    @Test
    @DisplayName("垃圾词不允许开启打码")
    void updateMaskEnabled_spamWord_returnsError() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1);
        word.setKeyword("看看");
        word.setCategory("spam");
        word.setMaskEnabled(0);
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);

        ResultBean result = sensitiveWordService.updateMaskEnabled(1, 1);

        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("三档处置 → 打码 → 严禁词且开启遮蔽")
    void updateAction_mask_setsSensitiveAndMasked() {
        SensitiveWord word = sensitiveWord();
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.updateAction(1, "mask");

        assertEquals(200, result.getCode());
        assertEquals("sensitive", word.getCategory());
        assertEquals(1, word.getMaskEnabled());
    }

    @Test
    @DisplayName("三档处置 → 仅监控 → 严禁词但不打码")
    void updateAction_monitor_setsSensitiveAndUnmasked() {
        SensitiveWord word = sensitiveWord();
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.updateAction(1, "monitor");

        assertEquals(200, result.getCode());
        assertEquals("sensitive", word.getCategory());
        assertEquals(0, word.getMaskEnabled());
    }

    @Test
    @DisplayName("三档处置 → 警告放行 → 垃圾词且不打码")
    void updateAction_warn_setsSpam() {
        SensitiveWord word = sensitiveWord();
        when(sensitiveWordMapper.selectById(1)).thenReturn(word);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);
        when(sensitiveWordMapper.selectList(any())).thenReturn(new ArrayList<>());

        ResultBean result = sensitiveWordService.updateAction(1, "warn");

        assertEquals(200, result.getCode());
        assertEquals("spam", word.getCategory());
        assertEquals(0, word.getMaskEnabled());
    }

    @Test
    @DisplayName("三档处置 → 非法动作 → 返回错误")
    void updateAction_invalid_returnsError() {
        assertEquals(500, sensitiveWordService.updateAction(1, "block").getCode());
        assertEquals(500, sensitiveWordService.updateAction(null, "mask").getCode());
    }

    private SensitiveWord sensitiveWord() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1);
        word.setKeyword("赌博");
        word.setMaskEnabled(1);
        word.setCategory("sensitive");
        return word;
    }

    @Test
    @DisplayName("启动加载 → 按 mask_enabled 拆分遮蔽词与监控词")
    void loadSensitiveWords_splitsMaskableWords() {
        SensitiveWord masked = new SensitiveWord();
        masked.setKeyword("赌博");
        masked.setMaskEnabled(1);
        SensitiveWord monitored = new SensitiveWord();
        monitored.setKeyword("看看");
        monitored.setMaskEnabled(0);
        // 历史数据：字段为空时按开启遮蔽处理
        SensitiveWord legacy = new SensitiveWord();
        legacy.setKeyword("抢劫");
        legacy.setMaskEnabled(null);
        // 垃圾词：不参与严禁词监控，走警告放行
        SensitiveWord spam = new SensitiveWord();
        spam.setKeyword("哈哈哈");
        spam.setMaskEnabled(0);
        spam.setCategory("spam");
        when(sensitiveWordMapper.selectList(any()))
                .thenReturn(Arrays.asList(masked, monitored, legacy, spam));

        sensitiveWordService.loadSensitiveWords();

        // 严禁词 3 个（含 mask_enabled=0 的"看看"），垃圾词单独成表
        assertEquals(3, SensitiveWordUtil.getSensitiveWords().size());
        List<String> maskWords = SensitiveWordUtil.getMaskWords();
        assertEquals(2, maskWords.size());
        assertTrue(maskWords.contains("赌博"));
        assertTrue(maskWords.contains("抢劫"));
        assertFalse(maskWords.contains("看看"));
        assertEquals(1, SensitiveWordUtil.getSpamWords().size());
        assertEquals("哈哈哈", SensitiveWordUtil.getSpamWords().get(0));
    }
}
