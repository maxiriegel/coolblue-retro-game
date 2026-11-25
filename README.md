# Coolblue Glimlach Run (Max, Remi & Vero Edition)

Ein schnelles Super-Mario-Style Jump-'n'-Run als **native Android-App** (Kotlin + Hardware-accelerated Canvas + 44.1 kHz Polyphonic Chiptune Synth) **und** identisch gepolishte **Browser-/PWA-Edition** (`docs/`), entwickelt für **Coolblue** mit **Max, Remi & Vero** als spielbaren Charakteren.

> *"Alles voor een glimlach!"*

## Features
- **3 Spielbare Charaktere:**
  - **Max** (*Express Bezorger*) – Coolblue-Cap mit orangem Schirm & weißem Glimlach-Emblem
  - **Remi** (*Glimlach Kampioen*) – Welliges dunkles Haar, markante Brille & orangene Schulterpartie
  - **Vero** (*Klantgeluk Ster*) – Kastanienbrauner mitschwingender Pferdeschwanz, oranges Haarband & Klantgeluk-Headset
  - Die **beiden jeweils anderen Teammitglieder** jubeln gemeinsam mit Sprechblase an der Ziellinie!
- **2 Themen-Levels (Niederländisch) mit Multi-Layer-Parallax:**
  - `1-1 Coolblue Magazijn` – Hochregallager mit Deckenstrahlern, automatisierter Mezzanine-Förderstrecke, blauen Kartons, animierten Förderbändern, orangen Spring-Pads & Coolblue-Lieferwagen im Ziel.
  - `1-2 Grachtengordel Express` – Holländische Grachtenhäuser (*Trap-*, *Klok-* & *Halsgevels* mit Tulpenkästen), Windmühlen, Grachten-Ente mit Coolblue-Cap, Backsteinbrücken, Dächer-Abkürzungen & Kundenhaus (*„BLIJE KLANT“*) mit Konfetti-Finale.
- **Arcade-Game-Feel & Chiptune-Sound:**
  - Squash-&-Stretch-Animationen, projizierte Bodenschatten, Staub- & Wasserspritzer-Partikel, Bakfiets-Turbo-Nachbilder & Luft-Combo-Multiplikator.
  - Polyphoner Chiptune-Synthesizer (Pulse 12.5%/25%, Triangle-Bass, LFSR-Percussion), aufsteigende Münz-Tonleitern, eigene Auswahl-Jingles für Max/Remi/Vero sowie zuschaltbarer Chiptune-Soundtrack je Szene.
- **Multi-Formfaktor-Design (Smartphone, Tablet, Foldable & Googlebook/Desktop):**
  - **Smartphone Hochformat (Portrait):** Vollwertiges vertikales Layout mit eigenem **Coolblue Arcade Control Deck** unterhalb des Spielfelds (Daumen verdecken nie das Geschehen).
  - **Smartphone Querformat (20:9 / 21:9 Ultrawide) & Tablets (4:3 / 16:10):** Dynamischer Edge-to-Edge-Viewport ohne schwarze Balken inkl. Safe-Area-/Cutout-Unterstützung.
- **Steuerung (Touch, Tastatur & Gamepad):**
  - `←` / `→` oder `A` / `D`: Rennen (bzw. Charakterwechsel im Startmenü)
  - `Leertaste` / `↑` / `W`: Springen (variable Sprunghöhe, Coyote Time & Jump Buffering)
  - `G` oder `⚡ Demo`: Demo-Modus (unverwundbar + Grachten-Rettung)
  - `1` / `2`: Direkt zu Level 1 oder 2 springen
  - `B`: Hintergrundmusik ein-/ausschalten
  - `M`: Gesamten Ton stummschalten
  - `T`: Touch-Buttons ein-/ausblenden
  - `F`: Vollbild (Browser-Edition)
  - `R` / `Esc`: Neustart / Startmenü

## Android-App bauen & testen
```bash
./gradlew testDebugUnitTest assembleDebug
```
Das gebaute APK liegt anschließend unter `app/build/outputs/apk/debug/CoolblueGlimlachRun-debug.apk`.

## Browser- / PWA-Release (`docs/`)
Im Ordner [`docs/`](docs/) liegt die eigenständige, installierbare HTML5-/Web-Audio-PWA ohne externe Abhängigkeiten:
- **GitHub Pages (1-Klick-Release):** In GitHub unter **Settings → Pages → Build and deployment** als Source `Deploy from a branch` → Branch `main` und Ordner `/docs` auswählen. Danach ist das Spiel sofort per Browser-Link auf Smartphone, Tablet und Desktop spielbar (und über *„Zum Home-Bildschirm hinzufügen“* auch offline als App installierbar).
- **Lokal im Browser starten:**
  ```bash
  python3 -m http.server 8080 --directory docs
  ```
