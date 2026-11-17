<template>
  <div v-if="visible" class="fixed inset-0 z-[100]">
    <!-- 遮罩（点击空白处关闭） -->
    <div class="absolute inset-0 bg-black/30" @click="handleClose"></div>
    <!-- 弹窗主体 -->
    <div class="relative h-full flex items-center justify-center p-4">
      <div class="bg-container rounded-xl border border-border shadow-xl w-full max-w-md p-5">
        <header class="flex items-center justify-between mb-4">
          <h3 class="text-[15px] font-semibold text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px] text-error">flag</span> 举报
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

        <div class="space-y-2.5">
          <select
            v-model="violationType"
            class="ui-input"
          >
            <option value="">请选择违规类型</option>
            <option v-for="t in violationTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <textarea
            v-model="description"
            rows="3"
            maxlength="200"
            placeholder="补充说明违规具体情况（选填，200 字以内）"
            class="ui-textarea"
          ></textarea>
          <label class="flex items-center gap-2 text-body-sm text-on-surface cursor-pointer select-none">
            <input v-model="isAnonymous" type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded">
            匿名举报（管理员仍可核查，举报人信息不对其他用户展示）
          </label>
          <p class="font-label-sm text-label-sm text-on-surface-variant">
            举报提交给管理员核查；核实属实可获得 +2 积分，恶意虚假举报将被扣分。
          </p>
        </div>

        <div class="flex justify-end gap-2 mt-4">
          <button
            class="ui-btn ui-btn-sm ui-btn-secondary"
            :disabled="submitting"
            @click="handleClose"
          >
            取消
          </button>
          <button
            class="ui-btn ui-btn-sm ui-btn-primary"
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
      isAnonymous: false,
      submitting: false,
      violationTypes: VIOLATION_TYPES,
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.violationType = ''
        this.description = ''
        this.isAnonymous = false
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
        isAnonymous: this.isAnonymous,
      }).then(resp => {
        this.submitting = false
        if (resp && resp.code === 200) {
          this.$emit('submitted')
          this.$emit('close')
          // 拦截器对 code 200 不再自动弹成功提示（成功提示统一在调用方弹，见 api-interceptor.js 注释），
          // 故此处手动弹一次性成功反馈，避免用户提交后零反馈；不会与拦截器双弹。
          this.$message.success('举报已提交，管理员会尽快核实处理')
        }
        // resp 为空 = 业务错误（拦截器已弹提示，如"您已举报过该内容"），弹窗保留供用户取消
      }).catch(() => { this.submitting = false })
    },
  },
}
</script>
