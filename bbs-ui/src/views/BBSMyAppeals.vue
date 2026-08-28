<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的申诉</h1>
    </header>

    <!-- 新建申诉 -->
    <div class="bg-container border border-outline-variant rounded-lg p-card-padding mb-8">
      <h3 class="font-headline-sm text-headline-sm mb-4">提交新申诉</h3>
      <div class="space-y-3">
        <select v-model="form.appealType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
          <option value="">请选择申诉类型</option>
          <option value="violation">违规申诉</option>
          <option value="points">积分申诉</option>
          <option value="other">其他</option>
        </select>
        <textarea v-model="form.content" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" rows="3" placeholder="请描述申诉理由"></textarea>
        <button class="px-5 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交申诉' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="list.length > 0" class="space-y-gutter">
        <div v-for="item in list" :key="item.id" class="bg-container border border-outline-variant rounded-lg p-card-padding">
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center gap-2 mb-2">
                <span :class="{
                  'px-2 py-0.5 rounded text-[12px] font-medium': true,
                  'bg-yellow-100 text-yellow-800': item.status === 'pending',
                  'bg-green-100 text-green-800': item.status === 'accepted',
                  'bg-red-100 text-red-800': item.status === 'rejected'
                }">{{ getStatusLabel(item.status) }}</span>
                <span class="font-body-sm text-on-surface-variant">{{ getAppealLabel(item.appealType) }}</span>
              </div>
              <p class="font-body-md text-body-md text-on-surface">{{ item.content }}</p>
              <p v-if="item.reviewRemark" class="font-body-sm text-body-sm text-on-surface-variant mt-2">审核意见: {{ item.reviewRemark }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ item.createTime }}</p>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-20">assignment</span>
        <p class="font-body-lg text-body-lg">暂无申诉记录</p>
      </div>
    </template>
  </main>
</template>

<script>
export default {
  name: 'BBSMyAppeals',
  data() {
    return {
      loading: false, submitting: false, list: [],
      form: { appealType: '', content: '' }
    }
  },
  mounted() { this.loadList() },
  methods: {
    getUserId() { return JSON.parse(localStorage.getItem('user') || '{}').id },
    getStatusLabel(s) { return { pending: '待审核', accepted: '已通过', rejected: '已驳回' }[s] || s },
    getAppealLabel(t) { return { violation: '违规申诉', points: '积分申诉', other: '其他' }[t] || t },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/appeal/myList', { userId, page: 1, size: 100 })
        if (res && res.code == 200 && res.obj) this.list = res.obj.records || []
        else this.list = []
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    async handleSubmit() {
      if (!this.form.appealType || !this.form.content) {
        this.$message && this.$message.warning ? this.$message.warning('请选择类型并填写内容') : alert('请选择类型并填写内容')
        return
      }
      const userId = this.getUserId()
      if (!userId) return
      this.submitting = true
      try {
        const res = await this.postRequest('/user/appeal/submit', { userId, appealType: this.form.appealType, content: this.form.content })
        if (res && res.code == 200) {
          alert('申诉已提交')
          this.form = { appealType: '', content: '' }
          await this.loadList()
        } else {
          alert((res && res.message) || '提交失败')
        }
      } catch (e) { alert('提交失败') }
      finally { this.submitting = false }
    }
  }
}
</script>
