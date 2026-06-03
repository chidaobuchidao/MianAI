import { ref, computed, onMounted } from 'vue'
import { get } from '@/utils/request'

interface AiConfig {
  provider: string
  preferredModel: string | null
  model: string | null
}

interface ModelPair {
  label: string
  fast: string
  pro: string
}

const PROVIDER_MODELS: Record<string, ModelPair> = {
  deepseek: { label: 'Flash / Pro', fast: 'deepseek-v4-flash', pro: 'deepseek-v4-pro' },
  qwen: { label: 'Turbo / Plus', fast: 'qwen-turbo', pro: 'qwen-plus' },
  doubao: { label: 'Lite / Pro', fast: 'doubao-lite-4k', pro: 'doubao-pro-4k' },
  zhipu: { label: 'Flash / Plus', fast: 'glm-4-flash', pro: 'glm-4-plus' },
}

export function useModelToggle() {
  const userProvider = ref('deepseek')
  const userPreferredModel = ref<string | null>(null)
  const currentModel = ref('deepseek-v4-flash')
  const configLoaded = ref(false)

  const modelPair = computed(() => PROVIDER_MODELS[userProvider.value])
  const hasToggle = computed(() => !!modelPair.value)
  const isPro = computed(() => modelPair.value ? currentModel.value === modelPair.value.pro : false)

  const toggleLabel = computed(() => {
    if (!modelPair.value) return currentModel.value
    return modelPair.value.label
  })

  const displayLeft = computed(() => modelPair.value?.fast.split('-').pop() || 'Fast')
  const displayRight = computed(() => modelPair.value?.pro.split('-').pop() || 'Pro')

  async function loadConfig() {
    try {
      const res = await get<AiConfig>('/api/user/ai-config')
      if (res.data) {
        userProvider.value = res.data.provider || 'deepseek'
        userPreferredModel.value = res.data.preferredModel || res.data.model
        // Set current model based on preferred or default
        const pair = PROVIDER_MODELS[userProvider.value]
        if (pair && userPreferredModel.value) {
          currentModel.value = userPreferredModel.value
        } else if (pair) {
          currentModel.value = pair.fast
        } else if (userPreferredModel.value) {
          currentModel.value = userPreferredModel.value
        }
      }
    } catch { /* use defaults */ }
    configLoaded.value = true
  }

  function toggle() {
    if (!modelPair.value) return
    currentModel.value = isPro.value ? modelPair.value.fast : modelPair.value.pro
  }

  onMounted(loadConfig)

  return {
    currentModel,
    hasToggle,
    isPro,
    toggleLabel,
    displayLeft,
    displayRight,
    toggle,
    configLoaded,
    userProvider,
  }
}
