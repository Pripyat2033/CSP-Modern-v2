package com.ben.csp.telephony;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The коммутатор: a manual exchange. The Director and the staff hold telephones
 * (vertushkas) on a shared battery; a call is set up the way a real operator set one
 * up, and the same limits apply:
 *
 * <ul>
 *   <li>Lifting the handset closes the loop: the line is <b>engaged</b> and its lamp
 *       lights at the board. Nothing else can use that line while it is off-hook.</li>
 *   <li>Dialling sends the wanted number. The dial has to be let go and come back
 *       before the digits are sent; a number is only routed when dialling stops.</li>
 *   <li>A free line <b>rings</b>. An engaged line is <b>busy</b> — the caller hears
 *       the engaged tone and cannot be connected.</li>
 *   <li>The called party answers: the two lines are joined and can talk. When either
 *       hangs up the connection is <b>released</b> and both lamps go out.</li>
 *   <li>A number that is not on the exchange is put through to the <b>operator</b>
 *       (line 0), who holds the caller and connects the wanted line by hand.</li>
 * </ul>
 *
 * <p>This class is pure logic — no Minecraft — so the sequence and the limits can be
 * tested exactly, and a real operator would recognise every step.
 */
public final class KommutatorExchange {

    public enum State { ON_HOOK, OFF_HOOK, DIALING, RINGING, TALKING, BUSY }

    public static final int OPERATOR = 0;
    /** A dialled number is routed this many ticks after the last digit (the dial return). */
    public static final int DIAL_RETURN_TICKS = 20;
    /** How long a line rings before the exchange gives up (the caller keeps trying). */
    public static final int RING_TIMEOUT_TICKS = 200;

    public static final class Line {
        public final int number;
        public final String label;
        public State state = State.ON_HOOK;
        public int peer = -1;
        public int heldFor = -1;   // operator: the caller being held
        public int wants = -1;     // operator: the number the held caller asked for
        public String digits = "";
        public int timer = 0;
        public boolean lamp = false;
        public boolean outgoing = false;   // true for the party who placed the call

        Line(int number, String label) {
            this.number = number;
            this.label = label;
        }
    }

    /** An event the exchange produced this tick, for the Minecraft layer to voice. */
    public record Event(Kind kind, int line, int other) {
        public enum Kind { RING, ANSWERED, RELEASED, BUSY, UNKNOWN_NUMBER, OPERATOR_CALL, GAVE_UP }
    }

    private final Map<Integer, Line> lines = new LinkedHashMap<>();
    private final List<Event> pending = new ArrayList<>();

    private void emit(Event.Kind kind, int line, int other) {
        pending.add(new Event(kind, line, other));
    }

    public void register(int number, String label) {
        lines.putIfAbsent(number, new Line(number, label));
    }

    public void unregister(int number) {
        Line l = lines.get(number);
        if (l != null && l.state != State.ON_HOOK) {
            release(l.number);
        }
        lines.remove(number);
    }

    public Line line(int number) {
        return lines.get(number);
    }

    public Map<Integer, Line> lines() {
        return lines;
    }

    public boolean exists(int number) {
        return lines.containsKey(number);
    }

    // ------------------------------------------------------------- subscriber side

    /** Lift the handset: the loop closes and the line is engaged. */
    public void offHook(int number) {
        Line l = lines.get(number);
        if (l == null || l.state != State.ON_HOOK) {
            return;
        }
        l.outgoing = false;
        l.state = State.OFF_HOOK;
        l.digits = "";
        l.timer = 0;
        l.lamp = true;
    }

    /** One dialled digit. Only while off-hook; digits accumulate until the dial returns. */
    public void dial(int number, char digit) {
        Line l = lines.get(number);
        if (l == null || (l.state != State.OFF_HOOK && l.state != State.DIALING)) {
            return;
        }
        if (l.state == State.OFF_HOOK) {
            l.state = State.DIALING;
        }
        l.digits += digit;
        l.timer = 0;
    }

    /** Hang up: release any connection and free the line. */
    public void hangUp(int number) {
        Line l = lines.get(number);
        if (l == null) {
            return;
        }
        release(number);
        l.state = State.ON_HOOK;
        l.digits = "";
        l.timer = 0;
        l.lamp = false;
    }

    /** Answer a ringing line: the two ends are joined and may talk. */
    public void answer(int number) {
        Line l = lines.get(number);
        if (l == null || l.state != State.RINGING || l.peer < 0 || l.outgoing) {
            return;   // only the party being called answers
        }
        Line caller = lines.get(l.peer);
        if (caller == null) {
            hangUp(number);
            return;
        }
        l.state = State.TALKING;
        caller.state = State.TALKING;
        l.lamp = true;
        caller.lamp = true;
        emit(Event.Kind.ANSWERED, number, caller.number);
    }

