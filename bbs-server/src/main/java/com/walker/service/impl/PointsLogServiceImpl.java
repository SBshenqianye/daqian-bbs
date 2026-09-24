package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.PointsLogMapper;
import com.walker.pojo.PointsLog;
import com.walker.service.PointsLogService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 积分调整日志服务实现类
 */
@Service
public class PointsLogServiceImpl extends ServiceImpl<PointsLogMapper, PointsLog> implements PointsLogService {

    @Autowired
    private PointsLogMapper pointsLogMapper;

    @Override
    public ResultBean addPointsLog(PointsLog pointsLog) {
        if (pointsLog.getUserId() == null || pointsLog.getPointsChange() == null) {
            return ResultBean.error("用户ID和积分变动不能为空");
        }
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        pointsLog.setCreateTime(format.format(date));
        this.save(pointsLog);
        return ResultBean.success("积分调整成功");
    }

    @Override
    public Integer getPointsAdjustment(Integer userId) {
        return pointsLogMapper.sumPointsChangeByUserId(userId);
    }

    @Override
    public ResultBean adjustUserPoints(Integer userId, Integer pointsChange, String reason,
                                       String relatedType, Integer relatedId, Integer operatorId) {
        if (userId == null || pointsChange == null) {
            return ResultBean.error("用户ID和积分变动不能为空");
        }
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPointsChange(pointsChange);
        log.setReason(reason);
        log.setRelatedType(relatedType);
        log.setRelatedId(relatedId);
        log.setOperatorId(operatorId);
        return addPointsLog(log);
    }

    @Override
    @Transactional
    public ResultBean undoPointsLog(Integer logId, Integer operatorId) {
        if (logId == null) {
            return ResultBean.error("记录ID不能为空");
        }
        // 查询原始记录
        PointsLog original = this.getById(logId);
        if (original == null) {
            return ResultBean.error("积分调整记录不存在");
        }
        if (original.getIsReversed() != null && original.getIsReversed() == 1) {
            return ResultBean.error("该记录已被撤销，不能重复撤销");
        }
        if ("undo".equals(original.getRelatedType())) {
            return ResultBean.error("撤销记录本身不能被撤销");
        }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = format.format(date);

        // 1. 创建反向记录
        PointsLog undoLog = new PointsLog();
        undoLog.setUserId(original.getUserId());
        undoLog.setPointsChange(-original.getPointsChange());
        undoLog.setReason("撤销积分调整");
        undoLog.setRelatedType("undo");
        undoLog.setRelatedId(logId);
        undoLog.setOperatorId(operatorId);
        undoLog.setCreateTime(now);
        undoLog.setReversingRecord(logId);
        this.save(undoLog);

        // 2. 标记原记录为已撤销
        original.setIsReversed(1);
        original.setReversedBy(undoLog.getId());
        this.updateById(original);

        return ResultBean.success("撤销成功");
    }

    @Override
    public ResultBean getUserPointsLog(Integer userId, Integer page, Integer size) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        Page<PointsLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsLog::getUserId, userId);
        wrapper.orderByDesc(PointsLog::getCreateTime);
        Page<PointsLog> result = this.page(pageParam, wrapper);
        // #18 用户端不暴露自增 id：转 Map 输出，剔除 id/userId/operatorId 等内部字段；
        // 撤销记录(relatedType=undo)若被撤销的原记录也在本页，给出同页锚点 undoAnchor 供前端滚动定位。
        List<PointsLog> rows = result.getRecords();
        Map<Integer, Integer> idToIdx = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId() != null) {
                idToIdx.put(rows.get(i).getId(), i);
            }
        }
        // 双向配对：key=本页索引, value=配对记录索引（撤销记录↔原记录，双向可跳转）
        Map<Integer, Integer> pair = new HashMap<>();
        // 1) reversingRecord 显式指定
        for (int i = 0; i < rows.size(); i++) {
            Integer targetId = rows.get(i).getReversingRecord();
            if (targetId != null && idToIdx.containsKey(targetId)) {
                pair.put(i, idToIdx.get(targetId));
                pair.put(idToIdx.get(targetId), i);
            }
        }
        // 2) reason 含"扣回"的扣分记录，本页内找同 relatedType+relatedId、分值相反的最近正分
        for (int i = 0; i < rows.size(); i++) {
            if (pair.containsKey(i)) continue;
            PointsLog r = rows.get(i);
            if (r.getPointsChange() != null && r.getPointsChange() < 0
                    && r.getReason() != null && r.getReason().contains("扣回")) {
                for (int j = i - 1; j >= 0; j--) {
                    PointsLog p = rows.get(j);
                    if (!pair.containsValue(j)
                            && p.getPointsChange() != null && p.getPointsChange() > 0
                            && Integer.valueOf(-r.getPointsChange()).equals(p.getPointsChange())
                            && java.util.Objects.equals(r.getRelatedType(), p.getRelatedType())
                            && java.util.Objects.equals(r.getRelatedId(), p.getRelatedId())) {
                        pair.put(i, j);
                        break;
                    }
                }
            }
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            PointsLog r = rows.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pointsChange", r.getPointsChange());
            m.put("reason", r.getReason());
            m.put("relatedType", r.getRelatedType());
            m.put("createTime", r.getCreateTime());
            m.put("isReversed", r.getIsReversed());
            m.put("undoAnchor", pair.containsKey(i) ? "log-" + pair.get(i) : null);
            records.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public int countReplyPointsForArticle(Integer userId, Integer articleId) {
        if (userId == null || articleId == null) {
            return 0;
        }
        Integer count = pointsLogMapper.countReplyPointsForArticle(userId, articleId);
        return count != null ? count : 0;
    }

    @Override
    public int countAdoptPointsForArticle(Integer userId, Integer articleId) {
        if (userId == null || articleId == null) {
            return 0;
        }
        Integer count = pointsLogMapper.countAdoptPointsForArticle(userId, articleId);
        return count != null ? count : 0;
    }

    @Override
    public int countSuggestionAdoptForArticle(Integer articleId) {
        if (articleId == null) {
            return 0;
        }
        Integer count = pointsLogMapper.countSuggestionAdoptForArticle(articleId);
        return count != null ? count : 0;
    }
}
