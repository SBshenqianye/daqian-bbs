<template>
  <div class="min-h-screen flex flex-col bg-surface">
    <!-- Main Content Area -->
    <main class="flex-grow flex flex-col pt-10 pb-28 px-4 sm:px-10 max-w-7xl mx-auto w-full">
      <!-- Title Input Section -->
      <div class="mb-8 relative flex items-center group">
        <textarea
          ref="titleInput"
          v-model="articleTitle"
          class="title-textarea w-full bg-transparent border-b border-transparent hover:border-gray-200 focus:border-transparent focus:ring-0 text-4xl font-bold text-gray-800 placeholder-gray-300 p-0 pb-2 resize-none overflow-hidden transition-all duration-200"
          placeholder="请输入文章标题"
          rows="1"
          @input="autoResizeTitle"
        ></textarea>
        <span class="material-symbols-outlined absolute right-0 text-gray-300 pointer-events-none group-hover:text-gray-400">edit</span>
      </div>

      <!-- 操作栏：链接/图片 -->
      <div class="flex items-center gap-1 px-3 py-2 border border-gray-200 rounded-t-lg bg-gray-50/50">
        <button
          class="inline-flex items-center gap-1 px-2.5 py-1 text-sm text-gray-600 hover:text-primary hover:bg-blue-50 rounded transition-colors"
          title="插入链接"
          @click="openLinkDialog"
        >
          <span class="material-symbols-outlined text-[18px]">link</span>
        </button>
        <button
          class="inline-flex items-center gap-1 px-2.5 py-1 text-sm text-gray-600 hover:text-primary hover:bg-blue-50 rounded transition-colors"
          title="插入图片"
          @mousedown="saveSelection"
          @click="triggerImgUpload"
        >
          <span class="material-symbols-outlined text-[18px]">image</span>
        </button>
        <input ref="imgInput" type="file" accept="image/jpeg,image/png,image/gif,image/bmp,image/webp" hidden @change="handleImgUpload">
      </div>

      <!-- 链接弹窗（点击周围不会关闭，避免误触丢失编辑） -->
      <div v-if="showLinkDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/20">
        <div class="bg-white rounded-lg shadow-xl p-5 w-80">
          <h4 class="text-sm font-semibold text-gray-700 mb-4">{{ editingLink ? '编辑链接' : '插入链接' }}</h4>
          <div class="space-y-3">
            <input
              v-model="linkText"
              class="w-full px-3 py-2 border border-gray-200 rounded text-sm focus:border-primary focus:ring-1 focus:ring-primary/20 outline-none"
              placeholder="链接文本"
              @keyup.enter="$refs.linkUrlInput.focus()"
            >
            <input
              ref="linkUrlInput"
              v-model="linkUrl"
              class="w-full px-3 py-2 border border-gray-200 rounded text-sm focus:border-primary focus:ring-1 focus:ring-primary/20 outline-none"
              placeholder="链接地址"
              @keyup.enter="confirmLink"
            >
          </div>
          <div class="flex justify-end gap-2 mt-4">
            <button class="px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700" @click="closeLinkDialog">取消</button>
            <button
              class="px-4 py-1.5 text-sm rounded transition-all"
              :class="editingLink ? 'bg-orange-500 text-white hover:opacity-90' : 'bg-primary text-white hover:opacity-90'"
              @click="confirmLink"
            >{{ editingLink ? '保存修改' : '确定' }}</button>
          </div>
        </div>
      </div>

      <!-- 可视化编辑器 (contenteditable) -->
      <div
        ref="editorDiv"
        contenteditable="true"
        class="editor-content w-full border border-t-0 border-gray-200 rounded-b-lg p-4 text-sm text-gray-800 leading-normal outline-none transition-colors min-h-[500px] focus:border-gray-300"
        @blur="syncContent"
        @paste="handlePaste"
        @click="handleEditorClick"
      ></div>

      <!-- Tags Section -->
      <div class="mt-8">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-sm font-semibold text-gray-700 mr-1">标签：</span>
          <button
            v-for="label in labelList"
            :key="label.labelId"
            class="px-3 py-1 rounded-full border text-sm transition-colors"
            :class="String(selectedLabelId) === String(label.labelId) ? 'border-primary bg-blue-50 text-primary' : 'border-gray-200 text-gray-500 hover:border-primary hover:text-primary'"
            @click="selectedLabelId = label.labelId"
          >
            {{ label.labelName }}
          </button>
        </div>
      </div>

      <!-- Attachment Section -->
      <div class="mt-8">
        <div class="flex items-center justify-between mb-4">
          <h4 class="text-sm font-semibold text-gray-700">附件上传 (最多5个)</h4>
          <span class="text-xs text-gray-400">不允许上传 .exe, .bat, .sh 格式，单文件不超过 50MB</span>
        </div>
        <button
          class="inline-flex items-center px-4 py-2 bg-primary text-white text-sm font-medium rounded-lg hover:opacity-90 transition-colors shadow-sm"
          @click="triggerAttachment"
        >
          <span class="material-symbols-outlined text-lg mr-2">add</span>
          添加附件
        </button>
        <input ref="attachmentInput" class="hidden" multiple type="file" @change="handleAttachmentAdd">

        <!-- Attachment List -->
        <div class="mt-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          <div
            v-for="(file, index) in attachments"
            :key="index"
            class="flex items-center p-3 bg-white border border-gray-200 rounded-lg group hover:border-blue-200 transition-colors"
          >
            <div class="w-10 h-10 bg-blue-50 flex items-center justify-center rounded text-blue-500 mr-3">
              <span class="material-symbols-outlined">description</span>
            </div>
            <div class="flex-grow min-w-0">
              <p class="text-sm font-medium text-gray-700 truncate">{{ file.fileName || file.name }}</p>
              <p class="text-xs text-gray-400">{{ file.size }}</p>
            </div>
            <button class="p-1 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity" @click="removeAttachment(index)">
              <span class="material-symbols-outlined text-lg">delete</span>
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- Bottom Action Bar -->
    <div class="bottom-bar fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 py-4 px-10 flex justify-center space-x-4 z-40">
      <button class="px-10 py-2.5 bg-primary text-white font-medium rounded-full hover:opacity-90 transition-all shadow-md active:scale-95" @click="handlePublish" :disabled="publishing">
        {{ publishing ? '发布中...' : '发布' }}
      </button>
      <button class="px-10 py-2.5 font-medium rounded-full hover:bg-gray-200 transition-all active:scale-95 bg-red-500 text-white" @click="$router.back()">
        取消
      </button>
    </div>
  </div>
