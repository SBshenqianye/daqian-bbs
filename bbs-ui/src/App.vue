<template>
    <div id="app-root">
        <!-- Use new design header for stitch pages, original header for existing pages -->
        <StitchHeader v-if="isStitchRoute" />
        <BBSHeader v-else />
        <div :class="{ 'pt-16': isStitchRoute }">
            <router-view />
        </div>
    </div>
</template>

<script>
    import './assets/iconfont/iconfont.js'
    import BBSHeader from "@/components/BBSHeader";
    import StitchHeader from "@/components/StitchHeader";
    export default {
        name: 'App',
        components: { BBSHeader, StitchHeader },
        computed: {
            isStitchRoute() {
                return this.$route.path.startsWith('/stitch-')
            },
        },
    }
</script>

<style>
    .icon {
        width: 1em;
        height: 1em;
        vertical-align: -0.15em;
        fill: currentColor;
        overflow: hidden;
    }

    /*设置el-cascader的高度*/
    .el-cascader-panel{
        height: 200px;
    }
</style>
