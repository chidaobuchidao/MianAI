import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

interface RevealOptions {
  y?: number
  x?: number
  opacity?: number
  scale?: number
  duration?: number
  delay?: number
  stagger?: number
  ease?: string
  start?: string
  once?: boolean
}

export function useScrollReveal(
  target: Ref<HTMLElement | null | undefined>,
  options: RevealOptions = {}
) {
  const isRevealed = ref(false)

  const {
    y = 30,
    x = 0,
    opacity = 0,
    scale = 1,
    duration = 0.8,
    delay = 0,
    stagger = 0.06,
    ease = 'power3.out',
    start = 'top 85%',
    once = true
  } = options

  let st: ScrollTrigger | null = null

  onMounted(() => {
    const el = target.value
    if (!el) return

    const children = el.children.length > 0 && stagger > 0
      ? Array.from(el.children)
      : el

    gsap.fromTo(
      children,
      { y, x, opacity, scale },
      {
        y: 0,
        x: 0,
        opacity: 1,
        scale: 1,
        duration,
        delay,
        stagger: stagger > 0 ? stagger : 0,
        ease,
        scrollTrigger: {
          trigger: el,
          start,
          once,
          toggleActions: once ? 'play none none none' : 'play none none reverse'
        },
        onStart: () => { isRevealed.value = true }
      }
    )
  })

  onUnmounted(() => {
    st?.kill()
  })

  return { isRevealed }
}

export function useSplitReveal(
  target: Ref<HTMLElement | null | undefined>,
  options: RevealOptions = {}
) {
  const {
    y = 40,
    x = 0,
    opacity = 0,
    duration = 1.2,
    stagger = 0.03,
    ease = 'power3.out',
    start = 'top 85%',
    once = true
  } = options

  onMounted(() => {
    const el = target.value
    if (!el) return
    if (!el.textContent) return

    const text = el.textContent
    el.textContent = ''

    // Split into characters wrapped in spans
    const chars = text.split('').map(ch => {
      const span = document.createElement('span')
      span.style.display = 'inline-block'
      span.style.whiteSpace = ch === ' ' ? 'pre' : 'normal'
      span.textContent = ch
      el.appendChild(span)
      return span
    })

    gsap.fromTo(
      chars,
      { y, x, opacity, rotateX: -10 },
      {
        y: 0,
        x: 0,
        opacity: 1,
        rotateX: 0,
        duration,
        stagger,
        ease,
        scrollTrigger: { trigger: el, start, once }
      }
    )
  })
}
