<template>
  <main class="max-w-5xl mx-auto px-page-margin-mobile md:px-page-margin-desktop py-6 md:py-8 bg-surface min-h-screen">
    <!-- 空态：帖子已被删除或不可见 -->
    <div v-if="articleState === 'deleted'" class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
      <span class="material-symbols-outlined text-7xl mb-4 opacity-15">delete_outline</span>
      <h2 class="font-headline-md text-headline-md text-on-surface mb-2">该帖子已被删除或不可见</h2>
      <p class="font-body-md text-body-md mb-8">原帖可能已被作者删除、管理员下架或正在审核中。</p>
      <router-link to="/forum" class="px-6 py-2.5 bg-primary-container text-white rounded-lg font-label-md text-label-md hover:bg-primary transition-primary">
        返回论坛
      </router-link>
    </div>

    <!-- 空态：帖子不存在 -->
    <div v-else-if="articleState === 'not_exist'" class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
      <span class="material-symbols-outlined text-7xl mb-4 opacity-15">search_off</span>
      <h2 class="font-headline-md text-headline-md text-on-surface mb-2">帖子不存在</h2>
      <p class="font-body-md text-body-md mb-8">您访问的帖子不存在，或链接地址有误。</p>
      <router-link to="/forum" class="px-6 py-2.5 bg-primary-container text-white rounded-lg font-label-md text-label-md hover:bg-primary transition-primary">
        返回论坛
      </router-link>
    </div>

    <!-- 帖子主体：标题 → 作者/元信息 → 正文 → 互动 -->
    <article v-if="articleState === 'normal'" class="bg-container rounded-2xl border border-border shadow-sm overflow-hidden mb-gutter">
      <div class="px-5 md:px-8 pt-6 pb-5">
        <header>
          <h1 class="text-[21px] md:text-[25px] font-bold leading-snug tracking-tight text-on-surface">{{ article.title }}</h1>

          <div class="flex flex-wrap items-center justify-between gap-2.5 mt-3">
            <div class="flex items-center gap-2.5 min-w-0">
              <BBSUserBadge
                :avatar="article.authorAvatar"
                :nickname="article.author"
                :org-name="article.authorOrgName"
                :org-name-full="article.authorOrgNameFull"
                :level-index="article.authorLevelIndex"
                :user-id="article.userId"
                size="md"
              />
              <span class="w-px h-7 bg-border shrink-0"></span>
              <div class="ui-meta text-[11.5px] leading-relaxed shrink-0">
                <span class="ui-meta-item"><span class="material-symbols-outlined">schedule</span>{{ formatTime(article.publishTime) }}</span>
                <span class="ui-meta-item"><span class="material-symbols-outlined">visibility</span>{{ article.views }}</span>
                <span class="ui-meta-item"><span class="material-symbols-outlined">chat_bubble</span>{{ totalCommentCount }}</span>
              </div>
            </div>

            <div class="flex items-center gap-1.5">
              <span v-if="article.isFeatured" class="ui-chip ui-chip-gold">
                <span class="material-symbols-outlined filled-icon">stars</span>精华
              </span>
              <el-tooltip :content="articleTagDescription" placement="top" effect="dark" :disabled="!articleTagDescription">
                <span v-if="articleTagName" class="ui-chip ui-chip-primary">{{ articleTagName }}</span>
              </el-tooltip>
              <span v-if="article.isAnonymous" class="ui-chip ui-chip-muted">匿名</span>
              <button
                v-if="canReportArticle"
                class="ui-icon-btn is-danger"
                title="举报该帖"
                @click="openArticleReport"
              >
                <span class="material-symbols-outlined">flag</span>
              </button>
            </div>
          </div>
        </header>

        <div class="h-px bg-border my-4"></div>

        <!-- 正文：第一视觉重心 -->
        <section v-if="renderedHtml" class="markdown-body md-body article-body" v-html="renderedHtml"></section>
        <section v-else class="article-body text-on-surface-variant">暂无内容</section>
      </div>

      <!-- 互动条：克制地放在正文之后，不抢正文 -->
      <footer class="px-5 md:px-8 py-2 border-t border-border bg-surface-container-lowest/70 flex items-center gap-1.5 flex-wrap">
        <button class="int-btn" :class="{ 'is-on': interaction.liked }" @click="toggleLike">
          <span class="material-symbols-outlined">{{ interaction.liked ? 'thumb_up' : 'thumb_up_off_alt' }}</span>
          <span>点赞</span><span class="tabnum">{{ interaction.likes || 0 }}</span>
        </button>
        <button class="int-btn" :class="{ 'is-fav': interaction.favorited }" @click="toggleFavorite">
          <span class="material-symbols-outlined">{{ interaction.favorited ? 'bookmark' : 'bookmark_border' }}</span>
          <span>收藏</span><span class="tabnum">{{ interaction.favorites || 0 }}</span>
        </button>
        <button class="int-btn" @click="shareArticle">
          <span class="material-symbols-outlined">share</span>
          <span>分享</span><span class="tabnum">{{ interaction.shares || 0 }}</span>
        </button>
        <span v-if="article.lastReplyTime" class="ml-auto text-[11px] text-outline">最后回复 {{ formatTime(article.lastReplyTime) }}</span>
      </footer>
    </article>

    <!-- Attachments Section -->
    <section v-if="articleState === 'normal' && article.attachments && article.attachments.length > 0" class="ui-card p-4 mb-3">
      <div class="flex items-center gap-2 mb-3">
        <span class="w-6 h-6 rounded-md bg-primary/10 text-primary grid place-items-center">
          <span class="material-symbols-outlined text-[15px]">attachment</span>
        </span>
        <h3 class="text-[14px] font-semibold text-on-surface">附件 <span class="text-[11px] text-outline font-normal">{{ article.attachments.length }}</span></h3>
      </div>
      <div class="space-y-2">
        <div
          v-for="(att, i) in article.attachments"
          :key="i"
          class="flex items-center justify-between p-2.5 bg-surface rounded-lg border border-outline-variant hover:border-primary/40 transition-colors group"
        >
          <div class="flex items-center gap-2.5 min-w-0">
            <span class="material-symbols-outlined text-outline group-hover:text-primary text-[18px]">description</span>
            <span class="text-[12.5px] text-on-surface truncate">{{ att.name }}</span>
          </div>
          <button class="flex items-center gap-1.5 bg-primary text-white px-3 py-1.5 rounded-lg text-[11.5px] font-medium hover:opacity-90 transition-opacity shrink-0" @click="downloadFile(att.filePath, att.name)">
            <span class="material-symbols-outlined text-[16px]">download</span>
            下载
          </button>
        </div>
      </div>
    </section>

    <!-- Comment Section -->
    <section v-if="articleState === 'normal'" class="bg-container rounded-2xl border border-border shadow-sm overflow-hidden" :class="{ 'pb-16': showStickyBar }">
      <div class="px-5 md:px-8 py-5">
        <div class="flex items-center gap-2 mb-3">
          <h3 class="ui-section-title">
            <span class="material-symbols-outlined text-[16px] text-primary">forum</span>评论
          </h3>
          <span class="ui-chip ui-chip-muted tabnum">{{ totalCommentCount }}</span>
        </div>

        <!-- 评论输入：卡片式，按钮不再与输入框争抢视线 -->
        <div class="rounded-xl border border-border bg-surface-container-lowest p-3">
          <div class="flex gap-2.5">
            <BBSAvatar :src="currentUserAvatar" :size="32" :show-level="false" />
            <div class="flex-1 min-w-0">
              <textarea
                ref="commentInput"
                v-model="newComment"
                class="w-full bg-transparent border-0 outline-none text-[13px] leading-relaxed resize-none placeholder:text-outline"
                :placeholder="commentPlaceholder"
                rows="2"
                @keydown.enter.exact.prevent="submitComment"
                @input="autoResizeCommentInput"
              ></textarea>
              <div class="flex items-center justify-between gap-2 mt-1">
                <label v-if="allowAnonymousComment" class="flex items-center gap-1.5 text-[11px] text-on-surface-variant cursor-pointer select-none">
                  <input v-model="newCommentAnonymous" type="checkbox" class="w-3 h-3 text-primary border-outline-variant rounded">
                  匿名发表
                </label>
                <span v-else class="text-[11px] text-outline">Enter 发送 · Shift+Enter 换行</span>
                <button
                  class="ui-btn ui-btn-xs ui-btn-primary"
                  @click="submitComment"
                >发表评论</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Comments List -->
        <div class="mt-3">
          <BBSCommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :currentUserAvatar="currentUserAvatar"
            @delete="handleDeleteComment"
            @reply="handleReply"
            @adopt="handleAdopt"
            @report="handleCommentReport"
            @like="handleLikeComment"
          />

          <!-- 空状态：全无评论 -->
          <div
            v-if="showCommentEmptyState && totalCommentCount === 0"
            class="ui-empty"
          >
            <span class="material-symbols-outlined">chat_bubble_outline</span>
            <p class="ui-empty-title">还没有评论</p>
            <p class="ui-empty-desc">来说点什么吧～</p>
          </div>

          <!-- 空状态：评论稀少 -->
          <div
            v-if="showCommentEmptyState && totalCommentCount > 0"
            class="mt-1 py-5 flex items-center justify-center gap-2 text-[11.5px] text-on-surface-variant"
          >
            <span class="h-px w-10 bg-border"></span>
            评论不多，来说点什么吧～
            <span class="h-px w-10 bg-border"></span>
          </div>
        </div>
      </div>
    </section>

    <!-- Sticky Reply Bar -->
    <transition name="reply-bar-slide">
      <div
        v-if="articleState === 'normal' && showStickyBar"
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
              class="ui-btn ui-btn-sm ui-btn-primary flex-shrink-0"
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

    <!-- 举报弹窗（文章/评论/回复共用） -->
    <BBSReportDialog
      :visible="reportDialog.visible"
      :target-type="reportDialog.targetType"
      :target-id="reportDialog.targetId"
      :target-preview="reportDialog.targetPreview"
      @close="reportDialog.visible = false"
    />
  </main>
