<template>
  <main class="login-shell min-h-[calc(100vh-64px)] flex items-center justify-center p-4 bg-surface">
    <div class="bg-container rounded-2xl border border-border shadow-[0_12px_40px_-18px_rgba(15,23,42,0.25)] overflow-hidden max-w-[400px] w-full">
      <div class="px-7 pt-7 pb-8">
        <div class="pb-5 text-center">
          <span class="w-11 h-11 rounded-2xl bg-primary/10 text-primary grid place-items-center mx-auto mb-3">
            <span class="material-symbols-outlined text-[24px]">lock_reset</span>
          </span>
          <h1 class="text-[20px] font-semibold text-on-surface">修改密码</h1>
          <p class="text-[11.5px] text-outline mt-1">首次登录，请设置新密码</p>
        </div>

        <form class="space-y-4" @submit.prevent="handleChangePassword">
          <div>
            <label class="ui-form-label">当前密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="form.currentPassword"
                class="ui-input col-start-1 row-start-1 pl-9 pr-10"
                placeholder="请输入当前密码"
                :type="showCurrent ? 'text' : 'password'"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-2.5 text-outline text-[18px] pointer-events-none">lock</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-2 text-outline hover:text-primary transition-colors" type="button" @click="showCurrent = !showCurrent">
                <span class="material-symbols-outlined text-[18px]">{{ showCurrent ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <div>
            <label class="ui-form-label">新密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="form.newPassword"
                class="ui-input col-start-1 row-start-1 pl-9 pr-10"
                placeholder="请输入新密码"
                :type="showNew ? 'text' : 'password'"
                @input="checkStrength"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-2.5 text-outline text-[18px] pointer-events-none">lock_open</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-2 text-outline hover:text-primary transition-colors" type="button" @click="showNew = !showNew">
                <span class="material-symbols-outlined text-[18px]">{{ showNew ? 'visibility_off' : 'visibility' }}</span>
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

          <div>
            <label class="ui-form-label">确认新密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="form.confirmPassword"
                class="ui-input col-start-1 row-start-1 pl-9 pr-10"
                placeholder="请再次输入新密码"
                :type="showConfirm ? 'text' : 'password'"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-2.5 text-outline text-[18px] pointer-events-none">lock_open</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-2 text-outline hover:text-primary transition-colors" type="button" @click="showConfirm = !showConfirm">
                <span class="material-symbols-outlined text-[18px]">{{ showConfirm ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            <p v-if="form.confirmPassword && form.confirmPassword !== form.newPassword" class="text-error text-[11px] mt-1">
              两次输入的密码不一致
            </p>
          </div>

          <button
            class="ui-btn ui-btn-sm ui-btn-primary w-full"
            type="submit"
            :disabled="submitting || !isFormValid"
          >
            {{ submitting ? '提交中...' : '确认修改' }}
          </button>

          <p class="text-center text-[11px] text-outline pt-1">
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

<style scoped>
.login-shell {
  background-image:
    radial-gradient(720px 360px at 50% -10%, rgba(0, 82, 217, 0.07), transparent 70%),
    radial-gradient(520px 300px at 90% 100%, rgba(0, 82, 217, 0.04), transparent 70%);
}
</style>
