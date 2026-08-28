package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.Appeal;
import com.walker.vo.ResultBean;

/**
 * 申诉服务接口
 */
public interface AppealService extends IService<Appeal> {

    /**
     * 用户提交申诉
     */
    ResultBean submitAppeal(Integer userId, String appealType, Integer relatedId, String content);

    /**
     * 管理员审核申诉
     */
    ResultBean reviewAppeal(Integer appealId, Integer reviewerId, String status, String remark);

    /**
     * 分页查询申诉列表（管理员端）
     */
    ResultBean listAppeals(String status, Integer page, Integer size);

    /**
     * 用户查看自己的申诉记录
     */
    ResultBean listMyAppeals(Integer userId, Integer page, Integer size);
}
