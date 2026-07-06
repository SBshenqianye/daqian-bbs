<template>
  <div class="flex gap-2.5">
    <div class="w-7 h-7 rounded-full overflow-hidden border border-border flex-shrink-0 mt-0.5">
      <img :src="comment.avatar || require('../assets/portrait.png')" class="w-full h-full object-cover">
    </div>
    <div class="flex-grow min-w-0">
      <div class="flex items-center justify-between mb-0.5">
        <span class="font-bold text-on-surface font-body-sm">{{ comment.author }}</span>
        <div class="flex gap-2 text-on-surface-variant font-label-sm">
          <button class="hover:text-primary transition-primary flex items-center gap-0.5" @click="handleToggleReply">
            <span class="material-symbols-outlined text-[14px]">reply</span> 回复
          </button>
          <button v-if="comment.canDelete" class="hover:text-error transition-primary flex items-center gap-0.5" @click="$emit('delete', comment)">
            <span class="material-symbols-outlined text-[14px]">delete</span> 删除
          </button>
        </div>
      </div>
      <p class="text-outline text-label-xs mb-1">{{ comment.time }}</p>
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
            @delete="$emit('delete', $event)"
            @reply="$emit('reply', $event)"
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
export default {
  name: 'BBSCommentItem',
  inject: ['replyState'],
  props: {
    comment: {
      type: Object,
      required: true,
    },
    currentUserAvatar: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      replyText: '',
      maxVisibleReplies: 3,
      expandedReplies: false,
    }
  },
  computed: {
    showReplyInput() {
      return this.replyState.activeId === this.comment.id
    },
    displayChildren() {
      if (!this.comment.children) return []
      if (this.expandedReplies) return this.comment.children
      return this.comment.children.slice(0, this.maxVisibleReplies)
    },
  },
  methods: {
    handleToggleReply() {
      this.replyState.activeId = this.showReplyInput ? null : this.comment.id
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
