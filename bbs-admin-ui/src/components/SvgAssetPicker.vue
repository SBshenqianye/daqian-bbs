<template>
  <el-dialog
    :visible="visible"
    :title="title || '选择素材'"
    width="720px"
    append-to-body
    custom-class="svg-picker-dialog"
    :close-on-click-modal="true"
    @close="$emit('close')"
  >
    <div class="flex items-center gap-2 mb-3">
      <button
        v-for="c in categories"
        :key="c.key"
        class="px-2.5 py-1 rounded-lg text-[11px] border"
        :class="filter === c.key ? 'bg-primary text-white border-primary' : 'bg-surface text-on-surface-variant border-outline-variant hover:border-primary'"
        @click="filter = c.key"
      >{{ c.label }}</button>
      <button class="ml-auto text-[11px] text-primary hover:opacity-80" @click="load">刷新</button>
    </div>

    <div v-if="loading" class="py-10 text-center text-[12px] text-on-surface-variant">加载中...</div>
    <div v-else-if="filtered.length" class="grid grid-cols-4 sm:grid-cols-6 gap-2 max-h-[340px] overflow-y-auto">
      <div
        v-for="a in filtered"
        :key="a.id"
        class="group relative border border-outline-variant rounded-lg p-2 hover:border-primary cursor-pointer bg-surface-container-lowest"
        :title="a.assetName"
        @click="$emit('select', a)"
      >
        <span class="bbs-level-svg h-12 inline-flex w-full justify-center" v-html="a.svg"></span>
        <p class="text-[10px] text-on-surface-variant truncate mt-1 text-center">{{ a.assetName }}</p>
        <button
          class="absolute top-1 right-1 w-5 h-5 rounded bg-white/90 text-error opacity-0 group-hover:opacity-100"
          title="删除素材"
          @click.stop="remove(a)"
        >×</button>
      </div>
    </div>
    <div v-else class="py-10 text-center">
      <span class="material-symbols-outlined text-[40px] text-outline/30 block mb-2">draw</span>
      <p class="text-[12px] text-on-surface-variant mb-1">还没有可用素材</p>
      <p class="text-[11px] text-outline">去「SVG 工作台」绘制并保存后，回来刷新即可</p>
    </div>

    <div slot="footer" class="flex items-center justify-between gap-2">
      <button class="inline-flex items-center gap-1 px-3 py-2 text-[12px] text-primary hover:bg-primary/8 rounded-lg" @click="goStudio">
        <span class="material-symbols-outlined text-[16px]">draw</span>
        去 SVG 工作台绘制
      </button>
      <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="$emit('close')">取消</button>
    </div>
  </el-dialog>
</template>

<script>
const CATEGORIES = [
  { key: 'all', label: '全部' },
  { key: 'medal', label: '勋章' },
  { key: 'level', label: '等级图标' },
  { key: 'other', label: '其他' },
]

export default {
  name: 'SvgAssetPicker',
  props: {
    visible: { type: Boolean, default: false },
    category: { type: String, default: 'medal' },
    title: { type: String, default: '选择素材' },
  },
  data() {
    return { assets: [], loading: false, filter: this.category || 'all', categories: CATEGORIES }
  },
  computed: {
    filtered() {
      if (this.filter === 'all') return this.assets
      return this.assets.filter(a => (a.category || 'other') === this.filter)
    },
  },
  watch: {
    visible(v) {
      if (v) {
        this.filter = this.category || 'all'
        this.load()
      }
    },
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/admin/svg-asset/list')
        this.assets = (res && res.obj) || []
      } catch (e) {
        this.assets = []
      } finally {
        this.loading = false
      }
    },
    async remove(asset) {
      try {
        await this.$confirm(`确定删除素材「${asset.assetName}」？`, '删除素材', { type: 'warning' })
      } catch (e) {
        return
      }
      const res = await this.postRequest('/admin/svg-asset/delete', { id: asset.id })
      if (res && res.code === 200) {
        this.$message.success('已删除')
        this.load()
      } else {
        this.$message.error('删除失败')
      }
    },
    goStudio() {
      this.$emit('close')
      this.$router.push('/svgStudio')
      this.$message.info('绘制并保存后，回到此处刷新即可选择')
    },
  },
}
</script>
