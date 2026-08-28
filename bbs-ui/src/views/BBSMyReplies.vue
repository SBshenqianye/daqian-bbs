<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <!-- Page Title -->
    <header class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的互动</h1>
    </header>

    <!-- Tab 栏 -->
    <div class="flex items-center gap-1 mb-8 overflow-x-auto">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="px-5 py-2.5 text-sm font-semibold rounded-lg transition-all whitespace-nowrap shrink-0 flex items-center gap-2"
        :class="activeTab === tab.key
          ? 'bg-primary text-on-primary shadow-sm'
          : 'text-on-surface-variant hover:text-primary hover:bg-primary/5 bg-surface-container-low'"
        @click="switchTab(tab.key)"
      >
        <span class="material-symbols-outlined text-[18px]">{{ tab.icon }}</span>
        {{ tab.label }}
        <!-- 未读角标（互动消息分类：回复/评论我，独立于"消息通知"计数） -->
        <span
          v-if="tab.key === 'repliedToMe' && unreadInteraction > 0"
          class="min-w-[18px] h-[18px] px-1 bg-error rounded-full text-white text-[10px] font-bold flex items-center justify-center leading-none flex-shrink-0"
        >
          {{ unreadInteraction > 99 ? '99+' : unreadInteraction }}
        </span>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <!-- 我回复的列表 -->
    <template v-if="!loading && activeTab === 'myReplies'">
      <div v-if="myReplies.length > 0" class="space-y-gutter">
        <div
          v-for="(item, index) in myReplies"
          :key="'my-' + index"
          class="bg-container border border-outline-variant rounded-lg p-card-padding flex flex-col gap-3 relative group card-shadow hover:border-primary-container transition-colors cursor-pointer card-clickable"
          @click="goToArticle(item)"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-grow min-w-0">
              <h3 class="font-headline-sm text-headline-sm text-on-surface mb-1 truncate">
                {{ item.articleTitle || '未知帖子' }}
              </h3>
              <p class="font-body-md text-body-md text-on-surface-variant line-clamp-2" style="white-space: pre-line">
                {{ item.content }}
              </p>
            </div>
            <div v-if="item.articleImage" class="w-16 h-16 flex-shrink-0 rounded overflow-hidden">
              <img alt="Cover" class="w-full h-full object-cover" :src="normalizeFileUrl(item.articleImage)">
            </div>
          </div>
          <div class="flex items-center gap-4 font-label-md text-label-md text-on-surface-variant">
            <span class="flex items-center gap-1">
              <span class="material-symbols-outlined text-[16px]">schedule</span>
              {{ friendlyTime(item.time) }}
            </span>
            <span class="flex items-center gap-1">
              <span class="material-symbols-outlined text-[16px]">article</span>
              我的回复
            </span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-20">reply</span>
        <p class="font-body-lg text-body-lg">暂无回复记录</p>
        <p class="font-body-md text-body-md text-outline mt-1">去论坛参与讨论吧</p>
      </div>
    </template>

    <!-- 回复我的列表 -->
    <template v-if="!loading && activeTab === 'repliedToMe'">
      <div v-if="repliedToMe.length > 0" class="space-y-gutter">
        <div
          v-for="(item, index) in repliedToMe"
          :key="'to-' + index"
          class="bg-container border border-outline-variant rounded-lg p-card-padding flex gap-4 relative group card-shadow hover:border-primary-container transition-colors cursor-pointer card-clickable"
          @click="goToArticle(item)"
        >
          <!-- 对方头像 -->
          <img
            v-if="item.fromPortrait"
            alt="Avatar"
            class="w-10 h-10 rounded-full border border-outline-variant object-cover flex-shrink-0"
            :src="normalizeFileUrl(item.fromPortrait)"
            @error="$event.target.src = require('@/assets/portrait.png')"
          >
          <div v-else class="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center flex-shrink-0">
            <span class="material-symbols-outlined text-outline text-xl">person</span>
          </div>

          <div class="flex-grow min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-label-md text-label-md font-semibold text-on-surface truncate">
                {{ item.fromNickname || '匿名用户' }}
              </span>
              <span v-if="item.fromOrgName" class="font-body-sm text-body-sm text-outline truncate">
                {{ item.fromOrgName }}
              </span>
              <span class="text-outline ml-auto flex-shrink-0 font-body-sm text-body-sm">
                {{ friendlyTime(item.time) }}
              </span>
            </div>
            <p class="font-body-md text-body-md text-on-surface-variant mb-2">
              {{ item.replyRelation || '回复了你' }}
            </p>
            <p class="font-body-md text-body-md text-on-surface line-clamp-2" style="white-space: pre-line">
              {{ item.content }}
            </p>
            <p v-if="item.articleTitle" class="font-body-sm text-body-sm text-outline mt-2 truncate">
              帖子：{{ item.articleTitle }}
            </p>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-20">quickreply</span>
        <p class="font-body-lg text-body-lg">暂无回复消息</p>
        <p class="font-body-md text-body-md text-outline mt-1">发布的帖子还没有收到回复</p>
      </div>
    </template>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="mt-8 flex justify-center">
      <div class="flex gap-2">
        <button
          class="w-9 h-9 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
          :disabled="currentPage <= 1"
          @click="currentPage--; fetchData()"
        >
          <span class="material-symbols-outlined text-[20px]">chevron_left</span>
        </button>
        <button
          v-for="page in displayPages"
          :key="page"
          class="w-9 h-9 flex items-center justify-center rounded-md font-bold text-sm transition-all"
          :class="page === currentPage ? 'bg-primary text-white shadow-sm' : 'border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary'"
          @click="currentPage = page; fetchData()"
        >
          {{ page }}
        </button>
        <button
          class="w-9 h-9 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
          :disabled="currentPage >= totalPages"
          @click="currentPage++; fetchData()"
        >
          <span class="material-symbols-outlined text-[20px]">chevron_right</span>
        </button>
      </div>
    </div>
  </main>
