package com.walker.controller;


import com.alibaba.fastjson.JSONObject;
import com.walker.vo.excel.ImportPreviewVO;
import com.walker.vo.excel.ImportResultVO;
import com.github.pagehelper.PageInfo;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.service.SaOrgService;
import com.walker.utils.FilePathNormalizer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.vo.ResultBean;
import com.walker.service.UserService;
import com.walker.vo.param.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/04/17 12:51
 */

@Api(tags = "UserController")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SaOrgService saOrgService;

    @Value("${storage.path}")
    private String basePath;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/common/user/info")
    public Map<String, Object> getUserInfo(Principal principal){

        String username = principal.getName();
        User user = userService.getUserByUsername(username);
        user.setPassword(null);

        // 查询带组织信息的完整用户
        User resolvedUser = userService.queryUserinfoById(user.getId());
        if (resolvedUser != null) {
            user.setOrgName(resolvedUser.getOrgName());
            user.setDeptName(resolvedUser.getDeptName());
        }

        // 手动构建返回Map，确保所有新字段被传递
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("password", null);
        map.put("nickname", user.getNickname());
        map.put("portrait", FilePathNormalizer.normalizeFieldUrl(user.getPortrait(), contextPath));
        map.put("gender", user.getGender());
        map.put("introduce", user.getIntroduce());
        map.put("city", user.getCity());
        map.put("fans", user.getFans());
        map.put("attention", user.getAttention());
        map.put("good", user.getGood());
        map.put("isAlive", user.getIsAlive());
        map.put("isDelete", user.getIsDelete());
        map.put("createTime", user.getCreateTime());
        map.put("phone", user.getPhone());
        map.put("idCard", user.getIdCard());
        map.put("orgNo", user.getOrgNo());
        map.put("orgName", user.getOrgName());
        map.put("orgNameFull", resolveFullOrgName(user.getOrgNo(), user.getOrgName()));
        map.put("deptName", user.getDeptName());
        map.put("userType", user.getUserType());
        map.put("personnelId", user.getPersonnelId());
        map.put("isFirstLogin", user.getIsFirstLogin());
        return map;
    }


    @ApiOperation(value = "修改当前用户头像")
    @PutMapping("/user/updatePortrait")
    public ResultBean updatePortrait(@RequestParam("userId") Integer id,@RequestParam("file") MultipartFile file) throws Exception{

        String pType = file.getContentType();
        pType = pType.substring(pType.indexOf("/") + 1);
        if ("jpeg".equals(pType)){
            pType = "jpg";
        }

        Long time = System.currentTimeMillis();

        // 文件保存的路径
        String path = basePath +"User/"+"id_"+id+"/portrait/"+time+"_."+pType;

        File outFile = new File(path);
        File parentDir = outFile.getParentFile();
        if (parentDir != null && !parentDir.isDirectory()) {
            if (!parentDir.mkdirs() && !parentDir.exists()) {
                throw new IOException("无法创建上传目录: " + parentDir.getAbsolutePath());
            }
        }
        try {
            // 将前端传递的文件保存到本地服务器路径下
            file.transferTo(new File(path));

            // 同步到数据库的路径
            String pathDB = contextPath + "/files/User/" +"id_"+id+"/portrait/"+time+"_."+pType;
            return userService.updatePortrait(id,pathDB);

        }catch (Exception e){
            e.printStackTrace();
        }

        return ResultBean.error("上传头像失败");
    }

    @ApiOperation(value = "修改当前用户信息")
    @PutMapping("/user/updateUserinfo")
    public ResultBean updateUserinfo(@RequestBody UserInfoUpdateParam userInfoUpdateParam, HttpServletRequest request){

        return userService.updateUserinfo(userInfoUpdateParam);
    }

    @ApiOperation(value = "通过用户ID获取用户信息")
    @PostMapping("/common/user/getUserinfoById/{id}")
    public Map<String, Object> getUserinfoById(@PathVariable("id") Integer id){
        User user = userService.queryUserinfoById(id);
        user.setPassword(null);
        user.setPortrait(FilePathNormalizer.normalizeFieldUrl(user.getPortrait(), contextPath));
        // 由于 User POJO 不存储 orgNameFull，将其放入扩展属性
        // 前端通过 JSON 序列化可获取，此处用 User 的瞬态字段已移除，
        // 所以用 JSONString 方式附加（通过 customizer）或直接返回 User（前端取不到 orgNameFull）
        // 改用 Map 返回以确保包含 orgNameFull
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("portrait", user.getPortrait());
        result.put("gender", user.getGender());
        result.put("introduce", user.getIntroduce());
        result.put("city", user.getCity());
        result.put("orgNo", user.getOrgNo());
        result.put("orgName", user.getOrgName());
        result.put("deptName", user.getDeptName());
        result.put("userType", user.getUserType());
        result.put("personnelId", user.getPersonnelId());
        result.put("orgNameFull", resolveFullOrgName(user.getOrgNo(), user.getOrgName()));
        return result;
    }

    /**
     * 从 bbs_sa_org 查询组织的完整名称（用户 orgName 可能已被 display 过滤覆盖）
     */
    private String resolveFullOrgName(String orgNo, String fallbackName) {
        if (orgNo == null) return fallbackName;
        SaOrg org = saOrgService.getOne(
                new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getOrgNo, orgNo)
                        .eq(SaOrg::getIsDelete, 0)
        );
        return org != null ? org.getOrgName() : fallbackName;
    }

    @ApiOperation(value = "获取所有用户(搜索 + 分页)")
    @PostMapping("/getAllUser")
    public ResultBean getAllUser(@RequestBody JSONObject params){
        if (params != null && !params.isEmpty()){
            int pageIndex = params.getIntValue("pageIndex");
            int pageSize = params.getIntValue("pageSize");
            String searchInfo = params.getString("searchInfo");

            PageInfo<User> pageInfo = userService.getAllUserByPageAndSearch(pageIndex,pageSize,searchInfo);
            return ResultBean.success("成功获取！",pageInfo);
        }
        return ResultBean.error("参数不能为空");
    }

    @ApiOperation(value = "通过用户id删除用户")
    @PostMapping("/admin/deleteUserByUserId")
    public ResultBean deleteUserByUserId(@RequestBody UserOperationParam userOperationParam){
        if (userOperationParam.getUserId() != null){
            return userService.deleteUserByUserId(userOperationParam.getUserId());
        }
        return ResultBean.error("用户id不能为空！");
    }

    @ApiOperation(value = "通过用户id修改用户alive值")
    @PostMapping("/admin/updateUserAliveByUserId")
    public ResultBean updateUserAliveByUserId(@RequestBody UserOperationParam userOperationParam){
        if (userOperationParam.getUserId() != null){
            return userService.updateUserAliveByUserId(userOperationParam.getUserId());
        }
        return ResultBean.success("修改失败！");
    }

    @ApiOperation(value = "通过用户id批量删除用户")
    @PostMapping("/admin/batchDeleteUsersByUserIds")
    public ResultBean batchDeleteUsersByUserIds(@RequestBody UserOperationParam userOperationParam){
        if (StringUtils.isNoneBlank(userOperationParam.getUserIds())){

            List<String> list = Arrays.asList(userOperationParam.getUserIds().split(","));

            List<Integer> userIds1 = new ArrayList<>();
            list.forEach(userid->{
                userIds1.add(Integer.valueOf(userid));
            });

            return userService.batchDeleteUsersByUserIds(userIds1);
        }
        return ResultBean.error("参数不能为空！");
    }


    @ApiOperation(value = "获取用户总数")
    @GetMapping("/admin/getUserCount/all")
    public ResultBean getUserCount(){
        return userService.getUserCount();
    }

    @ApiOperation(value = "获取地图数据(城市人数和比例)")
    @GetMapping("/admin/getAllCity/all")
    public ResultBean getAllCity(){
        return userService.getAllCity();
    }

    @ApiOperation(value = "获取系统最近一年的用户注册数据")
    @GetMapping("/admin/getUserDataWithMonth/all")
    public  ResultBean getUserDataWithMonth(){
        return userService.getUserDataWithMonth();
    }

    @ApiOperation(value = "修改用户密码")
    @PostMapping("/user/modPwd")
    public ResultBean modPwd(@RequestBody UserModPwdParam userModPwdParam){
        if (userModPwdParam.getId() == null
                || StringUtils.isEmpty(userModPwdParam.getPassword())
                || StringUtils.isEmpty(userModPwdParam.getNewPassword())){
            return ResultBean.error("参数不全！");
        }
        return userService.modPwd(userModPwdParam);
    }

    /**
     * 方法描述 更改用户角色
     * @author chengQing
     * @date 2026/3/12 15:51
     * @param userOperationParam 修改参数
     * @return ResultBean 返回结果
     */
    @ApiOperation(value = "更改用户角色")
    @PostMapping("/updateUserRole")
    public ResultBean updateUserRole(@RequestBody UserOperationParam userOperationParam){
        if (userOperationParam.getUserId() != null && !StringUtils.isEmpty(userOperationParam.getRoleType())){
            return userService.updateUserRole(userOperationParam.getUserId(), userOperationParam.getRoleType());
        }
        return ResultBean.error("参数不全！");
    }

    /**
     * 方法描述 修改用户单位
     * @author chengQing
     * @date 2026/4/9 16:27
     * @param
     * @return
     * @throws
     */
    @ApiOperation(value = "修改用户单位")
    @PostMapping("/user/modOrgNo")
    public ResultBean modOrgNo(@RequestBody UserModOrgNoParam userModOrgNoParam){
        return userService.modOrgNo(userModOrgNoParam);
    }

    @ApiOperation(value = "导入预览：解析Excel但不存库")
    @PostMapping("/admin/previewImport")
    public ImportPreviewVO previewImport(@RequestParam("file") MultipartFile file) {
        return userService.previewImport(file);
    }

    @ApiOperation(value = "执行导入（异步）：解析Excel并写入数据库，返回 taskId")
    @PostMapping("/admin/importUsers")
    public ResultBean importUsers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "adjustments", required = false) String adjustmentsJson) {
        Map<Integer, String> adjustments = null;
        if (StringUtils.isNoneBlank(adjustmentsJson)) {
            JSONObject adjObj = JSONObject.parseObject(adjustmentsJson);
            adjustments = new HashMap<>();
            for (String key : adjObj.keySet()) {
                adjustments.put(Integer.parseInt(key), adjObj.getString(key));
            }
        }

        // 检测是否有正在进行的导入任务（全局防重复）
        Map<String, Object> current = userService.getCurrentImportTask();
        if (current != null && "processing".equals(current.get("status"))) {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", current.get("taskId"));
            map.put("reused", true);
            return ResultBean.success("检测到正在进行的导入任务，已自动接续", map);
        }

        String taskId = userService.importUsersFromExcelAsync(file, adjustments);
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("reused", false);
        return ResultBean.success("导入任务已创建", map);
    }

    @ApiOperation(value = "查询导入任务进度")
    @GetMapping("/admin/importProgress/{taskId}")
    public ResultBean getImportProgress(@PathVariable String taskId) {
        Map<String, Object> progress = userService.getImportTaskProgress(taskId);
        if (progress == null) return ResultBean.error("任务不存在");
        return ResultBean.success(progress);
    }

    @ApiOperation(value = "获取当前导入任务（无需 taskId，供页面刷新/跨管理员恢复使用）")
    @GetMapping("/admin/import/current")
    public ResultBean getCurrentImport() {
        Map<String, Object> current = userService.getCurrentImportTask();
        if (current == null) {
            return ResultBean.error("当前无导入任务");
        }
        return ResultBean.success(current);
    }

    @ApiOperation(value = "管理员新增用户")
    @PostMapping("/admin/addUser")
    public ResultBean adminAddUser(@RequestBody AdminUserAddParam param) {
        return userService.adminAddUser(param);
    }

    @ApiOperation(value = "管理员更新用户详细信息")
    @PostMapping("/admin/updateUserDetail")
    public ResultBean adminUpdateUserDetail(@RequestBody AdminUserUpdateParam param) {
        return userService.adminUpdateUserDetail(param);
    }
}
