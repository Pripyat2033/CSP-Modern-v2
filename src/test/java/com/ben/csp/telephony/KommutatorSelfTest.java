package com.ben.csp.telephony;

import com.ben.csp.telephony.KommutatorExchange.Event;
import com.ben.csp.telephony.KommutatorExchange.State;

import java.util.List;

/**
 * The exchange must behave the way a real operator expects: engaged, ring, answer,
 * busy, release, and the operator putting a call through by hand.
 */
public final class KommutatorSelfTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        basicCall();
        busyLine();
        unknownNumberGoesToOperator();
        operatorConnectsByHand();
        diallingReturn();
        ringGivesUp();

        System.out.println(failures == 0
            ? "KommutatorSelfTest: " + checks + " checks passed"
            : "KommutatorSelfTest: " + failures + " FAILED of " + checks);
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static KommutatorExchange exchange() {
        KommutatorExchange x = new KommutatorExchange();
        x.register(0, "operator");
        x.register(101, "director");
        x.register(102, "chief engineer");
        x.register(103, "foreman");
        return x;
    }

    /** Let the dial return, so the number is routed. */
    private static void settle(KommutatorExchange x) {
        for (int i = 0; i < KommutatorExchange.DIAL_RETURN_TICKS; i++) {
            x.tick();
        }
    }

    private static void basicCall() {
        KommutatorExchange x = exchange();
        x.offHook(101);
        eq("off-hook engages the line", State.OFF_HOOK, x.line(101).state);
        eq("and lights the lamp", true, x.line(101).lamp);
        x.dial(101, '1');
        x.dial(101, '0');
        x.dial(101, '2');
        eq("dialling", State.DIALING, x.line(101).state);
        settle(x);
        eq("ringing after the dial returns", State.RINGING, x.line(102).state);
        eq("caller is ringing too", State.RINGING, x.line(101).state);
        x.answer(102);
        eq("answered: talking", State.TALKING, x.line(102).state);
        eq("both ends joined", State.TALKING, x.line(101).state);
        eq("peers are set", 102, x.line(101).peer);
        x.hangUp(101);
        eq("release frees the called line", State.ON_HOOK, x.line(102).state);
        eq("and the caller", State.ON_HOOK, x.line(101).state);
        eq("lamps out", false, x.line(102).lamp);
    }

    private static void busyLine() {
        KommutatorExchange x = exchange();
        x.offHook(102);
        x.offHook(101);
        x.dial(101, '1');
        x.dial(101, '0');
        x.dial(101, '2');
        settle(x);
        eq("calling an engaged line gives the busy tone", State.BUSY, x.line(101).state);
        eq("the engaged line is untouched", State.OFF_HOOK, x.line(102).state);
    }

    private static void unknownNumberGoesToOperator() {
        KommutatorExchange x = exchange();
        x.offHook(101);
        for (char c : "999".toCharArray()) {
            x.dial(101, c);
        }
        settle(x);
        eq("unknown number is handed to the operator", 101, x.line(0).heldFor);
        eq("with the wanted number", 999, x.line(0).wants);
    }

    private static void operatorConnectsByHand() {
        KommutatorExchange x = exchange();
        x.offHook(101);
        for (char c : "999".toCharArray()) {
            x.dial(101, c);
        }
        settle(x);
        boolean ok = x.operatorConnect(103);
        eq("operator puts the call through", true, ok);
        eq("the wanted line rings", State.RINGING, x.line(103).state);
        eq("the caller is ringing", State.RINGING, x.line(101).state);
    }

    private static void diallingReturn() {
        KommutatorExchange x = exchange();
        x.offHook(101);
        x.dial(101, '1');
        eq("not routed immediately", State.DIALING, x.line(101).state);
        for (int i = 0; i < KommutatorExchange.DIAL_RETURN_TICKS; i++) {
            x.tick();
        }
        eq("routed only when the dial returns", State.OFF_HOOK, x.line(101).state);
    }

    private static void ringGivesUp() {
        KommutatorExchange x = exchange();
        x.offHook(101);
        for (char c : "102".toCharArray()) {
            x.dial(101, c);
        }
        settle(x);
        boolean gaveUp = false;
        for (int i = 0; i < KommutatorExchange.RING_TIMEOUT_TICKS; i++) {
            gaveUp |= x.tick().stream().anyMatch(e -> e.kind() == Event.Kind.GAVE_UP);
        }
        eq("an unanswered line is released", true, gaveUp);
        eq("caller back on hook", State.ON_HOOK, x.line(101).state);
        eq("called line back on hook", State.ON_HOOK, x.line(102).state);
    }

    private static void eq(String what, Object want, Object got) {
        checks++;
        if (!java.util.Objects.equals(want, got)) {
            failures++;
            System.out.println("  FAIL: " + what + " -> " + got + ", wanted " + want);
        }
    }
}
