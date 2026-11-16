<template>
  <div class="space-y-8">
    <div>
      <h3 class="font-headline-sm text-headline-sm text-on-surface">安全设置</h3>
      <p class="font-body-md text-body-md text-on-surface-variant mt-2">定期更换密码可以提高您的账户安全性。</p>
    </div>
    <form class="max-w-md space-y-6" @submit.prevent="handlePasswordChange">
      <div class="space-y-2">
        <label class="font-label-md text-label-md text-on-surface-variant">用户名</label>
        <input
          class="w-full px-4 py-3 rounded-lg border border-outline-variant bg-surface-container-low text-on-surface-variant font-body-md"
          disabled
          type="text"
          :value="userInfo.username || userInfo.phone"
        >
      </div>
      <div class="space-y-2">
        <label class="font-label-md text-label-md text-on-surface-variant">当前密码</label>
        <input
          v-model="form.currentPassword"
          class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
          placeholder="请输入原密码"
          type="password"
        >
      </div>
      <div class="space-y-2">
        <label class="font-label-md text-label-md text-on-surface-variant">新密码</label>
        <input
          v-model="form.newPassword"
          class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
          placeholder="6-20位字符，包含字母及数字"
          type="password"
        >
      </div>
      <div class="space-y-2">
        <label class="font-label-md text-label-md text-on-surface-variant">确认新密码</label>
        <input
          v-model="form.confirmPassword"
          class="w-full px-4 py-3 rounded-lg border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
          placeholder="请再次输入新密码"
          type="password"
        >
      </div>
      <div class="flex gap-4 pt-4">
        <button class="px-8 py-2.5 bg-primary text-white font-label-md text-label-md rounded-lg hover:bg-primary-container transition-colors shadow-md" type="submit" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交修改' }}
        </button>
        <button class="px-8 py-2.5 bg-white text-on-surface-variant border border-outline-variant font-label-md text-label-md rounded-lg hover:bg-surface-container-low transition-colors" type="button" @click="resetForm">
          重置
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import { Message } from 'element-ui'
import { getUser, setUser } from '@/utils/auth'

export default {
  name: 'AccountPassword',
  data() {
    return {
      userInfo: { userId: '', username: '', phone: '' },
      form: { currentPassword: '', newPassword: '', confirmPassword: '' },
      submitting: false,
    }
  },
  mounted() {
    this.loadUserInfo()
  },
  methods: {
    loadUserInfo() {
      const cached = window.sessionStorage.getItem('user')
      if (cached) {
        try { this.apply(JSON.parse(cached)) } catch (e) { /* ignore */ }
      }
      this.getRequest('/common/user/info').then(resp => {
        if (resp) this.apply(resp)
      }).catch(() => {})
    },
    apply(u) {
      this.userInfo.userId = String(u.id || u.userId || '')
      this.userInfo.username = u.username || ''
      this.userInfo.phone = u.phone || ''
    },
    handlePasswordChange() {
      const { currentPassword, newPassword, confirmPassword } = this.form
      if (!currentPassword || !currentPassword.trim()) {
        Message({ type: 'warning', message: '原密码不能为空！', offset: 54 })
        return
      }
      if (!newPassword || !newPassword.trim()) {
        Message({ type: 'warning', message: '新密码不能为空！', offset: 54 })
        return
      }
      if (!confirmPassword || !confirmPassword.trim()) {
        Message({ type: 'warning', message: '重复新密码不能为空！', offset: 54 })
        return
      }
      if (currentPassword.trim() === newPassword.trim()) {
        Message({ type: 'warning', message: '新密码不能与原密码相同！', offset: 54 })
        return
      }
      if (newPassword !== confirmPassword) {
        Message({ type: 'warning', message: '新密码与重复新密码不一致！', offset: 54 })
        return
      }
      this.submitting = true
      this.postRequest('/user/modPwd', {
        id: parseInt(this.userInfo.userId),
        password: currentPassword.trim(),
        newPassword: newPassword.trim(),
      }).then(resp => {
        this.submitting = false
        if (resp) {
          const user = getUser()
          if (user) {
            user.isFirstLogin = 0
            setUser(user, !!window.localStorage.getItem('user'))
          }
          Message({ type: 'success', message: '密码修改成功', offset: 54 })
          this.$router.replace('/forum')
        }
      }).catch(err => { console.warn('[AccountPassword] change', err); this.submitting = false })
    },
    resetForm() {
      this.form = { currentPassword: '', newPassword: '', confirmPassword: '' }
    },
  },
}
</script>
