package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.Violation;
import com.walker.vo.ResultBean;

/**
 * 违规扣分服务接口
 */
public interface ViolationService extends IService<Violation> {

    /**
     * 管理员记录违规并扣分
     */
    ResultBean addViolation(Integer userId, String violationType, String relatedType,
                            Integer relatedId, Integer operatorId, String remark);

    /**
     * 分页查询违规记录（管理员端）
     */
    ResultBean listViolations(Integer userId, Integer page, Integer size);

    /**
     * 用户查看自己的违规记录
     */
    ResultBean listMyViolations(Integer userId, Integer page, Integer size);

    /**
     * #14 管理员手动取消违规：回滚扣分、恢复被隐藏/删除的内容、状态置已取消并留痕。
     * 操作人以 JWT 登录态身份为准；已取消不可重复取消；存在审核中申诉的记录拒绝取消。
     */
    ResultBean cancelViolation(Integer violationId, String reason);

    /**
     * #14/#12 申诉通过时自动取消关联违规（回滚扣分、恢复内容、状态置已取消）。
     * 由 AppealServiceImpl.reviewAppeal 在申诉通过时调用，操作人即申诉审核人。
     */
    void autoCancelByAppeal(Integer violationId, Integer reviewerId, String reviewRemark);
}
