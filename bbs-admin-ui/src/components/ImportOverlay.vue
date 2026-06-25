<template>
  <div>
    <template v-if="hasToken">
    <!-- ====== Preview Loading — 非模态浮层 ====== -->
    <div
      v-if="store.status === 'previewing'"
      class="fixed z-[200]"
      style="top: 50%; left: 50%; transform: translate(-50%, -50%);"
    >
      <div class="bg-container rounded-xl shadow-2xl px-8 py-6 flex items-center gap-4">
        <span class="material-symbols-outlined text-primary animate-spin" style="font-size: 28px;">sync</span>
        <div>
          <p class="text-body-md text-on-surface font-medium">正在解析文件...</p>
          <p class="text-body-md text-on-surface-variant">请稍候</p>
        </div>
      </div>
    </div>

    <!-- ====== Import Progress — SideDock 侧边停靠 ====== -->
    <SideDock
      v-if="store.status === 'importing'"
      :edge="dockEdge"
      :expanded="dockExpanded"
      :panel-size="282"
      @update:edge="dockEdge = $event"
      @update:expanded="dockExpanded = $event"
    >
      <template #tab>
        <div class="flex flex-col items-center justify-center w-full h-full gap-0.5 text-primary" title="导入进度">
          <span class="material-symbols-outlined" style="font-size: 18px;">cloud_upload</span>
          <span class="text-xs font-bold leading-none">{{ progressPercent }}%</span>
        </div>
      </template>

      <div class="flex flex-col p-5" style="width: 280px; height: 140px;">
        <!-- 头部 -->
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <div class="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center">
              <span class="material-symbols-outlined text-primary animate-spin" style="font-size: 16px;">sync</span>
            </div>
            <span class="text-sm font-semibold text-gray-800">导入数据</span>
          </div>
          <button class="w-6 h-6 rounded-full flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-all" @click="minimize" title="关闭">
            <span class="material-symbols-outlined" style="font-size: 14px;">close</span>
          </button>
        </div>

        <!-- 进度条 -->
        <div class="flex-1 flex flex-col justify-center gap-2.5">
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-primary rounded-full transition-all duration-300 ease-out"
              :style="{ width: progressPercent + '%' }"
            ></div>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-gray-700">{{ store.progress }} / {{ store.total }} 条</span>
            <span class="text-xs text-gray-400">{{ progressPercent }}%</span>
          </div>
        </div>
      </div>
    </SideDock>

    <!-- ====== Import Done / Error — 非模态结果弹窗 ====== -->
    <div
      v-if="store.status === 'done' || store.status === 'error'"
      class="fixed z-[200]"
      style="top: 50%; left: 50%; transform: translate(-50%, -50%);"
    >
      <div
        ref="resultCard"
        class="bg-container w-full rounded-xl shadow-2xl border border-outline-variant flex flex-col select-none"
        style="max-width: 520px; max-height: 80vh; transform: translate3d(0,0,0); transition: none;"
        @mousedown.prevent="startDragResult"
      >
        <!-- header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined" :class="store.status === 'done' ? 'text-green-600' : 'text-error'">
              {{ store.status === 'done' ? 'check_circle' : 'error' }}
            </span>
            {{ store.status === 'done' ? '导入完成' : '导入失败' }}
          </h3>
          <button class="text-outline hover:text-error transition-colors" @click.stop="close">
            <span class="material-symbols-outlined" style="font-size: 18px;">close</span>
          </button>
        </div>

        <!-- body -->
        <div class="p-5 overflow-y-auto flex-1" @mousedown.stop>
          <div v-if="store.status === 'done' && store.result" class="space-y-4">
            <div class="flex gap-3">
              <div class="flex-1 bg-green-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-green-700">{{ store.result.userSuccessCount || 0 }}</p>
                <p class="text-sm text-green-600">导入成功</p>
              </div>
              <div class="flex-1 bg-red-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-red-700">{{ store.result.userFailCount || 0 }}</p>
                <p class="text-sm text-red-600">导入失败/跳过</p>
              </div>
              <div class="flex-1 bg-blue-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-blue-700">{{ store.result.orgCreatedCount || 0 }}</p>
                <p class="text-sm text-blue-600">新建组织</p>
              </div>
            </div>

            <div v-if="failedDetails.length" class="border border-border rounded-lg overflow-hidden">
              <p class="px-3 py-2 bg-surface-container-low text-label-md text-on-surface-variant font-medium border-b border-border">
                失败/跳过明细
              </p>
              <div class="max-h-48 overflow-y-auto">
                <table class="w-full text-left">
                  <thead>
                    <tr class="bg-surface-container-low border-b border-border">
                      <th class="p-2 text-label-md text-on-surface-variant">行号</th>
                      <th class="p-2 text-label-md text-on-surface-variant">姓名</th>
                      <th class="p-2 text-label-md text-on-surface-variant">操作</th>
                      <th class="p-2 text-label-md text-on-surface-variant">原因</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="d in failedDetails" :key="d.rowNum" class="border-b border-border text-body-md">
                      <td class="p-2">{{ d.rowNum }}</td>
                      <td class="p-2">{{ d.nickname || '-' }}</td>
                      <td class="p-2">
                        <span class="inline-flex items-center px-2 py-0.5 rounded text-[12px] font-medium whitespace-nowrap"
                          :class="d.action === '跳过' ? 'bg-amber-50 text-amber-700' : 'bg-red-50 text-red-700'">
                          {{ d.action }}
                        </span>
                      </td>
                      <td class="p-2 text-red-600">{{ d.message }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <p class="text-center text-body-md text-on-surface-variant">请确认导入结果后关闭</p>
          </div>

          <div v-else-if="store.status === 'error'" class="py-6 text-center flex flex-col items-center gap-3">
            <span class="material-symbols-outlined text-error" style="font-size: 48px;">error</span>
            <p class="text-body-md text-on-surface font-medium">导入失败</p>
            <p class="text-body-md text-on-surface-variant">{{ store.error || '未知错误' }}</p>
          </div>
        </div>

        <!-- footer -->
        <div v-if="store.status === 'done' || store.status === 'error'" class="flex justify-end px-5 py-4 border-t border-outline-variant bg-surface-container-lowest" @mousedown.stop>
          <button class="px-7 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="close">确定</button>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<script>
import { importStore, resetImport } from '@/utils/importStore'
import SideDock from './SideDock.vue'

const POLL_INTERVAL = 1500

export default {
  name: 'ImportOverlay',
  components: { SideDock },
  data() {
    return {
      store: importStore,
      countdown: 0,
      countdownTimer: null,
      pollTimer: null,
      dockEdge: 'right',
      dockExpanded: false,
      dragResultState: null,
      recoveryAttempted: false,
    }
  },
  computed: {
    hasToken() {
      return !!window.sessionStorage.getItem('tokenStr') && this.$route.path !== '/login'
    },
    progressPercent() {
      if (!this.store.total) return 0
      return Math.round((this.store.progress / this.store.total) * 100)
    },
    failedDetails() {
      if (!this.store.result || !this.store.result.details) return []
      return this.store.result.details.filter(d => !d.success)
    },
  },
  watch: {
    'store.status'(val) {
      if (val === 'importing') {
        this.dockEdge = 'right'
        this.dockExpanded = true
        this.startPolling()
      } else if (val === 'done') {
        this.stopPolling()
        this.startCountdown()
      } else if (val === 'error') {
        this.stopPolling()
      }
    },
  },
  mounted() {
    this.recoverImportState()
  },
  beforeDestroy() {
    this.stopPolling()
    this.stopCountdown()
    document.removeEventListener('mousemove', this.onDragResult)
    document.removeEventListener('mouseup', this.stopDragResult)
  },
  methods: {
    startPolling() {
      this.stopPolling()
      this.pollTimer = setInterval(() => {
        const taskId = importStore.taskId
        if (!taskId) return
        const url = `${process.env.VUE_APP_BBS_API || ''}/admin/importProgress/${taskId}`
        fetch(url, {
          headers: { 'Authorization': window.sessionStorage.getItem('tokenStr') || '' }
        })
          .then(r => r.json())
          .then(data => {
            if (data && data.code === 200 && data.obj) {
              const p = data.obj
              importStore.progress = p.progress || 0
              importStore.total = p.total || 0
              if (p.status === 'done' && p.result) {
                importStore.result = p.result
                importStore.status = 'done'
              } else if (p.status === 'error') {
                importStore.error = p.error || '导入出错'
                importStore.status = 'error'
              }
            }
          })
          .catch(() => {})
      }, POLL_INTERVAL)
    },
    stopPolling() {
      if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null }
    },
    startCountdown() {
      // 不再自动关闭，等待手动确认
    },
    stopCountdown() {
      // 无需操作
    },
    close() {
      this.stopCountdown()
      this.stopPolling()
      resetImport()
    },
    minimize() {
      // 只折叠面板，不停止轮询、不 reset，后台继续跑
      this.dockExpanded = false
    },

    /** 页面挂载时 / 刷新后尝试恢复进行中的导入任务 */
    async recoverImportState() {
      if (this.recoveryAttempted) return
      this.recoveryAttempted = true
      // 没有登录则不恢复
      const token = window.sessionStorage.getItem('tokenStr')
      if (!token) return
      // 如果已经有活跃状态（刚点了导入），不覆盖
      if (importStore.status !== 'idle') return
      try {
        const base = process.env.VUE_APP_BBS_API || ''
        const resp = await fetch(`${base}/admin/import/current`, {
          headers: { 'Authorization': token }
        })
        const data = await resp.json()
        if (data && data.code === 200 && data.obj) {
          const p = data.obj
          importStore.taskId = p.taskId || null
          importStore.progress = p.progress || 0
          importStore.total = p.total || 0
          if (p.status === 'processing') {
            importStore.status = 'importing'
          } else if (p.status === 'done') {
            importStore.result = p.result
            importStore.status = 'done'
          } else if (p.status === 'error') {
            importStore.error = p.error || '导入出错'
            importStore.status = 'error'
          }
        }
      } catch (e) {
        // 静默失败，无任务可恢复
      }
    },

    // ====== drag for result card ======
    startDragResult(e) {
      const el = this.$refs.resultCard
      if (!el) return
      if (!e.target.closest('.border-b')) return
      const s = el.style.transform
      let ox = 0, oy = 0
      const m = s && s.match(/translate3?d?\(([-\d.]+)px,?\s*([-\d.]+)px/)
      if (m) { ox = parseFloat(m[1]); oy = parseFloat(m[2]) }
      this.dragResultState = { el, startX: e.clientX, startY: e.clientY, origX: ox, origY: oy }
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'grabbing'
      document.addEventListener('mousemove', this.onDragResult)
      document.addEventListener('mouseup', this.stopDragResult)
    },
    onDragResult(e) {
      if (!this.dragResultState) return
      const d = this.dragResultState
      d.el.style.transform = 'translate3d(' + (d.origX + e.clientX - d.startX) + 'px,' + (d.origY + e.clientY - d.startY) + 'px,0)'
    },
    stopDragResult() {
      if (!this.dragResultState) return
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      this.dragResultState = null
      document.removeEventListener('mousemove', this.onDragResult)
      document.removeEventListener('mouseup', this.stopDragResult)
    },
  },
}
</script>
