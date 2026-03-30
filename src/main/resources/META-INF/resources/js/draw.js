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

window.Baz.draw = {
    renderSparkline: renderSparkline,
};
