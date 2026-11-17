<template>
  <div :id="'comment-' + comment.id" class="comment-anchor" :class="isReply ? 'reply-block' : 'comment-block'">
    <div class="flex items-start justify-between gap-2">
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-1.5 flex-wrap">
          <BBSUserBadge
            :avatar="comment.avatar"
            :nickname="comment.author"
            :show-org="false"
            :user-id="comment.userId"
            :level-index="comment.levelIndex"
            :size="isReply ? 'xs' : 'sm'"
          />
          <span
            v-if="comment.isAnonymous"
            class="px-1.5 py-0.5 rounded bg-surface-variant text-[10px] text-on-surface-variant"
          >匿名</span>
          <span v-if="isArticleAuthor" class="px-1.5 py-0.5 rounded bg-primary/10 text-primary text-[10px]">楼主</span>
          <span v-if="adoptBadge" class="px-1.5 py-0.5 rounded text-[10px] border" :class="adoptBadge.cls">{{ adoptBadge.text }}</span>
        </div>
        <div class="flex items-center gap-2 mt-0.5 text-[11px] text-outline" :class="isReply ? '' : 'ml-9'">
          <span>{{ comment.time }}</span>
          <span v-if="comment.replyTo" class="truncate">回复 <span class="text-primary">@{{ comment.replyTo }}</span></span>
        </div>
      </div>

      <!-- 动作区：小图标，避免喧宾夺主 -->
      <div class="flex items-center gap-0.5 shrink-0">
        <button
          class="comment-action"
          :class="{ 'is-on': comment.liked }"
          :title="comment.liked ? '取消点赞' : '点赞'"
          @click="$emit('like', comment)"
        >
          <span class="material-symbols-outlined">{{ comment.liked ? 'thumb_up' : 'thumb_up_off_alt' }}</span>
          <span v-if="comment.goodNum > 0" class="tabnum">{{ comment.goodNum }}</span>
        </button>
        <button class="comment-action" title="回复" @click="handleToggleReply">
          <span class="material-symbols-outlined">reply</span>
        </button>
        <button
          v-if="canAdopt && comment.adoptStatus === 0"
          class="comment-action is-adopt"
          title="采纳为最佳解答"
          @click="$emit('adopt', { id: comment.id, adoptType: isReply ? 'reply' : 'comment' })"
        >
          <span class="material-symbols-outlined">check_circle</span>
        </button>
        <button v-if="canReport" class="comment-action" title="举报" @click="$emit('report', comment)">
          <span class="material-symbols-outlined">flag</span>
        </button>
        <button v-if="comment.canDelete" class="comment-action is-danger" title="删除" @click="$emit('delete', comment)">
          <span class="material-symbols-outlined">delete</span>
        </button>
      </div>
    </div>

    <div class="mt-1.5 text-[12.5px] leading-relaxed text-on-surface whitespace-pre-line break-words" :class="isReply ? '' : 'ml-9'">
      {{ comment.content }}
    </div>

    <!-- 回复输入框：与楼中楼同一套浅底卡片语言，左边缘对齐正文列 -->
    <transition name="reply-box">
      <div v-if="showReplyInput" :class="isReply ? 'mt-1.5' : 'mt-2.5 ml-9'">
        <div class="reply-editor" :class="{ 'is-nested': isReply }">
          <div class="flex items-start gap-2">
            <BBSAvatar :src="currentUserAvatar" :size="24" :show-level="false" />
            <div class="flex-grow min-w-0">
              <textarea
                v-model="replyText"
                class="reply-editor-input"
                :placeholder="'回复 ' + (comment.author || '楼主')"
                rows="2"
                @keydown.enter.exact.prevent="submitReply"
                @input="autoResizeReplyInput"
              ></textarea>
              <div class="flex items-center justify-between gap-2 mt-1.5">
                <label v-if="allowAnonymous" class="flex items-center gap-1 text-[11px] text-on-surface-variant cursor-pointer select-none">
                  <input v-model="replyAnonymous" type="checkbox" class="w-3 h-3 text-primary border-outline-variant rounded">
                  匿名回复
                </label>
                <span v-else class="text-[11px] text-outline">Enter 发送 · Shift+Enter 换行</span>
                <div class="flex items-center gap-1.5">
                  <button class="ui-btn ui-btn-xs ui-btn-ghost" @click="cancelReply">取消</button>
                  <button class="ui-btn ui-btn-xs ui-btn-primary" @click="submitReply">发送</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 楼中楼 -->
    <div v-if="comment.children && comment.children.length > 0" class="mt-2.5 space-y-1.5" :class="isReply ? '' : 'ml-9'">
      <BBSCommentItem
        v-for="child in displayChildren"
        :key="child.id"
        :comment="child"
        :currentUserAvatar="currentUserAvatar"
        :isReply="true"
        @delete="$emit('delete', $event)"
        @reply="$emit('reply', $event)"
        @adopt="$emit('adopt', $event)"
        @report="$emit('report', $event)"
        @like="$emit('like', $event)"
      />
      <button
        v-if="comment.children.length > maxVisibleReplies"
        class="mt-1 text-[11px] font-medium text-primary hover:opacity-80"
        @click="expandedReplies = !expandedReplies"
      >
        {{ expandedReplies ? '收起回复' : '展开全部 ' + comment.children.length + ' 条回复' }}
      </button>
    </div>
  </div>
