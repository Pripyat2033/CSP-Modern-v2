package com.ben.csp.enterprise;

import java.util.List;

/**
 * The real people of the ChNPP construction, taken from their biographies.
 *
 * <p>Source: Russian Wikipedia, articles "Кизима, Василий Трофимович",
 * "Брюханов, Виктор Петрович", "Проценко, Мария Владимировна".
 *
 * <p>The chain of command was two parallel lines, not one. The operating
 * enterprise was directed by Bryukhanov; the construction was headed by Kizima
 * as chief of the ChNPP construction administration, a separate authority that
 * answered up the construction ministry, not to the director. Pripyat, the town,
 * was the architect Protsenko's.
 */
public final class HistoricalFigures {

    private HistoricalFigures() {}

    public static final String SOURCE =
        "ru.wikipedia: Кизима, Василий Трофимович; Брюханов, Виктор Петрович; Проценко, Мария Владимировна";

    /** Head of the ChNPP construction administration, 1971-1986. */
    public static final HistoricalPerson KIZIMA = new HistoricalPerson(
        "kizima", "Кизима, Василий Трофимович", "Vasily Trofimovich Kizima",
        "Chief of the ChNPP construction administration",
        "Управление строительства Чернобыльской АЭС",
        "Dec 1971 - 1986",
        SOURCE, Provenance.VERIFIED);

    /** Director of the ChNPP, 1970-1986. */
    public static final HistoricalPerson BRYUKHANOV = new HistoricalPerson(
        "bryukhanov", "Брюханов, Виктор Петрович", "Viktor Petrovich Bryukhanov",
        "Director of the ChNPP",
        "Чернобыльская АЭС им. В. И. Ленина",
        "Apr 1970 - Jul 1986",
        SOURCE, Provenance.VERIFIED);

    /** Chief architect of Pripyat, 1979-. */
    public static final HistoricalPerson PROTSENKO = new HistoricalPerson(
        "protsenko", "Проценко, Мария Владимировна", "Maria Vladimirovna Protsenko",
        "Chief architect of Pripyat",
        "Дипромисто",
        "1979 -",
        SOURCE, Provenance.VERIFIED);

    /** Everyone recorded here. */
    public static List<HistoricalPerson> all() {
        return List.of(KIZIMA, BRYUKHANOV, PROTSENKO);
    }

    public static HistoricalPerson byId(String id) {
        return all().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    }
}
