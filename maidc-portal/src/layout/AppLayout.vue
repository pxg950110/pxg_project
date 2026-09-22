<template>
  <div class="flex h-screen w-screen overflow-hidden bg-slate-50 font-sans text-slate-800 antialiased">
    <!-- Modern Clinical Dark Navy Sidebar -->
    <Sidebar />

    <!-- Main Application Column -->
    <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
      <!-- Clinical Header -->
      <Header />

      <!-- Capsule Multi-tab Tags View -->
      <TagsView />

      <!-- Content Viewport -->
      <main class="flex-1 overflow-y-auto overflow-x-hidden p-4 md:p-6 bg-slate-50 min-h-[calc(100vh-98px)]">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import TagsView from './components/TagsView.vue'
</script>

<style lang="scss">
/* Route Transition Animations */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.22s cubic-bezier(0.4, 0, 0.2, 1),
              transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
