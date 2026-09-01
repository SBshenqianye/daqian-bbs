package com.walker.service;

import com.walker.pojo.Dict;

import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/6 17:29
 * @PackageName:com.walker.service
 * @ClassName: DictService
 * @Description: 数据字典接口层
 */
public interface DictService {

    /**
     * 新增字典
     */
    boolean saveDict(Dict dict);

    /**
     * 修改字典
     */
    boolean updateDict(Dict dict);

    /**
     * 根据id删除字典
     */
    boolean removeDictById(Integer id);

    /**
     * 根据id查询字典
     */
    Dict getDictById(Integer id);

    /**
     * 查询字典列表（不分页）
     */
    List<Dict> listDict();

    /**
     * 根据字典类型查询数据字典列表
     */
    List<Dict> listDictByType(String dictType);

    /**
     * 根据字典键获取值（统一查询入口）
     */
    String getValueByKey(String key);
}
