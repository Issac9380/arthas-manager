package com.arthasmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * HTTP 请求日志配置。
 *
 * <p>仅在 {@code verbose} profile 下激活，避免在生产环境中打印请求体（可能含敏感信息）。
 *
 * <p>配合 logback-spring.xml 中对 {@code CommonsRequestLoggingFilter} 的 DEBUG 级别，
 * 可以在控制台 / 日志文件中看到完整的 HTTP 请求信息：
 * <pre>
 * Before request [POST /api/arthas/execute, client=127.0.0.1,
 *   headers=[Authorization:"Bearer xxx", Content-Type:"application/json"],
 *   payload={"commandType":"jvm","sessionId":"..."}]
 * </pre>
 *
 * <p>不需要时，去掉 {@code verbose} profile 即可关闭，零改动业务代码。
 */
@Configuration
@Profile("verbose")
public class LoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10_000);      // 请求体最多打印 10 KB
        filter.setIncludeHeaders(true);
        filter.setHeaderPredicate(name ->        // 脱敏：不打印 Authorization 原文
                !name.equalsIgnoreCase("authorization") &&
                !name.equalsIgnoreCase("cookie"));
        filter.setIncludeClientInfo(true);
        filter.setBeforeMessagePrefix(">>> HTTP REQUEST  | ");
        filter.setAfterMessagePrefix("<<< HTTP RESPONSE | ");
        return filter;
    }
}
