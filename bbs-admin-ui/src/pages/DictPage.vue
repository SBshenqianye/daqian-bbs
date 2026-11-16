<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- 头部 -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">settings</span>
            配置管理
          </h1>
          <p class="text-body-md text-secondary mt-1">按值类型自动匹配控件：开关 / 分段按钮 / 滑块 / 微调 / 输入框</p>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-40 text-[12px]"
            :disabled="!canWriteConfig"
            @click="openAdd"
          >
            <span class="material-symbols-outlined text-[18px]">add</span>新增字典
          </button>
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant text-[12px] disabled:opacity-40"
            :disabled="!canWriteConfig"
            title="删除无键 / 无类型等无效配置"
            @click="tidy"
          >
            <span class="material-symbols-outlined text-[18px]">auto_fix_high</span>清理无效配置
          </button>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 border border-outline-variant rounded-lg text-[12px]" @click="loadDictList">
            <span class="material-symbols-outlined text-[18px]">refresh</span>刷新
          </button>
        </div>
      </div>

      <!-- 列表 -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="bg-surface-container-low border-b border-border text-[12px]">
                <th class="p-3 text-left font-medium text-on-surface-variant min-w-[140px]">中文描述</th>
                <th class="p-3 text-left font-medium text-on-surface-variant min-w-[260px]">值</th>
                <th class="p-3 text-left font-medium text-on-surface-variant min-w-[120px]">键</th>
                <th class="p-3 text-left font-medium text-on-surface-variant min-w-[150px]">备注</th>
                <th class="p-3 text-left font-medium text-on-surface-variant w-[100px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in dictList" :key="item.id || index" class="border-b border-border/70 hover:bg-surface-container-low/40">
                <td class="p-3">
                  <p class="text-[13px] text-on-surface">{{ item.dictLabel }}</p>
                  <p class="text-[10px] text-outline mt-0.5">{{ item.dictType }}</p>
                </td>
                <td class="p-3">
                  <!-- 枚举选项：只读 -->
                  <span v-if="isEnumOption(item)" class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-surface-variant text-[11px] text-on-surface-variant">
                    <span class="font-mono">{{ item.dictValue }}</span>
                    <span class="text-outline">·</span>{{ item.dictLabel }}
                  </span>

                  <!-- 开关 -->
                  <div v-else-if="editorOf(item).control === 'switch'" class="flex items-center gap-2">
                    <button
                      class="switch"
                      :class="{ 'is-on': isOn(item) }"
                      :disabled="!canWriteConfig"
                      role="switch"
                      :aria-checked="isOn(item)"
                      @click="setValue(item, isOn(item) ? '0' : '1')"
                    >
                      <span class="switch-knob"></span>
                    </button>
                    <span class="text-[11px]" :class="isOn(item) ? 'text-primary' : 'text-outline'">{{ isOn(item) ? '开' : '关' }}</span>
                  </div>

                  <!-- 分段按钮 -->
                  <div v-else-if="editorOf(item).control === 'segmented'" class="inline-flex rounded-lg border border-outline-variant overflow-hidden">
                    <button
                      v-for="opt in editorOf(item).options"
                      :key="opt.value"
                      class="px-2.5 py-1.5 text-[11px] transition-colors disabled:opacity-50"
                      :class="String(item.dictValue) === String(opt.value) ? 'bg-primary text-white' : 'bg-surface text-on-surface-variant hover:bg-surface-variant'"
                      :disabled="!canWriteConfig"
                      @click="setValue(item, opt.value)"
                    >{{ opt.label }}</button>
                  </div>

                  <!-- 滑块 -->
                  <div v-else-if="editorOf(item).control === 'slider'" class="flex items-center gap-2 min-w-[200px]">
                    <input
                      type="range"
                      class="flex-1 accent-primary"
                      :min="editorOf(item).min"
                      :max="editorOf(item).max"
                      :step="editorOf(item).step"
                      :value="Number(item.dictValue) || 0"
                      :disabled="!canWriteConfig"
                      @input="onSlide(item, $event)"
                    >
                    <span class="w-16 text-right text-[12px] tabnum text-on-surface">{{ item.dictValue }}{{ editorOf(item).unit }}</span>
                  </div>

                  <!-- 微调 -->
                  <div v-else-if="editorOf(item).control === 'stepper'" class="inline-flex items-center gap-1">
                    <button class="step-btn" :disabled="!canWriteConfig" @click="stepValue(item, -1)">−</button>
                    <input
                      class="w-20 px-2 py-1 text-[12px] text-right bg-surface border border-outline-variant rounded outline-none focus:border-primary"
                      :value="item.dictValue"
                      :disabled="!canWriteConfig"
                      @change="setValue(item, $event.target.value)"
                    >
                    <button class="step-btn" :disabled="!canWriteConfig" @click="stepValue(item, 1)">+</button>
                    <span class="text-[11px] text-outline">{{ editorOf(item).unit }}</span>
                  </div>

                  <!-- 文本 -->
                  <input
                    v-else
                    class="w-full max-w-[320px] px-2 py-1 text-[12px] bg-surface border border-outline-variant rounded outline-none focus:border-primary"
                    :value="item.dictValue"
                    :disabled="!canWriteConfig"
                    @change="setValue(item, $event.target.value)"
                  >
                </td>
                <td class="p-3">
                  <span v-if="item.dictKey" class="text-[12px] font-mono text-on-surface-variant">{{ item.dictKey }}</span>
                  <span v-else class="text-[11px] text-warning" title="程序无法按 key 读取；可用“整理键”自动补齐">—</span>
                </td>
                <td class="p-3">
                  <span class="block max-w-[200px] truncate text-[12px] text-on-surface-variant" :title="item.remark">{{ item.remark || '—' }}</span>
                </td>
                <td class="p-3">
                  <div class="flex items-center gap-1">
                    <button class="icon-btn" :disabled="!canWriteConfig" title="编辑描述/键/备注" @click="openEdit(item)">
                      <span class="material-symbols-outlined text-[16px]">edit</span>
                    </button>
                    <button class="icon-btn is-danger" :disabled="!canWriteConfig" title="删除" @click="handleDelete(item)">
                      <span class="material-symbols-outlined text-[16px]">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="dictList.length === 0">
                <td colspan="5" class="p-16 text-center text-on-surface-variant">
                  <span class="material-symbols-outlined text-[44px] opacity-20 block mb-2">settings</span>
                  暂无配置数据
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 新增 -->
    <div v-if="addVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="addVisible = false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm">新增字典</h3>
          <button class="text-outline hover:text-error" @click="addVisible = false"><span class="material-symbols-outlined">close</span></button>
        </div>
        <div class="p-5 space-y-3">
          <div>
            <label class="text-[12px] text-secondary">中文描述 *</label>
            <input v-model="addForm.dictLabel" class="field" maxlength="100" placeholder="如：发帖积分">
          </div>
          <div>
            <label class="text-[12px] text-secondary">字典类型 *</label>
            <input v-model="addForm.dictType" class="field" maxlength="100" placeholder="如：article_publish_mode">
          </div>
          <div>
            <label class="text-[12px] text-secondary">值 *</label>
            <input v-model="addForm.dictValue" class="field" maxlength="100" placeholder="如：AUTO / 1 / 10">
          </div>
          <div>
            <div class="flex items-center justify-between">
              <label class="text-[12px] text-secondary">键（程序读取用）</label>
              <button class="text-[11px] text-primary" @click="addForm.dictKey = slug(addForm.dictType || addForm.dictLabel)">按类型自动生成</button>
            </div>
            <input v-model="addForm.dictKey" class="field font-mono" maxlength="100" placeholder="如：article_publish_mode">
          </div>
          <div>
            <label class="text-[12px] text-secondary">排序</label>
            <input v-model.number="addForm.dictSort" type="number" min="0" class="field">
          </div>
          <div>
            <label class="text-[12px] text-secondary">备注</label>
            <textarea v-model="addForm.remark" rows="2" class="field resize-none" maxlength="200"></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-2 p-4 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="addVisible = false">取消</button>
          <button class="px-5 py-2 bg-primary text-white rounded-lg text-[12px]" @click="submitAdd">确定</button>
        </div>
      </div>
    </div>

    <!-- 编辑（只改描述/键/备注/排序，值在列表内直接调） -->
    <div v-if="editVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="editVisible = false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm">编辑字典</h3>
          <button class="text-outline hover:text-error" @click="editVisible = false"><span class="material-symbols-outlined">close</span></button>
        </div>
        <div class="p-5 space-y-3">
          <div>
            <label class="text-[12px] text-secondary">中文描述</label>
            <input v-model="editForm.dictLabel" class="field">
          </div>
          <div>
            <label class="text-[12px] text-secondary">当前值</label>
            <input :value="editForm.dictValue" class="field bg-surface-variant text-on-surface-variant" disabled>
          </div>
          <div>
            <label class="text-[12px] text-secondary">键</label>
            <input v-model="editForm.dictKey" class="field font-mono" maxlength="100">
          </div>
          <div>
            <label class="text-[12px] text-secondary">排序</label>
            <input v-model.number="editForm.dictSort" type="number" min="0" class="field">
          </div>
          <div>
            <label class="text-[12px] text-secondary">备注</label>
            <textarea v-model="editForm.remark" rows="2" class="field resize-none" maxlength="200"></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-2 p-4 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="editVisible = false">取消</button>
          <button class="px-5 py-2 bg-primary text-white rounded-lg text-[12px]" @click="submitEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import adminStore from '@/utils/adminStore'
