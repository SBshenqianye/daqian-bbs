package com.walker.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内容质量检测回归测试（2026-09 生产故障：正常文章被误判为垃圾导致静默发布失败）
 *
 * 覆盖两类场景：
 * 1. 正常文章（含"好/看看/可以/不错/顶/打卡"等高频词、纯图片/图文、Markdown 语法）→ 不得判为垃圾
 * 2. 真实垃圾（叠词灌水、灌水黑话、纯数字、空内容）→ 必须判为垃圾
 */
class ContentQualityUtilTest {

    // ── 场景 1：正常文章不得误判 ──

    @Test
    @DisplayName("标题/正文含高频词「好」不误判")
    void normalArticleWithHaoWord() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "大家好，今天分享一个好消息",
                "大家好，今天给大家分享一个好消息：我们的项目顺利上线了，大家都辛苦了。");
        assertFalse(r.isSpam(), "含「好」的正常文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("正文含「看看」「可以」不误判")
    void normalArticleWithCommonWords() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "新版首页上线",
                "大家可以看看新版首页的效果，有什么问题可以随时反馈给我，谢谢支持。");
        assertFalse(r.isSpam(), "含「看看/可以」的正常文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("正文含「不错」「打卡」不误判")
    void normalArticleWithNiceAndCheckin() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "本周工作总结",
                "这周整体进展不错，团队每天都按时打卡，周五下班前完成了全部交付。");
        assertFalse(r.isSpam(), "含「不错/打卡」的正常文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("纯图片文章不误判（图片语法被剥离后正文为空，不扣内容过短）")
    void pureImageArticle() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "活动现场照片",
                "![活动现场](/bbs-server/files/2026/09/23/abc.jpg)");
        assertFalse(r.isSpam(), "纯图片文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("图文混排不误判")
    void imageWithTextArticle() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "团建游记",
                "周末团建去了郊区，风景很好。\n![合影](/bbs-server/files/2026/09/23/team.jpg)\n大家都很开心。");
        assertFalse(r.isSpam(), "图文混排文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("HTML 图片标签的图文内容不误判")
    void htmlImgArticle() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "产品截图说明",
                "这是新版本的截图：<img src=\"/bbs-server/files/2026/09/23/shot.png\"> 可以看到新的布局。");
        assertFalse(r.isSpam(), "含 <img> 的图文文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("含 Markdown 语法的技术文章不误判（mark 已从词表移除，避免误伤 Markdown）")
    void markdownArticle() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "Markdown 入门指南",
                "Markdown 是一种轻量级标记语言，**加粗**用两个星号，列表用减号，写起来很方便。");
        assertFalse(r.isSpam(), "含 Markdown 的技术文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("含「测试」二字的正常文章不误判（测试已从词表移除）")
    void articleWithTestWord() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "接口测试报告",
                "本次对发布接口做了完整的测试，覆盖正常发布、异常输入等场景，全部通过。");
        assertFalse(r.isSpam(), "含「测试」的正常文章不应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("短小但完整的正常分享不误判")
    void shortNormalArticle() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "食堂新菜",
                "今天食堂新出了红烧肉，味道很好，推荐大家去尝尝。");
        assertFalse(r.isSpam(), "正常短文章不应判为垃圾，实际: " + r.getDetail());
    }

    // ── 场景 2：真实垃圾必须拦截 ──

    @Test
    @DisplayName("叠词灌水必须判为垃圾")
    void spamWithLaughWords() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "哈哈哈",
                "哈哈哈哈哈哈哈哈哈");
        assertTrue(r.isSpam(), "叠词灌水应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("灌水黑话必须判为垃圾")
    void spamWithJargon() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "沙发",
                "沙发占楼，路过看看");
        assertTrue(r.isSpam(), "灌水黑话应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("纯数字灌水必须判为垃圾")
    void spamWithDigits() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "666",
                "666666 888888 123456");
        assertTrue(r.isSpam(), "纯数字灌水应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("空内容必须判为垃圾")
    void spamWithEmptyContent() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent("", "");
        assertTrue(r.isSpam(), "空内容应判为垃圾，实际: " + r.getDetail());
    }

    @Test
    @DisplayName("无意义重复字符必须判为垃圾")
    void spamWithRepeatedChars() {
        ContentQualityUtil.QualityResult r = ContentQualityUtil.checkContent(
                "水帖",
                "啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊");
        assertTrue(r.isSpam(), "无意义重复字符应判为垃圾，实际: " + r.getDetail());
    }
}
