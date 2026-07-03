package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.FansMapper;
import com.walker.pojo.Fans;
import com.walker.pojo.User;
import com.walker.service.FansService;
import com.walker.service.UserService;
import com.walker.utils.FilePathNormalizer;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Service
public class FansServiceImpl extends ServiceImpl<FansMapper, Fans> implements FansService {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private FansMapper fansMapper;

    @Autowired
    private UserService userService;

    @Override
    public boolean cancelFans(Integer userId, Integer attentionId) {
        Fans fans = this.getFans(userId, attentionId);
        if (ObjectUtils.isEmpty(fans)){
            return false;
        }
        fansMapper.deleteById(fans.getId());
        // 更新用户表
        User attentionUser = userService.getById(userId);
        attentionUser.setAttention(attentionUser.getAttention() - 1);
        userService.updateById(attentionUser);

        User fansUser = userService.getById(attentionId);
        fansUser.setFans(fansUser.getFans() - 1);
        userService.updateById(fansUser);

        return true;
    }

    @Override
    public boolean saveForm(Fans form) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDate = format.format(new Date());
        form.setCreateTime(currentDate);
        this.save(form);
        // 更新用户表
        User attentionUser = userService.getById(form.getUserId());
        attentionUser.setAttention(attentionUser.getAttention() + 1);
        userService.updateById(attentionUser);

        User fansUser = userService.getById(form.getAttentionId());
        fansUser.setFans(fansUser.getFans() + 1);
        userService.updateById(fansUser);

        return true;
    }

    @Override
    public boolean getFansInfo(Integer userId, Integer attentionId) {
        Fans fans = this.getFans(userId, attentionId);
        if (ObjectUtils.isEmpty(fans)){
            return false;
        }
        return true;
    }

    @Override
    public List<Map<String, String>> getAttentionList(Integer userId) {
        List<Fans> list = fansMapper.selectList(new LambdaQueryWrapper<Fans>()
                .eq(Fans::getUserId, userId)
        );
        return this.transFans(list,"1");
    }

    @Override
    public List<Map<String, String>> getFansList(Integer userId) {
        List<Fans> list = fansMapper.selectList(new LambdaQueryWrapper<Fans>()
                .eq(Fans::getAttentionId, userId)
        );
        return this.transFans(list,"2");
    }

    /**
     * 数据封装
     * @param list
     * @return
     */
    private List<Map<String,String>> transFans(List<Fans> list,String type){
        ArrayList<Map<String, String>> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)){
            return result;
        }
        for (Fans fans : list) {
            Map<String, String> map = new HashMap<>();
            Integer userId = 0;
            if ("1".equals(type)){
                userId = fans.getAttentionId();
            }else {
                userId = fans.getUserId();
            }
            User user = userService.getById(userId);
            map.put("name",user.getNickname());
            map.put("portrait", FilePathNormalizer.normalizeFieldUrl(user.getPortrait(), contextPath));
            map.put("createTime",fans.getCreateTime());
            result.add(map);
        }
        return result;
    }

    /**
     * 通过用户ID和被关注用户ID查询关注信息
     * @param userId
     * @param attentionId
     * @return
     */
    private Fans getFans(Integer userId,Integer attentionId){
        LambdaQueryWrapper<Fans> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Fans::getAttentionId,attentionId)
                .eq(Fans::getUserId,userId);
        return fansMapper.selectOne(lambdaQueryWrapper);
    }

}
