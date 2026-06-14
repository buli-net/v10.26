package net.buli.w.ui;
import android.app.Activity; 
import android.content.SharedPreferences; 
import android.os.Bundle;
import android.widget.LinearLayout; 
import android.widget.ScrollView; 
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color; 
import android.view.Gravity;
import android.view.ViewGroup; 
import org.json.JSONObject;
import java.util.*;

public class NotificationsActivity extends Activity {
 protected void onCreate(Bundle b){ 
  super.onCreate(b);
  ScrollView sv=new ScrollView(this); 
  LinearLayout ll=new LinearLayout(this); 
  ll.setOrientation(LinearLayout.VERTICAL); 
  sv.addView(ll);
  
  SharedPreferences sp=getSharedPreferences("notif",0);
  List<String> keys=new ArrayList<>(sp.getAll().keySet()); 
  Collections.sort(keys,Collections.reverseOrder());
  
  int count=0;
  for(String k:keys){ 
   if(!k.startsWith("n_"))continue; 
   count++;
   try{
    JSONObject o=new JSONObject(sp.getString(k,"")); 
    String t=o.getString("title"); 
    String txt=o.getString("text");
    String ex=o.optString("extra",""); 
    boolean read=o.getBoolean("read");
    
    TextView tv=new TextView(this); 
    tv.setTextSize(16); 
    tv.setPadding(32,32,32,32);
    
    int c=t.toLowerCase().contains("received")?Color.parseColor("#2E7D32"):
          t.toLowerCase().contains("sent")?Color.parseColor("#C62828"):
          Color.parseColor("#F9A825");
    
    tv.setText("● "+t+"\n"+txt+(ex.isEmpty()?"":"\n"+ex)); 
    tv.setTextColor(read?0xFF888888:c); 
    if(!read)tv.setBackgroundColor(0x22000000);
    
    final String key=k; 
    final JSONObject obj=o;
    tv.setOnClickListener(v->{
     try{
      obj.put("read",true);
      sp.edit().putString(key,obj.toString()).apply();
      v.setBackgroundColor(0);
      tv.setTextColor(0xFF888888);
      Toast.makeText(NotificationsActivity.this,"Đã đọc",Toast.LENGTH_SHORT).show();
     }catch(Exception e){}
    });
    
    ll.addView(tv,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
   }catch(Exception e){}
  }
  
  if(count==0){
   TextView tv=new TextView(this);
   tv.setText("Chưa có thông báo");
   tv.setGravity(Gravity.CENTER);
   tv.setTextSize(18);
   tv.setPadding(0,300,0,0);
   ll.addView(tv);
  }
  
  setContentView(sv);
 }
}