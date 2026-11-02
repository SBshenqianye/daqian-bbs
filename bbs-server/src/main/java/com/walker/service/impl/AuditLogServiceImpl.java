package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.AuditLogMapper;
import com.walker.pojo.AuditLog;
import com.walker.service.AuditLogService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog>
        implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog entity) {
        if (entity == null) {
            return;
        }
        try {
            normalize(entity);
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            // 审计失败只打 warn，绝不向业务抛异常
            log.warn("[审计日志] 写入失败，已忽略: module={}, action={}, err={}",
                    entity.getModule(), entity.getAction(), e.getMessage());
        }
    }

    @Override
    public Map<String, Object> list(Integer page, Integer size,
                                    String module, String action,
                                    Integer operatorId, String operatorName,
                                    String result, String startTime, String endTime,
                                    String targetType, String targetId) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(module)) {
            wrapper.eq(AuditLog::getModule, module.trim());
        }
        if (StringUtils.isNotBlank(action)) {
            wrapper.eq(AuditLog::getAction, action.trim());
        }
        if (operatorId != null) {
            wrapper.eq(AuditLog::getOperatorId, operatorId);
        }
        if (StringUtils.isNotBlank(operatorName)) {
            wrapper.like(AuditLog::getOperatorName, operatorName.trim());
        }
        if (StringUtils.isNotBlank(result)) {
            wrapper.eq(AuditLog::getResult, result.trim());
        }
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.ge(AuditLog::getCreateTime, startTime.trim());
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.le(AuditLog::getCreateTime, endTime.trim());
        }
        if (StringUtils.isNotBlank(targetType)) {
            wrapper.eq(AuditLog::getTargetType, targetType.trim());
        }
        if (StringUtils.isNotBlank(targetId)) {
            wrapper.eq(AuditLog::getTargetId, targetId.trim());
        }
        wrapper.orderByDesc(AuditLog::getCreateTime).orderByDesc(AuditLog::getId);

        Page<AuditLog> resultPage = auditLogMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        data.put("page", current);
        data.put("size", pageSize);
        return data;
    }

    private void normalize(AuditLog entity) {
        if (StringUtils.isBlank(entity.getCreateTime())) {
            entity.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        entity.setOperatorName(truncate(entity.getOperatorName(), 64));
        entity.setModule(truncate(entity.getModule(), 32));
        entity.setAction(truncate(entity.getAction(), 64));
        entity.setTargetType(truncate(entity.getTargetType(), 32));
        entity.setTargetId(truncate(entity.getTargetId(), 128));
        entity.setRequestMethod(truncate(entity.getRequestMethod(), 10));
        entity.setRequestPath(truncate(entity.getRequestPath(), 500));
        entity.setIp(truncate(entity.getIp(), 64));
        entity.setUserAgent(truncate(entity.getUserAgent(), 500));
        entity.setResult(truncate(entity.getResult(), 16));
        entity.setErrorMsg(truncate(entity.getErrorMsg(), 1000));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
