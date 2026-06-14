package net.buli.w.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.net.URL;

public class NotificationsActivity extends Activity {
    private LinearLayout listContainer;
    private SharedPreferences prefs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private String currentFilter = "all";
    private LinearLayout tabBar;
    private final int pageSize = 50;
    private int currentPage = 1;
    private boolean isDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Notifications");
        prefs = getSharedPreferences("notif", 0);
        isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(isDark ? 0xFF121212 : 0xFFFFFFFF);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        String[] tabs = {"All", "Received", "Sent", "Peer", "Sync", "Mempool"};
        for (String t : tabs) {
            Button btn = new Button(this);
            btn.setText(t);
            btn.setAllCaps(false);
            btn.setTag(t.toLowerCase());
            btn.setOnClickListener(v -> {
                currentFilter = (String) v.getTag();
                currentPage = 1;
                updateTabs();
                loadList();
            });
            tabBar.addView(btn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(tabBar, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        updateTabs();

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll);

        loadList();
        setContentView(root);
    }

    private void updateTabs() {
        int activeBg = 0xFF3F51B5;
        int inactiveText = isDark ? 0xFFCCCCCC : 0xFF333333;
        for (int i = 0; i < tabBar.getChildCount(); i++) {
            Button b = (Button) tabBar.getChildAt(i);
            boolean sel = b.getTag().equals(currentFilter);
            b.setBackgroundColor(sel ? activeBg : Color.TRANSPARENT);
            b.setTextColor(sel ? Color.WHITE : inactiveText);
        }
    }

    private void loadList() {
        listContainer.removeAllViews();
        int readColor = isDark ? 0xFFAAAAAA : 0xFF666666;

        if (currentFilter.equals("mempool")) {
            addMempoolTitle("Đang tải mempool.space...");
            new Thread(() -> {
                try {
                    String fees = new Scanner(new URL("https://mempool.space/api/v1/fees/recommended").openStream()).useDelimiter("\\A").next();
                    String mempool = new Scanner(new URL("https://mempool.space/api/mempool").openStream()).useDelimiter("\\A").next();
                    String blocks = new Scanner(new URL("https://mempool.space/api/v1/blocks").openStream()).useDelimiter("\\A").next();
                    String ln = new Scanner(new URL("https://mempool.space/api/v1/lightning/statistics/latest").openStream()).useDelimiter("\\A").next();
                    String acc = new Scanner(new URL("https://mempool.space/api/v1/accelerator/stats").openStream()).useDelimiter("\\A").next();

                    JSONObject f = new JSONObject(fees);
                    JSONObject m = new JSONObject(mempool);
                    JSONArray b = new JSONArray(blocks);
                    JSONObject l = new JSONObject(ln);
                    JSONObject a = new JSONObject(acc);

                    runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        addMempoolTitle("Fees");
                        addMempoolRow("Fastest", f.optInt("fastestFee") + " sat/vB");
                        addMempoolRow("Half hour", f.optInt("halfHourFee") + " sat/vB");
                        addMempoolRow("Hour", f.optInt("hourFee") + " sat/vB");
                        addMempoolRow("Minimum", f.optInt("minimumFee") + " sat/vB");
                        addMempoolRow("Economy", f.optInt("economyFee") + " sat/vB");

                        addMempoolTitle("Mempool");
                        addMempoolRow("Tx count", String.format("%,d", m.optInt("count")));
                        addMempoolRow("vSize", String.format("%.2f MB", m.optInt("vsize") / 1e6));
                        addMempoolRow("Total fee", String.format("%.3f BTC", m.optLong("total_fee") / 1e8));

                        addMempoolTitle("Blocks");
                        for (int i = 0; i < Math.min(5, b.length()); i++) {
                            JSONObject blk = b.optJSONObject(i);
                            addMempoolRow(""+blk.optInt("height"), blk.optInt("tx_count")+" tx • "+String.format("%.2f MB", blk.optDouble("size")/1e6));
                        }

                        addMempoolTitle("Lightning");
                        JSONObject latest = l.optJSONObject("latest");
                        if (latest != null) {
                            addMempoolRow("Capacity", String.format("%.2f BTC", latest.optLong("total_capacity")/1e8));
                            addMempoolRow("Nodes", String.format("%,d", latest.optInt("node_count")));
                            addMempoolRow("Channels", String.format("%,d", latest.optInt("channel_count")));
                        }

                        addMempoolTitle("Accelerator");
                        addMempoolRow("Pending", String.valueOf(a.optInt("pending")));
                        addMempoolRow("Accelerated 24h", String.valueOf(a.optInt("accelerated_count")));
                        addMempoolRow("Total bid", String.format("%.4f BTC", a.optLong("total_bid_boost")/1e8));

                        Button refresh = new Button(this);
                        refresh.setText("Refresh");
                        refresh.setAllCaps(false);
                        refresh.setOnClickListener(v -> loadList());
                        listContainer.addView(refresh);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        addMempoolRow("Lỗi", e.getMessage());
                    });
                }
            }).start();
            return;
        }

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        Button markAll = new Button(this);
        markAll.setText("Mark all as read");
        markAll.setAllCaps(false);
        markAll.setOnClickListener(v -> {
            int count = 0;
            SharedPreferences.Editor ed = prefs.edit();
            for (String k : prefs.getAll().keySet()) {
                if (!k.startsWith("n_")) continue;
                try {
                    JSONObject o = new JSONObject(prefs.getString(k, ""));
                    if (matchesFilter(o) && !o.optBoolean("read", false)) {
                        o.put("read", true);
                        ed.putString(k, o.toString());
                        count++;
                    }
                } catch (Exception ignored) {}
            }
            ed.apply();
            Toast.makeText(this, "Marked " + count + " as read", Toast.LENGTH_SHORT).show();
            loadList();
        });

