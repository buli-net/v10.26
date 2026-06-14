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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class NotificationsActivity extends Activity {

    private LinearLayout listContainer;
    private SharedPreferences prefs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    );
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
        
        isDark = (getResources().getConfiguration().uiMode 
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(isDark ? 0xFF121212 : 0xFFFFFFFF);

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        
        String[] tabs = {
            "All",
            "Received",
            "Sent",
            "Peer",
            "Sync",
            "Mempool"
        };
        
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
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            );
            
            tabBar.addView(btn, lp);
        }
        
        root.addView(
            tabBar,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );
        
        updateTabs();

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        scroll.setLayoutParams(scrollLp);
        
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

    private JSONObject getJson(String url) {
        try {
            String data = new Scanner(new URL(url).openStream())
                .useDelimiter("\\A")
                .next();
            return new JSONObject(data);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray getJsonArray(String url) {
        try {
            String data = new Scanner(new URL(url).openStream())
                .useDelimiter("\\A")
                .next();
            return new JSONArray(data);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadList() {
        listContainer.removeAllViews();
        
        int readColor = isDark ? 0xFFAAAAAA : 0xFF666666;

        if (currentFilter.equals("mempool")) {
            addMempoolTitle("Đang tải thống kê BTC...");
            
            new Thread(() -> {
                JSONObject fees = getJson(
                    "https://mempool.space/api/v1/fees/recommended"
                );
                JSONObject mempool = getJson(
                    "https://mempool.space/api/mempool"
                );
                JSONArray blocks = getJsonArray(
                    "https://mempool.space/api/v1/blocks"
                );
                JSONObject price = getJson(
                    "https://mempool.space/api/v1/prices"
                );
                JSONObject diff = getJson(
                    "https://mempool.space/api/v1/difficulty-adjustment"
                );
                JSONObject hash = getJson(
                    "https://mempool.space/api/v1/mining/hashrate/3d"
                );
                JSONObject ln = getJson(
                    "https://mempool.space/api/v1/lightning/statistics/latest"
                );

                int height = 0;
                try {
                    String h = new Scanner(
                        new URL("https://mempool.space/api/blocks/tip/height")
                            .openStream()
                    ).useDelimiter("\\A").next().trim();
                    height = Integer.parseInt(h);
                } catch (Exception ignored) {}

                double totalSupply = 21000000;
                int halvings = height / 210000;
                double reward = 50.0 / Math.pow(2, halvings);
                
                double btcMined = 0;
                for (int i = 0; i < halvings; i++) {
                    btcMined += 210000 * (50.0 / Math.pow(2, i));
                }
                btcMined += (height % 210000) * reward;
                
                double percentMined = btcMined / totalSupply * 100;
                int nextHalving = (halvings + 1) * 210000;
                int blocksToHalving = nextHalving - height;

                int finalHeight = height;
                
                runOnUiThread(() -> {
                    listContainer.removeAllViews();

                    // Bitcoin Network
                    addMempoolTitle("Bitcoin Network");
                    addMempoolRow(
                        "Block hiện tại",
                        String.format("%,d", finalHeight)
                    );
                    addMempoolRow(
                        "Block đang đào",
                        String.format("%,d", finalHeight + 1)
                    );
                    addMempoolRow(
                        "BTC đã khai thác",
                        String.format("%,.2f", btcMined)
                    );
                    addMempoolRow(
                        "Tổng cung tối đa",
                        "21,000,000"
                    );
                    addMempoolRow(
                        "BTC còn lại",
                        String.format("%,.2f", totalSupply - btcMined)
                    );
                    addMempoolRow(
                        "Đã đào",
                        String.format("%.2f%%", percentMined)
                    );

                    // Halving
                    addMempoolTitle("Halving");
                    addMempoolRow(
                        "Số lần halving",
                        String.valueOf(halvings)
                    );
                    addMempoolRow(
                        "Phần thưởng block",
                        reward + " BTC"
                    );
                    addMempoolRow(
                        "Halving tiếp theo",
                        "Block " + String.format("%,d", nextHalving)
                    );
                    addMempoolRow(
                        "Còn lại",
                        blocksToHalving + " blocks (~" 
                        + (blocksToHalving / 144) + " ngày)"
                    );

                    // Giá
                    if (price != null) {
                        addMempoolTitle("Giá BTC");
                        addMempoolRow(
                            "USD",
                            "$" + String.format("%,.0f", price.optDouble("USD"))
                        );
                        addMempoolRow(
                            "EUR",
                            "€" + String.format("%,.0f", price.optDouble("EUR"))
                        );
                        addMempoolRow(
                            "VND",
                            String.format("%,.0f", price.optDouble("USD") * 25400) + " đ"
                        );
                    }

                    // Phí
                    if (fees != null) {
                        addMempoolTitle("Phí giao dịch");
                        addMempoolRow(
                            "Nhanh nhất",
                            fees.optInt("fastestFee") + " sat/vB"
                        );
                        addMempoolRow(
                            "30 phút",
                            fees.optInt("halfHourFee") + " sat/vB"
                        );
                        addMempoolRow(
                            "1 giờ",
                            fees.optInt("hourFee") + " sat/vB"
                        );
                        addMempoolRow(
                            "Tiết kiệm",
                            fees.optInt("economyFee") + " sat/vB"
                        );
                        addMempoolRow(
                            "Tối thiểu",
                            fees.optInt("minimumFee") + " sat/vB"
                        );
                    }

                    // Mempool
                    if (mempool != null) {
                        addMempoolTitle("Mempool");
                        addMempoolRow(
                            "Giao dịch chờ",
                            String.format("%,d", mempool.optInt("count"))
                        );
                        addMempoolRow(
                            "Dung lượng",
                            String.format("%.2f MB", mempool.optInt("vsize") / 1e6)
                        );
                        addMempoolRow(
                            "Phí tổng",
                            String.format("%.3f BTC", mempool.optLong("total_fee") / 1e8)
                        );
                    }

                    // Block
                    if (blocks != null && blocks.length() > 0) {
                        JSONObject b0 = blocks.optJSONObject(0);
                        addMempoolTitle("Block mới nhất");
                        addMempoolRow("Height", String.valueOf(b0.optInt("height")));
                        addMempoolRow("Số TX", String.valueOf(b0.optInt("tx_count")));
                        addMempoolRow(
                            "Kích thước",
                            String.format("%.2f MB", b0.optDouble("size") / 1e6)
                        );
                    }

                    // Mining
                    if (diff != null) {
                        addMempoolTitle("Khai thác");
                        addMempoolRow(
                            "Difficulty",
                            String.format("%.2f T", diff.optDouble("currentDifficulty") / 1e12)
                        );
                        addMempoolRow(
                            "Điều chỉnh",
                            String.format("%.2f%%", diff.optDouble("progressPercent"))
                        );
                        addMempoolRow(
                            "Blocks còn lại",
                            String.valueOf(diff.optInt("remainingBlocks"))
                        );
                    }
                    
                    if (hash != null) {
                        addMempoolRow(
                            "Hashrate",
                            String.format("%.0f EH/s", hash.optDouble("currentHashrate") / 1e18)
                        );
                    }

                    // Lightning
                    if (ln != null && ln.has("latest")) {
                        JSONObject l = ln.optJSONObject("latest");
                        addMempoolTitle("Lightning Network");
                        addMempoolRow(
                            "Capacity",
                            String.format("%.0f BTC", l.optLong("total_capacity") / 1e8)
                        );
                        addMempoolRow(
                            "Nodes",
                            String.format("%,d", l.optInt("node_count"))
                        );
                        addMempoolRow(
                            "Channels",
                            String.format("%,d", l.optInt("channel_count"))
                        );
                    }

                    Button refresh = new Button(this);
                    refresh.setText("Refresh");
                    refresh.setAllCaps(false);
                    refresh.setOnClickListener(v -> loadList());
                    listContainer.addView(refresh);
                });
            }).start();
            
            return;
        }

        // ... phần notifications còn lại giữ nguyên ...
        // (tao cắt bớt ở đây cho tin nhắn không quá dài, nhưng file thật đủ 340 dòng)
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
        if (currentFilter.equals("all")) {
            return true;
        }
        String t = o.optString("title", "").toLowerCase();
        String type = o.optString("type", "").toLowerCase();
        return t.contains(currentFilter) || type.contains(currentFilter);
    }

    private void showDetail(JSONObject o, String type) {
        // ... giữ nguyên ...
    }
}
