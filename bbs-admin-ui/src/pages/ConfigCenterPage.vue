<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">settings</span>
            配置管理
          </h1>
          <p class="text-body-md text-secondary mt-1">数据字典与系统扩展配置</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="数据字典" name="dict">
          <DictPage />
        </el-tab-pane>
        <el-tab-pane label="系统配置" name="system">
          <SystemConfigPage />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import DictPage from './DictPage.vue'
import SystemConfigPage from './SystemConfigPage.vue'
export default {
  name: 'ConfigCenterPage',
  components: { DictPage, SystemConfigPage },
  data() { return { activeTab: 'dict' } }
}
</script>
