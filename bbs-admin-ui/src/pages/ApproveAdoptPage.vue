<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-green-600">check_circle</span>
            采纳审批
          </h1>
          <p class="text-body-md text-secondary mt-1">审核问题求助帖的最佳解答采纳</p>
        </div>
      </div>

      <!-- List -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20 mb-2 block">check_circle</span>
            <p class="text-body-md">暂无待审批的采纳</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">文章</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">回复内容</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">回复人</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">楼主</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.replyId" class="hover:bg-surface-container-low/50">
                <td class="px-4 py-3 text-body-sm max-w-[180px]">
                  <span class="truncate block">{{ item.articleTitle || '未知帖子' }}</span>
                  <div class="flex gap-1 items-center mt-0.5">
                    <span v-if="item.labelName" class="text-[11px] text-outline">{{ item.labelName }}</span>
                    <span class="text-[11px] px-1 py-0.5 rounded" :class="item.type === 'reply' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'">
                      {{ item.type === 'reply' ? '回复' : '评论' }}
                    </span>
                  </div>
                </td>
                <td class="px-4 py-3 text-body-sm max-w-[250px]">
                  <span class="line-clamp-2 block">{{ item.replyContent || '-' }}</span>
                </td>
                <td class="px-4 py-3 text-body-sm">{{ item.replyNickname || '-' }}</td>
                <td class="px-4 py-3 text-body-sm">{{ item.articleNickname || '-' }}</td>
                <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.replyTime || '-' }}</td>
                <td class="px-4 py-3 text-body-sm">
                  <div class="flex gap-1">
                    <button class="px-3 py-1 bg-green-50 text-green-700 rounded text-[12px] hover:bg-green-100 font-medium" @click="handleApprove(item, 'confirm')">
                      通过
                    </button>
                    <button class="px-3 py-1 bg-red-50 text-red-700 rounded text-[12px] hover:bg-red-100 font-medium" @click="handleApprove(item, 'reject')">
                      拒绝
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- Pagination -->
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
  name: 'ApproveAdoptPage',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    getAdminId() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (admin) return JSON.parse(admin).id
      } catch (e) {}
      return 1
    },
    async loadList() {
      this.loading = true
      try {
        const res = await this.postRequest('/reply/admin/pendingAdopts', {
          page: this.currentPage,
          size: this.pageSize,
          adminId: this.getAdminId(),
        })
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else {
          this.list = []
        }
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    handleApprove(item, action) {
      const label = action === 'confirm' ? '通过' : '拒绝'
      const type = action === 'confirm' ? 'success' : 'warning'
      this.$confirm(`确定${label}该采纳申请？`, `确认${label}`, { type })
        .then(() => this.doApprove(item, action))
        .catch(() => {})
    },
    async doApprove(item, action) {
      try {
        const params = {
          articleId: item.articleId,
          action,
          adminId: this.getAdminId(),
        }
        if (item.type === 'reply') {
          params.replyId = item.id
        } else {
          params.commentId = item.id
        }
        const res = await this.postRequest('/reply/admin/approveAdopt', params)
        if (res && res.code == 200) {
          this.$message.success(action === 'confirm' ? '已通过' : '已拒绝')
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '操作失败')
        }
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    changePage(page) {
      this.currentPage = page
      this.loadList()
    },
  },
}
</script>
