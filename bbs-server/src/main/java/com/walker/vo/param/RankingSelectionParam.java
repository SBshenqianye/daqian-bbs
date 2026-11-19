package com.walker.vo.param;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜成员三态树保存参数。
 *
 * <p>来自 el-tree：checkedKeys=全选节点，halfCheckedKeys=部分选中节点。</p>
 */
@Data
public class RankingSelectionParam {
    private Integer categoryId;
    private List<String> checkedKeys = new ArrayList<>();
    private List<String> halfCheckedKeys = new ArrayList<>();
}
