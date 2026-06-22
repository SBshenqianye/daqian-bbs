<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">rate_review</span>
            帖子管理
          </h1>
          <p class="text-body-md text-secondary mt-1">审核和管理所有用户发布的帖子</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="bg-container border border-border rounded-xl overflow-hidden mb-6">
        <div class="flex border-b border-border">
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative" :class="activeTab === 'done' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'" @click="activeTab = 'done'">
            已审核
            <span class="ml-2 px-3 py-[3px] text-xs font-medium rounded-full" :class="activeTab === 'done' ? 'bg-primary text-on-primary' : 'bg-surface-variant text-on-surface-variant'">{{ done.length }}</span>
            <span v-if="activeTab === 'done'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative" :class="activeTab === 'pending' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'" @click="activeTab = 'pending'">
            未审核
            <span class="ml-2 px-3 py-[3px] text-xs font-medium rounded-full" :class="activeTab === 'pending' ? 'bg-primary text-on-primary' : 'bg-surface-variant text-on-surface-variant'">{{ notDone.length }}</span>
            <span v-if="activeTab === 'pending'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
        </div>

        <!-- Audited Articles -->
        <div v-if="activeTab === 'done'" class="p-card-padding">
          <div v-if="done.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">check_circle</span>
            <p class="text-body-md">暂无已审核文章</p>
          </div>
          <div v-else class="space-y-3">
            <div v-for="article in done" :key="article.articleId" class="flex items-center justify-between p-4 bg-surface-container-low rounded-lg border border-outline-variant/50 hover:border-primary/30 transition-all group">
              <div class="flex-1 min-w-0">
                <span class="font-headline-sm text-headline-sm text-primary cursor-pointer hover:underline" @click="handleRead(article.articleId)">《{{ article.articleTitle }}》</span>
                <span class="text-body-md text-on-surface-variant ml-3">{{ article.createTime }}</span>
              </div>
              <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDel(article.articleId)">
                <span class="material-symbols-outlined text-[14px]">delete</span>
                删除
              </button>
            </div>
          </div>
          <div class="mt-6 pt-4 border-t border-border">
            <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-error text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleBatchDeleteArticlesByAlive">
              <span class="material-symbols-outlined text-[18px]">delete_sweep</span>
              删除全部文章
            </button>
          </div>
        </div>

        <!-- Pending Audit Articles -->
        <div v-if="activeTab === 'pending'" class="p-card-padding">
          <div v-if="notDone.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">pending_actions</span>
            <p class="text-body-md">暂无待审核文章</p>
          </div>
          <div v-else class="space-y-3">
            <div v-for="article in notDone" :key="article.articleId" class="flex items-center justify-between p-4 bg-surface-container-low rounded-lg border border-outline-variant/50 hover:border-primary/30 transition-all group">
              <div class="flex-1 min-w-0">
                <span class="font-headline-sm text-headline-sm text-primary cursor-pointer hover:underline" @click="handleRead(article.articleId)">《{{ article.articleTitle }}》</span>
                <span class="text-body-md text-on-surface-variant ml-3">{{ article.createTime }}</span>
              </div>
              <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="handleAudit(article.articleId)">
                <span class="material-symbols-outlined text-[14px]">check</span>
                通过审核
              </button>
            </div>
          </div>
          <div class="mt-6 pt-4 border-t border-border">
            <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleBatchAudit">
              <span class="material-symbols-outlined text-[18px]">fact_check</span>
              全部通过审核
            </button>
          </div>
        </div>
      </div>

      <!-- Article Detail Dialog -->
      <!-- Backdrop (fixed, stays in place) -->
      <div v-if="detailVisible" class="fixed inset-0 bg-black/30 z-40" @click="closeDetail"></div>
      <!-- Dialog wrapper (scrollable, like el-dialog) -->
      <div v-if="detailVisible" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="bg-container w-full max-w-4xl mx-auto my-[5vh] rounded-xl shadow-2xl">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">article</span>
              帖子详情
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="closeDetail">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5" v-loading="detailLoading">
            <div v-if="detailTitle" class="mb-4">
              <h2 class="font-headline-md text-headline-md text-on-surface">标题：《{{ detailTitle }}》</h2>
            </div>
            <div class="markdown-body detail-content" v-html="renderedContent"></div>
            <div v-if="detailFileList && detailFileList.length > 0" class="mt-6 bg-surface-container-low rounded-lg p-4">
              <h4 class="font-headline-sm text-headline-sm text-on-surface mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-[20px]">attach_file</span>
                附件列表
              </h4>
              <div class="space-y-2">
                <div v-for="(file, index) in detailFileList" :key="index" class="flex items-center justify-between p-3 bg-container rounded border border-outline-variant/50">
                  <span class="font-body-md text-on-surface flex items-center gap-2">
                    <span class="material-symbols-outlined text-outline text-[18px]">description</span>
                    {{ file.fileName }}
                  </span>
                  <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="downloadFile(file.filePath, file.fileName)">
                    <span class="material-symbols-outlined text-[14px]">download</span>
                    下载
                  </button>
                </div>
              </div>
            </div>
            <div class="mt-6">
              <h4 class="font-headline-sm text-headline-sm text-on-surface mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-[20px]">comment</span>
                评论（{{ detailCommentCount }}）
              </h4>
              <div v-if="detailCommentCount > 0" class="space-y-4">
                <div v-for="(item, index) in detailComments" :key="item.commentId || index" class="bg-surface-container-low rounded-lg p-4 border border-outline-variant/50">
                  <div class="flex items-center gap-3 mb-2">
                    <img class="w-9 h-9 rounded-full bg-surface-variant object-cover" :src="getAvatarUrl(item.portrait)" alt="">
                    <div>
                      <span class="font-headline-sm text-headline-sm text-on-surface">{{ item.nickname || '未知用户' }}</span>
                      <span class="text-body-md text-on-surface-variant ml-2">{{ item.commentTime }}</span>
                    </div>
                  </div>
                  <p class="text-body-md text-on-surface ml-12">{{ item.commentContent }}</p>
                  <div v-if="item.reply && item.reply.length" class="ml-12 mt-3 pl-4 border-l-2 border-outline-variant/30 space-y-3">
                    <div v-for="(reply, rIdx) in item.reply" :key="reply.replyId || rIdx" class="bg-surface-container rounded-lg p-3">
                      <div class="flex items-center gap-2 mb-1.5">
                        <img class="w-7 h-7 rounded-full bg-surface-variant object-cover" :src="getAvatarUrl(reply.portrait)" alt="">
                        <span class="font-headline-sm text-headline-sm text-on-surface text-[13px]">{{ reply.nickname || '未知用户' }}</span>
                        <span class="text-body-md text-on-surface-variant text-[12px]">{{ reply.replyTime }}</span>
                      </div>
                      <p class="text-body-md text-on-surface-variant ml-9">
                        <span v-if="reply.replyToNickname" class="text-primary">回复 {{ reply.replyToNickname }}：</span>
                        {{ reply.replyContent }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="text-center py-8 text-on-surface-variant flex flex-col items-center gap-2">
                <span class="material-symbols-outlined text-[36px] opacity-30">chat</span>
                <p class="text-body-md">暂无评论</p>
              </div>
            </div>
          </div>
          <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-6 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="closeDetail">关 闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it/dist/markdown-it'
import 'mavon-editor/dist/markdown/github-markdown.min.css'

export default {
  name: 'ArticlePage',
  data() {
    return {
      activeTab: 'done',
      done: [],
      notDone: [],
      detailVisible: false,
      detailLoading: false,
      detailArticleId: null,
      detailTitle: '',
      detailContent: '',
      detailComments: [],
      detailFileList: [],
      avatarBase: process.env.VUE_APP_BBS_API || ''
    }
  },
  computed: {
    detailCommentCount() {
      if (!this.detailComments.length) return 0
      return this.detailComments.reduce((sum, item) => {
        return sum + 1 + (item.reply && item.reply.length ? item.reply.length : 0)
      }, 0)
    },
    renderedContent() {
      if (!this.detailContent) return ''
      return this._renderMarkdown(this.detailContent)
    }
  },
  mounted() {
    this.getAliveArticles()
    this.getNotAliveArticles()
  },
  methods: {
    getAvatarUrl(portrait) {
      if (!portrait) return require('../assets/img/img.jpeg')
      const path = portrait.startsWith('/') ? portrait : `/${portrait}`
      return `${this.avatarBase}${path}`
    },
    handleRead(articleId) {
      this.detailArticleId = articleId
      this.detailVisible = true
      this.$nextTick(() => {
        this.loadArticleDetail(articleId)
      })
    },
    closeDetail() {
      this.detailVisible = false
      this.detailArticleId = null
      this.detailTitle = ''
      this.detailContent = ''
      this.detailComments = []
      this.detailFileList = []
    },
    loadArticleDetail(articleId) {
      this.detailLoading = true
      this.getRequest('/admin/getArticleByArticleId', articleId).then(resp => {
        this.detailLoading = false
        if (resp) {
          this.detailContent = resp.obj.articleContent
          this.detailTitle = resp.obj.articleTitle
          this.getArticleFileByArticleId(articleId)
          this.getCommentByArticleId(articleId)
        }
      }).catch(() => { this.detailLoading = false })
    },
    getArticleFileByArticleId(articleId) {
      this.postRequest(`/common/getArticleFileByArticleId/${articleId}`, {}).then(res => {
        let list = []
        if (Array.isArray(res)) list = res
        else if (res && Array.isArray(res.obj)) list = res.obj
        else if (res && Array.isArray(res.listBean)) list = res.listBean
        this.detailFileList = list
      }).catch(() => { this.detailFileList = [] })
    },
    getCommentByArticleId(articleId) {
      this.postRequest(`/common/comment/getCommentReply/${articleId}`).then(res => {
        this.detailComments = (res && Array.isArray(res)) ? res : []
      }).catch(() => { this.detailComments = [] })
    },
    downloadFile(filePath, fileName) {
      if (!filePath) return
      const base = process.env.VUE_APP_BBS_BASE_API || ''
      const url = base.endsWith('/') ? base + filePath.replace(/^\//, '') : (filePath.startsWith('/') ? base + filePath : base + '/' + filePath)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName || ''
      a.style.display = 'none'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    },
    _renderMarkdown(content) {
      if (!this._md) {
        this._md = new MarkdownIt({
          html: true,
          xhtmlOut: true,
          breaks: true,
          linkify: false,
          typographer: true
        })
      }
      return this._md.render(content)
    },
    handleDel(articleId) {
      this.$confirm('删除文章会连评论一并删除，确定要删除该文章吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/deleteArticleByArticleId', { articleId }).then(resp => {
          if (resp) {
            this.$message.success('删除成功！')
            this.getAliveArticles()
            this.getNotAliveArticles()
          }
        })
      }).catch(() => {})
    },
    handleAudit(articleId) {
      this.$confirm('确定此篇文章通过审核吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/auditArticleByArticleId', { articleId }).then(resp => {
          if (resp) {
            this.$message.success('审核通过！')
            this.getAliveArticles()
            this.getNotAliveArticles()
          }
        })
      }).catch(() => {})
    },
    handleBatchDeleteArticlesByAlive() {
      this.$confirm('确定要删除该文章吗？', '提示', { type: 'error' }).then(() => {
        this.postRequest('/admin/handleBatchDeleteArticlesByAlive/all', {}).then(resp => {
          if (resp) { this.getNotAliveArticles(); this.getAliveArticles() }
        })
      }).catch(() => {})
    },
    handleBatchAudit() {
      this.$confirm('确定全部通过审核吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/batchAudit/', {}).then(resp => {
          if (resp) {
            this.$message.success('全部审核通过！')
            this.getAliveArticles()
            this.getNotAliveArticles()
          }
        })
      }).catch(() => {})
    },
    getAliveArticles() {
      this.getRequest('/admin/getAliveArticles', 'all').then(resp => {
        if (resp) this.done = resp.obj
      })
    },
    getNotAliveArticles() {
      this.getRequest('/admin/getNotAliveArticles', 'all').then(resp => {
        if (resp) this.notDone = resp.obj
      })
    }
  }
}
</script>

<style>
/* 弹窗内渲染的 Markdown 内容样式 */
.detail-content {
  padding: 8px 0;
  background: transparent !important;
}
.detail-content h1,
.detail-content h2,
.detail-content h3,
.detail-content h4,
.detail-content h5,
.detail-content h6 {
  margin-top: 16px;
  margin-bottom: 8px;
}
.detail-content p {
  margin-bottom: 8px;
  line-height: 1.7;
}
</style>
