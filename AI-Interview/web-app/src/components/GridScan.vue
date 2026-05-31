<template>
  <div ref="containerRef" class="gridscan" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

interface Props {
  lineThickness?: number
  linesColor?: string
  scanColor?: string
  scanOpacity?: number
  gridScale?: number
  lineStyle?: 'solid' | 'dashed' | 'dotted'
  scanDuration?: number
  scanDelay?: number
  scanGlow?: number
  scanSoftness?: number
  noiseIntensity?: number
}

const props = withDefaults(defineProps<Props>(), {
  lineThickness: 1.2,
  linesColor: '#4a3a5c',
  scanColor: '#D9750A',
  scanOpacity: 0.45,
  gridScale: 0.12,
  lineStyle: 'solid',
  scanDuration: 2.5,
  scanDelay: 2.0,
  scanGlow: 0.6,
  scanSoftness: 2.5,
  noiseIntensity: 0.008
})

const containerRef = ref<HTMLElement>()
let renderer: any | null = null
let material: any | null = null
let rafId = 0

const vert = `
varying vec2 vUv;
void main() { vUv = uv; gl_Position = vec4(position.xy, 0.0, 1.0); }
`

const frag = `
precision highp float;
uniform vec3 iResolution;
uniform float iTime;
uniform vec2 uSkew;
uniform float uTilt;
uniform float uYaw;
uniform float uLineThickness;
uniform vec3 uLinesColor;
uniform vec3 uScanColor;
uniform float uGridScale;
uniform float uLineStyle;
uniform float uLineJitter;
uniform float uScanOpacity;
uniform float uScanDirection;
uniform float uNoise;
uniform float uScanGlow;
uniform float uScanSoftness;
uniform float uPhaseTaper;
uniform float uScanDuration;
uniform float uScanDelay;
varying vec2 vUv;

float smoother01(float a, float b, float x) {
  float t = clamp((x - a) / max(1e-5, b - a), 0.0, 1.0);
  return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 p = (2.0 * fragCoord - iResolution.xy) / iResolution.y;
    vec3 ro = vec3(0.0);
    vec3 rd = normalize(vec3(p, 2.0));

    float cR = cos(uTilt), sR = sin(uTilt);
    rd.xy = mat2(cR, -sR, sR, cR) * rd.xy;

    float cY = cos(uYaw), sY = sin(uYaw);
    rd.xz = mat2(cY, -sY, sY, cY) * rd.xz;

    vec2 skew = clamp(uSkew, vec2(-0.7), vec2(0.7));
    rd.xy += skew * rd.z;

    vec3 color = vec3(0.0);
    float minT = 1e20;
    float gridScale = max(1e-5, uGridScale);
    float fadeStrength = 2.0;
    vec2 gridUV = vec2(0.0);
    float hitIsY = 1.0;

    for (int i = 0; i < 4; i++) {
        float isY = float(i < 2);
        float pos = mix(-0.2, 0.2, float(i)) * isY + mix(-0.5, 0.5, float(i - 2)) * (1.0 - isY);
        float num = pos - (isY * ro.y + (1.0 - isY) * ro.x);
        float den = isY * rd.y + (1.0 - isY) * rd.x;
        float t = num / den;
        vec3 h = ro + rd * t;
        float depthBoost = smoothstep(0.0, 3.0, h.z);
        h.xy += skew * 0.15 * depthBoost;
        bool use = t > 0.0 && t < minT;
        gridUV = use ? mix(h.zy, h.xz, isY) / gridScale : gridUV;
        minT = use ? t : minT;
        hitIsY = use ? isY : hitIsY;
    }

    vec3 hit = ro + rd * minT;
    float dist = length(hit - ro);
    float fx = fract(gridUV.x);
    float fy = fract(gridUV.y);
    float ax = min(fx, 1.0 - fx);
    float ay = min(fy, 1.0 - fy);
    float wx = fwidth(gridUV.x);
    float wy = fwidth(gridUV.y);
    float halfPx = max(0.0, uLineThickness) * 0.5;
    float tx = halfPx * wx;
    float ty = halfPx * wy;
    float aax = wx;
    float aay = wy;
    float lineX = 1.0 - smoothstep(tx, tx + aax, ax);
    float lineY = 1.0 - smoothstep(ty, ty + aay, ay);

    if (uLineStyle > 0.5) {
        float dashRepeat = 4.0;
        float dashDuty = 0.5;
        float vy = fract(gridUV.y * dashRepeat);
        float vx = fract(gridUV.x * dashRepeat);
        if (uLineStyle < 1.5) { lineX *= step(vy, dashDuty); lineY *= step(vx, dashDuty); }
    }
    float primaryMask = max(lineX, lineY);
    float lineVis = primaryMask;

    float dur = max(0.05, uScanDuration);
    float del = max(0.0, uScanDelay);
    float scanZMax = 2.0;
    float sigma = max(0.001, 0.18 * uScanGlow * uScanSoftness);

    float cycle = dur + del;
    float tCycle = mod(iTime, cycle);
    float scanPhase = clamp((tCycle - del) / dur, 0.0, 1.0);
    if (uScanDirection > 0.5 && uScanDirection < 1.5) scanPhase = 1.0 - scanPhase;
    else if (uScanDirection > 1.5) {
        float t2 = mod(max(0.0, iTime - del), 2.0 * dur);
        scanPhase = t2 < dur ? t2 / dur : 1.0 - (t2 - dur) / dur;
    }
    float scanZ = scanPhase * scanZMax;
    float dz = abs(hit.z - scanZ);
    float lineBand = exp(-0.5 * (dz * dz) / (sigma * sigma));
    float headW = clamp(uPhaseTaper, 0.0, 0.49);
    float headFade = smoother01(0.0, headW, scanPhase);
    float tailFade = 1.0 - smoother01(1.0 - headW, 1.0, scanPhase);
    float combinedPulse = lineBand * headFade * tailFade * clamp(uScanOpacity, 0.0, 1.0);

    float fade = exp(-dist * fadeStrength);
    vec3 gridCol = uLinesColor * lineVis * fade;
    vec3 scanCol = uScanColor * combinedPulse;
    color = gridCol + scanCol;

    float n = fract(sin(dot(gl_FragCoord.xy + vec2(iTime * 123.4), vec2(12.9898,78.233))) * 43758.5453123);
    color += (n - 0.5) * uNoise;
    color = clamp(color, 0.0, 1.0);
    float alpha = clamp(max(lineVis, combinedPulse), 0.0, 1.0);
    fragColor = vec4(color, alpha);
}

void main() {
  vec4 c;
  mainImage(c, vUv * iResolution.xy);
  gl_FragColor = c;
}
`

