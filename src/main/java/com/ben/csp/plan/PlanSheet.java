package com.ben.csp.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One drawing: a plan at a fixed standard scale, with its own title block and
 * its own view of the territory. Sheets at different scales are different
 * drawings of different detail, not magnifications of one image.
 */
public final class PlanSheet {

    private final String sheetNumber;
    private final String title;
    private final Scale scale;
    private final PlanView view;
    private final TitleBlock titleBlock;
    private final List<ProblemMarker> markers = new ArrayList<>();

    public PlanSheet(String sheetNumber, String title, Scale scale, PlanView view,
                     TitleBlock titleBlock) {
        this.sheetNumber = Objects.requireNonNull(sheetNumber, "sheetNumber");
        this.title = Objects.requireNonNull(title, "title");
        this.scale = Objects.requireNonNull(scale, "scale");
        this.view = Objects.requireNonNull(view, "view");
        this.titleBlock = Objects.requireNonNull(titleBlock, "titleBlock");
    }

    public String sheetNumber() { return sheetNumber; }
    public String title() { return title; }
    public Scale scale() { return scale; }
    public PlanView view() { return view; }
    public TitleBlock titleBlock() { return titleBlock; }

    public PlanSheet mark(ProblemMarker marker) {
        markers.add(Objects.requireNonNull(marker, "marker"));
        return this;
    }

    public List<ProblemMarker> markers() {
        return Collections.unmodifiableList(markers);
    }

    /** The sheet number a callout at a point leads to, if there is one. */
    public java.util.Optional<String> calloutAt(double x, double z, double tolerance) {
        for (ProblemMarker m : markers) {
            if (Math.hypot(m.x() - x, m.z() - z) <= tolerance && m.hasCallout()) {
                return java.util.Optional.of(m.detailSheetNumber());
            }
        }
        return java.util.Optional.empty();
    }
}
