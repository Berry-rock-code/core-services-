# Address Normalization Rules

Normalization is the step that makes addresses from three different systems (Salesforce, Google Sheets, Buildium) comparable. Each system has its own data-entry conventions, abbreviations, and quirks. Without normalization, "123 North Main Street" and "123 N Main St" would never match even though they refer to the same property.

The normalization pipeline is implemented in a single class, `AddressNormalizer` (`util` package), which is a Spring `@Component` injected by both execution paths:

- `AddressPipelineService` — startup-time batch pipeline
- `AddressSyncService` — API-triggered sync path

Rules are applied in the order listed below. The order matters: some rules would produce incorrect results if applied out of sequence.

---

## Rule 1 — Uppercase

**What it does:** Converts the entire string to uppercase before any other rule runs.

**Why:** All subsequent regex rules use uppercase literals. Uppercasing first ensures that "Main St", "MAIN ST", and "main st" all reach the same state before pattern matching begins.

| Before | After |
|--------|-------|
| `123 Main Street` | `123 MAIN STREET` |
| `123 n. oak ave` | `123 N. OAK AVE` |

---

## Rule 2 — Punctuation Removal

**What it does:** Removes periods, commas, hash signs, and apostrophes.

**Why:** Different data sources use punctuation inconsistently. Salesforce may store "123 N. Main St." while the Loan Tape has "123 N Main St". Removing these characters before comparison eliminates false mismatches.

Pattern: `[.,#']` replaced with empty string.

| Before | After |
|--------|-------|
| `123 N. Main St., Apt #4` | `123 N MAIN ST APT 4` |
| `St. Louis, MO` | `ST LOUIS MO` |

---

## Rule 3 — Whitespace Collapse

**What it does:** Replaces any run of one or more whitespace characters (spaces, tabs, newlines) with a single space, then trims leading and trailing spaces.

**Why:** Copy-paste into sheets and Salesforce form fields frequently introduces double spaces or leading/trailing spaces. Without this step, "123  Main St" and "123 Main St" would not match.

| Before | After |
|--------|-------|
| `123  Main  St ` | `123 MAIN ST` |

---

## Rule 4 — ZIP+4 Stripping

**What it does:** Truncates ZIP+4 format (`12345-6789`) to the base 5-digit ZIP code.

**Why:** Salesforce stores postal codes in ZIP+4 format while Buildium and the Loan Tape typically store only 5-digit ZIPs. Without stripping the extension, `74101-4321` and `74101` would never match on the postal code component of the key.

Pattern: `\b(\d{5})-\d{4}\b` replaced with `$1`.

| Before | After |
|--------|-------|
| `74101-4321` | `74101` |
| `74101` | `74101` (unchanged) |

---

## Rule 5 — Street Suffix Standardization

**What it does:** Replaces full street suffix words with their standard USPS abbreviations.

**Why:** Salesforce often stores full words ("Street", "Avenue") while Buildium and manual entries use abbreviations ("St", "Ave"). This is the highest-volume source of address mismatches in the data set.

All replacements use word-boundary anchors (`\b`) to avoid matching partial words (e.g., "STREETCAR" would not be changed to "STCAR").

| Full word | Abbreviation |
|-----------|-------------|
| STREET | ST |
| AVENUE | AVE |
| ROAD | RD |
| DRIVE | DR |
| LANE | LN |
| COURT | CT |
| PLACE | PL |
| BOULEVARD | BLVD |
| TERRACE | TER |
| PARKWAY | PKWY |
| CIRCLE | CIR |

| Before | After |
|--------|-------|
| `123 MAIN STREET` | `123 MAIN ST` |
| `456 OAK AVENUE` | `456 OAK AVE` |
| `789 ELM BOULEVARD` | `789 ELM BLVD` |

---

## Rule 6 — City Aliases

**What it does:** Collapses known city name variants to a canonical short form.

**Why:** Berry Rock operates primarily in Oklahoma City, St. Louis, and Kansas City markets. These cities appear under several spellings and abbreviations across the three data sources. Canonicalizing them to short tokens (`OKC`, `STL`, `KC`) ensures cross-source consistency.

The Saint Louis variants must be applied **before** the general directional NORTH/SOUTH rules to prevent "SAINT" from being consumed first.

| Input variant | Canonical form |
|---------------|----------------|
| OKLAHOMA CITY | OKC |
| ST LOUIS | STL |
| SAINT LOUIS | STL |
| KANSAS CITY | KC |

| Before | After |
|--------|-------|
| `TULSA OK` | `TULSA OK` (unchanged; not an alias) |
| `OKLAHOMA CITY OK` | `OKC OK` |
| `SAINT LOUIS MO` | `STL MO` |
| `ST LOUIS MO` | `STL MO` |
| `KANSAS CITY MO` | `KC MO` |

---

## Rule 7 — Directional Expansion

**What it does:** Abbreviates full compass direction words to their standard one- or two-letter forms.

**Why:** Compound directionals (NORTHWEST, NORTHEAST) must be handled before single-letter directionals (NORTH, WEST) to avoid converting NORTHWEST to NW being pre-empted by NORTH being changed to N first, which would leave WEST unprocessed.

| Full word | Abbreviation |
|-----------|-------------|
| NORTHWEST | NW |
| NORTHEAST | NE |
| SOUTHWEST | SW |
| SOUTHEAST | SE |
| NORTH | N |
| SOUTH | S |
| EAST | E |
| WEST | W |

| Before | After |
|--------|-------|
| `123 NORTH MAIN ST` | `123 N MAIN ST` |
| `456 NORTHWEST HWY` | `456 NW HWY` |
| `789 EAST BROADWAY` | `789 E BROADWAY` |

---

## Rule 8 — Trailing Unit Suffix Removal

**What it does:** Strips trailing patterns of the form `- <digits>` from the end of the string (e.g., `123 MAIN ST - 12` becomes `123 MAIN ST`).

**Why:** Some data sources append unit numbers in a dash-separated format at the end of the street address line. Buildium stores unit addresses without this suffix. Stripping it allows the street line to match across sources that handle unit information differently.

Pattern: `\s*-\s*\d+$` replaced with empty string.

| Before | After |
|--------|-------|
| `123 MAIN ST - 4` | `123 MAIN ST` |
| `456 OAK AVE - 12` | `456 OAK AVE` |
| `789 ELM DR` | `789 ELM DR` (unchanged) |

---

## Composite Key Format

After each address component is individually normalized, the four components are joined into a single pipe-delimited key:

```
<normalized address>|<normalized city>|<normalized state>|<normalized ZIP>
```

Example:

| Input | Normalized component |
|-------|----------------------|
| `"123 North Main Street"` | `123 N MAIN ST` |
| `"Oklahoma City"` | `OKC` |
| `"OK"` | `OK` |
| `"74101-4321"` | `74101` |

Resulting key: `123 N MAIN ST|OKC|OK|74101`

If a component is missing (`null`), its segment in the key is left empty (two adjacent pipes or a trailing pipe) so that the key structure remains consistent and records with missing fields do not accidentally match records where the field is present.
