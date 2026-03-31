// game.js — orchestrator: state, WebSocket, input, DOM updates
(function () {
    var chart = window.Baz.chart;
    var draw = window.Baz.draw;

    var el = document.getElementById("game-data");
    var gameId = el.dataset.gameId;
    var playerId = el.dataset.playerId;

    var protocol = location.protocol === "https:" ? "wss:" : "ws:";
    var ws = new WebSocket(protocol + "//" + location.host + "/game/" + gameId);

    // --------------- state ---------------
    var state = {
        status: "Connecting...",
        playerId: playerId,
        joined: false,
        allPlayersReady: false,
        gameFinished: false,
        localFreezeActive: false,
        currentTick: 0,
        ticksRemaining: null,
        players: {},
        prices: {},
        prevPrices: {},
        marketCaps: {},
        symbols: [],
        chart: chart.createChartState(180),
        hoveredSymbol: null,
        tradingDisabledReason: "",
        inventory: [],
        delistedSymbols: {},
        darkPool: null, // { tierName, targetSymbol, tokens, ticks }
        bubbles: {},    // { symbol: { factor, threshold } }
        liquidity: {},  // { symbol: { remaining, max } }
    };

    // --------------- helpers ---------------
    function formatPrice(cents) {
        if (cents === undefined || cents === null) return "--";
        return "$" + (cents / 100).toFixed(2);
    }

    function holdingText(holdings) {
        if (!holdings) return "no shares";
        var symbols = Object.keys(holdings).sort();
        var entries = [];
        for (var i = 0; i < symbols.length; i++) {
            var symbol = symbols[i];
            if (holdings[symbol] > 0) {
                var text = symbol + ":" + holdings[symbol];
                if (state.delistedSymbols[symbol]) {
                    entries.push('<span class="holding--delisted">' + text + '</span>');
                } else {
                    entries.push(text);
                }
            }
        }
        return entries.length > 0 ? entries.join(" ") : "no shares";
    }

    function visibleSymbols() {
        if (state.symbols && state.symbols.length > 0) {
            return state.symbols.slice().sort();
        }
        return Object.keys(state.prices).sort();
    }

    function ensureSymbolKnown(symbol) {
        if (!symbol) return;
        if (state.symbols.indexOf(symbol) === -1) {
            state.symbols = state.symbols.concat([symbol]);
        }
    }

    function ensureChartSeries(symbols) {
        for (var i = 0; i < symbols.length; i++) {
            var symbol = symbols[i];
            if (chart.getSeries(state.chart, symbol).length === 0 && state.prices[symbol] !== undefined) {
                state.chart = chart.appendPrice(state.chart, symbol, state.prices[symbol]);
            }
        }
    }

    function tradeBlocker() {
        if (state.gameFinished) return "Trading closed";
        if (ws.readyState !== WebSocket.OPEN) return "Connection unavailable";
        if (!state.joined) return "Joining game";
        if (state.localFreezeActive) return "Orders frozen";
        if (Object.keys(state.prices).length === 0) return "Waiting for market open";
        return "";
    }

    function syncDerivedState() {
        state.tradingDisabledReason = tradeBlocker();
    }

    // --------------- notifications ---------------
    var NOTIF_MAX = 5;
    var NOTIF_DURATION_MS = 3500;
    var NOTIF_FADE_MS = 400;
    var notifContainer = document.getElementById("notifications");

    function addNotification(text, tone) {
        var el = document.createElement("div");
        el.className = "notif notif--" + (tone || "neutral");
        el.textContent = text;
        notifContainer.insertBefore(el, notifContainer.firstChild);

        // enforce max
        while (notifContainer.children.length > NOTIF_MAX) {
            notifContainer.removeChild(notifContainer.lastChild);
        }

        // auto-fade
        setTimeout(function () {
            el.classList.add("notif--fading");
            setTimeout(function () {
                if (el.parentNode) el.parentNode.removeChild(el);
            }, NOTIF_FADE_MS);
        }, NOTIF_DURATION_MS);
    }

    function setGameState(data) {
        state.prevPrices = Object.assign({}, state.prices);
        state.prices = Object.assign({}, data.prices || {});
        state.symbols = (data.symbols || Object.keys(state.prices)).slice();
        ensureChartSeries(state.symbols);
        if (data.players) {
            state.players = data.players;
        }
        if (data.marketCaps) {
            state.marketCaps = data.marketCaps;
        }
    }

    // --------------- powerup helpers ---------------
    function groupInventory(inventory) {
        var map = {};
        var order = [];
        for (var i = 0; i < inventory.length; i++) {
            var p = inventory[i];
            var key = p.name;
            if (map[key]) {
                map[key].count++;
            } else {
                map[key] = { name: p.name, description: p.description, usageType: p.usageType, consumptionMode: p.consumptionMode || "single", count: 1 };
                order.push(key);
            }
        }
        var result = [];
        for (var j = 0; j < order.length; j++) {
            result.push(map[order[j]]);
        }
        return result;
    }

    var powerupTrayEl = null;
    var powerupDescEl = null;
    var powerupCountEl = null;

    function renderPowerupTray() {
        if (!powerupTrayEl) powerupTrayEl = document.getElementById("powerup-tray");
        if (!powerupDescEl) powerupDescEl = document.getElementById("powerup-desc");
        if (!powerupCountEl) powerupCountEl = document.getElementById("powerup-footer__count");

        powerupTrayEl.innerHTML = "";
        powerupDescEl.textContent = "";

        var total = state.inventory.length;
        if (powerupCountEl) {
            powerupCountEl.textContent = total > 0 ? total : "";
            powerupCountEl.classList.toggle("visible", total > 0);
        }

        var groups = groupInventory(state.inventory);

        for (var i = 0; i < groups.length; i++) {
            (function (g) {
                var card = document.createElement("div");
                card.className = "powerup-card";
                card.setAttribute("tabindex", "0");

                card.setAttribute("data-powerup-name", g.name);

                var isTargetPlayer = g.usageType === "target_player";
                var isTargetSymbol = g.usageType === "target_symbol";
                if (isTargetPlayer) {
                    card.classList.add("powerup-card--targeted");
                    card.setAttribute("draggable", "true");
                } else if (isTargetSymbol) {
                    card.classList.add("powerup-card--symbol");
                    card.setAttribute("draggable", "true");
                } else {
                    card.classList.add("powerup-card--instant");
                }

                var nameEl = document.createElement("span");
                nameEl.className = "powerup-card__name";
                nameEl.textContent = g.name;
                card.appendChild(nameEl);

                if (g.count > 1) {
                    var badge = document.createElement("span");
                    badge.className = "powerup-card__badge";
                    badge.textContent = "\u00d7" + g.count;
                    card.appendChild(badge);
                }

                var typeEl = document.createElement("span");
                typeEl.className = "powerup-card__type";
                typeEl.textContent = isTargetPlayer ? "targeted" : (isTargetSymbol ? "symbol" : "instant");
                card.appendChild(typeEl);

                // click
                card.addEventListener("click", function () {
                    if (isTargetPlayer) {
                        showTargetPicker(g.name);
                    } else if (isTargetSymbol) {
                        showSymbolPicker(g.name);
                    } else if (g.consumptionMode === "all") {
                        sendUsePowerup(g.name, null, g.count);
                    } else {
                        sendUsePowerup(g.name);
                    }
                });

                // keyboard
                card.addEventListener("keydown", function (e) {
                    if (e.key === " " || e.key === "Enter") {
                        e.preventDefault();
                        if (isTargetPlayer) {
                            showTargetPicker(g.name);
                        } else if (isTargetSymbol) {
                            showSymbolPicker(g.name);
                        } else if (g.consumptionMode === "all") {
                            sendUsePowerup(g.name, null, g.count);
                        } else {
                            sendUsePowerup(g.name);
                        }
                    }
                });


                // drag for player-targeted powerups
                if (isTargetPlayer) {
                    card.addEventListener("dragstart", function (e) {
                        e.dataTransfer.setData("text/plain", g.name);
                        card.classList.add("powerup-card--dragging");
                        var pids = Object.keys(playerBoxEls);
                        for (var k = 0; k < pids.length; k++) {
                            if (pids[k] !== playerId) {
                                playerBoxEls[pids[k]].root.classList.add("player-box--drop-target");
                            }
                        }
                    });
                    card.addEventListener("dragend", function () {
                        card.classList.remove("powerup-card--dragging");
                        var pids = Object.keys(playerBoxEls);
                        for (var k = 0; k < pids.length; k++) {
                            playerBoxEls[pids[k]].root.classList.remove("player-box--drop-target");
                        }
                    });
                }

                // drag for symbol-targeted powerups
                if (isTargetSymbol) {
                    card.addEventListener("dragstart", function (e) {
                        e.dataTransfer.setData("text/plain", g.name);
                        card.classList.add("powerup-card--dragging");
                    });
                    card.addEventListener("dragend", function () {
                        card.classList.remove("powerup-card--dragging");
                        var syms = Object.keys(tickerCardEls);
                        for (var k = 0; k < syms.length; k++) {
                            tickerCardEls[syms[k]].root.classList.remove("ticker-card--drop-target");
                        }
                    });
                }

                powerupTrayEl.appendChild(card);
            })(groups[i]);
        }
    }

    // --------------- element caches ---------------
    var playerBoxEls = {};   // playerId → {root, nameEl, cashEl, holdingsEl}
    var tickerCardEls = {};  // symbol → {root, priceEl, canvas, hintEl}

    // --------------- DOM creation ---------------
    function ensurePlayerBox(pid) {
        if (playerBoxEls[pid]) return;
        var root = document.createElement("div");
        root.className = "player-box";

        var nameEl = document.createElement("div");
        nameEl.className = "player-box__name";
        root.appendChild(nameEl);

        var cashEl = document.createElement("div");
        cashEl.className = "player-box__cash";
        root.appendChild(cashEl);

        var holdingsEl = document.createElement("div");
        holdingsEl.className = "player-box__holdings";
        root.appendChild(holdingsEl);

        // drag-and-drop target for targeted powerups
        if (pid !== playerId) {
            root.addEventListener("dragover", function (e) {
                e.preventDefault();
            });
            root.addEventListener("drop", function (e) {
                e.preventDefault();
                var powerupName = e.dataTransfer.getData("text/plain");
                if (powerupName) {
                    sendUsePowerup(powerupName, pid);
                }
                root.classList.remove("player-box--drop-target");
            });
            root.addEventListener("dragleave", function () {
                root.classList.remove("player-box--drop-target");
            });
        }

        document.getElementById("player-bar").appendChild(root);
        playerBoxEls[pid] = { root: root, nameEl: nameEl, cashEl: cashEl, holdingsEl: holdingsEl };
    }

    function ensureTickerCard(symbol) {
        if (tickerCardEls[symbol]) return;
        var root = document.createElement("div");
        root.className = "ticker-card";

        var infoCol = document.createElement("div");
        infoCol.className = "ticker-card__info";

        var symbolEl = document.createElement("div");
        symbolEl.className = "ticker-card__symbol";
        symbolEl.textContent = symbol;
        infoCol.appendChild(symbolEl);

        var marketCapEl = document.createElement("div");
        marketCapEl.className = "ticker-card__market-cap";
        infoCol.appendChild(marketCapEl);

        var priceEl = document.createElement("div");
        priceEl.className = "ticker-card__price";
        var priceCurrency = document.createElement("span");
        priceCurrency.className = "ticker-card__currency";
        var priceValue = document.createElement("span");
        priceEl.appendChild(priceCurrency);
        priceEl.appendChild(priceValue);
        priceValue.textContent = "Waiting...";
        infoCol.appendChild(priceEl);

        var liqRow = document.createElement("div");
        liqRow.className = "ticker-card__liquidity";

        var liqTrack = document.createElement("div");
        liqTrack.className = "ticker-card__liquidity-track";
        var liqFill = document.createElement("div");
        liqFill.className = "ticker-card__liquidity-fill";
        liqTrack.appendChild(liqFill);
        liqRow.appendChild(liqTrack);

        var liqLabel = document.createElement("span");
        liqLabel.className = "ticker-card__liquidity-label";
        liqRow.appendChild(liqLabel);

        infoCol.appendChild(liqRow);

        root.appendChild(infoCol);

        var canvas = document.createElement("canvas");
        canvas.className = "ticker-card__sparkline";
        canvas.setAttribute("data-symbol", symbol);
        root.appendChild(canvas);

        var hintEl = document.createElement("div");
        hintEl.className = "ticker-card__hint";
        root.appendChild(hintEl);

        root.addEventListener("mouseenter", function () { state.hoveredSymbol = symbol; });
        root.addEventListener("mouseleave", function () {
            if (state.hoveredSymbol === symbol) state.hoveredSymbol = null;
        });

        // drag-and-drop target for symbol-targeted powerups
        var dragEnterCount = 0;
        root.addEventListener("dragover", function (e) {
            e.preventDefault();
        });
        root.addEventListener("dragenter", function () {
            dragEnterCount++;
            root.classList.add("ticker-card--drop-target");
        });
        root.addEventListener("dragleave", function () {
            dragEnterCount--;
            if (dragEnterCount <= 0) {
                dragEnterCount = 0;
                root.classList.remove("ticker-card--drop-target");
            }
        });
        root.addEventListener("drop", function (e) {
            e.preventDefault();
            dragEnterCount = 0;
            var powerupName = e.dataTransfer.getData("text/plain");
            if (powerupName) {
                sendUsePowerup(powerupName, null, null, symbol);
            }
            root.classList.remove("ticker-card--drop-target");
        });

        document.getElementById("ticker-cards").appendChild(root);
        tickerCardEls[symbol] = { root: root, priceEl: priceEl, priceCurrency: priceCurrency, priceValue: priceValue, canvas: canvas, hintEl: hintEl, marketCapEl: marketCapEl, liqFill: liqFill, liqLabel: liqLabel };

        // sync canvas pixel buffer to CSS size
        requestAnimationFrame(function () {
            canvas.width = canvas.clientWidth;
            canvas.height = canvas.clientHeight;
        });
    }

    // --------------- DOM update ---------------
    function updatePlayerBox(pid) {
        ensurePlayerBox(pid);
        var els = playerBoxEls[pid];
        var p = state.players[pid];
        if (!p) return;

        var isLocal = pid === state.playerId;
        var isFrozen = isLocal && state.localFreezeActive;

        els.nameEl.textContent = isLocal ? pid + " (you)" : pid;
        els.cashEl.textContent = formatPrice(p.cashBalance);
        els.holdingsEl.innerHTML = isLocal ? holdingText(p.holdings) : "";

        els.root.classList.toggle("player-box--local", isLocal && !isFrozen);
        els.root.classList.toggle("player-box--frozen", isFrozen);
    }

    function updateTickerPrice(symbol) {
        if (state.delistedSymbols[symbol]) return;
        ensureTickerCard(symbol);
        var els = tickerCardEls[symbol];
        var price = state.prices[symbol];

        if (price === undefined) {
            els.priceCurrency.textContent = "";
            els.priceValue.textContent = "Waiting...";
            els.priceCurrency.classList.remove("price--up", "price--down");
            return;
        }

        els.priceCurrency.textContent = "$";
        els.priceValue.textContent = (price / 100).toFixed(2);
        var prev = state.prevPrices[symbol];
        els.priceCurrency.classList.remove("price--up", "price--down");
        void els.priceCurrency.offsetWidth; // force reflow to retrigger animation
        if (prev !== undefined && price > prev) {
            els.priceCurrency.classList.add("price--up");
        } else if (prev !== undefined && price < prev) {
            els.priceCurrency.classList.add("price--down");
        }
    }

    var MARKET_CAP_LABELS = {
        STARTUP: "Startup",
        MID_CAP: "Mid Cap",
        BLUE_CHIP: "Blue Chip",
    };

    function updateTickerMarketCap(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        var cap = state.marketCaps[symbol];
        els.marketCapEl.textContent = cap ? (MARKET_CAP_LABELS[cap] || cap) : "";
        els.marketCapEl.className = "ticker-card__market-cap" + (cap ? " ticker-card__market-cap--" + cap.toLowerCase().replace("_", "-") : "");
    }

    function updateHints() {
        syncDerivedState();
        var symbols = Object.keys(tickerCardEls);
        for (var i = 0; i < symbols.length; i++) {
            var sym = symbols[i];
            var els = tickerCardEls[sym];
            if (state.delistedSymbols[sym]) {
                els.hintEl.textContent = "";
            } else if (state.tradingDisabledReason) {
                els.hintEl.textContent = state.tradingDisabledReason;
            } else if (state.prices[sym] === undefined) {
                els.hintEl.textContent = "";
            } else {
                els.hintEl.textContent = "hover + hold B/S to trade";
            }
        }
    }

    function updateTimer() {
        var el = document.getElementById("game-timer");
        var t = state.ticksRemaining;
        if (t === null || t === undefined) { el.textContent = ""; return; }
        el.textContent = t + " ticks remaining";
    }

    function updateInventory() {
        renderPowerupTray();
    }

    function updateDarkPoolStatus() {
        var el = document.getElementById("dark-pool-status");
        if (!el) {
            el = document.createElement("div");
            el.id = "dark-pool-status";
            var timer = document.getElementById("game-timer");
            timer.parentNode.insertBefore(el, timer.nextSibling);
        }
        if (!state.darkPool) {
            el.textContent = "";
            el.classList.remove("dark-pool-status--active");
            return;
        }
        var dp = state.darkPool;
        var target = dp.targetSymbol ? dp.targetSymbol : "ALL";
        el.textContent = "Dark Pool [" + target + "] — " + dp.tokens + " trades left";
        el.classList.add("dark-pool-status--active");
    }

    function setDarkPoolHighlight(targetSymbol, active) {
        if (targetSymbol) {
            var els = tickerCardEls[targetSymbol];
            if (els) els.root.classList.toggle("ticker-card--dark-pool", active);
        } else {
            // Premium (all symbols)
            var syms = Object.keys(tickerCardEls);
            for (var i = 0; i < syms.length; i++) {
                tickerCardEls[syms[i]].root.classList.toggle("ticker-card--dark-pool", active);
            }
        }
    }

    function updateTickerDelisted(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        var delisted = !!state.delistedSymbols[symbol];
        els.root.classList.toggle("ticker-card--delisted", delisted);
        if (delisted) {
            els.priceCurrency.textContent = "";
            els.priceValue.textContent = "DELISTED";
        }
    }

    function updateTickerBubbleTint(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        if (state.delistedSymbols[symbol]) return;
        var b = state.bubbles[symbol];
        if (!b || b.threshold <= 0) {
            els.root.style.background = "";
            return;
        }
        var ratio = b.factor / b.threshold;
        if (ratio < 0.25) {
            els.root.style.background = "";
        } else if (ratio < 0.5) {
            els.root.style.background = "rgba(255, 200, 0, 0.06)";
        } else if (ratio < 0.75) {
            els.root.style.background = "rgba(255, 140, 0, 0.12)";
        } else {
            els.root.style.background = "rgba(214, 40, 40, 0.18)";
        }
    }

    function updateTickerLiquidity(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        var l = state.liquidity[symbol];
        var remaining = 0;
        var max = 1;
        if (l) {
            remaining = state.localFreezeActive ? 0 : l.remaining;
            max = l.max;
        }
        var pct = max > 0 ? (remaining / max) * 100 : 0;
        els.liqFill.style.width = pct + "%";
        if (pct > 50) {
            els.liqFill.style.background = "var(--game-green)";
        } else if (pct > 20) {
            els.liqFill.style.background = "var(--game-gold)";
        } else {
            els.liqFill.style.background = "var(--game-red)";
        }
        els.liqLabel.textContent = remaining + "/" + max;
    }

    function reorderTickerCards() {
        var container = document.getElementById("ticker-cards");
        var symbols = Object.keys(tickerCardEls);
        symbols.sort(function (a, b) {
            var da = state.delistedSymbols[a] ? 1 : 0;
            var db = state.delistedSymbols[b] ? 1 : 0;
            if (da !== db) return da - db;
            return a.localeCompare(b);
        });
        for (var i = 0; i < symbols.length; i++) {
            container.appendChild(tickerCardEls[symbols[i]].root);
        }
    }

    function redrawSparkline(symbol) {
        if (!tickerCardEls[symbol]) return;
        var c = tickerCardEls[symbol].canvas;
        if (c.width === 0 || c.height === 0) {
            c.width = c.clientWidth;
            c.height = c.clientHeight;
        }
        draw.renderSparkline(c, state.chart, symbol);
    }

    function redrawAllSparklines() {
        var symbols = Object.keys(tickerCardEls);
        for (var i = 0; i < symbols.length; i++) {
            redrawSparkline(symbols[i]);
        }
    }

    // --------------- message handlers ---------------
    var messageHandlers = {
        JOINED: function () {
            state.joined = true;
            state.status = "Connected";
        },
        PLAYER_JOINED: function () {},
        ALL_PLAYERS_READY: function () {
            state.allPlayersReady = true;
            state.status = "All players ready";
        },
        PLAYERS_STATE: function (data) {
            state.players = data.players;
            var pids = Object.keys(state.players).sort();
            for (var i = 0; i < pids.length; i++) {
                updatePlayerBox(pids[i]);
            }
        },
        GAME_STATE: function (data) {
            setGameState(data);
            if (data.ticksRemaining !== undefined) state.ticksRemaining = data.ticksRemaining;
            if (!state.gameFinished) {
                state.status = "Connected";
            }
            var symbols = visibleSymbols();
            for (var i = 0; i < symbols.length; i++) {
                if (state.prices[symbols[i]] === 0) {
                    state.delistedSymbols[symbols[i]] = true;
                }
                ensureTickerCard(symbols[i]);
                updateTickerPrice(symbols[i]);
                updateTickerMarketCap(symbols[i]);
                updateTickerDelisted(symbols[i]);
            }
            reorderTickerCards();
            var pids = Object.keys(state.players).sort();
            for (var j = 0; j < pids.length; j++) {
                updatePlayerBox(pids[j]);
            }
            updateHints();
            updateTimer();
            // delay sparkline draw until canvases have layout dimensions
            requestAnimationFrame(function () {
                var syms = Object.keys(tickerCardEls);
                for (var k = 0; k < syms.length; k++) {
                    var c = tickerCardEls[syms[k]].canvas;
                    c.width = c.clientWidth;
                    c.height = c.clientHeight;
                }
                redrawAllSparklines();
            });
        },
        TICKER_TICKED: function (data) {
            ensureSymbolKnown(data.symbol);
            state.prevPrices[data.symbol] = state.prices[data.symbol];
            state.prices[data.symbol] = data.price;
            if (data.marketCap) {
                state.marketCaps[data.symbol] = data.marketCap;
            }
            state.chart = chart.appendPrice(state.chart, data.symbol, data.price);
            ensureTickerCard(data.symbol);
            updateTickerPrice(data.symbol);
            updateTickerMarketCap(data.symbol);
            redrawSparkline(data.symbol);
            updateHints();
        },
        ORDER_ACTIVITY: function (data) {
            state.chart = chart.appendAnnotation(state.chart, data.symbol, data.side, data.darkPool);
            redrawSparkline(data.symbol);
        },
        ORDER_FILLED: function (data) {
            var verb = data.side === "BUY" ? "Bought" : "Sold";
            addNotification(verb + " " + data.symbol + " at " + formatPrice(data.price), "positive");
            if (state.darkPool && state.darkPool.tokens > 0) {
                state.darkPool.tokens--;
                updateDarkPoolStatus();
            }
        },
        GAME_TICK: function (data) {
            state.currentTick = data.tick;
            state.ticksRemaining = data.ticksRemaining;
            if (!state.gameFinished) {
                state.status = "Connected";
            }
            updateTimer();
        },
        GAME_FINISHED: function () {
            state.gameFinished = true;
            state.ticksRemaining = 0;
            state.status = "Connected";
            updateHints();
            updateTimer();
            setTimeout(function () {
                window.location.href = "/game/" + gameId + "/recap";
            }, 3000);
        },
        POWERUP_AWARDED: function (data) {
            if (data.recipient === playerId) {
                state.inventory = state.inventory.concat([{
                    name: data.powerupName,
                    description: data.description || "",
                    usageType: data.usageType || "instant",
                    consumptionMode: data.consumptionMode || "single"
                }]);
                updateInventory();
                addNotification("You received " + data.powerupName + "!", "positive");
            } else {
                addNotification(data.recipient + " received " + data.powerupName, "neutral");
            }
        },
        POWERUP_ACTIVATED: function (data) {
            if (data.user === playerId) {
                var idx = -1;
                for (var i = 0; i < state.inventory.length; i++) {
                    if (state.inventory[i].name === data.powerupName) { idx = i; break; }
                }
                if (idx !== -1) {
                    state.inventory = state.inventory.slice(0, idx).concat(state.inventory.slice(idx + 1));
                }
                updateInventory();
                addNotification("You used " + data.powerupName, "neutral");
            } else {
                addNotification(data.user + " used " + data.powerupName, "neutral");
            }
        },
        FREEZE_STARTED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = true;
            updatePlayerBox(playerId);
            updateHints();
            var syms = Object.keys(tickerCardEls);
            for (var k = 0; k < syms.length; k++) { updateTickerLiquidity(syms[k]); }
            var secs = data.durationSeconds ? data.durationSeconds + "s" : "a while";
            addNotification("Orders frozen for " + secs + "!", "negative");
        },
        FREEZE_EXPIRED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = false;
            updatePlayerBox(playerId);
            updateHints();
            var syms = Object.keys(tickerCardEls);
            for (var k = 0; k < syms.length; k++) { updateTickerLiquidity(syms[k]); }
            addNotification("Freeze expired \u2014 trading resumed", "positive");
        },
        SENTIMENT_BOOST_ACTIVATED: function (data) {
            addNotification(data.symbol + " sentiment boosted!", "positive");
        },
        TICKER_DELISTED: function (data) {
            state.delistedSymbols[data.symbol] = true;
            updateTickerDelisted(data.symbol);
            var card = tickerCardEls[data.symbol];
            if (card) {
                card.root.classList.remove('ticker-card--bubble-warning');
                void card.root.offsetWidth;
                card.root.classList.add('ticker-card--delisting');
                card.root.addEventListener('animationend', function (e) {
                    if (e.animationName === 'delist-flash') {
                        card.root.classList.remove('ticker-card--delisting');
                    }
                }, { once: true });
            }
            reorderTickerCards();
            updateHints();
            addNotification(data.symbol + " has been delisted!", "negative");
        },
        DIVIDEND_PAID: function (data) {
            if (data.playerId === playerId) {
                var tier = data.tierName ? " (" + data.tierName + ")" : "";
                addNotification("Dividend: +" + formatPrice(data.amount) + " from " + data.symbol + tier, "positive");
            } else if (data.playerId) {
                addNotification(data.playerId + " received dividend from " + data.symbol, "neutral");
            }
        },
        BUBBLE_WARNING: function (data) {
            var card = tickerCardEls[data.symbol];
            if (card) {
                card.root.classList.remove('ticker-card--bubble-warning');
                void card.root.offsetWidth;
                card.root.classList.add('ticker-card--bubble-warning');
                card.root.addEventListener('animationend', function (e) {
                    if (e.animationName === 'bubble-flash') {
                        card.root.classList.remove('ticker-card--bubble-warning');
                    }
                }, { once: true });
            }
            addNotification(data.symbol + ' is overheating!', 'negative');
        },

        DARK_POOL_ACTIVATED: function (data) {
            state.darkPool = {
                tierName: data.tierName,
                targetSymbol: data.targetSymbol,
                tokens: data.tokens,
                ticks: data.ticks
            };
            updateDarkPoolStatus();
            setDarkPoolHighlight(data.targetSymbol, true);
            var target = data.targetSymbol ? " on " + data.targetSymbol : " (all symbols)";
            addNotification(data.tierName + " active" + target + " — " + data.tokens + " trades", "positive");
        },
        DARK_POOL_EXPIRED: function () {
            if (state.darkPool) {
                setDarkPoolHighlight(state.darkPool.targetSymbol, false);
            }
            state.darkPool = null;
            updateDarkPoolStatus();
            addNotification("Dark Pool expired", "neutral");
        },

        ORDER_BLOCKED: function (data) {
            if (data.playerId === playerId) {
                addNotification("Order blocked: " + data.reason, "negative");
            }
        },

        MARKET_INDICATORS: function (data) {
            if (data.bubbles) {
                var symbols = Object.keys(data.bubbles);
                for (var i = 0; i < symbols.length; i++) {
                    state.bubbles[symbols[i]] = data.bubbles[symbols[i]];
                }
            }
            var syms = Object.keys(tickerCardEls);
            for (var j = 0; j < syms.length; j++) {
                updateTickerBubbleTint(syms[j]);
            }
        },

        LIQUIDITY_UPDATE: function (data) {
            if (data.liquidity) {
                var symbols = Object.keys(data.liquidity);
                for (var i = 0; i < symbols.length; i++) {
                    state.liquidity[symbols[i]] = data.liquidity[symbols[i]];
                }
            }
            var syms = Object.keys(tickerCardEls);
            for (var j = 0; j < syms.length; j++) {
                updateTickerLiquidity(syms[j]);
            }
        },

        ERROR: function (data) {
            if (data.code === "ORDER_REJECTED") {
                if (/frozen/i.test(data.message || "")) {
                    state.localFreezeActive = true;
                    updatePlayerBox(playerId);
                    updateHints();
                }
                addNotification("Order rejected: " + (data.message || "unknown"), "negative");
            }
        }
    };

    // --------------- WebSocket ---------------
    ws.onopen = function () {
        state.status = "Connected. Joining...";
        ws.send(JSON.stringify({ type: "JOIN", payload: { playerId: playerId } }));
    };

    ws.onmessage = function (event) {
        var msg = JSON.parse(event.data);
        var handler = messageHandlers[msg.type];
        if (handler) {
            handler(msg.data || {});
        }
    };

    ws.onclose = function () {
        state.status = "Disconnected";
        updateHints();
    };

    ws.onerror = function () {
        state.status = "Connection error";
        updateHints();
    };

    // --------------- input ---------------
    function sendOrder(type, symbol) {
        syncDerivedState();
        if (state.tradingDisabledReason) return;
        if (state.prices[symbol] === undefined) return;
        ws.send(JSON.stringify({
            type: type,
            payload: { ticker: symbol, price: state.prices[symbol] },
        }));
    }

    function sendUsePowerup(powerupName, targetPlayerId, quantity, targetSymbol) {
        var payload = { powerupName: powerupName };
        if (targetPlayerId) payload.targetPlayerId = targetPlayerId;
        if (quantity && quantity > 1) payload.quantity = quantity;
        if (targetSymbol) payload.targetSymbol = targetSymbol;
        ws.send(JSON.stringify({ type: "USE_POWERUP", payload: payload }));
    }

    // --------------- target picker ---------------
    var targetPickerEl = null;

    function showTargetPicker(powerupName) {
        if (targetPickerEl) removeTargetPicker();
        var others = Object.keys(state.players).filter(function (pid) { return pid !== playerId; });
        if (others.length === 0) return;

        targetPickerEl = document.createElement("div");
        targetPickerEl.className = "target-picker";
        targetPickerEl.innerHTML = "<div class='target-picker__title'>Choose target for " + powerupName + ":</div>";

        others.forEach(function (pid) {
            var btn = document.createElement("button");
            btn.className = "target-picker__btn";
            btn.textContent = pid;
            btn.addEventListener("click", function () {
                sendUsePowerup(powerupName, pid);
                removeTargetPicker();
            });
            targetPickerEl.appendChild(btn);
        });

        var cancelBtn = document.createElement("button");
        cancelBtn.className = "target-picker__btn target-picker__btn--cancel";
        cancelBtn.textContent = "Cancel";
        cancelBtn.addEventListener("click", removeTargetPicker);
        targetPickerEl.appendChild(cancelBtn);

        document.body.appendChild(targetPickerEl);
    }

    function removeTargetPicker() {
        if (targetPickerEl && targetPickerEl.parentNode) {
            targetPickerEl.parentNode.removeChild(targetPickerEl);
        }
        targetPickerEl = null;
    }

    function showSymbolPicker(powerupName) {
        if (targetPickerEl) removeTargetPicker();
        var symbols = visibleSymbols();
        if (symbols.length === 0) return;

        targetPickerEl = document.createElement("div");
        targetPickerEl.className = "target-picker";
        targetPickerEl.innerHTML = "<div class='target-picker__title'>Choose symbol for " + powerupName + ":</div>";

        for (var i = 0; i < symbols.length; i++) {
            (function (sym) {
                var btn = document.createElement("button");
                btn.className = "target-picker__btn";
                btn.textContent = sym;
                btn.addEventListener("click", function () {
                    sendUsePowerup(powerupName, null, null, sym);
                    removeTargetPicker();
                });
                targetPickerEl.appendChild(btn);
            })(symbols[i]);
        }

        var cancelBtn = document.createElement("button");
        cancelBtn.className = "target-picker__btn target-picker__btn--cancel";
        cancelBtn.textContent = "Cancel";
        cancelBtn.addEventListener("click", removeTargetPicker);
        targetPickerEl.appendChild(cancelBtn);

        document.body.appendChild(targetPickerEl);
    }

    var ORDER_REPEAT_MS = 150;
    var activeOrderInterval = null;
    var activeOrderKey = null;

    function startOrderRepeat(key, type) {
        if (activeOrderKey === key) return;
        stopOrderRepeat();
        activeOrderKey = key;
        // fire immediately, then repeat
        if (state.hoveredSymbol) sendOrder(type, state.hoveredSymbol);
        activeOrderInterval = setInterval(function () {
            if (state.hoveredSymbol) sendOrder(type, state.hoveredSymbol);
        }, ORDER_REPEAT_MS);
    }

    function stopOrderRepeat() {
        if (activeOrderInterval !== null) {
            clearInterval(activeOrderInterval);
            activeOrderInterval = null;
        }
        activeOrderKey = null;
    }

    document.addEventListener("keydown", function (e) {
        if (e.repeat && (e.key === "b" || e.key === "s")) return; // we handle repeat ourselves
        if (e.key === "b") {
            startOrderRepeat("b", "BUY");
        } else if (e.key === "s") {
            startOrderRepeat("s", "SELL");
        } else if (e.key === "u" && state.inventory.length > 0) {
            var groups = groupInventory(state.inventory);
            var g = groups[0];
            if (g.usageType === "target_player") {
                showTargetPicker(g.name);
            } else if (g.usageType === "target_symbol") {
                showSymbolPicker(g.name);
            } else if (g.consumptionMode === "all") {
                sendUsePowerup(g.name, null, g.count);
            } else {
                sendUsePowerup(g.name);
            }
        } else if (e.key === "Escape") {
            removeTargetPicker();
        }
    });

    document.addEventListener("keyup", function (e) {
        if (e.key === activeOrderKey) {
            stopOrderRepeat();
        }
    });

    // --------------- resize ---------------
    window.addEventListener("resize", function () {
        var symbols = Object.keys(tickerCardEls);
        for (var i = 0; i < symbols.length; i++) {
            var c = tickerCardEls[symbols[i]].canvas;
            c.width = c.clientWidth;
            c.height = c.clientHeight;
        }
        redrawAllSparklines();
    });
})();
