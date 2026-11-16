<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">leaderboard</span>
            排名管理
          </h1>
          <p class="text-body-md text-secondary mt-1">
            配置排行榜分类；从左侧组织树拖拽单位到右侧分类。分类之间允许重叠，组织榜按人均得分排序。
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

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-5">
        <!-- Left: org tree -->
        <section class="lg:col-span-4 bg-container border border-border rounded-xl p-card-padding">
          <div class="flex items-center justify-between mb-3">
            <h2 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-[20px]">account_tree</span>
              组织树
            </h2>
            <span class="text-[11px] text-on-surface-variant">拖拽到右侧分类</span>
          </div>
          <div class="relative mb-3">
            <input
              v-model="filterText"
              class="w-full h-9 pl-8 pr-3 bg-surface border border-outline-variant rounded-lg text-body-md text-on-surface placeholder:text-outline focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none"
              placeholder="搜索单位名称..."
            >
            <span class="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-outline" style="font-size:16px">search</span>
          </div>
          <div class="max-h-[65vh] overflow-y-auto pr-1">
            <OrgTree
              :nodes="orgTree"
              :filter-text="filterText"
              :loading="treeLoading"
              draggable-nodes
              @node-dragstart="onNodeDragStart"
            />
          </div>
        </section>

        <!-- Right: categories -->
        <section class="lg:col-span-8 space-y-4">
          <div v-if="loading" class="bg-container border border-border rounded-xl p-card-padding flex items-center justify-center py-16">
            <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
          </div>

          <template v-else>
            <div
              v-for="category in categories"
              :key="category.id"
              class="bg-container border rounded-xl p-card-padding transition-colors"
              :class="category.legacy ? 'border-outline-variant bg-surface-container-lowest' : 'border-border'"
              @dragover.prevent="onDragOver(category, $event)"
              @drop.prevent="onDrop(category, $event)"
            >
              <div class="flex flex-wrap items-start justify-between gap-3 mb-3">
                <div class="min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h3 class="font-headline-sm text-headline-sm text-on-surface">{{ category.categoryName }}</h3>
                    <span v-if="category.legacy" class="px-2 py-0.5 rounded-full bg-surface-variant text-on-surface-variant text-[11px]">系统内置 · 沿用原逻辑</span>
                    <span v-else class="px-2 py-0.5 rounded-full text-[11px]" :class="category.enabled === 1 ? 'bg-green-50 text-green-700' : 'bg-surface-variant text-on-surface-variant'">
                      {{ category.enabled === 1 ? '启用' : '停用' }}
                    </span>
                    <span class="text-[11px] text-on-surface-variant">{{ category.categoryCode }}</span>
                  </div>
                  <p class="text-[12px] text-on-surface-variant mt-1">{{ category.description || '暂无说明' }}</p>
                </div>
                <div v-if="!category.legacy" class="flex items-center gap-1.5">
                  <button class="px-2.5 py-1.5 text-[12px] text-primary bg-primary/5 rounded hover:bg-primary/10" @click="openEdit(category)">
                    编辑
                  </button>
                  <button class="px-2.5 py-1.5 text-[12px] text-error bg-error/5 rounded hover:bg-error/10" @click="removeCategory(category)">
                    删除
                  </button>
                </div>
              </div>

              <!-- Legacy read-only content -->
              <div v-if="category.legacy" class="rounded-lg border border-dashed border-outline-variant bg-surface px-4 py-3 text-[12px] text-on-surface-variant">
                该分类对应现有“内江供电公司本部”榜单，继续使用原组织排名与“参与排名”开关，不需要在这里配置成员。
              </div>

              <!-- Custom members -->
              <template v-else>
                <div v-if="category.members && category.members.length" class="flex flex-wrap gap-2">
                  <div
                    v-for="member in category.members"
                    :key="member.orgNo"
                    class="inline-flex items-center gap-2 pl-3 pr-2 py-1.5 rounded-lg border border-outline-variant bg-surface"
                    :title="member.orgNo"
                  >
                    <span class="text-[12px] text-on-surface max-w-[220px] truncate">{{ member.orgName }}</span>
                    <label class="inline-flex items-center gap-1 text-[11px] text-on-surface-variant cursor-pointer select-none">
                      <input
                        type="checkbox"
                        class="w-3.5 h-3.5 text-primary border-outline-variant rounded"
                        :checked="member.includeDescendants === 1"
                        @change="toggleDescendants(category, member, $event)"
                      >
                      含下级
                    </label>
                    <button class="w-5 h-5 rounded-full text-outline hover:text-error hover:bg-error/10 flex items-center justify-center" title="移除" @click="removeMember(category, member)">
                      <span class="material-symbols-outlined" style="font-size:14px">close</span>
                    </button>
                  </div>
                </div>
                <div
                  class="rounded-lg border border-dashed px-4 py-6 text-center transition-colors"
                  :class="dragOverId === category.id ? 'border-primary bg-primary/5 text-primary' : 'border-outline-variant text-on-surface-variant'"
                >
                  <span class="material-symbols-outlined block mb-1" style="font-size:22px">add_box</span>
                  <p class="text-[12px]">从左侧组织树拖入单位；默认包含下级，可在成员标签上切换</p>
                </div>
              </template>
            </div>

            <div v-if="categories.length <= 1" class="bg-container border border-dashed border-outline-variant rounded-xl p-10 text-center text-on-surface-variant">
              <span class="material-symbols-outlined text-[40px] opacity-20 block mb-2">leaderboard</span>
              <p class="text-body-md">还没有自定义排行榜分类</p>
              <button class="mt-3 px-4 py-2 bg-primary text-on-primary rounded-lg text-[12px]" @click="openCreate">新建第一个分类</button>
            </div>
          </template>
        </section>
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
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-label-md text-secondary mb-1">分类编码</label>
                <input v-model="form.categoryCode" maxlength="50" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="留空自动生成">
              </div>
              <div>
                <label class="block text-label-md text-secondary mb-1">排序</label>
                <input v-model.number="form.sortOrder" type="number" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="0">
              </div>
            </div>
            <div>
              <label class="block text-label-md text-secondary mb-1">说明</label>
              <textarea v-model="form.description" rows="3" maxlength="500" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none resize-none" placeholder="这个分类包含哪些单位"></textarea>
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
import OrgTree from '../components/OrgTree.vue'
import { handleResponse } from '../../../shared/feedback'

