<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="close">
    <div class="fixed inset-0 bg-black/30"></div>

    <div class="relative bg-container w-full max-w-2xl rounded-xl shadow-2xl flex flex-col" style="max-height:80vh;">
      <!-- Header -->
      <div class="flex items-center justify-between px-5 py-4 border-b border-border shrink-0">
        <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">add_reaction</span>
          选择图标
        </h3>
        <button class="text-outline hover:text-error transition-colors" @click="close">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Search -->
      <div class="px-5 py-3 border-b border-border shrink-0">
        <div class="grid grid-cols-1 grid-rows-1">
          <input
            ref="searchInput"
            v-model="searchInput"
            class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
            placeholder="搜索全部图标名称（支持中英文）"
          />
          <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[18px] pointer-events-none">search</span>
        </div>
      </div>

      <!-- Category Tabs with Scroll Arrows -->
      <div class="flex items-center shrink-0 px-5 py-2.5 border-b border-border select-none">
        <!-- Left scroll arrow -->
        <button
          class="shrink-0 w-6 h-6 flex items-center justify-center rounded hover:bg-surface-variant transition-colors"
          :class="canScrollLeft ? 'text-outline' : 'text-outline/20 pointer-events-none'"
          @click="scrollTabs(-200)"
        >
          <span class="material-symbols-outlined text-[16px]">chevron_left</span>
        </button>

        <!-- Tabs container -->
        <div
          ref="tabsContainer"
          class="flex gap-1.5 overflow-x-auto scrollbar-none mx-1"
          @scroll="updateScrollButtons"
        >
          <button
            ref="tabButtons"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg whitespace-nowrap transition-all text-[13px] font-medium shrink-0"
            :class="activeCategoryIndex === null ? 'bg-primary/10 text-primary' : 'text-outline hover:bg-surface-variant hover:text-on-surface'"
            @click="selectCategory(null)"
          >
            <span class="material-symbols-outlined" style="font-size: 16px;">apps</span>
            全部
            <span class="text-[11px] opacity-60 ml-0.5">{{ allIcons.length }}</span>
          </button>
          <button
            v-for="(cat, idx) in categories"
            :key="idx"
            ref="tabButtons"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg whitespace-nowrap transition-all text-[13px] font-medium shrink-0"
            :class="activeCategoryIndex === idx ? 'bg-primary/10 text-primary' : 'text-outline hover:bg-surface-variant hover:text-on-surface'"
            @click="selectCategory(idx)"
          >
            <span class="material-symbols-outlined" style="font-size: 16px;">{{ cat.icon }}</span>
            {{ cat.label }}
            <span class="text-[11px] opacity-60 ml-0.5">{{ cat.names.length }}</span>
          </button>
        </div>

        <!-- Right scroll arrow -->
        <button
          class="shrink-0 w-6 h-6 flex items-center justify-center rounded hover:bg-surface-variant transition-colors"
          :class="canScrollRight ? 'text-outline' : 'text-outline/20 pointer-events-none'"
          @click="scrollTabs(200)"
        >
          <span class="material-symbols-outlined text-[16px]">chevron_right</span>
        </button>
      </div>

      <!-- Icon Grid -->
      <div ref="gridContainer" class="flex-1 overflow-y-auto px-5 py-4">
        <div v-if="filteredIcons.length === 0" class="flex flex-col items-center justify-center py-12 text-on-surface-variant">
          <span class="material-symbols-outlined text-[48px] opacity-20">search_off</span>
          <p class="text-body-md mt-2">未找到匹配的图标</p>
        </div>

        <div v-else class="grid grid-cols-8 gap-2">
          <div
            v-for="icon in displayIcons"
            :key="icon"
            class="icon-item flex flex-col items-center justify-center p-2 rounded-lg cursor-pointer border-2 transition-all"
            :class="icon === selectedIcon ? 'border-primary bg-primary/5 shadow-sm' : 'border-transparent hover:border-outline-variant hover:bg-surface-container-low'"
            :title="icon + (zhNames[icon] ? ' — ' + zhNames[icon] : '')"
            @click="selectIcon(icon)"
          >
            <span class="material-symbols-outlined text-on-surface" style="font-size: 28px;">{{ icon }}</span>
            <span class="text-[8px] text-outline mt-0.5 w-full text-center leading-tight break-all">{{ zhNames[icon] || icon }}</span>
          </div>
        </div>

        <!-- Pagination Footer -->
        <div v-if="filteredIcons.length > 0" class="flex items-center justify-between pt-4 mt-4 border-t border-border">
          <span class="text-body-sm text-outline">共 {{ filteredIcons.length }} 个图标</span>
          <div v-if="totalPages > 1" class="flex items-center gap-2">
            <button
              class="w-8 h-8 rounded border border-outline-variant flex items-center justify-center hover:bg-surface-variant transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent"
              :disabled="page <= 1"
              @click="page--"
            >
              <span class="material-symbols-outlined text-[18px] text-on-surface">chevron_left</span>
            </button>
            <span class="text-body-md font-medium px-2 text-on-surface">{{ page }} / {{ totalPages }}</span>
            <button
              class="w-8 h-8 rounded border border-outline-variant flex items-center justify-center hover:bg-surface-variant transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent"
              :disabled="page >= totalPages"
              @click="page++"
            >
              <span class="material-symbols-outlined text-[18px] text-on-surface">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-between px-5 py-4 border-t border-border bg-surface-container-lowest shrink-0">
        <div class="flex items-center gap-3">
          <span v-if="selectedIcon" class="flex items-center gap-2 px-3 py-1.5 bg-primary/5 rounded-lg border border-primary/20">
            <span class="material-symbols-outlined text-primary text-[20px]">{{ selectedIcon }}</span>
            <span class="text-body-md text-primary font-medium">{{ zhNames[selectedIcon] || selectedIcon }}</span>
          </span>
          <span v-else class="text-body-md text-outline">未选择图标</span>
        </div>
        <div class="flex items-center gap-3">
          <button
            class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md"
            @click="close"
          >取消</button>
          <button
            class="px-7 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!selectedIcon"
            @click="confirm"
          >确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import categorizedData from '@/assets/icons/material-icons-categorized.json'
