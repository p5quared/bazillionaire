(function () {
  var canvas = document.getElementById('money-bg-canvas');
  if (!canvas) return;

  var ctx = canvas.getContext('2d');

  // Mulberry32 seeded PRNG
  function mulberry32(seed) {
    return function () {
      seed |= 0;
      seed = (seed + 0x6d2b79f5) | 0;
      var t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  var rand = mulberry32(0xdeadbeef);

  function drawBill(ctx, x, y, w, h, rotation, scale) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(rotation);
    ctx.scale(scale, scale);

    var bw = w;
    var bh = h;
    var r = 6;

    // Bill body
    ctx.beginPath();
    ctx.roundRect(-bw / 2, -bh / 2, bw, bh, r);
    ctx.fillStyle = '#85bb65';
    ctx.fill();
    ctx.strokeStyle = '#5a8a3c';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Inner frame
    var inset = 6;
    ctx.beginPath();
    ctx.roundRect(-bw / 2 + inset, -bh / 2 + inset, bw - inset * 2, bh - inset * 2, r - 2);
    ctx.strokeStyle = '#5a8a3c';
    ctx.lineWidth = 1;
    ctx.stroke();

    // Dollar sign
    ctx.fillStyle = '#3d6b24';
    ctx.font = '600 ' + Math.round(bh * 0.55) + 'px "DM Serif Display", Georgia, serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('$', 0, 1);

    // Denomination
    ctx.font = '700 ' + Math.round(bh * 0.16) + 'px "DM Sans", sans-serif';
    ctx.fillText('100', 0, bh / 2 - inset - 5);

    ctx.restore();
  }

  function draw() {
    var dpr = window.devicePixelRatio || 1;
    var W = window.innerWidth;
    var H = window.innerHeight;

    canvas.width = W * dpr;
    canvas.height = H * dpr;
    canvas.style.width = W + 'px';
    canvas.style.height = H + 'px';
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    ctx.clearRect(0, 0, W, H);

    // Reset PRNG for deterministic layout
    rand = mulberry32(0xdeadbeef);

    var billW = 90;
    var billH = 48;
    var count = Math.min(Math.ceil((W * H) / 2700), 450);

    for (var i = 0; i < count; i++) {
      var x = rand() * W;
      var y = rand() * H;
      var rotation = (rand() - 0.5) * (Math.PI / 3); // -30 to +30 deg
      var scale = 0.7 + rand() * 0.6;
      drawBill(ctx, x, y, billW, billH, rotation, scale);
    }
  }

  draw();

  // Debounced resize
  var resizeTimer;
  window.addEventListener('resize', function () {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(draw, 150);
  });
})();
