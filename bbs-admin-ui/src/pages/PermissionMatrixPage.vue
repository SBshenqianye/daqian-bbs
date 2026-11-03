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
            只读视图：角色 → 权限点、权限点 → 角色、接口 → 注解。权限点显示中文名称，鼠标悬浮可查看内部编码。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-body-sm text-on-surface-variant">{{ matrix.generatedAt || '—' }}</span>
          <button
            class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60 text-body-sm flex items-center gap-1"
            :disabled="loading"
            @click="load"
          >
            <span class="material-symbols-outlined text-[16px]">refresh</span>
            {{ loading ? '加载中...' : '刷新' }}
          </button>
        </div>
      </div>

      <!-- Summary -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div class="bg-container border border-border rounded-xl p-card-padding">
          <p class="text-body-sm text-on-surface-variant">接口总数</p>
          <p class="font-headline-md text-headline-md">{{ matrix.summary.endpointCount || 0 }}</p>
        </div>
        <div class="bg-container border border-border rounded-xl p-card-padding">
          <p class="text-body-sm text-on-surface-variant">未声明注解的 /admin 接口</p>
          <p class="font-headline-md text-headline-md" :class="matrix.summary.undeclaredAdminCount ? 'text-error' : ''">
            {{ matrix.summary.undeclaredAdminCount || 0 }}
          </p>
        </div>
        <div class="bg-container border border-border rounded-xl p-card-padding">
          <p class="text-body-sm text-on-surface-variant">非角色表达式</p>
          <p class="font-headline-md text-headline-md" :class="matrix.summary.nonRoleExpressionCount ? 'text-amber-600' : ''">
            {{ matrix.summary.nonRoleExpressionCount || 0 }}
          </p>
        </div>
      </div>

      <div
        v-if="matrix.summary.undeclaredAdminCount || matrix.summary.nonRoleExpressionCount"
        class="mb-6 flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-body-sm text-amber-800"
      >
        <span class="material-symbols-outlined text-[18px]">warning</span>
        <span>
          检测到 {{ matrix.summary.undeclaredAdminCount }} 个 /admin 接口没有 <code>@PreAuthorize</code>，
          {{ matrix.summary.nonRoleExpressionCount }} 个接口使用非角色表达式。上线前建议确认这些接口的权限边界。
        </span>
      </div>

      <!-- 显示权限组 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-title-lg text-title-lg">显示系统角色 / 临时权限组</h3>
          <span class="text-[12px] text-outline">默认勾选四个系统角色</span>
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
            <span class="text-[11px] px-1.5 py-0.5 rounded" :class="isSystemGroup(group) ? 'bg-surface-variant text-on-surface-variant' : 'bg-amber-50 text-amber-700'">
              {{ groupKind(group) }}
            </span>
            <span v-if="isSystemGroup(group) && group.autoApply" class="text-[11px] text-emerald-700">自动应用</span>
          </label>
          <span v-if="!matrix.groups || matrix.groups.length === 0" class="text-body-sm text-outline">
            暂无权限组
          </span>
        </div>
      </div>

      <!-- 角色 → 权限点 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-title-lg text-title-lg">系统角色 / 临时权限组 → 权限点</h3>
          <span class="text-[12px] text-outline">鼠标悬浮权限点可查看内部编码</span>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          <div v-for="role in matrix.roles" :key="role.code" class="border border-outline-variant rounded-lg p-4">
            <div class="mb-2">
              <span
                class="font-title-md text-title-md cursor-help border-b border-dotted border-outline/50"
                :title="'user_type=' + role.userType + ' / ' + role.code"
              >{{ roleLabel(role.code) }}</span>
            </div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="code in role.permissions"
                :key="code"
                class="text-[12px] px-2 py-0.5 rounded-full bg-primary-fixed/60 text-on-surface cursor-help border-b border-dotted border-outline/50"
                :title="code"
              >{{ permissionLabel(code) }}</span>
              <span v-if="!role.permissions || role.permissions.length === 0" class="text-body-sm text-on-surface-variant">
                无管理端权限点
              </span>
            </div>
          </div>
          <div
            v-for="group in visibleGroups"
            :key="'group-card-' + group.id"
            class="border border-primary/30 bg-primary/5 rounded-lg p-4"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="font-title-md text-title-md">{{ group.groupName }}</span>
              <span class="text-[11px] px-2 py-0.5 rounded-full" :class="isSystemGroup(group) ? 'bg-primary-fixed/60 text-on-surface' : 'bg-amber-50 text-amber-700'">
                {{ groupKind(group) }}
              </span>
            </div>
            <div class="text-[11px] text-outline mb-2">
              {{ isSystemGroup(group) ? '角色基线：' + roleLabel(group.baseRole) : '临时叠加/收回' }}
              · {{ (group.permissions || []).length }} 项
              <template v-if="isSystemGroup(group)"> · {{ group.autoApply ? '自动应用' : '已停用（回退角色基线）' }} {{ group.roleUserCount || 0 }} 人</template>
            </div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="code in group.permissions"
                :key="code"
                class="text-[12px] px-2 py-0.5 rounded-full bg-primary-fixed/60 text-on-surface cursor-help border-b border-dotted border-outline/50"
                :title="code"
              >{{ permissionLabel(code) }}</span>
              <span v-if="!group.permissions || group.permissions.length === 0" class="text-body-sm text-on-surface-variant">
                无权限
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 权限点 → 角色 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-title-lg text-title-lg">权限点 → 角色</h3>
          <span class="text-[12px] text-outline">鼠标悬浮权限点可查看内部编码</span>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-body-sm">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant">权限点</th>
                <th v-for="role in roleColumns" :key="role.key" class="px-3 py-2 text-center font-medium text-on-surface-variant">
                  {{ role.label }}
                </th>
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
                <td v-for="role in roleColumns" :key="role.key" class="px-3 py-2 text-center">
                  <span v-if="permission[role.key]" class="text-emerald-600">✓</span>
                  <span v-else class="text-outline">—</span>
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
          <div class="flex items-center gap-3">
            <label class="flex items-center gap-2 text-body-sm text-on-surface-variant cursor-pointer">
              <input v-model="onlyIssues" type="checkbox" class="accent-primary" />
              只看未声明/非角色
            </label>
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索路径 / 注解 / 控制器"
              class="px-3 py-1.5 w-64 bg-surface border border-outline-variant rounded-lg text-body-sm outline-none focus:border-primary"
            />
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full min-w-[1200px] text-body-sm">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap">方法</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap min-w-[280px]">路径</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant min-w-[320px]">注解表达式</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap min-w-[150px]">解析角色</th>
                <th class="px-3 py-2 text-left font-medium text-on-surface-variant whitespace-nowrap min-w-[260px]">处理器</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in filteredEndpoints" :key="item.method + ' ' + item.path" class="hover:bg-surface-container-low/50">
                <td class="px-3 py-2 align-top whitespace-nowrap">
                  <span class="text-[11px] px-2 py-0.5 rounded bg-surface-variant text-on-surface-variant">{{ item.method }}</span>
                </td>
                <td class="px-3 py-2 align-top font-mono text-[12px] whitespace-nowrap">{{ item.path }}</td>
                <td class="px-3 py-2 align-top font-mono text-[12px] break-all">
                  <span v-if="item.declared">{{ item.expression }}</span>
                  <span v-else class="text-error font-semibold">未声明</span>
                </td>
                <td class="px-3 py-2 align-top">
                  <template v-if="item.roles && item.roles.length">
                    <span
                      v-for="role in item.roles"
                      :key="role"
                      class="inline-block mr-1 mb-1 text-[11px] px-2 py-0.5 rounded-full bg-primary-fixed/60 cursor-help border-b border-dotted border-outline/50"
                      :title="role"
                    >{{ roleLabel(role) }}</span>
                  </template>
                  <span v-else-if="item.nonRoleExpression" class="text-amber-600">非角色表达式</span>
                  <span v-else class="text-outline">—</span>
                </td>
                <td class="px-3 py-2 align-top text-[12px] text-on-surface-variant font-mono whitespace-nowrap">{{ item.handler }}</td>
              </tr>
              <tr v-if="filteredEndpoints.length === 0">
                <td colspan="5" class="px-3 py-6 text-center text-on-surface-variant">没有匹配的接口</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p class="text-[12px] text-on-surface-variant mt-3">
          说明：本页只读取代码中的角色/权限点与已注册的 <code>@PreAuthorize</code>，不会修改任何权限。
          接口上的注解仍是最终边界；权限点用于菜单与业务层判断。
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import {
  PERMISSION_LABELS,
  groupKindLabel,
  isSystemGroup as isSystemGroupData,
  labelOf,
  roleLabelOf
} from '@/utils/labels'

export default {
  name: 'PermissionMatrixPage',
  data() {
    return {
      loading: false,
      keyword: '',
      onlyIssues: false,
      matrix: {
        generatedAt: '',
        summary: {},
        roles: [],
        permissions: [],
        groups: [],
        endpoints: []
      },
      visibleGroupIds: [],
      roleColumns: [
        { key: 'user', label: '普通用户' },
        { key: 'moderator', label: '版主' },
        { key: 'admin', label: '管理员' },
        { key: 'super', label: '超级管理员' }
      ]
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
        if (this.onlyIssues && item.declared && !item.nonRoleExpression) return false
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
    roleLabel(code) {
      return roleLabelOf(code)
    },
    permissionLabel(code) {
      return labelOf(PERMISSION_LABELS, code, code)
    },
    isSystemGroup(group) {
      return isSystemGroupData(group)
    },
    groupKind(group) {
      return groupKindLabel(group)
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
            roles: [],
            permissions: [],
            groups: [],
            endpoints: []
          }, resp.obj)
          const defaultGroups = (this.matrix.groups || []).filter(group => group.system || group.baseRole)
          this.visibleGroupIds = defaultGroups.map(group => group.id)
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
