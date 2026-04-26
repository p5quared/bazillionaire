// draw.js — sparkline-only renderer
window.Baz = window.Baz || {};

var chart = window.Baz.chart;

function rgb(r, g, b) { return "rgb(" + r + "," + g + "," + b + ")"; }

function drawChartLine(ctx, points) {
    if (!points || points.length < 2) return;
    ctx.beginPath();
    ctx.moveTo(points[0].x, points[0].y);
    for (var i = 1; i < points.length; i++) {
        ctx.lineTo(points[i].x, points[i].y);
    }
    ctx.lineWidth = 2;
    ctx.strokeStyle = rgb(17, 17, 17);
    ctx.stroke();
}

function drawMarkers(ctx, markers) {
    for (var i = 0; i < markers.length; i++) {
        var marker = markers[i];
        var sz = 5;
        ctx.beginPath();
        if (marker.side === "BUY") {
            ctx.moveTo(marker.x, marker.y + 3);
            ctx.lineTo(marker.x - sz, marker.y + 3 + sz * 2);
            ctx.lineTo(marker.x + sz, marker.y + 3 + sz * 2);
            ctx.closePath();
            if (marker.darkPool) {
                ctx.strokeStyle = rgb(10, 143, 63);
                ctx.lineWidth = 1.5;
                ctx.stroke();
            } else {
                ctx.fillStyle = rgb(10, 143, 63);
                ctx.fill();
            }
        } else {
            ctx.moveTo(marker.x, marker.y - 3);
            ctx.lineTo(marker.x - sz, marker.y - 3 - sz * 2);
            ctx.lineTo(marker.x + sz, marker.y - 3 - sz * 2);
            ctx.closePath();
            if (marker.darkPool) {
                ctx.strokeStyle = rgb(214, 40, 40);
                ctx.lineWidth = 1.5;
                ctx.stroke();
            } else {
                ctx.fillStyle = rgb(214, 40, 40);
                ctx.fill();
            }
        }
    }
}

function renderSparkline(canvasEl, chartState, symbol) {
    var w = canvasEl.width;
    var h = canvasEl.height;
    var ctx = canvasEl.getContext("2d");
    ctx.clearRect(0, 0, w, h);

    var series = chart.getSeries(chartState, symbol);
    if (series.length < 2) return;

    var range = chart.computeDynamicRange(series, 100);
    var rect = { x: 0, y: 0, w: w, h: h };
    var points = chart.buildLinePoints(series, rect, chartState.maxPoints, range);
    var markers = chart.buildAnnotationMarkers(
        series,
        chart.getAnnotations(chartState, symbol),
        rect,
        chartState.maxPoints,
        range
    );

    drawChartLine(ctx, points);
    drawMarkers(ctx, markers);
}

// --------------- SVG sparkline renderer ---------------
var SVG_NS = "http://www.w3.org/2000/svg";
var SVG_W = 200;
var SVG_H = 44;

function svgEl(tag, attrs) {
    var el = document.createElementNS(SVG_NS, tag);
    if (attrs) {
        var keys = Object.keys(attrs);
        for (var i = 0; i < keys.length; i++) {
            el.setAttribute(keys[i], attrs[keys[i]]);
        }
    }
    return el;
}

function buildPathD(points) {
    if (!points || points.length < 2) return "";
    var d = "M" + points[0].x.toFixed(1) + "," + points[0].y.toFixed(1);
    for (var i = 1; i < points.length; i++) {
        d += " L" + points[i].x.toFixed(1) + "," + points[i].y.toFixed(1);
    }
    return d;
}

function renderSparklineSVG(containerEl, chartState, symbol) {
    containerEl.innerHTML = "";

    var series = chart.getSeries(chartState, symbol);
    if (series.length < 2) return;

    var range = chart.computeDynamicRange(series, 100);
    var rect = { x: 0, y: 0, w: SVG_W, h: SVG_H };
    var points = chart.buildLinePoints(series, rect, chartState.maxPoints, range);
    var markers = chart.buildAnnotationMarkers(
        series,
        chart.getAnnotations(chartState, symbol),
        rect,
        chartState.maxPoints,
        range
    );

    var up = series[series.length - 1] >= series[0];
    var color = up ? "var(--green, #2d7a3f)" : "var(--red, #b83227)";
    var gradId = "sg-" + symbol.replace(/[^a-zA-Z0-9]/g, "");

    var svg = svgEl("svg", {
        viewBox: "0 0 " + SVG_W + " " + SVG_H,
        preserveAspectRatio: "none",
        style: "width:100%;height:100%;display:block;"
    });

    // gradient
    var defs = svgEl("defs");
    var grad = svgEl("linearGradient", { id: gradId, x1: "0", x2: "0", y1: "0", y2: "1" });
    var stop1 = svgEl("stop", { offset: "0", "stop-color": color, "stop-opacity": "0.18" });
    var stop2 = svgEl("stop", { offset: "1", "stop-color": color, "stop-opacity": "0" });
    grad.appendChild(stop1);
    grad.appendChild(stop2);
    defs.appendChild(grad);
    svg.appendChild(defs);

    // fill area
    var lineD = buildPathD(points);
    if (lineD) {
        var lastPt = points[points.length - 1];
        var firstPt = points[0];
        var fillD = lineD + " L" + lastPt.x.toFixed(1) + "," + SVG_H + " L" + firstPt.x.toFixed(1) + "," + SVG_H + " Z";
        svg.appendChild(svgEl("path", { d: fillD, fill: "url(#" + gradId + ")" }));
        svg.appendChild(svgEl("path", { d: lineD, fill: "none", stroke: color, "stroke-width": "1.6" }));
    }

    // annotation markers
    for (var i = 0; i < markers.length; i++) {
        var m = markers[i];
        var sz = 4;
        var pts;
        if (m.side === "BUY") {
            pts = (m.x) + "," + (m.y + 2) + " " + (m.x - sz) + "," + (m.y + 2 + sz * 2) + " " + (m.x + sz) + "," + (m.y + 2 + sz * 2);
        } else {
            pts = (m.x) + "," + (m.y - 2) + " " + (m.x - sz) + "," + (m.y - 2 - sz * 2) + " " + (m.x + sz) + "," + (m.y - 2 - sz * 2);
        }
        var mColor = m.side === "BUY" ? "var(--green, #2d7a3f)" : "var(--red, #b83227)";
        var polyAttrs = { points: pts };
        if (m.darkPool) {
            polyAttrs.fill = "none";
            polyAttrs.stroke = mColor;
            polyAttrs["stroke-width"] = "1.5";
        } else {
            polyAttrs.fill = mColor;
        }
        svg.appendChild(svgEl("polygon", polyAttrs));
    }

    containerEl.appendChild(svg);
}

window.Baz.draw = {
    renderSparkline: renderSparkline,
    renderSparklineSVG: renderSparklineSVG,
};
