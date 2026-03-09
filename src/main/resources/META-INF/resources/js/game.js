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
        symbols: [],
        chart: chart.createChartState(60),
        hoveredSymbol: null,
        tradingDisabledReason: "",
        inventory: [],
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
            if (holdings[symbol] > 0) entries.push(symbol + ":" + holdings[symbol]);
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

    function setGameState(data) {
        state.prevPrices = Object.assign({}, state.prices);
        state.prices = Object.assign({}, data.prices || {});
        state.symbols = (data.symbols || Object.keys(state.prices)).slice();
        ensureChartSeries(state.symbols);
        if (data.players) {
            state.players = data.players;
        }
    }

    // --------------- element caches ---------------
    var playerBoxEls = {};   // playerId → {root, nameEl, cashEl, holdingsEl, inventoryEl}
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

        var inventoryEl = document.createElement("div");
        inventoryEl.className = "player-box__inventory";
        root.appendChild(inventoryEl);

        document.getElementById("player-bar").appendChild(root);
        playerBoxEls[pid] = { root: root, nameEl: nameEl, cashEl: cashEl, holdingsEl: holdingsEl, inventoryEl: inventoryEl };
    }

    function ensureTickerCard(symbol) {
        if (tickerCardEls[symbol]) return;
        var root = document.createElement("div");
        root.className = "ticker-card";

        var symbolEl = document.createElement("div");
        symbolEl.className = "ticker-card__symbol";
        symbolEl.textContent = symbol;
        root.appendChild(symbolEl);

        var priceEl = document.createElement("div");
        priceEl.className = "ticker-card__price";
        priceEl.textContent = "Waiting...";
        root.appendChild(priceEl);

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

        document.getElementById("ticker-cards").appendChild(root);
        tickerCardEls[symbol] = { root: root, priceEl: priceEl, canvas: canvas, hintEl: hintEl };

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
        els.holdingsEl.textContent = holdingText(p.holdings);

        els.root.classList.toggle("player-box--local", isLocal && !isFrozen);
        els.root.classList.toggle("player-box--frozen", isFrozen);

        if (isLocal && state.inventory && state.inventory.length > 0) {
            els.inventoryEl.textContent = "[" + state.inventory.join(", ") + "]";
            els.inventoryEl.style.display = "";
        } else {
            els.inventoryEl.textContent = "";
            els.inventoryEl.style.display = "none";
        }
    }

    function updateTickerPrice(symbol) {
        ensureTickerCard(symbol);
        var els = tickerCardEls[symbol];
        var price = state.prices[symbol];

        if (price === undefined) {
            els.priceEl.textContent = "Waiting...";
            els.priceEl.classList.remove("price--up", "price--down");
            return;
        }

        els.priceEl.textContent = formatPrice(price);
        var prev = state.prevPrices[symbol];
        els.priceEl.classList.toggle("price--up", prev !== undefined && price > prev);
        els.priceEl.classList.toggle("price--down", prev !== undefined && price < prev);
    }

    function updateHints() {
        syncDerivedState();
        var symbols = Object.keys(tickerCardEls);
        for (var i = 0; i < symbols.length; i++) {
            var sym = symbols[i];
            var els = tickerCardEls[sym];
            if (state.tradingDisabledReason) {
                els.hintEl.textContent = state.tradingDisabledReason;
            } else if (state.prices[sym] === undefined) {
                els.hintEl.textContent = "";
            } else {
                els.hintEl.textContent = "hover + hold B/S to trade";
            }
        }
    }

    function updateInventory() {
        if (playerBoxEls[state.playerId]) {
            updatePlayerBox(state.playerId);
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
            if (!state.gameFinished) {
                state.status = "Connected";
            }
            var symbols = visibleSymbols();
            for (var i = 0; i < symbols.length; i++) {
                ensureTickerCard(symbols[i]);
                updateTickerPrice(symbols[i]);
            }
            var pids = Object.keys(state.players).sort();
            for (var j = 0; j < pids.length; j++) {
                updatePlayerBox(pids[j]);
            }
            updateHints();
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
            state.chart = chart.appendPrice(state.chart, data.symbol, data.price);
            ensureTickerCard(data.symbol);
            updateTickerPrice(data.symbol);
            redrawSparkline(data.symbol);
            updateHints();
        },
        ORDER_FILLED: function (data) {
            if (!data.playerId) return;
            state.chart = chart.appendAnnotation(state.chart, data.symbol, data.side);
            redrawSparkline(data.symbol);
        },
        GAME_TICK: function (data) {
            state.currentTick = data.tick;
            state.ticksRemaining = data.ticksRemaining;
            if (!state.gameFinished) {
                state.status = "Connected";
            }
        },
        GAME_FINISHED: function () {
            state.gameFinished = true;
            state.status = "Connected";
            updateHints();
        },
        POWERUP_AWARDED: function (data) {
            if (data.recipient === playerId) {
                state.inventory = state.inventory.concat([data.powerupName]);
                updateInventory();
            }
        },
        POWERUP_ACTIVATED: function (data) {
            if (data.user === playerId) {
                var idx = state.inventory.indexOf(data.powerupName);
                if (idx !== -1) {
                    state.inventory = state.inventory.slice(0, idx).concat(state.inventory.slice(idx + 1));
                }
                updateInventory();
            }
        },
        FREEZE_STARTED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = true;
            updatePlayerBox(playerId);
            updateHints();
        },
        FREEZE_EXPIRED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = false;
            updatePlayerBox(playerId);
            updateHints();
        },
        ERROR: function (data) {
            if (data.code === "ORDER_REJECTED" && /frozen/i.test(data.message || "")) {
                state.localFreezeActive = true;
                updatePlayerBox(playerId);
                updateHints();
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

    function sendUsePowerup(powerupName) {
        ws.send(JSON.stringify({
            type: "USE_POWERUP",
            payload: { powerupName: powerupName },
        }));
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
            sendUsePowerup(state.inventory[0]);
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
