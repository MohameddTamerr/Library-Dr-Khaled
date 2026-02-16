package com.library.pos.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SecurityUtil {

    private static final String SECRET_PHRASE = "DrKhaled_Library_POS_Secret_Key_2024_#$@!";
    private static final String REVOCATION_FILE = "revoked_devices.dat";

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

    public static Path getRevocationFilePath() {
        return Paths.get(REVOCATION_FILE);
    }

    public static boolean isCurrentMachineRevoked() {
        return isMachineRevoked(getMachineId());
    }

    public static boolean isMachineRevoked(String machineId) {
        if (machineId == null || machineId.isBlank()) {
            return false;
        }
        return listRevokedMachines().contains(machineId.trim());
    }

    public static Set<String> listRevokedMachines() {
        Path path = getRevocationFilePath();
        if (!Files.exists(path)) {
            return Collections.emptySet();
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Set<String> revoked = new LinkedHashSet<>();
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                String value = line.trim();
                if (value.isBlank() || value.startsWith("#")) {
                    continue;
                }
                revoked.add(value);
            }
            return Collections.unmodifiableSet(revoked);
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    public static boolean revokeMachine(String machineId) throws IOException {
        String normalized = normalizeMachineId(machineId);
        Set<String> revoked = new LinkedHashSet<>(listRevokedMachines());
        boolean changed = revoked.add(normalized);
        if (changed) {
            writeRevokedMachines(revoked);
        }
        return changed;
    }

    public static boolean unrevokeMachine(String machineId) throws IOException {
        String normalized = normalizeMachineId(machineId);
        Set<String> revoked = new LinkedHashSet<>(listRevokedMachines());
        boolean changed = revoked.remove(normalized);
        if (changed) {
            writeRevokedMachines(revoked);
        }
        return changed;
    }

    private static void writeRevokedMachines(Set<String> revoked) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Library POS revoked machine IDs");
        lines.add("# One machine ID per line");

        List<String> sorted = new ArrayList<>(revoked);
        Collections.sort(sorted);
        lines.addAll(sorted);

        Files.write(getRevocationFilePath(), lines, StandardCharsets.UTF_8);
    }

    private static String normalizeMachineId(String machineId) {
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("Machine ID cannot be empty.");
        }
        return machineId.trim();
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
