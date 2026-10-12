# Capsibus — UI Specification

Companion to `capsibus-prd.md`. Use this to drive a Claude Design pass **before** any Compose code is written. §8 is a paste-ready prompt.

---

## 1. Frame

| | |
|---|---|
| **Purpose** | Point a phone at a chili leaf, get either a diagnosis you can act on or an honest refusal |
| **Audience** | Smallholder growers and agriculture-extension staff in Malaysia; also FYP examiners |
| **Context of use** | Outdoors, bright sun, one hand, possibly dirty screen, no data connection |
| **Emotional tone** | Instrument, not assistant. Calm, precise, unhurried. It reports; it does not chirp. |
| **One thing to remember** | That the app told you when it *didn't* know |

## 2. Visual direction — industrial field instrument

Commit to this and do not blend it with the usual soft-green agri-app look. Reference points: a handheld multimeter, a soil-moisture meter readout, a lab instrument panel. Dark, high-contrast, numerically honest.

Why this direction rather than friendly-green: the product's differentiator is that it refuses to guess. Soft, reassuring UI actively undercuts that — it makes a rejection feel like a failure rather than a correct, deliberate output. Instrument language makes "insufficient confidence" read as the machine working properly.

**Explicitly avoid:** leaf-and-sun logo motifs, rounded pastel cards, gradient-on-white hero panels, illustrations of smiling farmers, emoji in verdict copy.

## 3. Visual system

### Colour

Dark-first. Sunlight legibility is the binding constraint — mid-tone-on-mid-tone dies outdoors.

| Token | Hex | Use |
|---|---|---|
| `surface` | `#12100E` | app background, warm-shifted near-black |
| `surfaceRaised` | `#1D1A17` | cards, sheets |
| `border` | `#332E29` | hairlines, 1dp |
| `onSurface` | `#F2EDE7` | primary text |
| `onSurfaceMuted` | `#9B928A` | secondary text, labels |
| `accent` | `#E0452B` | chili red — primary action, brand mark **only** |
| `statusHealthy` | `#4E9A5F` | Healthy verdict |
| `statusDisease` | `#D97706` | Bacterial / Fungal / Pests / Virus verdict |
| `statusUncertain` | `#7A736C` | Uncertain and NotChili — deliberately neutral |
| `statusError` | `#B33A28` | decode / interpreter failures |

Two rules that matter more than the palette itself:

- **Accent red is reserved.** Primary buttons and the wordmark. It never appears on a verdict, or it will read as an alarm.
- **Uncertain is grey, not yellow.** Yellow codes as a warning; a rejection is not a warning, it is a correct output. Grey says "no reading."

All verdict states carry an icon and a text label in addition to colour — never colour alone.

### Typography

- **Display / verdict:** a condensed grotesque with real presence — *Archivo Condensed* or *Oswald*. Uppercase, tight tracking, for the verdict word only.
- **Body / UI:** *Inter*. Minimum 16sp body, 14sp for the disclaimer. Nothing smaller.
- **Numerics:** *JetBrains Mono* or *Roboto Mono* with tabular figures, for every confidence percentage and probability value. Monospace numbers are the single strongest signal of the instrument direction, and they stop the probability list from shifting as values change.

Scale: 48 / 28 / 20 / 16 / 14, with the 48 reserved for the verdict.

### Spacing, surface, motion

- 8dp base rhythm; 24dp screen gutters; 16dp between stacked blocks.
- Touch targets ≥ 56dp, larger than the 48dp minimum — this is used with gloves and dirty hands.
- Corners 4dp. Sharp reads as instrument; 16dp+ reads as consumer app.
- Surfaces separated by 1dp `border` hairlines, not shadows. One elevated surface only: the result sheet.
- Background carries a very subtle warm noise texture at ~2% opacity so the dark field isn't dead flat.
- **Motion budget: one moment.** The result reveal — verdict block fades and rises 12dp over 240ms with the confidence figure counting up from 0 over 400ms. Everything else is a plain 150ms crossfade. No spring physics, no shimmer, no confetti on Healthy.

