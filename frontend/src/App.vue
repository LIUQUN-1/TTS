<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <h1>TTS商品监控管理系统</h1>
    </el-header>
    <el-main class="app-main">
      <ProductList />
    </el-main>
  </el-container>
</template>

<script setup>
import ProductList from './components/ProductList.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 16px;
}

.app-container {
  height: 100%;
}

.app-header {
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 30px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  flex-shrink: 0;
}

.app-header h1 {
  font-size: 22px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .app-header {
    padding: 0 15px;
  }
  
  .app-header h1 {
    font-size: 18px;
  }
}

.app-main {
  background: #f5f7fa;
  padding: 20px;
  flex: 1;
  overflow: auto;
}

@media (max-width: 768px) {
  .app-main {
    padding: 10px;
  }
}
</style>
