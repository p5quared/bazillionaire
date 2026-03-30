// chart.js — chart state management, color palette, and shared layout constants
window.Baz = window.Baz || {};

// --------------- color palette ---------------
var COLORS = {
    fg:       "rgb(17, 17, 17)",
    bg:       "rgb(255, 255, 255)",
    bgHover:  "rgb(245, 245, 255)",
    bgLocal:  "rgb(255, 248, 230)",
    bgFrozen: "rgb(255, 238, 238)",
    disabled: "rgb(190, 190, 190)",
    muted:    "rgb(85, 85, 85)",
    border:   "rgb(17, 17, 17)",
    canvas:   "rgb(240, 240, 240)",
    green:    "rgb(10, 143, 63)",
    greenHov: "rgb(8, 120, 50)",
    red:      "rgb(214, 40, 40)",
    redHov:   "rgb(180, 30, 30)",
    amber:    "rgb(185, 120, 20)",
};

// --------------- layout constants ---------------
var LAYOUT = {
    STATUS_H:        0,
    PLAYER_H:        70,
    PLAYER_Y:        4,
    TICKER_CARD_GAP: -3,
    BTN_W:           70,
    BTN_H:           28,
    PAD:             12,
    OUTLINE_W:       3,
    FONT:            "monospace",
};

// --------------- chart state ---------------
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
                return { index: annotation.index - 1, side: annotation.side, darkPool: annotation.darkPool };
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

function appendAnnotation(chartState, symbol, side, darkPool) {
    var history = chartState.historyBySymbol[symbol];
    if (!history || history.length === 0) return chartState;
    var annotations = (chartState.annotationsBySymbol[symbol] || []).concat([{
        index: history.length - 1,
        side: side,
        darkPool: !!darkPool,
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
    var halfRange = Math.max(currentPrice * 0.10, 100);
    return {
        min: currentPrice - halfRange,
        max: currentPrice + halfRange,
    };
}

function computeDynamicRange(series, windowSize) {
    var start = Math.max(0, series.length - windowSize);
    var min = series[start];
    var max = series[start];
    for (var i = start + 1; i < series.length; i++) {
        if (series[i] < min) min = series[i];
        if (series[i] > max) max = series[i];
    }
    var padding = Math.max((max - min) * 0.1, 50);
    return {
        min: min - padding,
        max: max + padding,
    };
}

function priceToY(price, minPrice, maxPrice, y, h) {
    return y + h - ((price - minPrice) / (maxPrice - minPrice)) * h;
}

function buildLinePoints(series, rect, maxPoints, range) {
    var stepX = rect.w / (maxPoints - 1);
    var points = [];
    for (var i = 0; i < series.length; i++) {
        points.push({
            x: rect.x + i * stepX,
            y: priceToY(series[i], range.min, range.max, rect.y, rect.h),
        });
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
            darkPool: annotation.darkPool,
            x: rect.x + annotation.index * stepX,
            y: priceToY(series[annotation.index], range.min, range.max, rect.y, rect.h),
        });
    }
    return markers;
}

window.Baz.COLORS = COLORS;
window.Baz.LAYOUT = LAYOUT;
window.Baz.chart = {
    createChartState: createChartState,
    appendPrice: appendPrice,
    appendAnnotation: appendAnnotation,
    getSeries: getSeries,
    getAnnotations: getAnnotations,
    computeFixedRange: computeFixedRange,
    computeDynamicRange: computeDynamicRange,
    priceToY: priceToY,
    buildLinePoints: buildLinePoints,
    buildAnnotationMarkers: buildAnnotationMarkers,
};
