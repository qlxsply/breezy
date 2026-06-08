<template>
  <button
    class="bz-icon-action-button"
    :class="[`bz-icon-action-button--${tone}`]"
    :style="buttonStyle"
    :type="nativeType"
    :disabled="disabled"
    :title="title"
    :aria-label="ariaLabel || title"
    @click="onClick"
  >
    <svg
      v-if="icon === 'plus'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 5v14M5 12h14"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'minus'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M5 12h14"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'search'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle
        cx="11"
        cy="11"
        r="6.5"
        stroke="currentColor"
        stroke-width="1.8"
      />
      <path
        d="M16 16l5 5"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'refresh'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M20 11a8 8 0 0 0-13.66-5.66L4 8"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M4 4v4h4"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M4 13a8 8 0 0 0 13.66 5.66L20 16"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M20 20v-4h-4"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'pause'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <rect
        x="6.5"
        y="5.5"
        width="4"
        height="13"
        rx="1"
        stroke="currentColor"
        stroke-width="1.8"
      />
      <rect
        x="13.5"
        y="5.5"
        width="4"
        height="13"
        rx="1"
        stroke="currentColor"
        stroke-width="1.8"
      />
    </svg>
    <svg
      v-else-if="icon === 'resume'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M8 6.5v11l9-5.5-9-5.5z"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linejoin="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'check'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M5.5 12.5l4 4L18.5 7.5"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'check-note'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M4 5.5h16v13H4z"
        stroke="currentColor"
        stroke-width="1.7"
      />
      <path
        d="M7 9.5h6M7 12.5h4"
        stroke="currentColor"
        stroke-width="1.6"
        stroke-linecap="round"
      />
      <path
        d="M13.8 15.2l1.7 1.8 3.2-3.6"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'delete'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M4 7H20"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
      <path
        d="M9 5.5C9 4.67 9.67 4 10.5 4H13.5C14.33 4 15 4.67 15 5.5V7H9V5.5Z"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linejoin="round"
      />
      <rect
        x="6"
        y="7"
        width="12"
        height="13"
        rx="2"
        stroke="currentColor"
        stroke-width="1.8"
      />
      <path
        d="M10 11V17"
        stroke="currentColor"
        stroke-width="1.6"
        stroke-linecap="round"
      />
      <path
        d="M14 11V17"
        stroke="currentColor"
        stroke-width="1.6"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'save'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M5 4h11l3 3v13H5z"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linejoin="round"
      />
      <path
        d="M8 4v6h8V4"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linejoin="round"
      />
      <path
        d="M9 18h6"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'close'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M6 6l12 12M18 6L6 18"
        stroke="currentColor"
        stroke-width="1.9"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'upload'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 16V6"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
      <path
        d="M8.5 9.5L12 6l3.5 3.5"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M5 18.5h14"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'download'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 5v10"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
      <path
        d="M8.5 12.5 12 16l3.5-3.5"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M5 19h14"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'copy'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <rect
        x="8"
        y="8"
        width="11"
        height="12"
        rx="2"
        stroke="currentColor"
        stroke-width="1.7"
      />
      <path
        d="M6 15H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'compare'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <rect
        x="3"
        y="4"
        width="18"
        height="16"
        rx="2"
        stroke="currentColor"
        stroke-width="1.6"
      />
      <path
        d="M12 4V20"
        stroke="currentColor"
        stroke-width="1.6"
      />
      <path
        d="M6 8H9"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-linecap="round"
      />
      <path
        d="M6 11H10"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-linecap="round"
      />
      <path
        d="M6 14H8.5"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-linecap="round"
      />
      <path
        d="M14.5 8H18"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-linecap="round"
      />
      <path
        d="M14.5 11H17"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-linecap="round"
      />
      <path
        d="M14.5 14H18.5"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-linecap="round"
      />
    </svg>
    <svg
      v-else-if="icon === 'list'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M8 7h11M8 12h11M8 17h11"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
      />
      <circle
        cx="4.5"
        cy="7"
        r="1"
        fill="currentColor"
      />
      <circle
        cx="4.5"
        cy="12"
        r="1"
        fill="currentColor"
      />
      <circle
        cx="4.5"
        cy="17"
        r="1"
        fill="currentColor"
      />
    </svg>
    <svg
      v-else-if="icon === 'more'"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle
        cx="6"
        cy="12"
        r="1.7"
        fill="currentColor"
      />
      <circle
        cx="12"
        cy="12"
        r="1.7"
        fill="currentColor"
      />
      <circle
        cx="18"
        cy="12"
        r="1.7"
        fill="currentColor"
      />
    </svg>
    <svg
      v-else
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M3 17.25V21h3.75L18.2 9.56l-3.75-3.75L3 17.25z"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linejoin="round"
      />
      <path
        d="M13.9 6.06l3.75 3.75"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
      />
    </svg>
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzIconActionButton",
});

const props = withDefaults(
  defineProps<{
    icon:
      | "plus"
      | "minus"
      | "edit"
      | "search"
      | "refresh"
      | "pause"
      | "resume"
      | "check"
      | "check-note"
      | "delete"
      | "save"
      | "close"
      | "upload"
      | "download"
      | "copy"
      | "compare"
      | "list"
      | "more";
    tone?: "default" | "primary" | "danger";
    disabled?: boolean;
    title?: string;
    ariaLabel?: string;
    nativeType?: "button" | "submit" | "reset";
    size?: number;
  }>(),
  {
    tone: "default",
    disabled: false,
    title: "",
    ariaLabel: "",
    nativeType: "button",
    size: 28,
  },
);

const emit = defineEmits<{
  (e: "click", event: MouseEvent): void;
}>();

const buttonStyle = computed(() => ({
  "--bz-icon-action-size": `${props.size}px`,
}));

function onClick(event: MouseEvent) {
  if (props.disabled) {
    event.preventDefault();
    return;
  }
  emit("click", event);
}
</script>

<style scoped>
.bz-icon-action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--bz-icon-action-size);
  height: var(--bz-icon-action-size);
  padding: 0;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: transparent;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.bz-icon-action-button svg {
  width: calc(var(--bz-icon-action-size) * 0.58);
  height: calc(var(--bz-icon-action-size) * 0.58);
}

.bz-icon-action-button:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.bz-icon-action-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.bz-icon-action-button--primary {
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.35);
}

.bz-icon-action-button--primary:hover:not(:disabled) {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.1);
}

.bz-icon-action-button--danger {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.35);
}

.bz-icon-action-button--danger:hover:not(:disabled) {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.1);
}
</style>
