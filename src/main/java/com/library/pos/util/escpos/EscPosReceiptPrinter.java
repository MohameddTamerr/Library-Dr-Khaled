package com.library.pos.util.escpos;

import com.library.pos.util.escpos.ReceiptModels.Order;

import java.io.ByteArrayOutputStream;
import java.util.List;

public final class EscPosReceiptPrinter {

    private final UsbPrinterService usbPrinterService;

    public EscPosReceiptPrinter() {
        this(new UsbPrinterService());
    }

    public EscPosReceiptPrinter(UsbPrinterService usbPrinterService) {
        this.usbPrinterService = usbPrinterService;
    }

    public void printOrderReceipt(Order order, ReceiptConfig config, String printerName) {
        if (order == null) {
            throw new IllegalArgumentException("order is required");
        }
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }

        ReceiptFormatter formatter = new ReceiptFormatter(config);
        List<String> lines = formatter.format(order, config.arabicPreferred());

        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        EscPosCommands.init(out);
        EscPosCommands.alignLeft(out);
        if (config.codeTable() != null) {
            EscPosCommands.selectCodeTable(out, config.codeTable());
        }

        for (String line : lines) {
            EscPosCommands.textLine(out, line, config.charset());
        }

        EscPosCommands.feed(out, config.feedLinesBeforeCut());
        if (config.enableDrawerKick()) {
            EscPosCommands.drawerKick(out);
        }
        if (config.enableCut()) {
            EscPosCommands.cut(out);
        }

        usbPrinterService.printRaw(printerName, out.toByteArray());
    }
}
