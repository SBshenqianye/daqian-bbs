<template>
  <header
    class="fixed top-0 left-0 right-0 h-16 bg-container z-50 transition-shadow duration-300"
    :class="{ 'shadow-md border-b-0': scrolled, 'border-b border-outline-variant': !scrolled }"
  >
    <div class="max-w-7xl mx-auto h-full px-page-margin-desktop flex items-center justify-between gap-8">
      <!-- Left: Brand & Main Nav -->
      <div class="flex items-center gap-8 flex-shrink-0">
        <router-link
          to="/stitch-index"
          class="text-headline-lg font-semibold text-brand-blue tracking-tight no-underline"
        >
          大千智荟交流论坛
        </router-link>
        <nav class="hidden md:flex items-center gap-6">
          <router-link
            to="/stitch-index"
            class="font-label-md text-label-md pb-1 border-b-2 transition-colors"
            :class="isActive('/stitch-index') ? 'text-brand-blue border-brand-blue' : 'text-on-surface-variant border-transparent hover:text-brand-blue'"
          >
            论坛
          </router-link>
          <router-link
            to="/stitch-points"
            class="font-label-md text-label-md pb-1 border-b-2 transition-colors"
            :class="isActive('/stitch-points') ? 'text-brand-blue border-brand-blue' : 'text-on-surface-variant border-transparent hover:text-brand-blue'"
          >
            排行榜
          </router-link>
        </nav>
      </div>

      <!-- Middle: Search Bar -->
      <div class="flex-grow max-w-2xl hidden md:block">
        <div class="relative group">
          <span
            class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline transition-colors"
            :class="{ 'text-brand-blue': searchFocused }"
          >search</span>
          <input
            v-model="searchQuery"
            class="w-full h-10 pl-10 pr-4 bg-surface-container-low rounded-lg border border-transparent focus:border-brand-blue focus:bg-container focus:ring-0 transition-all font-body-md text-body-md outline-none"
            placeholder="搜索讨论、创意或人员..."
            type="text"
            @focus="searchFocused = true"
            @blur="searchFocused = false"
            @keyup.enter="handleSearch"
          >
        </div>
      </div>

      <!-- Right: Actions & Profile -->
      <div class="flex items-center gap-6 flex-shrink-0">
        <router-link
          to="/stitch-write"
          class="flex items-center gap-2 bg-brand-blue text-on-primary px-5 py-2 rounded-lg font-label-md text-label-md hover:bg-primary-container transition-all active:scale-95 no-underline"
        >
          <span class="material-symbols-outlined text-[20px]">add</span>
          发布
        </router-link>

        <!-- User Profile Section (logged in) -->
        <div
          v-if="isLogin && user"
          class="relative"
          :class="{ 'user-trigger-active': userMenuOpen }"
          @mouseenter="userMenuOpen = true"
          @mouseleave="userMenuOpen = false"
        >
          <div class="cursor-pointer" @click="userMenuOpen = !userMenuOpen">
            <img
              alt="Avatar"
              class="w-10 h-10 rounded-full border border-outline-variant object-cover"
              :src="portrait"
            >
          </div>
          <!-- Popover Content -->
          <div
            v-show="userMenuOpen"
            class="user-popover"
            style="display: block;"
          >
            <div class="w-64 bg-container rounded-lg shadow-[0_8px_30px_rgb(0,0,0,0.12)] border border-outline-variant p-4">
              <!-- User Info -->
              <div class="flex items-center gap-4 mb-4 pb-4 border-b border-outline-variant">
                <img
                  alt="Large Avatar"
                  class="w-14 h-14 rounded-full border border-outline-variant object-cover"
                  :src="portrait"
                >
                <div class="overflow-hidden">
                  <h3 class="font-headline-sm text-headline-sm break-words text-on-surface">{{ username }}</h3>
                  <p class="font-label-md text-label-md text-on-surface-variant truncate">ID: {{ userId }}</p>
                </div>
              </div>
              <!-- Action List -->
              <div class="flex flex-col gap-1">
                <router-link
                  to="/stitch-userinfo"
                  class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-surface-container-low text-on-surface-variant hover:text-brand-blue transition-colors font-body-md text-body-md no-underline"
                >
                  <span class="material-symbols-outlined text-[20px]">person</span>
                  个人中心
                </router-link>
                <router-link
                  to="/stitch-stat"
                  class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-surface-container-low text-on-surface-variant hover:text-brand-blue transition-colors font-body-md text-body-md no-underline"
                >
                  <span class="material-symbols-outlined text-[20px]">edit_square</span>
                  我的发布
                </router-link>
                <div class="h-[1px] bg-outline-variant my-1"></div>
                <a
                  class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-error-container/20 text-error hover:text-error transition-colors font-body-md text-body-md cursor-pointer"
                  @click="handleLogout"
                >
                  <span class="material-symbols-outlined text-[20px]">logout</span>
                  退出登录
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Login button (not logged in) -->
        <router-link
          v-else
          to="/stitch-login"
          class="flex items-center gap-2 bg-surface-container-low text-on-surface-variant px-5 py-2 rounded-lg font-label-md text-label-md hover:bg-outline-variant transition-all active:scale-95 no-underline"
        >
          登录/注册
        </router-link>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AppHeader',
  data() {
    return {
      scrolled: false,
      searchFocused: false,
      searchQuery: '',
      userMenuOpen: false,
      isLogin: false,
      user: null,
    }
  },
  computed: {
    portrait() {
      if (this.user && this.user.portrait) {
        return `${process.env.VUE_APP_BBS_API}${this.user.portrait}`
      }
      return require('../assets/portrait.png')
    },
    username() {
      return this.user && this.user.nickname ? this.user.nickname : '论坛用户'
    },
    userId() {
      return this.user && this.user.id ? this.user.id : '-'
    },
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
    this.checkLoginState()
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    checkLoginState() {
      if (window.sessionStorage.getItem('tokenStr')) {
        this.isLogin = true
        this.user = JSON.parse(window.sessionStorage.getItem('user') || 'null')
      } else {
        this.isLogin = false
        this.user = null
      }
    },
    handleScroll() {
      this.scrolled = window.scrollY > 10
    },
    handleSearch() {
      if (this.searchQuery.trim()) {
        this.$router.push('/search/keywords/' + encodeURIComponent(this.searchQuery.trim()))
      }
    },
    handleLogout() {
      window.sessionStorage.removeItem('tokenStr')
      window.sessionStorage.removeItem('user')
      this.user = null
      this.isLogin = false
      this.$router.push('/stitch-login')
    },
    isActive(path) {
      return this.$route.path === path
    },
  },
}
</script>
