package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.walker.pojo.User;
import com.walker.vo.ResultBean;
import com.walker.vo.param.UserInfoUpdateParam;
import com.walker.vo.param.UserModOrgNoParam;
import com.walker.vo.param.UserModPwdParam;

import com.walker.vo.excel.ImportPreviewVO;
import com.walker.vo.excel.ImportResultVO;
import com.walker.vo.param.AdminUserAddParam;
import com.walker.vo.param.AdminUserUpdateParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author walker
 * @since 2022/04/16 20:18
 */
public interface UserService extends IService<User> {

    /**
     * 登录之后返回token
     * @param username
     * @param password
     * @param channel 登录渠道
     * @param request
     * @return
     */
    ResultBean login(String username, String password,String channel, HttpServletRequest request);

    /**
     * 根据用户名获取用户
     * @param username
     * @return
     */
    User getUserByUsername(String username);

    /**
     * 用户注册
     * @param username
     * @param password
     * @param request
     * @param phone
     * @param orgNo
     * @param nickname
     * @return
     */
    ResultBean register(String username, String password,String phone, String orgNo, String nickname, HttpServletRequest request);

    /**
     * 用户上传头像
     * @param id
     * @param pathDB
     * @return
     */
    ResultBean updatePortrait(Integer id, String pathDB);

    /**
     * 用户修改信息（除头像）
     * @param userInfoUpdateParam
     */
    ResultBean updateUserinfo(UserInfoUpdateParam userInfoUpdateParam);

    /**
     * 通过用户id获取用户信息
     * @param id
     * @return
     */
    User queryUserinfoById(Integer id);


    /**
     * 通过id批量查询用户信息
     * @param userId
     * @return
     */
    User getUserInfoByUserId(Integer userId);


    /**
     * 获取所有的用户信息 分页 搜索
     * @param pageIndex
     * @param pageSize
     * @param searchInfo
     * @return
     */
    PageInfo<User> getAllUserByPageAndSearch(int pageIndex, int pageSize, String searchInfo);


    /**
     * 通过用户id删除用户
     * @param id
     * @return
     */
    ResultBean deleteUserByUserId(Integer id);


    /**
     * 通过用户id修改状态
     * @param id
     * @return
     */
    ResultBean updateUserAliveByUserId(Integer id);


    /**
     * 通过用户id批量删除用户
     * @param userIds
     * @return
     */
    ResultBean batchDeleteUsersByUserIds(List<Integer> userIds);


    /**
     * 获取用户总数
     * @return
     */
    ResultBean getUserCount();

    /**
     * 获取管理端首页的地图数据（城市、城市名、占比）
     * @return
     */
    ResultBean getAllCity();


    /**
     * 获取最近一年用户的注册数据
     * @return
     */
    ResultBean getUserDataWithMonth();

    /**
     * 修改用户密码
     * @param userModPwdParam 用户id、原密码、新密码
     * @return
     */
    ResultBean modPwd(UserModPwdParam userModPwdParam);

    /**
     * 修改用户角色
     * @param userId 用户id
     * @param roleType 角色类型
     * @return ResultBean
     */
    ResultBean updateUserRole(Integer userId, String roleType);

    /**
     * 方法描述
     * @author chengQing
     * @date 2026/4/9 16:29
     * @param userModOrgNoParam 参数实体类
     * @return ResultBean
     */
    ResultBean modOrgNo(UserModOrgNoParam userModOrgNoParam);

    /**
     * 导入预览：解析Excel但不存库
     * @param file 上传的Excel文件
     * @return 预览结果
     */
    ImportPreviewVO previewImport(MultipartFile file);

    /**
     * 异步导入：解析Excel并写入数据库，立即返回 taskId
     * @param file 上传的Excel文件
     * @param adjustments 管理员手动修正的账号映射（rowNum → 修正后的username），可为空
     * @return 任务ID
     */
    String importUsersFromExcelAsync(MultipartFile file, Map<Integer, String> adjustments);

    /**
     * 获取导入任务进度
     * @param taskId 任务ID
     * @return { status, progress, total, result, error }
     */
    Map<String, Object> getImportTaskProgress(String taskId);

    /**
     * 获取当前导入任务（无需 taskId），供页面刷新/跨管理员恢复使用
     * @return { taskId, status, progress, total, result, error }，无任务时返回 null
     */
    Map<String, Object> getCurrentImportTask();

    /**
     * 管理员更新用户详细信息
     * @param param 用户更新参数（昵称、手机号、单位、角色、状态等）
     * @return 操作结果
     */
    /**
     * 管理员新增用户
     * @param param 新增用户参数（用户名、昵称、手机号、单位、角色、状态）
     * @return 操作结果
     */
    ResultBean adminAddUser(AdminUserAddParam param);

    ResultBean adminUpdateUserDetail(AdminUserUpdateParam param);
}
