package com.walker.controller;


import com.walker.pojo.Community;
import com.walker.service.CommunityService;
import com.walker.service.UserService;
import com.walker.vo.CommunityVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.CommunityParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Api(tags = "CommunityController")
@RestController
public class CommunityController {

    @Autowired
    private CommunityService communityService;
    @Autowired
    private UserService userService;

    @Value("${storage.path}")
    private String basePath;


    @ApiOperation(value = "获取所有社区名称")
    @GetMapping("/community/getCommunity")
    public List<Community> getCommunity(){

        List<Community> communities = communityService.queryAllCommunity();

        return communities;
    }


    @ApiOperation(value = "获取所有社区列表")
    @GetMapping("/common/community/getCommunityList")
    public List<CommunityVO> getCommunityList(){

        return communityService.queryAllCommunityList();

    }


    @ApiOperation(value = "通过社区ID获取社区信息")
    @PutMapping("/community/getCommunityById/{communityId}")
    public Community getCommunityById(@PathVariable("communityId") Integer communityId){

        return communityService.queryCommunityById(communityId);
    }

    @ApiOperation(value = "创建社区")
    @PostMapping(value = "/community/createCommunity")
    public ResultBean createCommunity(@RequestBody CommunityParam communityParam){

        return communityService.createCommunity(communityParam);
    }


    @ApiOperation(value = "获取所有的社区信息")
    @GetMapping("/admin/getAllCommunity/all")
    public ResultBean getAllCommunity(){
        return communityService.getAllCommunity();
    }

    @ApiOperation(value = "通过关键词搜索朝代信息")
    @GetMapping("/admin/getCommunityByKeywords/{keywords}")
    public ResultBean getCommunityByKeywords(@PathVariable String keywords){
        if (StringUtils.isNoneBlank(keywords)){
            return communityService.getCommunityByKeywords(keywords);
        }
        return ResultBean.error("关键词不能为空！");
    }


    @ApiOperation(value = "修改社区的状态")
    @PutMapping("/admin/updateCommunityStatus/{communityId}")
    public ResultBean updateCommunityStatus(@PathVariable Integer communityId){
        return communityService.updateCommunityStatus(communityId);
    }


    @ApiOperation(value = "通过社区id删除社区")
    @DeleteMapping("/admin/deleteCommunityByCommunityId/{communityId}")
    public ResultBean deleteCommunityByCommunityId(@PathVariable Integer communityId){
        return communityService.deleteCommunityByCommunityId(communityId);
    }


    @ApiOperation(value = "获取社区的数量")
    @GetMapping("/admin/getCommunityCount/all")
    public ResultBean getCommunityCount(){
        return communityService.getCommunityCount();
    }


    @ApiOperation(value = "上传社区图片放回地址")
    @PutMapping("/community/createCommunityReturnImage")
    public String createCommunityReturnImage(@RequestParam("userId") Integer id,@RequestParam("file") MultipartFile file) throws Exception{
        String pType = file.getContentType();
        pType = pType.substring(pType.indexOf("/") + 1);
        if ("jpeg".equals(pType)){
            pType = "jpg";
        }

        long time = System.currentTimeMillis();

        // 文件保存的路径
        String path = basePath +"Admin/communityImage/"+time+"_."+pType;
        String pathDB = "";

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
            pathDB = "/files/Admin/communityImage/" +time+"_."+pType;

        }catch (Exception e){
            e.printStackTrace();
        }

        return pathDB;
    }

}

