<template>
  <!-- 内容预览弹窗（举报/违规两条管理线共用：文章详情 + 评论/回复加载 + 已删除内容可见） -->
  <div>
  <div v-if="visible" class="fixed inset-0 bg-black/30 z-40" @click="$emit('close')"></div>
  <div v-if="visible" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="bg-container w-full max-w-4xl mx-auto my-[5vh] rounded-xl shadow-2xl">
      <!-- Header -->
      <div class="flex items-center justify-between p-5 border-b border-outline-variant">
        <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">article</span>
          <template v-if="isArticleContext">
            {{ detailTitle ? '文章《' + detailTitle + '》详情' : '文章详情' }}
            <span v-if="item && item.targetType !== 'article'" class="text-[12px] font-normal text-amber-600 ml-1">（含{{ getTargetTypeLabel(item.targetType) }}记录）</span>
          </template>
          <template v-else>
            {{ item ? getTargetTypeLabel(item.targetType) + '详情' : '内容预览' }}
          </template>
        </h3>
        <button class="text-outline hover:text-error transition-colors" @click="$emit('close')">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <!-- Body -->
      <div class="p-5" v-loading="loading">
        <template v-if="item">
          <!-- 记录原因（仅当评论/回复无所属文章时显示在顶部） -->
          <div v-if="item.targetType !== 'article' && !item.targetArticleId" class="mb-4 px-3 py-2 bg-amber-50 border border-amber-200 rounded-lg flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-600 text-[16px]">flag</span>
            <span class="text-[12px] font-medium text-amber-700">记录原因：</span>
            <span class="text-[13px] text-amber-800">{{ item.reason || '无' }}</span>
          </div>

          <!-- 文章上下文 -->
          <template v-if="isArticleContext">
            <div v-if="articleLoadFailed" class="py-12 text-center">
              <span class="material-symbols-outlined text-outline text-[48px]">article</span>
              <p class="mt-3 text-body-md text-on-surface-variant">该文章已被彻底删除（数据库中无记录）</p>
            </div>
            <template v-else>
              <!-- 整篇文章（标题+正文+附件）高亮容器 -->
              <div v-if="detailTitle" class="mb-6 relative report-highlighted rounded-lg p-5 border border-amber-300">
                <div class="absolute -left-3 top-0 bottom-0 w-1 bg-amber-500 rounded-full"></div>
                <h2 class="font-headline-md text-headline-md text-on-surface flex items-center gap-2">
                  标题：《{{ detailTitle }}》
                  <span v-if="articleDeleted" class="px-2 py-0.5 rounded text-[12px] font-medium bg-red-100 text-red-700 border border-red-200">已删除</span>
                </h2>
                <div class="flex items-center gap-3 mt-2">
                  <span class="text-body-md text-on-surface-variant flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">person</span>
                    {{ item.targetAuthorName || '未知' }} (ID: {{ item.targetAuthorId || '?' }})
                  </span>
                </div>
                <div v-if="item.reason" class="mt-2 px-3 py-2 bg-amber-100 border border-amber-200 rounded-lg inline-flex items-center gap-2">
                  <span class="material-symbols-outlined text-amber-600 text-[16px]">flag</span>
                  <span class="text-[12px] font-medium text-amber-700">关联内容 — {{ item.reason }}</span>
                </div>
                <div v-if="articleDeleted" class="mt-3 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
                  <span class="material-symbols-outlined text-red-500 text-[16px]">info</span>
                  <span class="text-[13px] text-red-700">该文章已被用户删除（管理员仍可查看内容）</span>
                </div>
                <div class="mt-4 markdown-body detail-content" v-html="renderedContent"></div>
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
                    </div>
                  </div>
                </div>
              </div>
              <!-- 评论 -->
              <div class="mt-6">
                <h4 class="font-headline-sm text-headline-sm text-on-surface mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-[20px]">comment</span>
                  评论（{{ detailCommentCount }}）
                </h4>
                <div v-if="detailComments.length > 0" class="space-y-4">
                  <div v-for="(c, index) in detailComments" :key="c.commentId || index"
                    :class="[
                      'rounded-lg p-4 border',
                      isHighlightedComment(c) ? 'bg-amber-50 border-amber-300 relative report-highlighted' : 'bg-surface-container-low border-outline-variant/50'
                    ]">
                    <div v-if="isHighlightedComment(c)" data-report-highlight class="absolute -left-3 top-0 bottom-0 w-1 bg-amber-500 rounded-full"></div>
                    <div v-if="isHighlightedComment(c) && item.reason" class="mb-2 px-3 py-1.5 bg-amber-100 border border-amber-200 rounded-lg inline-flex items-center gap-1.5">
                      <span class="material-symbols-outlined text-amber-600 text-[14px]">flag</span>
                      <span class="text-[11px] font-medium text-amber-700">关联内容 — {{ item.reason }}</span>
                    </div>
                    <div class="flex items-center gap-3 mb-2">
                      <img class="w-9 h-9 rounded-full bg-surface-variant object-cover" :src="c.portrait || defaultAvatar" alt="">
                      <div>
                        <span class="font-headline-sm text-headline-sm text-on-surface">{{ c.nickname || '未知用户' }}</span>
                        <span class="text-body-md text-on-surface-variant ml-2">{{ c.commentTime }}</span>
                      </div>
                    </div>
                    <p class="text-body-md text-on-surface ml-12">{{ c.commentContent }}</p>
                    <div v-if="c.reply && c.reply.length" class="ml-12 mt-3 pl-4 border-l-2 border-outline-variant/30 space-y-3">
                      <div v-for="(reply, rIdx) in c.reply" :key="reply.replyId || rIdx"
                        :class="[
                          'rounded-lg p-3 relative',
                          isHighlightedReply(reply) ? 'bg-amber-50 border border-amber-300 relative report-highlighted' : 'bg-surface-container'
                        ]">
                        <div v-if="isHighlightedReply(reply)" data-report-highlight class="absolute -left-3 top-0 bottom-0 w-1 bg-amber-500 rounded-full"></div>
                        <div v-if="isHighlightedReply(reply) && item.reason" class="mb-1.5 px-2 py-1 bg-amber-100 border border-amber-200 rounded inline-flex items-center gap-1">
                          <span class="material-symbols-outlined text-amber-600 text-[12px]">flag</span>
                          <span class="text-[10px] font-medium text-amber-700">关联内容 — {{ item.reason }}</span>
                        </div>
                        <div class="flex items-center gap-2 mb-1.5">
                          <img class="w-7 h-7 rounded-full bg-surface-variant object-cover" :src="reply.portrait || defaultAvatar" alt="">
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
                <div v-else class="py-6 text-center text-on-surface-variant text-body-md">暂无评论</div>
              </div>
            </template>
          </template>

          <!-- 评论/回复裸详情（所属文章不存在或无 articleId 时） -->
          <template v-else>
            <div v-if="articleLoadFailed" class="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <span class="material-symbols-outlined text-red-500 text-[16px]">error</span>
              <span class="text-[13px] text-red-700">所属文章已删除或不存在，无法加载完整上下文</span>
            </div>
            <div class="flex items-center gap-3 mb-3">
              <span class="px-2 py-0.5 rounded text-[12px] font-medium bg-surface-container-low text-on-surface-variant">
                {{ getTargetTypeLabel(item.targetType) }}
              </span>
              <span class="text-body-md text-on-surface-variant">
                作者：{{ item.targetAuthorName || '未知' }} (ID: {{ item.targetAuthorId || '?' }})
              </span>
            </div>
            <div v-if="item.targetContent" class="text-body-md text-on-surface whitespace-pre-wrap leading-relaxed bg-surface-container-low rounded-lg p-4">{{ item.targetContent }}</div>
            <div v-else class="text-body-md text-outline italic py-8 text-center">（内容已删除或无法加载）</div>
          </template>
        </template>
      </div>
    </div>
  </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it/dist/markdown-it'
