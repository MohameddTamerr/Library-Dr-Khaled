package com.library.pos.util.escpos;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ReceiptConfig {

    private final int paperWidthChars;
    private final String currency;
    private final Locale locale;
    private final Charset charset;
    private final boolean enableCut;
    private final boolean enableDrawerKick;
    private final boolean arabicPreferred;
    private final Integer codeTable;
    private final int feedLinesBeforeCut;

    private ReceiptConfig(Builder builder) {
        this.paperWidthChars = builder.paperWidthChars;
        this.currency = builder.currency;
        this.locale = builder.locale;
        this.charset = builder.charset;
        this.enableCut = builder.enableCut;
        this.enableDrawerKick = builder.enableDrawerKick;
        this.arabicPreferred = builder.arabicPreferred;
        this.codeTable = builder.codeTable;
        this.feedLinesBeforeCut = builder.feedLinesBeforeCut;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int paperWidthChars() {
        return paperWidthChars;
    }

    public String currency() {
        return currency;
    }

    public Locale locale() {
        return locale;
    }

    public Charset charset() {
        return charset;
    }

    public boolean enableCut() {
        return enableCut;
    }

    public boolean enableDrawerKick() {
        return enableDrawerKick;
    }

    public boolean arabicPreferred() {
        return arabicPreferred;
    }

    public Integer codeTable() {
        return codeTable;
    }

    public int feedLinesBeforeCut() {
        return feedLinesBeforeCut;
    }

    public static final class Builder {
        private int paperWidthChars = 32;
        private String currency = "ج.م";
        private Locale locale = Locale.forLanguageTag("ar-EG");
        private Charset charset = StandardCharsets.UTF_8;
        private boolean enableCut = true;
        private boolean enableDrawerKick = false;
        private boolean arabicPreferred = true;
        private Integer codeTable = null;
        private int feedLinesBeforeCut = 3;

        public Builder paperWidthChars(int paperWidthChars) {
            if (paperWidthChars < 24) {
                throw new IllegalArgumentException("paperWidthChars must be >= 24");
            }
            this.paperWidthChars = paperWidthChars;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency == null ? "" : currency;
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = locale == null ? Locale.ROOT : locale;
            return this;
        }

        public Builder charset(Charset charset) {
            this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
            return this;
        }

        public Builder enableCut(boolean enableCut) {
            this.enableCut = enableCut;
            return this;
        }

        public Builder enableDrawerKick(boolean enableDrawerKick) {
            this.enableDrawerKick = enableDrawerKick;
            return this;
        }

        public Builder arabicPreferred(boolean arabicPreferred) {
            this.arabicPreferred = arabicPreferred;
            return this;
        }

        public Builder codeTable(Integer codeTable) {
            this.codeTable = codeTable;
            return this;
        }

        public Builder feedLinesBeforeCut(int feedLinesBeforeCut) {
            this.feedLinesBeforeCut = Math.max(0, feedLinesBeforeCut);
            return this;
        }

        public ReceiptConfig build() {
            return new ReceiptConfig(this);
        }
    }
}