function srgb(hex: string): any {
  const c = new THREE.Color(hex)
  return c.convertSRGBToLinear()
}

onMounted(() => {
  const container = containerRef.value
  if (!container) return

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
  renderer.setSize(container.clientWidth, container.clientHeight)
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.setClearColor(0x000000, 0)
  container.appendChild(renderer.domElement)

  const uniforms = {
    iResolution: { value: new THREE.Vector3(container.clientWidth, container.clientHeight, renderer.getPixelRatio()) },
    iTime: { value: 0 },
    uSkew: { value: new THREE.Vector2(0, 0) },
    uTilt: { value: 0 },
    uYaw: { value: 0 },
    uLineThickness: { value: props.lineThickness },
    uLinesColor: { value: srgb(props.linesColor) },
    uScanColor: { value: srgb(props.scanColor) },
    uGridScale: { value: props.gridScale },
    uLineStyle: { value: props.lineStyle === 'dashed' ? 1 : props.lineStyle === 'dotted' ? 2 : 0 },
    uLineJitter: { value: 0.05 },
    uScanOpacity: { value: props.scanOpacity },
    uNoise: { value: props.noiseIntensity },
    uScanGlow: { value: props.scanGlow },
    uScanSoftness: { value: props.scanSoftness },
    uPhaseTaper: { value: 0.9 },
    uScanDuration: { value: props.scanDuration },
    uScanDelay: { value: props.scanDelay },
    uScanDirection: { value: 2 } // pingpong
  }

  material = new THREE.ShaderMaterial({
    uniforms,
    vertexShader: vert,
    fragmentShader: frag,
    transparent: true,
    depthWrite: false,
    depthTest: false
  })

  const scene = new THREE.Scene()
  const camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1)
  const quad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), material)
  scene.add(quad)

  const onResize = () => {
    if (!renderer) return
    renderer.setSize(container.clientWidth, container.clientHeight)
    material!.uniforms.iResolution.value.set(container.clientWidth, container.clientHeight, renderer.getPixelRatio())
  }
  window.addEventListener('resize', onResize)

  const tick = () => {
    if (!material) return
    material.uniforms.iTime.value = performance.now() / 1000
    renderer!.render(scene, camera)
    rafId = requestAnimationFrame(tick)
  }
  rafId = requestAnimationFrame(tick)

  onUnmounted(() => {
    cancelAnimationFrame(rafId)
    window.removeEventListener('resize', onResize)
    material?.dispose()
    quad.geometry.dispose()
    renderer?.dispose()
    renderer?.forceContextLoss()
    if (renderer?.domElement) container.removeChild(renderer.domElement)
  })
})
</script>

<style scoped>
.gridscan {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
</style>
