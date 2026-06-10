<template>
  <div class="admin-action-bar">
    <button
      v-for="action in actions"
      :key="action.key"
      class="admin-action-link"
      :class="`is-${action.tone || 'neutral'}`"
      type="button"
      :disabled="action.disabled"
      @click="action.handler"
    >
      {{ action.label }}
    </button>

    <bz-dropdown
      v-if="moreActions.length > 0"
      trigger="click"
    >
      <button
        class="admin-action-link is-more"
        type="button"
        :disabled="moreDisabled"
      >
        更多
      </button>
      <template #dropdown>
        <bz-dropdown-menu>
          <bz-dropdown-item
            v-for="action in moreActions"
            :key="action.key"
            :disabled="action.disabled"
            @click="action.handler"
          >
            <span
              class="admin-action-dropdown-label"
              :class="`is-${action.tone || 'neutral'}`"
            >
              {{ action.label }}
            </span>
          </bz-dropdown-item>
        </bz-dropdown-menu>
      </template>
    </bz-dropdown>
  </div>
</template>

<script setup lang="ts">
import type { AdminActionItem } from "../../types/admin-action";

withDefaults(
  defineProps<{
    actions?: AdminActionItem[];
    moreActions?: AdminActionItem[];
    moreDisabled?: boolean;
  }>(),
  {
    actions: () => [],
    moreActions: () => [],
    moreDisabled: false,
  },
);
</script>

<style scoped>
.admin-action-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-action-link,
.admin-action-dropdown-label {
  padding: 0;
  background: transparent;
  border: none;
  font-size: 13px;
  line-height: 1.4;
}

.admin-action-link {
  cursor: pointer;
}

.admin-action-link.is-detail,
.admin-action-dropdown-label.is-detail {
  color: #1677ff;
}

.admin-action-link.is-edit,
.admin-action-dropdown-label.is-edit {
  color: #1677ff;
}

.admin-action-link.is-enable,
.admin-action-dropdown-label.is-enable {
  color: #16a34a;
}

.admin-action-link.is-disable,
.admin-action-dropdown-label.is-disable {
  color: #dc2626;
}

.admin-action-link.is-delete,
.admin-action-dropdown-label.is-delete {
  color: #dc2626;
}

.admin-action-link.is-pause,
.admin-action-dropdown-label.is-pause {
  color: #f59e0b;
}

.admin-action-link.is-copy,
.admin-action-dropdown-label.is-copy {
  color: #475569;
}

.admin-action-link.is-more,
.admin-action-dropdown-label.is-more,
.admin-action-link.is-neutral,
.admin-action-dropdown-label.is-neutral {
  color: #475569;
}

.admin-action-link:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}
</style>
