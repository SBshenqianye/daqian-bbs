package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.SensitiveWordMapper;
import com.walker.pojo.SensitiveWord;
import com.walker.service.SensitiveWordService;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.ResultBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author chengQing
 * @Date 2026/4/8 17:54
 * @PackageName:com.walker.service.impl
 * @ClassName: SensitiveWordServiceImpl
 * @Description: 敏感词接口层实现类
 */
@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Override
    public List<SensitiveWord> getList() {
        return sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                .orderByAsc(SensitiveWord::getId)
        );
    }

    @Override
    public ResultBean getPage(int page, int size, String keyword) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(SensitiveWord::getKeyword, keyword.trim());
        }
        wrapper.orderByAsc(SensitiveWord::getId);

        Page<SensitiveWord> pageParam = new Page<>(page, size);
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return ResultBean.success("成功获取！", data);
    }

    @Override
    public ResultBean addSensitiveWord(String keyword) {
        // 先查询敏感词是否已存在
        Long count = sensitiveWordMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getKeyword, keyword)
        );
        if (count > 0) {
            return ResultBean.error("添加失败！敏感词已存在！");
        }
        // 组装数据并添加到数据库
        SensitiveWord sensitiveWord = new SensitiveWord();
        sensitiveWord.setKeyword(keyword);
        sensitiveWord.setMaskEnabled(1);
        sensitiveWord.setCategory("sensitive");
        int result = sensitiveWordMapper.insert(sensitiveWord);
        if (result > 0) {
            // 刷新敏感词列表
            refresh();
            return ResultBean.success("添加成功！");
        }
        return ResultBean.error("添加失败！");
    }

    @Override
    public ResultBean batchAdd(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return ResultBean.error("导入数据为空！");
        }

        int addedCount = 0;
        int skippedCount = 0;
        int duplicateCount = 0;

        for (String keyword : keywords) {
            if (StringUtils.isBlank(keyword)) {
                skippedCount++;
                continue;
            }
            String trimmed = keyword.trim();
            // 检查是否已存在
            Long count = sensitiveWordMapper.selectCount(
                    new LambdaQueryWrapper<SensitiveWord>()
                            .eq(SensitiveWord::getKeyword, trimmed)
            );
            if (count > 0) {
                duplicateCount++;
                continue;
            }
            SensitiveWord word = new SensitiveWord();
            word.setKeyword(trimmed);
            word.setMaskEnabled(1);
            word.setCategory("sensitive");
            int result = sensitiveWordMapper.insert(word);
            if (result > 0) {
                addedCount++;
            }
        }

        // 刷新缓存
        if (addedCount > 0) {
            refresh();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", keywords.size());
        data.put("addedCount", addedCount);
        data.put("duplicateCount", duplicateCount);
        data.put("skippedCount", skippedCount);

        String msg = String.format("导入完成：新增 %d 条，重复跳过 %d 条，空白跳过 %d 条",
                addedCount, duplicateCount, skippedCount);
        return ResultBean.success(msg, data);
    }

    @Override
    public ResultBean delSensitiveWord(Integer id) {
        int result = sensitiveWordMapper.deleteById(id);
        if (result > 0) {
            // 刷新敏感词列表
            refresh();
            return ResultBean.success("删除成功！");
        }
        return ResultBean.error("删除失败！");
    }

    /**
     * PostConstruct 启动加载
     */
    @PostConstruct
    @Override
    public void loadSensitiveWords() {
        List<SensitiveWord> list = this.list();

        // 严禁词（category 为空按严禁词处理，兼容历史数据）：参与监控、命中采集与风险统计
        List<String> words = list.stream()
                .filter(word -> !isSpam(word))
                .map(SensitiveWord::getKeyword)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        // 其中 mask_enabled=1 的才在前台打码
        List<String> maskable = list.stream()
                .filter(word -> !isSpam(word))
                .filter(word -> word.getMaskEnabled() == null || word.getMaskEnabled() == 1)
                .map(SensitiveWord::getKeyword)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        // 垃圾/灌水词：不打码，命中即"警告放行"（内容可见但不计分）
        List<String> spam = list.stream()
                .filter(this::isSpam)
                .map(SensitiveWord::getKeyword)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        // 存入工具类（静态变量）
        SensitiveWordUtil.updateSensitiveWords(words, maskable, spam);
    }

    private boolean isSpam(SensitiveWord word) {
        return word != null && "spam".equalsIgnoreCase(word.getCategory());
    }

    @Override
    public ResultBean updateAction(Integer id, String action) {
        if (id == null) {
            return ResultBean.error("敏感词id不能为空！");
        }
        String normalized = action == null ? "" : action.trim().toLowerCase();
        if (!"mask".equals(normalized) && !"monitor".equals(normalized) && !"warn".equals(normalized)) {
            return ResultBean.error("处置方式有误，只能是 mask / monitor / warn！");
        }
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            return ResultBean.error("敏感词不存在！");
        }
        boolean spam = "warn".equals(normalized);
        word.setCategory(spam ? "spam" : "sensitive");
        word.setMaskEnabled("mask".equals(normalized) ? 1 : 0);
        int result = sensitiveWordMapper.updateById(word);
        if (result > 0) {
            refresh();
            if ("mask".equals(normalized)) {
                return ResultBean.success("已设为严禁词并打码！");
            }
            if ("monitor".equals(normalized)) {
                return ResultBean.success("已设为严禁词、仅监控（不打码，仍计入风险）！");
            }
            return ResultBean.success("已设为垃圾词（警告放行，不计分）！");
        }
        return ResultBean.error("修改失败！");
    }

    @Override
    public ResultBean updateCategory(Integer id, String category) {
        if (id == null) {
            return ResultBean.error("敏感词id不能为空！");
        }
        if (!"spam".equalsIgnoreCase(category) && !"sensitive".equalsIgnoreCase(category)) {
            return ResultBean.error("分类参数有误，只能是 sensitive 或 spam！");
        }
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            return ResultBean.error("敏感词不存在！");
        }
        boolean spam = "spam".equalsIgnoreCase(category);
        word.setCategory(spam ? "spam" : "sensitive");
        // 垃圾词不参与打码；改为严禁词时默认打码，之后可单独关闭
        word.setMaskEnabled(spam ? 0 : 1);
        int result = sensitiveWordMapper.updateById(word);
        if (result > 0) {
            refresh();
            return ResultBean.success(spam ? "已改为垃圾词（警告放行）！" : "已改为严禁词！");
        }
        return ResultBean.error("修改失败！");
    }

    @Override
    public ResultBean updateMaskEnabled(Integer id, Integer enabled) {
        if (id == null) {
            return ResultBean.error("敏感词id不能为空！");
        }
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            return ResultBean.error("遮蔽开关参数有误！");
        }
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            return ResultBean.error("敏感词不存在！");
        }
        if (enabled == 1 && "spam".equalsIgnoreCase(word.getCategory())) {
            return ResultBean.error("垃圾词走警告放行，不参与打码；如需打码请先改为严禁词！");
        }
        word.setMaskEnabled(enabled);
        int result = sensitiveWordMapper.updateById(word);
        if (result > 0) {
            refresh();
            return ResultBean.success(enabled == 1 ? "已开启遮蔽！" : "已关闭遮蔽（仍保留监控）！");
        }
        return ResultBean.error("修改失败！");
    }

    /**
     * 方法描述 重新加载敏感词
     * @author chengQing
     * @date 2026/4/9 15:05
     */
    public void refresh() {
        // 重新加载敏感词并更新工具类
        loadSensitiveWords();
    }
}
