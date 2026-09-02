package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.Article;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.service.SaOrgService;
import com.walker.vo.ResultBean;
import com.walker.vo.SaOrgTreeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author chengQing
 * @Date 2026/3/3 14:48
 * @PackageName:com.walker.service.impl
 * @ClassName: SaOrgServiceImpl
 * @Description: 单位实现层
 */
@Service
public class SaOrgServiceImpl extends ServiceImpl<SaOrgMapper, SaOrg> implements SaOrgService {
    @Autowired
    private SaOrgMapper saOrgMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<SaOrgTreeVO> getOrgTree() {
        // 查询全量单位列表（排除已删除的节点）
        List<SaOrg> orgList = this.list(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getIsDelete, 0)
                .orderByAsc(SaOrg::getOrgNo));
        if (orgList == null || orgList.isEmpty()) {
            return new ArrayList<>();
        }

        // 先将所有单位转成 VO，并用 orgNo 建立映射
        Map<String, SaOrgTreeVO> orgMap = new HashMap<>();
        for (SaOrg org : orgList) {
            SaOrgTreeVO vo = new SaOrgTreeVO();
            vo.setId(org.getOrgNo());
            vo.setLabel(org.getOrgName());
            vo.setPOrgNo(org.getPOrgNo());
            vo.setIsRankingSelected(org.getIsRankingSelected());
            vo.setIsDisplaySelected(org.getIsDisplaySelected());
            orgMap.put(org.getOrgNo(), vo);
        }

