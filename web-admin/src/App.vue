<template>
  <RouterView v-slot="{ Component }">
    <component
      :is="Component"
      v-if="layoutMode === 'blank'"
    />
    <AdminLayout v-else>
      <component :is="Component" />
    </AdminLayout>
  </RouterView>
  <BzMessageHost />
  <BzConfirmHost />
</template>

<script setup lang="ts">
import AdminLayout from "@admin/layout/AdminLayout.vue";
import { BzConfirmHost, BzMessageHost } from "@shared/components/bz";
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const layoutMode = computed(() => (route.meta.layout === "blank" ? "blank" : "admin"));
</script>
