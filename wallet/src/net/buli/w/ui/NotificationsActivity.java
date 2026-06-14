package net.buli.w.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.buli.w.R;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends Activity {
    private RecyclerView list;
    private Adapter adapter;
    private String currentTab = "all";
    private SharedPreferences sp;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        sp = getSharedPreferences("notif",0);
        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        list.setAdapter(adapter);

        findViewById(R.id.tab_all).setOnClickListener(v->switchTab("all"));
        findViewById(R.id.tab_received).setOnClickListener(v->switchTab("received"));
        findViewById(R.id.tab_sent).setOnClickListener(v->switchTab("sent"));
        findViewById(R.id.tab_peer).setOnClickListener(v->switchTab("peer"));
        findViewById(R.id.tab_sync).setOnClickListener(v->switchTab("sync"));

        findViewById(R.id.btn_mark_all).setOnClickListener(v->markAllRead());
        findViewById(R.id.btn_delete_all).setOnClickListener(v->deleteAll());

        switchTab("all");
    }

    private void switchTab(String tab){
        currentTab = tab;
        load();
    }

    private void load(){
        List<Item> items = new ArrayList<>();
        for(String k: sp.getAll().keySet()){
            if(!k.startsWith("n_")) continue;
            try{
                JSONObject o = new JSONObject(sp.getString(k,""));
                Item it = new Item();
                it.key = k;
                it.title = o.optString("title");
                it.text = o.optString("text");
                it.extra = o.optString("extra");
                it.time = o.optLong("time");
                it.read = o.optBoolean("read");
                it.type = o.optString("type","unknown");
                if(currentTab.equals("all") || it.type.equalsIgnoreCase(currentTab) || 
                   (currentTab.equals("received") && it.title.contains("Nhận")) ||
                   (currentTab.equals("sent") && it.title.contains("Gửi")) ||
                   (currentTab.equals("peer") && it.type.equals("peer")) ||
                   (currentTab.equals("sync") && it.type.equals("sync"))
                ){
                    items.add(it);
                }
            }catch(Exception e){}
        }
        Collections.sort(items,(a,b)->Long.compare(b.time,a.time));
        adapter.setItems(items);
    }

    private void markAllRead(){
        SharedPreferences.Editor ed = sp.edit();
        for(String k: sp.getAll().keySet()){
            if(k.startsWith("n_")){
                try{
                    JSONObject o = new JSONObject(sp.getString(k,""));
                    o.put("read",true);
                    ed.putString(k,o.toString());
                }catch(Exception e){}
            }
        }
        ed.apply();
        load();
    }

    private void deleteAll(){
        SharedPreferences.Editor ed = sp.edit();
        for(String k: sp.getAll().keySet()) if(k.startsWith("n_")) ed.remove(k);
        ed.apply();
        load();
    }

    class Item{ String key,title,text,extra,type; long time; boolean read; }

    class Adapter extends RecyclerView.Adapter<VH>{
        List<Item> items = new ArrayList<>();
        void setItems(List<Item> l){ items=l; notifyDataSetChanged(); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification,p,false));
        }
        @Override public void onBindViewHolder(@NonNull VH h,int pos){
            Item it = items.get(pos);
            String title = it.title;
            String subtitle = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date(it.time));
            
            // === HIỂN THỊ TÓM TẮT TRONG LIST - SỬA THEO YÊU CẦU ===
            try{
                JSONObject e = it.extra!=null && it.extra.startsWith("{") ? new JSONObject(it.extra) : null;
                if(it.type.equals("sync") && e!=null){
                    String pct = e.optString("percent", "");
                    if(pct.isEmpty() && e.has("blocksLeft")){
                        // fallback tính từ dữ liệu cũ
                        pct = "?";
                    }
                    title = "Syncing • " + (pct.isEmpty()?"":pct+"%");
                    subtitle = e.optInt("blocksLeft",0) + " blocks left" + (e.has("speed")?" • "+e.optString("speed"):"");
                } else if(it.type.equals("peer") && e!=null){
                    String country = e.optString("country","");
                    if(country.isEmpty()) country = "Unknown";
                    title = (it.title.contains("connected")?"Peer ✓":"Peer ✗") + " • " + country;
                    subtitle = e.optString("ip", it.text) + (e.has("ping")?" • "+e.optString("ping"):"");
                } else if(it.type.equals("received") || it.type.equals("sent")){
                    title = it.title + " • " + it.text;
                }
            }catch(Exception ignored){}
            
            h.title.setText(title);
            h.subtitle.setText(subtitle);
            h.itemView.setAlpha(it.read?0.6f:1f);
            h.itemView.setOnClickListener(v->showDetail(it));
        }
        @Override public int getItemCount(){ return items.size(); }
    }

    static class VH extends RecyclerView.ViewHolder{
        TextView title,subtitle;
        VH(View v){ super(v); title=v.findViewById(R.id.title); subtitle=v.findViewById(R.id.subtitle); }
    }

    private void showDetail(Item it){
        try{
            JSONObject e = it.extra!=null && it.extra.startsWith("{") ? new JSONObject(it.extra) : new JSONObject();
            String msg = "";
            String type = it.type;
            
            // === POPUP FULL CHI TIẾT ===
            if(type.equals("sync")){
                msg = "Type: Sync\n" +
                      "Blocks left: " + e.optInt("blocksLeft",0) + "\n" +
                      "Tiến độ: " + e.optString("percent","?") + "%\n" +
                      "Tốc độ: " + e.optString("speed","N/A") + "\n" +
                      "ETA: " + e.optString("eta","N/A") + "\n" +
                      "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date(it.time));
            } else if(type.equals("peer")){
                msg = "Type: Peer\n" +
                      "Address: " + e.optString("ip", it.text) + "\n" +
                      "Country: " + e.optString("country","Unknown") + "\n" +
                      "Ping: " + e.optString("ping","N/A") + "\n" +
                      "Version: " + e.optString("version","N/A") + "\n" +
                      "Peers: " + e.optString("peers","N/A") + "\n" +
                      "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date(it.time));
            } else {
                msg = "Type: " + it.title + "\n" +
                      "Amount: " + it.text + "\n" +
                      (e.has("txid")?"TxID: "+e.optString("txid")+"\n":"") +
                      (e.has("from")?"From: "+e.optString("from")+"\n":"") +
                      (e.has("to")?"To: "+e.optString("to")+"\n":"") +
                      "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date(it.time));
            }

            new AlertDialog.Builder(this)
                .setTitle(it.title)
                .setMessage(msg)
                .setPositiveButton("Copy", (d,w)->{
                    ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("notif", msg));
                    Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Share", (d,w)->{
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, msg);
                    startActivity(Intent.createChooser(i,"Share"));
                })
                .setNegativeButton("Close",null)
                .show();

            // mark as read
            JSONObject o = new JSONObject(sp.getString(it.key,""));
            o.put("read",true);
            sp.edit().putString(it.key,o.toString()).apply();
            load();

        }catch(Exception ex){}
    }
}
