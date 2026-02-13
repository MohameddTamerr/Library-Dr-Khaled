package com.library.pos.util;

import com.library.pos.model.Sale;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReceiptTextExporter {

    private ReceiptTextExporter() {
    }

    public static void save(String receipt, List<Sale> sales) {
        try {
            Path dir = Paths.get("receipts");
            Files.createDirectories(dir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String orderId = "NA";
            if (sales != null && !sales.isEmpty() && sales.get(0) != null && sales.get(0).getId() != null) {
                orderId = String.valueOf(sales.get(0).getId());
            }
            Path file = dir.resolve("receipt_" + orderId + "_" + timestamp + ".txt");

            String plain = (receipt == null ? "" : receipt)
                    .replace("<<C>>", "")
                    .replace("<<L>>", "")
                    .replace("<<R>>", "");
            Files.writeString(file, plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
