package com.ben.csp.radiology;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REKORD layer 4: turns a release into a dose to a person.
 *
 * <p>Every factor here is quoted from IAEA-TECDOC-1162, the emergency assessor's
 * handbook. Nothing is modelled from first principles, because the dose
 * coefficients <em>are</em> the model: they are the output of decades of ICRP
 * dosimetry, and re-deriving them in a Minecraft mod would be worse than
 * quoting them.
 *
 * <p>The three pathways the TECDOC defines, and the equation it gives for each:
 *
 * <ul>
 *   <li><b>Ground shine.</b> {@code E = C_ground * CF4}, with the shielding
 *       factor and occupancy fraction applied afterwards (Procedure E3 step 3).
 *       This is the dominant long-term pathway after Chernobyl.</li>
 *   <li><b>Inhalation.</b> {@code E = C_air * CF2 * T}, where {@code C_air} is
 *       the air concentration and {@code T} the hours spent in the plume
 *       (Procedure E5 step 2).</li>
 *   <li><b>Ingestion.</b> {@code E = A_intake * CF5} (Procedure E6).</li>
 * </ul>
 *
 * <p>Air concentration itself comes from Procedure E5a:
 * {@code C_air = Q * DF / u}, with {@code Q} the release rate, {@code DF} the
 * dilution factor for the stability class and distance, and {@code u} the wind
 * speed.
 *
 * <p><b>What a number from this class means.</b> Every inhalation and thyroid
 * factor is the <em>conservative</em> value — the TECDOC's own choice of the
 * highest value across body types and chemical forms. A dose computed here is
 * therefore an upper bound, not a best estimate, and {@link DoseResult} says so
 * in its own fields rather than leaving the caller to remember.
 */
public final class DoseCalculator {

    public static final String RESOURCE = "/data/csp/radiology/dose-data.json";

    /** TECDOC Table E4 default: fraction of time spent where shielding applies. */
    public static final double DEFAULT_OCCUPANCY_FRACTION = 0.6;

    /** TECDOC Table E4: shielding factor for a typical urban building. */
    public static final double DEFAULT_SHIELDING_FACTOR = 0.3;

    private static final List<String> PROBLEMS = new ArrayList<>();
    private static boolean loaded;

    private static Map<String, NuclideDose> nuclides = new LinkedHashMap<>();
    private static double[] distancesKm = new double[0];
    private static Map<String, double[]> dilution = new LinkedHashMap<>();

    private DoseCalculator() {}

    public static List<String> problems() {
        load();
        return Collections.unmodifiableList(PROBLEMS);
    }

    /** Dose factors for one nuclide, or null if the nuclide is unknown. */
    public static NuclideDose factors(String symbol) {
        load();
        return nuclides.get(symbol);
    }

    public static Map<String, NuclideDose> allFactors() {
        load();
        return Collections.unmodifiableMap(nuclides);
    }

    // ---------------------------------------------------------------- pathways

    /**
     * Ground-shine dose over a period, from a deposition.
     *
     * @param symbol        nuclide
     * @param depositionBqPerM2 activity on the ground per square metre
     * @param period        which integration period to use
     * @param shielding     shielding factor (1.0 = outdoors, unshielded)
     * @param occupancy     fraction of time the shielding applies
     * @return effective dose in mSv, or NaN if the nuclide has no ground factor
     */
    public static double groundDose(String symbol, double depositionBqPerM2,
                                    GroundPeriod period, double shielding,
                                    double occupancy) {
        NuclideDose n = factors(symbol);
        if (n == null || Double.isNaN(n.groundPerDeposition(period))) {
            return Double.NaN;
        }
        double depositionKbqPerM2 = depositionBqPerM2 / 1000.0;
        double unshielded = depositionKbqPerM2 * n.groundPerDeposition(period);
        // TECDOC Procedure E3 step 3: E_po = E * (SF*OF + (1-OF)). The rest of
        // the time, 1-OF, the person is outdoors and receives the full dose.
        return unshielded * (shielding * occupancy + (1.0 - occupancy));
    }

