package com.walker.service;

import com.walker.mapper.ArticleLabelMapper;
import com.walker.pojo.ArticleLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 标签积分策略：no_points=1 时，该标签下的发帖/评论/回复/采纳/精华/热度等互动不计分。
 * 每日浏览、举报核实奖励不受标签影响。
 */
@Service
public class LabelPointsPolicyService {

    @Autowired
    private ArticleLabelMapper articleLabelMapper;

    public boolean isNoPoints(Integer labelId) {
        if (labelId == null) {
            return false;
        }
        ArticleLabel label = articleLabelMapper.selectById(labelId);
        return label != null && Integer.valueOf(1).equals(label.getNoPoints());
    }

    public boolean shouldAward(Integer labelId, String action) {
        if (isNoPoints(labelId)) {
            return false;
        }
        return true;
    }

    public boolean allowsAnonymous(Integer labelId) {
        if (labelId == null) {
            return false;
        }
        ArticleLabel label = articleLabelMapper.selectById(labelId);
        return label != null && Integer.valueOf(1).equals(label.getAllowAnonymous());
    }
}
