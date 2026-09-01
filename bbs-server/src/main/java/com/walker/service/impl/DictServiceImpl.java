package com.walker.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.DictMapper;
import com.walker.pojo.Dict;
import com.walker.service.DictService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/6 17:30
 * @PackageName:com.walker.service.impl
 * @ClassName: DictServiceImpl
 * @Description: 字典管理实现层
 */
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Override
    public boolean saveDict(Dict dict) {
        String currentDateTime = DateUtil.formatDateTime(new Date());
        dict.setCreateTime(currentDateTime);
        return save(dict);
    }

    @Override
    public boolean updateDict(Dict dict) {
        String currentDateTime = DateUtil.formatDateTime(new Date());
        dict.setUpdateTime(currentDateTime);
        return updateById(dict);
    }

    @Override
    public boolean removeDictById(Integer id) {
        return removeById(id);
    }

    @Override
    public Dict getDictById(Integer id) {
        return getById(id);
    }

    @Override
    public List<Dict> listDict() {
        return list();
    }

    @Override
    public List<Dict> listDictByType(String dictType) {
        return lambdaQuery().eq(Dict::getDictType, dictType).list();
    }

    @Override
    public String getValueByKey(String key) {
        return getBaseMapper().selectValueByKey(key);
    }
}
