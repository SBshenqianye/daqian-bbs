<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center mb-6">
        <button class="inline-flex items-center gap-1 px-3 py-1.5 text-primary hover:bg-primary/5 rounded-lg transition-colors font-body-md mr-4" @click="goBack">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          返回
        </button>
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface">积分排名详情</h1>
        </div>
      </div>

      <!-- Tabs -->
      <div class="bg-container border border-border rounded-xl overflow-hidden mb-6">
        <div class="flex border-b border-border">
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative" :class="activeTab === '01' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'" @click="switchTab('01')">
            当月排名
            <span v-if="activeTab === '01'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative" :class="activeTab === '02' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'" @click="switchTab('02')">
            累计排名
            <span v-if="activeTab === '02'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-12">
          <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>

        <!-- Detail List -->
        <div v-else class="p-card-padding">
          <div v-if="rankList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">leaderboard</span>
            <p class="text-body-md">暂无排名数据</p>
          </div>
          <div v-else class="space-y-3">
            <div v-for="(item, index) in rankList" :key="item.orgNo || index" class="flex items-center gap-4 p-4 bg-surface-container-low rounded-lg border border-outline-variant/50 hover:border-primary/30 transition-all">
              <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-[13px]" :class="getRankClass(index + 1)">
                {{ index + 1 }}
              </div>
              <div class="flex-1">
                <span class="font-headline-sm text-headline-sm" :class="{ 'text-primary cursor-pointer hover:underline': item.isSelf === 1 }" @click="onRowClick(item)">{{ item.orgName }}</span>
              </div>
              <div class="flex items-center gap-4 text-body-md text-on-surface-variant">
                <span>{{ activeTab === '01' ? '发帖' : '累计发帖' }}: {{ item.posts }}</span>
                <span>{{ activeTab === '01' ? '回帖' : '累计回帖' }}: {{ item.replies }}</span>
              </div>
              <div class="text-right">
                <p class="font-bold text-primary text-lg">{{ item.points }}</p>
              </div>
              <span class="text-label-md text-on-surface-variant bg-surface-variant px-2 py-0.5 rounded-full">#{{ item.rankNum }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BBSPointsDetail',
  data() {
    return {
      activeTab: '01',
      orgNo: '',
      rankList: [],
      loading: false
    }
  },
  mounted() {
    this.orgNo = this.$route.query.orgNo || ''
    this.activeTab = this.$route.query.rankType || '01'
    this.fetchRank()
  },
  watch: {
    '$route.query'() {
      this.orgNo = this.$route.query.orgNo || ''
      this.activeTab = this.$route.query.rankType || '01'
      this.fetchRank()
    }
  },
  methods: {
    goBack() {
      this.$router.push('/points')
    },
    switchTab(tab) {
      this.activeTab = tab
      this.fetchRank()
    },
    fetchRank() {
      this.loading = true
      this.postRequest('/common/pointsRank', { rankType: this.activeTab, orgNo: this.orgNo }).then(resp => {
        this.loading = false
        const list = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
          : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
          : (resp && resp.data && Array.isArray(resp.data)) ? resp.data
          : Array.isArray(resp) ? resp : []
        this.rankList = list
      }).catch(() => { this.loading = false; this.rankList = [] })
    },
    onRowClick(row) {
      if (row.isSelf === 1 && row.orgNo) {
        this.$router.push({
          path: '/points-detail',
          query: { orgNo: row.orgNo, rankType: this.activeTab }
        })
      }
    },
    getRankClass(rank) {
      if (rank === 1) return 'bg-rank-gold text-white shadow-sm'
      if (rank === 2) return 'bg-rank-silver text-white shadow-sm'
      if (rank === 3) return 'bg-rank-bronze text-white shadow-sm'
      return 'bg-surface-variant text-on-surface-variant'
    }
  }
}
</script>
