/*
 * Ant Group
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package com.alipay.rta.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class MD5Util {

    public MD5Util() {
    }

    public static String hexDigest(String plainText) {
        if (plainText == null) {
            return null;
        } else {
            try {
                MessageDigest e = MessageDigest.getInstance("MD5");
                e.update(plainText.getBytes());
                return getDigest(e);
            } catch (NoSuchAlgorithmException e) {
                log.error("md5出错", e);
                return "";
            }
        }
    }

    public static MessageDigest start() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error("md5出错", e);
            return null;
        }
    }

    public static boolean update(MessageDigest messageDigest, String text) {
        try {
            messageDigest.update(text.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            log.error("md5出错", e);
            return false;
        }
    }

    public static String getDigest(MessageDigest messageDigest) {
        try {
            byte[] byteArray = messageDigest.digest();
            StringBuilder stringBuilder = new StringBuilder();

            for (byte b : byteArray) {
                if (Integer.toHexString(255 & b).length() == 1) {
                    stringBuilder.append("0").append(Integer.toHexString(255 & b));
                } else {
                    stringBuilder.append(Integer.toHexString(255 & b));
                }
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("md5出错", e);
            return "";
        }
    }
}

