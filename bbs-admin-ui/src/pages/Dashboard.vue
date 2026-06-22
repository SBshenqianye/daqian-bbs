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

      <!-- Quick Stats -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div class="bg-container border border-border rounded-xl p-card-padding hover:shadow-sm transition-shadow">
          <div class="flex items-center justify-between mb-3">
            <span class="material-symbols-outlined text-[32px] text-primary">group</span>
            <span class="text-label-md text-on-surface-variant">用户管理</span>
          </div>
          <p class="font-headline-sm text-on-surface">管理注册用户</p>
          <p class="text-body-md text-secondary mt-1">查看、搜索、修改用户信息</p>
        </div>
        <div class="bg-container border border-border rounded-xl p-card-padding hover:shadow-sm transition-shadow">
          <div class="flex items-center justify-between mb-3">
            <span class="material-symbols-outlined text-[32px] text-tertiary-container">article</span>
            <span class="text-label-md text-on-surface-variant">帖子管理</span>
          </div>
          <p class="font-headline-sm text-on-surface">审核与管理帖子</p>
          <p class="text-body-md text-secondary mt-1">审核、删除、查看帖子详情</p>
        </div>
        <!-- <div class="bg-container border border-border rounded-xl p-card-padding hover:shadow-sm transition-shadow">
          <div class="flex items-center justify-between mb-3">
            <span class="material-symbols-outlined text-[32px] text-brand-blue">bar_chart</span>
            <span class="text-label-md text-on-surface-variant">统计概览</span>
          </div>
          <p class="font-headline-sm text-on-surface">数据统计分析</p>
          <p class="text-body-md text-secondary mt-1">按单位查看文章发布统计</p>
        </div> -->
        <div class="bg-container border border-border rounded-xl p-card-padding hover:shadow-sm transition-shadow">
          <div class="flex items-center justify-between mb-3">
            <span class="material-symbols-outlined text-[32px] text-rank-gold">workspace_premium</span>
            <span class="text-label-md text-on-surface-variant">积分排名</span>
          </div>
          <p class="font-headline-sm text-on-surface">单位活跃度排名</p>
          <p class="text-body-md text-secondary mt-1">当月/累计活跃度积分排名</p>
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
  name: 'Dashboard'
}
</script>
