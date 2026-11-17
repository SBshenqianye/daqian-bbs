<template>
  <main :class="embedded ? '' : 'max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen'">
    <header v-if="!embedded" class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的积分记录</h1>
    </header>

    <div v-if="levelInfo" class="ui-card p-4 mb-4 flex items-center gap-4 flex-wrap">
      <div class="flex items-center gap-2.5 min-w-0">
        <span class="w-9 h-9 rounded-lg bg-amber-50 grid place-items-center shrink-0">
          <span class="material-symbols-outlined text-[19px] text-amber-600 filled-icon">emoji_events</span>
        </span>
        <div class="min-w-0">
          <p class="text-[14px] font-semibold text-on-surface">Lv.{{ levelInfo.level }}</p>
          <p class="text-[11.5px] text-outline">累计 {{ levelInfo.totalPoints }} 积分</p>
        </div>
      </div>
      <div class="flex-1 min-w-[140px]">
        <div class="h-1.5 bg-surface-container rounded-full overflow-hidden">
          <div class="h-full bg-primary rounded-full transition-all" :style="{ width: (levelInfo.totalPoints % 100) + '%' }"></div>
        </div>
        <p class="text-[10.5px] text-outline mt-1">距下一级还需 {{ 100 - (levelInfo.totalPoints % 100) }} 分</p>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="list.length > 0" class="space-y-2">
        <div v-for="(item, idx) in list" :id="'log-'+idx" :key="idx"
             class="ui-card p-3.5 flex items-center gap-3"
             :class="[(item.isReversed === 1 ? 'opacity-50' : ''), highlighted === 'log-'+idx ? 'ring-2 ring-primary bg-primary/10' : '']">
          <span
            class="w-8 h-8 rounded-lg grid place-items-center shrink-0"
            :class="item.pointsChange > 0 ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'"
          >
            <span class="material-symbols-outlined text-[17px]">{{ item.pointsChange > 0 ? 'add_circle' : 'remove_circle' }}</span>
          </span>
          <div class="flex-1 min-w-0">
            <a v-if="hasPair(item)" href="javascript:;" class="text-[13px] text-primary hover:underline truncate block"
               @click="jumpToPair(item)">
              {{ item.reason || '积分变动' }} {{ pairArrow(item) }}
            </a>
            <p v-else class="text-[13px] text-on-surface truncate">{{ item.reason || '积分变动' }}</p>
            <p class="ui-meta mt-1"><span class="material-symbols-outlined">schedule</span>{{ item.createTime }}</p>
          </div>
          <span class="text-[15px] font-bold shrink-0 tabnum" :class="item.pointsChange > 0 ? 'text-green-600' : 'text-red-600'">
            {{ item.pointsChange > 0 ? '+' : '' }}{{ item.pointsChange }}
          </span>
        </div>
      </div>
      <div v-else class="ui-empty">
        <span class="material-symbols-outlined">receipt_long</span>
        <p class="ui-empty-title">暂无积分记录</p>
        <p class="ui-empty-desc">发帖、回复和被采纳都会获得积分</p>
      </div>
      <div v-if="total > pageSize" class="ui-pager mt-4">
        <button class="ui-pager-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
        <span class="text-[12px] text-on-surface-variant px-1 tabnum">{{ currentPage }} / {{ Math.ceil(total / pageSize) }}</span>
        <button class="ui-pager-btn" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </template>
  </main>
</template>

<script>
import { getUser } from '@/utils/auth'

export default {
  name: 'BBSMyPointsLog',
  props: { embedded: { type: Boolean, default: false } },
  data() {
    return { loading: false, list: [], total: 0, currentPage: 1, pageSize: 20, levelInfo: null, highlighted: '', pendingPairTime: null }
  },
  mounted() {
    this.loadLevel()
    if (this.$route.query.highlightTime) this.pendingPairTime = this.$route.query.highlightTime
    this.loadList()
  },
  methods: {
    getUserId() { const user = getUser(); return user && user.id },
    async loadLevel() {
      const userId = this.getUserId()
      if (!userId) return
      try {
        const res = await this.getRequest(`/user/level?userId=${userId}`)
        if (res && res.code == 200) this.levelInfo = res.obj
      } catch (e) { /* ignore */ }
    },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/points/myLog', { userId, page: this.currentPage, size: this.pageSize })
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
          // 翻页后定位到目标记录（等 loading 关闭+DOM 渲染完）
          if (this.pendingPairTime) {
            const t = this.pendingPairTime
            this.pendingPairTime = null
            setTimeout(() => this.locateByTime(t), 300)
          }
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    hasPair(item) { return !!(item.undoAnchor || item.pairPage) },
    pairArrow(item) {
      // 撤销记录（扣分/扣回）→ ↩ 跳向原加分；原记录（加分）→ ↪ 跳向撤销
      return item.pointsChange < 0 ? ' ↩' : ' ↪'
    },
    jumpToPair(item) {
      if (item.undoAnchor) {
        const el = document.getElementById(item.undoAnchor)
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' })
          this.highlighted = item.undoAnchor
          setTimeout(() => { this.highlighted = '' }, 2000)
        }
      } else if (item.pairPage) {
        // 目标就在当前页，直接定位不弹窗
        if (item.pairPage === this.currentPage) {
          this.locateByTime(item.pairTime)
          return
        }
        this.$confirm(`配对记录在第 ${item.pairPage} 页，是否跳转？`, '跨页跳转', {
          confirmButtonText: '跳转', cancelButtonText: '取消', type: 'info'
        }).then(() => {
          this.pendingPairTime = item.pairTime
          this.currentPage = item.pairPage
          this.loadList()
        }).catch(() => {})
      }
    },
    locateByTime(time) {
      if (!time) return
      const idx = this.list.findIndex(r => r.createTime === time)
      if (idx >= 0) {
        const anchor = 'log-' + idx
        const el = document.getElementById(anchor)
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' })
          this.highlighted = anchor
          setTimeout(() => { this.highlighted = '' }, 2000)
        }
      }
    },
    changePage(p) { this.currentPage = p; this.loadList() }
  }
}
</script>
