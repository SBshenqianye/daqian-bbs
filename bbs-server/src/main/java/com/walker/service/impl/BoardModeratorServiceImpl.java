package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.BoardModeratorMapper;
import com.walker.mapper.ModeratorRewardCancelMapper;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.ModeratorRewardCancel;
import com.walker.pojo.PointsLog;
import com.walker.pojo.User;
import com.walker.service.BoardModeratorService;
import com.walker.service.DictService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.UserService;
import com.walker.utils.ConstantUtil;
import com.walker.vo.ResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoardModeratorServiceImpl extends ServiceImpl<BoardModeratorMapper, BoardModerator> implements BoardModeratorService {

    private static final Logger log = LoggerFactory.getLogger(BoardModeratorServiceImpl.class);

    @Autowired
    private BoardModeratorMapper boardModeratorMapper;

    @Autowired
    private ModeratorRewardCancelMapper cancelMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DictService dictService;

    @Override
    @Transactional
    public ResultBean appoint(Integer userId, Integer labelId, Integer operatorId) {
        if (userId == null || labelId == null) {
            return ResultBean.error("参数不完整");
        }

        // 检查是否已有有效版主
        BoardModerator existing = boardModeratorMapper.findByUserAndLabel(userId, labelId);
        if (existing != null) {
            return ResultBean.error("该用户已是此版块版主");
        }

        // 检查累计积分是否>=300
        Integer totalPoints = pointsLogService.getPointsAdjustment(userId);
        if (totalPoints == null || totalPoints < 300) {
            return ResultBean.error("该用户累计积分不足300分，无法任命为版主");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        BoardModerator moderator = new BoardModerator();
        moderator.setUserId(userId);
        moderator.setLabelId(labelId);
        moderator.setRoleType("moderator");
        moderator.setStatus(1);
        moderator.setAppointTime(fmt.format(now));
        moderator.setCreateTime(fmt.format(now));
        this.save(moderator);

        return ResultBean.success("版主任命成功");
    }

    @Override
    @Transactional
    public ResultBean dismiss(Integer userId, Integer labelId) {
        if (userId == null || labelId == null) {
            return ResultBean.error("参数不完整");
        }

        BoardModerator existing = boardModeratorMapper.findByUserAndLabel(userId, labelId);
        if (existing == null) {
            return ResultBean.error("该用户不是此版块版主");
        }

        existing.setStatus(0);
        this.updateById(existing);

        return ResultBean.success("版主已撤销");
    }

    @Override
    public ResultBean listModerators(Integer page, Integer size) {
        Page<BoardModerator> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BoardModerator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BoardModerator::getStatus, 1);
        wrapper.orderByDesc(BoardModerator::getAppointTime);
        Page<BoardModerator> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public boolean isModerator(Integer userId, Integer labelId) {
        BoardModerator mod = boardModeratorMapper.findByUserAndLabel(userId, labelId);
        return mod != null;
    }

    @Override
    @Transactional
    public ResultBean monthlyReward(Integer operatorId) {
        return doMonthlyReward(operatorId, false);
    }

    /**
     * 内部统一发放逻辑（手动/自动共用）
     * @param operatorId 操作人ID
     * @param isAuto 是否为自动发放
     */
    private ResultBean doMonthlyReward(Integer operatorId, boolean isAuto) {
        // 1. 查询所有有效版主
        LambdaQueryWrapper<BoardModerator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BoardModerator::getStatus, 1);
        List<BoardModerator> moderators = this.list(wrapper);

        if (moderators.isEmpty()) {
            return ResultBean.success("当前无有效版主，无需发放奖励");
        }

        // 2. 去重（同一用户可能任多个版块版主，每月只发一次）
        Set<Integer> uniqueModeratorIds = moderators.stream()
                .map(BoardModerator::getUserId)
                .collect(Collectors.toSet());

        // 3. 获取当前月份（用于检查已发放 + 取消记录）
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());

        // 4. 获取本月取消列表
        List<Integer> cancelledUserIds = cancelMapper.findCancelledUserIds(currentMonth);

        int rewardPoints = 15;
        int rewarded = 0;
        List<Integer> alreadyRewarded = new ArrayList<>();
        List<Integer> cancelled = new ArrayList<>();

        for (Integer userId : uniqueModeratorIds) {
            // 检查本月是否已发放过版主履职奖励
            LambdaQueryWrapper<PointsLog> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(PointsLog::getUserId, userId)
                    .likeRight(PointsLog::getCreateTime, currentMonth)
                    .eq(PointsLog::getReason, "版主月度履职奖励");
            long existingCount = pointsLogService.count(checkWrapper);

            if (existingCount > 0) {
                alreadyRewarded.add(userId);
                continue;
            }

            // 检查是否被取消
            if (cancelledUserIds.contains(userId)) {
                cancelled.add(userId);
                continue;
            }

            // 发放15积分
            pointsLogService.adjustUserPoints(userId, rewardPoints, "版主月度履职奖励",
                    "moderator_reward", null, operatorId);

            // 通知版主
            notificationService.createNotification(userId, operatorId, "moderator_reward",
                    "恭喜！您获得本月版主履职奖励 +" + rewardPoints + "积分",
                    "user", userId);

            rewarded++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalModerators", uniqueModeratorIds.size());
        data.put("rewarded", rewarded);
        data.put("alreadyRewarded", alreadyRewarded.size());
        data.put("cancelled", cancelled.size());
        data.put("isAuto", isAuto);

        String prefix = isAuto ? "[自动发放] " : "";
        String msg = prefix + "版主履职奖励发放完成：共" + uniqueModeratorIds.size() + "位版主，"
                + "发放" + rewarded + "人";
        if (!alreadyRewarded.isEmpty()) {
            msg += "，" + alreadyRewarded.size() + "人本月已发放跳过";
        }
        if (!cancelled.isEmpty()) {
            msg += "，" + cancelled.size() + "人已被取消";
        }
        return ResultBean.success(msg, data);
    }

    /**
     * 每月1日凌晨1点自动发放（默认发放日，可通过数据字典 moderator_reward_day 修改）
     * 检查：自动发放开关打开 + 今天是发放日 + 本月尚未发放
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void scheduledMonthlyReward() {
        try {
            // 检查自动发放开关（dict_value = '1' 表示开启）
            String enabled = dictService.getValueByKey(ConstantUtil.MODERATOR_REWARD_AUTO);
            if (!"1".equals(enabled)) {
                log.info("[版主奖励] 自动发放未开启，跳过");
                return;
            }

            // 检查发放日（dict_value = 日期，1-28）
            String dayStr = dictService.getValueByKey(ConstantUtil.MODERATOR_REWARD_DAY);
            int rewardDay = 1; // 默认每月1日
            if (dayStr != null) {
                try { rewardDay = Integer.parseInt(dayStr); } catch (NumberFormatException e) { /* use default */ }
            }

            Calendar cal = Calendar.getInstance();
            int today = cal.get(Calendar.DAY_OF_MONTH);
            if (today != rewardDay) {
                log.info("[版主奖励] 今天是{}号，不是发放日（{}号），跳过", today, rewardDay);
                return;
            }

            // 检查本月是否已发放过（防止重启重复触发）
            SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
            String currentMonth = monthFmt.format(new Date());
            LambdaQueryWrapper<BoardModerator> modWrapper = new LambdaQueryWrapper<>();
            modWrapper.eq(BoardModerator::getStatus, 1);
            List<BoardModerator> mods = this.list(modWrapper);
            Set<Integer> modIds = mods.stream().map(BoardModerator::getUserId).collect(Collectors.toSet());

            boolean alreadyDistributed = false;
            for (Integer modId : modIds) {
                LambdaQueryWrapper<PointsLog> check = new LambdaQueryWrapper<>();
                check.eq(PointsLog::getUserId, modId)
                        .likeRight(PointsLog::getCreateTime, currentMonth)
                        .eq(PointsLog::getReason, "版主月度履职奖励");
                if (pointsLogService.count(check) > 0) {
                    alreadyDistributed = true;
                    break;
                }
            }
            if (alreadyDistributed) {
                log.info("[版主奖励] 本月已发放过，跳过");
                return;
            }

            log.info("[版主奖励] 触发自动发放，共{}位版主", modIds.size());
            ResultBean result = doMonthlyReward(1, true);
            log.info("[版主奖励] 自动发放结果：{}", result.getMessage());
        } catch (Exception e) {
            log.error("[版主奖励] 自动发放异常", e);
        }
    }

    @Override
    @Transactional
    public ResultBean cancelReward(Integer userId, Integer operatorId, String remark) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }

        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());

        // 检查本月是否已发放
        LambdaQueryWrapper<PointsLog> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PointsLog::getUserId, userId)
                .likeRight(PointsLog::getCreateTime, currentMonth)
                .eq(PointsLog::getReason, "版主月度履职奖励");
        if (pointsLogService.count(checkWrapper) > 0) {
            return ResultBean.error("该版主本月已发放奖励，无法取消");
        }

        // 检查是否已取消
        List<Integer> cancelledIds = cancelMapper.findCancelledUserIds(currentMonth);
        if (cancelledIds.contains(userId)) {
            return ResultBean.error("该版主已被取消本月奖励");
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ModeratorRewardCancel cancel = new ModeratorRewardCancel();
        cancel.setYearMonth(currentMonth);
        cancel.setUserId(userId);
        cancel.setOperatorId(operatorId);
        cancel.setRemark(remark);
        cancel.setCreateTime(fmt.format(new Date()));
        cancelMapper.insert(cancel);

        // 通知被取消的版主
        notificationService.createNotification(userId, operatorId, "moderator_reward_cancelled",
                "您的本月版主履职奖励已被取消" + (remark != null ? "（" + remark + "）" : ""),
                "user", userId);

        return ResultBean.success("已取消该版主本月履职奖励");
    }

    @Override
    @Transactional
    public ResultBean restoreReward(Integer userId) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }

        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());

        LambdaQueryWrapper<ModeratorRewardCancel> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ModeratorRewardCancel::getYearMonth, currentMonth)
                .eq(ModeratorRewardCancel::getUserId, userId);
        boolean removed = cancelMapper.delete(deleteWrapper) > 0;

        if (removed) {
            return ResultBean.success("已恢复该版主本月履职奖励资格");
        } else {
            return ResultBean.error("该版主未被取消奖励，无需恢复");
        }
    }

    @Override
    public ResultBean listCancelledRewards() {
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());

        List<ModeratorRewardCancel> list = cancelMapper.findByYearMonth(currentMonth);

        // 填充用户信息
        Set<Integer> userIds = new HashSet<>();
        for (ModeratorRewardCancel c : list) userIds.add(c.getUserId());
        Map<Integer, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            for (User u : users) nameMap.put(u.getId(), u.getNickname());
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (ModeratorRewardCancel c : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("userId", c.getUserId());
            map.put("userName", nameMap.getOrDefault(c.getUserId(), ""));
            map.put("remark", c.getRemark());
            map.put("createTime", c.getCreateTime());
            records.add(map);
        }

        return ResultBean.success("查询成功", records);
    }

    @Override
    public ResultBean autoMonthlyReward() {
        return doMonthlyReward(1, true);
    }
}
