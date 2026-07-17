package com.walker.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.SystemConfigMapper;
import com.walker.pojo.SystemConfig;
import com.walker.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 系统配置 Service 实现
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Override
    public boolean saveConfig(SystemConfig config) {
        String now = DateUtil.formatDateTime(new Date());
        config.setCreateTime(now);
        return save(config);
    }

    @Override
    public boolean updateConfig(SystemConfig config) {
        String now = DateUtil.formatDateTime(new Date());
        config.setUpdateTime(now);
        return updateById(config);
    }

    @Override
    public boolean removeConfigById(Integer id) {
        return removeById(id);
    }

    @Override
    public SystemConfig getConfigById(Integer id) {
        return getById(id);
    }

    @Override
    public List<SystemConfig> listConfig() {
        return lambdaQuery().orderByAsc(SystemConfig::getSortOrder).list();
    }

    @Override
    public List<SystemConfig> listByGroup(String group) {
        return lambdaQuery()
                .eq(SystemConfig::getConfigGroup, group)
                .orderByAsc(SystemConfig::getSortOrder)
                .list();
    }

    @Override
    public SystemConfig getByKey(String configKey) {
        return lambdaQuery().eq(SystemConfig::getConfigKey, configKey).one();
    }
}
