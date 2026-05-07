<script setup lang="ts">
import AiAssistant from './components/AiAssistant.vue'
</script>

<template>
  <n-config-provider abstract>
    <n-message-provider>
      <router-view />
      <AiAssistant />
    </n-message-provider>
  </n-config-provider>
</template>
