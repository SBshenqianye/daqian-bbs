<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">workspace_premium</span>
            成就管理
          </h1>
          <p class="text-body-md text-secondary mt-1">配置成就目标、数量、AND/OR 组合；勋章支持燕尾旗 SVG 模板导入</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant text-label-md" @click="downloadTemplate">下载勋章模板</button>
          <button class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 text-label-md" @click="openCreate">新建成就</button>
        </div>
      </div>

      <div v-if="loading" class="py-20 text-center text-on-surface-variant">加载中...</div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <div v-for="item in list" :key="item.id" class="bg-container border border-border rounded-xl p-card-padding">
          <div class="flex items-start gap-3">
            <div class="w-16 h-16 shrink-0" v-html="item.iconSvg || defaultMedal"></div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <h3 class="font-headline-sm text-on-surface truncate">{{ item.achievementName }}</h3>
                <span class="text-[10px] px-1.5 py-0.5 rounded bg-surface-variant text-on-surface-variant">{{ item.category || '成长' }}</span>
              </div>
              <p class="text-[12px] text-on-surface-variant mt-1 line-clamp-2">{{ item.description || '暂无说明' }}</p>
              <p class="text-[11px] text-outline mt-1">{{ item.achievementCode }}</p>
            </div>
          </div>
          <div class="mt-3 p-2 rounded-lg bg-surface text-[11px] text-on-surface-variant break-all">
            {{ formatRule(item.ruleJson) }}
          </div>
          <div class="flex justify-end gap-2 mt-3">
            <button class="px-3 py-1.5 text-[12px] text-primary bg-primary/5 rounded" @click="openEdit(item)">编辑</button>
            <button class="px-3 py-1.5 text-[12px] text-error bg-error/5 rounded" @click="remove(item)">删除</button>
          </div>
        </div>
      </div>

      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }">
        <div class="fixed inset-0 bg-black/30" @click="dialogVisible = false"></div>
        <div class="relative bg-container w-full max-w-2xl rounded-xl shadow-2xl max-h-[88vh] overflow-y-auto">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant sticky top-0 bg-container z-10">
            <h3 class="font-headline-sm text-headline-sm">{{ form.id ? '编辑成就' : '新建成就' }}</h3>
            <button class="text-outline hover:text-error" @click="dialogVisible = false"><span class="material-symbols-outlined">close</span></button>
          </div>
          <div class="p-5 space-y-4">
            <div class="grid grid-cols-2 gap-3">
              <div><label class="text-label-md text-secondary">成就名称 *</label><input v-model="form.achievementName" class="w-full mt-1 px-3 py-2 bg-surface border border-outline-variant rounded-lg"></div>
              <div><label class="text-label-md text-secondary">编码（留空自动）</label><input v-model="form.achievementCode" class="w-full mt-1 px-3 py-2 bg-surface border border-outline-variant rounded-lg"></div>
            </div>
            <div><label class="text-label-md text-secondary">描述</label><textarea v-model="form.description" rows="2" class="w-full mt-1 px-3 py-2 bg-surface border border-outline-variant rounded-lg resize-none"></textarea></div>
            <div class="grid grid-cols-3 gap-3">
              <div><label class="text-label-md text-secondary">分类</label><input v-model="form.category" class="w-full mt-1 px-3 py-2 bg-surface border border-outline-variant rounded-lg"></div>
              <div><label class="text-label-md text-secondary">条件逻辑</label><select v-model="form.ruleLogic" class="w-full mt-1 px-3 py-2 bg-surface border border-outline-variant rounded-lg"><option value="ALL">全部满足（交集）</option><option value="ANY">任一满足（并集）</option></select></div>
              <div><label class="text-label-md text-secondary">排序</label><input v-model.number="form.sortOrder" type="number" class="w-full mt-1 px-3 py-2 bg-surface border border-outline-variant rounded-lg"></div>
            </div>
            <div class="flex items-center gap-4">
              <div class="w-20 h-20 rounded-lg border border-dashed border-outline-variant flex items-center justify-center" v-html="form.iconSvg || defaultMedal"></div>
              <div class="space-y-1">
                <button class="px-3 py-1.5 text-[12px] border border-outline rounded" @click="pickerVisible = true">选择勋章</button>
                <p class="text-[11px] text-on-surface-variant">从「SVG 工作台」素材库选择；没有合适的就去工作台绘制</p>
              </div>
            </div>

            <div class="border-t border-outline-variant pt-3">
              <div class="flex items-center justify-between mb-2">
                <span class="text-label-md text-secondary">达成条件</span>
                <button class="text-[12px] text-primary" @click="addCondition">+ 添加条件</button>
              </div>
              <div v-for="(c, i) in conditions" :key="i" class="grid grid-cols-12 gap-2 mb-2 items-center">
                <select v-model="c.metric" class="col-span-5 px-2 py-1.5 bg-surface border border-outline-variant rounded text-[12px]">
                  <option v-for="m in metrics" :key="m.value" :value="m.value">{{ m.label }}</option>
                </select>
                <select v-model="c.op" class="col-span-2 px-2 py-1.5 bg-surface border border-outline-variant rounded text-[12px]">
                  <option value=">=">≥</option><option value=">">＞</option><option value="=">＝</option><option value="<=">≤</option><option value="<">＜</option>
                </select>
                <input v-model.number="c.value" type="number" class="col-span-3 px-2 py-1.5 bg-surface border border-outline-variant rounded text-[12px]">
                <button class="col-span-2 text-[11px] text-error" @click="conditions.splice(i, 1)">删除</button>
              </div>
              <p v-if="conditions.length === 0" class="text-[11px] text-on-surface-variant">至少添加一个条件</p>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant">
            <button class="px-4 py-2 border border-outline rounded-lg" @click="dialogVisible = false">取消</button>
            <button class="px-6 py-2 bg-primary text-on-primary rounded-lg disabled:opacity-50" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 勋章素材选择（绘制在独立 SVG 工作台完成） -->
    <SvgAssetPicker
      :visible="pickerVisible"
      category="medal"
      title="选择勋章"
      @close="pickerVisible = false"
      @select="onPickMedal"
    />
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import { nextOverlayZ } from '@/utils/overlay'
import SvgAssetPicker from '@/components/SvgAssetPicker.vue'