</template>

<script>
import BBSCommentItem from '@/components/BBSCommentItem.vue'
import BBSReportDialog from '@/components/BBSReportDialog.vue'
import BBSAvatar from '@/components/BBSAvatar.vue'
import BBSUserBadge from '@/components/BBSUserBadge.vue'
import { getArticleById, getArticleStatus, getUserinfoById, getArticleFileByArticleId } from '@/api/article'
import { getCommentReply } from '@/api/comment'
import { normalizeFileUrl, normalizeUrls } from '@/utils/utils'
import { mdToHtml } from '@/utils/markdown'
import { dateStr } from '@/utils/time'
import { Message } from 'element-ui'
import { handleResponse } from '../../../shared/feedback'

// 保持 github-markdown 样式用于 v-html 渲染的 markdown-body
import 'mavon-editor/dist/markdown/github-markdown.min.css'

// 评论区域空白填充阈值：评论数低于此值时显示空状态，避免头重脚轻
const MIN_COMMENT_FILLER = 3

export default {
  name: 'BBSArticleDetails',
  components: { BBSCommentItem, BBSReportDialog, BBSAvatar, BBSUserBadge },
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
      newCommentAnonymous: false,
      interaction: { liked: false, favorited: false, likes: 0, favorites: 0, shares: 0 },
      articleId: this.$route.params.articleId,
      loading: true,
      // 帖子对外展示状态：loading | normal | deleted | not_exist（#5）
      articleState: 'loading',
      // VUE_APP_BBS_API、VUE_APP_BBS_BASE_FILE 废弃，文件统一使用 /files/ 原始路径
      // Current user
      currentUser: null,
      currentUserAvatar: '',
      // Article data
      article: {
        title: '',
        publishTime: '',
        lastReplyTime: '',
        views: 0,
        tag: '',
        tagId: '',
        userId: null,
        author: '',
        authorTitle: '',
        authorAvatar: '',
        authorOrgName: '',
        authorOrgNameFull: '',
        authorLevelIndex: null,
        isAnonymous: false,
        labelAllowAnonymous: 0,
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
      // 举报弹窗状态（文章/评论/回复共用）
      reportDialog: { visible: false, targetType: 'article', targetId: null, targetPreview: '' },
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
    /** 帖子标签是否为"问题求助"用途（按 labelType 判断，与标签名解耦） */
    isQuestionLabel() {
      if (!this.article.tagId || !this.labelList.length) return false
      const label = this.labelList.find(l => String(l.labelId) === String(this.article.tagId))
      return !!(label && label.labelType === 'question')
    },
    /** 是否可举报文章：已登录且不是自己的文章 */
    canReportArticle() {
      return this.currentUserId != null && this.article.userId != null
        && String(this.currentUserId) !== String(this.article.userId)
    },
    allowAnonymousComment() {
      return this.article.labelAllowAnonymous === 1
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
      this.resolveArticleState(this.articleId)
    }
    this.$nextTick(() => {
      this.initStickyBar()
    })
    // 随机文明提示语
    const hints = '（Shift+Enter 换行）'
    const prompts = [
      '文明发言，理性讨论' + hints,
      '发表你的看法...' + hints,
      '欢迎分享你的观点' + hints,
      '请友善评论' + hints,
      '说点什么吧...' + hints,
      '你的每一条评论都很重要' + hints,
      '尊重他人，理性表达' + hints,
      '好文值得一赞，也欢迎评论' + hints,
      '请不要发布违规内容' + hints,
      '聊聊你的想法？' + hints,
      '营造良好社区氛围，从评论做起' + hints,
      '留下你的真知灼见' + hints,
      '你的评论是对作者最大的鼓励' + hints,
      '用心评论，传递价值' + hints,
      '友好交流，共同进步' + hints,
    ]
    this.commentPlaceholder = prompts[Math.floor(Math.random() * prompts.length)]
  },
  methods: {
    formatTime(val) {
      return dateStr(val)
    },
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
    /**
     * 先识别帖子对外展示状态（#5）：
     * NORMAL → 走原有加载流程；DELETED → 显示"已删除或不可见"空态页；其余 → 显示"不存在"空态页。
     * 状态接口异常时退回原有加载逻辑，不阻断详情展示。
     */
    resolveArticleState(id) {
      getArticleStatus(id).then(resp => {
        const status = resp && resp.obj
        if (status === 'NORMAL') {
          this.articleState = 'normal'
          this.loadArticle(id)
          this.loadComments(id)
          this.loadArticleFiles(id)
        } else if (status === 'DELETED') {
          this.articleState = 'deleted'
        } else {
          this.articleState = 'not_exist'
        }
      }).catch(err => {
        console.warn('[BBSArticleDetails] resolveArticleState', err)
        this.articleState = 'normal'
        this.loadArticle(id)
        this.loadComments(id)
        this.loadArticleFiles(id)
      })
    },
    loadArticle(id) {
      this.loading = true
      getArticleById(id).then(resp => {
        this.loading = false
        if (resp) {
          this.article.title = resp.articleTitle || ''
          this.article.publishTime = resp.createTime || ''
          this.article.lastReplyTime = resp.lastReplyTime || ''
          this.article.views = resp.articleViewNum || 0
          this.article.tagId = resp.articleLabelId
          this.article.userId = resp.userId
          this.article.author = resp.articleAuthor || ''
          this.article.authorLevelIndex = resp.authorLevelIndex || null
          this.article.authorOrgName = resp.authorOrgName || ''
          this.article.authorOrgNameFull = resp.authorOrgNameFull || ''
          const rawContent = resp.articleContentHtml || resp.articleContent || ''
          const normalized = normalizeUrls(rawContent)
          this.article.contentHtml = normalized
          this.rawContent = normalized
          this.article.articleImage = normalizeFileUrl(resp.articleImage || '')
          this.article.isAnonymous = resp.isAnonymous === 1 || resp.isAnonymous === '1'
          this.article.labelAllowAnonymous = resp.labelAllowAnonymous
          // Fetch author info
          if (resp.userId) {
            this.loadAuthorInfo(resp.userId)
          }
          this.loadInteraction(id)
        }
      }).catch(err => {
        console.warn('[BBSArticleDetails] loadArticle', err)
        this.loading = false
      })
    },
    loadInteraction(articleId) {
      this.getRequest(`/common/article/interaction?articleId=${articleId}`).then(resp => {
        if (resp && resp.obj) this.interaction = resp.obj
      }).catch(() => {})
    },
    toggleLike() {
      if (!this.currentUser) {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      this.postRequest('/article/like', { articleId: Number(this.articleId) }).then(resp => {
        if (resp && resp.obj) this.interaction = resp.obj
        if (resp && resp.message) this.$message.success(resp.message)
      }).catch(() => {})
    },
    toggleFavorite() {
      if (!this.currentUser) {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      this.postRequest('/article/favorite', { articleId: Number(this.articleId) }).then(resp => {
        if (resp && resp.obj) this.interaction = resp.obj
        if (resp && resp.message) this.$message.success(resp.message)
      }).catch(() => {})
    },
    async shareArticle() {
      const url = window.location.href
      try {
        if (navigator.share) {
          await navigator.share({ title: this.article.title, url })
        } else {
          await navigator.clipboard.writeText(url)
          this.$message.success('链接已复制')
        }
        this.postRequest('/article/share', { articleId: Number(this.articleId) }).then(resp => {
          if (resp && resp.obj) this.interaction = resp.obj
          else this.interaction.shares = (this.interaction.shares || 0) + 1
        }).catch(() => {})
      } catch (e) {
        // 用户取消分享不提示
      }
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
        levelIndex: c.levelIndex || null,
        isAnonymous: c.isAnonymous === 1 || c.isAnonymous === '1',
        goodNum: c.goodNum || 0,
        liked: !!c.liked,
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
            levelIndex: r.levelIndex || null,
            isAnonymous: r.isAnonymous === 1 || r.isAnonymous === '1',
            goodNum: r.goodNum || 0,
            liked: !!r.liked,
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
        isAnonymous: this.newCommentAnonymous ? 1 : 0,
      }
      this.putRequest('/comment/userComment', tempData).then(resp => {
        if (resp) {
          this.newComment = ''
          this.newCommentAnonymous = false
          this.loadComments(this.articleId)
          this.loadInteraction(this.articleId)
        }
      }).catch(err => { console.warn('[BBSArticleDetails] submitComment', err) })
    },
    handleReply({ commentId, replyContent, replyToUserId, isAnonymous }) {
      if (!this.currentUser) return
      const params = {
        replyUserId: this.currentUser.id,
        replyContent,
        commentId,
        isAnonymous: isAnonymous ? 1 : 0,
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
    /** 评论 / 楼中楼回复点赞（同一张互动表，targetType 区分） */
    handleLikeComment(comment) {
      if (!this.currentUser) {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      if (!comment || comment.id == null) return
      const isReply = String(comment.id) !== String(comment.commentRootId)
      this.postRequest('/common/thumb', {
        targetType: isReply ? 'reply' : 'comment',
        targetId: Number(comment.id),
      }).then(resp => {
        const data = resp && resp.obj
        if (!data) return
        const target = this.findCommentNode(comment.id)
        if (target) {
          target.goodNum = data.likes || 0
          target.liked = !!data.liked
        }
      }).catch(err => { console.warn('[BBSArticleDetails] likeComment', err) })
    },
    findCommentNode(id) {
      for (let i = 0; i < this.comments.length; i++) {
        const c = this.comments[i]
        if (String(c.id) === String(id)) return c
        const children = c.children || []
        for (let j = 0; j < children.length; j++) {
          if (String(children[j].id) === String(id)) return children[j]
        }
      }
      return null
    },
    handleAdopt({ id, adoptType }) {      if (!this.currentUser || !id) return
      this.$confirm('确定采纳该内容为最佳解答？提交后等待管理员审核。若该文章已有待审批的采纳，将替换之前的采纳。', '采纳确认', {
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
          handleResponse(resp, { successMsg: resp && resp.message, onSuccess: () => this.loadComments(this.articleId) })
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
            handleResponse(resp, { silent: true, onSuccess: () => this.loadComments(this.articleId) })
          }).catch(err => { console.warn('[BBSArticleDetails] deleteReply', err) })
        } else {
          this.postRequest('/comment/deleteCommentById', { commentId }).then(resp => {
            handleResponse(resp, { silent: true, onSuccess: () => this.loadComments(this.articleId) })
          }).catch(err => { console.warn('[BBSArticleDetails] deleteComment', err) })
        }
      }).catch(() => {})
    },
    /** 打开举报弹窗（统一入口：未登录跳登录页；已举报过则即时提醒，不打开弹窗） */
    async openReportDialog(payload) {
      if (!this.currentUser) {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      // 前置检查：点举报即提醒是否已举报过，而不是等填完提交才提示。
      // 口径与后端提交拦截一致（pending 等待 / confirmed 已核实；rejected 后允许再报）。
      try {
        const resp = await this.postRequest('/user/report/check', {
          reporterId: Number(this.currentUser.id),
          targetType: payload.targetType,
          targetId: Number(payload.targetId),
        })
        if (resp && resp.code === 200 && resp.obj && resp.obj.reported) {
          const msg = resp.obj.status === 'confirmed'
            ? '该内容已被核实处理，无需重复举报'
            : '您已举报过该内容，请等待审核结果'
          this.$message.warning(msg)
          return
        }
      } catch (e) { /* 查询异常不阻断，仍允许打开弹窗提交（后端提交时兜底拦截） */ }
      this.reportDialog = {
        visible: true,
        targetType: payload.targetType,
        targetId: payload.targetId,
        targetPreview: payload.targetPreview || '',
      }
    },
    /** 举报文章 */
    openArticleReport() {
      const preview = this.article.title ? '文章《' + this.article.title + '》' : '文章 #' + this.articleId
      this.openReportDialog({ targetType: 'article', targetId: Number(this.articleId), targetPreview: preview })
    },
    /** 举报评论/回复（BBSCommentItem @report 逐层冒泡，replyKey 区分层级） */
    handleCommentReport(comment) {
      if (!comment) return
      const isReply = comment.id !== comment.commentRootId
      const preview = (comment.author ? comment.author + '：' : '') + String(comment.content || '').slice(0, 80)
      this.openReportDialog({
        targetType: isReply ? 'reply' : 'comment',
        targetId: comment.id,
        targetPreview: preview,
      })
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

/* 正文：以可读性为第一目标，标题/段落/引用都有明确层级 */
.md-body {
  font-size: 15px;
  line-height: 1.9;
  color: #1f2937;
  word-break: break-word;
}

/* 短帖也保留足够的呼吸空间，避免正文被标题和互动条上下夹住 */
.article-body {
  min-height: 220px;
}
@media (min-width: 768px) {
  .article-body {
    min-height: 280px;
  }
}
.md-body ::v-deep p {
  margin: 0 0 0.9em;
}
.md-body ::v-deep p:last-child {
  margin-bottom: 0;
}
.md-body ::v-deep h1,
.md-body ::v-deep h2,
.md-body ::v-deep h3,
.md-body ::v-deep h4 {
  margin: 1.4em 0 0.6em;
  font-weight: 700;
  line-height: 1.5;
  color: #111827;
}
.md-body ::v-deep h1 { font-size: 20px; }
.md-body ::v-deep h2 { font-size: 18px; }
.md-body ::v-deep h3 { font-size: 16px; }
.md-body ::v-deep ul,
.md-body ::v-deep ol {
  margin: 0.6em 0 1em;
  padding-left: 1.4em;
}
.md-body ::v-deep li {
  margin: 0.3em 0;
}
.md-body ::v-deep blockquote {
  margin: 1em 0;
  padding: 0.5em 0 0.5em 1em;
  border-left: 3px solid #dbeafe;
  color: #4b5563;
  background: #f8fafc;
}
.md-body ::v-deep img {
  border-radius: 10px;
  max-width: 100%;
}
.md-body ::v-deep code {
  background: #f1f5f9;
  border-radius: 4px;
  padding: 0.1em 0.35em;
  font-size: 0.92em;
}
.md-body ::v-deep pre {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
  overflow-x: auto;
  margin: 1em 0;
}
.md-body ::v-deep pre code {
  background: transparent;
  color: inherit;
  padding: 0;
}
.md-body ::v-deep table {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  font-size: 13px;
}
.md-body ::v-deep th,
.md-body ::v-deep td {
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  text-align: left;
}
.md-body ::v-deep th {
  background: #f8fafc;
}

/* 互动条按钮：默认低对比度，只在悬停/激活时强调 */
.int-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
  transition: background-color 0.15s ease, color 0.15s ease;
}
.int-btn .material-symbols-outlined {
  font-size: 17px;
}
.int-btn:hover {
  background: rgba(148, 163, 184, 0.14);
  color: #334155;
}
.int-btn.is-on {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
}
.int-btn.is-fav {
  color: #b45309;
  background: rgba(180, 83, 9, 0.08);
}
.tabnum {
  font-variant-numeric: tabular-nums;
}
</style>