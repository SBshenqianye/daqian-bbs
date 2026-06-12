<template>
  <aside
    class="stitch-sidebar bg-surface-container-lowest border-r border-outline-variant overflow-y-auto overflow-x-hidden transition-all duration-300 ease-in-out flex flex-col"
    :class="collapse ? 'w-16' : 'w-64'"
    @dragleave="onSidebarDragLeave"
  >
    <nav class="flex-1 py-3 px-2 space-y-0.5">
      <template v-for="(item, index) in menuItems">
        <div
          :key="item.label"
          draggable="true"
          @dragstart="onDragStart($event, index)"
          @dragover.prevent="onDragOver(index)"
          @dragenter.prevent="onDragEnter(index)"
          @dragleave="onDragLeave($event, index)"
          @drop.prevent="onDrop(index)"
          @dragend="onDragEnd"
          class="relative sidebar-menu-item"
          :class="{
            'opacity-40 dragover-opacity': dragIndex === index,
            'border-t-2 border-primary': dragOverIndex === index && dragIndex !== null && dragIndex !== index,
          }"
        >
          <!-- Drag handle indicator -->
          <div
            v-show="!collapse"
            class="drag-handle absolute left-0 top-0 bottom-0 w-5 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-grab"
            :class="{ 'opacity-100': dragIndex === index }"
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
                  :class="isActive(child.path) ? 'text-primary bg-primary-fixed/50 font-semibold is-child-active' : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'"
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
            <!-- Active indicator dot -->
            <span v-if="isActive(item.path) && collapse" class="absolute right-1 w-1.5 h-1.5 rounded-full bg-primary"></span>
          </router-link>
        </div>
      </template>
    </nav>

    <!-- Bottom collapse toggle -->
    <div class="p-2 border-t border-outline-variant/50">
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

const STORAGE_KEY = 'bbs-admin-sidebar-order'

export default {
  name: 'Sidebar',
  data() {
    return {
      collapse: false,
      showLabels: true,
      dragIndex: null,
      dragOverIndex: null,
      menuItems: [
        { icon: 'dashboard', label: '系统首页', path: '/dashboard' },
        { icon: 'group', label: '用户管理', path: '/user' },
        { icon: 'article', label: '帖子管理', path: '/article' },
        { icon: 'label', label: '标签管理', path: '/articleLable' },
        { icon: 'tune', label: '配置管理', path: '/dict' },
        { icon: 'emoji_events', label: '积分排名', path: '/points' },
        { icon: 'business', label: '单位管理', path: '/unitManage' },
        { icon: 'block', label: '敏感词管理', path: '/sensitiveWord' },
        // { icon: 'view_carousel', label: '轮播图管理', path: '/slideshow' },
        // { icon: 'bar_chart', label: '文章统计', path: '/statistic' },
        // { icon: 'edit_note', label: '写文章', path: '/write' },
      ].map(item => ({ ...item, _open: false })),
    }
  },
  watch: {
    $route() {
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
    })
    // Load saved order from localStorage
    this.loadMenuOrder()
    // Auto-expand on mount
    this.$nextTick(() => {
      this.menuItems.forEach(item => {
        if (item.children) {
          item._open = item.children.some(child => this.isActive(child.path))
        }
      })
    })
  },
  methods: {
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
    toggleSidebar() {
      const willCollapse = !this.collapse
      if (willCollapse) {
        // Collapsing: hide text immediately, then shrink sidebar
        this.showLabels = false
      }
      this.collapse = willCollapse
      bus.$emit('collapse', this.collapse)
      if (!willCollapse) {
        // Expanding: sidebar has 300ms transition, show text after it finishes
        setTimeout(() => { this.showLabels = true }, 310)
      }
    },

    // --- Drag and Drop ---
    onDragStart(e, index) {
      this.dragIndex = index
      e.dataTransfer.effectAllowed = 'move'
      e.dataTransfer.setData('text/plain', index.toString())
      // Add dragging class after a tick for visual feedback
      this.$nextTick(() => {
        e.target.classList.add('dragging')
      })
    },
    onDragOver(index) {
      this.dragOverIndex = index
    },
    onDragEnter(index) {
      this.dragOverIndex = index
    },
    onDragLeave(e, index) {
      // Only clear if actually leaving this item's wrapper (not entering a child)
      const wrapper = e.currentTarget
      const relatedTarget = e.relatedTarget
      if (!wrapper.contains(relatedTarget)) {
        if (this.dragOverIndex === index) {
          this.dragOverIndex = null
        }
      }
    },
    onDrop(index) {
      if (this.dragIndex === null || this.dragIndex === index) {
        this.cleanupDrag()
        return
      }
      // Reorder: splice out the dragged item and insert at target position
      const [movedItem] = this.menuItems.splice(this.dragIndex, 1)
      // If target index is after the removed item, it shifted by -1
      const targetIndex = index > this.dragIndex ? index - 1 : index
      this.menuItems.splice(targetIndex, 0, movedItem)
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
      }
    },
    cleanupDrag() {
      this.dragIndex = null
      this.dragOverIndex = null
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
.stitch-sidebar {
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}
.stitch-sidebar:hover {
  scrollbar-color: #c3c6d7 transparent;
}
.stitch-sidebar::-webkit-scrollbar {
  width: 4px;
}
.stitch-sidebar::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
}
.stitch-sidebar:hover::-webkit-scrollbar-thumb {
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
