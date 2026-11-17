<template>
  <div class="space-y-5">
    <div>
      <h3 class="ui-section-title">
        <span class="material-symbols-outlined text-[16px] text-primary">shield</span>安全设置
      </h3>
      <p class="text-[12px] text-on-surface-variant mt-1.5">定期更换密码可以提高您的账户安全性。</p>
    </div>
    <form class="max-w-md space-y-4" @submit.prevent="handlePasswordChange">
      <div>
        <label class="ui-form-label">用户名</label>
        <input
          class="ui-input bg-surface-container-low text-on-surface-variant"
          disabled
          type="text"
          :value="userInfo.username || userInfo.phone"
        >
      </div>
      <div>
        <label class="ui-form-label">当前密码</label>
        <input
          v-model="form.currentPassword"
          class="ui-input"
          placeholder="请输入原密码"
          type="password"
        >
      </div>
      <div>
        <label class="ui-form-label">新密码</label>
        <input
          v-model="form.newPassword"
          class="ui-input"
          placeholder="6-20 位字符，包含字母及数字"
          type="password"
        >
      </div>
      <div>
        <label class="ui-form-label">确认新密码</label>
        <input
          v-model="form.confirmPassword"
          class="ui-input"
          placeholder="请再次输入新密码"
          type="password"
        >
      </div>
      <div class="flex gap-2 pt-2">
        <button class="ui-btn ui-btn-sm ui-btn-primary" type="submit" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交修改' }}
        </button>
        <button class="ui-btn ui-btn-sm ui-btn-secondary" type="button" @click="resetForm">
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
