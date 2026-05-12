    (async () => {
        'use strict';

        const LOCALE_URL = new URL('locales/en.json', document.baseURI).href;
        const STRINGS = await fetch(LOCALE_URL).then(resp => {
            if (!resp.ok) {
                throw new Error(`Failed to load locales: HTTP ${resp.status}`);
            }
            return resp.json();
        });

        function t(key) {
            const val = STRINGS[key];
            if (typeof val !== 'string') {
                console.warn('Missing locale entry:', key);
                return key;
            }
            return val;
        }

        function format(template, vars) {
            let s = template;
            for (const [k, v] of Object.entries(vars)) {
                s = s.split(`{${k}}`).join(String(v));
            }
            return s;
        }

        function hydrateStaticStrings() {
            document.querySelector('meta[name="description"]')
                    ?.setAttribute('content', t('meta.description'));
            document.title = t('meta.pageTitle');
            document.querySelectorAll('[data-i18n]').forEach(el => {
                el.textContent = t(el.dataset.i18n);
            });
            document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
                el.setAttribute('placeholder', t(el.dataset.i18nPlaceholder));
            });
            document.querySelectorAll('[data-i18n-title]').forEach(el => {
                el.setAttribute('title', t(el.dataset.i18nTitle));
            });
            document.querySelectorAll('[data-i18n-aria-label]').forEach(el => {
                el.setAttribute('aria-label', t(el.dataset.i18nAriaLabel));
            });
        }

        hydrateStaticStrings();

        // ── DOM refs ──────────────────────────────────────────────────────────
        const screenStart   = document.getElementById('screen-start');
        const screenGame    = document.getElementById('screen-game');
        const screenResult  = document.getElementById('screen-result');

        const canvas        = document.getElementById('game-canvas');
        const ctx           = canvas.getContext('2d');

        const scoreValue    = document.getElementById('score-value');
        const scoreAnn      = document.getElementById('score-announcer');
        const gameAnn       = document.getElementById('game-announcer');

        const resultHeading = document.getElementById('result-heading');
        const resultMsg     = document.getElementById('result-message');
        const resultIcon    = document.getElementById('result-icon');
        const finalScore    = document.getElementById('final-score');

        const btnStart      = document.getElementById('btn-start');
        const btnPause      = document.getElementById('btn-pause');
        const btnQuit       = document.getElementById('btn-quit');
        const btnPlayAgain  = document.getElementById('btn-play-again');
        const btnMainMenu   = document.getElementById('btn-main-menu');
        const btnSubmit     = document.getElementById('btn-submit');
        const submitForm    = document.getElementById('submit-form');
        const submitStatus  = document.getElementById('submit-status');
        const nicknameInput = document.getElementById('nickname-input');
        const contrastToggle= document.getElementById('contrast-toggle');

        // ── Helpers: screen management with focus ─────────────────────────────
        function showScreen(toShow, focusEl) {
            [screenStart, screenGame, screenResult].forEach(s => {
                const isShown = s === toShow;
                s.hidden = !isShown;
                s.setAttribute('aria-hidden', String(!isShown));
            });
            if (focusEl) {
                // Defer one tick so the element is visible before focusing
                requestAnimationFrame(() => focusEl.focus());
            }
        }

        // ── High-contrast toggle ──────────────────────────────────────────────
        contrastToggle.addEventListener('click', () => {
            const isHC = document.body.classList.toggle('high-contrast');
            contrastToggle.setAttribute('aria-pressed', String(isHC));
            // Re-draw canvas with updated colours
            if (gameRunning) drawFrame();
        });

        // ── Difficulty ────────────────────────────────────────────────────────
        const TICK_MS = { EASY: 150, MEDIUM: 100, HARD: 60 };
        const WIN_SCORE = 50;
        let difficulty  = 'EASY';

        function selectedDifficulty() {
            return document.querySelector('input[name="difficulty"]:checked')?.value ?? 'EASY';
        }

        // ── HiDPI canvas setup ────────────────────────────────────────────────
        // Scale the canvas backing store by devicePixelRatio so it renders
        // sharply on retina / high-DPI displays. The CSS size stays at 480px
        // (controlled by the stylesheet); only the internal resolution scales.
        const LOGICAL_SIZE = 480;
        function initCanvasResolution() {
            const dpr = window.devicePixelRatio || 1;
            canvas.width  = LOGICAL_SIZE * dpr;
            canvas.height = LOGICAL_SIZE * dpr;
            ctx.scale(dpr, dpr);
        }
        initCanvasResolution();

        // ── Game state ────────────────────────────────────────────────────────
        const COLS = 20, ROWS = 20;
        const CELL = LOGICAL_SIZE / COLS;   // always based on logical pixels

        let snake, direction, nextDir, food, score, gameRunning, paused, loopId;

        function initGame() {
            snake     = [{ x: 10, y: 10 }];
            direction = { x: 1, y: 0 };
            nextDir   = { x: 1, y: 0 };
            score     = 0;
            paused    = false;
            gameRunning = true;
            placeFood();
            updateScoreDisplay(0);
            btnPause.textContent   = t('game.pauseInitial');
            btnPause.setAttribute('aria-pressed', 'false');
        }

        // Use crypto.getRandomValues() rather than Math.random() so the food
        // placement uses a cryptographically unpredictable source.
        function randInt(max) {
            const buf = new Uint32Array(1);
            crypto.getRandomValues(buf);
            return buf[0] % max;
        }

        function placeFood() {
            let pos;
            do {
                pos = { x: randInt(COLS), y: randInt(ROWS) };
            } while (snake.some(s => s.x === pos.x && s.y === pos.y));
            food = pos;
        }

        // ── Score ─────────────────────────────────────────────────────────────
        function updateScoreDisplay(newScore) {
            score = newScore;
            scoreValue.textContent = score;
            // Polite live region – announced without interrupting the user
            scoreAnn.textContent = format(t('announce.scoreUpdated'), { score });
        }

        // ── Game loop ─────────────────────────────────────────────────────────
        function tick() {
            if (!gameRunning || paused) return;

            direction = { ...nextDir };

            const head = {
                x: snake[0].x + direction.x,
                y: snake[0].y + direction.y
            };

            // Wall collision
            if (head.x < 0 || head.x >= COLS || head.y < 0 || head.y >= ROWS) {
                endGame(false); return;
            }
            // Self collision
            if (snake.some(s => s.x === head.x && s.y === head.y)) {
                endGame(false); return;
            }

            snake.unshift(head);

            if (head.x === food.x && head.y === food.y) {
                updateScoreDisplay(score + 1);
                if (score >= WIN_SCORE) { endGame(true); return; }
                placeFood();
            } else {
                snake.pop();
            }

            drawFrame();
        }

        function startLoop() {
            clearInterval(loopId);
            loopId = setInterval(tick, TICK_MS[difficulty]);
        }

        function stopLoop() {
            clearInterval(loopId);
        }

        // ── Touch / swipe input (mobile gameplay) ────────────────────────────
        // Minimum swipe distance in CSS pixels to register as intentional.
        const SWIPE_THRESHOLD = 20;
        let touchStartX = 0, touchStartY = 0;

        canvas.addEventListener('touchstart', e => {
            e.preventDefault();
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
        }, { passive: false });

        canvas.addEventListener('touchend', e => {
            e.preventDefault();
            const dx = e.changedTouches[0].clientX - touchStartX;
            const dy = e.changedTouches[0].clientY - touchStartY;

            if (Math.abs(dx) < SWIPE_THRESHOLD && Math.abs(dy) < SWIPE_THRESHOLD) return;
            if (!gameRunning || paused) return;

            let d;
            if (Math.abs(dx) >= Math.abs(dy)) {
                d = dx > 0 ? { x: 1, y: 0 } : { x: -1, y: 0 };
            } else {
                d = dy > 0 ? { x: 0, y: 1 } : { x: 0, y: -1 };
            }
            // Prevent 180° reversal
            if (d.x !== -direction.x || d.y !== -direction.y) nextDir = d;
        }, { passive: false });

        // ── Drawing ───────────────────────────────────────────────────────────
        const COLOR = {
            bg:     () => document.body.classList.contains('high-contrast') ? '#000' : '#0f172a',
            grid:   () => document.body.classList.contains('high-contrast') ? '#333' : '#1e293b',
            snake:  () => document.body.classList.contains('high-contrast') ? '#6bff6b' : '#4ade80',
            head:   () => document.body.classList.contains('high-contrast') ? '#a3ffaa' : '#86efac',
            food:   () => document.body.classList.contains('high-contrast') ? '#ff6b6b' : '#f87171',
            text:   () => document.body.classList.contains('high-contrast') ? '#fff'    : '#94a3b8',
        };

        function drawFrame() {
            const W = canvas.width, H = canvas.height;

            // Background
            ctx.fillStyle = COLOR.bg();
            ctx.fillRect(0, 0, W, H);

            // Subtle grid lines
            ctx.strokeStyle = COLOR.grid();
            ctx.lineWidth = 0.5;
            for (let x = 0; x <= W; x += CELL) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
            for (let y = 0; y <= H; y += CELL) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }

            // Food
            ctx.fillStyle = COLOR.food();
            ctx.beginPath();
            ctx.arc(food.x * CELL + CELL/2, food.y * CELL + CELL/2, CELL/2 - 2, 0, Math.PI * 2);
            ctx.fill();

            // Snake segments
            snake.forEach((seg, i) => {
                ctx.fillStyle = i === 0 ? COLOR.head() : COLOR.snake();
                const r = 3;
                const x = seg.x * CELL + 1, y = seg.y * CELL + 1,
                      w = CELL - 2,          h = CELL - 2;
                ctx.beginPath();
                ctx.moveTo(x + r, y);
                ctx.arcTo(x + w, y, x + w, y + h, r);
                ctx.arcTo(x + w, y + h, x, y + h, r);
                ctx.arcTo(x, y + h, x, y, r);
                ctx.arcTo(x, y, x + w, y, r);
                ctx.closePath();
                ctx.fill();
            });

            // Pause overlay
            if (paused) {
                ctx.fillStyle = 'rgba(0,0,0,.65)';
                ctx.fillRect(0, 0, W, H);
                ctx.fillStyle = '#fff';
                ctx.font = 'bold 32px system-ui';
                ctx.textAlign = 'center';
                ctx.fillText(t('canvas.pausedHeading'), W / 2, H / 2 - 10);
                ctx.font = '14px system-ui';
                ctx.fillStyle = COLOR.text();
                ctx.fillText(t('canvas.pausedHint'), W / 2, H / 2 + 20);
            }
        }

        // ── End game ──────────────────────────────────────────────────────────
        function endGame(won) {
            gameRunning = false;
            stopLoop();

            // Assertive live region – interrupts the screen reader immediately
            gameAnn.textContent = won ? t('announce.youWin') : t('announce.gameOver');

            finalScore.textContent = score;
            submitStatus.textContent = '';
            submitStatus.className = '';
            nicknameInput.value = '';
            btnSubmit.disabled = false;

            if (won) {
                resultIcon.textContent    = '🏆';
                resultHeading.textContent = t('result.winTitle');
                resultMsg.textContent     = format(t('result.winBody'), { score, difficulty });
            } else {
                resultIcon.textContent    = '💀';
                resultHeading.textContent = t('result.gameOverTitle');
                resultMsg.textContent     = format(t('result.lossBody'), { score, difficulty });
            }

            showScreen(screenResult, resultHeading);
        }

        // ── Keyboard input ────────────────────────────────────────────────────
        const DIR_MAP = {
            ArrowUp:    { x: 0, y: -1 }, KeyW:       { x: 0, y: -1 },
            ArrowDown:  { x: 0, y:  1 }, KeyS:       { x: 0, y:  1 },
            ArrowLeft:  { x:-1, y:  0 }, KeyA:       { x:-1, y:  0 },
            ArrowRight: { x: 1, y:  0 }, KeyD:       { x: 1, y:  0 },
        };

        document.addEventListener('keydown', e => {
            // Global shortcuts
            if (e.code === 'KeyP' && screenGame && !screenGame.hidden) {
                e.preventDefault();
                togglePause();
                return;
            }
            if (e.code === 'KeyQ' && screenGame && !screenGame.hidden) {
                e.preventDefault();
                handleQuit();
                return;
            }

            const d = DIR_MAP[e.code];
            if (!d) return;

            // Prevent arrow keys from scrolling the page while playing
            if (!screenGame.hidden) e.preventDefault();

            if (!gameRunning || paused) return;
            // Prevent 180° reversal
            if (d.x !== -direction.x || d.y !== -direction.y) {
                nextDir = d;
            }
        });

        // ── Pause ─────────────────────────────────────────────────────────────
        function togglePause() {
            if (!gameRunning) return;
            paused = !paused;
            btnPause.textContent = paused ? t('game.resumeToggle') : t('game.pauseInitial');
            btnPause.setAttribute('aria-pressed', String(paused));
            if (paused) {
                stopLoop();
                gameAnn.textContent = t('announce.paused');
                drawFrame();
            } else {
                gameAnn.textContent = t('announce.resumed');
                startLoop();
            }
        }

        // ── Quit ──────────────────────────────────────────────────────────────
        function handleQuit() {
            gameRunning = false;
            stopLoop();
            showScreen(screenStart, btnStart);
        }

        // ── Button wiring ─────────────────────────────────────────────────────
        btnStart.addEventListener('click', () => {
            difficulty = selectedDifficulty();
            initGame();
            showScreen(screenGame, canvas);
            // Kick off the first draw then start the loop
            drawFrame();
            startLoop();
        });

        btnPause.addEventListener('click', togglePause);
        btnQuit.addEventListener('click', handleQuit);

        btnPlayAgain.addEventListener('click', () => {
            // Replay with same difficulty
            initGame();
            showScreen(screenGame, canvas);
            drawFrame();
            startLoop();
        });

        btnMainMenu.addEventListener('click', () => {
            showScreen(screenStart, btnStart);
        });

        // ── Score submission ──────────────────────────────────────────────────
        submitForm.addEventListener('submit', async e => {
            e.preventDefault();

            // Capture safe, validated values before any async work
            const rawNickname = nicknameInput.value.trim();
            const diff        = difficulty;          // one of EASY | MEDIUM | HARD
            const sc          = score;               // non-negative integer

            const NICKNAME_RE = /^[A-Za-z0-9_]{1,20}$/;
            if (!NICKNAME_RE.test(rawNickname)) {
                submitStatus.textContent = t('nickname.clientValidationError');
                submitStatus.className   = 'error';
                nicknameInput.focus();
                return;
            }

            // Snapshot validated value to ensure no mutation after validation
            const nickname = rawNickname;

            btnSubmit.disabled = true;
            submitStatus.textContent = '';
            submitStatus.className   = '';

            try {
                const res = await fetch('/api/v1/highscores', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ nickname, score: sc, difficulty: diff })
                });

                if (res.ok) {
                    submitStatus.textContent = t('submit.success');
                    submitStatus.className   = 'success';
                } else {
                    const body = await res.json().catch(() => ({}));
                    submitStatus.textContent = body.message ?? t('submit.fallbackError');
                    submitStatus.className   = 'error';
                    btnSubmit.disabled = false;
                }
            } catch {
                submitStatus.textContent = t('submit.networkError');
                submitStatus.className   = 'error';
                btnSubmit.disabled = false;
            }
        });

        // ── CSS :has() polyfill for older browsers ────────────────────────────
        // Firefox < 121 and Safari < 15.4 do not support :has(). The .checked
        // class is toggled via JS so the selected difficulty is still visually
        // highlighted in those browsers.
        function updateDifficultyHighlight() {
            document.querySelectorAll('.difficulty-options label').forEach(label => {
                const input = label.querySelector('input');
                label.classList.toggle('checked', input ? input.checked : false);
            });
        }
        document.querySelectorAll('input[name="difficulty"]').forEach(input => {
            input.addEventListener('change', updateDifficultyHighlight);
        });
        updateDifficultyHighlight(); // apply on load

        // ── Init: show start screen ───────────────────────────────────────────
        showScreen(screenStart, btnStart);

    })();
