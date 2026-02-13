package com.library.pos.util.escpos;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class UsbPrinterService {

    public PrintService findPrinter(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            return null;
        }

        if (printerName != null && !printerName.isBlank()) {
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(printerName)) {
                    return service;
                }
            }

            String target = printerName.toLowerCase(Locale.ROOT);
            for (PrintService service : services) {
                if (service.getName().toLowerCase(Locale.ROOT).contains(target)) {
                    return service;
                }
            }
        }

        for (PrintService service : services) {
            String n = service.getName().toLowerCase(Locale.ROOT);
            if (n.contains("xp-370b") || n.contains("xp370b") || n.contains("xprinter")) {
                return service;
            }
        }

        return null;
    }

    public void printRaw(String printerName, byte[] payload) {
        PrintService service = findPrinter(printerName);
        if (service == null) {
            throw new IllegalStateException("Printer not found: " + printerName);
        }
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Print payload is empty");
        }

        List<DocFlavor> attemptFlavors = List.of(
                DocFlavor.BYTE_ARRAY.AUTOSENSE,
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,
                DocFlavor.INPUT_STREAM.AUTOSENSE);

        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        List<String> errors = new ArrayList<>();
        for (DocFlavor flavor : attemptFlavors) {
            if (!service.isDocFlavorSupported(flavor)) {
                errors.add("Not supported flavor: " + flavor);
                continue;
            }
            try {
                DocPrintJob job = service.createPrintJob();
                Doc doc;
                if (DocFlavor.INPUT_STREAM.AUTOSENSE.equals(flavor)) {
                    doc = new SimpleDoc(new ByteArrayInputStream(payload), flavor, null);
                } else if (DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8.equals(flavor)) {
                    byte[] text = new String(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
                    doc = new SimpleDoc(text, flavor, null);
                } else {
                    doc = new SimpleDoc(payload, flavor, null);
                }
                job.print(doc, attrs);
                return;
            } catch (PrintException ex) {
                errors.add("Flavor " + flavor + " failed: " + ex.getMessage());
            }
        }

        throw new RuntimeException(
                "Failed to print on '" + service.getName() + "'. Attempts: " + String.join(" | ", errors)
                        + ". Supported flavors: " + Arrays.toString(service.getSupportedDocFlavors()));
    }
}
