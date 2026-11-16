<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2"><span class="material-symbols-outlined text-primary">event_repeat</span>运营周期</h1>
          <p class="text-body-md text-secondary mt-1">按年度/期次切换运营方案；总积分不重置，只影响榜单与周期配置</p>
        </div>
        <button class="px-4 py-2 bg-primary text-on-primary rounded-lg text-[12px]" @click="openCreate">新建周期</button>
      </div>

      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <table class="w-full text-[13px]">
          <thead>
            <tr class="bg-surface-container-low text-on-surface-variant">
              <th class="p-3 text-left font-medium">周期名称</th>
              <th class="p-3 text-left font-medium">起止日期</th>
              <th class="p-3 text-left font-medium">状态</th>
              <th class="p-3 text-left font-medium w-[160px]">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in list" :key="c.id" class="border-t border-outline-variant/50">
              <td class="p-3">
                <p class="text-on-surface">{{ c.cycleName }}</p>
                <p class="text-[10px] text-outline font-mono">{{ c.cycleCode }}</p>
              </td>
              <td class="p-3 text-on-surface-variant">{{ c.startDate }} ~ {{ c.endDate }}</td>
              <td class="p-3">
                <span class="px-2 py-0.5 rounded-full text-[11px]" :class="c.status===1?'bg-emerald-50 text-emerald-700':c.status===2?'bg-surface-variant text-on-surface-variant':'bg-amber-50 text-amber-700'">
                  {{ c.status===1?'启用中':c.status===2?'已归档':'草稿' }}
                </span>
              </td>
              <td class="p-3">
                <div class="flex gap-2">
                  <button v-if="c.status!==1" class="text-primary text-[12px] hover:opacity-80" @click="activate(c)">激活</button>
                  <button class="text-primary text-[12px] hover:opacity-80" @click="openConfig(c)">周期配置</button>
                </div>
              </td>
            </tr>
            <tr v-if="!list.length">
              <td colspan="4" class="p-12 text-center text-on-surface-variant">暂无周期</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新建/编辑周期 -->
    <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="dialogVisible=false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
        <div class="px-5 py-3 border-b border-outline-variant">
          <h3 class="text-[14px] font-semibold text-on-surface">{{ form.id ? '编辑周期' : '新建周期' }}</h3>
        </div>
        <div class="p-5 space-y-3">
          <div>
            <label class="field-label">周期名称 <span class="text-error">*</span></label>
            <input v-model="form.cycleName" class="field" placeholder="如：2027 年度运营周期">
            <p class="field-hint">仅用于管理端展示与切换。</p>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="field-label">开始日期 <span class="text-error">*</span></label>
              <input v-model="form.startDate" type="date" class="field">
            </div>
            <div>
              <label class="field-label">结束日期 <span class="text-error">*</span></label>
              <input v-model="form.endDate" type="date" class="field">
            </div>
          </div>
          <p class="field-hint">周期内产生的积分日志会带上该周期 ID，用于按周期统计；总积分不会因切换周期而清零。</p>
        </div>
        <div class="flex justify-end gap-2 px-5 py-3 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="dialogVisible=false">取消</button>
          <button class="px-4 py-2 bg-primary text-white rounded-lg text-[12px]" @click="save">保存</button>
        </div>
      </div>
    </div>

    <!-- 周期配置 -->
    <div v-if="configVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="configVisible=false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-2xl rounded-xl shadow-2xl overflow-hidden max-h-[86vh] flex flex-col">
        <div class="px-5 py-3 border-b border-outline-variant shrink-0">
          <h3 class="text-[14px] font-semibold text-on-surface">周期配置 · {{ configCycle.cycleName }}</h3>
          <p class="text-[11px] text-on-surface-variant mt-0.5">仅覆盖本周期；未配置的键继续使用全局字典</p>
        </div>
        <div class="p-5 space-y-4 overflow-y-auto">
          <div>
            <p class="text-[12px] text-secondary mb-2">已覆盖配置</p>
            <div v-if="configs.length" class="rounded-lg border border-outline-variant divide-y divide-outline-variant/50">
              <div v-for="c in configs" :key="c.id" class="flex items-center gap-3 px-3 py-2 text-[12px]">
                <span class="w-52 truncate font-mono text-on-surface">{{ c.configKey }}</span>
                <span class="flex-1 truncate text-on-surface-variant">{{ c.configValue }}</span>
              </div>
            </div>
            <p v-else class="text-[12px] text-on-surface-variant">暂无覆盖配置</p>
          </div>
          <div class="rounded-lg border border-outline-variant p-3">
            <p class="text-[12px] text-secondary mb-2">新增 / 更新覆盖</p>
            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="field-label">配置键</label>
                <input v-model="configKey" class="field font-mono" placeholder="如 post">
                <p class="field-hint">与「配置管理」里的键一致，如 post、reply、featured。</p>
              </div>
              <div>
                <label class="field-label">配置值</label>
                <input v-model="configValue" class="field" placeholder="如 5">
                <p class="field-hint">在该周期内覆盖全局值。</p>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2 px-5 py-3 border-t border-outline-variant bg-surface-container-lowest shrink-0">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="configVisible=false">关闭</button>
          <button class="px-4 py-2 bg-primary text-white rounded-lg text-[12px]" @click="saveConfig">保存配置</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import { nextOverlayZ } from '@/utils/overlay'

export default {
  name: 'OperationCyclePage',
  data() {
    return {
      list: [],
      dialogVisible: false,
      form: {},
      configVisible: false,
      configCycle: {},
      configs: [],
      configKey: '',
      configValue: '',
      overlayZ: 3000,
    }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      const res = await this.getRequestUrl('/admin/operation-cycle/list')
      this.list = (res && res.obj) || []
    },
    openCreate() {
      this.overlayZ = nextOverlayZ()
      this.form = { id: null, cycleName: '', startDate: '', endDate: '', status: 0 }
      this.dialogVisible = true
    },
    async save() {
      if (!this.form.cycleName || !this.form.cycleName.trim()) { this.$message.warning('请填写周期名称'); return }
      if (!this.form.startDate || !this.form.endDate) { this.$message.warning('请选择起止日期'); return }
      const res = await this.postRequest('/admin/operation-cycle/save', this.form)
      handleResponse(res, { successMsg: '保存成功', errorMsg: '保存失败', onSuccess: () => { this.dialogVisible = false; this.load() } })
    },
    async activate(c) {
      const res = await this.postRequest('/admin/operation-cycle/activate', { id: c.id })
      handleResponse(res, { successMsg: '已激活', errorMsg: '激活失败', onSuccess: () => this.load() })
    },
    async openConfig(c) {
      this.overlayZ = nextOverlayZ()
      this.configCycle = c
      this.configKey = ''
      this.configValue = ''
      this.configVisible = true
      const res = await this.getRequestUrl(`/admin/operation-cycle/config/${c.id}`)
      this.configs = (res && res.obj) || []
    },
    async saveConfig() {
      if (!this.configKey) { this.$message.warning('请填写配置键'); return }
      const res = await this.postRequest('/admin/operation-cycle/config/save', {
        cycleId: this.configCycle.id,
        configKey: this.configKey,
        configValue: this.configValue,
      })
      handleResponse(res, { successMsg: '保存成功', errorMsg: '保存失败', onSuccess: () => this.openConfig(this.configCycle) })
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
