<template>
  <div class="stitch-wrapper h-full w-full flex flex-col bg-background overflow-hidden">
    <!-- Fixed Header -->
    <Header />

    <!-- Body: Sidebar + Content -->
    <div class="flex flex-1 overflow-hidden relative">
      <!-- Sidebar -->
      <Sidebar />

      <!-- Main Content Area (flex sibling after sidebar, no extra margin needed) -->
      <div
        class="flex-1 flex flex-col overflow-hidden transition-all duration-300 ease-in-out"
      >
        <!-- Tags Bar -->
        <Tags />

        <!-- Page Content -->
        <div class="flex-1 overflow-y-auto bg-background" ref="contentWrapper">
          <transition name="page-fade" mode="out-in">
            <router-view :key="pageKey" class="min-h-full" />
          </transition>

          <!-- Back to top -->
          <transition name="fade">
            <button
              v-if="showBackTop"
              class="fixed bottom-8 right-8 w-10 h-10 bg-container border border-outline-variant rounded-xl shadow-md flex items-center justify-center text-on-surface-variant hover:bg-surface-container-low hover:text-primary transition-all z-20"
              @click="scrollToTop"
            >
              <span class="material-symbols-outlined text-[20px]">arrow_upward</span>
            </button>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'
import Tags from './Tags.vue'
import bus from './bus'

export default {
  name: 'Home',
  components: { Header, Sidebar, Tags },
  data() {
    return {
      collapse: false,
      pageNonce: 0,
      showBackTop: false,
    }
  },
  computed: {
    // key 用 fullPath：查询参数变化会重挂载，页面在 mounted/created 里重读 query；
    // pageNonce 由全局错误处理（bus 'repair-page'）触发 +1，用于内容区白屏后原地重挂载
    pageKey() {
      return this.$route.fullPath + ':' + this.pageNonce
    },
  },
  created() {
    bus.$on('collapse-content', this.onCollapseChange)
    bus.$on('repair-page', this.onRepairPage)
  },
  beforeDestroy() {
    bus.$off('collapse-content', this.onCollapseChange)
    bus.$off('repair-page', this.onRepairPage)
  },
  mounted() {
    // Back to top listener
    const wrapper = this.$refs.contentWrapper
    if (wrapper) {
      wrapper.addEventListener('scroll', () => {
        this.showBackTop = wrapper.scrollTop > 300
      })
    }

    // Handle initial collapse state
    if (document.body.clientWidth < 1500) {
      bus.$emit('collapse', true)
    }
  },
  methods: {
    onCollapseChange(msg) {
      this.collapse = msg
    },
    onRepairPage() {
      this.pageNonce += 1
    },
    scrollToTop() {
      const wrapper = this.$refs.contentWrapper
      if (wrapper) {
        wrapper.scrollTo({ top: 0, behavior: 'smooth' })
      }
    },
  },
}
</script>

<style scoped>
.stitch-wrapper {
  /* Full viewport layout */
}

/* Page transition */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-fade-enter {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}
</style>