    /**
     * Committed effective dose from inhalation during a plume passage.
     *
     * @param symbol           nuclide
     * @param airConcentrationBqPerM3 activity concentration in air
     * @param exposureHours    hours spent breathing that air
     * @return committed effective dose in mSv, or NaN if the pathway gives none
     */
    public static double inhalationDose(String symbol, double airConcentrationBqPerM3,
                                        double exposureHours) {
        NuclideDose n = factors(symbol);
        if (n == null || Double.isNaN(n.inhalationPerHour())) {
            return Double.NaN;
        }
        double kbqPerM3 = airConcentrationBqPerM3 / 1000.0;
        return kbqPerM3 * n.inhalationPerHour() * exposureHours;
    }

    /**
     * Committed equivalent dose to the thyroid from inhalation.
     *
     * <p>Separate from the effective dose because that separation is the whole
     * radiological story of Chernobyl: iodine concentrates in the gland, so a
     * child's thyroid dose can be an order of magnitude above the whole-body
     * dose from the same intake.
     *
     * @param child true to use the 10-year-old factor
     */
    public static double thyroidDose(String symbol, double airConcentrationBqPerM3,
                                     double exposureHours, boolean child) {
        NuclideDose n = factors(symbol);
        if (n == null || Double.isNaN(n.thyroidPerHour(child))) {
            return Double.NaN;
        }
        double kbqPerM3 = airConcentrationBqPerM3 / 1000.0;
        return kbqPerM3 * n.thyroidPerHour(child) * exposureHours;
    }

    /**
     * Committed effective dose from ingesting an activity.
     *
     * @param intakeBq activity ingested
     * @return committed effective dose in mSv, or NaN if the pathway gives none
     */
    public static double ingestionDose(String symbol, double intakeBq) {
        NuclideDose n = factors(symbol);
        if (n == null || Double.isNaN(n.ingestionPerIntake())) {
            return Double.NaN;
        }
        return (intakeBq / 1000.0) * n.ingestionPerIntake();
    }

    // --------------------------------------------------------------- transport

    /**
     * Ground-level air concentration downwind of a release, Procedure E5a.
     *
     * @param releaseRateBqPerS source term release rate
     * @param distanceKm        downwind distance
     * @param stabilityClass    Pasquill class, "A" to "F"
     * @param windSpeedMPerS    mean wind speed
     * @return concentration in Bq/m3, or NaN outside the tabulated range
     */
    public static double airConcentration(double releaseRateBqPerS, double distanceKm,
                                          String stabilityClass, double windSpeedMPerS) {
        double df = dilutionFactor(distanceKm, stabilityClass);
        if (Double.isNaN(df) || windSpeedMPerS <= 0.0) {
            return Double.NaN;
        }
        double releaseKbqPerS = releaseRateBqPerS / 1000.0;
        // C [kBq/m3] = Q [kBq/s] * DF [1/m2] / u [m/s]
        return releaseKbqPerS * df / windSpeedMPerS * 1000.0;
    }

    /**
     * Dilution factor for a distance and stability class.
     *
     * <p>Interpolates linearly between the tabulated distances, and returns NaN
     * outside them rather than extrapolating: the TECDOC's factors are fitted
     * over 0.5-30 km and extending them would be inventing data.
     */
    public static double dilutionFactor(double distanceKm, String stabilityClass) {
        load();
        double[] column = dilution.get(stabilityClass);
        if (column == null || distancesKm.length == 0) {
            return Double.NaN;
        }
        if (distanceKm < distancesKm[0] || distanceKm > distancesKm[distancesKm.length - 1]) {
            return Double.NaN;
        }
        for (int i = 0; i < distancesKm.length - 1; i++) {
            if (distanceKm >= distancesKm[i] && distanceKm <= distancesKm[i + 1]) {
                double t = (distanceKm - distancesKm[i])
                    / (distancesKm[i + 1] - distancesKm[i]);
                return column[i] + t * (column[i + 1] - column[i]);
            }
        }
        return column[column.length - 1];
    }

