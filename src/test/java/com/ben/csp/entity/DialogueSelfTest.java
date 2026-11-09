package com.ben.csp.entity;

import com.ben.csp.entity.dialogue.DialogueIntent;

/** Verifies the plain-words parser that lets the Director talk to the workers. */
public final class DialogueSelfTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        expect("hello there", DialogueIntent.GREET);
        expect("Hi", DialogueIntent.GREET);
        expect("come here", DialogueIntent.COME);
        expect("come to me please", DialogueIntent.COME);
        expect("follow me", DialogueIntent.FOLLOW);
        expect("stand down", DialogueIntent.STAND_DOWN);
        expect("wait here", DialogueIntent.STAND_DOWN);
        expect("stop", DialogueIntent.STAND_DOWN);
        expect("to your post", DialogueIntent.GO_POST);
        expect("get to work", DialogueIntent.GO_POST);
        expect("report", DialogueIntent.STATUS);
        expect("what are you doing", DialogueIntent.STATUS);
        expect("who are you", DialogueIntent.WHO);
        expect("what is this place", DialogueIntent.PLACE);
        expect("what can you do", DialogueIntent.HELP);
        expect("the reactor is beautiful today", DialogueIntent.UNKNOWN);
        expect("", DialogueIntent.UNKNOWN);
        expect(null, DialogueIntent.UNKNOWN);

        System.out.println(failures == 0
            ? "DialogueSelfTest: " + checks + " checks passed"
            : "DialogueSelfTest: " + failures + " FAILED of " + checks);
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void expect(String text, DialogueIntent want) {
        checks++;
        DialogueIntent got = DialogueIntent.fromText(text);
        if (got != want) {
            failures++;
            System.out.println("  FAIL: \"" + text + "\" -> " + got + ", wanted " + want);
        }
    }
}
