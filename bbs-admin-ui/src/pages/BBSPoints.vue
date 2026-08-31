<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-rank-gold">leaderboard</span>
            积分排名
          </h1>
          <p class="text-body-md text-secondary mt-1">{{ activeMode === 'org' ? '查看单位活跃度排名' : '查看个人积分排名' }}</p>
        </div>
        <div class="flex items-center gap-2">
          <el-button type="success" plain size="small" :loading="exporting" @click="handleExport">
            <span class="material-symbols-outlined text-[16px] mr-1" style="vertical-align: text-bottom;">download</span>
            导出 Excel
          </el-button>
          <el-button type="primary" plain size="small" @click="$router.push('/unitManage')">
            <span class="material-symbols-outlined text-[16px] mr-1" style="vertical-align: text-bottom;">tune</span>
            配置排名单位
          </el-button>
        </div>
      </div>

      <!-- Mode Tabs（单位排名 / 个人排名） -->
      <div class="bg-container border border-border rounded-xl overflow-hidden mb-4">
        <div class="flex border-b border-border">
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative"
            :class="activeMode === 'org' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'"
            @click="switchMode('org')">
            单位排名
            <span v-if="activeMode === 'org'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative"
            :class="activeMode === 'personal' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'"
            @click="switchMode('personal')">
            个人排名
            <span v-if="activeMode === 'personal'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
        </div>

        <!-- Time Range Filter -->
        <div class="flex flex-wrap items-center gap-2 px-5 py-3 border-b border-outline-variant/30 bg-surface-container-lowest">
          <span class="text-sm text-on-surface-variant font-medium">时间：</span>
          <el-radio-group v-model="activeTimeKey" size="small" @change="onTimeChange">
            <el-radio-button label="month">本月</el-radio-button>
            <el-radio-button label="quarter">本季</el-radio-button>
            <el-radio-button label="year">本年</el-radio-button>
            <el-radio-button label="all">全部</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="customDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            size="small"
            class="time-range-picker"
            @change="onCustomDateChange"
          />
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-12">
          <i class="el-icon-loading text-2xl text-primary"></i>
          <span class="ml-2 text-on-surface-variant text-sm">加载中...</span>
        </div>

        <!-- ====== 单位排名 ====== -->
        <template v-if="!loading && activeMode === 'org'">
          <div v-if="rankList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">leaderboard</span>
            <p class="text-body-md">暂无排名数据</p>
          </div>
          <div v-else class="p-card-padding">
            <div class="space-y-3">
              <div v-for="(item, index) in rankList" :key="item.orgNo || index"
                class="flex items-center gap-4 p-4 bg-surface-container-low rounded-lg border border-outline-variant/50 hover:border-primary/30 transition-all"
                :class="{ 'border-l-3 border-l-primary bg-primary/[0.03]': item.isSelf === 1 }">
                <div class="flex-shrink-0 w-9 h-9 flex items-center justify-center rounded-full font-bold text-[14px]" :class="getRankClass(index + 1)">
                  {{ index + 1 }}
                </div>
                <div class="flex-1 min-w-0">
                  <span class="font-headline-sm text-headline-sm text-on-surface"
                    :class="{ 'text-primary cursor-pointer hover:underline': item.isSelf === 1 }"
                    @click="onRowClick(item)">{{ item.orgName }}</span>
                </div>
                <div class="flex items-center gap-6 text-body-md text-on-surface-variant">
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">edit_note</span>
                    发帖: {{ item.posts }}
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">chat_bubble</span>
                    回帖: {{ item.replies }}
                  </span>
                </div>
                <div class="text-right">
                  <p class="text-[11px] text-on-surface-variant font-medium">活跃度</p>
                  <p class="text-xl font-bold text-primary">{{ item.points }}</p>
                </div>
                <span class="text-label-md text-on-surface-variant bg-surface-variant px-2 py-0.5 rounded-full">#{{ item.rankNum }}</span>
              </div>
            </div>
          </div>
        </template>

        <!-- ====== 个人排名 ====== -->
        <template v-if="!loading && activeMode === 'personal'">
          <div v-if="personalList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">person_off</span>
            <p class="text-body-md">暂无个人排名数据</p>
          </div>
          <div v-else class="p-card-padding">
            <div class="space-y-2">
              <div v-for="(item, i) in personalList" :key="item.userId"
                class="flex items-center gap-4 p-4 bg-surface-container-lowest border border-outline-variant rounded-lg hover:shadow-sm transition-all"
                :class="currentUserId && item.userId === currentUserId ? 'border-l-4 border-l-primary bg-primary/[0.04]' : ''">
                <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-sm" :class="getRankClass(i + 1)">
                  {{ i + 1 }}
                </div>
                <div class="w-10 h-10 rounded-full bg-surface-variant flex items-center justify-center shrink-0 overflow-hidden border border-outline-variant">
                  <img v-if="item.portrait" :src="item.portrait" class="w-full h-full object-cover" @error="$event.target.style.display='none'" />
                  <span v-else class="material-symbols-outlined text-on-surface-variant">person</span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-bold text-on-surface text-sm truncate">{{ item.nickName }}</p>
                  <p class="text-xs text-on-surface-variant truncate">{{ item.orgName }}</p>
                </div>
                <div class="text-right shrink-0">
                  <p class="text-xs text-on-surface-variant">发帖 {{ item.posts }} / 回帖 {{ item.replies }}</p>
                  <p class="text-xl font-bold text-primary">{{ item.points }} <span class="text-sm font-normal text-on-surface-variant">分</span></p>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Detail Dialog（单位下钻） -->
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[8vh]" @click.self="detailVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-3xl rounded-xl shadow-2xl overflow-hidden max-h-[80vh] flex flex-col">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant shrink-0">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">leaderboard</span>
              详情 - {{ detailOrgName }}
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="detailVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="flex-1 min-h-0 overflow-y-auto p-5">
            <div v-if="detailLoading" class="flex items-center justify-center py-12">
              <i class="el-icon-loading text-2xl text-primary"></i>
            </div>
            <div v-else>
              <div v-if="detailList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
                <span class="material-symbols-outlined text-[48px] opacity-20">leaderboard</span>
                <p class="text-body-md">暂无数据</p>
              </div>
              <div v-else class="space-y-3">
                <div v-for="(item, index) in detailList" :key="item.orgNo || index"
                  class="flex items-center gap-4 p-4 bg-surface-container-low rounded-lg border border-outline-variant/50">
                  <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-[13px] bg-surface-variant text-on-surface-variant">{{ index + 1 }}</div>
                  <div class="flex-1">
                    <span class="font-headline-sm text-headline-sm text-on-surface"
                      :class="{ 'text-primary cursor-pointer': item.isSelf === 1 }"
                      @click="onDetailRowClick(item)">{{ item.orgName }}</span>
                  </div>
                  <div class="flex items-center gap-4 text-body-md text-on-surface-variant">
                    <span>发帖: {{ item.posts }}</span>
                    <span>回帖: {{ item.replies }}</span>
                  </div>
                  <div class="text-right">
                    <p class="font-bold text-primary text-lg">{{ item.points }}</p>
                  </div>
                  <span class="text-label-md text-on-surface-variant">#{{ item.rankNum }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest shrink-0">
            <el-button size="small" @click="detailVisible = false">关闭</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const TIME_RANGES = {
  month:   () => { const n = new Date(), y = n.getFullYear(), m = n.getMonth(); return { start: `${y}-${p(m+1)}-01`, end: `${y}-${p(m+1)}-${p(new Date(y,m+1,0).getDate())}` } },
  quarter: () => { const n = new Date(), y = n.getFullYear(), m = n.getMonth(), qs = Math.floor(m/3)*3, qe = qs+2; return { start: `${y}-${p(qs+1)}-01`, end: `${y}-${p(qe+1)}-${p(new Date(y,qe+1,0).getDate())}` } },
  year:    () => { const y = new Date().getFullYear(); return { start: `${y}-01-01`, end: `${y}-12-31` } },
  all:     () => ({ start: '2000-01-01', end: new Date().toISOString().slice(0,10) }),
}
function p(n) { return String(n).padStart(2, '0') }

