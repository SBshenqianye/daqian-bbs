<template>
  <div ref="root" class="relative inline-block">
    <button
      type="button"
      class="inline-flex items-center justify-between gap-2 rounded-lg border border-outline-variant bg-surface text-on-surface hover:border-primary/40 transition-colors outline-none focus:border-primary/40"
      :class="triggerClass"
      :aria-expanded="open ? 'true' : 'false'"
      @click="toggle"
    >
      <span class="truncate">{{ currentLabel }}</span>
      <span class="material-symbols-outlined shrink-0 text-outline transition-transform" :class="{ 'rotate-180': open }">expand_more</span>
    </button>

    <transition name="bbs-select-fade">
      <div
        v-if="open"
        class="absolute right-0 z-40 mt-1 min-w-full w-max max-w-[220px] py-1 rounded-lg border border-border bg-container shadow-lg overflow-hidden"
      >
        <button
          v-for="opt in options"
          :key="String(opt.value)"
          type="button"
          class="w-full flex items-center justify-between gap-3 px-3 py-1.5 text-left text-[12px] whitespace-nowrap transition-colors"
          :class="String(opt.value) === String(value) ? 'bg-primary/10 text-primary' : 'text-on-surface hover:bg-surface-variant'"
          @click="pick(opt)"
        >
          <span>{{ opt.label }}</span>
          <span v-if="String(opt.value) === String(value)" class="material-symbols-outlined text-[14px]">check</span>
        </button>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'BBSSelect',
  props: {
    value: { type: [String, Number], default: '' },
    options: { type: Array, default: () => [] },
    placeholder: { type: String, default: '请选择' },
    size: { type: String, default: 'sm' },
  },
  data() {
    return { open: false }
  },
  computed: {
    currentLabel() {
      const hit = this.options.find(o => String(o.value) === String(this.value))
      return hit ? hit.label : this.placeholder
    },
    triggerClass() {
      return this.size === 'xs'
        ? 'h-7 pl-2 pr-1.5 text-[11px]'
        : 'h-8 pl-2.5 pr-1.5 text-[12px]'
    },
  },
  mounted() {
    document.addEventListener('click', this.onDocClick, true)
  },
  beforeDestroy() {
    document.removeEventListener('click', this.onDocClick, true)
  },
  methods: {
    toggle() {
      this.open = !this.open
    },
    pick(opt) {
      this.open = false
      if (String(opt.value) !== String(this.value)) this.$emit('input', opt.value)
      this.$emit('change', opt.value)
    },
    onDocClick(e) {
      if (!this.open) return
      const root = this.$refs.root
      if (root && !root.contains(e.target)) this.open = false
    },
  },
}
</script>

<style scoped>
.bbs-select-fade-enter-active,
.bbs-select-fade-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}
.bbs-select-fade-enter,
.bbs-select-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
