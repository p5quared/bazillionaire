// game.js — orchestrator: state, WebSocket, input, main draw loop
(function () {
    var chart = window.Baz.chart;
    var draw = window.Baz.draw;
    var L = window.Baz.LAYOUT;

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
        chart: chart.createChartState(60),
        hoveredSymbol: null,
        scrollY: 0,
    };

    function W() { return width(); }
    function H() { return height(); }

    // --------------- WebSocket ---------------
    var protocol = location.protocol === "https:" ? "wss:" : "ws:";
    var ws = new WebSocket(protocol + "//" + location.host + "/game/" + gameId);

    var messageHandlers = {
        PLAYERS_STATE: function (data) {
            state.players = data.players;
        },
        GAME_STATE: function (data) {
            state.prevPrices = Object.assign({}, state.prices);
            state.prices = data.prices;
            var syms = Object.keys(state.prices);
            for (var j = 0; j < syms.length; j++) {
                var s = syms[j];
                if (chart.getSeries(state.chart, s).length === 0) {
                    state.chart = chart.appendPrice(state.chart, s, state.prices[s]);
                }
            }
            if (data.players) state.players = data.players;
        },
        TICKER_TICKED: function (data) {
            state.prevPrices[data.symbol] = state.prices[data.symbol];
            state.prices[data.symbol] = data.price;
            state.chart = chart.appendPrice(state.chart, data.symbol, data.price);
        },
        ORDER_FILLED: function (data) {
            state.chart = chart.appendAnnotation(state.chart, data.symbol, data.side);
        },
    };

    ws.onopen = function () {
        state.status = "Connected. Joining...";
        ws.send(JSON.stringify({ type: "JOIN", payload: { playerId: playerId } }));
    };

    ws.onmessage = function (event) {
        var msg = JSON.parse(event.data);
        var handler = messageHandlers[msg.type];
        if (handler) {
            state.status = "Connected";
            handler(msg.data);
        }
    };

    ws.onclose = function () {
        state.status = "Disconnected";
    };
    ws.onerror = function () {
        state.status = "Connection error";
    };

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

    // --------------- main draw loop ---------------
    onDraw(function () {
        var mp = mousePos();
        var cw = W();
        var ch = H();

        draw.drawStatusBar(state, cw);
        draw.drawPlayers(state, cw);
        draw.drawTickerCards(state, mp, cw, ch);
    });
})();