        Button deleteAll = new Button(this);
        deleteAll.setText("Delete all");
        deleteAll.setAllCaps(false);
        deleteAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete all?")
                .setMessage("Delete all " + currentFilter + " notifications?")
                .setPositiveButton("Delete", (d,w) -> {
                    SharedPreferences.Editor ed = prefs.edit();
                    int del = 0;
                    for (String k : new ArrayList<>(prefs.getAll().keySet())) {
                        if (!k.startsWith("n_")) continue;
                        try {
                            JSONObject o = new JSONObject(prefs.getString(k, ""));
                            if (matchesFilter(o)) { ed.remove(k); del++; }
                        } catch (Exception ignored) {}
                    }
                    ed.apply();
                    Toast.makeText(this, "Deleted " + del, Toast.LENGTH_SHORT).show();
                    loadList();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        topBar.addView(markAll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topBar.addView(deleteAll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        listContainer.addView(topBar);

        List<JSONObject> items = new ArrayList<>();
        List<String> keys = new ArrayList<>(prefs.getAll().keySet());
        Collections.sort(keys, Collections.reverseOrder());
        for (String k : keys) {
            if (!k.startsWith("n_")) continue;
            try {
                JSONObject o = new JSONObject(prefs.getString(k, ""));
                o.put("_key", k);
                if (matchesFilter(o)) items.add(o);
            } catch (Exception ignored) {}
        }

        int end = Math.min(currentPage * pageSize, items.size());
        for (int i = 0; i < end; i++) {
            JSONObject o = items.get(i);
            try {
                String title = o.optString("title", "");
                String type = o.optString("type", "");
                if (type.isEmpty()) {
                    String lt = title.toLowerCase();
                    type = lt.contains("received") ? "received" : lt.contains("sent") ? "sent" : lt.contains("peer") ? "peer" : "sync";
                }
                boolean isRead = o.optBoolean("read", false);
                long time = o.optLong("time", System.currentTimeMillis());

                TextView tv = new TextView(this);
                tv.setTextSize(16);
                tv.setPadding(32, 28, 32, 28);
                tv.setText("● " + title + "\n" + dateFormat.format(new Date(time)));
                int color = title.toLowerCase().contains("received") ? (isDark ? 0xFF81C784 : 0xFF2E7D32)
                        : title.toLowerCase().contains("sent") ? (isDark ? 0xFFE57373 : 0xFFC62828)
                        : title.toLowerCase().contains("peer") ? (isDark ? 0xFF64B5F6 : 0xFF1565C0)
                        : (isDark ? 0xFFFFD54F : 0xFFF9A825);
                tv.setTextColor(isRead ? readColor : color);

                final JSONObject obj = o;
                final String finalType = type;
                tv.setOnClickListener(v -> {
                    try {
                        if (!obj.optBoolean("read", false)) {
                            obj.put("read", true);
                            prefs.edit().putString(obj.getString("_key"), obj.toString()).apply();
                            tv.setTextColor(readColor);
                        }
                    } catch (Exception ignored) {}
                    showDetail(obj, finalType);
                });
                listContainer.addView(tv);
            } catch (Exception ignored) {}
        }

        if (end < items.size()) {
            Button more = new Button(this);
            more.setText("Load more (" + (items.size() - end) + ")");
            more.setAllCaps(false);
            more.setOnClickListener(v -> { currentPage++; loadList(); });
            listContainer.addView(more);
        }

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No notifications");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 300, 0, 0);
            empty.setTextColor(readColor);
            listContainer.addView(empty);
        }
    }

    private void addMempoolTitle(String t) {
        TextView tv = new TextView(this);
        tv.setText("● " + t);
        tv.setTextSize(16);
        tv.setPadding(32, 32, 32, 8);
        tv.setTextColor(isDark ? 0xFFFFFFFF : 0xFF000000);
        listContainer.addView(tv);
    }
    private void addMempoolRow(String k, String v) {
        TextView tv = new TextView(this);
        tv.setText(k + ": " + v);
        tv.setTextSize(14);
        tv.setPadding(48, 8, 32, 8);
        tv.setTextColor(isDark ? 0xFFCCCCCC : 0xFF333333);
        listContainer.addView(tv);
    }

    private boolean matchesFilter(JSONObject o) {
        if (currentFilter.equals("all")) return true;
        String t = o.optString("title", "").toLowerCase();
        String type = o.optString("type", "").toLowerCase();
        return t.contains(currentFilter) || type.contains(currentFilter);
    }

    private void showDetail(JSONObject o, String type) {
        try {
            String extraRaw = o.optString("extra", "");
            JSONObject extra;
            try { extra = new JSONObject(extraRaw); } 
            catch (Exception e) { extra = new JSONObject(); extra.put("raw", extraRaw); }
            
            StringBuilder sb = new StringBuilder();
            String time = dateFormat.format(new Date(o.optLong("time")));

            if ("received".equals(type)) {
                sb.append("Type: Received\n");
                sb.append("Amount: ").append(o.optString("text")).append("\n");
                sb.append("From: ").append(extra.optString("address", extra.optString("raw","N/A"))).append("\n");
                if (extra.has("txid")) sb.append("TxID: ").append(extra.optString("txid")).append("\n");
                sb.append("Time: ").append(time);
            } else if ("sent".equals(type)) {
                sb.append("Type: Sent\n");
                sb.append("Amount: ").append(o.optString("text")).append("\n");
                sb.append("To: ").append(extra.optString("to", extra.optString("raw","N/A"))).append("\n");
                if (extra.has("txid")) sb.append("TxID: ").append(extra.optString("txid")).append("\n");
                if (extra.has("fee")) sb.append("Fee: ").append(extra.optString("fee")).append(" BTC\n");
                sb.append("Time: ").append(time);
            } else if ("peer".equals(type)) {
                sb.append("Type: Peer\n");
                sb.append("Address: ").append(o.optString("text")).append("\n");
                if (extra.has("height")) sb.append("Height: ").append(extra.optString("height")).append("\n");
                if (extra.has("peers")) sb.append("Peers: ").append(extra.optString("peers")).append("\n");
                sb.append(extra.optString("raw","")).append("\n");
                sb.append("Time: ").append(time);
            } else {
                sb.append("Type: Sync\n");
                sb.append(o.optString("text")).append("\n");
                if (extra.has("progress")) sb.append("Progress: ").append(extra.optString("progress")).append("%\n");
                if (extra.has("blocks")) sb.append("Blocks: ").append(extra.optString("blocks")).append("\n");
                sb.append(extra.optString("raw","")).append("\n");
                sb.append("Time: ").append(time);
            }

            String detail = sb.toString();
            String txid = extra.optString("txid", "");

            new AlertDialog.Builder(this)
                    .setTitle(o.optString("title"))
                    .setMessage(detail)
                    .setPositiveButton("Close", null)
                    .setNeutralButton("Copy", (d, w) -> {
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("notif", txid.isEmpty() ? detail : txid));
                        Toast.makeText(this, txid.isEmpty() ? "Copied" : "TxID copied", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Share", (d, w) -> {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_TEXT, detail);
                        startActivity(Intent.createChooser(i, "Share"));
                    })
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
