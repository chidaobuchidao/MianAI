<!-- PC 端 Canvas 粒子背景，仅 H5 生效 -->
<template>
  <!-- #ifdef H5 -->
  <canvas
    id="particle-canvas"
    class="particle-canvas"
    :style="{ opacity: opacity }"
  />
  <!-- #endif -->
  <!-- #ifndef H5 -->
  <view />
  <!-- #endif -->
</template>

<script setup lang="ts">
import { ref } from 'vue';

const opacity = ref(0.6);

// #ifdef H5
import { onMounted, onUnmounted } from 'vue';

let canvas: HTMLCanvasElement | null = null;
let ctx: CanvasRenderingContext2D | null = null;
let animId = 0;
let particles: Array<{ x: number; y: number; vx: number; vy: number; r: number; a: number }> = [];

onMounted(() => {
  canvas = document.getElementById('particle-canvas') as HTMLCanvasElement;
  if (!canvas) return;

  ctx = canvas.getContext('2d');
  if (!ctx) return;

  const resize = () => {
    if (!canvas) return;
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    initParticles();
  };
  window.addEventListener('resize', resize);
  resize();

  function initParticles() {
    if (!canvas) return;
    const w = canvas.width;
    const h = canvas.height;
    const count = Math.min(80, Math.floor((w * h) / 15000));
    particles = [];
    for (let i = 0; i < count; i++) {
      particles.push({
        x: Math.random() * w,
        y: Math.random() * h,
        vx: (Math.random() - 0.5) * 0.3,
        vy: (Math.random() - 0.5) * 0.3,
        r: Math.random() * 3 + 1,
        a: Math.random() * 0.4 + 0.05,
      });
    }
  }

  function animate() {
    if (!ctx || !canvas) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    for (const p of particles) {
      p.x += p.vx;
      p.y += p.vy;
      if (p.x < 0) p.x = canvas.width;
      if (p.x > canvas.width) p.x = 0;
      if (p.y < 0) p.y = canvas.height;
      if (p.y > canvas.height) p.y = 0;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(217,117,10,${p.a})`;
      ctx.fill();
    }
    animId = requestAnimationFrame(animate);
  }
  animate();
});

onUnmounted(() => {
  cancelAnimationFrame(animId);
  window.removeEventListener('resize', () => {});
});
// #endif
</script>

<style scoped>
.particle-canvas {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
}
</style>
