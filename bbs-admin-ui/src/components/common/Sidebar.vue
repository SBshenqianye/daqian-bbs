<template>
  <aside
    class="stitch-sidebar bg-surface-container-lowest border-r border-outline-variant overflow-x-hidden transition-all duration-300 ease-in-out flex flex-col h-full"
    :class="collapse ? 'w-16' : 'w-64'"
    @dragleave="onSidebarDragLeave"
  >
    <nav class="stitch-nav flex-1 min-h-0 overflow-y-auto overflow-x-hidden py-3 px-2 space-y-0.5">
      <template v-for="group in menuGroups">
        <div
          :key="'group-' + group.key"
          class="px-3 pt-3 pb-1 text-[11px] font-semibold tracking-wider text-outline uppercase"
          :class="{ 'hidden': collapse }"
        >
          {{ group.label }}
        </div>
        <template v-for="(item, index) in group.items">
          <div
            :key="item.label"
            draggable="true"
            @dragstart="onDragStart($event, group.key, index)"
            @dragover.prevent="onDragOver(group.key, index)"
            @dragenter.prevent="onDragEnter(group.key, index)"
            @dragleave="onDragLeave($event, group.key, index)"
            @drop.prevent="onDrop(group.key, index)"
            @dragend="onDragEnd"
            class="relative sidebar-menu-item"
            :class="{
              'opacity-40 dragover-opacity': dragGroup === group.key && dragIndex === index,
              'border-t-2 border-primary': dragOverGroup === group.key && dragOverIndex === index && dragIndex !== null && dragIndex !== index,
            }"
          >
            <!-- Drag handle indicator -->
            <div
              v-show="!collapse"
              class="drag-handle absolute left-0 top-0 bottom-0 w-5 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-grab"
              :class="{ 'opacity-100': dragGroup === group.key && dragIndex === index }"
            >
              <span class="material-symbols-outlined text-[14px] text-outline">drag_indicator</span>
            </div>

            <!-- Item with submenu -->
            <div v-if="item.children && item.children.length">
              <button
                class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 font-body-md text-body-md"
                :class="[
                  isParentActive(item) ? 'text-primary bg-primary-fixed/60 font-semibold is-parent-active' : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface',
                  !collapse ? 'pl-8' : ''
                ]"
                @click="toggleSubmenu(item)"
              >
                <span class="material-symbols-outlined text-[22px] flex-shrink-0">{{ item.icon }}</span>
                <span v-show="showLabels" class="truncate flex-1 text-left">{{ item.label }}</span>
                <span v-show="showLabels" class="material-symbols-outlined text-[16px] transition-transform duration-200" :class="{ 'rotate-180': item._open }">expand_more</span>
              </button>
              <transition name="submenu">
                <div v-show="item._open && showLabels" class="ml-8 mt-0.5 space-y-0.5 border-l-2 border-outline-variant/30 pl-2">
                  <router-link
                    v-for="child in item.children"
                    :key="child.label"
                    :to="child.path"
                    class="block px-3 py-2 rounded-lg transition-all duration-200 font-body-md text-body-md"
                    :class="[isActive(child.path) ? 'text-primary bg-primary-fixed/50 font-semibold is-child-active' : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface', { 'hidden': !canSee(child) }]"
                  >
                    {{ child.label }}
                  </router-link>
                </div>
              </transition>
            </div>

            <!-- Regular item -->
            <router-link
              v-else
              :to="item.path"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 font-body-md text-body-md group"
              :class="[
                isActive(item.path) ? 'text-primary bg-primary-fixed/60 font-semibold shadow-md' : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface',
                !collapse ? 'pl-8' : ''
              ]"
            >
              <span class="material-symbols-outlined text-[22px] flex-shrink-0 transition-colors" :class="isActive(item.path) ? 'text-primary' : 'text-outline group-hover:text-on-surface-variant'">{{ item.icon }}</span>
              <span v-show="showLabels" class="truncate">{{ item.label }}</span>
              <!-- 未处理红点：展开态显示数量，折叠态显示小圆点（#6） -->
              <span v-if="badgeOf(item) > 0 && showLabels" class="ml-auto bg-error text-on-error text-[11px] font-medium leading-none px-1.5 py-1 rounded-full">{{ badgeOf(item) > 99 ? '99+' : badgeOf(item) }}</span>
              <span v-if="badgeOf(item) > 0 && !showLabels" class="absolute top-1 right-1 w-2 h-2 rounded-full bg-error"></span>
              <!-- Active indicator dot -->
              <span v-if="isActive(item.path) && collapse" class="absolute right-1 w-1.5 h-1.5 rounded-full bg-primary"></span>
            </router-link>
          </div>
        </template>
      </template>
    </nav>

    <!-- Bottom collapse toggle -->
    <div class="p-2 border-t border-outline-variant/50 flex-shrink-0">
      <button
        class="w-full flex items-center justify-center gap-2 px-3 py-2.5 rounded-lg text-on-surface-variant hover:bg-surface-container-low transition-colors font-label-md text-label-md"
        @click="toggleSidebar"
      >
        <span class="material-symbols-outlined text-[20px] transition-all duration-300">{{ collapse ? 'chevron_right' : 'chevron_left' }}</span>
        <span v-show="showLabels">收起侧栏</span>
      </button>
    </div>
  </aside>
