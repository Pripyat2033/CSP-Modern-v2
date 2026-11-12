package com.ben.csp.rbmk.tools;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.physics.RbmkTransientSolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A live, steerable Blok you can operate from a terminal — no Minecraft.
 *
 * <p>This drives the <b>time-dependent two-group spatial solver</b>
 * ({@link RbmkTransientSolver}): the neutron flux over all 1,872 channels is the
 * state, with delayed neutrons per node. There is NO point kinetics and no lumped
 * reactivity — the power is the mean fission over the flux field, so moving a rod
 * changes the flux where the geometry says it should.
 *
 * <p>Run:
 * <pre>
 *   gradle build
 *   java -cp build/classes/java/main com.ben.csp.rbmk.tools.ReactorConsole demo
 * </pre>
 * Commands: rod, scram, run, status, reset, help, quit.
 *
 * <p>NOT YET IN: the thermal-hydraulic feedback (void, fuel/graphite temperature)
 * is not closed back into the cells here, so this bench shows the neutronics of a
 * rod motion, not yet the full accident feedback loop. That loop is the next piece.
 */
public final class ReactorConsole {

    private static final double DT = 0.001;     // fixed 1 ms step
    private static final int SWEEPS = 2;        // Gauss-Seidel passes per step
    private static final double REPORT_EVERY = 1.0;

    private RbmkTransientSolver core = new RbmkTransientSolver();
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
        System.out.println("  RBMK-1000  ·  spatial transient bench  ·  flux is the state");
        System.out.println("  time-dependent 2-group diffusion over "
            + RbmkGeometry.TOTAL_CHANNELS + " channels x 28 nodes, 1 ms step");
        System.out.println("================================================================");
        System.out.println("  type 'help'; 'status' shows the core now.");
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
                case "scram", "az5" -> scram();
                case "run" -> run(t);
                case "reset" -> { core = new RbmkTransientSolver(); simTime = 0; System.out.println("  core reset to critical"); }
                case "quit", "exit" -> { return; }
                default -> System.out.println("  ? unknown command; try 'help'");
            }
        }
    }

    private void help() {
        System.out.println("""
              rod <0..1>   set the rod bank (0 = fully out, 1 = fully in)
              scram        AZ-5: drive all rods fully in
              run <sec>    advance the core that many simulated seconds
              status       print the current core state
              reset        return to the critical state
              quit         leave
            """);
    }

    private void rod(String[] t) {
        if (t.length < 2) {
            System.out.println("  usage: rod <0..1>");
            return;
        }
        double v = clamp01(Double.parseDouble(t[1]));
        core.setRodInsertion(v);
        System.out.printf("  rods -> %.2f%n", v);
    }

    private void scram() {
        core.setRodInsertion(1.0);
        System.out.println("  AZ-5: all rods fully in");
    }

    private void run(String[] t) {
        double secs = t.length < 2 ? 5.0 : Double.parseDouble(t[1]);
        if (secs <= 0 || secs > 3600) {
            System.out.println("  usage: run <seconds, 0..3600>");
            return;
        }
        int steps = (int) Math.round(secs / DT);
        double nextReport = simTime + REPORT_EVERY;
        System.out.println("  time(s)   power     peak    step(ms)");
        for (int i = 0; i < steps; i++) {
            core.step(DT, SWEEPS);
            simTime += DT;
            if (simTime + 1e-9 >= nextReport) {
                report();
                nextReport += REPORT_EVERY;
            }
        }
        report();
    }

    private void report() {
        System.out.printf("  %7.1f  %8.4f  %8.4f  %7.2f%n",
            simTime, core.power(), core.peakFission(), core.lastStepMs());
    }

    private void status() {
        System.out.printf("  t=%.1fs  power=%.4f  peak=%.4f  step=%.2f ms%n",
            simTime, core.power(), core.peakFission(), core.lastStepMs());
    }

    private void demo() {
        System.out.println("  DEMO: critical -> pull rods -> excursion -> AZ-5.");
        System.out.println();
        System.out.println("  [1] hold at the critical state (rods 0.80)");
        run(new String[]{"run", "10"});

        System.out.println();
        System.out.println("  [2] rods to 0.70 — the flux grows where the rods leave");
        core.setRodInsertion(0.70);
        run(new String[]{"run", "20"});

        System.out.println();
        System.out.println("  [3] AZ-5 — all rods in");
        scram();
        run(new String[]{"run", "10"});
        System.out.println();
        System.out.println("  done: the power came from the flux field, not a fitted curve.");
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