    // ------------------------------------------------------------- routing

    private void route(Line caller) {
        String d = caller.digits;
        caller.digits = "";
        if (d.isEmpty()) {
            caller.state = State.OFF_HOOK;
            return;
        }
        int wanted;
        try {
            wanted = Integer.parseInt(d);
        } catch (NumberFormatException e) {
            caller.state = State.OFF_HOOK;
            return;
        }
        if (wanted == caller.number) {
            caller.state = State.OFF_HOOK;
            return;
        }
        if (wanted == OPERATOR && exists(OPERATOR)) {
            connectOrRing(caller, lines.get(OPERATOR));
            return;
        }
        Line target = lines.get(wanted);
        if (target == null) {
            // Not on the exchange: the operator takes the call and connects it by hand.
            caller.state = State.OFF_HOOK;
            if (exists(OPERATOR)) {
                Line op = lines.get(OPERATOR);
                op.heldFor = caller.number;
                op.wants = wanted;
                emit(Event.Kind.OPERATOR_CALL, caller.number, wanted);
            }
            return;
        }
        connectOrRing(caller, target);
    }

    private void connectOrRing(Line caller, Line target) {
        if (target.state != State.ON_HOOK) {
            caller.state = State.BUSY;   // engaged tone; the caller must try later
            emit(Event.Kind.BUSY, caller.number, target.number);
            return;
        }
        target.state = State.RINGING;
        target.peer = caller.number;
        target.timer = 0;
        target.outgoing = false;
        caller.state = State.RINGING;
        caller.peer = target.number;
        caller.timer = 0;
        caller.outgoing = true;
    }

    /** The operator connects the number a held caller asked for. */
    public boolean operatorConnect(int wanted) {
        Line op = lines.get(OPERATOR);
        if (op == null || op.heldFor < 0) {
            return false;
        }
        Line caller = lines.get(op.heldFor);
        Line target = lines.get(wanted);
        if (caller == null || target == null) {
            return false;
        }
        op.heldFor = -1;
        op.wants = -1;
        if (target.state != State.ON_HOOK) {
            caller.state = State.BUSY;
            emit(Event.Kind.BUSY, caller.number, wanted);
            return false;
        }
        // the operator plugs the two cords together and drops off the line
        target.state = State.RINGING;
        target.peer = caller.number;
        target.timer = 0;
        target.outgoing = false;
        caller.state = State.RINGING;
        caller.peer = target.number;
        caller.timer = 0;
        caller.outgoing = true;
        return true;
    }

    private void release(int number) {
        Line l = lines.get(number);
        if (l == null) {
            return;
        }
        int peer = l.peer;
        if (peer >= 0) {
            emit(Event.Kind.RELEASED, number, peer);
        }
        l.peer = -1;
        l.timer = 0;
        l.outgoing = false;
        if (peer >= 0) {
            Line other = lines.get(peer);
            if (other != null) {
                other.peer = -1;
                other.state = State.ON_HOOK;
                other.timer = 0;
                other.lamp = false;
            }
        }
        if (l.state == State.TALKING || l.state == State.RINGING || l.state == State.BUSY) {
            l.state = State.ON_HOOK;
            l.lamp = false;
        }
    }

    // ------------------------------------------------------------- time

    /** Advance one tick and report what happened, so the world can ring bells and talk. */
    public List<Event> tick() {
        List<Event> events = new ArrayList<>();
        for (Line l : lines.values()) {
            switch (l.state) {
                case DIALING -> {
                    l.timer++;
                    if (l.timer >= DIAL_RETURN_TICKS) {
                        route(l);
                    }
                }
                case RINGING -> {
                    l.timer++;
                    if (l.timer % 40 == 0) {
                        events.add(new Event(Event.Kind.RING, l.number, l.peer));
                    }
                    if (l.timer >= RING_TIMEOUT_TICKS) {
                        events.add(new Event(Event.Kind.GAVE_UP, l.number, l.peer));
                        int peer = l.peer;
                        release(l.number);
                        l.state = State.ON_HOOK;
                        l.lamp = false;
                        if (peer >= 0 && lines.containsKey(peer)) {
                            lines.get(peer).state = State.ON_HOOK;
                            lines.get(peer).lamp = false;
                        }
                    }
                }
                case BUSY -> {
                    l.timer++;
                    if (l.timer >= 60) {
                        l.state = State.ON_HOOK;   // the caller gives up and hangs up
                        l.lamp = false;
                    }
                }
                default -> { }
            }
        }
        events.addAll(pending);
        pending.clear();
        return events;
    }
}
