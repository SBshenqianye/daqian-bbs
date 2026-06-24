package com.walker.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音工具类
 * 用于生成拼音账号和密码强度校验
 */
public class PinyinUtil {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    /**
     * 根据姓名和身份证后4位生成账号
     * 规则: 姓全拼(首字母大写) + 名每个字首字母(大写) + 身份证后4位
     * 示例: 李秋志 + 3911 → LiQZ3911
     *       袁志 + 0711  → YuanZ0711
     *
     * @param name       姓名（中文）
     * @param idCardLast4 身份证号后4位
     * @return 生成的用户名
     */
    public static String generateUsername(String name, String idCardLast4) {
        if (name == null || name.isEmpty()) {
            return "user" + idCardLast4;
        }

        StringBuilder sb = new StringBuilder();

        // 第一个字 = 姓，取全拼
        String surnamePinyin = getPinyin(name.charAt(0));
        if (surnamePinyin != null && !surnamePinyin.isEmpty()) {
            sb.append(Character.toUpperCase(surnamePinyin.charAt(0)));
            if (surnamePinyin.length() > 1) {
                sb.append(surnamePinyin.substring(1));
            }
        } else {
            sb.append(Character.toUpperCase(name.charAt(0)));
        }

        // 剩余字 = 名，取首字母大写
        for (int i = 1; i < name.length(); i++) {
            String pinyin = getPinyin(name.charAt(i));
            if (pinyin != null && !pinyin.isEmpty()) {
                sb.append(Character.toUpperCase(pinyin.charAt(0)));
            } else {
                sb.append(Character.toUpperCase(name.charAt(i)));
            }
        }

        // 追加身份证后4位
        sb.append(idCardLast4);

        return sb.toString();
    }

    /**
     * 获取单个汉字的拼音（取第一个读音，多音字走默认读音）
     */
    private static String getPinyin(char c) {
        try {
            String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
            if (pinyins != null && pinyins.length > 0) {
                return pinyins[0];
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            // ignore
        }
        return String.valueOf(c);
    }

    /**
     * 验证密码强度：至少8位，必须同时包含数字、小写字母、大写字母
     *
     * @param password 待校验的密码
     * @return 符合要求返回true，否则false
     */
    public static boolean checkPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
        }
        return hasDigit && hasLower && hasUpper;
    }

    /**
     * 获取密码强度等级
     *
     * @param password 密码
     * @return 0=弱, 1=中, 2=强
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return 0;
        }
        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
        }
        int score = (hasDigit ? 1 : 0) + (hasLower ? 1 : 0) + (hasUpper ? 1 : 0);
        // 8位以上有3种类型 = 强, 有2种 = 中, 1种或以下 = 弱
        return score >= 3 ? 2 : (score >= 2 ? 1 : 0);
    }
}
