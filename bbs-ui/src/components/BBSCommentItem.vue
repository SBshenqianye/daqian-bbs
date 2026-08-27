<template>
  <div :id="'comment-' + comment.id" class="flex gap-2.5 comment-anchor">
    <div class="flex-grow min-w-0">
      <div class="flex items-center justify-between mb-0.5">
        <bbs-user-badge :avatar="comment.avatar" :nickname="comment.author" :org-name="comment.orgName" :org-name-full="comment.orgNameFull" size="xs" layout="inline" />
        <!-- 采纳状态标签 -->
        <div class="flex gap-2 items-center text-on-surface-variant font-label-sm">
          <span
            v-if="comment.adoptStatus === 1"
            class="px-2 py-0.5 bg-yellow-50 text-yellow-700 rounded text-[11px] font-medium border border-yellow-200"
          >
            待审批
          </span>
          <span
            v-else-if="comment.adoptStatus === 2"
            class="px-2 py-0.5 bg-green-50 text-green-700 rounded text-[11px] font-medium border border-green-200"
          >
            ✓ 已采纳
          </span>
          <span
            v-else-if="comment.adoptStatus === 3"
            class="px-2 py-0.5 bg-red-50 text-red-500 rounded text-[11px] font-medium border border-red-100"
          >
            已拒绝
          </span>
          <template v-if="canAdopt && comment.adoptStatus === 0">
            <button
              class="hover:text-primary transition-primary flex items-center gap-0.5 px-2 py-0.5 bg-primary/5 rounded text-[11px] hover:bg-primary/10"
              @click="$emit('adopt', { id: comment.id, adoptType: isReply ? 'reply' : 'comment' })"
            >
              <span class="material-symbols-outlined text-[14px]">check_circle</span> 采纳
            </button>
          </template>
          <button class="hover:text-primary transition-primary flex items-center gap-0.5" @click="handleToggleReply">
            <span class="material-symbols-outlined text-[14px]">reply</span> 回复
          </button>
          <button v-if="comment.canDelete" class="hover:text-error transition-primary flex items-center gap-0.5" @click="$emit('delete', comment)">
            <span class="material-symbols-outlined text-[14px]">delete</span> 删除
          </button>
        </div>
      </div>
      <p class="text-outline text-[11px] leading-tight mb-1 opacity-60">{{ comment.time }}</p>
      <div class="font-body-sm text-on-surface leading-relaxed mb-2 whitespace-pre-line">
        <span v-if="comment.replyTo" class="text-primary-container font-medium mr-1.5">回复{{ comment.replyTo }}:</span>
        {{ comment.content }}
      </div>

      <!-- Reply Input Box (紧贴层主内容，位于楼中楼之前) -->
      <div v-if="showReplyInput" class="mt-2 mb-2 flex gap-2.5 items-start">
        <div class="w-7 h-7 rounded-full overflow-hidden border border-border flex-shrink-0">
          <img :src="currentUserAvatar || require('../assets/portrait.png')" class="w-full h-full object-cover">
        </div>
        <div class="flex-grow flex gap-1.5">
          <textarea
            v-model="replyText"
            class="flex-grow p-1.5 rounded-lg border border-border text-label-sm bg-surface focus:border-primary-container outline-none resize-none"
            :placeholder="'回复 ' + comment.author"
            rows="1"
            @keydown.enter.exact="submitReply"
            @input="autoResizeReplyInput"
          ></textarea>
          <button class="bg-primary-container/10 text-primary-container px-3 py-1 rounded-lg font-label-sm text-label-sm hover:bg-primary-container hover:text-white transition-primary whitespace-nowrap" @click="submitReply">
            回复
          </button>
        </div>
      </div>

      <!-- Nested Replies (自动折叠) -->
      <div v-if="comment.children && comment.children.length > 0" class="ml-8 border-l-2 border-surface-container pl-3">
        <div class="space-y-3">
          <BBSCommentItem
            v-for="child in displayChildren"
            :key="child.id"
            :comment="child"
            :currentUserAvatar="currentUserAvatar"
            :isReply="true"
            @delete="$emit('delete', $event)"
            @reply="$emit('reply', $event)"
            @adopt="$emit('adopt', $event)"
          />
        </div>
        <!-- 展开/折叠按钮 -->
        <button
          v-if="comment.children.length > maxVisibleReplies"
          class="mt-2 text-label-sm font-label-sm text-primary-container hover:text-primary transition-primary cursor-pointer"
          @click="expandedReplies = !expandedReplies"
        >
          <template v-if="expandedReplies">
            收起回复
          </template>
          <template v-else>
            展开全部 {{ comment.children.length }} 条回复
          </template>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import BBSUserBadge from '@/components/BBSUserBadge'

export default {
  name: 'BBSCommentItem',
  components: { BBSUserBadge },
  inject: ['replyState', 'adoptState'],
  props: {
    comment: {
      type: Object,
      required: true,
    },
    currentUserAvatar: {
      type: String,
      default: '',
    },
    /** 是否为回复层级（顶层评论为 false，嵌套回复为 true） */
    isReply: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      replyText: '',
      maxVisibleReplies: 3,
      expandedReplies: false,
    }
  },
  mounted() {
    // 监听展开事件（deep-link 滚动到折叠回复时）
    this.$bus && this.$bus.$on('expandComment', this.handleExpandComment)
  },
  beforeDestroy() {
    this.$bus && this.$bus.$off('expandComment', this.handleExpandComment)
  },
  computed: {
    showReplyInput() {
      return this.replyState.activeId === this.comment.replyKey
    },
    displayChildren() {
      if (!this.comment.children) return []
      if (this.expandedReplies) return this.comment.children
      return this.comment.children.slice(0, this.maxVisibleReplies)
    },
    /** 是否显示采纳按钮：帖子是问题求助 + 当前用户是楼主 + 当前用户不是该评论/回复的作者 */
    canAdopt() {
      return this.adoptState.isQuestionLabel && this.adoptState.currentUserId != null
        && this.adoptState.currentUserId === this.comment.articleAuthorId
        && this.adoptState.currentUserId !== this.comment.userId
    },
  },
  methods: {
    handleExpandComment(commentId) {
      // 如果是自己的评论被请求展开，且有折叠的回复，展开它们
      if (String(this.comment.id) === String(commentId) && this.comment.children && this.comment.children.length > this.maxVisibleReplies) {
        this.expandedReplies = true
      }
    },
    handleToggleReply() {
      this.replyState.activeId = this.showReplyInput ? null : this.comment.replyKey
    },
    autoResizeReplyInput(e) {
      const el = e.target
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
    },
    submitReply() {
      if (!this.replyText.trim()) return
      const replyToUserId = this.comment.userId != null
        ? this.comment.userId
        : this.comment.replyTargetUserId != null
          ? this.comment.replyTargetUserId
          : undefined
      if (replyToUserId == null) {
        console.warn('[CommentItem] replyToUserId is missing, comment data:', JSON.parse(JSON.stringify(this.comment)))
      }
      this.$emit('reply', {
        commentId: this.comment.commentRootId,
        replyContent: this.replyText,
        replyToUserId,
      })
      this.replyText = ''
      this.replyState.activeId = null
    },
  },
}
</script>

<style scoped>
.comment-anchor {
  scroll-margin-top: 80px;
}
</style>
