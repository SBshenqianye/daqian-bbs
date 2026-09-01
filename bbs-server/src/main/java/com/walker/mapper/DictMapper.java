package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Author chengQing
 * @Date 2026/3/6 17:32
 * @PackageName:com.walker.mapper
 * @ClassName: DictMapper
 * @Description: 数据字典持久层
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {

    /**
     * 根据字典类型和值获取标签
     */
    @Select("SELECT dict_label FROM bbs_dict WHERE dict_type = #{type} AND dict_value = #{value} LIMIT 1")
    String selectLabelByTypeAndValue(@Param("type") String type, @Param("value") String value);

    /**
     * 根据字典类型获取第一条的dict_value（用于获取配置值如阈值）
     */
    @Select("SELECT dict_value FROM bbs_dict WHERE dict_type = #{type} ORDER BY dict_sort LIMIT 1")
    String selectValueByType(@Param("type") String type);

    /**
     * 根据字典键获取dict_value（统一查询入口）
     */
    @Select("SELECT dict_value FROM bbs_dict WHERE dict_key = #{key} LIMIT 1")
    String selectValueByKey(@Param("key") String key);
}
