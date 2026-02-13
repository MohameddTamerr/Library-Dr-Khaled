package com.library.pos.util.escpos;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public final class EscPosCommands {

    private EscPosCommands() {
    }

    public static void init(ByteArrayOutputStream out) {
        out.write(0x1B);
        out.write('@');
    }

    private static void align(ByteArrayOutputStream out, int alignment) {
        out.write(0x1B);
        out.write('a');
        out.write(alignment);
    }

    public static void alignLeft(ByteArrayOutputStream out) {
        align(out, 0);
    }

    public static void alignCenter(ByteArrayOutputStream out) {
        align(out, 1);
    }

    public static void alignRight(ByteArrayOutputStream out) {
        align(out, 2);
    }

    public static void bold(ByteArrayOutputStream out, boolean enabled) {
        out.write(0x1B);
        out.write('E');
        out.write(enabled ? 1 : 0);
    }

    public static void doubleWidth(ByteArrayOutputStream out, boolean enabled) {
        out.write(0x1D);
        out.write('!');
        out.write(enabled ? 0x10 : 0x00);
    }

    public static void feed(ByteArrayOutputStream out, int lines) {
        out.write(0x1B);
        out.write('d');
        out.write(Math.max(0, Math.min(255, lines)));
    }

    public static void cut(ByteArrayOutputStream out) {
        out.write(0x1D);
        out.write('V');
        out.write(0x00);
    }

    public static void drawerKick(ByteArrayOutputStream out) {
        out.write(0x1B);
        out.write('p');
        out.write(0x00);
        out.write(0x3C);
        out.write(0x78);
    }

    public static void selectCodeTable(ByteArrayOutputStream out, int n) {
        out.write(0x1B);
        out.write('t');
        out.write(n & 0xFF);
    }

    public static void textLine(ByteArrayOutputStream out, String text, Charset charset) {
        byte[] bytes = (text == null ? "" : text).getBytes(charset);
        out.writeBytes(bytes);
        out.write('\n');
    }
}
