package com.ben.csp.block.entity;


/**
 * Teletype Printer Block - Soviet ICMS Output Device.
 */
public class TeletypePrinterBlock {

    private static String register(String name) {
        // Registration removed: this previously called Registry.register(..., null),
        // which throws at runtime. These are identity strings only - no block is
        // registered here. Actual blocks are registered in ModBlocks.
        return "csp-modern:" + name;
    }

    public static final String TELETYPE_PRINTER = register("teletype_printer");

    public enum PrinterState { IDLE, READY, RECEIVING, PRINTING }

    public static void init() {
        // Initialization completed at static block level
    }
}
