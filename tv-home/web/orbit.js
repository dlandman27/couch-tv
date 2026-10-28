// The Orbit look's controller: the glass nav, who has focus, and the wiring between pages. Each
// page owns its own file (orbit-home.js, orbit-apps.js, orbit-tv.js); the sports hub and the
// score pill live here. Served by the same bridge as the other looks (config.js + /api/*), runs
// in the launcher's webview (window.TVOS) or a browser (?mock=1 uses mock.js, as index.html does).
(function () {
  "use strict";

  var $ = function (id) { return document.getElementById(id); };
  var TV = window.TVOS || null;
  var MOCK = /[?&]mock\b/.test(location.search);
  var CFG = window.COUCH_CONFIG || {};   // read again in start(): with ?mock it arrives late

  function esc(v) { return String(v == null ? "" : v).replace(/[&<>"]/g, function (c) { return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]; }); }

  // What this TV keeps for itself (the app order, the New dots), shared with the other looks.
  var PFX = MOCK ? "mock:" : "";
  function kept(key) {
    var v = null;
    try { if (TV && TV.kept) v = TV.kept(PFX + key); } catch (e) {}
    if (v == null) try { v = localStorage.getItem(PFX + key); } catch (e) {}
    try { return v == null ? null : JSON.parse(v); } catch (e) { return null; }
  }
  function keep(key, value) {
    var j = JSON.stringify(value);
    try { localStorage.setItem(PFX + key, j); } catch (e) {}
    try { if (TV && TV.keep) TV.keep(PFX + key, j); } catch (e) {}
  }

  function api(method, path, body) {
    if (MOCK && window.MOCK_API) return window.MOCK_API(method, path, body);
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
    { id: "home", name: "Home", icon: "house" },
    { id: "movies", name: "Movies" },
    { id: "shows", name: "Shows" },
    { id: "livetv", name: "Live TV" },
    { id: "scores", name: "Sports" }
  ];
  var TV_MODE = { movies: "vod", shows: "series", livetv: "live" };
  var ICONS_NAV = [
    { id: "apps", label: "Apps", svg: '<svg viewBox="0 0 24 24"><rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/></svg>' },
    { id: "settings", label: "Settings", svg: '<svg viewBox="0 0 24 24"><path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"/><circle cx="12" cy="12" r="3"/></svg>' }
  ];
  var HOUSE = '<svg viewBox="0 0 24 24"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>';

  var tab = "home";
  // zone: tabs | icons | home | tv | eps | apps | sfilters | strip | shero | steams | pill
  var zone = "tabs";
  var ti = 0, iw = 0, fi = 0, si = 0, yi = 0;
  var shows = [], people = [], who = null;
  var GAMES = [], LG_NAMES = {}, SP = { leagues: [], follow: [], all_live: [], watch: {} };
  var filter = "all", featId = null, hideScores = false;
  try { hideScores = localStorage.getItem("orbit.hidescores") === "1"; } catch (e) {}
  var pillI = 0, pillTimer = null, scoresTimer = null;

  var ICON_PLAY = '<svg viewBox="0 0 24 24"><path d="M8 5.5v13l11-6.5z"/></svg>';
  var ICON_CHEV = '<svg viewBox="0 0 24 24" style="fill:none;stroke:currentColor;stroke-width:2"><path d="M9.5 5.5 16 12l-6.5 6.5"/></svg>';

  function banner(pkg) {
    if (!TV && window.MOCK_ICON) return window.MOCK_ICON(pkg) || "";
    return "http://tvos.invalid/banner/" + encodeURIComponent(pkg);
  }

  // ---------------- the pages, each in its own file ----------------
  var HOLD_MS = 600;    // how long OK is held to pick an app up
  var APPS = window.ORBIT_APPS({ $: $, esc: esc, api: api, kept: kept, keep: keep, banner: banner,
    launch: function (pkg) { launch(pkg); }, TV: TV });

  var TVL = window.ORBIT_TV({ $: $, esc: esc, api: api, kept: kept, keep: keep });

  var HOME = window.ORBIT_HOME({ $: $, esc: esc, api: api,
    focused: function () { return zone === "home"; },
    shows: function () { return shows; },
    openShow: function (it) { openShow(it); },
    openSeries: function (it) { TVL.openSeriesById(it.id, it.name); zone = "eps"; },
    openProvider: function (p) {
      var toShows = !p.vod && p.series;
      showTab(toShows ? "shows" : "movies");
      TVL.setMode(toShows ? "series" : "vod", toShows ? p.series : p.vod);
      zone = "tv"; TVL.enter();
      renderNav();
    }
  });

  // ---------------- the glass nav ----------------
  function initNav() {
    $("nav-tabs").innerHTML = TABS.map(function (t, i) {
      return '<span class="nav-item" id="navt-' + t.id + '">' + (t.icon ? HOUSE : "") + "<span>" + esc(t.name) + "</span></span>";
    }).join("");
    $("nav-icons").innerHTML = ICONS_NAV.map(function (b) {
      return '<span class="nav-item is-icon" id="navi-' + b.id + '" aria-label="' + esc(b.label) + '">' + b.svg + "</span>";
    }).join("");
  }

  function renderNav() {
    TABS.forEach(function (t, i) {
      var el = $("navt-" + t.id);
      el.className = "nav-item" + (t.id === tab ? " on" : "") + (zone === "tabs" && i === ti ? " focus" : "");
    });
    ICONS_NAV.forEach(function (b, i) {
      var el = $("navi-" + b.id);
      el.className = "nav-item is-icon" + (tab === "apps" && b.id === "apps" ? " on" : "") + (zone === "icons" && i === iw ? " focus" : "");
    });
    var p = people.filter(function (x) { return x.id === who; })[0];
    $("avatar").textContent = p ? p.name.charAt(0).toUpperCase() : "·";
    $("avatar").className = "avatar" + (zone === "icons" && iw === ICONS_NAV.length ? " focus" : "");
    // the glow pill slides under the active tab
    var on = $("navt-" + (tab === "apps" ? "home" : tab));
    var pill = $("nav-pill");
    if (on && tab !== "apps") {
      pill.style.opacity = "1";
      pill.style.transform = "translate3d(" + on.offsetLeft + "px, 0, 0)";
      pill.style.width = on.offsetWidth + "px";
    } else pill.style.opacity = "0";
  }

  function showTab(id) {
    tab = id;
    var page = { home: "home", movies: "tv", shows: "tv", livetv: "tv", scores: "scores", apps: "apps" }[id];
    ["home", "tv", "scores", "apps"].forEach(function (t) { $("page-" + t).classList.toggle("on", t === page); });
    if (TV_MODE[id]) TVL.setMode(TV_MODE[id]);
    if (id === "home") HOME.ensure();
    if (id === "scores") renderSports();
    if (id === "apps") APPS.render(zone === "apps");
    renderNav();
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
        '<span' + (SP.follow.indexOf(s.abbr) >= 0 ? ' class="mine"' : "") + ">" + esc(s.abbr) + (g.state === "pre" || hideScores ? "" : " <b>" + esc(s.score) + "</b>") + "</span>";
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
        if (tab === "scores") renderSports();
      }, function () {})
      .then(function () {
        var live = GAMES.some(function (g) { return g.state === "in"; });
        scoresTimer = setTimeout(refreshScores, live ? 60000 : 300000);
      });
  }

  // ---------------- the sports hub ----------------
  var SPORT_OF = { nfl: "Football", ncaaf: "Football", nba: "Basketball", mlb: "Baseball", nhl: "Hockey", mls: "Soccer", "eng.1": "Soccer", "uefa.champions": "Soccer" };

  /** The filter chips: All Sports, one per sport among the configured leagues, Hide Scores last. */
  function sportChips() {
    var seen = [], chips = [{ id: "all", name: "All Sports" }];
    SP.leagues.forEach(function (lg) {
      var s = SPORT_OF[lg];
      if (s && seen.indexOf(s) < 0) { seen.push(s); chips.push({ id: s, name: s }); }
    });
    chips.push({ id: "hide", name: "Hide Scores" });
    return chips;
  }

  function stripGames() {
    var rank = { "in": 0, pre: 1, post: 2 };
    return GAMES.filter(function (g) { return filter === "all" || SPORT_OF[g.league] === filter; })
      .sort(function (a, b) {
        return (rank[a.state] || 0) - (rank[b.state] || 0)
          || (followedGame(b) ? 1 : 0) - (followedGame(a) ? 1 : 0)
          || (Date.parse(a.start) || 0) - (Date.parse(b.start) || 0);
      });
  }

  /** The hero's game: the one picked from the strip or the panel, else live beats upcoming beats done, mine beat the rest. */
  function featured() {
    var list = stripGames();
    if (!list.length) return null;
    if (featId) for (var i = 0; i < list.length; i++) if (list[i].id === featId) return list[i];
    var best = list[0], bs = -1;
    list.forEach(function (g) {
      var s = (g.state === "in" ? 4 : g.state === "pre" ? 2 : 0) + (followedGame(g) ? 3 : 0);
      if (s > bs) { bs = s; best = g; }
    });
    return best;
  }

  /** The side panel's rows: each followed team with its game today, then a couple of upcoming games. */
  function sideRows() {
    var rows = SP.follow.map(function (abbr) {
      var g = null;
      GAMES.forEach(function (x) { if (!g && (x.away.abbr === abbr || x.home.abbr === abbr)) g = x; });
      return { team: abbr, side: g ? (g.away.abbr === abbr ? g.away : g.home) : null, game: g };
    });
    stripGames().filter(function (g) { return g.state === "pre" && !followedGame(g); }).slice(0, 2)
      .forEach(function (g) { rows.push({ coming: true, game: g }); });
    return rows;
  }

  function gameScore(g) { return hideScores || g.state === "pre" ? "" : g.away.score + "–" + g.home.score; }

  function renderFilters() {
    $("sp-filters").innerHTML = sportChips().map(function (c, i) {
      var on = c.id === "hide" ? hideScores : filter === c.id;
      return '<span class="chip' + (c.id === "hide" ? " toggle" : "") + (on ? " on" : "") + (zone === "sfilters" && i === fi ? " focus" : "") + '">' + esc(c.name) + "</span>";
    }).join("");
  }

  function renderStrip() {
    var list = stripGames();
    $("sp-strip").innerHTML = list.length ? list.map(function (g, i) {
      var det = (g.state === "in" ? '<i class="dot"></i>' : "") + esc(g.detail);
      var side = function (s) {
        return '<span class="sg-side">' + (s.logo ? '<img src="' + esc(s.logo) + '" alt="">' : "") +
          '<span class="sg-abbr' + (SP.follow.indexOf(s.abbr) >= 0 ? " mine" : "") + '">' + esc(s.abbr) + "</span>" +
          (g.state !== "pre" && !hideScores ? '<span class="sg-sc">' + esc(s.score) + "</span>" : "") + "</span>";
      };
      return '<div class="sg' + (g.state === "in" ? " live" : "") + (zone === "strip" && i === si ? " focus" : "") + '">' +
        '<div class="sg-top"><span>' + esc(LG_NAMES[g.league] || g.league) + '</span><span class="sg-det">' + det + "</span></div>" +
        '<div class="sg-teams">' + side(g.away) + '<span class="sg-bar"></span>' + side(g.home) + "</div></div>";
    }).join("") : '<p class="b-none">No games today.</p>';
    var f = $("sp-strip").getElementsByClassName("focus")[0];
    if (f && f.scrollIntoView) f.scrollIntoView({ inline: "center", block: "nearest" });
  }

  function renderSHero() {
    var g = featured(), el = $("sp-hero");
    el.classList.toggle("none", !g);
    if (!g) { el.innerHTML = ""; el.classList.remove("focus"); return; }
    var logo = function (s) { return s.logo ? '<img src="' + esc(s.logo) + '" alt="">' : '<span class="sh-ini">' + esc(s.abbr) + "</span>"; };
    var score = gameScore(g);
    var pkg = SP.watch[g.league];
    el.innerHTML =
      '<div class="sh-logos">' + logo(g.away) + '<span class="sh-at">at</span>' + logo(g.home) + "</div>" +
      '<div class="sh-text">' +
        '<p class="hero-kicker">' + (g.state === "in" ? '<span class="sh-live"><i class="dot"></i>LIVE</span>' : "") + esc(LG_NAMES[g.league] || g.league) + "</p>" +
        '<h1 class="sh-title">' + esc(g.away.name) + " at " + esc(g.home.name) + "</h1>" +
        '<p class="hero-sub">' + esc(g.detail) + (score ? "  ·  " + esc(score) : "") + "</p>" +
        (pkg ? '<div class="hero-btns"><span class="hbtn' + (zone === "shero" ? " focus" : "") + '">' + ICON_PLAY + "<span>Watch</span></span></div>" : "") +
      "</div>";
    el.classList.toggle("focus", zone === "shero" && !pkg);
  }

  function renderSide() {
    var rows = sideRows(), upHead = false;
    $("sp-side").innerHTML = '<h2 class="sp-h">Your Teams</h2>' + (rows.length ? rows.map(function (r, i) {
      var pre = "";
      if (r.coming && !upHead) { upHead = true; pre = '<h2 class="sp-h up">Coming Up</h2>'; }
      var focus = zone === "steams" && i === yi ? " focus" : "";
      var g = r.game;
      if (r.coming) {
        return pre + '<div class="yt-row' + focus + '">' + (g.away.logo ? '<img src="' + esc(g.away.logo) + '" alt="">' : "") +
          '<div class="yt-t"><span class="yt-name">' + esc(g.away.name) + " at " + esc(g.home.name) + '</span><span class="yt-sub">' + esc(g.detail) + "</span></div>" + ICON_CHEV + "</div>";
      }
      var sub = "No game today", live = false;
      if (g && g.state === "in") { live = true; sub = "Playing now" + (gameScore(g) ? " · " + gameScore(g) : ""); }
      else if (g && g.state === "pre") sub = g.detail;
      else if (g) sub = "Final" + (gameScore(g) ? " · " + gameScore(g) : "");
      return pre + '<div class="yt-row' + focus + '">' +
        (r.side && r.side.logo ? '<img src="' + esc(r.side.logo) + '" alt="">' : '<span class="yt-ini">' + esc(r.team.charAt(0)) + "</span>") +
        '<div class="yt-t"><span class="yt-name">' + esc(r.side ? r.side.name : r.team) + '</span><span class="yt-sub' + (live ? " live" : "") + '">' + esc(sub) + "</span></div>" +
        (g ? ICON_CHEV : "") + "</div>";
    }).join("") : '<p class="yt-none">Pick teams to follow in Settings on the phone.</p>');
    var f = $("sp-side").getElementsByClassName("focus")[0];
    if (f && f.scrollIntoView) f.scrollIntoView({ block: "nearest" });
  }

  function renderSports() { renderFilters(); renderStrip(); renderSHero(); renderSide(); }

  // ---------------- focus and keys ----------------
  function paint() {
    renderNav();
    renderPill();
    if (tab === "home") HOME.render(zone === "home");
    if (tab === "apps") APPS.render(zone === "apps");
    if (TV_MODE[tab]) TVL.render(zone === "tv");
    if (tab === "scores") renderSports();
  }

  function enterPage() {
    if (tab === "home") { zone = "home"; HOME.enter(); }
    else if (tab === "apps") { zone = "apps"; APPS.focusFirst(); }
    else if (TV_MODE[tab]) { zone = "tv"; TVL.enter(); }
    else zone = "sfilters";
  }

  function move(dx, dy) {
    if (zone === "eps") { TVL.move(dx, dy); return; }
    if (zone === "tabs") {
      if (dx > 0 && ti === TABS.length - 1) { zone = "icons"; iw = 0; }
      else if (dx) ti = Math.max(0, Math.min(TABS.length - 1, ti + dx));
      else if (dy > 0) enterPage();
    } else if (zone === "icons") {
      if (dx < 0 && iw === 0) zone = "tabs";
      else if (dx) iw = Math.max(0, Math.min(ICONS_NAV.length, iw + dx));
      else if (dy > 0) enterPage();
    } else if (zone === "home") {
      var r = HOME.move(dx, dy);
      if (r === "leave") zone = "tabs";
      else if (r === "pill" && pillGames().length) zone = "pill";
    } else if (zone === "tv") {
      if (TVL.move(dx, dy) === "leave") zone = "tabs";
    } else if (zone === "pill") {
      if (dy < 0 || dx < 0) zone = tab === "home" ? "home" : "tabs";
    } else if (zone === "apps") {
      if (APPS.move(dx, dy) === "leave") zone = "tabs";
    } else if (zone === "sfilters") {
      if (dy < 0) zone = "tabs";
      else if (dy > 0 && stripGames().length) { zone = "strip"; si = Math.min(si, stripGames().length - 1); }
      else if (dx) fi = Math.max(0, Math.min(sportChips().length - 1, fi + dx));
    } else if (zone === "strip") {
      if (dy < 0) zone = "sfilters";
      else if (dy > 0 && featured()) zone = "shero";
      else if (dx) si = Math.max(0, Math.min(stripGames().length - 1, si + dx));
    } else if (zone === "shero") {
      if (dy < 0) zone = "strip";
      else if (dx > 0 && sideRows().length) { zone = "steams"; yi = 0; }
    } else if (zone === "steams") {
      if (dx < 0) zone = "shero";
      else if (dy < 0 && yi === 0) zone = "strip";
      else if (dy) yi = Math.max(0, Math.min(sideRows().length - 1, yi + dy));
    }
    paint();
  }

  function activate() {
    if (zone === "eps") { TVL.ok(); return; }
    if (zone === "tabs") { showTab(TABS[ti].id); return; }
    if (zone === "icons") {
      if (iw === ICONS_NAV.length) { nextWho(); return; }
      var b = ICONS_NAV[iw];
      if (b.id === "apps") { showTab("apps"); enterPage(); paint(); }
      else if (b.id === "settings") api("POST", "/api/launch", { app: "settings" }).catch(function () {});
      return;
    }
    if (zone === "home") { HOME.ok(); return; }
    if (zone === "tv") { TVL.ok(); return; }
    if (zone === "pill") { showTab("scores"); ti = 4; zone = stripGames().length ? "strip" : "sfilters"; si = 0; paint(); return; }
    if (zone === "apps") { APPS.ok(); return; }
    if (zone === "sfilters") {
      var ch = sportChips()[fi];
      if (!ch) return;
      if (ch.id === "hide") {
        hideScores = !hideScores;
        try { localStorage.setItem("orbit.hidescores", hideScores ? "1" : "0"); } catch (e) {}
      } else { filter = ch.id; featId = null; si = 0; }
      paint();
      return;
    }
    if (zone === "strip") { var g = stripGames()[si]; if (g) { featId = g.id; zone = "shero"; paint(); } return; }
    if (zone === "shero") { var fg = featured(); if (fg && SP.watch[fg.league]) launch(SP.watch[fg.league]); return; }
    if (zone === "steams") { var r = sideRows()[yi]; if (r && r.game) { featId = r.game.id; paint(); } return; }
  }

  function back() {
    if (APPS.cancel()) return;   // an app in hand goes back where it was
    if (zone === "eps") { TVL.back(); zone = tab === "home" ? "home" : "tv"; paint(); return; }
    if (zone === "tv" && TVL.back()) return;
    if (zone === "tabs" || zone === "icons") { if (tab !== "home") { ti = 0; showTab("home"); } return; }
    zone = "tabs";
    ti = Math.max(0, TABS.map(function (t) { return t.id; }).indexOf(tab));
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
    renderNav();
    loadShows();
  }

  // OK acts when it is let go, so holding it can pick an app up instead (the other looks do the same).
  var okDown = false, okHeld = false, okTimer = null;
  function okReset() { okDown = false; okHeld = false; clearTimeout(okTimer); }
  document.addEventListener("keydown", function (e) {
    var k = e.key;
    if (k === "Enter") {
      e.preventDefault();
      if (e.repeat || okDown) return;   // a held key repeating: the timer is already running
      okDown = true; okHeld = false;
      okTimer = setTimeout(function () { okHeld = true; if (!(zone === "apps" && APPS.hold())) activate(); }, HOLD_MS);
      return;
    }
    if (k === "ArrowLeft") move(-1, 0);
    else if (k === "ArrowRight") move(1, 0);
    else if (k === "ArrowUp") move(0, -1);
    else if (k === "ArrowDown") move(0, 1);
    else if (k === "Escape" || k === "Backspace") back();
    else return;
    e.preventDefault();
  });
  document.addEventListener("keyup", function (e) {
    if (e.key !== "Enter") return;
    e.preventDefault();
    if (!okDown) return;
    var held = okHeld;
    okReset();
    if (!held) activate();
  });
  window.addEventListener("blur", okReset);

  // the launcher calls these
  window.tv = {
    back: back,
    home: function () { okReset(); APPS.cancel(); TVL.back(); ti = 0; zone = "tabs"; showTab("home"); refreshAll(); },
    resume: function () { okReset(); refreshAll(); },
    photosAdded: function () {}
  };

  // ---------------- data ----------------
  function loadShows() {
    return api("GET", "/api/continue" + (who ? "?who=" + encodeURIComponent(who) : ""))
      .then(function (j) { shows = j.items || []; }, function () {})
      .then(function () { if (tab === "home") HOME.render(zone === "home"); });
  }

  function loadSettings() {
    return api("GET", "/api/settings").then(function (j) {
      var sp = j.settings && j.settings.sports;
      if (sp && typeof sp === "object") {
        SP.leagues = Array.isArray(sp.leagues) ? sp.leagues : [];
        SP.follow = (Array.isArray(sp.follow) ? sp.follow : []).map(String);
        SP.all_live = (Array.isArray(sp.all_live) ? sp.all_live : []).map(String);
        SP.watch = sp.watch && typeof sp.watch === "object" && !Array.isArray(sp.watch) ? sp.watch : {};
      }
      APPS.setHidden(j.settings && j.settings.hidden_apps);
      people = j.people || CFG.people || [];
      if (people.length && !who) who = people[0].id;
    }, function () {});
  }

  function refreshAll() {
    if (APPS.lifting()) return;   // never reshuffle under an app being moved
    loadSettings().then(function () {
      loadShows();
      APPS.load().then(paint);
      refreshScores();
    });
  }

  function start() {
    CFG = window.COUCH_CONFIG || {};
    APPS.hidePackages(CFG.home && CFG.home.hide_packages);
    initNav();

    // the pill takes turns through the games
    pillTimer = setInterval(function () { if (pillGames().length > 1) { pillI++; renderPill(); } }, 6000);

    showTab(tab);
    refreshAll();
    paint();
  }

  if (MOCK) {
    var sc = document.createElement("script");
    sc.src = "mock.js";
    sc.onload = start;
    document.head.appendChild(sc);
  } else {
    start();
  }
})();
