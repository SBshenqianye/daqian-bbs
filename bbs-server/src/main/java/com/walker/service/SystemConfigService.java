package com.walker.service;

import com.walker.pojo.SystemConfig;

import java.util.List;

/**
 * 系统配置 Service 接口
 */
public interface SystemConfigService {

    /**
     * 新增配置
     */
    boolean saveConfig(SystemConfig config);

    /**
     * 修改配置
     */
    boolean updateConfig(SystemConfig config);

    /**
     * 删除配置
     */
    boolean removeConfigById(Integer id);

    /**
     * 根据 ID 查询
     */
    SystemConfig getConfigById(Integer id);

    /**
     * 查询所有配置
     */
    List<SystemConfig> listConfig();

    /**
     * 根据分组查询
     */
    List<SystemConfig> listByGroup(String group);

    /**
     * 根据配置键查询
     */
    SystemConfig getByKey(String configKey);
}
