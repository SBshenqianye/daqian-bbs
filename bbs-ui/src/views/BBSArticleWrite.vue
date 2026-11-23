<template>
  <div class="min-h-screen flex flex-col bg-surface">
    <!-- Main Content Area -->
    <main class="flex-grow flex flex-col pt-6 pb-24 px-4 sm:px-8 max-w-5xl mx-auto w-full">
      <!-- Title Input Section -->
      <div class="mb-5 relative flex items-center group">
        <textarea
          ref="titleInput"
          v-model="articleTitle"
          class="title-textarea w-full bg-transparent border-b border-transparent hover:border-outline-variant focus:border-transparent focus:ring-0 text-[24px] md:text-[28px] font-bold text-on-surface placeholder:text-outline/60 p-0 pb-2 resize-none overflow-hidden transition-all duration-200"
          placeholder="请输入文章标题"
          rows="1"
          maxlength="30"
          @input="autoResizeTitle"
        ></textarea>
        <span class="material-symbols-outlined absolute right-0 text-outline/60 pointer-events-none group-hover:text-outline">edit</span>
      </div>

      <!-- Markdown 格式操作栏 -->
      <div class="flex items-center gap-0.5 px-2.5 py-1.5 border border-outline-variant rounded-t-xl bg-surface-container-low/60 flex-wrap">
        <!-- 格式按钮 -->
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="加粗 (Ctrl+B)"
          @mousedown.prevent
          @click="insertMdFormat('bold')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="斜体 (Ctrl+I)"
          @mousedown.prevent
          @click="insertMdFormat('italic')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M10 4h4m-2 0v16m-4 0h8"/></svg>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="删除线"
          @mousedown.prevent
          @click="insertMdFormat('strikethrough')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M17 12H7m10-4H9m8 8H5"/></svg>
        </button>
        <span class="w-px h-5 bg-outline-variant mx-1"></span>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="标题"
          @mousedown.prevent
          @click="insertMdFormat('heading')"
        >
          <span class="text-sm font-bold">H</span>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="引用块"
          @mousedown.prevent
          @click="insertMdFormat('quote')"
        >
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/></svg>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="行内代码"
          @mousedown.prevent
          @click="insertMdFormat('code')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/></svg>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="代码块"
          @mousedown.prevent
          @click="insertMdFormat('codeblock')"
        >
          <span class="text-xs font-mono font-bold">{ }</span>
        </button>
        <span class="w-px h-5 bg-outline-variant mx-1"></span>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="无序列表"
          @mousedown.prevent
          @click="insertMdFormat('ul')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 6h16M4 12h16M4 18h16"/><circle cx="2" cy="6" r="1" fill="currentColor"/><circle cx="2" cy="12" r="1" fill="currentColor"/><circle cx="2" cy="18" r="1" fill="currentColor"/></svg>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="有序列表"
          @mousedown.prevent
          @click="insertMdFormat('ol')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M7 6h13M7 12h13M7 18h13"/><text x="2" y="7.5" font-size="7" fill="currentColor" font-weight="bold">1</text><text x="2" y="13.5" font-size="7" fill="currentColor" font-weight="bold">2</text><text x="2" y="19.5" font-size="7" fill="currentColor" font-weight="bold">3</text></svg>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="分割线"
          @mousedown.prevent
          @click="insertMdFormat('hr')"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 12h14"/></svg>
        </button>
        <span class="w-px h-5 bg-outline-variant mx-1"></span>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="插入链接"
          @mousedown.prevent="saveSelection"
          @click="openLinkDialog"
        >
          <span class="material-symbols-outlined text-[18px]">link</span>
        </button>
        <button
          class="inline-flex items-center justify-center w-8 h-8 text-on-surface-variant hover:text-primary hover:bg-primary/10 rounded transition-colors"
          title="插入图片"
          @mousedown.prevent="saveSelection"
          @click="triggerImgUpload"
        >
          <span class="material-symbols-outlined text-[18px]">image</span>
        </button>
        <input ref="imgInput" type="file" accept="image/jpeg,image/png,image/gif,image/bmp,image/webp" hidden @change="handleImgUpload">
        <!-- 右侧：预览切换 -->
        <div class="flex-1"></div>
        <button
          class="inline-flex items-center gap-1 px-2.5 py-1 text-[11.5px] rounded-md transition-colors"
          :class="showPreview ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:text-primary hover:bg-primary/10'"
          title="预览 Markdown"
          @click="togglePreview"
        >
          <span class="material-symbols-outlined text-[18px]">visibility</span>
          <span>预览</span>
        </button>
      </div>

      <!-- 链接弹窗（点击周围不会关闭，避免误触丢失编辑） -->
      <div v-if="showLinkDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/20 p-4">
        <div class="bg-container rounded-xl border border-border shadow-xl p-5 w-80">
          <h4 class="text-[14px] font-semibold text-on-surface mb-4">插入链接</h4>
          <div class="space-y-2.5">
            <input
              v-model="linkText"
              class="ui-input"
              placeholder="链接文本"
              @keyup.enter="$refs.linkUrlInput.focus()"
            >
            <input
              ref="linkUrlInput"
              v-model="linkUrl"
              class="ui-input"
              placeholder="链接地址"
              @keyup.enter="confirmLink"
            >
          </div>
          <div class="flex justify-end gap-2 mt-4">
            <button class="px-3 py-1.5 text-[12px] text-on-surface-variant hover:text-on-surface" @click="closeLinkDialog">取消</button>
            <button
              class="px-4 py-1.5 text-[12px] rounded-lg transition-all text-white bg-primary hover:opacity-90"
              @click="confirmLink"
            >确定</button>
          </div>
        </div>
      </div>

      <!-- 正文编辑器（Markdown 源码）+ 渲染预览 -->
      <div class="flex flex-col md:flex-row border border-t-0 border-outline-variant rounded-b-xl min-h-[500px] overflow-hidden bg-container">
        <textarea
          ref="mdInput"
          v-model="markdownContent"
          class="editor-textarea w-full p-4 font-mono text-[13px] text-on-surface leading-relaxed outline-none resize-none bg-container"
          :class="showPreview ? 'min-h-[320px] md:min-h-[500px] md:flex-1 min-w-0 border-b md:border-b-0 md:border-r border-outline-variant' : 'min-h-[500px]'"
          placeholder="请输入正文内容，支持 Markdown 语法（**加粗**、### 标题、- 列表）"
          spellcheck="false"
          @keydown="handleEditorKeydown"
        ></textarea>
        <!-- Markdown 预览面板（复用正文渲染方式） -->
        <div v-if="showPreview" class="w-full md:flex-1 min-w-0 p-4 overflow-y-auto bg-container min-h-[240px]">
          <section class="markdown-body" v-if="renderedHtml" v-html="renderedHtml"></section>
          <section v-else class="text-outline text-[12px] text-center mt-20">暂无内容可预览</section>
        </div>
      </div>

      <!-- Tags Section -->
      <div class="mt-6">
        <div class="flex items-center gap-1.5 flex-wrap">
          <span class="text-[12px] font-semibold text-on-surface mr-1 flex items-center gap-1">
            <span class="material-symbols-outlined text-[15px] text-primary">sell</span>标签
          </span>
          <button
            v-for="label in labelList"
            :key="label.labelId"
            class="px-2.5 py-1 rounded-full border text-[11.5px] transition-colors"
            :class="String(selectedLabelId) === String(label.labelId) ? 'border-primary/40 bg-primary/10 text-primary' : 'border-outline-variant text-on-surface-variant hover:border-primary/40 hover:text-primary'"
            @click="selectedLabelId = label.labelId; isAnonymous = false"
          >
            {{ label.labelName }}
          </button>
        </div>
        <label v-if="selectedLabelAllowsAnonymous" class="flex items-center gap-2 mt-2.5 text-[11.5px] text-on-surface-variant cursor-pointer select-none">
          <input v-model="isAnonymous" type="checkbox" class="w-3.5 h-3.5 text-primary border-outline-variant rounded">
          匿名发布（该标签允许匿名；互动是否计分由标签策略决定）
        </label>
      </div>

      <!-- Attachment Section -->
      <div class="mt-6">
        <div class="flex items-center justify-between gap-3 mb-3 flex-wrap">
          <h4 class="text-[12.5px] font-semibold text-on-surface flex items-center gap-1">
            <span class="material-symbols-outlined text-[15px] text-primary">attach_file</span>附件上传
            <span class="font-normal text-outline">（最多 5 个）</span>
          </h4>
          <span class="text-[10.5px] text-outline">不支持 .exe / .bat / .sh，单文件不超过 50MB</span>
        </div>
        <button
          class="ui-btn ui-btn-xs bg-primary/10 text-primary hover:bg-primary/20"
          @click="triggerAttachment"
        >
          <span class="material-symbols-outlined text-[16px] mr-1">add</span>
          添加附件
        </button>
        <input ref="attachmentInput" class="hidden" multiple type="file" @change="handleAttachmentAdd">

        <!-- Attachment List -->
        <div class="mt-3 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
          <div
            v-for="(file, index) in attachments"
            :key="index"
            class="flex items-center p-2.5 bg-container border border-outline-variant rounded-lg group hover:border-primary/40 transition-colors"
          >
            <div class="w-8 h-8 bg-primary/10 flex items-center justify-center rounded-md text-primary mr-2.5 shrink-0">
              <span class="material-symbols-outlined text-[17px]">description</span>
            </div>
            <div class="flex-grow min-w-0">
              <p class="text-[11.5px] font-medium text-on-surface truncate">{{ file.fileName || file.name }}</p>
              <p class="text-[10.5px] text-outline">{{ file.size }}</p>
            </div>
            <button class="ui-icon-btn is-danger opacity-0 group-hover:opacity-100" @click="removeAttachment(index)">
              <span class="material-symbols-outlined">delete</span>
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- Bottom Action Bar -->
    <div class="bottom-bar fixed bottom-0 left-0 right-0 bg-container border-t border-border py-3 px-4 sm:px-8 flex justify-center gap-2.5 z-40">
      <button class="ui-btn ui-btn-sm ui-btn-primary px-5" @click="handlePublish" :disabled="publishing || !canPublish">
        {{ publishing ? (isEditing ? '保存中...' : '发布中...') : (canPublish ? (isEditing ? '保存修改' : '发布') : (isEditing ? '无编辑权限' : '无发帖权限')) }}
      </button>
      <button class="ui-btn ui-btn-sm ui-btn-secondary px-5" @click="$router.back()">
        取消
      </button>
    </div>
  </div>
