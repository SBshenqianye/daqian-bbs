package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.BoardModeratorMapper;
import com.walker.mapper.ModeratorRewardCancelMapper;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.config.security.Permission;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.ModeratorRewardCancel;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.PointsLog;
import com.walker.pojo.User;
import com.walker.service.ArticleLabelService;
import com.walker.service.BoardModeratorService;
import com.walker.service.CurrentUserService;
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

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ArticleLabelService articleLabelService;

    @Autowired
    private PermissionGroupMapper permissionGroupMapper;

    @Autowired
    private PermissionGroupItemMapper permissionGroupItemMapper;

    @Override
    @Transactional
    public ResultBean appoint(Integer userId, Integer labelId, Integer operatorId) {
        if (userId == null || labelId == null) {
            return ResultBean.error("参数不完整");
        }
        // 操作人以登录态身份为准：前端传入的 operatorId 必须与当前登录用户一致，防伪造
        Integer currentUserId = currentUserService.getCurrentUserId();
        if (currentUserId == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        if (operatorId != null && !currentUserId.equals(operatorId)) {
            return ResultBean.error("操作人身份校验失败，请重新登录后操作");
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

        // 撤销后重新任命：复用 status=0 的历史记录，避免撞 uk_board_mod_user_label
        BoardModerator history = boardModeratorMapper.findAnyByUserAndLabel(userId, labelId);
        if (history != null) {
            history.setRoleType("moderator");
            history.setStatus(1);
            history.setAppointTime(fmt.format(now));
            this.updateById(history);
            upgradeAccountToModerator(userId);
            return ResultBean.success("版主任命成功");
        }

        BoardModerator moderator = new BoardModerator();
        moderator.setUserId(userId);
        moderator.setLabelId(labelId);
        moderator.setRoleType("moderator");
        moderator.setStatus(1);
        moderator.setAppointTime(fmt.format(now));
        moderator.setCreateTime(fmt.format(now));
        this.save(moderator);

        upgradeAccountToModerator(userId);

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
        return listModerators(page, size, null);
    }

    @Override
    public ResultBean listModerators(Integer page, Integer size, String keyword) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        List<BoardModerator> all = this.list(new LambdaQueryWrapper<BoardModerator>()
                .eq(BoardModerator::getStatus, 1)
                .orderByDesc(BoardModerator::getAppointTime));

        // 按用户聚合；LinkedHashMap 的顺序即“最近任命优先”
        Map<Integer, List<BoardModerator>> byUser = new LinkedHashMap<>();
        for (BoardModerator m : all) {
            byUser.computeIfAbsent(m.getUserId(), k -> new ArrayList<>()).add(m);
        }

        // 无有效板块的版主也要出现在列表里，便于重新分配板块（D5）
        Integer moderatorGroupId = userService.resolveModeratorGroupId();
        if (moderatorGroupId != null) {
            List<User> moderatorUsers = userService.list(new LambdaQueryWrapper<User>()
                    .eq(User::getPrimaryGroupId, moderatorGroupId)
                    .eq(User::getIsDelete, 0));
            for (User u : moderatorUsers) {
                byUser.computeIfAbsent(u.getId(), k -> new ArrayList<>());
            }
        }

        List<Integer> userIds = new ArrayList<>(byUser.keySet());
        Map<Integer, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            userService.fillPrimaryGroupNames(users);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase(Locale.ROOT);
            userIds.removeIf(id -> {
                User u = userMap.get(id);
                if (u == null) return true;
                String username = u.getUsername() != null ? u.getUsername().toLowerCase(Locale.ROOT) : "";
                String nickname = u.getNickname() != null ? u.getNickname().toLowerCase(Locale.ROOT) : "";
                return !username.contains(kw) && !nickname.contains(kw);
            });
        }

        // 有板块的按最近任命时间倒序，无板块的排最后
        userIds.sort((a, b) -> {
            String ta = latestAppointTime(byUser.get(a));
            String tb = latestAppointTime(byUser.get(b));
            if (ta == null && tb == null) return Integer.compare(a, b);
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        int total = userIds.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(from + size, total);
        List<Integer> pageIds = from < total ? userIds.subList(from, to) : Collections.emptyList();

        Set<Integer> labelIds = new HashSet<>();
        for (Integer id : pageIds) {
            for (BoardModerator m : byUser.get(id)) {
                labelIds.add(m.getLabelId());
            }
        }
        Map<Integer, String> labelNameMap = new HashMap<>();
        if (!labelIds.isEmpty()) {
            for (ArticleLabel l : articleLabelService.listByIds(labelIds)) {
                labelNameMap.put(l.getLabelId(), l.getLabelName());
            }
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (Integer id : pageIds) {
            List<BoardModerator> rows = byUser.get(id);
            User u = userMap.get(id);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", id);
            item.put("username", u != null ? u.getUsername() : "");
            item.put("nickname", u != null ? u.getNickname() : "");
            item.put("primaryGroupId", u != null ? u.getPrimaryGroupId() : null);
            item.put("primaryGroupName", u != null ? u.getPrimaryGroupName() : null);
            item.put("primaryGroupCode", u != null ? u.getPrimaryGroupCode() : null);
            item.put("boardCount", rows.size());
            item.put("appointTime", latestAppointTime(rows));

            List<Map<String, Object>> boards = new ArrayList<>();
            for (BoardModerator m : rows) {
                Map<String, Object> board = new LinkedHashMap<>();
                board.put("labelId", m.getLabelId());
                board.put("labelName", labelNameMap.getOrDefault(m.getLabelId(), "标签#" + m.getLabelId()));
                board.put("appointTime", m.getAppointTime());
                boards.add(board);
            }
            item.put("boards", boards);

            // 兼容旧前端字段（单板块行）
            if (!rows.isEmpty()) {
                BoardModerator first = rows.get(0);
                item.put("id", first.getId());
                item.put("labelId", first.getLabelId());
                item.put("roleType", first.getRoleType());
            } else {
                item.put("id", null);
                item.put("labelId", null);
                item.put("roleType", "moderator");
            }
            records.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        return ResultBean.success("查询成功", data);
    }

    private String latestAppointTime(List<BoardModerator> rows) {
        if (rows == null || rows.isEmpty()) return null;
        String latest = null;
        for (BoardModerator m : rows) {
            if (m.getAppointTime() != null && (latest == null || m.getAppointTime().compareTo(latest) > 0)) {
                latest = m.getAppointTime();
            }
        }
        return latest;
    }

    @Override
    @Transactional
    public ResultBean updateBoards(Integer userId, List<Integer> labelIds) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return ResultBean.error("用户不存在");
        }

        Set<Integer> desired = new LinkedHashSet<>();
        if (labelIds != null) {
            for (Integer labelId : labelIds) {
                if (labelId != null) desired.add(labelId);
            }
        }

        if (!desired.isEmpty()) {
            List<ArticleLabel> labels = articleLabelService.listByIds(desired);
            Set<Integer> existingIds = labels.stream()
                    .map(ArticleLabel::getLabelId)
                    .collect(Collectors.toSet());
            if (existingIds.size() != desired.size()) {
                return ResultBean.error("存在无效的板块");
            }
        }

        List<BoardModerator> current = this.list(new LambdaQueryWrapper<BoardModerator>()
                .eq(BoardModerator::getUserId, userId)
                .eq(BoardModerator::getStatus, 1));
        Map<Integer, BoardModerator> currentMap = new LinkedHashMap<>();
        for (BoardModerator m : current) {
            currentMap.put(m.getLabelId(), m);
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<Integer> added = new ArrayList<>();
        List<Integer> removed = new ArrayList<>();
        List<Integer> kept = new ArrayList<>();

        for (Integer labelId : desired) {
            if (currentMap.containsKey(labelId)) {
                kept.add(labelId);
                continue;
            }
            BoardModerator history = boardModeratorMapper.findAnyByUserAndLabel(userId, labelId);
            if (history != null) {
                history.setRoleType("moderator");
                history.setStatus(1);
                history.setAppointTime(now);
                this.updateById(history);
            } else {
                BoardModerator moderator = new BoardModerator();
                moderator.setUserId(userId);
                moderator.setLabelId(labelId);
                moderator.setRoleType("moderator");
                moderator.setStatus(1);
                moderator.setAppointTime(now);
                moderator.setCreateTime(now);
                this.save(moderator);
            }
            added.add(labelId);
        }

        for (Map.Entry<Integer, BoardModerator> entry : currentMap.entrySet()) {
            if (!desired.contains(entry.getKey())) {
                BoardModerator moderator = entry.getValue();
                moderator.setStatus(0);
                this.updateById(moderator);
                removed.add(entry.getKey());
            }
        }

        if (!added.isEmpty()) {
            upgradeAccountToModerator(user);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("added", added);
        data.put("removed", removed);
        data.put("kept", kept);
        data.put("boardCount", desired.size());
        return ResultBean.success("板块更新成功", data);
    }

    /**
     * 任命/新增板块时，将普通用户的主权限组切换为默认版主权限组；
     * 已属于管理类或其他定制权限组的账号保持不变。
     */
    private void upgradeAccountToModerator(Integer userId) {
        upgradeAccountToModerator(userService.getById(userId));
    }

    private void upgradeAccountToModerator(User user) {
        if (user == null) {
            return;
        }
        Integer moderatorGroupId = userService.resolveModeratorGroupId();
        if (moderatorGroupId == null) {
            return;
        }
        Integer currentGroupId = user.getPrimaryGroupId();
        if (currentGroupId != null && moderatorGroupId.equals(currentGroupId)) {
            return;
        }
        // 只把“无主组/普通用户组”的账号升级为版主；管理类或其他自定义组保持不变。
        // 判断依据是主组自身是否带 admin:access，而不是写死 role_user 编码。
        if (currentGroupId != null) {
            PermissionGroup current = permissionGroupMapper.selectById(currentGroupId);
            if (current != null && !isOrdinaryGroup(current)) {
                return;
            }
        }
        User update = new User();
        update.setId(user.getId());
        update.setPrimaryGroupId(moderatorGroupId);
        userService.updateById(update);
        user.setPrimaryGroupId(moderatorGroupId);
    }

    /**
     * 普通用户组：主组自身没有 admin:access。
     * 不依赖 role_user 编码，内置组被重命名或删除后仍能正确识别。
     */
    private boolean isOrdinaryGroup(PermissionGroup group) {
        if (group == null || group.getId() == null) {
            return true;
        }
        Long count = permissionGroupItemMapper.selectCount(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .eq(PermissionGroupItem::getGroupId, group.getId())
                        .eq(PermissionGroupItem::getPermissionCode, Permission.ADMIN_ACCESS.getCode())
                        .eq(PermissionGroupItem::getEffect, "grant"));
        return count == null || count == 0;
    }

    @Override
    public boolean isModerator(Integer userId, Integer labelId) {
        return hasActiveRole(userId, labelId);
    }

    @Override
    public boolean hasActiveRole(Integer userId, Integer labelId) {
        if (userId == null || labelId == null) {
            return false;
        }
        return boardModeratorMapper.findByUserAndLabel(userId, labelId) != null;
    }

    @Override
    @Transactional
    public ResultBean monthlyReward(Integer operatorId) {
        // 手动发放：操作人以登录态身份为准（自动发放走 autoMonthlyReward，固定系统超管 id=1）
        Integer currentUserId = currentUserService.getCurrentUserId();
        if (currentUserId == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        if (operatorId != null && !currentUserId.equals(operatorId)) {
            return ResultBean.error("操作人身份校验失败，请重新登录后操作");
        }
        return doMonthlyReward(currentUserId, false);
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
        // 操作人以登录态身份为准，不信任前端传入的 operatorId
        Integer currentUserId = currentUserService.getCurrentUserId();
        if (currentUserId == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        if (operatorId != null && !currentUserId.equals(operatorId)) {
            return ResultBean.error("操作人身份校验失败，请重新登录后操作");
        }
        // 后续落库与通知均以登录态 currentUserId 为准
        operatorId = currentUserId;

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
