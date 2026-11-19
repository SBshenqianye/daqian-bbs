package com.walker.service;

import com.walker.mapper.SvgAssetMapper;
import com.walker.pojo.SvgAsset;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SvgAssetServiceTest {

    @InjectMocks
    private SvgAssetService service;

    @Mock private SvgAssetMapper svgAssetMapper;

    @Test
    @DisplayName("安全过滤：纯 SVG 通过，含脚本/事件外链被拒")
    void sanitize_acceptsPureSvgAndRejectsScripts() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"12\" cy=\"12\" r=\"8\"/></svg>";
        assertEquals(svg, service.sanitize(svg));

        assertNull(service.sanitize(null));
        assertNull(service.sanitize("   "));
        assertNull(service.sanitize("not a svg"));
        assertNull(service.sanitize("<svg onload=\"alert(1)\"></svg>"));
        assertNull(service.sanitize("<svg><script>alert(1)</script></svg>"));
        assertNull(service.sanitize("<svg><a href=\"http://evil.test\">x</a></svg>"));
    }

    @Test
    @DisplayName("列表：纯查询，不再写入任何素材")
    void list_isReadOnlyQuery() {
        List<SvgAsset> rows = Collections.singletonList(new SvgAsset());
        when(svgAssetMapper.selectList(any())).thenReturn(rows);

        ResultBean result = service.list(null);

        assertEquals(200, result.getCode());
        assertSame(rows, result.getObj());
        verify(svgAssetMapper, never()).insert(any());
        verify(svgAssetMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("保存：空图形被拒绝，不落库")
    void save_rejectsEmptySvg() {
        ResultBean empty = service.save(null);
        assertEquals(500, empty.getCode());

        ResultBean noSvg = service.save(new SvgAsset());
        assertEquals(500, noSvg.getCode());

        verify(svgAssetMapper, never()).insert(any());
    }

    @Test
    @DisplayName("保存：新增素材补全默认分类与时间戳")
    void save_insertFillsDefaults() {
        SvgAsset asset = new SvgAsset();
        asset.setSvg("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"8\" height=\"8\"/></svg>");

        ResultBean result = service.save(asset);

        assertEquals(200, result.getCode());
        ArgumentCaptor<SvgAsset> captor = ArgumentCaptor.forClass(SvgAsset.class);
        verify(svgAssetMapper).insert(captor.capture());
        SvgAsset saved = captor.getValue();
        assertEquals("未命名素材", saved.getAssetName());
        assertEquals("other", saved.getCategory());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("保存/删除：更新不存在的素材、缺少 ID 删除均报错")
    void saveAndDelete_reportInvalidTargets() {
        SvgAsset asset = new SvgAsset();
        asset.setId(99);
        asset.setSvg("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"8\" height=\"8\"/></svg>");
        when(svgAssetMapper.selectById(99)).thenReturn(null);

        assertEquals(500, service.save(asset).getCode());
        verify(svgAssetMapper, never()).updateById(any());

        assertEquals(500, service.delete(null).getCode());
        verify(svgAssetMapper, never()).deleteById(any(Integer.class));
    }
}
