<template>
    <div id="app-root">
        <StitchHeader />
        <div class="pt-16">
            <router-view />
        </div>
    </div>
</template>

<script>
    import './assets/iconfont/iconfont.js'
    import StitchHeader from "@/components/StitchHeader";
    export default {
        name: 'App',
        components: { StitchHeader },
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