export default {
  name: 'RankingManagePage',
  components: { OrgTree },
  data() {
    return {
      loading: false,
      treeLoading: false,
      saving: false,
      orgTree: [],
      categories: [],
      filterText: '',
      dragPayload: null,
      dragOverId: null,
      dialogVisible: false,
      form: {
        id: null,
        categoryName: '',
        categoryCode: '',
        description: '',
        sortOrder: 0,
        enabled: true,
      },
    }
  },
  mounted() {
    this.loadAll()
  },
  methods: {
    async loadAll() {
      this.loading = true
      this.treeLoading = true
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
        this.treeLoading = false
      }
    },
    onNodeDragStart(payload) {
      this.dragPayload = payload
    },
    onDragOver(category) {
      if (category.legacy) return
      this.dragOverId = category.id
    },
    async onDrop(category, event) {
      this.dragOverId = null
      if (category.legacy) {
        this.$message.info('“内江供电公司本部”沿用原逻辑，不需要配置成员')
        return
      }
      let payload = this.dragPayload
      try {
        const raw = event.dataTransfer.getData('application/x-bbs-org')
        if (raw) payload = JSON.parse(raw)
      } catch (e) { /* ignore */ }
      if (!payload || !payload.orgNo) {
        this.$message.warning('请从左侧组织树拖入单位')
        return
      }
      try {
        const res = await this.postRequest('/admin/ranking/category/org/add', {
          categoryId: category.id,
          orgNo: payload.orgNo,
          includeDescendants: true,
        })
        handleResponse(res, {
          successMsg: `已加入：${payload.orgName || payload.orgNo}`,
          errorMsg: '加入失败',
          onSuccess: () => this.loadAll(),
        })
      } catch (e) {
        this.$message.error('加入失败')
      }
    },
    async toggleDescendants(category, member, event) {
      const include = event.target.checked
      try {
        const res = await this.postRequest('/admin/ranking/category/org/update', {
          categoryId: category.id,
          orgNo: member.orgNo,
          includeDescendants: include,
        })
        handleResponse(res, {
          successMsg: include ? '已改为包含下级' : '已改为仅本级',
          errorMsg: '更新失败',
          onSuccess: () => this.loadAll(),
        })
      } catch (e) {
        this.$message.error('更新失败')
      }
    },
    removeMember(category, member) {
      this.$confirm(`确定从「${category.categoryName}」移除「${member.orgName}」吗？`, '移除成员', { type: 'warning' })
        .then(async () => {
          const res = await this.postRequest('/admin/ranking/category/org/remove', {
            categoryId: category.id,
            orgNo: member.orgNo,
          })
          handleResponse(res, {
            successMsg: '已移除',
            errorMsg: '移除失败',
            onSuccess: () => this.loadAll(),
          })
        })
        .catch(() => {})
    },
    openCreate() {
      this.form = {
        id: null,
        categoryName: '',
        categoryCode: '',
        description: '',
        sortOrder: this.nextSortOrder(),
        enabled: true,
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
    nextSortOrder() {
      const max = this.categories.reduce((m, c) => Math.max(m, Number(c.sortOrder) || 0), 0)
      return max + 10
    },
  },
}
</script>
