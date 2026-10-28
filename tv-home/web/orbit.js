// The Orbit look: one hero, tabs, continue watching, a live-score pill. Served by the same bridge
// as the other looks (config.js + /api/*), runs in the launcher's webview (window.TVOS) or a browser
// for development (?mock=1 uses mock.js for everything, the same as index.html).
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
    { id: "foryou", name: "For You" },
    { id: "apps", name: "Apps" },
    { id: "tv", name: "Live" },
    { id: "scores", name: "Sports" }
  ];
  var tab = "foryou";                        // the page being shown
  var zone = "tabs";                         // tabs | avatar | hero | cards | pill | apps | sfilters | strip | shero | steams
  var ti = 0, hi = 0, ci = 0, fi = 0, si = 0, yi = 0;
  var shows = [], people = [], who = null;
  var GAMES = [], LG_NAMES = {}, SP = { leagues: [], follow: [], all_live: [], watch: {} };
  var filter = "all", featId = null, hideScores = false;
  try { hideScores = localStorage.getItem("orbit.hidescores") === "1"; } catch (e) {}
  var pillI = 0, pillTimer = null, scoresTimer = null;

  var ICON_PLAY = '<svg viewBox="0 0 24 24"><path d="M8 5.5v13l11-6.5z"/></svg>';
  var ICON_BALL = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5"/><path d="M12 3.5v17"/><path d="M5.6 6c3.6 3.5 9.2 3.5 12.8 0M5.6 18c3.6-3.5 9.2-3.5 12.8 0"/></svg>';
  var ICON_CHEV = '<svg viewBox="0 0 24 24" style="fill:none;stroke:currentColor;stroke-width:2"><path d="M9.5 5.5 16 12l-6.5 6.5"/></svg>';

  function banner(pkg) {
    if (!TV && window.MOCK_ICON) return window.MOCK_ICON(pkg) || "";
    return "http://tvos.invalid/banner/" + encodeURIComponent(pkg);
  }

  // The Apps page lives in its own file (orbit-apps.js) and owns everything inside the grid.
  var HOLD_MS = 600;    // how long OK is held to pick an app up
  var APPS = window.ORBIT_APPS({ $: $, esc: esc, api: api, kept: kept, keep: keep, banner: banner,
    launch: function (pkg) { launch(pkg); }, TV: TV });

  // The Live page (the household's TV line) lives in orbit-tv.js the same way.
  var TVL = window.ORBIT_TV({ $: $, esc: esc, api: api, kept: kept, keep: keep });

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
    ["foryou", "apps", "tv", "scores"].forEach(function (t) { $("page-" + t).classList.toggle("on", t === id); });
    if (id === "scores") renderSports();
    if (id === "apps") APPS.render(zone === "apps");
    if (id === "tv") TVL.ensure();
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
    return APPS.top4().map(function (a) { return { kind: "app", it: a }; });
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

  // ---------------- live: the sports hub ----------------
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
  function paint() { renderTabs(); renderHero(); renderCards(); renderPill(); if (tab === "apps") APPS.render(zone === "apps"); if (tab === "tv") TVL.render(zone === "tv"); if (tab === "scores") renderSports(); }

  function cardCount() { return cardItems().length; }

  function move(dx, dy) {
    if (zone === "tabs") {
      if (dx > 0 && ti === TABS.length - 1) zone = "avatar";
      else if (dx) ti = Math.max(0, Math.min(TABS.length - 1, ti + dx));
      else if (dy > 0) zone = tab === "foryou" ? "hero" : tab === "apps" ? "apps" : tab === "tv" ? "tv" : "sfilters";
    } else if (zone === "avatar") {
      if (dx < 0) zone = "tabs";
      else if (dy > 0) zone = tab === "foryou" ? "hero" : tab === "apps" ? "apps" : tab === "tv" ? "tv" : "sfilters";
    } else if (zone === "tv") {
      if (TVL.move(dx, dy) === "leave") zone = "tabs";
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
    if (zone === "tabs") { showTab(TABS[ti].id); return; }
    if (zone === "avatar") { nextWho(); return; }
    if (zone === "hero") {
      var it = heroItem();
      if (!it) { ti = 1; APPS.focusFirst(); zone = "apps"; showTab("apps"); paint(); return; }
      openShow(it);
      return;
    }
    if (zone === "cards") {
      var c = cardItems()[ci];
      if (c && c.kind === "show") openShow(c.it);
      else if (c) launch(c.it.package);
      return;
    }
    if (zone === "pill") { ti = 3; showTab("scores"); zone = stripGames().length ? "strip" : "sfilters"; si = 0; paint(); return; }
    if (zone === "apps") { APPS.ok(); return; }
    if (zone === "tv") { TVL.ok(); return; }
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
    if (zone === "tv" && TVL.back()) return;   // the series overlay closes first
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
    home: function () { okReset(); APPS.cancel(); ti = 0; zone = "tabs"; showTab("foryou"); refreshAll(); },
    resume: function () { okReset(); refreshAll(); },
    photosAdded: function () {}
  };

  // ---------------- data ----------------
  function loadShows() {
    return api("GET", "/api/continue" + (who ? "?who=" + encodeURIComponent(who) : ""))
      .then(function (j) { shows = j.items || []; }, function () {})
      .then(function () { renderHero(); renderCards(); });
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
