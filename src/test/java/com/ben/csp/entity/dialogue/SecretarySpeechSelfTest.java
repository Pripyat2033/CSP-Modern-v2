package com.ben.csp.entity.dialogue;

/**
 * Verifies the spoken route into the zone: the Director talks to the secretary, and
 * the words become zone operations. Two things are checked — that the zone phrases
 * parse to the right intent, and that the secretary (and only the secretary) answers
 * them — plus that the existing worker intents still parse as they did.
 */
public final class SecretarySpeechSelfTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        // The zone phrases reach the zone intents.
        expect("found the site", DialogueIntent.FOUND_SITE);
        expect("start the works", DialogueIntent.FOUND_SITE);
        expect("lay out the zone", DialogueIntent.FOUND_SITE);
        expect("how is the site", DialogueIntent.SITE_REPORT);
        expect("how far have the enterprises come", DialogueIntent.SITE_REPORT);
        expect("muster the workforce", DialogueIntent.MUSTER);
        expect("bring the men out", DialogueIntent.MUSTER);
        expect("the desk", DialogueIntent.PROMBAZA_REPORT);
        expect("what has arrived", DialogueIntent.PROMBAZA_REPORT);
        expect("work the wagons", DialogueIntent.DELIVER);
        expect("clear the siding", DialogueIntent.DELIVER);

        // The zone phrases are checked FIRST, so they do not fall through to the
        // personal intents ("how is the site" must not read as a worker's STATUS).
        expect("how is the site", DialogueIntent.SITE_REPORT);
        expect("the inspection", DialogueIntent.PROMBAZA_REPORT);

        // The existing worker intents are untouched.
        expect("hello", DialogueIntent.GREET);
        expect("come here", DialogueIntent.COME);
        expect("follow me", DialogueIntent.FOLLOW);
        expect("stand down", DialogueIntent.STAND_DOWN);
        expect("to your post", DialogueIntent.GO_POST);
        expect("report", DialogueIntent.STATUS);
        expect("who are you", DialogueIntent.WHO);
        expect("what is this place", DialogueIntent.PLACE);
        expect("what can you do", DialogueIntent.HELP);
        expect("the reactor is beautiful today", DialogueIntent.UNKNOWN);

        // The secretary answers the zone intents, and only those.
        check(SecretarySpeech.handles(DialogueIntent.FOUND_SITE), "she takes founding the site");
        check(SecretarySpeech.handles(DialogueIntent.SITE_REPORT), "she reports the site");
        check(SecretarySpeech.handles(DialogueIntent.MUSTER), "she musters the workforce");
        check(SecretarySpeech.handles(DialogueIntent.PROMBAZA_REPORT), "she reports the desk");
        check(SecretarySpeech.handles(DialogueIntent.DELIVER), "she works the wagons");
        check(!SecretarySpeech.handles(DialogueIntent.STATUS), "a status is not a zone matter");
        check(!SecretarySpeech.handles(DialogueIntent.GREET), "a greeting is not a zone matter");
        check(!SecretarySpeech.handles(DialogueIntent.UNKNOWN), "an unknown is not a zone matter");

        System.out.println(failures == 0
            ? "SecretarySpeechSelfTest: " + checks + " checks passed"
            : "SecretarySpeechSelfTest: " + failures + " FAILED of " + checks);
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

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL: " + what);
        }
    }
}
