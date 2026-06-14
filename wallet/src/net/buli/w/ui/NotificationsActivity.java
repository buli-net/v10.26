package net.buli.w.ui;
import android.app.Activity; import android.content.SharedPreferences; import android.os.Bundle;
import android.widget.LinearLayout; import android.widget.ScrollView; import android.widget.TextView;
import android.graphics.Color; import android.view.ViewGroup; import org.json.JSONObject;
import java.util.*;
public class NotificationsActivity extends Activity {
 protected void onCreate(Bundle b){ super.onCreate(b);
  ScrollView sv=new ScrollView(this); LinearLayout ll=new LinearLayout(this); ll.setOrientation(1); sv.addView(ll);
  SharedPreferences sp=getSharedPreferences("notif",0);
  List<String> keys=new ArrayList<>(sp.getAll().keySet()); Collections.sort(keys,Collections.reverseOrder());
  for(String k:keys){ if(!k.startsWith("n_"))continue; try{
   JSONObject o=new JSONObject(sp.getString(k,"")); String t=o.getString("title"); String txt=o.getString("text");
   String ex=o.optString("extra",""); boolean read=o.getBoolean("read");
   TextView tv=new TextView(this); tv.setTextSize(16); tv.setPadding(24,24,24,24);
   int c=t.toLowerCase().contains("received")?Color.parseColor("#2E7D32"):t.toLowerCase().contains("sent")?Color.parseColor("#C62828"):Color.parseColor("#F9A825");
   tv.setText("● "+t+"\n"+txt+"\n"+ex); tv.setTextColor(c); if(!read)tv.setBackgroundColor(0x22000000);
   final String key=k; tv.setOnClickListener(v->{try{o.put("read",true);sp.edit().putString(key,o.toString()).apply();v.setBackgroundColor(0);}catch(Exception e){}});
   ll.addView(tv,new ViewGroup.LayoutParams(-1,-2));
  }catch(Exception e){}}
  setContentView(sv);
 }
}
