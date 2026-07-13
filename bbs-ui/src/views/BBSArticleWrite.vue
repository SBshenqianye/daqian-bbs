<template>
  <div class="min-h-screen flex flex-col bg-surface">
    <!-- Main Content Area -->
    <main class="flex-grow flex flex-col pt-10 pb-28 px-4 sm:px-10 max-w-7xl mx-auto w-full">
      <!-- Title Input Section -->
      <div class="mb-8 relative flex items-center group">
        <textarea
          ref="titleInput"
          v-model="articleTitle"
          class="w-full bg-transparent border-b border-transparent hover:border-gray-200 focus:border-primary focus:ring-0 text-4xl font-bold text-gray-800 placeholder-gray-300 p-0 pb-2 resize-none overflow-hidden transition-all duration-200"
          placeholder="请输入文章标题"
          rows="1"
          @input="autoResizeTitle"
        ></textarea>
        <span class="material-symbols-outlined absolute right-0 text-gray-300 pointer-events-none group-hover:text-gray-400">edit</span>
      </div>

      <!-- Markdown Editor (using mavon-editor) -->
      <div class="bg-white border border-gray-200 rounded-lg flex flex-col flex-grow shadow-sm overflow-hidden">
        <mavon-editor
          ref="mdEditor"
          v-model="markdownContent"
          :toolbars="toolbars"
          :subfield="true"
          :default-open="'edit'"
          :placeholder="'从这里开始你的创作...'"
          :box-shadow="false"
          :ishljs="true"
          :externalLink="localExternalLink"
          style="min-height: 500px; position: relative; z-index: 1;"
          @imgAdd="handleImgAdd"
        />
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
      <button class="px-10 py-2.5 bg-primary text-white font-medium rounded-full hover:opacity-90 transition-all shadow-md active:scale-95" @click="showPublishModal = true">
        发布文章
      </button>
      <button class="px-10 py-2.5 font-medium rounded-full hover:bg-gray-200 transition-all active:scale-95 bg-red-500 text-white" @click="$router.back()">
        取消发布
      </button>
    </div>

    <!-- Publish Modal -->
    <div v-if="showPublishModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showPublishModal = false">
      <div class="modal-overlay absolute inset-0"></div>
      <div class="bg-white rounded-lg shadow-2xl w-full max-w-2xl relative z-10 overflow-hidden">
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-100 flex justify-between items-center">
          <h3 class="text-lg font-semibold text-gray-800">发布文章</h3>
          <button class="text-gray-400 hover:text-gray-600 p-1" @click="showPublishModal = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-6 space-y-6">
          <div class="flex flex-col md:flex-row gap-6">
            <!-- Left: Cover Upload -->
            <div class="w-full md:w-1/3">
              <label class="block text-sm font-medium text-gray-700 mb-2">文章封面</label>
              <div
                class="aspect-[4/3] w-full bg-gray-50 border-2 border-dashed border-gray-200 rounded-lg flex flex-col items-center justify-center text-gray-400 cursor-pointer hover:border-primary hover:bg-blue-50/50 transition-all group"
                @click="triggerCoverUpload"
              >
                <span v-if="!coverPreview" class="material-symbols-outlined text-3xl group-hover:text-primary transition-colors">add</span>
                <span v-if="!coverPreview" class="text-xs mt-2">上传封面图</span>
                <img v-if="coverPreview" :src="coverPreview" class="w-full h-full object-cover rounded-lg">
              </div>
              <input ref="coverInput" accept="image/*" class="hidden" type="file" @change="handleCoverChange">
            </div>
            <!-- Right: Summary -->
            <div class="flex-grow">
              <label class="block text-sm font-medium text-gray-700 mb-2">文章摘要</label>
              <div class="relative">
                <textarea
                  v-model="articleSummary"
                  class="w-full border-gray-200 rounded-lg focus:ring-primary/20 focus:border-primary text-sm p-3 placeholder-gray-300 resize-none"
                  maxlength="250"
                  placeholder="请输入摘要，吸引更多读者..."
                  rows="5"
                ></textarea>
                <span class="absolute bottom-2 right-2 text-xs text-gray-400">{{ articleSummary.length }}/250</span>
              </div>
            </div>
          </div>
          <!-- Tags -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-3">选择标签</label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="label in labelList"
                :key="label.labelId"
                class="px-4 py-1.5 rounded-full border text-sm transition-colors"
                :class="String(selectedLabelId) === String(label.labelId) ? 'border-primary bg-blue-50 text-primary' : 'border-gray-200 text-gray-600 hover:border-primary hover:text-primary hover:bg-blue-50'"
                @click="selectedLabelId = label.labelId"
              >
                {{ label.labelName }}
              </button>
            </div>
            <p v-if="selectedLabelDescription" class="mt-1.5 text-xs text-gray-400 ml-0.5">
              {{ selectedLabelDescription }}
            </p>
          </div>
        </div>
        <!-- Footer -->
        <div class="px-6 py-4 bg-gray-50 flex justify-end gap-3">
          <button class="px-6 py-2 text-sm font-medium text-gray-600 hover:text-gray-800 transition-colors" @click="showPublishModal = false">取消</button>
          <button class="px-8 py-2 text-white text-sm font-bold rounded-lg hover:opacity-90 transition-all shadow-sm bg-red-500" @click="handlePublish">确认发布</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
