package com.ben.csp.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One drawing: the plan document shown over a fixed piece of the world, at a
 * fixed standard scale, with its title block and its problem markers. Sheets at
 * different scales are different drawings of different detail, not magnifications
 * of one image.
 */
public final class PlanSheet {

    private final String sheetNumber;
    private final String title;
    private final Scale scale;
    private final PlanExtent extent;
    private final PlanDocument document;
    private final TitleBlock titleBlock;
    private final List<ProblemMarker> markers = new ArrayList<>();

    public PlanSheet(String sheetNumber, String title, Scale scale, PlanExtent extent,
                     PlanDocument document, TitleBlock titleBlock) {
        this.sheetNumber = Objects.requireNonNull(sheetNumber, "sheetNumber");
        this.title = Objects.requireNonNull(title, "title");
        this.scale = Objects.requireNonNull(scale, "scale");
        this.extent = Objects.requireNonNull(extent, "extent");
        this.document = Objects.requireNonNull(document, "document");
        this.titleBlock = Objects.requireNonNull(titleBlock, "titleBlock");
    }

    public String sheetNumber() { return sheetNumber; }
    public String title() { return title; }
    public Scale scale() { return scale; }
    public PlanExtent extent() { return extent; }
    public PlanDocument document() { return document; }
    public TitleBlock titleBlock() { return titleBlock; }

    /** A view of this sheet fitted to a surface of the given pixel size. */
    public PlanView viewFor(int widthPx, int heightPx, double marginPx) {
        return PlanView.fit(extent.minX(), extent.minZ(), extent.maxX(), extent.maxZ(),
                            widthPx, heightPx, marginPx);
    }

    public PlanSheet mark(ProblemMarker marker) {
        markers.add(Objects.requireNonNull(marker, "marker"));
        return this;
    }

    public List<ProblemMarker> markers() {
        return Collections.unmodifiableList(markers);
    }

    /** The sheet number a callout at a point leads to, if there is one. */
    public Optional<String> calloutAt(double x, double z, double tolerance) {
        for (ProblemMarker m : markers) {
            if (Math.hypot(m.x() - x, m.z() - z) <= tolerance && m.hasCallout()) {
                return Optional.of(m.detailSheetNumber());
            }
        }
        return Optional.empty();
    }
}
