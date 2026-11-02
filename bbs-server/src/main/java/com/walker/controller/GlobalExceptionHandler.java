package com.walker.controller;

import com.walker.config.audit.AuditDenialRecorder;
import com.walker.config.security.ForbiddenException;
import com.walker.vo.ResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理Controller层的异常，返回友好的错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private AuditDenialRecorder auditDenialRecorder;
    
    /**
     * 处理参数校验异常（@Valid + @RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultBean<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("参数校验失败: {}", message);
        return ResultBean.error(message);
    }
    
    /**
     * 处理参数绑定异常（@Valid + 表单参数）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultBean<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("参数绑定失败: {}", message);
        return ResultBean.error(message);
    }
    
    /**
     * 处理约束校验异常（@Validated + 方法参数）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultBean<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        logger.warn("约束校验失败: {}", message);
        return ResultBean.error(message);
    }
    
    /**
     * 权限不足：统一返回既有 403 结构（ResultBean code=403），并记录拒绝日志。
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultBean<Void> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        String username = "anonymous";
        Integer userId = null;
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.walker.pojo.User) {
            com.walker.pojo.User user = (com.walker.pojo.User) auth.getPrincipal();
            username = user.getUsername();
            userId = user.getId();
        }
        logger.warn("[权限拒绝] userId={}, username={}, {} {}, reason={}",
                userId, username, request.getMethod(), request.getRequestURI(), e.getMessage());
        auditDenialRecorder.record(request, e.getMessage());
        return ResultBean.forbidden(e.getMessage());
    }

    /**
     * @PreAuthorize 等方法级安全拒绝：统一返回 403 结构。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultBean<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        logger.warn("[权限拒绝] {} {} reason={}", request.getMethod(), request.getRequestURI(), e.getMessage());
        auditDenialRecorder.record(request, "权限不足，请联系管理员");
        return ResultBean.forbidden("权限不足，请联系管理员");
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultBean<Void> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return ResultBean.error(e.getMessage());
    }
    
    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultBean<Void> handleException(Exception e) {
        logger.error("系统异常: ", e);
        return ResultBean.error("系统内部错误，请联系管理员");
    }
    
    /**
     * 业务异常类
     */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
        
        public BusinessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}