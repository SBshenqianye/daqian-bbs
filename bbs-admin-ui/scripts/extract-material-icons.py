#!/usr/bin/env python3
"""Extract Material Symbols icon names from woff2 GSUB ligature table."""

import json, re, sys, os
from fontTools.ttLib import TTFont

FONT_PATH = os.path.join(os.path.dirname(__file__), '..', 'src', 'assets', 'fonts', 'MaterialSymbolsOutlined.woff2')
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), '..', 'src', 'assets', 'icons', 'material-icons.json')


def build_glyph_to_char():
    """Build mapping from glyph name to the character it represents."""
    mapping = {}

    # Single lowercase letters map to themselves
    for c in 'abcdefghijklmnopqrstuvwxyz':
        mapping[c] = c

    # Digits (glyph names use English words: digit_zero, digit_one, digit_two, ...)
    digit_words = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine']
    digits = {f'digit_{word}': str(n) for n, word in enumerate(digit_words)}
    mapping.update(digits)

    # Special characters used in Material Symbols icon names
    special = {
        'underscore': '_',
        'hyphen': '-',
        'period': '.',
        'space': ' ',
        'exclam': '!',
        'question': '?',
        'ampersand': '&',
        'asciitilde': '~',
        'equal': '=',
        'plus': '+',
        'slash': '/',
        'asterisk': '*',
        'percent': '%',
        'dollar': '$',
        'numbersign': '#',
        'at': '@',
        'bar': '|',
        'colon': ':',
        'semicolon': ';',
        'less': '<',
        'greater': '>',
        'comma': ',',
        'quotesingle': "'",
        'quotedbl': '"',
        'grave': '`',
        'asciicircum': '^',
        'braceleft': '{',
        'braceright': '}',
        'bracketleft': '[',
        'bracketright': ']',
        'parenleft': '(',
        'parenright': ')',
    }
    mapping.update(special)
    return mapping


def extract_icon_names():
    font = TTFont(FONT_PATH)
    gsub = font['GSUB'].table
    glyph_to_char = build_glyph_to_char()

    lookup = gsub.LookupList.Lookup[0]
    names = set()

    for subtable in lookup.SubTable:
        ext = subtable.ExtSubTable
        for cov_char, ligs in ext.ligatures.items():
            first = glyph_to_char.get(str(cov_char), str(cov_char))
            for lig in ligs:
                parts = [first]
                for c in lig.Component:
                    c_str = str(c)
                    parts.append(glyph_to_char.get(c_str, c_str))
                full_name = ''.join(parts)
                # Material Symbols icon names use: a-z, 0-9, underscore
                if re.match(r'^[a-z][a-z0-9_]*$', full_name):
                    names.add(full_name)

    return sorted(names)


def main():
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    names = extract_icon_names()

    with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
        json.dump(names, f, ensure_ascii=False, indent=2)

    print(f'Extracted {len(names)} icon names -> {OUTPUT_PATH}')

    # Verify key icons
    expected = ['thumb_up', 'help', 'folder_open', 'home', 'label', 'bookmark', 'search', 'add', 'edit', 'delete']
    for name in expected:
        found = name in names
        status = 'OK' if found else 'MISSING'
        print(f'  {name}: {status}')


if __name__ == '__main__':
    main()
