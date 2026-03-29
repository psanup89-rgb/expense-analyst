# Design System Specification: Editorial Kinetic Finance

## 1. Overview & Creative North Star: "The Neon Ledger"
This design system rejects the "standard dashboard" aesthetic in favor of **Kinetic Editorialism**. It treats financial data not as a static spreadsheet, but as a living, breathing narrative. By combining a deep, ink-like foundation with high-energy neon accents, we create a high-contrast environment that feels both authoritative and dangerously modern.

**The Creative North Star** is "Precision Glow." We break the traditional grid through intentional asymmetry—placing heavy display typography against vast open space—and use overlapping "glass" layers to create a sense of three-dimensional data. This isn't just a tool; it’s a high-performance instrument.

---

## 2. Colors & Surface Architecture
The color palette is built on high-contrast "signals" against a void. We move beyond flat backgrounds by using a tiered system of dark surfaces.

### The Palette
*   **Primary (Neon Green - `#CCFF00`):** Used for growth, "In" transactions, and primary kinetic actions.
*   **Secondary (Yellow - `#EAEA00`):** Reserved for "Out" transactions and cautionary data points.
*   **Tertiary/Error (Deep Red - `#FFB4AB`):** For negative balances and critical alerts.
*   **The Foundation:** `surface` (#131313) provides the obsidian canvas.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section content. Separation must be achieved through:
1.  **Tonal Shifts:** Placing a `surface-container-low` card on a `surface` background.
2.  **Negative Space:** Using the `12` (3rem) or `16` (4rem) spacing tokens to create mental boundaries.

### Glass & Gradient Logic
To elevate the UI from "flat dark mode" to "premium digital," use **Glassmorphism** for floating elements (like the navigation). 
*   **Floating Nav:** Apply `surface-container-high` at 60% opacity with a `20px` backdrop blur.
*   **Signature Textures:** Apply a subtle radial gradient on large `primary` buttons, transitioning from `primary_fixed` to `primary_fixed_dim` to give the neon a "gas-discharge" glow rather than a flat fill.

---

## 3. Typography: The Hierarchical Pulse
We use a tri-font system to balance readability with a sophisticated, tech-forward editorial feel.

*   **Display & Headline (Plus Jakarta Sans):** Our "Voice." Large, bold, and expressive. Use `display-lg` for total balances to create an unapologetic focal point.
*   **Title & Body (Inter):** Our "Functional Core." Clean, high-legibility sans-serif for transaction names and descriptions.
*   **Labels (Space Grotesk):** Our "Technical Detail." Used in `label-md` or `label-sm`. Always uppercase with `0.05em` letter spacing in a muted gray (`on-surface-variant`). This mimics the "small caps" aesthetic of high-end financial reports.

---

## 4. Elevation & Depth: Tonal Layering
We do not use shadows to mimic light; we use tonal shifts to mimic physical stacking.

*   **The Layering Principle:** 
    *   **Base:** `surface-container-lowest` (The deepest background).
    *   **Sections:** `surface-container-low`.
    *   **Interactive Cards:** `surface-container-highest` (Creates a natural lift).
*   **Ambient Shadows:** If a card must "float" (e.g., a modal), use a shadow with a `48px` blur at 8% opacity, tinted with `surface-tint` (#abd600) to create a subtle green-tinted atmospheric glow.
*   **The Ghost Border:** If accessibility requires a container definition, use `outline-variant` at **15% opacity**. Never use a 100% opaque stroke.

---

## 5. Components

### Pill-Shaped Buttons
*   **Primary:** Background `primary_fixed` (#c3f400), Text `on_primary_fixed` (#161e00). Shape: `full` (9999px). 
*   **Secondary:** Ghost style. `outline` stroke at 20% opacity. Text `primary`.
*   **States:** On hover, apply a `surface-bright` inner glow to simulate a light turning on.

### Circular Transaction Icons
*   **Style:** Every transaction icon must be contained in a perfect circle using `surface-container-high`. 
*   **Visual Cue:** Use a 2px "indicator dot" in the top right of the circle (Neon Green for 'In', Yellow for 'Out') to categorize at a glance without needing text.

### Inputs & Fields
*   **Style:** "Bottom-Line Only" or "Soft Surface." Avoid the "box" look. Use `surface-container-low` with a `sm` (0.5rem) rounded top.
*   **Error State:** The label shifts to `tertiary_fixed_dim` and the bottom border glows with a 2px `error` stroke.

### Cards & Lists (The Divider-Free Rule)
*   **Lists:** Forbid the use of divider lines. Separate transaction items using `8` (2rem) vertical padding. 
*   **Cards:** Use `md` (1.5rem) or `lg` (2rem) corner radius. Content should be "in-set" using the `6` (1.5rem) spacing token to create a breathing room effect.

### Floating Navigation
*   **Context:** A bottom-docked pill. 
*   **Visuals:** `surface-container-highest` at 70% opacity. Backdrop blur: `16px`. Icons should be `on-surface` when inactive and `primary` when active.

---

## 6. Do’s and Don’ts

### Do
*   **Do** use asymmetrical layouts. A balance value can be aligned left while the "trend" chart is aligned right with significant white space between them.
*   **Do** use the `primary` Neon Green for "In" charts and `secondary` Yellow for "Out" charts to ensure instant cognitive recognition.
*   **Do** embrace "Deep Dark." Let the `surface-container-lowest` occupy at least 30% of the screen to make the neon elements pop.

### Don't
*   **Don't** use pure white (#FFFFFF) for body text. Use `on_surface` (#e5e2e1) to reduce eye strain in dark environments.
*   **Don't** use standard "Drop Shadows." They look muddy on deep gray. Use Tonal Layering or tinted Ambient Shadows instead.
*   **Don't** use 90-degree corners. Everything must feel approachable; use the `md` to `full` roundedness scale for all containers.