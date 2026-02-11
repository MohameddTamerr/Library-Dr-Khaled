package com.library.pos.util;

import org.junit.jupiter.api.Test;

public class SecurityUtilTest {

    @Test
    public void generateLicenseFile() throws java.io.IOException {
        String machineId = SecurityUtil.getMachineId();
        String licenseKey = SecurityUtil.generateLicenseKey(machineId);

        System.out.println("Machine ID: " + machineId);
        System.out.println("License Key: " + licenseKey);

        java.nio.file.Files.writeString(java.nio.file.Paths.get("license.dat"), licenseKey);
        java.nio.file.Files.writeString(java.nio.file.Paths.get("machine_info.txt"),
                "ID: " + machineId + "\nKey: " + licenseKey);
    }
}
