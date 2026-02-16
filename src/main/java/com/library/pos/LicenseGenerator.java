package com.library.pos;

import com.library.pos.util.SecurityUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class LicenseGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==========================================");
        System.out.println("   Library POS - License Key Generator    ");
        System.out.println("==========================================");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) {
                continue;
            }

            switch (choice) {
                case "1" -> generateLicense(scanner);
                case "2" -> revokeMachine(scanner);
                case "3" -> unrevokeMachine(scanner);
                case "4" -> listRevoked();
                case "5", "exit", "q", "quit" -> {
                    scanner.close();
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 1-5.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\nChoose an action:");
        System.out.println("1) Generate license key");
        System.out.println("2) Revoke device");
        System.out.println("3) Remove device from revoked list");
        System.out.println("4) Show revoked devices");
        System.out.println("5) Exit");
        System.out.print("Option: ");
    }

    private static void generateLicense(Scanner scanner) {
        String machineId = promptMachineId(scanner);
        if (machineId == null) {
            return;
        }

        String licenseKey = SecurityUtil.generateLicenseKey(machineId);
        System.out.println("------------------------------------------");
        System.out.println("Machine ID : " + machineId);
        System.out.println("License Key: " + licenseKey);
        System.out.println("------------------------------------------");
    }

    private static void revokeMachine(Scanner scanner) {
        String machineId = promptMachineId(scanner);
        if (machineId == null) {
            return;
        }

        try {
            boolean changed = SecurityUtil.revokeMachine(machineId);
            if (changed) {
                System.out.println("Device revoked successfully.");
            } else {
                System.out.println("Device is already revoked.");
            }
            System.out.println("Revocation file: " + SecurityUtil.getRevocationFilePath().toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Failed to revoke device: " + e.getMessage());
        }
    }

    private static void unrevokeMachine(Scanner scanner) {
        String machineId = promptMachineId(scanner);
        if (machineId == null) {
            return;
        }

        try {
            boolean changed = SecurityUtil.unrevokeMachine(machineId);
            if (changed) {
                System.out.println("Device removed from revoked list.");
            } else {
                System.out.println("Device was not in revoked list.");
            }
            System.out.println("Revocation file: " + SecurityUtil.getRevocationFilePath().toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Failed to update revoked list: " + e.getMessage());
        }
    }

    private static void listRevoked() {
        Set<String> revoked = SecurityUtil.listRevokedMachines();
        System.out.println("------------------------------------------");
        System.out.println("Revocation file: " + SecurityUtil.getRevocationFilePath().toAbsolutePath());
        if (revoked.isEmpty()) {
            System.out.println("No revoked devices.");
            System.out.println("------------------------------------------");
            return;
        }

        List<String> items = new ArrayList<>(revoked);
        items.sort(String::compareTo);
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ". " + items.get(i));
        }
        System.out.println("------------------------------------------");
    }

    private static String promptMachineId(Scanner scanner) {
        System.out.println("\nEnter Client Machine ID (or blank to cancel):");
        String machineId = scanner.nextLine().trim();
        if (machineId.isEmpty()) {
            return null;
        }
        return machineId;
    }

}
