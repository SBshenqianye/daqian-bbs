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
                <el-tooltip :content="articleTagDescription" placement="top" effect="dark" :disabled="!articleTagDescription">
                  <span class="px-2 py-0.5 bg-surface-container-low text-primary-container rounded border border-outline-variant font-medium">{{ articleTagName || '--' }}</span>
                </el-tooltip>
              </div>
            </div>
            <div class="flex items-center gap-3 py-1 px-3 bg-surface-container-lowest border border-border rounded-lg">
              <div class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-white overflow-hidden shadow-inner">
                <img alt="Author" class="w-full h-full object-cover" :src="article.authorAvatar || require('@/assets/portrait.png')">
              </div>
              <div class="flex flex-col">
                <span class="font-bold text-on-surface">{{ article.author }}</span>
                <span v-if="article.authorOrgName" class="text-[10px] text-outline" :title="article.authorOrgNameFull || article.authorOrgName">{{ article.authorOrgName }}</span>
                <span v-else class="text-[10px] text-outline">{{ article.authorTitle }}</span>
              </div>
            </div>
          </div>
        </header>
        <hr class="border-border mb-8">

        <!-- Article Content -->
        <section class="markdown-body" v-if="renderedHtml" v-html="renderedHtml"></section>
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
    <section class="bg-container rounded-lg border border-border shadow-sm overflow-hidden" :class="{ 'pb-16': showStickyBar }">
      <div class="p-card-padding">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            评论 <span class="px-2 py-0.5 bg-surface-container-high rounded-full text-label-md">{{ totalCommentCount }}</span>
          </h3>
        </div>

        <!-- Comment Input -->
        <div class="flex items-center gap-4 mb-10">
          <div class="w-10 h-10 rounded-full overflow-hidden bg-surface-variant border border-outline-variant flex-shrink-0">
            <img :src="currentUserAvatar || require('@/assets/portrait.png')" class="w-full h-full object-cover">
          </div>
          <textarea
            ref="commentInput"
            v-model="newComment"
            class="flex-grow min-w-0 py-2 px-4 rounded-lg border border-border focus:border-primary-container focus:ring-1 focus:ring-primary-container bg-surface font-body-md text-body-md transition-primary outline-none resize-none"
            :placeholder="commentPlaceholder"
            rows="1"
            @keydown.enter.exact="submitComment"
            @input="autoResizeCommentInput"
          ></textarea>
          <button
            class="bg-primary-container text-white px-6 py-2 rounded-lg font-label-md text-label-md hover:shadow-md hover:bg-primary transition-primary active:scale-95 flex-shrink-0"
            @click="submitComment"
          >
            发表评论
          </button>
        </div>

        <!-- Comments List -->
        <div class="space-y-6">
          <BBSCommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :currentUserAvatar="currentUserAvatar"
            @delete="handleDeleteComment"
            @reply="handleReply"
            @adopt="handleAdopt"
          />

          <!-- 空状态：全无评论 -->
          <div
            v-if="showCommentEmptyState && totalCommentCount === 0"
            class="flex flex-col items-center justify-center py-16 text-on-surface-variant"
          >
            <span class="material-symbols-outlined text-7xl mb-4 opacity-10">chat_bubble_outline</span>
            <p class="font-headline-sm text-headline-sm mb-2">暂无评论</p>
            <p class="font-body-md text-body-md">快来发表第一条评论吧～</p>
          </div>

          <!-- 空状态：仅有零散评论 -->
          <div
            v-if="showCommentEmptyState && totalCommentCount > 0"
            class="flex flex-col items-center justify-center py-8 text-on-surface-variant border-t border-border mt-6"
          >
            <span class="material-symbols-outlined text-4xl mb-2 opacity-20">chat_bubble_outline</span>
            <p class="font-body-md text-body-md">评论不多，来说点什么吧～</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Sticky Reply Bar -->
    <transition name="reply-bar-slide">
      <div
        v-if="showStickyBar"
        class="bottom-bar fixed bottom-0 left-0 right-0 bg-container border-t border-border z-40"
      >
        <div class="max-w-5xl mx-auto px-page-margin-mobile md:px-page-margin-desktop py-3 flex items-center gap-4">
          <!-- Avatar -->
          <div class="w-10 h-10 rounded-full overflow-hidden bg-surface-variant border border-outline-variant flex-shrink-0">
            <img :src="currentUserAvatar || require('@/assets/portrait.png')" class="w-full h-full object-cover">
          </div>

          <template v-if="currentUser">
            <textarea
              v-model="newComment"
              class="flex-grow min-w-0 py-2 px-4 rounded-lg border border-border focus:border-primary-container focus:ring-1 focus:ring-primary-container bg-surface font-body-md text-body-md transition-primary outline-none resize-none"
              :placeholder="commentPlaceholder"
              rows="1"
              @keydown.enter.exact="submitComment"
              @input="autoResizeCommentInput"
            ></textarea>
            <button
              class="bg-primary-container text-white px-6 py-2 rounded-lg font-label-md text-label-md hover:shadow-md hover:bg-primary transition-primary active:scale-95 flex-shrink-0"
              @click="submitComment"
            >
              发表评论
            </button>
          </template>
          <template v-else>
            <span class="flex-grow text-center text-on-surface-variant font-body-md text-body-md">
              请先
              <router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }" class="text-primary-container hover:underline">
                登录
              </router-link>
              后发表评论
            </span>
          </template>
        </div>
      </div>
    </transition>
  </main>
