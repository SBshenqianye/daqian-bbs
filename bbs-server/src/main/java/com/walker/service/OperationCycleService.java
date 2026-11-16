package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.walker.mapper.CycleConfigMapper;
import com.walker.mapper.OperationCycleMapper;
import com.walker.pojo.CycleConfig;
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
        return ResultBean.success("已激活");
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
}
