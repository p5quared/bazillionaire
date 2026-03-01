function TickerChart(canvas, config) {
    config = config || {};
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.maxPoints = config.maxPoints || 60;
    this.lineColor = config.lineColor || '#111';
    this.lineWidth = config.lineWidth || 2;
    this.history = [];
    this.annotations = [];
}
TickerChart.prototype.addPrice = function(priceCents) {
    this.history.push(priceCents);
    if (this.history.length > this.maxPoints) {
        this.history.shift();
        for (var i = this.annotations.length - 1; i >= 0; i--) {
            this.annotations[i].index--;
            if (this.annotations[i].index < 0) this.annotations.splice(i, 1);
        }
    }
    this.render();
};
TickerChart.prototype.addAnnotation = function(side) {
    if (this.history.length > 0) {
        this.annotations.push({ index: this.history.length - 1, side: side });
    }
};
TickerChart.prototype.render = function() {
    var w = this.canvas.width;
    var h = this.canvas.height;
    var ctx = this.ctx;
    ctx.clearRect(0, 0, w, h);
    if (this.history.length < 2) return;

    var min = this.history[0], max = this.history[0];
    for (var i = 1; i < this.history.length; i++) {
        if (this.history[i] < min) min = this.history[i];
        if (this.history[i] > max) max = this.history[i];
    }
    var pad = Math.max((max - min) * 0.1, 1);
    min -= pad;
    max += pad;

    var stepX = w / (this.maxPoints - 1);
    var rangeY = max - min;

    ctx.beginPath();
    ctx.strokeStyle = this.lineColor;
    ctx.lineWidth = this.lineWidth;
    ctx.lineJoin = 'round';
    for (var i = 0; i < this.history.length; i++) {
        var x = i * stepX;
        var y = h - ((this.history[i] - min) / rangeY) * h;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
    }
    ctx.stroke();

    for (var i = 0; i < this.annotations.length; i++) {
        var a = this.annotations[i];
        var ax = a.index * stepX;
        var ay = h - ((this.history[a.index] - min) / rangeY) * h;
        var size = 4;
        ctx.beginPath();
        if (a.side === 'BUY') {
            ctx.fillStyle = '#0a8f3f';
            ay += size + 2;
            ctx.moveTo(ax, ay - size * 2);
            ctx.lineTo(ax - size, ay);
            ctx.lineTo(ax + size, ay);
        } else {
            ctx.fillStyle = '#d62828';
            ay -= size + 2;
            ctx.moveTo(ax, ay + size * 2);
            ctx.lineTo(ax - size, ay);
            ctx.lineTo(ax + size, ay);
        }
        ctx.closePath();
        ctx.fill();
    }
};
TickerChart.prototype.reset = function() {
    this.history = [];
    this.annotations = [];
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
};

window.TickerChart = TickerChart;
