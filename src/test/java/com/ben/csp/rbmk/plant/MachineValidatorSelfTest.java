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

        // --- turbine: a box shell ---
        MachineSpec tur = new MachineSpec("t", "tur", null, 156, 40, 48,
            MachineSpec.Shape.BOX_SHELL);
        check(tur.isShell(0, 0, 0), "box corner is shell");
        check(tur.isShell(0, 20, 24), "box face is shell");
        check(!tur.isShell(78, 20, 24), "box interior is not shell");
        int[] tc = tur.controllerOffset();
        check(tur.isShell(tc[0], tc[1], tc[2]), "turbine controller sits on the shell");

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
        check(tur.sizeX == 156, "turbogenerator is 156 blocks long (39 m at 1:4)");

        System.out.println("MachineValidatorSelfTest: " + checks + " checks passed.");
    }
}
