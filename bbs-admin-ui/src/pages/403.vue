<template>
  <div class="min-h-screen bg-surface flex items-center justify-center">
    <div class="text-center max-w-md px-8">
      <div class="mb-6 relative inline-block">
        <span class="text-[120px] font-bold text-error/20 absolute -top-6 -left-6 select-none">4</span>
        <span class="material-symbols-outlined text-[100px] text-error mx-4 relative z-10" style="font-variation-settings: 'FILL' 1;">block</span>
        <span class="text-[120px] font-bold text-error/20 absolute -top-6 -right-6 select-none">3</span>
      </div>
      <h1 class="font-headline-lg text-headline-lg text-on-surface mb-3">没有权限</h1>
      <p class="text-body-lg text-secondary mb-8">啊哦~ 你没有权限访问该页面哦</p>
      <div class="flex items-center justify-center gap-4">
        <router-link to="/dashboard" class="px-6 py-2.5 bg-primary text-on-primary font-headline-sm text-headline-sm rounded hover:opacity-90 transition-all shadow-sm inline-flex items-center gap-2">
          <span class="material-symbols-outlined text-[18px]">home</span>
          返回首页
        </router-link>
        <button class="px-6 py-2.5 border border-outline-variant text-on-surface rounded hover:bg-surface-container transition-all font-headline-sm text-headline-sm inline-flex items-center gap-2" @click="goBack">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          返回上一页
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: '403',
  methods: {
    goBack() {
      this.$router.go(-1)
    }
  }
}
</script>
