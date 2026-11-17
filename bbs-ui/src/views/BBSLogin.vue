<template>
  <main class="login-shell min-h-[calc(100vh-64px)] flex items-center justify-center p-4 bg-surface">
    <div class="w-full max-w-[380px] bg-container rounded-2xl border border-border shadow-[0_12px_40px_-18px_rgba(15,23,42,0.25)] overflow-hidden">
      <div class="px-7 pt-7 pb-8">
        <div class="flex flex-col items-center pb-6">
          <span class="w-11 h-11 rounded-2xl bg-primary/10 text-primary grid place-items-center mb-3">
            <span class="material-symbols-outlined text-[24px]">forum</span>
          </span>
          <h1 class="text-[20px] font-semibold text-on-surface">欢迎回来</h1>
          <p class="text-[11.5px] text-outline mt-1">登录大千智荟交流论坛</p>
        </div>
        <form class="space-y-4" @submit.prevent="handleLogin">
          <div>
            <label class="ui-form-label">账号</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="loginForm.username"
                class="ui-input col-start-1 row-start-1 pl-9"
                placeholder="请输入账号"
                type="text"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-2.5 text-outline text-[18px] pointer-events-none">person</span>
            </div>
          </div>
          <div>
            <label class="ui-form-label">密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="loginForm.password"
                class="ui-input col-start-1 row-start-1 pl-9 pr-10"
                placeholder="请输入密码"
                :type="showLoginPassword ? 'text' : 'password'"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-2.5 text-outline text-[18px] pointer-events-none">lock</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-2 text-outline hover:text-primary transition-colors" type="button" @click="showLoginPassword = !showLoginPassword">
                <span class="material-symbols-outlined text-[18px]">{{ showLoginPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>
          <label class="inline-flex items-center gap-2 cursor-pointer select-none">
            <input v-model="loginForm.remember" type="checkbox">
            <span class="text-[11.5px] text-on-surface-variant">记住我</span>
          </label>
          <button class="ui-btn ui-btn-sm ui-btn-primary w-full" type="submit">
            登录
          </button>
          <p class="text-center text-[11px] leading-relaxed text-outline pt-1">
            账号由管理员统一分配，如有问题请联系管理员
          </p>
        </form>
      </div>
    </div>
  </main>
</template>

<script>
import { Message } from 'element-ui'
import { setToken, setUser, getRememberedAccount, setRememberedAccount, removeRememberedAccount } from '@/utils/auth'

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
  mounted() {
    // 读取记住我的凭证，自动填充账密
    var account = getRememberedAccount()
    if (account) {
      this.loginForm.username = account.username
      this.loginForm.password = account.password
      this.loginForm.remember = true
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
          var remember = this.loginForm.remember
          setToken(tokenStr, remember)
          this.$bus.$emit('isLogin', true)

          // 使用登录响应中的 user 数据（后端已显式写入 isFirstLogin）
          var user = resp.obj.user
          if (user) {
            setUser(user, remember)
            if (user.isFirstLogin == 1) {
              this.$router.replace('/change-password')
              setTimeout(function() { location.reload() }, 600)
              return
            }
          }

          // 记住我 → 保存凭证用于登录页自动回填；不记住 → 清除旧凭证
          if (remember) {
            setRememberedAccount(this.loginForm.username, this.loginForm.password)
          } else {
            removeRememberedAccount()
          }

          var redirect = this.$route.query.redirect || '/forum'
          this.$router.replace(redirect)
          setTimeout(function() { location.reload() }, 600)
        }
      }).catch(err => {
        console.warn('[BBSLogin] handleLogin', err)
        this.loginLoading = false
      })
    },
  },
}
</script>

<style scoped>
/* 极淡的品牌色晕染，保持简洁的同时让登录页不至于像空白表单 */
.login-shell {
  background-image:
    radial-gradient(720px 360px at 50% -10%, rgba(0, 82, 217, 0.07), transparent 70%),
    radial-gradient(520px 300px at 90% 100%, rgba(0, 82, 217, 0.04), transparent 70%);
}
</style>