</template>

<script>
import BBSAvatar from '@/components/BBSAvatar.vue'
import BBSUserBadge from '@/components/BBSUserBadge.vue'

export default {
  name: 'BBSCommentItem',
  components: { BBSAvatar, BBSUserBadge },
  inject: ['replyState', 'adoptState'],
  props: {
    comment: { type: Object, required: true },
    currentUserAvatar: { type: String, default: '' },
    /** 顶层评论 false，楼中楼回复 true */
    isReply: { type: Boolean, default: false },
  },
  data() {
    return {
      replyText: '',
      replyAnonymous: false,
      maxVisibleReplies: 3,
      expandedReplies: false,
    }
  },
  mounted() {
    this.$bus && this.$bus.$on('expandComment', this.handleExpandComment)
  },
  beforeDestroy() {
    this.$bus && this.$bus.$off('expandComment', this.handleExpandComment)
  },
  computed: {
    showReplyInput() {
      return this.replyState.activeId === this.comment.replyKey
    },
    allowAnonymous() {
      return !!(this.adoptState && this.adoptState.article
        && this.adoptState.article.labelAllowAnonymous === 1)
    },
    displayChildren() {
      if (!this.comment.children) return []
      if (this.expandedReplies) return this.comment.children
      return this.comment.children.slice(0, this.maxVisibleReplies)
    },
    isArticleAuthor() {
      return this.comment.userId != null
        && this.comment.articleAuthorId != null
        && String(this.comment.userId) === String(this.comment.articleAuthorId)
    },
    adoptBadge() {
      if (this.comment.adoptStatus === 1) return { text: '待审批', cls: 'bg-amber-50 text-amber-700 border-amber-200' }
      if (this.comment.adoptStatus === 2) return { text: '✓ 已采纳', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' }
      if (this.comment.adoptStatus === 3) return { text: '已拒绝', cls: 'bg-rose-50 text-rose-600 border-rose-100' }
      return null
    },
    canAdopt() {
      return this.adoptState.isQuestionLabel && this.adoptState.currentUserId != null
        && this.adoptState.currentUserId === this.comment.articleAuthorId
        && this.adoptState.currentUserId !== this.comment.userId
    },
    canReport() {
      return this.adoptState.currentUserId != null
        && this.adoptState.currentUserId !== this.comment.userId
    },
  },
  methods: {
    handleExpandComment(commentId) {
      if (String(this.comment.id) === String(commentId)
        && this.comment.children && this.comment.children.length > this.maxVisibleReplies) {
        this.expandedReplies = true
      }
    },
    handleToggleReply() {
      this.replyState.activeId = this.showReplyInput ? null : this.comment.replyKey
    },
    cancelReply() {
      this.replyText = ''
      this.replyAnonymous = false
      this.replyState.activeId = null
    },
    autoResizeReplyInput(e) {
      const el = e.target
      el.style.height = 'auto'
      el.style.height = Math.min(el.scrollHeight, 160) + 'px'
    },
    submitReply() {
      if (!this.replyText.trim()) return
      const replyToUserId = this.comment.userId != null
        ? this.comment.userId
        : this.comment.replyTargetUserId != null
          ? this.comment.replyTargetUserId
          : undefined
      this.$emit('reply', {
        commentId: this.comment.commentRootId,
        replyContent: this.replyText,
        replyToUserId,
        isAnonymous: this.replyAnonymous ? 1 : 0,
      })
      this.replyText = ''
      this.replyAnonymous = false
      this.replyState.activeId = null
    },
  },
}
</script>

<style scoped>
.comment-anchor {
  scroll-margin-top: 96px;
}

.comment-block {
  padding: 16px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
}

.comment-block:first-child {
  border-top: none;
}

/* 楼中楼：独立浅底块，楼层之间不再靠左边线，避免上下楼视觉混杂 */
.reply-block {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f9fc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

/* 内联回复编辑器：与楼中楼同一套浅底卡片语言，替代原先裸 textarea 的硬方框 */
.reply-editor {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f9fc;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

/* 楼中楼内部的编辑器：父块已是浅底，改白底避免同色叠加 */
.reply-editor.is-nested {
  background: #ffffff;
  border-color: rgba(148, 163, 184, 0.24);
}

.reply-editor-input {
  display: block;
  width: 100%;
  min-height: 40px;
  padding: 0;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  font-size: 12.5px;
  line-height: 1.55;
  color: #181c23;
}

.reply-editor-input::placeholder {
  color: #94a3b8;
}

/* 展开/收起：避免输入框突然插入把下面的楼层顶开 */
.reply-box-enter-active,
.reply-box-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.reply-box-enter,
.reply-box-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

.comment-action {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 24px;
  padding: 0 5px;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.comment-action .material-symbols-outlined {
  font-size: 15px;
}

.comment-action:hover {
  background: rgba(148, 163, 184, 0.14);
  color: #475569;
}

.comment-action.is-on {
  color: #2563eb;
}

.comment-action.is-adopt {
  color: #059669;
}

.comment-action.is-danger:hover {
  color: #dc2626;
}

.tabnum {
  font-variant-numeric: tabular-nums;
}
</style>
