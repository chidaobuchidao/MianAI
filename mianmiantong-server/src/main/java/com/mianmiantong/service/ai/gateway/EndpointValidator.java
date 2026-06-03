package com.mianmiantong.service.ai.gateway;

import java.net.InetAddress;
import java.net.URI;

/**
 * 端点验证器 - SSRF 防护
 * 验证用户提供的自定义端点安全性
 */
public class EndpointValidator {

    /**
     * 验证端点安全性
     * @param endpoint 端点 URL
     * @throws IllegalArgumentException 如果端点不安全
     */
    public void validate(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("端点不能为空");
        }

        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的端点 URL: " + endpoint);
        }

        // 仅允许 HTTPS
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("仅支持 HTTPS 端点");
        }

        // 检查主机名
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("端点缺少主机名");
        }

        // 屏蔽 localhost
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
            throw new IllegalArgumentException("不允许访问 localhost");
        }

        // 屏蔽私有 IP
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                throw new IllegalArgumentException("不允许访问私有网络地址");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            // DNS 解析失败时允许继续（可能是公网域名）
        }
    }
}
