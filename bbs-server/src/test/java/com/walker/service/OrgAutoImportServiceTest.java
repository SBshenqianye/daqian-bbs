package com.walker.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.OrgImportBatchMapper;
import com.walker.mapper.OrgImportItemMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.OrgImportBatch;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgAutoImportServiceTest {

    @InjectMocks
    private OrgAutoImportService service;

    @Mock private OrgImportBatchMapper batchMapper;
    @Mock private OrgImportItemMapper itemMapper;
    @Mock private SaOrgMapper orgMapper;
    @Mock private UserMapper userMapper;

    @Test
    @DisplayName("表头别名：组织编码/组织名称/上级单位编号也能正确解析")
    void validate_acceptsHeaderAliases() throws Exception {
        when(batchMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            OrgImportBatch batch = invocation.getArgument(0);
            batch.setId(101);
            return 1;
        }).when(batchMapper).insert(any(OrgImportBatch.class));

        MockMultipartFile file = xlsx(
                Arrays.asList("组织编码", "组织名称", "上级单位编号", "状态", "版本"),
                Arrays.asList("5140499", "示例单位", "51101", "1", "2026"));

        ResultBean result = service.validate(file);

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getObj();
        OrgImportBatch batch = (OrgImportBatch) data.get("batch");
        assertEquals(1, batch.getTotalCount());
        assertEquals(1, batch.getAddCount());
        assertEquals(1, batch.getSafeFlag());
    }

    @Test
    @DisplayName("重复文件：相同 hash 的批次已存在时拒绝导入")
    void validate_rejectsDuplicateHash() throws Exception {
        OrgImportBatch existing = new OrgImportBatch();
        existing.setId(88);
        existing.setFileHash("same");
        when(batchMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(existing));

        MockMultipartFile file = xlsx(
                Arrays.asList("单位编号", "单位名称", "父级单位编号", "状态", "版本"),
                Arrays.asList("5140499", "示例单位", "51404", "1", "2026"));

        ResultBean result = service.validate(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("批次 #88"));
    }

    @Test
    @DisplayName("缺少必填表头：直接报错，不生成批次")
    void validate_rejectsMissingHeader() throws Exception {
        MockMultipartFile file = xlsx(
                Arrays.asList("随便一列", "随便二列"),
                Arrays.asList("a", "b"));

        ResultBean result = service.validate(file);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("表头缺少必填列"));
    }

    private MockMultipartFile xlsx(List<String> headers, List<String> values) {
        List<List<String>> head = new java.util.ArrayList<>();
        for (String h : headers) head.add(Collections.singletonList(h));
        List<List<String>> data = new java.util.ArrayList<>();
        data.add(values);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(head).sheet("组织导入").doWrite(data);
        return new MockMultipartFile("file", "org.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
}
