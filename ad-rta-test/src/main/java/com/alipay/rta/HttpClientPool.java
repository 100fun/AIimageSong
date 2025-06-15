/*
 * Ant Group
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package com.alipay.rta;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.CharCodingConfig;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpConnectionFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientPool implements Closeable {

    private final PoolingHttpClientConnectionManager connectionManager;
    private final CloseableHttpClient                httpClient;

    public HttpClientPool() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(250, TimeUnit.MILLISECONDS) //ConnectionManager获取连接的超时时间
                .setCookieSpec(StandardCookieSpec.IGNORE) //关闭cookie，启动手动cookie模式
                .build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(250, TimeUnit.MILLISECONDS) //读写超时时间
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(250, TimeUnit.MILLISECONDS) //连接超时时间
                .setValidateAfterInactivity(Timeout.NEG_ONE_MILLISECOND) //传入负值表示关闭stale连接检查
                .build();

        CharCodingConfig charCodingConfig = CharCodingConfig.custom()
                .setCharset(StandardCharsets.UTF_8)
                .build();

        Http1Config http1Config = Http1Config.custom()
                .setBufferSize(4096)
                .build();

        HttpConnectionFactory<ManagedHttpClientConnection> connectionFactory =
                ManagedHttpClientConnectionFactory.builder()
                        .charCodingConfig(charCodingConfig)
                        .http1Config(http1Config)
                        .build();

        ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public TimeValue getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                return TimeValue.ofMilliseconds(60 * 1000);
            }
        };

        connectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register(URIScheme.HTTP.id, PlainConnectionSocketFactory.getSocketFactory())
                        .register(URIScheme.HTTPS.id, SSLConnectionSocketFactory.getSocketFactory())
                        .build(),
                connectionFactory
        );

        connectionManager.setDefaultConnectionConfig(connectionConfig);
        connectionManager.setDefaultSocketConfig(socketConfig);
        connectionManager.setMaxTotal(8 * 128);
        connectionManager.setDefaultMaxPerRoute(128);

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(connectionKeepAliveStrategy)
                // 定期回收空闲连接, 解决close_wait问题
                .evictIdleConnections(TimeValue.ofSeconds(60L))
                // 回收过期连接
                .evictExpiredConnections()
                // 关闭自动重试策略
                .disableAutomaticRetries()
                .build();
    }

    @Override
    public void close() {
        try {
            httpClient.close();
            connectionManager.close();
        } catch (IOException e) {
            log.error("httpClient close error", e);
        }
    }

    public byte[] post(String url, byte[] body, long timeoutMs) throws Exception {
        return post(url, body, timeoutMs, null);
    }

    public byte[] post(String url, byte[] body, long timeoutMs, Header[] headers) throws Exception {
        RequestConfig.Builder builder = RequestConfig.custom()
                // 设置客户端等待服务端返回数据的超时时间
                .setResponseTimeout(Timeout.ofMilliseconds(timeoutMs))
                .setConnectTimeout(Timeout.ofMilliseconds(timeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeoutMs));

        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(builder.build());
        if (headers == null) {
            httpPost.setHeader("Content-Type", "application/octet-stream");
            //兼容application/x-protobuf
            //httpPost.setHeader("Content-Type", "application/x-protobuf;charset=UTF-8");
        } else {
            httpPost.setHeaders(headers);
        }
        httpPost.setEntity(new ByteArrayEntity(body, null));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            log.debug("========Response Header========");
            for (Header header : response.getHeaders()) {
                log.debug(header.toString());
            }
            if (200 != response.getCode() || response.getEntity().getContentLength() == 0) {
                log.error("http post error, response = {}", response);
            }
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

}