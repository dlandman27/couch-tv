// The Live tab: the household's own TV line (live channels, films and series), served per
// category by the bridge (/api/tv/*). Three modes along the top; live channels browse as rows,
// films and series as poster walls; a series opens its seasons in an overlay. Owns everything
// inside #page-tv, the same way orbit-apps.js owns the Apps page.
(function () {
  "use strict";

  window.ORBIT_TV = function (ctx) {
    var $ = ctx.$, esc = ctx.esc, api = ctx.api, kept = ctx.kept, keep = ctx.keep;

    var MODES = [
      { id: "live", name: "Live TV", cols: 1 },
      { id: "vod", name: "Movies", cols: 7 },
      { id: "series", name: "Series", cols: 7 }
    ];
    var ROWS_DRAWN = 10;   // grid rows (or 60 list rows) around the focus: whole categories would crawl

    // Per-mode browse state, so flipping between Movies and Live TV loses nothing.
    function freshState() { return { cats: [], items: [], ci: 0, ii: 0, loadedCat: null, note: "", ready: false }; }
    var S = { live: freshState(), vod: freshState(), series: freshState() };
    var mode = kept("orbit.tv.mode") || "live";
    var sub = "modes";                     // modes | cats | grid | eps
    var mi = 0, configured = null;
    var eps = null, epSeason = 0, epI = 0, epFor = null;   // the series overlay

    // ---------------- data ----------------
    function st() { return S[mode]; }
    function modeAt() { return MODES.map(function (m) { return m.id; }).indexOf(mode); }
    function cols() { return MODES[modeAt()].cols; }

    function ensure() {
      mi = modeAt();
      var s = st();
      if (s.ready) { render(true); return; }
      render(true);
      api("GET", "/api/tv/categories?kind=" + mode).then(function (j) {
        configured = !!j.configured;
        s.cats = j.categories || [];
        s.ready = true;
        var last = kept("orbit.tv.cat." + mode);
        s.ci = Math.max(0, s.cats.map(function (c) { return c.id; }).indexOf(last));
        if (s.cats.length) loadCat(s.cats[s.ci].id); else render(true);
      }, function (e) { s.note = e.message; render(true); });
    }

    function loadCat(id) {
      var s = st(), forMode = mode;
      s.loadedCat = id; s.items = []; s.ii = 0; s.note = "";
      keep("orbit.tv.cat." + mode, id);
      render(true);
      api("GET", "/api/tv/channels?kind=" + mode + "&cat=" + encodeURIComponent(id)).then(function (j) {
        if (mode !== forMode || st().loadedCat !== id) return;
        st().items = j.items || [];
        render(true);
      }, function (e) { if (st().loadedCat === id) { st().note = e.message; render(true); } });
    }

    function openSeries(it) {
      eps = { loading: true, name: it.name }; epSeason = 0; epI = 0; epFor = it.id;
      render(true);
      api("GET", "/api/tv/episodes?id=" + encodeURIComponent(it.id)).then(function (j) {
        if (epFor !== it.id || !eps) return;
        eps = { name: j.name || it.name, seasons: (j.seasons || []).filter(function (se) { return (se.episodes || []).length; }) };
        render(true);
      }, function (e) { if (eps) { eps = { name: it.name, error: e.message, seasons: [] }; render(true); } });
    }

    function play(url) {
      api("POST", "/api/tv/play", { url: url }).catch(function (e) { st().note = e.message; render(true); });
    }

    // ---------------- drawing ----------------
    function stars(r) {
      if (!r) return "";
      var out = '<span class="tvp-stars">';
      for (var i = 1; i <= 5; i++) out += '<svg viewBox="0 0 24 24" class="' + (i <= Math.round(r) ? "fill" : "") + '"><path d="M12 3l2.7 5.8 6.3.7-4.7 4.3 1.3 6.2-5.6-3.2-5.6 3.2 1.3-6.2L3 9.5l6.3-.7z"/></svg>';
      return out + "</span>";
    }

    function renderModes(focused) {
      $("tv-modes").innerHTML = MODES.map(function (m, i) {
        return '<span class="tvm' + (m.id === mode ? " on" : "") + (focused && sub === "modes" && i === mi ? " focus" : "") + '">' + esc(m.name) + "</span>";
      }).join("");
    }

    function renderCats(focused) {
      var s = st();
      var from = Math.max(0, Math.min(s.ci - 8, s.cats.length - 18));
      $("tv-cats").innerHTML = s.cats.slice(from, from + 18).map(function (c, i) {
        var at = from + i;
        return '<div class="tv-cat' + (c.id === s.loadedCat ? " on" : "") +
          (focused && sub === "cats" && at === s.ci ? " focus" : "") + '">' + esc(c.name) + "</div>";
      }).join("");
    }

    function renderItems(focused) {
      var s = st(), el = $("tv-list");
      el.className = "tv-list" + (mode === "live" ? "" : " wall");
      if (!s.items.length) {
        el.innerHTML = '<p class="tv-none">' + esc(s.note || (s.loadedCat ? "Loading…" : "Pick a group.")) + "</p>";
        return;
      }
      var n = cols(), perWindow = mode === "live" ? 60 : n * ROWS_DRAWN;
      var from = Math.max(0, Math.min((Math.floor(s.ii / n) - 2) * n, s.items.length - perWindow));
      if (from < 0) from = 0;
      el.innerHTML = s.items.slice(from, from + perWindow).map(function (c, i) {
        var at = from + i, focus = focused && sub === "grid" && at === s.ii ? " focus" : "";
        if (mode === "live") {
          return '<div class="tv-row' + focus + '">' +
            (c.logo ? '<img src="' + esc(c.logo) + '" alt="" loading="lazy" onerror="this.remove()">' : '<span class="tv-dot"></span>') +
            '<span class="tv-name">' + esc(c.name) + "</span></div>";
        }
        return '<div class="tvp' + focus + '">' +
          '<div class="tvp-art"' + (c.logo ? ' style="background-image:url(&quot;' + esc(c.logo) + '&quot;)"' : "") + '></div>' +
          '<div class="tvp-meta"><span class="tvp-name">' + esc(c.name) + "</span>" + stars(c.rating) + "</div></div>";
      }).join("");
    }

    function renderEps(focused) {
      var el = $("tv-eps");
      el.classList.toggle("on", !!eps);
      if (!eps) { el.innerHTML = ""; return; }
      if (eps.loading) { el.innerHTML = '<div class="eps-box"><p class="tv-none">Loading…</p></div>'; return; }
      var season = (eps.seasons || [])[epSeason];
      el.innerHTML = '<div class="eps-box">' +
        '<h2 class="eps-title">' + esc(eps.name) + "</h2>" +
        (eps.error ? '<p class="tv-none">' + esc(eps.error) + "</p>" :
          '<div class="eps-seasons">' + eps.seasons.map(function (se, i) {
            return '<span class="tvm' + (i === epSeason ? " on" : "") + (focused && sub === "eps" && epI < 0 ? "" : "") + '">S' + se.season + "</span>";
          }).join("") + "</div>" +
          '<div class="eps-list">' + (season ? season.episodes.map(function (e, i) {
            return '<div class="eps-row' + (focused && sub === "eps" && i === epI ? " focus" : "") + '">' +
              '<span class="eps-num">E' + e.num + "</span><span class=\"tv-name\">" + esc(e.title || "Episode " + e.num) + "</span></div>";
          }).join("") : "") + "</div>") +
        "</div>";
      var f = el.getElementsByClassName("focus")[0];
      if (f && f.scrollIntoView) f.scrollIntoView({ block: "nearest" });
    }

    function render(focused) {
      if (!$("page-tv")) return;
      if (configured === false) {
        $("tv-modes").innerHTML = ""; $("tv-cats").innerHTML = "";
        $("tv-list").innerHTML = '<p class="tv-none">No TV line on this TV yet. Add your playlist’s server, username and password in settings.</p>';
        return;
      }
      renderModes(focused);
      if (!st().ready) { $("tv-cats").innerHTML = ""; $("tv-list").innerHTML = '<p class="tv-none">' + esc(st().note || "Loading your line…") + "</p>"; renderEps(focused); return; }
      renderCats(focused);
      renderItems(focused);
      renderEps(focused);
      var f = $("page-tv").getElementsByClassName("focus")[0];
      if (f && f.scrollIntoView && !eps) f.scrollIntoView({ block: "nearest" });
    }

    // ---------------- keys ----------------
    /** Arrows inside the page. Returns "leave" when focus moves back up to the tabs. */
    function move(dx, dy) {
      var s = st();
      if (eps) {
        if (!eps.seasons || !eps.seasons.length) return "";
        if (dx && eps.seasons.length > 1) { epSeason = Math.max(0, Math.min(eps.seasons.length - 1, epSeason + dx)); epI = 0; }
        else if (dy) epI = Math.max(0, Math.min(eps.seasons[epSeason].episodes.length - 1, epI + dy));
        render(true);
        return "";
      }
      if (sub === "modes") {
        if (dy < 0) return "leave";
        if (dx) { mi = Math.max(0, Math.min(MODES.length - 1, mi + dx)); setMode(MODES[mi].id); return ""; }
        if (dy > 0) sub = s.cats.length ? "cats" : "grid";
      } else if (sub === "cats") {
        if (dy < 0 && s.ci === 0) { sub = "modes"; render(true); return ""; }
        if (dy) s.ci = Math.max(0, Math.min(s.cats.length - 1, s.ci + dy));
        else if (dx > 0 && s.items.length) sub = "grid";
      } else {
        var n = cols(), row = Math.floor(s.ii / n);
        if (dx < 0 && s.ii % n === 0) sub = "cats";
        else if (dy < 0 && row === 0) { sub = "modes"; render(true); return ""; }
        else if (dx) { var nx = s.ii + dx; if (nx >= 0 && nx < s.items.length && Math.floor(nx / n) === row) s.ii = nx; }
        else if (dy) { var ny = s.ii + dy * n; if (ny >= 0) s.ii = Math.min(s.items.length - 1, ny); }
      }
      render(true);
      return "";
    }

    function setMode(id) {
      mode = id;
      keep("orbit.tv.mode", id);
      ensure();
    }

    function ok() {
      var s = st();
      if (eps) {
        var se = eps.seasons && eps.seasons[epSeason], e = se && se.episodes[epI];
        if (e) { play(e.url); }
        return;
      }
      if (sub === "modes") { setMode(MODES[mi].id); return; }
      if (sub === "cats") { var c = s.cats[s.ci]; if (c) loadCat(c.id); return; }
      var it = s.items[s.ii];
      if (!it) return;
      if (mode === "series") openSeries(it);
      else play(it.url);
    }

    /** Back inside the page: closes the series overlay. Returns true when it ate the press. */
    function back() {
      if (!eps) return false;
      eps = null; epFor = null;
      render(true);
      return true;
    }

    function enter() { sub = "modes"; ensure(); }

    return { render: render, move: move, ok: ok, back: back, enter: enter, ensure: ensure };
  };
})();
