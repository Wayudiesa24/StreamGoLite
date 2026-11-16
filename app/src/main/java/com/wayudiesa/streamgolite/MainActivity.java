package com.wayudiesa.streamgolite;

import android.app.Activity;
import android.app.AlertDialog;
import androidx.browser.customtabs.CustomTabsIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Independent lightweight YouTube mobile website companion. */
public class MainActivity extends Activity {
    private static final String HOME_URL = "https://m.youtube.com/";
    private static final String SAVED_KEY = "saved";
    private static final String RECENT_KEY = "recent";
    private static final String ACCOUNT_MODE_KEY = "account_mode";
    private static final String SUBSCRIPTIONS_URL = "https://m.youtube.com/feed/subscriptions";
    private static final String LIBRARY_URL = "https://m.youtube.com/feed/you";
    private static final int DISCOVER = 0, SAVED = 1, SETTINGS = 2, BROWSER = 3;
    private SharedPreferences prefs;
    private LinearLayout root;
    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private TextView browserLabel;
    private int page = DISCOVER;
    private int bg, surface, ink, muted, accent, border;
    private boolean dark;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("streamgo", MODE_PRIVATE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (!acceptSharedLink(getIntent())) show();
    }

    private int d(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable shape(int fill, int radius, int line) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(d(radius));
        if (line != Color.TRANSPARENT) g.setStroke(d(1), line);
        return g;
    }

    private TextView action(String value, Runnable fn, boolean filled) {
        TextView t = text(value, 14, true, filled ? Color.WHITE : accent);
        t.setGravity(Gravity.CENTER);
        t.setPadding(d(15), d(13), d(15), d(13));
        t.setBackground(shape(filled ? accent : surface, 15, filled ? Color.TRANSPARENT : border));
        t.setOnClickListener(v -> fn.run());
        return t;
    }

    private void gap(LinearLayout root, int value) {
        root.addView(new View(this), new LinearLayout.LayoutParams(1, d(value)));
    }

    private void colors() {
        dark = prefs.getBoolean("dark", false);
        bg = Color.parseColor(dark ? "#101420" : "#F7F8FC");
        surface = Color.parseColor(dark ? "#1E2635" : "#FFFFFF");
        ink = Color.parseColor(dark ? "#F6F7FB" : "#171D30");
        muted = Color.parseColor(dark ? "#B0BACD" : "#70788D");
        accent = Color.parseColor("#5849E8");
        border = Color.parseColor(dark ? "#344055" : "#E9ECF4");
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
              View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void show() {
        colors();
        root = column();
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(bg);
        setContentView(root);
        switch (page) {
            case DISCOVER: discover(); break;
            case SAVED: saved(); break;
            case SETTINGS: settings(); break;
            default: browser(); break;
        }
    }

    private void go(int screen) {
        if (screen != BROWSER && page == BROWSER && web != null) {
            web.loadUrl("about:blank");
            web.onPause();
        }
        page = screen;
        show();
    }

    private LinearLayout content() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column();
        body.setPadding(d(22), d(21), d(22), d(20));
        scroll.addView(body);
        return body;
    }

    private void heading(LinearLayout body, String title, String subtitle) {
        body.addView(text(title, 23, true, ink));
        if (!subtitle.isEmpty()) {
            gap(body, 5);
            body.addView(text(subtitle, 13, false, muted));
        }
    }

    private void navbar() {
        LinearLayout nav = row();
        nav.setPadding(d(10), d(10), d(10), d(9));
        nav.setBackgroundColor(surface);
        navButton(nav, "⌂", "Discover", DISCOVER);
        navButton(nav, "☆", "Saved", SAVED);
        navButton(nav, "⚙", "Settings", SETTINGS);
        root.addView(nav);
    }

    private void navButton(LinearLayout parent, String symbol, String name, int screen) {
        LinearLayout cell = column();
        cell.setGravity(Gravity.CENTER);
        int color = page == screen ? accent : muted;
        cell.addView(text(symbol, 23, false, color));
        cell.addView(text(name, 11, page == screen, color));
        cell.setOnClickListener(v -> go(screen));
        parent.addView(cell, new LinearLayout.LayoutParams(0, d(54), 1));
    }

