package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.LoginLog;
import com.walker.vo.ResultBean;

/**
 * 每日登录浏览日志服务接口
 */
public interface LoginLogService extends IService<LoginLog> {

    /**
     * 记录用户登录
     */
    ResultBean dailyLogin(Integer userId);

    /**
     * 浏览心跳上报（累计浏览时间）
     */
    ResultBean browseHeartbeat(Integer userId);
}
