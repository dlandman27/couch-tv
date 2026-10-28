// The Apps page of the Orbit look: every app on the TV in the saved order, names under the art, a
// monogram while an app has no banner, and hold OK to pick one up and move it. The order is the
// same one Night's dock and the phone's Arrange apps use (/api/app-order), kept on the TV when
// there is no bridge. orbit.js owns the tabs and the keys and calls in through ORBIT_APPS(ctx).
(function () {
  "use strict";

  // ctx: $, esc, api, kept, keep, banner, launch, TV (the TVOS object or null)
  window.ORBIT_APPS = function (ctx) {
    var $ = ctx.$, esc = ctx.esc, api = ctx.api, kept = ctx.kept, keep = ctx.keep;

    var COLS = 6;         // the grid
    // Never shown: home screens (this page is the home) and apps a tile already covers.
    var HIDDEN = [
      "com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "app.couchtv.launcher",
      "com.limelight", "com.google.android.play.games", "com.google.android.videos"
    ];
    // The TV's own tools close the grid and stay put; /api/app-order never lists them.
    var PINNED = ["com.android.vending", "com.android.tv.settings"];
    // Before anyone arranges them: the order the Night look starts with.
    var APP_ORDER = [
      "com.google.android.youtube.tv", "com.netflix.ninja", "au.net.abc.iview", "com.disney.disneyplus",
      "au.com.stan.and", "au.com.streamotion.ares.tv", "com.amazon.amazonvideo.livingroom",
      "com.sbs.ondemand.tv", "com.apple.atve.androidtv.appletv", "com.cbs.ca", "au.com.ninenow.ctv",
      "com.swm.live", "au.com.tenplay", "au.com.kayosports.tv", "au.com.foxtel.atv", "com.wbd.stream",
      "com.spotify.tv.android", "com.google.android.youtube.tvmusic"
    ];
    var APP_LAST = ["com.mediatek.wwtv.tvcenter"];

    var apps = [], ai = 0, focused = false;
    var where = "grid";                           // focus within the page: "grid" | "head" (the sort chip)
    var sortMode = kept("orbit.apps.sort") === "az" ? "az" : "custom";   // "custom" = the saved order
    var ORDER = { line: 10, packages: null };     // the saved arrangement
    var lift = null;                              // the app picked up to move: its package
    var liftWas = null;                           // the order to go back to if Back puts it down
    var newApps = kept("tvhome.apps.new") || [];  // installed since this TV first listed its apps
    var hiddenTV = [];                            // hidden with Home settings (settings.hidden_apps)
    var gridEls = [], builtKey = "", scrollRow = 0;
    var hintFlash = "", hintTimer = null;

    function shown(a) { return a.package && HIDDEN.indexOf(a.package) < 0 && hiddenTV.indexOf(a.package) < 0; }
    function orderable(a) { return PINNED.indexOf(a.package) < 0; }

    function defaultRank(p) {
      var i = APP_ORDER.indexOf(p);
      if (i >= 0) return i;
      i = APP_LAST.indexOf(p);
      return i >= 0 ? 2000 + i : 1000;
    }

    // The saved order first, apps it has never heard of after, the TV's own tools at the end.
    function sortApps() {
      var at = null;
      if (ORDER.packages) { at = {}; ORDER.packages.forEach(function (p, i) { at[p] = i; }); }
      var rank = function (a) {
        if (at && at[a.package] !== undefined) return at[a.package];
        return (at ? 5000 : 0) + defaultRank(a.package);
      };
      var movable = apps.filter(orderable).sort(function (a, b) { return rank(a) - rank(b) || a.name.localeCompare(b.name); });
      var pinned = apps.filter(function (a) { return !orderable(a); })
        .sort(function (a, b) { return PINNED.indexOf(a.package) - PINNED.indexOf(b.package); });
      apps = movable.concat(pinned);
    }

    // The view order: the saved custom order, or A–Z (the TV's own tools stay pinned at the end
    // either way). Toggling never changes the saved order; it is only how the grid is shown.
    function applySort() {
      if (sortMode === "az") {
        var movable = apps.filter(orderable).sort(function (a, b) { return a.name.localeCompare(b.name); });
        apps = movable.concat(apps.filter(function (a) { return !orderable(a); }));
      } else {
        sortApps();
      }
      builtKey = "";
    }
    function toggleSort() {
      sortMode = sortMode === "az" ? "custom" : "az";
      keep("orbit.apps.sort", sortMode);
      applySort();
      ai = 0; scrollRow = 0;
      render(focused);
    }

    // The order lives with the bridge (/api/app-order, the one the phone's Arrange apps edits); an
    // order made while the computer was off is kept on the TV and handed over when it answers again.
    function loadOrder() {
      var lo = kept("tvhome.order.tv");
      if (lo && lo.packages) ORDER = { line: lo.line, packages: lo.packages };
      if (lo && lo.pending) {
        return api("POST", "/api/app-order", { order: lo.packages, line: lo.line })
          .then(function () { lo.pending = false; keep("tvhome.order.tv", lo); }, function () {});
      }
      return api("GET", "/api/app-order").then(function (j) {
        if (j.saved) ORDER = { line: j.line, packages: (j.apps || []).map(function (a) { return a.package; }) };
      }, function () {});
    }

    function persistOrder() {
      ORDER.packages = apps.filter(orderable).map(function (a) { return a.package; });
      ORDER.line = Math.max(1, Math.min(10, ORDER.line || 10));
      var local = { line: ORDER.line, packages: ORDER.packages, pending: true };
      keep("tvhome.order.tv", local);
      api("POST", "/api/app-order", { order: ORDER.packages, line: ORDER.line })
        .then(function () { local.pending = false; keep("tvhome.order.tv", local); }, function () {});
    }

    // Apps installed since this TV first listed them wear a dot until opened once (Night's dot too).
    function detectNew() {
      if (!apps.length) return;
      var pk = apps.filter(orderable).map(function (a) { return a.package; });
      var seen = kept("tvhome.apps.seen");
      if (!seen) { keep("tvhome.apps.seen", pk); return; }
      var fresh = pk.filter(function (p) { return seen.indexOf(p) < 0; });
      if (!fresh.length) return;
      keep("tvhome.apps.seen", seen.concat(fresh));
      newApps = newApps.concat(fresh.filter(function (p) { return newApps.indexOf(p) < 0; }));
      keep("tvhome.apps.new", newApps);
    }
    function openedOnce(pkg) {
      if (newApps.indexOf(pkg) < 0) return;
      newApps = newApps.filter(function (p) { return p !== pkg; });
      keep("tvhome.apps.new", newApps);
      builtKey = "";   // the dot has to go
    }

    // ---------------- drawing ----------------
    // a steady colour per app, behind the banner and for the monogram while there is none
    function monoStyle(name) {
      var h = 0;
      for (var i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) % 360;
      return "background:linear-gradient(135deg,hsl(" + h + ",42%,34%),hsl(" + ((h + 40) % 360) + ",48%,20%))";
    }

    function appTile(a) {
      var src = ctx.banner(a.package);
      var logo = !ctx.TV && window.MOCK_ICON && src;   // dev previews: a square logo, not a wide banner
      return '<div class="appc" aria-label="' + esc(a.name) + '">' +
        '<div class="art" style="' + monoStyle(a.name) + '">' +
        '<span class="mono">' + esc((a.name || "?").charAt(0).toUpperCase()) + "</span>" +
        (src ? '<img' + (logo ? ' class="logo"' : "") + ' src="' + esc(src) + '" alt="" decoding="async" onerror="this.className=\'bad\'">' : "") +
        "</div>" +
        (newApps.indexOf(a.package) >= 0 ? '<i class="dot" aria-label="New"></i>' : "") +
        '<div class="alabel">' + esc(a.name) + "</div></div>";
    }

    function appsKey() { return apps.map(function (a) { return a.package; }).join(",") + "|" + newApps.join(","); }

    function render(focus) {
      focused = focus;
      var key = appsKey();
      if (key !== builtKey) {
        builtKey = key;
        $("app-grid").innerHTML = apps.length ? apps.map(appTile).join("")
          : '<p class="apps-empty">No apps to show yet.</p>';
        gridEls = [].slice.call($("app-grid").getElementsByClassName("appc"));
        $("apps-count").textContent = apps.length ? String(apps.length) : "";
      }
      paint();
    }

    function paint() {
      for (var i = 0; i < gridEls.length; i++) {
        gridEls[i].className = "appc" +
          (focused && where === "grid" && i === ai && !lift ? " focus" : "") +
          (lift && apps[i] && apps[i].package === lift ? " lift" : "");
      }
      var sort = $("apps-sort");
      if (sort) {
        sort.textContent = sortMode === "az" ? "A–Z" : "Custom";
        sort.className = "apps-sort" + (focused && where === "head" ? " focus" : "");
      }
      follow();
      hint();
    }

    function hint() {
      $("apps-hint").textContent = hintFlash ||
        (lift ? "Move with the arrows · OK drops it · Back puts it back" :
         where === "head" ? "OK switches between your order and A–Z" :
         sortMode === "az" ? "Sorted A–Z · switch to Custom up top to move apps" :
         "Hold OK to move an app");
    }
    function flash(text) {
      clearTimeout(hintTimer);
      hintFlash = text;
      hint();
      hintTimer = setTimeout(function () { hintFlash = ""; hint(); }, 3500);
    }

    // The grid slides under its window so the focused row is always whole and never clipped.
    function follow() {
      var view = $("apps-view"), grid = $("app-grid");
      if (!gridEls.length) {
        grid.style.transform = "";
        view.className = "apps-view";
        return;
      }
      var rows = Math.ceil(gridEls.length / COLS);
      var stride = rows > 1 ? gridEls[COLS].offsetTop - gridEls[0].offsetTop : (gridEls[0].offsetHeight || 1);
      var seen = Math.max(1, Math.floor((view.clientHeight - gridEls[0].offsetHeight) / stride) + 1);
      var row = Math.floor(ai / COLS);
      if (row < scrollRow) scrollRow = row;
      if (row > scrollRow + seen - 1) scrollRow = row - seen + 1;
      scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, rows - seen)));
      grid.style.transform = scrollRow ? "translateY(" + (-scrollRow * stride) + "px)" : "";
      view.className = "apps-view" + (scrollRow > 0 ? " fade-top" : "") + (scrollRow < rows - seen ? " fade-bot" : "");
    }

    // A move slides every tile between the old and the new spot (record, reorder, replay).
    function slide(mutate) {
      gridEls.forEach(function (el) { el._x = el.offsetLeft; el._y = el.offsetTop; });
      mutate();
      gridEls.forEach(function (el) {
        var dx = el._x - el.offsetLeft, dy = el._y - el.offsetTop;
        el.style.transition = "none";
        el.style.transform = dx || dy ? "translate(" + dx + "px," + dy + "px)" : "";
      });
      void $("app-grid").offsetWidth;   // let the browser see them in the old spots
      gridEls.forEach(function (el) { el.style.transition = ""; el.style.transform = ""; });
    }

    function moveTo(to) {
      var from = ai, max = apps.filter(orderable).length - 1;
      if (to < 0 || to > max || to === from) return;
      slide(function () {
        apps.splice(to, 0, apps.splice(from, 1)[0]);
        gridEls.splice(to, 0, gridEls.splice(from, 1)[0]);
        var grid = $("app-grid");
        gridEls.forEach(function (el) { grid.appendChild(el); });
      });
      ai = to;
      builtKey = appsKey();
    }

    // ---------------- what orbit.js calls ----------------
    /** Arrows inside the page. "leave": focus walks off the top, back to the tabs. */
    function move(dx, dy) {
      if (where === "head") {              // on the sort chip above the grid
        if (dy > 0) where = "grid";        // down, back into the apps
        else if (dy < 0) return "leave";   // up, out to the tabs
        return;
      }
      var row = Math.floor(ai / COLS);
      if (lift) {
        // the picked-up app rides the arrows; left and right cross rows, up and down keep the column
        if (dx) moveTo(ai + dx);
        else if (dy) {
          var at = ai + dy * COLS, last = apps.filter(orderable).length - 1;
          if (dy > 0 && at > last && row < Math.floor(last / COLS)) at = last;
          moveTo(at);
        }
        return;
      }
      if (dy < 0 && row === 0) { where = "head"; return; }   // up from the top row: the sort chip
      if (dx) { var nx = ai + dx; if (nx >= 0 && nx < apps.length && Math.floor(nx / COLS) === row) ai = nx; }
      else if (dy) {
        var ny = ai + dy * COLS;
        if (dy > 0 && ny >= apps.length && row < Math.floor((apps.length - 1) / COLS)) ny = apps.length - 1;
        if (ny >= 0 && ny < apps.length) ai = ny;
      }
    }

    /** OK held: pick the focused app up. True when the hold meant something here. */
    function hold() {
      if (where === "head") return true;   // nothing to pick up on the sort chip
      if (sortMode === "az") { flash("Switch to Custom up top to move apps."); return true; }
      var a = apps[ai];
      if (!a) return false;
      if (!orderable(a)) { flash(a.name + " stays put."); return true; }
      lift = a.package;
      liftWas = apps.slice();
      paint();
      return true;
    }

    /** OK let go: flip the sort, drop the moved app, or open the focused one. */
    function ok() {
      if (where === "head") { toggleSort(); return; }
      if (lift) {
        lift = null; liftWas = null;
        persistOrder();
        paint();
        flash("Saved.");
        return;
      }
      var a = apps[ai];
      if (a) { openedOnce(a.package); ctx.launch(a.package); }
    }

    /** Back while an app is in hand: put everything back the way it was. */
    function cancel() {
      if (!lift) return false;
      var pkg = lift;
      lift = null;
      if (liftWas) { apps = liftWas; liftWas = null; builtKey = ""; }
      ai = Math.max(0, apps.map(function (a) { return a.package; }).indexOf(pkg));
      render(focused);
      flash("Put back.");
      return true;
    }

    /** The saved order and the TV's apps; resolves once both answered (or failed quietly). */
    function load() {
      var done = function () { applySort(); detectNew(); builtKey = ""; };
      return loadOrder().then(function () {
        if (window.MOCK_APPS) {
          apps = window.MOCK_APPS.map(function (a) { return { name: a.name, package: a.package }; }).filter(shown);
          return done();
        }
        if (ctx.TV) {
          try {
            apps = JSON.parse(ctx.TV.apps()).map(function (a) { return { name: a.label || a.package, package: a.package }; }).filter(shown);
            return done();
          } catch (e) {}
        }
        return api("GET", "/api/apps").then(function (j) {
          apps = (j.apps || []).filter(function (a) { return a.installed !== false && a.id !== "home"; })
            .map(function (a) { return { name: a.name || a.package, package: a.package }; }).filter(shown);
          done();
        }, function () {});
      });
    }

    return {
      load: load,
      render: render,
      move: move,
      hold: hold,
      ok: ok,
      cancel: cancel,
      lifting: function () { return !!lift; },
      top4: function () { return apps.slice(0, 4); },
      focusFirst: function () { ai = 0; scrollRow = 0; where = "grid"; },
      hidePackages: function (list) {
        (Array.isArray(list) ? list : []).forEach(function (p) {
          if (typeof p === "string" && HIDDEN.indexOf(p) < 0) HIDDEN.push(p);
        });
      },
      setHidden: function (list) {
        hiddenTV = (Array.isArray(list) ? list : []).filter(function (p) { return typeof p === "string"; });
      }
    };
  };
})();
