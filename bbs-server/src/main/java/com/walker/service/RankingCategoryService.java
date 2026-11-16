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

    ResultBean updateOrgDescendants(Integer categoryId, String orgNo, Boolean includeDescendants);

    ResultBean removeOrg(Integer categoryId, String orgNo);

    ResultBean getOrgRank(Integer categoryId, String startTime, String endTime);
}