import zhNames from '@/assets/icons/material-icons-zh.json'

const PAGE_SIZE = 48

export default {
  name: 'IconPicker',
  props: {
    value: { type: String, default: '' },
    visible: { type: Boolean, default: false }
  },
  data() {
    return {
      categories: [],
      activeCategoryIndex: null,
      searchInput: '',
      searchQuery: '',
      searchInputTimer: null,
      selectedIcon: this.value || '',
      page: 1,
      isSearching: false,
      canScrollLeft: false,
      canScrollRight: false
    }
  },
  computed: {
    zhNames() {
      return zhNames
    },
    /** Flat list of all icon names across all categories */
    allIcons() {
      const names = []
      for (const cat of this.categories) {
        for (const name of cat.names) {
          names.push(name)
        }
      }
      return names
    },
    /** Icons filtered by search keyword (global search always searches all) */
    searchedIcons() {
      if (!this.searchQuery.trim()) return []
      const q = this.searchQuery.trim().toLowerCase()
      return this.allIcons.filter(name =>
        name.includes(q) || (zhNames[name] && zhNames[name].includes(q))
      )
    },
    /** Icons to show after applying category + search filters */
    filteredIcons() {
      if (this.isSearching) return this.searchedIcons
      if (this.activeCategoryIndex !== null) {
        const cat = this.categories[this.activeCategoryIndex]
        return cat ? cat.names : []
      }
      return this.allIcons
    },
    displayIcons() {
      const start = (this.page - 1) * PAGE_SIZE
      return this.filteredIcons.slice(start, start + PAGE_SIZE)
    },
    totalPages() {
      return Math.ceil(this.filteredIcons.length / PAGE_SIZE) || 1
    }
  },
  watch: {
    value(val) {
      this.selectedIcon = val || ''
    },
    visible(val) {
      if (val) {
        this.resetPagination()
        this.$nextTick(() => {
          this.updateScrollButtons()
          if (this.$refs.searchInput) this.$refs.searchInput.focus()
        })
      }
    },
    searchInput() {
      clearTimeout(this.searchInputTimer)
      this.searchInputTimer = setTimeout(() => {
        this.searchQuery = this.searchInput
        this.page = 1
        this.isSearching = !!this.searchInput.trim()
      }, 200)
    }
  },
  created() {
    this.categories = categorizedData.categories || []
  },
  mounted() {
    this.$nextTick(() => {
      this.updateScrollButtons()
    })
  },
  methods: {
    selectCategory(idx) {
      clearTimeout(this.searchInputTimer)
      this.activeCategoryIndex = idx
      this.searchInput = ''
      this.searchQuery = ''
      this.isSearching = false
      this.page = 1
      this.$nextTick(() => {
        if (this.$refs.gridContainer) {
          this.$refs.gridContainer.scrollTop = 0
        }
        // Scroll the selected tab into view
        this.scrollActiveTabIntoView()
      })
    },
    scrollActiveTabIntoView() {
      const container = this.$refs.tabsContainer
      if (!container || !this.$refs.tabButtons) return
      const activeIdx = this.activeCategoryIndex === null ? 0 : this.activeCategoryIndex + 1
      const activeBtn = this.$refs.tabButtons[activeIdx]
      if (!activeBtn) return
      const containerRect = container.getBoundingClientRect()
      const btnRect = activeBtn.getBoundingClientRect()
      if (btnRect.left < containerRect.left || btnRect.right > containerRect.right) {
        activeBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' })
      }
    },
    scrollTabs(offset) {
      const container = this.$refs.tabsContainer
      if (!container) return
      container.scrollBy({ left: offset, behavior: 'smooth' })
    },
    updateScrollButtons() {
      const container = this.$refs.tabsContainer
      if (!container) {
        this.canScrollLeft = false
        this.canScrollRight = false
        return
      }
      this.canScrollLeft = container.scrollLeft > 2
      this.canScrollRight = container.scrollLeft < container.scrollWidth - container.clientWidth - 2
    },
    resetPagination() {
      this.page = 1
      this.searchInput = ''
      this.searchQuery = ''
      this.isSearching = false
      this.selectedIcon = this.value || ''
      this.activeCategoryIndex = null
    },
    selectIcon(icon) {
      this.selectedIcon = icon
    },
    confirm() {
      this.$emit('input', this.selectedIcon)
      this.$emit('change', this.selectedIcon)
      this.$emit('close')
    },
    close() {
      this.selectedIcon = this.value || ''
      this.$emit('close')
    }
  },
  beforeDestroy() {
    clearTimeout(this.searchInputTimer)
  }
}
</script>

<style scoped>
.icon-item .material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 28;
  /* Force ligature rendering for long icon names */
  font-variant-ligatures: normal;
  font-feature-settings: 'liga' on;
}

/* Hide scrollbar for category tabs */
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.scrollbar-none::-webkit-scrollbar {
  display: none;
}
</style>
