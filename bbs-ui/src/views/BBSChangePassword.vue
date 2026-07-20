<template>
  <main class="min-h-screen flex items-center justify-center p-4 bg-background">
    <div class="bg-container rounded-lg border border-outline-variant shadow-sm overflow-hidden form-transition max-w-md w-full">
      <div class="px-8 pt-8 pb-10">
        <div class="pb-6 text-center">
          <span class="material-symbols-outlined text-primary text-[48px]">lock_reset</span>
          <h1 class="font-headline-lg text-headline-lg text-on-surface mt-2">修改密码</h1>
          <p class="text-body-md text-on-surface-variant mt-1">首次登录，请设置新密码</p>
        </div>

        <form class="space-y-5" @submit.prevent="handleChangePassword">
          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">当前密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="form.currentPassword"
                class="w-full col-start-1 row-start-1 pl-10 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                placeholder="请输入当前密码"
                :type="showCurrent ? 'text' : 'password'"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">lock</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-3 text-outline hover:text-primary" type="button" @click="showCurrent = !showCurrent">
                <span class="material-symbols-outlined text-[20px]">{{ showCurrent ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">新密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="form.newPassword"
                class="w-full col-start-1 row-start-1 pl-10 pr-10 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                placeholder="请输入新密码"
                :type="showNew ? 'text' : 'password'"
                @input="checkStrength"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">lock_open</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-3 text-outline hover:text-primary" type="button" @click="showNew = !showNew">
                <span class="material-symbols-outlined text-[20px]">{{ showNew ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            <!-- 密码强度指示器 -->
            <div class="mt-2">
              <div class="h-1.5 bg-outline-variant/30 rounded-full overflow-hidden">
                <div class="h-full transition-all duration-300 rounded-full" :class="strengthBarClass" :style="{ width: strengthWidth + '%' }"></div>
              </div>
              <div class="flex items-center justify-between mt-1">
                <p class="text-[12px]" :class="strengthTextClass">{{ strengthLabel }}</p>
                <p class="text-[12px] text-on-surface-variant">
                  <span :class="form.newPassword.length >= 8 ? 'text-green-600' : 'text-outline'">✓ 8位+</span>
                  <span class="mx-1">·</span>
                  <span :class="hasDigit ? 'text-green-600' : 'text-outline'">数字</span>
                  <span class="mx-1">·</span>
                  <span :class="hasLower ? 'text-green-600' : 'text-outline'">小写</span>
                  <span class="mx-1">·</span>
                  <span :class="hasUpper ? 'text-green-600' : 'text-outline'">大写</span>
                </p>
              </div>
            </div>
          </div>

          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">确认新密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="form.confirmPassword"
                class="w-full col-start-1 row-start-1 pl-10 pr-10 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                placeholder="请再次输入新密码"
                :type="showConfirm ? 'text' : 'password'"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">lock_open</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-3 text-outline hover:text-primary" type="button" @click="showConfirm = !showConfirm">
                <span class="material-symbols-outlined text-[20px]">{{ showConfirm ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            <p v-if="form.confirmPassword && form.confirmPassword !== form.newPassword" class="text-error text-[12px] mt-0.5">
              两次输入的密码不一致
            </p>
          </div>

          <button
            class="w-full py-3 bg-primary text-on-primary font-headline-sm text-headline-sm rounded hover:opacity-90 transition-all active:scale-[0.98] shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            type="submit"
            :disabled="submitting || !isFormValid"
          >
            {{ submitting ? '提交中...' : '确认修改' }}
          </button>

          <p class="text-center font-body-md text-body-md text-on-surface-variant pt-2">
            <button class="text-primary font-semibold hover:opacity-80" type="button" @click="handleLogout">返回登录</button>
          </p>
        </form>
      </div>
    </div>
  </main>
</template>

<script>
import { Message } from 'element-ui'
import { getUser, setUser } from '@/utils/auth'

export default {
  name: 'BBSChangePassword',
  data() {
    return {
      form: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      },
      showCurrent: false,
      showNew: false,
      showConfirm: false,
      submitting: false,
      // 密码强度状态
      strengthLevel: 0, // 0=弱 1=中 2=强
      hasDigit: false,
      hasLower: false,
      hasUpper: false,
    }
  },
  computed: {
    isFormValid() {
      return (
        this.form.currentPassword &&
        this.form.newPassword &&
        this.form.confirmPassword &&
        this.form.confirmPassword === this.form.newPassword &&
        this.strengthLevel >= 2 &&
        this.form.newPassword.length >= 8
      )
    },
    strengthWidth() {
      if (!this.form.newPassword) return 0
      return (this.strengthLevel / 2) * 100
    },
    strengthBarClass() {
      if (!this.form.newPassword) return 'bg-outline-variant'
      if (this.strengthLevel === 0) return 'bg-error'
      if (this.strengthLevel === 1) return 'bg-warning'
      return 'bg-success'
    },
    strengthLabel() {
      if (!this.form.newPassword) return '请输入密码'
      if (this.strengthLevel === 0) return '弱 — 需包含数字、小写字母和大写字母'
      if (this.strengthLevel === 1) return '中 — 建议增加大写字母'
      return '强 — 密码强度符合要求'
    },
    strengthTextClass() {
      if (!this.form.newPassword) return 'text-on-surface-variant'
      if (this.strengthLevel === 0) return 'text-error'
      if (this.strengthLevel === 1) return 'text-warning'
      return 'text-success'
    },
  },
  mounted() {
    // 如果用户已改密（isFirstLogin=0），跳转到首页
    const userStr = window.sessionStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        if (user.isFirstLogin === 0) {
          this.$router.replace('/forum')
        }
      } catch (e) {
        // ignore
      }
    }
  },
  methods: {
    checkStrength() {
      const pwd = this.form.newPassword || ''
      this.hasDigit = /[0-9]/.test(pwd)
      this.hasLower = /[a-z]/.test(pwd)
      this.hasUpper = /[A-Z]/.test(pwd)

      if (pwd.length < 8) {
        this.strengthLevel = 0
        return
      }

      const score = (this.hasDigit ? 1 : 0) + (this.hasLower ? 1 : 0) + (this.hasUpper ? 1 : 0)
      if (score >= 3) {
        this.strengthLevel = 2
      } else if (score >= 2) {
        this.strengthLevel = 1
      } else {
        this.strengthLevel = 0
      }
    },
    handleChangePassword() {
      if (!this.isFormValid) return

      // 新旧密码不能相同
      if (this.form.currentPassword === this.form.newPassword) {
        Message({ type: 'error', message: '新密码不能与当前密码相同', offset: 54 })
        return
      }

      this.submitting = true

      // 从 session 获取当前用户 ID
      const userStr = window.sessionStorage.getItem('user')
      let userId = null
      if (userStr) {
        try {
          userId = JSON.parse(userStr).id
        } catch (e) {}
      }
      if (!userId) {
        Message({ type: 'error', message: '登录信息已过期，请重新登录', offset: 54 })
        this.handleLogout()
        return
      }

      this.postRequest('/user/modPwd', {
        id: userId,
        password: this.form.currentPassword,
        newPassword: this.form.newPassword,
      }).then(resp => {
        this.submitting = false
        if (resp) {
          // 更新本地用户信息的 isFirstLogin 状态（token 仍然有效，无需重新登录）
          const user = getUser()
          if (user) {
            user.isFirstLogin = 0
            setUser(user, !!window.localStorage.getItem('user'))
          }
          // 自动登录进入首页
          this.$router.replace('/forum')
        }
      }).catch(err => {
        console.warn('[BBSChangePassword] handleChangePassword', err)
        this.submitting = false
      })
    },
    handleLogout() {
      window.sessionStorage.removeItem('tokenStr')
      window.sessionStorage.removeItem('user')
      this.$bus.$emit('isLogin', false)
      this.$router.replace('/login')
    },
  },
}
</script>
