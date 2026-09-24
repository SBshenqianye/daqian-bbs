<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的积分记录</h1>
    </header>

    <div v-if="levelInfo" class="bg-container border border-outline-variant rounded-lg p-card-padding mb-8 flex items-center gap-6">
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined text-[40px] text-primary">emoji_events</span>
        <div>
          <p class="font-headline-sm text-headline-sm text-on-surface">等级 {{ levelInfo.level }}</p>
          <p class="font-body-md text-body-md text-on-surface-variant">累计 {{ levelInfo.totalPoints }} 积分</p>
        </div>
      </div>
      <div class="flex-1 h-3 bg-surface-container-low rounded-full overflow-hidden">
        <div class="h-full bg-primary rounded-full transition-all" :style="{ width: (levelInfo.totalPoints % 100) + '%' }"></div>
      </div>
      <span class="font-body-sm text-body-sm text-on-surface-variant">距下一级还需 {{ 100 - (levelInfo.totalPoints % 100) }} 分</span>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="list.length > 0" class="space-y-gutter">
        <div v-for="(item, idx) in list" :id="'log-'+idx" :key="idx"
             class="bg-container border border-outline-variant rounded-lg p-card-padding"
             :class="[(item.isReversed === 1 ? 'opacity-50' : ''), highlighted === 'log-'+idx ? 'ring-2 ring-primary bg-primary/10' : '']">
          <div class="flex items-center justify-between">
            <div>
              <a v-if="hasPair(item)" href="javascript:;" class="font-body-md text-body-md text-primary hover:underline"
                 @click="jumpToPair(item)">
                {{ item.reason || '积分变动' }} {{ pairArrow(item) }}
              </a>
              <p v-else class="font-body-md text-body-md text-on-surface">{{ item.reason || '积分变动' }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ item.createTime }}</p>
            </div>
            <span class="font-headline-sm text-headline-sm" :class="item.pointsChange > 0 ? 'text-green-600' : 'text-red-600'">
              {{ item.pointsChange > 0 ? '+' : '' }}{{ item.pointsChange }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-20">receipt_long</span>
        <p class="font-body-lg text-body-lg">暂无积分记录</p>
      </div>
      <div v-if="total > pageSize" class="flex justify-center mt-8 gap-2">
        <button class="px-4 py-2 border rounded-lg text-body-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
        <span class="px-4 py-2 text-body-sm text-on-surface-variant">{{ currentPage }}/{{ Math.ceil(total / pageSize) }}</span>
        <button class="px-4 py-2 border rounded-lg text-body-sm" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </template>
  </main>
</template>

<script>
export default {
  name: 'BBSMyPointsLog',
  data() {
    return { loading: false, list: [], total: 0, currentPage: 1, pageSize: 20, levelInfo: null, highlighted: '', pendingPairTime: null }
  },
  mounted() {
    this.loadLevel()
    this.loadList()
  },
  methods: {
    getUserId() { return JSON.parse(localStorage.getItem('user') || '{}').id },
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
          // 翻页后定位到目标记录
          if (this.pendingPairTime) {
            this.$nextTick(() => this.locateByTime(this.pendingPairTime))
            this.pendingPairTime = null
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
