<template>
  <main class="max-w-5xl mx-auto px-page-margin-mobile md:px-page-margin-desktop py-8 bg-surface min-h-screen">
    <!-- Article Header Card -->
    <article class="bg-container rounded-lg border border-border shadow-sm overflow-hidden mb-gutter">
      <div class="p-card-padding">
        <!-- Title & Meta -->
        <header class="mb-6">
          <h1 class="font-headline-lg text-headline-lg text-on-surface mb-4 leading-tight">
            {{ article.title }}
          </h1>
          <div class="flex flex-wrap items-center justify-between gap-4 text-on-surface-variant font-label-md text-label-md">
            <div class="flex items-center space-x-6">
              <span class="flex items-center gap-1.5">
                <span class="material-symbols-outlined text-[16px]">schedule</span>
                发布时间: {{ article.publishTime }}
              </span>
              <div class="flex items-center gap-2">
                <span>文章标签:</span>
                <span class="px-2 py-0.5 bg-surface-container-low text-primary-container rounded border border-outline-variant font-medium">{{ articleTagName || '--' }}</span>
              </div>
            </div>
            <div class="flex items-center gap-3 py-1 px-3 bg-surface-container-lowest border border-border rounded-lg">
              <div class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-white overflow-hidden shadow-inner">
                <img alt="Author" class="w-full h-full object-cover" :src="article.authorAvatar">
              </div>
              <div class="flex flex-col">
                <span class="font-bold text-on-surface">{{ article.author }}</span>
                <span class="text-[10px] text-outline">{{ article.authorTitle }}</span>
              </div>
            </div>
          </div>
        </header>
        <hr class="border-border mb-8">

        <!-- Article Content -->
        <section class="markdown-body" v-if="mdContent">
          <mavon-editor
            ref="previewMd"
            v-model="mdContent"
            v-bind="previewEditor"
            class="me-preview-editor"
          />
        </section>
        <section class="markdown-body font-body-lg text-body-lg text-on-surface" v-else>
          <p class="text-on-surface-variant">暂无内容</p>
        </section>
      </div>
    </article>

    <!-- Attachments Section -->
    <section v-if="article.attachments && article.attachments.length > 0" class="bg-container rounded-lg border border-border shadow-sm p-card-padding mb-gutter">
      <div class="flex items-center gap-2 mb-4">
        <span class="material-symbols-outlined text-primary-container">attachment</span>
        <h3 class="font-headline-sm text-headline-sm text-on-surface">附件 ({{ article.attachments.length }})</h3>
      </div>
      <div class="space-y-3">
        <div
          v-for="(att, i) in article.attachments"
          :key="i"
          class="flex items-center justify-between p-3 bg-surface rounded-lg border border-outline-variant hover:border-primary-container transition-primary group"
        >
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-outline group-hover:text-primary-container">description</span>
            <span class="font-body-md text-body-md text-on-surface">{{ att.name }}</span>
          </div>
          <button class="flex items-center gap-2 bg-primary-container text-white px-4 py-1.5 rounded text-label-md font-label-md hover:bg-primary transition-primary" @click="downloadFile(att.filePath, att.name)">
            <span class="material-symbols-outlined text-[18px]">download</span>
            下载
          </button>
        </div>
      </div>
    </section>

    <!-- Comment Section -->
    <section class="bg-container rounded-lg border border-border shadow-sm overflow-hidden">
      <div class="p-card-padding">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            评论 <span class="px-2 py-0.5 bg-surface-container-high rounded-full text-label-md">{{ totalCommentCount }}</span>
          </h3>
        </div>

        <!-- Comment Input -->
        <div class="flex gap-4 mb-10">
          <div class="w-10 h-10 rounded-full bg-surface-variant flex-shrink-0 overflow-hidden border border-outline-variant">
            <img alt="Current User" :src="currentUserAvatar || require('@/assets/portrait.png')">
          </div>
          <div class="flex-grow space-y-3">
            <textarea
              v-model="newComment"
              class="w-full p-3 rounded-lg border border-border focus:border-primary-container focus:ring-1 focus:ring-primary-container min-h-[100px] font-body-md text-body-md bg-surface transition-primary resize-none"
              placeholder="请输入评论内容..."
            ></textarea>
            <div class="flex justify-end">
              <button
                class="bg-primary-container text-white px-6 py-2 rounded-lg font-label-md text-label-md hover:shadow-md hover:bg-primary transition-primary active:scale-95"
                @click="submitComment"
              >
                发表评论
              </button>
            </div>
          </div>
        </div>

        <!-- Comments List -->
        <div class="space-y-8">
          <BBSCommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            @delete="handleDeleteComment"
            @reply="handleReply"
          />
        </div>
      </div>
    </section>
  </main>