## 4. Screens

### 4.1 Home

Not a symmetrical two-button card grid. Asymmetric vertical composition:

- Wordmark **CAPSIBUS** top-left, mono-spaced, letter-spaced, small. Below it in muted 14sp: `chili leaf diagnostic · offline`. (Long-press the wordmark → dev screen, debug builds only.)
- Large negative space in the upper third.
- **Take Photo** — full-width primary, accent fill, 64dp tall, camera glyph left-aligned with the label.
- **Choose from Gallery** — full-width secondary, 1dp border, transparent fill, directly beneath at 12dp gap. Visibly subordinate, not a twin.
- Bottom-anchored, muted 12sp: `Model: InceptionV3 · τ 0.70 · on-device`. Small, technical, deliberately unexplained. It signals the instrument character to anyone who looks, and it is genuinely useful during the viva.

### 4.2 Camera

- Full-bleed CameraX preview.
- Framing guide: a 1dp accent-tinted square inset ~12% from the edges, corner ticks only, not a full box.
- Hint pill below the guide, `surfaceRaised` at 80% opacity: **Fill the frame with a single leaf. Avoid whole-plant shots.**
- Shutter: 72dp circle, accent ring, bottom-centre. Close (×) top-left. Nothing else.
- On capture: preview freezes, guide fades, indeterminate 2dp accent line at the top edge while inference runs. No spinner overlay, no "Analysing…" modal.

### 4.3 Result

Scrollable column on `surface`:

1. **Image** — 16:9 crop of the submitted photo, full-bleed to the gutters, 4dp corners. Show the image *as the model received it* (post-EXIF-rotation). If it's meaningfully different from what the user framed, they deserve to see that.
2. **Verdict block** — the composition anchor. A 4dp vertical status bar on the left edge, then:
   - Verdict word, 48sp display, uppercase: `BACTERIAL` / `HEALTHY` / `UNCERTAIN` / `NOT A CHILI LEAF`
   - Directly under it, mono, 28sp: `93.4%` — with a 14sp muted `CONFIDENCE` label above in letter-spaced caps
   - For Uncertain and NotChili, the number still shows, in `statusUncertain`. Hiding it would be dishonest and would also weaken the demo.
3. **Action text** — 16sp body on `surfaceRaised`, 20dp padding. The Pests caveat renders in `statusUncertain` inside the same block, not as a separate warning card.
4. **Detailed probabilities** — collapsed row: `Detailed probabilities ▾`. Expanded: six rows, class name left, mono value right, a 2dp horizontal bar beneath each scaled to the probability. Rows sorted descending. This is the section that makes the app legible to an examiner in ten seconds.
5. **Disclaimer** — 14sp `onSurfaceMuted`, 1dp top hairline, no icon, no coloured box. Present, permanent, not shouting.
6. **Analyse Another** — full-width secondary, bottom.

### 4.4 States

| State | Treatment |
|---|---|
| Inference running | 2dp indeterminate accent line at top edge; frozen preview stays visible |
| Permission denied | `surfaceRaised` card on Home, plain-language reason, **Retry** primary; gallery button stays enabled |
| Permission permanently denied | Same card, primary becomes **Open Settings** |
| Decode failure | Result screen with `statusError` bar, verdict word `UNREADABLE`, no confidence figure, action text explains and offers **Analyse Another** |
| Interpreter failure | Full-screen error, mono technical line in debug builds only |
| No camera hardware | Take Photo hidden entirely; gallery button promotes to primary styling |

Every error keeps the same verdict-block skeleton. Consistent structure across success and failure is what makes an interface feel like an instrument rather than a set of unrelated screens.

## 5. Accessibility

- Verdict, confidence, and action text form one merged semantics node so TalkBack reads them as a unit rather than three fragments.
- Content descriptions on the shutter, close, and expand controls.
- Text scales to 200% without clipping — the verdict word must be allowed to wrap or shrink, not truncate.
- All text/background pairs meet WCAG AA; the 48sp verdict meets AAA at large-text sizing.
- Never colour-only: icon plus label on every status.

