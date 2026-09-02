package com.walker.service;

import com.walker.mapper.SystemConfigMapper;
import com.walker.pojo.SystemConfig;
import com.walker.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = systemConfigService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(systemConfigService, systemConfigMapper);
    }

    @Test
    @DisplayName("保存配置 → 设置创建时间并保存")
    void saveConfig_setsCreateTime() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("site_name");
        config.setConfigValue("大千智荟");
        when(systemConfigMapper.insert(any(SystemConfig.class))).thenReturn(1);

        boolean result = systemConfigService.saveConfig(config);
        assertTrue(result);
        assertNotNull(config.getCreateTime());
    }

    @Test
    @DisplayName("更新配置 → 设置更新时间并更新")
    void updateConfig_setsUpdateTime() {
        SystemConfig config = new SystemConfig();
        config.setId(1);
        config.setConfigValue("新名称");
        when(systemConfigMapper.updateById(any(SystemConfig.class))).thenReturn(1);

        boolean result = systemConfigService.updateConfig(config);
        assertTrue(result);
        assertNotNull(config.getUpdateTime());
    }

    @Test
    @Disabled("MyBatis-Plus SqlHelper requires Spring context for removeById")
    @DisplayName("删除配置 → 成功")
    void removeConfigById_succeeds() {
        when(systemConfigMapper.deleteById(1)).thenReturn(1);
        assertTrue(systemConfigService.removeConfigById(1));
    }

    @Test
    @Disabled("MyBatis-Plus SqlHelper requires Spring context for removeById")
    @DisplayName("删除配置 → 失败")
    void removeConfigById_fails() {
        when(systemConfigMapper.deleteById(999)).thenReturn(0);
        assertFalse(systemConfigService.removeConfigById(999));
    }

    @Test
    @DisplayName("获取配置 → 存在 → 返回配置")
    void getConfigById_found_returnsConfig() {
        SystemConfig config = new SystemConfig();
        config.setId(1);
        config.setConfigKey("site_name");
        when(systemConfigMapper.selectById(1)).thenReturn(config);

        SystemConfig result = systemConfigService.getConfigById(1);
        assertNotNull(result);
        assertEquals("site_name", result.getConfigKey());
    }

    @Test
    @DisplayName("获取配置 → 不存在 → 返回null")
    void getConfigById_notFound_returnsNull() {
        when(systemConfigMapper.selectById(999)).thenReturn(null);
        assertNull(systemConfigService.getConfigById(999));
    }
}
