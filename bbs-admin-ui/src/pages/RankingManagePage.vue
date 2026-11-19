<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-5xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">leaderboard</span>
            排名管理
          </h1>
          <p class="text-body-md text-secondary mt-1">
            配置排行榜分类与参与单位；拖动卡片调整榜单顺序。勾选父级包含全部下级；取消某个下级即可只包含部分下级。
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md"
            @click="openCreate"
          >
            <span class="material-symbols-outlined text-[18px]">add</span>
            新建分类
          </button>
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md"
            @click="loadAll"
          >
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            刷新
          </button>
        </div>
      </div>

      <div v-if="loading" class="bg-container border border-border rounded-xl p-card-padding flex items-center justify-center py-16">
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <template v-else>
        <draggable
          v-model="categories"
          handle=".category-drag-handle"
          ghost-class="category-drag-ghost"
          :animation="150"
          @end="onCategoryDragEnd"
        >
          <div
            v-for="category in categories"
            :key="category.id"
            class="bg-container border border-border rounded-xl p-card-padding mb-4"
          >
            <div class="flex flex-wrap items-start justify-between gap-3 mb-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span
                    class="material-symbols-outlined category-drag-handle cursor-move text-outline flex-shrink-0"
                    title="拖动调整榜单顺序"
                  >drag_indicator</span>
                  <h3 class="font-headline-sm text-headline-sm text-on-surface">{{ category.categoryName }}</h3>
                  <span class="px-2 py-0.5 rounded-full text-[11px]" :class="category.enabled === 1 ? 'bg-green-50 text-green-700' : 'bg-surface-variant text-on-surface-variant'">
                    {{ category.enabled === 1 ? '启用' : '停用' }}
                  </span>
                  <span class="text-[11px] text-on-surface-variant">{{ category.categoryCode }}</span>
                </div>
                <p class="text-[12px] text-on-surface-variant mt-1">{{ category.description || '暂无说明' }}</p>
              </div>
              <div class="flex items-center gap-1.5">
                <button class="px-3 py-1.5 text-[12px] text-primary bg-primary/5 rounded hover:bg-primary/10" @click="openSelect(category)">
                  配置参与单位
                </button>
                <button class="px-2.5 py-1.5 text-[12px] text-primary bg-primary/5 rounded hover:bg-primary/10" @click="openEdit(category)">
                  编辑
                </button>
                <button
                  v-if="isBuiltIn(category)"
                  class="px-2.5 py-1.5 text-[12px] text-on-surface-variant bg-surface-variant/60 rounded hover:bg-surface-variant"
                  @click="resetCategory(category)"
                >
                  恢复默认设置
                </button>
                <button class="px-2.5 py-1.5 text-[12px] text-error bg-error/5 rounded hover:bg-error/10" @click="removeCategory(category)">
                  删除
                </button>
              </div>
            </div>

          <div v-if="includeMembers(category).length" class="flex flex-wrap gap-2">
            <div
              v-for="member in includeMembers(category)"
              :key="member.orgNo"
              class="inline-flex items-center gap-1.5 pl-3 pr-2.5 py-1.5 rounded-lg border border-outline-variant bg-surface"
              :title="member.orgNo"
            >
              <span class="text-[12px] text-on-surface max-w-[240px] truncate">{{ member.orgName }}</span>
              <span
                v-if="member.includeDescendants === 1"
                class="px-1.5 py-0.5 rounded bg-primary/10 text-primary text-[10px]"
              >含下级</span>
            </div>
            <span v-if="excludeCount(category)" class="inline-flex items-center px-2.5 py-1.5 rounded-lg bg-surface-variant text-[11px] text-on-surface-variant">
              已排除 {{ excludeCount(category) }} 处下级
            </span>
          </div>
          <div v-else class="rounded-lg border border-dashed border-outline-variant px-4 py-5 text-center text-[12px] text-on-surface-variant">
            尚未配置参与单位，该榜单暂无数据
          </div>
        </div>
        </draggable>

        <div v-if="!categories.length" class="bg-container border border-dashed border-outline-variant rounded-xl p-10 text-center text-on-surface-variant">
          <span class="material-symbols-outlined text-[40px] opacity-20 block mb-2">leaderboard</span>
          <p class="text-body-md">还没有排行榜分类</p>
          <button class="mt-3 px-4 py-2 bg-primary text-on-primary rounded-lg text-[12px]" @click="openCreate">新建第一个分类</button>
        </div>
      </template>

      <!-- 参与单位选择弹窗 -->
      <div v-if="selectVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="selectVisible = false"></div>
        <div class="relative bg-container w-full max-w-2xl rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <div>
              <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">account_tree</span>
                配置参与单位
              </h3>
              <p class="text-[12px] text-on-surface-variant mt-1">当前分类：{{ selectTarget && selectTarget.categoryName }}</p>
            </div>
            <button class="text-outline hover:text-error" @click="selectVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5">
            <div class="relative mb-3">
              <input
                v-model="treeFilter"
                class="w-full h-9 pl-8 pr-3 bg-surface border border-outline-variant rounded-lg text-body-md text-on-surface placeholder:text-outline focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none"
                placeholder="搜索单位名称..."
                @input="filterTree"
              >
              <span class="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-outline" style="font-size:16px">search</span>
            </div>
            <div class="max-h-[52vh] overflow-y-auto border border-outline-variant rounded-lg p-2">
              <el-tree
                ref="rankTree"
                :data="orgTree"
                node-key="id"
                show-checkbox
                :props="{ label: 'label', children: 'children' }"
                :filter-node-method="filterNode"
                :default-expand-all="true"
                class="ranking-select-tree"
                @check="onTreeCheck"
              />
            </div>
            <p class="mt-2 text-[11px] text-on-surface-variant">
              勾选父级 = 本级及全部下级参与；取消某个下级后父级显示为部分选中，仅勾选的下级参与。父级被自动勾选只表示下级已全选，不会单独成榜。
            </p>
          </div>
          <div class="flex items-center justify-between gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <div class="flex items-center gap-2">
              <button class="px-3 py-2 text-[12px] text-on-surface-variant hover:text-on-surface" @click="clearSelection">清空</button>
              <button
                v-if="isBuiltIn(selectTarget)"
                class="px-3 py-2 text-[12px] text-primary bg-primary/5 rounded-lg hover:bg-primary/10"
                @click="resetDefaultMembers"
              >
                恢复默认成员
              </button>
            </div>
            <div class="flex gap-3">
              <button class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant" @click="selectVisible = false">取消</button>
              <button class="px-6 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-50" :disabled="saving" @click="saveSelection">
                {{ saving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Category dialog -->
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="dialogVisible = false"></div>
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">{{ form.id ? 'edit' : 'add_circle' }}</span>
              {{ form.id ? '编辑分类' : '新建分类' }}
            </h3>
            <button class="text-outline hover:text-error" @click="dialogVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="block text-label-md text-secondary mb-1">分类名称 <span class="text-error">*</span></label>
              <input v-model="form.categoryName" maxlength="100" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="如：地市支撑机构">
            </div>
            <div>
              <label class="block text-label-md text-secondary mb-1">分类编码</label>
              <input
                v-model="form.categoryCode"
                :disabled="form.builtIn"
                maxlength="50"
                class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none disabled:bg-surface-variant disabled:text-on-surface-variant"
                placeholder="留空自动生成"
              >
              <p class="mt-1 text-[10px] text-on-surface-variant">
                {{ form.builtIn ? '内置分类编码用于系统识别，不可修改。' : '仅用于系统识别，用户端不展示；留空自动生成。' }}
                榜单顺序在列表中拖动卡片调整。
              </p>
            </div>
            <div>
              <label class="block text-label-md text-secondary mb-1">说明</label>
              <textarea v-model="form.description" rows="3" maxlength="500" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none resize-none" placeholder="这个分类包含哪些单位"></textarea>
              <p class="mt-1 text-[10px] text-on-surface-variant">仅管理端可见，用户端榜单不会展示说明。</p>
            </div>
            <div class="flex items-center gap-2">
              <input id="ranking-enabled" v-model="form.enabled" type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded">
              <label for="ranking-enabled" class="text-body-md text-on-surface">启用该分类（用户端显示）</label>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant" @click="dialogVisible = false">取消</button>
            <button class="px-6 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-50" :disabled="saving" @click="saveCategory">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import draggable from 'vuedraggable'

const BUILTIN_CODES = ['legacy_root', 'county_units', 'company_units', 'support_units', 'collective_units']
const BUILTIN_NAMES = ['内江供电公司本部', '县级供电企业', '公司所属各单位', '地市支撑机构', '集体企业及相关单位']

export default {
  name: 'RankingManagePage',
  components: { draggable },
  data() {
    return {
      loading: false,
      saving: false,
      orgTree: [],
      categories: [],
      // 参与单位选择
      selectVisible: false,
      selectTarget: null,
      treeFilter: '',
      // 显式勾选的节点（真正参与成榜的根），排除 el-tree 自动勾选的父级
      explicitKeys: [],
      // 分类编辑
      dialogVisible: false,
      form: {
        id: null,
        categoryName: '',
        categoryCode: '',
        description: '',
        sortOrder: 0,
        enabled: true,
        builtIn: false,
      },
    }
  },
  mounted() {
    this.loadAll()
  },
  methods: {
    async loadAll() {
      this.loading = true
      try {
        const [treeRes, categoryRes] = await Promise.all([
          this.getRequestUrl('/common/saOrgTree'),
          this.getRequestUrl('/admin/ranking/categories'),
        ])
        this.orgTree = treeRes && treeRes.code == 200 ? (treeRes.obj || []) : []
        this.categories = categoryRes && categoryRes.code == 200 ? (categoryRes.obj || []) : []
      } catch (e) {
        this.orgTree = []
        this.categories = []
        this.$message.error('加载排名配置失败')
      } finally {
        this.loading = false
      }
    },

    includeMembers(category) {
      return (category.members || []).filter(m => m.selectionType !== 'exclude')
    },
    excludeCount(category) {
      return (category.members || []).filter(m => m.selectionType === 'exclude').length
    },
    isBuiltIn(category) {
      return !!category && (BUILTIN_CODES.includes(category.categoryCode) || BUILTIN_NAMES.includes(category.categoryName))
    },

    async onCategoryDragEnd() {
      const orders = this.categories.map((c, i) => ({ id: c.id, sortOrder: (i + 1) * 10 }))
      this.categories.forEach((c, i) => { c.sortOrder = (i + 1) * 10 })
      try {
        const res = await this.postRequest('/admin/ranking/category/sort', orders)
        handleResponse(res, {
          successMsg: '排序已保存',
          errorMsg: '排序保存失败',
          onError: () => this.loadAll(),
        })
      } catch (e) {
        this.$message.error('排序保存失败')
        this.loadAll()
      }
    },
    async resetDefaultMembers() {
      if (!this.selectTarget) return
      try {
        const res = await this.getRequestUrl(`/admin/ranking/category/defaultMembers?id=${this.selectTarget.id}`)
        if (!res || res.code != 200 || !Array.isArray(res.obj)) {
          this.$message.error((res && res.message) || '恢复默认成员失败')
          return
        }
        const tree = this.$refs.rankTree
        this.explicitKeys = res.obj.slice()
        if (tree) {
          tree.setCheckedKeys([])
          tree.setCheckedKeys(res.obj)
        }
        this.$message.success('已载入默认参与单位，点击“保存”后生效')
      } catch (e) {
        this.$message.error('恢复默认成员失败')
      }
    },

    openSelect(category) {
      this.selectTarget = category
      this.treeFilter = ''
      this.selectVisible = true
      this.$nextTick(() => this.applySelection(category))
    },
    applySelection(category) {
      const tree = this.$refs.rankTree
      if (!tree) return
      const members = category.members || []
      const includes = members.filter(m => m.selectionType !== 'exclude')
      this.explicitKeys = includes.map(m => m.orgNo)
      tree.setCheckedKeys([])
      // 含下级的节点：整棵子树勾选
      tree.setCheckedKeys(includes.filter(m => m.includeDescendants === 1).map(m => m.orgNo))
      // 仅本级的节点：只勾选自身
      includes.filter(m => m.includeDescendants !== 1).forEach(m => {
        const node = tree.getNode(m.orgNo)
        if (node) node.setChecked(true, false)
      })
      // 剔除节点：连同下级一起取消
      members.filter(m => m.selectionType === 'exclude').forEach(m => {
        const node = tree.getNode(m.orgNo)
        if (node) node.setChecked(false, true)
      })
    },
    /** 只记录用户真正点选的节点，避免 el-tree 把“下级全选”的父级也算成成员 */
    onTreeCheck(data) {
      const tree = this.$refs.rankTree
      if (!tree) return
      const node = tree.getNode(data.id)
      if (!node) return
      if (node.checked) {
        if (!this.explicitKeys.includes(data.id)) this.explicitKeys.push(data.id)
        // 父级已覆盖的后代不再重复记录
        this.explicitKeys = this.explicitKeys.filter(k =>
          k === data.id || !this.isDescendantOf(k, data.id, tree))
      } else {
        this.explicitKeys = this.explicitKeys.filter(k =>
          k !== data.id && !this.isDescendantOf(k, data.id, tree))
      }
    },
    isDescendantOf(id, ancestorId, tree) {
      let node = tree.getNode(id)
      while (node && node.parent && node.parent.data && node.parent.data.id) {
        if (node.parent.data.id === ancestorId) return true
        node = node.parent
      }
      return false
    },
    hasCheckedDescendant(id, checkedSet, tree) {
      const node = tree.getNode(id)
      if (!node || !node.childNodes) return false
      for (const child of node.childNodes) {
        if (checkedSet.has(child.data.id)) return true
        if (this.hasCheckedDescendant(child.data.id, checkedSet, tree)) return true
      }
      return false
    },
    filterTree() {
      const tree = this.$refs.rankTree
      if (tree) tree.filter((this.treeFilter || '').trim() || null)
    },
    filterNode(value, data) {
      if (!value) return true
      return (data.label || '').toLowerCase().includes(value.toLowerCase())
    },
    clearSelection() {
      const tree = this.$refs.rankTree
      if (tree) tree.setCheckedKeys([])
      this.explicitKeys = []
    },
    async saveSelection() {
      const tree = this.$refs.rankTree
      if (!tree || !this.selectTarget) return
      const checkedSet = new Set(tree.getCheckedKeys())
      const checkedKeys = []
      checkedSet.forEach(id => {
        // 过滤 el-tree 因“下级全选”而自动勾选的父级；显式勾选的节点始终保留
        if (this.explicitKeys.includes(id) || !this.hasCheckedDescendant(id, checkedSet, tree)) {
          checkedKeys.push(id)
        }
      })
      const halfCheckedKeys = tree.getHalfCheckedKeys().filter(id => this.explicitKeys.includes(id))
      this.saving = true
      try {
        const res = await this.postRequest('/admin/ranking/category/org/setSelection', {
          categoryId: this.selectTarget.id,
          checkedKeys,
          halfCheckedKeys,
        })
        handleResponse(res, {
          successMsg: '保存成功',
          errorMsg: '保存失败',
          onSuccess: () => {
            this.selectVisible = false
            this.loadAll()
          },
        })
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },

    openCreate() {
      this.form = {
        id: null,
        categoryName: '',
        categoryCode: '',
        description: '',
        sortOrder: this.nextSortOrder(),
        enabled: true,
        builtIn: false,
      }
      this.dialogVisible = true
    },
    openEdit(category) {
      this.form = {
        id: category.id,
        categoryName: category.categoryName,
        categoryCode: category.categoryCode,
        description: category.description || '',
        sortOrder: category.sortOrder || 0,
        enabled: category.enabled === 1,
        builtIn: this.isBuiltIn(category),
      }
      this.dialogVisible = true
    },
    async saveCategory() {
      if (!this.form.categoryName || !this.form.categoryName.trim()) {
        this.$message.warning('请输入分类名称')
        return
      }
      this.saving = true
      try {
        const res = await this.postRequest('/admin/ranking/category/save', {
          id: this.form.id,
          categoryName: this.form.categoryName.trim(),
          categoryCode: this.form.categoryCode.trim(),
          description: this.form.description,
          sortOrder: this.form.sortOrder || 0,
          enabled: this.form.enabled ? 1 : 0,
        })
        handleResponse(res, {
          successMsg: '保存成功',
          errorMsg: '保存失败',
          onSuccess: () => {
            this.dialogVisible = false
            this.loadAll()
          },
        })
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },
    removeCategory(category) {
      this.$confirm(`删除分类「${category.categoryName}」后，成员配置也会删除，确定继续吗？`, '删除分类', { type: 'warning' })
        .then(async () => {
          const res = await this.postRequest('/admin/ranking/category/delete', { id: category.id })
          handleResponse(res, {
            successMsg: '删除成功',
            errorMsg: '删除失败',
            onSuccess: () => this.loadAll(),
          })
        })
        .catch(() => {})
    },
    resetCategory(category) {
      this.$confirm(
        `将把「${category.categoryName}」恢复为内置默认：名称、说明、排序和启用状态会重置，参与单位保持不变。确定继续吗？`,
        '恢复默认',
        { type: 'warning' }
      )
        .then(async () => {
          const res = await this.postRequest('/admin/ranking/category/resetDefault', { id: category.id })
          handleResponse(res, {
            successMsg: '已恢复默认配置',
            errorMsg: '恢复失败',
            onSuccess: () => this.loadAll(),
          })
        })
        .catch(() => {})
    },
    nextSortOrder() {
      const max = this.categories.reduce((m, c) => Math.max(m, Number(c.sortOrder) || 0), 0)
      return max + 10
    },
  },
}
</script>

<style scoped>
.ranking-select-tree {
  font-size: 13px;
}
.category-drag-ghost {
  opacity: 0.4;
}
</style>