import 'mavon-editor/dist/markdown/github-markdown.min.css'
import { Message, Loading } from 'element-ui'
import { getArticleById, getArticleFileByArticleId } from '@/api/article'
import { normalizeFileUrl, normalizeUrls } from '@/utils/utils'

export default {
  name: 'BBSArticleWrite',
  components: { mavonEditor },
  data() {
    return {
      // 离线环境：mavon-editor 外链钩子指向本地 /public/lib/ 资源
      localExternalLink: {
        hljs_js: () => process.env.BASE_URL + 'lib/highlight/highlight.min.js',
        hljs_css: (css) => process.env.BASE_URL + `lib/highlight/styles/${css}.min.css`,
        hljs_lang: (lang) => process.env.BASE_URL + `lib/highlight/languages/${lang}.min.js`,
        markdown_css: false,  // 已通过 import 本地导入
        katex_js: () => process.env.BASE_URL + 'lib/katex/katex.min.js',
        katex_css: () => process.env.BASE_URL + 'lib/katex/katex.min.css',
      },
      articleId: this.$route.query.articleId || null,
      articleTitle: '',
      markdownContent: '',
      articleSummary: '',
      showPublishModal: false,
      coverPreview: null,
      coverFile: null,
      selectedLabelId: null,
      labelList: [],
      // Attachments: { fileId, fileName } after server upload
      attachments: [],
      uploading: false,
      toolbars: {
        bold: true,
        italic: true,
        header: true,
        underline: true,
        strikethrough: true,
        mark: true,
        superscript: true,
        subscript: true,
        quote: true,
        ol: true,
        ul: true,
        link: true,
        imagelink: true,
        code: true,
        table: true,
        fullscreen: true,
        readmodel: true,
        htmlcode: false,
        help: true,
        undo: true,
        redo: true,
        trash: true,
        save: false,
        navigation: true,
        subfield: true,
        preview: true,
      },
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
  mounted() {
    this.loadLabels()
    if (this.articleId) {
      this.loadArticleForEdit(this.articleId)
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
          this.markdownContent = normalizeUrls(resp.articleContent || '')
          this.articleSummary = resp.articleSummary || ''
          this.selectedLabelId = resp.articleLabelId
          if (resp.articleImage) {
            this.coverPreview = normalizeFileUrl(resp.articleImage)
          }
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
    handleImgAdd(pos, file) {
      const formData = new FormData()
      formData.append('file', file)
      this.postRequest('/common/upload', formData).then(res => {
        if (res && res.url) {
          this.$refs.mdEditor.$img2Url(pos, res.url)
        } else if (res && typeof res === 'string') {
          this.$refs.mdEditor.$img2Url(pos, res)
        } else {
          this.$refs.mdEditor.$img2Url(pos, '')
        }
      }).catch(err => {
        console.warn('[BBSArticleWrite] handleImgAdd', err)
        this.$refs.mdEditor.$img2Url(pos, '')
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
    triggerCoverUpload() {
      this.$refs.coverInput.click()
    },
    handleCoverChange(e) {
      const file = e.target.files[0]
      if (file) {
        this.coverFile = file
        const reader = new FileReader()
        reader.onload = (ev) => { this.coverPreview = ev.target.result }
        reader.readAsDataURL(file)
      }
    },
    handlePublish() {
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
      if (!this.articleSummary.trim()) {
        Message({ message: '摘要不能为空', type: 'warning', showClose: true, offset: 54 })
        return
      }

      const userStr = window.sessionStorage.getItem('user')
      if (!userStr) {
        Message({ message: '请先登录', type: 'warning', showClose: true, offset: 54 })
        return
      }
      const user = JSON.parse(userStr)

      const article = {
        articleTitle: this.articleTitle,
        articleContent: this.markdownContent,
        articleContentHtml: this.markdownContent,
        articleSummary: this.articleSummary,
        articleTypeId: 0,
        articleCommunityId: 0,
        articleLabelId: isNaN(parseInt(this.selectedLabelId)) ? 0 : parseInt(this.selectedLabelId),
        userId: user.id,
        articleAuthor: user.nickname,
        articleImage: '',
        files: this.attachments.map(a => a.fileId),
      }

      if (this.articleId) {
        article.articleId = this.articleId
      }

      this.showPublishModal = false
      const loading = Loading.service({ lock: true, text: '发布中，请稍后...' })

      const doPublish = (imageUrl) => {
        if (imageUrl) article.articleImage = imageUrl
        const endpoint = this.articleId ? '/article/editArticle' : '/article/publish'
        this.postRequest(endpoint, article).then(resp => {
          loading.close()
          if (resp) {
            // 成功提示由 axios 响应拦截器（api.js）统一处理
            this.$router.push('/stat')
          }
        }).catch(err => {
          console.warn('[BBSArticleWrite] publish', err)
          loading.close()
        })
      }

      // Upload cover if exists
      if (this.coverFile) {
        const formData = new FormData()
        formData.append('userId', user.id)
        formData.append('image', this.coverFile)
        this.postRequest('/article/coverImg', formData).then(resp => {
          doPublish(normalizeFileUrl(resp && typeof resp === 'string' ? resp : (resp && resp.url || '')))
        }).catch(err => {
          console.warn('[BBSArticleWrite] coverUpload', err)
          doPublish('')
        })
      } else {
        doPublish(this.coverPreview && typeof this.coverPreview === 'string' && !this.coverPreview.startsWith('data:') ? normalizeFileUrl(this.coverPreview) : '')
      }
    },
  },
}
</script>
