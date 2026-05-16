<template>
  <div ref="containerRef" class="particle-bg" aria-hidden="true" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Renderer, Camera, Geometry, Program, Mesh } from 'ogl'

interface Props {
  particleCount?: number
  particleSpread?: number
  speed?: number
  particleColors?: string[]
  moveOnHover?: boolean
  hoverFactor?: number
  alphaParticles?: boolean
  particleBaseSize?: number
  sizeRandomness?: number
  cameraDistance?: number
  disableRotation?: boolean
  pixelRatio?: number
}

const props = withDefaults(defineProps<Props>(), {
  particleCount: 200,
  particleSpread: 10,
  speed: 0.1,
  particleColors: () => ['#ffffff', '#f5f0eb', '#e8e0d5'],
  moveOnHover: true,
  hoverFactor: 1,
  alphaParticles: true,
  particleBaseSize: 100,
  sizeRandomness: 1,
  cameraDistance: 20,
  disableRotation: false,
  pixelRatio: 1
})

const containerRef = ref<HTMLElement>()

const vertex = /* glsl */ `
  attribute vec3 position;
  attribute vec4 random;
  attribute vec3 color;

  uniform mat4 modelMatrix;
  uniform mat4 viewMatrix;
  uniform mat4 projectionMatrix;
  uniform float uTime;
  uniform float uSpread;
  uniform float uBaseSize;
  uniform float uSizeRandomness;

  varying vec4 vRandom;
  varying vec3 vColor;

  void main() {
    vRandom = random;
    vColor = color;

    vec3 pos = position * uSpread;
    pos.z *= 10.0;

    vec4 mPos = modelMatrix * vec4(pos, 1.0);
    float t = uTime;
    mPos.x += sin(t * random.z + 6.28 * random.w) * mix(0.1, 1.5, random.x);
    mPos.y += sin(t * random.y + 6.28 * random.x) * mix(0.1, 1.5, random.w);
    mPos.z += sin(t * random.w + 6.28 * random.y) * mix(0.1, 1.5, random.z);

    vec4 mvPos = viewMatrix * mPos;

    if (uSizeRandomness == 0.0) {
      gl_PointSize = uBaseSize;
    } else {
      gl_PointSize = (uBaseSize * (1.0 + uSizeRandomness * (random.x - 0.5))) / length(mvPos.xyz);
    }

    gl_Position = projectionMatrix * mvPos;
  }
`

const fragment = /* glsl */ `
  precision highp float;

  uniform float uTime;
  uniform float uAlphaParticles;
  varying vec4 vRandom;
  varying vec3 vColor;

  void main() {
    vec2 uv = gl_PointCoord.xy;
    float d = length(uv - vec2(0.5));

    if(uAlphaParticles < 0.5) {
      if(d > 0.5) {
        discard;
      }
      gl_FragColor = vec4(vColor + 0.2 * sin(uv.yxx + uTime + vRandom.y * 6.28), 1.0);
    } else {
      float circle = smoothstep(0.5, 0.4, d) * 0.8;
      gl_FragColor = vec4(vColor + 0.2 * sin(uv.yxx + uTime + vRandom.y * 6.28), circle);
    }
  }
`

function hexToRgb(hex: string): [number, number, number] {
  const clean = hex.replace(/^#/, '')
  const h = clean.length === 3 ? clean.split('').map(c => c + c).join('') : clean
  const int = parseInt(h, 16)
  return [(int >> 16) & 255, (int >> 8) & 255, int & 255].map(v => v / 255) as [number, number, number]
}

let animationId = 0

onMounted(() => {
  const container = containerRef.value
  if (!container) return

  const renderer = new Renderer({
    dpr: props.pixelRatio,
    depth: false,
    alpha: true
  })
  const gl = renderer.gl
  container.appendChild(gl.canvas)
  gl.clearColor(0, 0, 0, 0)

  const camera = new Camera(gl, { fov: 15 })
  camera.position.set(0, 0, props.cameraDistance)

  const resize = () => {
    const w = container.clientWidth
    const h = container.clientHeight
    renderer.setSize(w, h)
    camera.perspective({ aspect: gl.canvas.width / gl.canvas.height })
  }
  window.addEventListener('resize', resize)
  resize()

  const mouse = { x: 0, y: 0 }
  const handleMouseMove = (e: MouseEvent) => {
    const rect = container.getBoundingClientRect()
    mouse.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -(((e.clientY - rect.top) / rect.height) * 2 - 1)
  }

  if (props.moveOnHover) {
    container.addEventListener('mousemove', handleMouseMove)
  }

  const count = props.particleCount
  const positions = new Float32Array(count * 3)
  const randoms = new Float32Array(count * 4)
  const colors = new Float32Array(count * 3)
  const palette = props.particleColors

  for (let i = 0; i < count; i++) {
    let x: number, y: number, z: number, len: number
    do {
      x = Math.random() * 2 - 1
      y = Math.random() * 2 - 1
      z = Math.random() * 2 - 1
      len = x * x + y * y + z * z
    } while (len > 1 || len === 0)
    const r = Math.cbrt(Math.random())
    positions.set([x * r, y * r, z * r], i * 3)
    randoms.set([Math.random(), Math.random(), Math.random(), Math.random()], i * 4)
    const col = hexToRgb(palette[Math.floor(Math.random() * palette.length)])
    colors.set(col, i * 3)
  }

  const geometry = new Geometry(gl, {
    position: { size: 3, data: positions },
    random: { size: 4, data: randoms },
    color: { size: 3, data: colors }
  })

  const program = new Program(gl, {
    vertex,
    fragment,
    uniforms: {
      uTime: { value: 0 },
      uSpread: { value: props.particleSpread },
      uBaseSize: { value: props.particleBaseSize * props.pixelRatio },
      uSizeRandomness: { value: props.sizeRandomness },
      uAlphaParticles: { value: props.alphaParticles ? 1 : 0 }
    },
    transparent: true,
    depthTest: false
  })

  const particles = new Mesh(gl, { mode: gl.POINTS, geometry, program })

  let lastTime = performance.now()
  let elapsed = 0

  const update = (t: number) => {
    animationId = requestAnimationFrame(update)
    const delta = t - lastTime
    lastTime = t
    elapsed += delta * props.speed

    program.uniforms.uTime.value = elapsed * 0.001

    if (props.moveOnHover) {
      particles.position.x = -mouse.x * props.hoverFactor
      particles.position.y = -mouse.y * props.hoverFactor
    }

    if (!props.disableRotation) {
      particles.rotation.x = Math.sin(elapsed * 0.0002) * 0.1
      particles.rotation.y = Math.cos(elapsed * 0.0005) * 0.15
      particles.rotation.z += 0.01 * props.speed
    }

    renderer.render({ scene: particles, camera })
  }

  animationId = requestAnimationFrame(update)

  onUnmounted(() => {
    window.removeEventListener('resize', resize)
    if (props.moveOnHover) {
      container.removeEventListener('mousemove', handleMouseMove)
    }
    cancelAnimationFrame(animationId)
    if (container.contains(gl.canvas)) {
      container.removeChild(gl.canvas)
    }
  })
})
</script>

<style scoped>
.particle-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle-bg :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