</template>

<script>
import BBSCommentItem from '@/components/BBSCommentItem.vue'
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
import { getArticleById, getUserinfoById, getArticleFileByArticleId } from '@/api/article'
import { getCommentReply } from '@/api/comment'
import { normalizeUrls } from '@/utils/utils'
import { Message } from 'element-ui'

export default {
  name: 'BBSArticleDetails',
  components: { BBSCommentItem, mavonEditor },
  data() {
    return {
      newComment: '',
      articleId: this.$route.params.articleId,
      loading: true,
      apiBase: process.env.VUE_APP_BBS_API || '',
      fileBase: process.env.VUE_APP_BBS_BASE_FILE || '',
      // Current user
      currentUser: null,
      currentUserAvatar: '',
      // Article data
      article: {
        title: '',
        publishTime: '',
        tag: '',
        tagId: '',
        author: '',
        authorTitle: '',
        authorAvatar: '',
        contentHtml: '',
        articleImage: '',
        contentParagraphs: [],
        attachments: [],
      },
      // Label map
      labelList: [],
      // User info cache
      authorInfo: {},
      // Comment & reply
      comments: [],
      // Markdown content for preview
      mdContent: '',
      // Markdown editor preview config
      previewEditor: {
        toolbarsFlag: false,
        subfield: false,
        defaultOpen: 'preview',
        editable: false,
        scrollStyle: true,
        boxShadow: false,
        ishljs: true,
      },
    }
  },
  computed: {
    totalCommentCount() {
      const countChildren = (list) => {
        let count = 0
        list.forEach(c => {
          count++
          if (c.children && c.children.length) count += countChildren(c.children)
        })
        return count
      }
      return countChildren(this.comments)
    },
    articleTagName() {
      if (!this.article.tagId || !this.labelList.length) return ''
      const label = this.labelList.find(l => String(l.labelId) === String(this.article.tagId))
      return label ? label.labelName : ''
    },
  },
  mounted() {
    this.initCurrentUser()
    this.loadLabels()
    if (this.articleId) {
      this.loadArticle(this.articleId)
      this.loadComments(this.articleId)
      this.loadArticleFiles(this.articleId)
    }
  },
  methods: {
    initCurrentUser() {
      try {
        const u = window.sessionStorage.getItem('user')
        if (u) {
          this.currentUser = JSON.parse(u)
          if (this.currentUser.portrait) {
            this.currentUserAvatar = this.apiBase + this.currentUser.portrait
          }
        }
      } catch (e) { /* ignore */ }
    },
    loadLabels() {
      this.getRequest('/common/getArticleLabel').then(resp => {
        if (resp && Array.isArray(resp)) {
          this.labelList = resp
        }
      }).catch(() => {})
    },
    loadArticle(id) {
      this.loading = true
      getArticleById(id).then(resp => {
        this.loading = false
        if (resp) {
          this.article.title = resp.articleTitle || ''
          this.article.publishTime = resp.createTime || ''
          this.article.tagId = resp.articleLabelId
          this.article.author = resp.articleAuthor || ''
          const rawContent = resp.articleContentHtml || resp.articleContent || ''
          this.article.contentHtml = normalizeUrls(rawContent)
          this.mdContent = normalizeUrls(rawContent)
          this.article.articleImage = resp.articleImage ? this.fileBase + resp.articleImage : ''
          // Fetch author info
          if (resp.userId) {
            this.loadAuthorInfo(resp.userId)
          }
        }
      }).catch(() => {
        this.loading = false
      })
    },
    loadAuthorInfo(userId) {
      getUserinfoById(userId).then(resp => {
        if (resp) {
          this.authorInfo = resp
          if (resp.portrait) {
            this.article.authorAvatar = this.apiBase + resp.portrait
          }
          this.article.authorTitle = resp.title || ''
        }
      }).catch(() => {})
    },
    loadComments(articleId) {
      getCommentReply(articleId).then(resp => {
        if (resp && Array.isArray(resp)) {
          this.comments = resp.map(c => this.mapComment(c))
        } else {
          this.comments = []
        }
      }).catch(() => {
        this.comments = []
      })
    },
    // Map API comment to BBSCommentItem props format
    mapComment(c) {
      const myId = this.currentUser ? this.currentUser.id : null
      const commentId = c.commentId || c.id
      const mapped = {
        id: commentId,
        commentRootId: commentId,
        userId: c.userId,
        author: c.nickname || '',
        avatar: c.portrait ? this.apiBase + c.portrait : '',
        time: c.commentTime || '',
        content: c.commentContent || '',
        canDelete: myId != null && String(c.userId) === String(myId),
        children: (c.reply || []).map(r => ({
          id: r.replyId || r.id,
          commentRootId: commentId,
          userId: r.replyUserId || r.userId,
          replyTargetUserId: r.replyToUserId,
          author: r.nickname || '',
          avatar: r.portrait ? this.apiBase + r.portrait : '',
          time: r.replyTime || '',
          content: r.replyContent || '',
          replyTo: r.replyToNickname || '',
          canDelete: myId != null && String(r.replyUserId || r.userId) === String(myId),
          children: [],
        })),
      }
      if (mapped.userId == null) {
        console.warn('[mapComment] comment without userId:', JSON.parse(JSON.stringify(c)))
      }
      return mapped
    },
    loadArticleFiles(articleId) {
      getArticleFileByArticleId(articleId).then(resp => {
        if (resp) {
          const list = Array.isArray(resp) ? resp : (resp.data || [])
          this.article.attachments = list.map(f => ({
            name: f.fileName || f.name || '附件',
            filePath: f.filePath || '',
            fileId: f.fileId || f.id,
          }))
        }
      }).catch(() => {
        this.article.attachments = []
      })
    },
    submitComment() {
      if (!this.newComment.trim()) return
      if (!this.currentUser) {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      const tempData = {
        commentContent: this.newComment,
        commentUserId: this.currentUser.id,
        commentArticleId: this.articleId,
      }
      this.putRequest('/comment/userComment', tempData).then(resp => {
        if (resp) {
          this.newComment = ''
          this.loadComments(this.articleId)
        }
      }).catch(() => {
        Message({ type: 'error', message: '评论失败', offset: 54 })
      })
    },
    handleReply({ commentId, replyContent, replyToUserId }) {
      if (!this.currentUser) return
      const params = {
        replyUserId: this.currentUser.id,
        replyContent,
        commentId,
      }
      if (replyToUserId != null) {
        params.replyToUserId = replyToUserId
      }
      this.putRequest('/reply/userReply', params).then(resp => {
        if (resp) {
          Message({ type: 'success', message: '回复成功', offset: 54 })
          this.loadComments(this.articleId)
        } else {
          Message({ type: 'error', message: '回复失败', offset: 54 })
        }
      }).catch(() => {
        Message({ type: 'error', message: '回复失败', offset: 54 })
      })
    },
    handleDeleteComment(comment) {
      const commentId = comment.id || comment
      const isReply = commentId !== comment.commentRootId
      this.$confirm('确定删除该' + (isReply ? '回复' : '评论') + '？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        if (isReply) {
          this.postRequest('/reply/deleteReplyById', { replyId: commentId }).then(resp => {
            if (resp) {
              Message({ type: 'success', message: '删除成功', offset: 54 })
              this.loadComments(this.articleId)
            }
          }).catch(() => {
            Message({ type: 'error', message: '删除失败', offset: 54 })
          })
        } else {
          this.postRequest('/comment/deleteCommentById', { commentId }).then(resp => {
            if (resp) {
              Message({ type: 'success', message: '删除成功', offset: 54 })
              this.loadComments(this.articleId)
            }
          }).catch(() => {
            Message({ type: 'error', message: '删除失败', offset: 54 })
          })
        }
      }).catch(() => {})
    },
    downloadFile(filePath, fileName) {
      if (!filePath) return
      const url = this.fileBase + (filePath.startsWith('/') ? filePath : '/' + filePath)
      fetch(url, { method: 'GET', credentials: 'include' })
        .then(resp => {
          if (!resp.ok) throw new Error('下载失败')
          return resp.blob()
        })
        .then(blob => {
          const blobUrl = URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = blobUrl
          link.setAttribute('download', fileName || 'download')
          link.style.display = 'none'
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          URL.revokeObjectURL(blobUrl)
        })
        .catch(() => {
          Message({ message: '下载失败，请稍后重试', type: 'warning', showClose: true, offset: 54 })
        })
    },
  },
}
</script>

<style scoped>
.me-preview-editor {
  min-height: 100px !important;
  z-index: 1 !important;
}
.me-preview-editor .v-note-op {
  display: none !important;
}
.me-preview-editor .v-note-panel {
  border: none !important;
  box-shadow: none !important;
}
.me-preview-editor .v-note-wrapper {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  z-index: 1;
}
</style>
