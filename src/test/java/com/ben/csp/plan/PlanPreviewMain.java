package com.ben.csp.plan;

import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.PrombazaRegistry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

/** Renders a sample prombaza plan to a PNG so it can be looked at. */
public final class PlanPreviewMain {
    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "/tmp/plan_preview.png";

        PrombazaRegistry reg = new PrombazaRegistry();
        reg.designate("pb-1", "Central Prombaza", Area.of(100, 60, 100, 220, 80, 180), 1, "Ben");
        reg.designate("pb-2", "North Yard", Area.of(260, 60, 90, 340, 80, 150), 2, "Ben");
        PlanDocument doc = new PrombazaPlanSource(reg, "Prombaza plan").buildPlan().orElseThrow();

        int w = 512, h = 384;
        RasterPlanRenderer r = new RasterPlanRenderer(w, h);
        PlanView view = PlanView.fit(90, 80, 350, 200, w, h, 20);
        r.begin(view);
        r.drawAll(doc.visiblePrimitives());
        r.end();

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, r.pixels(), 0, w);
        // paper background under the lines
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (r.pixelAt(x, y) == 0) {
                    img.setRGB(x, y, 0xFFF4EFE2);
                }
            }
        }
        ImageIO.write(img, "png", new File(out));
        System.out.println("wrote " + out + " (" + w + "x" + h + ")");
    }
}
