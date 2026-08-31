<template>
  <div class="bg-surface min-h-screen">
    <!-- Sticky Header -->
    <div ref="pageHeader" class="sticky top-0 z-20 bg-surface border-b border-outline-variant/30 shadow-sm">
      <div class="max-w-7xl mx-auto px-page-margin-desktop py-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">account_tree</span>
              组织管理
            </h1>
            <p class="text-body-md text-secondary mt-1">管理组织单位结构、配置排名参与和用户前台显示</p>
          </div>
          <div class="flex items-center gap-2">
            <!-- Search -->
            <div class="relative">
              <input
                v-model="filterText"
                class="w-52 h-9 pl-8 pr-3 bg-surface border border-outline-variant rounded-lg text-body-md text-on-surface placeholder:text-outline focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all"
                placeholder="搜索单位名称..."
              />
              <div class="absolute left-2.5 inset-y-0 flex items-center pointer-events-none">
                <span class="material-symbols-outlined text-outline leading-none" style="font-size:16px">search</span>
              </div>
              <button
                v-if="filterText"
                class="absolute right-1.5 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center rounded text-outline hover:text-on-surface hover:bg-surface-variant transition-all"
                @click="filterText = ''"
              >
                <span class="material-symbols-outlined" style="font-size:12px">close</span>
              </button>
            </div>
          </div>
          <div v-if="orgTree.length" class="flex items-center gap-1">
            <button
              class="inline-flex items-center gap-1 px-2.5 py-1.5 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
              style="font-size: 12px;"
              @click="$refs.orgTree.expandAll()"
            >
              <span class="material-symbols-outlined" style="font-size: 14px;">unfold_more</span>
              展开全部
            </button>
            <button
              class="inline-flex items-center gap-1 px-2.5 py-1.5 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
              style="font-size: 12px;"
              @click="$refs.orgTree.collapseAll()"
            >
              <span class="material-symbols-outlined" style="font-size: 14px;">unfold_less</span>
              收起全部
            </button>
          </div>
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!hasChanges || saving"
            @click="handleSave"
          >
            <span class="material-symbols-outlined text-[18px]">save</span>
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ⭐ 悬浮层级导航：完全复用 OrgTree 行结构与缩进 -->
    <!-- 始终渲染在 DOM 中（opacity 控制显示/隐藏），确保导航高度始终可测 -->
    <div
      class="org-floating-nav bg-container border border-border"
      :class="{ 'nav-visible': showFloatingNav && currentContextPath.length }"
      :style="{ top: navTop + 'px', left: navLeft + 'px', width: navWidth + 'px' }"
    >
      <div class="flex flex-col gap-0.5" style="padding: 4px 20px;">
      <div
        v-for="(item, idx) in currentContextPath"
        :key="item.id"
        class="group flex items-center gap-1 px-3 py-1.5 rounded-lg border mb-0.5 cursor-pointer"
        :class="[
          'd' + Math.min(item._treeDepth, 6),
          idx === currentContextPath.length - 1
            ? 'bg-primary/15 border-primary text-primary font-semibold'
            : 'bg-surface-container-low border-transparent hover:border-outline-variant/30 hover:bg-surface-container-low/80'
        ]"
        :title="item.label"
        @click.stop="navigateToNode(item)"
      >
        <!-- ↓↓↓ 与 OrgTree.vue 完全一致的内层结构（始终显示 chevron，全部为可折叠节点） ↓↓↓ -->
        <div class="flex items-center gap-1 min-w-0 flex-1 w-full">
          <!-- 箭头：所有项都可折叠，使用 tree-chevron + tree-copen 保证旋转动画 -->
          <button
            class="w-5 h-5 flex items-center justify-center rounded hover:bg-surface-variant transition-colors flex-shrink-0 -ml-0.5"
          >
            <span class="material-symbols-outlined tree-chevron tree-copen" style="font-size:14px">chevron_right</span>
          </button>

          <!-- 图标：全部为可折叠，永远显示 folder -->
          <span class="material-symbols-outlined flex-shrink-0 text-outline" style="font-size: 18px;">folder</span>

          <!-- 标签 -->
          <span class="flex-1 font-body-md truncate min-w-0 ml-1">{{ item.label }}</span>
        </div>
      </div>
      </div>
    </div>

    <!-- Content -->
    <div ref="contentWrap" class="max-w-7xl mx-auto px-page-margin-desktop py-6">

      <!-- Selected summary -->
      <div v-if="orgTree.length && (rankingSelectedCount > 0 || displaySelectedCount > 0)" class="mb-4 flex items-center gap-4 text-body-md">
        <div v-if="rankingSelectedCount > 0" class="px-3 py-1.5 bg-primary/5 border border-primary/15 rounded-lg flex items-center gap-1.5 text-primary">
          <span class="material-symbols-outlined" style="font-size:16px">emoji_events</span>
          排名：<strong>{{ rankingSelectedCount }}</strong> 个单位
        </div>
        <div v-if="displaySelectedCount > 0" class="px-3 py-1.5 bg-primary/5 border border-primary/15 rounded-lg flex items-center gap-1.5 text-primary">
          <span class="material-symbols-outlined" style="font-size:16px">visibility</span>
          显示：<strong>{{ displaySelectedCount }}</strong> 个节点
        </div>
      </div>

      <!-- Loading（始终挂载，CSS 切换，避免 v-if 销毁树 DOM 导致白屏） -->
      <div
        class="flex items-center justify-center py-16"
        :class="loading ? '' : 'hidden'"
      >
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- Tree Card（始终挂载，CSS 切换，OrgTree 内部已处理空状态） -->
      <div
        ref="treeCard"
        class="bg-container border border-border rounded-xl p-card-padding"
        :class="loading ? 'hidden' : ''"
      >
        <OrgTree
          ref="orgTree"
          mode="unit-manage"
          :nodes="orgTree"
          :filter-text="filterText"
          :loading="false"
          @node-click="onNodeClick"
          @toggle-ranking="toggleRanking"
          @toggle-display="toggleDisplay"
          @cascade-ranking="cascadeRanking"
          @cascade-display="cascadeDisplay"
          @add-node="openAdd"
          @delete-node="handleRemove"
        />
      </div>

      <!-- Add Dialog -->
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="dialogVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">add_circle</span>
              新增下级单位
            </h3>
            <button class="w-8 h-8 flex items-center justify-center rounded-full text-outline hover:bg-surface-variant transition-colors" @click="dialogVisible = false">
              <span class="material-symbols-outlined" style="font-size: 18px;">close</span>
            </button>
          </div>
          <div class="p-5">
            <div v-if="addParentLabel" class="mb-4 px-3 py-2 bg-surface-variant rounded-lg text-body-md text-on-surface-variant flex items-center gap-2">
              <span class="material-symbols-outlined" style="font-size: 16px;">arrow_upward</span>
              上级单位：{{ addParentLabel }}
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">单位名称</label>
              <input
                ref="orgNameInput"
                v-model="addOrgName"
                class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md"
                placeholder="请输入单位名称"
                maxlength="50"
                @keyup.enter="onSubmit"
              >
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="dialogVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="onSubmit">确认</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import OrgTree from '../components/OrgTree.vue'

