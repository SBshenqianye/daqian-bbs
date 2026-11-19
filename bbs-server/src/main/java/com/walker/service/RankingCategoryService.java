package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.RankingCategory;
import com.walker.vo.ResultBean;

/**
 * 排行榜分类与组织榜服务。
 */
public interface RankingCategoryService extends IService<RankingCategory> {

    ResultBean listPublicCategories();

    ResultBean listAdminCategories();

    ResultBean saveCategory(RankingCategory category);

    ResultBean deleteCategory(Integer id);

    ResultBean addOrg(Integer categoryId, String orgNo, Boolean includeDescendants);

    ResultBean saveSelection(Integer categoryId, java.util.List<String> checkedKeys,
                             java.util.List<String> halfCheckedKeys);

    ResultBean updateOrgDescendants(Integer categoryId, String orgNo, Boolean includeDescendants);

    ResultBean removeOrg(Integer categoryId, String orgNo);

    ResultBean getOrgRank(Integer categoryId, String startTime, String endTime);

    /** 恢复内置分类的默认名称、说明、排序与参与单位；自定义分类不支持。 */
    ResultBean resetCategoryToDefault(Integer categoryId);

    /** 恢复内置分类的默认参与单位，返回可作为榜单根的 orgNo 列表（前端预览后再保存）。 */
    ResultBean getDefaultMembers(Integer categoryId);

    /** 拖动排序：按传入顺序批量写入 sortOrder。 */
    ResultBean updateSortOrders(java.util.List<com.walker.vo.param.RankingSortItem> items);

    /** 返回分类实际参与统计的组织编号集合（用于榜单钻取/导出复用同一口径）。 */
    java.util.List<String> listAllowedOrgNos(Integer categoryId);
}
