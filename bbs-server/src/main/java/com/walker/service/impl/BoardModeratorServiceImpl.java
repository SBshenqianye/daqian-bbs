package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.BoardModeratorMapper;
import com.walker.pojo.BoardModerator;
import com.walker.service.BoardModeratorService;
import com.walker.service.PointsLogService;
import com.walker.service.UserService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class BoardModeratorServiceImpl extends ServiceImpl<BoardModeratorMapper, BoardModerator> implements BoardModeratorService {

    @Autowired
    private BoardModeratorMapper boardModeratorMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private UserService userService;

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
}
