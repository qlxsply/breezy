<template>
  <teleport to="body">
    <div
      v-if="open"
      class="admin-entity-drawer"
    >
      <button
        class="admin-entity-drawer__mask"
        type="button"
        aria-label="关闭抽屉"
        @click="$emit('close')"
      ></button>

      <section
        class="admin-entity-drawer__panel"
        :style="panelStyle"
      >
        <header class="admin-entity-drawer__header">
          <div class="admin-entity-drawer__title-wrap">
            <div class="admin-entity-drawer__title">{{ title }}</div>
            <div
              v-if="$slots.extra"
              class="admin-entity-drawer__extra"
            >
              <slot name="extra" />
            </div>
          </div>

          <button
            class="admin-entity-drawer__close"
            type="button"
            aria-label="关闭抽屉"
            @click="$emit('close')"
          >
            <span aria-hidden="true">x</span>
          </button>
        </header>

        <div
          v-loading="loading"
          class="admin-entity-drawer__body"
        >
          <slot />
        </div>

        <footer
          v-if="$slots.footer"
          class="admin-entity-drawer__footer"
        >
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    width?: string;
    loading?: boolean;
  }>(),
  {
    width: "960px",
    loading: false,
  },
);

defineEmits<{
  (e: "close"): void;
}>();

const panelStyle = computed(() => ({ width: props.width }));
</script>

<style scoped>
.admin-entity-drawer {
  position: fixed;
  inset: 0;
  z-index: 4000;
  display: flex;
  justify-content: flex-end;
}

.admin-entity-drawer__mask {
  flex: 1;
  border: none;
  background: rgba(15, 23, 42, 0.38);
  cursor: pointer;
}

.admin-entity-drawer__panel {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: min(100vw, 1120px);
  background: #ffffff;
  box-shadow: -16px 0 40px rgba(15, 23, 42, 0.18);
}

.admin-entity-drawer__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.admin-entity-drawer__title-wrap {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.admin-entity-drawer__title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.admin-entity-drawer__extra {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-entity-drawer__close {
  width: 34px;
  height: 34px;
  border: 1px solid #dbe1ea;
  border-radius: 10px;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
}

.admin-entity-drawer__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
}

.admin-entity-drawer__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid #e5e7eb;
  background: #ffffff;
}

@media (max-width: 960px) {
  .admin-entity-drawer__panel {
    width: 100% !important;
    max-width: 100vw;
  }

  .admin-entity-drawer__body,
  .admin-entity-drawer__header,
  .admin-entity-drawer__footer {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
