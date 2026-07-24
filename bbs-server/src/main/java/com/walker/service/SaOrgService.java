package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.SaOrg;
import com.walker.vo.ResultBean;
import com.walker.vo.SaOrgTreeVO;

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
}
