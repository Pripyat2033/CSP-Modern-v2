package com.ben.csp.plan;

import java.util.Objects;

/**
 * The штамп — the title block of a drawing: what the sheet is, at what scale,
 * which sheet of how many, who drew and approved it and when. This is where the
 * legible text of a plan lives, rather than scattered across the drawing.
 */
public final class TitleBlock {

    private final String objectName;
    private final String sheetTitle;
    private final Scale scale;
    private final String sheetNumber;
    private final int sheetCount;
    private final String drawnBy;
    private final String approvedBy;
    private final long day;

    public TitleBlock(String objectName, String sheetTitle, Scale scale, String sheetNumber,
                      int sheetCount, String drawnBy, String approvedBy, long day) {
        this.objectName = Objects.requireNonNull(objectName, "objectName");
        this.sheetTitle = Objects.requireNonNull(sheetTitle, "sheetTitle");
        this.scale = Objects.requireNonNull(scale, "scale");
        this.sheetNumber = Objects.requireNonNull(sheetNumber, "sheetNumber");
        if (sheetCount < 1) {
            throw new IllegalArgumentException("sheetCount must be at least 1");
        }
        this.sheetCount = sheetCount;
        this.drawnBy = drawnBy;
        this.approvedBy = approvedBy;
        this.day = day;
    }

    public String objectName() { return objectName; }
    public String sheetTitle() { return sheetTitle; }
    public Scale scale() { return scale; }
    public String sheetNumber() { return sheetNumber; }
    public int sheetCount() { return sheetCount; }
    public String drawnBy() { return drawnBy; }
    public String approvedBy() { return approvedBy; }
    public long day() { return day; }

    /** e.g. "Лист 3 из 5, 1:500". */
    public String sheetLine() {
        return "Лист " + sheetNumber + " из " + sheetCount + ", " + scale.label();
    }
}
