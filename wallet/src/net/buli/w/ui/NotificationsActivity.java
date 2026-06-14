package net.buli.w.ui;
import android.app.Activity; 
import android.app.AlertDialog;
import android.content.SharedPreferences; 
import android.os.Bundle;
import android.widget.LinearLayout; 
import android.widget.ScrollView; 
import android.widget.TextView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.Toast;
import android.graphics.Color; 
import android.view.Gravity;
import android.view.ViewGroup; 
import org.json.JSONObject;
import java.util.*;
import java.text.SimpleDateFormat;

public class NotificationsActivity extends Activity {
 LinearLayout ll; 
 SharedPreferences sp;
 SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
 String currentFilter = "all";
 LinearLayout tabBar;

 protected void onCreate(Bundle b){ 
  super.onCreate(b);
  sp=getSharedPreferences("notif",0);

  LinearLayout root = new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);

  // ---- TAB BAR ----
  HorizontalScrollView hsv = new HorizontalScrollView(this);
  hsv.setHorizontalScrollBarEnabled(false);
  tabBar = new LinearLayout(this);
  tabBar.setOrientation(LinearLayout.HORIZONTAL);
  hsv.addView(tabBar);
  root.addView(hsv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  
  String[] tabs = {"All","Received","Sent","Peer","Sync"};
  for(String t: tabs){
    Button btn = new Button(this);
    btn.setText(t);
    btn.setAllCaps(false);
    btn.setTag(t.toLowerCase());
    btn.setOnClickListener(v->{
        currentFilter = (String)v.getTag();
        updateTabs();
        loadList();
    });
    tabBar.addView(btn);
  }
  updateTabs();

  ScrollView sv=new ScrollView(this); 
  ll=new LinearLayout(this); 
  ll.setOrientation(LinearLayout.VERTICAL); 
  sv.addView(ll);
  root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

  loadList();
  setContentView(root);
 }

 void updateTabs(){
    for(int i=0;i<tabBar.getChildCount();i++){
        Button b = (Button)tabBar.getChildAt(i);
        boolean sel = b.getTag().equals(currentFilter);
        b.setBackgroundColor(sel?0xFF1565C0:0x00000000);
        b.setTextColor(sel?Color.WHITE:0xFFAAAAAA);
    }
 }

 void loadList(){
  ll.removeAllViews();

  // nút đánh dấu theo tab
  Button markAll = new Button(this);
  markAll.setText("Đánh dấu tất cả đã đọc ("+currentFilter+")");
  markAll.setAllCaps(false);
  markAll.setOnClickListener(v->{
    SharedPreferences.Editor ed=sp.edit();
    int cnt=0;
    for(String k: sp.getAll().keySet()) if(k.startsWith("n_")){
      try{ 
        JSONObject o=new JSONObject(sp.getString(k,""));
        String title=o.optString("title","").toLowerCase();
        // ALL = đánh dấu hết, các tab khác = lọc
        if(currentFilter.equals("all") || matchesFilter(title)){
            if(!o.optBoolean("read",false)){
                o.put("read",true); 
                ed.putString(k,o.toString()); 
                cnt++;
            }
        }
      }catch(Exception e){}
    }
    ed.apply(); 
    Toast.makeText(this,"Đã đọc "+cnt+" thông báo",Toast.LENGTH_SHORT).show(); 
    loadList();
  });
  ll.addView(markAll, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

  List<String> keys=new ArrayList<>(sp.getAll().keySet()); 
  Collections.sort(keys,Collections.reverseOrder());

  int count=0;
  for(String k:keys){ 
   if(!k.startsWith("n_"))continue; 
   try{
    JSONObject o=new JSONObject(sp.getString(k,"")); 
    String t=o.optString("title",""); 
    if(!matchesFilter(t.toLowerCase())) continue;
    count++;
    String txt=o.optString("text","");
    String ex=o.optString("extra",""); 
    boolean read=o.optBoolean("read",false);
    long time=o.optLong("time",System.currentTimeMillis());

    TextView tv=new TextView(this); 
    tv.setTextSize(16); 
    tv.setPadding(32,28,32,28);

    int c = t.toLowerCase().contains("received")? Color.parseColor("#2E7D32"):
            t.toLowerCase().contains("sent")? Color.parseColor("#C62828"):
            t.toLowerCase().contains("peer")? Color.parseColor("#1565C0"):
            Color.parseColor("#F9A825");

    tv.setText("● "+t+"\n"+fmt.format(new Date(time))); 
    tv.setTextColor(read?0xFF888888:c); 
    if(!read) tv.setBackgroundColor(0x22000000);

    final String key=k; 
    final JSONObject obj=o;
    final String detail = txt + (ex.isEmpty()?"":"\n\n"+ex);

    tv.setOnClickListener(v->{
     try{
      if(!obj.getBoolean("read")){
        obj.put("read",true);
        sp.edit().putString(key,obj.toString()).apply();
        tv.setTextColor(0xFF888888);
        v.setBackgroundColor(0);
      }
      new AlertDialog.Builder(NotificationsActivity.this)
        .setTitle(obj.getString("title"))
        .setMessage("Thời gian: "+fmt.format(new Date(obj.getLong("time")))+"\n\n"+detail)
        .setPositiveButton("Đóng", null)
        .setNeutralButton("Copy", (d,w)->{
            android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("notif", detail));
            Toast.makeText(NotificationsActivity.this,"Đã copy",Toast.LENGTH_SHORT).show();
        })
        .show();
     }catch(Exception e){
        Toast.makeText(NotificationsActivity.this,"Lỗi: "+e.getMessage(),Toast.LENGTH_SHORT).show();
     }
    });

    ll.addView(tv,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
   }catch(Exception e){}
  }

  if(count==0){
   TextView tv=new TextView(this);
   tv.setText("Chưa có thông báo ("+currentFilter+")");
   tv.setGravity(Gravity.CENTER);
   tv.setTextSize(18);
   tv.setPadding(0,300,0,0);
   ll.addView(tv);
  }
 }

 boolean matchesFilter(String title){
    if(currentFilter.equals("all")) return true;
    if(currentFilter.equals("received")) return title.contains("received");
    if(currentFilter.equals("sent")) return title.contains("sent");
    if(currentFilter.equals("peer")) return title.contains("peer");
    if(currentFilter.equals("sync")) return title.contains("sync");
    return true;
 }
}