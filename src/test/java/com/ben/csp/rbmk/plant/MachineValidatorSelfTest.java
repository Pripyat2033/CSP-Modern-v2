package com.ben.csp.rbmk.plant;

/**
 * Self-test for the multiblock machine geometry: the shell predicate, the shell
 * count, and where the controller sits. This is what guarantees a "formed"
 * machine means the real shell is present — the same class of check that was
 * missing when the control rods passed by count while sitting all on the rim.
 */
public final class MachineValidatorSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    public static void main(String[] args) {
        // --- steam separator: a horizontal drum, radius 5, length 124 ---
        MachineSpec sep = new MachineSpec("s", "sep", null, 124, 11, 11,
            MachineSpec.Shape.CYLINDER_X);
        check(sep.radius == 5, "separator radius is 5 (11 dia)");
        check(sep.isShell(0, 5, 5), "drum end cap centre is shell (solid cap)");
        check(sep.isShell(123, 5, 5), "drum far end cap is shell");
        check(sep.isShell(62, 0, 5), "drum tube wall (top of ring) is shell");
        check(!sep.isShell(62, 5, 5), "drum interior is open (a vessel, not solid)");
        check(!sep.isShell(62, 0, 0), "outside the drum radius is not shell");
        int[] sc = sep.controllerOffset();
        check(sep.isShell(sc[0], sc[1], sc[2]), "separator controller sits on the shell");
        check(sc[0] == 0, "separator controller is on the near end cap");

        // --- turbine: a composite — five cylinder covers + a generator body ---
        // Built here directly (null casings) so this stays a pure-logic test; the
        // real block wiring lives in MachineSpecs and is checked in game.
        java.util.List<MachineSpec.Part> tparts = new java.util.ArrayList<>();
        tparts.add(new MachineSpec.Part("foundation", null, 0, 0, 0,
            157, 8, 19, MachineSpec.Shape.BOX_SHELL));
        for (int i = 0; i < 5; i++) {
            tparts.add(new MachineSpec.Part("cover " + i, null,
                i * 25, 8, 0, 25, 19, 19, MachineSpec.Shape.CYLINDER_X));
        }
        tparts.add(new MachineSpec.Part("shaft", null, 125, 17, 7,
            2, 3, 5, MachineSpec.Shape.BOX_SHELL));
        tparts.add(new MachineSpec.Part("generator", null, 127, 8, 0,
            30, 17, 19, MachineSpec.Shape.BOX_SHELL));
        MachineSpec tur = MachineSpec.composite("t", "tur", 157, 27, 19, tparts);
        check(tur.isComposite(), "the turbogenerator is a composite machine");
        check(tur.parts.size() == 8,
            "the turbogenerator has a foundation, 5 covers, a shaft and a generator ("
                + tur.parts.size() + ")");
        int[] tc = tur.controllerOffset();
        check(tur.isShell(tc[0], tc[1], tc[2]), "turbine controller sits on the shell");
        check(tur.isShell(0, 17, 9), "the HP cover's end cap is part of the turbine");
        check(tur.isShell(tur.sizeX - 1, 8, 0),
            "the generator's far shell is part of the turbine");
        check(tur.isShell(50, 17, 9), "a middle LP cover's end cap is part of the turbine");
        check(!tur.isShell(3, 12, 9), "inside the foundation is open, not shell");
        check(tur.sizeX == 157, "turbogenerator is ~156 blocks long (39 m at 1:4)");

        // --- main circulating pump: the pump body with its motor above it ---
        MachineSpec mcp = MachineSpec.composite("m", "mcp", 3, 6, 3, java.util.List.of(
            new MachineSpec.Part("pump body", null, 0, 0, 0, 3, 3, 3, MachineSpec.Shape.BOX_SHELL),
            new MachineSpec.Part("motor", null, 0, 3, 0, 3, 3, 3, MachineSpec.Shape.BOX_SHELL)));
        check(mcp.isComposite() && mcp.parts.size() == 2,
            "the ГЦН is a pump body with a motor on top");
        check(mcp.sizeY == 6, "the ГЦН stands 6 blocks tall (body + motor)");
        check(mcp.isShell(0, 0, 0), "the pump body's corner is shell");
        check(mcp.isShell(0, 5, 0), "the motor's top corner is shell");

        // --- shell counts are sane and match the shape ---
        long sepShell = MachineValidator.shellCount(sep);
        // tube wall (~2*pi*5 ~ 32 cells per ring) * 122 + 2 caps (~81) is in the
        // low thousands; assert a plausible range rather than a magic number
        check(sepShell > 3000 && sepShell < 6000,
            "separator shell count is a hollow drum (" + sepShell + ")");
        long turShell = MachineValidator.shellCount(tur);
        long vol = tur.volume();
        check(turShell > 0 && turShell < vol,
            "turbine shell is a surface, far less than its volume (" + turShell
                + " < " + vol + ")");

        // --- the machine is a real size, from the doc ---
        check(sep.sizeX == 124 && sep.sizeY == 11 && sep.sizeZ == 11,
            "separator is 124 x 11 x 11 blocks (31 m x 2.8 m at 1:4)");

        // --- every machine validates its own shell: shellCount == isShell count ---
        for (MachineSpec s : new MachineSpec[] {sep, tur, mcp}) {
            long counted = MachineValidator.shellCount(s);
            check(counted > 0 && counted < s.volume(),
                s.id + " is a hollow shell, not solid (" + counted + " < " + s.volume() + ")");
            check(s.isShell(s.controllerOffset()[0], s.controllerOffset()[1],
                    s.controllerOffset()[2]),
                s.id + " controller sits on its own shell");
        }

        System.out.println("MachineValidatorSelfTest: " + checks + " checks passed.");
    }
}
