package com.walker.utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 内容质量检测工具 — 基于规则的垃圾/低质量内容识别。
 * <p>
 * 采用简化的贝叶斯思想：对多维度特征（长度、字符多样性、重复模式、关键词）分别打分，
 * 加权求和得到质量分（0-100）。低于阈值的视为垃圾内容，不计入积分。
 * <p>
 * 调用方式：{@link #checkContent(String, String)} → {@link QualityResult}
 */
public class ContentQualityUtil {

    /** 垃圾内容判定阈值：score < 此值视为 spam */
    private static final int SPAM_THRESHOLD = 40;

    /** 边界阈值：score < 此值视为低质量（不推荐但允许展示） */
    private static final int LOW_QUALITY_THRESHOLD = 60;

    // ── 常见垃圾/灌水短语（从 bbs_sensitive_word 补充，此处为硬编码兜底） ──

    private static final List<String> SPAM_PHRASES = Arrays.asList(
        // 无意义叠词
        "哈哈哈", "嘻嘻嘻", "嘿嘿嘿", "啊啊啊", "嗯嗯嗯", "哦哦哦", "呵呵呵",
        "啦啦啦", "呜呜呜", "哈哈哈啊", "嘿嘿",
        // 灌水常用
        "沙发", "占位", "占楼", "路过", "马克", "mark", "mark一下",
        "顶", "顶贴", "灌水", "水水水", "水帖", "路过看看",
        "来了", "看看", "路过", "打卡", "签到",
        // 纯数字灌水
        "666", "6666", "66666", "666666", "888", "8888", "111", "11111",
        "123", "1234", "12345", "123456",
        // 无意义单字/双字
        "好", "嗯", "哦", "啊", "额", "呃", "好吧", "可以",
        "是的", "对的", "不错", "挺好", "挺好的", "还好",
        // 测试类
        "test", "测试", "测试测试", "测试一下", "testtest"
    );

    // ── 正则模式预编译 ──

    /** 连续3个以上相同字符（含中文和英文字母数字），如 "哈哈哈"、"111"、"aaa" */
    private static final Pattern REPEAT_PATTERN = Pattern.compile("(.)\\1{2,}");

    /** 纯数字 */
    private static final Pattern PURE_DIGITS = Pattern.compile("^\\d+$");

    /** 纯标点/符号（含空格） */
    private static final Pattern PURE_SYMBOLS = Pattern.compile("^[\\s\\p{Punct}★☆♠♥♦♣△▲▽▼○●◎◇◆□■]+$");

    /** 纯英文字母（无意义，如 "abc"、"xyz"） */
    private static final Pattern PURE_ASCII_LETTERS = Pattern.compile("^[a-zA-Z]{1,5}$");

    // ── 公共 API ──

    /**
     * 检测内容质量
     *
     * @param title   标题（可为 null）
     * @param content 正文内容（纯文本或 Markdown）
     * @return 质量检测结果
     */
    public static QualityResult checkContent(String title, String content) {
        String plainTitle = cleanHtml(title);
        String plainContent = cleanHtml(content);

        // 合并检查
        String fullText = ((plainTitle != null ? plainTitle : "") + " " + (plainContent != null ? plainContent : "")).trim();

        // 空内容
        if (fullText.isEmpty()) {
            return new QualityResult(0, true, "内容为空");
        }

        int score = 100; // 满分 100，逐项扣分
        List<String> reasons = new ArrayList<>();

        // ── 1. 长度检查 ──
        if (plainContent != null && plainContent.length() < 3) {
            score -= 50;
            reasons.add("内容过短（不足3字）");
        } else if (plainContent != null && plainContent.length() < 8) {
            score -= 20;
            reasons.add("内容较短");
        }

        if (plainTitle != null && plainTitle.length() < 2) {
            score -= 30;
            reasons.add("标题过短");
        }

        // ── 2. 纯数字/纯符号 ──
        if (plainContent != null) {
            String trimmed = plainContent.trim();
            if (PURE_DIGITS.matcher(trimmed).matches()) {
                score -= 60;
                reasons.add("内容为纯数字");
            } else if (PURE_SYMBOLS.matcher(trimmed).matches()) {
                score -= 60;
                reasons.add("内容为纯符号");
            } else if (PURE_ASCII_LETTERS.matcher(trimmed).matches()) {
                score -= 40;
                reasons.add("内容为无意义英文字母");
            }
        }

        // ── 3. 重复字符比例 ──
        double diversity = calcCharDiversity(fullText);
        if (diversity < 0.1) {
            score -= 60;
            reasons.add("字符多样性极低（" + String.format("%.0f", diversity * 100) + "%）");
        } else if (diversity < 0.2) {
            score -= 35;
            reasons.add("字符多样性较低（" + String.format("%.0f", diversity * 100) + "%）");
        }

        // ── 4. 连续重复字符 ──
        if (REPEAT_PATTERN.matcher(fullText).find()) {
            // 检查是否整个内容基本都是重复字符
            String noRepeat = fullText.replaceAll("(.)\\1{2,}", "");
            if (noRepeat.length() < fullText.length() * 0.4) {
                score -= 50;
                reasons.add("大量重复字符");
            } else {
                score -= 15;
                reasons.add("包含重复字符");
            }
        }

        // ── 5. 垃圾关键词匹配 ──
        String lowerText = fullText.toLowerCase();
        boolean matchedSpam = false;
        for (String phrase : SPAM_PHRASES) {
            if (lowerText.contains(phrase.toLowerCase())) {
                matchedSpam = true;
                score -= 40;
                reasons.add("匹配垃圾关键词「" + phrase + "」");
                break; // 只扣一次
            }
        }

        // ── 6. 敏感词命中也降低质量分 ──
        if (!matchedSpam && SensitiveWordUtil.getSensitiveWords().stream()
                .anyMatch(w -> lowerText.contains(w.toLowerCase()))) {
            score -= 30;
            reasons.add("包含敏感词");
        }

        // ── 7. 内容与标题相关性（简单检查：内容是否完全不含标题中的词） ──
        if (plainTitle != null && plainTitle.length() >= 4 && plainContent != null) {
            String[] titleChars = plainTitle.split("");
            boolean hasOverlap = false;
            for (String c : titleChars) {
                if (c.length() > 1 && plainContent.contains(c)) {
                    hasOverlap = true;
                    break;
                }
            }
            // 不强制要求重叠，仅做参考，不扣分
        }

        // ── 最终判定 ──
        score = Math.max(0, score);
        boolean isSpam = score < SPAM_THRESHOLD;
        boolean isLowQuality = score < LOW_QUALITY_THRESHOLD && !isSpam;

        String verdict = isSpam ? "垃圾内容"
                : isLowQuality ? "低质量内容"
                : "正常内容";

        return new QualityResult(score, isSpam, isLowQuality,
                reasons.isEmpty() ? verdict : verdict + "（" + String.join("、", reasons) + "）");
    }

    /**
     * 便捷方法：判断是否为垃圾内容
     */
    public static boolean isSpam(String title, String content) {
        return checkContent(title, content).isSpam();
    }

    // ── 内部工具方法 ──

    /**
     * 去除 HTML 标签，返回纯文本
     */
    private static String cleanHtml(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]+>", "").replaceAll("![\\s\\S]*?]\\([\\s\\S]*?\\)", "")
                .trim();
    }

    /**
     * 计算字符多样性：去重后字符数 / 总字符数
     */
    private static double calcCharDiversity(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 只考虑中文和字母数字（忽略空白和标点）
        String filtered = text.replaceAll("[\\s\\p{Punct}]", "");
        if (filtered.isEmpty()) return 0;
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : filtered.toCharArray()) {
            uniqueChars.add(c);
        }
        return (double) uniqueChars.size() / filtered.length();
    }

    // ── 结果类 ──

    public static class QualityResult {
        private final int score;
        private final boolean spam;
        private final boolean lowQuality;
        private final String detail;

        public QualityResult(int score, boolean spam, String detail) {
            this(score, spam, false, detail);
        }

        public QualityResult(int score, boolean spam, boolean lowQuality, String detail) {
            this.score = score;
            this.spam = spam;
            this.lowQuality = lowQuality;
            this.detail = detail;
        }

        /** 质量分 0-100 */
        public int getScore() { return score; }

        /** 是否为垃圾内容（score < 40） */
        public boolean isSpam() { return spam; }

        /** 是否为低质量内容（40 <= score < 60） */
        public boolean isLowQuality() { return lowQuality; }

        /** 详细判定原因 */
        public String getDetail() { return detail; }

        /** 是否通过质量检查（非垃圾） */
        public boolean isPassed() { return !spam; }

        @Override
        public String toString() {
            return "QualityResult{score=" + score + ", spam=" + spam
                    + ", lowQuality=" + lowQuality + ", detail='" + detail + "'}";
        }
    }
}
