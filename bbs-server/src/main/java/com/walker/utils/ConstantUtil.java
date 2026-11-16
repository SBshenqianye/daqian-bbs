package com.walker.utils;

/**
 * @Author chengQing
 * @Date 2026/3/6 10:59
 * @PackageName:com.walker.utils
 * @ClassName: ConstantUtil
 * @Description: 静态变量工具类
 */
public class ConstantUtil {
    /**
     * 内江市公司单位编号
     */
    public static final String ORG_NEI_JIANG = "51404";
    /**
     * 魔法值-01
     */
    public static final String MANA_ZERO_ONE = "01";
    /**
     * 魔法值-02
     */
    public static final String MANA_ZERO_TWO = "02";
    /**
     * 魔法值-9
     */
    public static final int MANA_NINE = 9;

    /**
     * 魔法值-积分开始日期-键
     */
    public static final String MANA_POINTS_START_TIME = "points-start-time";

    /**
     * 魔法值-积分结束日期-键
     */
    public static final String MANA_POINTS_END_TIME = "point-end-time";
    /**
     * 魔法值-发帖积分-键
     */
    public static final String MANA_POST = "post";
    /**
     * 魔法值-回帖积分-键
     */
    public static final String MANA_REPLY = "reply";

    /**
     * 魔法值-0
     */
    public static final String MANA_ZERO = "0";

    /**
     * 魔法值-1
     */
    public static final String MANA_ONE = "1";

    /**
     * 魔法值-2
     */
    public static final String MANA_TWO = "2";

    /**
     * 魔法值-精华帖积分-键
     */
    public static final String MANA_FEATURED = "featured";

    /**
     * 版主履职奖励-自动发放开关（dict_value='1'开启）
     */
    public static final String MODERATOR_REWARD_AUTO = "moderator_reward_auto";

    /**
     * 版主履职奖励-发放日（dict_value=日期，1-28）
     */
    public static final String MODERATOR_REWARD_DAY = "moderator_reward_day";

    /**
     * 版主履职奖励-每月积分（默认 400）
     */
    public static final String MODERATOR_REWARD_POINTS = "moderator_reward_points";

    /**
     * 每日有效浏览分钟数（默认 1）
     */
    public static final String LOGIN_BROWSE_MINUTES = "login_browse_minutes";

    /**
     * 月累计扣分提醒-开关（默认 1）
     */
    public static final String MONTHLY_DEDUCTION_ALERT_ENABLED = "monthly_deduction_alert_enabled";

    /**
     * 月累计扣分提醒-阈值（默认 10）
     */
    public static final String MONTHLY_DEDUCTION_ALERT_THRESHOLD = "monthly_deduction_threshold";

    /** 发帖审核模式：AUTO / PRE_REVIEW / POST_REVIEW */
    public static final String ARTICLE_PUBLISH_MODE = "article_publish_mode";

    /** 精华终审模式：ADMIN_FINAL / MODERATOR_FINAL */
    public static final String FEATURED_FINAL_REVIEW_MODE = "featured_final_review_mode";
}
