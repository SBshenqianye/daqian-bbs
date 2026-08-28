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
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">内容摘要</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">回复人</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">楼主</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.id" class="hover:bg-surface-container-low/50">
                <td class="px-4 py-3 text-body-sm max-w-[180px]">
                  <a class="text-primary-container hover:underline cursor-pointer block truncate" @click="openArticle(item)" :title="item.articleTitle || '未知帖子'">
                    {{ item.articleTitle || '未知帖子' }}
                  </a>
                  <div class="flex gap-1 items-center mt-0.5">
                    <span v-if="item.labelName" class="text-[11px] text-outline">{{ item.labelName }}</span>
                    <span class="text-[11px] px-1 py-0.5 rounded" :class="item.type === 'reply' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'">
                      {{ item.type === 'reply' ? '楼中楼回复' : '评论' }}
                    </span>
                  </div>
                </td>
                <td class="px-4 py-3 text-body-sm max-w-[250px]">
                  <span class="line-clamp-2 block cursor-pointer hover:text-primary-container" @click="showDetail(item)" :title="'点击查看完整内容'">{{ item.content || '-' }}</span>
                </td>
                <td class="px-4 py-3 text-body-sm">{{ item.authorName || '-' }}</td>
                <td class="px-4 py-3 text-body-sm">{{ item.articleAuthorName || '-' }}</td>
                <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.time || '-' }}</td>
                <td class="px-4 py-3 text-body-sm">
                  <div class="flex gap-1">
                    <button class="px-3 py-1 text-[12px] rounded font-medium text-white bg-green-600 hover:bg-green-700" @click="handleApprove(item, 'confirm')">
                      通过
                    </button>
                    <button class="px-3 py-1 text-[12px] rounded font-medium text-white bg-red-500 hover:bg-red-600" @click="handleApprove(item, 'reject')">
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

      <!-- Detail Dialog -->
      <el-dialog title="采纳详情" :visible.sync="detailVisible" width="600px" :close-on-click-modal="true">
        <div v-if="detailItem" class="space-y-4">
          <!-- 文章信息 -->
          <div class="bg-surface-container-low rounded-lg p-4">
            <div class="flex items-center gap-2 mb-2">
              <span class="text-body-sm font-medium text-on-surface-variant">所属文章</span>
              <a class="text-primary-container text-body-sm hover:underline cursor-pointer" @click="openArticle(detailItem)">
                {{ detailItem.articleTitle || '未知帖子' }}
              </a>
            </div>
            <div class="flex gap-2 text-[11px] text-outline">
              <span v-if="detailItem.labelName">{{ detailItem.labelName }}</span>
              <span>{{ detailItem.type === 'reply' ? '楼中楼回复' : '评论' }}</span>
              <span>{{ detailItem.time || '' }}</span>
            </div>
          </div>
          <!-- 人员信息 -->
          <div class="flex items-center gap-6 text-body-sm">
            <div>
              <span class="text-on-surface-variant">回复人：</span>
              <span class="font-medium">{{ detailItem.authorName || '-' }}</span>
            </div>
            <div>
              <span class="text-on-surface-variant">楼主：</span>
              <span class="font-medium">{{ detailItem.articleAuthorName || '-' }}</span>
            </div>
          </div>
          <!-- 完整内容 -->
          <div>
            <div class="text-body-sm font-medium text-on-surface-variant mb-2">完整内容</div>
            <div class="border border-outline-variant rounded-lg p-4 text-body-md text-on-surface whitespace-pre-line max-h-[300px] overflow-y-auto bg-surface">
              {{ detailItem.content || '无内容' }}
            </div>
          </div>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="success" @click="handleApprove(detailItem, 'confirm'); detailVisible = false">通过</el-button>
          <el-button type="danger" @click="handleApprove(detailItem, 'reject'); detailVisible = false">拒绝</el-button>
        </span>
      </el-dialog>
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
      detailVisible: false,
      detailItem: null,
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
    showDetail(item) {
      this.detailItem = item
      this.detailVisible = true
    },
    openArticle(item) {
      if (!item || !item.articleId) return
      // 用户前台文章详情页地址，按实际部署路径调整
      const base = process.env.VUE_APP_BBS_UI_URL || window.location.origin
      window.open(`${base}/#/article/${item.articleId}`, '_blank')
    },
    handleApprove(item, action) {
      if (!item) return
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
