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
import com.walker.service.OperationCycleService;
import com.walker.service.RankingCategoryService;
import com.walker.vo.PointsRankVO;
import com.walker.vo.RankingCategoryOrgVO;
import com.walker.vo.RankingCategoryVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.RankingSortItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 排行榜分类服务。
 *
 * <p>分类成员通过三态树勾选：全选=含全部下级，部分选=只含勾选的下级，
 * 未勾选的子级写入 exclude 行，从上级“含下级”聚合中剔除。</p>
 */
@Service
public class RankingCategoryServiceImpl extends ServiceImpl<RankingCategoryMapper, RankingCategory>
        implements RankingCategoryService {

    private static final String LEGACY_CATEGORY_CODE = "legacy_root";

    private static final String TYPE_INCLUDE = "include";
    private static final String TYPE_EXCLUDE = "exclude";

    @Autowired
    private RankingCategoryOrgMapper categoryOrgMapper;

    @Autowired
    private RankingStatsMapper rankingStatsMapper;

    @Autowired
    private SaOrgMapper saOrgMapper;

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
            RankingCategoryVO vo = toVO(category, false);
            // 用户端只需要名称和排序；说明与编码属于管理端配置，不下发
            vo.setDescription(null);
            vo.setCategoryCode(null);
            result.add(vo);
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
     * 现在统一指向 category_code=legacy_root 的真实分类。
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
        row.setSelectionType(TYPE_INCLUDE);
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
        row.setSelectionType(TYPE_INCLUDE);
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
    @Transactional
    public ResultBean resetCategoryToDefault(Integer categoryId) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        RankingCategory category = this.getById(targetId);
        if (category == null) {
            return ResultBean.error("分类不存在");
        }
        CategoryDefault def = defaultOf(category.getCategoryCode());
        if (def == null) {
            def = defaultOf(category.getCategoryName());
        }
        if (def == null) {
            return ResultBean.error("该分类没有内置默认配置");
        }
        if (!def.code.equals(category.getCategoryCode())) {
            Long sameCode = this.count(new LambdaQueryWrapper<RankingCategory>()
                    .eq(RankingCategory::getCategoryCode, def.code)
                    .ne(RankingCategory::getId, category.getId()));
            if (sameCode != null && sameCode > 0) {
                return ResultBean.error("默认编码已被其它分类占用，请先处理后再恢复");
            }
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        category.setCategoryCode(def.code);
        category.setCategoryName(def.name);
        category.setDescription(def.description);
        category.setSortOrder(def.sortOrder);
        category.setEnabled(1);
        category.setUpdateTime(now);
        this.updateById(category);
        return ResultBean.success("已恢复默认配置");
    }

    @Override
    public ResultBean getDefaultMembers(Integer categoryId) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        RankingCategory category = this.getById(targetId);
        if (category == null) {
            return ResultBean.error("分类不存在");
        }
        CategoryDefault def = defaultOf(category.getCategoryCode());
        if (def == null) {
            def = defaultOf(category.getCategoryName());
        }
        if (def == null) {
            return ResultBean.error("该分类没有内置默认配置");
        }
        List<SaOrg> orgs = saOrgMapper.selectList(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getIsDelete, 0));
        List<SaOrg> members = defaultMembers(def.code, orgs);
        if (CollectionUtils.isEmpty(members)) {
            return ResultBean.error("未找到默认组织，请先在组织管理中导入组织");
        }
        List<String> orgNos = new ArrayList<>();
        for (SaOrg org : members) {
            orgNos.add(org.getOrgNo());
        }
        return ResultBean.success("查询成功", orgNos);
    }

    @Override
    @Transactional
    public ResultBean updateSortOrders(List<RankingSortItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return ResultBean.error("参数不能为空");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (RankingSortItem item : items) {
            if (item == null || item.getId() == null) continue;
            RankingCategory category = new RankingCategory();
            category.setId(item.getId());
            category.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            category.setUpdateTime(now);
            this.updateById(category);
        }
        return ResultBean.success("排序已保存");
    }

    /**
     * 与 v045/v047/v056/v069 的种子口径保持一致，供“恢复默认参与单位”使用。
     */
    private List<SaOrg> defaultMembers(String categoryCode, List<SaOrg> orgs) {
        List<SaOrg> result = new ArrayList<>();
        if (orgs == null) return result;
        String countyRoot = null;
        String companyRoot = null;
        for (SaOrg org : orgs) {
            if (countyRoot == null && "县级供电企业".equals(org.getOrgName())) {
                countyRoot = org.getOrgNo();
            }
            if (companyRoot == null && "公司所属各单位".equals(org.getOrgName())) {
                companyRoot = org.getOrgNo();
            }
        }
        for (SaOrg org : orgs) {
            String name = org.getOrgName() == null ? "" : org.getOrgName();
            String parent = org.getPOrgNo();
            if ("legacy_root".equals(categoryCode)) {
                if ("51404".equals(parent)) result.add(org);
            } else if ("county_units".equals(categoryCode)) {
                if ((countyRoot != null && countyRoot.equals(parent))
                        || ("51404".equals(parent)
                        && (name.contains("供电分公司") || "市区供电中心".equals(name)))) {
                    result.add(org);
                }
            } else if ("company_units".equals(categoryCode)) {
                if (companyRoot != null && companyRoot.equals(parent)) result.add(org);
            } else if ("support_units".equals(categoryCode)) {
                if (SUPPORT_ORG_NAMES.contains(name)
                        || name.contains("项目管理中心")
                        || name.contains("供电服务指挥中心")) {
                    result.add(org);
                }
            } else if ("collective_units".equals(categoryCode)) {
                if ("5140403".equals(parent)
                        && (name.contains("星原") || name.contains("三新")
                        || name.contains("乐山城电") || name.contains("东祥") || name.contains("西星"))) {
                    result.add(org);
                }
            }
        }
        return result;
    }

    private static final Set<String> SUPPORT_ORG_NAMES = new HashSet<>(Arrays.asList(
            "变电运检中心", "输电运检中心", "客户服务中心", "经济技术研究所",
            "信息通信分公司", "综合服务中心", "供电服务指挥中心"));

    private CategoryDefault defaultOf(String codeOrName) {
        if ("legacy_root".equals(codeOrName) || "内江供电公司本部".equals(codeOrName)) {
            return new CategoryDefault("legacy_root", "内江供电公司本部",
                    "原“本部”排名，已改为普通分类，可自由配置成员与含下级", 0);
        }
        if ("county_units".equals(codeOrName) || "县级供电企业".equals(codeOrName)) {
            return new CategoryDefault("county_units", "县级供电企业", "县级供电公司/供电中心，含下级供电所", 20);
        }
        if ("company_units".equals(codeOrName) || "公司所属各单位".equals(codeOrName)) {
            return new CategoryDefault("company_units", "公司所属各单位", "内江公司根节点下直属单位，含下级", 30);
        }
        if ("support_units".equals(codeOrName) || "地市支撑机构".equals(codeOrName)) {
            return new CategoryDefault("support_units", "地市支撑机构", "变电运检、输电运检、客户服务等支撑机构", 40);
        }
        if ("collective_units".equals(codeOrName) || "集体企业及相关单位".equals(codeOrName)) {
            return new CategoryDefault("collective_units", "集体企业及相关单位", "集体企业及相关单位", 50);
        }
        return null;
    }

    private static class CategoryDefault {
        private final String code;
        private final String name;
        private final String description;
        private final int sortOrder;

        private CategoryDefault(String code, String name, String description, int sortOrder) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.sortOrder = sortOrder;
        }
    }

    @Override
    @Transactional
    public ResultBean saveSelection(Integer categoryId, List<String> checkedKeys, List<String> halfCheckedKeys) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.error("分类不存在");
        }
        if (this.getById(targetId) == null) {
            return ResultBean.error("分类不存在");
        }
        List<SaOrg> orgs = saOrgMapper.selectList(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getIsDelete, 0));
        Map<String, SaOrg> orgMap = new HashMap<>();
        if (orgs != null) {
            for (SaOrg org : orgs) {
                orgMap.put(org.getOrgNo(), org);
            }
        }
        Map<String, List<SaOrg>> childrenMap = buildChildrenMap(orgs, orgMap);
        Set<String> checked = checkedKeys == null ? new HashSet<>() : new HashSet<>(checkedKeys);
        Set<String> half = halfCheckedKeys == null ? new HashSet<>() : new HashSet<>(halfCheckedKeys);

        List<RankingCategoryOrg> rows = new ArrayList<>();
        int[] sort = {0};
        for (SaOrg org : orgs == null ? new ArrayList<SaOrg>() : orgs) {
            String p = org.getPOrgNo();
            if (p != null && !p.isEmpty() && orgMap.containsKey(p)) continue;
            serializeOrg(org, false, checked, half, childrenMap, rows, sort);
        }

        categoryOrgMapper.delete(new LambdaQueryWrapper<RankingCategoryOrg>()
                .eq(RankingCategoryOrg::getCategoryId, targetId));
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (RankingCategoryOrg row : rows) {
            row.setCategoryId(targetId);
            row.setCreateTime(now);
            categoryOrgMapper.insert(row);
        }
        return ResultBean.success("保存成功", rows.size());
    }

    /**
     * 把三态树选择序列化为最小行集：
     * <ul>
     *   <li>全选节点 → include + include_descendants=1；</li>
     *   <li>部分选节点 → 自身 include + 未勾选子级写 exclude；</li>
     *   <li>未勾选且被上级覆盖 → exclude 整棵子树。</li>
     * </ul>
     */
    private void serializeOrg(SaOrg node, boolean covered,
                              Set<String> checked, Set<String> half,
                              Map<String, List<SaOrg>> childrenMap,
                              List<RankingCategoryOrg> out, int[] sort) {
        String orgNo = node.getOrgNo();
        boolean isChecked = checked.contains(orgNo);
        boolean isHalf = half.contains(orgNo);
        List<SaOrg> children = childrenMap.getOrDefault(orgNo, new ArrayList<>());

        if (!isChecked && !isHalf) {
            if (covered) {
                out.add(newRow(orgNo, 1, TYPE_EXCLUDE, sort));
                return;
            }
            for (SaOrg child : children) {
                serializeOrg(child, false, checked, half, childrenMap, out, sort);
            }
            return;
        }
        if (isChecked && !isHalf) {
            if (!covered) {
                // 兼容历史“仅本级”成员：本级勾选但整棵子树都没有其它勾选时，不自动扩成含下级
                boolean hasSelectedDescendant = hasSelectedDescendant(node, checked, half, childrenMap);
                int includeDescendants = hasSelectedDescendant ? 1 : 0;
                out.add(newRow(orgNo, includeDescendants, TYPE_INCLUDE, sort));
            }
            return;
        }
        // 部分选中：本级与已勾选下级参与，未勾选下级在上一步写成 exclude
        if (!covered) {
            out.add(newRow(orgNo, 1, TYPE_INCLUDE, sort));
        }
        for (SaOrg child : children) {
            serializeOrg(child, true, checked, half, childrenMap, out, sort);
        }
    }

    private boolean hasSelectedDescendant(SaOrg node, Set<String> checked, Set<String> half,
                                          Map<String, List<SaOrg>> childrenMap) {
        for (SaOrg child : childrenMap.getOrDefault(node.getOrgNo(), new ArrayList<>())) {
            if (checked.contains(child.getOrgNo()) || half.contains(child.getOrgNo())) return true;
            if (hasSelectedDescendant(child, checked, half, childrenMap)) return true;
        }
        return false;
    }

    private RankingCategoryOrg newRow(String orgNo, int includeDescendants, String type, int[] sort) {
        RankingCategoryOrg row = new RankingCategoryOrg();
        row.setOrgNo(orgNo);
        row.setIncludeDescendants(includeDescendants);
        row.setSelectionType(type);
        row.setSortOrder(sort[0]);
        sort[0] += 10;
        return row;
    }

    @Override
    public ResultBean getOrgRank(Integer categoryId, String startTime, String endTime) {
        String[] range = resolveRange(startTime, endTime);
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) {
            return ResultBean.success("查询成功", new ArrayList<>());
        }
        RankingCategory category = this.getById(targetId);
        if (category == null) {
            return ResultBean.success("查询成功", new ArrayList<>());
        }
        List<RankingCategoryOrg> rows = categoryOrgMapper.selectList(
                new LambdaQueryWrapper<RankingCategoryOrg>()
                        .eq(RankingCategoryOrg::getCategoryId, targetId)
                        .orderByAsc(RankingCategoryOrg::getSortOrder)
                        .orderByAsc(RankingCategoryOrg::getId));
        List<RankingCategoryOrg> includes = new ArrayList<>();
        List<RankingCategoryOrg> excludes = new ArrayList<>();
        for (RankingCategoryOrg row : rows) {
            if (TYPE_EXCLUDE.equals(row.getSelectionType())) {
                excludes.add(row);
            } else {
                includes.add(row);
            }
        }
        if (includes.isEmpty()) {
            return ResultBean.success("查询成功", new ArrayList<>());
        }

        Map<String, SaOrg> orgMap = loadOrgMap();
        List<PointsRankVO> result = new ArrayList<>();
        for (RankingCategoryOrg include : topMostIncludes(includes, orgMap)) {
            List<String> allowed = allowedOrgNos(include, excludes, orgMap);
            PointsRankVO vo = new PointsRankVO();
            vo.setOrgNo(include.getOrgNo());
            SaOrg org = orgMap.get(include.getOrgNo());
            vo.setOrgName(org != null ? org.getOrgName() : include.getOrgNo());
            int memberCount = allowed.isEmpty() ? 0 : safeInt(rankingStatsMapper.countMembersByOrgs(allowed));
            int points = allowed.isEmpty() ? 0
                    : safeInt(rankingStatsMapper.sumPointsByOrgs(allowed, range[0], range[1]));
            int posts = allowed.isEmpty() ? 0
                    : safeInt(rankingStatsMapper.countPostsByOrgs(allowed, range[0], range[1]));
            int replies = allowed.isEmpty() ? 0
                    : safeInt(rankingStatsMapper.countRepliesByOrgs(allowed, range[0], range[1]));
            vo.setPoints(points);
            vo.setMemberCount(memberCount);
            vo.setAvgPoints(avg(points, memberCount));
            vo.setPosts(posts);
            vo.setReplies(replies);
            result.add(vo);
        }
        sortAndRank(result);
        return ResultBean.success("查询成功", result);
    }

    @Override
    public List<String> listAllowedOrgNos(Integer categoryId) {
        Integer targetId = resolveCategoryId(categoryId);
        if (targetId == null) return new ArrayList<>();
        List<RankingCategoryOrg> rows = categoryOrgMapper.selectList(
                new LambdaQueryWrapper<RankingCategoryOrg>()
                        .eq(RankingCategoryOrg::getCategoryId, targetId));
        List<RankingCategoryOrg> includes = new ArrayList<>();
        List<RankingCategoryOrg> excludes = new ArrayList<>();
        for (RankingCategoryOrg row : rows) {
            if (TYPE_EXCLUDE.equals(row.getSelectionType())) {
                excludes.add(row);
            } else {
                includes.add(row);
            }
        }
        if (includes.isEmpty()) return new ArrayList<>();
        Map<String, SaOrg> orgMap = loadOrgMap();
        Set<String> allowed = new HashSet<>();
        for (RankingCategoryOrg include : topMostIncludes(includes, orgMap)) {
            allowed.addAll(allowedOrgNos(include, excludes, orgMap));
        }
        return new ArrayList<>(allowed);
    }

    /**
     * 榜单行只取最外层 include：被另一个“含下级”include 覆盖的行不再单独出榜。
     */
    private List<RankingCategoryOrg> topMostIncludes(List<RankingCategoryOrg> includes,
                                                     Map<String, SaOrg> orgMap) {
        List<RankingCategoryOrg> top = new ArrayList<>();
        for (RankingCategoryOrg current : includes) {
            boolean covered = false;
            for (RankingCategoryOrg other : includes) {
                if (other == current) continue;
                if (isSubtreeInclude(other) && !other.getOrgNo().equals(current.getOrgNo())
                        && isTreeDescendant(current.getOrgNo(), other.getOrgNo(), orgMap)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) top.add(current);
        }
        return top;
    }

    /**
     * 计算一个榜单行实际参与统计的组织编号：落在 include 组织子树内，且不在任何 exclude 子树内。
     */
    private List<String> allowedOrgNos(RankingCategoryOrg include,
                                       List<RankingCategoryOrg> excludes,
                                       Map<String, SaOrg> orgMap) {
        List<String> allowed = new ArrayList<>();
        for (SaOrg org : orgMap.values()) {
            String orgNo = org.getOrgNo();
            if (!covers(include, orgNo, orgMap)) continue;
            boolean excluded = false;
            for (RankingCategoryOrg exclude : excludes) {
                if (covers(exclude, orgNo, orgMap)) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) allowed.add(orgNo);
        }
        return allowed;
    }

    private boolean covers(RankingCategoryOrg row, String orgNo, Map<String, SaOrg> orgMap) {
        if (rowsSameOrg(row.getOrgNo(), orgNo)) return true;
        return isSubtreeInclude(row) && isTreeDescendant(orgNo, row.getOrgNo(), orgMap);
    }

    /**
     * 按 p_org_no 链判断祖先关系。组织编号前缀与实际父子关系并不总是一致
     * （例如“内江星原公司”与“乐山城电内江分公司”是兄弟节点，但编号都以 5140404 开头），
     * 因此这里必须走树结构而不是字符串前缀。
     */
    private boolean isTreeDescendant(String orgNo, String ancestorNo, Map<String, SaOrg> orgMap) {
        if (orgNo == null || ancestorNo == null || orgNo.equals(ancestorNo)) return false;
        SaOrg current = orgMap.get(orgNo);
        int guard = 0;
        while (current != null && guard++ < 64) {
            String parent = current.getPOrgNo();
            if (parent == null || parent.isEmpty()) return false;
            if (parent.equals(ancestorNo)) return true;
            current = orgMap.get(parent);
        }
        return false;
    }

    private boolean rowsSameOrg(String a, String b) {
        return a != null && a.equals(b);
    }

    private boolean isSubtreeInclude(RankingCategoryOrg row) {
        return !Integer.valueOf(0).equals(row.getIncludeDescendants());
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
        int includeCount = 0;
        for (RankingCategoryOrg row : rows) {
            if (!TYPE_EXCLUDE.equals(row.getSelectionType())) includeCount++;
        }
        vo.setOrgCount(includeCount);
        if (withMembers) {
            Map<String, SaOrg> orgMap = loadOrgMap();
            List<RankingCategoryOrgVO> members = new ArrayList<>();
            for (RankingCategoryOrg row : rows) {
                RankingCategoryOrgVO member = new RankingCategoryOrgVO();
                member.setOrgNo(row.getOrgNo());
                member.setOrgName(orgMap.containsKey(row.getOrgNo())
                        ? orgMap.get(row.getOrgNo()).getOrgName() : row.getOrgNo());
                member.setIncludeDescendants(row.getIncludeDescendants());
                member.setSelectionType(TYPE_EXCLUDE.equals(row.getSelectionType())
                        ? TYPE_EXCLUDE : TYPE_INCLUDE);
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

    private Map<String, List<SaOrg>> buildChildrenMap(List<SaOrg> orgs, Map<String, SaOrg> orgMap) {
        Map<String, List<SaOrg>> children = new HashMap<>();
        if (orgs == null) return children;
        for (SaOrg org : orgs) {
            String p = org.getPOrgNo();
            if (p == null || p.isEmpty() || !orgMap.containsKey(p)) continue;
            children.computeIfAbsent(p, k -> new ArrayList<>()).add(org);
        }
        return children;
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
