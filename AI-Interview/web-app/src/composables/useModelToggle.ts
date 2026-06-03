import { ref, computed, onMounted } from 'vue'
import { get } from '@/utils/request'

interface AiConfig {
  provider: string
  preferredModel: string | null
  model: string | null
}

export interface ModelOption {
  id: string
  label: string
}

const PROVIDER_OPTIONS: Record<string, ModelOption[]> = {
  deepseek: [
    { id: 'deepseek-v4-flash', label: 'Flash' },
    { id: 'deepseek-v4-pro', label: 'Pro' },
  ],
  qwen: [
    { id: 'qwen-turbo', label: 'Turbo' },
    { id: 'qwen-plus', label: 'Plus' },
  ],
  doubao: [
    { id: 'doubao-lite-4k', label: 'Lite' },
    { id: 'doubao-pro-4k', label: 'Pro' },
    { id: 'doubao-pro-32k', label: 'Pro-32k' },
  ],
  zhipu: [
    { id: 'glm-4-flash', label: 'Flash' },
    { id: 'glm-4-plus', label: 'Plus' },
    { id: 'glm-4-long', label: 'Long' },
  ],
}

export function useModelToggle() {
  const userProvider = ref('deepseek')
  const userPreferredModel = ref<string | null>(null)
  const currentModel = ref('deepseek-v4-flash')
  const configLoaded = ref(false)

  const options = computed(() => PROVIDER_OPTIONS[userProvider.value] || [])
  const hasOptions = computed(() => options.value.length >= 2)
  const selectedLabel = computed(() => {
    const found = options.value.find(o => o.id === currentModel.value)
    return found?.label || currentModel.value
  })

  function selectModel(id: string) {
    currentModel.value = id
  }

  async function loadConfig() {
    try {
      const res = await get<AiConfig>('/api/user/ai-config')
      if (res.data) {
        userProvider.value = res.data.provider || 'deepseek'
        userPreferredModel.value = res.data.preferredModel || res.data.model
        const opts = PROVIDER_OPTIONS[userProvider.value]
        if (opts && userPreferredModel.value) {
          const match = opts.find(o => o.id === userPreferredModel.value)
          currentModel.value = match ? match.id : opts[0].id
        } else if (opts) {
          currentModel.value = opts[0].id
        } else if (userPreferredModel.value) {
          currentModel.value = userPreferredModel.value
        }
      }
    } catch { /* use defaults */ }
    configLoaded.value = true
  }

  onMounted(loadConfig)

  return {
    currentModel,
    options,
    hasOptions,
    selectedLabel,
    selectModel,
    configLoaded,
    userProvider,
  }
}
