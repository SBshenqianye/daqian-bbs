package com.walker.controller;

import com.walker.pojo.Dict;
import com.walker.service.DictService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/6 17:27
 * @PackageName:com.walker.controller
 * @ClassName: DictController
 * @Description: 数据字典控制层
 */
@Api(tags = "数据字典")
@RestController
public class DictController {
    @Autowired
    private DictService dictService;

    @ApiOperation(value = "新增字典")
    @PostMapping("/admin/addDict")
    public ResultBean addDict(@RequestBody @Valid Dict dict) {
        boolean ok = dictService.saveDict(dict);
        return ok ? ResultBean.success("新增成功！") : ResultBean.error("新增失败！");
    }

    @ApiOperation(value = "删除字典")
    @PostMapping("/admin/deleteDict")
    public ResultBean deleteDict(@RequestBody Dict dict) {
        if (dict.getId() == null) {
            return ResultBean.error("字典id不能为空！");
        }
        boolean ok = dictService.removeDictById(dict.getId());
        return ok ? ResultBean.success("删除成功！") : ResultBean.error("删除失败！");
    }

    @ApiOperation(value = "修改字典")
    @PostMapping("/admin/updateDict")
    public ResultBean updateDict(@RequestBody @Valid Dict dict) {
        if (dict.getId() == null) {
            return ResultBean.error("参数有误，id不能为空！");
        }
        boolean ok = dictService.updateDict(dict);
        return ok ? ResultBean.success("修改成功！") : ResultBean.error("修改失败！");
    }

    @ApiOperation(value = "根据id查询字典")
    @PostMapping("/common/getDictById")
    public ResultBean getDictById(@RequestParam Integer id) {
        if (id == null) {
            return ResultBean.error("字典id不能为空！");
        }
        Dict dict = dictService.getDictById(id);
        return dict != null ? ResultBean.success("查询成功！", dict) : ResultBean.error("字典不存在！");
    }

    @ApiOperation(value = "查询字典列表（不分页）")
    @PostMapping("/admin/listDict")
    public ResultBean listDict() {
        List<Dict> list = dictService.listDict();
        return ResultBean.success("成功获取！", list);
    }

    @ApiOperation(value = "根据字典类型查询数据字典列表")
    @PostMapping("/common/listDictByType")
    public ResultBean listDictByType(@RequestBody Dict dict) {
        if (StringUtils.isEmpty(dict.getDictType())) {
            return ResultBean.error("字典类型不能为空！");
        }
        List<Dict> list = dictService.listDictByType(dict.getDictType().trim());
        return ResultBean.success("成功获取！", list);
    }
}
