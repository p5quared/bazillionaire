// game.js — orchestrator: state, WebSocket, input, DOM updates
(function () {
    var chart = window.Baz.chart;
    var draw = window.Baz.draw;

    var el = document.getElementById("game-data");
    var gameId = el.dataset.gameId;
    var playerId = el.dataset.playerId;

    var protocol = location.protocol === "https:" ? "wss:" : "ws:";
    var ws = new WebSocket(protocol + "//" + location.host + "/game/" + gameId);

    var TICKS_PER_SECOND = 4;

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
        totalDuration: null,
        initialPortfolioValue: null,
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
        darkPool: null,
        bubbles: {},
        liquidity: {},
        tapeEvents: [],
        wireNotifications: [],
    };

    // --------------- helpers ---------------
    function formatPrice(cents) {
        if (cents === undefined || cents === null) return "--";
        return "$" + (cents / 100).toFixed(2);
    }

    function fmtK(n) {
        if (n >= 1e8) return "$" + (n / 1e8).toFixed(2) + "M";
        if (n >= 1e5) return "$" + (n / 1e5).toFixed(1) + "K";
        return "$" + (n / 100).toFixed(0);
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

    // --------------- portfolio / rankings ---------------
    function computePortfolioValue(pid) {
        var p = state.players[pid];
        if (!p) return 0;
        var total = p.cashBalance;
        if (p.holdings) {
            var syms = Object.keys(p.holdings);
            for (var i = 0; i < syms.length; i++) {
                var sym = syms[i];
                var qty = p.holdings[sym];
                var price = state.prices[sym] || 0;
                total += qty * price;
            }
        }
        return total;
    }

    function computeRankings() {
        var pids = Object.keys(state.players);
        var ranked = [];
        for (var i = 0; i < pids.length; i++) {
            ranked.push({ pid: pids[i], value: computePortfolioValue(pids[i]) });
        }
        ranked.sort(function (a, b) { return b.value - a.value; });
        for (var j = 0; j < ranked.length; j++) {
            ranked[j].rank = j + 1;
        }
        return ranked;
    }

    function ticksToMMSS(ticks) {
        var totalSeconds = Math.ceil(ticks / TICKS_PER_SECOND);
        var mm = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
        var ss = String(totalSeconds % 60).padStart(2, "0");
        return mm + ":" + ss;
    }

    function ordinalSuffix(n) {
        var s = ["th", "st", "nd", "rd"];
        var v = n % 100;
        return (s[(v - 20) % 10] || s[v] || s[0]);
    }

    function computeDeltaPct(symbol) {
        var series = chart.getSeries(state.chart, symbol);
        if (series.length < 2) return null;
        var prev = series[series.length - 2];
        var curr = series[series.length - 1];
        if (prev === 0) return null;
        return ((curr - prev) / prev) * 100;
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
        while (notifContainer.children.length > NOTIF_MAX) {
            notifContainer.removeChild(notifContainer.lastChild);
        }
        setTimeout(function () {
            el.classList.add("notif--fading");
            setTimeout(function () {
                if (el.parentNode) el.parentNode.removeChild(el);
            }, NOTIF_FADE_MS);
        }, NOTIF_DURATION_MS);
    }

    // --------------- tape + wire ---------------
    function addTapeEvent(who, what, tone) {
        var now = new Date();
        var hh = String(now.getHours()).padStart(2, "0");
        var mm = String(now.getMinutes()).padStart(2, "0");
        state.tapeEvents.unshift({ time: hh + ":" + mm, who: who, what: what, tone: tone || "neutral" });
        if (state.tapeEvents.length > 100) state.tapeEvents = state.tapeEvents.slice(0, 100);
        renderTape();
    }

    function addWireNotification(text, tone) {
        var now = new Date();
        var hh = String(now.getHours()).padStart(2, "0");
        var mm = String(now.getMinutes()).padStart(2, "0");
        state.wireNotifications.unshift({ time: hh + ":" + mm, text: text, tone: tone || "neutral" });
        if (state.wireNotifications.length > 30) state.wireNotifications = state.wireNotifications.slice(0, 30);
        addNotification(text, tone);
        renderWireFeed();
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

    // --------------- element caches ---------------
    var tickerCardEls = {};  // symbol → {root, priceEl, deltaEl, sparklineEl, liqFill, liqPct, btnBuy, btnSell, ribbonHeld, ribbonDark, ribbonHot, nameEl}
    var standingEls = {};    // playerId → {root}

    // --------------- topbar element cache ---------------
    var topbarEls = {
        timerDigits: document.getElementById("topbar-timer-digits"),
        timerProgress: document.getElementById("topbar-timer-progress"),
        portfolioValue: document.getElementById("topbar-portfolio-value"),
        portfolioPl: document.getElementById("topbar-portfolio-pl"),
        portfolioCash: document.getElementById("topbar-portfolio-cash"),
        portfolioRank: document.getElementById("topbar-portfolio-rank"),
        statusPill: document.getElementById("topbar-status-pill"),
        brandSub: document.getElementById("topbar-brand-sub"),
    };

    // --------------- topbar updates ---------------
    function updateTopbar() {
        // Timer
        if (state.ticksRemaining !== null) {
            topbarEls.timerDigits.textContent = ticksToMMSS(state.ticksRemaining);
            if (state.totalDuration > 0) {
                var pct = ((state.totalDuration - state.ticksRemaining) / state.totalDuration) * 100;
                topbarEls.timerProgress.style.inset = "0 " + (100 - pct) + "% 0 0";
            }
        }

        // Portfolio
        var rankings = computeRankings();
        var myRank = null;
        var totalPlayers = rankings.length;
        for (var i = 0; i < rankings.length; i++) {
            if (rankings[i].pid === playerId) {
                myRank = rankings[i];
                break;
            }
        }

        var myPortfolio = computePortfolioValue(playerId);
        var myPlayer = state.players[playerId];
        var myCash = myPlayer ? myPlayer.cashBalance : 0;

        topbarEls.portfolioValue.textContent = fmtK(myPortfolio);
        topbarEls.portfolioCash.textContent = fmtK(myCash);

        // P/L
        if (state.initialPortfolioValue !== null) {
            var pl = myPortfolio - state.initialPortfolioValue;
            var plPct = state.initialPortfolioValue > 0 ? (pl / state.initialPortfolioValue * 100) : 0;
            var sign = pl >= 0 ? "+" : "-";
            topbarEls.portfolioPl.innerHTML = sign + fmtK(Math.abs(pl)) +
                ' <span class="pl-pct">' + (pl >= 0 ? "\u25B2" : "\u25BC") + " " + Math.abs(plPct).toFixed(1) + "%</span>";
            topbarEls.portfolioPl.className = "topbar-stat__value topbar-stat__value--pl " + (pl >= 0 ? "is-positive" : "is-negative");
        }

        // Rank
        if (myRank) {
            topbarEls.portfolioRank.innerHTML = myRank.rank + "<sup>" + ordinalSuffix(myRank.rank) + "</sup>" +
                ' <span class="rank-total">of ' + totalPlayers + "</span>";
        }

        // Status pill
        if (state.gameFinished) {
            topbarEls.statusPill.textContent = "Markets Closed";
            topbarEls.statusPill.className = "bz-pill";
        } else if (state.localFreezeActive) {
            topbarEls.statusPill.textContent = "Frozen";
            topbarEls.statusPill.className = "bz-pill bz-pill--burgundy";
        } else if (state.joined) {
            topbarEls.statusPill.textContent = "Markets Open";
            topbarEls.statusPill.className = "bz-pill bz-pill--live";
        } else {
            topbarEls.statusPill.textContent = "Connecting";
            topbarEls.statusPill.className = "bz-pill";
        }

        // Brand sub
        topbarEls.brandSub.textContent = "Game #" + gameId.substring(0, 8);
    }

    // --------------- standings ---------------
    function updateStandings() {
        var container = document.getElementById("standings-list");
        var rankings = computeRankings();
        var maxVal = rankings.length > 0 ? rankings[0].value : 1;

        // clear old drop target listeners
        standingEls = {};
        container.innerHTML = "";

        for (var i = 0; i < rankings.length; i++) {
            var r = rankings[i];
            var isYou = r.pid === playerId;
            var isFrozen = isYou && state.localFreezeActive;

            var entry = document.createElement("div");
            entry.className = "standing-entry" +
                (isYou ? " standing-entry--you" : "") +
                (isFrozen ? " standing-entry--frozen" : "");

            // Top row
            var topRow = document.createElement("div");
            topRow.className = "standing-entry__top";

            var leftSide = document.createElement("div");
            leftSide.className = "standing-entry__left";

            var rankSpan = document.createElement("span");
            rankSpan.className = "standing-entry__rank";
            rankSpan.textContent = "#" + r.rank;
            leftSide.appendChild(rankSpan);

            var nameSpan = document.createElement("span");
            nameSpan.className = "standing-entry__name";
            nameSpan.textContent = r.pid;

            // HOST badge — check if this player was the first to join (index 0 in players)
            // We don't have host info from the backend, so skip host badge for now

            if (isFrozen) {
                var frozenBadge = document.createElement("span");
                frozenBadge.className = "standing-entry__badge standing-entry__badge--frozen";
                frozenBadge.textContent = "FROZEN";
                nameSpan.appendChild(frozenBadge);
            }

            leftSide.appendChild(nameSpan);
            topRow.appendChild(leftSide);

            var valueSpan = document.createElement("span");
            valueSpan.className = "standing-entry__value";
            valueSpan.textContent = fmtK(r.value);
            topRow.appendChild(valueSpan);

            entry.appendChild(topRow);

            // Bar
            var bar = document.createElement("div");
            bar.className = "standing-entry__bar";
            var fill = document.createElement("div");
            fill.className = "standing-entry__bar-fill";
            var pct = maxVal > 0 ? (r.value / maxVal) * 100 : 0;
            fill.style.inset = "0 " + (100 - pct) + "% 0 0";
            bar.appendChild(fill);
            entry.appendChild(bar);

            // drag-and-drop target for targeted powerups (not for self)
            if (!isYou) {
                (function (pid, entryEl) {
                    entryEl.addEventListener("dragover", function (e) { e.preventDefault(); });
                    entryEl.addEventListener("dragenter", function () {
                        entryEl.classList.add("standing-entry--drop-target");
                    });
                    entryEl.addEventListener("dragleave", function () {
                        entryEl.classList.remove("standing-entry--drop-target");
                    });
                    entryEl.addEventListener("drop", function (e) {
                        e.preventDefault();
                        var powerupName = e.dataTransfer.getData("text/plain");
                        if (powerupName) sendUsePowerup(powerupName, pid);
                        entryEl.classList.remove("standing-entry--drop-target");
                    });
                })(r.pid, entry);
            }

            container.appendChild(entry);
            standingEls[r.pid] = { root: entry };
        }
    }

    // --------------- tape rendering ---------------
    function renderTape() {
        var container = document.getElementById("tape-list");
        container.innerHTML = "";
        var max = Math.min(state.tapeEvents.length, 30);
        for (var i = 0; i < max; i++) {
            var ev = state.tapeEvents[i];
            var row = document.createElement("div");
            row.className = "tape-entry";

            var timeSpan = document.createElement("span");
            timeSpan.className = "tape-entry__time";
            timeSpan.textContent = ev.time;
            row.appendChild(timeSpan);

            var content = document.createElement("span");
            var whoEl = document.createElement("strong");
            whoEl.className = "tape-entry__who";
            whoEl.textContent = ev.who;
            content.appendChild(whoEl);
            content.appendChild(document.createTextNode(" "));

            var whatEl = document.createElement("span");
            whatEl.className = "tape-entry__what--" + ev.tone;
            whatEl.textContent = ev.what;
            content.appendChild(whatEl);

            row.appendChild(content);
            container.appendChild(row);
        }
    }

    // --------------- wire feed rendering ---------------
    function renderWireFeed() {
        var container = document.getElementById("wire-feed-list");
        container.innerHTML = "";
        var max = Math.min(state.wireNotifications.length, 20);
        for (var i = 0; i < max; i++) {
            var n = state.wireNotifications[i];
            var entry = document.createElement("div");
            entry.className = "wire-entry wire-entry--" + n.tone;

            var timeSpan = document.createElement("span");
            timeSpan.className = "wire-entry__time";
            timeSpan.textContent = n.time;
            entry.appendChild(timeSpan);

            entry.appendChild(document.createTextNode(n.text));
            container.appendChild(entry);
        }
    }

    // --------------- in-effect rendering ---------------
    function updateInEffect() {
        var container = document.getElementById("in-effect-list");
        container.innerHTML = "";

        if (state.darkPool) {
            var dp = state.darkPool;
            var target = dp.targetSymbol || "ALL";
            var row = document.createElement("div");
            row.className = "effect-entry effect-entry--dark";
            row.innerHTML = '<span>\u25C6 Dark Pool \u00B7 ' + target + '</span>' +
                '<span class="effect-entry__timer">' + dp.tokens + ' trades</span>';
            container.appendChild(row);
        }

        var syms = Object.keys(state.bubbles);
        for (var i = 0; i < syms.length; i++) {
            var b = state.bubbles[syms[i]];
            if (b && b.threshold > 0) {
                var ratio = b.factor / b.threshold;
                if (ratio >= 0.5) {
                    var row2 = document.createElement("div");
                    row2.className = "effect-entry effect-entry--warn";
                    row2.innerHTML = '<span>\u25B2 ' + syms[i] + ' Overheating</span>' +
                        '<span class="effect-entry__timer">' + Math.round(ratio * 100) + '%</span>';
                    container.appendChild(row2);
                }
            }
        }
    }

    // --------------- ticker card creation ---------------
    function ensureTickerCard(symbol) {
        if (tickerCardEls[symbol]) return;
        var root = document.createElement("div");
        root.className = "ticker-card";

        // Ribbons (hidden by default)
        var ribbonHeld = document.createElement("div");
        ribbonHeld.className = "ticker-card__ribbon ticker-card__ribbon--held";
        ribbonHeld.style.display = "none";
        root.appendChild(ribbonHeld);

        var ribbonDark = document.createElement("div");
        ribbonDark.className = "ticker-card__ribbon ticker-card__ribbon--dark";
        ribbonDark.style.display = "none";
        root.appendChild(ribbonDark);

        var ribbonHot = document.createElement("div");
        ribbonHot.className = "ticker-card__ribbon ticker-card__ribbon--hot";
        ribbonHot.textContent = "\u25B2 Overheat";
        ribbonHot.style.display = "none";
        root.appendChild(ribbonHot);

        // Symbol row
        var symRow = document.createElement("div");
        symRow.className = "ticker-card__sym-row";

        var symbolEl = document.createElement("span");
        symbolEl.className = "ticker-card__symbol";
        symbolEl.textContent = symbol;
        symRow.appendChild(symbolEl);

        var capEl = document.createElement("span");
        capEl.className = "ticker-card__cap";
        symRow.appendChild(capEl);

        root.appendChild(symRow);

        // Company name placeholder
        var nameEl = document.createElement("div");
        nameEl.className = "ticker-card__name";
        nameEl.textContent = symbol;
        root.appendChild(nameEl);

        // Price column
        var priceCol = document.createElement("div");
        priceCol.className = "ticker-card__price-col";

        var priceEl = document.createElement("div");
        priceEl.className = "ticker-card__price";
        priceEl.textContent = "Waiting...";
        priceCol.appendChild(priceEl);

        var deltaEl = document.createElement("div");
        deltaEl.className = "ticker-card__delta";
        priceCol.appendChild(deltaEl);

        root.appendChild(priceCol);

        // Sparkline container (SVG will be rendered into this)
        var sparklineEl = document.createElement("div");
        sparklineEl.className = "ticker-card__sparkline";
        root.appendChild(sparklineEl);

        // Footer row
        var footer = document.createElement("div");
        footer.className = "ticker-card__footer";

        // Liquidity
        var liqDiv = document.createElement("div");
        liqDiv.className = "ticker-card__liq";

        var liqLabel = document.createElement("span");
        liqLabel.className = "ticker-card__liq-label";
        liqLabel.textContent = "Liq";
        liqDiv.appendChild(liqLabel);

        var liqTrack = document.createElement("div");
        liqTrack.className = "ticker-card__liq-track";
        var liqFill = document.createElement("div");
        liqFill.className = "ticker-card__liq-fill";
        liqTrack.appendChild(liqFill);
        liqDiv.appendChild(liqTrack);

        var liqPct = document.createElement("span");
        liqPct.className = "ticker-card__liq-pct";
        liqDiv.appendChild(liqPct);

        footer.appendChild(liqDiv);

        // Buy button
        var btnBuy = document.createElement("button");
        btnBuy.className = "ticker-card__btn ticker-card__btn--buy";
        btnBuy.innerHTML = "<kbd>B</kbd> Buy";
        footer.appendChild(btnBuy);

        // Sell button
        var btnSell = document.createElement("button");
        btnSell.className = "ticker-card__btn ticker-card__btn--sell";
        btnSell.innerHTML = "<kbd>S</kbd> Sell";
        footer.appendChild(btnSell);

        root.appendChild(footer);

        // Button click handlers
        btnBuy.addEventListener("click", function () { sendOrder("BUY", symbol); });
        btnSell.addEventListener("click", function () { sendOrder("SELL", symbol); });

        // Interactions
        root.addEventListener("mouseenter", function () { state.hoveredSymbol = symbol; });
        root.addEventListener("mouseleave", function () {
            if (state.hoveredSymbol === symbol) state.hoveredSymbol = null;
        });

        // drag-and-drop target for symbol-targeted powerups
        var dragEnterCount = 0;
        root.addEventListener("dragover", function (e) { e.preventDefault(); });
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
            if (powerupName) sendUsePowerup(powerupName, null, null, symbol);
            root.classList.remove("ticker-card--drop-target");
        });

        document.getElementById("ticker-cards").appendChild(root);
        tickerCardEls[symbol] = {
            root: root, priceEl: priceEl, deltaEl: deltaEl, sparklineEl: sparklineEl,
            capEl: capEl, liqFill: liqFill, liqPct: liqPct, btnBuy: btnBuy, btnSell: btnSell,
            ribbonHeld: ribbonHeld, ribbonDark: ribbonDark, ribbonHot: ribbonHot, nameEl: nameEl
        };
    }

    // --------------- DOM update ---------------
    var MARKET_CAP_LABELS = {
        STARTUP: "Startup",
        MID_CAP: "Mid Cap",
        BLUE_CHIP: "Blue Chip",
    };

    function updateTickerPrice(symbol) {
        if (state.delistedSymbols[symbol]) return;
        ensureTickerCard(symbol);
        var els = tickerCardEls[symbol];
        var price = state.prices[symbol];

        if (price === undefined) {
            els.priceEl.textContent = "Waiting...";
            els.priceEl.className = "ticker-card__price";
            els.deltaEl.textContent = "";
            return;
        }

        var prev = state.prevPrices[symbol];
        var up = prev !== undefined ? price >= prev : true;

        els.priceEl.textContent = formatPrice(price);
        els.priceEl.className = "ticker-card__price " + (up ? "ticker-card__price--up" : "ticker-card__price--down");

        // Flash animation on $ sign
        els.priceEl.classList.remove("price--up", "price--down");
        void els.priceEl.offsetWidth;
        if (prev !== undefined && price > prev) {
            els.priceEl.classList.add("price--up");
        } else if (prev !== undefined && price < prev) {
            els.priceEl.classList.add("price--down");
        }

        // Delta %
        var delta = computeDeltaPct(symbol);
        if (delta !== null) {
            var arrow = delta >= 0 ? "\u25B2" : "\u25BC";
            var sign = delta >= 0 ? "+" : "";
            els.deltaEl.textContent = arrow + " " + sign + delta.toFixed(2) + "%";
            els.deltaEl.className = "ticker-card__delta " + (delta >= 0 ? "ticker-card__delta--up" : "ticker-card__delta--down");
        }

        // Held ribbon
        updateTickerRibbons(symbol);
    }

    function updateTickerRibbons(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;

        var myPlayer = state.players[playerId];
        var held = myPlayer && myPlayer.holdings && myPlayer.holdings[symbol] ? myPlayer.holdings[symbol] : 0;
        var isDarkPool = state.darkPool && (state.darkPool.targetSymbol === symbol || (!state.darkPool.targetSymbol));
        var isHot = state.bubbles[symbol] && state.bubbles[symbol].threshold > 0 &&
            (state.bubbles[symbol].factor / state.bubbles[symbol].threshold) >= 0.75;

        var hasRibbon = held > 0 || isDarkPool || isHot;

        if (held > 0) {
            els.ribbonHeld.textContent = "Held \u00B7 " + held;
            els.ribbonHeld.style.display = "";
        } else {
            els.ribbonHeld.style.display = "none";
        }

        if (isDarkPool) {
            els.ribbonDark.textContent = "Dark Pool";
            els.ribbonDark.style.display = "";
        } else {
            els.ribbonDark.style.display = "none";
        }

        if (isHot && !isDarkPool) {
            els.ribbonHot.style.display = "";
        } else {
            els.ribbonHot.style.display = "none";
        }

        els.root.classList.toggle("ticker-card--has-ribbon", hasRibbon);

        // sell button opacity
        els.btnSell.classList.toggle("ticker-card__btn--disabled", held <= 0);
    }

    function updateTickerMarketCap(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        var cap = state.marketCaps[symbol];
        els.capEl.textContent = cap ? (MARKET_CAP_LABELS[cap] || cap) : "";
        els.capEl.className = "ticker-card__cap" + (cap ? " ticker-card__cap--" + cap.toLowerCase().replace("_", "-") : "");
    }

    function updateTickerDelisted(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        var delisted = !!state.delistedSymbols[symbol];
        els.root.classList.toggle("ticker-card--delisted", delisted);
        if (delisted) {
            els.priceEl.textContent = "DELISTED";
            els.priceEl.className = "ticker-card__price";
            els.deltaEl.textContent = "";
        }
    }

    function updateTickerBubbleTint(symbol) {
        var els = tickerCardEls[symbol];
        if (!els) return;
        if (state.delistedSymbols[symbol]) return;
        var b = state.bubbles[symbol];
        if (!b || b.threshold <= 0) {
            els.root.style.background = "";
            els.root.classList.remove("ticker-card--hot");
            return;
        }
        var ratio = b.factor / b.threshold;
        if (ratio < 0.25) {
            els.root.style.background = "";
            els.root.classList.remove("ticker-card--hot");
        } else if (ratio < 0.5) {
            els.root.style.background = "rgba(232, 196, 71, 0.06)";
            els.root.classList.remove("ticker-card--hot");
        } else if (ratio < 0.75) {
            els.root.style.background = "rgba(232, 196, 71, 0.12)";
            els.root.classList.add("ticker-card--hot");
        } else {
            els.root.style.background = "rgba(184, 50, 39, 0.12)";
            els.root.classList.add("ticker-card--hot");
        }
        updateTickerRibbons(symbol);
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
        els.liqFill.style.inset = "0 " + (100 - pct) + "% 0 0";
        if (pct > 50) {
            els.liqFill.style.background = "var(--green)";
        } else if (pct > 20) {
            els.liqFill.style.background = "var(--gold)";
        } else {
            els.liqFill.style.background = "var(--red)";
        }
        els.liqPct.textContent = Math.round(pct) + "%";
    }

    function setDarkPoolHighlight(targetSymbol, active) {
        if (targetSymbol) {
            var els = tickerCardEls[targetSymbol];
            if (els) {
                els.root.classList.toggle("ticker-card--dark-pool", active);
                updateTickerRibbons(targetSymbol);
            }
        } else {
            var syms = Object.keys(tickerCardEls);
            for (var i = 0; i < syms.length; i++) {
                tickerCardEls[syms[i]].root.classList.toggle("ticker-card--dark-pool", active);
                updateTickerRibbons(syms[i]);
            }
        }
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
        var container = tickerCardEls[symbol].sparklineEl;
        draw.renderSparklineSVG(container, state.chart, symbol);
    }

    function redrawAllSparklines() {
        var symbols = Object.keys(tickerCardEls);
        for (var i = 0; i < symbols.length; i++) {
            redrawSparkline(symbols[i]);
        }
    }

    function updateAllRibbons() {
        var syms = Object.keys(tickerCardEls);
        for (var i = 0; i < syms.length; i++) {
            updateTickerRibbons(syms[i]);
        }
    }

    // --------------- arsenal (powerup tray) ---------------
    function renderArsenal() {
        var arsenalGrid = document.getElementById("arsenal-grid");
        arsenalGrid.innerHTML = "";

        var kickerEl = document.getElementById("arsenal-kicker");
        kickerEl.textContent = "Arsenal \u00B7 " + state.inventory.length + " cards";

        var groups = groupInventory(state.inventory);

        for (var i = 0; i < groups.length; i++) {
            (function (g) {
                var card = document.createElement("button");
                card.className = "powerup-card";
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

                // Count badge
                var countEl = document.createElement("div");
                countEl.className = "powerup-card__count";
                var countColor = isTargetPlayer ? "red" : isTargetSymbol ? "green" : "ink";
                countEl.classList.add("powerup-card__count--" + countColor);
                countEl.textContent = "\u00D7" + g.count;
                card.appendChild(countEl);

                // Top section
                var topDiv = document.createElement("div");
                var typeEl = document.createElement("div");
                typeEl.className = "powerup-card__type";
                typeEl.textContent = isTargetPlayer ? "Targeted" : isTargetSymbol ? "Symbol" : "Instant";
                topDiv.appendChild(typeEl);

                var nameEl = document.createElement("div");
                nameEl.className = "powerup-card__name";
                nameEl.textContent = g.name;
                topDiv.appendChild(nameEl);

                card.appendChild(topDiv);

                // Description
                var descEl = document.createElement("div");
                descEl.className = "powerup-card__desc";
                descEl.textContent = g.description || "";
                card.appendChild(descEl);

                // Click handler
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

                // Drag for player-targeted
                if (isTargetPlayer) {
                    card.addEventListener("dragstart", function (e) {
                        e.dataTransfer.setData("text/plain", g.name);
                        card.classList.add("powerup-card--dragging");
                        // Highlight standings
                        var entries = document.querySelectorAll(".standing-entry:not(.standing-entry--you)");
                        for (var k = 0; k < entries.length; k++) {
                            entries[k].classList.add("standing-entry--drop-target");
                        }
                    });
                    card.addEventListener("dragend", function () {
                        card.classList.remove("powerup-card--dragging");
                        var entries = document.querySelectorAll(".standing-entry--drop-target");
                        for (var k = 0; k < entries.length; k++) {
                            entries[k].classList.remove("standing-entry--drop-target");
                        }
                    });
                }

                // Drag for symbol-targeted
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

                arsenalGrid.appendChild(card);
            })(groups[i]);
        }
    }

    // --------------- message handlers ---------------
    var messageHandlers = {
        JOINED: function () {
            state.joined = true;
            state.status = "Connected";
            updateTopbar();
        },
        PLAYER_JOINED: function () {},
        ALL_PLAYERS_READY: function () {
            state.allPlayersReady = true;
            state.status = "All players ready";
        },
        PLAYERS_STATE: function (data) {
            state.players = data.players;
            // Capture initial portfolio for P/L calculation
            if (state.initialPortfolioValue === null && state.prices && Object.keys(state.prices).length > 0) {
                state.initialPortfolioValue = computePortfolioValue(playerId);
            }
            updateStandings();
            updateTopbar();
            updateAllRibbons();
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
            updateStandings();
            updateTopbar();
            // Capture initial portfolio
            if (state.initialPortfolioValue === null) {
                state.initialPortfolioValue = computePortfolioValue(playerId);
            }
            // Update symbol count in kicker
            document.getElementById("center-kicker").textContent =
                "Live Quotes \u00B7 " + symbols.length + " Symbols";
            // Delay sparkline draw until layout is ready
            requestAnimationFrame(function () {
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
        },
        ORDER_ACTIVITY: function (data) {
            state.chart = chart.appendAnnotation(state.chart, data.symbol, data.side, data.darkPool);
            redrawSparkline(data.symbol);
        },
        ORDER_FILLED: function (data) {
            var verb = data.side === "BUY" ? "Bought" : "Sold";
            var text = verb + " " + data.symbol + " at " + formatPrice(data.price);
            addTapeEvent("You", text, "pos");
            addWireNotification(text, "positive");
            if (state.darkPool && state.darkPool.tokens > 0) {
                state.darkPool.tokens--;
                updateInEffect();
            }
        },
        GAME_TICK: function (data) {
            state.currentTick = data.tick;
            state.ticksRemaining = data.ticksRemaining;
            if (state.totalDuration === null) {
                state.totalDuration = data.tick + data.ticksRemaining;
            }
            if (!state.gameFinished) {
                state.status = "Connected";
            }
            updateTopbar();
        },
        GAME_FINISHED: function () {
            state.gameFinished = true;
            state.ticksRemaining = 0;
            state.status = "Game Over";
            updateTopbar();
            addTapeEvent("Market", "Closing bell!", "warn");
            addWireNotification("Markets closed! Final standings...", "neutral");
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
                renderArsenal();
                addTapeEvent("You", "received " + data.powerupName, "pos");
                addWireNotification("You received " + data.powerupName + "!", "positive");
            } else {
                addTapeEvent(data.recipient, "received " + data.powerupName, "neutral");
                addWireNotification(data.recipient + " received " + data.powerupName, "neutral");
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
                renderArsenal();
                addTapeEvent("You", "used " + data.powerupName, "neutral");
                addWireNotification("You used " + data.powerupName, "neutral");
            } else {
                addTapeEvent(data.user, "used " + data.powerupName, "neutral");
                addWireNotification(data.user + " used " + data.powerupName, "neutral");
            }
        },
        FREEZE_STARTED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = true;
            updateStandings();
            updateTopbar();
            var syms = Object.keys(tickerCardEls);
            for (var k = 0; k < syms.length; k++) { updateTickerLiquidity(syms[k]); }
            var secs = data.durationSeconds ? data.durationSeconds + "s" : "a while";
            addTapeEvent("System", "Orders frozen for " + secs, "neg");
            addWireNotification("Orders frozen for " + secs + "!", "negative");
            updateInEffect();
        },
        FREEZE_EXPIRED: function (data) {
            if (data.frozenPlayer && data.frozenPlayer !== playerId) return;
            state.localFreezeActive = false;
            updateStandings();
            updateTopbar();
            var syms = Object.keys(tickerCardEls);
            for (var k = 0; k < syms.length; k++) { updateTickerLiquidity(syms[k]); }
            addTapeEvent("System", "Freeze expired \u2014 trading resumed", "pos");
            addWireNotification("Freeze expired \u2014 trading resumed", "positive");
            updateInEffect();
        },
        SENTIMENT_BOOST_ACTIVATED: function (data) {
            addTapeEvent(data.symbol, "sentiment boosted", "pos");
            addWireNotification(data.symbol + " sentiment boosted!", "positive");
        },
        TICKER_DELISTED: function (data) {
            state.delistedSymbols[data.symbol] = true;
            updateTickerDelisted(data.symbol);
            var card = tickerCardEls[data.symbol];
            if (card) {
                card.root.classList.remove("ticker-card--bubble-warning");
                void card.root.offsetWidth;
                card.root.classList.add("ticker-card--delisting");
                card.root.addEventListener("animationend", function (e) {
                    if (e.animationName === "delist-flash") {
                        card.root.classList.remove("ticker-card--delisting");
                    }
                }, { once: true });
            }
            reorderTickerCards();
            addTapeEvent(data.symbol, "delisted!", "neg");
            addWireNotification(data.symbol + " has been delisted!", "negative");
        },
        DIVIDEND_PAID: function (data) {
            if (data.playerId === playerId) {
                var tier = data.tierName ? " (" + data.tierName + ")" : "";
                var text = "Dividend: +" + formatPrice(data.amount) + " from " + data.symbol + tier;
                addTapeEvent("You", text, "pos");
                addWireNotification(text, "positive");
            } else if (data.playerId) {
                addTapeEvent(data.playerId, "received dividend from " + data.symbol, "neutral");
                addWireNotification(data.playerId + " received dividend from " + data.symbol, "neutral");
            }
        },
        BUBBLE_WARNING: function (data) {
            var card = tickerCardEls[data.symbol];
            if (card) {
                card.root.classList.remove("ticker-card--bubble-warning");
                void card.root.offsetWidth;
                card.root.classList.add("ticker-card--bubble-warning");
                card.root.addEventListener("animationend", function (e) {
                    if (e.animationName === "bubble-flash") {
                        card.root.classList.remove("ticker-card--bubble-warning");
                    }
                }, { once: true });
            }
            addTapeEvent(data.symbol, "\u25B2 overheating", "warn");
            addWireNotification(data.symbol + " is overheating! Bubble forming.", "negative");
            updateInEffect();
        },
        DARK_POOL_ACTIVATED: function (data) {
            state.darkPool = {
                tierName: data.tierName,
                targetSymbol: data.targetSymbol,
                tokens: data.tokens,
                ticks: data.ticks
            };
            setDarkPoolHighlight(data.targetSymbol, true);
            var target = data.targetSymbol ? " on " + data.targetSymbol : " (all symbols)";
            addTapeEvent("Dark Pool", "open \u00B7 " + (data.targetSymbol || "ALL"), "neutral");
            addWireNotification(data.tierName + " active" + target + " \u2014 " + data.tokens + " trades", "positive");
            updateInEffect();
        },
        DARK_POOL_EXPIRED: function () {
            if (state.darkPool) {
                setDarkPoolHighlight(state.darkPool.targetSymbol, false);
            }
            state.darkPool = null;
            addTapeEvent("Dark Pool", "expired", "neutral");
            addWireNotification("Dark Pool expired", "neutral");
            updateInEffect();
        },
        ORDER_BLOCKED: function (data) {
            if (data.playerId === playerId) {
                addWireNotification("Order blocked: " + data.reason, "negative");
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
            updateInEffect();
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
                    updateStandings();
                    updateTopbar();
                }
                addWireNotification("Order rejected: " + (data.message || "unknown"), "negative");
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
        updateTopbar();
    };

    ws.onerror = function () {
        state.status = "Connection error";
        updateTopbar();
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
        if (e.repeat && (e.key === "b" || e.key === "s")) return;
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
        redrawAllSparklines();
    });
})();
