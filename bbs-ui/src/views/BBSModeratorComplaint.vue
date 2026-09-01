<template>
  <main class="max-w-4xl mx-auto py-12 px-page-margin-desktop bg-surface min-h-screen">
    <h1 class="font-headline-lg text-headline-lg text-on-surface mb-8">版主投诉</h1>

    <!-- 提交投诉 -->
    <div class="bg-container border border-outline-variant rounded-lg p-6 mb-8">
      <h2 class="font-headline-sm text-headline-sm mb-4">提交投诉</h2>
      <p class="text-body-sm text-on-surface-variant mb-4">如发现版主存在徇私评定精华、随意删帖、履职不作为等问题，可在此提交投诉。科技数字化工作部将在5个工作日内核查反馈。</p>

      <div class="space-y-4">
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">被投诉版主用户ID</label>
          <input v-model="form.moderatorId" type="number" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="输入版主用户ID">
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">版块标签ID（选填）</label>
          <input v-model="form.labelId" type="number" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="如知道具体版块可填写">
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">投诉内容</label>
          <textarea v-model="form.content" rows="5" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none" placeholder="请详细描述投诉事项，包括具体行为、时间等信息..." maxlength="500"></textarea>
          <span class="text-body-sm text-on-surface-variant">{{ (form.content || '').length }}/500</span>
        </div>
        <button class="px-5 py-2 bg-primary text-on-primary rounded hover:opacity-90 disabled:opacity-60 text-body-md" :disabled="submitting || !form.moderatorId || !form.content.trim()" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交投诉' }}
        </button>
      </div>
    </div>

    <!-- 我的投诉记录 -->
    <div class="bg-container border border-outline-variant rounded-lg p-6">
      <h2 class="font-headline-sm text-headline-sm mb-4">我的投诉记录</h2>
      <div v-if="loading" class="py-8 text-center text-on-surface-variant">加载中...</div>
      <div v-else-if="complaints.length === 0" class="py-8 text-center text-on-surface-variant">暂无投诉记录</div>
      <div v-else class="space-y-4">
        <div v-for="item in complaints" :key="item.id" class="border border-outline-variant/50 rounded-lg p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-body-sm text-on-surface-variant">投诉 #{{ item.id }} · 版主：{{ item.moderatorName || '用户#' + item.moderatorId }}</span>
            <span class="px-2 py-0.5 rounded text-[11px] font-medium"
              :class="{
                'bg-yellow-100 text-yellow-800': item.status === 'pending',
                'bg-green-100 text-green-800': item.status === 'accepted',
                'bg-red-100 text-red-800': item.status === 'rejected'
              }">{{ statusLabel(item.status) }}</span>
          </div>
          <p class="text-body-md text-on-surface mb-2">{{ item.content }}</p>
          <div class="text-body-sm text-on-surface-variant space-y-1">
            <p>提交时间：{{ item.createTime }}</p>
            <p v-if="item.reviewRemark">审核结果：{{ item.reviewRemark }}</p>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script>
export default {
  name: 'BBSModeratorComplaint',
  data() {
    return {
      submitting: false,
      loading: false,
      form: { moderatorId: '', labelId: '', content: '' },
      complaints: []
    }
  },
  mounted() {
    this.loadComplaints()
  },
  methods: {
    statusLabel(s) {
      return { pending: '审核中', accepted: '已采纳', rejected: '已驳回' }[s] || s
    },
    async loadComplaints() {
      const userStr = sessionStorage.getItem('user')
      if (!userStr) return
      const user = JSON.parse(userStr)
      this.loading = true
      try {
        const res = await this.postRequest('/user/moderatorComplaint/myList', { reporterId: user.id })
        if (res && res.code == 200 && res.obj) {
          this.complaints = res.obj
        } else { this.complaints = [] }
      } catch (e) { this.complaints = [] }
      finally { this.loading = false }
    },
    async handleSubmit() {
      const userStr = sessionStorage.getItem('user')
      if (!userStr) { this.$message.warning('请先登录'); return }
      const user = JSON.parse(userStr)
      if (!this.form.moderatorId) { this.$message.warning('请输入版主用户ID'); return }
      if (!this.form.content.trim()) { this.$message.warning('请填写投诉内容'); return }
      this.submitting = true
      try {
        const res = await this.postRequest('/user/moderatorComplaint/submit', {
          reporterId: user.id,
          moderatorId: parseInt(this.form.moderatorId),
          labelId: this.form.labelId ? parseInt(this.form.labelId) : null,
          content: this.form.content.trim()
        })
        if (res && res.code == 200) {
          this.$message.success(res.message || '投诉已提交')
          this.form = { moderatorId: '', labelId: '', content: '' }
          await this.loadComplaints()
        } else {
          this.$message.error((res && res.message) || '提交失败')
        }
      } catch (e) { this.$message.error('提交失败') }
      finally { this.submitting = false }
    }
  }
}
</script>
