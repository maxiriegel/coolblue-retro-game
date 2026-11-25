# Coolblue Glimlach Run

Ein schnelles Super-Mario-Style Jump-'n'-Run als native Android-App (Kotlin + Hardware-accelerated Canvas + 8-Bit Chiptune PCM Synth), entwickelt für eine Live-Demo auf dem Googlebook für **Coolblue** mit **Max & Remi** als spielbaren Charakteren.

> *"Alles voor een glimlach!"*

## Features
- **Charakterauswahl:** Spiele als **Max** oder **Remi** im Coolblue-Outfit – der jeweils andere jubelt mit Sprechblase an der Ziellinie.
- **2 Themen-Levels (Niederländisch):**
  - `1-1 Coolblue Magazijn` – Hochregallager mit blauen Kartons, animierten Förderbändern, orangen Spring-Pads & Coolblue-Lieferwagen im Ziel.
  - `1-2 Grachtengordel Express` – Holländische Grachtenhäuser, Windmühle, Brücken, Dächer-Abkürzungen & Kundenhaus (*„BLIJE KLANT“*) mit Konfetti-Finale.
- **Mario-Mechaniken im Coolblue-Style:** Blaue & orange `?`-Kartons (Glimlachen & **Bakfiets Turbo-Schild**), „Trage Bezorging“-Schnecken & `1★`-Wolken durch Draufspringen in ein Lächeln verwandeln.
- **Googlebook-Steuerung & Demo-Komfort:**
  - `←` / `→` oder `A` / `D`: Rennen
  - `Leertaste` / `↑` / `W`: Springen (variable Sprunghöhe, Coyote Time & Jump Buffering)
  - `G` oder `⚡ Demo`: Demo-Modus (unverwundbar + Grachten-Rettung)
  - `1` / `2`: Direkt zu Level 1 oder 2 springen
  - `T`: Touch-Buttons ein-/ausblenden
  - `M`: Retro-Chiptune-Sound stummschalten
  - `R` / `Esc`: Neustart / Startmenü
- **Persistente Highscore-Liste:** Lokale *Top Bezorgers*-Rangliste (Max vs. Remi) via `SharedPreferences`.

## Build & Tests
```bash
./gradlew testDebugUnitTest assembleDebug
```
Das gebaute APK liegt anschließend unter `app/build/outputs/apk/debug/CoolblueGlimlachRun-debug.apk`.
