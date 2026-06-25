package com.walker.controller;

import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.SaOrg;
import com.walker.service.SaOrgService;
import com.walker.vo.ResultBean;
import com.walker.vo.SaOrgTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author chengQing
 * @Date 2026/3/3 14:45
 * @PackageName:com.walker.controller
 * @ClassName: SaOrgController
 * @Description: 单位控制层
 */
@Api(tags = "SaOrgController")
@RestController
public class SaOrgController {

    @Autowired
    private SaOrgService saOrgService;

    @Autowired
    private SaOrgMapper saOrgMapper;
    /**
     * 查询单位树形结构
     */
    @ApiOperation("查询单位树形结构")
    @GetMapping("/common/saOrgTree")
    public ResultBean getSaOrgTree() {
        return ResultBean.success("成功获取！",saOrgService.getOrgTree());
    }

    /**
     * 方法描述 删除单位
     * @author chengQing
     * @date 2026/4/8 17:02
     * @param orgNo 单位编号
     * @return ResultBean 返回封装结果
     */
    @ApiOperation("删除单位")
    @GetMapping("/saOrg/deleteSaOrgByOrgNo")
    public ResultBean deleteSaOrgByOrgNo(@RequestParam("orgNo") String orgNo) {
        return saOrgService.deleteSaOrgByOrgNo(orgNo);
    }

    /**
     * 方法描述 添加单位
     * @author chengQing
     * @date 2026/4/8 17:04
     * @param pOrgNo 父级单位
     * @param orgName 单位名称
     * @return ResultBean 返回封装结果
     */
    @ApiOperation("添加单位")
    @GetMapping("/saOrg/addSaOrg")
    public ResultBean addSaOrg(@RequestParam("pOrgNo") String pOrgNo, @RequestParam("orgName") String orgName) {
        return saOrgService.addSaOrg(pOrgNo, orgName);
    }

    @ApiOperation("获取所有单位及排名选中状态")
    @GetMapping("/common/saOrg/rankingOrgs")
    public ResultBean getRankingOrgs() {
        List<SaOrg> allOrgs = saOrgMapper.selectList(null);
        return ResultBean.success("获取成功", allOrgs);
    }

    @ApiOperation("批量更新排名选中状态")
    @PostMapping("/common/saOrg/batchUpdateRanking")
    public ResultBean batchUpdateRanking(@RequestBody Map<String, Boolean> rankingMap) {
        if (rankingMap == null || rankingMap.isEmpty()) {
            return ResultBean.error("参数不能为空");
        }
        for (Map.Entry<String, Boolean> entry : rankingMap.entrySet()) {
            SaOrg org = new SaOrg();
            org.setIsRankingSelected(entry.getValue() ? 1 : 0);
            saOrgMapper.update(org,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SaOrg>()
                            .eq(SaOrg::getOrgNo, entry.getKey())
            );
        }
        return ResultBean.success("更新成功");
    }

}