import 'mavon-editor/dist/markdown/github-markdown.min.css'

export default {
  name: 'ContentPreviewDialog',
  props: {
    visible: { type: Boolean, default: false },
    // { targetType, targetId, targetArticleId, targetAuthorId, targetAuthorName, targetContent, reason }
    item: { type: Object, default: null }
  },
  data() {
    return {
      loading: false,
      detailTitle: '',
      detailContent: '',
      detailComments: [],
      detailFileList: [],
      defaultAvatar: require('../assets/img/img.jpeg'),
      articleLoadFailed: false,
      articleDeleted: false,
      _md: null
    }
  },
  computed: {
    isArticleContext() {
      if (!this.item) return false
      if (this.item.targetType === 'article') return true
      return this.item.targetArticleId && !this.articleLoadFailed
    },
    renderedContent() {
      if (!this.detailContent) return ''
      if (!this._md) {
        this._md = new MarkdownIt({ html: true, xhtmlOut: true, breaks: true, linkify: false, typographer: true })
      }
      return this._md.render(this.detailContent)
    },
    detailCommentCount() {
      if (!this.detailComments.length) return 0
      return this.detailComments.reduce((sum, c) => sum + 1 + (c.reply && c.reply.length ? c.reply.length : 0), 0)
    }
  },
  watch: {
    visible(v) {
      if (v) this.open()
    }
  },
  methods: {
    getTargetTypeLabel(t) { return { article: '文章', comment: '评论', reply: '回复' }[t] || t },
    isHighlightedComment(c) {
      if (!this.item) return false
      return this.item.targetType === 'comment' && String(this.item.targetId) === String(c.commentId)
    },
    isHighlightedReply(r) {
      if (!this.item) return false
      return this.item.targetType === 'reply' && String(this.item.targetId) === String(r.replyId)
    },
    open() {
      this.detailTitle = ''
      this.detailContent = ''
      this.detailComments = []
      this.detailFileList = []
      this.articleLoadFailed = false
      this.articleDeleted = false
      if (!this.item) return
      if (this.item.targetType === 'article') {
        this.loadArticleDetail(this.item.targetId)
      } else if (this.item.targetType === 'comment' || this.item.targetType === 'reply') {
        if (this.item.targetArticleId) this.loadArticleDetail(this.item.targetArticleId)
      }
    },
    loadArticleDetail(articleId) {
      this.loading = true
      this.articleLoadFailed = false
      this.articleDeleted = false
      this.getRequest('/admin/getArticleByIdInclDeleted', articleId).then(resp => {
        this.loading = false
        if (resp && resp.obj) {
          this.detailContent = resp.obj.articleContent || ''
          this.detailTitle = resp.obj.articleTitle || ''
          this.articleDeleted = resp.obj.isDelete === 1
          this.loadArticleFiles(articleId)
          this.loadArticleComments(articleId)
        } else {
          this.articleLoadFailed = true
        }
      }).catch(() => { this.loading = false; this.articleLoadFailed = true })
    },
    loadArticleFiles(articleId) {
      this.postRequest(`/common/getArticleFileByArticleId/${articleId}`, {}).then(res => {
        let list = []
        if (Array.isArray(res)) list = res
        else if (res && Array.isArray(res.obj)) list = res.obj
        else if (res && Array.isArray(res.listBean)) list = res.listBean
        this.detailFileList = list
      }).catch(() => { this.detailFileList = [] })
    },
    loadArticleComments(articleId) {
      this.postRequest(`/common/comment/getCommentReply/${articleId}`).then(res => {
        const raw = (res && Array.isArray(res)) ? res : []
        this.detailComments = raw.map(c => ({
          ...c,
          portrait: c.portrait || '',
          reply: (c.reply || []).map(r => ({ ...r, portrait: r.portrait || '' }))
        }))
        if (this.item && this.item.targetType !== 'article') {
          setTimeout(() => this.scrollToHighlighted(), 150)
        }
      }).catch(() => { this.detailComments = [] })
    },
    scrollToHighlighted(retries = 3) {
      if (!this.item) return
      const panel = this.$el.closest('.z-50') || document.querySelector('.z-50')
      if (!panel) return
      const highlighted = panel.querySelector('[data-report-highlight]')
      if (highlighted) {
        const card = highlighted.closest('.rounded-lg')
        if (card) { card.scrollIntoView({ behavior: 'smooth', block: 'center' }); return }
      }
      if (retries > 0) setTimeout(() => this.scrollToHighlighted(retries - 1), 200)
    }
  }
}
</script>

<style scoped>
@keyframes report-highlight-pulse {
  0%, 100% { background-color: rgb(255 251 235); }
  50%      { background-color: rgb(254 243 199); }
}
.report-highlighted {
  animation: report-highlight-pulse 2s ease-in-out 3;
  box-shadow: inset 0 0 0 2px rgb(251 191 36);
}
</style>
