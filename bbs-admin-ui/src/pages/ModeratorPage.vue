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
        <div class="flex items-center gap-2">
          <button class="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 disabled:opacity-60 text-body-sm flex items-center gap-1" :disabled="rewarding" @click="handleMonthlyReward">
            <span class="material-symbols-outlined text-[16px]" style="vertical-align: text-bottom;">payments</span>
            {{ rewarding ? '发放中...' : '手动发放本月奖励' }}
          </button>
        </div>
      </div>

      <!-- 自动发放设置 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-4">自动发放设置</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">自动发放开关</label>
            <select v-model="autoConfig.enabled" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
              <option value="0">关闭</option>
              <option value="1">开启</option>
            </select>
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">每月发放日（1-28）</label>
            <input v-model="autoConfig.day" type="number" min="1" max="28" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="1">
          </div>
          <div class="flex gap-2">
            <button class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60 text-body-sm" :disabled="savingConfig" @click="saveAutoConfig">
              {{ savingConfig ? '保存中...' : '保存设置' }}
            </button>
            <button class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 text-body-sm" @click="handleTriggerAuto">
              立即触发
            </button>
          </div>
        </div>
      </div>

      <!-- 本月取消列表 -->
      <div v-if="cancelledList.length > 0" class="bg-orange-50 border border-orange-200 rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-3 text-orange-800">本月已取消奖励的版主（{{ cancelledList.length }}人）</h3>
        <div class="flex flex-wrap gap-2">
          <span v-for="item in cancelledList" :key="item.userId" class="inline-flex items-center gap-1 px-3 py-1 bg-white border border-orange-300 rounded-full text-body-sm">
            {{ item.userName || '用户#' + item.userId }}
            <button class="text-green-600 hover:text-green-800 ml-1" @click="handleRestore(item)" title="恢复奖励">恢复</button>
          </span>
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
                  <button class="px-2 py-1 bg-orange-50 text-orange-700 rounded text-[12px] hover:bg-orange-100 mr-1" @click="handleCancel(item)" title="取消本月奖励">取消奖励</button>
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

      <!-- 取消奖励弹窗 -->
      <div v-show="cancelDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="cancelDialogVisible = false"></div>
        <div class="relative bg-container rounded-xl shadow-xl w-full max-w-md p-6">
          <h3 class="font-headline-sm text-headline-sm mb-4">取消版主本月奖励</h3>
          <p class="text-body-md text-on-surface-variant mb-2">
            将取消 <span class="font-medium">{{ cancelForm.userName }}</span> 本月的履职奖励。
          </p>
          <div class="mb-4">
            <label class="block text-body-sm text-on-surface-variant mb-1">取消原因</label>
            <textarea v-model="cancelForm.remark" rows="3" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none" placeholder="填写取消原因..."></textarea>
          </div>
          <div class="flex justify-end gap-2">
            <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant" @click="cancelDialogVisible = false">取消</button>
            <button class="px-4 py-2 bg-orange-600 text-white rounded hover:bg-orange-700 disabled:opacity-60" :disabled="cancelSaving" @click="doCancel">
              {{ cancelSaving ? '处理中...' : '确认取消' }}
            </button>
          </div>
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
      rewarding: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      form: { userId: '', labelId: '' },
      autoConfig: { enabled: '0', day: '1' },
      savingConfig: false,
      cancelledList: [],
      cancelDialogVisible: false,
      cancelSaving: false,
      cancelForm: { userId: null, userName: '', remark: '' }
    }
  },
  mounted() {
    this.loadList()
    this.loadAutoConfig()
    this.loadCancelledList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await this.postRequest('/admin/moderator/list', { page: this.currentPage, size: this.pageSize })
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    async loadAutoConfig() {
      try {
        const res = await this.postRequest('/admin/dict/list', { dictType: 'moderator_reward_auto' })
        if (res && res.code == 200 && res.obj && res.obj.records) {
          const autoItem = res.obj.records.find(r => r.dictKey === 'moderator_reward_auto')
          if (autoItem) this.autoConfig.enabled = autoItem.dictValue || '0'
        }
        const res2 = await this.postRequest('/admin/dict/list', { dictType: 'moderator_reward_day' })
        if (res2 && res2.code == 200 && res2.obj && res2.obj.records) {
          const dayItem = res2.obj.records.find(r => r.dictKey === 'moderator_reward_day')
          if (dayItem) this.autoConfig.day = dayItem.dictValue || '1'
        }
      } catch (e) { /* ignore */ }
    },
    async saveAutoConfig() {
      this.savingConfig = true
      try {
        // 更新自动发放开关
        await this.postRequest('/admin/dict/update', {
          dictType: 'moderator_reward_auto', dictValue: this.autoConfig.enabled,
          dictLabel: '版主奖励自动发放开关', dictKey: 'moderator_reward_auto'
        })
        // 更新发放日
        await this.postRequest('/admin/dict/update', {
          dictType: 'moderator_reward_day', dictValue: this.autoConfig.day,
          dictLabel: '版主奖励发放日', dictKey: 'moderator_reward_day'
        })
        this.$message.success('设置已保存')
      } catch (e) { this.$message.error('保存失败') }
      finally { this.savingConfig = false }
    },
    handleTriggerAuto() {
      this.$confirm('立即触发自动发放？将按当前取消列表执行。', '触发自动发放', { type: 'info' })
        .then(async () => {
          try {
            const res = await this.postRequest('/admin/moderator/triggerAutoReward', {})
            if (res && res.code == 200) {
              this.$message.success(res.message || '触发成功')
            } else {
              this.$message.error((res && res.message) || '触发失败')
            }
          } catch (e) { this.$message.error('触发失败') }
        }).catch(() => {})
    },
    async loadCancelledList() {
      try {
        const res = await this.postRequest('/admin/moderator/cancelledRewards', {})
        if (res && res.code == 200 && res.obj) {
          this.cancelledList = res.obj
        } else { this.cancelledList = [] }
      } catch (e) { this.cancelledList = [] }
    },
    handleCancel(item) {
      this.cancelForm = { userId: item.userId, userName: item.userId, remark: '' }
      this.cancelDialogVisible = true
    },
    async doCancel() {
      this.cancelSaving = true
      try {
        const user = JSON.parse(sessionStorage.getItem('user') || '{}')
        const res = await this.postRequest('/admin/moderator/cancelReward', {
          userId: this.cancelForm.userId,
          operatorId: user.id || 1,
          remark: this.cancelForm.remark || null
        })
        if (res && res.code == 200) {
          this.$message.success(res.message || '已取消')
          this.cancelDialogVisible = false
          await this.loadCancelledList()
        } else {
          this.$message.error((res && res.message) || '取消失败')
        }
      } catch (e) { this.$message.error('取消失败') }
      finally { this.cancelSaving = false }
    },
    async handleRestore(item) {
      try {
        const res = await this.postRequest('/admin/moderator/restoreReward', { userId: item.userId })
        if (res && res.code == 200) {
          this.$message.success('已恢复')
          await this.loadCancelledList()
        } else {
          this.$message.error((res && res.message) || '恢复失败')
        }
      } catch (e) { this.$message.error('恢复失败') }
    },
    async handleAppoint() {
      if (!this.form.userId || !this.form.labelId) {
        this.$message.warning('请填写用户ID和版块标签ID')
        return
      }
      this.appointing = true
      try {
        const res = await this.postRequest('/admin/moderator/appoint', {
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
        const res = await this.postRequest('/admin/moderator/dismiss', { userId: item.userId, labelId: item.labelId })
        if (res && res.code == 200) {
          this.$message.success('已撤销')
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '撤销失败')
        }
      } catch (e) { this.$message.error('撤销失败') }
    },
    changePage(page) { this.currentPage = page; this.loadList() },
    handleMonthlyReward() {
      this.$confirm('确定为所有有效版主发放本月履职奖励（每人15积分）？已发放过的版主将自动跳过。', '发放履职奖励', { type: 'info' })
        .then(() => this.doMonthlyReward())
        .catch(() => {})
    },
    async doMonthlyReward() {
      this.rewarding = true
      try {
        const user = JSON.parse(sessionStorage.getItem('user') || '{}')
        const res = await this.postRequest('/admin/moderator/monthlyReward', { operatorId: user.id || 1 })
        if (res && res.code == 200) {
          this.$message.success(res.message || '发放成功')
        } else {
          this.$message.error((res && res.message) || '发放失败')
        }
      } catch (e) { this.$message.error('发放失败') }
      finally { this.rewarding = false }
    }
  }
}
</script>