</template>

<script>
import bus from './bus'
import adminStore from '@/utils/adminStore'
import { BADGE_REFRESH_EVENT } from '@/utils/badgeRefresh'

const STORAGE_KEY = 'bbs-admin-sidebar-order'

export default {
  name: 'Sidebar',
  data() {
    return {
      collapse: false,
      showLabels: true,
      labelTimer: null,
      dragIndex: null,
      dragOverIndex: null,
      dragGroup: null,
      dragOverGroup: null,
      // 未处理待办计数（#6），与 Dashboard 共用 /admin/dashboard/counts 接口
      badgeCounts: {
        articlePending: 0,
        violationActive: 0,
        reportPending: 0,
        appealPending: 0,
        moderatorComplaintPending: 0,
        adoptPending: 0,
        featuredPending: 0,
      },
      // 菜单 label → 待办计数键（按 label 关联，不受拖拽排序影响）
      badgeKeys: {
        '帖子管理': 'articlePending',
        '违规管理': 'violationActive',
        '举报管理': 'reportPending',
        '申诉管理': 'appealPending',
        '版主投诉': 'moderatorComplaintPending',
        '采纳审批': 'adoptPending',
        '精华审批': 'featuredPending',
      },
      menuItems: [
        // 业务管理：内容审核、互动治理、板块运营
        { icon: 'dashboard', label: '系统首页', path: '/dashboard', permission: 'dashboard:read', category: 'business' },
        { icon: 'article', label: '帖子管理', path: '/article', permission: 'article:read', category: 'business' },
        { icon: 'emoji_events', label: '积分排名', path: '/points', permission: 'points:read', category: 'business' },
        { icon: 'gavel', label: '违规管理', path: '/violation', permission: 'violation:review', category: 'business' },
        { icon: 'feedback', label: '举报管理', path: '/report', permission: 'report:review', category: 'business' },
        { icon: 'assignment', label: '申诉管理', path: '/appeal', permission: 'appeal:review', category: 'business' },
        { icon: 'check_circle', label: '采纳审批', path: '/approve-adopt', permission: 'adopt:review', category: 'business' },
        { icon: 'stars', label: '精华审批', path: '/featured-recommendation', permission: 'featured:review', category: 'business' },
        // 系统管理：账号、配置、组织、版主体系
        { icon: 'group', label: '用户管理', path: '/user', permission: 'user:read', category: 'system' },
        { icon: 'label', label: '标签管理', path: '/articleLable', permission: 'config:read', category: 'system' },
        { icon: 'tune', label: '配置管理', path: '/dict', permission: 'config:read', category: 'system' },
        { icon: 'account_tree', label: '组织管理', path: '/unitManage', permission: 'config:read', category: 'system' },
        { icon: 'block', label: '敏感词管理', path: '/sensitiveWord', permission: 'config:read', category: 'system' },
        { icon: 'shield', label: '版主管理', path: '/moderator', permission: 'moderator:manage', category: 'system' },
        { icon: 'feedback', label: '版主投诉', path: '/moderator-complaint', permission: 'moderator:complaint:review', category: 'system' },
  { icon: 'policy', label: '权限矩阵', path: '/permission-matrix', permission: 'permission:manage', category: 'system' },
        { icon: 'admin_panel_settings', label: '权限管理', path: '/permission-groups', permission: 'permission:manage', category: 'system' },
        { icon: 'receipt_long', label: '审计日志', path: '/audit', permission: 'audit:read', category: 'system' },
        // { icon: 'view_carousel', label: '轮播图管理', path: '/slideshow' },
        // { icon: 'bar_chart', label: '文章统计', path: '/statistic' },
        // { icon: 'edit_note', label: '写文章', path: '/write' },
      ].map(item => ({ ...item, _open: false })),
    }
  },
  computed: {
    /** 按“业务管理 / 系统管理”分组；无权限的菜单不进入分组 */
    menuGroups() {
      const defs = [
        { key: 'business', label: '业务管理' },
        { key: 'system', label: '系统管理' },
      ]
      return defs
        .map(def => ({
          ...def,
          items: this.menuItems.filter(item => item.category === def.key && this.canSee(item)),
        }))
        .filter(group => group.items.length > 0)
    },
  },
  watch: {
    $route() {
      // 路由变化：待办处理后返回侧栏时刷新红点计数
      this.loadBadgeCounts()
      // Auto-expand parent of active child
      this.menuItems.forEach(item => {
        if (item.children) {
          item._open = item.children.some(child => this.isActive(child.path))
        }
      })
    },
  },
  created() {
    bus.$on('collapse', (msg) => {
      this.collapse = msg
      this.syncLabels(msg)
    })
    // 待办操作成功后刷新红点（#9：处理完待办数字即时更新，无需等路由跳转）
    bus.$on(BADGE_REFRESH_EVENT, this.loadBadgeCounts)
    // 切回本页签时补一次刷新（覆盖在其他终端/页签处理待办的情况）
    document.addEventListener('visibilitychange', this.onVisibilityChange)
    // Load saved order from localStorage
    this.loadMenuOrder()
    this.loadBadgeCounts()
    // Auto-expand on mount
    this.$nextTick(() => {
      this.menuItems.forEach(item => {
        if (item.children) {
          item._open = item.children.some(child => this.isActive(child.path))
        }
      })
    })
  },
  beforeDestroy() {
    bus.$off(BADGE_REFRESH_EVENT, this.loadBadgeCounts)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
    clearTimeout(this.labelTimer)
  },
  methods: {
    /** 按登录返回的 permissions 过滤菜单（后端仍是最终边界） */
    canSee(item) {
      return adminStore.hasPermission(item.permission)
    },
    onVisibilityChange() {
      if (!document.hidden) this.loadBadgeCounts()
    },
    /** 某菜单项的未处理待办数（无映射或无计数返回 0） */
    badgeOf(item) {
      const key = this.badgeKeys[item.label]
      return key ? (this.badgeCounts[key] || 0) : 0
    },
    /** 拉取各模块待办计数（与 Dashboard 同源） */
    loadBadgeCounts() {
      this.postRequest('/admin/dashboard/counts', {}).then(resp => {
        if (resp && resp.code === 200 && resp.obj) {
          this.badgeCounts = Object.assign({}, this.badgeCounts, resp.obj)
        }
      }).catch(err => {
        console.warn('[Sidebar] loadBadgeCounts', err)
      })
    },
    isActive(path) {
      if (!path) return false
      return this.$route.path === path || this.$route.path.startsWith(path + '/')
    },
    isParentActive(item) {
      if (!item.children) return false
      return item.children.some(child => this.isActive(child.path))
    },
    toggleSubmenu(item) {
      item._open = !item._open
    },
    // Label visibility follows collapse state; driven by bus so top/bottom buttons
    // and initial auto-collapse (Home.vue) all behave identically
    syncLabels(collapse) {
      clearTimeout(this.labelTimer)
      if (collapse) {
        // Collapsing: hide text immediately, then shrink sidebar
        this.showLabels = false
      } else {
        // Expanding: sidebar has 300ms transition, show text after it finishes
        this.labelTimer = setTimeout(() => { this.showLabels = true }, 310)
      }
    },
    toggleSidebar() {
      bus.$emit('collapse', !this.collapse)
    },

    // --- Drag and Drop（仅允许同分类内排序） ---
    onDragStart(e, group, index) {
      this.dragGroup = group
      this.dragIndex = index
      e.dataTransfer.effectAllowed = 'move'
      e.dataTransfer.setData('text/plain', index.toString())
      // Add dragging class after a tick for visual feedback
      this.$nextTick(() => {
        e.target.classList.add('dragging')
      })
    },
    onDragOver(group, index) {
      this.dragOverGroup = group
      this.dragOverIndex = index
    },
    onDragEnter(group, index) {
      this.dragOverGroup = group
      this.dragOverIndex = index
    },
    onDragLeave(e, group, index) {
      // Only clear if actually leaving this item's wrapper (not entering a child)
      const wrapper = e.currentTarget
      const relatedTarget = e.relatedTarget
      if (!wrapper.contains(relatedTarget)) {
        if (this.dragOverGroup === group && this.dragOverIndex === index) {
          this.dragOverIndex = null
          this.dragOverGroup = null
        }
      }
    },
    onDrop(group, index) {
      if (this.dragGroup !== group || this.dragIndex === null || this.dragIndex === index) {
        this.cleanupDrag()
        return
      }
      const groupDef = this.menuGroups.find(g => g.key === group)
      if (!groupDef) {
        this.cleanupDrag()
        return
      }
      // 只重排当前分类内可见菜单
      const visible = groupDef.items.slice()
      const [movedItem] = visible.splice(this.dragIndex, 1)
      const targetIndex = index > this.dragIndex ? index - 1 : index
      visible.splice(targetIndex, 0, movedItem)

      // 保留该分类中因权限隐藏的项，避免拖拽后丢失
      const hiddenItems = this.menuItems.filter(item => item.category === group && !this.canSee(item))
      const newGroupItems = [...visible, ...hiddenItems]
      const otherItems = this.menuItems.filter(item => item.category !== group)
      this.menuItems = group === 'business'
        ? [...newGroupItems, ...otherItems]
        : [...otherItems, ...newGroupItems]

      // Persist the new order
      this.saveMenuOrder()
      this.cleanupDrag()
    },
    onDragEnd() {
      this.cleanupDrag()
    },
    onSidebarDragLeave(e) {
      // Clear hover state when leaving the sidebar entirely
      const sidebar = e.currentTarget
      const relatedTarget = e.relatedTarget
      if (!sidebar.contains(relatedTarget)) {
        this.dragOverIndex = null
        this.dragOverGroup = null
      }
    },
    cleanupDrag() {
      this.dragIndex = null
      this.dragOverIndex = null
      this.dragGroup = null
      this.dragOverGroup = null
    },

    // --- Persistence ---
    loadMenuOrder() {
      try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (!saved) return
        const order = JSON.parse(saved)
        if (!Array.isArray(order) || order.length === 0) return

        // Build a lookup by label
        const itemMap = {}
        this.menuItems.forEach(item => { itemMap[item.label] = item })

        const reordered = []
        const used = new Set()

        // Restore saved order, skipping items that no longer exist
        order.forEach(label => {
          if (itemMap[label] && !used.has(label)) {
            reordered.push(itemMap[label])
            used.add(label)
          }
        })

        // Append any items that were added later (not in saved order)
        this.menuItems.forEach(item => {
          if (!used.has(item.label)) {
            reordered.push(item)
          }
        })

        if (reordered.length === this.menuItems.length) {
          this.menuItems = reordered
        }
      } catch (e) {
        // Corrupted data — silently use default order
      }
    },
    saveMenuOrder() {
      try {
        const order = this.menuItems.map(item => item.label)
        localStorage.setItem(STORAGE_KEY, JSON.stringify(order))
      } catch (e) {
        // localStorage unavailable or full — silently ignore
      }
    },
  },
}
</script>

