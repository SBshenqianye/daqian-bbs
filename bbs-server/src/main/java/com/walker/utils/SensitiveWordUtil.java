package com.walker.utils;

import com.walker.pojo.Article;
import com.walker.service.ArticleService;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author chengQing
 * @Date 2026/4/9 14:57
 * @PackageName:com.walker.utils
 * @ClassName: SensitiveWordUtil
 * @Description: 敏感词工具类
 */
public class SensitiveWordUtil {
    /**
     * 敏感词列表（全量，用于质量检测与命中统计）
     */
    public static List<String> sensitiveWords = new ArrayList<>();

    /**
     * 参与脱敏遮蔽的词（mask_enabled=1）。
     * 与 {@link #sensitiveWords} 分开维护：关闭遮蔽的词仍保留在监控口径里，只是不再前台打码。
     */
    public static List<String> maskWords = new ArrayList<>();

    /**
     * 垃圾词/灌水词（category=spam）。命中后不打码，走"警告放行"：内容可见但不计分。
     */
    public static List<String> spamWords = new ArrayList<>();

    /**
     * 更新敏感词列表（由 Service 调用）。默认全部参与遮蔽、且均为严禁词，兼容旧调用。
     */
    public static void updateSensitiveWords(List<String> words) {
        updateSensitiveWords(words, words, new ArrayList<String>());
    }

    /**
     * 兼容两参数调用：全量为严禁词，指定其中参与遮蔽的子集。
     */
    public static void updateSensitiveWords(List<String> words, List<String> maskable) {
        updateSensitiveWords(words, maskable, new ArrayList<String>());
    }

    /**
     * 更新敏感词列表，并指定其中参与脱敏遮蔽的子集。
     *
     * @param words     全量敏感词（监控口径）
     * @param maskable  需要在前台打码的词
     * @param spam      垃圾词/灌水词
     */
    public static void updateSensitiveWords(List<String> words, List<String> maskable, List<String> spam) {
        sensitiveWords = new ArrayList<>(words == null ? new ArrayList<String>() : words);
        maskWords = new ArrayList<>(maskable == null ? new ArrayList<String>() : maskable);
        spamWords = new ArrayList<>(spam == null ? new ArrayList<String>() : spam);
        System.out.println("敏感词已更新，共 " + sensitiveWords.size() + " 个，其中参与遮蔽 "
                + maskWords.size() + " 个，垃圾词 " + spamWords.size() + " 个");
    }

    /**
     * 获取敏感词列表
     */
    public static List<String> getSensitiveWords() {
        return new ArrayList<>(sensitiveWords);
    }

    /**
     * 获取参与脱敏遮蔽的词列表
     */
    public static List<String> getMaskWords() {
        return new ArrayList<>(maskWords);
    }

    /**
     * 获取垃圾词列表（警告放行口径）
     */
    public static List<String> getSpamWords() {
        return new ArrayList<>(spamWords);
    }

    /**
     * 命中检测：返回文本中命中的严禁词（去重，最多 {@code limit} 个）。
     * 用于写库时采集命中记录与判定是否进入审核。
     */
    public static List<String> match(String text, int limit) {
        List<String> hits = new ArrayList<>();
        if (text == null || text.isEmpty() || sensitiveWords.isEmpty()) {
            return hits;
        }
        for (String word : sensitiveWords) {
            if (word != null && !word.isEmpty() && text.contains(word)) {
                if (!hits.contains(word)) {
                    hits.add(word);
                    if (hits.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return hits;
    }

    /**
     * 命中检测（默认最多保留 10 个词）
     */
    public static List<String> match(String text) {
        return match(text, 10);
    }

    /**
     * 脱敏处理
     */
    public static String desensitize(String text) {
        if (text == null || text.isEmpty() || maskWords.isEmpty()) {
            return text;
        }

        String result = text;
        // 注意：必须用字面替换（replace），不能用 replaceAll——敏感词库中含正则元字符（如 *、{10}、[、.）的词条
        // 会作为正则编译而抛 PatternSyntaxException（曾导致帖子列表接口 500）
        for (String word : maskWords) {
            result = result.replace(word, "***");
        }
        return result;
    }

    /**
     * 方法描述 批量脱敏文章
     * @author chengQing
     * @date 2026/4/9 15:22
     * @param articles 文章列表
     * @return List<Article> 脱敏之后的文章列表
     */
    public static List<Article> desensitizeArticles(List<Article> articles) {
        if (CollectionUtils.isEmpty( articles)) {
            return articles;
        }
        // 脱敏标题、内容、内容HTML、摘要
        for (Article article : articles) {
            article.setArticleTitle(desensitize(article.getArticleTitle()));
            article.setArticleContentHtml(desensitize(article.getArticleContentHtml()));
            article.setArticleContent(desensitize(article.getArticleContent()));
            article.setArticleSummary(desensitize(article.getArticleSummary()));
        }
        return articles;
    }

    /**
     * 方法描述 批量脱敏文章
     * @author chengQing
     * @date 2026/4/9 15:22
     * @param article 文章
     * @return List<Article> 脱敏之后的文章列表
     */
    public static Article desensitizeArticle(Article article) {
        if (!Objects.isNull(article)) {
            // 脱敏标题、内容、内容HTML、摘要
            article.setArticleTitle(desensitize(article.getArticleTitle()));
            article.setArticleContentHtml(desensitize(article.getArticleContentHtml()));
            article.setArticleContent(desensitize(article.getArticleContent()));
            article.setArticleSummary(desensitize(article.getArticleSummary()));
        }
        return article;
    }

}
