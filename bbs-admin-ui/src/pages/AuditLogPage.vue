<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600">receipt_long</span>
            审计日志
          </h1>
          <p class="text-body-md text-secondary mt-1">
            关键写操作与权限拒绝留痕。密码、Token 等敏感字段不落库。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-body-sm text-on-surface-variant">共 {{ total }} 条</span>
          <button
            class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60 text-body-sm flex items-center gap-1"
            :disabled="loading"
            @click="load"
          >
            <span class="material-symbols-outlined text-[16px]">refresh</span>
            {{ loading ? '加载中...' : '刷新' }}
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3">
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">模块</label>
            <select v-model="filters.module" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md">
              <option value="">全部</option>
              <option v-for="item in modules" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">操作</label>
            <input v-model.trim="filters.action" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md" placeholder="如 ARTICLE_AUDIT" />
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">操作人 ID</label>
            <input v-model.trim="filters.operatorId" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md" placeholder="如 1" />
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">操作人用户名</label>
            <input v-model.trim="filters.operatorName" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md" placeholder="支持模糊查询" />
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">结果</label>
            <select v-model="filters.result" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md">
              <option value="">全部</option>
              <option value="success">success</option>
              <option value="fail">fail</option>
            </select>
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">目标类型</label>
            <input v-model.trim="filters.targetType" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md" placeholder="如 ARTICLE" />
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">开始时间</label>
            <input v-model.trim="filters.startTime" type="datetime-local" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md" />
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary mb-1.5 block">结束时间</label>
            <input v-model.trim="filters.endTime" type="datetime-local" class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary outline-none font-body-md text-body-md" />
          </div>
        </div>
        <div class="flex items-center justify-end gap-3 mt-4">
          <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="resetFilters">重置</button>
          <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="search">查询</button>
        </div>
      </div>

      <!-- Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[160px]">时间</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[140px]">操作人</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">模块</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[180px]">操作</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[160px]">目标</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[80px]">结果</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[140px]">IP</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[90px]">详情</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in records" :key="row.id" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4 font-body-sm text-on-surface-variant">{{ row.createTime }}</td>
                <td class="p-4 font-body-md text-on-surface">
                  <div>{{ row.operatorName || '-' }}</div>
                  <div v-if="row.operatorId" class="text-[12px] text-outline">ID: {{ row.operatorId }}</div>
                </td>
                <td class="p-4 font-body-md text-on-surface">{{ row.module }}</td>
                <td class="p-4 font-mono text-[12px] text-on-surface-variant">{{ row.action }}</td>
                <td class="p-4 font-body-sm text-on-surface-variant">
                  <span v-if="row.targetType">{{ row.targetType }}</span>
                  <span v-if="row.targetId" class="ml-1 text-outline">#{{ row.targetId }}</span>
                  <span v-if="!row.targetType && !row.targetId">-</span>
                </td>
                <td class="p-4">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-[12px] font-medium"
                    :class="row.result === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'"
                  >{{ row.result }}</span>
                </td>
                <td class="p-4 font-body-sm text-on-surface-variant">{{ row.ip || '-' }}</td>
                <td class="p-4">
                  <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="openDetail(row)">
                    <span class="material-symbols-outlined text-[14px]">visibility</span>
                    查看
                  </button>
                </td>
              </tr>
              <tr v-if="records.length === 0">
                <td colspan="8" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">receipt_long</span>
                    <p class="text-body-md">{{ loading ? '加载中...' : '暂无审计日志' }}</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="flex items-center justify-between px-4 py-3 border-t border-border bg-surface-container-lowest">
          <span class="text-body-sm text-on-surface-variant">第 {{ page }} / {{ totalPages }} 页</span>
          <div class="flex items-center gap-2">
            <button class="px-3 py-1.5 border border-outline rounded text-body-sm disabled:opacity-40" :disabled="page <= 1 || loading" @click="prevPage">上一页</button>
            <button class="px-3 py-1.5 border border-outline rounded text-body-sm disabled:opacity-40" :disabled="page >= totalPages || loading" @click="nextPage">下一页</button>
          </div>
        </div>
      </div>

      <!-- Detail Dialog -->
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="detailVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-4xl rounded-xl shadow-2xl overflow-hidden max-h-[90vh] flex flex-col">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">receipt_long</span>
              审计详情
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="detailVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4 overflow-y-auto">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-body-sm">
              <div><span class="text-on-surface-variant">时间：</span>{{ detail.createTime || '-' }}</div>
              <div><span class="text-on-surface-variant">操作人：</span>{{ detail.operatorName || '-' }}<span v-if="detail.operatorId"> (ID: {{ detail.operatorId }})</span></div>
              <div><span class="text-on-surface-variant">结果：</span>{{ detail.result }}</div>
              <div><span class="text-on-surface-variant">模块：</span>{{ detail.module }}</div>
              <div><span class="text-on-surface-variant">操作：</span>{{ detail.action }}</div>
              <div><span class="text-on-surface-variant">目标：</span>{{ detail.targetType || '-' }}<span v-if="detail.targetId"> #{{ detail.targetId }}</span></div>
              <div class="md:col-span-2"><span class="text-on-surface-variant">请求：</span>{{ detail.requestMethod }} {{ detail.requestPath }}</div>
              <div><span class="text-on-surface-variant">IP：</span>{{ detail.ip || '-' }}</div>
            </div>
            <div v-if="detail.errorMsg" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-body-sm text-red-700">
              {{ detail.errorMsg }}
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p class="font-label-md text-label-md text-secondary mb-1.5">变更前</p>
                <pre class="bg-surface-container-low border border-outline-variant rounded-lg p-3 text-[12px] overflow-auto max-h-[320px] whitespace-pre-wrap">{{ pretty(detail.beforeJson) }}</pre>
              </div>
              <div>
                <p class="font-label-md text-label-md text-secondary mb-1.5">变更后</p>
                <pre class="bg-surface-container-low border border-outline-variant rounded-lg p-3 text-[12px] overflow-auto max-h-[320px] whitespace-pre-wrap">{{ pretty(detail.afterJson) }}</pre>
              </div>
            </div>
            <div v-if="detail.userAgent">
              <p class="font-label-md text-label-md text-secondary mb-1.5">User-Agent</p>
              <p class="text-body-sm text-on-surface-variant break-all">{{ detail.userAgent }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import adminStore from '@/utils/adminStore'

const MODULES = [
  { value: 'ARTICLE', label: '文章' },
  { value: 'REPORT', label: '举报' },
  { value: 'VIOLATION', label: '违规' },
  { value: 'APPEAL', label: '申诉' },
  { value: 'ADOPT', label: '采纳' },
  { value: 'FEATURED', label: '精华' },
  { value: 'MODERATOR', label: '版主' },
  { value: 'USER', label: '用户' },
  { value: 'CONFIG', label: '配置' },
  { value: 'AUTH', label: '权限拒绝' }
]

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatLocalDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function fromInputDateTime(value) {
  if (!value) return ''
  // datetime-local: 2026-11-01T10:00 -> 2026-11-01 10:00:00
  const normalized = value.replace('T', ' ')
  return normalized.length === 16 ? `${normalized}:00` : normalized
}

export default {
  name: 'AuditLogPage',
  data() {
    return {
      modules: MODULES,
      loading: false,
      records: [],
      total: 0,
      page: 1,
      size: 20,
      filters: {
        module: '',
        action: '',
        operatorId: '',
        operatorName: '',
        result: '',
        targetType: '',
        startTime: '',
        endTime: ''
      },
      detailVisible: false,
      detail: {}
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.size))
    },
    isSuperAdmin() {
      return adminStore.isSuperAdmin()
    }
  },
  mounted() {
    const end = new Date()
    const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000)
    this.filters.startTime = formatLocalDateTime(start).replace(' ', 'T').slice(0, 16)
    this.filters.endTime = formatLocalDateTime(end).replace(' ', 'T').slice(0, 16)
    this.load()
  },
  methods: {
    buildQuery() {
      const params = new URLSearchParams()
      params.set('page', this.page)
      params.set('size', this.size)
      const mapping = {
        module: 'module',
        action: 'action',
        operatorId: 'operatorId',
        operatorName: 'operatorName',
        result: 'result',
        targetType: 'targetType'
      }
      Object.keys(mapping).forEach(key => {
        const value = (this.filters[key] || '').toString().trim()
        if (value) params.set(mapping[key], value)
      })
      const start = fromInputDateTime(this.filters.startTime)
      const end = fromInputDateTime(this.filters.endTime)
      if (start) params.set('startTime', start)
      if (end) params.set('endTime', end)
      return params.toString()
    },
    load() {
      if (!this.isSuperAdmin) return
      this.loading = true
      this.getRequestUrl(`/admin/audit/list?${this.buildQuery()}`)
        .then(resp => {
          if (resp && resp.code === 200 && resp.obj) {
            this.records = resp.obj.records || []
            this.total = resp.obj.total || 0
            this.page = resp.obj.page || this.page
            this.size = resp.obj.size || this.size
          } else {
            this.records = []
            this.total = 0
            if (resp && resp.message) this.$message.error(resp.message)
          }
        })
        .catch(() => {
          this.records = []
          this.total = 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    search() {
      this.page = 1
      this.load()
    },
    resetFilters() {
      this.filters = {
        module: '',
        action: '',
        operatorId: '',
        operatorName: '',
        result: '',
        targetType: '',
        startTime: '',
        endTime: ''
      }
      this.page = 1
      this.load()
    },
    prevPage() {
      if (this.page <= 1) return
      this.page -= 1
      this.load()
    },
    nextPage() {
      if (this.page >= this.totalPages) return
      this.page += 1
      this.load()
    },
    openDetail(row) {
      this.detail = row || {}
      this.detailVisible = true
    },
    pretty(value) {
      if (!value) return '—'
      try {
        return JSON.stringify(JSON.parse(value), null, 2)
      } catch (e) {
        return value
      }
    }
  }
}
</script>
