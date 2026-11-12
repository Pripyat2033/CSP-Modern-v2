package com.ben.csp.rbmk.tools;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.physics.ReactorCore;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A live, steerable Blok you can operate from a terminal — no Minecraft.
 *
 * <p>This exists to answer one question with something you can actually run: can
 * the reactor be simulated live, at a fine timestep, with the operator free to
 * deviate? It uses the real physics ({@link ReactorCore}: point kinetics +
 * reactivity feedback + void + xenon), advanced at a fixed 1 ms step, and lets
 * you drive the rods, set the regulator, and scram — then watch the core respond.
 *
 * <p>It is the bench, not the game. Run:
 * <pre>
 *   gradle build
 *   java -cp build/classes/java/main com.ben.csp.rbmk.tools.ReactorConsole demo
 * </pre>
 * Commands: rod, auto, manual, scram, run, status, help, quit. Start with "help".
 */
public final class ReactorConsole {

    private static final double DT = 0.001;          // fixed 1 ms physics step
    private static final double REPORT_EVERY = 1.0;  // print a line every sim second

    private final ReactorCore core = new ReactorCore();
    private double simTime = 0;

    private ReactorConsole() {}

    public static void main(String[] args) throws Exception {
        ReactorConsole c = new ReactorConsole();
        c.banner();
        if (args.length > 0 && args[0].equals("demo")) {
            c.demo();
            return;
        }
        c.repl();
    }

    private void banner() {
        System.out.println("================================================================");
        System.out.println("  RBMK-1000  ·  live reactor bench  ·  fixed step 1 ms");
        System.out.println("  real lattice: " + RbmkGeometry.TOTAL_CHANNELS + " channels, "
            + RbmkGeometry.CORE_HEIGHT_M + " m active height");
        System.out.println("================================================================");
        System.out.println("  type 'help' for commands; 'status' shows the core now.");
        System.out.println();
    }

    private void repl() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("blok> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null) {
                break;
            }
            String[] t = line.trim().split("\\s+");
            if (t.length == 0 || t[0].isEmpty()) {
                continue;
            }
            switch (t[0]) {
                case "help" -> help();
                case "status" -> status();
                case "rod" -> rod(t);
                case "auto" -> auto(t);
                case "manual" -> { core.setAutoRegulator(false); System.out.println("  regulator: MANUAL"); }
                case "scram", "az5" -> scram();
                case "run" -> run(t);
                case "quit", "exit" -> { return; }
                default -> System.out.println("  ? unknown command; try 'help'");
            }
        }
    }

    private void help() {
        System.out.println("""
              rod <0..1>   set the rod bank target (0 = fully out, 1 = fully in)
              auto         automatic regulator on, holding the current power
              manual       regulator off — you drive the rods directly
              scram        AZ-5: drive all rods fully in
              run <sec>    advance the core that many simulated seconds
              status       print the current core state
              quit         leave
            """);
    }

    private void rod(String[] t) {
        if (t.length < 2) {
            System.out.println("  usage: rod <0..1>");
            return;
        }
        double v = clamp01(Double.parseDouble(t[1]));
        core.setAutoRegulator(false);
        core.setRodTarget(v);
        System.out.printf("  rods -> %.2f (manual)%n", v);
    }

    private void auto(String[] t) {
        core.setAutoRegulator(true);
        System.out.printf("  regulator: AUTO, holding at current power (~%.2f)%n", core.power());
    }

    private void scram() {
        core.setAutoRegulator(false);
        core.setRodTarget(1.0);
        System.out.println("  AZ-5: driving all rods fully in");
    }

    private void run(String[] t) {
        double secs = t.length < 2 ? 5.0 : Double.parseDouble(t[1]);
        if (secs <= 0 || secs > 3600) {
            System.out.println("  usage: run <seconds, 0..3600>");
            return;
        }
        int steps = (int) Math.round(secs / DT);
        double nextReport = simTime + REPORT_EVERY;
        System.out.println("  time(s)   power    rods   void   fuel(C)  xenon");
        for (int i = 0; i < steps; i++) {
            core.step(DT);
            simTime += DT;
            if (simTime + 1e-9 >= nextReport) {
                report();
                nextReport += REPORT_EVERY;
            }
        }
        report();
    }

    private void report() {
        System.out.printf("  %7.1f  %6.3f  %5.2f  %5.3f  %7.1f  %5.3f%n",
            simTime, core.power(), core.insertion(), core.voidFraction(),
            core.fuelTempC(), core.xenonRatio());
    }

    private void status() {
        System.out.printf("  t=%.1fs  power=%.3f  rods=%.2f  void=%.3f  fuel=%.1fC  xenon=%.3f  rho=%.5f%n",
            simTime, core.power(), core.insertion(), core.voidFraction(),
            core.fuelTempC(), core.xenonRatio(), core.reactivity());
    }

    /** A scripted run that shows the core holding, then being pushed, then scrammed. */
    private void demo() {
        System.out.println("  DEMO: hold at power, pull the rods, watch the void push back, scram.");
        System.out.println();
        core.setAutoRegulator(true);
        System.out.println("  [1] regulator ON — holding nominal power");
        run(new String[]{"run", "20"});

        System.out.println();
        System.out.println("  [2] regulator OFF, rods pulled out to 0.60 — power rises");
        core.setAutoRegulator(false);
        core.setRodTarget(0.60);
        run(new String[]{"run", "30"});

        System.out.println();
        System.out.println("  [3] rods to 0.40 — deeper excursion; void feedback bites");
        core.setRodTarget(0.40);
        run(new String[]{"run", "30"});

        System.out.println();
        System.out.println("  [4] AZ-5 scram — rods fully in");
        scram();
        run(new String[]{"run", "20"});
        System.out.println();
        System.out.println("  done. That is a live core, steered by hand, at a 1 ms step.");
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
