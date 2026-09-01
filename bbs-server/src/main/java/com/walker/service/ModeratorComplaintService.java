package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.ModeratorComplaint;
import com.walker.vo.ResultBean;

/**
 * 版主投诉服务接口
 */
public interface ModeratorComplaintService extends IService<ModeratorComplaint> {

    /**
     * 用户提交投诉
     */
    ResultBean submit(Integer reporterId, Integer moderatorId, Integer labelId, String content);

    /**
     * 管理员审核投诉（接受/拒绝）
     * 接受时自动撤销版主身份
     */
    ResultBean review(Integer complaintId, String status, String remark, Integer reviewerId);

    /**
     * 查询投诉列表（管理员端，分页）
     */
    ResultBean listComplaints(Integer page, Integer size, String status);

    /**
     * 查询用户自己的投诉记录
     */
    ResultBean listMyComplaints(Integer reporterId);
}
