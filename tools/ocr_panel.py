#!/usr/bin/env python3
"""ocr_panel.py — read a control panel from a PHOTOGRAPH into a control layout.

I (the agent) cannot see images. But a control-panel photograph is mostly *text
at positions*, and that I CAN extract: OCR finds the labels and their pixel
boxes, and a constrained dictionary — the 1983 regulation's own abbreviations
plus the standard Soviet panel vocabulary — corrects OCR's Latin/Cyrillic
confusions (АЗ-5 is read as "A3-5", ОРМ as "OPM", ГЦН as "UH"). The result is a
JSON layout of labelled controls with normalised (0..1) positions, plus an ASCII
sketch, which a human then corrects and approves before anything is built.

This turns "place every control by hand" into "photograph the desk; correct a
short OCR list" — bounded, reviewable work, not months of guessing.

Usage:
    python3 tools/ocr_panel.py PANEL.jpg [--out layout.json] [--ascii-cols 80]

Needs: pip install rapidocr-onnxruntime pillow
"""
import argparse
import difflib
import json
import sys

# ------------------------------------------------------------------ dictionary

# OCR reads Cyrillic as Latin look-alikes. Map the Latin side back to Cyrillic
# before matching, so "A3-5" -> "АЗ-5" and "OPM" -> "ОРМ".
CONFUSABLES = {
    "A": "А", "B": "В", "C": "С", "E": "Е", "H": "Н", "K": "К", "M": "М",
    "O": "О", "P": "Р", "T": "Т", "X": "Х", "Y": "У", "y": "у", "c": "с",
    "3": "З", "0": "О", "6": "б", "9": "г", "4": "Ч", "N": "И", "n": "п",
    "p": "р", "a": "а", "e": "е", "o": "о", "x": "х", "u": "и",
}

# The vocabulary a Soviet RBMK panel label is drawn from. Seeded from the 1983
# regulation's abbreviation list (1Э-С-II) and the plant's own equipment
# register; extend as more panels are read.
GLOSSARY = [
    # protections / reactor control
    "АЗ-1", "АЗ-2", "АЗ-3", "АЗ-4", "АЗ-5", "АЗС", "КАЗ", "ОРМ", "АР", "АРМ",
    "СУЗ", "УСЗ", "РР", "ПГ", "СФКРЭ", "СЦК", "МТК", "МТО", "КЦТК", "КРБ",
    # detectors / instrumentation
    "ИК", "КД", "ДКЭ", "ИСС", "УП", "ИВ", "КИП",
    # people / places
    "СИУР", "СИУТ", "СИУБ", "ЗНСС", "НСС", "НСЭЦ", "БЩУ-О", "БЩУ-Н", "ЦЩУ",
    "РПУ", "ПУЛЬТ А", "ПУЛЬТ Т", "ПУЛЬТ П", "ПУЛЬТ Д",
    # circuits / plant
    "ГЦН", "МЦК", "КМПЦ", "ТК", "КОО", "ПГ", "БС", "БРУ-К", "БРУ-Д", "САОР",
    "СОС", "ГПЗ", "СРК", "ПЭН", "КЭН", "НЧК", "БЧК", "ПТН", "ТГ", "ДГ", "ТСН",
    "МВ", "ОРУ", "КРУ", "ГРУ", "РЩ", "БЩС", "ГРЩ", "СН",
    # electrical / general
    "МОЩНОСТЬ", "РАСХОД", "ДАВЛЕНИЕ", "УРОВЕНЬ", "ТЕМПЕРАТУРА", "ВЛАЖНОСТЬ",
    "ГРАФИТ", "РЕАКТОР", "ТУРБИНА", "ГЕНЕРАТОР", "НАСОС", "СЕПАРАТОР",
    "СТОП", "ПУСК", "АВАРИЯ", "БЛОКИРОВКА", "ОТКЛ", "ВКЛ",
]


def normalize(s: str) -> str:
    """Uppercase, drop separators, map Latin look-alikes to Cyrillic."""
    out = []
    for ch in s.upper():
        if ch.isalnum() or ch in "-/":
            out.append(CONFUSABLES.get(ch, ch))
    return "".join(out)


def match(text: str, glossary):
    """Best (canonical, ratio) for an OCR string against the glossary."""
    n = normalize(text)
    best, score = None, 0.0
    for term in glossary:
        r = difflib.SequenceMatcher(None, n, normalize(term)).ratio()
        if r > score:
            best, score = term, r
    return best, score


def read_panel(image_path, glossary):
    from rapidocr_onnxruntime import RapidOCR
    ocr = RapidOCR()
    res, _ = ocr(image_path)
    if not res:
        return [], 0, 0
    xs, ys = [], []
    for box, _t, _s in res:
        for p in box:
            xs.append(p[0]); ys.append(p[1])
    w = max(xs) or 1; h = max(ys) or 1
    controls = []
    for box, text, score in res:
        bx = [p[0] for p in box]; by = [p[1] for p in box]
        x0, y0, x1, y1 = min(bx), min(by), max(bx), max(by)
        canon, ratio = match(text, glossary)
        controls.append({
            "ocr": text,
            "label": canon if ratio >= 0.6 else text,
            "confidence": round(float(score), 2),
            "match": round(ratio, 2),
            "needs_review": ratio < 0.75,
            "x0": round(x0 / w, 3), "y0": round(y0 / h, 3),
            "x1": round(x1 / w, 3), "y1": round(y1 / h, 3),
        })
    controls.sort(key=lambda c: (round(c["y0"], 1), c["x0"]))
    return controls, w, h


def ascii_sketch(controls, cols=80, rows=24):
    grid = [[" " for _ in range(cols)] for _ in range(rows)]
    for c in controls:
        cx = int((c["x0"] + c["x1"]) / 2 * (cols - 1))
        cy = int((c["y0"] + c["y1"]) / 2 * (rows - 1))
        label = c["label"][:8]
        for i, ch in enumerate(label):
            if 0 <= cy < rows and 0 <= cx + i < cols:
                grid[cy][cx + i] = ch
    return "\n".join("".join(r) for r in grid)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("image")
    ap.add_argument("--out", default=None)
    ap.add_argument("--ascii-cols", type=int, default=80)
    args = ap.parse_args()

    controls, _w, _h = read_panel(args.image, GLOSSARY)
    if not controls:
        print("No text found in the image.", file=sys.stderr)
        return 1

    print(f"# {len(controls)} controls read from {args.image}\n")
    print("## ASCII sketch (positions are real, text is the corrected label)")
    print(ascii_sketch(controls, cols=args.ascii_cols))
    print("\n## Controls")
    for c in controls:
        flag = "  <-- REVIEW" if c["needs_review"] else ""
        print(f"  {c['label']:<10} ocr={c['ocr']!r:<16} match={c['match']} "
              f"conf={c['confidence']}  x[{c['x0']:.2f}..{c['x1']:.2f}] "
              f"y[{c['y0']:.2f}..{c['y1']:.2f}]{flag}")

    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump({"controls": controls}, f, ensure_ascii=False, indent=2)
        print(f"\nwrote {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
