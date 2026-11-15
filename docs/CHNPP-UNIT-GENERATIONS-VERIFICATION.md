# ChNPP unit generations — verification note

Requested by Ben: verify that ChNPP units 1–2 differ from units 3–4 (different
design generation), and that unit 4 differs from unit 3 (he cites a different
ceiling/roof, plus minor changes such as pipe routes). This note records what is
**confirmed**, what is **not**, and on which **axis** any difference lies.

## Confirmed: the site has two construction stages

The plant's own operating regulation — `docs/primary-sources/reglament-1E-S-II-1983.txt`
— is titled *Технологический регламент эксплуатации АЭС с реакторами РБМК-1000,
1Э-С-II-1983*, and states (line 24) that it is **"по эксплуатации 3 и 4
энергоблоков Чернобыльской АЭС"**, with general operational command over
**"II очереди ЧАЭС"** (the second stage). The "**II**" in the document code is the
stage. So the regulation we already hold is the **units 3 and 4** document, and it
distinguishes a first stage from a second.

ru.wikipedia (Чернобыльская АЭС) states directly: *«Первая очередь ЧАЭС (первый и
второй энергоблоки…) была построена в 1970—1977 годах, вторая очередь (третий и
четвёртый энергоблоки с аналогичными реакторами) была построена на этой же
площадке»* — Stage I = units 1–2 (1970–1977), Stage II = units 3–4.

INSAG-7 (IAEA, local copy `/workspace/insag7.pdf`) independently refers to the
**"second stages of the Kursk and Chernobyl plants"** and to the Technical Safety
Report **"for the Second Stage of the Kursk and Chernobyl Nuclear Power Plants"**
(GIDROPROEKT Rep. 180, 1974). So Stage II was a standard design shared between
Kursk and Chernobyl.

**Conclusion: units 1–2 (Stage I) and units 3–4 (Stage II) are confirmed as two
different construction stages. This part of Ben's statement is correct.**

## Confirmed: the RBMK-1000 generation taxonomy

Per the RBMK reference, RBMK reactors fall into **three generations**:

| Generation | Period | Safety basis |
|-----------|--------|--------------|
| Generation 1 | early–mid 1970s | before OPB-82 |
| Generation 2 | late 1970s – early 1980s | OPB-82 (1982) |
| Generation 3 | post-1986 | OPB-88; **only Smolensk-3** |

The decisive, measurable marker is the **core channel count**:

- **Generation 1**: **1693** fuel channels + **170** control-rod channels.
- **Generation 2** (explicitly *"such as Kursk and Chernobyl 3/4"*): **1661** fuel
  channels + **211** control-rod channels.

Our project's `Instrumentation` already models **1661 / 211**, i.e. the
second-generation core — consistent with ChNPP 3/4 being Stage II / Generation 2.

**Conclusion: units 3–4 are confirmed second-generation RBMK-1000. This part is
correct.**

## NOT confirmed: unit 4 differing from unit 3

The standard taxonomy groups **units 3 AND 4 together** as one generation
(Generation 2 / Stage II), with the same second-generation core (1661/211). No
source located treats unit 3 and unit 4 as different *reactor* generations.

Ben's cited difference (a different **ceiling/roof** in unit 4 vs unit 3, plus
changed pipe routes) — if real — lies on a **different axis**: it is a
**building / as-built** difference between two units of the *same* generation, not
a reactor-generation difference. Construction of a large Soviet power plant was
spread over years, and later units of a stage commonly carry as-built changes
(roof/roofing, cable and pipe routing) without changing the reactor design.

**Status: unverified.** No authoritative source on the specific unit-3 vs unit-4
ceiling/roof difference was found in this pass — web search engines (Google, Bing,
DuckDuckGo, Mojeek, Startpage, Ecosia, searx) all blocked automated access, and
the local PDFs do not address it. This needs either a specific source from Ben, or
a documentary/photographic comparison of the two units' reactor-hall ceilings.

## Practical consequence for the reconstruction

The right modelling axis is the **generation** (Stage I vs Stage II), which is
confirmed and already reflected in the project. Any unit-3 vs unit-4 building
differences are a **finer, per-unit layer** and must be sourced before they are
built — they should not be inferred from the generation.
