<template>
    <div id="app">
        <router-view></router-view>
        <ImportOverlay />
    </div>
</template>

<script>
import ImportOverlay from '@/components/ImportOverlay.vue'
export default {
  name: 'App',
  components: { ImportOverlay }
}
</script>

<style>
    @import "./assets/css/main.css";
    @import "./assets/css/color-dark.css";     /*深色主题*/
    /*@import "./assets/css/theme-green/color-green.css";   浅绿色主题*/
</style>