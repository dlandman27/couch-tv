// The home page: a hero carousel of the line's newest films, Browse by Provider, and trending
// rails, fed by one /api/tv/home call (plus keep-watching). Owns everything inside #page-home,
// the same way orbit-apps.js owns the Apps page.
(function () {
  "use strict";

  window.ORBIT_HOME = function (ctx) {
    var $ = ctx.$, esc = ctx.esc, api = ctx.api;
    var ICON_PLAY = '<svg viewBox="0 0 24 24"><path d="M8 5.5v13l11-6.5z"/></svg>';

    var data = null, note = "", loading = false;
    var hi = 0;                      // the hero slide on show
    var sub = "hero", ri = 0;        // hero | prov | row:cw | row:movies | row:series, and the index within
    var heroTimer = null, lastTurn = 0;

    var BRAND_COLORS = {
      netflix: "#e50914", osn: "#8b5cf6", disney: "#0e45a6", apple: "#a2aaad", prime: "#00a8e1",
      hbo: "#5822b4", paramount: "#0064ff", crunchyroll: "#f47521", discovery: "#2175ff",
      hulu: "#1ce783", peacock: "#000000", starz: "#101820"
    };

    // ---------------- data ----------------
    function ensure() {
      if (data || loading) { render(true); return; }
      loading = true;
      render(true);
      api("GET", "/api/tv/home").then(function (j) {
        loading = false;
        if (j.configured === false) { note = "none"; render(true); return; }
        data = { featured: j.featured || [], movies: j.movies || [], series: j.series || [], providers: j.providers || [] };
        startTurning();
        render(true);
      }, function (e) { loading = false; note = e.message; render(true); });
    }

    function startTurning() {
      clearInterval(heroTimer);
      heroTimer = setInterval(function () {
        if (!data || !data.featured.length || document.hidden) return;
        if (Date.now() - lastTurn < 7500) return;   // a remote press holds the slide a while
        hi = (hi + 1) % data.featured.length;
        renderHero(ctx.focused());
      }, 8000);
    }

    // ---------------- rows ----------------
    function rows() {
      var out = [];
      if (ctx.shows().length) out.push({ id: "cw", name: "Continue Watching", items: ctx.shows() });
      if (data && data.movies.length) out.push({ id: "movies", name: "Trending Movies", items: data.movies });
      if (data && data.series.length) out.push({ id: "series", name: "Trending Series", items: data.series });
      return out;
    }

    function zones() {
      var z = [];
      if (data && data.featured.length) z.push("hero");
      if (data && data.providers.length) z.push("prov");
      rows().forEach(function (r) { z.push("row:" + r.id); });
      return z.length ? z : ["hero"];
    }

    // ---------------- drawing ----------------
    function stars(r) {
      if (!r) return "";
      var out = '<span class="tvp-stars">';
      for (var i = 1; i <= 5; i++) out += '<svg viewBox="0 0 24 24" class="' + (i <= Math.round(r) ? "fill" : "") + '"><path d="M12 3l2.7 5.8 6.3.7-4.7 4.3 1.3 6.2-5.6-3.2-5.6 3.2 1.3-6.2L3 9.5l6.3-.7z"/></svg>';
      return out + "</span>";
    }

    function renderHero(focused) {
      var el = $("hc");
      if (!data || !data.featured.length) {
        el.innerHTML = '<div class="hc-text"><p class="hero-kicker">' +
          esc(note === "none" ? "Add your TV line in settings to fill this page." : (note || "Loading your line…")) + "</p></div>";
        return;
      }
      var it = data.featured[hi % data.featured.length];
      // two stacked art layers crossfade; the top one drifts (the reference design's slow zoom)
      var under = data.featured[(hi + data.featured.length - 1) % data.featured.length];
      el.innerHTML =
        '<div class="hc-art"><div class="hc-layer" style="background-image:url(&quot;' + esc(under.backdrop) + '&quot;)"></div>' +
        '<div class="hc-layer turn" style="background-image:url(&quot;' + esc(it.backdrop) + '&quot;)"></div></div>' +
        '<div class="hc-text">' +
          '<p class="hero-kicker">Picked for you</p>' +
          '<h1 class="hero-title">' + esc(it.name) + "</h1>" +
          '<p class="hc-meta">' + stars(it.rating) + (it.year ? '<span>' + esc(it.year) + "</span>" : "") +
            (it.genre ? '<span class="hc-genre">' + esc(it.genre) + "</span>" : "") + "</p>" +
          (it.plot ? '<p class="hc-plot">' + esc(it.plot) + "</p>" : "") +
          '<div class="hero-btns"><span class="hbtn' + (focused && sub === "hero" ? " focus" : "") + '">' + ICON_PLAY + "<span>Play</span></span></div>" +
        "</div>" +
        '<div class="hc-dots">' + data.featured.map(function (_, i) {
          return '<span class="hc-dot' + (i === hi ? " on" : "") + '"></span>';
        }).join("") + "</div>";
    }

    function renderProviders(focused) {
      var el = $("home-prov");
      if (!data || !data.providers.length) { el.innerHTML = ""; $("home-prov-title").classList.add("none"); return; }
      $("home-prov-title").classList.remove("none");
      el.innerHTML = data.providers.map(function (p, i) {
        return '<div class="prov' + (focused && sub === "prov" && i === ri ? " focus" : "") + '" style="--brand:' +
          (BRAND_COLORS[p.id] || "#302741") + '"><span>' + esc(p.name) + "</span></div>";
      }).join("");
    }

    function cardHTML(r, it, focus) {
      if (r.id === "cw") {
        var art = it.backdrop || it.poster || "";
        return '<div class="card slim' + focus + '"' + (art ? ' style="background-image:url(&quot;' + esc(art) + '&quot;)"' : "") + ">" +
          '<div class="meta"><span class="name">' + esc(it.title) + "</span></div></div>";
      }
      return '<div class="tvp' + focus + '">' +
        '<div class="tvp-art"' + (it.logo ? ' style="background-image:url(&quot;' + esc(it.logo) + '&quot;)"' : "") + '></div>' +
        '<div class="tvp-meta"><span class="tvp-name">' + esc(it.name) + "</span>" + stars(it.rating) + "</div></div>";
    }

    function renderRows(focused) {
      $("home-rows").innerHTML = rows().map(function (r) {
        var mine = focused && sub === "row:" + r.id;
        return '<h2 class="row-title">' + esc(r.name) + '</h2><div class="rail" id="rail-' + r.id + '">' +
          r.items.slice(0, 15).map(function (it, i) { return cardHTML(r, it, mine && i === ri ? " focus" : ""); }).join("") + "</div>";
      }).join("");
      var f = $("home-rows").getElementsByClassName("focus")[0];
      if (f && f.scrollIntoView) f.scrollIntoView({ block: "center", inline: "center" });
    }

    function render(focused) {
      if (!$("page-home")) return;
      renderHero(focused);
      renderProviders(focused);
      renderRows(focused);
    }

    // ---------------- keys ----------------
    function zoneItems() {
      if (sub === "prov") return data ? data.providers.length : 0;
      if (sub.indexOf("row:") === 0) {
        var r = rows().filter(function (x) { return "row:" + x.id === sub; })[0];
        return r ? Math.min(15, r.items.length) : 0;
      }
      return 1;
    }

    /** Arrows inside the page. Returns "leave" upward to the nav, "pill" downward past the last row. */
    function move(dx, dy) {
      var z = zones(), at = z.indexOf(sub);
      if (at < 0) { sub = z[0]; at = 0; }
      if (dy) {
        var to = at + dy;
        if (to < 0) return "leave";
        if (to >= z.length) return "pill";
        sub = z[to]; ri = 0;
        lastTurn = Date.now();
        render(true);
        return "";
      }
      if (sub === "hero") {
        // left and right turn the carousel by hand
        if (data && data.featured.length) { hi = (hi + dx + data.featured.length) % data.featured.length; lastTurn = Date.now(); renderHero(true); }
        return "";
      }
      ri = Math.max(0, Math.min(zoneItems() - 1, ri + dx));
      // a focus move along a row only swaps two classes; rebuilding every rail is what lags
      var box = sub === "prov" ? $("home-prov") : $("rail-" + sub.slice(4));
      if (box) {
        var old = box.getElementsByClassName("focus")[0];
        if (old) old.classList.remove("focus");
        var next = box.children[ri];
        if (next) {
          next.classList.add("focus");
          if (next.scrollIntoView) next.scrollIntoView({ block: "nearest", inline: "center" });
          return "";
        }
      }
      render(true);
      return "";
    }

    function ok() {
      if (sub === "hero") {
        var it = data && data.featured[hi];
        if (it) api("POST", "/api/tv/play", { url: it.url }).catch(function () {});
        return;
      }
      if (sub === "prov") {
        var p = data && data.providers[ri];
        if (p) ctx.openProvider(p);
        return;
      }
      var r = rows().filter(function (x) { return "row:" + x.id === sub; })[0];
      var item = r && r.items[ri];
      if (!item) return;
      if (r.id === "cw") ctx.openShow(item);
      else if (item.kind === "series") ctx.openSeries(item);
      else api("POST", "/api/tv/play", { url: item.url }).catch(function () {});
    }

    function enter() { sub = "hero"; ri = 0; ensure(); }

    return { render: render, move: move, ok: ok, enter: enter, ensure: ensure,
      refresh: function () { data = null; ensure(); } };
  };
})();
