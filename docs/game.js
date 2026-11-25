// Coolblue Glimlach Run — Browser / PWA Edition (Max, Remi & Vero)
(() => {
  'use strict';

  const CONFIG = {
    VIEW_WIDTH: 1280,
    VIEW_HEIGHT: 720,
    GROUND_Y: 604,
    GRAVITY: 2150,
    MAX_FALL_SPEED: 1100,
    BASE_RUN_SPEED: 415,
    SHIELD_RUN_SPEED: 495,
    GROUND_ACCEL: 2800,
    AIR_ACCEL: 1900,
    GROUND_FRICTION: 2600,
    AIR_FRICTION: 550,
    JUMP_VELOCITY: -790,
    SPRING_VELOCITY: -1140,
    STOMP_BOUNCE_VELOCITY: -620,
    STOMP_HIGH_BOUNCE_VELOCITY: -820,
    CONVEYOR_PUSH_SPEED: 195,
    COYOTE_TIME: 0.12,
    JUMP_BUFFER_TIME: 0.13,
    STARTING_HEARTS: 3,
    LEVEL_TIME_LIMIT: 120,
    TOTAL_LEVELS: 2
  };

  const CHARACTERS = {
    MAX: {
      id: 'MAX',
      displayName: 'Max',
      badgeTitle: 'Express Bezorger',
      cheerForHero: 'Top gedaan, Max!',
      accent: '#0090E3',
      ordinal: 0
    },
    REMI: {
      id: 'REMI',
      displayName: 'Remi',
      badgeTitle: 'Glimlach Kampioen',
      cheerForHero: 'Lekker bezig, Remi!',
      accent: '#FF6600',
      ordinal: 1
    },
    VERO: {
      id: 'VERO',
      displayName: 'Vero',
      badgeTitle: 'Klantgeluk Ster',
      cheerForHero: 'Super bezorgd, Vero!',
      accent: '#00A896',
      ordinal: 2
    }
  };
  const CHAR_ORDER = ['MAX', 'REMI', 'VERO'];

  function getTeammates(charId) {
    return CHAR_ORDER.filter((id) => id !== charId).map((id) => CHARACTERS[id]);
  }

  // =========================================================================
  // PERSISTENT SCORE REPOSITORY (localStorage)
  // =========================================================================
  const STORAGE_KEY = 'coolblue_glimlach_scores_v1';
  let memoryScores = [];

  const ScoreStore = {
    getTopScores(limit = 5) {
      let source = memoryScores;
      try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (raw) {
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed)) source = parsed;
        }
      } catch (_) {}
      return source
        .filter((e) => e && CHARACTERS[e.character] && typeof e.score === 'number')
        .sort((a, b) => (b.score - a.score) || (b.smiles - a.smiles))
        .slice(0, limit);
    },
    saveScore(entry) {
      const list = this.getTopScores(15);
      list.push(entry);
      list.sort((a, b) => (b.score - a.score) || (b.smiles - a.smiles));
      memoryScores = list.slice(0, 10);
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(memoryScores));
      } catch (_) {}
    }
  };

  // =========================================================================
  // LEVEL FACTORY (1-1 Coolblue Magazijn & 1-2 Grachtengordel Express)
  // =========================================================================
  function createLevel(levelNumber) {
    return levelNumber === 1 ? createWarehouseLevel() : createCanalsLevel();
  }

  function addCoinArc(coins, nextIdObj, startX, count, spacing, baseY, arcHeight, phaseStep = 0.65) {
    for (let i = 0; i < count; i++) {
      const frac = count <= 1 ? 0.5 : i / (count - 1);
      const curve = 4 * frac * (1 - frac);
      coins.push({
        id: nextIdObj.val++,
        x: startX + i * spacing,
        y: baseY - curve * arcHeight,
        radius: 18,
        collected: false,
        bobPhase: i * phaseStep
      });
    }
  }

  function createWarehouseLevel() {
    const g = CONFIG.GROUND_Y;
    let boxId = 1;
    let enemyId = 1;
    const coinId = { val: 1 };

    const platforms = [
      { x: 0, y: g, width: 1150, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 760, y: g - 68, width: 82, height: 68, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 1275, y: g, width: 725, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 1330, y: g - 20, width: 250, height: 20, type: 'CONVEYOR_RIGHT', springAnimTimer: 0 },
      { x: 1610, y: 466, width: 255, height: 28, type: 'PALLET', springAnimTimer: 0 },
      { x: 2125, y: g, width: 645, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 2185, y: g - 22, width: 84, height: 22, type: 'SPRING_PAD', springAnimTimer: 0 },
      { x: 2320, y: 355, width: 345, height: 28, type: 'PALLET', springAnimTimer: 0 },
      { x: 2595, y: g - 64, width: 76, height: 64, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 2790, y: 518, width: 130, height: 26, type: 'PALLET', springAnimTimer: 0 },
      { x: 2945, y: g, width: 710, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 3005, y: g - 20, width: 235, height: 20, type: 'CONVEYOR_RIGHT', springAnimTimer: 0 },
      { x: 3360, y: 472, width: 210, height: 28, type: 'PALLET', springAnimTimer: 0 },
      { x: 3720, y: g, width: 1220, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 3830, y: g - 56, width: 68, height: 56, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 3898, y: g - 112, width: 68, height: 112, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 3966, y: g - 168, width: 68, height: 168, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 4034, y: g - 224, width: 92, height: 224, type: 'PARCEL_STACK', springAnimTimer: 0 }
    ];

    const boxes = [
      { id: boxId++, x: 440, y: 442, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 565, y: 442, size: 54, reward: 'BAKFIETS_SHIELD', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 1710, y: 326, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 2465, y: 218, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 3265, y: 442, size: 54, reward: 'BAKFIETS_SHIELD', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 3440, y: 336, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 }
    ];

    const coins = [];
    addCoinArc(coins, coinId, 250, 4, 46, 520, 55);
    addCoinArc(coins, coinId, 735, 3, 52, 465, 45);
    addCoinArc(coins, coinId, 1155, 3, 52, 495, 55);
    addCoinArc(coins, coinId, 1365, 4, 52, 520, 20);
    addCoinArc(coins, coinId, 1640, 4, 55, 415, 15);
    addCoinArc(coins, coinId, 2355, 6, 52, 305, 25);
    addCoinArc(coins, coinId, 2815, 3, 44, 462, 22);
    addCoinArc(coins, coinId, 3035, 4, 52, 520, 18);
    addCoinArc(coins, coinId, 4165, 4, 52, 345, 55);

    const enemies = [
      { id: enemyId++, x: 920, y: g - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 860, maxX: 1100, vx: -72, baseY: g - 40, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 1720, y: g - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 1610, maxX: 1930, vx: -82, baseY: g - 40, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 1790, y: 380, width: 58, height: 42, type: 'ONE_STAR_CLOUD', minX: 1620, maxX: 1950, vx: 95, baseY: 380, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 2420, y: g - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 2295, maxX: 2560, vx: -78, baseY: g - 40, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 3300, y: 415, width: 58, height: 42, type: 'ONE_STAR_CLOUD', minX: 3120, maxX: 3520, vx: -100, baseY: 415, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 3450, y: g - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 3290, maxX: 3600, vx: -85, baseY: g - 40, alive: true, squashedTimer: 0, animTime: 0 }
    ];

    return {
      levelNumber: 1,
      code: '1-1',
      title: 'Coolblue Magazijn',
      subtitle: 'Vandaag besteld, morgen in huis!',
      theme: 'WAREHOUSE',
      worldWidth: 4940,
      spawnX: 95,
      spawnY: g - 70,
      flagX: 4420,
      flagGroundY: g,
      platforms,
      boxes,
      coins,
      enemies
    };
  }

  function createCanalsLevel() {
    const g = CONFIG.GROUND_Y;
    let boxId = 100;
    let enemyId = 100;
    const coinId = { val: 100 };

    const platforms = [
      { x: 0, y: g, width: 880, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 955, y: 536, width: 325, height: 32, type: 'BRICK_BRIDGE', springAnimTimer: 0 },
      { x: 1355, y: g, width: 670, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 1450, y: g - 22, width: 84, height: 22, type: 'SPRING_PAD', springAnimTimer: 0 },
      { x: 1595, y: 362, width: 330, height: 30, type: 'ROOFTOP', springAnimTimer: 0 },
      { x: 2115, y: 542, width: 195, height: 32, type: 'BRICK_BRIDGE', springAnimTimer: 0 },
      { x: 2405, y: 492, width: 195, height: 32, type: 'BRICK_BRIDGE', springAnimTimer: 0 },
      { x: 2690, y: g, width: 810, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 2755, y: g - 20, width: 285, height: 20, type: 'CONVEYOR_RIGHT', springAnimTimer: 0 },
      { x: 3365, y: g - 22, width: 84, height: 22, type: 'SPRING_PAD', springAnimTimer: 0 },
      { x: 3570, y: 418, width: 275, height: 30, type: 'ROOFTOP', springAnimTimer: 0 },
      { x: 3620, y: 548, width: 180, height: 30, type: 'BRICK_BRIDGE', springAnimTimer: 0 },
      { x: 3935, y: g, width: 1420, height: 140, type: 'GROUND', springAnimTimer: 0 },
      { x: 4220, y: g - 56, width: 68, height: 56, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 4288, y: g - 112, width: 68, height: 112, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 4356, y: g - 168, width: 68, height: 168, type: 'PARCEL_STACK', springAnimTimer: 0 },
      { x: 4424, y: g - 224, width: 92, height: 224, type: 'PARCEL_STACK', springAnimTimer: 0 }
    ];

    const boxes = [
      { id: boxId++, x: 410, y: 442, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 525, y: 442, size: 54, reward: 'BAKFIETS_SHIELD', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 1730, y: 222, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 2930, y: 438, size: 54, reward: 'BAKFIETS_SHIELD', isHit: false, bumpOffset: 0 },
      { id: boxId++, x: 3685, y: 276, size: 54, reward: 'SMILE_BURST', isHit: false, bumpOffset: 0 }
    ];

    const coins = [];
    addCoinArc(coins, coinId, 230, 3, 48, 520, 42, 0.7);
    addCoinArc(coins, coinId, 995, 5, 58, 475, 55, 0.7);
    addCoinArc(coins, coinId, 1625, 5, 58, 310, 22, 0.7);
    addCoinArc(coins, coinId, 2155, 3, 52, 482, 32, 0.7);
    addCoinArc(coins, coinId, 2445, 3, 52, 432, 32, 0.7);
    addCoinArc(coins, coinId, 2785, 5, 52, 520, 18, 0.7);
    addCoinArc(coins, coinId, 3595, 5, 52, 362, 28, 0.7);
    addCoinArc(coins, coinId, 4560, 4, 54, 345, 58, 0.7);

    const enemies = [
      { id: enemyId++, x: 1120, y: 536 - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 970, maxX: 1230, vx: -76, baseY: 496, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 1780, y: g - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 1600, maxX: 1960, vx: -84, baseY: g - 40, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 2290, y: 415, width: 58, height: 42, type: 'ONE_STAR_CLOUD', minX: 2120, maxX: 2520, vx: 98, baseY: 415, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 3150, y: g - 40, width: 52, height: 40, type: 'SLOW_SNAIL', minX: 3060, maxX: 3320, vx: -88, baseY: g - 40, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 3220, y: 390, width: 58, height: 42, type: 'ONE_STAR_CLOUD', minX: 3040, maxX: 3440, vx: -108, baseY: 390, alive: true, squashedTimer: 0, animTime: 0 },
      { id: enemyId++, x: 4070, y: 468, width: 58, height: 42, type: 'ONE_STAR_CLOUD', minX: 3960, maxX: 4200, vx: 92, baseY: 468, alive: true, squashedTimer: 0, animTime: 0 }
    ];

    return {
      levelNumber: 2,
      code: '1-2',
      title: 'Grachtengordel Express',
      subtitle: 'Alles voor een glimlach!',
      theme: 'CANALS',
      worldWidth: 5355,
      spawnX: 95,
      spawnY: g - 70,
      flagX: 4810,
      flagGroundY: g,
      platforms,
      boxes,
      coins,
      enemies
    };
  }

  // =========================================================================
  // GAME ENGINE
  // =========================================================================
  function clamp(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  function rectsOverlap(ax, ay, aw, ah, bx, by, bw, bh) {
    return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
  }

  class GameEngine {
    constructor(audio) {
      this.audio = audio;
      this.screen = 'TITLE';
      this.selectedCharacter = 'MAX';
      this.currentLevelNumber = 1;
      this.level = createLevel(1);
      this.player = this._makePlayer(95, CONFIG.GROUND_Y - 70, false);
      this.hearts = CONFIG.STARTING_HEARTS;
      this.score = 0;
      this.smilesCollected = 0;
      this.enemiesSmiled = 0;
      this.timeRemaining = CONFIG.LEVEL_TIME_LIMIT;
      this.totalElapsedSeconds = 0;
      this.cameraX = 0;
      this.viewportWidth = CONFIG.VIEW_WIDTH;
      // Default touch controls on for touch-capable devices, toggleable anytime
      this.touchControlsVisible = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0) || window.innerWidth < 1024;
      this.demoModeEnabled = false;
      this.worldTime = 0;
      this.screenShakeTimer = 0;
      this.bannerMessage = null;
      this.bannerTimer = 0;
      this.flagSlideProgress = 0;
      this.levelClearDelayTimer = 0;
      this.runScoreSaved = false;

      this.powerUps = [];
      this.particles = [];
      this.trailGhosts = [];
      this.floatingTexts = [];
      this.confetti = [];

      this.lastJumpHeld = false;
      this.trailSpawnTimer = 0;
      this.coinStreak = 0;
      this.coinStreakTimer = 0;

      this._syncMusic();
    }

    _makePlayer(x, y, shield) {
      return {
        x, y,
        width: 44, height: 62,
        vx: 0, vy: 0,
        facingRight: true,
        onGround: false,
        coyoteTimer: 0,
        jumpBufferTimer: 0,
        hasBakfietsShield: shield,
        invulnTimer: 0,
        animTime: 0,
        lastSafeX: x,
        lastSafeY: y,
        squashX: 1,
        stretchY: 1,
        runDustTimer: 0,
        airCombo: 0
      };
    }

    get hero() {
      return CHARACTERS[this.selectedCharacter];
    }

    selectCharacter(id) {
      if (!CHARACTERS[id]) return;
      this.selectedCharacter = id;
      this.audio.playCharacterMotif(id);
    }

    cycleCharacter(delta) {
      const idx = CHAR_ORDER.indexOf(this.selectedCharacter);
      const nextIdx = (idx + delta + CHAR_ORDER.length) % CHAR_ORDER.length;
      this.selectCharacter(CHAR_ORDER[nextIdx]);
    }

    toggleDemoMode() {
      this.demoModeEnabled = !this.demoModeEnabled;
      this.showBanner(this.demoModeEnabled ? 'Demo-modus AAN (Onkwetsbaar!)' : 'Demo-modus UIT');
      this.audio.play('SELECT');
    }

    toggleTouchControls() {
      this.touchControlsVisible = !this.touchControlsVisible;
      this.audio.play('SELECT');
    }

    toggleMute() {
      const muted = this.audio.toggleMute();
      if (!muted) this.audio.play('SELECT');
    }

    toggleMusic() {
      const on = this.audio.toggleMusic();
      this.showBanner(on ? '🎵 Chiptune-muziek AAN' : '🎵 Chiptune-muziek UIT');
      this.audio.play('SELECT');
    }

    returnToTitle() {
      this.screen = 'TITLE';
      this.confetti = [];
      this.particles = [];
      this.trailGhosts = [];
      this.floatingTexts = [];
      this._syncMusic();
      this.audio.play('SELECT');
    }

    startNewGame(startLevel = 1) {
      this.score = 0;
      this.smilesCollected = 0;
      this.enemiesSmiled = 0;
      this.hearts = CONFIG.STARTING_HEARTS;
      this.totalElapsedSeconds = 0;
      this.runScoreSaved = false;
      this._loadLevel(clamp(startLevel, 1, CONFIG.TOTAL_LEVELS), false);
      this.screen = 'PLAYING';
      this._syncMusic();
      this.audio.play('SELECT');
    }

    jumpToLevel(lvl) {
      const target = clamp(lvl, 1, CONFIG.TOTAL_LEVELS);
      if (this.screen === 'TITLE' || this.screen === 'GAME_OVER' || this.screen === 'VICTORY') {
        this.startNewGame(target);
      } else {
        this._loadLevel(target, this.player.hasBakfietsShield);
        this.screen = 'PLAYING';
        this._syncMusic();
        this.audio.play('SELECT');
      }
    }

    restartCurrentLevel() {
      if (this.screen === 'GAME_OVER' || this.screen === 'VICTORY') {
        this.startNewGame(this.screen === 'VICTORY' ? 1 : this.currentLevelNumber);
      } else {
        this.hearts = CONFIG.STARTING_HEARTS;
        this._loadLevel(this.currentLevelNumber, false);
        this.screen = 'PLAYING';
        this._syncMusic();
        this.audio.play('SELECT');
      }
    }

    advanceFromLevelClear() {
      if (this.screen !== 'LEVEL_CLEAR') return;
      if (this.currentLevelNumber < CONFIG.TOTAL_LEVELS) {
        this._loadLevel(this.currentLevelNumber + 1, this.player.hasBakfietsShield);
        this.screen = 'PLAYING';
        this._syncMusic();
        this.audio.play('SELECT');
      } else {
        this._enterVictory();
      }
    }

    _loadLevel(levelNumber, preserveShield) {
      this.currentLevelNumber = levelNumber;
      this.level = createLevel(levelNumber);
      this.player = this._makePlayer(this.level.spawnX, this.level.spawnY, preserveShield);
      this.timeRemaining = CONFIG.LEVEL_TIME_LIMIT;
      this.cameraX = 0;
      this.flagSlideProgress = 0;
      this.levelClearDelayTimer = 0;
      this.coinStreak = 0;
      this.coinStreakTimer = 0;
      this.trailSpawnTimer = 0;
      this.powerUps = [];
      this.particles = [];
      this.trailGhosts = [];
      this.floatingTexts = [];
      this.confetti = [];
      this.lastJumpHeld = false;
      this.showBanner(`${this.level.code} ${this.level.title} — ${this.level.subtitle}`);
    }

    _syncMusic() {
      if (this.screen === 'TITLE') {
        this.audio.setMusicScene('TITLE');
      } else if (this.screen === 'PLAYING') {
        this.audio.setMusicScene(this.level.theme === 'WAREHOUSE' ? 'WAREHOUSE' : 'CANALS');
      } else {
        this.audio.setMusicScene('NONE');
      }
    }

    showBanner(msg) {
      this.bannerMessage = msg;
      this.bannerTimer = 3.2;
    }

    update(rawDt, input) {
      const dt = clamp(rawDt, 0, 0.05);
      this.worldTime += dt;

      if (this.bannerTimer > 0) {
        this.bannerTimer = Math.max(0, this.bannerTimer - dt);
        if (this.bannerTimer === 0) this.bannerMessage = null;
      }
      if (this.screenShakeTimer > 0) {
        this.screenShakeTimer = Math.max(0, this.screenShakeTimer - dt);
      }
      if (this.coinStreakTimer > 0) {
        this.coinStreakTimer = Math.max(0, this.coinStreakTimer - dt);
        if (this.coinStreakTimer === 0) this.coinStreak = 0;
      }

      this._updateParticles(dt);
      this._updateTrailGhosts(dt);
      this._updateFloatingTexts(dt);
      this._updateConfetti(dt);

      if (this.screen === 'PLAYING') {
        this._updatePlaying(dt, input);
      } else if (this.screen === 'LEVEL_CLEAR') {
        this.flagSlideProgress = Math.min(1, this.flagSlideProgress + dt * 1.4);
        this.levelClearDelayTimer += dt;
        if (this.confetti.length < 95 && this.levelClearDelayTimer < 2.2) {
          this._spawnConfettiBurst(this.level.flagX, 240, 4);
        }
      } else if (this.screen === 'VICTORY') {
        this.flagSlideProgress = 1;
        if (this.confetti.length < 120) {
          this._spawnConfettiBurst(this.cameraX + Math.random() * this.viewportWidth, -10, 2);
        }
      }
    }

    _updatePlaying(dt, input) {
      const p = this.player;
      this.totalElapsedSeconds += dt;
      this.timeRemaining = Math.max(0, this.timeRemaining - dt);
      if (this.timeRemaining <= 0 && !this.demoModeEnabled) {
        this._triggerGameOver();
        return;
      }

      p.squashX += (1 - p.squashX) * Math.min(1, dt * 12);
      p.stretchY += (1 - p.stretchY) * Math.min(1, dt * 12);

      for (const plat of this.level.platforms) {
        if (plat.springAnimTimer > 0) plat.springAnimTimer = Math.max(0, plat.springAnimTimer - dt);
      }
      for (const box of this.level.boxes) {
        if (box.bumpOffset > 0) box.bumpOffset = Math.max(0, box.bumpOffset - dt * 95);
      }

      if (p.invulnTimer > 0) p.invulnTimer = Math.max(0, p.invulnTimer - dt);
      if (input.jumpJustPressed) {
        p.jumpBufferTimer = CONFIG.JUMP_BUFFER_TIME;
      } else if (p.jumpBufferTimer > 0) {
        p.jumpBufferTimer = Math.max(0, p.jumpBufferTimer - dt);
      }

      if (p.onGround) {
        p.coyoteTimer = CONFIG.COYOTE_TIME;
      } else if (p.coyoteTimer > 0) {
        p.coyoteTimer = Math.max(0, p.coyoteTimer - dt);
      }

      const maxSpeed = p.hasBakfietsShield ? CONFIG.SHIELD_RUN_SPEED : CONFIG.BASE_RUN_SPEED;
      const accel = p.onGround ? CONFIG.GROUND_ACCEL : CONFIG.AIR_ACCEL;
      const friction = p.onGround ? CONFIG.GROUND_FRICTION : CONFIG.AIR_FRICTION;

      const moveDir = (input.left && !input.right) ? -1 : (input.right && !input.left) ? 1 : 0;
      if (moveDir !== 0) {
        p.facingRight = moveDir > 0;
        p.vx = clamp(p.vx + moveDir * accel * dt, -maxSpeed, maxSpeed);
      } else {
        const decel = friction * dt;
        if (Math.abs(p.vx) <= decel) p.vx = 0;
        else p.vx -= Math.sign(p.vx) * decel;
      }

      if (p.jumpBufferTimer > 0 && p.coyoteTimer > 0) {
        p.vy = CONFIG.JUMP_VELOCITY;
        p.onGround = false;
        p.coyoteTimer = 0;
        p.jumpBufferTimer = 0;
        p.squashX = 0.78;
        p.stretchY = 1.25;
        this.audio.play('JUMP');
        this._spawnDustBurst(p.x + p.width / 2, p.y + p.height, 7);
      }

      if (!input.jumpHeld && this.lastJumpHeld && p.vy < -280) {
        p.vy *= 0.56;
      }
      this.lastJumpHeld = input.jumpHeld;

      p.vy = Math.min(CONFIG.MAX_FALL_SPEED, p.vy + CONFIG.GRAVITY * dt);

      const conveyorPush = this._computeConveyorVelocity();
      const prevX = p.x;
      p.x = clamp(p.x + (p.vx + conveyorPush) * dt, 0, this.level.worldWidth - p.width);
      this._resolveHorizontalCollisions(prevX);

      const wasOnGround = p.onGround;
      const preFallVy = p.vy;
      const prevBottom = p.y + p.height;
      const prevTop = p.y;
      p.y += p.vy * dt;
      p.onGround = false;
      this._resolveVerticalCollisions(prevTop, prevBottom);

      if (!wasOnGround && p.onGround) {
        p.airCombo = 0;
        if (preFallVy > 250) {
          const intensity = clamp((preFallVy - 250) / 700, 0.15, 1);
          p.squashX = 1 + 0.24 * intensity;
          p.stretchY = 1 - 0.20 * intensity;
          this._spawnDustBurst(p.x + p.width / 2, p.y + p.height, Math.floor(4 + intensity * 5));
          if (preFallVy > 420) this.audio.play('LAND');
        }
      }

      if (p.onGround && Math.abs(p.vx) > 20) {
        p.animTime += dt * (Math.abs(p.vx) / 88);
        if (Math.abs(p.vx) > 290) {
          p.runDustTimer -= dt;
          if (p.runDustTimer <= 0) {
            p.runDustTimer = 0.11;
            const footX = p.facingRight ? p.x + 8 : p.x + p.width - 8;
            this.particles.push({
              x: footX,
              y: p.y + p.height - 3,
              vx: -p.vx * 0.22 + (Math.random() - 0.5) * 30,
              vy: -25 - Math.random() * 35,
              life: 0.26, maxLife: 0.26,
              size: 7 + Math.random() * 4,
              color: '#FFFFFF',
              shape: 'DUST', rotation: 0, vRot: 0
            });
          }
        }
      } else if (!p.onGround) {
        p.animTime += dt * 3;
      }

      if ((p.hasBakfietsShield || this.demoModeEnabled) && (Math.abs(p.vx) > 310 || Math.abs(p.vy) > 520)) {
        this.trailSpawnTimer -= dt;
        if (this.trailSpawnTimer <= 0) {
          this.trailSpawnTimer = 0.045;
          this.trailGhosts.push({ x: p.x, y: p.y, life: 0.18, maxLife: 0.18 });
        }
      }

      if (p.y > CONFIG.VIEW_HEIGHT + 70) {
        this._handlePitFall();
        if (this.screen !== 'PLAYING') return;
      }

      this._updateCoins();
      this._updatePowerUps(dt);
      this._updateEnemies(dt, input.jumpHeld);
      if (this.screen !== 'PLAYING') return;

      if (p.x + p.width >= this.level.flagX - 10) {
        this._triggerLevelClear();
        return;
      }

      const lookAhead = this.viewportWidth < 980 ? 0.32 : 0.36;
      const targetCamX = clamp(
        p.x - this.viewportWidth * lookAhead,
        0,
        Math.max(0, this.level.worldWidth - this.viewportWidth)
      );
      this.cameraX += (targetCamX - this.cameraX) * Math.min(1, dt * 8.5);
    }

    _computeConveyorVelocity() {
      const p = this.player;
      if (!p.onGround) return 0;
      const feetY = p.y + p.height;
      for (const plat of this.level.platforms) {
        if (plat.type !== 'CONVEYOR_RIGHT' && plat.type !== 'CONVEYOR_LEFT') continue;
        if (p.x + p.width > plat.x + 2 && p.x < plat.x + plat.width - 2 && Math.abs(feetY - plat.y) <= 6) {
          return plat.type === 'CONVEYOR_RIGHT' ? CONFIG.CONVEYOR_PUSH_SPEED : -CONFIG.CONVEYOR_PUSH_SPEED;
        }
      }
      return 0;
    }

    _isFullSolid(type) {
      return type === 'GROUND' || type === 'PARCEL_STACK';
    }

    _resolveHorizontalCollisions(prevX) {
      const p = this.player;
      for (const plat of this.level.platforms) {
        if (!this._isFullSolid(plat.type)) continue;
        if (rectsOverlap(p.x, p.y + 4, p.width, p.height - 8, plat.x, plat.y, plat.width, plat.height)) {
          if (prevX + p.width <= plat.x + 6) { p.x = plat.x - p.width; p.vx = 0; }
          else if (prevX >= plat.x + plat.width - 6) { p.x = plat.x + plat.width; p.vx = 0; }
        }
      }
      for (const box of this.level.boxes) {
        if (rectsOverlap(p.x, p.y + 4, p.width, p.height - 8, box.x, box.y, box.size, box.size)) {
          if (prevX + p.width <= box.x + 6) { p.x = box.x - p.width; p.vx = 0; }
          else if (prevX >= box.x + box.size - 6) { p.x = box.x + box.size; p.vx = 0; }
        }
      }
    }

    _resolveVerticalCollisions(prevTop, prevBottom) {
      const p = this.player;
      for (const plat of this.level.platforms) {
        const horiz = p.x + p.width > plat.x + 4 && p.x < plat.x + plat.width - 4;
        if (!horiz) continue;
        if (p.vy >= 0 && prevBottom <= plat.y + 18 && p.y + p.height >= plat.y) {
          p.y = plat.y - p.height;
          if (plat.type === 'SPRING_PAD') {
            p.vy = CONFIG.SPRING_VELOCITY;
            p.onGround = false;
            p.squashX = 0.70;
            p.stretchY = 1.35;
            plat.springAnimTimer = 0.38;
            this.audio.play('SPRING');
            this._spawnSmileSparkles(plat.x + plat.width / 2, plat.y, 12, '#FF6600', false, true);
            this.floatingTexts.push({ x: plat.x + plat.width / 2, y: plat.y - 18, text: 'BOING!', color: '#FF6600', life: 0.75, maxLife: 0.75, scale: 1.1 });
          } else {
            p.vy = 0;
            p.onGround = true;
            if (plat.type === 'GROUND' || plat.type === 'BRICK_BRIDGE' || plat.type === 'PALLET') {
              p.lastSafeX = clamp(p.x, plat.x + 12, Math.max(plat.x + 12, plat.x + plat.width - p.width - 12));
              p.lastSafeY = plat.y - p.height - 4;
            }
          }
        } else if (this._isFullSolid(plat.type) && p.vy < 0 && prevTop >= plat.y + plat.height - 18 && p.y <= plat.y + plat.height) {
          p.y = plat.y + plat.height;
          p.vy = 0;
        }
      }

      for (const box of this.level.boxes) {
        const horiz = p.x + p.width > box.x + 4 && p.x < box.x + box.size - 4;
        if (!horiz) continue;
        if (p.vy >= 0 && prevBottom <= box.y + 18 && p.y + p.height >= box.y) {
          p.y = box.y - p.height;
          p.vy = 0;
          p.onGround = true;
        } else if (p.vy < 0 && prevTop >= box.y + box.size - 22 && p.y <= box.y + box.size) {
          p.y = box.y + box.size;
          p.vy = 60;
          p.squashX = 1.16;
          p.stretchY = 0.86;
          this._triggerQuestionBox(box);
        }
      }
    }

    _triggerQuestionBox(box) {
      if (box.isHit) {
        box.bumpOffset = 6;
        this.audio.play('BOX_EMPTY');
        return;
      }
      box.isHit = true;
      box.bumpOffset = 20;
      this.audio.play('BOX_HIT');

      if (box.reward === 'SMILE_BURST') {
        this.smilesCollected += 3;
        this.score += 300;
        this.audio.playCoinStreak(2);
        this._spawnSmileSparkles(box.x + box.size / 2, box.y - 10, 14, '#FF9900', true, true);
        this.floatingTexts.push({ x: box.x + box.size / 2, y: box.y - 24, text: '+3 Glimlachen!', color: '#FF6600', life: 1.0, maxLife: 1.0, scale: 1.0 });
      } else {
        this.powerUps.push({ x: box.x + 4, y: box.y - 42, width: 48, height: 38, vx: 135, vy: -260 });
        this._spawnSmileSparkles(box.x + box.size / 2, box.y - 14, 12, '#0090E3', false, true);
        this.floatingTexts.push({ x: box.x + box.size / 2, y: box.y - 24, text: 'Coolblue Bakfiets!', color: '#0090E3', life: 1.05, maxLife: 1.05, scale: 1.1 });
      }
    }

    _updateCoins() {
      const p = this.player;
      const pCx = p.x + p.width / 2;
      const pCy = p.y + p.height / 2;
      for (const coin of this.level.coins) {
        if (coin.collected) continue;
        const dx = pCx - coin.x;
        const dy = pCy - coin.y;
        const hitDist = coin.radius + 26;
        if (dx * dx + dy * dy <= hitDist * hitDist) {
          coin.collected = true;
          this.smilesCollected += 1;
          this.score += 100;
          this.audio.playCoinStreak(this.coinStreak);
          this.coinStreak = Math.min(6, this.coinStreak + 1);
          this.coinStreakTimer = 0.85;
          this._spawnSmileSparkles(coin.x, coin.y, 8, '#FFAA00', true, true);
          this.floatingTexts.push({ x: coin.x, y: coin.y - 14, text: '+100', color: '#FF6600', life: 0.7, maxLife: 0.7, scale: 1.0 });
        }
      }
    }

    _updatePowerUps(dt) {
      const p = this.player;
      for (let i = this.powerUps.length - 1; i >= 0; i--) {
        const item = this.powerUps[i];
        item.vy = Math.min(CONFIG.MAX_FALL_SPEED, item.vy + CONFIG.GRAVITY * 0.7 * dt);
        const prevBottom = item.y + item.height;
        item.x += item.vx * dt;
        item.y += item.vy * dt;

        for (const plat of this.level.platforms) {
          const horiz = item.x + item.width > plat.x && item.x < plat.x + plat.width;
          if (horiz && item.vy >= 0 && prevBottom <= plat.y + 18 && item.y + item.height >= plat.y) {
            item.y = plat.y - item.height;
            item.vy = 0;
          }
        }
        if (item.x <= 0 || item.x + item.width >= this.level.worldWidth) item.vx = -item.vx;

        if (rectsOverlap(p.x, p.y, p.width, p.height, item.x, item.y, item.width, item.height)) {
          p.hasBakfietsShield = true;
          p.squashX = 0.85;
          p.stretchY = 1.18;
          this.score += 500;
          this.audio.play('POWERUP');
          this._spawnSmileSparkles(p.x + p.width / 2, p.y + 20, 18, '#0090E3', true, true);
          this.floatingTexts.push({ x: p.x + p.width / 2, y: p.y - 26, text: 'BAKFIETS TURBO!', color: '#0090E3', life: 1.15, maxLife: 1.15, scale: 1.15 });
          this.powerUps.splice(i, 1);
        } else if (item.y > CONFIG.VIEW_HEIGHT + 120) {
          this.powerUps.splice(i, 1);
        }
      }
    }

    _updateEnemies(dt, jumpHeld) {
      const p = this.player;
      for (const enemy of this.level.enemies) {
        enemy.animTime += dt;
        if (!enemy.alive) {
          if (enemy.squashedTimer > 0) enemy.squashedTimer = Math.max(0, enemy.squashedTimer - dt);
          continue;
        }
        enemy.x += enemy.vx * dt;
        if (enemy.x <= enemy.minX) { enemy.x = enemy.minX; enemy.vx = Math.abs(enemy.vx); }
        else if (enemy.x + enemy.width >= enemy.maxX) { enemy.x = enemy.maxX - enemy.width; enemy.vx = -Math.abs(enemy.vx); }
        if (enemy.type === 'ONE_STAR_CLOUD') {
          enemy.y = enemy.baseY + Math.sin(enemy.animTime * 3.2) * 18;
        }

        if (rectsOverlap(p.x + 4, p.y + 4, p.width - 8, p.height - 4, enemy.x + 3, enemy.y + 3, enemy.width - 6, enemy.height - 6)) {
          const playerFeet = p.y + p.height;
          const stompZone = enemy.y + enemy.height * 0.62;
          const isStomp = p.vy > 0 && playerFeet <= stompZone + 14;
          if (isStomp || this.demoModeEnabled) {
            enemy.alive = false;
            enemy.squashedTimer = 0.65;
            this.enemiesSmiled += 1;
            p.airCombo += 1;
            const mult = Math.min(4, p.airCombo);
            const pts = (enemy.type === 'ONE_STAR_CLOUD' ? 300 : 200) * mult;
            this.score += pts;
            p.vy = jumpHeld ? CONFIG.STOMP_HIGH_BOUNCE_VELOCITY : CONFIG.STOMP_BOUNCE_VELOCITY;
            p.onGround = false;
            p.squashX = 1.22;
            p.stretchY = 0.80;
            this.audio.play('STOMP');
            this._spawnSmileSparkles(enemy.x + enemy.width / 2, enemy.y + enemy.height / 2, 16, '#FF6600', true, true);
            const prefix = mult >= 2 ? `${mult}x COMBO! ` : '';
            const msg = enemy.type === 'ONE_STAR_CLOUD' ? `${prefix}5 STERREN! +${pts}` : `${prefix}BLIJ! +${pts}`;
            this.floatingTexts.push({ x: enemy.x + enemy.width / 2, y: enemy.y - 16, text: msg, color: '#0090E3', life: 1.05, maxLife: 1.05, scale: mult >= 2 ? 1.15 : 1.0 });
          } else {
            this._damagePlayer();
            if (this.screen !== 'PLAYING') return;
          }
        }
      }
    }

    _damagePlayer() {
      const p = this.player;
      if (p.invulnTimer > 0 || this.demoModeEnabled) return;
      this.screenShakeTimer = 0.25;
      if (p.hasBakfietsShield) {
        p.hasBakfietsShield = false;
        p.invulnTimer = 1.6;
        p.vy = -420;
        p.squashX = 1.2;
        p.stretchY = 0.82;
        this.audio.play('SHIELD_SAVE');
        this._spawnSmileSparkles(p.x + p.width / 2, p.y + 24, 14, '#0090E3', false, true);
        this.floatingTexts.push({ x: p.x + p.width / 2, y: p.y - 20, text: 'Schild gered!', color: '#0090E3', life: 0.95, maxLife: 0.95, scale: 1 });
        return;
      }
      this.hearts -= 1;
      this.audio.play('HURT');
      if (this.hearts <= 0) {
        this._triggerGameOver();
      } else {
        p.invulnTimer = 1.8;
        p.vy = -460;
        p.vx = p.facingRight ? -220 : 220;
        p.squashX = 0.82;
        p.stretchY = 1.20;
        this.floatingTexts.push({ x: p.x + p.width / 2, y: p.y - 20, text: 'Oeps!', color: '#E53935', life: 0.85, maxLife: 0.85, scale: 1 });
      }
    }

    _handlePitFall() {
      const p = this.player;
      if (this.level.theme === 'CANALS') {
        this._spawnWaterSplash(p.x + p.width / 2, CONFIG.GROUND_Y + 26, 14);
        this.audio.play('SPLASH');
      }
      if (this.demoModeEnabled) {
        p.x = p.lastSafeX;
        p.y = p.lastSafeY - 30;
        p.vx = 0;
        p.vy = -520;
        p.squashX = 0.78;
        p.stretchY = 1.26;
        this.audio.play('SPRING');
        this.floatingTexts.push({ x: p.x + p.width / 2, y: p.y - 20, text: 'Demo Reddingsactie!', color: '#0090E3', life: 0.95, maxLife: 0.95, scale: 1 });
        return;
      }
      this.hearts -= 1;
      this.audio.play('HURT');
      this.screenShakeTimer = 0.3;
      if (this.hearts <= 0) {
        this._triggerGameOver();
      } else {
        p.x = p.lastSafeX;
        p.y = p.lastSafeY;
        p.vx = 0;
        p.vy = -220;
        p.invulnTimer = 1.8;
        this.floatingTexts.push({ x: p.x + p.width / 2, y: p.y - 18, text: 'Plons! -1 ♥', color: '#E53935', life: 0.95, maxLife: 0.95, scale: 1 });
      }
    }

    _triggerLevelClear() {
      const p = this.player;
      const timeBonus = Math.max(0, Math.floor(this.timeRemaining) * 10);
      const heightBonus = Math.floor(clamp(this.level.flagGroundY - p.y, 0, 320) / 2) * 5;
      this.score += 1000 + timeBonus + heightBonus;
      p.x = this.level.flagX - 18;
      p.vx = 0;
      p.vy = 0;
      this.trailGhosts = [];
      this._spawnConfettiBurst(this.level.flagX, 260, 65);
      this.floatingTexts.push({
        x: this.level.flagX, y: 220,
        text: `PAKKET BEZORGD! +${1000 + timeBonus + heightBonus}`,
        color: '#FF6600', life: 1.6, maxLife: 1.6, scale: 1.15
      });

      if (this.currentLevelNumber >= CONFIG.TOTAL_LEVELS) {
        this.audio.play('VICTORY');
        this._enterVictory();
      } else {
        this.audio.play('LEVEL_CLEAR');
        this.screen = 'LEVEL_CLEAR';
        this.levelClearDelayTimer = 0;
        this._syncMusic();
      }
    }

    _enterVictory() {
      this.screen = 'VICTORY';
      this._syncMusic();
      this._saveScore(CONFIG.TOTAL_LEVELS);
      this._spawnConfettiBurst(this.level.flagX, 180, 90);
    }

    _triggerGameOver() {
      this.screen = 'GAME_OVER';
      this._syncMusic();
      this.audio.play('GAME_OVER');
      this._saveScore(this.currentLevelNumber);
    }

    _saveScore(levelReached) {
      if (this.runScoreSaved || this.score <= 0) return;
      this.runScoreSaved = true;
      ScoreStore.saveScore({
        character: this.selectedCharacter,
        score: this.score,
        smiles: this.smilesCollected,
        levelReached,
        timeSeconds: Math.max(1, Math.floor(this.totalElapsedSeconds)),
        timestampMs: Date.now()
      });
    }

    _spawnDustBurst(x, y, count) {
      for (let i = 0; i < count; i++) {
        const angle = -0.25 - Math.random() * 2.6;
        const speed = 55 + Math.random() * 115;
        this.particles.push({
          x, y,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          life: 0.36, maxLife: 0.36,
          size: 7 + Math.random() * 5,
          color: '#FFFFFF', shape: 'DUST', rotation: 0, vRot: 0
        });
      }
    }

    _spawnWaterSplash(x, y, count) {
      for (let i = 0; i < count; i++) {
        const angle = -0.35 - Math.random() * 2.4;
        const speed = 130 + Math.random() * 220;
        this.particles.push({
          x: x + (Math.random() - 0.5) * 28, y,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed - 60,
          life: 0.55, maxLife: 0.55,
          size: 7 + Math.random() * 6,
          color: i % 3 === 0 ? '#FFFFFF' : '#4FC3F7',
          shape: 'SPLASH', rotation: 0, vRot: 0
        });
      }
    }

    _spawnSmileSparkles(x, y, count, primaryColor, includeSmiles = false, includeStars = false) {
      for (let i = 0; i < count; i++) {
        const angle = Math.random() * Math.PI * 2;
        const speed = 90 + Math.random() * 220;
        const color = i % 4 === 0 ? '#FFFFFF' : (i % 3 === 0 ? '#0090E3' : primaryColor);
        const isSmile = includeSmiles && i < 2;
        const shape = isSmile ? 'SMILE' : (includeStars && i % 2 === 0 ? 'STAR' : 'SPARK');
        this.particles.push({
          x, y,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed - 95,
          life: 0.68, maxLife: 0.68,
          size: isSmile ? 18 : 8 + Math.random() * 6,
          color, shape,
          rotation: Math.random() * Math.PI * 2,
          vRot: (Math.random() - 0.5) * 6
        });
      }
    }

    _spawnConfettiBurst(originX, originY, count) {
      const palette = ['#0090E3', '#FF6600', '#FFFFFF', '#FFD54F', '#00A896', '#29B6F6'];
      for (let i = 0; i < count; i++) {
        this.confetti.push({
          x: originX + (Math.random() - 0.5) * 280,
          y: originY + (Math.random() - 0.5) * 120,
          vx: (Math.random() - 0.5) * 330,
          vy: -130 - Math.random() * 250,
          rotation: Math.random() * Math.PI * 2,
          vRot: (Math.random() - 0.5) * 8,
          width: 10 + Math.random() * 8,
          height: 6 + Math.random() * 6,
          color: palette[i % palette.length]
        });
      }
    }

    _updateParticles(dt) {
      for (let i = this.particles.length - 1; i >= 0; i--) {
        const p = this.particles[i];
        p.life -= dt;
        if (p.life <= 0) this.particles.splice(i, 1);
        else {
          p.vy += (p.shape === 'DUST' ? 120 : 480) * dt;
          p.x += p.vx * dt;
          p.y += p.vy * dt;
          p.rotation += p.vRot * dt;
        }
      }
    }

    _updateTrailGhosts(dt) {
      for (let i = this.trailGhosts.length - 1; i >= 0; i--) {
        this.trailGhosts[i].life -= dt;
        if (this.trailGhosts[i].life <= 0) this.trailGhosts.splice(i, 1);
      }
    }

    _updateFloatingTexts(dt) {
      for (let i = this.floatingTexts.length - 1; i >= 0; i--) {
        const f = this.floatingTexts[i];
        f.life -= dt;
        if (f.life <= 0) this.floatingTexts.splice(i, 1);
        else f.y -= 58 * dt;
      }
    }

    _updateConfetti(dt) {
      for (let i = this.confetti.length - 1; i >= 0; i--) {
        const c = this.confetti[i];
        c.vy += 380 * dt;
        c.x += c.vx * dt;
        c.y += c.vy * dt;
        c.rotation += c.vRot * dt;
        if (c.y > CONFIG.VIEW_HEIGHT + 60) this.confetti.splice(i, 1);
      }
    }
  }

  // =========================================================================
  // CANVAS RENDERER (ADAPTIVE PORTRAIT, ULTRAWIDE, TABLET & DESKTOP)
  // =========================================================================
  function roundRectPath(ctx, x, y, w, h, r) {
    const rr = Math.min(r, w / 2, h / 2);
    ctx.beginPath();
    ctx.moveTo(x + rr, y);
    ctx.arcTo(x + w, y, x + w, y + h, rr);
    ctx.arcTo(x + w, y + h, x, y + h, rr);
    ctx.arcTo(x, y + h, x, y, rr);
    ctx.arcTo(x, y, x + w, y, rr);
    ctx.closePath();
  }

  function fillRoundRect(ctx, x, y, w, h, r, color) {
    ctx.fillStyle = color;
    roundRectPath(ctx, x, y, w, h, r);
    ctx.fill();
  }

  function strokeRoundRect(ctx, x, y, w, h, r, color, lineW) {
    ctx.strokeStyle = color;
    ctx.lineWidth = lineW;
    roundRectPath(ctx, x, y, w, h, r);
    ctx.stroke();
  }

  class GameRenderer {
    constructor() {
      this.activeButtons = [];
      this.virtualWidth = CONFIG.VIEW_WIDTH;
      this.virtualHeight = CONFIG.VIEW_HEIGHT;
      this.isPortrait = false;
      this.scaleFactor = 1;
      this.offsetX = 0;
      this.offsetY = 0;
      this.safe = { left: 12, top: 8, right: 12, bottom: 10 };
    }

    updateMetrics(cssW, cssH, rawSafe) {
      const w = Math.max(1, cssW);
      const h = Math.max(1, cssH);
      if (h > w * 1.05) {
        this.isPortrait = true;
        this.virtualWidth = 760;
        this.virtualHeight = clamp(760 * (h / w), 1020, 1820);
      } else {
        this.isPortrait = false;
        this.virtualHeight = CONFIG.VIEW_HEIGHT;
        this.virtualWidth = clamp(CONFIG.VIEW_HEIGHT * (w / h), 960, 1680);
      }
      this.scaleFactor = Math.max(0.001, Math.min(w / this.virtualWidth, h / this.virtualHeight));
      this.offsetX = (w - this.virtualWidth * this.scaleFactor) / 2;
      this.offsetY = (h - this.virtualHeight * this.scaleFactor) / 2;

      this.safe = {
        left: clamp((rawSafe.left || 0) / this.scaleFactor, 0, 96),
        top: clamp((rawSafe.top || 0) / this.scaleFactor, 0, 84),
        right: clamp((rawSafe.right || 0) / this.scaleFactor, 0, 96),
        bottom: clamp((rawSafe.bottom || 0) / this.scaleFactor, 0, 84)
      };
    }

    screenToVirtual(sx, sy) {
      return {
        x: (sx - this.offsetX) / this.scaleFactor,
        y: (sy - this.offsetY) / this.scaleFactor
      };
    }

    render(ctx, cssW, cssH, rawSafe, engine, pressedSet) {
      this.activeButtons = [];
      this.updateMetrics(cssW, cssH, rawSafe);
      engine.viewportWidth = this.virtualWidth;

      ctx.fillStyle = '#001B38';
      ctx.fillRect(0, 0, cssW, cssH);

      ctx.save();
      ctx.translate(this.offsetX, this.offsetY);
      ctx.scale(this.scaleFactor, this.scaleFactor);
      ctx.beginPath();
      ctx.rect(0, 0, this.virtualWidth, this.virtualHeight);
      ctx.clip();

      if (engine.screen === 'TITLE') {
        if (this.isPortrait) this._drawTitlePortrait(ctx, engine);
        else this._drawTitleLandscape(ctx, engine);
      } else {
        if (this.isPortrait) {
          this._drawGameplayPortrait(ctx, engine, pressedSet);
        } else {
          this._drawGameplayWorld(ctx, engine, 0, 0, this.virtualHeight);
          this._drawHudLandscape(ctx, engine);
          if (engine.screen === 'PLAYING' && engine.touchControlsVisible) {
            this._drawTouchLandscape(ctx, pressedSet);
          }
        }
        if (engine.screen === 'LEVEL_CLEAR') this._drawLevelClearOverlay(ctx, engine);
        else if (engine.screen === 'VICTORY') this._drawVictoryOverlay(ctx, engine);
        else if (engine.screen === 'GAME_OVER') this._drawGameOverOverlay(ctx, engine);
      }

      ctx.restore();
    }

    _setFont(ctx, size, align = 'center') {
      ctx.font = `800 ${size}px system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`;
      ctx.textAlign = align;
      ctx.textBaseline = 'alphabetic';
    }

    _drawPillButton(ctx, x, y, w, h, label, bg, fg, action, fontSize) {
      this.activeButtons.push({ action, x, y, w, h });
      fillRoundRect(ctx, x + 2, y + 3, w, h, 16, 'rgba(0,15,36,0.22)');
      fillRoundRect(ctx, x, y, w, h, 16, bg);
      strokeRoundRect(ctx, x, y, w, h, 16, 'rgba(255,255,255,0.38)', 2);
      this._setFont(ctx, fontSize, 'center');
      ctx.fillStyle = fg;
      ctx.fillText(label, x + w / 2, y + h / 2 + fontSize * 0.34);
    }

    // =========================================================================
    // TITLE SCREENS
    // =========================================================================
    _drawTitleBg(ctx, time) {
      const grad = ctx.createLinearGradient(0, 0, 0, this.virtualHeight);
      grad.addColorStop(0, '#0098EE');
      grad.addColorStop(1, '#004A87');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, this.virtualWidth, this.virtualHeight);

      ctx.fillStyle = 'rgba(255,255,255,0.08)';
      ctx.beginPath();
      ctx.moveTo(this.virtualWidth * 0.15, 0);
      ctx.lineTo(this.virtualWidth * 0.42, 0);
      ctx.lineTo(this.virtualWidth * 0.18, this.virtualHeight);
      ctx.lineTo(-this.virtualWidth * 0.05, this.virtualHeight);
      ctx.closePath();
      ctx.fill();

      // Canal skyline silhouette
      const baseY = this.virtualHeight;
      const cols = ['rgba(0,32,66,0.28)', 'rgba(0,42,82,0.34)', 'rgba(0,25,55,0.24)'];
      let x = 0, idx = 0;
      while (x < this.virtualWidth + 90) {
        const w = 78 + (idx % 3) * 18;
        const h = 115 + (idx % 4) * 28;
        ctx.fillStyle = cols[idx % cols.length];
        ctx.fillRect(x, baseY - h, w - 4, h);
        ctx.fillRect(x + 12, baseY - h - 18, w - 28, 18);
        ctx.fillRect(x + 24, baseY - h - 32, w - 52, 14);
        x += w;
        idx++;
      }

      for (let i = 0; i <= 8; i++) {
        const sx = ((i * 175 + time * 28) % (this.virtualWidth + 90)) - 45;
        const sy = 90 + Math.sin(time * 2 + i) * 24 + (i % 4) * (this.virtualHeight * 0.19);
        ctx.fillStyle = 'rgba(255,255,255,0.13)';
        ctx.beginPath();
        ctx.arc(sx, sy, 20 + (i % 3) * 5, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    _drawTitleLandscape(ctx, engine) {
      this._drawTitleBg(ctx, engine.worldTime);
      const contentW = clamp(this.virtualWidth - this.safe.left - this.safe.right - 48, 880, 1320);
      const leftX = (this.virtualWidth - contentW) / 2;
      const rightX = leftX + contentW;
      const centerX = this.virtualWidth / 2;
      const topY = this.safe.top + 10;

      this._drawPillButton(ctx, rightX - 384, topY, 142, 38,
        engine.demoModeEnabled ? '⚡ Demo: AAN' : '⚡ Demo: UIT',
        engine.demoModeEnabled ? '#FF6600' : 'rgba(0,43,84,0.72)', '#FFF', 'TOGGLE_DEMO', 15);
      this._drawPillButton(ctx, rightX - 234, topY, 84, 38,
        engine.audio.isMusicEnabled ? '🎵 Aan' : '🎵 Uit',
        engine.audio.isMusicEnabled ? '#0069B4' : 'rgba(0,43,84,0.72)', '#FFF', 'TOGGLE_MUSIC', 15);
      this._drawPillButton(ctx, rightX - 142, topY, 84, 38,
        engine.audio.isMuted ? '🔇 Uit' : '🔊 Aan', 'rgba(0,43,84,0.72)', '#FFF', 'TOGGLE_MUTE', 15);
      this._drawPillButton(ctx, rightX - 50, topY, 50, 38, '⛶', 'rgba(0,43,84,0.72)', '#FFF', 'TOGGLE_FULLSCREEN', 18);

      const titleSize = contentW < 1040 ? 45 : 54;
      this._setFont(ctx, titleSize, 'center');
      ctx.fillStyle = 'rgba(0,22,48,0.4)';
      ctx.fillText('COOLBLUE GLIMLACH RUN', centerX + 4, topY + 92);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('COOLBLUE GLIMLACH RUN', centerX, topY + 88);

      fillRoundRect(ctx, centerX - 280, topY + 102, 560, 38, 19, '#FF6600');
      this._setFont(ctx, 19.5, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('Alles voor een glimlach!  •  Max, Remi & Vero', centerX, topY + 128);

      const cardsAreaW = contentW * 0.66;
      const gap = 14;
      const cardW = (cardsAreaW - gap * 2) / 3;
      const cardTop = 206, cardH = 288;

      this._setFont(ctx, 21.5, 'left');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('Kies je bezorger:', leftX + 4, cardTop - 12);

      CHAR_ORDER.forEach((id, idx) => {
        this._drawCharacterCard(
          ctx,
          leftX + idx * (cardW + gap), cardTop, cardW, cardH,
          CHARACTERS[id],
          engine.selectedCharacter === id,
          engine.worldTime,
          `SELECT_${id}`
        );
      });

      const lbLeft = leftX + cardsAreaW + 20;
      this._drawLeaderboardCard(ctx, lbLeft, cardTop, rightX - lbLeft, cardH);

      const btnTop = 516, btnH = 88;
      const startW = contentW * 0.39;
      const lvlW = contentW * 0.20;
      this._drawPillButton(ctx, leftX, btnTop, startW, btnH,
        `▶  START ALS ${engine.hero.displayName.toUpperCase()}!`,
        '#FF6600', '#FFF', 'START_GAME', contentW < 1040 ? 22 : 26);

      const l1Left = leftX + startW + 12;
      this._drawPillButton(ctx, l1Left, btnTop, lvlW, btnH, '1-1 Magazijn', '#004E89', '#FFF', 'START_LEVEL_1', 21);
      const l2Left = l1Left + lvlW + 12;
      this._drawPillButton(ctx, l2Left, btnTop, lvlW, btnH, '1-2 Grachten', '#004E89', '#FFF', 'START_LEVEL_2', 21);
      const tLeft = l2Left + lvlW + 12;
      this._drawPillButton(ctx, tLeft, btnTop, rightX - tLeft, btnH,
        engine.touchControlsVisible ? '🎮 Touch: AAN' : '🎮 Touch: UIT',
        'rgba(0,43,84,0.78)', '#FFF', 'TOGGLE_TOUCH', 17.5);

      const footH = Math.max(44, Math.min(54, this.virtualHeight - this.safe.bottom - 632));
      fillRoundRect(ctx, leftX, 626, contentW, footH, 15, 'rgba(0,26,56,0.6)');
      this._setFont(ctx, contentW < 1040 ? 13.5 : 16, 'center');
      ctx.fillStyle = '#D6F0FF';
      ctx.fillText(
        '← → / A D = Rennen  •  Spatie / ↑ / W = Springen  •  1 / 2 = Level  •  G = Demo  •  B = Muziek  •  F = Volledig scherm',
        centerX, 626 + footH / 2 + 6
      );
    }

    _drawTitlePortrait(ctx, engine) {
      this._drawTitleBg(ctx, engine.worldTime);
      const leftX = Math.max(24, this.safe.left + 14);
      const rightX = this.virtualWidth - Math.max(24, this.safe.right + 14);
      const contentW = rightX - leftX;
      const centerX = this.virtualWidth / 2;
      let curY = this.safe.top + 18;

      const qGap = 10;
      const qW = (contentW - qGap * 3) / 4;
      this._drawPillButton(ctx, leftX, curY, qW, 46,
        engine.demoModeEnabled ? '⚡ Demo AAN' : '⚡ Demo UIT',
        engine.demoModeEnabled ? '#FF6600' : 'rgba(0,43,84,0.75)', '#FFF', 'TOGGLE_DEMO', 16.5);
      this._drawPillButton(ctx, leftX + (qW + qGap), curY, qW, 46,
        engine.audio.isMusicEnabled ? '🎵 Muziek' : '🎵 Stil',
        engine.audio.isMusicEnabled ? '#0069B4' : 'rgba(0,43,84,0.75)', '#FFF', 'TOGGLE_MUSIC', 16.5);
      this._drawPillButton(ctx, leftX + (qW + qGap) * 2, curY, qW, 46,
        engine.audio.isMuted ? '🔇 Geluid' : '🔊 Geluid', 'rgba(0,43,84,0.75)', '#FFF', 'TOGGLE_MUTE', 16.5);
      this._drawPillButton(ctx, leftX + (qW + qGap) * 3, curY, qW, 46, '⛶ Scherm', 'rgba(0,43,84,0.75)', '#FFF', 'TOGGLE_FULLSCREEN', 16.5);
      curY += 74;

      this._setFont(ctx, 45, 'center');
      ctx.fillStyle = 'rgba(0,22,48,0.4)';
      ctx.fillText('COOLBLUE GLIMLACH RUN', centerX + 3, curY + 42);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('COOLBLUE GLIMLACH RUN', centerX, curY + 39);
      curY += 56;

      fillRoundRect(ctx, centerX - 260, curY, 520, 42, 21, '#FF6600');
      this._setFont(ctx, 20.5, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('Alles voor een glimlach!  •  Max, Remi & Vero', centerX, curY + 28);
      curY += 68;

      this._setFont(ctx, 23, 'left');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('Kies je bezorger:', leftX + 4, curY);
      curY += 16;

      const cardGap = 12;
      const cardW = (contentW - cardGap * 2) / 3;
      const cardH = clamp(this.virtualHeight * 0.22, 245, 305);
      CHAR_ORDER.forEach((id, idx) => {
        this._drawCharacterCard(
          ctx,
          leftX + idx * (cardW + cardGap), curY, cardW, cardH,
          CHARACTERS[id],
          engine.selectedCharacter === id,
          engine.worldTime,
          `SELECT_${id}`
        );
      });
      curY += cardH + 22;

      this._drawPillButton(ctx, leftX, curY, contentW, 86,
        `▶  START ALS ${engine.hero.displayName.toUpperCase()}!`,
        '#FF6600', '#FFF', 'START_GAME', 29);
      curY += 102;

      const halfW = (contentW - 14) / 2;
      this._drawPillButton(ctx, leftX, curY, halfW, 66, '📦  1-1 Magazijn', '#004E89', '#FFF', 'START_LEVEL_1', 22);
      this._drawPillButton(ctx, leftX + halfW + 14, curY, halfW, 66, '🚲  1-2 Grachten', '#004E89', '#FFF', 'START_LEVEL_2', 22);
      curY += 84;

      const lbH = Math.max(180, this.virtualHeight - this.safe.bottom - 20 - curY);
      this._drawLeaderboardCard(ctx, leftX, curY, contentW, lbH);
    }

    _drawCharacterCard(ctx, x, y, w, h, charObj, selected, time, action) {
      this.activeButtons.push({ action, x, y, w, h });
      fillRoundRect(ctx, x + 3, y + 6, w, h, 22, 'rgba(0,15,36,0.22)');
      fillRoundRect(ctx, x, y, w, h, 22, selected ? '#FFFFFF' : 'rgba(232,246,255,0.92)');
      strokeRoundRect(ctx, x, y, w, h, 22, selected ? charObj.accent : 'rgba(255,255,255,0.5)', selected ? 6.5 : 2.5);

      ctx.fillStyle = selected ? `${charObj.accent}24` : 'rgba(0,95,158,0.09)';
      ctx.beginPath();
      ctx.arc(x + w / 2, y + h * 0.43, Math.min(w * 0.34, 64), 0, Math.PI * 2);
      ctx.fill();

      if (selected) {
        const bw = Math.min(118, w - 20);
        fillRoundRect(ctx, x + w - bw - 10, y + 10, bw, 28, 12, charObj.accent);
        this._setFont(ctx, 14.5, 'center');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText('GEKOZEN ✓', x + w - bw / 2 - 10, y + 29);
      }

      const bounceY = selected ? Math.abs(Math.sin(time * 5.2)) * 12 : Math.sin(time * 2.2 + charObj.ordinal) * 2;
      const spriteScale = clamp(w / 160, 1.28, 1.62);
      ctx.save();
      ctx.translate(x + w / 2, y + h * 0.44 - bounceY);
      ctx.scale(spriteScale, spriteScale);
      this._drawHeroSprite(ctx, -22, -31, 44, 62, charObj, true, selected ? time * 6 : 0, selected && bounceY > 5, false, time, false, 1, 1, false);
      ctx.restore();

      const compact = w < 225;
      this._setFont(ctx, compact ? 26 : 31, 'center');
      ctx.fillStyle = '#002B54';
      ctx.fillText(charObj.displayName.toUpperCase(), x + w / 2, y + h - 48);

      this._setFont(ctx, compact ? 15 : 17.5, 'center');
      ctx.fillStyle = charObj.accent;
      ctx.fillText(charObj.badgeTitle, x + w / 2, y + h - 20);
    }

    _drawLeaderboardCard(ctx, x, y, w, h) {
      fillRoundRect(ctx, x, y, w, h, 22, 'rgba(0,34,70,0.86)');
      strokeRoundRect(ctx, x, y, w, h, 22, 'rgba(255,255,255,0.45)', 2.5);

      this._setFont(ctx, 22, 'center');
      ctx.fillStyle = '#FFC400';
      ctx.fillText('🏆 TOP BEZORGERS', x + w / 2, y + 38);

      const maxRows = clamp(Math.floor((h - 62) / 40), 2, 5);
      const entries = ScoreStore.getTopScores(maxRows);
      const narrowLb = w < 335;
      if (entries.length === 0) {
        this._setFont(ctx, narrowLb ? 15.5 : 18.5, 'center');
        ctx.fillStyle = '#B8E2FF';
        ctx.fillText('Nog geen ritten opgeslagen.', x + w / 2, y + h / 2 - 10);
        this._setFont(ctx, narrowLb ? 15 : 18, 'center');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText('Wie pakt het record:', x + w / 2, y + h / 2 + 20);
        this._setFont(ctx, narrowLb ? 17.5 : 20.5, 'center');
        ctx.fillStyle = '#FF8822';
        ctx.fillText('Max, Remi of Vero?', x + w / 2, y + h / 2 + 50);
      } else {
        let ry = y + 78;
        entries.forEach((entry, idx) => {
          const ch = CHARACTERS[entry.character] || CHARACTERS.MAX;
          fillRoundRect(ctx, x + 14, ry - 24, w - 28, 35, 10, idx === 0 ? 'rgba(255,196,0,0.3)' : 'rgba(255,255,255,0.16)');
          ctx.fillStyle = ch.accent;
          ctx.beginPath();
          ctx.arc(x + 30, ry - 6, 6, 0, Math.PI * 2);
          ctx.fill();

          this._setFont(ctx, 18, 'left');
          ctx.fillStyle = idx === 0 ? '#FFC400' : '#FFFFFF';
          ctx.fillText(`${idx + 1}. ${ch.displayName}`, x + 42, ry);

          this._setFont(ctx, 18, 'right');
          ctx.fillStyle = '#7CE0FF';
          ctx.fillText(`${entry.score} pt  •  😄${entry.smiles}`, x + w - 24, ry);
          ry += 39;
        });
      }
    }

    // =========================================================================
    // PORTRAIT GAMEPLAY + ARCADE CONTROL DECK
    // =========================================================================
    _drawGameplayPortrait(ctx, engine, pressedSet) {
      const deckH = engine.touchControlsVisible
        ? clamp(this.virtualHeight * 0.27, 250, 430)
        : 92 + this.safe.bottom;
      const worldBottomY = Math.max(720, this.virtualHeight - deckH);
      const worldOffsetY = worldBottomY - CONFIG.VIEW_HEIGHT;

      this._drawGameplayWorld(ctx, engine, 0, worldOffsetY, worldBottomY);
      this._drawHudPortrait(ctx, engine);
      this._drawPortraitDeck(ctx, engine, worldBottomY, pressedSet);
    }

    _drawHudPortrait(ctx, engine) {
      const topPad = this.safe.top;
      const barH = topPad + 76;
      ctx.fillStyle = 'rgba(0,34,70,0.88)';
      ctx.fillRect(0, 0, this.virtualWidth, barH);
      ctx.fillStyle = '#FF6600';
      ctx.fillRect(0, barH - 3, this.virtualWidth, 3);

      const row1Y = topPad + 31;
      const row2Y = topPad + 62;

      fillRoundRect(ctx, 16, topPad + 9, 92, 30, 12, engine.hero.accent);
      this._setFont(ctx, 17.5, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText(engine.hero.displayName.toUpperCase(), 62, topPad + 30);

      let heartsStr = '';
      for (let i = 1; i <= CONFIG.STARTING_HEARTS; i++) heartsStr += i <= engine.hearts ? '♥' : '♡';
      this._setFont(ctx, 24, 'left');
      ctx.fillStyle = '#FF5252';
      ctx.fillText(heartsStr, 120, row1Y);

      if (engine.player.hasBakfietsShield) {
        fillRoundRect(ctx, 206, topPad + 9, 108, 30, 12, '#0090E3');
        this._setFont(ctx, 14, 'center');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText('🚲 TURBO', 260, topPad + 29);
      }

      this._setFont(ctx, 20.5, 'right');
      ctx.fillStyle = engine.timeRemaining < 20 ? '#FF6E6E' : '#B8E2FF';
      ctx.fillText(`⏱ ${Math.floor(engine.timeRemaining)}s`, this.virtualWidth - 18, row1Y);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText(`${engine.score} pt`, this.virtualWidth - 112, row1Y);
      ctx.fillStyle = '#FFC400';
      ctx.fillText(`😄 ${engine.smilesCollected}`, this.virtualWidth - 238, row1Y);

      this._setFont(ctx, 17, 'left');
      ctx.fillStyle = '#8CE0FF';
      ctx.fillText(`${engine.level.code} ${engine.level.title}`, 18, row2Y);

      const prog = clamp(engine.player.x / engine.level.flagX, 0, 1);
      const barLeft = 295, barW = this.virtualWidth - 18 - barLeft;
      fillRoundRect(ctx, barLeft, row2Y - 12, barW, 11, 5.5, 'rgba(255,255,255,0.36)');
      fillRoundRect(ctx, barLeft, row2Y - 12, Math.max(10, barW * prog), 11, 5.5, '#FF6600');

      if (engine.bannerMessage && engine.screen === 'PLAYING') {
        const alpha = Math.min(1, engine.bannerTimer * 1.4) * 0.92;
        fillRoundRect(ctx, 36, barH + 12, this.virtualWidth - 72, 42, 20, `rgba(0,144,227,${alpha})`);
        this._setFont(ctx, 18, 'center');
        ctx.fillStyle = `rgba(255,255,255,${alpha})`;
        ctx.fillText(engine.bannerMessage, this.virtualWidth / 2, barH + 39);
      }
    }

    _drawPortraitDeck(ctx, engine, deckTop, pressedSet) {
      const grad = ctx.createLinearGradient(0, deckTop, 0, this.virtualHeight);
      grad.addColorStop(0, '#003566');
      grad.addColorStop(1, '#001B38');
      ctx.fillStyle = grad;
      ctx.fillRect(0, deckTop, this.virtualWidth, this.virtualHeight - deckTop);
      ctx.fillStyle = '#FF6600';
      ctx.fillRect(0, deckTop, this.virtualWidth, 5);

      const utilTop = deckTop + 16;
      const padX = 18, gap = 8;
      const utils = [
        [engine.demoModeEnabled ? '⚡ Demo' : '⚡ Norm', engine.demoModeEnabled ? '#FF6600' : '#005F9E', 'TOGGLE_DEMO'],
        ['L1', engine.currentLevelNumber === 1 ? '#0090E3' : '#1D4366', 'START_LEVEL_1'],
        ['L2', engine.currentLevelNumber === 2 ? '#0090E3' : '#1D4366', 'START_LEVEL_2'],
        ['🎮', engine.touchControlsVisible ? '#0090E3' : '#1D4366', 'TOGGLE_TOUCH'],
        [engine.audio.isMusicEnabled ? '🎵' : '🎵✕', engine.audio.isMusicEnabled ? '#0090E3' : '#1D4366', 'TOGGLE_MUSIC'],
        [engine.audio.isMuted ? '🔇' : '🔊', '#1D4366', 'TOGGLE_MUTE'],
        ['⌂', '#1D4366', 'RETURN_TITLE']
      ];
      const pillW = (this.virtualWidth - padX * 2 - gap * (utils.length - 1)) / utils.length;
      utils.forEach(([lbl, bg, act], i) => {
        this._drawPillButton(ctx, padX + i * (pillW + gap), utilTop, pillW, 44, lbl, bg, '#FFF', act, 16);
      });

      if (!engine.touchControlsVisible || engine.screen !== 'PLAYING') return;

      const controlsTop = utilTop + 62;
      const controlsBot = this.virtualHeight - Math.max(18, this.safe.bottom + 10);
      const availH = Math.max(130, controlsBot - controlsTop);
      const btnH = Math.min(168, availH * 0.88);
      const cy = (controlsTop + controlsBot) / 2;

      this._drawTouchPad(ctx, 26, cy - btnH / 2, 158, btnH, '◀', 'TOUCH_LEFT', false, pressedSet.has('TOUCH_LEFT'));
      this._drawTouchPad(ctx, 198, cy - btnH / 2, 158, btnH, '▶', 'TOUCH_RIGHT', false, pressedSet.has('TOUCH_RIGHT'));
      this._drawPillButton(ctx, 382, cy - 26, 116, 52, '↺ Herstart', '#144272', '#B8E2FF', 'RESTART', 16.5);

      const jumpSize = Math.min(184, availH * 0.94);
      this._drawTouchPad(ctx, this.virtualWidth - 28 - jumpSize, cy - jumpSize / 2, jumpSize, jumpSize, 'SPRING', 'TOUCH_JUMP', true, pressedSet.has('TOUCH_JUMP'));
    }

    // =========================================================================
    // WORLD & SPRITES
    // =========================================================================
    _drawGameplayWorld(ctx, engine, topClip, worldOffsetY, bottomClip) {
      const shakeX = engine.screenShakeTimer > 0 ? Math.sin(engine.worldTime * 80) * 7 : 0;
      const shakeY = engine.screenShakeTimer > 0 ? Math.cos(engine.worldTime * 75) * 5 : 0;

      ctx.save();
      ctx.beginPath();
      ctx.rect(0, topClip, this.virtualWidth, bottomClip - topClip);
      ctx.clip();

      if (engine.level.theme === 'WAREHOUSE') {
        if (worldOffsetY > 0) {
          ctx.fillStyle = '#C4E8FF';
          ctx.fillRect(0, 0, this.virtualWidth, worldOffsetY + 2);
        }
        ctx.save();
        ctx.translate(0, worldOffsetY);
        this._drawWarehouseBg(ctx, engine.cameraX, engine.worldTime);
        ctx.restore();
      } else {
        if (worldOffsetY > 0) {
          ctx.fillStyle = '#38B6F6';
          ctx.fillRect(0, 0, this.virtualWidth, worldOffsetY + 2);
        }
        ctx.save();
        ctx.translate(0, worldOffsetY);
        this._drawCanalsBg(ctx, engine.cameraX, engine.worldTime);
        ctx.restore();
      }

      ctx.save();
      ctx.translate(-engine.cameraX + shakeX, worldOffsetY + shakeY);

      if (engine.level.theme === 'CANALS') {
        this._drawCanalWater(ctx, engine.cameraX, engine.worldTime);
      }

      for (const plat of engine.level.platforms) {
        if (plat.x + plat.width < engine.cameraX - 140 || plat.x > engine.cameraX + this.virtualWidth + 140) continue;
        this._drawPlatform(ctx, plat, engine.worldTime, engine.level.theme);
      }

      for (const box of engine.level.boxes) {
        if (box.x + box.size < engine.cameraX - 80 || box.x > engine.cameraX + this.virtualWidth + 80) continue;
        this._drawQuestionBox(ctx, box, engine.worldTime);
      }

      for (const coin of engine.level.coins) {
        if (coin.collected) continue;
        if (coin.x < engine.cameraX - 50 || coin.x > engine.cameraX + this.virtualWidth + 50) continue;
        this._drawSmileCoin(ctx, coin.x, coin.y + Math.sin(engine.worldTime * 4.2 + coin.bobPhase) * 6, coin.radius, engine.worldTime + coin.bobPhase);
      }

      for (const item of engine.powerUps) {
        this._drawBakfietsPowerUp(ctx, item.x, item.y, item.width, item.height, engine.worldTime);
      }

      this._drawFinishArea(ctx, engine);

      for (const enemy of engine.level.enemies) {
        if (!enemy.alive && enemy.squashedTimer <= 0) continue;
        if (enemy.x + enemy.width < engine.cameraX - 80 || enemy.x > engine.cameraX + this.virtualWidth + 80) continue;
        this._drawEnemy(ctx, enemy, engine.level);
      }

      const p = engine.player;
      this._drawProjectedShadow(ctx, p.x + p.width / 2, p.y + p.height, 20, engine.level);

      for (const ghost of engine.trailGhosts) {
        const a = clamp(ghost.life / ghost.maxLife, 0, 1) * 0.36;
        fillRoundRect(ctx, ghost.x + 6, ghost.y + 8, p.width - 12, p.height - 10, 14, `rgba(0,168,243,${a})`);
      }

      const blinkHide = p.invulnTimer > 0 && (Math.floor(p.invulnTimer * 18) % 2 === 0);
      if (!blinkHide) {
        this._drawHeroSprite(
          ctx, p.x, p.y, p.width, p.height,
          engine.hero, p.facingRight, p.animTime, !p.onGround,
          p.hasBakfietsShield || engine.demoModeEnabled,
          engine.worldTime, true, p.squashX, p.stretchY, p.invulnTimer > 1.1
        );
      }

      for (const pt of engine.particles) this._drawParticle(ctx, pt);

      for (const c of engine.confetti) {
        ctx.save();
        ctx.translate(c.x, c.y);
        ctx.rotate(c.rotation);
        fillRoundRect(ctx, -c.width / 2, -c.height / 2, c.width, c.height, 2, c.color);
        ctx.restore();
      }

      for (const f of engine.floatingTexts) {
        const alpha = clamp(f.life / f.maxLife, 0, 1);
        ctx.save();
        ctx.globalAlpha = alpha;
        this._setFont(ctx, 24 * (f.scale || 1), 'center');
        ctx.fillStyle = '#001E3C';
        ctx.fillText(f.text, f.x + 2, f.y + 2);
        ctx.fillStyle = f.color;
        ctx.fillText(f.text, f.x, f.y);
        ctx.restore();
      }

      ctx.restore();
      ctx.restore();
    }

    _drawProjectedShadow(ctx, cx, feetY, baseRx, level) {
      let sy = null;
      for (const plat of level.platforms) {
        if (cx >= plat.x + 4 && cx <= plat.x + plat.width - 4 && plat.y >= feetY - 6) {
          if (sy === null || plat.y < sy) sy = plat.y;
        }
      }
      for (const box of level.boxes) {
        if (cx >= box.x + 2 && cx <= box.x + box.size - 2 && box.y >= feetY - 6) {
          if (sy === null || box.y < sy) sy = box.y;
        }
      }
      if (sy === null) return;
      const dist = Math.max(0, sy - feetY);
      if (dist > 280) return;
      const factor = clamp(1 - dist / 280, 0.25, 1);
      ctx.fillStyle = `rgba(0,22,48,${0.33 * factor})`;
      ctx.beginPath();
      ctx.ellipse(cx, sy, baseRx * factor, 4.5 * factor, 0, 0, Math.PI * 2);
      ctx.fill();
    }

    _drawParticle(ctx, pt) {
      const ratio = clamp(pt.life / pt.maxLife, 0, 1);
      ctx.save();
      ctx.globalAlpha = ratio;
      if (pt.shape === 'SMILE') {
        this._drawMiniSmile(ctx, pt.x, pt.y, pt.size * 0.68);
      } else if (pt.shape === 'STAR') {
        ctx.translate(pt.x, pt.y);
        ctx.rotate(pt.rotation);
        ctx.fillStyle = pt.color;
        const rad = pt.size * 0.72, inner = rad * 0.36;
        ctx.beginPath();
        for (let i = 0; i < 8; i++) {
          const ang = (i * Math.PI) / 4;
          const d = i % 2 === 0 ? rad : inner;
          if (i === 0) ctx.moveTo(Math.cos(ang) * d, Math.sin(ang) * d);
          else ctx.lineTo(Math.cos(ang) * d, Math.sin(ang) * d);
        }
        ctx.closePath();
        ctx.fill();
      } else if (pt.shape === 'SPLASH') {
        ctx.fillStyle = pt.color;
        ctx.beginPath();
        ctx.ellipse(pt.x, pt.y, pt.size * 0.4, pt.size * 0.65, 0, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.fillStyle = pt.color;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, pt.size * 0.5, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();
    }

    _drawWarehouseBg(ctx, cameraX, time) {
      const grad = ctx.createLinearGradient(0, 0, 0, CONFIG.VIEW_HEIGHT);
      grad.addColorStop(0, '#C4E8FF');
      grad.addColorStop(1, '#EFF9FF');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, this.virtualWidth, CONFIG.VIEW_HEIGHT);

      // Upper mezzanine conveyor with moving Coolblue parcels
      ctx.fillStyle = '#A7D2F0';
      ctx.fillRect(0, 252, this.virtualWidth, 10);
      const mezStep = 145;
      const mezShift = -(((cameraX * 0.16 - time * 55) % mezStep) + mezStep) % mezStep;
      let mx = mezShift - mezStep, mIdx = 0;
      while (mx < this.virtualWidth + mezStep) {
        const bw = mIdx % 2 === 0 ? 36 : 28;
        const bh = mIdx % 3 === 0 ? 28 : 22;
        fillRoundRect(ctx, mx, 252 - bh, bw, bh, 4, 'rgba(0,144,227,0.53)');
        ctx.fillStyle = 'rgba(255,102,0,0.58)';
        ctx.fillRect(mx + 6, 244, bw - 12, 3);
        mx += mezStep;
        mIdx++;
      }

      // Ceiling beam + trusses + volumetric lamps
      ctx.fillStyle = '#7EB8E2';
      ctx.fillRect(0, 0, this.virtualWidth, 34);
      ctx.strokeStyle = 'rgba(92,143,181,0.35)';
      ctx.lineWidth = 3;
      let tx = -(cameraX * 0.2) % 90;
      while (tx < this.virtualWidth + 90) {
        ctx.beginPath();
        ctx.moveTo(tx, 4); ctx.lineTo(tx + 45, 32); ctx.lineTo(tx + 90, 4);
        ctx.stroke();
        tx += 90;
      }

      const lampSpacing = 320;
      let lx = (-(cameraX * 0.25) % lampSpacing) - lampSpacing;
      while (lx < this.virtualWidth + lampSpacing) {
        ctx.fillStyle = '#5C8FB5';
        ctx.fillRect(lx - 3, 34, 6, 54);
        fillRoundRect(ctx, lx - 36, 84, 72, 19, 8, '#002B54');
        fillRoundRect(ctx, lx - 26, 97, 52, 6, 3, '#FFF9C4');
        ctx.fillStyle = 'rgba(255,244,190,0.18)';
        ctx.beginPath();
        ctx.moveTo(lx - 28, 103); ctx.lineTo(lx + 28, 103);
        ctx.lineTo(lx + 102, 320); ctx.lineTo(lx - 102, 320);
        ctx.closePath();
        ctx.fill();
        lx += lampSpacing;
      }

      const bannerCycle = 1180;
      let bx = (-(cameraX * 0.22) % bannerCycle) + 130;
      while (bx < this.virtualWidth + 620) {
        fillRoundRect(ctx, bx, 126, 544, 104, 18, 'rgba(0,144,227,0.9)');
        strokeRoundRect(ctx, bx, 126, 544, 104, 18, 'rgba(255,255,255,0.58)', 2.5);
        this._setFont(ctx, 35, 'center');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText('coolblue magazijn', bx + 260, 178);
        ctx.fillStyle = '#FF6600';
        ctx.beginPath();
        ctx.arc(bx + 448, 168, 10, 0, Math.PI * 2);
        ctx.fill();
        this._setFont(ctx, 16, 'center');
        ctx.fillStyle = '#D8F1FF';
        ctx.fillText('Alles voor een glimlach • Vandaag besteld, morgen in huis!', bx + 272, 212);
        bx += bannerCycle;
      }

      const rackSpacing = 280;
      let rx = (-(cameraX * 0.38) % rackSpacing) - rackSpacing;
      let rackIdx = 0;
      while (rx < this.virtualWidth + rackSpacing) {
        ctx.fillStyle = '#8BB9DC';
        ctx.fillRect(rx, 286, 12, CONFIG.GROUND_Y - 286);
        ctx.fillRect(rx + 210, 286, 12, CONFIG.GROUND_Y - 286);
        for (let tier = 0; tier <= 2; tier++) {
          const sy = 340 + tier * 88;
          ctx.fillStyle = '#F58E3C';
          ctx.fillRect(rx, sy, 222, 10);
          for (let col = 0; col <= 2; col++) {
            if ((rackIdx + tier + col) % 4 === 0) continue;
            const boxX = rx + 22 + col * 60;
            const boxH = (tier + col) % 2 === 0 ? 46 : 38;
            fillRoundRect(ctx, boxX, sy - boxH, 48, boxH, 5, 'rgba(0,144,227,0.6)');
            ctx.fillStyle = 'rgba(255,255,255,0.65)';
            ctx.fillRect(boxX + 19, sy - boxH, 10, 9);
            ctx.fillStyle = 'rgba(255,102,0,0.62)';
            ctx.fillRect(boxX + 12, sy - 14, 24, 5);
          }
        }
        rx += rackSpacing;
        rackIdx++;
      }
    }

    _drawCanalsBg(ctx, cameraX, time) {
      const grad = ctx.createLinearGradient(0, 0, 0, CONFIG.VIEW_HEIGHT);
      grad.addColorStop(0, '#38B6F6');
      grad.addColorStop(1, '#E1F5FE');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, this.virtualWidth, CONFIG.VIEW_HEIGHT);

      const sunX = this.virtualWidth - 155;
      const halo = Math.sin(time * 2.2) * 6;
      ctx.fillStyle = 'rgba(255,230,128,0.22)';
      ctx.beginPath(); ctx.arc(sunX, 114, 84 + halo, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = 'rgba(255,224,102,0.34)';
      ctx.beginPath(); ctx.arc(sunX, 114, 62, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = '#FFD54F';
      ctx.beginPath(); ctx.arc(sunX, 114, 42, 0, Math.PI * 2); ctx.fill();

      // Distant flying birds
      ctx.strokeStyle = 'rgba(0,58,108,0.45)';
      ctx.lineWidth = 2.2;
      for (let b = 0; b < 4; b++) {
        const span = this.virtualWidth + 120;
        const bx = (((b * 310 - cameraX * 0.08 + time * 24) % span) + span) % span - 60;
        const by = 82 + (b % 2) * 32 + Math.sin(time * 4 + b) * 5;
        const wing = Math.sin(time * 7 + b * 1.3) * 5;
        ctx.beginPath();
        ctx.moveTo(bx - 8, by - wing); ctx.lineTo(bx, by); ctx.lineTo(bx + 8, by - wing);
        ctx.stroke();
      }

      // Fluffy clouds
      const cloudSpan = 410;
      let cx = -((cameraX * 0.14 + time * 12) % cloudSpan);
      let ci = 0;
      while (cx < this.virtualWidth + 300) {
        const cy = 104 + (ci % 3) * 42;
        ctx.fillStyle = 'rgba(255,255,255,0.9)';
        ctx.beginPath();
        ctx.arc(cx, cy, 30, 0, Math.PI * 2);
        ctx.arc(cx + 28, cy - 12, 36, 0, Math.PI * 2);
        ctx.arc(cx + 62, cy, 28, 0, Math.PI * 2);
        ctx.fill();
        cx += cloudSpan;
        ci++;
      }

      // Dutch Windmills
      const millCycle = 1280;
      const millX = 680 - ((cameraX * 0.22) % millCycle);
      [-millCycle, 0, millCycle].forEach((off) => {
        const mx = millX + off;
        if (mx > -180 && mx < this.virtualWidth + 180) this._drawWindmill(ctx, mx, 505, time);
      });

      // Dutch Canal Houses (Grachtenpanden with 3 gable types & tulips!)
      const palette = ['#C85A3F', '#2F6D80', '#D9A13B', '#4B5F73', '#B8474B', '#3E7B67'];
      const houseW = 112;
      const startHouse = Math.floor((cameraX * 0.36) / houseW) - 2;
      const endHouse = startHouse + Math.ceil(this.virtualWidth / houseW) + 5;
      for (let i = startHouse; i <= endHouse; i++) {
        const hx = i * houseW - cameraX * 0.36;
        const h = 228 + (Math.abs(i * 37) % 4) * 34;
        const topY = CONFIG.GROUND_Y - h;
        const baseCol = palette[Math.abs(i) % palette.length];
        ctx.fillStyle = baseCol;
        ctx.fillRect(hx + 2, topY, houseW - 4, h);

        const gable = Math.abs(i) % 3;
        if (gable === 0) {
          ctx.fillRect(hx + 16, topY - 22, houseW - 32, 22);
          ctx.fillRect(hx + 32, topY - 42, houseW - 64, 20);
          ctx.fillStyle = '#FFFFFF';
          ctx.fillRect(hx + 14, topY - 25, houseW - 28, 5);
          ctx.fillRect(hx + 30, topY - 45, houseW - 60, 5);
        } else if (gable === 1) {
          ctx.beginPath();
          ctx.arc(hx + houseW / 2, topY - 12, 34, 0, Math.PI * 2);
          ctx.fill();
          ctx.strokeStyle = '#FFFFFF';
          ctx.lineWidth = 3.5;
          ctx.beginPath();
          ctx.arc(hx + houseW / 2, topY - 12, 34, Math.PI * 1.03, Math.PI * 1.97);
          ctx.stroke();
        } else {
          ctx.fillRect(hx + 28, topY - 38, houseW - 56, 38);
          ctx.fillStyle = '#FFFFFF';
          ctx.fillRect(hx + 24, topY - 43, houseW - 48, 6);
        }

        for (let row = 0; row <= 2; row++) {
          const wy = topY + 28 + row * 60;
          if (wy + 36 < CONFIG.GROUND_Y - 14) {
            [hx + 20, hx + 64].forEach((wx) => {
              ctx.fillStyle = '#FFFFFF';
              ctx.fillRect(wx - 2, wy - 2, 30, 38);
              ctx.fillStyle = '#2A4B63';
              ctx.fillRect(wx + 1, wy + 1, 24, 32);
              ctx.fillStyle = 'rgba(255,240,170,0.38)';
              ctx.fillRect(wx + 1, wy + 1, 11, 15);
              if (row === 1) {
                ctx.fillStyle = '#6D4C41';
                ctx.fillRect(wx - 1, wy + 32, 28, 6);
                ['#FF6600', '#FFC400', '#FF5252'].forEach((tc, ti) => {
                  ctx.fillStyle = tc;
                  ctx.beginPath();
                  ctx.arc(wx + 6 + ti * 7.5, wy + 30, 3.2, 0, Math.PI * 2);
                  ctx.fill();
                });
              }
            });
          }
        }

        if (Math.abs(i) % 5 === 2) {
          fillRoundRect(ctx, hx + 10, CONFIG.GROUND_Y - 48, houseW - 20, 22, 6, '#0090E3');
          ctx.fillStyle = '#FF6600';
          ctx.fillRect(hx + 10, CONFIG.GROUND_Y - 26, houseW - 20, 6);
        }
      }
    }

    _drawWindmill(ctx, cx, baseY, time) {
      ctx.fillStyle = '#7A5C43';
      ctx.beginPath();
      ctx.moveTo(cx - 38, baseY);
      ctx.lineTo(cx + 38, baseY);
      ctx.lineTo(cx + 22, baseY - 138);
      ctx.lineTo(cx - 22, baseY - 138);
      ctx.closePath();
      ctx.fill();

      ctx.fillStyle = '#5D4037';
      ctx.fillRect(cx - 36, baseY - 68, 72, 8);
      ctx.fillStyle = '#002B54';
      ctx.beginPath();
      ctx.arc(cx, baseY - 140, 25, Math.PI, 0);
      ctx.fill();

      ctx.save();
      ctx.translate(cx, baseY - 132);
      ctx.rotate((time * 28 * Math.PI) / 180);
      for (let b = 0; b < 4; b++) {
        ctx.rotate(Math.PI / 2);
        ctx.fillStyle = '#5A3E28';
        ctx.fillRect(-3, 0, 6, 94);
        ctx.fillStyle = 'rgba(255,255,255,0.86)';
        ctx.fillRect(3, 16, 19, 72);
      }
      ctx.restore();
    }

    _drawCanalWater(ctx, cameraX, time) {
      const left = cameraX - 60;
      const right = cameraX + this.virtualWidth + 60;
      const grad = ctx.createLinearGradient(0, CONFIG.GROUND_Y + 22, 0, CONFIG.VIEW_HEIGHT + 40);
      grad.addColorStop(0, '#0288D1');
      grad.addColorStop(1, '#01579B');
      ctx.fillStyle = grad;
      ctx.fillRect(left, CONFIG.GROUND_Y + 22, right - left, CONFIG.VIEW_HEIGHT - CONFIG.GROUND_Y + 20);

      const waveStep = 60;
      let wx = (Math.floor(left / waveStep) - 1) * waveStep;
      while (wx < right) {
        const wy = CONFIG.GROUND_Y + 34 + Math.sin(time * 3.5 + wx * 0.04) * 4;
        fillRoundRect(ctx, wx, wy, 36, 5.5, 3, '#4FC3F7');
        fillRoundRect(ctx, wx + 14, wy + 18, 24, 4, 2, 'rgba(255,255,255,0.45)');
        wx += waveStep;
      }

      // Cute bobbing yellow canal duck wearing a Coolblue cap!
      const duckX = 1120 + Math.sin(time * 1.2) * 35;
      const duckY = CONFIG.GROUND_Y + 22 + Math.sin(time * 4.2) * 3.5;
      if (duckX > left && duckX < right) {
        ctx.fillStyle = '#FFC400';
        ctx.beginPath(); ctx.ellipse(duckX, duckY - 1, 12, 7, 0, 0, Math.PI * 2); ctx.fill();
        ctx.beginPath(); ctx.arc(duckX + 8, duckY - 11, 7, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#FF6600';
        ctx.beginPath(); ctx.ellipse(duckX + 16, duckY - 10, 4, 2, 0, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#0090E3';
        ctx.beginPath(); ctx.arc(duckX + 8, duckY - 15, 6, Math.PI, 0); ctx.fill();
      }
    }

    _drawPlatform(ctx, p, time, theme) {
      if (p.type === 'GROUND') {
        ctx.fillStyle = theme === 'CANALS' ? '#455A64' : '#37474F';
        ctx.fillRect(p.x, p.y, p.width, p.height);
        ctx.fillStyle = '#0090E3';
        ctx.fillRect(p.x, p.y, p.width, 18);
        ctx.fillStyle = 'rgba(255,255,255,0.35)';
        ctx.fillRect(p.x, p.y, p.width, 4);
        ctx.fillStyle = '#FF6600';
        ctx.fillRect(p.x, p.y + 18, p.width, 6);
        if (theme === 'WAREHOUSE') {
          ctx.fillStyle = '#FFC400';
          ctx.fillRect(p.x + p.width - 28, p.y, 28, 18);
          ctx.fillStyle = '#002B54';
          ctx.fillRect(p.x + p.width - 20, p.y, 8, 18);
        }
        let tx = p.x + 14;
        while (tx < p.x + p.width - 14) {
          fillRoundRect(ctx, tx, p.y + 38, Math.min(54, p.x + p.width - 10 - tx), 14, 4, '#546E7A');
          tx += 76;
        }
      } else if (p.type === 'PALLET') {
        fillRoundRect(ctx, p.x, p.y, p.width, p.height, 6, '#DEB887');
        ctx.fillStyle = '#A46D2A';
        ctx.fillRect(p.x + 8, p.y + p.height - 9, p.width - 16, 9);
        ctx.fillStyle = '#7D4E16';
        ctx.fillRect(p.x + 10, p.y + 9, 18, p.height - 13);
        ctx.fillRect(p.x + p.width / 2 - 9, p.y + 9, 18, p.height - 13);
        ctx.fillRect(p.x + p.width - 28, p.y + 9, 18, p.height - 13);
        strokeRoundRect(ctx, p.x, p.y, p.width, p.height, 6, '#6D4312', 2.5);
      } else if (p.type === 'PARCEL_STACK') {
        const boxH = 56;
        let y = p.y;
        while (y < p.y + p.height - 2) {
          const h = Math.min(boxH, p.y + p.height - y);
          fillRoundRect(ctx, p.x + 1, y + 1, p.width - 2, h - 2, 6, '#0090E3');
          fillRoundRect(ctx, p.x + 3, y + 3, p.width - 6, 6, 4, 'rgba(255,255,255,0.26)');
          strokeRoundRect(ctx, p.x + 1, y + 1, p.width - 2, h - 2, 6, '#005F9E', 2.5);
          ctx.fillStyle = 'rgba(255,255,255,0.88)';
          ctx.fillRect(p.x + p.width / 2 - 10, y + 3, 20, 10);
          if (h >= 40) {
            ctx.strokeStyle = '#FF6600';
            ctx.lineWidth = 3.5;
            ctx.beginPath();
            ctx.arc(p.x + p.width / 2, y + h / 2 + 2, 13, 0.3, Math.PI - 0.3);
            ctx.stroke();
          }
          y += boxH;
        }
      } else if (p.type === 'CONVEYOR_RIGHT' || p.type === 'CONVEYOR_LEFT') {
        fillRoundRect(ctx, p.x, p.y, p.width, p.height, 10, '#263238');
        const dir = p.type === 'CONVEYOR_RIGHT' ? 1 : -1;
        const step = 28;
        const shift = (((time * 135 * dir) % step) + step) % step;
        ctx.fillStyle = '#FF6600';
        let sx = p.x + 14 + shift;
        const cy = p.y + p.height / 2;
        while (sx < p.x + p.width - 14) {
          ctx.beginPath();
          ctx.moveTo(sx - 5 * dir, cy - 6);
          ctx.lineTo(sx + 5 * dir, cy);
          ctx.lineTo(sx - 5 * dir, cy + 6);
          ctx.lineTo(sx - 1 * dir, cy);
          ctx.closePath();
          ctx.fill();
          sx += step;
        }
        [p.x + 10, p.x + p.width - 10].forEach((rx) => {
          ctx.fillStyle = '#CFD8DC';
          ctx.beginPath(); ctx.arc(rx, cy, 6.5, 0, Math.PI * 2); ctx.fill();
        });
        strokeRoundRect(ctx, p.x, p.y, p.width, p.height, 10, '#90A4AE', 2.5);
      } else if (p.type === 'BRICK_BRIDGE') {
        fillRoundRect(ctx, p.x, p.y, p.width, p.height, 8, '#A64B38');
        ctx.fillStyle = '#6D2C1E';
        ctx.beginPath();
        ctx.ellipse(p.x + p.width / 2, p.y + p.height, p.width * 0.32, 14, 0, Math.PI, 0);
        ctx.fill();
        ctx.fillStyle = '#E6D5B8';
        ctx.fillRect(p.x, p.y, p.width, 8);
        ctx.strokeStyle = '#263238';
        ctx.lineWidth = 2.2;
        ctx.beginPath();
        ctx.moveTo(p.x + 8, p.y - 12);
        ctx.lineTo(p.x + p.width - 8, p.y - 12);
        ctx.stroke();
        for (let px = p.x + 8; px <= p.x + p.width - 8; px += 38) {
          ctx.beginPath(); ctx.moveTo(px, p.y - 13); ctx.lineTo(px, p.y); ctx.stroke();
        }
      } else if (p.type === 'ROOFTOP') {
        fillRoundRect(ctx, p.x, p.y, p.width, p.height, 8, '#D85A2A');
        ctx.fillStyle = '#FF8A50';
        for (let rx = p.x + 8; rx < p.x + p.width - 14; rx += 24) {
          ctx.beginPath();
          ctx.arc(rx + 11, p.y + 9, 10, Math.PI, 0);
          ctx.fill();
        }
        strokeRoundRect(ctx, p.x, p.y, p.width, p.height, 8, '#8C2D12', 2.5);
      } else if (p.type === 'SPRING_PAD') {
        const pop = p.springAnimTimer > 0 ? Math.sin(p.springAnimTimer * 28) * 11 : 0;
        fillRoundRect(ctx, p.x + 4, p.y + p.height - 7, p.width - 8, 7, 3, '#002B54');
        ctx.strokeStyle = '#CFD8DC';
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.moveTo(p.x + 18, p.y + p.height - 6); ctx.lineTo(p.x + p.width - 18, p.y + 8 - pop);
        ctx.moveTo(p.x + p.width - 18, p.y + p.height - 6); ctx.lineTo(p.x + 18, p.y + 8 - pop);
        ctx.stroke();
        fillRoundRect(ctx, p.x, p.y - pop, p.width, 12, 6, '#FF6600');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(p.x + 14, p.y + 3 - pop, p.width - 28, 4);
      }
    }

    _drawQuestionBox(ctx, box, time) {
      const bob = !box.isHit ? Math.sin(time * 4.5 + box.id) * 2.5 : 0;
      const y = box.y - box.bumpOffset + bob;
      fillRoundRect(ctx, box.x + 3, y + 5, box.size, box.size, 10, 'rgba(0,22,48,0.18)');

      if (!box.isHit) {
        const pulse = Math.sin(time * 5.5 + box.id) * 0.5 + 0.5;
        fillRoundRect(ctx, box.x, y, box.size, box.size, 10, box.reward === 'BAKFIETS_SHIELD' ? '#FF6600' : '#0090E3');
        fillRoundRect(ctx, box.x + 4, y + 4, box.size - 8, 10, 6, 'rgba(255,255,255,0.26)');
        strokeRoundRect(ctx, box.x, y, box.size, box.size, 10, '#FFFFFF', 3 + pulse * 1.4);

        ctx.fillStyle = 'rgba(255,255,255,0.78)';
        [[8, 8], [box.size - 8, 8], [8, box.size - 8], [box.size - 8, box.size - 8]].forEach(([dx, dy]) => {
          ctx.beginPath(); ctx.arc(box.x + dx, y + dy, 2.5, 0, Math.PI * 2); ctx.fill();
        });

        this._setFont(ctx, 34, 'center');
        ctx.fillStyle = 'rgba(0,20,45,0.32)';
        ctx.fillText('?', box.x + box.size / 2 + 1.5, y + box.size / 2 + 13.5);
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText('?', box.x + box.size / 2, y + box.size / 2 + 12);
      } else {
        fillRoundRect(ctx, box.x, y, box.size, box.size, 8, '#78909C');
        strokeRoundRect(ctx, box.x, y, box.size, box.size, 8, '#455A64', 2.5);
        ctx.strokeStyle = '#FF6600';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.arc(box.x + box.size / 2, y + box.size / 2, 11, 0.35, Math.PI - 0.35);
        ctx.stroke();
      }
    }

    _drawSmileCoin(ctx, cx, cy, r, phase) {
      const spinW = Math.max(0.3, Math.abs(Math.cos(phase * 2.6))) * r;
      ctx.fillStyle = '#E68900';
      ctx.beginPath(); ctx.ellipse(cx, cy, spinW + 2, r, 0, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = '#FFC400';
      ctx.beginPath(); ctx.ellipse(cx, cy, spinW, r, 0, 0, Math.PI * 2); ctx.fill();
      ctx.strokeStyle = '#FF6600';
      ctx.lineWidth = 2.8;
      ctx.stroke();

      ctx.fillStyle = 'rgba(255,255,255,0.58)';
      ctx.beginPath(); ctx.ellipse(cx - spinW * 0.32, cy - r * 0.45, spinW * 0.22, r * 0.2, 0, 0, Math.PI * 2); ctx.fill();

      if (spinW > r * 0.52) {
        ctx.fillStyle = '#002B54';
        ctx.beginPath();
        ctx.arc(cx - spinW * 0.34, cy - 4, 2.3, 0, Math.PI * 2);
        ctx.arc(cx + spinW * 0.34, cy - 4, 2.3, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#002B54';
        ctx.lineWidth = 2.6;
        ctx.beginPath();
        ctx.ellipse(cx, cy + 2, spinW * 0.5, 6, 0, 0.25, Math.PI - 0.25);
        ctx.stroke();
      }
    }

    _drawMiniSmile(ctx, cx, cy, r) {
      ctx.fillStyle = '#FFC400';
      ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.fill();
      ctx.strokeStyle = '#002B54';
      ctx.lineWidth = 2;
      ctx.beginPath(); ctx.arc(cx, cy + r * 0.08, r * 0.5, 0.3, Math.PI - 0.3); ctx.stroke();
    }

    _drawBakfietsPowerUp(ctx, x, y, w, h, time) {
      const glow = 5 + Math.sin(time * 8) * 3.5;
      ctx.fillStyle = 'rgba(0,144,227,0.34)';
      ctx.beginPath(); ctx.arc(x + w / 2, y + h / 2, w * 0.68 + glow, 0, Math.PI * 2); ctx.fill();

      [x + 11, x + w - 11].forEach((wx) => {
        ctx.fillStyle = '#002B54';
        ctx.beginPath(); ctx.arc(wx, y + h - 8, 8, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath(); ctx.arc(wx, y + h - 8, 3.5, 0, Math.PI * 2); ctx.fill();
      });

      fillRoundRect(ctx, x + 20, y + 6, w - 24, h - 16, 5, '#0090E3');
      ctx.strokeStyle = '#FF6600';
      ctx.lineWidth = 3.5;
      ctx.beginPath();
      ctx.moveTo(x + 11, y + h - 8); ctx.lineTo(x + 22, y + 12);
      ctx.moveTo(x + 15, y + 8); ctx.lineTo(x + 24, y + 8);
      ctx.stroke();
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 2.4;
      ctx.beginPath(); ctx.arc(x + 31, y + 16, 6, 0.3, Math.PI - 0.3); ctx.stroke();
    }

    _drawEnemy(ctx, enemy, level) {
      if (!enemy.alive) {
        if (enemy.type === 'ONE_STAR_CLOUD') {
          const cx = enemy.x + enemy.width / 2, cy = enemy.y + enemy.height / 2;
          ctx.fillStyle = 'rgba(255,255,255,0.92)';
          ctx.beginPath();
          ctx.arc(cx - 14, cy + 2, 14, 0, Math.PI * 2);
          ctx.arc(cx, cy - 5, 18, 0, Math.PI * 2);
          ctx.arc(cx + 14, cy + 2, 14, 0, Math.PI * 2);
          ctx.fill();
          this._setFont(ctx, 18, 'center');
          ctx.fillStyle = '#FFC400';
          ctx.fillText('5★', cx, cy + 6);
        } else {
          this._drawMiniSmile(ctx, enemy.x + enemy.width / 2, enemy.y + enemy.height - 14, 18);
        }
        return;
      }

      if (enemy.type === 'SLOW_SNAIL') {
        this._drawProjectedShadow(ctx, enemy.x + enemy.width / 2, enemy.y + enemy.height, 22, level);
        const facingLeft = enemy.vx < 0;
        const cx = enemy.x + enemy.width / 2;
        const bottom = enemy.y + enemy.height;
        const squish = Math.sin(enemy.animTime * 7) * 2;

        fillRoundRect(ctx, enemy.x + 2 - squish, bottom - 15, enemy.width - 4 + squish * 2, 15, 8, '#90A4AE');
        const shellCx = facingLeft ? cx + 6 : cx - 6;
        ctx.fillStyle = '#D32F2F';
        ctx.beginPath(); ctx.arc(shellCx, bottom - 22, 17, 0, Math.PI * 2); ctx.fill();
        ctx.strokeStyle = '#FFCDD2';
        ctx.lineWidth = 2.8;
        ctx.beginPath(); ctx.arc(shellCx, bottom - 22, 9.5, 0, Math.PI * 2); ctx.stroke();

        const headX = facingLeft ? enemy.x + 11 : enemy.x + enemy.width - 11;
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath(); ctx.arc(headX, bottom - 22, 6.2, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#000000';
        ctx.beginPath(); ctx.arc(headX + (facingLeft ? -2 : 2), bottom - 22, 2.8, 0, Math.PI * 2); ctx.fill();
        ctx.strokeStyle = '#B71C1C';
        ctx.lineWidth = 2.8;
        ctx.beginPath(); ctx.moveTo(headX - 6, bottom - 30); ctx.lineTo(headX + 6, bottom - 26); ctx.stroke();
      } else {
        const cx = enemy.x + enemy.width / 2, cy = enemy.y + enemy.height / 2;
        if (Math.sin(enemy.animTime * 9) > 0.65) {
          ctx.strokeStyle = '#FFC400';
          ctx.lineWidth = 2.5;
          ctx.beginPath();
          ctx.moveTo(cx - 2, cy + 18); ctx.lineTo(cx + 4, cy + 26); ctx.lineTo(cx - 3, cy + 33);
          ctx.stroke();
        }
        ctx.fillStyle = '#37474F';
        ctx.beginPath();
        ctx.arc(cx - 16, cy + 4, 16, 0, Math.PI * 2);
        ctx.arc(cx, cy - 6, 21, 0, Math.PI * 2);
        ctx.arc(cx + 16, cy + 4, 16, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillRect(cx - 18, cy + 2, 36, 18);
        this._setFont(ctx, 18, 'center');
        ctx.fillStyle = '#FF5252';
        ctx.fillText('1★', cx, cy + 9);
      }
    }

    _drawFinishArea(ctx, engine) {
      const fx = engine.level.flagX;
      const gy = engine.level.flagGroundY;

      ctx.fillStyle = '#CFD8DC';
      ctx.fillRect(fx - 5, gy - 310, 10, 310);
      ctx.fillStyle = '#FF6600';
      ctx.beginPath(); ctx.arc(fx, gy - 316, 14, 0, Math.PI * 2); ctx.fill();
      fillRoundRect(ctx, fx - 24, gy - 26, 48, 26, 5, '#002B54');

      const flagY = gy - 295 + engine.flagSlideProgress * 190;
      ctx.fillStyle = '#0090E3';
      ctx.beginPath();
      ctx.moveTo(fx + 5, flagY);
      ctx.lineTo(fx + 108, flagY + 26);
      ctx.lineTo(fx + 5, flagY + 54);
      ctx.closePath();
      ctx.fill();
      ctx.strokeStyle = '#FF6600';
      ctx.lineWidth = 4;
      ctx.beginPath(); ctx.arc(fx + 41, flagY + 24, 18, 0.3, Math.PI - 0.3); ctx.stroke();

      if (engine.level.theme === 'WAREHOUSE') this._drawCoolblueVan(ctx, fx + 195, gy);
      else this._drawCustomerHouse(ctx, fx + 200, gy);

      // Both teammates cheering together!
      const mates = getTeammates(engine.selectedCharacter);
      let bubbleAnchorY = gy - 80;
      mates.forEach((mate, idx) => {
        const bounce = Math.abs(Math.sin(engine.worldTime * 6.4 + idx * 1.35)) * 16;
        const mx = fx + 68 + idx * 58;
        const my = gy - 62 - bounce;
        if (idx === 0) bubbleAnchorY = my;
        this._drawProjectedShadow(ctx, mx + 22, my + 62, 18, engine.level);
        this._drawHeroSprite(ctx, mx, my, 44, 62, mate, false, engine.worldTime * 4.2 + idx, bounce > 6, false, engine.worldTime, true, 1, 1, false);
      });

      const bubbleCx = fx + 118;
      fillRoundRect(ctx, bubbleCx - 118, bubbleAnchorY - 82, 236, 42, 14, '#FFFFFF');
      strokeRoundRect(ctx, bubbleCx - 118, bubbleAnchorY - 82, 236, 42, 14, '#FF6600', 3);
      this._setFont(ctx, 17, 'center');
      ctx.fillStyle = '#002B54';
      ctx.fillText(engine.hero.cheerForHero, bubbleCx, bubbleAnchorY - 55);
    }

    _drawCoolblueVan(ctx, x, gy) {
      ctx.fillStyle = 'rgba(0,22,48,0.25)';
      ctx.beginPath(); ctx.ellipse(x + 114, gy - 1, 102, 7, 0, 0, Math.PI * 2); ctx.fill();
      fillRoundRect(ctx, x, gy - 122, 224, 100, 16, '#0090E3');
      ctx.fillStyle = '#FF6600';
      ctx.fillRect(x + 8, gy - 34, 208, 8);
      fillRoundRect(ctx, x + 156, gy - 110, 56, 46, 8, '#B3E5FC');
      fillRoundRect(ctx, x + 214, gy - 52, 11, 14, 4, '#FFF59D');
      this._setFont(ctx, 26, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('coolblue', x + 78, gy - 72);
      ctx.fillStyle = '#FF6600';
      ctx.beginPath(); ctx.arc(x + 138, gy - 80, 6, 0, Math.PI * 2); ctx.fill();
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 3;
      ctx.beginPath(); ctx.arc(x + 78, gy - 60, 22, 0.3, Math.PI - 0.3); ctx.stroke();

      [x + 52, x + 174].forEach((wx) => {
        ctx.fillStyle = '#002B54';
        ctx.beginPath(); ctx.arc(wx, gy - 20, 20, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#CFD8DC';
        ctx.beginPath(); ctx.arc(wx, gy - 20, 9, 0, Math.PI * 2); ctx.fill();
      });
    }

    _drawCustomerHouse(ctx, x, gy) {
      ctx.fillStyle = '#C45B3E';
      ctx.fillRect(x, gy - 205, 210, 205);
      ctx.fillRect(x + 25, gy - 235, 160, 30);
      ctx.fillRect(x + 55, gy - 262, 100, 27);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(x + 22, gy - 238, 166, 5);
      ctx.fillRect(x + 52, gy - 265, 106, 5);

      fillRoundRect(ctx, x + 26, gy - 175, 58, 53, 6, '#FFE082');
      fillRoundRect(ctx, x + 126, gy - 175, 58, 53, 6, '#FFE082');
      fillRoundRect(ctx, x + 74, gy - 94, 62, 94, 8, '#FF6600');
      ctx.fillStyle = '#FFC400';
      ctx.beginPath(); ctx.arc(x + 124, gy - 46, 4.5, 0, Math.PI * 2); ctx.fill();

      fillRoundRect(ctx, x + 52, gy - 118, 106, 24, 8, '#0090E3');
      this._setFont(ctx, 15, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText('BLIJE KLANT', x + 105, gy - 101);
    }

    // =========================================================================
    // HERO SPRITES: MAX, REMI & VERO
    // =========================================================================
    _drawHeroSprite(ctx, x, y, width, height, charObj, facingRight, runningAnim, inAir, hasShield, worldTime, showNameTag, squashX = 1, stretchY = 1, isHurt = false) {
      const cx = x + width / 2;
      const cy = y + height / 2;

      if (hasShield) {
        const pulse = 4 + Math.sin(worldTime * 10) * 3;
        ctx.fillStyle = 'rgba(0,144,227,0.22)';
        ctx.beginPath(); ctx.arc(cx, cy, 42 + pulse, 0, Math.PI * 2); ctx.fill();
        ctx.strokeStyle = '#FF6600';
        ctx.lineWidth = 3;
        ctx.beginPath(); ctx.arc(cx, cy, 38 + pulse * 0.5, 0, Math.PI * 2); ctx.stroke();
        ctx.fillStyle = '#FFC400';
        for (let s = 0; s < 3; s++) {
          const ang = worldTime * 5.2 + (s * Math.PI * 2) / 3;
          ctx.beginPath(); ctx.arc(cx + Math.cos(ang) * 39, cy + Math.sin(ang) * 39, 4, 0, Math.PI * 2); ctx.fill();
        }
      }

      ctx.save();
      ctx.translate(cx, y + height);
      ctx.scale(facingRight ? squashX : -squashX, stretchY);
      ctx.translate(0, -height);

      const stride = inAir ? 9.5 : Math.sin(runningAnim * 2.4) * 10.5;

      // VERO's bouncy chestnut-brown ponytail!
      if (charObj.id === 'VERO') {
        const ponyBounce = inAir ? -6 : Math.abs(Math.cos(runningAnim * 2.4)) * 4.5;
        ctx.save();
        ctx.translate(-11, 7 + ponyBounce * 0.4);
        ctx.rotate(((inAir ? -24 : -10 - stride * 0.9) * Math.PI) / 180);
        ctx.fillStyle = '#FF6600';
        ctx.beginPath(); ctx.arc(0, 0, 3.8, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#5D3A1A';
        ctx.beginPath(); ctx.ellipse(-7, 4, 8, 6.5, 0, 0, Math.PI * 2); ctx.fill();
        ctx.beginPath(); ctx.arc(-13, 7, 4.5, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#8B5A2B';
        ctx.beginPath(); ctx.ellipse(-7, 2, 5, 2.8, 0, 0, Math.PI * 2); ctx.fill();
        ctx.restore();
      }

      // Back arm
      const backArmY = inAir ? 16 : 26 + stride * 0.35;
      fillRoundRect(ctx, -16, 22, 8, Math.max(8, backArmY - 15), 4, '#005F9E');
      ctx.fillStyle = '#F5C293';
      ctx.beginPath(); ctx.arc(-13, backArmY + 6, 4.5, 0, Math.PI * 2); ctx.fill();

      // Legs & sneakers with white soles
      fillRoundRect(ctx, -12 + stride * 0.45, 40, 10, 16, 4, '#002B54');
      fillRoundRect(ctx, 2 - stride * 0.45, 40, 10, 16, 4, '#002B54');
      [-13 + stride * 0.45, 1 - stride * 0.45].forEach((lx) => {
        fillRoundRect(ctx, lx, 53, 15, 8, 4, '#FF6600');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(lx, 59, 15, 3);
      });

      // Torso: Coolblue Uniform
      fillRoundRect(ctx, -15, 20, 30, 24, 8, '#0090E3');
      if (charObj.id === 'MAX') {
        ctx.fillStyle = '#FF6600';
        ctx.fillRect(-15, 35, 30, 5);
        fillRoundRect(ctx, -12, 23, 6, 5, 2, '#FFFFFF');
      } else if (charObj.id === 'REMI') {
        fillRoundRect(ctx, -15, 20, 30, 7.5, 6, '#FF6600');
      } else {
        ctx.fillStyle = '#00A896';
        ctx.fillRect(-15, 36, 30, 5);
        ctx.fillStyle = '#FF6600';
        ctx.beginPath();
        ctx.moveTo(-8, 20); ctx.lineTo(8, 20); ctx.lineTo(1, 27);
        ctx.closePath(); ctx.fill();
      }

      // White smile on chest
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 2.5;
      ctx.beginPath(); ctx.arc(1.5, 29, 6.5, 0.3, Math.PI - 0.3); ctx.stroke();

      // Front arm
      const armY = inAir ? 9 : 28 - stride * 0.38;
      fillRoundRect(ctx, 7, Math.min(armY, 22), 9, Math.max(7, 29 - Math.min(armY, 22)), 4, '#0090E3');
      ctx.fillStyle = '#FFD1A4';
      ctx.beginPath(); ctx.arc(14.5, armY, 5.2, 0, Math.PI * 2); ctx.fill();

      // Head
      ctx.fillStyle = '#FFD1A4';
      ctx.beginPath(); ctx.arc(0, 11, 13, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = 'rgba(255,105,97,0.26)';
      ctx.beginPath(); ctx.arc(7.5, 13.5, 3.2, 0, Math.PI * 2); ctx.fill();

      // Blink & expression
      const blinking = ((worldTime + charObj.ordinal * 1.17) % 3.7) < 0.11;
      if (blinking && !isHurt) {
        ctx.strokeStyle = '#002B54';
        ctx.lineWidth = 2.2;
        ctx.beginPath(); ctx.moveTo(2.5, 9.2); ctx.lineTo(8, 9.2); ctx.stroke();
      } else {
        ctx.fillStyle = '#002B54';
        ctx.beginPath(); ctx.arc(5.2, 9, 2.6, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#FFFFFF';
        ctx.beginPath(); ctx.arc(6, 8.1, 0.9, 0, Math.PI * 2); ctx.fill();
      }

      if (isHurt) {
        ctx.fillStyle = '#8D4E2A';
        ctx.beginPath(); ctx.arc(5.5, 15.5, 2.5, 0, Math.PI * 2); ctx.fill();
      } else {
        ctx.strokeStyle = '#8D4E2A';
        ctx.lineWidth = 2.1;
        ctx.beginPath(); ctx.arc(5.2, 13.5, 4.5, 0.2, Math.PI - 0.25); ctx.stroke();
      }

      // Headgear & Hair
      if (charObj.id === 'MAX') {
        ctx.fillStyle = '#4E342E';
        ctx.fillRect(-12, 4, 6, 8);
        ctx.fillStyle = '#0090E3';
        ctx.beginPath(); ctx.arc(0, 4.5, 14, Math.PI, 0); ctx.fill();
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 1.8;
        ctx.beginPath(); ctx.arc(1.5, 0.5, 4, 0.3, Math.PI - 0.3); ctx.stroke();
        fillRoundRect(ctx, 2, 2, 17.5, 5, 3, '#FF6600');
      } else if (charObj.id === 'REMI') {
        ctx.fillStyle = '#3E2723';
        ctx.beginPath(); ctx.arc(0, 4, 14, Math.PI * 0.96, Math.PI * 2.04); ctx.fill();
        ctx.beginPath();
        ctx.arc(-7, 0, 6, 0, Math.PI * 2);
        ctx.arc(2, -2.5, 6.2, 0, Math.PI * 2);
        ctx.arc(9, 0, 4.5, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = 'rgba(129,212,250,0.32)';
        ctx.fillRect(1.5, 6.5, 8.5, 6);
        strokeRoundRect(ctx, 1, 6, 9.5, 7, 2, '#002B54', 2.3);
        ctx.beginPath(); ctx.moveTo(-4, 9); ctx.lineTo(1, 9); ctx.stroke();
      } else {
        // VERO: Chestnut-brown hair + Coolblue Orange Headband + Klantgeluk Headset!
        ctx.fillStyle = '#5D3A1A';
        ctx.beginPath(); ctx.arc(0, 4.5, 14.2, Math.PI * 0.94, Math.PI * 2.06); ctx.fill();
        fillRoundRect(ctx, -13.5, 2, 6, 15, 4, '#5D3A1A');
        ctx.beginPath(); ctx.ellipse(4, 0.5, 8, 4.5, 0, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#8B5A2B';
        ctx.beginPath(); ctx.ellipse(4, -0.8, 5, 2.5, 0, 0, Math.PI * 2); ctx.fill();

        // Orange headband
        ctx.strokeStyle = '#FF6600';
        ctx.lineWidth = 3.6;
        ctx.beginPath(); ctx.arc(-0.5, 4, 11.5, Math.PI * 1.12, Math.PI * 1.82); ctx.stroke();

        // Headset earcup & mic boom
        ctx.fillStyle = '#002B54';
        ctx.beginPath(); ctx.arc(-3, 10.5, 3.6, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#00A896';
        ctx.beginPath(); ctx.arc(-3, 10.5, 1.7, 0, Math.PI * 2); ctx.fill();
        ctx.strokeStyle = '#002B54';
        ctx.lineWidth = 1.8;
        ctx.beginPath(); ctx.moveTo(-2, 12); ctx.lineTo(5.5, 16.5); ctx.stroke();
        ctx.fillStyle = '#FF6600';
        ctx.beginPath(); ctx.arc(6, 16.5, 1.8, 0, Math.PI * 2); ctx.fill();
      }

      ctx.restore();

      if (showNameTag) {
        fillRoundRect(ctx, cx - 29, y - 26, 58, 20, 9, charObj.accent);
        this._setFont(ctx, 13.5, 'center');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText(charObj.displayName.toUpperCase(), cx, y - 11.5);
      }
    }

    // =========================================================================
    // LANDSCAPE HUD & TOUCH PADS
    // =========================================================================
    _drawHudLandscape(ctx, engine) {
      const barH = Math.max(62, this.safe.top + 52);
      ctx.fillStyle = 'rgba(0,36,74,0.86)';
      ctx.fillRect(0, 0, this.virtualWidth, barH);
      ctx.fillStyle = '#FF6600';
      ctx.fillRect(0, barH - 3, this.virtualWidth, 3);

      const cy = barH - 24;
      const leftPad = Math.max(18, this.safe.left + 10);
      const rightEdge = this.virtualWidth - Math.max(14, this.safe.right + 8);

      fillRoundRect(ctx, leftPad, cy - 19, 76, 31, 10, engine.hero.accent);
      this._setFont(ctx, 16.5, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText(engine.hero.displayName.toUpperCase(), leftPad + 38, cy + 3);

      let heartsStr = '';
      for (let i = 1; i <= CONFIG.STARTING_HEARTS; i++) heartsStr += i <= engine.hearts ? '♥' : '♡';
      this._setFont(ctx, 24, 'left');
      ctx.fillStyle = '#FF5252';
      ctx.fillText(heartsStr, leftPad + 86, cy + 5);

      let nextLeft = leftPad + 166;
      if (engine.player.hasBakfietsShield) {
        fillRoundRect(ctx, nextLeft, cy - 19, 104, 31, 12, '#0090E3');
        this._setFont(ctx, 14, 'center');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillText('🚲 SCHILD', nextLeft + 52, cy + 2);
        nextLeft += 114;
      }

      let rx = rightEdge;
      const placeRight = (w, lbl, bg, act, fs = 16) => {
        this._drawPillButton(ctx, rx - w, cy - 20, w, 35, lbl, bg, '#FFF', act, fs);
        rx -= w + 6;
      };
      placeRight(38, '⛶', '#1D4366', 'TOGGLE_FULLSCREEN', 18);
      placeRight(38, '⌂', '#1D4366', 'RETURN_TITLE', 18);
      placeRight(38, '↺', '#1D4366', 'RESTART', 18);
      placeRight(40, engine.audio.isMuted ? '🔇' : '🔊', '#1D4366', 'TOGGLE_MUTE', 16);
      placeRight(40, '🎵', engine.audio.isMusicEnabled ? '#0090E3' : '#1D4366', 'TOGGLE_MUSIC', 16);
      placeRight(40, '🎮', engine.touchControlsVisible ? '#0090E3' : '#1D4366', 'TOGGLE_TOUCH', 16);
      placeRight(40, 'L2', engine.currentLevelNumber === 2 ? '#0090E3' : '#1D4366', 'START_LEVEL_2', 14.5);
      placeRight(40, 'L1', engine.currentLevelNumber === 1 ? '#0090E3' : '#1D4366', 'START_LEVEL_1', 14.5);
      placeRight(94, engine.demoModeEnabled ? '⚡ Demo' : '⚡ Norm', engine.demoModeEnabled ? '#FF6600' : '#005F9E', 'TOGGLE_DEMO', 15);

      const statsRight = rx - 12;
      this._setFont(ctx, 19.5, 'right');
      ctx.fillStyle = engine.timeRemaining < 20 ? '#FF6E6E' : '#B8E2FF';
      ctx.fillText(`⏱ ${Math.floor(engine.timeRemaining)}s`, statsRight, cy + 4);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText(`${engine.score} pt`, statsRight - 82, cy + 4);
      ctx.fillStyle = '#FFC400';
      ctx.fillText(`😄 ${engine.smilesCollected}`, statsRight - 190, cy + 4);

      const progAvail = (statsRight - 275) - nextLeft;
      if (progAvail >= 110) {
        this._setFont(ctx, 16, 'left');
        ctx.fillStyle = '#8CE0FF';
        ctx.fillText(progAvail < 185 ? engine.level.code : `${engine.level.code} ${engine.level.title}`, nextLeft, cy - 5);
        const prog = clamp(engine.player.x / engine.level.flagX, 0, 1);
        const barW = Math.min(210, progAvail - 8);
        fillRoundRect(ctx, nextLeft, cy + 3, barW, 10, 5, 'rgba(255,255,255,0.42)');
        fillRoundRect(ctx, nextLeft, cy + 3, Math.max(8, barW * prog), 10, 5, '#FF6600');
      }

      if (engine.bannerMessage && engine.screen === 'PLAYING') {
        const alpha = Math.min(1, engine.bannerTimer * 1.4) * 0.92;
        const bw = Math.min(620, this.virtualWidth - 120);
        fillRoundRect(ctx, this.virtualWidth / 2 - bw / 2, barH + 14, bw, 42, 21, `rgba(0,144,227,${alpha})`);
        this._setFont(ctx, 19.5, 'center');
        ctx.fillStyle = `rgba(255,255,255,${alpha})`;
        ctx.fillText(engine.bannerMessage, this.virtualWidth / 2, barH + 41);
      }
    }

    _drawTouchLandscape(ctx, pressedSet) {
      const leftX = Math.max(28, this.safe.left + 18);
      const rightX = this.virtualWidth - Math.max(28, this.safe.right + 18);
      const botY = this.virtualHeight - Math.max(26, this.safe.bottom + 14);
      this._drawTouchPad(ctx, leftX, botY - 108, 108, 108, '◀', 'TOUCH_LEFT', false, pressedSet.has('TOUCH_LEFT'));
      this._drawTouchPad(ctx, leftX + 124, botY - 108, 108, 108, '▶', 'TOUCH_RIGHT', false, pressedSet.has('TOUCH_RIGHT'));
      this._drawTouchPad(ctx, rightX - 128, botY - 124, 128, 124, 'SPRING', 'TOUCH_JUMP', true, pressedSet.has('TOUCH_JUMP'));
    }

    _drawTouchPad(ctx, x, y, w, h, label, action, isOrange, isPressed) {
      this.activeButtons.push({ action, x, y, w, h });
      const ins = isPressed ? 4 : 0;
      const bg = isOrange
        ? (isPressed ? 'rgba(255,136,34,0.92)' : 'rgba(255,102,0,0.68)')
        : (isPressed ? 'rgba(0,144,227,0.88)' : 'rgba(0,43,84,0.58)');
      fillRoundRect(ctx, x + ins, y + ins, w - ins * 2, h - ins * 2, 28, bg);
      strokeRoundRect(ctx, x + ins, y + ins, w - ins * 2, h - ins * 2, 28, isPressed ? '#FFFFFF' : 'rgba(255,255,255,0.84)', isPressed ? 4.5 : 3);
      this._setFont(ctx, label.length > 2 ? 23 : 36, 'center');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText(label, x + w / 2, y + h / 2 + 11);
    }

    // =========================================================================
    // OVERLAYS
    // =========================================================================
    _drawLevelClearOverlay(ctx, engine) {
      ctx.fillStyle = 'rgba(0,25,55,0.6)';
      ctx.fillRect(0, 0, this.virtualWidth, this.virtualHeight);
      const cw = Math.min(650, this.virtualWidth - 52), ch = 435;
      const cx = this.virtualWidth / 2 - cw / 2, cy = this.virtualHeight / 2 - ch / 2;
      fillRoundRect(ctx, cx, cy, cw, ch, 28, '#FFFFFF');
      strokeRoundRect(ctx, cx, cy, cw, ch, 28, '#0090E3', 6);

      this._setFont(ctx, 40, 'center');
      ctx.fillStyle = '#0090E3';
      ctx.fillText('PAKKET BEZORGD! 😄', this.virtualWidth / 2, cy + 74);

      const mates = getTeammates(engine.selectedCharacter);
      this._setFont(ctx, 21.5, 'center');
      ctx.fillStyle = '#FF6600';
      ctx.fillText(`${mates[0].displayName} & ${mates[1].displayName}: "${engine.hero.cheerForHero}"`, this.virtualWidth / 2, cy + 120);

      this._setFont(ctx, 25, 'center');
      ctx.fillStyle = '#002B54';
      ctx.fillText(`Score: ${engine.score} punten`, this.virtualWidth / 2, cy + 190);
      ctx.fillText(`Glimlachen verzameld: 😄 ${engine.smilesCollected}`, this.virtualWidth / 2, cy + 234);
      ctx.fillText(`Klanten blij gemaakt: ⭐ ${engine.enemiesSmiled}`, this.virtualWidth / 2, cy + 278);

      this._drawPillButton(ctx, cx + 48, cy + ch - 102, cw - 96, 72, 'DOOR NAAR 1-2 GRACHTENGORDEL ▶', '#FF6600', '#FFF', 'NEXT_LEVEL', 22);
    }

    _drawVictoryOverlay(ctx, engine) {
      ctx.fillStyle = 'rgba(0,25,55,0.66)';
      ctx.fillRect(0, 0, this.virtualWidth, this.virtualHeight);
      const mates = getTeammates(engine.selectedCharacter);

      if (this.isPortrait) {
        const cw = this.virtualWidth - 48;
        const ch = Math.min(880, this.virtualHeight - this.safe.top - this.safe.bottom - 60);
        const cx = this.virtualWidth / 2 - cw / 2, cy = this.virtualHeight / 2 - ch / 2;
        fillRoundRect(ctx, cx, cy, cw, ch, 28, '#FFFFFF');
        strokeRoundRect(ctx, cx, cy, cw, ch, 28, '#FF6600', 6.5);

        this._setFont(ctx, 35, 'center');
        ctx.fillStyle = '#0090E3';
        ctx.fillText('ALLES VOOR EEN GLIMLACH! 🎉', this.virtualWidth / 2, cy + 62);
        this._setFont(ctx, 19.5, 'center');
        ctx.fillStyle = '#FF6600';
        ctx.fillText(`${engine.hero.displayName}, ${mates[0].displayName} & ${mates[1].displayName} hebben alles bezorgd!`, this.virtualWidth / 2, cy + 100);

        fillRoundRect(ctx, cx + 28, cy + 124, cw - 56, 194, 18, '#EBF7FF');
        this._setFont(ctx, 22, 'left');
        ctx.fillStyle = '#002B54';
        ctx.fillText(`Bezorger: ${engine.hero.displayName}`, cx + 50, cy + 166);
        ctx.fillText(`Totaalscore: ${engine.score} pt`, cx + 50, cy + 204);
        ctx.fillText(`Glimlachen: 😄 ${engine.smilesCollected}   •   ⭐ ${engine.enemiesSmiled}`, cx + 50, cy + 242);
        ctx.fillText(`Tijd: ${Math.floor(engine.totalElapsedSeconds)} sec`, cx + 50, cy + 280);

        this._drawLeaderboardCard(ctx, cx + 28, cy + 334, cw - 56, ch - 514);
        this._drawPillButton(ctx, cx + 28, cy + ch - 160, cw - 56, 66, '↺  NOG EEN KEER SPELEN', '#FF6600', '#FFF', 'START_GAME', 22);
        this._drawPillButton(ctx, cx + 28, cy + ch - 82, cw - 56, 64, '⌂  KIES MAX / REMI / VERO', '#0090E3', '#FFF', 'RETURN_TITLE', 22);
      } else {
        const cw = Math.min(860, this.virtualWidth - 64), ch = 562;
        const cx = this.virtualWidth / 2 - cw / 2, cy = (this.virtualHeight - ch) / 2;
        fillRoundRect(ctx, cx, cy, cw, ch, 30, '#FFFFFF');
        strokeRoundRect(ctx, cx, cy, cw, ch, 30, '#FF6600', 7);

        this._setFont(ctx, 43, 'center');
        ctx.fillStyle = '#0090E3';
        ctx.fillText('ALLES VOOR EEN GLIMLACH! 🎉', this.virtualWidth / 2, cy + 70);
        this._setFont(ctx, 21.5, 'center');
        ctx.fillStyle = '#FF6600';
        ctx.fillText(`${engine.hero.displayName}, ${mates[0].displayName} & ${mates[1].displayName} hebben alle pakketten bezorgd!`, this.virtualWidth / 2, cy + 112);

        const boxW = cw / 2 - 48;
        fillRoundRect(ctx, cx + 34, cy + 142, boxW, ch - 262, 18, '#EBF7FF');
        this._setFont(ctx, 22.5, 'left');
        ctx.fillStyle = '#002B54';
        ctx.fillText(`Bezorger: ${engine.hero.displayName}`, cx + 56, cy + 194);
        ctx.fillText(`Totaalscore: ${engine.score} pt`, cx + 56, cy + 240);
        ctx.fillText(`Glimlachen: 😄 ${engine.smilesCollected}`, cx + 56, cy + 286);
        ctx.fillText(`Vijanden blij: ⭐ ${engine.enemiesSmiled}`, cx + 56, cy + 332);
        ctx.fillText(`Tijd: ${Math.floor(engine.totalElapsedSeconds)} sec`, cx + 56, cy + 378);

        this._drawLeaderboardCard(ctx, this.virtualWidth / 2 + 14, cy + 142, boxW, ch - 262);
        this._drawPillButton(ctx, cx + 38, cy + ch - 94, cw / 2 - 50, 68, '↺  NOG EEN KEER SPELEN', '#FF6600', '#FFF', 'START_GAME', 21);
        this._drawPillButton(ctx, this.virtualWidth / 2 + 12, cy + ch - 94, cw / 2 - 50, 68, '⌂  WISSEL MAX / REMI / VERO', '#0090E3', '#FFF', 'RETURN_TITLE', 20);
      }
    }

    _drawGameOverOverlay(ctx, engine) {
      ctx.fillStyle = 'rgba(0,20,45,0.66)';
      ctx.fillRect(0, 0, this.virtualWidth, this.virtualHeight);
      const cw = Math.min(590, this.virtualWidth - 48), ch = 370;
      const cx = this.virtualWidth / 2 - cw / 2, cy = this.virtualHeight / 2 - ch / 2;
      fillRoundRect(ctx, cx, cy, cw, ch, 28, '#FFFFFF');
      strokeRoundRect(ctx, cx, cy, cw, ch, 28, '#E53935', 6);

      this._setFont(ctx, 40, 'center');
      ctx.fillStyle = '#D32F2F';
      ctx.fillText('OEPS, VERTRAGING!', this.virtualWidth / 2, cy + 76);
      this._setFont(ctx, 21.5, 'center');
      ctx.fillStyle = '#002B54';
      ctx.fillText(`Geen zorgen ${engine.hero.displayName} — probeer het direct opnieuw!`, this.virtualWidth / 2, cy + 130);
      this._setFont(ctx, 24, 'center');
      ctx.fillStyle = '#0090E3';
      ctx.fillText(`Score: ${engine.score} pt  •  😄 ${engine.smilesCollected}`, this.virtualWidth / 2, cy + 182);

      this._drawPillButton(ctx, cx + 36, cy + ch - 102, cw / 2 - 46, 70, '↺  OPNIEUW (R)', '#FF6600', '#FFF', 'RESTART', 21);
      this._drawPillButton(ctx, this.virtualWidth / 2 + 10, cy + ch - 102, cw / 2 - 46, 70, '⌂  STARTMENU', '#0090E3', '#FFF', 'RETURN_TITLE', 21);
    }
  }

  // =========================================================================
  // BOOTSTRAP, INPUT (KEYBOARD + MULTI-TOUCH + GAMEPAD) & MAIN LOOP
  // =========================================================================
  function boot() {
    const canvas = document.getElementById('game-canvas');
    const probe = document.getElementById('safe-area-probe');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    const audio = new window.CoolblueAudio();
    const engine = new GameEngine(audio);
    const renderer = new GameRenderer();

    // Expose handle for automated Playwright verification & live debugging
    window.__COOLBLUE_GAME__ = { engine, renderer, audio };

    let cssW = window.innerWidth;
    let cssH = window.innerHeight;
    let rawSafe = { left: 0, top: 0, right: 0, bottom: 0 };

    function resizeCanvas() {
      cssW = Math.max(320, window.innerWidth || document.documentElement.clientWidth || 960);
      cssH = Math.max(320, window.innerHeight || document.documentElement.clientHeight || 600);
      const dpr = Math.min(window.devicePixelRatio || 1, 3);
      canvas.width = Math.round(cssW * dpr);
      canvas.height = Math.round(cssH * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

      if (probe) {
        const cs = window.getComputedStyle(probe);
        rawSafe = {
          top: parseFloat(cs.top) || 0,
          right: parseFloat(cs.right) || 0,
          bottom: parseFloat(cs.bottom) || 0,
          left: parseFloat(cs.left) || 0
        };
      }
    }
    window.addEventListener('resize', resizeCanvas);
    window.addEventListener('orientationchange', () => setTimeout(resizeCanvas, 80));
    if (typeof ResizeObserver !== 'undefined') {
      new ResizeObserver(resizeCanvas).observe(document.documentElement);
    }
    resizeCanvas();

    function toggleFullscreen() {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen?.().catch(() => {});
      } else {
        document.exitFullscreen?.().catch(() => {});
      }
    }

    let keyLeft = false, keyRight = false, keyJump = false, jumpQueued = false;
    const activePointers = new Map();
    let padPrevJump = false;

    function isContinuous(act) {
      return act === 'TOUCH_LEFT' || act === 'TOUCH_RIGHT' || act === 'TOUCH_JUMP';
    }

    function findHitAction(vx, vy, allowSlop = true) {
      for (let i = renderer.activeButtons.length - 1; i >= 0; i--) {
        const b = renderer.activeButtons[i];
        if (vx >= b.x && vx <= b.x + b.w && vy >= b.y && vy <= b.y + b.h) return b.action;
      }
      if (allowSlop) {
        for (let i = renderer.activeButtons.length - 1; i >= 0; i--) {
          const b = renderer.activeButtons[i];
          if (!isContinuous(b.action)) continue;
          const pad = 22;
          if (vx >= b.x - pad && vx <= b.x + b.w + pad && vy >= b.y - pad && vy <= b.y + b.h + pad) return b.action;
        }
      }
      return null;
    }

    function dispatchClick(action) {
      audio.unlock();
      switch (action) {
        case 'SELECT_MAX': engine.selectCharacter('MAX'); break;
        case 'SELECT_REMI': engine.selectCharacter('REMI'); break;
        case 'SELECT_VERO': engine.selectCharacter('VERO'); break;
        case 'START_GAME': engine.startNewGame(1); break;
        case 'START_LEVEL_1': engine.jumpToLevel(1); break;
        case 'START_LEVEL_2': engine.jumpToLevel(2); break;
        case 'TOGGLE_DEMO': engine.toggleDemoMode(); break;
        case 'TOGGLE_TOUCH': engine.toggleTouchControls(); break;
        case 'TOGGLE_MUSIC': engine.toggleMusic(); break;
        case 'TOGGLE_MUTE': engine.toggleMute(); break;
        case 'TOGGLE_FULLSCREEN': toggleFullscreen(); break;
        case 'RESTART': engine.restartCurrentLevel(); break;
        case 'RETURN_TITLE': engine.returnToTitle(); break;
        case 'NEXT_LEVEL': engine.advanceFromLevelClear(); break;
      }
    }

    canvas.addEventListener('pointerdown', (e) => {
      e.preventDefault();
      audio.unlock();
      canvas.setPointerCapture?.(e.pointerId);
      const { x, y } = renderer.screenToVirtual(e.clientX, e.clientY);
      const hit = findHitAction(x, y, true);
      if (hit) {
        if (isContinuous(hit)) {
          activePointers.set(e.pointerId, hit);
          if (hit === 'TOUCH_JUMP') jumpQueued = true;
        } else {
          dispatchClick(hit);
        }
      }
    }, { passive: false });

    canvas.addEventListener('pointermove', (e) => {
      if (!activePointers.has(e.pointerId)) return;
      e.preventDefault();
      const { x, y } = renderer.screenToVirtual(e.clientX, e.clientY);
      const hit = findHitAction(x, y, true);
      if (hit && isContinuous(hit)) {
        if (activePointers.get(e.pointerId) !== 'TOUCH_JUMP' && hit === 'TOUCH_JUMP') jumpQueued = true;
        activePointers.set(e.pointerId, hit);
      } else {
        activePointers.delete(e.pointerId);
      }
    }, { passive: false });

    const releasePointer = (e) => {
      activePointers.delete(e.pointerId);
    };
    canvas.addEventListener('pointerup', releasePointer);
    canvas.addEventListener('pointercancel', releasePointer);

    window.addEventListener('keydown', (e) => {
      audio.unlock();
      const repeat = e.repeat;
      switch (e.code) {
        case 'ArrowLeft':
        case 'KeyA':
          e.preventDefault();
          if (engine.screen === 'TITLE' && !repeat) engine.cycleCharacter(-1);
          else keyLeft = true;
          break;
        case 'ArrowRight':
        case 'KeyD':
          e.preventDefault();
          if (engine.screen === 'TITLE' && !repeat) engine.cycleCharacter(1);
          else keyRight = true;
          break;
        case 'Space':
        case 'ArrowUp':
        case 'KeyW':
        case 'KeyZ':
          e.preventDefault();
          if (!repeat) {
            if (engine.screen === 'TITLE') engine.startNewGame(1);
            else if (engine.screen === 'LEVEL_CLEAR') engine.advanceFromLevelClear();
            else if (engine.screen === 'GAME_OVER') engine.restartCurrentLevel();
            else if (engine.screen === 'VICTORY') engine.startNewGame(1);
            else { keyJump = true; jumpQueued = true; }
          }
          break;
        case 'Enter':
        case 'NumpadEnter':
          e.preventDefault();
          if (!repeat) {
            if (engine.screen === 'TITLE') engine.startNewGame(1);
            else if (engine.screen === 'LEVEL_CLEAR') engine.advanceFromLevelClear();
            else if (engine.screen === 'GAME_OVER') engine.restartCurrentLevel();
            else if (engine.screen === 'VICTORY') engine.startNewGame(1);
          }
          break;
        case 'KeyR':
          if (!repeat && engine.screen !== 'TITLE') engine.restartCurrentLevel();
          break;
        case 'KeyM':
          if (!repeat) engine.toggleMute();
          break;
        case 'KeyB':
          if (!repeat) engine.toggleMusic();
          break;
        case 'KeyG':
          if (!repeat) engine.toggleDemoMode();
          break;
        case 'KeyT':
          if (!repeat) engine.toggleTouchControls();
          break;
        case 'KeyF':
          if (!repeat) toggleFullscreen();
          break;
        case 'Digit1':
          if (!repeat) engine.jumpToLevel(1);
          break;
        case 'Digit2':
          if (!repeat) engine.jumpToLevel(2);
          break;
        case 'Escape':
          if (!repeat && engine.screen !== 'TITLE') engine.returnToTitle();
          break;
      }
    });

    window.addEventListener('keyup', (e) => {
      if (e.code === 'ArrowLeft' || e.code === 'KeyA') keyLeft = false;
      else if (e.code === 'ArrowRight' || e.code === 'KeyD') keyRight = false;
      else if (e.code === 'Space' || e.code === 'ArrowUp' || e.code === 'KeyW' || e.code === 'KeyZ') keyJump = false;
    });

    function readGamepad() {
      const pads = navigator.getGamepads ? navigator.getGamepads() : [];
      for (const gp of pads) {
        if (!gp) continue;
        const left = (gp.axes[0] < -0.35) || gp.buttons[14]?.pressed;
        const right = (gp.axes[0] > 0.35) || gp.buttons[15]?.pressed;
        const jump = Boolean(gp.buttons[0]?.pressed || gp.buttons[1]?.pressed || gp.buttons[12]?.pressed);
        const justJump = jump && !padPrevJump;
        padPrevJump = jump;
        return { left, right, jump, justJump };
      }
      padPrevJump = false;
      return { left: false, right: false, jump: false, justJump: false };
    }

    let lastTime = performance.now();
    function frame(now) {
      const dt = (now - lastTime) / 1000;
      lastTime = now;

      let tLeft = false, tRight = false, tJump = false;
      for (const act of activePointers.values()) {
        if (act === 'TOUCH_LEFT') tLeft = true;
        if (act === 'TOUCH_RIGHT') tRight = true;
        if (act === 'TOUCH_JUMP') tJump = true;
      }
      const gp = readGamepad();

      const input = {
        left: keyLeft || tLeft || gp.left,
        right: keyRight || tRight || gp.right,
        jumpHeld: keyJump || tJump || gp.jump,
        jumpJustPressed: jumpQueued || gp.justJump
      };
      jumpQueued = false;

      const pressedSet = new Set();
      if (input.left) pressedSet.add('TOUCH_LEFT');
      if (input.right) pressedSet.add('TOUCH_RIGHT');
      if (input.jumpHeld) pressedSet.add('TOUCH_JUMP');

      engine.update(dt, input);
      renderer.render(ctx, cssW, cssH, rawSafe, engine, pressedSet);
      requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);

    if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
      navigator.serviceWorker.register('./sw.js').catch(() => {});
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
