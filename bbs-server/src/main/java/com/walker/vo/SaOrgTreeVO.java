package com.walker.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/3 15:04
 * @PackageName:com.walker.vo
 * @ClassName: SaOrgTreeVO
 * @Description: 单位树实体类
 */
@Data
public class SaOrgTreeVO {
    /**
     * 单位编号
     */
    private String id;
    /**
     * 单位名称
     */
    private String label;
    /**
     * 父级单位编号
     */
    private String pOrgNo;
    /**
     * 子级树
     */
    private List<SaOrgTreeVO> children;

    /**
     * 是否参与排名:0=否,1=是
     */
    private Integer isRankingSelected;

}
