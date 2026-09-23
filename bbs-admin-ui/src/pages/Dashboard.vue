<template>
  <div class="min-h-screen bg-surface">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-8">
      <!-- Welcome Hero -->
      <div class="bg-gradient-to-br from-primary to-primary-fixed-dim rounded-xl p-8 md:p-12 mb-8 text-white">
        <div class="flex items-center gap-4 mb-4">
          <span class="material-symbols-outlined text-[40px] text-white/90">admin_panel_settings</span>
          <div>
            <h1 class="font-headline-lg text-headline-lg text-on-primary">欢迎使用后台管理系统</h1>
            <p class="text-body-md text-on-primary/70 mt-1">大千智荟创新创意交流论坛 - 管理平台</p>
          </div>
        </div>
        <p class="text-body-lg text-on-primary/80 max-w-2xl">在这里您可以管理用户、文章、标签、社区等所有论坛资源。</p>
      </div>

      <!-- 数据概览（#7 真实统计） -->
      <div class="mb-8">
        <h2 class="font-headline-md text-headline-md text-on-surface mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">insights</span>
          数据概览
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <div v-for="card in overviewCards" :key="card.label"
               class="bg-container border border-border rounded-xl p-4 hover:shadow-sm transition-shadow">
            <div class="flex items-center justify-between mb-2">
              <span class="material-symbols-outlined text-[24px]" :class="card.iconColor">{{ card.icon }}</span>
            </div>
            <p class="font-headline-sm text-on-surface text-xl leading-tight">{{ formatNum(card.value) }}</p>
            <p class="text-label-sm text-on-surface-variant mt-1">{{ card.label }}</p>
            <p v-if="card.hint" class="text-caption-sm text-on-surface-variant/70 mt-0.5">{{ card.hint }}</p>
          </div>
        </div>
      </div>

      <!-- Quick Links -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <h2 class="font-headline-md text-headline-md text-on-surface mb-6 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">quicklink</span>
          快捷操作
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <router-link to="/user" class="flex items-center gap-3 p-4 bg-surface-container-low rounded-lg hover:bg-surface-container transition-colors border border-outline-variant/50 group">
            <span class="material-symbols-outlined text-primary text-[24px]">manage_accounts</span>
            <span class="font-body-md text-on-surface group-hover:text-primary transition-colors">用户管理</span>
          </router-link>
          <router-link to="/article" class="flex items-center gap-3 p-4 bg-surface-container-low rounded-lg hover:bg-surface-container transition-colors border border-outline-variant/50 group">
            <span class="material-symbols-outlined text-tertiary-container text-[24px]">rate_review</span>
            <span class="font-body-md text-on-surface group-hover:text-primary transition-colors">帖子管理</span>
          </router-link>
          <router-link to="/report" class="flex items-center gap-3 p-4 bg-surface-container-low rounded-lg hover:bg-surface-container transition-colors border border-outline-variant/50 group">
            <span class="material-symbols-outlined text-rank-gold text-[24px]">flag</span>
            <span class="font-body-md text-on-surface group-hover:text-primary transition-colors">举报管理</span>
            <span v-if="stats.reportPending > 0" class="ml-auto bg-error text-on-error text-xs px-2 py-[2px] rounded-full">{{ stats.reportPending }}</span>
          </router-link>
          <router-link to="/points" class="flex items-center gap-3 p-4 bg-surface-container-low rounded-lg hover:bg-surface-container transition-colors border border-outline-variant/50 group">
            <span class="material-symbols-outlined text-rank-gold text-[24px]">leaderboard</span>
            <span class="font-body-md text-on-surface group-hover:text-primary transition-colors">积分排名</span>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {
        userCount: 0,
        articleCount: 0,
        commentCount: 0,
        reportPending: 0,
        appealPending: 0,
        violationActive: 0,
        articlePending: 0,
        featuredCount: 0
      }
    }
  },
  computed: {
    overviewCards() {
      return [
        { label: '注册用户', value: this.stats.userCount, icon: 'group', iconColor: 'text-primary' },
        { label: '帖子总数', value: this.stats.articleCount, icon: 'article', iconColor: 'text-tertiary-container', hint: '待审 ' + this.stats.articlePending + ' · 精华 ' + this.stats.featuredCount },
        { label: '评论总数', value: this.stats.commentCount, icon: 'chat', iconColor: 'text-tertiary-container' },
        { label: '举报待审', value: this.stats.reportPending, icon: 'flag', iconColor: 'text-rank-gold' },
        { label: '申诉待审', value: this.stats.appealPending, icon: 'gavel', iconColor: 'text-rank-gold' },
        { label: '进行中违规', value: this.stats.violationActive, icon: 'gpp_bad', iconColor: 'text-error' }
      ]
    }
  },
  mounted() {
    this.loadStats()
  },
  methods: {
    formatNum(n) {
      const v = Number(n) || 0
      return v >= 10000 ? (v / 10000).toFixed(1).replace(/\.0$/, '') + 'w' : String(v)
    },
    loadStats() {
      this.postRequest('/admin/dashboard/counts', {}).then(resp => {
        if (resp && resp.code === 200 && resp.obj) {
          this.stats = Object.assign({}, this.stats, resp.obj)
        }
      }).catch(err => {
        console.warn('[Dashboard] load counts', err)
      })
    }
  }
}
</script>
