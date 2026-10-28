// The Orbit look: one hero, tabs, continue watching, a live-score pill. Served by the same bridge
// as the other looks (config.js + /api/*), runs in the launcher's webview (window.TVOS) or a browser.
(function () {
  "use strict";

  var $ = function (id) { return document.getElementById(id); };
  var TV = window.TVOS || null;
  var CFG = window.COUCH_CONFIG || {};

  function esc(v) { return String(v == null ? "" : v).replace(/[&<>"]/g, function (c) { return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]; }); }

  function api(method, path, body) {
    return new Promise(function (resolve, reject) {
      var x = new XMLHttpRequest();
      x.open(method, path, true);
      x.timeout = 12000;
      if (body) x.setRequestHeader("Content-Type", "application/json");
      x.onload = function () {
        var j = null; try { j = JSON.parse(x.responseText); } catch (e) {}
        if (x.status >= 200 && x.status < 300) resolve(j || {});
        else reject(new Error((j && j.error) || ("HTTP " + x.status)));
      };
      x.onerror = x.ontimeout = function () { reject(new Error("no bridge")); };
      x.send(body ? JSON.stringify(body) : null);
    });
  }

  // ---------------- state ----------------
  var TABS = [
    { id: "foryou", name: "For You" },
    { id: "apps", name: "Apps" },
    { id: "scores", name: "Scores" }
  ];
  var tab = "foryou";                        // the page being shown
  var zone = "tabs";                         // tabs | avatar | hero | cards | pill | apps | board
  var ti = 0, hi = 0, ci = 0, ai = 0, bi = 0;
  var shows = [], apps = [], people = [], who = null;
  var GAMES = [], LG_NAMES = {}, SP = { leagues: [], follow: [], all_live: [] };
  var pillI = 0, pillTimer = null, scoresTimer = null;

  var ICON_PLAY = '<svg viewBox="0 0 24 24"><path d="M8 5.5v13l11-6.5z"/></svg>';
  var ICON_BALL = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5"/><path d="M12 3.5v17"/><path d="M5.6 6c3.6 3.5 9.2 3.5 12.8 0M5.6 18c3.6-3.5 9.2-3.5 12.8 0"/></svg>';
  var ICON_CHEV = '<svg viewBox="0 0 24 24" style="fill:none;stroke:currentColor;stroke-width:2"><path d="M9.5 5.5 16 12l-6.5 6.5"/></svg>';

  function banner(pkg) { return "http://tvos.invalid/banner/" + encodeURIComponent(pkg); }

  // ---------------- header ----------------
  function renderTabs() {
    $("tabs").innerHTML = TABS.map(function (t, i) {
      return '<span class="tab' + (t.id === tab ? " on" : "") + (zone === "tabs" && i === ti ? " focus" : "") + '">' + esc(t.name) + "</span>";
    }).join("");
    $("avatar").className = "avatar" + (zone === "avatar" ? " focus" : "");
    var p = people.filter(function (x) { return x.id === who; })[0];
    $("avatar").textContent = p ? p.name.charAt(0).toUpperCase() : "·";
  }

  function showTab(id) {
    tab = id;
    ["foryou", "apps", "scores"].forEach(function (t) { $("page-" + t).classList.toggle("on", t === id); });
    if (id === "scores") renderBoard();
    if (id === "apps") renderApps();
    renderTabs();
  }

  // ---------------- for you ----------------
  function heroItem() { return shows[0] || null; }

  function renderHero() {
    var it = heroItem();
    var art = "";
    if (it) {
      art = it.backdrop || it.poster || (it.app && it.app.package ? banner(it.app.package) : "");
      $("hero-kicker").textContent = "Keep watching";
      $("hero-title").textContent = it.title || "";
      var bits = [];
      if (it.season && it.episode) bits.push("S" + it.season + " · E" + it.episode);
      if (it.episode_title) bits.push(it.episode_title);
      if (it.app && it.app.name) bits.push(it.app.name);
      $("hero-sub").textContent = bits.join("  ·  ");
    } else {
      $("hero-kicker").textContent = "Picked for you";
      $("hero-title").textContent = greeting();
      $("hero-sub").textContent = "Pick an app, or ask the phone remote to play something.";
    }
    if (!art) {   // no show to feature: a scene for the hour keeps the card from sitting empty
      var h = new Date().getHours();
      art = "backgrounds/" + (h < 7 ? "scene-night.jpg" : h < 11 ? "scene-dawn.jpg" : h < 16 ? "scene-water.jpg" : h < 20 ? "scene-golden.jpg" : "scene-night.jpg");
    }
    $("hero-art").style.backgroundImage = 'url("' + art.replace(/"/g, "") + '")';
    var btns = ['<span class="hbtn' + (zone === "hero" && hi === 0 ? " focus" : "") + '" id="hb-watch">' + ICON_PLAY + "<span>" + (it ? "Watch" : "Apps") + "</span></span>"];
    $("hero-btns").innerHTML = btns.join("");
  }

  function greeting() {
    var h = new Date().getHours();
    return h < 5 ? "Up late?" : h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
  }

  function minutesLeftText(it) { return it && it.progress && it.progress.minutes_left ? it.progress.minutes_left + " min left" : ""; }

  /** The card row under the hero: more to keep watching, or the first apps while there is none. */
  function cardItems() {
    var cw = shows.slice(1, 5);
    if (cw.length) return cw.map(function (it) { return { kind: "show", it: it }; });
    return apps.slice(0, 4).map(function (a) { return { kind: "app", it: a }; });
  }

  function renderCards() {
    var list = cardItems();
    var fromShows = list.length && list[0].kind === "show";
    $("cw-title").textContent = fromShows ? "Continue Watching" : "Your apps";
    $("cw-title").classList.toggle("none", !list.length);
    $("cards").classList.toggle("none", !list.length);
    $("cards").innerHTML = list.map(function (c, i) {
      var focus = zone === "cards" && i === ci ? " focus" : "";
      if (c.kind === "app") {
        return '<div class="card' + focus + '" style="background-image:url(&quot;' + esc(banner(c.it.package)) + '&quot;)">' +
          '<div class="meta"><span class="name">' + esc(c.it.name) + "</span></div></div>";
      }
      var it = c.it;
      var art = it.backdrop || it.poster || (it.app && it.app.package ? banner(it.app.package) : "");
      var sub = minutesLeftText(it) || (it.season && it.episode ? "S" + it.season + " E" + it.episode : (it.app && it.app.name) || "");
      var pct = it.progress && it.progress.percent;
      return '<div class="card' + focus + '"' +
        (art ? ' style="background-image:url(&quot;' + esc(art) + '&quot;)"' : "") + ">" +
        '<div class="meta"><span class="name">' + esc(it.title) + '</span><span class="left">' + esc(sub) + "</span></div>" +
        (pct ? '<div class="bar"><i style="width:' + Math.min(100, Math.max(2, pct)) + '%"></i></div>' : "") +
        "</div>";
    }).join("");
  }

  // ---------------- score pill ----------------
  function followedGame(g) { return SP.follow.indexOf(g.home.abbr) >= 0 || SP.follow.indexOf(g.away.abbr) >= 0; }

  function pillGames() {
    var rank = { "in": 0, pre: 1, post: 2 };
    return GAMES.filter(function (g) { return followedGame(g) || (g.state === "in" && SP.all_live.indexOf(g.league) >= 0); })
      .sort(function (a, b) {
        return (rank[a.state] || 0) - (rank[b.state] || 0)
          || (followedGame(b) ? 1 : 0) - (followedGame(a) ? 1 : 0)
          || (Date.parse(a.start) || 0) - (Date.parse(b.start) || 0);
      });
  }

  function renderPill() {
    var list = pillGames();
    var el = $("score-pill");
    el.classList.toggle("show", list.length > 0);
    el.classList.toggle("focus", zone === "pill");
    if (!list.length) return;
    var g = list[pillI % list.length];
    var side = function (s) {
      return (s.logo ? '<img src="' + esc(s.logo) + '" alt="">' : "") +
        '<span' + (SP.follow.indexOf(s.abbr) >= 0 ? ' class="mine"' : "") + ">" + esc(s.abbr) + (g.state === "pre" ? "" : " <b>" + esc(s.score) + "</b>") + "</span>";
    };
    el.innerHTML = side(g.away) + "<span>—</span>" + side(g.home) +
      '<span class="det">' + esc(g.detail) + '</span><span class="chev">' + ICON_CHEV + "</span>";
  }

  function refreshScores() {
    clearTimeout(scoresTimer);
    if (!SP.leagues.length) return;
    api("GET", "/api/scores?leagues=" + encodeURIComponent(SP.leagues.join(",")))
      .then(function (j) {
        GAMES = j.games || [];
        (j.leagues || []).forEach(function (l) { LG_NAMES[l.id] = l.name; });
        renderPill();
        if (tab === "scores") renderBoard();
      }, function () {})
      .then(function () {
        var live = GAMES.some(function (g) { return g.state === "in"; });
        scoresTimer = setTimeout(refreshScores, live ? 60000 : 300000);
      });
  }

  // ---------------- apps ----------------
  function renderApps() {
    $("app-grid").innerHTML = apps.map(function (a, i) {
      return '<div class="appc' + (zone === "apps" && i === ai ? " focus" : "") + '" style="background-image:url(&quot;' + esc(banner(a.package)) + '&quot;)" aria-label="' + esc(a.name) + '"></div>';
    }).join("");
  }

  // ---------------- scores board ----------------
  function boardRows() {
    var rows = [];
    SP.leagues.forEach(function (lg) {
      var games = GAMES.filter(function (g) { return g.league === lg; });
      if (!games.length) return;
      rows.push({ head: LG_NAMES[lg] || lg });
      games.forEach(function (g) { rows.push({ game: g }); });
    });
    return rows;
  }

  function renderBoard() {
    var rows = boardRows();
    $("board").innerHTML = rows.length ? rows.map(function (r, i) {
      if (r.head) return '<h2 class="b-lg">' + esc(r.head) + "</h2>";
      var g = r.game;
      var side = function (s) {
        return (s.logo ? '<img class="b-logo" src="' + esc(s.logo) + '" alt="">' : "") +
          '<span class="b-team' + (SP.follow.indexOf(s.abbr) >= 0 ? " mine" : "") + '">' + esc(s.name) + "</span>";
      };
      var score = g.state === "pre" ? "" : '<span class="b-score">' + esc(g.away.score) + "–" + esc(g.home.score) + "</span>";
      return '<div class="b-game' + (g.state === "in" ? " live" : "") + (zone === "board" && i === bi ? " focus" : "") + '">' +
        side(g.away) + '<span class="b-at">@</span>' + side(g.home) + score +
        '<span class="b-detail">' + esc(g.detail) + "</span></div>";
    }).join("") : '<p class="b-none">No games today.</p>';
    var f = $("board").getElementsByClassName("focus")[0];
    if (f && f.scrollIntoView) f.scrollIntoView({ block: "center" });
  }

  // ---------------- focus and keys ----------------
  function paint() { renderTabs(); renderHero(); renderCards(); renderPill(); if (tab === "apps") renderApps(); if (tab === "scores") renderBoard(); }

  function cardCount() { return cardItems().length; }

  function move(dx, dy) {
    if (zone === "tabs") {
      if (dx > 0 && ti === TABS.length - 1) zone = "avatar";
      else if (dx) ti = Math.max(0, Math.min(TABS.length - 1, ti + dx));
      else if (dy > 0) zone = tab === "foryou" ? "hero" : tab === "apps" ? "apps" : "board";
    } else if (zone === "avatar") {
      if (dx < 0) zone = "tabs";
      else if (dy > 0) zone = tab === "foryou" ? "hero" : tab === "apps" ? "apps" : "board";
    } else if (zone === "hero") {
      if (dy < 0) zone = "tabs";
      else if (dy > 0) { if (cardCount()) { zone = "cards"; ci = 0; } else if (pillGames().length) zone = "pill"; }
    } else if (zone === "cards") {
      if (dy < 0) zone = "hero";
      else if (dy > 0 && pillGames().length) zone = "pill";
      else if (dx > 0 && ci === cardCount() - 1 && pillGames().length) zone = "pill";
      else if (dx) ci = Math.max(0, Math.min(cardCount() - 1, ci + dx));
    } else if (zone === "pill") {
      if (dy < 0 || dx < 0) zone = cardCount() ? "cards" : "hero";
    } else if (zone === "apps") {
      var cols = 6, row = Math.floor(ai / cols);
      if (dy < 0 && row === 0) zone = "tabs";
      else if (dx) { var nx = ai + dx; if (nx >= 0 && nx < apps.length && Math.floor(nx / cols) === row) ai = nx; }
      else if (dy) { var ny = ai + dy * cols; if (ny >= 0 && ny < apps.length) ai = ny; }
    } else if (zone === "board") {
      if (dy < 0 && firstGameRow() === bi) { zone = "tabs"; paint(); return; }
      if (dy) {
        var rows = boardRows(), i = bi + dy;
        while (i >= 0 && i < rows.length && rows[i].head) i += dy;
        if (i >= 0 && i < rows.length) bi = i;
      }
    }
    paint();
  }

  function firstGameRow() {
    var rows = boardRows(), i = 0;
    while (i < rows.length && rows[i].head) i++;
    return i;
  }

  function activate() {
    if (zone === "tabs") { showTab(TABS[ti].id); return; }
    if (zone === "avatar") { nextWho(); return; }
    if (zone === "hero") {
      var it = heroItem();
      if (!it) { ti = 1; showTab("apps"); zone = "apps"; ai = 0; paint(); return; }
      openShow(it);
      return;
    }
    if (zone === "cards") {
      var c = cardItems()[ci];
      if (c && c.kind === "show") openShow(c.it);
      else if (c) launch(c.it.package);
      return;
    }
    if (zone === "pill") { ti = 2; showTab("scores"); zone = "board"; bi = firstGameRow(); paint(); return; }
    if (zone === "apps") { var a = apps[ai]; if (a) launch(a.package); return; }
  }

  function back() {
    if (zone === "tabs" || zone === "avatar") { if (tab !== "foryou") { ti = 0; showTab("foryou"); } return; }
    zone = "tabs";
    ti = TABS.map(function (t) { return t.id; }).indexOf(tab);
    paint();
  }

  function openShow(it) {
    api("POST", "/api/continue/open", { id: it.id, who: who || "both" })
      .catch(function () { if (TV && it.app && it.app.package) TV.launch(it.app.package); });
  }

  function launch(pkg) {
    if (TV && TV.launch(pkg)) return;
    api("POST", "/api/launch", { app: pkg }).catch(function () {});
  }

  function nextWho() {
    if (!people.length) return;
    var i = people.map(function (p) { return p.id; }).indexOf(who);
    who = people[(i + 1) % people.length].id;
    api("POST", "/api/who", { who: who }).catch(function () {});
    renderTabs();
    loadShows();
  }

  document.addEventListener("keydown", function (e) {
    var k = e.key;
    if (k === "ArrowLeft") move(-1, 0);
    else if (k === "ArrowRight") move(1, 0);
    else if (k === "ArrowUp") move(0, -1);
    else if (k === "ArrowDown") move(0, 1);
    else if (k === "Enter") activate();
    else if (k === "Escape" || k === "Backspace") back();
    else return;
    e.preventDefault();
  });

  // the launcher calls these
  window.tv = {
    back: back,
    home: function () { ti = 0; zone = "tabs"; showTab("foryou"); refreshAll(); },
    resume: function () { refreshAll(); },
    photosAdded: function () {}
  };

  // ---------------- data ----------------
  function loadShows() {
    return api("GET", "/api/continue" + (who ? "?who=" + encodeURIComponent(who) : ""))
      .then(function (j) { shows = j.items || []; }, function () {})
      .then(function () { renderHero(); renderCards(); });
  }

  function loadApps() {
    if (TV) {
      try {
        apps = JSON.parse(TV.apps()).map(function (a) { return { name: a.label, package: a.package }; });
        return Promise.resolve();
      } catch (e) {}
    }
    return api("GET", "/api/apps").then(function (j) {
      apps = (j.apps || []).filter(function (a) { return a.installed !== false && a.id !== "home"; })
        .map(function (a) { return { name: a.name, package: a.package }; });
    }, function () {});
  }

  function loadSettings() {
    return api("GET", "/api/settings").then(function (j) {
      var sp = j.settings && j.settings.sports;
      if (sp && typeof sp === "object") {
        SP.leagues = Array.isArray(sp.leagues) ? sp.leagues : [];
        SP.follow = (Array.isArray(sp.follow) ? sp.follow : []).map(String);
        SP.all_live = (Array.isArray(sp.all_live) ? sp.all_live : []).map(String);
      }
      people = j.people || [];
      if (people.length && !who) who = people[0].id;
    }, function () {});
  }

  function refreshAll() {
    loadSettings().then(function () {
      loadShows();
      loadApps().then(paint);
      refreshScores();
    });
  }

  // the pill takes turns through the games
  pillTimer = setInterval(function () { if (pillGames().length > 1) { pillI++; renderPill(); } }, 6000);

  showTab(tab);
  refreshAll();
  paint();
})();
