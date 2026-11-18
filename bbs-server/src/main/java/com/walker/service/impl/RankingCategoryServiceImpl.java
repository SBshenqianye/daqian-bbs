package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.RankingCategoryMapper;
import com.walker.mapper.RankingCategoryOrgMapper;
import com.walker.mapper.RankingStatsMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.RankingCategory;
import com.walker.pojo.RankingCategoryOrg;
import com.walker.pojo.SaOrg;
import com.walker.service.ArticleService;
import com.walker.service.OperationCycleService;
import com.walker.service.RankingCategoryService;
import com.walker.utils.ConstantUtil;
import com.walker.vo.PointsRankVO;
import com.walker.vo.RankingCategoryOrgVO;
import com.walker.vo.RankingCategoryVO;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 排行榜分类服务。
 *
 * <p>“内江供电公司本部”是系统内置的 LEGACY 分类（id=0），沿用原有排名逻辑，
 * 不写入分类表、不参与拖拽；其余分类由管理员在系统管理中配置。</p>
 */
@Service
public class RankingCategoryServiceImpl extends ServiceImpl<RankingCategoryMapper, RankingCategory>
        implements RankingCategoryService {

    private static final int LEGACY_CATEGORY_ID = 0;
    private static final String LEGACY_CATEGORY_CODE = "legacy_root";
    private static final String LEGACY_CATEGORY_NAME = "内江供电公司本部";

    @Autowired
    private RankingCategoryOrgMapper categoryOrgMapper;

    @Autowired
    private RankingStatsMapper rankingStatsMapper;

    @Autowired
    private SaOrgMapper saOrgMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private OperationCycleService operationCycleService;

    @Override
    public ResultBean listPublicCategories() {
        List<RankingCategoryVO> result = new ArrayList<>();
        List<RankingCategory> categories = this.list(new LambdaQueryWrapper<RankingCategory>()
                .eq(RankingCategory::getEnabled, 1)
                .orderByAsc(RankingCategory::getSortOrder)
                .orderByAsc(RankingCategory::getId));
        for (RankingCategory category : categories) {
            result.add(toVO(category, false));
        }
        return ResultBean.success("查询成功", result);
    }

    @Override
    public ResultBean listAdminCategories() {
        List<RankingCategoryVO> result = new ArrayList<>();
        List<RankingCategory> categories = this.list(new LambdaQueryWrapper<RankingCategory>()
                .orderByAsc(RankingCategory::getSortOrder)
                .orderByAsc(RankingCategory::getId));
        for (RankingCategory category : categories) {
            result.add(toVO(category, true));
        }
        return ResultBean.success("查询成功", result);
    }

    /**
     * 分类 ID 归一化：id=0 是历史虚拟分类（内江供电公司本部），
     * 现在统一指向 category_code=legacy_root 的真实分类，便于像其他分类一样配置成员。
     */
    private Integer resolveCategoryId(Integer id) {
        if (id == null) return null;
        if (id > 0) {
            RankingCategory direct = this.getById(id);
            if (direct != null) return direct.getId();
        }
        RankingCategory byCode = this.getOne(new LambdaQueryWrapper<RankingCategory>()
                .eq(RankingCategory::getCategoryCode, LEGACY_CATEGORY_CODE)
                .last("LIMIT 1"));
        return byCode == null ? null : byCode.getId();
    }

    @Override
    @Transactional
    public ResultBean saveCategory(RankingCategory category) {
        if (category == null || !StringUtils.hasText(category.getCategoryName())) {
            return ResultBean.error("分类名称不能为空");
        }
        String name = category.getCategoryName().trim();
        List<RankingCategory> sameName = this.list(new LambdaQueryWrapper<RankingCategory>()
                .eq(RankingCategory::getCategoryName, name)
                .ne(category.getId() != null, RankingCategory::getId, category.getId()));
        if (!CollectionUtils.isEmpty(sameName)) {
            return ResultBean.error("分类名称已存在");
        }
        String code = StringUtils.hasText(category.getCategoryCode())
                ? category.getCategoryCode().trim()
                : "RC" + System.currentTimeMillis();
        List<RankingCategory> sameCode = this.list(new LambdaQueryWrapper<RankingCategory>()
                .eq(RankingCategory::getCategoryCode, code)
                .ne(category.getId() != null, RankingCategory::getId, category.getId()));
        if (!CollectionUtils.isEmpty(sameCode)) {
            return ResultBean.error("分类编码已存在");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        category.setCategoryName(name);
        category.setCategoryCode(code);
        if (category.getEnabled() == null) {
            category.setEnabled(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getId() == null) {
            category.setCreateTime(now);
            this.save(category);
        } else {
            RankingCategory old = this.getById(category.getId());
            if (old == null) {
                return ResultBean.error("分类不存在");
            }
            category.setCreateTime(old.getCreateTime());
            category.setUpdateTime(now);
            this.updateById(category);
        }
        return ResultBean.success("保存成功", category.getId());
    }

    @Override
    @Transactional
    public ResultBean deleteCategory(Integer id) {
        Integer targetId = resolveCategoryId(id);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        RankingCategory category = this.getById(targetId);
        if (category == null) {
            return ResultBean.error("分类不存在");
        }
        categoryOrgMapper.delete(new LambdaQueryWrapper<RankingCategoryOrg>()
                .eq(RankingCategoryOrg::getCategoryId, targetId));
        this.removeById(targetId);
        return ResultBean.success("删除成功");
    }

    @Override
    @Transactional
    public ResultBean addOrg(Integer categoryId, String orgNo, Boolean includeDescendants) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        if (!StringUtils.hasText(orgNo)) {
            return ResultBean.error("组织编号不能为空");
        }
        RankingCategory category = this.getById(targetId);
        if (category == null) {
            return ResultBean.error("分类不存在");
        }
        SaOrg org = saOrgMapper.selectOne(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getOrgNo, orgNo)
                .eq(SaOrg::getIsDelete, 0)
                .last("LIMIT 1"));
        if (org == null) {
            return ResultBean.error("组织不存在或已删除");
        }
        Long exists = categoryOrgMapper.selectCount(new LambdaQueryWrapper<RankingCategoryOrg>()
                .eq(RankingCategoryOrg::getCategoryId, targetId)
                .eq(RankingCategoryOrg::getOrgNo, orgNo));
        if (exists != null && exists > 0) {
            return ResultBean.error("该组织已在分类中");
        }
        Integer maxSort = categoryOrgMapper.selectList(new LambdaQueryWrapper<RankingCategoryOrg>()
                        .eq(RankingCategoryOrg::getCategoryId, targetId))
                .stream()
                .map(RankingCategoryOrg::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        RankingCategoryOrg row = new RankingCategoryOrg();
        row.setCategoryId(targetId);
        row.setOrgNo(orgNo);
        row.setIncludeDescendants(Boolean.FALSE.equals(includeDescendants) ? 0 : 1);
        row.setSortOrder(maxSort + 10);
        row.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        categoryOrgMapper.insert(row);
        return ResultBean.success("已加入分类");
    }

    @Override
    public ResultBean updateOrgDescendants(Integer categoryId, String orgNo, Boolean includeDescendants) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        RankingCategoryOrg row = findCategoryOrg(targetId, orgNo);
        if (row == null) {
            return ResultBean.error("分类成员不存在");
        }
        row.setIncludeDescendants(Boolean.FALSE.equals(includeDescendants) ? 0 : 1);
        categoryOrgMapper.updateById(row);
        return ResultBean.success("更新成功");
    }

    @Override
    public ResultBean removeOrg(Integer categoryId, String orgNo) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        int deleted = categoryOrgMapper.delete(new LambdaQueryWrapper<RankingCategoryOrg>()
                .eq(RankingCategoryOrg::getCategoryId, targetId)
                .eq(RankingCategoryOrg::getOrgNo, orgNo));
        if (deleted == 0) {
            return ResultBean.error("分类成员不存在");
        }
        return ResultBean.success("已移除");
    }

    @Override
    public ResultBean getOrgRank(Integer categoryId, String startTime, String endTime) {
        String[] range = resolveRange(startTime, endTime);
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            // 兼容：分类尚未初始化时退回原「本部」排名逻辑，避免首页空白
            return legacyRank(range[0], range[1]);
        }
        RankingCategory category = this.getById(targetId);
        if (category == null) {
            return legacyRank(range[0], range[1]);
        }
        List<RankingCategoryOrg> members = categoryOrgMapper.selectList(
                new LambdaQueryWrapper<RankingCategoryOrg>()
                        .eq(RankingCategoryOrg::getCategoryId, targetId)
                        .orderByAsc(RankingCategoryOrg::getSortOrder)
                        .orderByAsc(RankingCategoryOrg::getId));
        if (members.isEmpty()) {
            // 成员为空（历史数据）时同样回落，管理员配置成员后即按配置生效
            return legacyRank(range[0], range[1]);
        }
        Map<String, SaOrg> orgMap = loadOrgMap();
        List<PointsRankVO> rows = new ArrayList<>();
        for (RankingCategoryOrg member : members) {
            SaOrg org = orgMap.get(member.getOrgNo());
            String orgName = org != null ? org.getOrgName() : member.getOrgNo();
            boolean include = !Integer.valueOf(0).equals(member.getIncludeDescendants());
            int memberCount = safeInt(rankingStatsMapper.countMembers(member.getOrgNo(), include));
            int points = safeInt(rankingStatsMapper.sumPoints(member.getOrgNo(), include, range[0], range[1]));
            int posts = safeInt(rankingStatsMapper.countPosts(member.getOrgNo(), include, range[0], range[1]));
            int replies = safeInt(rankingStatsMapper.countReplies(member.getOrgNo(), include, range[0], range[1]));
            PointsRankVO vo = new PointsRankVO();
            vo.setOrgNo(member.getOrgNo());
            vo.setOrgName(orgName);
            vo.setPoints(points);
            vo.setMemberCount(memberCount);
            vo.setAvgPoints(avg(points, memberCount));
            vo.setPosts(posts);
            vo.setReplies(replies);
            rows.add(vo);
        }
        sortAndRank(rows);
        return ResultBean.success("查询成功", rows);
    }

    private ResultBean legacyRank(String startTime, String endTime) {
        com.walker.vo.param.PointsRankParam param = new com.walker.vo.param.PointsRankParam();
        param.setRankType(ConstantUtil.MANA_ZERO_ONE);
        param.setOrgNo(ConstantUtil.ORG_NEI_JIANG);
        param.setStartTime(startTime);
        param.setEndTime(endTime);
        ResultBean result = articleService.pointsRank(param);
        if (result == null || result.getCode() != 200 || !(result.getObj() instanceof List)) {
            return result;
        }
        @SuppressWarnings("unchecked")
        List<PointsRankVO> rows = (List<PointsRankVO>) result.getObj();
        if (rows != null) {
            for (PointsRankVO row : rows) {
                int memberCount = safeInt(rankingStatsMapper.countMembers(row.getOrgNo(), true));
                row.setMemberCount(memberCount);
                row.setAvgPoints(avg(row.getPoints(), memberCount));
            }
            sortAndRank(rows);
        }
        return result;
    }

    private RankingCategoryVO buildLegacyCategory() {
        RankingCategoryVO vo = new RankingCategoryVO();
        vo.setId(LEGACY_CATEGORY_ID);
        vo.setCategoryCode(LEGACY_CATEGORY_CODE);
        vo.setCategoryName(LEGACY_CATEGORY_NAME);
        vo.setDescription("沿用原有组织排名逻辑，不参与分类配置");
        vo.setSortOrder(10);
        vo.setEnabled(1);
        vo.setLegacy(true);
        vo.setOrgCount(null);
        return vo;
    }

    private RankingCategoryVO toVO(RankingCategory category, boolean withMembers) {
        RankingCategoryVO vo = new RankingCategoryVO();
        vo.setId(category.getId());
        vo.setCategoryCode(category.getCategoryCode());
        vo.setCategoryName(category.getCategoryName());
        vo.setDescription(category.getDescription());
        vo.setSortOrder(category.getSortOrder());
        vo.setEnabled(category.getEnabled());
        vo.setLegacy(false);
        List<RankingCategoryOrg> rows = categoryOrgMapper.selectList(
                new LambdaQueryWrapper<RankingCategoryOrg>()
                        .eq(RankingCategoryOrg::getCategoryId, category.getId())
                        .orderByAsc(RankingCategoryOrg::getSortOrder)
                        .orderByAsc(RankingCategoryOrg::getId));
        vo.setOrgCount(rows.size());
        if (withMembers) {
            Map<String, SaOrg> orgMap = loadOrgMap();
            List<RankingCategoryOrgVO> members = new ArrayList<>();
            for (RankingCategoryOrg row : rows) {
                RankingCategoryOrgVO member = new RankingCategoryOrgVO();
                member.setOrgNo(row.getOrgNo());
                member.setOrgName(orgMap.containsKey(row.getOrgNo())
                        ? orgMap.get(row.getOrgNo()).getOrgName() : row.getOrgNo());
                member.setIncludeDescendants(row.getIncludeDescendants());
                member.setSortOrder(row.getSortOrder());
                members.add(member);
            }
            vo.setMembers(members);
        }
        return vo;
    }

    private RankingCategoryOrg findCategoryOrg(Integer categoryId, String orgNo) {
        return categoryOrgMapper.selectOne(new LambdaQueryWrapper<RankingCategoryOrg>()
                .eq(RankingCategoryOrg::getCategoryId, categoryId)
                .eq(RankingCategoryOrg::getOrgNo, orgNo)
                .last("LIMIT 1"));
    }

    private Map<String, SaOrg> loadOrgMap() {
        List<SaOrg> orgs = saOrgMapper.selectList(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getIsDelete, 0));
        Map<String, SaOrg> map = new HashMap<>();
        if (orgs != null) {
            for (SaOrg org : orgs) {
                map.put(org.getOrgNo(), org);
            }
        }
        return map;
    }

    private void sortAndRank(List<PointsRankVO> rows) {
        rows.sort(Comparator
                .comparing((PointsRankVO v) -> v.getAvgPoints() == null ? 0D : v.getAvgPoints())
                .reversed()
                .thenComparing(v -> v.getPoints() == null ? 0 : v.getPoints(), Comparator.reverseOrder())
                .thenComparing(v -> v.getOrgNo() == null ? "" : v.getOrgNo()));
        int rank = 1;
        for (PointsRankVO row : rows) {
            row.setRankNum(rank++);
        }
    }

    private double avg(Integer points, Integer memberCount) {
        if (points == null || memberCount == null || memberCount <= 0) {
            return 0D;
        }
        return Math.round(points * 100.0 / memberCount) / 100.0;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String[] resolveRange(String startTime, String endTime) {
        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
            return new String[]{startTime, endTime};
        }
        // 未显式传时间时，默认按当前启用的运营周期取数；没有周期再回落到本月
        try {
            com.walker.pojo.OperationCycle cycle = operationCycleService.getActiveCycle();
            if (cycle != null && StringUtils.hasText(cycle.getStartDate())
                    && StringUtils.hasText(cycle.getEndDate())) {
                return new String[]{cycle.getStartDate(), cycle.getEndDate()};
            }
        } catch (Exception ignore) {
            // 周期表尚未迁移时回落本月
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String start = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String end = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        return new String[]{start, end};
    }
}
