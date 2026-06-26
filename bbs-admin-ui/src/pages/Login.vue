<template>
  <main class="min-h-screen flex items-center justify-center p-4 bg-background">
    <div class="bg-container rounded-lg border border-outline-variant shadow-sm overflow-hidden form-transition" style="width: 420px;">
      <!-- Header -->
      <div class="bg-primary px-8 py-6 text-center">
        <h1 class="font-headline-lg text-headline-lg text-on-primary">管理员登录</h1>
        <p class="text-body-md text-on-primary/80 mt-1">大千智荟创新创意交流论坛</p>
      </div>

      <!-- Login Form -->
      <div class="px-8 pt-8 pb-10">
        <form class="space-y-5" @submit.prevent="submitForm">
          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">用户名</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="param.username"
                class="w-full col-start-1 row-start-1 pl-10 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                placeholder="请输入用户名"
                type="text"
                @keyup.enter="submitForm"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">person</span>
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="font-label-md text-label-md text-secondary ml-0.5">密码</label>
            <div class="grid grid-cols-1 grid-rows-1">
              <input
                v-model="param.password"
                class="w-full col-start-1 row-start-1 pl-10 pr-10 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                :class="{ 'password-masked': !showPassword }"
                placeholder="请输入密码"
                type="text"
                @keyup.enter="submitForm"
              >
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[20px] pointer-events-none">lock</span>
              <button class="col-start-1 row-start-1 self-center justify-self-end mr-3 text-outline hover:text-primary" type="button" @click="showPassword = !showPassword">
                <span class="material-symbols-outlined text-[20px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <div v-if="errorMsg" class="bg-error-container border border-error/20 rounded-lg px-4 py-3">
            <p class="text-body-md text-error flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px]">error</span>
              {{ errorMsg }}
            </p>
          </div>

          <button
            class="w-full py-3 bg-primary text-on-primary font-headline-sm text-headline-sm rounded hover:opacity-90 transition-all active:scale-[0.98] shadow-sm disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            type="submit"
            :disabled="loading"
          >
            <span v-if="loading" class="inline-block w-4 h-4 border-2 border-on-primary/30 border-t-on-primary rounded-full animate-spin"></span>
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>
      </div>
    </div>
  </main>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      param: {
        username: '',
        password: ''
      },
      showPassword: false,
      loading: false,
      errorMsg: ''
    }
  },
  methods: {
    submitForm() {
      this.errorMsg = ''
      if (!this.param.username || !this.param.username.trim()) {
        this.errorMsg = '请输入用户名'
        return
      }
      if (!this.param.password) {
        this.errorMsg = '请输入密码'
        return
      }
      this.loading = true
      this.postRequest('/common/login', { ...this.param, channel: '02' }).then(resp => {
        this.loading = false
        if (resp && resp.obj) {
          const tokenStr = resp.obj.tokenHead + resp.obj.token
          window.sessionStorage.setItem('tokenStr', tokenStr)
          window.sessionStorage.setItem('admin', JSON.stringify(resp.obj.user))
          this.$message.success('登录成功！')
          const redirect = this.$route.query.redirect || '/'
          this.$router.push(redirect)
        } else {
          this.errorMsg = (resp && resp.message) ? resp.message : '登录失败，请检查用户名和密码'
        }
      }).catch(err => {
        this.loading = false
        this.errorMsg = (err.response && err.response.data && err.response.data.message)
          ? err.response.data.message
          : '登录失败，请稍后重试'
      })
    }
  }
}
</script>

<style scoped>
.password-masked {
  -webkit-text-security: disc;
}
</style>
