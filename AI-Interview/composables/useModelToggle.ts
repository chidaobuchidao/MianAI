import { computed, onMounted, ref } from 'vue';
import { get } from '@/utils/request';

export interface ModelOption {
  id: string;
  label: string;
}

export interface ProviderPreset {
  id: string;
  name: string;
  models?: Array<string | ModelOption>;
}

interface AiConfig {
  provider?: string;
  preferredModel?: string | null;
  model?: string | null;
}

const FALLBACK_OPTIONS: Record<string, ModelOption[]> = {
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
};

export function fallbackModels(provider: string): ModelOption[] {
  return FALLBACK_OPTIONS[provider] || FALLBACK_OPTIONS.deepseek;
}

export function useModelToggle() {
  const userProvider = ref('deepseek');
  const currentModel = ref('deepseek-v4-flash');
  const configLoaded = ref(false);

  const options = computed(() => fallbackModels(userProvider.value));
  const hasOptions = computed(() => options.value.length >= 2);
  const selectedLabel = computed(() => options.value.find((o) => o.id === currentModel.value)?.label || currentModel.value);

  function selectModel(id: string) {
    currentModel.value = id;
  }

  async function loadConfig() {
    try {
      const res = await get<AiConfig>('/api/user/ai-config');
      const cfg = res.data || {};
      userProvider.value = cfg.provider || 'deepseek';
      const opts = fallbackModels(userProvider.value);
      const preferred = cfg.preferredModel || cfg.model || opts[0]?.id || 'deepseek-v4-flash';
      currentModel.value = opts.some((o) => o.id === preferred) ? preferred : (opts[0]?.id || preferred);
    } catch {
      // 使用默认模型
    } finally {
      configLoaded.value = true;
    }
  }

  onMounted(loadConfig);

  return { currentModel, options, hasOptions, selectedLabel, selectModel, configLoaded, userProvider, loadConfig };
}
