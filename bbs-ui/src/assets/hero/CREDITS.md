# 首页 Hero 地标图片来源

首页顶部影像带默认用以下 4 张照片；**后台「系统配置 → 首页 Hero 影像带」可以逐格替换**（`v078` 起），
上传后写入 `bbs_system_config` 的 `hero_image_1..4` / `hero_caption`，留空即回到本文件记录的默认图。
圣水寺与隆昌石牌坊来自 Wikimedia Commons（按各自 CC BY-SA 授权展示）；俯瞰内江、太白楼按内部宣传使用，
来源与作者一并记录在此。如替换/新增图片，请同步更新本文件与页面右下角的来源标注。

| 文件 | 内容 | 作者 | 授权 | 来源 |
|------|------|------|------|------|
| `neijiang-overlook.jpg` | 俯瞰内江（沱江大弯、城区与跨江大桥，航拍） | 项目方供图 | 内部使用 | 用户提供（`需求更新/11月/内江俯瞰图.jpg`，原图 3840×2160） |
| `neijiang-shengshui.jpg` | 内江圣水寺 | Xin | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | [File:Shengshui Temple Neijiang.JPG](https://commons.wikimedia.org/wiki/File:Shengshui_Temple_Neijiang.JPG) |
| `neijiang-paifang.jpg` | 隆昌石牌坊（北关“民之父母”牛树梅德政坊） | Zhangzhugang | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | [File:Longchang Shi Paifang 2014.04.23 10-02-04.jpg](https://commons.wikimedia.org/wiki/File:Longchang_Shi_Paifang_2014.04.23_10-02-04.jpg) |
| `neijiang-power-building.jpg` | 国网大楼（内江电力大楼） | 项目方供图 | 内部使用 | 用户提供（`大楼图片.jpg`） |
| `neijiang-taibai.jpg` | 内江太白楼（**已从影像带撤下，文件保留备回退**） | 高勇 | 公开报道配图，内部宣传使用 | [自贡网《内自同城｜太白楼：李白送友写下的诗，内江人把它建成了楼》](https://files.zgm.cn/thumb/74/74/7474b19710ad9ba4e5251914a39b3e81.jpg) |
| `neijiang-tuojiang.jpg` | 沱江畔的内江城区（**已从影像带撤下，文件保留备回退**） | PanShiBo | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | [File:NeiJiang.JPG](https://commons.wikimedia.org/wiki/File:NeiJiang.JPG) |

俯瞰图原图 3840×2160，缩到 1600×900、JPEG q84；太白楼原图为 800×534，底部带“内江 高勇 摄”水印，
已裁掉底部 30%（保留楼体与匾额）后统一缩放为 1100px 宽、JPEG q84；圣水寺与石牌坊为 1100px 宽、JPEG q82；
国网大楼为原始 600×802 竖图，按 38% 纵向焦点裁切并放大 1.18 展示。

## 后台替换（推荐）

后台「系统配置 → 首页 Hero 影像带」：逐格上传（自动走 `/common/upload`，落 `common/upload/yyyy-MM-dd/`）、
可单格「恢复默认」、可改右下角来源标注；保存后用户端刷新即生效。第 4 格换成自定义图时不再套用竖幅推近裁切，
一律 `object-fit: cover` 居中。上传建议横图 ≥1600×900，单张 ≤10MB。

## 直接换文件（发版方式）

用同名文件覆盖本目录下的图片即可，无需改代码：

1. 第 1～3 格：`neijiang-overlook.jpg` / `neijiang-shengshui.jpg` / `neijiang-paifang.jpg`（`background-image` 渲染）
2. 第 4 格：`neijiang-power-building.jpg`，由 `.hero-photo__slot` + `img.hero-photo__slot-img` 渲染，
   竖图可通过 `object-position` 调整取景焦点

`hero-photo__slot` 与其他图片同为等宽自适应格子，替换后叠接与版式自动保持一致。
