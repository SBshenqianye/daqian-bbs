<template>
  <div class="min-h-screen bg-surface flex items-center justify-center px-4">
    <div class="w-full max-w-md bg-container border border-border rounded-2xl shadow-lg p-8">
      <div class="flex items-center gap-2 mb-2">
        <span class="material-symbols-outlined text-primary text-[26px]">lock_reset</span>
        <h1 class="font-headline-sm text-headline-sm text-on-surface">修改初始密码</h1>
      </div>
      <p class="text-body-sm text-on-surface-variant mb-6">
        首次登录或密码被管理员重置后，需要先修改密码才能继续使用管理后台。
      </p>

      <form class="space-y-4" @submit.prevent="submit">
        <div>
          <label class="block text-label-md text-secondary mb-1">当前密码</label>
          <input
            v-model="oldPassword"
            type="password"
            autocomplete="current-password"
            class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded-lg text-body-md outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            placeholder="请输入当前密码"
          />
        </div>
        <div>
          <label class="block text-label-md text-secondary mb-1">新密码</label>
          <input
            v-model="newPassword"
            type="password"
            autocomplete="new-password"
            class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded-lg text-body-md outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            placeholder="至少 8 位，含数字、小写和大写字母"
          />
        </div>
        <div>
          <label class="block text-label-md text-secondary mb-1">确认新密码</label>
          <input
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            class="w-full px-3 py-2.5 bg-surface border border-outline-variant rounded-lg text-body-md outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            placeholder="再次输入新密码"
          />
        </div>

        <p v-if="error" class="text-body-sm text-error">{{ error }}</p>

        <button
          type="submit"
          class="w-full py-2.5 bg-primary text-on-primary rounded-lg font-label-lg text-label-lg hover:opacity-90 disabled:opacity-60"
          :disabled="submitting"
        >
          {{ submitting ? '提交中...' : '修改密码并进入后台' }}
        </button>
      </form>

      <div class="mt-5 flex items-center justify-between text-body-sm text-on-surface-variant">
        <span>账号：{{ username }}</span>
        <button class="text-primary hover:underline" @click="logout">退出登录</button>
      </div>
    </div>
  </div>
</template>

<script>
import adminStore from '@/utils/adminStore'

export default {
  name: 'ChangePasswordPage',
  data() {
    return {
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      submitting: false,
      error: ''
    }
  },
  computed: {
    username() {
      return adminStore.getUsername() || '—'
    }
  },
  methods: {
    validate() {
      if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
        return '请填写完整'
      }
      if (this.newPassword.length < 8) {
        return '新密码至少 8 位'
      }
      if (!/[0-9]/.test(this.newPassword) || !/[a-z]/.test(this.newPassword) || !/[A-Z]/.test(this.newPassword)) {
        return '新密码需同时包含数字、小写字母和大写字母'
      }
      if (this.newPassword === this.oldPassword) {
        return '新密码不能与当前密码相同'
      }
      if (this.newPassword !== this.confirmPassword) {
        return '两次输入的新密码不一致'
      }
      return ''
    },
    async submit() {
      this.error = this.validate()
      if (this.error) return

      this.submitting = true
      try {
        const resp = await this.postRequest('/user/modPwd', {
          id: adminStore.getAdminId(),
          password: this.oldPassword,
          newPassword: this.newPassword
        })
        if (resp && resp.code == 200) {
          adminStore.updateAdmin({ isFirstLogin: 0 })
          this.$message.success('密码修改成功')
          this.$router.replace('/dashboard')
        } else {
          this.error = (resp && resp.message) ? resp.message : '修改失败，请稍后重试'
        }
      } catch (e) {
        this.error = '修改失败，请稍后重试'
      } finally {
        this.submitting = false
      }
    },
    logout() {
      adminStore.logout()
      this.$router.replace('/login')
    }
  }
}
</script>