import { nextOverlayZ } from '@/utils/overlay'

/** 已知配置项 → 最佳控件；未登记的按类型自动推断 */
const SPECS = {
  article_publish_mode: {
    control: 'segmented',
    options: [
      { label: '自动通过', value: 'AUTO' },
      { label: '前置审核', value: 'PRE_REVIEW' },
      { label: '先发后审', value: 'POST_REVIEW' },
    ],
  },
  featured_final_review_mode: {
    control: 'segmented',
    options: [
      { label: '管理员终审', value: 'ADMIN_FINAL' },
      { label: '版主终审', value: 'MODERATOR_FINAL' },
    ],
  },
  featured_public_enabled: { control: 'switch' },
  featured_public_days: { control: 'stepper', min: 1, max: 30, step: 1, unit: '工作日' },
  featured_showcase_count: { control: 'slider', min: 1, max: 12, step: 1, unit: '条' },
  monthly_deduction_alert_threshold: { control: 'stepper', min: 1, max: 100, step: 1, unit: '分' },
  moderator_reward_points: { control: 'stepper', min: 0, max: 2000, step: 50, unit: '分' },
}

const BOOLEAN_KEY = /(enabled|enable|_flag$|^is_|allow_|no_points|anonymous|public_)/

export default {
  name: 'DictPage',
  data() {
    return {
      dictList: [],
      addVisible: false,
      editVisible: false,
      addForm: { dictType: '', dictValue: '', dictLabel: '', dictSort: 0, remark: '', dictKey: '' },
      editForm: { id: null, dictType: '', dictValue: '', dictLabel: '', dictSort: 0, remark: '', dictKey: '' },
      originalDictKey: '',
      overlayZ: 3000,
    }
  },
  computed: {
    canWriteConfig() {
      return adminStore.hasPermission('config:write')
    },
    typeCount() {
      const map = {}
      this.dictList.forEach(r => {
        const t = String(r.dictType || '').trim()
        if (t) map[t] = (map[t] || 0) + 1
      })
      return map
    },
  },
  mounted() { this.loadDictList() },
  methods: {
    isEnumOption(row) {
      const t = String(row.dictType || '').trim()
      return !String(row.dictKey || '').trim() && t && (this.typeCount[t] || 0) > 1
    },
    isOn(row) {
      return String(row.dictValue) === '1'
    },
    editorOf(row) {
      const key = String(row.dictKey || '').trim()
      const type = String(row.dictType || '').trim().toLowerCase()
      const value = row.dictValue == null ? '' : String(row.dictValue).trim()
      if (SPECS[key]) return { unit: '', step: 1, min: 0, max: 100, ...SPECS[key] }
      if (type === 'switch') return { control: 'switch' }
      if ((value === '0' || value === '1') && BOOLEAN_KEY.test(key || type)) return { control: 'switch' }
      if (value !== '' && !isNaN(Number(value)) && /^-?\d+(\.\d+)?$/.test(value)) {
        const n = Number(value)
        return n >= 100
          ? { control: 'slider', min: 0, max: Math.max(1000, Math.ceil(n / 100) * 200), step: 1, unit: '' }
          : { control: 'stepper', min: 0, max: Number.MAX_SAFE_INTEGER, step: 1, unit: '' }
      }
      return { control: 'text' }
    },
    slug(value) {
      return String(value || '')
        .toLowerCase()
        .replace(/[^a-z0-9_]+/g, '_')
        .replace(/^_+|_+$/g, '')
        .slice(0, 100)
    },
    currentUsername() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (admin) return JSON.parse(admin).username || ''
      } catch (e) { /* ignore */ }
      return ''
    },
    loadDictList() {
      this.postRequest('/admin/listDict', {}).then(resp => {
        if (resp) {
          const list = resp.obj || resp.data || resp.list || (Array.isArray(resp) ? resp : [])
          this.dictList = Array.isArray(list) ? list : []
        }
      })
    },
    payloadOf(row, overrides) {
      return {
        id: row.id,
        dictType: row.dictType,
        dictValue: row.dictValue,
        dictLabel: row.dictLabel,
        dictSort: Number(row.dictSort) || 0,
        updateBy: this.currentUsername(),
        remark: row.remark || '',
        dictKey: row.dictKey || null,
        ...overrides,
      }
    },
    /** 值变更即时保存 */
    setValue(row, value) {
      if (!this.canWriteConfig) return
      if (value === '' || value == null) return
      const next = String(value)
      if (next === String(row.dictValue)) return
      const old = row.dictValue
      row.dictValue = next
      this.postRequest('/admin/updateDict', this.payloadOf(row)).then(resp => {
        if (!resp || resp.code !== 200) {
          row.dictValue = old
          handleResponse(resp, { errorMsg: '保存失败' })
        }
      }).catch(() => {
        row.dictValue = old
        this.$message.error('保存失败')
      })
    },
    stepValue(row, dir) {
      const spec = this.editorOf(row)
      const step = Number(spec.step) || 1
      const current = Number(row.dictValue) || 0
      const max = spec.max == null ? Number.MAX_SAFE_INTEGER : Number(spec.max)
      const min = spec.min == null ? 0 : Number(spec.min)
      const next = Math.min(max, Math.max(min, current + dir * step))
      this.setValue(row, next)
    },
    onSlide(row, event) {
      this.setValue(row, event.target.value)
    },
    /** 一键整理：删除无键 / 无类型记录（无键配置程序无法按 key 读取，属无效数据） */
    tidy() {
      const noType = this.dictList.filter(r => !String(r.dictType || '').trim())
      const keyless = this.dictList.filter(r => String(r.dictType || '').trim() && !String(r.dictKey || '').trim())
      if (!noType.length && !keyless.length) {
        this.$message.success('没有需要清理的配置项')
        return
      }
      const parts = []
      if (keyless.length) {
        const names = keyless.slice(0, 5).map(r => r.dictLabel || r.dictValue).join('、')
        parts.push(`删除 ${keyless.length} 条无键配置（${names}${keyless.length > 5 ? ' 等' : ''}）`)
      }
      if (noType.length) parts.push(`删除 ${noType.length} 条无类型配置`)
      this.$confirm(parts.join('；') + '。无键配置程序无法按 key 读取，删除后不影响已有读取逻辑。是否继续？', '清理无效配置', { type: 'warning' })
        .then(async () => {
          for (const row of keyless) {
            await this.postRequest('/admin/deleteDict', { id: row.id })
          }
          for (const row of noType) {
            await this.postRequest('/admin/deleteDict', { id: row.id })
          }
          this.$message.success('清理完成')
          this.loadDictList()
        }).catch(() => {})
    },
    openAdd() {
      this.overlayZ = nextOverlayZ()
      this.addForm = { dictType: '', dictValue: '', dictLabel: '', dictSort: 0, remark: '', dictKey: '' }
      this.addVisible = true
    },
    submitAdd() {
      const dictType = (this.addForm.dictType || '').trim()
      const dictValue = (this.addForm.dictValue || '').trim()
      const dictLabel = (this.addForm.dictLabel || '').trim()
      if (!dictType) { this.$message.warning('字典类型不能为空'); return }
      if (!dictValue) { this.$message.warning('值不能为空'); return }
      if (!dictLabel) { this.$message.warning('中文描述不能为空'); return }
      this.postRequest('/admin/addDict', {
        dictType,
        dictValue,
        dictLabel,
        dictSort: Number(this.addForm.dictSort) || 0,
        createBy: this.currentUsername(),
        remark: (this.addForm.remark || '').trim(),
        dictKey: (this.addForm.dictKey || '').trim() || this.slug(dictType) || null,
      }).then(resp => {
        handleResponse(resp, { successMsg: '添加成功', onSuccess: () => { this.addVisible = false; this.loadDictList() } })
      })
    },
    openEdit(row) {
      if (!row) return
      this.overlayZ = nextOverlayZ()
      this.editForm = {
        id: row.id,
        dictType: row.dictType,
        dictValue: row.dictValue,
        dictLabel: row.dictLabel || '',
        dictSort: Number(row.dictSort) || 0,
        remark: row.remark || '',
        dictKey: row.dictKey || '',
      }
      this.originalDictKey = row.dictKey || ''
      this.editVisible = true
    },
    submitEdit() {
      const { id, dictType, dictValue, dictLabel, dictSort, remark, dictKey } = this.editForm
      if (id == null) { this.$message.warning('ID不能为空'); return }
      if (!dictLabel || !dictLabel.trim()) { this.$message.warning('中文描述不能为空'); return }
      const newKey = (dictKey || '').trim()
      const payload = {
        id,
        dictType,
        dictValue,
        dictLabel,
        dictSort: Number(dictSort) || 0,
        updateBy: this.currentUsername(),
        remark: (remark || '').trim(),
        dictKey: newKey || null,
      }
      if (newKey !== (this.originalDictKey || '')) {
        this.$confirm(
          `键用于程序按 key 读取配置。将键从「${this.originalDictKey || '无'}」改为「${newKey || '无'}」后，旧键将立即失效，可能影响对应功能。是否继续？`,
          '修改键的风险提示',
          { type: 'warning', confirmButtonText: '确认修改', cancelButtonText: '取消' }
        ).then(() => this.doUpdateDict(payload)).catch(() => {})
        return
      }
      this.doUpdateDict(payload)
    },
    doUpdateDict(payload) {
      this.postRequest('/admin/updateDict', payload).then(resp => {
        handleResponse(resp, { successMsg: '修改成功', onSuccess: () => { this.editVisible = false; this.loadDictList() } })
      })
    },
    handleDelete(row) {
      if (!row || row.id == null) { this.$message.warning('无法获取记录ID'); return }
      this.$confirm('确定要删除该字典项吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/deleteDict', { id: row.id }).then(resp => {
          handleResponse(resp, { onSuccess: () => this.loadDictList() })
        })
      }).catch(() => {})
    },
  },
}
</script>

<style scoped>
.field {
  width: 100%;
  margin-top: 4px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}
.field:focus { border-color: #2563eb; }

.step-btn {
  width: 24px;
  height: 26px;
  border-radius: 6px;
  border: 1px solid #dbe1ea;
  background: #fff;
  color: #475569;
  font-size: 13px;
  line-height: 1;
}
.step-btn:hover:not(:disabled) { border-color: #2563eb; color: #2563eb; }
.step-btn:disabled { opacity: 0.4; }

.switch {
  position: relative;
  width: 40px;
  height: 22px;
  border-radius: 999px;
  background: #cbd5e1;
  transition: background-color 0.18s ease;
  flex: none;
  padding: 0;
}
.switch.is-on { background: #2563eb; }
.switch-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
  transition: transform 0.18s ease;
}
.switch.is-on .switch-knob { transform: translateX(18px); }
.switch:disabled { opacity: 0.45; }

.icon-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}
.icon-btn:hover:not(:disabled) { background: rgba(148, 163, 184, 0.14); color: #334155; }
.icon-btn.is-danger:hover:not(:disabled) { color: #dc2626; }
.icon-btn:disabled { opacity: 0.35; }

.tabnum { font-variant-numeric: tabular-nums; }
</style>
