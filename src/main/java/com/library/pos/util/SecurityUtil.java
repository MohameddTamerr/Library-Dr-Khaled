package com.library.pos.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class SecurityUtil {

    private static final String SECRET_PHRASE = "DrKhaled_Library_POS_Secret_Key_2024_#$@!";

    public static String getMachineId() {
        try {
            String computerName = System.getenv("COMPUTERNAME");
            String userName = System.getProperty("user.name");
            String processorId = getProcessorId();
            String motherboardSn = getMotherboardSN();

            String rawId = computerName + "|" + userName + "|" + processorId + "|" + motherboardSn;
            return hash(rawId);
        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN-MACHINE-ID";
        }
    }

    public static boolean validateKey(String licenseKey) {
        String machineId = getMachineId();
        String expectedKey = generateLicenseKey(machineId);
        return expectedKey.equals(licenseKey);
    }

    public static String generateLicenseKey(String machineId) {
        try {
            String rawKey = machineId + "|" + SECRET_PHRASE;
            return hash(rawKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getProcessorId() {
        try {
            Process process = Runtime.getRuntime().exec("wmic cpu get processorid");
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.toLowerCase().contains("processorid")) {
                    sb.append(line.trim());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "CPU-ID-UNAVAILABLE";
        }
    }

    private static String getMotherboardSN() {
        try {
            Process process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.toLowerCase().contains("serialnumber")) {
                    sb.append(line.trim());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "MB-SN-UNAVAILABLE";
        }
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).trim(); // Basic Base64 hash
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
