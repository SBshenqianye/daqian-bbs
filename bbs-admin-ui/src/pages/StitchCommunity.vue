<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">communities</span>
            社区管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理论坛社区列表</p>
        </div>
      </div>

      <!-- Search -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex-1 min-w-[200px]">
            <div class="relative">
              <span class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline text-[18px]">search</span>
              <input v-model="searchInfo" class="w-full pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="搜索社区名称" @keyup.enter="handleSearch">
            </div>
          </div>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleSearch">
            <span class="material-symbols-outlined text-[18px]">search</span>
            搜索
          </button>
        </div>
      </div>

      <!-- Communities Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">ID</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[120px]">社区名称</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[150px]">社区介绍</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">社区人数</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">封面</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">状态</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[170px]">添加时间</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[160px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in communities" :key="item.communityId || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4 font-body-md text-on-surface">{{ item.communityId }}</td>
                <td class="p-4 font-body-md text-on-surface font-medium max-w-[200px] truncate" :title="item.communityName">{{ item.communityName }}</td>
                <td class="p-4 font-body-md text-on-surface-variant max-w-[200px] truncate" :title="item.communityIntroduce">{{ item.communityIntroduce || '-' }}</td>
                <td class="p-4 font-body-md text-on-surface">{{ item.communityUserNum || 0 }}</td>
                <td class="p-4">
                  <img v-if="item.communityImage" class="w-12 h-12 rounded-lg object-cover border border-outline-variant" :src="item.communityImage" alt="">
                  <span v-else class="text-on-surface-variant text-body-md">-</span>
                </td>
                <td class="p-4">
                  <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-medium" :class="item.enable === 0 ? 'bg-green-50 text-green-700' : 'bg-error-container text-error'">
                    <span class="w-1.5 h-1.5 rounded-full" :class="item.enable === 0 ? 'bg-green-500' : 'bg-error'"></span>
                    {{ item.enable === 0 ? '活跃' : '禁用' }}
                  </span>
                </td>
                <td class="p-4 font-body-md text-on-surface-variant">{{ item.createTime }}</td>
                <td class="p-4">
                  <div class="flex items-center gap-2">
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-warning bg-warning/5 rounded hover:bg-warning/10 transition-colors" @click="updateCommunityStatus(item.communityId)">
                      <span class="material-symbols-outlined text-[14px]">toggle_on</span>
                      编辑状态
                    </button>
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="deleteCommunity(item.communityId)">
                      <span class="material-symbols-outlined text-[14px]">delete</span>
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="communities.length === 0">
                <td colspan="8" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">groups</span>
                    <p class="text-body-md">暂无社区数据</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StitchCommunity',
  data() {
    return { searchInfo: '', communities: [] }
  },
  mounted() { this.getAllCommunity() },
  methods: {
    getAllCommunity() {
      this.getRequest('/admin/getAllCommunity', 'all').then(resp => {
        if (resp) this.communities = resp.obj
      })
    },
    handleSearch() {
      if (this.searchInfo === '') this.getAllCommunity()
      else {
        this.getRequest('/admin/getCommunityByKeywords', this.searchInfo).then(resp => {
          if (resp) this.communities = resp.obj
        })
      }
    },
    updateCommunityStatus(communityId) {
      this.$confirm('确定修改该社区状态吗？', '提示', { type: 'warning' }).then(() => {
        this.putRequest(`/admin/updateCommunityStatus/${communityId}`).then(() => {
          this.getAllCommunity()
          this.$message.success('修改成功！')
        })
      }).catch(() => {})
    },
    deleteCommunity(communityId) {
      this.$confirm('确定删除该社区吗？', '提示', { type: 'warning' }).then(() => {
        this.deleteRequest('/admin/deleteCommunityByCommunityId', communityId).then(resp => {
          if (resp) { this.getAllCommunity(); this.$message.success('删除成功！') }
        })
      }).catch(() => {})
    }
  }
}
</script>
