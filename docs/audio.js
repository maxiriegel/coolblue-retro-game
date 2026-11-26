// Coolblue Glimlach Run — Polyphonic Web Audio API Chiptune Synthesizer & Sequencer
(() => {
  class ChiptuneWebAudio {
    constructor() {
      this.ctx = null;
      this.isMuted = false;
      this.isMusicEnabled = true;
      this.scene = 'TITLE';
      this.pulse25 = null;
      this.pulse12 = null;
      this.noiseBuffer = null;
      this.musicTimer = null;
      this.stepIndex = 0;
      this.nextNoteTime = 0;

      this.scenes = {
        TITLE: {
          stepSec: 0.155,
          vol: 0.065,
          melody: [
            523.25, 659.25, 783.99, 659.25, 880.00, 783.99, 659.25, 587.33,
            523.25, 659.25, 783.99, 1046.50, 987.77, 880.00, 783.99, 0
          ],
          bass: [
            130.81, 0, 196.00, 0, 220.00, 0, 174.61, 0,
            130.81, 0, 196.00, 0, 146.83, 0, 196.00, 0
          ]
        },
        WAREHOUSE: {
          stepSec: 0.132,
          vol: 0.075,
          melody: [
            523.25, 0, 659.25, 783.99, 880.00, 783.99, 659.25, 523.25,
            587.33, 659.25, 783.99, 659.25, 523.25, 587.33, 659.25, 0,
            523.25, 0, 659.25, 783.99, 1046.50, 987.77, 880.00, 783.99,
            698.46, 783.99, 880.00, 783.99, 659.25, 587.33, 783.99, 0
          ],
          bass: [
            130.81, 130.81, 196.00, 196.00, 220.00, 220.00, 174.61, 174.61,
            146.83, 146.83, 196.00, 196.00, 130.81, 146.83, 164.81, 196.00,
            130.81, 130.81, 196.00, 196.00, 220.00, 220.00, 164.81, 164.81,
            174.61, 174.61, 196.00, 196.00, 130.81, 196.00, 130.81, 0
          ]
        },
        CANALS: {
          stepSec: 0.128,
          vol: 0.075,
          melody: [
            587.33, 739.99, 880.00, 0, 987.77, 880.00, 739.99, 659.25,
            587.33, 659.25, 739.99, 880.00, 739.99, 659.25, 587.33, 0,
            659.25, 783.99, 987.77, 1174.66, 1174.66, 987.77, 880.00, 739.99,
            659.25, 739.99, 880.00, 987.77, 880.00, 739.99, 880.00, 0
          ],
          bass: [
            146.83, 220.00, 146.83, 220.00, 196.00, 246.94, 164.81, 220.00,
            146.83, 220.00, 185.00, 220.00, 164.81, 220.00, 146.83, 0,
            164.81, 246.94, 196.00, 246.94, 220.00, 277.18, 185.00, 220.00,
            164.81, 220.00, 220.00, 164.81, 146.83, 220.00, 146.83, 0
          ]
        }
      };
    }

    unlock() {
      if (!this.ctx) {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return;
        this.ctx = new AudioCtx();
        this.pulse25 = this._createPulseWave(0.25);
        this.pulse12 = this._createPulseWave(0.125);
        this.noiseBuffer = this._createLfsrNoiseBuffer();
        this._startScheduler();
      }
      if (this.ctx && this.ctx.state === 'suspended') {
        this.ctx.resume().catch(() => {});
      }
    }

    _createPulseWave(duty) {
      const harmonics = 24;
      const real = new Float32Array(harmonics + 1);
      const imag = new Float32Array(harmonics + 1);
      for (let n = 1; n <= harmonics; n++) {
        imag[n] = (2 / (n * Math.PI)) * Math.sin(n * Math.PI * duty);
      }
      return this.ctx.createPeriodicWave(real, imag);
    }

    _createLfsrNoiseBuffer() {
      const sr = this.ctx.sampleRate;
      const len = Math.floor(sr * 0.6);
      const buf = this.ctx.createBuffer(1, len, sr);
      const data = buf.getChannelData(0);
      let lfsr = 0x5A31;
      for (let i = 0; i < len; i++) {
        if (i % 5 === 0) {
          const bit = ((lfsr ^ (lfsr >>> 1)) & 1);
          lfsr = (lfsr >>> 1) | (bit << 14);
        }
        data[i] = (lfsr & 1) ? 0.75 : -0.75;
      }
      return buf;
    }

    toggleMute() {
      this.isMuted = !this.isMuted;
      return this.isMuted;
    }

    toggleMusic() {
      this.isMusicEnabled = !this.isMusicEnabled;
      return this.isMusicEnabled;
    }

    setMusicScene(scene) {
      if (this.scene !== scene) {
        this.scene = scene;
        this.stepIndex = 0;
      }
    }

    _tone(startHz, endHz, startOffset, durSec, wave = 'pulse25', vol = 0.16) {
      if (!this.ctx || this.isMuted) return;
      const t0 = this.ctx.currentTime + startOffset;
      const t1 = t0 + durSec;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      if (wave === 'pulse25' && this.pulse25) {
        osc.setPeriodicWave(this.pulse25);
      } else if (wave === 'pulse12' && this.pulse12) {
        osc.setPeriodicWave(this.pulse12);
      } else {
        osc.type = wave === 'square' ? 'square' : 'triangle';
      }

      osc.frequency.setValueAtTime(Math.max(30, startHz), t0);
      if (Math.abs(endHz - startHz) > 0.5) {
        osc.frequency.linearRampToValueAtTime(Math.max(30, endHz), t1);
      }

      gain.gain.setValueAtTime(0.0001, t0);
      gain.gain.linearRampToValueAtTime(vol, t0 + Math.min(0.008, durSec * 0.25));
      gain.gain.exponentialRampToValueAtTime(0.0008, t1);

      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start(t0);
      osc.stop(t1 + 0.01);
    }

    _noise(startOffset, durSec, vol = 0.1, filterHz = 1800) {
      if (!this.ctx || !this.noiseBuffer || this.isMuted) return;
      const t0 = this.ctx.currentTime + startOffset;
      const t1 = t0 + durSec;
      const src = this.ctx.createBufferSource();
      src.buffer = this.noiseBuffer;

      const filter = this.ctx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(filterHz, t0);

      const gain = this.ctx.createGain();
      gain.gain.setValueAtTime(vol, t0);
      gain.gain.exponentialRampToValueAtTime(0.001, t1);

      src.connect(filter);
      filter.connect(gain);
      gain.connect(this.ctx.destination);
      src.start(t0);
      src.stop(t1 + 0.01);
    }

    playCoinStreak(streak = 0) {
      this.unlock();
      if (!this.ctx || this.isMuted) return;
      const ratio = Math.pow(2, Math.min(6, Math.max(0, streak)) / 12);
      const b5 = 987.77 * ratio;
      const e6 = 1318.51 * ratio;
      this._tone(b5, b5, 0, 0.055, 'pulse25', 0.16);
      this._tone(b5 * 0.5, b5 * 0.5, 0, 0.055, 'triangle', 0.11);
      this._tone(e6, e6, 0.052, 0.145, 'pulse25', 0.18);
      this._tone(e6 * 0.5, e6 * 0.5, 0.052, 0.145, 'triangle', 0.12);
    }

    playCharacterMotif(character) {
      this.unlock();
      if (!this.ctx || this.isMuted) return;
      if (character === 'MAX') {
        [523.25, 659.25, 783.99, 1046.50].forEach((f, i) => {
          this._tone(f, f, i * 0.05, i === 3 ? 0.12 : 0.05, 'pulse25', 0.14);
        });
      } else if (character === 'REMI') {
        [587.33, 739.99, 932.33, 1174.66].forEach((f, i) => {
          this._tone(f, f, i * 0.048, i === 3 ? 0.12 : 0.048, 'pulse12', 0.14);
        });
      } else {
        [659.25, 880.00, 1046.50, 1318.51].forEach((f, i) => {
          this._tone(f, i === 3 ? 1567.98 : f, i * 0.046, i === 3 ? 0.13 : 0.046, 'pulse25', 0.15);
        });
      }
    }

    play(effect) {
      this.unlock();
      if (!this.ctx || this.isMuted) return;
      switch (effect) {
        case 'JUMP':
          this._tone(235, 560, 0, 0.065, 'pulse25', 0.16);
          this._tone(560, 770, 0.06, 0.075, 'pulse25', 0.14);
          this._tone(145, 390, 0, 0.13, 'triangle', 0.14);
          break;
        case 'LAND':
          this._tone(150, 68, 0, 0.055, 'triangle', 0.12);
          this._noise(0, 0.04, 0.05, 900);
          break;
        case 'SMILE_COIN':
          this.playCoinStreak(0);
          break;
        case 'BOX_HIT':
          this._noise(0, 0.045, 0.1, 1400);
          this._tone(195, 430, 0, 0.048, 'pulse25', 0.17);
          this._tone(587.33, 783.99, 0.045, 0.075, 'triangle', 0.16);
          break;
        case 'BOX_EMPTY':
          this._noise(0, 0.055, 0.1, 700);
          this._tone(180, 105, 0, 0.065, 'triangle', 0.16);
          break;
        case 'POWERUP':
          [523.25, 659.25, 783.99, 987.77, 1046.50, 1318.51].forEach((f, i) => {
            this._tone(f, f, i * 0.052, i === 5 ? 0.16 : 0.055, 'pulse25', 0.16);
            this._tone(f * 0.5, f * 0.5, i * 0.052, i === 5 ? 0.16 : 0.055, 'triangle', 0.11);
          });
          break;
        case 'STOMP':
          this._noise(0, 0.035, 0.1, 1600);
          this._tone(440, 659.25, 0.02, 0.055, 'pulse25', 0.17);
          this._tone(880, 1174.66, 0.07, 0.09, 'pulse25', 0.16);
          break;
        case 'SPRING':
          this._tone(195, 980, 0, 0.19, 'pulse25', 0.17);
          this._tone(980, 1320, 0.18, 0.08, 'pulse12', 0.13);
          this._tone(130, 660, 0, 0.24, 'triangle', 0.15);
          break;
        case 'SHIELD_SAVE':
          this._noise(0, 0.06, 0.11, 2200);
          this._tone(880, 440, 0, 0.07, 'pulse12', 0.16);
          this._tone(659.25, 987.77, 0.065, 0.11, 'pulse25', 0.16);
          break;
        case 'HURT':
          this._noise(0, 0.09, 0.12, 950);
          this._tone(440, 293.66, 0, 0.07, 'pulse12', 0.18);
          this._tone(293.66, 196, 0.07, 0.075, 'pulse12', 0.17);
          this._tone(196, 123.47, 0.145, 0.11, 'square', 0.15);
          break;
        case 'SPLASH':
          this._noise(0, 0.12, 0.14, 1200);
          this._tone(320, 620, 0.03, 0.09, 'triangle', 0.12);
          break;
        case 'LEVEL_CLEAR':
          [
            [523.25, 0, 0.085],
            [659.25, 0.085, 0.085],
            [783.99, 0.17, 0.085],
            [1046.50, 0.255, 0.145],
            [880.00, 0.40, 0.085],
            [1046.50, 0.485, 0.28]
          ].forEach(([f, off, dur]) => {
            this._tone(f, f, off, dur, 'pulse25', 0.17);
            this._tone(f * 0.5, f * 0.5, off, dur, 'triangle', 0.12);
          });
          break;
        case 'VICTORY':
          [
            [523.25, 0, 0.08],
            [659.25, 0.08, 0.08],
            [783.99, 0.16, 0.08],
            [1046.50, 0.24, 0.13],
            [1174.66, 0.37, 0.095],
            [1318.51, 0.465, 0.115],
            [1567.98, 0.58, 0.34]
          ].forEach(([f, off, dur]) => {
            this._tone(f, f, off, dur, 'pulse25', 0.18);
            this._tone(f * 0.5, f * 0.5, off, dur, 'triangle', 0.12);
          });
          break;
        case 'GAME_OVER':
          [
            [523.25, 493.88, 0, 0.12],
            [466.16, 440.00, 0.12, 0.12],
            [392.00, 349.23, 0.24, 0.14],
            [329.63, 261.63, 0.38, 0.26]
          ].forEach(([s, e, off, dur]) => {
            this._tone(s, e, off, dur, 'pulse25', 0.16);
          });
          break;
        case 'SELECT':
        default:
          this._tone(659.25, 659.25, 0, 0.042, 'pulse25', 0.13);
          this._tone(987.77, 987.77, 0.04, 0.068, 'triangle', 0.14);
          break;
      }
    }

    _startScheduler() {
      if (this.musicTimer) return;
      this.nextNoteTime = this.ctx.currentTime + 0.05;
      this.musicTimer = setInterval(() => {
        if (!this.ctx || this.ctx.state !== 'running') return;
        if (this.isMuted || !this.isMusicEnabled || this.scene === 'NONE' || document.hidden) {
          this.nextNoteTime = this.ctx.currentTime + 0.08;
          return;
        }
        const cfg = this.scenes[this.scene];
        if (!cfg) return;

        while (this.nextNoteTime < this.ctx.currentTime + 0.18) {
          const idx = this.stepIndex % cfg.melody.length;
          const mFreq = cfg.melody[idx];
          const bFreq = cfg.bass[idx % cfg.bass.length];
          const offset = Math.max(0, this.nextNoteTime - this.ctx.currentTime);

          if (mFreq > 1) {
            this._tone(mFreq, mFreq, offset, cfg.stepSec * 0.78, 'pulse25', cfg.vol);
          }
          if (bFreq > 1) {
            this._tone(bFreq, bFreq, offset, cfg.stepSec * 0.85, 'triangle', cfg.vol * 0.95);
          }
          if (idx % 2 === 1) {
            this._noise(offset, 0.022, cfg.vol * 0.25, 4200);
          }
          this.nextNoteTime += cfg.stepSec;
          this.stepIndex++;
        }
      }, 60);
    }
  }

  window.CoolblueAudio = ChiptuneWebAudio;
})();
