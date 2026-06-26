<template>
  <div v-if="visible" class="fixed inset-0 flex items-center justify-center p-4" style="z-index: 100;">
    <!-- backdrop -->
    <div class="fixed inset-0 bg-black/30" @click="$emit('close')"></div>
    <!-- dialog card -->
    <div ref="dialogCard" class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl flex flex-col" style="max-height: 80vh; transform: translate3d(0,0,0); transition: none;">
      <!-- drag handle header -->
      <div class="flex items-center justify-between p-5 border-b border-outline-variant select-none" @mousedown.prevent="startDrag">
        <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">corporate_fare</span>
          选择单位
        </h3>
        <button class="text-outline hover:text-error transition-colors" @click="$emit('close')">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- search + toolbar -->
      <div class="px-5 pt-4 pb-2">
        <div class="grid grid-cols-1 grid-rows-1">
          <input
            v-model="filterText"
            class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md"
            placeholder="搜索单位名称"
          >
          <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline pointer-events-none" style="font-size: 18px;">search</span>
        </div>
        <div v-if="treeData.length" class="flex gap-2 mt-2">
          <button
            class="inline-flex items-center gap-1 px-2.5 py-1 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
            style="font-size: 11px;"
            @click="$refs.orgTree.expandAll()"
          >
            <span class="material-symbols-outlined" style="font-size: 13px;">unfold_more</span>
            全部展开
          </button>
          <button
            class="inline-flex items-center gap-1 px-2.5 py-1 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
            style="font-size: 11px;"
            @click="$refs.orgTree.collapseAll()"
          >
            <span class="material-symbols-outlined" style="font-size: 13px;">unfold_less</span>
            全部折叠
          </button>
        </div>
      </div>

      <!-- tree body -->
      <div class="px-4 pb-4 overflow-y-auto flex-1">
        <OrgTree
          ref="orgTree"
          :nodes="treeData"
          :loading="loading"
          :selected-id="localSelectedId"
          :filter-text="filterTextLocal"
          @node-click="onNodeClick"
        />
      </div>

      <!-- footer -->
      <div class="flex justify-between items-center px-5 py-4 border-t border-outline-variant bg-surface-container-lowest">
        <span class="text-body-md text-on-surface-variant">{{ localSelectedLabel || '未选择' }}</span>
        <div class="flex gap-3">
          <button
            class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md"
            @click="$emit('close')"
          >取消</button>
          <button
            class="px-7 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm"
            :disabled="!localSelectedId"
            @click="confirm"
          >确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import OrgTree from './OrgTree.vue'

export default {
  name: 'OrgTreePicker',
  components: { OrgTree },
  props: {
    visible: { type: Boolean, default: false },
    selectedId: { type: String, default: '' },
    selectedLabel: { type: String, default: '' }
  },
  data() {
    return {
      loading: false,
      treeData: [],
      filterText: '',
      localSelectedId: '',
      localSelectedLabel: '',
      dragState: null
    }
  },
  computed: {
    /** Pass to OrgTree only when non-empty, so the tree stays
     *  interactive (expandable) when search is cleared. */
    filterTextLocal() {
      const ft = (this.filterText || '').trim()
      return ft || undefined
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.localSelectedId = this.selectedId || ''
        this.localSelectedLabel = this.selectedLabel || ''
        this.filterText = ''
        if (!this.treeData.length) this.loadTree()
      }
    }
  },
  methods: {
    async loadTree() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/common/saOrgTree')
        if (res && res.obj) {
          this.treeData = res.obj
        }
      } catch (e) { this.treeData = [] }
      this.loading = false
    },

    onNodeClick(node) {
      this.localSelectedId = node.id
      this.localSelectedLabel = node.label
    },

    confirm() {
      if (this.localSelectedId) {
        this.$emit('select', { id: this.localSelectedId, label: this.localSelectedLabel })
      }
    },

    // ====== drag ======
    startDrag(e) {
      const el = this.$refs.dialogCard
      if (!el) return
      const cur = el.style.transform
      let ox = 0, oy = 0
      const m = cur.match(/translate3?d?\(([-\d.]+)px,?\s*([-\d.]+)px/)
      if (m) { ox = parseFloat(m[1]); oy = parseFloat(m[2]) }
      this.dragState = { el, startX: e.clientX, startY: e.clientY, origX: ox, origY: oy }
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'grabbing'
      document.addEventListener('mousemove', this.onDrag)
      document.addEventListener('mouseup', this.stopDrag)
    },
    onDrag(e) {
      if (!this.dragState || !this.dragState.el) return
      const d = this.dragState
      d.el.style.transform = 'translate3d(' + (d.origX + e.clientX - d.startX) + 'px,' + (d.origY + e.clientY - d.startY) + 'px,0)'
    },
    stopDrag() {
      if (!this.dragState) return
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      this.dragState = null
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
    }
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag)
    document.removeEventListener('mouseup', this.stopDrag)
  }
}
</script>