</template>

<script>
import BBSCommentItem from '@/components/BBSCommentItem.vue'
import { getArticleById, getUserinfoById, getArticleFileByArticleId } from '@/api/article'
import { getCommentReply } from '@/api/comment'
import { normalizeFileUrl, normalizeUrls } from '@/utils/utils'
import { mdToHtml } from '@/utils/markdown'
import { Message } from 'element-ui'

// 保持 github-markdown 样式用于 v-html 渲染的 markdown-body
import 'mavon-editor/dist/markdown/github-markdown.min.css'

// 评论区域空白填充阈值：评论数低于此值时显示空状态，避免头重脚轻
const MIN_COMMENT_FILLER = 3

export default {
  name: 'BBSArticleDetails',
  components: { BBSCommentItem },
  provide() {
    return {
      replyState: this.replyState,
      // 提供父组件实例引用，子组件通过 adoptState.isQuestionLabel / adoptState.currentUserId 始终读到最新值
      // （provide() 只执行一次，直接传原始值会快照为 false；传引用则始终跟随父组件 computed 变化）
      adoptState: this,
    }
  },
  data() {
    return {
      newComment: '',
      articleId: this.$route.params.articleId,
      loading: true,
      // VUE_APP_BBS_API、VUE_APP_BBS_BASE_FILE 废弃，文件统一使用 /files/ 原始路径
      // Current user
      currentUser: null,
      currentUserAvatar: '',
      // Article data
      article: {
        title: '',
        publishTime: '',
        tag: '',
        tagId: '',
        userId: null,
        author: '',
        authorTitle: '',
        authorAvatar: '',
        authorOrgName: '',
        authorOrgNameFull: '',
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
      commentsLoaded: false,
      // Raw markdown content (converted to HTML via computed)
      rawContent: '',
      showStickyBar: false,
      commentInputObserver: null,
      commentPlaceholder: '',
      replyState: { activeId: null },
      scrollToTarget: null,
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
    showCommentEmptyState() {
      return this.commentsLoaded && this.comments.length < MIN_COMMENT_FILLER
    },
    articleTagName() {
      if (!this.article.tagId || !this.labelList.length) return ''
      const label = this.labelList.find(l => String(l.labelId) === String(this.article.tagId))
      return label ? label.labelName : ''
    },
    articleTagDescription() {
      if (!this.article.tagId || !this.labelList.length) return ''
      const label = this.labelList.find(l => String(l.labelId) === String(this.article.tagId))
      return label ? (label.description || '') : ''
    },
    renderedHtml() {
      return mdToHtml(this.rawContent)
    },
    /** 当前用户ID */
    currentUserId() {
      return this.currentUser ? this.currentUser.id : null
    },
    /** 帖子标签是否为"问题求助" */
    isQuestionLabel() {
      return this.articleTagName === '问题求助'
    },
  },
  mounted() {
    this.initCurrentUser()
    this.loadLabels()
    // 读取 deep-link 参数（commentId / replyId）
    const query = this.$route.query
    if (query.commentId || query.replyId) {
      this.scrollToTarget = query.replyId || query.commentId
    }
    if (this.articleId) {
      this.loadArticle(this.articleId)
      this.loadComments(this.articleId)
      this.loadArticleFiles(this.articleId)
    }
    this.$nextTick(() => {
      this.initStickyBar()
    })
    // 随机文明提示语
    const prompts = [
      '文明发言，理性讨论',
      '发表你的看法...',
      '欢迎分享你的观点',
      '请友善评论',
      '说点什么吧...',
      '你的每一条评论都很重要',
      '尊重他人，理性表达',
      '好文值得一赞，也欢迎评论',
      '请不要发布违规内容',
      '聊聊你的想法？',
      '营造良好社区氛围，从评论做起',
      '留下你的真知灼见',
      '你的评论是对作者最大的鼓励',
      '用心评论，传递价值',
      '友好交流，共同进步',
    ]
    this.commentPlaceholder = prompts[Math.floor(Math.random() * prompts.length)]
  },
  methods: {
    initCurrentUser() {
      try {
        const u = window.sessionStorage.getItem('user')
        if (u) {
          this.currentUser = JSON.parse(u)
          if (this.currentUser.portrait) {
            this.currentUserAvatar = normalizeFileUrl(this.currentUser.portrait)
          }
        }
      } catch (e) { /* ignore */ }
    },
    loadLabels() {
      this.getRequest('/common/getArticleLabel').then(resp => {
        if (resp && Array.isArray(resp)) {
          this.labelList = resp
        }
      }).catch(err => { console.warn('[BBSArticleDetails] loadLabels', err) })
    },
    loadArticle(id) {
      this.loading = true
      getArticleById(id).then(resp => {
        this.loading = false
        if (resp) {
          this.article.title = resp.articleTitle || ''
          this.article.publishTime = resp.createTime || ''
          this.article.tagId = resp.articleLabelId
          this.article.userId = resp.userId
          this.article.author = resp.articleAuthor || ''
          const rawContent = resp.articleContentHtml || resp.articleContent || ''
          const normalized = normalizeUrls(rawContent)
          this.article.contentHtml = normalized
          this.rawContent = normalized
          this.article.articleImage = normalizeFileUrl(resp.articleImage || '')
          // Fetch author info
          if (resp.userId) {
            this.loadAuthorInfo(resp.userId)
          }
        }
      }).catch(err => {
        console.warn('[BBSArticleDetails] loadArticle', err)
        this.loading = false
      })
    },
    loadAuthorInfo(userId) {
      getUserinfoById(userId).then(resp => {
        if (resp) {
          this.authorInfo = resp
          if (resp.portrait) {
            this.article.authorAvatar = normalizeFileUrl(resp.portrait)
          }
          this.article.authorOrgName = resp.orgName || ''
          this.article.authorOrgNameFull = resp.orgNameFull || ''
          this.article.authorTitle = resp.title || ''
        }
      }).catch(err => { console.warn('[BBSArticleDetails] loadAuthorInfo', err) })
    },
    loadComments(articleId) {
      getCommentReply(articleId).then(resp => {
        if (resp && Array.isArray(resp)) {
          this.comments = resp.map(c => this.mapComment(c))
        } else {
          this.comments = []
        }
        this.commentsLoaded = true
        // deep-link: 评论加载完成后滚动到目标位置
        this.$nextTick(() => {
          if (this.scrollToTarget) {
            this.scrollToComment(this.scrollToTarget)
          }
        })
      }).catch(err => {
        console.warn('[BBSArticleDetails] loadComments', err)
        this.comments = []
        this.commentsLoaded = true
      })
    },
    // Map API comment to BBSCommentItem props format
    mapComment(c) {
      const myId = this.currentUser ? this.currentUser.id : null
      const commentId = c.commentId || c.id
      const articleAuthorId = this.article && this.article.userId ? this.article.userId : null
      const mapped = {
        id: commentId,
        replyKey: 'c-' + commentId,
        commentRootId: commentId,
        userId: c.userId,
        articleAuthorId,
        author: c.nickname || '',
        avatar: normalizeFileUrl(c.portrait || ''),
        orgName: c.orgName || '',
        orgNameFull: c.orgNameFull || '',
        time: c.commentTime || '',
        content: c.commentContent || '',
        canDelete: myId != null && String(c.userId) === String(myId),
        adoptStatus: c.adoptStatus || 0,
        children: (c.reply || []).map(r => {
          const replyId = r.replyId || r.id
          return {
            id: replyId,
            replyKey: 'r-' + replyId,
            commentRootId: commentId,
            userId: r.replyUserId || r.userId,
            replyTargetUserId: r.replyToUserId,
            articleAuthorId,
            author: r.nickname || '',
            avatar: normalizeFileUrl(r.portrait || ''),
            orgName: r.orgName || '',
            orgNameFull: r.orgNameFull || '',
            time: r.replyTime || '',
            content: r.replyContent || '',
            replyTo: r.replyToNickname || '',
            canDelete: myId != null && String(r.replyUserId || r.userId) === String(myId),
            adoptStatus: r.adoptStatus || 0,
            children: [],
          }
        }),
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
      }).catch(err => {
        console.warn('[BBSArticleDetails] loadArticleFiles', err)
        this.article.attachments = []
      })
    },
    autoResizeCommentInput(e) {
      const el = e.target
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
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
      }).catch(err => { console.warn('[BBSArticleDetails] submitComment', err) })
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
          this.loadComments(this.articleId)
        } else {
          Message({ type: 'error', message: '回复失败', offset: 54 })
        }
      }).catch(err => { console.warn('[BBSArticleDetails] handleReply', err) })
    },
    handleAdopt({ id, adoptType }) {
      if (!this.currentUser || !id) return
      this.$confirm('确定采纳该内容为最佳解答？提交后等待管理员审核。', '采纳确认', {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'info',
      }).then(() => {
        const params = {
          articleId: this.articleId,
          userId: this.currentUser.id,
        }
        if (adoptType === 'reply') {
          params.replyId = id
        } else {
          params.commentId = id
        }
        this.postRequest('/reply/article/adoptReply', params).then(resp => {
          if (resp && resp.code === 200) {
            // 成功提示由 axios 拦截器统一处理，无需手动弹出
            this.loadComments(this.articleId)
          } else {
            Message({ type: 'warning', message: (resp && resp.message) || '操作失败', offset: 54 })
          }
        }).catch(err => { console.warn('[BBSArticleDetails] handleAdopt', err) })
      }).catch(() => {})
    },
    handleDeleteComment(comment) {
      if (!comment) return
      const commentId = comment.id != null ? comment.id : null
      if (commentId == null) {
        console.warn('[handleDeleteComment] missing id:', comment)
        return
      }
      const isReply = commentId !== comment.commentRootId
      this.$confirm('确定删除该' + (isReply ? '回复' : '评论') + '？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        if (isReply) {
          this.postRequest('/reply/deleteReplyById', { replyId: commentId }).then(resp => {
            if (resp) {
              this.loadComments(this.articleId)
            }
          }).catch(err => { console.warn('[BBSArticleDetails] deleteReply', err) })
        } else {
          this.postRequest('/comment/deleteCommentById', { commentId }).then(resp => {
            if (resp) {
              this.loadComments(this.articleId)
            }
          }).catch(err => { console.warn('[BBSArticleDetails] deleteComment', err) })
        }
      }).catch(() => {})
    },
    initStickyBar() {
      const input = this.$refs && this.$refs.commentInput
      if (!input) return

      this.commentInputObserver = new IntersectionObserver(
        ([entry]) => {
          this.showStickyBar = !entry.isIntersecting
        },
        {
          rootMargin: '0px 0px -40px 0px',
          threshold: 0,
        }
      )

      this.commentInputObserver.observe(input)
    },
    downloadFile(filePath, fileName) {
      if (!filePath) return
      const url = filePath.startsWith('/') ? filePath : '/' + filePath
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
        .catch(err => {
          console.warn('[BBSArticleDetails] downloadFile', err)
          Message({ message: '下载失败，请稍后重试', type: 'warning', showClose: true, offset: 54 })
        })
    },
    /**
     * 滚动到指定评论/回复位置并高亮（DOM 直接操作，不依赖 Vue 响应式）
     */
    scrollToComment(targetId) {
      const el = document.getElementById('comment-' + targetId)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        this.applyHighlight(el)
        return
      }
      // 如果元素不存在（可能在折叠的回复中），展开后重试
      for (const comment of this.comments) {
        if (comment.children) {
          const found = comment.children.find(c => String(c.id) === String(targetId))
          if (found) {
            this.expandCommentReplies(comment.id)
            this.$nextTick(() => {
              setTimeout(() => {
                const retryEl = document.getElementById('comment-' + targetId)
                if (retryEl) {
                  retryEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
                  this.applyHighlight(retryEl)
                }
              }, 200)
            })
            return
          }
        }
      }
    },
    /**
     * 对指定 DOM 元素应用高亮动画（添加 class → 3 秒后移除）
     */
    applyHighlight(el) {
      el.classList.add('comment-highlight')
      setTimeout(() => {
        el.classList.remove('comment-highlight')
      }, 3200)
    },
    /**
     * 展开指定评论的折叠回复
     */
    expandCommentReplies(commentId) {
      // 通过触发 BBSCommentItem 的展开逻辑来实现
      // 这里用一个简单的方式：设置 comment 的 expandedReplies
      // 由于 BBSCommentItem 使用内部状态，我们通过 provide/inject 传递展开信号
      this.$bus && this.$bus.$emit('expandComment', commentId)
    },
  },
  beforeDestroy() {
    if (this.commentInputObserver) {
      this.commentInputObserver.disconnect()
      this.commentInputObserver = null
    }
  },
}
</script>

<style>
/* 覆盖 github-markdown CSS 默认的 p 16px margin */
.markdown-body p {
  margin: 0;
  line-height: 1.5;
}
/* 评论/回复高亮动画（全局样式，供 DOM 操作添加 class 使用） */
.comment-highlight {
  animation: comment-highlight-fade 3s ease-out forwards;
  border-radius: 8px;
  padding: 4px 8px;
  margin: -4px -8px;
}
@keyframes comment-highlight-fade {
  0% { background-color: rgba(25, 118, 210, 0.20); }
  100% { background-color: transparent; }
}
</style>
<style scoped>
/* Sticky reply bar slide transition */
.reply-bar-slide-enter-active,
.reply-bar-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.reply-bar-slide-enter,
.reply-bar-slide-leave-to {
  transform: translateY(100%);
}
</style>