<style scoped>
.stitch-nav {
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}
.stitch-nav:hover {
  scrollbar-color: #c3c6d7 transparent;
}
.stitch-nav::-webkit-scrollbar {
  width: 4px;
}
.stitch-nav::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
}
.stitch-nav:hover::-webkit-scrollbar-thumb {
  background: #c3c6d7;
}

.submenu-enter-active,
.submenu-leave-active {
  transition: all 0.2s ease;
}
.submenu-enter,
.submenu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* --- Drag & Drop --- */

/* Make menu items show grab cursor */
.sidebar-menu-item {
  cursor: grab;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.sidebar-menu-item:active {
  cursor: grabbing;
}

/* Hide the drag handle by default; show on hover */
.sidebar-menu-item .drag-handle {
  opacity: 0;
  transition: opacity 0.15s ease;
}

.sidebar-menu-item:hover .drag-handle {
  opacity: 1;
}

/* When the item itself is being dragged, make the handle always visible */
.sidebar-menu-item.dragover-opacity .drag-handle,
.sidebar-menu-item.opacity-40 .drag-handle {
  opacity: 1;
}

/* Ensure the drag-over border doesn't overlap too aggressively */
.sidebar-menu-item.border-t-2.border-primary {
  margin-top: 0;
}

/* Subtle shadow on dragged item ghost */
.dragging {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* --- Active state accent bar --- */

/* Left accent bar for all active items */
.sidebar-menu-item .is-parent-active,
.sidebar-menu-item .router-link-active {
  position: relative;
  overflow: hidden;
}
.sidebar-menu-item .is-parent-active::before,
.sidebar-menu-item .router-link-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  bottom: 5px;
  width: 3px;
  background-color: var(--md-sys-color-primary, #1976d2);
  border-radius: 0 3px 3px 0;
  transition: all 0.2s ease;
}
</style>
