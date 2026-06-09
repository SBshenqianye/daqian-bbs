<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">slideshow</span>
            轮播图管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理首页轮播图</p>
        </div>
      </div>

      <!-- Actions -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="openAdd">
            <span class="material-symbols-outlined text-[18px]">add</span>
            添加轮播图
          </button>
        </div>
      </div>

      <!-- Slideshow Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">ID</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">优先级</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">预览</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[170px]">创建时间</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">修改优先级</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in slideshows" :key="item.slideshowId || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4 font-body-md text-on-surface">{{ item.slideshowId }}</td>
                <td class="p-4">
                  <span class="px-2.5 py-0.5 rounded-full text-[12px] font-medium bg-primary/10 text-primary">{{ item.successive }}</span>
                </td>
                <td class="p-4">
                  <img v-if="item.imageUrl" class="w-24 h-14 rounded-lg object-cover border border-outline-variant" :src="item.imageUrl" alt="">
                  <span v-else class="text-on-surface-variant text-body-md">无图片</span>
                </td>
                <td class="p-4 font-body-md text-on-surface-variant">{{ item.createTime }}</td>
                <td class="p-4">
                  <div class="flex items-center gap-2">
                    <input v-model.number="item.successive" type="number" min="1" class="w-20 px-3 py-1.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" @focus="editSlideshowId = item.slideshowId" @change="saveSuccessive(item.slideshowId, item.successive)">
                  </div>
                </td>
              </tr>
              <tr v-if="slideshows.length === 0">
                <td colspan="5" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">view_carousel</span>
                    <p class="text-body-md">暂无轮播图</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Add Dialog -->
      <div v-if="addVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="addVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">add_photo_alternate</span>
              添加轮播图
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="addVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">优先级</label>
              <input v-model.number="addForm.successive" type="number" min="1" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="值越大优先级越高">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">轮播图</label>
              <div class="border-2 border-dashed border-outline-variant rounded-lg p-6 text-center hover:border-primary transition-colors cursor-pointer" @click="triggerUpload">
                <span class="material-symbols-outlined text-[36px] text-outline">add_photo_alternate</span>
                <p class="text-body-md text-on-surface-variant mt-2">点击添加轮播图</p>
              </div>
              <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileSelect">
              <div v-if="previewUrl" class="mt-3">
                <img :src="previewUrl" class="w-full h-32 object-cover rounded-lg border border-outline-variant">
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="addVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="addSlideshow">确定</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StitchSlideshow',
  data() {
    return {
      slideshows: [],
      addVisible: false,
      addForm: { successive: 1, imageUrl: '' },
      editSlideshowId: null,
      selectedFile: null,
      previewUrl: ''
    }
  },
  mounted() { this.getSlideshow() },
  methods: {
    getSlideshow() {
      this.getRequest('/admin', 'getSlideshow').then(resp => {
        if (resp) this.slideshows = resp.obj
      })
    },
    openAdd() {
      this.addForm = { successive: 1, imageUrl: '' }
      this.selectedFile = null
      this.previewUrl = ''
      this.addVisible = true
    },
    triggerUpload() {
      this.$refs.fileInput.click()
    },
    handleFileSelect(e) {
      const file = e.target.files[0]
      if (!file) return
      this.selectedFile = file
      this.previewUrl = URL.createObjectURL(file)
      const formData = new FormData()
      formData.append('image', file)
      this.postRequest('/admin/addSlideshowReturnImageUrl', formData).then(resp => {
        if (resp) this.addForm.imageUrl = resp
      })
    },
    addSlideshow() {
      this.postRequest('/admin/addSlideshow', this.addForm).then(resp => {
        if (resp) {
          this.addVisible = false
          this.getSlideshow()
          this.$message.success('添加成功！')
        }
      })
    },
    saveSuccessive(slideshowId, successive) {
      if (slideshowId) {
        this.putRequest(`/admin/editSuccessive/${slideshowId}/${successive}`).then(() => {})
      }
      this.getSlideshow()
    }
  }
}
</script>