function walkTree(nodes, fn) {
  if (!nodes || !Array.isArray(nodes)) return
  for (const n of nodes) {
    fn(n)
    if (n.children && n.children.length) walkTree(n.children, fn)
  }
}

export default {
  name: 'BBSUnitManage',
  components: { OrgTree },
  data() {
    return {
      loading: false,
      saving: false,
      orgTree: [],
      filterText: '',
      // 排名开关
      rankingMap: {},
      originalRanking: {},
      // 显示开关
      displayMap: {},
      originalDisplay: {},
      // 节点 id → 节点对象映射（快速父节点查找）
      nodeMap: {},
      // 新增弹窗
      dialogVisible: false,
      addPOrgNo: '',
      addParentLabel: '',
      addOrgName: '',
      // ⭐ 悬浮导航
      currentContextPath: [],
      showFloatingNav: false,
      navTop: 96,
      navLeft: 0,
      navWidth: 0,
    }
  },
  computed: {
    rankingSelectedCount() {
      return Object.values(this.rankingMap).filter(Boolean).length
    },
    displaySelectedCount() {
      return Object.values(this.displayMap).filter(Boolean).length
    },
    hasChanges() {
      return Object.keys(this.rankingMap).some(k => this.rankingMap[k] !== this.originalRanking[k])
          || Object.keys(this.displayMap).some(k => this.displayMap[k] !== this.originalDisplay[k])
    },
  },
  mounted() {
    this.loadData()
    // 页面实际在 Home.vue 的 overflow-y-auto 容器内滚动，不是 window
    let el = this.$el.parentElement
    while (el && el !== document.body) {
      if (window.getComputedStyle(el).overflowY === 'auto') break
      el = el.parentElement
    }
    this._scrollContainer = el || window
    this._scrollContainer.addEventListener('scroll', this._onScroll, { passive: true })
    // 挂载后测量 header 高度用于悬浮导航定位
    this.$nextTick(() => this._updateNavTop())
    // 窗口 resize 时重新测量导航宽度/位置
    window.addEventListener('resize', this._updateNavTop)
  },
  beforeDestroy() {
    if (this._scrollContainer) {
      this._scrollContainer.removeEventListener('scroll', this._onScroll)
    }
    window.removeEventListener('resize', this._updateNavTop)
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/common/saOrgTree')
        if (res.code == 200) {
          this.orgTree = res.obj || []
          // 建立节点 id → 节点索引，并补齐缺失的 pOrgNo（树结构通过 children 体现）
          this.nodeMap = {}
          const fillParentRef = (nodes, parentId) => {
            for (const n of nodes) {
              if (parentId && (n.pOrgNo == null || n.pOrgNo === '')) n.pOrgNo = parentId
              this.nodeMap[n.id] = n
              if (n.children && n.children.length) fillParentRef(n.children, n.id)
            }
          }
          fillParentRef(this.orgTree, null)
          // 初始化 maps
          this.originalRanking = {}
          this.displayMap = {}
          this.originalDisplay = {}
          walkTree(this.orgTree, n => {
            const rankVal = n.isRankingSelected === 1 || n.isRankingSelected === true
            this.$set(this.rankingMap, n.id, rankVal)
            this.$set(this.originalRanking, n.id, rankVal)
            const dispVal = n.isDisplaySelected === 1 || n.isDisplaySelected === true
            this.$set(this.displayMap, n.id, dispVal)
            this.$set(this.originalDisplay, n.id, dispVal)
          })
        } else {
          this.orgTree = []
        }
        // 设置初始层级路径
        this._setInitialContext()
      } catch (e) { this.orgTree = [] }
      this.loading = false
    },

    // ════════ 层级路径追踪 ════════

    /** 从任意节点向上回溯到根，返回 [根, …, 当前] 路径 */
    getPath(id) {
      if (!id || !this.nodeMap[id]) return []
      const path = []
      let current = this.nodeMap[id]
      while (current) {
        path.unshift(current)
        current = current.pOrgNo ? this.nodeMap[current.pOrgNo] : null
      }
      return path
    },

    /** 更新导航路径 */
    updateContext(node) {
      const id = (node && node.id) || ''
      if (!id || !this.nodeMap[id]) return
      this.currentContextPath = this.getPath(id)
    },

    /** 点击导航项 → toggle 折叠/展开并滚动到该节点 */
    navigateToNode(node) {
      if (!node || !this.$refs.orgTree) return
      this.$refs.orgTree.toggleNodeById(node.id)
      this.$nextTick(() => {
        const row = document.querySelector(`[data-nid="${node.id}"]`)
        if (row) row.scrollIntoView({ behavior: 'smooth', block: 'center' })
        this._onScroll()
      })
    },

    /** 加载数据后初始化为第一个根节点 */
    _setInitialContext() {
      this.showFloatingNav = false
      this.currentContextPath = []
    },

    /** 测量 pageHeader 在视口中的底部位置 → 悬浮导航的 top */
    _updateNavTop() {
      const el = this.$refs && this.$refs.pageHeader
      if (el) this.navTop = el.getBoundingClientRect().bottom
      // 从卡片元素测量，确保悬浮导航外框与卡片外框对齐
      const cardEl = this.$refs && this.$refs.treeCard
      if (cardEl) {
        const rect = cardEl.getBoundingClientRect()
        this.navLeft = rect.left
        this.navWidth = rect.width
      } else {
        // 回退：直接从 contentWrap 测量
        const contentEl = this.$refs && this.$refs.contentWrap
        if (contentEl) {
          const rect = contentEl.getBoundingClientRect()
          this.navLeft = rect.left
          this.navWidth = rect.width
        }
      }
    },

    /** ─── 滚动处理 ─── */
    _onScroll() {
      this._updateNavTop()

      const rows = document.querySelectorAll('[data-nid]')
      if (!rows.length || !this.orgTree.length) {
        this.showFloatingNav = false
        this.currentContextPath = []
        return
      }

      const headerBottom = this.navTop

      // ── 滞后显示/隐藏 ──
      const firstRect = rows[0].getBoundingClientRect()
      if (this.showFloatingNav) {
        if (firstRect.top >= headerBottom + 25) {
          this.showFloatingNav = false
          this.currentContextPath = []
          return
        }
      } else {
        if (firstRect.top >= headerBottom - 5) return
        this.showFloatingNav = true
      }

      // ── 扫描范围 = headerBottom + 导航高度（动态） ──
      //    有估算验证 + 路径前缀对齐保护，不会形成反馈回路。
      const navEl2 = this.$el && this.$el.querySelector('.org-floating-nav')
      const navH = navEl2 ? navEl2.offsetHeight : 55
      const threshold = headerBottom + navH

      const fullStack = []
      for (const row of rows) {
        const rect = row.getBoundingClientRect()
        if (rect.height === 0) continue
        if (!(rect.top < threshold)) break

        const node = this.nodeMap[row.dataset.nid]
        if (node && node.children && node.children.length > 0) {
          fullStack.push(Object.assign({}, node, {
            _treeDepth: this.getPath(node.id).length - 1,
            _rectBottom: rect.bottom
          }))
        }
      }

      // ── 确定路径：以 fullStack 末尾（最接近阈值）的节点为准 ──
      //    路径在第一个不可折叠的祖先处截断：若 child 因无 children 而被排除，
      //    则 grandchild 也不应出现（不能跳过中间层级）。
      let newPath = []
      if (fullStack.length > 0) {
        const rawPath = this.getPath(fullStack[fullStack.length - 1].id)
        for (const n of rawPath) {
          if (!(n.children && n.children.length > 0)) break
          newPath.push(n)
        }
      }

      // ── 检测同层替换 ──
      let _replaceId = null
      if (this._lastPath && newPath.length > 0) {
        for (let d = 0; d < Math.min(this._lastPath.length, newPath.length); d++) {
          if (this._lastPath[d].id !== newPath[d].id) {
            _replaceId = this._lastPath[d].id
            break
          }
        }
      }

      // ── 清理过期 pending（用户滚离旧上下文后自动清除） ──
      if (this._pendingReplaceId && !_replaceId) {
        const stillRelevant = fullStack.some(item => {
          const p = this.getPath(item.id)
          return p.some(n => n.id === this._pendingReplaceId)
        })
        if (!stillRelevant) this._pendingReplaceId = null
      }

      // ── 同层替换：两阶段 ──
      //    阶段1（pending）：删旧兄弟，不添新兄弟 → 导航变短
      //    阶段2（confirm）：新兄弟位置达到缩短后的导航底部 → 加入
      const rid = _replaceId || this._pendingReplaceId
      if (_replaceId) this._pendingReplaceId = _replaceId

      if (rid) {
        // 从 fullStack 末尾找第一个不在旧兄弟子树中的节点（最深的新节点）
        let checkItem = null
        for (let i = fullStack.length - 1; i >= 0; i--) {
          const p = this.getPath(fullStack[i].id)
          if (!p.some(n => n.id === rid)) { checkItem = fullStack[i]; break }
        }
        if (checkItem) {
          const dp = this.getPath(checkItem.id).filter(n => n.children && n.children.length > 0)
          // Fix1: 一致延迟的 checkH = navH - 20，节点需多滚动 20px 才被确认
          const checkH = Math.max(10, navH - 20)
          const row = document.querySelector('[data-nid="' + checkItem.id + '"]')
          if (row && row.getBoundingClientRect().top < headerBottom + checkH) {
            // 确认：新兄弟已到达缩短后的导航底部 → 切换
            newPath = dp
            this._pendingReplaceId = null
          } else {
            // 未确认：删旧（从当前导航内容中移除旧兄弟子树），不添新 → 导航变短
            this.currentContextPath = this.currentContextPath.filter(item => {
              const p = this.getPath(item.id)
              return !p.some(n => n.id === rid)
            })
            // Fix2: fallback 到 newPath 根节点而非 fullStack（避免重引入旧子树）
            if (!this.currentContextPath.length && newPath.length > 0) {
              this.currentContextPath = [Object.assign({}, newPath[0], {
                _treeDepth: this.getPath(newPath[0].id).length - 1
              })]
            }
            return
          }
        }
      }

      // Fix4: 简化展示，直接使用 newPath
      this._lastPath = newPath
      this.currentContextPath = newPath.map(n => Object.assign({}, n, {
        _treeDepth: this.getPath(n.id).length - 1
      }))
    },

    // ---- 树交互 ----
    onNodeClick(node) {
      if (node._hasChildren) {
        this.$refs.orgTree.toggleNode(node)
      }
      this.$nextTick(() => this._onScroll())
    },

    // ---- 排名开关 ----
    getRankingVal(id) {
      return this.rankingMap[id] === true
    },
    toggleRanking(node) {
      const val = !(this.rankingMap[node.id] === true)
      this.$set(this.rankingMap, node.id, val)
      node.isRankingSelected = val ? 1 : 0
    },
    cascadeRanking(node, selected) {
      const val = selected ? 1 : 0
      this.$set(this.rankingMap, node.id, !!val)
      node.isRankingSelected = val
      walkTree(node.children, child => {
        this.$set(this.rankingMap, child.id, !!val)
        child.isRankingSelected = val
      })
    },
    cascadeDisplay(node, selected) {
      const val = selected ? 1 : 0
      this.$set(this.displayMap, node.id, !!val)
      node.isDisplaySelected = val
      walkTree(node.children, child => {
        this.$set(this.displayMap, child.id, !!val)
        child.isDisplaySelected = val
      })
    },

    // ---- 显示开关 ----
    getDisplayVal(id) {
      return this.displayMap[id] === true
    },
    toggleDisplay(node) {
      const val = !(this.displayMap[node.id] === true)
      this.$set(this.displayMap, node.id, val)
      node.isDisplaySelected = val ? 1 : 0
    },

    // ---- 保存 ----
    async handleSave() {
      if (!this.hasChanges) {
        this.$message.info('没有需要保存的变更')
        return
      }
      this.saving = true
      try {
        const rankingChanged = Object.keys(this.rankingMap).some(k => this.rankingMap[k] !== this.originalRanking[k])
        const displayChanged = Object.keys(this.displayMap).some(k => this.displayMap[k] !== this.originalDisplay[k])

        const promises = []
        if (rankingChanged) {
          promises.push(this.postRequest('/common/saOrg/batchUpdateRanking', this.rankingMap))
        }
        if (displayChanged) {
          promises.push(this.postRequest('/common/saOrg/batchUpdateDisplay', this.displayMap))
        }

        const results = await Promise.all(promises)
        const allOk = results.every(r => r && r.code === 200)

        if (allOk) {
          this.$message.success('保存成功')
          // 更新原始状态
          Object.keys(this.rankingMap).forEach(k => { this.originalRanking[k] = this.rankingMap[k] })
          Object.keys(this.displayMap).forEach(k => { this.originalDisplay[k] = this.displayMap[k] })
        } else {
          // 部分失败时重新加载数据以恢复真实状态
          this.$message.error('部分配置保存失败，已重置')
          this.loadData()
        }
      } catch (e) {
        this.$message.error('保存失败')
        this.loadData()
      }
      this.saving = false
    },

    // ---- 新增单位 ----
    openAdd(data) {
      this.addPOrgNo = data.id
      this.addParentLabel = data.label
      this.addOrgName = ''
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.orgNameInput) this.$refs.orgNameInput.focus()
      })
    },
    async onSubmit() {
      if (!this.addOrgName.trim()) { this.$message.warning('请输入单位名称'); return }
      try {
        const res = await this.getRequestUrl(`/saOrg/addSaOrg?pOrgNo=${this.addPOrgNo}&orgName=${encodeURIComponent(this.addOrgName.trim())}`)
        if (res.code == 200) {
          this.$message.success('新增成功')
          this.dialogVisible = false
          this.addOrgName = ''
          this.loadData()
        } else {
          this.$message.error(res.message || '新增失败')
        }
      } catch (e) { this.$message.error('新增失败') }
    },

    // ---- 删除单位 ----
    handleRemove(data) {
      this.$confirm('确定删除该单位吗？', '提示', { type: 'warning' }).then(async () => {
        try {
          const res = await this.getRequestUrl(`/saOrg/deleteSaOrgByOrgNo?orgNo=${data.id}`)
          if (res.code == 200) { this.$message.success('删除成功'); this.loadData() }
          else { this.$message.error(res.message || '删除失败') }
        } catch (e) { this.$message.error('删除失败') }
      }).catch(() => {})
    },
  },
}
</script>

<style scoped>
.org-floating-nav {
  position: fixed;
  z-index: 999;
  /* 用 max-height 防止暴涨，超出内部滚动；高度在 0~190px 间自然变化 */
  max-height: 190px;
  overflow-y: auto;
  scrollbar-width: thin;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  will-change: transform, opacity;
  /* 默认隐藏：透明 + 禁止交互 */
  opacity: 0;
  transform: translateY(-4px);
  pointer-events: none;
  transition: opacity 0.12s ease, transform 0.12s ease;
}
.org-floating-nav.nav-visible {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

</style>
