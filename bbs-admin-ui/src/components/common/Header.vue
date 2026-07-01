<template>
  <header class="stitch-header h-16 bg-container border-b border-outline-variant flex items-center justify-between px-4 sticky top-0 z-30 shadow-sm">
    <!-- Left: collapse button + logo -->
    <div class="flex items-center gap-3">
      <button
        class="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-surface-container-low transition-colors text-on-surface-variant"
        @click="toggleCollapse"
      >
        <span class="material-symbols-outlined text-[22px]">{{ localCollapse ? 'menu_open' : 'menu' }}</span>
      </button>
      <div class="flex items-center gap-2 select-none">
        <span class="material-symbols-outlined text-primary text-[26px]">forum</span>
        <span class="font-headline-sm text-headline-sm text-on-surface hidden sm:inline">大千智荟创新创意交流论坛</span>
      </div>
    </div>

    <!-- Right: user info -->
    <div class="relative" ref="userMenu">
      <!-- Trigger -->
      <div
        class="flex items-center gap-3 cursor-pointer px-3 py-1.5 rounded-lg hover:bg-surface-container-low transition-colors select-none"
        @click="showDropdown = !showDropdown"
      >
        <div class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center overflow-hidden border-2 border-surface-container-high ring-2 ring-white">
          <img :src="avatarUrl" @error="onAvatarError" class="w-full h-full object-cover" alt="avatar" />
        </div>
        <span class="font-label-md text-label-md text-on-surface hidden sm:inline">{{ username }}</span>
        <span class="material-symbols-outlined text-[16px] text-outline transition-transform duration-200" :class="{ 'rotate-180': showDropdown }">expand_more</span>
      </div>

      <!-- Dropdown Menu -->
      <transition name="fade-down">
        <div
          v-if="showDropdown"
          class="absolute top-full right-0 mt-2 w-52 bg-container border border-outline-variant rounded-xl shadow-lg overflow-hidden z-50 py-1"
        >
          <div class="px-4 py-3 border-b border-outline-variant/60">
            <p class="font-label-md text-label-md text-on-surface">{{ username }}</p>
            <p class="text-[11px] text-outline mt-0.5">管理员</p>
          </div>
          <a
            :href="userClientHref"
            target="_blank"
            class="flex items-center gap-3 px-4 py-2.5 hover:bg-surface-container-low transition-colors text-on-surface"
            @click="showDropdown = false"
          >
            <span class="material-symbols-outlined text-[20px] text-outline">open_in_new</span>
            <span class="font-body-md">前往用户界面</span>
          </a>
          <div class="border-t border-outline-variant/60 mx-3"></div>
          <button
            class="w-full flex items-center gap-3 px-4 py-2.5 hover:bg-surface-container-low transition-colors text-on-surface"
            @click="handleLogout"
          >
            <span class="material-symbols-outlined text-[20px] text-error">logout</span>
            <span class="font-body-md text-error">退出登录</span>
          </button>
        </div>
      </transition>

      <!-- Dropdown backdrop -->
      <div v-if="showDropdown" class="fixed inset-0 z-40" @click="showDropdown = false"></div>
    </div>
  </header>
</template>

<script>
import bus from './bus'

export default {
  name: 'Header',
  data() {
    return {
      localCollapse: false,
      showDropdown: false,
      adminInfo: null,
      userClientHref: process.env.VUE_APP_BBS_USER_API || '',
      defaultPortrait: require('@/assets/portrait.png'),
    }
  },
  computed: {
    username() {
      return (this.adminInfo && this.adminInfo.username) || '管理员'
    },
    avatarUrl() {
      const baseApi = process.env.VUE_APP_BBS_API || ''
      const portrait = this.adminInfo && this.adminInfo.portrait
      if (!portrait) return this.defaultPortrait
      const path = portrait.startsWith('/') ? portrait : '/' + portrait
      return baseApi + path
    },
  },
  created() {
    this.loadAdminInfo()
    bus.$on('collapse', (msg) => {
      this.localCollapse = msg
    })
  },
  methods: {
    loadAdminInfo() {
      try {
        const raw = window.sessionStorage.getItem('admin')
        this.adminInfo = raw ? JSON.parse(raw) : null
      } catch (e) {
        this.adminInfo = null
      }
    },
    toggleCollapse() {
      this.localCollapse = !this.localCollapse
      bus.$emit('collapse', this.localCollapse)
    },
    onAvatarError(e) {
      e.target.src = this.defaultPortrait
    },
    handleLogout() {
      this.showDropdown = false
      window.sessionStorage.removeItem('admin')
      this.$router.push('/login')
    },
  },
}
</script>

<style scoped>
.fade-down-enter-active,
.fade-down-leave-active {
  transition: all 0.2s ease;
}
.fade-down-enter,
.fade-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
