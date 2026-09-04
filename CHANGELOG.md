# Changelog

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式。

## [未发布]

### 新增
- **bbs-ui 发帖页**：新增 Markdown 格式工具栏（加粗/斜体/删除线/标题/引用/代码/列表/分割线/链接/图片）
- **bbs-ui 发帖页**：新增实时 Markdown 预览面板，与正文页共用 mdToHtml 渲染 + github-markdown 样式

### 修复
- **bbs-ui 发帖页**：编辑已有帖子时 Markdown 语法符号被编辑器渲染导致预览无法还原格式
- **bbs-ui 发帖页**：段落换行丢失（htmlToMd 段落连接符 `\n` → `\n\n`）
- **bbs-ui 发帖页**：连续列表项之间空行导致序号重置为 1

### 优化
- **bbs-ui 发帖页**：有序列表保留原始序号显示，与编辑器中所见一致
- **bbs-ui 发帖页**：预览面板自适应内容高度，移除固定 max-height 限制