</template>

<script>
import { getUser } from '@/utils/auth'
import { normalizeFileUrl, friendlyTime } from '@/utils/utils'
import notificationStore from '@/utils/notificationStore'

export default {
  name: 'BBSMyReplies',
  data() {
    return {
      loading: false,
      activeTab: 'myReplies',
      tabs: [
        { key: 'myReplies', label: '我回复的', icon: 'reply' },
        { key: 'repliedToMe', label: '回复我的', icon: 'quickreply' },
      ],
      myReplies: [],
      repliedToMe: [],
      total: 0,
      currentPage: 1,
      pageSize: 20,
    }
  },
  computed: {
    /** 互动消息未读数（全局通知 store，与"消息通知"分类相互独立） */
    unreadInteraction() {
      return notificationStore.count('interaction')
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    displayPages() {
      const pages = []
      const start = Math.max(1, this.currentPage - 2)
      const end = Math.min(this.totalPages, this.currentPage + 2)
      for (let i = start; i <= end; i++) {
        pages.push(i)
      }
      return pages
    },
    currentUserId() {
      const user = getUser()
      return user ? user.id : null
    },
  },
  mounted() {
    // 根据路由参数决定初始 Tab
    const tab = this.$route.query.tab
    if (tab === 'repliedToMe') {
      this.activeTab = 'repliedToMe'
      // 直接通过 URL 进入"回复我的"tab 时也标记已读
      this.$nextTick(() => this.markAsRead())
    }
    this.fetchData()
  },
  methods: {
    normalizeFileUrl,
    friendlyTime,

    switchTab(key) {
      this.activeTab = key
      this.currentPage = 1
      this.total = 0
      this.fetchData()
      // 切换到"回复我的"时标记互动消息分类已读（只清互动分类，不影响系统通知）
      if (key === 'repliedToMe' && this.unreadInteraction > 0) {
        this.markAsRead()
      }
    },

    fetchData() {
      if (this.activeTab === 'myReplies') {
        this.fetchMyReplies()
      } else {
        this.fetchRepliedToMe()
      }
    },

    fetchMyReplies() {
      if (!this.currentUserId) return
      this.loading = true
      this.getRequest(`/reply/myReplies?userId=${this.currentUserId}&page=${this.currentPage}&size=${this.pageSize}`).then(resp => {
        this.loading = false
        const data = resp && resp.obj
        this.myReplies = (data && data.list) || []
        this.total = (data && data.total) || 0
      }).catch(err => {
        console.warn('[BBSMyReplies] fetchMyReplies', err)
        this.loading = false
        this.myReplies = []
        this.total = 0
      })
    },

    fetchRepliedToMe() {
      if (!this.currentUserId) return
      this.loading = true
      this.getRequest(`/reply/repliedToMe?userId=${this.currentUserId}&page=${this.currentPage}&size=${this.pageSize}`).then(resp => {
        this.loading = false
        const data = resp && resp.obj
        this.repliedToMe = (data && data.list) || []
        this.total = (data && data.total) || 0
      }).catch(err => {
        console.warn('[BBSMyReplies] fetchRepliedToMe', err)
        this.loading = false
        this.repliedToMe = []
        this.total = 0
      })
    },

    markAsRead() {
      // 只把互动消息分类（reply/comment）标记为已读，
      // 系统通知（采纳/违规等）的未读计数不受影响
      notificationStore.markCategoryRead('interaction')
    },

    goToArticle(item) {
      if (!item || !item.articleId) return
      const query = {}
      // 带上 commentId 和 replyId 用于定位到具体评论/回复
      if (item.commentId) query.commentId = item.commentId
      if (item.replyId) query.replyId = item.replyId
      this.$router.push({
        name: 'BBSArticleDetails',
        params: { articleId: item.articleId },
        query,
      })
    },
  },
}
</script>
