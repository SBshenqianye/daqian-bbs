# 首页 Hero 地标图片来源

首页顶部影像带使用以下照片：圣水寺与隆昌石牌坊来自 Wikimedia Commons（按各自 CC BY-SA 授权展示），
太白楼取自公开报道配图（自贡网/内江日报，摄影 高勇），国网大楼由项目方提供。后两张按内部宣传使用，
来源与作者一并记录在此。如替换/新增图片，请同步更新本文件与页面右下角的来源标注。

| 文件 | 内容 | 作者 | 授权 | 来源 |
|------|------|------|------|------|
| `neijiang-taibai.jpg` | 内江太白楼（大千园圆顶山，仿唐三层楼阁） | 高勇 | 公开报道配图，内部宣传使用 | [自贡网《内自同城｜太白楼：李白送友写下的诗，内江人把它建成了楼》](https://files.zgm.cn/thumb/74/74/7474b19710ad9ba4e5251914a39b3e81.jpg) |
| `neijiang-shengshui.jpg` | 内江圣水寺 | Xin | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | [File:Shengshui Temple Neijiang.JPG](https://commons.wikimedia.org/wiki/File:Shengshui_Temple_Neijiang.JPG) |
| `neijiang-paifang.jpg` | 隆昌石牌坊（北关“民之父母”牛树梅德政坊） | Zhangzhugang | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | [File:Longchang Shi Paifang 2014.04.23 10-02-04.jpg](https://commons.wikimedia.org/wiki/File:Longchang_Shi_Paifang_2014.04.23_10-02-04.jpg) |
| `neijiang-power-building.jpg` | 国网大楼（内江电力大楼） | 项目方供图 | 内部使用 | 用户提供（`大楼图片.jpg`） |
| `neijiang-tuojiang.jpg` | 沱江畔的内江城区（**已从影像带撤下，文件保留备回退**） | PanShiBo | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | [File:NeiJiang.JPG](https://commons.wikimedia.org/wiki/File:NeiJiang.JPG) |

太白楼原图为 800×534，底部带“内江 高勇 摄”水印，已裁掉底部 30%（保留楼体与匾额）后统一缩放为 1100px 宽、JPEG q84；
圣水寺与石牌坊为 1100px 宽、JPEG q82；国网大楼为原始 600×802 竖图，按 38% 纵向焦点裁切并放大 1.18 展示。

## 最右一格（国网大楼）

第四格由 `bbs-ui/src/views/BBSForum.vue` 中的 `.hero-photo__slot` + `img.hero-photo__slot-img` 渲染，
图片以 14% 左缘渐变并入影像带。替换图片时：

1. 用同名文件覆盖 `bbs-ui/src/assets/hero/neijiang-power-building.jpg`（无需改代码），或
2. 改 `heroImages.building` 的引用路径；竖图可通过 `object-position` 调整取景焦点。

`hero-photo__slot` 与其他图片同为等宽自适应格子，替换后叠接与版式自动保持一致。
