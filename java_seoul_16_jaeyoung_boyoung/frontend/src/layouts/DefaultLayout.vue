<script setup>
import SideNav from '../components/SideNav.vue'
</script>

<template>
  <div class="shell">
    <SideNav />
    <main class="main">
      <div class="content">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
  background: #f7f8fa;
}
.main {
  flex: 1;
  min-width: 0;
}
.content {
  max-width: 1160px;
  margin: 0 auto;
  padding: 28px 32px 48px;
  box-sizing: border-box;
}
</style>
