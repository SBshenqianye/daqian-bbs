package com.walker.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageInfo;
import com.walker.config.security.JwtTokenUtil;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.utils.ConstantUtil;
import com.walker.vo.MapCityVO;
import com.walker.service.impl.OrgImportService;
import com.walker.utils.PinyinUtil;
import com.walker.vo.ResultBean;
import com.walker.service.UserService;
import com.walker.vo.UserMonthVO;
import com.walker.vo.excel.ImportPreviewVO;
import com.walker.vo.excel.ImportResultVO;
import com.walker.vo.excel.UserExcelRow;
import com.walker.vo.param.AdminUserAddParam;
import com.walker.vo.param.AdminUserUpdateParam;
import com.walker.vo.param.UserInfoUpdateParam;
import com.walker.vo.param.UserModOrgNoParam;
import com.walker.vo.param.UserModPwdParam;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.springframework.web.multipart.MultipartFile;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author walker
 * @since 2022/04/16 20:18
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Value("${jwt.tokenHead}")
    private String tokenHead;

    @Autowired
    private SaOrgMapper saOrgMapper;

    @Autowired
    private OrgImportService orgImportService;

    // ========== 异步导入任务跟踪 ==========
    private final Map<String, ImportTask> importTasks = new ConcurrentHashMap<>();
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();
    /** 当前（最近创建的）任务ID，供跨管理员恢复使用 */
    private volatile String currentTaskId = null;
    /** 已完成任务保留时长 */
    private static final long CLEANUP_THRESHOLD_MS = 5 * 60 * 1000L;

    public static class ImportTask {
        private final String taskId;
        private volatile String status;
        private volatile int progress;
        private volatile int total;
        private ImportResultVO result;
        private String error;
        private volatile long completedAt;

        public ImportTask(String taskId, int total) {
            this.taskId = taskId;
            this.total = total;
            this.status = "processing";
            this.progress = 0;
        }
        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public void setStatus(String s) {
            status = s;
            if ("done".equals(s) || "error".equals(s)) {
                completedAt = System.currentTimeMillis();
            }
        }
        public int getProgress() { return progress; }
        public void setProgress(int p) { progress = p; }
        public int getTotal() { return total; }
        public void setTotal(int t) { total = t; }
        public ImportResultVO getResult() { return result; }
        public void setResult(ImportResultVO r) { result = r; }
        public String getError() { return error; }
        public void setError(String e) { error = e; }
        public long getCompletedAt() { return completedAt; }
    }

    /**
     * 登录之后返回token
     * @param username
     * @param password
     * @param channel 登录渠道
     * @param request
     * @return
     * JWT
     * Json Web Token
     */
    @Override
    public ResultBean login(String username, String password, String channel, HttpServletRequest request) {

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (userDetails != null && passwordEncoder.matches(password,userDetails.getPassword())){
            User user = (User) userDetails;
            if (ConstantUtil.MANA_ZERO_ONE.equals(channel) || ConstantUtil.MANA_ZERO_TWO.equals(channel)) {
                if (ConstantUtil.MANA_ZERO_TWO.equals(channel) && ConstantUtil.MANA_ONE.equals(user.getUserType())) {
                    return ResultBean.error("用户名或密码不正确！");
                }
            } else {
                return ResultBean.error("非法渠道登录！");
            }
            //更新security 登录用户对象
            UsernamePasswordAuthenticationToken authenticationToken = new
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // 生成token
            String token = jwtTokenUtil.generateToken(userDetails);
            HashMap<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("token",token);
            tokenMap.put("tokenHead",tokenHead);
            JSONObject jsonObject = (JSONObject) JSON.toJSON(user);
            jsonObject.put("password", null);
            jsonObject.put("isFirstLogin", user.getIsFirstLogin());
            tokenMap.put("user", jsonObject);

            return ResultBean.success("登录成功！",tokenMap);
        }
        return ResultBean.error("用户名或密码不正确！");
    }

    /**
     * 更据用户名获取用户信息
     * @param username
     * @return
     */
    @Override
    public User getUserByUsername(String username) {
        User user = null;
        if (StringUtils.isNoneBlank(username)){
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username)
            );
        }
        return user;
        // return userMapper.selectOne(new QueryWrapper<User>().eq("username",username));
    }

    /**
     * 用户注册
     * @param username
     * @param password
     * @param phone
     * @param orgNo
     * @param nickname
     * @param request
     * @return
     */
    @Override
    public ResultBean register(String username, String password, String phone, String orgNo, String nickname, HttpServletRequest request) {
        return ResultBean.error("注册功能已关闭，请联系管理员创建账号");
    }

    /**
     * 用户上传头像
     * @param id
     * @param pathDB
     * @return
     */
    @Override
    public ResultBean updatePortrait(Integer id, String pathDB) {

        this.update(Wrappers.lambdaUpdate(User.class).set(User::getPortrait,pathDB).eq(User::getId,id));

        // this.update(new UpdateWrapper<User>().set("portrait",pathDB).eq("id",id));

        return ResultBean.success("头像上传成功！");

    }

    /**
     * 用户修改信息（除头像）
     * @param userInfoUpdateParam
     */
    @Override
    public ResultBean updateUserinfo(UserInfoUpdateParam userInfoUpdateParam) {
        User user = new User();
        user.setId(userInfoUpdateParam.getId());
        user.setPhone(userInfoUpdateParam.getPhone());
        this.updateById(user);

        return ResultBean.success("保存成功！");
    }

    /**
     * 通过用户Id获取用户信息
     * @param id
     * @return
     */
    @Override
    public User queryUserinfoById(Integer id) {
        User user = this.getById(id);
        if (user != null) {
            resolveOrgInfo(Collections.singletonList(user));
        }
        return user;
    }

    @Override
    public User getUserInfoByUserId(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public PageInfo<User> getAllUserByPageAndSearch(int pageIndex, int pageSize, String searchInfo) {
        // 使用 MyBatis-Plus 原生分页
        Page<User> mpPage = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getIsDelete, 0);
        if (StringUtils.isNoneBlank(searchInfo)) {
            wrapper.and(w -> w.like(User::getUsername, searchInfo)
                    .or()
                    .like(User::getNickname, searchInfo)
                    .or()
                    .like(User::getPersonnelId, searchInfo));
        }
        Page<User> result = userMapper.selectPage(mpPage, wrapper);
        // 解析单位名称和部门名称
        resolveOrgInfo(result.getRecords());
        // 转为 PageInfo 兼容前端
        PageInfo<User> pageInfo = new PageInfo<>(result.getRecords());
        pageInfo.setTotal(result.getTotal());
        pageInfo.setPageNum((int) result.getCurrent());
        pageInfo.setPageSize((int) result.getSize());
        pageInfo.setPages((int) result.getPages());
        return pageInfo;
    }

    /** 根节点 org_no（内江市） */
    private static final String ROOT_ORG_NO = "51404";

    /**
     * 批量解析用户的单位名称和部门名称
     * 从 bbs_sa_org 表查询 orgNo 对应的 orgName，区分单位和部门层级
     */
    private void resolveOrgInfo(List<User> users) {
        if (CollectionUtils.isEmpty(users)) return;

        Set<String> orgNos = users.stream()
                .map(User::getOrgNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (orgNos.isEmpty()) return;

        // 批量查询用户所属组织
        List<SaOrg> orgList = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>()
                        .in(SaOrg::getOrgNo, orgNos)
                        .eq(SaOrg::getIsDelete, 0)
        );
        if (CollectionUtils.isEmpty(orgList)) return;

        // 查出组织对应的父级单位（部门需要找到上级单位名称）
        Set<String> parentOrgNos = orgList.stream()
                .map(SaOrg::getPOrgNo)
                .filter(Objects::nonNull)
                .filter(p -> !ROOT_ORG_NO.equals(p))
                .filter(p -> !orgNos.contains(p))
                .collect(Collectors.toSet());
        if (!parentOrgNos.isEmpty()) {
            List<SaOrg> parentOrgs = saOrgMapper.selectList(
                    new LambdaQueryWrapper<SaOrg>()
                            .in(SaOrg::getOrgNo, parentOrgNos)
                            .eq(SaOrg::getIsDelete, 0)
            );
            orgList.addAll(parentOrgs);
        }

        Map<String, SaOrg> orgMap = orgList.stream()
                .collect(Collectors.toMap(SaOrg::getOrgNo, org -> org, (a, b) -> a));

        for (User user : users) {
            String orgNo = user.getOrgNo();
            if (orgNo == null || ROOT_ORG_NO.equals(orgNo)) continue;

            SaOrg org = orgMap.get(orgNo);
            if (org == null) continue;

            if (ROOT_ORG_NO.equals(org.getPOrgNo())) {
                // 直接挂载在根节点下 → 是单位
                user.setOrgName(org.getOrgName());
            } else {
                // 挂载在非根节点下 → 是部门
                user.setDeptName(org.getOrgName());
                SaOrg parentOrg = orgMap.get(org.getPOrgNo());
                if (parentOrg != null) {
                    user.setOrgName(parentOrg.getOrgName());
                } else {
                    // 查不到上级时降级显示部门名
                    user.setOrgName(org.getOrgName());
                }
            }
        }
    }

    @Override
    public ResultBean deleteUserByUserId(Integer id) {
        userMapper.deleteById(id);
        return ResultBean.success("删除成功！");
    }

    @Override
    public ResultBean updateUserAliveByUserId(Integer id) {
        User user = userMapper.selectById(id);

        User updateUser = new User();
        updateUser.setId(id);
        if (user.getIsAlive() == 0){
            updateUser.setIsAlive(1);
        }else {
            updateUser.setIsAlive(0);
        }
        userMapper.updateById(updateUser);
        return ResultBean.success("成功修改用户状态！");
    }

    @Override
    public ResultBean batchDeleteUsersByUserIds(List<Integer> userIds) {
        userMapper.deleteBatchIds(userIds);
        return ResultBean.success("批量删除用户成功！");
    }

    @Override
    public ResultBean getUserCount() {
        Long count = userMapper.selectCount(null);
        return ResultBean.success("获取成功！",count);
    }

    @Override
    public ResultBean getAllCity() {
        // 用户数量、占比

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .isNotNull(User::getCity)
        );
        // 用户总数
        double userCount = users.size();

        List<MapCityVO> list = new ArrayList<>();

        users.stream()
                .map(user -> {
                    String substring = user.getCity().substring(0, user.getCity().indexOf("-"));// 只获取“-”前面的省份
                    if (substring.contains("省")){
                        substring = substring.replace("省", ""); // 去除省字
                    }
                    return substring;
                })
                .collect(Collectors.groupingBy(String::valueOf))
                .forEach((cityName, cityNameList) -> {
                    // 计算占比
                    double cityUserCount = cityNameList.size();
                    NumberFormat numberFormat = NumberFormat.getPercentInstance();
                    numberFormat.setMinimumFractionDigits(2);
                    String perf = numberFormat.format(cityUserCount / userCount);

                    MapCityVO cityVO = new MapCityVO();
                    cityVO.setName(cityName)
                            .setValue(cityNameList.size())
                            .setPerf(perf);
                    list.add(cityVO);
                });
        // System.out.println("list = " + list.toString());

        return ResultBean.success("获取成功!",list);
    }

    @Override
    public ResultBean getUserDataWithMonth() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 获取当前的时间
        String currentDate = format.format(calendar.getTime());

        // 一年前的时间
        calendar.add(Calendar.YEAR, -1);
        String lastDate = format.format(calendar.getTime());
        // System.out.println("lastDate = " + lastDate);

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .between(User::getCreateTime, lastDate, currentDate)
                        .orderByAsc(User::getCreateTime)
        );
        // System.out.println("users = " + users);

        List<UserMonthVO> list = new ArrayList<>();
        users.stream()
                .map(user -> user.getCreateTime().substring(0,7))   // 截取年月
                .collect(Collectors.groupingBy(String::valueOf))
                .forEach((date, dateList) -> {
                    UserMonthVO userMonthVO = new UserMonthVO();
                    userMonthVO.setTime(date)
                                    .setCount(dateList.size());
                    list.add(userMonthVO);
                });
        // System.out.println("list = " + list);
        return ResultBean.success("获取成功！",list);
    }

    /**
     * 修改用户密码：先根据用户id查询用户是否存在，校验原密码后加密新密码更新
     * @param userModPwdParam 用户id、原密码、新密码
     * @return
     */
    @Override
    public ResultBean modPwd(UserModPwdParam userModPwdParam) {
        Integer userId = userModPwdParam.getId();
        User user = this.getById(userId);
        if (user == null) {
            return ResultBean.error("用户不存在");
        }
        String rawPassword = userModPwdParam.getPassword();
        String dbPassword = user.getPassword();
        if (!passwordEncoder.matches(rawPassword, dbPassword)) {
            return ResultBean.error("密码不正确");
        }
        // 校验新密码强度
        String newPassword = userModPwdParam.getNewPassword();
        if (!PinyinUtil.checkPasswordStrength(newPassword)) {
            return ResultBean.error("密码强度不足，需至少8位且包含数字、小写字母和大写字母");
        }
        // 新密码不能与原密码相同（明文比较，跳过BCrypt匹配）
        if (rawPassword.equals(newPassword)) {
            return ResultBean.error("新密码不能与原密码相同");
        }
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        this.update(Wrappers.lambdaUpdate(User.class)
                .set(User::getPassword, encodedNewPassword)
                .set(User::getIsFirstLogin, 0)
                .eq(User::getId, userId));
        return ResultBean.success("修改成功");
    }

    @Override
    public ResultBean updateUserRole(Integer userId, String roleType) {
        if (ConstantUtil.MANA_ZERO_ONE.equals(roleType)) {
            roleType = ConstantUtil.MANA_ONE;
        } else if (ConstantUtil.MANA_ZERO_TWO.equals(roleType)) {
            roleType = ConstantUtil.MANA_TWO;
        } else {
            return ResultBean.error("权限越界");
        }

        int result = userMapper.updateUserRole(userId, roleType);
        if (result > 0) {
            return ResultBean.success("操作成功");
        }
        return ResultBean.error("操作失败");
    }

    @Override
    public ResultBean modOrgNo(UserModOrgNoParam userModOrgNoParam) {
        // 先查询用户信息
        List<User> userList = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getId, userModOrgNoParam.getId())
        );
        if (CollectionUtils.isEmpty(userList)) {
            return ResultBean.error("用户不存在");
        }
        // 对比单位编号是否和之前一样
        User user = userList.get(0);
        if (userModOrgNoParam.getOrgNo().equals(user.getOrgNo())) {
            return ResultBean.error("用户单位不能与原单位相同");
        }
        List <SaOrg> saOrgList = saOrgMapper.selectList(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, userModOrgNoParam.getOrgNo()));
        if (CollectionUtils.isEmpty(saOrgList)) {
            return ResultBean.error("单位编号不正确");
        }
        // 更新用户单位编号和昵称
        String [] nicknameArr = user.getNickname().split("-");
        if (nicknameArr.length > 1) {
            user.setNickname(saOrgList.get(0).getOrgName()+user.getNickname().substring(user.getNickname().indexOf("-")));
        }
        user.setOrgNo(userModOrgNoParam.getOrgNo());
        int result = userMapper.update(user, new LambdaQueryWrapper<User>()
                .eq(User::getId, user.getId()));
        if (result > 0) {
            return ResultBean.success("操作成功");
        }
        return ResultBean.error("操作失败");
    }

    // ==================== Excel 导入 ====================

    @Override
    public ImportPreviewVO previewImport(MultipartFile file) {
        ImportPreviewVO preview = new ImportPreviewVO();
        try (InputStream is = file.getInputStream()) {
            List<UserExcelRow> rows = EasyExcel.read(is)
                    .head(UserExcelRow.class)
                    .sheet()
                    .doReadSync();

            preview.setTotalCount(rows.size());

            // 组织预览
            List<ImportPreviewVO.OrgPreview> orgPreviews = orgImportService.previewOrgs(rows);
            preview.setOrgs(orgPreviews);
            preview.setOrgCount(orgPreviews.size());

            // 用户预览
            List<ImportPreviewVO.UserPreview> userPreviews = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                UserExcelRow row = rows.get(i);
                ImportPreviewVO.UserPreview up = new ImportPreviewVO.UserPreview();
                up.setRowNum(i + 1);
                up.setNickname(row.getNickname());
                up.setPersonnelId(row.getPersonnelId());
                up.setIdCard(row.getIdCard());
                up.setOrgName(row.getOrgName());
                up.setDeptName(row.getDeptName());
                up.setEditable(true);

                // 生成账号
                String idCardLast4 = row.getIdCard() != null && row.getIdCard().length() >= 4
                        ? row.getIdCard().substring(row.getIdCard().length() - 4)
                        : "0000";
                up.setUsername(PinyinUtil.generateUsername(row.getNickname(), idCardLast4));

                // 判断是新增还是更新
                User existing = userMapper.selectOne(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getPersonnelId, row.getPersonnelId())
                                .last("LIMIT 1")
                );
                if (existing == null) {
                    existing = userMapper.selectOne(
                            new LambdaQueryWrapper<User>()
                                    .eq(User::getIdCard, row.getIdCard())
                                    .last("LIMIT 1")
                    );
                }
                up.setAction(existing != null ? "update" : "new");
                userPreviews.add(up);
            }
            preview.setUsers(userPreviews);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return preview;
    }

    @Override
    public String importUsersFromExcelAsync(MultipartFile file, Map<Integer, String> adjustments) {
        // 1. 检测是否有正在 processing 的任务，有则直接返回已有 taskId
        for (Map.Entry<String, ImportTask> entry : importTasks.entrySet()) {
            if ("processing".equals(entry.getValue().getStatus())) {
                return entry.getKey();
            }
        }

        // 2. 清理已完成超过阈值的旧任务
        cleanupOldTasks();

        // 3. 创建新任务
        String taskId = UUID.randomUUID().toString();
        ImportTask task = new ImportTask(taskId, 0);
        importTasks.put(taskId, task);
        currentTaskId = taskId;

        importExecutor.submit(() -> {
            try {
                runImport(task, file, adjustments);
            } catch (Exception e) {
                task.setStatus("error");
                task.setError(e.getMessage());
                e.printStackTrace();
            }
        });

        return taskId;
    }

    @Override
    public Map<String, Object> getImportTaskProgress(String taskId) {
        ImportTask task = importTasks.get(taskId);
        if (task == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", task.getTaskId());
        map.put("status", task.getStatus());
        map.put("progress", task.getProgress());
        map.put("total", task.getTotal());
        map.put("result", task.getResult());
        map.put("error", task.getError());
        return map;
    }

    @Override
    public Map<String, Object> getCurrentImportTask() {
        if (currentTaskId == null) return null;
        return getImportTaskProgress(currentTaskId);
    }

    /** 移除已完成超过阈值的旧任务（保留 currentTaskId 引用的任务） */
    private void cleanupOldTasks() {
        long now = System.currentTimeMillis();
        importTasks.entrySet().removeIf(entry -> {
            ImportTask task = entry.getValue();
            if ("processing".equals(task.getStatus())) return false;
            if (entry.getKey().equals(currentTaskId)) return false;
            return task.getCompletedAt() > 0
                && (now - task.getCompletedAt()) > CLEANUP_THRESHOLD_MS;
        });
    }

    /** 在后台线程中执行实际导入 */
    private void runImport(ImportTask task, MultipartFile file, Map<Integer, String> adjustments) {
        ImportResultVO result = new ImportResultVO();
        List<ImportResultVO.RowResult> details = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (InputStream is = file.getInputStream()) {
            List<UserExcelRow> rows = EasyExcel.read(is)
                    .head(UserExcelRow.class)
                    .sheet()
                    .doReadSync();

            int total = rows.size();
            task.total = total;
            result.setTotalCount(total);

            // 1. 先导入组织
            OrgImportService.OrgImportResult orgResult = orgImportService.importOrgs(rows);
            result.setOrgCreatedCount(orgResult.createdCount);

            int newCount = 0, updateCount = 0, skipCount = 0, failCount = 0;

            // 2. 逐行导入人员
            for (int i = 0; i < rows.size(); i++) {
                UserExcelRow row = rows.get(i);
                ImportResultVO.RowResult detail = new ImportResultVO.RowResult();
                detail.setRowNum(i + 1);
                detail.setNickname(row.getNickname());
                detail.setPersonnelId(row.getPersonnelId());
                detail.setOrgName(row.getOrgName());

                // 检查必要键值：personnelId 和 idCard 至少有一个
                boolean hasPersonnelId = row.getPersonnelId() != null && !row.getPersonnelId().trim().isEmpty();
                boolean hasIdCard = row.getIdCard() != null && !row.getIdCard().trim().isEmpty();
                if (!hasPersonnelId && !hasIdCard) {
                    detail.setUsername("");
                    detail.setAction("跳过");
                    detail.setSuccess(false);
                    detail.setMessage("缺少人员编号和身份证号");
                    skipCount++;
                    details.add(detail);
                    task.setProgress(i + 1);
                    continue;
                }

                try {
                    // 生成账号（支持管理员手动修正）
                    String idCardLast4 = row.getIdCard() != null && row.getIdCard().length() >= 4
                            ? row.getIdCard().substring(row.getIdCard().length() - 4)
                            : "0000";
                    String username = PinyinUtil.generateUsername(row.getNickname(), idCardLast4);
                    if (adjustments != null && adjustments.containsKey(i + 1)) {
                        username = adjustments.get(i + 1);
                    }
                    detail.setUsername(username);

                    // 匹配组织
                    String orgNo = orgImportService.findBestOrgNo(row,
                            orgResult.orgNoMap, orgResult.deptNoMap);

                    // (personnel_id, id_card) 双键匹配
                    User existingUser = userMapper.selectOne(
                            new LambdaQueryWrapper<User>()
                                    .eq(User::getPersonnelId, row.getPersonnelId())
                                    .last("LIMIT 1")
                    );
                    if (existingUser == null) {
                        existingUser = userMapper.selectOne(
                                new LambdaQueryWrapper<User>()
                                        .eq(User::getIdCard, row.getIdCard())
                                        .last("LIMIT 1")
                        );
                    }

                    if (existingUser != null) {
                        // 白名单覆盖 —— 不改 username，保持已有账号不变
                        boolean passwordChanged = existingUser.getIsFirstLogin() != null
                                && existingUser.getIsFirstLogin() == 0;

                        existingUser.setNickname(row.getNickname());
                        existingUser.setPersonnelId(row.getPersonnelId());
                        existingUser.setIdCard(row.getIdCard());
                        existingUser.setOrgNo(orgNo);

                        if (!passwordChanged) {
                            existingUser.setPassword(new BCryptPasswordEncoder().encode("1234@abcD"));
                            existingUser.setIsFirstLogin(1);
                        }

                        userMapper.updateById(existingUser);
                        detail.setAction("覆盖");
                        updateCount++;
                    } else {
                        String finalUsername = ensureUniqueUsername(username);
                        detail.setUsername(finalUsername);

                        User newUser = new User();
                        newUser.setUsername(finalUsername)
                                .setPassword(new BCryptPasswordEncoder().encode("1234@abcD"))
                                .setNickname(row.getNickname())
                                .setPersonnelId(row.getPersonnelId())
                                .setIdCard(row.getIdCard())
                                .setOrgNo(orgNo)
                                .setPhone("")
                                .setGender("0")
                                .setUserType("1")
                                .setFans(0)
                                .setAttention(0)
                                .setGood(0)
                                .setIsAlive(0)
                                .setIsFirstLogin(1)
                                .setCreateTime(dateFormat.format(new Date()));
                        userMapper.insert(newUser);
                        detail.setAction("新增");
                        newCount++;
                    }

                    detail.setSuccess(true);
                    detail.setMessage("导入成功");
                } catch (Exception e) {
                    detail.setSuccess(false);
                    detail.setMessage(e.getMessage());
                    failCount++;
                }
                details.add(detail);
                task.setProgress(i + 1);
            }

            result.setUserNewCount(newCount);
            result.setUserUpdatedCount(updateCount);
            result.setUserFailCount(failCount + skipCount);
            result.setUserSuccessCount(newCount + updateCount);
            result.setDetails(details);
            task.setResult(result);
            task.setStatus("done");
        } catch (Exception e) {
            task.setStatus("error");
            task.setError(e.getMessage());
            e.printStackTrace();
        }
    }

    /** 保证 username 唯一，重复时追加 _1, _2 ... */
    private String ensureUniqueUsername(String username) {
        String candidate = username;
        int suffix = 1;
        while (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, candidate)) > 0) {
            candidate = username + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    @Override
    public ResultBean adminUpdateUserDetail(AdminUserUpdateParam param) {
        if (param.getId() == null) {
            return ResultBean.error("用户ID不能为空");
        }
        User user = this.getById(param.getId());
        if (user == null) {
            return ResultBean.error("用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(param.getId());

        if (StringUtils.isNoneBlank(param.getNickname())) {
            updateUser.setNickname(param.getNickname());
        }
        if (StringUtils.isNoneBlank(param.getPhone())) {
            updateUser.setPhone(param.getPhone());
        }
        if (StringUtils.isNoneBlank(param.getOrgNo())) {
            updateUser.setOrgNo(param.getOrgNo());
        }
        if (StringUtils.isNoneBlank(param.getUserType())) {
            updateUser.setUserType(param.getUserType());
        }
        if (param.getIsAlive() != null) {
            updateUser.setIsAlive(param.getIsAlive());
        }
        if (StringUtils.isNoneBlank(param.getPersonnelId())) {
            updateUser.setPersonnelId(param.getPersonnelId());
        }
        if (StringUtils.isNoneBlank(param.getIdCard())) {
            updateUser.setIdCard(param.getIdCard());
        }
        if (StringUtils.isNoneBlank(param.getUsername())) {
            // 校验用户名唯一性
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, param.getUsername())
                            .ne(User::getId, param.getId())
                            .last("LIMIT 1")
            );
            if (existing != null) {
                return ResultBean.error("用户名已被其他用户使用");
            }
            updateUser.setUsername(param.getUsername());
        }
        // 优先使用自定义密码，其次判断是否重置
        if (StringUtils.isNoneBlank(param.getPassword())) {
            updateUser.setPassword(new BCryptPasswordEncoder().encode(param.getPassword()));
            updateUser.setIsFirstLogin(0);
        } else if (param.getResetPassword() != null && param.getResetPassword()) {
            updateUser.setPassword(new BCryptPasswordEncoder().encode("1234@abcD"));
            updateUser.setIsFirstLogin(1);
        }

        userMapper.updateById(updateUser);
        return ResultBean.success("更新成功");
    }

    @Override
    public ResultBean adminAddUser(AdminUserAddParam param) {
        // 校验用户名唯一性
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, param.getUsername())) > 0) {
            return ResultBean.error("用户名已存在");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        User newUser = new User();
        newUser.setUsername(param.getUsername())
                .setPassword(new BCryptPasswordEncoder().encode("1234@abcD"))
                .setNickname(param.getNickname())
                .setPhone(param.getPhone() != null ? param.getPhone() : "")
                .setOrgNo(param.getOrgNo())
                .setGender("0")
                .setUserType(param.getUserType() != null ? param.getUserType() : "1")
                .setIsAlive(param.getIsAlive() != null ? param.getIsAlive() : 0)
                .setFans(0)
                .setAttention(0)
                .setGood(0)
                .setIsFirstLogin(1)
                .setCreateTime(dateFormat.format(new Date()));
        userMapper.insert(newUser);
        return ResultBean.success("用户创建成功");
    }

}
