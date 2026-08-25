package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.SaOrg;
import com.walker.vo.ResultBean;
import com.walker.vo.SaOrgTreeVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author chengQing
 * @Date 2026/3/3 14:46
 * @PackageName:com.walker.service
 * @ClassName: SaOrgService
 * @Description: 单位接口层
 */
public interface SaOrgService extends IService<SaOrg> {

    /**
     * 方法描述 查询全量单位并组装为树形结构
     * @author chengQing
     * @date 2026/3/3 15:54
     * @return List<SaOrgTreeVO>
     */
    List<SaOrgTreeVO> getOrgTree();

    /**
     * 方法描述 通过orgNo删除单位信息
     * @author chengQing
     * @date 2026/4/8 16:48
     * @param orgNo 单位编号
     * @return ResultBean 返回结果
     * @throws
     */
    ResultBean deleteSaOrgByOrgNo(String orgNo);

    /**
     * 方法描述 添加单位
     * @author chengQing
     * @date 2026/4/8 17:04
     * @param pOrgNo 父级单位
     * @param orgName 单位名称
     * @return ResultBean 返回封装结果
     */
    ResultBean addSaOrg(String pOrgNo, String orgName);

    /**
     * 批量更新显示选中状态
     * @param displayMap orgNo → 是否显示
     */
    void batchUpdateDisplay(Map<String, Boolean> displayMap);

    /**
     * 解析组织在用户前台的显示层级名称：
     * 沿 org_tree 路径自底向上找第一个 is_display_selected=1 的祖先，
     * 用户自身组织被隐藏时返回上一可见级名称；查不到时返回 fallbackName。
     * @param orgNo 组织编号
     * @param fallbackName 兜底名称（通常传 user.orgName）
     */
    String resolveDisplayOrgName(String orgNo, String fallbackName);

    /**
     * 批量解析显示层级名称：orgNo → 过滤后的层级名称（查不到的 orgNo 不在 map 中）
     * @param orgNos 组织编号集合
     */
    Map<String, String> resolveDisplayOrgNames(Collection<String> orgNos);
}
