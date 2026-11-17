<template>
  <header class="flex items-end justify-between gap-4 mb-5">
    <div class="flex items-center gap-3 min-w-0">
      <span
        v-if="icon"
        class="w-10 h-10 rounded-xl grid place-items-center shrink-0"
        :class="iconBg"
      >
        <span class="material-symbols-outlined text-[22px]" :class="iconColor">{{ icon }}</span>
      </span>
      <div class="min-w-0">
        <h1 class="text-[20px] md:text-[22px] font-semibold tracking-tight text-on-surface truncate">{{ title }}</h1>
        <p v-if="subtitle" class="text-[12px] text-outline mt-0.5 truncate">{{ subtitle }}</p>
      </div>
    </div>
    <div v-if="$slots.default" class="flex items-center gap-2 shrink-0">
      <slot />
    </div>
  </header>
</template>

<script>
export default {
  name: 'PageHeader',
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
    icon: { type: String, default: '' },
    /** primary | gold | danger | muted */
    tone: { type: String, default: 'primary' },
  },
  computed: {
    iconBg() {
      return {
        primary: 'bg-primary/10',
        gold: 'bg-amber-50',
        danger: 'bg-error/10',
        muted: 'bg-surface-container',
      }[this.tone] || 'bg-primary/10'
    },
    iconColor() {
      return {
        primary: 'text-primary',
        gold: 'text-amber-600',
        danger: 'text-error',
        muted: 'text-on-surface-variant',
      }[this.tone] || 'text-primary'
    },
  },
}
</script>
