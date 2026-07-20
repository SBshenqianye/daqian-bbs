package com.walker.controller;

import com.walker.pojo.SystemConfig;
import com.walker.service.SystemConfigService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制层
 */
@Api(tags = "系统配置")
@RestController
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @ApiOperation(value = "新增配置")
    @PostMapping("/admin/systemConfig/add")
    public ResultBean addConfig(@RequestBody SystemConfig config) {
        if (config == null || StringUtils.isEmpty(config.getConfigKey())) {
            return ResultBean.error("配置键不能为空！");
        }
        // 检查 key 是否已存在
        SystemConfig exist = systemConfigService.getByKey(config.getConfigKey().trim());
        if (exist != null) {
            return ResultBean.error("配置键已存在！");
        }
        config.setConfigKey(config.getConfigKey().trim());
        boolean ok = systemConfigService.saveConfig(config);
        return ok ? ResultBean.success("新增成功！") : ResultBean.error("新增失败！");
    }

    @ApiOperation(value = "修改配置")
    @PostMapping("/admin/systemConfig/update")
    public ResultBean updateConfig(@RequestBody SystemConfig config) {
        if (config == null || config.getId() == null) {
            return ResultBean.error("参数有误，id不能为空！");
        }
        boolean ok = systemConfigService.updateConfig(config);
        return ok ? ResultBean.success("修改成功！") : ResultBean.error("修改失败！");
    }

    @ApiOperation(value = "删除配置")
    @PostMapping("/admin/systemConfig/delete")
    public ResultBean deleteConfig(@RequestBody SystemConfig config) {
        if (config.getId() == null) {
            return ResultBean.error("配置id不能为空！");
        }
        boolean ok = systemConfigService.removeConfigById(config.getId());
        return ok ? ResultBean.success("删除成功！") : ResultBean.error("删除失败！");
    }

    @ApiOperation(value = "查询配置列表（不分页）")
    @PostMapping("/admin/systemConfig/list")
    public ResultBean listConfig() {
        List<SystemConfig> list = systemConfigService.listConfig();
        return ResultBean.success("成功获取！", list);
    }

    @ApiOperation(value = "根据分组查询配置列表")
    @PostMapping("/common/systemConfig/listByGroup")
    public ResultBean listByGroup(@RequestBody SystemConfig param) {
        if (StringUtils.isEmpty(param.getConfigGroup())) {
            return ResultBean.error("配置分组不能为空！");
        }
        List<SystemConfig> list = systemConfigService.listByGroup(param.getConfigGroup().trim());
        return ResultBean.success("成功获取！", list);
    }

    @ApiOperation(value = "根据配置键查询")
    @PostMapping("/common/systemConfig/getByKey")
    public ResultBean getByKey(@RequestBody SystemConfig param) {
        if (StringUtils.isEmpty(param.getConfigKey())) {
            return ResultBean.error("配置键不能为空！");
        }
        SystemConfig config = systemConfigService.getByKey(param.getConfigKey().trim());
        return config != null ? ResultBean.success("查询成功！", config) : ResultBean.error("配置不存在！");
    }
}
