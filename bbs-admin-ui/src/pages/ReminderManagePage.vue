<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-6xl mx-auto px-page-margin-desktop py-6">
      <div class="mb-5">
        <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">notifications_active</span>
          提醒配置
        </h1>
        <p class="text-[12px] text-secondary mt-1">
          系统每 5 分钟扫描一次待办；命中规则后向「拥有该权限点」的用户发送聚合站内提醒，同一小时不重复发送。
        </p>
      </div>

      <div class="bg-primary/5 border border-primary/15 rounded-xl px-4 py-3 mb-4 text-[12px] text-on-surface-variant leading-relaxed">
        <b class="text-on-surface">字段说明：</b>
        延迟 = 待办产生后多久开始提醒；重复间隔 = 同一批待办再次提醒的最小间隔；每日上限 = 单日最多提醒次数；
        标题模板中的 <code class="px-1 rounded bg-surface-variant">{count}</code> 会替换成当前待办数量。
      </div>

      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <table class="w-full text-[12px]">
          <thead>
            <tr class="bg-surface-container-low text-on-surface-variant">
              <th class="p-3 text-left font-medium">规则名称</th>
              <th class="p-3 text-left font-medium">触发事件</th>
              <th class="p-3 text-left font-medium">接收权限点</th>
              <th class="p-3 text-left font-medium">范围</th>
              <th class="p-3 text-left font-medium">节奏</th>
              <th class="p-3 text-left font-medium w-20">状态</th>
              <th class="p-3 text-left font-medium w-20">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rules" :key="r.id" class="border-t border-outline-variant/60 hover:bg-surface-container-lowest">
              <td class="p-3">
                <p class="text-on-surface">{{ r.ruleName }}</p>
                <p class="text-[10px] text-outline font-mono">{{ r.ruleCode }}</p>
              </td>
              <td class="p-3">
                <p class="text-on-surface-variant">{{ eventLabel(r.eventType) }}</p>
                <p class="text-[10px] text-outline font-mono">{{ r.eventType }}</p>
              </td>
              <td class="p-3 font-mono text-[11px] text-on-surface-variant">{{ r.permissionCode }}</td>
              <td class="p-3 text-on-surface-variant">{{ r.scopeType === 'BOARD' ? '按板块' : '全部' }}</td>
              <td class="p-3 text-on-surface-variant">
                {{ r.delayMinutes }} 分钟起 · 每 {{ r.repeatMinutes }} 分钟一次 · 每天 ≤ {{ r.maxPerDay }} 次
              </td>
              <td class="p-3">
                <span class="px-2 py-0.5 rounded-full text-[11px]" :class="r.enabled === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-surface-variant text-on-surface-variant'">
                  {{ r.enabled === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td class="p-3">
                <button class="text-primary text-[12px] hover:opacity-80" @click="openEdit(r)">编辑</button>
              </td>
            </tr>
            <tr v-if="!rules.length">
              <td colspan="7" class="p-12 text-center text-on-surface-variant">暂无提醒规则</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 编辑 -->
    <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="dialogVisible = false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-2xl rounded-xl shadow-2xl max-h-[88vh] overflow-y-auto">
        <div class="flex items-center justify-between px-5 py-3 border-b border-outline-variant sticky top-0 bg-container z-10">
          <div>
            <h3 class="text-[14px] font-semibold text-on-surface">编辑提醒规则</h3>
            <p class="text-[11px] text-on-surface-variant mt-0.5">{{ eventLabel(form.eventType) }} · {{ form.eventType }}</p>
          </div>
          <button class="text-outline hover:text-error" @click="dialogVisible = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="p-5 space-y-4">
          <div>
            <label class="field-label">规则名称</label>
            <input v-model="form.ruleName" class="field" placeholder="如：待审核举报">
            <p class="field-hint">只影响管理端展示，可自由命名。</p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="field-label">接收权限点</label>
              <el-select v-model="form.permissionCode" filterable placeholder="选择权限点" class="w-full">
                <el-option
                  v-for="p in permissions"
                  :key="p.code"
                  :label="p.label + ' · ' + p.code"
                  :value="p.code"
                />
              </el-select>
              <p class="field-hint">选择后，只有拥有该权限点的用户会收到提醒。</p>
            </div>
            <div>
              <label class="field-label">提醒范围</label>
              <select v-model="form.scopeType" class="field">
                <option value="ALL">全部（超管/全局角色）</option>
                <option value="BOARD">按板块（版主等）</option>
              </select>
              <p class="field-hint">按板块时只发给对应板块负责人。</p>
            </div>
          </div>

          <div class="grid grid-cols-3 gap-3">
            <div>
              <label class="field-label">延迟提醒（分钟）</label>
              <input v-model.number="form.delayMinutes" type="number" min="0" max="1440" class="field">
              <p class="field-hint">待办产生后多久开始提醒。</p>
            </div>
            <div>
              <label class="field-label">重复间隔（分钟）</label>
              <input v-model.number="form.repeatMinutes" type="number" min="5" max="1440" class="field">
              <p class="field-hint">同一批待办再次提醒的最小间隔。</p>
            </div>
            <div>
              <label class="field-label">每日上限（次）</label>
              <input v-model.number="form.maxPerDay" type="number" min="1" max="48" class="field">
              <p class="field-hint">单日最多提醒次数。</p>
            </div>
          </div>

          <div>
            <label class="field-label">标题模板</label>
            <input v-model="form.titleTemplate" class="field" placeholder="您有 {count} 条待审核举报">
            <p class="field-hint"><code>{count}</code> 会替换成当前待办数量；留空则使用默认文案。</p>
          </div>

          <label class="flex items-center gap-2 text-[12px] text-on-surface cursor-pointer select-none">
            <input v-model="form.enabled" type="checkbox" class="w-4 h-4 accent-primary">
            启用该规则
          </label>
        </div>

        <div class="flex justify-end gap-2 px-5 py-3 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="dialogVisible = false">取消</button>
          <button class="px-5 py-2 bg-primary text-white rounded-lg text-[12px]" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import { nextOverlayZ } from '@/utils/overlay'

const EVENT_LABELS = {
  article_pending: '待审核帖子',
  report_pending: '待审核举报',
  appeal_pending: '待审核申诉',
  featured_pending: '精华待终审',
  adopt_pending: '采纳待审批',
  complaint_pending: '版主投诉待处理',
}

export default {
  name: 'ReminderManagePage',
  data() {
    return { rules: [], permissions: [], dialogVisible: false, form: {}, overlayZ: 3000 }
  },
  mounted() { this.load(); this.loadPermissions() },
  methods: {
    async loadPermissions() {
      try {
        const res = await this.getRequestUrl('/admin/reminder/permissions')
        this.permissions = (res && res.obj) || []
      } catch (e) {
        this.permissions = []
      }
    },
    eventLabel(type) {
      return EVENT_LABELS[type] || type || '未知事件'
    },
    async load() {
      const res = await this.getRequestUrl('/admin/reminder/rules')
      this.rules = (res && res.obj) || []
    },
    openEdit(r) {
      this.overlayZ = nextOverlayZ()
      this.form = {
        id: r.id,
        ruleCode: r.ruleCode,
        ruleName: r.ruleName,
        eventType: r.eventType,
        permissionCode: r.permissionCode,
        scopeType: r.scopeType || 'ALL',
        delayMinutes: r.delayMinutes == null ? 10 : r.delayMinutes,
        repeatMinutes: r.repeatMinutes == null ? 60 : r.repeatMinutes,
        maxPerDay: r.maxPerDay == null ? 8 : r.maxPerDay,
        titleTemplate: r.titleTemplate || '',
        enabled: r.enabled === 1,
      }
      this.dialogVisible = true
    },
    async save() {
      if (!this.form.ruleName || !this.form.ruleName.trim()) {
        this.$message.warning('请填写规则名称')
        return
      }
      if (!this.form.permissionCode || !this.form.permissionCode.trim()) {
        this.$message.warning('请填写接收权限点')
        return
      }
      const payload = { ...this.form, enabled: this.form.enabled ? 1 : 0 }
      const res = await this.postRequest('/admin/reminder/save', payload)
      handleResponse(res, { successMsg: '保存成功', errorMsg: '保存失败', onSuccess: () => { this.dialogVisible = false; this.load() } })
    },
  },
}
</script>

<style scoped>
.field-label {
  display: block;
  font-size: 12px;
  color: #475569;
  margin-bottom: 4px;
}
.field {
  width: 100%;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}
.field:focus { border-color: #2563eb; }
.field-hint {
  margin-top: 3px;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}
</style>
