# Coolblue Glimlach Run (Max, Remi & Vero Edition)

A fast-paced Super Mario-style retro platformer built as both a **native Android app** (Kotlin + hardware-accelerated Canvas + 44.1 kHz polyphonic chiptune synth) and an identically polished **Browser / PWA edition** (`docs/`), created as a live demo for **Coolblue** featuring **Max, Remi & Vero** as playable heroes.

> *"Alles voor een glimlach!"*

## Features
- **3 Playable Heroes:**
  - **Max** (*Express Bezorger*) – Iconic Coolblue cap with orange visor and white smile emblem
  - **Remi** (*Glimlach Kampioen*) – Wavy dark hair, sleek glasses, and orange shoulder yoke
  - **Vero** (*Klantgeluk Ster*) – Bouncy chestnut-brown ponytail, Coolblue-orange headband, and customer-happiness headset
  - Whichever hero you choose, **both other teammates** jump and cheer together with a personalized speech bubble at the finish line!
- **2 Dutch-Themed Levels with Multi-Layer Parallax:**
  - `1-1 Coolblue Magazijn` – High-bay fulfillment warehouse with volumetric ceiling spotlights, automated upper mezzanine parcel conveyor, stacked Coolblue boxes, animated boost conveyors, orange spring pads, and a Coolblue delivery van at the finish.
  - `1-2 Grachtengordel Express` – Iconic Dutch canal houses (*trap-*, *klok-*, and *halsgevels* with tulip flower boxes), spinning windmills, a bobbing yellow canal duck wearing a Coolblue cap, brick bridges, rooftop shortcuts, and a customer house (*"BLIJE KLANT"*) confetti finale.
- **Arcade Game-Feel & Polyphonic Chiptune Audio:**
  - Squash-and-stretch physics, projected ground shadows, dust & water-splash particles, Bakfiets Turbo speed afterimages, and airborne stomp combo multipliers.
  - 6-voice polyphonic chiptune synthesizer (12.5% & 25% pulse waves, triangle bass, 15-bit LFSR noise percussion), rising coin-streak pitches, signature character selection jingles, and a toggleable scene-specific chiptune soundtrack.
- **Adaptive Multi-Form-Factor Layout (Smartphone, Tablet, Foldable & Chromebook/Desktop):**
  - **Smartphone Portrait:** Dedicated vertical layout with a tactile **Coolblue Arcade Control Deck** below the gameplay viewport so thumbs never obscure the action.
  - **Smartphone Landscape (20:9 / 21:9 Ultrawide) & Tablets (4:3 / 16:10):** Dynamic edge-to-edge viewport with zero letterboxing and full display-cutout / safe-area inset support.
- **Controls (Touch, Keyboard & Gamepad):**
  - `←` / `→` or `A` / `D`: Run (or cycle character on the title screen)
  - `Space` / `↑` / `W`: Jump (variable jump height, coyote time & jump buffering)
  - `G` or `⚡ Demo`: Toggle Demo Mode (invulnerable + automatic pit-fall rescue)
  - `1` / `2`: Jump directly to Level 1 or Level 2
  - `B`: Toggle background chiptune music
  - `M`: Mute / unmute all audio
  - `T`: Show / hide on-screen touch controls
  - `F`: Toggle fullscreen (Browser edition)
  - `R` / `Esc`: Restart level / return to title screen

## Android Build & Unit Tests
```bash
./gradlew testDebugUnitTest assembleDebug
```
The compiled debug APK is written to `app/build/outputs/apk/debug/CoolblueGlimlachRun-debug.apk`.

## Browser / PWA Edition (`docs/`)
The [`docs/`](docs/) directory contains the zero-dependency, offline-installable HTML5 Canvas + Web Audio PWA (plus a single-file bundle at [`docs/standalone.html`](docs/standalone.html)):
- **GitHub Pages (1-Click Deployment):** Under **Settings → Pages → Build and deployment**, set **Source** to `Deploy from a branch`, select branch `main` and folder `/docs`. Once published, the game can be played in any mobile, tablet, or desktop browser and installed offline via *"Add to Home Screen"*.
- **Run locally:**
  ```bash
  python3 -m http.server 8080 --directory docs
  ```