    private void discover() {
        LinearLayout body = content();
        LinearLayout brand = row();
        TextView icon = text("▶", 22, true, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(accent, 15, Color.TRANSPARENT));
        brand.addView(icon, new LinearLayout.LayoutParams(d(48), d(48)));
        LinearLayout ident = column();
        ident.setPadding(d(12), 0, 0, 0);
        ident.addView(text("StreamGo", 22, true, ink));
        ident.addView(text("LITE · VIDEO COMPANION", 10, true, muted));
        brand.addView(ident, new LinearLayout.LayoutParams(0, -2, 1));
        brand.addView(action("⚙", () -> go(SETTINGS), false));
        body.addView(brand);
        gap(body, 30);
        body.addView(text("Watch more.\nUse less.", 35, true, ink));
        gap(body, 7);
        body.addView(text("Find the videos you love, without a complicated interface.", 14, false, muted));
        gap(body, 23);

        LinearLayout search = row();
        search.setBackground(shape(surface, 19, border));
        search.setPadding(d(12), d(7), d(7), d(7));
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(15);
        field.setTextColor(ink);
        field.setHintTextColor(muted);
        field.setHint("Search YouTube");
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setBackgroundColor(Color.TRANSPARENT);
        search.addView(field, new LinearLayout.LayoutParams(0, d(48), 1));
        Runnable doSearch = () -> {
            String value = field.getText().toString().trim();
            if (value.isEmpty()) return;
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
            search(value);
        };
        search.addView(action("GO →", doSearch, true));
        field.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                doSearch.run(); return true;
            }
            return false;
        });
        body.addView(search);
        gap(body, 18);
        accountCard(body);
        gap(body, 26);
        heading(body, "Explore topics", "Pick something to enjoy");
        gap(body, 15);
        String[][] topics = {
            {"♫", "Music"}, {"▣", "Learning"},
            {"✦", "Technology"}, {"◎", "Podcasts"},
            {"★", "Ghana"}, {"◈", "Sports"}
        };
        for (int i = 0; i < topics.length; i += 2) {
            LinearLayout items = row();
            for (int j = i; j < i + 2; j++) {
                String topic = topics[j][1];
                LinearLayout card = column();
                card.setPadding(d(17), d(18), d(10), d(18));
                card.setBackground(shape(surface, 19, border));
                card.addView(text(topics[j][0], 24, true, accent));
                gap(card, 11);
                card.addView(text(topic, 16, true, ink));
                card.setOnClickListener(v -> search(topic));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, d(115), 1);
                if (j % 2 == 1) params.leftMargin = d(11);
                items.addView(card, params);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) lp.topMargin = d(11);
            body.addView(items, lp);
        }
        gap(body, 24);
        TextView tip = text("DATA-SAVING TIP\n\nHide thumbnails in Settings. For video quality, use the controls provided by YouTube.", 13, false, ink);
        tip.setPadding(d(20), d(19), d(20), d(19));
        tip.setBackground(shape(dark ? Color.parseColor("#2D2853") : Color.parseColor("#EBE9FF"), 20, Color.TRANSPARENT));
        tip.setOnClickListener(v -> go(SETTINGS));
        body.addView(tip);
        JSONArray recent = json(RECENT_KEY);
        if (recent.length() > 0) {
            gap(body, 24);
            heading(body, "Recent searches", "Jump back in");
            gap(body, 10);
            for (int i = 0; i < Math.min(5, recent.length()); i++) {
                String q = recent.optString(i, "");
                if (q.isEmpty()) continue;
                TextView item = text("↗    " + q, 14, false, ink);
                item.setPadding(d(16), d(13), d(12), d(13));
                item.setBackground(shape(surface, 13, border));
                item.setOnClickListener(v -> search(q));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = d(7);
                body.addView(item, lp);
            }
        }
        navbar();
    }

    private JSONArray json(String key) {
        try { return new JSONArray(prefs.getString(key, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void accountCard(LinearLayout body) {
        LinearLayout card = column();
        card.setPadding(d(18), d(17), d(18), d(18));
        card.setBackground(shape(dark ? Color.parseColor("#29274A") : Color.parseColor("#EEEAFE"), 19, Color.TRANSPARENT));
        card.addView(text("YOUR YOUTUBE ACCOUNT", 11, true, accent));
        gap(card, 7);
        card.addView(text("Sign in safely. Keep your favourites.", 17, true, ink));
        gap(card, 6);
        card.addView(text("Open YouTube through Chrome. If you have Premium, YouTube can recognise your subscription there. Your password stays with Google.", 12, false, muted));
        gap(card, 14);
        card.addView(action("YouTube · sign in", () -> accountTab(HOME_URL), true));
        gap(card, 12);
        LinearLayout quick = row();
        quick.addView(action("Subscriptions", () -> accountTab(SUBSCRIPTIONS_URL), false));
        quick.addView(action("My YouTube", () -> accountTab(LIBRARY_URL), false));
        card.addView(quick);
        body.addView(card);
        gap(body, 9);
        body.addView(text(usesAccountMode() ? "Browsing: signed-in Chrome mode" : "Browsing: lightweight internal mode", 12, false, muted));
    }

    private boolean usesAccountMode() {
        return prefs.getBoolean(ACCOUNT_MODE_KEY, true);
    }

    private void accountTab(String address) {
        if (!isYoutube(address)) return;
        Uri uri = Uri.parse(address);
        try {
            CustomTabsIntent tabs = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(accent)
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                    .build();
            // Chrome Custom Tabs can share Chrome sign-in, without giving StreamGo access to cookies.
            tabs.intent.setPackage("com.android.chrome");
            try {
                tabs.launchUrl(this, uri);
            } catch (ActivityNotFoundException chromeMissing) {
                tabs.intent.setPackage(null);
                tabs.launchUrl(this, uri);
            }
        } catch (Exception unavailable) {
            try {
                Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                browser.setPackage("com.android.chrome");
                startActivity(browser);
            } catch (Exception e) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception ignored) {
                    Toast.makeText(this, "No browser available to sign in", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean acceptSharedLink(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return false;
        if (!"text/plain".equals(intent.getType())) return false;
        String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (shared == null) return false;
        Matcher matcher = Pattern.compile("https://[^\\s<>]+", Pattern.CASE_INSENSITIVE).matcher(shared);
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("[),.!?]+$", "");
            if (isYoutube(candidate)) {
                storeBookmark(candidate, "Shared YouTube link");
                page = SAVED;
                show();
                Toast.makeText(this, "YouTube link saved", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        Toast.makeText(this, "No valid YouTube link was shared", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        acceptSharedLink(intent);
    }

    private void storeBookmark(String address, String title) {
        if (!isYoutube(address)) return;
        JSONArray old = json(SAVED_KEY), next = new JSONArray();
        try {
            JSONObject record = new JSONObject();
            record.put("url", address);
            record.put("title", title == null || title.isEmpty() ? "YouTube link" : title);
            next.put(record);
            for (int i = 0; i < old.length() && next.length() < 80; i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null && !address.equals(item.optString("url"))) next.put(item);
            }
            prefs.edit().putString(SAVED_KEY, next.toString()).apply();
        } catch (JSONException ignored) { }
    }

    private void search(String query) {
        JSONArray old = json(RECENT_KEY);
        JSONArray next = new JSONArray();
        next.put(query);
        for (int i = 0; i < old.length() && next.length() < 10; i++) {
            String item = old.optString(i, "");
            if (!item.isEmpty() && !item.equalsIgnoreCase(query)) next.put(item);
        }
        prefs.edit().putString(RECENT_KEY, next.toString()).apply();
        open("https://m.youtube.com/results?search_query=" + Uri.encode(query));
    }

    private boolean isYoutube(String address) {
        try {
            Uri uri = Uri.parse(address);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("youtube.com") || host.endsWith(".youtube.com") ||
                   host.equals("youtu.be") || host.equals("youtube-nocookie.com") ||
                   host.endsWith(".youtube-nocookie.com");
        } catch (Exception e) { return false; }
    }

    private void open(String address) {
        if (!isYoutube(address)) {
            Toast.makeText(this, "Only secure YouTube links are supported", Toast.LENGTH_LONG).show();
            return;
        }
        if (usesAccountMode()) {
            accountTab(address);
            return;
        }
        page = BROWSER;
        show();
        web.onResume();
        web.loadUrl(address);
    }

    private void ensureWeb() {
        if (web != null) return;
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setLoadsImagesAutomatically(!prefs.getBoolean("hide_images", false));
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String link = r.getUrl().toString();
                if (isYoutube(link)) return false;
                external(link);
                return true;
            }
            @Override public void onPageFinished(WebView v, String url) {
                if (browserLabel != null) browserLabel.setText("YouTube · web player");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) { callback.onCustomViewHidden(); return; }
                customView = view;
                customCallback = callback;
                ViewGroup decor = (ViewGroup)getWindow().getDecorView();
                decor.addView(customView, new ViewGroup.LayoutParams(-1, -1));
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
            @Override public void onHideCustomView() { exitFullscreen(); }
        });
    }

    private void exitFullscreen() {
        if (customView == null) return;
        ((ViewGroup)getWindow().getDecorView()).removeView(customView);
        customView = null;
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
        colors();
    }

    private String current() {
        String url = web == null ? null : web.getUrl();
        return url == null || !isYoutube(url) ? HOME_URL : url;
    }

    private void browser() {
        ensureWeb();
        LinearLayout top = row();
        top.setPadding(d(11), d(9), d(11), d(9));
        top.setBackgroundColor(surface);
        top.addView(action("‹", this::browserBack, false));
        browserLabel = text("YouTube · web player", 13, true, ink);
        browserLabel.setGravity(Gravity.CENTER_VERTICAL);
        browserLabel.setPadding(d(12), 0, d(6), 0);
        top.addView(browserLabel, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(action("☆", this::bookmark, false));
        top.addView(action("Chrome", () -> accountTab(current()), false));
        top.addView(action("↗", () -> external(current()), false));
        root.addView(top);
        if (web.getParent() instanceof ViewGroup) ((ViewGroup)web.getParent()).removeView(web);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout bottom = row();
        bottom.setPadding(d(8), d(8), d(8), d(8));
        bottom.setBackgroundColor(surface);
        bottomItem(bottom, "‹ Back", this::browserBack);
        bottomItem(bottom, "⌂ Home", () -> go(DISCOVER));
        bottomItem(bottom, "↻ Reload", () -> web.reload());
        bottomItem(bottom, "↗ YouTube", () -> external(current()));
        root.addView(bottom);
    }

    private void bottomItem(LinearLayout parent, String title, Runnable handler) {
        TextView t = text(title, 12, true, accent);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(v -> handler.run());
        parent.addView(t, new LinearLayout.LayoutParams(0, d(44), 1));
    }

    private void browserBack() {
        if (customView != null) exitFullscreen();
        else if (web != null && web.canGoBack()) web.goBack();
        else go(DISCOVER);
    }

    private void external(String address) {
        Uri uri;
        try { uri = Uri.parse(address); }
        catch (Exception e) { uri = Uri.parse(HOME_URL); }
        if (!"https".equalsIgnoreCase(uri.getScheme())) uri = Uri.parse(HOME_URL);
        try {
            Intent yt = new Intent(Intent.ACTION_VIEW, uri);
            yt.setPackage("com.google.android.youtube");
            startActivity(yt);
        } catch (ActivityNotFoundException e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
            catch (Exception ignored) {
                Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bookmark() {
        storeBookmark(current(), web == null ? "YouTube link" : web.getTitle());
        Toast.makeText(this, "Saved on your phone", Toast.LENGTH_SHORT).show();
    }

    private void removeBookmark(String address) {
        JSONArray old = json(SAVED_KEY);
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null && !address.equals(item.optString("url"))) next.put(item);
        }
        prefs.edit().putString(SAVED_KEY, next.toString()).apply();
        show();
    }

    private void saved() {
        LinearLayout body = content();
        heading(body, "Saved videos", "Your saved YouTube links stay on this device");
        gap(body, 12);
        body.addView(text("In signed-in mode, tap Share on a YouTube video and choose StreamGo Lite Beta to save its link here.", 12, false, muted));
        gap(body, 21);
        JSONArray list = json(SAVED_KEY);
        if (list.length() == 0) {
            body.addView(text("☆\n\nNothing saved yet.\n\nWhile watching, tap ☆ to save a video link.", 17, false, muted));
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) continue;
            String address = item.optString("url", "");
            if (!isYoutube(address)) continue;
            LinearLayout card = column();
            card.setPadding(d(17), d(16), d(17), d(16));
            card.setBackground(shape(surface, 16, border));
            card.addView(text(item.optString("title", "Saved YouTube page"), 15, true, ink));
            gap(card, 8);
            TextView urlText = text(address, 12, false, muted);
            urlText.setSingleLine(true);
            card.addView(urlText);
            gap(card, 12);
            LinearLayout controls = row();
            controls.addView(action("Open", () -> open(address), true));
            controls.addView(action("Remove", () -> removeBookmark(address), false));
            card.addView(controls);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = d(11);
            body.addView(card, lp);
        }
        navbar();
    }

    private void settingToggle(LinearLayout body, String name, String detail, String key) {
        LinearLayout item = row();
        item.setPadding(d(17), d(16), d(17), d(16));
        item.setBackground(shape(surface, 16, border));
        LinearLayout labels = column();
        labels.addView(text(name, 15, true, ink));
        gap(labels, 4);
        labels.addView(text(detail, 12, false, muted));
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, false));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            if (key.equals("hide_images") && web != null)
                web.getSettings().setLoadsImagesAutomatically(!checked);
            show();
        });
        item.addView(toggle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = d(13);
        body.addView(item, lp);
        item.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
    }

    private void settings() {
        LinearLayout body = content();
        heading(body, "Settings", "Make StreamGo Lite your own");
        gap(body, 25);
        settingToggle(body, "Signed-in Chrome mode", "Recommended: account browsing uses Chrome instead of the internal viewer", ACCOUNT_MODE_KEY);
        settingToggle(body, "Dark mode", "A darker look for evening viewing", "dark");
        settingToggle(body, "Hide thumbnails", "Experimental: reduces browsing images, not video data", "hide_images");
        gap(body, 5);
        body.addView(action("Sign in to YouTube", () -> accountTab(HOME_URL), true));
        gap(body, 10);
        body.addView(action("Subscriptions", () -> accountTab(SUBSCRIPTIONS_URL), false));
        gap(body, 10);
        body.addView(action("My YouTube library", () -> accountTab(LIBRARY_URL), false));
        gap(body, 12);
        body.addView(text("Chrome tabs share Chrome's YouTube sign-in, never the internal WebView. Premium benefits require an active subscription. Use the official YouTube app for background play and offline downloads.", 12, false, muted));
        gap(body, 14);
        body.addView(action("Clear recent searches", () -> {
            prefs.edit().remove(RECENT_KEY).apply();
            Toast.makeText(this, "Search history cleared", Toast.LENGTH_SHORT).show();
        }, false));
        gap(body, 12);
        body.addView(action("Open official YouTube", () -> external(HOME_URL), true));
        gap(body, 24);
        body.addView(text("StreamGo Lite Beta 2.0 is an independent companion, not affiliated with Google or YouTube. StreamGo cannot read your Google password or Chrome cookies.", 13, false, muted));
        navbar();
    }

    @Override public void onBackPressed() {
        if (page == BROWSER) browserBack();
        else if (page != DISCOVER) go(DISCOVER);
        else super.onBackPressed();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
    }

    @Override protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null && page == BROWSER) web.onResume();
    }

    @Override protected void onDestroy() {
        if (customView != null) exitFullscreen();
        if (web != null) {
            if (web.getParent() instanceof ViewGroup) ((ViewGroup)web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