## 6. Localisation readiness

Ship English strings only for v1, but keep every user-facing string in `strings.xml` with no concatenation and no sentence-fragment assembly. Bahasa Malaysia is the obvious next locale for actual field use, and retrofitting concatenated strings is miserable.

## 7. Do not build

Out of scope for v1 — recorded so they don't creep in: diagnosis history, sharing/export, onboarding carousel, dark/light toggle (dark only), settings screen beyond the hidden dev panel, Grad-CAM overlay, treatment-product lookup.

---

## 8. Paste-ready Claude Design prompt

> Design the mobile UI for **Capsibus**, an offline Android app that photographs a chili leaf and returns either a disease diagnosis with a confidence percentage, or an explicit refusal when the model isn't confident enough. Used outdoors in bright Malaysian sun by smallholder growers, one-handed.
>
> **Visual direction: industrial field instrument.** Think handheld multimeter or lab readout — dark, high-contrast, numerically precise. Commit fully; do not blend in friendly-green agri-app styling. No leaf-and-sun logos, no pastel rounded cards, no gradient hero panels, no illustrated farmers. The product's whole point is that it admits uncertainty, and soft reassuring UI would undercut that.
>
> **Palette (dark only):** background `#12100E`, raised surface `#1D1A17`, hairline border `#332E29`, primary text `#F2EDE7`, muted text `#9B928A`, accent chili-red `#E0452B` reserved strictly for primary buttons and the wordmark. Status colours: healthy `#4E9A5F`, disease `#D97706`, uncertain `#7A736C` (neutral grey, deliberately not yellow — a rejection is a correct output, not a warning), error `#B33A28`.
>
> **Type:** condensed uppercase grotesque (Archivo Condensed / Oswald) for the verdict word at 48sp only; Inter for body at 16sp minimum; monospace with tabular figures (JetBrains Mono) for every percentage and probability value. Scale 48/28/20/16/14. 8dp spacing rhythm, 24dp gutters, 4dp corners, 1dp hairline separators instead of shadows, touch targets 56dp minimum.
>
> **Three screens:**
>
> 1. **Home** — asymmetric, not a two-button grid. Wordmark CAPSIBUS top-left with a muted `chili leaf diagnostic · offline` subtitle. Generous empty upper third. Full-width accent "Take Photo" button 64dp tall, then a visibly subordinate outlined "Choose from Gallery" below it. Bottom-anchored small mono line: `Model: InceptionV3 · τ 0.70 · on-device`.
>
> 2. **Camera** — full-bleed preview, square framing guide drawn as corner ticks only, hint pill reading "Fill the frame with a single leaf. Avoid whole-plant shots.", 72dp accent-ringed shutter bottom-centre, close × top-left. Nothing else.
>
> 3. **Result** — image at top (16:9), then a verdict block with a 4dp coloured status bar on its left edge, the verdict word in 48sp uppercase display type, a small letter-spaced "CONFIDENCE" label, and the percentage in 28sp mono. Below: action-advice text on a raised surface; a collapsed "Detailed probabilities ▾" row that expands to six class rows each with a mono value and a 2dp bar, sorted descending; a permanent 14sp muted disclaimer above a hairline; and a full-width outlined "Analyse Another" button.
>
> **Show four result variants:** confident diagnosis (`BACTERIAL 93.4%`, orange status), healthy (`HEALTHY 98.1%`, green), rejection (`UNCERTAIN 62.0%`, neutral grey), and non-chili (`NOT A CHILI LEAF 85.0%`, neutral grey). The uncertain and non-chili states must look composed and intentional, not like errors — that is the most important thing to get right in this design.
>
> **Motion:** exactly one moment. The result verdict fades and rises 12dp over 240ms while the confidence figure counts up from zero over 400ms. Everything else is a 150ms crossfade. No springs, no shimmer.
>
> Design for a 393×852 viewport.