</template>

<script>
import { Message, Loading } from 'element-ui'
import { getArticleById, getArticleFileByArticleId } from '@/api/article'
import { mdToHtml, htmlToMd } from '@/utils/markdown'

export default {
  name: 'BBSArticleWrite',
  data() {
    return {
      articleId: this.$route.query.articleId || null,
      articleTitle: '',
      markdownContent: '',
      articleSummary: '',
      selectedLabelId: null,
      labelList: [],
      // Attachments: { fileId, fileName } after server upload
      attachments: [],
      showLinkDialog: false,
      editingLink: false,  // true=编辑已有链接, false=新建链接
      linkText: '',
      linkUrl: '',
      uploading: false,
      publishing: false,
      syncingContent: false,
      savedRange: null,
    }
  },
  computed: {
    selectedLabelName() {
      const found = this.labelList.find(l => String(l.labelId) === String(this.selectedLabelId))
      return found ? found.labelName : ''
    },
    selectedLabelDescription() {
      const found = this.labelList.find(l => String(l.labelId) === String(this.selectedLabelId))
      return found ? (found.description || '') : ''
    },
  },
  watch: {
    markdownContent(val) {
      // 外部修改（加载文章时）同步到编辑区
      if (!this.syncingContent && this.$refs.editorDiv) {
        const html = this.mdToHtml(val)
        if (html !== this.$refs.editorDiv.innerHTML) {
          this.$refs.editorDiv.innerHTML = html
        }
      }
    },
  },
  mounted() {
    this.loadLabels()
    if (this.articleId) {
      this.loadArticleForEdit(this.articleId)
    } else {
      // 新文章：初始化空内容
      this.$nextTick(() => { if (this.$refs.editorDiv) this.$refs.editorDiv.innerHTML = '' })
    }
  },
  methods: {
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
      getArticleById(id).then(resp => {
        if (resp) {
          this.articleTitle = resp.articleTitle || ''
          this.markdownContent = resp.articleContent || ''
          this.articleSummary = resp.articleSummary || ''
          this.selectedLabelId = resp.articleLabelId
          // Load attachment files
          this.loadArticleFiles(id)
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
    // --- Markdown ⇄ HTML 转换（共用函数在 utils/markdown.js） ---
    mdToHtml(md) { return mdToHtml(md) },
    htmlToMd(html) { return htmlToMd(html) },
    syncContent() {
      // 编辑区内容同步回 markdownContent
      if (!this.$refs.editorDiv || this.syncingContent) return
      this.syncingContent = true
      this.markdownContent = this.htmlToMd(this.$refs.editorDiv.innerHTML)
      this.$nextTick(() => { this.syncingContent = false })
    },
    insertHtml(html) {
      // 在光标处插入 HTML
      const sel = window.getSelection()
      if (!sel || !sel.rangeCount) return
      const range = sel.getRangeAt(0)
      range.deleteContents()
      const frag = range.createContextualFragment(html)
      range.insertNode(frag)
      // 光标移到插入内容末尾
      range.setStartAfter(frag)
      range.collapse(true)
      sel.removeAllRanges()
      sel.addRange(range)
      this.syncContent()
    },
    handlePaste(e) {
      // 粘贴时阻止带格式粘贴，只保留纯文本
      e.preventDefault()
      const text = (e.clipboardData || window.clipboardData).getData('text/plain')
      document.execCommand('insertText', false, text)
      this.syncContent()
    },
    handleEditorClick(e) {
      // 点击编辑区内的超链接直接打开编辑弹窗
      let node = e.target
      while (node && node !== this.$refs.editorDiv) {
        if (node.nodeName === 'A') {
          e.preventDefault()
          // 将光标移到链接内
          const sel = window.getSelection()
          if (sel) {
            const range = document.createRange()
            range.selectNodeContents(node)
            sel.removeAllRanges()
            sel.addRange(range)
          }
          this.openLinkDialog()
          return
        }
        node = node.parentNode
      }
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
        const url = typeof res === 'string' ? res : (res && res.url ? res.url : '')
        if (url) {
          this.restoreSelection()
          this.insertHtml(`<img src="${url}" alt="图片" style="max-width:100%;display:block;margin:8px 0">`)
        }
      }).catch(err => { console.warn('[BBSArticleWrite] imgUpload', err) })
      this.$refs.imgInput.value = ''
    },
    // --- 链接插入/编辑 ---
    openLinkDialog() {
      // 打开弹窗前保存光标位置、检测是否在已有链接上
      this.saveSelection()

      // 检测光标是否在已有 <a> 上
      const sel = window.getSelection()
      if (sel && sel.rangeCount) {
        let node = sel.getRangeAt(0).startContainer
        while (node && node !== this.$refs.editorDiv) {
          if (node.nodeName === 'A') {
            this.linkText = node.textContent || ''
            this.linkUrl = node.getAttribute('href') || ''
            this.editingLink = true
            this.showLinkDialog = true
            this.$nextTick(() => {
              this.$refs.linkUrlInput && this.$refs.linkUrlInput.focus()
            })
            return
          }
          node = node.parentNode
        }
      }
      // 新建链接
      this.linkText = ''
      this.linkUrl = ''
      this.editingLink = false
      this.showLinkDialog = true
      this.$nextTick(() => {
        this.$refs.linkUrlInput && this.$refs.linkUrlInput.focus()
      })
    },
    closeLinkDialog() {
      this.showLinkDialog = false
      this.linkText = ''
      this.linkUrl = ''
      this.editingLink = false
    },
    confirmLink() {
      const text = this.linkText.trim() || '链接'
      const url = this.linkUrl.trim()
      if (!url) {
        Message({ message: '请输入链接地址', type: 'warning', showClose: true, offset: 54 })
        return
      }

      // 恢复光标（弹窗打开前保存的位置）
      this.restoreSelection()

      try {
        const sel = window.getSelection()

        if (this.editingLink && sel && sel.rangeCount) {
          // 编辑已有链接：从光标向上找 <a> 并更新
          let node = sel.getRangeAt(0).startContainer
          while (node && node.nodeName !== 'A' && node !== this.$refs.editorDiv) {
            node = node.parentNode
          }
          if (node && node.nodeName === 'A') {
            node.setAttribute('href', url)
            node.textContent = text
            this.syncContent()
          }
        } else if (sel && sel.rangeCount) {
          // 新建链接：插入到光标处
          this.insertHtml(`<a href="${url}">${text}</a>`)
        }
      } catch (e) {
        console.warn('[BBSArticleWrite] confirmLink error', e)
      } finally {
        this.closeLinkDialog()
      }
    },
    saveSelection() {
      const sel = window.getSelection()
      if (sel && sel.rangeCount) {
        this.savedRange = sel.getRangeAt(0)
      }
    },
    restoreSelection() {
      if (this.savedRange) {
        try {
          const sel = window.getSelection()
          sel.removeAllRanges()
          sel.addRange(this.savedRange)
        } catch (e) {
          // range 可能已失效，忽略
        }
        this.savedRange = null
      }
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
        .replace(/[#*`>~\-\+]/g, '')           // 标记符号
        .replace(/\n{2,}/g, '\n')              // 多余空行
        .trim()
      const lines = plainText.split('\n').filter(l => l.trim())
      return lines.slice(0, 2).join('\n').substring(0, 250)
    },
    handlePublish() {
      if (this.publishing) return
      // 先把编辑区内容同步过来
      this.syncContent()
      if (!this.articleTitle.trim()) {
        Message({ message: '标题不能为空', type: 'warning', showClose: true, offset: 54 })
        return
      }
      if (this.articleTitle.length > 30) {
        Message({ message: '标题最多30个字', type: 'warning', showClose: true, offset: 54 })
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
        articleCommunityId: 0,
        articleLabelId: isNaN(parseInt(this.selectedLabelId)) ? 0 : parseInt(this.selectedLabelId),
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
        if (resp) {
          this.$router.push('/stat')
        }
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

<!-- 非 scoped：编辑区内动态插入的 <a> 不受 scoped 影响，需要全局样式 -->
<style>
.editor-content a {
  color: #2563eb !important;
  text-decoration: underline !important;
  font-weight: 500;
  cursor: pointer;
}
.editor-content a:hover {
  color: #1d4ed8 !important;
  background-color: rgba(37, 99, 235, 0.06);
}
</style>
