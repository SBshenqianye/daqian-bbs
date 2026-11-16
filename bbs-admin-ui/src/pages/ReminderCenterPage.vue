<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-4xl mx-auto px-page-margin-desktop py-6">
      <div class="flex items-center justify-between mb-5">
        <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2"><span class="material-symbols-outlined text-primary">notifications</span>提醒中心</h1>
        <button class="px-4 py-2 bg-primary text-on-primary rounded-lg text-[12px]" @click="markAll">全部已读</button>
      </div>
      <div class="bg-container border border-border rounded-xl divide-y divide-outline-variant/40">
        <div v-for="n in list" :key="n.id" class="p-4 flex items-start gap-3" :class="n.isRead===0?'bg-primary/[0.02]':''">
          <span class="material-symbols-outlined text-primary mt-0.5">notifications</span>
          <div class="flex-1 min-w-0">
            <p class="text-body-md text-on-surface">{{ n.title }}</p>
            <p class="text-[11px] text-outline mt-1">{{ n.createTime }} · {{ n.type }}</p>
          </div>
          <span v-if="n.isRead===0" class="w-2 h-2 rounded-full bg-error mt-1"></span>
        </div>
        <div v-if="!list.length" class="p-10 text-center text-on-surface-variant">暂无提醒</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ReminderCenterPage',
  data() { return { list: [] } },
  mounted() { this.load() },
  methods: {
    userId() { try { const u = JSON.parse(sessionStorage.getItem('admin') || '{}'); return u.id } catch (e) { return null } },
    async load() {
      const id = this.userId(); if (!id) return
      const res = await this.getRequestUrl(`/notification/list?userId=${id}&page=1&size=50`)
      this.list = (res && res.obj && res.obj.records) || []
    },
    async markAll() {
      const id = this.userId(); if (!id) return
      await this.postRequest(`/notification/markAllRead?userId=${id}`, {})
      this.load()
    },
  },
}
</script>
