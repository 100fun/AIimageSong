/*
 * Ant Group
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package com.alipay.rta.util;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtobufUtils {

    private static final JsonFormat.Printer jsonPrinter = JsonFormat.printer();

    public static String toJson(MessageOrBuilder message) {
        try {
            return jsonPrinter.print(message);
        } catch (Exception e) {
            log.error("safeToJson failed", e);
            return "";
        }
    }

}
