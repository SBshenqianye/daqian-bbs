<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600">shield</span>
            版主管理
          </h1>
          <p class="text-body-md text-secondary mt-1">任命和撤销版块管理员</p>
        </div>
      </div>

      <!-- Appoint Dialog -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-4">任命版主</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">用户ID</label>
            <input v-model="form.userId" type="number" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="输入用户ID">
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">版块标签ID</label>
            <input v-model="form.labelId" type="number" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="输入标签ID">
          </div>
          <div class="flex items-end">
            <button class="px-5 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="appointing" @click="handleAppoint">
              {{ appointing ? '任命中...' : '任命' }}
            </button>
          </div>
        </div>
      </div>

      <!-- List -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <h3 class="font-title-lg text-title-lg mb-4">当前版主</h3>
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无版主</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">ID</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">用户ID</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">版块标签ID</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">角色</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">任命时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.id" class="hover:bg-surface-container-low/50">
                <td class="px-4 py-3 text-body-sm">{{ item.id }}</td>
                <td class="px-4 py-3 text-body-sm">{{ item.userId }}</td>
                <td class="px-4 py-3 text-body-sm">{{ item.labelId }}</td>
                <td class="px-4 py-3 text-body-sm">{{ item.roleType === 'admin' ? '管理员' : '版主' }}</td>
                <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.appointTime }}</td>
                <td class="px-4 py-3 text-body-sm">
                  <button class="px-2 py-1 bg-red-50 text-red-700 rounded text-[12px] hover:bg-red-100" @click="handleDismiss(item)">撤销</button>
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
  name: 'ModeratorPage',
  data() {
    return {
      loading: false,
      appointing: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      form: { userId: '', labelId: '' }
    }
  },
  mounted() { this.loadList() },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await this.postRequestUrl('/admin/moderator/list', { page: this.currentPage, size: this.pageSize })
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    async handleAppoint() {
      if (!this.form.userId || !this.form.labelId) {
        this.$message.warning('请填写用户ID和版块标签ID')
        return
      }
      this.appointing = true
      try {
        const res = await this.postRequestUrl('/admin/moderator/appoint', {
          userId: parseInt(this.form.userId), labelId: parseInt(this.form.labelId), operatorId: 1
        })
        if (res && res.code == 200) {
          this.$message.success('任命成功')
          this.form = { userId: '', labelId: '' }
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '任命失败')
        }
      } catch (e) { this.$message.error('任命失败') }
      finally { this.appointing = false }
    },
    handleDismiss(item) {
      this.$confirm('确定撤销该版主？', '提示', { type: 'warning' })
        .then(() => this.doDismiss(item))
        .catch(() => {})
    },
    async doDismiss(item) {
      try {
        const res = await this.postRequestUrl('/admin/moderator/dismiss', { userId: item.userId, labelId: item.labelId })
        if (res && res.code == 200) {
          this.$message.success('已撤销')
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '撤销失败')
        }
      } catch (e) { this.$message.error('撤销失败') }
    },
    changePage(page) { this.currentPage = page; this.loadList() }
  }
}
</script>
