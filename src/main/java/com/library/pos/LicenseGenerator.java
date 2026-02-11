package com.library.pos;

import com.library.pos.util.SecurityUtil;
import java.util.Scanner;

public class LicenseGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==========================================");
        System.out.println("   Library POS - License Key Generator    ");
        System.out.println("==========================================");

        while (true) {
            System.out.println("\nEnter Client Machine ID (or 'exit' to quit):");
            String machineId = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(machineId)) {
                break;
            }

            if (machineId.isEmpty()) {
                continue;
            }

            String licenseKey = SecurityUtil.generateLicenseKey(machineId);
            System.out.println("------------------------------------------");
            System.out.println("License Key: " + licenseKey);
            System.out.println("------------------------------------------");
        }

        scanner.close();
    }
}
