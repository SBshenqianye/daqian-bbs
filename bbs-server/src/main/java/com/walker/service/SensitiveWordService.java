package com.walker.service;

import com.walker.pojo.SensitiveWord;
import com.walker.vo.ResultBean;

import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/4/8 17:54
 * @PackageName:com.walker.service
 * @ClassName: SensitiveWordService
 * @Description: 敏感词接口层
 */
public interface SensitiveWordService {

    /**
     * 方法描述 查询敏感词列表（全量，用于刷新缓存）
     * @author chengQing
     * @date 2026/4/8 18:02
     * @return ResultBean
     */
    List<SensitiveWord> getList();

    /**
     * 方法描述 分页查询敏感词列表
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @param keyword 搜索关键词（可选，模糊匹配）
     * @return ResultBean 包含分页数据 {list, total, page, size}
     */
    ResultBean getPage(int page, int size, String keyword);

    /**
     * 方法描述 添加敏感词
     * @author chengQing
     * @date 2026/4/9 10:45
     * @param keyword 敏感词
     * @return ResultBean 返回封装类
     */
    ResultBean addSensitiveWord(String keyword);

    /**
     * 方法描述 批量添加敏感词（导入用，跳过已存在的）
     * @param keywords 敏感词列表
     * @return ResultBean 返回导入结果
     */
    ResultBean batchAdd(List<String> keywords);

    /**
     * 方法描述 删除敏感词
     * @author chengQing
     * @date 2026/4/9 11:02
     * @param id 敏感词id
     * @return ResultBean 返回封装类
     */
    ResultBean delSensitiveWord(Integer id);

    /**
     * 方法描述 修改敏感词是否参与脱敏遮蔽
     * @param id 敏感词id
     * @param enabled 1=前台打码，0=保留监控但不打码
     * @return ResultBean 返回封装类
     */
    ResultBean updateMaskEnabled(Integer id, Integer enabled);

    /**
     * 方法描述 修改敏感词分类
     * spam=垃圾词（内容照常显示、不计分、提示），sensitive=严禁词（默认打码）
     * @param id 敏感词id
     * @param category 分类 sensitive/spam
     * @return ResultBean 返回封装类
     */
    ResultBean updateCategory(Integer id, String category);

    /**
     * 方法描述 用一个动作同时设置分类与遮蔽（后台三档下拉）
     * mask=严禁词且打码，monitor=严禁词仅监控，warn=垃圾词警告放行
     * @param id 敏感词id
     * @param action mask/monitor/warn
     * @return ResultBean 返回封装类
     */
    ResultBean updateAction(Integer id, String action);

    /**
     * 方法描述 项目启动时加载敏感词
     * @author chengQing
     * @date 2026/4/9 15:01
     */
    void loadSensitiveWords();
}
