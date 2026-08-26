package com.walker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.DictMapper;
import com.walker.mapper.LoginLogMapper;
import com.walker.pojo.LoginLog;
import com.walker.service.LoginLogService;
import com.walker.service.PointsLogService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private DictMapper dictMapper;

    @Override
    @Transactional
    public ResultBean dailyLogin(Integer userId) {
        Date now = new Date();
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String loginDate = dateFmt.format(now);
        String loginTime = timeFmt.format(now);

        LoginLog existing = loginLogMapper.findByUserAndDate(userId, loginDate);
        if (existing != null) {
            // 已有记录，返回当前状态
            Map<String, Object> data = new HashMap<>();
            data.put("browseMinutes", existing.getBrowseMinutes());
            data.put("pointsAwarded", existing.getPointsAwarded());
            return ResultBean.success("今日已登录", data);
        }

        // 新建今日登录记录（并发场景下可能已存在，捕获重复键异常）
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setLoginDate(loginDate);
        log.setLoginTime(loginTime);
        log.setBrowseMinutes(0);
        log.setPointsAwarded(0);
        log.setCreateTime(loginTime);
        try {
            this.save(log);
        } catch (DuplicateKeyException e) {
            // 并发插入，已有记录，直接查返回
            existing = loginLogMapper.findByUserAndDate(userId, loginDate);
            Map<String, Object> data = new HashMap<>();
            data.put("browseMinutes", existing.getBrowseMinutes());
            data.put("pointsAwarded", existing.getPointsAwarded());
            return ResultBean.success("今日已登录", data);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("browseMinutes", 0);
        data.put("pointsAwarded", 0);
        return ResultBean.success("登录成功", data);
    }

    @Override
    @Transactional
    public ResultBean browseHeartbeat(Integer userId) {
        Date now = new Date();
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        String loginDate = dateFmt.format(now);

        LoginLog existing = loginLogMapper.findByUserAndDate(userId, loginDate);
        if (existing == null) {
            // 自动创建今日记录（并发下可能由 dailyLogin 并发创建）
            dailyLogin(userId);
            existing = loginLogMapper.findByUserAndDate(userId, loginDate);
        }
        if (existing == null) {
            return ResultBean.error("登录记录异常，请稍后重试");
        }

        // 已发过分且满10分钟，无需继续累计
        if (existing.getPointsAwarded() != null && existing.getPointsAwarded() == 1) {
            Map<String, Object> data = new HashMap<>();
            data.put("browseMinutes", existing.getBrowseMinutes());
            data.put("pointsAwarded", 1);
            return ResultBean.success("今日积分已发放", data);
        }

        // 累加1分钟
        int newMinutes = (existing.getBrowseMinutes() == null ? 0 : existing.getBrowseMinutes()) + 1;
        existing.setBrowseMinutes(newMinutes);

        // 获取阈值（从字典获取，缺省10）
        int threshold = 10;
        try {
            String val = dictMapper.selectValueByType("login_browse_minutes");
            if (val != null) threshold = Integer.parseInt(val);
        } catch (Exception e) {
            // 使用默认值
        }

        // 达到阈值，发放积分
        if (newMinutes >= threshold && (existing.getPointsAwarded() == null || existing.getPointsAwarded() == 0)) {
            existing.setPointsAwarded(1);
            this.updateById(existing);

            pointsLogService.adjustUserPoints(userId, 1, "每日有效登录浏览积分",
                    "login", null, null);

            Map<String, Object> data = new HashMap<>();
            data.put("browseMinutes", newMinutes);
            data.put("pointsAwarded", 1);
            return ResultBean.success("恭喜获得每日登录浏览积分+1", data);
        }

        this.updateById(existing);

        Map<String, Object> data = new HashMap<>();
        data.put("browseMinutes", newMinutes);
        data.put("pointsAwarded", 0);
        return ResultBean.success("浏览时间已记录", data);
    }
}
