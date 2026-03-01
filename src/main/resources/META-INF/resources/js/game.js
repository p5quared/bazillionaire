(function () {
    var el = document.getElementById("game-data");
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

    // --------------- state ---------------
    var state = {
        status: "Connecting...",
        players: {},
        prices: {},
        prevPrices: {},
        chart: createChartState(60),
        hoveredSymbol: null,
        scrollY: 0,
    };

    // --------------- layout constants ---------------
    var STATUS_H = 30;
    var PLAYER_H = 70;
    var PLAYER_Y = STATUS_H;
    var TICKER_CARD_H = 300;
    var TICKER_CARD_GAP = -3;
    var CHART_H = 180;
    var BTN_W = 70;
    var BTN_H = 28;

    // --------------- helpers ---------------
    function formatPrice(cents) {
        return "$" + (cents / 100).toFixed(2);
    }

    function W() { return width(); }
    function H() { return height(); }

    function createChartState(maxPoints) {
        return {
            historyBySymbol: {},
            annotationsBySymbol: {},
            maxPoints: maxPoints,
        };
    }

    function ensureSymbolChart(chartState, symbol) {
        if (chartState.historyBySymbol[symbol] && chartState.annotationsBySymbol[symbol]) {
            return chartState;
        }
        var nextHistory = Object.assign({}, chartState.historyBySymbol);
        var nextAnnotations = Object.assign({}, chartState.annotationsBySymbol);
        if (!nextHistory[symbol]) nextHistory[symbol] = [];
        if (!nextAnnotations[symbol]) nextAnnotations[symbol] = [];
        return {
            historyBySymbol: nextHistory,
            annotationsBySymbol: nextAnnotations,
            maxPoints: chartState.maxPoints,
        };
    }

    function withSymbolValue(map, symbol, value) {
        var next = Object.assign({}, map);
        next[symbol] = value;
        return next;
    }

    function appendPrice(chartState, symbol, price) {
        var prepared = ensureSymbolChart(chartState, symbol);
        var history = prepared.historyBySymbol[symbol].concat([price]);
        var annotations = prepared.annotationsBySymbol[symbol];

        if (history.length > prepared.maxPoints) {
            history = history.slice(history.length - prepared.maxPoints);
            annotations = annotations
                .map(function (annotation) {
                    return { index: annotation.index - 1, side: annotation.side };
                })
                .filter(function (annotation) {
                    return annotation.index >= 0;
                });
        }

        return {
            historyBySymbol: withSymbolValue(prepared.historyBySymbol, symbol, history),
            annotationsBySymbol: withSymbolValue(prepared.annotationsBySymbol, symbol, annotations),
            maxPoints: prepared.maxPoints,
        };
    }

    function appendAnnotation(chartState, symbol, side) {
        var history = chartState.historyBySymbol[symbol];
        if (!history || history.length === 0) return chartState;
        var annotations = (chartState.annotationsBySymbol[symbol] || []).concat([{
            index: history.length - 1,
            side: side,
        }]);
        return {
            historyBySymbol: chartState.historyBySymbol,
            annotationsBySymbol: withSymbolValue(chartState.annotationsBySymbol, symbol, annotations),
            maxPoints: chartState.maxPoints,
        };
    }

    function getSeries(chartState, symbol) {
        return chartState.historyBySymbol[symbol] || [];
    }

    function getAnnotations(chartState, symbol) {
        return chartState.annotationsBySymbol[symbol] || [];
    }

    function computeFixedRange(currentPrice) {
        var halfRange = Math.max(currentPrice * 0.10, 100); // ±10% of current price, min ±$1
        return {
            min: currentPrice - halfRange,
            max: currentPrice + halfRange,
        };
    }

    function priceToY(price, minPrice, maxPrice, y, h) {
        return y + h - ((price - minPrice) / (maxPrice - minPrice)) * h;
    }

    function buildLinePoints(series, rect, maxPoints, range) {
        var stepX = rect.w / (maxPoints - 1);
        var points = [];
        for (var i = 0; i < series.length; i++) {
            points.push(vec2(
                rect.x + i * stepX,
                priceToY(series[i], range.min, range.max, rect.y, rect.h)
            ));
        }
        return points;
    }

    function buildAnnotationMarkers(series, annotations, rect, maxPoints, range) {
        var stepX = rect.w / (maxPoints - 1);
        var markers = [];
        for (var i = 0; i < annotations.length; i++) {
            var annotation = annotations[i];
            if (annotation.index < 0 || annotation.index >= series.length) continue;
            markers.push({
                side: annotation.side,
                x: rect.x + annotation.index * stepX,
                y: priceToY(series[annotation.index], range.min, range.max, rect.y, rect.h),
            });
        }
        return markers;
    }

    function drawLine(points) {
        drawLines({ pts: points, width: 2, color: rgb(17, 17, 17) });
    }

    function drawMarkers(markers) {
        for (var i = 0; i < markers.length; i++) {
            var marker = markers[i];
            var sz = 5;
            if (marker.side === "BUY") {
                drawTriangle({
                    p1: vec2(marker.x, marker.y + 3),
                    p2: vec2(marker.x - sz, marker.y + 3 + sz * 2),
                    p3: vec2(marker.x + sz, marker.y + 3 + sz * 2),
                    fill: true,
                    color: rgb(10, 143, 63),
                });
            } else {
                drawTriangle({
                    p1: vec2(marker.x, marker.y - 3),
                    p2: vec2(marker.x - sz, marker.y - 3 - sz * 2),
                    p3: vec2(marker.x + sz, marker.y - 3 - sz * 2),
                    fill: true,
                    color: rgb(214, 40, 40),
                });
            }
        }
    }

    function drawSparklineChart(options) {
        var series = getSeries(options.chartState, options.symbol);
        if (series.length < 2) return;

        var currentPrice = series[series.length - 1];
        var range = computeFixedRange(currentPrice);
        var points = buildLinePoints(series, options.rect, options.chartState.maxPoints, range);
        var markers = buildAnnotationMarkers(
            series,
            getAnnotations(options.chartState, options.symbol),
            options.rect,
            options.chartState.maxPoints,
            range
        );

        drawLine(points);
        drawMarkers(markers);
    }

    // --------------- WebSocket ---------------
    var protocol = location.protocol === "https:" ? "wss:" : "ws:";
    var ws = new WebSocket(protocol + "//" + location.host + "/game/" + gameId);

    ws.onopen = function () {
        state.status = "Connected. Joining...";
        ws.send(JSON.stringify({ type: "JOIN", payload: { playerId: playerId } }));
    };

    ws.onmessage = function (event) {
        state.status = "Connected";
        var msg = JSON.parse(event.data);

        if (msg.type === "PLAYERS_STATE") {
            state.players = msg.data.players;
        } else if (msg.type === "GAME_STATE") {
            state.prevPrices = Object.assign({}, state.prices);
            state.prices = msg.data.prices;
            var syms = Object.keys(state.prices);
            for (var j = 0; j < syms.length; j++) {
                var s = syms[j];
                if (getSeries(state.chart, s).length === 0) {
                    state.chart = appendPrice(state.chart, s, state.prices[s]);
                }
            }
            if (msg.data.players) state.players = msg.data.players;
        } else if (msg.type === "TICKER_TICKED") {
            state.prevPrices[msg.data.symbol] = state.prices[msg.data.symbol];
            state.prices[msg.data.symbol] = msg.data.price;
            state.chart = appendPrice(state.chart, msg.data.symbol, msg.data.price);
        } else if (msg.type === "ORDER_FILLED") {
            state.chart = appendAnnotation(state.chart, msg.data.symbol, msg.data.side);
        }
    };

    ws.onclose = function () {
        state.status = "Disconnected";
    };
    ws.onerror = function () {
        state.status = "Connection error";
    };

    function pointInRect(px, py, rx, ry, rw, rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    // --------------- input ---------------
    function sendOrder(type, symbol) {
        if (!state.prices[symbol]) return;
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

    onScroll(function (delta) {
        state.scrollY -= delta.y * 20;
        if (state.scrollY > 0) state.scrollY = 0;
    });

    function handleClick(mx, my) {
        var symbols = Object.keys(state.prices).sort();
        if (symbols.length === 0) return;
        var tickerYStart = STATUS_H + PLAYER_H + 8;
        var cardH = H() - tickerYStart - 12;
        var cardW = (W() - 24 + 3) / symbols.length - 3;
        for (var i = 0; i < symbols.length; i++) {
            var sym = symbols[i];
            var cardX = 12 + i * (cardW + 3);
            var cardY = tickerYStart;
            var btnY = cardY + cardH - BTN_H - 12;
            var btnCenterX = cardX + cardW / 2;
            if (pointInRect(mx, my, btnCenterX - BTN_W - 4, btnY, BTN_W, BTN_H)) {
                sendOrder("BUY", sym);
                return;
            }
            if (pointInRect(mx, my, btnCenterX + 4, btnY, BTN_W, BTN_H)) {
                sendOrder("SELL", sym);
                return;
            }
        }
    }

    function drawStatusBar(canvasWidth) {
        drawRect({
            pos: vec2(0, 0),
            width: canvasWidth,
            height: STATUS_H,
            color: rgb(255, 255, 255),
        });
        drawText({
            text: state.status,
            pos: vec2(10, 7),
            size: 14,
            font: "monospace",
            color: rgb(17, 17, 17),
        });
        drawRect({
            pos: vec2(0, STATUS_H - 3),
            width: canvasWidth,
            height: 3,
            color: rgb(17, 17, 17),
        });
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

    function drawPlayers(canvasWidth) {
        var playerIds = Object.keys(state.players).sort();
        var playerBoxW = playerIds.length > 0 ? Math.min(180, (canvasWidth - 24) / playerIds.length) : 180;
        for (var i = 0; i < playerIds.length; i++) {
            var pid = playerIds[i];
            var p = state.players[pid];
            var px = 12 + i * playerBoxW;

            drawRect({
                pos: vec2(px, PLAYER_Y + 8),
                width: playerBoxW,
                height: PLAYER_H - 12,
                color: rgb(255, 255, 255),
                outline: { width: 3, color: rgb(17, 17, 17) },
            });

            drawText({
                text: pid,
                pos: vec2(px + 8, PLAYER_Y + 16),
                size: 12,
                font: "monospace",
                color: rgb(17, 17, 17),
            });

            drawText({
                text: formatPrice(p.cashBalance),
                pos: vec2(px + 8, PLAYER_Y + 32),
                size: 16,
                font: "monospace",
                color: rgb(17, 17, 17),
            });

            drawText({
                text: holdingText(p.holdings),
                pos: vec2(px + 8, PLAYER_Y + 50),
                size: 10,
                font: "monospace",
                color: rgb(85, 85, 85),
            });
        }
    }

    function drawOrderButton(x, y, label, hovered, baseColor, hoverColor) {
        drawRect({
            pos: vec2(x, y),
            width: BTN_W,
            height: BTN_H,
            color: hovered ? hoverColor : baseColor,
            outline: { width: 2, color: rgb(17, 17, 17) },
        });
        drawText({
            text: label,
            pos: vec2(x + BTN_W / 2 - (label === "BUY" ? 14 : 16), y + 7),
            size: 13,
            font: "monospace",
            color: rgb(255, 255, 255),
        });
    }

    function drawTickerCard(symbol, cardRect, chartRect, mousePoint) {
        var isCardHovered = pointInRect(mousePoint.x, mousePoint.y, cardRect.x, cardRect.y, cardRect.w, cardRect.h);
        if (isCardHovered) state.hoveredSymbol = symbol;

        drawRect({
            pos: vec2(cardRect.x, cardRect.y),
            width: cardRect.w,
            height: cardRect.h,
            color: isCardHovered ? rgb(245, 245, 255) : rgb(255, 255, 255),
            outline: { width: 3, color: rgb(17, 17, 17) },
        });

        drawText({
            text: symbol,
            pos: vec2(cardRect.x + 12, cardRect.y + 10),
            size: 13,
            font: "monospace",
            color: rgb(17, 17, 17),
        });

        var price = state.prices[symbol];
        var prev = state.prevPrices[symbol];
        var priceColor = rgb(17, 17, 17);
        if (prev !== undefined && price > prev) priceColor = rgb(10, 143, 63);
        else if (prev !== undefined && price < prev) priceColor = rgb(214, 40, 40);

        drawText({
            text: formatPrice(price),
            pos: vec2(cardRect.x + 12, cardRect.y + 30),
            size: 40,
            font: "monospace",
            color: priceColor,
        });

        drawSparklineChart({
            symbol: symbol,
            rect: chartRect,
            chartState: state.chart,
        });

        var btnY = cardRect.y + cardRect.h - BTN_H - 12;
        var btnCenterX = cardRect.x + cardRect.w / 2;
        var buyX = btnCenterX - BTN_W - 4;
        var sellX = btnCenterX + 4;

        var buyHovered = pointInRect(mousePoint.x, mousePoint.y, buyX, btnY, BTN_W, BTN_H);
        drawOrderButton(
            buyX,
            btnY,
            "BUY",
            buyHovered,
            rgb(10, 143, 63),
            rgb(8, 120, 50)
        );

        var sellHovered = pointInRect(mousePoint.x, mousePoint.y, sellX, btnY, BTN_W, BTN_H);
        drawOrderButton(
            sellX,
            btnY,
            "SELL",
            sellHovered,
            rgb(214, 40, 40),
            rgb(180, 30, 30)
        );
    }

    function drawTickerCards(mousePoint, canvasWidth, canvasHeight) {
        var symbols = Object.keys(state.prices).sort();
        var tickerYStart = STATUS_H + PLAYER_H + 8;
        var cardH = canvasHeight - tickerYStart - 12;
        state.hoveredSymbol = null;
        if (symbols.length === 0) return;

        var cardW = (canvasWidth - 24 + 3) / symbols.length - 3;
        var chartW = cardW - 24;
        var chartH = cardH - 140;
        if (chartH < 20) chartH = 20;

        for (var i = 0; i < symbols.length; i++) {
            var sym = symbols[i];
            var cardX = 12 + i * (cardW + 3);
            var cardY = tickerYStart;
            drawTickerCard(
                sym,
                { x: cardX, y: cardY, w: cardW, h: cardH },
                { x: cardX + 12, y: cardY + 80, w: chartW, h: chartH },
                mousePoint
            );
        }
    }

    // --------------- main draw loop ---------------
    onDraw(function () {
        var mp = mousePos();
        var cw = W();
        var ch = H();

        drawStatusBar(cw);
        drawPlayers(cw);
        drawTickerCards(mp, cw, ch);
    });
})();
