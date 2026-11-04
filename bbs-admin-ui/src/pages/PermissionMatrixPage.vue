<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600">policy</span>
            权限矩阵
          </h1>
          <p class="text-body-md text-secondary mt-1">
            只读视图：权限组 → 权限点、权限点 → 权限组、接口 → 注解。权限点显示中文名称，鼠标悬浮可查看内部编码。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-body-sm text-on-surface-variant">{{ matrix.generatedAt || '—' }}</span>
          <button
            class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60 text-body-sm flex items-center gap-1"
            :disabled="loading"
            @click="load"
          >
            <span class="material-symbols-outlined" style="font-size:16px">refresh</span>
            {{ loading ? '加载中...' : '刷新' }}
          </button>
        </div>
      </div>

      <!-- Summary -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div class="bg-container border border-border rounded-xl p-card-padding">
          <p class="text-body-sm text-on-surface-variant">权限组</p>
          <p class="font-headline-md text-headline-md">{{ (matrix.groups || []).length }}</p>
        </div>
        <div class="bg-container border border-border rounded-xl p-card-padding">
          <p class="text-body-sm text-on-surface-variant">权限点</p>
          <p class="font-headline-md text-headline-md">{{ (matrix.permissions || []).length }}</p>
        </div>
        <div class="bg-container border border-border rounded-xl p-card-padding">
          <p class="text-body-sm text-on-surface-variant">接口</p>
          <p class="font-headline-md text-headline-md">{{ matrix.summary.endpointCount || 0 }}</p>
        </div>
      </div>

      <!-- 显示权限组 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-title-lg text-title-lg">显示权限组</h3>
          <span class="text-body-sm text-outline">勾选结果会保留，刷新后不丢失</span>
        </div>
        <div class="flex flex-wrap gap-x-6 gap-y-3">
          <label
            v-for="group in matrix.groups"
            :key="'visible-' + group.id"
            class="flex items-center gap-2 text-body-sm cursor-pointer"
          >
            <input
              type="checkbox"
              class="accent-primary"
              :checked="isGroupVisible(group.id)"
              @change="toggleGroupVisibility(group.id)"
            />
            <span class="text-on-surface" :title="group.groupCode">{{ group.groupName }}</span>
            <span v-if="group.enabled === 0" class="text-body-sm text-outline">（停用）</span>
          </label>
          <span v-if="!matrix.groups || matrix.groups.length === 0" class="text-body-sm text-outline">
            暂无权限组
          </span>
        </div>
      </div>

      <!-- 权限组 → 权限点 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-title-lg text-title-lg">权限组 → 权限点</h3>
          <span class="text-body-sm text-outline">鼠标悬浮权限点可查看内部编码</span>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          <div
            v-for="group in visibleGroups"
            :key="'group-card-' + group.id"
            class="border border-outline-variant rounded-lg p-4"
            :class="group.enabled === 0 ? 'opacity-60' : ''"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="font-title-md text-title-md" :title="group.groupCode">{{ group.groupName }}</span>
              <span class="text-body-sm px-2 py-0.5 rounded-full" :class="group.enabled === 0 ? 'bg-surface-variant text-on-surface-variant' : 'bg-primary-fixed/60 text-on-surface'">
                {{ group.enabled === 0 ? '停用' : '启用' }}
              </span>
            </div>
            <div class="text-body-sm text-outline mb-2">
              数据范围：{{ dataScopeLabel(group.dataScopeType) }}
              · {{ (group.permissions || []).length }} 项
            </div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="code in group.permissions"
                :key="code"
                class="text-body-sm px-2 py-0.5 rounded-full bg-primary-fixed/60 text-on-surface cursor-help border-b border-dotted border-outline/50"
                :title="code"
              >{{ permissionLabel(code) }}</span>
              <span v-if="!group.permissions || group.permissions.length === 0" class="text-body-sm text-on-surface-variant">
                无权限
              </span>
            </div>
          </div>
          <div v-if="visibleGroups.length === 0" class="text-body-sm text-on-surface-variant py-4">
            请在上方勾选要查看的权限组
          </div>
        </div>
      </div>

      <!-- 权限点 → 权限组 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-title-lg text-title-lg">权限点 → 权限组</h3>
          <span class="text-body-sm text-outline">鼠标悬浮权限点可查看内部编码</span>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-body-sm">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant">权限点</th>
                <th
                  v-for="group in visibleGroups"
                  :key="'gcol-' + group.id"
                  class="px-3 py-2 text-center font-medium text-on-surface-variant whitespace-nowrap"
                  :title="group.groupCode"
                >
                  {{ group.groupName }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="permission in matrix.permissions" :key="permission.code" class="hover:bg-surface-container-low/50">
                <td class="px-3 py-2">
                  <span
                    class="text-on-surface cursor-help border-b border-dotted border-outline/50"
                    :title="permission.code"
                  >{{ permissionLabel(permission.code) }}</span>
                </td>
                <td v-for="group in visibleGroups" :key="'gcell-' + group.id" class="px-3 py-2 text-center">
                  <span v-if="groupHasPermission(group, permission.code)" class="text-emerald-600">✓</span>
                  <span v-else class="text-outline">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 接口 → 注解 -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
          <h3 class="font-title-lg text-title-lg">接口 → 注解</h3>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索路径 / 注解 / 控制器"
            class="px-3 py-1.5 w-64 bg-surface border border-outline-variant rounded-lg text-body-sm outline-none focus:border-primary"
          />
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-body-sm" style="min-width: 1200px;">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap" style="width: 96px;">方法</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap" style="min-width: 280px;">路径</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant" style="min-width: 380px;">注解表达式</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap" style="min-width: 260px;">处理器</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in filteredEndpoints" :key="item.method + ' ' + item.path" class="hover:bg-surface-container-low/50">
                <td class="px-3 py-2 align-top whitespace-nowrap">
                  <span class="text-body-sm px-2 py-0.5 rounded bg-surface-variant text-on-surface-variant">{{ item.method }}</span>
                </td>
                <td class="px-3 py-2 align-top font-mono text-body-sm whitespace-nowrap">{{ item.path }}</td>
                <td class="px-3 py-2 align-top font-mono text-body-sm" style="white-space: normal; word-break: break-all;">
                  <span v-if="item.declared">{{ item.expression }}</span>
                  <span v-else class="text-outline">—</span>
                </td>
                <td class="px-3 py-2 align-top text-body-sm text-on-surface-variant font-mono whitespace-nowrap">{{ item.handler }}</td>
              </tr>
              <tr v-if="filteredEndpoints.length === 0">
                <td colspan="4" class="px-3 py-6 text-center text-on-surface-variant">没有匹配的接口</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p class="text-body-sm text-on-surface-variant mt-3">
          说明：本页只读取代码中的权限组、权限点与已注册的 <code>@PreAuthorize</code>，不会修改任何权限。
          接口上的注解是最终边界；权限点用于菜单与业务层判断。
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { PERMISSION_LABELS, labelOf } from '@/utils/labels'

const VISIBLE_GROUPS_KEY = 'permission-matrix-visible-groups'

export default {
  name: 'PermissionMatrixPage',
  data() {
    return {
      loading: false,
      keyword: '',
      matrix: {
        generatedAt: '',
        summary: {},
        permissions: [],
        groups: [],
        endpoints: []
      },
      visibleGroupIds: []
    }
  },
  computed: {
    visibleGroups() {
      const ids = this.visibleGroupIds.map(String)
      return (this.matrix.groups || []).filter(group => ids.indexOf(String(group.id)) !== -1)
    },
    filteredEndpoints() {
      const kw = (this.keyword || '').trim().toLowerCase()
      return (this.matrix.endpoints || []).filter(item => {
        if (!kw) return true
        const text = [item.path, item.expression, item.handler, item.method]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        return text.indexOf(kw) !== -1
      })
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    permissionLabel(code) {
      return labelOf(PERMISSION_LABELS, code, code)
    },
    dataScopeLabel(value) {
      return value === 'BOARD_MODERATOR' ? '按版主任职' : '全局'
    },
    isGroupVisible(id) {
      return this.visibleGroupIds.map(String).indexOf(String(id)) !== -1
    },
    toggleGroupVisibility(id) {
      const key = String(id)
      const index = this.visibleGroupIds.map(String).indexOf(key)
      if (index === -1) {
        this.visibleGroupIds.push(id)
      } else {
        this.visibleGroupIds.splice(index, 1)
      }
      this.saveVisibleGroups()
    },
    saveVisibleGroups() {
      try {
        window.localStorage.setItem(VISIBLE_GROUPS_KEY, JSON.stringify(this.visibleGroupIds))
      } catch (e) {
        // 忽略隐私模式等 localStorage 不可用场景
      }
    },
    restoreVisibleGroups(groups) {
      let saved = null
      try {
        const raw = window.localStorage.getItem(VISIBLE_GROUPS_KEY)
        if (raw) saved = JSON.parse(raw)
      } catch (e) {
        saved = null
      }
      const available = (groups || []).map(group => String(group.id))
      if (Array.isArray(saved)) {
        this.visibleGroupIds = saved.filter(id => available.indexOf(String(id)) !== -1)
        return
      }
      this.visibleGroupIds = (groups || []).map(group => group.id)
    },
    groupHasPermission(group, code) {
      return !!(group && group.permissions && group.permissions.indexOf(code) !== -1)
    },
    async load() {
      this.loading = true
      try {
        const resp = await this.getRequestUrl('/admin/permission/matrix')
        if (resp && resp.code == 200 && resp.obj) {
          this.matrix = Object.assign({
            generatedAt: '',
            summary: {},
            permissions: [],
            groups: [],
            endpoints: []
          }, resp.obj)
          this.restoreVisibleGroups(this.matrix.groups)
        }
      } catch (e) {
        console.warn('[PermissionMatrixPage] load', e)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
