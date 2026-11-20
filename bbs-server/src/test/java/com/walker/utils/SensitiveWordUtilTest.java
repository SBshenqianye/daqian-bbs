package com.walker.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 敏感词遮蔽开关：只有 mask_enabled=1 的词参与打码，关闭遮蔽的词仍留在监控口径里。
 */
class SensitiveWordUtilTest {

    @AfterEach
    void tearDown() {
        // 静态状态，测试后必须清空，避免污染其他用例
        SensitiveWordUtil.updateSensitiveWords(Collections.emptyList(), Collections.emptyList());
    }

    @Test
    @DisplayName("脱敏只替换参与遮蔽的词，关闭遮蔽的词保持原样")
    void desensitize_onlyReplacesMaskableWords() {
        SensitiveWordUtil.updateSensitiveWords(
                Arrays.asList("赌博", "看看"),   // 全量监控
                Collections.singletonList("赌博")  // 只有它参与打码
        );

        assertEquals("***和看看", SensitiveWordUtil.desensitize("赌博和看看"));
    }

    @Test
    @DisplayName("关闭遮蔽的词仍保留在监控列表里，供质量检测与命中统计使用")
    void monitoredWordsStillLoaded() {
        SensitiveWordUtil.updateSensitiveWords(
                Arrays.asList("赌博", "看看"),
                Collections.singletonList("赌博")
        );

        assertTrue(SensitiveWordUtil.getSensitiveWords().contains("看看"));
        assertFalse(SensitiveWordUtil.getMaskWords().contains("看看"));
    }

    @Test
    @DisplayName("命中检测：返回去重后的严禁词，不把垃圾词当命中")
    void match_returnsStrictWordsOnly() {
        SensitiveWordUtil.updateSensitiveWords(
                Arrays.asList("赌博", "抢劫"),
                Collections.singletonList("赌博"),
                Collections.singletonList("哈哈哈")
        );

        List<String> hits = SensitiveWordUtil.match("赌博和抢劫，哈哈哈");
        assertEquals(2, hits.size());
        assertTrue(hits.contains("赌博"));
        assertTrue(hits.contains("抢劫"));
        assertFalse(hits.contains("哈哈哈"));
    }

    @Test
    @DisplayName("命中检测：没有命中时返回空列表")
    void match_noHit_returnsEmpty() {
        SensitiveWordUtil.updateSensitiveWords(Collections.singletonList("赌博"), Collections.singletonList("赌博"));
        assertTrue(SensitiveWordUtil.match("今天天气不错").isEmpty());
    }

    @Test
    @DisplayName("关闭全部遮蔽后，正文原样返回")
    void allMaskDisabled_returnsTextUnchanged() {
        SensitiveWordUtil.updateSensitiveWords(Arrays.asList("看看", "mark"), Collections.emptyList());

        assertEquals("大家看看 markdown", SensitiveWordUtil.desensitize("大家看看 markdown"));
    }
}
