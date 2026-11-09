package com.ben.csp.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A set of sheets for one object, at several scales. There is one current sheet;
 * "zooming in" and "zooming out" step to the next more or less detailed standard
 * scale, and "following a callout" jumps to a named detail sheet.
 */
public final class SheetSet {

    private final String objectName;
    private final List<PlanSheet> sheets = new ArrayList<>();
    private int currentIndex;

    public SheetSet(String objectName) {
        this.objectName = Objects.requireNonNull(objectName, "objectName");
    }

    public String objectName() { return objectName; }

    public SheetSet add(PlanSheet sheet) {
        sheets.add(Objects.requireNonNull(sheet, "sheet"));
        return this;
    }

    public List<PlanSheet> sheets() {
        return Collections.unmodifiableList(sheets);
    }

    public PlanSheet current() {
        if (sheets.isEmpty()) {
            throw new IllegalStateException("the sheet set is empty");
        }
        return sheets.get(currentIndex);
    }

    public Optional<PlanSheet> byNumber(String number) {
        return sheets.stream().filter(s -> s.sheetNumber().equals(number)).findFirst();
    }

    /** The coarsest sheet (the widest view) is the natural starting point. */
    public SheetSet startCoarsest() {
        int best = -1;
        int bestDen = Integer.MIN_VALUE;
        for (int i = 0; i < sheets.size(); i++) {
            if (sheets.get(i).scale().denominator() > bestDen) {
                bestDen = sheets.get(i).scale().denominator();
                best = i;
            }
        }
        currentIndex = Math.max(0, best);
        return this;
    }

    public boolean canZoomIn() {
        return current().scale().canZoomIn();
    }

    public boolean canZoomOut() {
        return current().scale().canZoomOut();
    }

    /** Step to the next more detailed sheet that exists, skipping missing scales. */
    public boolean zoomIn() {
        Scale s = current().scale();
        while (s.canZoomIn()) {
            s = s.moreDetailed();
            PlanSheet next = atScale(s);
            if (next != null) {
                currentIndex = sheets.indexOf(next);
                return true;
            }
        }
        return false;
    }

    /** Step to the next coarser sheet that exists, skipping missing scales. */
    public boolean zoomOut() {
        Scale s = current().scale();
        while (s.canZoomOut()) {
            s = s.lessDetailed();
            PlanSheet next = atScale(s);
            if (next != null) {
                currentIndex = sheets.indexOf(next);
                return true;
            }
        }
        return false;
    }

    private PlanSheet atScale(Scale scale) {
        for (PlanSheet s : sheets) {
            if (s.scale() == scale) {
                return s;
            }
        }
        return null;
    }

    /** Follow a callout near a point to the detail sheet it names. */
    public Optional<PlanSheet> followCallout(double x, double z, double tolerance) {
        Optional<String> number = current().calloutAt(x, z, tolerance);
        if (number.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlanSheet> target = byNumber(number.get());
        target.ifPresent(s -> currentIndex = sheets.indexOf(s));
        return target;
    }
}
