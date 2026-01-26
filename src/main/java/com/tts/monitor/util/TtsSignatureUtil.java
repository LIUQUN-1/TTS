package com.tts.monitor.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TTS API 签名工具类
 * 参考官方文档：https://partner.tiktokshop.com/docv2/page/sign-your-api-request
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
@Slf4j
public class TtsSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Set<String> EXCLUDE_KEYS = Set.of("sign", "access_token");

    /**
     * 生成签名
     * 
     * @param params 查询参数（不包含sign和access_token）
     * @param body 请求体（POST请求时）
     * @param secret 应用密钥
     * @param pathname 请求路径（如：/affiliate_creator/202509/open_collaborations/products）
     * @param contentType 内容类型
     * @return 签名字符串
     */
    public static String generateSign(Map<String, String> params, String body, 
                                     String secret, String pathname, String contentType) {
        try {
            // Step 1: 提取并排序参数键（排除 sign 和 access_token）
            List<String> keys = params == null ? new ArrayList<>() : 
                params.keySet().stream()
                    .filter(k -> !EXCLUDE_KEYS.contains(k))
                    .sorted()
                    .toList();

            // Step 2: 拼接参数为 {key}{value} 格式
            StringBuilder paramString = new StringBuilder();
            for (String key : keys) {
                paramString.append(key).append(params.get(key));
            }

            // Step 3: 前置请求路径
            StringBuilder signString = new StringBuilder();
            signString.append(pathname).append(paramString);

            // Step 4: 如果不是 multipart/form-data 且有 body，追加 body
            if (!"multipart/form-data".equals(contentType) && body != null && !body.isEmpty()) {
                signString.append(body);
            }

            // Step 5: 用 secret 包裹
            String wrappedString = secret + signString + secret;

            // Step 6: HMAC-SHA256 签名
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(wrappedString.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String sign = hexString.toString();
            log.debug("生成签名成功 - pathname: {}, sign: {}", pathname, sign);
            return sign;

        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成签名（简化版，用于 GET 请求或无 body 的 POST 请求）
     * 
     * @param params 查询参数
     * @param secret 应用密钥
     * @param pathname 请求路径
     * @return 签名字符串
     */
    public static String generateSign(Map<String, String> params, String secret, String pathname) {
        return generateSign(params, null, secret, pathname, "application/json");
    }

    /**
     * 构建查询字符串（保持 product_ids 中的逗号不被编码）
     * 
     * @param params 查询参数
     * @return 查询字符串
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            String key = entry.getKey();
            String value = entry.getValue();
            
            queryString.append(urlEncode(key)).append("=");
            // product_ids 不编码逗号，其他参数正常编码
            if ("product_ids".equals(key)) {
                queryString.append(value);
            } else {
                queryString.append(urlEncode(value));
            }
        }
        return queryString.toString();
    }

    /**
     * URL 编码
     */
    private static String urlEncode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(str, StandardCharsets.UTF_8)
                .replace("+", "%20");
        } catch (Exception e) {
            return str;
        }
    }
}
