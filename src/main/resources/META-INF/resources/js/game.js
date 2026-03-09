// game.js — orchestrator: state, WebSocket, input, overlay UI, and main draw loop
(function () {
    var chart = window.Baz.chart;
    var draw = window.Baz.draw;

    var el = document.getElementById("game-data");
    var statusChipsEl = document.getElementById("game-status-chips");
    var toastStackEl = document.getElementById("game-toast-stack");
    var gameId = el.dataset.gameId;
    var playerId = el.dataset.playerId;

    kaplay({
        width: window.innerWidth,
        height: window.innerHeight,
        background: "#f0f0f0",
        crisp: true,
        stretch: true,
        letterbox: false,
    });

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
        notifications: [],
        notificationSeq: 0,
    };

    function W() { return width(); }
    function H() { return height(); }

    function nowMs() {
        return Date.now();
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
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

    function chipToneForState() {
        if (ws.readyState !== WebSocket.OPEN) return "bad";
        if (state.gameFinished) return "bad";
        if (state.localFreezeActive) return "warn";
        if (Object.keys(state.prices).length > 0) return "good";
        return "info";
    }

    function queueToast(title, message, tone, dedupeKey, ttlMs) {
        var expiresAt = nowMs() + (ttlMs || 3200);
        var key = dedupeKey || (title + ":" + message);
        for (var i = 0; i < state.notifications.length; i++) {
            if (state.notifications[i].key === key) {
                state.notifications[i].expiresAt = expiresAt;
                renderOverlay();
                return;
            }
        }
        state.notificationSeq += 1;
        state.notifications = state.notifications.concat([{
            id: state.notificationSeq,
            key: key,
            title: title,
            message: message,
            tone: tone || "info",
            expiresAt: expiresAt,
        }]);
        renderOverlay();
    }

    function pruneExpiredNotifications() {
        var currentTime = nowMs();
        var next = [];
        for (var i = 0; i < state.notifications.length; i++) {
            if (state.notifications[i].expiresAt > currentTime) {
                next.push(state.notifications[i]);
            }
        }
        if (next.length !== state.notifications.length) {
            state.notifications = next;
            renderOverlay();
        }
    }

    function renderOverlay() {
        syncDerivedState();

        var chips = [{
            label: state.status,
            tone: chipToneForState(),
        }];

        if (state.gameFinished) {
            chips.push({ label: "Game finished", tone: "bad" });
        } else if (state.joined && Object.keys(state.prices).length > 0) {
            chips.push({ label: "Market live", tone: "good" });
        } else if (state.joined) {
            chips.push({ label: "Waiting for start", tone: "info" });
        }

        if (state.currentTick > 0 || state.ticksRemaining !== null) {
            var tickText = "Tick " + state.currentTick;
            if (state.ticksRemaining !== null && !state.gameFinished) {
                tickText += " / " + state.ticksRemaining + " left";
            }
            chips.push({ label: tickText, tone: "info" });
        }

        if (state.localFreezeActive) {
            chips.push({ label: "Orders frozen", tone: "warn" });
        }

        statusChipsEl.innerHTML = chips.map(function (chip) {
            return '<div class="status-chip ' + chip.tone + '">' + escapeHtml(chip.label) + "</div>";
        }).join("");

        toastStackEl.innerHTML = state.notifications.map(function (notification) {
            return [
                '<div class="toast overlay-panel ' + notification.tone + '">',
                '<p class="toast-title">' + escapeHtml(notification.title) + "</p>",
                '<p class="toast-copy">' + escapeHtml(notification.message) + "</p>",
                "</div>"
            ].join("");
        }).join("");
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

    var messageHandlers = {
        JOINED: function () {
            state.joined = true;
            state.status = "Connected";
            renderOverlay();
        },
        PLAYER_JOINED: function (data) {
            if (data.playerId !== playerId) {
                queueToast("Player Joined", data.playerId + " entered the market.", "info",
                    "player-joined:" + data.playerId, 2400);
            }
        },
        ALL_PLAYERS_READY: function () {
            state.allPlayersReady = true;
            state.status = "All players ready";
            queueToast("Match Ready", "All players are ready. Opening market...", "info",
                "all-players-ready", 2600);
            renderOverlay();
        },
        PLAYERS_STATE: function (data) {
            state.players = data.players;
            renderOverlay();
        },
        GAME_STATE: function (data) {
            setGameState(data);
            if (!state.gameFinished) {
                state.status = "Connected";
            }
            renderOverlay();
        },
        TICKER_TICKED: function (data) {
            ensureSymbolKnown(data.symbol);
            state.prevPrices[data.symbol] = state.prices[data.symbol];
            state.prices[data.symbol] = data.price;
            state.chart = chart.appendPrice(state.chart, data.symbol, data.price);
        },
        ORDER_FILLED: function (data) {
            if (!data.playerId) return;
            state.chart = chart.appendAnnotation(state.chart, data.symbol, data.side);
        },
        GAME_TICK: function (data) {
            state.currentTick = data.tick;
            state.ticksRemaining = data.ticksRemaining;
            if (!state.gameFinished) {
                state.status = "Connected";
            }
            renderOverlay();
        },
        GAME_FINISHED: function () {
            state.gameFinished = true;
            state.status = "Connected";
            queueToast("Market Closed", "Trading is over. Final balances are locked.", "bad",
                "game-finished", 4200);
            renderOverlay();
        },
        POWERUP_AWARDED: function (data) {
            if (data.recipient === playerId) {
                queueToast("Powerup", "You received " + data.powerupName + ".", "good",
                    "powerup-self:" + data.powerupName, 3600);
            } else {
                queueToast("Powerup", data.recipient + " received " + data.powerupName + ".", "info",
                    "powerup-other:" + data.recipient + ":" + data.powerupName, 3200);
            }
        },
        FREEZE_STARTED: function () {
            state.localFreezeActive = true;
            queueToast("Freeze", "Your orders are frozen.", "warn", "freeze-started", 4200);
            renderOverlay();
        },
        FREEZE_EXPIRED: function () {
            state.localFreezeActive = false;
            queueToast("Freeze", "Order freeze expired. Trading restored.", "good",
                "freeze-expired", 3600);
            renderOverlay();
        },
        ERROR: function (data) {
            if (data.code === "ORDER_REJECTED" && /frozen/i.test(data.message || "")) {
                state.localFreezeActive = true;
            }
            queueToast("Server Error", data.message || "Unknown error.", "bad",
                "error:" + data.code + ":" + data.message, 3800);
            renderOverlay();
        }
    };

    // --------------- WebSocket ---------------
    ws.onopen = function () {
        state.status = "Connected. Joining...";
        renderOverlay();
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
        renderOverlay();
    };

    ws.onerror = function () {
        state.status = "Connection error";
        renderOverlay();
    };

    // --------------- input ---------------
    function queueBlockedTradeToast(reason) {
        var title = reason === "Orders frozen" ? "Freeze" : "Trading Locked";
        var tone = reason === "Connection unavailable" ? "bad" :
            (reason === "Orders frozen" ? "warn" : "info");
        queueToast(title, reason + ".", tone, "blocked-trade:" + reason, 2200);
    }

    function sendOrder(type, symbol) {
        syncDerivedState();
        if (state.tradingDisabledReason) {
            queueBlockedTradeToast(state.tradingDisabledReason);
            return;
        }
        if (state.prices[symbol] === undefined) {
            queueToast("Trading Locked", "No live price is available for " + symbol + ".", "info",
                "missing-price:" + symbol, 2200);
            return;
        }
        ws.send(JSON.stringify({
            type: type,
            payload: { ticker: symbol, price: state.prices[symbol] },
        }));
    }

    onKeyPress("b", function () {
        if (state.hoveredSymbol) sendOrder("BUY", state.hoveredSymbol);
    });

    onKeyPress("s", function () {
        if (state.hoveredSymbol) sendOrder("SELL", state.hoveredSymbol);
    });

    onClick(function () {
        var mp = mousePos();
        handleClick(mp.x, mp.y);
    });

    function handleClick(mx, my) {
        var symbols = draw.visibleSymbols(state);
        if (symbols.length === 0) return;
        var layouts = draw.computeCardLayout(symbols, W(), H());
        for (var i = 0; i < layouts.length; i++) {
            var lay = layouts[i];
            if (draw.pointInRect(mx, my, lay.buyBtnRect.x, lay.buyBtnRect.y, lay.buyBtnRect.w, lay.buyBtnRect.h)) {
                sendOrder("BUY", lay.symbol);
                return;
            }
            if (draw.pointInRect(mx, my, lay.sellBtnRect.x, lay.sellBtnRect.y, lay.sellBtnRect.w, lay.sellBtnRect.h)) {
                sendOrder("SELL", lay.symbol);
                return;
            }
        }
    }

    onUpdate(function () {
        pruneExpiredNotifications();
    });

    // --------------- main draw loop ---------------
    onDraw(function () {
        var mp = mousePos();
        var cw = W();
        var ch = H();

        syncDerivedState();
        draw.drawStatusBar(state, cw);
        draw.drawPlayers(state, cw);
        draw.drawTickerCards(state, mp, cw, ch);
    });

    renderOverlay();
})();
