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
}
