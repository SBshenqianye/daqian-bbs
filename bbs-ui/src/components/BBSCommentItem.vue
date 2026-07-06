<template>
  <div class="flex gap-3">
    <div class="w-8 h-8 rounded-full overflow-hidden border border-border flex-shrink-0">
      <img :src="comment.avatar || require('../assets/portrait.png')" class="w-full h-full object-cover">
    </div>
    <div class="flex-grow">
      <div class="flex items-center justify-between mb-1">
        <span class="font-bold text-on-surface font-body-md">{{ comment.author }}</span>
        <div class="flex gap-3 text-on-surface-variant font-label-md">
          <button class="hover:text-primary transition-primary flex items-center gap-1" @click="handleToggleReply">
            <span class="material-symbols-outlined text-[16px]">reply</span> 回复
          </button>
          <button v-if="comment.canDelete" class="hover:text-error transition-primary flex items-center gap-1" @click="$emit('delete', comment)">
            <span class="material-symbols-outlined text-[16px]">delete</span> 删除
          </button>
        </div>
      </div>
      <p class="text-outline text-label-md mb-2">{{ comment.time }}</p>
      <div class="bg-surface p-4 rounded-lg border border-outline-variant font-body-md text-on-surface leading-relaxed mb-4">
        <span v-if="comment.replyTo" class="text-primary-container font-medium mr-2">回复{{ comment.replyTo }}:</span>
        {{ comment.content }}
      </div>

      <!-- Nested Replies -->
      <div v-if="comment.children && comment.children.length > 0" class="ml-10 space-y-6 border-l-2 border-surface-container pl-4">
        <BBSCommentItem
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          :currentUserAvatar="currentUserAvatar"
          @delete="$emit('delete', $event)"
          @reply="$emit('reply', $event)"
        />
      </div>

      <!-- Reply Input Box -->
      <div v-if="showReplyInput" class="mt-4 ml-10 flex gap-3 items-start">
        <div class="w-8 h-8 rounded-full overflow-hidden border border-border flex-shrink-0">
          <img :src="currentUserAvatar || require('../assets/portrait.png')" class="w-full h-full object-cover">
        </div>
        <div class="flex-grow flex gap-2">
          <input
            v-model="replyText"
            class="flex-grow p-2 rounded-lg border border-border text-label-md bg-surface focus:border-primary-container outline-none"
            :placeholder="'回复 ' + comment.author"
            type="text"
            @keyup.enter="submitReply"
          >
          <button class="bg-primary-container/10 text-primary-container px-4 py-1.5 rounded-lg font-label-md text-label-md hover:bg-primary-container hover:text-white transition-primary" @click="submitReply">
            回复
          </button>
        </div>
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
    }
  },
  computed: {
    showReplyInput() {
      return this.replyState.activeId === this.comment.id
    },
  },
  methods: {
    handleToggleReply() {
      this.replyState.activeId = this.showReplyInput ? null : this.comment.id
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
