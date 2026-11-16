<template>
  <header class="stitch-header h-16 bg-container border-b border-outline-variant flex items-center justify-between px-4 sticky top-0 z-30 shadow-sm">
    <!-- Left: collapse button + logo -->
    <div class="flex items-center gap-3">
      <button
        class="w-9 h-9 flex items-center justify-center rounded-lg hover:bg-surface-container-low transition-colors text-on-surface-variant"
        @click="toggleCollapse"
      >
        <span class="material-symbols-outlined text-[22px]">{{ localCollapse ? 'menu' : 'menu_open' }}</span>
      </button>
      <div class="flex items-center gap-2 select-none">
        <span class="material-symbols-outlined text-primary text-[26px]">forum</span>
        <span class="font-headline-sm text-headline-sm text-on-surface hidden sm:inline">大千智荟创新创意交流论坛</span>
      </div>
    </div>

    <!-- Right: reminder bell + user info -->
    <button class="relative ml-auto mr-3 w-9 h-9 flex items-center justify-center rounded-lg hover:bg-surface-container-low text-on-surface-variant" title="提醒中心" @click="$router.push('/reminders')">
      <span class="material-symbols-outlined text-[22px]">notifications</span>
      <span v-if="unread > 0" class="absolute top-0.5 right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-error text-white text-[10px] leading-4 text-center">{{ unread > 99 ? '99+' : unread }}</span>
    </button>
    <div class="relative" ref="userMenu">
      <!-- Trigger -->
      <div
        class="flex items-center gap-3 cursor-pointer px-3 py-1.5 rounded-lg hover:bg-surface-container-low transition-colors select-none"
        @click="showDropdown = !showDropdown"
      >
        <div class="relative">
          <div class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center overflow-hidden border-2 border-surface-container-high ring-2 ring-white">
            <img :src="avatarUrl" @error="onAvatarError" class="w-full h-full object-cover" alt="avatar" />
          </div>
          <!-- 等级角标：与管理端其他位置一致 -->
          <span
            v-if="levelIndex"
            class="absolute -bottom-1 -right-1 w-[15px] h-[15px] rounded-full bg-white shadow flex items-center justify-center"
            :title="levelTip"
          >
            <span class="bbs-level-svg inline-flex items-center" style="height:11px" v-html="levelBadgeSvg"></span>
          </span>
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
import { levelEmblemSvg, levelSpec, normalizeLevel } from '../../../../shared/level-icons'

export default {
  name: 'Header',
  data() {
    return {
      localCollapse: false,
      showDropdown: false,
      adminInfo: null,
      unread: 0,
      levelIndex: null,
      levelName: '',
      // 用户前端链接：路由结构统一挂在 /bbs-user/ 下（dev 由本端口代理到 9081，生产同 nginx 域）。
      // VUE_APP_BBS_USER_API 可选：默认同源相对路径；设置后为绝对源地址前缀。
      userClientHref:
        (process.env.VUE_APP_BBS_USER_API || '') + '/bbs-user/#/userinfo',
      defaultPortrait: require('@/assets/portrait.png'),
    }
  },
  computed: {
    username() {
      return (this.adminInfo && this.adminInfo.username) || '管理员'
    },
    avatarUrl() {
      const portrait = this.adminInfo && this.adminInfo.portrait
      if (!portrait) return this.defaultPortrait
      return portrait.startsWith('/') ? portrait : '/' + portrait
    },
    levelBadgeSvg() {
      return levelEmblemSvg(this.levelIndex || 1, 14)
    },
    levelTip() {
      const s = levelSpec(this.levelIndex || 1)
      const name = this.levelName || s.name
      return `${name} · Lv.${normalizeLevel(this.levelIndex || 1)}`
    },
  },
  created() {
    this.loadAdminInfo()   // 立即显示缓存
    this.refreshUserInfo() // 异步拉取最新用户信息（含头像）覆盖更新
    this.loadUnread()
    const cachedId = this.adminInfo && this.adminInfo.id
    if (cachedId) this.loadLevel(cachedId)
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
    refreshUserInfo() {
      this.getRequestUrl('/common/user/info').then(resp => {
        if (resp && resp.portrait) {
          // /common/user/info 返回扁平 Map（非 ResultBean），resp 即用户信息
          this.adminInfo = resp
          window.sessionStorage.setItem('admin', JSON.stringify(resp))
          if (resp.id) this.loadLevel(resp.id)
        }
      }).catch(err => {
        console.warn('[Header] refreshUserInfo failed, using cached', err)
        this.loadAdminInfo()
      })
    },
    /** 导航栏头像等级角标 */
    loadLevel(userId) {
      if (!userId) return
      this.getRequest(`/common/level/user/${userId}`).then(res => {
        const lv = (res && res.obj) || null
        if (lv) {
          this.levelIndex = lv.levelIndex || lv.level || null
          this.levelName = lv.levelName || ''
        }
      }).catch(() => {})
    },
    loadUnread() {
      const id = (this.adminInfo && this.adminInfo.id) || (this.loadAdminInfo(), this.adminInfo && this.adminInfo.id)
      if (!id) return
      this.getRequestUrl(`/notification/unreadCount?userId=${id}`).then(res => {
        const count = res && res.obj != null ? res.obj : 0
        this.unread = Number(count) || 0
      }).catch(() => {})
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
