package com.walker.service;

import com.walker.mapper.SlideshowMapper;
import com.walker.pojo.Slideshow;
import com.walker.service.impl.SlideshowServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlideshowServiceTest {

    @InjectMocks
    private SlideshowServiceImpl slideshowService;

    @Mock
    private SlideshowMapper slideshowMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = slideshowService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(slideshowService, slideshowMapper);
    }

    @Test
    @DisplayName("查询所有轮播图 → 返回列表（最多4个）")
    void queryAllSlideshow_returnsList() {
        Slideshow s1 = new Slideshow();
        s1.setSlideshowId(1);
        s1.setImageUrl("img1.png");
        s1.setSuccessive(3);
        when(slideshowMapper.selectList(any())).thenReturn(Arrays.asList(s1));

        List<Slideshow> result = slideshowService.queryAllSlideshow();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("查询所有轮播图 → 无数据返回空列表")
    void queryAllSlideshow_empty() {
        when(slideshowMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Slideshow> result = slideshowService.queryAllSlideshow();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("保存轮播图 → 成功")
    void saveSlideshow_succeeds() {
        when(slideshowMapper.insert(any(Slideshow.class))).thenReturn(1);

        ResultBean result = slideshowService.saveSlideshow(5, "img.png");

        assertEquals(200, result.getCode());
        verify(slideshowMapper).insert(any(Slideshow.class));
    }

    @Test
    @DisplayName("管理端获取所有轮播图 → 返回列表")
    void getAllSlideshowByAdmin_returnsAll() {
        Slideshow s1 = new Slideshow();
        Slideshow s2 = new Slideshow();
        when(slideshowMapper.selectList(null)).thenReturn(Arrays.asList(s1, s2));

        ResultBean result = slideshowService.getAllSlideshowByAdmin();

        assertEquals(200, result.getCode());
        List<?> list = (List<?>) result.getObj();
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("更新轮播图优先级 → 成功")
    void updateSuccessive_succeeds() {
        when(slideshowMapper.updateById(any())).thenReturn(1);

        ResultBean result = slideshowService.updateSuccessive(1, 10);

        assertEquals(200, result.getCode());
        verify(slideshowMapper).updateById(any(Slideshow.class));
    }
}
