/*
 * Ant Group
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package com.alipay.rta;

import com.alipay.adbasiclib.rta.proto.RtaReqInfo;
import com.alipay.adbasiclib.rta.proto.RtaRequest;
import com.alipay.adbasiclib.rta.proto.RtaResponse;
import com.alipay.rta.util.MD5Util;
import com.alipay.rta.util.ProtobufUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 用于测试RTA HTTP接口
 */
@Slf4j
public class RtaTest {

    public static void main(String[] args) {
        RtaRequest request = buildRequest(RtaRequest.OsType.ANDROID);

        try (HttpClientPool httpClient = new HttpClientPool()) {
            long start = System.currentTimeMillis();
            byte[] rtaResponseByte = httpClient.post(
                    "http://127.0.0.1:8080/rta/alipay",
                    request.toByteArray(),
                    20000
            );
            long end = System.currentTimeMillis();
            log.debug("total ms: {}", (end - start));

            log.debug("Rta response body: {}", new String(rtaResponseByte));
            RtaResponse rtaResponse = RtaResponse.parseFrom(rtaResponseByte);

            log.debug("========RtaResponse========");
            log.debug(ProtobufUtils.toJson(rtaResponse));
        } catch (Exception e) {
            log.error("Rta request fail: ", e);
        }
    }

    /**
     * 在此构造请求参数
     *
     * @param osType 设备类型
     * @return RTA请求
     */
    public static RtaRequest buildRequest(RtaRequest.OsType osType) {
        RtaRequest.Builder rtaRequestBuilder = RtaRequest.newBuilder();

        rtaRequestBuilder.setUserId("2088802123844356");
        rtaRequestBuilder.setReqId(UUID.randomUUID().toString());
        rtaRequestBuilder.setRequestTime(System.currentTimeMillis());

        // 设备信息
        RtaRequest.DeviceInfo.Builder deviceInfoBuilder = RtaRequest.DeviceInfo.newBuilder();
        // ios
        if (osType == RtaRequest.OsType.IOS) {
            deviceInfoBuilder
                    .addDeviceId(RtaRequest.DeviceID.newBuilder()
                            .setType(RtaRequest.IDType.IDFA_HASH)
                            .setValue(MD5Util.hexDigest("739372CF-EA98-431F-AE1F-424BF1283444").toLowerCase())
                            //.setValue("idfa_hash_to_bid_1")
                            .build())
                    .addDeviceId(RtaRequest.DeviceID.newBuilder()
                            .setType(RtaRequest.IDType.CAID)
                            .setVersion("20220111")
                            .setValue("869120047154725")
                            .build())
                    .addDeviceId(RtaRequest.DeviceID.newBuilder()
                            .setType(RtaRequest.IDType.CAID)
                            .setVersion("20230330")
                            .setValue("869120047154725")
                            .build())
                    .setOsType(RtaRequest.OsType.IOS)
            ;
        }

        // android
        if (osType == RtaRequest.OsType.ANDROID) {
            deviceInfoBuilder
                    .addDeviceId(RtaRequest.DeviceID.newBuilder()
                            .setType(RtaRequest.IDType.OAID_HASH)
                            //.setValue(MD5Util.hexDigest("396bd57f-74b5-4118-8c3a-296b7babefc7").toLowerCase())
                            .setValue("oaid_hash_to_bid_1")
                            .build()
                    )
                    .setOsType(RtaRequest.OsType.ANDROID)
            ;
        }

        rtaRequestBuilder.setDeviceInfo(deviceInfoBuilder.build());

        // 现改为询问所有rta_id的参竞信息，不再透传
        boolean enableSendRtaId = false;
        if (enableSendRtaId) {
            // 参竞item信息
            RtaReqInfo.Builder rtaReqInfoBuilder = RtaReqInfo.newBuilder();

            // 一个参竞的rta_id（于RTA管理页面中配置）
            rtaReqInfoBuilder.setRtaId("rta_id_to_bid_1");
            rtaRequestBuilder.addRtaReqInfo(rtaReqInfoBuilder.build());

            // 一个不参竞的rta_id
            rtaReqInfoBuilder.setRtaId("rta_id_to_filter");
            rtaRequestBuilder.addRtaReqInfo(rtaReqInfoBuilder.build());
        }

        RtaRequest request = rtaRequestBuilder.build();
        log.debug("========RtaRequest========");
        log.debug(ProtobufUtils.toJson(request));

        return request;
    }

}