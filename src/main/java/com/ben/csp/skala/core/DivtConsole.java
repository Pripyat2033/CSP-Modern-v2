package com.ben.csp.skala.core;

/**
 * The DIVT console — the workplace of the chief computer engineer (ДИВТ), in the
 * SKALA computer room. This is the machine's engineering station, as distinct
 * from the operator request devices in the control room.
 *
 * <p>From the Chornobyl Family restoration, the DIVT panel is used to:
 * <ul>
 *   <li>prepare fuel reloading and enter reactor-operation calculations;</li>
 *   <li>enter reference (set-point) values;</li>
 *   <li>manage software and system integrity;</li>
 *   <li>retrieve data through its own request device.</li>
 * </ul>
 *
 * <p>Attached to it are the two punch-tape devices: an <b>FS-1501</b>
 * photoelectric reader (Czechoslovak, ~1500 symbols/s) and a <b>PL-150M</b> tape
 * punch. The tape carries the boot loader and the corrections to programs — the
 * punch is how a programmer wrote a change, and the reader is how the machine
 * took it in.
 *
 * <p>This class models the console's state and its tape operations. It does not
 * invent an interface beyond what the source describes.
 */
public final class DivtConsole {

    /** The two punch-tape devices attached to the console. */
    public enum PunchDevice {
        READER_FS1501("FS-1501", "photoelectric punch-tape reader (~1500 symbols/s)"),
        PUNCH_PL150M("PL-150M", "punch-tape machine (writes corrections)");

        public final String model;
        public final String description;

        PunchDevice(String model, String description) {
            this.model = model; this.description = description;
        }
    }

    private final Teletype controlTeletype;   // the console's own request output
    private String loadedProgram = "";
    private boolean tapeMounted;
    private String mountedTape = "";

    public DivtConsole(Teletype controlTeletype) {
        this.controlTeletype = controlTeletype;
    }

    /**
     * Boot the software from the library punch/magnetic tape. In reality this
     * takes about an hour to generate the executable tape; here it records the
     * action and the programs it brings up.
     */
    public String loadLibrary() {
        tapeMounted = true;
        mountedTape = "LIBRARY";
        controlTeletype.print(Teletype.dated(0, "DIVT: library tape mounted; booting"));
        for (SkalaProgram p : SkalaProgram.values()) {
            controlTeletype.print(Teletype.dated(0, "DIVT: program " + p.ru + " (" + p.latin + ") loaded"));
        }
        return "library loaded: " + SkalaProgram.COUNT + " programs";
    }

    /** Enter a reference (set-point) value for a code — a DIVT function. */
    public String enterReference(String code, double value) {
        controlTeletype.print(Teletype.dated(0,
            String.format("DIVT: reference %s := %.3f", code, value)));
        return code + " reference set to " + value;
    }

    /** Mount a program tape on the named device (reader or punch). */
    public String mount(PunchDevice device, String tape) {
        tapeMounted = true;
        mountedTape = tape;
        controlTeletype.print(Teletype.dated(0,
            "DIVT: " + device.model + " — tape " + tape + " mounted"));
        return device.model + " mounted " + tape;
    }

    public boolean tapeMounted() { return tapeMounted; }
    public String mountedTape() { return mountedTape; }
    public String loadedProgram() { return loadedProgram; }

    public Teletype teletype() { return controlTeletype; }
}
