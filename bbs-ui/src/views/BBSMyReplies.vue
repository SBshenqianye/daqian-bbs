<template>
  <main :class="embedded ? '' : 'max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen'">
    <!-- Page Title -->
    <header v-if="!embedded" class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的互动</h1>
    </header>

    <!-- Tab 栏：与论坛筛选/排行榜同一套轻量胶囊 -->
    <div class="inline-flex items-center gap-1 mb-4 p-1 rounded-xl border border-border bg-container overflow-x-auto max-w-full">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="ui-tab shrink-0"
        :class="{ 'is-active': activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        <span class="material-symbols-outlined text-[16px]">{{ tab.icon }}</span>
        {{ tab.label }}
        <!-- 未读角标（互动消息分类：回复/评论我，独立于"消息通知"计数） -->
        <span
          v-if="tab.key === 'repliedToMe' && unreadInteraction > 0"
          class="min-w-[16px] h-4 px-1 bg-error rounded-full text-white text-[9.5px] font-bold flex items-center justify-center leading-none flex-shrink-0"
        >
          {{ unreadInteraction > 99 ? '99+' : unreadInteraction }}
        </span>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <!-- 我回复的列表 -->
    <template v-if="!loading && activeTab === 'myReplies'">
      <div v-if="myReplies.length > 0" class="space-y-2.5">
        <div
          v-for="(item, index) in myReplies"
          :key="'my-' + index"
          class="ui-card ui-card-hover p-4 flex flex-col gap-2 cursor-pointer"
          @click="goToArticle(item)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="flex-grow min-w-0">
              <h3 class="text-[14px] font-semibold text-on-surface leading-snug truncate">
                {{ item.articleTitle || '未知帖子' }}
              </h3>
              <p class="mt-1 text-[12.5px] leading-relaxed text-on-surface-variant line-clamp-2 whitespace-pre-line">
                {{ item.content }}
              </p>
            </div>
            <div v-if="item.articleImage" class="w-[64px] h-[56px] flex-shrink-0 rounded-lg overflow-hidden border border-outline-variant">
              <img alt="Cover" class="w-full h-full object-cover" :src="normalizeFileUrl(item.articleImage)">
            </div>
          </div>
          <div class="ui-meta">
            <span class="ui-meta-item"><span class="material-symbols-outlined">schedule</span>{{ friendlyTime(item.time) }}</span>
            <span class="text-outline-variant">·</span>
            <span class="ui-meta-item"><span class="material-symbols-outlined">reply</span>我的回复</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="ui-empty">
        <span class="material-symbols-outlined">reply</span>
        <p class="ui-empty-title">暂无回复记录</p>
        <p class="ui-empty-desc">去论坛参与讨论吧</p>
      </div>
    </template>

    <!-- 回复我的列表 -->
    <template v-if="!loading && activeTab === 'repliedToMe'">
      <div v-if="repliedToMe.length > 0" class="space-y-2.5">
        <div
          v-for="(item, index) in repliedToMe"
          :key="'to-' + index"
          class="ui-card ui-card-hover p-4 flex gap-3 cursor-pointer"
          @click="goToArticle(item)"
        >
          <!-- 对方头像 -->
          <img
            v-if="item.fromPortrait"
            alt="Avatar"
            class="w-9 h-9 rounded-full border border-outline-variant object-cover flex-shrink-0"
            :src="normalizeFileUrl(item.fromPortrait)"
            @error="$event.target.src = require('@/assets/portrait.png')"
          >
          <div v-else class="w-9 h-9 rounded-full bg-surface-container flex items-center justify-center flex-shrink-0">
            <span class="material-symbols-outlined text-outline text-[18px]">person</span>
          </div>

          <div class="flex-grow min-w-0">
            <div class="flex items-center gap-1.5 min-w-0">
              <span class="text-[12.5px] font-semibold text-on-surface truncate">{{ item.fromNickname || '匿名用户' }}</span>
              <span v-if="item.fromOrgName" class="text-[11px] text-outline truncate">· {{ item.fromOrgName }}</span>
              <span class="ui-meta ml-auto flex-shrink-0">{{ friendlyTime(item.time) }}</span>
            </div>
            <p class="text-[11.5px] text-on-surface-variant mt-1">
              <span class="material-symbols-outlined text-[13px] align-[-2px] mr-0.5 text-primary">quickreply</span>{{ item.replyRelation || '回复了你' }}
            </p>
            <p class="text-[12.5px] leading-relaxed text-on-surface mt-1.5 line-clamp-2 whitespace-pre-line">{{ item.content }}</p>
            <p v-if="item.articleTitle" class="ui-meta mt-1.5">
              <span class="material-symbols-outlined">article</span>
              <span class="truncate">{{ item.articleTitle }}</span>
            </p>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="ui-empty">
        <span class="material-symbols-outlined">quickreply</span>
        <p class="ui-empty-title">暂无回复消息</p>
        <p class="ui-empty-desc">发布的帖子还没有收到回复</p>
      </div>
    </template>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="ui-pager mt-4">
      <button class="ui-pager-btn" :disabled="currentPage <= 1" @click="currentPage--; fetchData()">
        <span class="material-symbols-outlined text-[16px]">chevron_left</span>
      </button>
      <button
        v-for="page in displayPages"
        :key="page"
        class="ui-pager-btn"
        :class="{ 'is-active': page === currentPage }"
        @click="currentPage = page; fetchData()"
      >{{ page }}</button>
      <button class="ui-pager-btn" :disabled="currentPage >= totalPages" @click="currentPage++; fetchData()">
        <span class="material-symbols-outlined text-[16px]">chevron_right</span>
      </button>
      <span class="text-[11.5px] text-outline ml-1 tabnum">{{ currentPage }} / {{ totalPages }}</span>
    </div>
  </main>
</template>

<script>
import { getUser } from '@/utils/auth'
import { normalizeFileUrl, friendlyTime } from '@/utils/utils'
import notificationStore from '@/utils/notificationStore'

export default {
  name: 'BBSMyReplies',
  props: { embedded: { type: Boolean, default: false } },
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
    // 根据路由参数决定初始 Tab（内嵌在个人中心时用 sub 参数，避免与外层 tab 冲突）
    const sub = this.$route.query.sub
    const tab = this.$route.query.tab
    const initial = sub === 'repliedToMe' || tab === 'repliedToMe' ? 'repliedToMe' : 'myReplies'
    this.activeTab = initial
    if (initial === 'repliedToMe') {
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
