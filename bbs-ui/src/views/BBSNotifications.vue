<template>
  <main class="max-w-4xl mx-auto px-page-margin-mobile md:px-page-margin-desktop py-12 bg-surface min-h-screen">
    <!-- Page Header -->
    <header class="mb-8 flex items-center justify-between">
      <div>
        <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">notifications</span>
          消息通知
        </h1>
        <p v-if="unreadSystem > 0" class="text-body-md text-on-surface-variant mt-1">你有 {{ unreadSystem }} 条未读消息</p>
      </div>
      <button
        v-if="unreadSystem > 0"
        class="px-4 py-2 text-sm text-primary hover:bg-primary/5 rounded-lg transition-colors"
        @click="markAllRead"
      >
        全部已读
      </button>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <!-- Notification List -->
    <template v-else>
      <div v-if="notifications.length > 0" class="space-y-3">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="bg-container border rounded-lg p-4 flex items-start gap-4 transition-colors cursor-pointer hover:shadow-sm"
          :class="item.isRead === 0 ? 'border-primary/30 bg-primary/5' : 'border-outline-variant'"
          @click="handleNotificationClick(item)"
        >
          <!-- Icon -->
          <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0" :class="getIconBg(item.type)">
            <span class="material-symbols-outlined text-[20px]" :class="getIconColor(item.type)">{{ getIcon(item.type) }}</span>
          </div>
          <!-- Content -->
          <div class="flex-grow min-w-0">
            <p class="font-body-md text-body-md" :class="item.isRead === 0 ? 'text-on-surface font-medium' : 'text-on-surface-variant'">
              {{ item.title }}
            </p>
            <div class="flex items-center gap-3 mt-1.5">
              <span class="font-label-sm text-label-sm text-outline">{{ getTypeLabel(item.type) }}</span>
              <span class="font-label-sm text-label-sm text-outline">{{ friendlyTime(item.createTime) }}</span>
            </div>
          </div>
          <!-- Unread dot -->
          <div v-if="item.isRead === 0" class="w-2.5 h-2.5 rounded-full bg-primary flex-shrink-0 mt-2"></div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-7xl mb-4 opacity-15">notifications_none</span>
        <p class="font-headline-sm text-headline-sm mb-2">暂无系统通知</p>
        <p class="font-body-md text-body-md">帖子被采纳、举报核实或产生违规记录等系统事件时，你会在这里收到通知；回复/评论提醒请前往"回复我的"</p>
      </div>
    </template>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="mt-8 flex justify-center">
      <div class="flex gap-2">
        <button
          class="w-9 h-9 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
          :disabled="currentPage <= 1"
          @click="currentPage--; fetchList()"
        >
          <span class="material-symbols-outlined text-[20px]">chevron_left</span>
        </button>
        <button
          v-for="page in displayPages"
          :key="page"
          class="w-9 h-9 flex items-center justify-center rounded-md font-bold text-sm transition-all"
          :class="page === currentPage ? 'bg-primary text-white shadow-sm' : 'border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary'"
          @click="currentPage = page; fetchList()"
        >
          {{ page }}
        </button>
        <button
          class="w-9 h-9 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
          :disabled="currentPage >= totalPages"
          @click="currentPage++; fetchList()"
        >
          <span class="material-symbols-outlined text-[20px]">chevron_right</span>
        </button>
      </div>
    </div>
  </main>
</template>

<script>
import { getUser } from '@/utils/auth'
import { friendlyTime } from '@/utils/utils'
import notificationStore from '@/utils/notificationStore'

