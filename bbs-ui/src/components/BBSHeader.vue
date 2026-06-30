<template>
  <header
    class="fixed top-0 left-0 right-0 h-16 bg-container z-50 transition-shadow duration-300"
    :class="{ 'shadow-md border-b-0': scrolled, 'border-b border-outline-variant': !scrolled }"
  >
    <div class="max-w-7xl mx-auto h-full px-page-margin-desktop flex items-center justify-between gap-8">
      <!-- Left: Brand & Main Nav -->
      <div class="flex items-center gap-8 flex-shrink-0">
        <router-link
          to="/forum"
          class="text-headline-lg font-semibold text-brand-blue tracking-tight no-underline"
        >
          大千智荟交流论坛
        </router-link>
        <nav class="hidden md:flex items-center gap-6">
          <router-link
            to="/forum"
            class="font-label-md text-label-md pb-1 border-b-2 transition-colors"
            :class="isActive('/forum') ? 'text-brand-blue border-brand-blue' : 'text-on-surface-variant border-transparent hover:text-brand-blue'"
          >
            论坛
          </router-link>
          <router-link
            to="/points"
            class="font-label-md text-label-md pb-1 border-b-2 transition-colors"
            :class="isActive('/points') ? 'text-brand-blue border-brand-blue' : 'text-on-surface-variant border-transparent hover:text-brand-blue'"
          >
            排行榜
          </router-link>
        </nav>
      </div>

      <!-- Middle: Search Bar (only show on forum page) -->
      <div v-if="isForumPage" class="flex-grow max-w-2xl hidden md:block">
        <div class="grid grid-cols-1 grid-rows-1 group">
          <span
            class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline transition-colors pointer-events-none"
            :class="{ 'text-brand-blue': searchFocused }"
          >search</span>
          <input
            v-model="keywords"
            class="w-full col-start-1 row-start-1 h-10 pl-10 pr-4 bg-surface-container-low rounded-lg border border-transparent focus:border-brand-blue focus:bg-container focus:ring-0 transition-all font-body-md text-body-md outline-none"
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
          to="/write"
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
                  <p class="font-label-md text-label-md text-on-surface-variant truncate">人员编号: {{ personnelId }}</p>
                </div>
              </div>
              <!-- Action List -->
              <div class="flex flex-col gap-1">
                <router-link
                  to="/userinfo"
                  class="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-surface-container-low text-on-surface-variant hover:text-brand-blue transition-colors font-body-md text-body-md no-underline"
                >
                  <span class="material-symbols-outlined text-[20px]">person</span>
                  个人中心
                </router-link>
                <router-link
                  to="/stat"
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
          to="/login"
          class="flex items-center gap-2 bg-surface-container-low text-on-surface-variant px-5 py-2 rounded-lg font-label-md text-label-md hover:bg-outline-variant transition-all active:scale-95 no-underline"
        >
          登录
        </router-link>
      </div>
    </div>
  </header>
</template>

<script>
import { getToken, getUser, removeToken, removeUser } from '@/utils/auth'

export default {
  name: 'BBSHeader',
  data() {
    return {
      scrolled: false,
      searchFocused: false,
      keywords: '',
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
    personnelId() {
      return this.user && this.user.personnelId ? this.user.personnelId : '-'
    },
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
    this.checkLoginState()
    // Restore last search keywords
    const lastKeywords = window.localStorage.getItem('bbs_search_keywords')
    if (lastKeywords) {
      this.keywords = lastKeywords
    }
    // Listen for login state changes
    this.$bus && this.$bus.$on('isLogin', (data) => {
      this.isLogin = data
      this.checkLoginState()
    })
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
    this.$bus && this.$bus.$off('isLogin')
  },
  methods: {
    checkLoginState() {
      var token = getToken()
      if (token) {
        this.isLogin = true
        this.user = getUser()
      } else {
        this.isLogin = false
        this.user = null
      }
    },
    handleScroll() {
      this.scrolled = window.scrollY > 10
    },
    handleSearch() {
      const keywords = (this.keywords || '').trim()
      window.localStorage.setItem('bbs_search_keywords', keywords)

      if (!getToken()) {
        this.$router.push('/login')
        return
      }

      this.$bus && this.$bus.$emit('forumSearch', keywords)
    },
    handleLogout() {
      removeToken()
      removeUser()
      this.user = null
      this.isLogin = false
      this.$router.push('/login')
    },
    isActive(path) {
      return this.$route.path === path
    },
    isForumPage() {
      return this.$route.path === '/forum'
    },
  },
}
</script>
