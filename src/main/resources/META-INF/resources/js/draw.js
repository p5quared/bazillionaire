// draw.js — all rendering functions
window.Baz = window.Baz || {};

var C = window.Baz.COLORS;
var L = window.Baz.LAYOUT;
var chart = window.Baz.chart;

// --------------- helpers ---------------
function formatPrice(cents) {
    if (cents === undefined || cents === null) return "--";
    return "$" + (cents / 100).toFixed(2);
}

function pointInRect(px, py, rx, ry, rw, rh) {
    return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
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

function computeCardLayout(symbols, canvasWidth, canvasHeight) {
    var tickerYStart = L.STATUS_H + L.PLAYER_H + 8;
    var cardH = canvasHeight - tickerYStart - L.PAD;
    var cardW = (canvasWidth - 2 * L.PAD + 3) / symbols.length - 3;
    var chartW = cardW - 2 * L.PAD;
    var chartH = cardH - 140;
    if (chartH < 20) chartH = 20;

    var layouts = [];
    for (var i = 0; i < symbols.length; i++) {
        var cardX = L.PAD + i * (cardW + 3);
        var cardY = tickerYStart;
        var btnY = cardY + cardH - L.BTN_H - L.PAD;
        var btnCenterX = cardX + cardW / 2;
        layouts.push({
            symbol: symbols[i],
            cardRect:  { x: cardX, y: cardY, w: cardW, h: cardH },
            chartRect: { x: cardX + L.PAD, y: cardY + 80, w: chartW, h: chartH },
            buyBtnRect:  { x: btnCenterX - L.BTN_W - 4, y: btnY, w: L.BTN_W, h: L.BTN_H },
            sellBtnRect: { x: btnCenterX + 4, y: btnY, w: L.BTN_W, h: L.BTN_H },
        });
    }
    return layouts;
}

function visibleSymbols(state) {
    if (state.symbols && state.symbols.length > 0) {
        return state.symbols.slice().sort();
    }
    return Object.keys(state.prices).sort();
}

function statusLine(state) {
    var parts = [state.status];
    if (state.gameFinished) {
        parts.push("Final standings locked");
    } else if (visibleSymbols(state).length === 0) {
        parts.push("Waiting for market open");
    } else {
        parts.push("Market live");
    }
    if (state.currentTick > 0 || state.ticksRemaining !== null) {
        parts.push("Tick " + state.currentTick);
    }
    if (state.ticksRemaining !== null && !state.gameFinished) {
        parts.push(state.ticksRemaining + " ticks left");
    }
    if (state.localFreezeActive) {
        parts.push("Orders frozen");
    }
    return parts.join("  |  ");
}

// --------------- draw functions ---------------
function drawChartLine(points) {
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
    var series = chart.getSeries(options.chartState, options.symbol);
    if (series.length < 2) return;

    var currentPrice = series[series.length - 1];
    var range = chart.computeFixedRange(currentPrice);
    var points = chart.buildLinePoints(series, options.rect, options.chartState.maxPoints, range);
    var markers = chart.buildAnnotationMarkers(
        series,
        chart.getAnnotations(options.chartState, options.symbol),
        options.rect,
        options.chartState.maxPoints,
        range
    );

    drawChartLine(points);
    drawMarkers(markers);
}

function drawStatusBar(state, canvasWidth) {
    drawRect({
        pos: vec2(0, 0),
        width: canvasWidth,
        height: L.STATUS_H,
        color: rgb(255, 255, 255),
    });
    drawText({
        text: statusLine(state),
        pos: vec2(10, 7),
        size: 14,
        font: L.FONT,
        color: rgb(17, 17, 17),
    });
    drawRect({
        pos: vec2(0, L.STATUS_H - 3),
        width: canvasWidth,
        height: 3,
        color: rgb(17, 17, 17),
    });
}

function drawPlayers(state, canvasWidth) {
    var playerIds = Object.keys(state.players).sort();
    var playerBoxW = playerIds.length > 0 ? Math.min(180, (canvasWidth - 2 * L.PAD) / playerIds.length) : 180;
    for (var i = 0; i < playerIds.length; i++) {
        var pid = playerIds[i];
        var p = state.players[pid];
        var px = L.PAD + i * playerBoxW;
        var isLocalPlayer = pid === state.playerId;
        var isFrozenPlayer = isLocalPlayer && state.localFreezeActive;
        var boxColor = rgb(255, 255, 255);
        var outlineColor = rgb(17, 17, 17);
        if (isFrozenPlayer) {
            boxColor = rgb(255, 238, 238);
            outlineColor = rgb(214, 40, 40);
        } else if (isLocalPlayer) {
            boxColor = rgb(255, 248, 230);
            outlineColor = rgb(185, 120, 20);
        }

        drawRect({
            pos: vec2(px, L.PLAYER_Y + 8),
            width: playerBoxW,
            height: L.PLAYER_H - L.PAD,
            color: boxColor,
            outline: { width: L.OUTLINE_W, color: outlineColor },
        });

        drawText({
            text: isLocalPlayer ? pid + " (you)" : pid,
            pos: vec2(px + 8, L.PLAYER_Y + 16),
            size: 12,
            font: L.FONT,
            color: rgb(17, 17, 17),
        });

        drawText({
            text: formatPrice(p.cashBalance),
            pos: vec2(px + 8, L.PLAYER_Y + 32),
            size: 16,
            font: L.FONT,
            color: rgb(17, 17, 17),
        });

        drawText({
            text: holdingText(p.holdings),
            pos: vec2(px + 8, L.PLAYER_Y + 50),
            size: 10,
            font: L.FONT,
            color: rgb(85, 85, 85),
        });
    }
}

function drawOrderButton(x, y, label, hovered, enabled, baseColor, hoverColor) {
    drawRect({
        pos: vec2(x, y),
        width: L.BTN_W,
        height: L.BTN_H,
        color: enabled ? (hovered ? hoverColor : baseColor) : rgb(190, 190, 190),
        outline: { width: 2, color: rgb(17, 17, 17) },
    });
    drawText({
        text: label,
        pos: vec2(x + L.BTN_W / 2 - (label === "BUY" ? 14 : 16), y + 7),
        size: 13,
        font: L.FONT,
        color: enabled ? rgb(255, 255, 255) : rgb(90, 90, 90),
    });
}

function drawTickerCard(state, symbol, cardRect, chartRect, mousePoint) {
    var isCardHovered = pointInRect(mousePoint.x, mousePoint.y, cardRect.x, cardRect.y, cardRect.w, cardRect.h);
    if (isCardHovered) state.hoveredSymbol = symbol;

    drawRect({
        pos: vec2(cardRect.x, cardRect.y),
        width: cardRect.w,
        height: cardRect.h,
        color: isCardHovered ? rgb(245, 245, 255) : rgb(255, 255, 255),
        outline: { width: L.OUTLINE_W, color: rgb(17, 17, 17) },
    });

    drawText({
        text: symbol,
        pos: vec2(cardRect.x + L.PAD, cardRect.y + 10),
        size: 13,
        font: L.FONT,
        color: rgb(17, 17, 17),
    });

    var price = state.prices[symbol];
    var prev = state.prevPrices[symbol];
    var priceColor = rgb(17, 17, 17);
    if (prev !== undefined && price > prev) priceColor = rgb(10, 143, 63);
    else if (prev !== undefined && price < prev) priceColor = rgb(214, 40, 40);
    var canTradeThisCard = !state.tradingDisabledReason && price !== undefined;

    if (price === undefined) {
        drawText({
            text: "Waiting...",
            pos: vec2(cardRect.x + L.PAD, cardRect.y + 42),
            size: 20,
            font: L.FONT,
            color: rgb(85, 85, 85),
        });
    } else {
        drawText({
            text: formatPrice(price),
            pos: vec2(cardRect.x + L.PAD, cardRect.y + 30),
            size: 40,
            font: L.FONT,
            color: priceColor,
        });

        drawSparklineChart({
            symbol: symbol,
            rect: chartRect,
            chartState: state.chart,
        });
    }

    var btnY = cardRect.y + cardRect.h - L.BTN_H - L.PAD;
    var btnCenterX = cardRect.x + cardRect.w / 2;
    var buyX = btnCenterX - L.BTN_W - 4;
    var sellX = btnCenterX + 4;

    var buyHovered = canTradeThisCard &&
        pointInRect(mousePoint.x, mousePoint.y, buyX, btnY, L.BTN_W, L.BTN_H);
    drawOrderButton(buyX, btnY, "BUY", buyHovered, canTradeThisCard, rgb(10, 143, 63), rgb(8, 120, 50));

    var sellHovered = canTradeThisCard &&
        pointInRect(mousePoint.x, mousePoint.y, sellX, btnY, L.BTN_W, L.BTN_H);
    drawOrderButton(sellX, btnY, "SELL", sellHovered, canTradeThisCard, rgb(214, 40, 40), rgb(180, 30, 30));
}

function drawTickerCards(state, mousePoint, canvasWidth, canvasHeight) {
    var symbols = visibleSymbols(state);
    state.hoveredSymbol = null;
    if (symbols.length === 0) return;

    var layouts = computeCardLayout(symbols, canvasWidth, canvasHeight);
    for (var i = 0; i < layouts.length; i++) {
        var lay = layouts[i];
        drawTickerCard(state, lay.symbol, lay.cardRect, lay.chartRect, mousePoint);
    }
}

window.Baz.draw = {
    drawStatusBar: drawStatusBar,
    drawPlayers: drawPlayers,
    drawTickerCards: drawTickerCards,
    computeCardLayout: computeCardLayout,
    pointInRect: pointInRect,
    formatPrice: formatPrice,
    visibleSymbols: visibleSymbols,
};
