package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.AppealMapper;
import com.walker.pojo.Appeal;
import com.walker.service.AppealService;
import com.walker.service.NotificationService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AppealServiceImpl extends ServiceImpl<AppealMapper, Appeal> implements AppealService {

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public ResultBean submitAppeal(Integer userId, String appealType, Integer relatedId, String content) {
        if (userId == null || appealType == null || content == null || content.isEmpty()) {
            return ResultBean.error("参数不完整");
        }

        // 检查是否有待审核的申诉
        long count = this.count(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getUserId, userId)
                .eq(Appeal::getAppealType, appealType)
                .eq(relatedId != null, Appeal::getRelatedId, relatedId)
                .eq(Appeal::getStatus, "pending"));
        if (count > 0) {
            return ResultBean.error("您已有待审核的申诉，请等待处理");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Appeal appeal = new Appeal();
        appeal.setUserId(userId);
        appeal.setAppealType(appealType);
        appeal.setRelatedId(relatedId);
        appeal.setContent(content);
        appeal.setStatus("pending");
        appeal.setCreateTime(fmt.format(now));
        this.save(appeal);

        return ResultBean.success("申诉已提交，等待审核");
    }

    @Override
    @Transactional
    public ResultBean reviewAppeal(Integer appealId, Integer reviewerId, String status, String remark) {
        if (appealId == null || reviewerId == null || status == null) {
            return ResultBean.error("参数不完整");
        }

        Appeal appeal = this.getById(appealId);
        if (appeal == null) {
            return ResultBean.error("申诉记录不存在");
        }
        if (!"pending".equals(appeal.getStatus())) {
            return ResultBean.error("该申诉已处理");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        appeal.setStatus(status);
        appeal.setReviewerId(reviewerId);
        appeal.setReviewTime(fmt.format(now));
        appeal.setReviewRemark(remark);
        this.updateById(appeal);

        // 通知申诉人
        String title = "accepted".equals(status) ? "您的申诉已通过" : "您的申诉已被驳回";
        notificationService.createNotification(appeal.getUserId(), reviewerId,
                "appeal_review", title, "appeal", appealId);

        return ResultBean.success("审核完成");
    }

    @Override
    public ResultBean listAppeals(String status, Integer page, Integer size) {
        Page<Appeal> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Appeal::getStatus, status);
        }
        wrapper.orderByDesc(Appeal::getCreateTime);
        Page<Appeal> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listMyAppeals(Integer userId, Integer page, Integer size) {
        Page<Appeal> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appeal::getUserId, userId);
        wrapper.orderByDesc(Appeal::getCreateTime);
        Page<Appeal> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }
}
