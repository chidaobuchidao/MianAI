<template>
  <view class="mian-icon" :style="rootStyle">
    <image class="mian-icon__svg" :src="src" mode="aspectFit" />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface IconDef {
  viewBox?: string;
  body: string;
}

const props = withDefaults(defineProps<{
  name: string;
  size?: number | string;
  color?: string;
  strokeWidth?: number | string;
}>(), {
  size: 24,
  color: '#4A4A4A',
  strokeWidth: 1.8,
});

const icons: Record<string, IconDef> = {
  interview: { body: '<path d="M21 15a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M8 9h8M8 13h5"/>' },
  robot: { body: '<rect x="6" y="8" width="12" height="9" rx="3"/><path d="M12 8V5"/><path d="M9 5h6"/><path d="M5 11v4M19 11v4"/><circle cx="10" cy="12" r="1" fill="__COLOR__" stroke="none"/><circle cx="14" cy="12" r="1" fill="__COLOR__" stroke="none"/><path d="M10 16h4"/>' },
  doc: { body: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8M8 17h6"/>' },
  file: { body: '<path d="M14 2H7a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7z"/><path d="M14 2v5h5"/><path d="M9 13h6M9 17h4"/>' },
  clipboard: { body: '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4a3 3 0 0 1 6 0"/><path d="M9 8h6M8 13h8M8 17h5"/>' },
  target: { body: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1.5" fill="__COLOR__" stroke="none"/>' },
  chart: { body: '<path d="M4 19h16"/><path d="M7 16V9M12 16V5M17 16v-4"/>' },
  folder: { body: '<path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>' },
  arrowRight: { body: '<path d="M5 12h14"/><path d="M13 6l6 6-6 6"/>' },
  mic: { body: '<path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z"/><path d="M19 11v1a7 7 0 0 1-14 0v-1"/><path d="M12 19v3M8 22h8"/>' },
  send: { body: '<path d="M21 3 10 14"/><path d="m21 3-7 18-4-7-7-4z"/>' },
  user: { body: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>' },
  server: { body: '<rect x="4" y="4" width="16" height="6" rx="2"/><rect x="4" y="14" width="16" height="6" rx="2"/><path d="M8 7h.01M8 17h.01M12 7h4M12 17h4"/>' },
  layout: { body: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M9 9v11"/>' },
  gear: { body: '<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M4.93 4.93l2.12 2.12M16.95 16.95l2.12 2.12M2 12h3M19 12h3M4.93 19.07l2.12-2.12M16.95 7.05l2.12-2.12"/>' },
  code: { body: '<path d="m8 9-4 3 4 3"/><path d="m16 9 4 3-4 3"/><path d="m14 5-4 14"/>' },
  algorithm: { body: '<rect x="4" y="4" width="6" height="6" rx="1.5"/><rect x="14" y="4" width="6" height="6" rx="1.5"/><rect x="4" y="14" width="6" height="6" rx="1.5"/><rect x="14" y="14" width="6" height="6" rx="1.5"/><path d="M10 7h4M7 10v4M17 10v4M10 17h4"/>' },
  search: { body: '<circle cx="11" cy="11" r="7"/><path d="m16 16 4 4"/>' },
  dice: { body: '<rect x="4" y="4" width="16" height="16" rx="4"/><circle cx="9" cy="9" r="1" fill="__COLOR__" stroke="none"/><circle cx="15" cy="9" r="1" fill="__COLOR__" stroke="none"/><circle cx="12" cy="12" r="1" fill="__COLOR__" stroke="none"/><circle cx="9" cy="15" r="1" fill="__COLOR__" stroke="none"/><circle cx="15" cy="15" r="1" fill="__COLOR__" stroke="none"/>' },
  sparkle: { body: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/><path d="M5 16l.8 2.2L8 19l-2.2.8L5 22l-.8-2.2L2 19l2.2-.8z"/>' },
  trophy: { body: '<path d="M8 4h8v5a4 4 0 0 1-8 0z"/><path d="M8 6H5a2 2 0 0 0 2 4h1M16 6h3a2 2 0 0 1-2 4h-1"/><path d="M12 13v5M9 21h6M10 18h4"/>' },
  check: { body: '<path d="M20 6 9 17l-5-5"/>' },
  close: { body: '<path d="M18 6 6 18M6 6l12 12"/>' },
  circle: { body: '<circle cx="12" cy="12" r="9"/>' },
};

const rootStyle = computed(() => {
  const size = formatSize(props.size);
  return { width: size, height: size };
});

const src = computed(() => {
  const def = icons[props.name] || icons.circle;
  const color = props.color || '#4A4A4A';
  const strokeWidth = String(props.strokeWidth || 1.8);
  const body = def.body.replace(/__COLOR__/g, color);
  const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%" viewBox="' + (def.viewBox || '0 0 24 24') + '" fill="none" stroke="' + color + '" stroke-width="' + strokeWidth + '" stroke-linecap="round" stroke-linejoin="round">' + body + '</svg>';
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg);
});

function formatSize(size: number | string): string {
  if (typeof size === 'number') return size + 'px';
  if (/^\d+(\.\d+)?$/.test(size)) return size + 'px';
  return size;
}
</script>

<style scoped>
.mian-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  flex-shrink: 0;
}
.mian-icon__svg {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
