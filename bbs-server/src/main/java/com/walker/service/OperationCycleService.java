package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.walker.mapper.CycleConfigMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.mapper.OperationCycleMapper;
import com.walker.pojo.CycleConfig;
import com.walker.pojo.LevelProfile;
import com.walker.pojo.OperationCycle;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class OperationCycleService {

    @Autowired
    private OperationCycleMapper cycleMapper;
    @Autowired
    private CycleConfigMapper cycleConfigMapper;
    @Autowired
    private LevelProfileMapper levelProfileMapper;

    public ResultBean list() {
        return ResultBean.success("查询成功", cycleMapper.selectList(new LambdaQueryWrapper<OperationCycle>()
                .orderByDesc(OperationCycle::getStatus).orderByDesc(OperationCycle::getStartDate)));
    }

    public ResultBean save(OperationCycle cycle) {
        if (cycle == null || cycle.getCycleName() == null || cycle.getCycleName().trim().isEmpty()) {
            return ResultBean.error("周期名称不能为空");
        }
        if (cycle.getStartDate() == null || cycle.getEndDate() == null) {
            return ResultBean.error("起止日期不能为空");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (cycle.getCycleCode() == null || cycle.getCycleCode().trim().isEmpty()) {
            cycle.setCycleCode("CYCLE" + System.currentTimeMillis());
        }
        if (cycle.getStatus() == null) cycle.setStatus(0);
        if (cycle.getId() == null) {
            cycle.setCreateTime(now);
            cycle.setUpdateTime(now);
            cycleMapper.insert(cycle);
        } else {
            OperationCycle old = cycleMapper.selectById(cycle.getId());
            if (old == null) return ResultBean.error("周期不存在");
            if (!Integer.valueOf(0).equals(old.getStatus())) {
                return ResultBean.error("只有草稿周期可以编辑，请先复制为新周期");
            }
            cycle.setStatus(0);
            cycle.setUpdateTime(now);
            cycleMapper.updateById(cycle);
        }
        return ResultBean.success("保存成功", cycle.getId());
    }

    @Transactional
    public ResultBean activate(Integer id) {
        if (id == null) return ResultBean.error("缺少周期ID");
        OperationCycle cycle = cycleMapper.selectById(id);
        if (cycle == null) return ResultBean.error("周期不存在");
        cycleMapper.update(null, new LambdaUpdateWrapper<OperationCycle>()
                .set(OperationCycle::getStatus, 0));
        cycle.setStatus(1);
        cycleMapper.updateById(cycle);
        syncLevelProfileStatus(cycle.getLevelProfileId());
        return ResultBean.success("已激活");
    }

    /** 把当前启用周期的等级方案绑定改为指定方案（等级页切换方案时保持两处一致） */
    public void bindActiveCycleLevelProfile(Integer profileId) {
        if (profileId == null) return;
        OperationCycle active = getActiveCycle();
        if (active == null) return;
        cycleMapper.update(null, new LambdaUpdateWrapper<OperationCycle>()
                .eq(OperationCycle::getId, active.getId())
                .set(OperationCycle::getLevelProfileId, profileId));
    }

    /** 激活周期时反向同步等级方案的启用状态，避免周期与方案两处配置互相覆盖 */
    private void syncLevelProfileStatus(Integer profileId) {
        if (profileId == null) return;
        LevelProfile target = levelProfileMapper.selectById(profileId);
        if (target == null) return;
        levelProfileMapper.update(null, new LambdaUpdateWrapper<LevelProfile>()
                .set(LevelProfile::getStatus, 0));
        target.setStatus(1);
        target.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        levelProfileMapper.updateById(target);
    }

    public ResultBean listConfigs(Integer cycleId) {
        if (cycleId == null) return ResultBean.error("缺少周期ID");
        return ResultBean.success("查询成功", cycleConfigMapper.selectList(new LambdaQueryWrapper<CycleConfig>()
                .eq(CycleConfig::getCycleId, cycleId).orderByAsc(CycleConfig::getConfigKey)));
    }

    public ResultBean saveConfig(Integer cycleId, String key, String value) {
        if (cycleId == null || key == null || key.trim().isEmpty()) {
            return ResultBean.error("参数不完整");
        }
        CycleConfig existing = cycleConfigMapper.selectOne(new LambdaQueryWrapper<CycleConfig>()
                .eq(CycleConfig::getCycleId, cycleId)
                .eq(CycleConfig::getConfigKey, key.trim())
                .last("LIMIT 1"));
        if (existing == null) {
            existing = new CycleConfig();
            existing.setCycleId(cycleId);
            existing.setConfigKey(key.trim());
            existing.setConfigValue(value);
            cycleConfigMapper.insert(existing);
        } else {
            existing.setConfigValue(value);
            cycleConfigMapper.updateById(existing);
        }
        return ResultBean.success("保存成功");
    }

    public Integer currentCycleId() {
        try { return cycleMapper.selectActiveCycleId(); } catch (Exception e) { return null; }
    }

    public String getConfig(String key) {
        try { return cycleMapper.selectActiveCycleConfig(key); } catch (Exception e) { return null; }
    }

    /** 当前启用周期；没有则返回 null。 */
    public OperationCycle getActiveCycle() {
        List<OperationCycle> list = cycleMapper.selectList(new LambdaQueryWrapper<OperationCycle>()
                .eq(OperationCycle::getStatus, 1)
                .orderByDesc(OperationCycle::getId)
                .last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** 复制周期：日期、配置、等级方案一并带过去，新周期为草稿。 */
    @Transactional
    public ResultBean copy(Integer sourceId, String name) {
        if (sourceId == null) return ResultBean.error("缺少源周期ID");
        OperationCycle source = cycleMapper.selectById(sourceId);
        if (source == null) return ResultBean.error("源周期不存在");
        String newName = name == null || name.trim().isEmpty()
                ? source.getCycleName() + " 副本" : name.trim();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        OperationCycle copy = new OperationCycle();
        copy.setCycleCode("CYCLE" + System.currentTimeMillis());
        copy.setCycleName(newName);
        copy.setStartDate(source.getStartDate());
        copy.setEndDate(source.getEndDate());
        copy.setLevelProfileId(source.getLevelProfileId());
        copy.setStatus(0);
        copy.setCreateTime(now);
        copy.setUpdateTime(now);
        cycleMapper.insert(copy);

        List<CycleConfig> configs = cycleConfigMapper.selectList(new LambdaQueryWrapper<CycleConfig>()
                .eq(CycleConfig::getCycleId, sourceId));
        for (CycleConfig config : configs) {
            CycleConfig row = new CycleConfig();
            row.setCycleId(copy.getId());
            row.setConfigKey(config.getConfigKey());
            row.setConfigValue(config.getConfigValue());
            cycleConfigMapper.insert(row);
        }
        return ResultBean.success("已复制为「" + newName + "」", copy.getId());
    }

    /** 归档周期：启用中的周期需先激活其他周期。 */
    public ResultBean archive(Integer id) {
        if (id == null) return ResultBean.error("缺少周期ID");
        OperationCycle cycle = cycleMapper.selectById(id);
        if (cycle == null) return ResultBean.error("周期不存在");
        if (Integer.valueOf(1).equals(cycle.getStatus())) {
            return ResultBean.error("启用中的周期不能归档，请先激活其他周期");
        }
        if (Integer.valueOf(2).equals(cycle.getStatus())) {
            return ResultBean.error("该周期已归档");
        }
        cycle.setStatus(2);
        cycle.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        cycleMapper.updateById(cycle);
        return ResultBean.success("已归档");
    }
}