        // 组装树
        List<SaOrgTreeVO> roots = new ArrayList<>();
        for (SaOrg org : orgList) {
            SaOrgTreeVO current = orgMap.get(org.getOrgNo());
            String pOrgNo = org.getPOrgNo();
            if (pOrgNo == null || pOrgNo.trim().isEmpty()) {
                // 没有父节点，视为根节点
                roots.add(current);
            } else {
                SaOrgTreeVO parent = orgMap.get(pOrgNo);
                if (parent == null) {
                    // 找不到父节点时，降级为根节点，避免丢数据
                    roots.add(current);
                    continue;
                }
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(current);
            }
        }
        return roots;
    }

    @Override
    public ResultBean deleteSaOrgByOrgNo(String orgNo) {
        // 先查询单位下是否有子类，如果有则不允许删除
        List<SaOrg> saOrgList = saOrgMapper.selectList(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getIsDelete, 0)
                .eq(SaOrg::getPOrgNo, orgNo)
        );
        if (!CollectionUtils.isEmpty(saOrgList)) {
            return ResultBean.error("单位下有子单位，不允许删除！");
        }

        // 再查询是否有人员注册到此到单位，如果有则不允许删除
        List<User> userList = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getIsDelete, 0)
                .eq(User::getOrgNo, orgNo)
        );
        if (!CollectionUtils.isEmpty(userList)) {
            return ResultBean.error("有人员注册到此单位，不允许删除！");
        }
        // 删除 单位
        int delete = saOrgMapper.delete(new LambdaQueryWrapper<SaOrg>()
                .eq(SaOrg::getIsDelete, 0)
                .eq(SaOrg::getOrgNo, orgNo)
        );

        if (delete > 0) {
            return ResultBean.success("操作成功！");
        }
        return ResultBean.error("操作失败！");
    }

    @Override
    public ResultBean addSaOrg(String pOrgNo, String orgName) {
        // 先查询全部单位信息，然后获取最大id
        List<SaOrg> saOrgList = this.list();
        if (CollectionUtils.isEmpty(saOrgList)) {
            return ResultBean.error("无单位数据，操作失败！");
        }

        List<SaOrg> pSaOrgList = saOrgList.stream().filter(item -> item.getOrgNo().equals(pOrgNo) && item.getIsDelete() == 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pSaOrgList)) {
            return ResultBean.error("父级单位不正确，操作失败！");
        }

        Integer maxId = saOrgList.stream()
                .mapToInt(SaOrg::getId)
                .max()
                .orElse(0);
        // 获取pOrgNo单位下子单位最大 orgNo（用 Long 避免 11+ 位编号溢出）
        Long maxOrgNo = saOrgList.stream().filter(item -> item.getPOrgNo().equals(pOrgNo))
                .map(SaOrg::getOrgNo)
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);

        SaOrg saOrg = new SaOrg();
        saOrg.setId(maxId + 1);
        saOrg.setOrgNo(maxOrgNo == 0L ? pOrgNo + "01" : (maxOrgNo + 1L) + "");
        saOrg.setOrgName(orgName);
        saOrg.setPOrgNo(pOrgNo);
        saOrg.setIsDelete(0);
        saOrg.setOrgTree(pSaOrgList.get(0).getOrgTree()+"|"+saOrg.getOrgNo());
        int result = saOrgMapper.insert(saOrg);
        if (result > 0) {
            return ResultBean.success("操作成功！");
        }
        return ResultBean.error("入库失败！");
    }

    @Override
    public void batchUpdateDisplay(Map<String, Boolean> displayMap) {
        for (Map.Entry<String, Boolean> entry : displayMap.entrySet()) {
            SaOrg org = new SaOrg();
            org.setIsDisplaySelected(entry.getValue() ? 1 : 0);
            saOrgMapper.update(org,
                    new LambdaUpdateWrapper<SaOrg>()
                            .eq(SaOrg::getOrgNo, entry.getKey())
            );
        }
    }

    @Override
    public String resolveDisplayOrgName(String orgNo, String fallbackName) {
        if (orgNo == null || orgNo.trim().isEmpty()) return fallbackName;
        Map<String, String> map = resolveDisplayOrgNames(Collections.singletonList(orgNo));
        String name = map.get(orgNo);
        return name != null ? name : fallbackName;
    }

    @Override
    public Map<String, String> resolveDisplayOrgNames(Collection<String> orgNos) {
        Map<String, String> result = new HashMap<>();
        if (orgNos == null || orgNos.isEmpty()) return result;

        // 查询全量非删除组织，建立 orgNo → SaOrg 映射（祖先节点可能不在入参集合中）
        List<SaOrg> allOrgs = this.list(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getIsDelete, 0));
        if (allOrgs == null || allOrgs.isEmpty()) return result;

        Map<String, SaOrg> orgMap = allOrgs.stream()
                .collect(Collectors.toMap(SaOrg::getOrgNo, o -> o, (a, b) -> a));

        for (String orgNo : orgNos) {
            if (orgNo == null || orgNo.trim().isEmpty()) continue;
            SaOrg userOrg = orgMap.get(orgNo);
            if (userOrg == null || userOrg.getOrgTree() == null || userOrg.getOrgTree().isEmpty()) {
                result.put(orgNo, userOrg != null ? userOrg.getOrgName() : null);
                continue;
            }
            // 自底向上遍历 org_tree 路径，找第一个 is_display_selected=1 的祖先
            String[] path = userOrg.getOrgTree().split("\\|");
            String resolvedName = null;
            for (int i = path.length - 1; i >= 0; i--) {
                SaOrg ancestor = orgMap.get(path[i]);
                if (ancestor != null && Integer.valueOf(1).equals(ancestor.getIsDisplaySelected())) {
                    resolvedName = ancestor.getOrgName();
                    break;
                }
            }
            // 链上无可见级（自身及全部祖先都被隐藏）时，回退到直属父级，
            // 与 applyDisplayOrg 的 fallback 一致：显示上一级，而非原始名
            if (resolvedName == null && path.length >= 2) {
                SaOrg parent = orgMap.get(path[path.length - 2]);
                if (parent != null) {
                    resolvedName = parent.getOrgName();
                }
            }
            result.put(orgNo, resolvedName != null ? resolvedName : userOrg.getOrgName());
        }
        return result;
    }

    @Override
    public String resolveOrgPath(String orgNo) {
        if (orgNo == null || orgNo.trim().isEmpty()) return null;
        // 加载全部组织（含已删除），通过 p_org_no 向上回溯构建完整路径，
        // 不依赖 org_tree 字段（该字段可能不完整）
        List<SaOrg> allOrgs = this.list(new LambdaQueryWrapper<SaOrg>());
        if (allOrgs == null || allOrgs.isEmpty()) return null;
        Map<String, SaOrg> orgMap = allOrgs.stream()
                .collect(Collectors.toMap(SaOrg::getOrgNo, o -> o, (a, b) -> a));
        SaOrg current = orgMap.get(orgNo);
        if (current == null) return null;
        // 从当前节点向上回溯，每级 prepend 到列表头部
        List<String> names = new ArrayList<>();
        while (current != null) {
            names.add(0, current.getOrgName());
            String pNo = current.getPOrgNo();
            current = (pNo != null && !pNo.trim().isEmpty()) ? orgMap.get(pNo) : null;
        }
        return names.isEmpty() ? null : String.join(" > ", names);
    }
}