    /** Integration period for a ground-shine dose. */
    public enum GroundPeriod {
        FIRST_MONTH, SECOND_MONTH, FIFTY_YEAR
    }

    /** Dose factors for one nuclide. NaN means the source gives no factor. */
    public record NuclideDose(
        String symbol,
        double groundRatePerDeposition,
        double groundFirstMonth,
        double groundSecondMonth,
        double groundFiftyYear,
        double inhalationPerHour,
        double thyroidAdultPerHour,
        double thyroidChildPerHour,
        double ingestionPerIntake
    ) {
        public double groundPerDeposition(GroundPeriod period) {
            return switch (period) {
                case FIRST_MONTH -> groundFirstMonth;
                case SECOND_MONTH -> groundSecondMonth;
                case FIFTY_YEAR -> groundFiftyYear;
            };
        }

        public double thyroidPerHour(boolean child) {
            return child ? thyroidChildPerHour : thyroidAdultPerHour;
        }
    }

    // ------------------------------------------------------------------ loader

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try (InputStream in = DoseCalculator.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                PROBLEMS.add("resource not found: " + RESOURCE);
                return;
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

            JsonObject disp = root.getAsJsonObject("_dispersion");
            var distArr = disp.getAsJsonArray("distancesKm");
            distancesKm = new double[distArr.size()];
            for (int i = 0; i < distArr.size(); i++) {
                distancesKm[i] = distArr.get(i).getAsDouble();
            }
            JsonObject byClass = disp.getAsJsonObject("dilutionFactorByStability");
            for (Map.Entry<String, JsonElement> e : byClass.entrySet()) {
                var arr = e.getValue().getAsJsonArray();
                double[] col = new double[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    col[i] = arr.get(i).getAsDouble();
                }
                if (col.length != distancesKm.length) {
                    PROBLEMS.add("dilution column " + e.getKey() + " has "
                        + col.length + " entries, expected " + distancesKm.length);
                    continue;
                }
                dilution.put(e.getKey(), col);
            }

            JsonObject nucs = root.getAsJsonObject("nuclides");
            for (Map.Entry<String, JsonElement> e : nucs.entrySet()) {
                String symbol = e.getKey();
                JsonObject o = e.getValue().getAsJsonObject();
                try {
                    nuclides.put(symbol, new NuclideDose(
                        symbol,
                        opt(o, "ground", "doseRatePerDeposition"),
                        opt(o, "ground", "firstMonthPerDeposition"),
                        opt(o, "ground", "secondMonthPerDeposition"),
                        opt(o, "ground", "fiftyYearPerDeposition"),
                        opt(o, "inhalation", "perHourPerAirConcentration"),
                        opt(o, "thyroid", "adultPerHourPerAirConcentration"),
                        opt(o, "thyroid", "child10yPerHourPerAirConcentration"),
                        opt(o, "ingestion", "perIntake")));
                } catch (RuntimeException ex) {
                    PROBLEMS.add(symbol + ": " + ex);
                }
            }
        } catch (Exception e) {
            PROBLEMS.add("failed to read " + RESOURCE + ": " + e);
        }
    }

    /**
     * Reads an optional factor. A "NC" cell or a missing key both mean the
     * source gives no factor for this pathway, which is NaN, not zero: a zero
     * would be the claim that the pathway delivers no dose, which is a
     * different and stronger statement than "not credited".
     */
    private static double opt(JsonObject root, String block, String key) {
        JsonObject b = root.getAsJsonObject(block);
        if (b == null || !b.has(key)) {
            return Double.NaN;
        }
        JsonElement v = b.get(key);
        if (v.isJsonNull() || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) {
            return Double.NaN;
        }
        return v.getAsDouble();
    }
}