export default {
  name: 'BBSNotifications',
  data() {
    return {
      loading: false,
      notifications: [],
      total: 0,
      currentPage: 1,
      pageSize: 20,
    }
  },
  computed: {
    /** 系统通知未读数（全局通知 store；互动消息的未读在"回复我的"，与本页互不影响） */
    unreadSystem() {
      return notificationStore.count('system')
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    displayPages() {
      const pages = []
      const start = Math.max(1, this.currentPage - 2)
      const end = Math.min(this.totalPages, this.currentPage + 2)
      for (let i = start; i <= end; i++) pages.push(i)
      return pages
    },
    currentUserId() {
      const user = getUser()
      return user ? user.id : null
    },
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    friendlyTime,
    fetchList() {
      if (!this.currentUserId) return
      this.loading = true
      // 本页只展示"系统通知"分类；互动消息（回复/评论我）在"回复我的"页面
      this.getRequest(`/notification/list?userId=${this.currentUserId}&category=system&page=${this.currentPage}&size=${this.pageSize}`).then(resp => {
        this.loading = false
        const data = resp && resp.obj
        this.notifications = (data && data.records) || []
        this.total = (data && data.total) || 0
        // 进入通知页面时只标记系统通知分类为已读，不影响"回复我的"的互动未读
        this.markAllRead()
      }).catch(() => {
        this.loading = false
        this.notifications = []
        this.total = 0
      })
    },
    markAllRead() {
      if (!this.currentUserId) return
      // 只清系统通知分类（与页面内容一致）
      notificationStore.markCategoryRead('system')
      // 标记本地列表为已读（乐观更新，store.refresh 后会与服务端对齐）
      this.notifications.forEach(n => { n.isRead = 1 })
    },
    /**
     * 通知点击跳转：按通知 type 分发（type 决定"谁收到的、用来干什么"），
     * relatedType/relatedId 只作为内容锚点。不同类型互不干扰。
     */
    async handleNotificationClick(item) {
      switch (item.type) {
        case 'adopt_pending':
          // 版主/超管：跳管理端采纳审批页（生产同 nginx 同域，管理端为 hash 路由）
          window.open('/bbs-admin/#/approve-adopt', '_blank')
          return
        case 'report_pending':
          // 超管：跳管理端举报管理页（同 adopt_pending 跳管理端模式）
          window.open('/bbs-admin/#/report', '_blank')
          return
        case 'appeal_review':
          this.$router.push('/my-appeals')
          return
        case 'report_confirmed':
          this.$router.push('/my-reports')
          return
        case 'violation':
        case 'post_restricted':
          this.$router.push('/my-violations')
          return
      }

      // 其余类型（reply/comment/adopt/adopt_rejected/hot_bonus/suggestion_adopted）
      // 跳转关联文章详情，并尽量带上 commentId/replyId 定位到具体楼层
      if (item.relatedType === 'article' && item.relatedId) {
        this.goArticle(item.relatedId)
        return
      }
      if ((item.relatedType === 'reply' || item.relatedType === 'comment') && item.relatedId) {
        try {
          const resp = await this.getRequest(`/notification/resolveTarget?relatedType=${item.relatedType}&relatedId=${item.relatedId}`)
          const target = resp && resp.obj
          if (target && target.articleId) {
            this.goArticle(target.articleId, target.commentId, target.replyId)
            return
          }
        } catch (e) { /* 解析失败走兜底 */ }
      }
      // 兜底：无法定位时不再误跳其他页面，停留在当前列表
    },
    /** 跳转文章详情（可带评论/回复定位参数） */
    goArticle(articleId, commentId, replyId) {
      const query = {}
      if (commentId) query.commentId = commentId
      if (replyId) query.replyId = replyId
      this.$router.push({ name: 'BBSArticleDetails', params: { articleId }, query })
    },
    getIcon(type) {
      const icons = {
        reply: 'quickreply',
        comment: 'comment',
        adopt: 'check_circle',
        adopt_pending: 'pending',
        adopt_rejected: 'cancel',
        suggestion_adopted: 'lightbulb',
        hot_bonus: 'local_fire_department',
        violation: 'gavel',
        post_restricted: 'block',
        report_confirmed: 'flag',
        report_pending: 'flag',
        appeal_review: 'assignment',
      }
      return icons[type] || 'notifications'
    },
    getIconBg(type) {
      const map = {
        reply: 'bg-blue-50',
        comment: 'bg-purple-50',
        adopt: 'bg-green-50',
        adopt_pending: 'bg-yellow-50',
        adopt_rejected: 'bg-red-50',
        suggestion_adopted: 'bg-amber-50',
        hot_bonus: 'bg-orange-50',
        violation: 'bg-red-50',
        post_restricted: 'bg-gray-100',
        report_confirmed: 'bg-rose-50',
        report_pending: 'bg-yellow-50',
        appeal_review: 'bg-indigo-50',
      }
      return map[type] || 'bg-gray-50'
    },
    getIconColor(type) {
      const map = {
        reply: 'text-blue-600',
        comment: 'text-purple-600',
        adopt: 'text-green-600',
        adopt_pending: 'text-yellow-600',
        adopt_rejected: 'text-red-500',
        suggestion_adopted: 'text-amber-600',
        hot_bonus: 'text-orange-600',
        violation: 'text-red-600',
        post_restricted: 'text-gray-600',
        report_confirmed: 'text-rose-600',
        report_pending: 'text-yellow-600',
        appeal_review: 'text-indigo-600',
      }
      return map[type] || 'text-gray-600'
    },
    getTypeLabel(type) {
      const labels = {
        reply: '回复',
        comment: '评论',
        adopt: '采纳',
        adopt_pending: '待审批',
        adopt_rejected: '采纳拒绝',
        suggestion_adopted: '建议采纳',
        hot_bonus: '热度奖励',
        violation: '违规',
        post_restricted: '帖子限制',
        report_confirmed: '举报确认',
        report_pending: '举报待审',
        appeal_review: '申诉审核',
      }
      return labels[type] || '通知'
    },
  },
}
</script>
