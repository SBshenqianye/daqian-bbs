<template>
  <div v-if="visible" class="fixed inset-0 z-[100]">
    <!-- 遮罩（点击空白处关闭） -->
    <div class="absolute inset-0 bg-black/30" @click="handleClose"></div>
    <!-- 弹窗主体 -->
    <div class="relative h-full flex items-center justify-center p-4">
      <div class="bg-container rounded-lg border border-border shadow-xl w-full max-w-md p-6">
        <header class="flex items-center justify-between mb-4">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-[20px] text-error">flag</span> 实名举报
          </h3>
          <button class="text-on-surface-variant hover:text-on-surface transition-primary" @click="handleClose">
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </header>

        <!-- 举报对象预览 -->
        <div v-if="targetPreview" class="mb-4 px-3 py-2 bg-surface rounded-lg border border-outline-variant">
          <p class="font-label-sm text-label-sm text-on-surface-variant mb-0.5">举报对象</p>
          <p class="font-body-sm text-body-sm text-on-surface break-all">{{ targetPreview }}</p>
        </div>

        <div class="space-y-3">
          <select
            v-model="violationType"
            class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none font-body-md text-body-md"
          >
            <option value="">请选择违规类型</option>
            <option v-for="t in violationTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <textarea
            v-model="description"
            rows="3"
            maxlength="200"
            placeholder="补充说明违规具体情况（选填，200字以内）"
            class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none resize-none font-body-md text-body-md"
          ></textarea>
          <p class="font-label-sm text-label-sm text-on-surface-variant">
            举报将实名提交给管理员核查；核实属实可获得 +2 积分，恶意虚假举报将被扣分。
          </p>
        </div>

        <div class="flex justify-end gap-3 mt-5">
          <button
            class="px-5 py-2 rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container-low transition-primary disabled:opacity-60"
            :disabled="submitting"
            @click="handleClose"
          >
            取消
          </button>
          <button
            class="px-5 py-2 rounded-lg bg-primary-container text-white hover:bg-primary transition-primary active:scale-95 disabled:opacity-60"
            :disabled="submitting || !violationType"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '提交举报' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/** 违规类型选项（与管理端违规类型口径一致，不含"虚假举报"——那是审核结论而非举报动机） */
const VIOLATION_TYPES = [
  { value: 'spam', label: '恶意灌水' },
  { value: 'plagiarism', label: '抄袭剽窃' },
  { value: 'illegal', label: '违法违规内容' },
  { value: 'attack', label: '人身攻击' },
  { value: 'leak', label: '泄露秘密' },
  { value: 'other', label: '其他' },
]

export default {
  name: 'BBSReportDialog',
  props: {
    /** 是否显示弹窗 */
    visible: { type: Boolean, default: false },
    /** 举报目标类型：'article' | 'comment' | 'reply' */
    targetType: { type: String, default: 'article' },
    /** 举报目标 id */
    targetId: { type: [Number, String], default: null },
    /** 举报对象内容预览（展示用） */
    targetPreview: { type: String, default: '' },
  },
  data() {
    return {
      violationType: '',
      description: '',
      submitting: false,
      violationTypes: VIOLATION_TYPES,
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.violationType = ''
        this.description = ''
        this.submitting = false
      }
    },
  },
  methods: {
    handleClose() {
      if (!this.submitting) this.$emit('close')
    },
    getLoginUserId() {
      try {
        const u = window.sessionStorage.getItem('user')
        return u ? JSON.parse(u).id : null
      } catch (e) { return null }
    },
    handleSubmit() {
      if (this.submitting) return
      const reporterId = this.getLoginUserId()
      if (reporterId == null) {
        this.$emit('close')
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      const targetId = Number(this.targetId)
      if (!this.violationType || this.targetId == null || Number.isNaN(targetId)) {
        return
      }
      const label = (VIOLATION_TYPES.find(t => t.value === this.violationType) || {}).label || '违规'
      const reason = '【' + label + '】' + (this.description.trim() || '无补充说明')
      this.submitting = true
      // 后端 ReportController: POST /article/report（防重复：同一用户对同一目标未驳回的举报仅一条）
      // 响应拦截器约定：code 200 自动弹成功提示；业务错误弹 warning 后返回 undefined
      this.postRequest('/article/report', {
        reporterId: Number(reporterId),
        targetType: this.targetType,
        targetId,
        violationType: this.violationType,
        reason,
      }).then(resp => {
        this.submitting = false
        if (resp && resp.code === 200) {
          this.$emit('submitted')
          this.$emit('close')
        }
        // resp 为空 = 业务错误（拦截器已弹提示，如"您已举报过该内容"），弹窗保留供用户取消
      }).catch(() => { this.submitting = false })
    },
  },
}
</script>
