<template>
  <main class="min-h-screen flex items-center justify-center p-4 bg-background">
    <div class="bg-container login-box rounded-lg border border-outline-variant shadow-sm overflow-hidden form-transition">
      <!-- Login Section -->
      <div class="px-8 pt-8 pb-10">
        <div class="pb-6 text-center">
          <h1 class="font-headline-lg text-headline-lg text-on-surface">登录</h1>
        </div>
        <form class="space-y-5" @submit.prevent="handleLogin">
          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">账号</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="loginForm.username"
                class="w-full col-start-1 row-start-1 pl-10 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                placeholder="请输入账号"
                type="text"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">person</span>
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="loginForm.password"
                class="w-full col-start-1 row-start-1 pl-10 pr-10 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                :class="{ 'password-masked': !showLoginPassword }"
                placeholder="请输入密码"
                type="text"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">lock</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-3 text-outline hover:text-primary" type="button" @click="showLoginPassword = !showLoginPassword">
                <span class="material-symbols-outlined text-[20px]">{{ showLoginPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>
          <div class="flex items-center justify-between py-1">
            <label class="flex items-center gap-2 cursor-pointer">
              <input v-model="loginForm.remember" class="w-4 h-4 text-primary border-outline-variant rounded focus:ring-primary" type="checkbox">
              <span class="font-label-md text-label-md text-on-surface-variant">记住我</span>
            </label>
          </div>
          <button class="w-full py-3 bg-primary text-on-primary font-headline-sm text-headline-sm rounded hover:opacity-90 transition-all active:scale-[0.98] shadow-sm" type="submit">
            登录
          </button>
          <p class="text-center font-body-md text-body-md text-on-surface-variant pt-2">
            账号由管理员统一分配，如有问题请联系管理员
          </p>
        </form>
      </div>
    </div>
  </main>
</template>

<script>
import { Message } from 'element-ui'

export default {
  name: 'BBSLogin',
  data() {
    return {
      showLoginPassword: false,
      loginLoading: false,
      loginForm: {
        username: '',
        password: '',
        remember: false,
      },
    }
  },
  methods: {
    handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        Message({ type: 'error', message: '账号或密码不能为空！', offset: 54 })
        return
      }
      this.loginLoading = true
      this.postRequest('/common/login', { ...this.loginForm, channel: '01' }).then(resp => {
        this.loginLoading = false
        if (resp && resp.obj) {
          const tokenStr = resp.obj.tokenHead + resp.obj.token
          window.sessionStorage.setItem('tokenStr', tokenStr)
          this.$bus.$emit('isLogin', true)

          // 使用登录响应中的 user 数据（后端已显式写入 isFirstLogin）
          var user = resp.obj.user
          if (user) {
            window.sessionStorage.setItem('user', JSON.stringify(user))
            if (user.isFirstLogin == 1) {
              this.$router.replace('/change-password')
              setTimeout(function() { location.reload() }, 600)
              return
            }
          }

          var redirect = this.$route.query.redirect || '/forum'
          this.$router.replace(redirect)
          setTimeout(function() { location.reload() }, 600)
        }
      }).catch(() => {
        this.loginLoading = false
      })
    },
  },
}
</script>

<style scoped>
.password-masked {
  -webkit-text-security: disc;
}
</style>