export default {
  name: 'BBSPoints',
  data() {
    return {
      activeMode: 'org',     // 'org' | 'personal'
      activeTimeKey: 'month',
      customDateRange: null,
      timeRange: TIME_RANGES.month(),
      // 单位排名
      rankList: [],
      loading: false,
      // 个人排名
      personalList: [],
      currentUserId: null,
      // 导出
      exporting: false,
      // 下钻弹窗
      detailVisible: false,
      detailOrgNo: '',
      detailOrgName: '',
      detailList: [],
      detailLoading: false,
    }
  },
  mounted() {
    this.currentUserId = this.getCurrentUserId()
    this.fetchData()
  },
  methods: {
    getCurrentUserId() {
      try {
        const raw = sessionStorage.getItem('user')
        if (raw) { const u = JSON.parse(raw); return u.id || null }
      } catch (e) { /* ignore */ }
      return null
    },

    // ── Mode / Time ──

    switchMode(mode) {
      this.activeMode = mode
      this.fetchData()
    },

    onTimeChange() {
      this.customDateRange = null
      if (TIME_RANGES[this.activeTimeKey]) {
        this.timeRange = TIME_RANGES[this.activeTimeKey]()
      }
      this.fetchData()
    },

    onCustomDateChange(val) {
      if (val && val.length === 2) {
        this.activeTimeKey = 'custom'
        this.timeRange = { start: val[0], end: val[1] }
        this.fetchData()
      }
    },

    // ── Data Fetch ──

    fetchData() {
      if (this.activeMode === 'org') {
        this.fetchOrgRank()
      } else {
        this.fetchPersonalRank()
      }
    },

    fetchOrgRank() {
      this.loading = true
      this.postRequest('/common/pointsRank', {
        rankType: '01',
        orgNo: '',
        startTime: this.timeRange.start,
        endTime: this.timeRange.end,
      }).then(resp => {
        this.loading = false
        this.rankList = this.parseList(resp)
      }).catch(() => { this.loading = false; this.rankList = [] })
    },

    fetchPersonalRank() {
      this.loading = true
      this.postRequest('/common/personalPointsRank', {
        startTime: this.timeRange.start,
        endTime: this.timeRange.end,
        currentUserId: this.currentUserId,
        size: 50,
      }).then(resp => {
        this.loading = false
        const data = (resp && resp.obj) || resp
        this.personalList = (data && data.list) || []
      }).catch(() => { this.loading = false; this.personalList = [] })
    },

    parseList(resp) {
      return (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
        : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
        : Array.isArray(resp) ? resp : []
    },

    // ── Drill Down ──

    onRowClick(row) {
      if (row.isSelf === 1 && row.orgNo) {
        this.detailOrgNo = row.orgNo
        this.detailOrgName = row.orgName || ''
        this.detailVisible = true
        this.$nextTick(() => this.fetchDetail())
      }
    },

    fetchDetail() {
      if (!this.detailOrgNo) return
      this.detailLoading = true
      this.postRequest('/common/pointsRank', {
        rankType: '01',
        orgNo: this.detailOrgNo,
        startTime: this.timeRange.start,
        endTime: this.timeRange.end,
      }).then(resp => {
        this.detailLoading = false
        this.detailList = this.parseList(resp)
      }).catch(() => { this.detailLoading = false; this.detailList = [] })
    },

    onDetailRowClick(row) {
      if (row.isSelf === 1 && row.orgNo) {
        this.detailOrgNo = row.orgNo
        this.detailOrgName = row.orgName || ''
        this.fetchDetail()
      }
    },

    // ── Export ──

    handleExport() {
      this.exporting = true
      this.downloadFile('/admin/points/export', {
        rankType: this.activeMode === 'org' ? '01' : '02',
        startTime: this.timeRange.start,
        endTime: this.timeRange.end,
      }).then(resp => {
        this.exporting = false
        const blob = resp instanceof Blob ? resp : new Blob([resp])
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `积分排名_${this.activeMode === 'org' ? '单位' : '个人'}_${this.timeRange.start}_${this.timeRange.end}.xlsx`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        window.URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      }).catch(err => {
        this.exporting = false
        console.error('[Export] download failed', err)
        this.$message.error('导出失败，请稍后重试')
      })
    },

    // ── Helpers ──

    getRankClass(rank) {
      if (rank === 1) return 'bg-rank-gold text-white shadow-sm'
      if (rank === 2) return 'bg-rank-silver text-white shadow-sm'
      if (rank === 3) return 'bg-rank-bronze text-white shadow-sm'
      return 'bg-surface-variant text-on-surface-variant'
    },
  },
}
</script>

<style scoped>
.time-range-picker {
  width: 340px;
  margin-left: 8px;
}
.time-range-picker >>> .el-range__separator {
  margin: 0 4px;
}
</style>