</template>

<script>
import { Message, Loading } from 'element-ui'
import { getArticleForEdit, getArticleFileByArticleId } from '@/api/article'
import { mdToHtml } from '@/utils/markdown'
import { handleResponse } from '../../../shared/feedback'
import userStore from '@/utils/userStore'

// 复用正文 markdown 渲染样式（与 BBSArticleDetails 一致）
import 'mavon-editor/dist/markdown/github-markdown.min.css'

// 行内 Markdown 标记：有选区时包裹选区，无选区时插入占位符并选中占位符
const INLINE_MD = {
  bold: { before: '**', after: '**', placeholder: '粗体文字' },
  italic: { before: '*', after: '*', placeholder: '斜体文字' },
  strikethrough: { before: '~~', after: '~~', placeholder: '删除线文字' },
  code: { before: '`', after: '`', placeholder: '代码' },
}

export default {
  name: 'BBSArticleWrite',
  data() {
    return {
      articleId: this.$route.query.articleId || null,
      articleTitle: '',
      markdownContent: '',
      articleSummary: '',
      selectedLabelId: null,
      isAnonymous: false,
      labelList: [],
      // Attachments: { fileId, fileName } after server upload
      attachments: [],
      showLinkDialog: false,
      linkText: '',
      linkUrl: '',
      uploading: false,
      publishing: false,
      // textarea 光标/选区（{ start, end } 纯数字偏移），点工具栏或插图前保存
      savedSelection: null,
      showPreview: true,
    }
  },
  computed: {
    isEditing() {
      return !!this.articleId
    },
    canPublish() {
      // 编辑已有帖子走 post:update:own，新发帖走 post:create
      return userStore.hasPermission(this.isEditing ? 'post:update:own' : 'post:create')
    },
    selectedLabelName() {
      const found = this.labelList.find(l => String(l.labelId) === String(this.selectedLabelId))
      return found ? found.labelName : ''
    },
    selectedLabelAllowsAnonymous() {
      const found = this.labelList.find(l => String(l.labelId) === String(this.selectedLabelId))
      return !!(found && found.allowAnonymous === 1)
    },
    selectedLabelDescription() {
      const found = this.labelList.find(l => String(l.labelId) === String(this.selectedLabelId))
      return found ? (found.description || '') : ''
    },
    renderedHtml() {
      return mdToHtml(this.markdownContent)
    },
  },
  mounted() {
    this.checkPostRestriction()
    this.loadLabels()
    this.loadPreviewDefault()
    if (this.articleId) {
      this.loadArticleForEdit(this.articleId)
    }
  },
  methods: {
    togglePreview() {
      this.showPreview = !this.showPreview
    },
    /**
     * 预览默认开关由后台系统配置决定：bbs_system_config.editor_preview_default（config_group=editor）。
     * 0=默认关闭，其余/缺失=默认打开——配置拿不到时回落到打开，避免用户又只看到一堆符号。
     */
    loadPreviewDefault() {
      this.postRequest('/common/systemConfig/listByGroup', { configGroup: 'editor' }).then(resp => {
        const list = resp && Array.isArray(resp.obj) ? resp.obj : []
        const item = list.find(c => c.configKey === 'editor_preview_default')
        const raw = item && item.configValue != null ? String(item.configValue).trim() : ''
        this.showPreview = raw !== '0'
      }).catch(() => { this.showPreview = true })
    },
    handleEditorKeydown(e) {
      if (!(e.ctrlKey || e.metaKey) || e.altKey) return
      const key = (e.key || '').toLowerCase()
      if (key === 'b') { e.preventDefault(); this.insertMdFormat('bold') }
      else if (key === 'i') { e.preventDefault(); this.insertMdFormat('italic') }
    },
    /** 当前 textarea 光标/选区（未聚焦时回落到文末，避免往第一行插格式） */
    cursorRange() {
      const el = this.$refs.mdInput
      const value = this.markdownContent || ''
      if (!el || document.activeElement !== el) return { start: value.length, end: value.length }
      return {
        start: el.selectionStart == null ? value.length : el.selectionStart,
        end: el.selectionEnd == null ? value.length : el.selectionEnd,
      }
    },
    /**
     * 用 text 替换 [start, end)，并把选区放到插入内容内的相对位置。
     * textarea 的偏移是纯数字，不会再出现 contenteditable 里"选中占位符后半截"的问题。
     */
    replaceRange(start, end, text, selStartOffset, selEndOffset) {
      const value = this.markdownContent || ''
      this.markdownContent = value.slice(0, start) + text + value.slice(end)
      const caretStart = start + (selStartOffset == null ? text.length : selStartOffset)
      const caretEnd = start + (selEndOffset == null ? text.length : selEndOffset)
      this.$nextTick(() => {
        const el = this.$refs.mdInput
        if (!el) return
        el.focus()
        el.setSelectionRange(caretStart, caretEnd)
      })
    },
    insertMdFormat(type) {
      const value = this.markdownContent || ''
      const { start, end } = this.cursorRange()

      const inline = INLINE_MD[type]
      if (inline) {
        const selected = value.slice(start, end)
        const text = selected || inline.placeholder
        this.replaceRange(
          start, end,
          inline.before + text + inline.after,
          inline.before.length,
          inline.before.length + text.length
        )
        return
      }

      if (type === 'heading') this.toggleHeadingLine(value, start)
      else if (type === 'quote' || type === 'ul' || type === 'ol') this.toggleLinePrefix(type, value, start, end)
      else if (type === 'hr') this.insertHorizontalRule(value, start, end)
      else if (type === 'codeblock') this.insertCodeBlock(value, start, end)
    },
    /** 光标/选区覆盖的整行范围 */
    lineRangeOf(value, start, end) {
      const lineStart = value.lastIndexOf('\n', Math.max(0, start - 1)) + 1
      const nl = value.indexOf('\n', end)
      return { lineStart, lineEnd: nl === -1 ? value.length : nl }
    },
    /** 标题：把光标所在行在 "### " 与无前缀之间切换（多行选区只作用于首行） */
    toggleHeadingLine(value, start) {
      const lineStart = value.lastIndexOf('\n', Math.max(0, start - 1)) + 1
      const nl = value.indexOf('\n', start)
      const lineEnd = nl === -1 ? value.length : nl
      const line = value.slice(lineStart, lineEnd)
      const next = /^#{1,6}\s+/.test(line) ? line.replace(/^#{1,6}\s+/, '') : '### ' + line
      this.replaceRange(lineStart, lineEnd, next, next.length, next.length)
    },
    /** 引用 / 无序 / 有序列表：按整行切换，多行选区逐行处理 */
    toggleLinePrefix(type, value, start, end) {
      const { lineStart, lineEnd } = this.lineRangeOf(value, start, end)
      const lines = value.slice(lineStart, lineEnd).split('\n')
      const pattern = type === 'quote' ? /^\s*>\s?/ : type === 'ul' ? /^\s*[-*+]\s+/ : /^\s*\d+\.\s+/
      const nonEmpty = lines.filter(l => l.trim())
      const allPrefixed = nonEmpty.length > 0 && nonEmpty.every(l => pattern.test(l))
      let seq = 1
      const next = lines.map(line => {
        const body = line.replace(pattern, '')
        if (!line.trim() || allPrefixed) return body
        if (type === 'quote') return '> ' + body
        if (type === 'ul') return '- ' + body
        return `${seq++}. ` + body
      }).join('\n')
      this.replaceRange(lineStart, lineEnd, next, next.length, next.length)
    },
    insertHorizontalRule(value, start, end) {
      const lead = start === 0 || value[start - 1] === '\n' ? '' : '\n\n'
      const tail = end >= value.length || value[end] === '\n' ? '\n' : '\n\n'
      const insertion = lead + '---' + tail
      this.replaceRange(start, end, insertion, insertion.length, insertion.length)
    },
    insertCodeBlock(value, start, end) {
      const text = value.slice(start, end) || '代码块'
      const insertion = '```\n' + text + '\n```'
      this.replaceRange(start, end, insertion, 4, 4 + text.length)
    },
    checkPostRestriction() {
      const userStr = window.sessionStorage.getItem('user')
      if (!userStr) {
        this.$alert('请先登录后再发帖', '提示', {
          confirmButtonText: '确定',
          type: 'warning',
          showClose: false,
        }).then(() => { this.$router.back() })
        return
      }
      const userId = JSON.parse(userStr).id
      this.getRequest('/common/user/checkPostRestriction?userId=' + userId).then(resp => {
        if (resp && resp.restricted) {
          const msg = resp.until
            ? '您的发帖权限已被限制，截止时间：' + resp.until
            : '您的发帖权限已被永久限制，如有疑问请联系管理员'
          this.$alert(msg, '发帖受限', {
            confirmButtonText: '确定',
            type: 'error',
            showClose: false,
          }).then(() => { this.$router.back() })
        }
      }).catch(() => {})
    },
    autoResizeTitle() {
      const el = this.$refs.titleInput
      if (el) {
        el.style.height = 'auto'
        el.style.height = el.scrollHeight + 'px'
      }
    },
    loadLabels() {
      this.getRequest('/common/getArticleLabel').then(resp => {
        if (resp && Array.isArray(resp)) {
          this.labelList = resp
          if (resp.length > 0) {
            this.selectedLabelId = resp[0].labelId
          }
        }
      }).catch(err => {
        console.warn('[BBSArticleWrite] loadLabels', err)
        this.labelList = []
      })
    },
    loadArticleForEdit(id) {
      getArticleForEdit(id).then(resp => {
        // 接口返回 ResultBean，文章在 obj 里；兼容直接返回实体的老形态
        const data = resp && resp.obj ? resp.obj : resp
        if (data) {
          this.articleTitle = data.articleTitle || ''
          this.markdownContent = data.articleContent || ''
          this.articleSummary = data.articleSummary || ''
          this.selectedLabelId = data.articleLabelId
          this.isAnonymous = data.isAnonymous === 1 || data.isAnonymous === '1'
          // Load attachment files
          this.loadArticleFiles(id)
          this.$nextTick(() => this.autoResizeTitle())
        }
      }).catch(err => {
        console.warn('[BBSArticleWrite] loadArticleForEdit', err)
      })
    },
    loadArticleFiles(id) {
      getArticleFileByArticleId(id).then(resp => {
        if (resp) {
          const list = Array.isArray(resp) ? resp : (resp.data || [])
          this.attachments = list.map(item => ({
            fileId: item.fileId != null ? item.fileId : item.id,
            fileName: item.fileName || item.name || '附件',
            size: '',
          }))
        }
      }).catch(err => {
        console.warn('[BBSArticleWrite] loadArticleFiles', err)
        this.attachments = []
      })
    },
    triggerAttachment() {
      this.$refs.attachmentInput.click()
    },
    handleAttachmentAdd(e) {
      const files = e.target.files
      const userStr = window.sessionStorage.getItem('user')
      if (!userStr) {
        Message({ message: '请先登录', type: 'warning', showClose: true, offset: 54 })
        return
      }
      const userId = JSON.parse(userStr).id
      const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
      const allowed = Array.from(files).filter(f => {
        const ext = f.name.split('.').pop().toLowerCase()
        if (['exe', 'bat', 'sh', 'cmd', 'com', 'scr', 'msi', 'vbs', 'ps1'].includes(ext)) return false
        if (f.size > MAX_FILE_SIZE) {
          Message({ message: `"${f.name}" 超过 50MB 大小限制`, type: 'warning', showClose: true, offset: 54 })
          return false
        }
        return true
      })
      const remaining = 5 - this.attachments.length
      const toUpload = allowed.slice(0, remaining)
      toUpload.forEach(file => {
        const formData = new FormData()
        formData.append('userId', userId)
        formData.append('file', file)
        this.postRequest('/articleFile/upload', formData).then(resp => {
          if (resp && resp.code === 200 && resp.obj && resp.obj.fileId != null) {
            this.attachments.push({ fileId: resp.obj.fileId, fileName: file.name, size: (file.size / (1024 * 1024)).toFixed(1) + ' MB' })
          } else {
            Message({ message: (resp && resp.message) || '上传失败', type: 'error', showClose: true, offset: 54 })
          }
        }).catch(err => { console.warn('[BBSArticleWrite] uploadAttachment', err) })
      })
      this.$refs.attachmentInput.value = ''
    },
    removeAttachment(index) {
      this.attachments.splice(index, 1)
    },
    // --- 插入图片 / 链接 / 选区记忆（全部基于 textarea 偏移） ---
    saveSelection() {
      const el = this.$refs.mdInput
      if (!el) { this.savedSelection = null; return }
      this.savedSelection = {
        start: el.selectionStart == null ? 0 : el.selectionStart,
        end: el.selectionEnd == null ? 0 : el.selectionEnd,
      }
    },
    /** 优先用点按钮前记住的选区，否则用当前光标 */
    savedRangeOrCursor() {
      const value = this.markdownContent || ''
      const saved = this.savedSelection
      if (saved) {
        const start = Math.min(Math.max(saved.start, 0), value.length)
        const end = Math.min(Math.max(saved.end, start), value.length)
        return { start, end }
      }
      return this.cursorRange()
    },
    insertImageMarkdown(url) {
      const value = this.markdownContent || ''
      const { start, end } = this.savedRangeOrCursor()
      this.savedSelection = null
      // 图片独立成段，避免和前后正文粘连
      const lead = start === 0 || value[start - 1] === '\n' ? '' : '\n\n'
      const tail = end >= value.length || value[end] === '\n' ? '\n' : '\n\n'
      const insertion = lead + `![图片](${url})` + tail
      this.replaceRange(start, end, insertion, insertion.length, insertion.length)
    },
    insertLinkMarkdown(text, url) {
      const { start, end } = this.savedRangeOrCursor()
      this.savedSelection = null
      const insertion = `[${text}](${url})`
      this.replaceRange(start, end, insertion, insertion.length, insertion.length)
    },
    triggerImgUpload() {
      this.$refs.imgInput.click()
    },
    handleImgUpload(e) {
      const file = e.target.files[0]
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      this.postRequest('/common/upload', formData).then(res => {
        // 后端上传成功返回字符串路径（/bbs-server/files/...），失败时同样返回字符串错误信息（HTTP 200），需区分
        const isPath = typeof res === 'string' && (res.startsWith('/') || /^https?:\/\//i.test(res))
        if (isPath) {
          this.insertImageMarkdown(res)
        } else {
          const msg = typeof res === 'string' && res ? res : '图片上传失败，请稍后重试'
          Message({ message: msg, type: 'error', showClose: true, offset: 54 })
        }
        this.$refs.imgInput.value = ''
      }).catch(err => {
        console.warn('[BBSArticleWrite] imgUpload', err)
        this.$refs.imgInput.value = ''
        // 注意：api.js 拦截器已对网络/HTTP 错误统一弹窗，这里不重复提示
      })
    },
    // --- 链接插入 ---
    openLinkDialog() {
      const value = this.markdownContent || ''
      const { start, end } = this.savedRangeOrCursor()
      this.savedSelection = { start, end }
      this.linkText = start < end ? value.slice(start, end) : ''
      this.linkUrl = ''
      this.showLinkDialog = true
      this.$nextTick(() => {
        this.$refs.linkUrlInput && this.$refs.linkUrlInput.focus()
      })
    },
    closeLinkDialog() {
      this.showLinkDialog = false
      this.linkText = ''
      this.linkUrl = ''
      this.savedSelection = null
    },
    confirmLink() {
      const text = this.linkText.trim() || '链接'
      let url = this.linkUrl.trim()
      if (!url) {
        Message({ message: '请输入链接地址', type: 'warning', showClose: true, offset: 54 })
        return
      }
      if (/^(javascript|data|vbscript):/i.test(url)) {
        Message({ message: '链接地址不合法，请使用 http(s) 地址', type: 'warning', showClose: true, offset: 54 })
        return
      }
      // 用户常直接粘贴 "www.xxx.com"，补全协议，避免被当成站内相对路径
      if (!/^([a-z][a-z0-9+.-]*:|\/|#)/i.test(url)) url = 'https://' + url
      this.insertLinkMarkdown(text, url)
      this.showLinkDialog = false
      this.linkText = ''
      this.linkUrl = ''
    },
    // --- 自动提取封面 & 摘要 ---
    extractFirstImage(markdown) {
      const regex = /!\[.*?\]\((.*?)\)/
      const match = markdown.match(regex)
      return match ? match[1] : ''
    },
    extractSummary(markdown) {
      const plainText = markdown
        .replace(/!\[.*?\]\(.*?\)/g, '')      // 图片
        .replace(/\[([^\]]*)\]\(.*?\)/g, '$1') // 链接
        .replace(/[#*`>~+-]/g, '')             // 标记符号
        .replace(/\n{2,}/g, '\n')              // 多余空行
        .trim()
      const lines = plainText.split('\n').filter(l => l.trim())
      return lines.slice(0, 2).join('\n').substring(0, 250)
    },
    handlePublish() {
      if (this.publishing) return
      if (!this.articleTitle.trim()) {
        Message({ message: '标题不能为空', type: 'warning', showClose: true, offset: 54 })
        return
      }
      if (this.articleTitle.length > 30) {
        Message({ message: '标题最多30个字', type: 'warning', showClose: true, offset: 54 })
        return
      }
      // 标题不允许包含图片或富文本（HTML 标签、markdown 图片语法）
      if (/<[^>]*>|!\[[^\]]*\]\([^)]*\)/.test(this.articleTitle)) {
        Message({ message: '标题不允许包含图片或富文本内容', type: 'warning', showClose: true, offset: 54 })
        return
      }
      if (!this.markdownContent.trim()) {
        Message({ message: '内容不能为空', type: 'warning', showClose: true, offset: 54 })
        return
      }

      const userStr = window.sessionStorage.getItem('user')
      if (!userStr) {
        Message({ message: '请先登录', type: 'warning', showClose: true, offset: 54 })
        return
      }
      const user = JSON.parse(userStr)

      // 自动提取封面和摘要
      const articleImage = this.extractFirstImage(this.markdownContent)
      const articleSummary = this.extractSummary(this.markdownContent)

      const article = {
        articleTitle: this.articleTitle,
        articleContent: this.markdownContent,
        articleContentHtml: this.markdownContent,
        articleSummary,
        articleImage,
        articleTypeId: 0,
        articleLabelId: isNaN(parseInt(this.selectedLabelId)) ? 0 : parseInt(this.selectedLabelId),
        isAnonymous: this.isAnonymous ? 1 : 0,
        userId: user.id,
        articleAuthor: user.nickname,
        files: this.attachments.map(a => a.fileId),
      }

      if (this.articleId) {
        article.articleId = this.articleId
      }

      this.publishing = true
      const loading = Loading.service({ lock: true, text: '发布中，请稍后...' })

      const endpoint = this.articleId ? '/article/editArticle' : '/article/publish'
      this.postRequest(endpoint, article).then(resp => {
        loading.close()
        this.publishing = false
        // 成功静默跳转（未传 successMsg 不弹 Toast）；失败弹出后端 message（不再静默吞掉，2026-09 生产故障）
        handleResponse(resp, { onSuccess: () => this.$router.push({ path: '/userinfo', query: { tab: 'posts' } }) })
      }).catch(err => {
        console.warn('[BBSArticleWrite] publish', err)
        loading.close()
        this.publishing = false
      })
    },
  },
}
</script>

<style scoped>
/* 去掉所有输入框蓝色焦点框 */
.title-textarea:focus,
textarea:focus {
  outline: none !important;
  box-shadow: none !important;
}
</style>

<!-- 非 scoped：预览面板渲染的是 v-html 内容，scoped 样式覆盖不到 -->
<style>
/* Markdown 预览样式 —— 覆盖 Tailwind Preflight 对 p/h1-h6/ul/ol/blockquote/pre/hr 的重置 */
.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.4;
}
.markdown-body h1 { font-size: 2em; }
.markdown-body h2 { font-size: 1.5em; }
.markdown-body h3 { font-size: 1.25em; }
.markdown-body p {
  margin-top: 0;
  margin-bottom: 16px;
  line-height: 1.8;
  color: #434654;
}
.markdown-body ul,
.markdown-body ol {
  padding-left: 2em;
  margin-top: 0;
  margin-bottom: 16px;
}
.markdown-body ul { list-style: disc; }
.markdown-body ol { list-style: decimal; }
.markdown-body li {
  margin-bottom: 4px;
  line-height: 1.8;
}
.markdown-body blockquote {
  margin: 0 0 16px 0;
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
}
.markdown-body pre {
  margin-top: 0;
  margin-bottom: 16px;
  padding: 16px;
  overflow-x: auto;
  background-color: #f6f8fa;
  border-radius: 6px;
  line-height: 1.5;
}
.markdown-body code {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 85%;
  background-color: rgba(27,31,35,0.05);
  padding: 0.2em 0.4em;
  border-radius: 3px;
}
.markdown-body pre code {
  background: none;
  padding: 0;
  font-size: 100%;
}
.markdown-body img {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
  height: auto;
}
.markdown-body a {
  color: #0366d6;
  text-decoration: none;
}
.markdown-body a:hover {
  text-decoration: underline;
}
.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}
.markdown-body table th,
.markdown-body table td {
  border: 1px solid #dfe2e5;
  padding: 6px 13px;
}
.markdown-body table th {
  background-color: #f6f8fa;
  font-weight: 600;
}
.markdown-body hr {
  border: 0;
  border-top: 1px solid #e1e4e8;
  margin: 24px 0;
}
</style>
