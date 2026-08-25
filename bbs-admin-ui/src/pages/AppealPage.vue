<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-blue-600">assignment</span>
            申诉管理
          </h1>
          <p class="text-body-md text-secondary mt-1">审核用户申诉</p>
        </div>
      </div>

      <!-- Filter -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center gap-3">
          <select v-model="filterStatus" class="px-3 py-2 bg-surface border border-outline-variant rounded-lg text-body-sm focus:border-primary outline-none">
            <option value="">全部状态</option>
            <option value="pending">待审核</option>
            <option value="accepted">已通过</option>
            <option value="rejected">已驳回</option>
          </select>
          <button class="px-3 py-2 bg-primary-container text-on-primary-container rounded-lg hover:opacity-90 text-body-sm" @click="loadList">查询</button>
        </div>
      </div>

      <!-- List -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无申诉记录</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">ID</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">用户</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">类型</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">内容</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">状态</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.id" class="hover:bg-surface-container-low/50">
                <td class="px-4 py-3 text-body-sm">{{ item.id }}</td>
                <td class="px-4 py-3 text-body-sm">{{ item.userId }}</td>
                <td class="px-4 py-3 text-body-sm">{{ getAppealLabel(item.appealType) }}</td>
                <td class="px-4 py-3 text-body-sm max-w-[250px] truncate">{{ item.content }}</td>
                <td class="px-4 py-3 text-body-sm">
                  <span :class="{
                    'px-2 py-0.5 rounded text-[12px] font-medium': true,
                    'bg-yellow-100 text-yellow-800': item.status === 'pending',
                    'bg-green-100 text-green-800': item.status === 'accepted',
                    'bg-red-100 text-red-800': item.status === 'rejected'
                  }">{{ getStatusLabel(item.status) }}</span>
                </td>
                <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.createTime }}</td>
                <td class="px-4 py-3 text-body-sm">
                  <div v-if="item.status === 'pending'" class="flex gap-1">
                    <button class="px-2 py-1 bg-green-50 text-green-700 rounded text-[12px] hover:bg-green-100" @click="handleReview(item, 'accepted')">通过</button>
                    <button class="px-2 py-1 bg-red-50 text-red-700 rounded text-[12px] hover:bg-red-100" @click="handleReview(item, 'rejected')">驳回</button>
                  </div>
                  <span v-else class="text-on-surface-variant text-[12px]">{{ item.reviewRemark || '已处理' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="flex justify-end mt-4" v-if="total > pageSize">
          <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
          <span class="px-3 py-1 text-body-sm text-on-surface-variant">{{ currentPage }}/{{ Math.ceil(total / pageSize) }}</span>
          <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AppealPage',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      filterStatus: ''
    }
  },
  mounted() { this.loadList() },
  methods: {
    getStatusLabel(s) { return { pending: '待审核', accepted: '已通过', rejected: '已驳回' }[s] || s },
    getAppealLabel(t) { return { violation: '违规申诉', points: '积分申诉', other: '其他' }[t] || t },
    async loadList() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.filterStatus) params.status = this.filterStatus
        const res = await this.postRequestUrl('/admin/appeal/list', params)
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    handleReview(item, status) {
      const label = status === 'accepted' ? '通过' : '驳回'
      this.$prompt('审核备注（可选）', `确定${label}该申诉？`, { type: status === 'accepted' ? 'success' : 'warning' })
        .then(({ value }) => this.doReview(item.id, status, value))
        .catch(() => {})
    },
    async doReview(appealId, status, remark) {
      try {
        const res = await this.postRequestUrl('/admin/appeal/review', { appealId, reviewerId: 1, status, remark })
        if (res && res.code == 200) {
          this.$message.success('审核完成')
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '审核失败')
        }
      } catch (e) { this.$message.error('审核失败') }
    },
    changePage(page) { this.currentPage = page; this.loadList() }
  }
}
</script>