const METRICS = [
  { value: 'article_count', label: '发帖数' },
  { value: 'comment_count', label: '评论数' },
  { value: 'reply_count', label: '回复数' },
  { value: 'like_received', label: '获赞数' },
  { value: 'favorite_received', label: '被收藏数' },
  { value: 'view_count', label: '总浏览' },
  { value: 'featured_count', label: '精华数' },
  { value: 'adopted_count', label: '被采纳数' },
  { value: 'points_total', label: '累计积分' },
  { value: 'login_days', label: '登录天数' },
  { value: 'level_index', label: '当前等级' },
]
const DEFAULT_MEDAL = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#1f6f5c" stroke="#fff" stroke-width="6"/><circle cx="128" cy="112" r="28" fill="none" stroke="#fff" stroke-width="4"/></svg>'

export default {
  name: 'AchievementManagePage',
  components: { SvgAssetPicker },
  data() {
    return {
      loading: false, saving: false, dialogVisible: false,
      list: [], metrics: METRICS, conditions: [],
      defaultMedal: DEFAULT_MEDAL,
      form: { id: null, achievementName: '', achievementCode: '', description: '', category: '成长', iconSvg: '', ruleLogic: 'ALL', sortOrder: 0, enabled: 1 },
      drawer: { visible: false, shapes: [], current: null, fill: '#1f6f5c', stroke: '#ffffff', width: 6 },
      drag: { point: -1 },
      pickerVisible: false,
      overlayZ: 3000,
    }
  },
  computed: {
    anchors() {
      return this.drawer.current ? this.drawer.current.points : []
    },
    draftSvg() {
      const shapes = this.drawer.shapes.slice()
      if (this.drawer.current && this.drawer.current.points.length >= 3) {
        shapes.push(this.drawer.current)
      }
      return this.shapesToSvg(shapes)
    },
  },
  mounted() { this.load() },
  methods: {
    onPickMedal(asset) {
      if (!asset || !asset.svg) return
      this.form.iconSvg = asset.svg
      this.pickerVisible = false
      this.$message.success('已应用勋章「' + (asset.assetName || '素材') + '」')
    },
    /* ---------- 勋章绘制器 ---------- */
    openDrawer() {
      this.drawer.visible = true
      this.drawer.shapes = []
      this.drawer.current = { points: [], closed: false, fill: this.drawer.fill, stroke: this.drawer.stroke, width: this.drawer.width }
      this.applyPreset('flag')
    },
    pathOf(shape) {
      if (!shape || !shape.points.length) return ''
      const parts = shape.points.map((p, i) => (i === 0 ? `M${p.x} ${p.y}` : `L${p.x} ${p.y}`))
      return parts.join(' ') + (shape.closed ? ' Z' : '')
    },
    shapesToSvg(shapes) {
      const body = (shapes || []).map(s => {
        if (!s.points || s.points.length < 2) return ''
        return `<path d="${this.pathOf(s)}" fill="${s.fill}" stroke="${s.stroke}" stroke-width="${s.width}" stroke-linejoin="round"/>`
      }).join('')
      return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256">${body}</svg>`
    },
    canvasPoint(event) {
      const svg = this.$refs.canvas
      if (!svg) return { x: 0, y: 0 }
      const rect = svg.getBoundingClientRect()
      if (!rect.width || !rect.height) return { x: 0, y: 0 }
      const x = ((event.clientX - rect.left) / rect.width) * 256
      const y = ((event.clientY - rect.top) / rect.height) * 256
      const snap = v => Math.round(v / 4) * 4
      return { x: Math.max(2, Math.min(254, snap(x))), y: Math.max(2, Math.min(254, snap(y))) }
    },
    onCanvasDown(event) {
      if (!this.drawer.current) {
        this.drawer.current = { points: [], closed: false, fill: this.drawer.fill, stroke: this.drawer.stroke, width: this.drawer.width }
      }
      const cur = this.drawer.current
      if (cur.closed) {
        // 已闭合则开始新形状
        this.drawer.current = { points: [], closed: false, fill: this.drawer.fill, stroke: this.drawer.stroke, width: this.drawer.width }
      }
      const p = this.canvasPoint(event)
      // 命中已有节点 → 拖动
      for (let i = 0; i < this.drawer.current.points.length; i++) {
        const a = this.drawer.current.points[i]
        if (Math.abs(a.x - p.x) <= 8 && Math.abs(a.y - p.y) <= 8) {
          this.drag.point = i
          return
        }
      }
      this.drawer.current.points.push(p)
    },
    onCanvasMove(event) {
      if (this.drag.point < 0 || !this.drawer.current) return
      const p = this.canvasPoint(event)
      this.drawer.current.points.splice(this.drag.point, 1, p)
    },
    onCanvasUp() {
      this.drag.point = -1
    },
    closePath() {
      const cur = this.drawer.current
      if (!cur || cur.points.length < 3) {
        this.$message.warning('至少需要 3 个节点才能闭合')
        return
      }
      cur.closed = true
      this.drawer.shapes.push(cur)
      this.drawer.current = { points: [], closed: false, fill: this.drawer.fill, stroke: this.drawer.stroke, width: this.drawer.width }
    },
    undoPoint() {
      if (this.drawer.current && this.drawer.current.points.length) {
        this.drawer.current.points.pop()
        return
      }
      this.undoShape()
    },
    undoShape() {
      this.drawer.shapes.pop()
    },
    clearCanvas() {
      this.drawer.shapes = []
      this.drawer.current = { points: [], closed: false, fill: this.drawer.fill, stroke: this.drawer.stroke, width: this.drawer.width }
    },
    applyPreset(kind) {
      let points = []
      if (kind === 'flag') {
        points = [{ x: 44, y: 36 }, { x: 212, y: 36 }, { x: 212, y: 214 }, { x: 128, y: 170 }, { x: 44, y: 214 }]
      } else if (kind === 'shield') {
        points = [{ x: 128, y: 24 }, { x: 220, y: 64 }, { x: 220, y: 136 }, { x: 128, y: 232 }, { x: 36, y: 136 }, { x: 36, y: 64 }]
      } else {
        points = []
        for (let i = 0; i < 24; i++) {
          const a = (Math.PI * 2 * i) / 24
          points.push({ x: Math.round(128 + 96 * Math.cos(a)), y: Math.round(128 + 96 * Math.sin(a)) })
        }
      }
      this.drawer.current = { points, closed: false, fill: this.drawer.fill, stroke: this.drawer.stroke, width: this.drawer.width }
      this.closePath()
    },
    saveDrawing() {
      if (this.drawer.current && this.drawer.current.points.length >= 3) {
        this.closePath()
      }
      if (!this.drawer.shapes.length) {
        this.$message.warning('请先绘制至少一个闭合形状')
        return
      }
      const svg = this.shapesToSvg(this.drawer.shapes)
      if (svg.length > 100000) {
        this.$message.error('图形过于复杂')
        return
      }
      this.form.iconSvg = svg
      this.drawer.visible = false
      this.$message.success('已应用为勋章')
    },
    async load() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/admin/achievement/list')
        this.list = (res && res.obj) || []
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    formatRule(json) {
      try {
        const r = JSON.parse(json || '{}')
        const parts = (r.conditions || []).map(c => `${this.labelOf(c.metric)} ${c.op} ${c.value}`)
        return (r.logic === 'ANY' ? '任一：' : '全部：') + parts.join(' 且 ')
      } catch (e) { return json || '' }
    },
    labelOf(metric) { const m = METRICS.find(x => x.value === metric); return m ? m.label : metric },
    openCreate() {
      this.overlayZ = nextOverlayZ()
      this.form = { id: null, achievementName: '', achievementCode: '', description: '', category: '成长', iconSvg: '', ruleLogic: 'ALL', sortOrder: 0, enabled: 1 }
      this.conditions = [{ metric: 'article_count', op: '>=', value: 1 }]
      this.dialogVisible = true
    },
    openEdit(item) {
      this.overlayZ = nextOverlayZ()
      this.form = { ...item, iconSvg: item.iconSvg || '' }
      try {
        const r = JSON.parse(item.ruleJson || '{}')
        this.conditions = (r.conditions || []).map(c => ({ ...c }))
        this.form.ruleLogic = r.logic || item.ruleLogic || 'ALL'
      } catch (e) { this.conditions = [] }
      this.dialogVisible = true
    },
    addCondition() { this.conditions.push({ metric: 'article_count', op: '>=', value: 1 }) },
    async handleMedalImport(event) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await this.uploadFile('/admin/achievement/medal/import', formData)
        if (res && res.obj) this.form.iconSvg = res.obj
        else this.$message.error((res && res.message) || '导入失败')
      } catch (e) { this.$message.error('导入失败') }
      event.target.value = ''
    },
    async downloadTemplate() {
      try {
        const res = await this.getRequestUrl('/admin/achievement/medal/template')
        const svg = (res && res.obj) || ''
        const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url; a.download = '燕尾旗勋章模板.svg'; a.click()
        URL.revokeObjectURL(url)
      } catch (e) { this.$message.error('下载失败') }
    },
    async save() {
      if (!this.form.achievementName || !this.form.achievementName.trim()) { this.$message.warning('请输入成就名称'); return }
      if (!this.conditions.length) { this.$message.warning('请至少添加一个条件'); return }
      this.saving = true
      const payload = { ...this.form, ruleJson: JSON.stringify({ logic: this.form.ruleLogic, conditions: this.conditions }) }
      try {
        const res = await this.postRequest('/admin/achievement/save', payload)
        handleResponse(res, { successMsg: '保存成功', errorMsg: '保存失败', onSuccess: () => { this.dialogVisible = false; this.load() } })
      } catch (e) { this.$message.error('保存失败') }
      finally { this.saving = false }
    },
    remove(item) {
      this.$confirm(`确定删除成就「${item.achievementName}」吗？`, '删除成就', { type: 'warning' })
        .then(async () => {
          const res = await this.postRequest('/admin/achievement/delete', { id: item.id })
          handleResponse(res, { successMsg: '删除成功', errorMsg: '删除失败', onSuccess: () => this.load() })
        }).catch(() => {})
    },
  },
}
</script>

<style scoped>
.preset {
  padding: 4px 10px;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 11px;
}
.preset:hover {
  border-color: #2563eb;
  color: #1d4ed8;
}
.preset.is-danger {
  color: #dc2626;
}
.preset.is-danger:hover {
  border-color: #fca5a5;
}
</style>
