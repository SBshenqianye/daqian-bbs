package com.walker.controller;

import com.walker.pojo.ArticleLabel;
import com.walker.pojo.Article;
import com.walker.service.ArticleLabelService;
import com.walker.service.ArticleService;
import com.walker.service.BoardModeratorService;
import com.walker.service.CommentService;
import com.walker.service.ReplyService;
import com.walker.vo.ResultBean;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Q9：仍有有效版主的板块禁止删除。
 */
@ExtendWith(MockitoExtension.class)
class ArticleLabelDeleteGuardTest {

    @InjectMocks
    private ArticleLabelController controller;

    @Mock
    private ArticleLabelService articleLabelService;

    @Mock
    private ReplyService replyService;

    @Mock
    private ArticleService articleService;

    @Mock
    private CommentService commentService;

    @Mock
    private BoardModeratorService boardModeratorService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Article.class
        );
    }

    @Test
    @DisplayName("删除板块 → 仍有有效版主 → 拒绝且不删除")
    void delete_withActiveModerators_forbidden() {
        when(boardModeratorService.count(any())).thenReturn(2L);

        ArticleLabel label = new ArticleLabel();
        label.setLabelId(10);

        ResultBean result = controller.deleteArticleLabel(label);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("2 位版主"));
        verify(articleLabelService, never()).removeById(any());
    }

    @Test
    @DisplayName("删除板块 → 无有效版主 → 进入原有待审批检查")
    void delete_withoutActiveModerators_proceeds() {
        when(boardModeratorService.count(any())).thenReturn(0L);
        when(articleService.list(any())).thenReturn(new java.util.ArrayList<>());
        when(articleLabelService.removeById(10)).thenReturn(true);

        ArticleLabel label = new ArticleLabel();
        label.setLabelId(10);

        ResultBean result = controller.deleteArticleLabel(label);

        assertEquals(200, result.getCode());
        verify(articleLabelService).removeById(10);
    }
}
