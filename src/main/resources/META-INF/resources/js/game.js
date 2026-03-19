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
        chart: chart.createChartState(180),
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

                var isTargeted = g.usageType === "target_player";
                if (isTargeted) {
                    card.classList.add("powerup-card--targeted");
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
                typeEl.textContent = isTargeted ? "targeted" : "instant";
                card.appendChild(typeEl);

                // click
                card.addEventListener("click", function () {
                    if (isTargeted) {
                        showTargetPicker(g.name);
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
                        if (isTargeted) {
                            showTargetPicker(g.name);
                        } else if (g.consumptionMode === "all") {
                            sendUsePowerup(g.name, null, g.count);
                        } else {
                            sendUsePowerup(g.name);
                        }
                    }
                });


                // drag for targeted powerups
                if (isTargeted) {
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

        var priceEl = document.createElement("div");
        priceEl.className = "ticker-card__price";
        priceEl.textContent = "Waiting...";
        infoCol.appendChild(priceEl);

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
        els.holdingsEl.textContent = isLocal ? holdingText(p.holdings) : "";

        els.root.classList.toggle("player-box--local", isLocal && !isFrozen);
        els.root.classList.toggle("player-box--frozen", isFrozen);
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

    function updateTimer() {
        var el = document.getElementById("game-timer");
        var t = state.ticksRemaining;
        if (t === null || t === undefined) { el.textContent = ""; return; }
        el.textContent = t + " ticks remaining";
    }

    function updateInventory() {
        renderPowerupTray();
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
                ensureTickerCard(symbols[i]);
                updateTickerPrice(symbols[i]);
            }
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
            state.chart = chart.appendPrice(state.chart, data.symbol, data.price);
            ensureTickerCard(data.symbol);
            updateTickerPrice(data.symbol);
            redrawSparkline(data.symbol);
            updateHints();
        },
        ORDER_ACTIVITY: function (data) {
            state.chart = chart.appendAnnotation(state.chart, data.symbol, data.side);
            redrawSparkline(data.symbol);
        },
        ORDER_FILLED: function (data) {
            var verb = data.side === "BUY" ? "Bought" : "Sold";
            addNotification(verb + " " + data.symbol + " at " + formatPrice(data.price), "positive");
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
            var secs = data.durationSeconds ? data.durationSeconds + "s" : "a while";
            addNotification("Orders frozen for " + secs + "!", "negative");
        },
        FREEZE_EXPIRED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = false;
            updatePlayerBox(playerId);
            updateHints();
            addNotification("Freeze expired \u2014 trading resumed", "positive");
        },
        DIVIDEND_PAID: function (data) {
            if (data.playerId === playerId) {
                var tier = data.tierName ? " (" + data.tierName + ")" : "";
                addNotification("Dividend: +" + formatPrice(data.amount) + " from " + data.symbol + tier, "positive");
            } else if (data.playerId) {
                addNotification(data.playerId + " received dividend from " + data.symbol, "neutral");
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

    function sendUsePowerup(powerupName, targetPlayerId, quantity) {
        var payload = { powerupName: powerupName };
        if (targetPlayerId) payload.targetPlayerId = targetPlayerId;
        if (quantity && quantity > 1) payload.quantity = quantity;
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
