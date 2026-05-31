package com.mianmiantong.service.ai;

public final class AiModelSelector {
    public static final String FLASH = "deepseek-v4-flash";
    public static final String PRO = "deepseek-v4-pro";

    private AiModelSelector() {
    }

    public static String normalize(String model) {
        return PRO.equals(model) ? PRO : FLASH;
    }
}
