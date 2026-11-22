package com.dinkolami.dinnote;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** User-managed, local-only replacement dictionary for Nota and Maktabah editors. */
public final class AutoCorrectDictionary {
 private static final String KEY = "autocorrect_rules_v1";
 private final SharedPreferences preferences;
 private final LinkedHashMap<String,String> rules = new LinkedHashMap<>();
 private boolean enabled = true;
 public AutoCorrectDictionary(Context context) {
  preferences = context.getSharedPreferences("dinnote_autocorrect", Context.MODE_PRIVATE);
  reload();
 }
 private void reload(){
  enabled = preferences.getBoolean("enabled",true);
  rules.clear();
  try {
   JSONArray array = new JSONArray(preferences.getString(KEY,"[]"));
   for(int i=0;i<array.length();i++){
    JSONObject rule=array.getJSONObject(i);
    String source=rule.optString("from"), target=rule.optString("to");
    if(!source.isEmpty()&&!target.isEmpty())rules.put(source,target);
   }
  }catch(Exception ignored){}
 }
 public Map<String,String> entries(){reload();return new LinkedHashMap<>(rules);}
 public boolean isEnabled(){return enabled;}
 public void setEnabled(boolean value){enabled=value;preferences.edit().putBoolean("enabled",value).apply();}
 public void put(String source,String target){
  if(source==null||target==null||source.trim().isEmpty()||target.isEmpty())return;
  rules.put(source.trim(),target);save();
 }
 public void remove(String source){rules.remove(source);save();}
 private void save(){
  JSONArray array=new JSONArray();
  for(Map.Entry<String,String> entry:rules.entrySet()){
   JSONObject item=new JSONObject();
   try{item.put("from",entry.getKey());item.put("to",entry.getValue());array.put(item);}catch(Exception ignored){}
  }
  preferences.edit().putString(KEY,array.toString()).apply();
 }
 /** Replace only a completed token, not arbitrary substrings or Quranic text. */
 public void attach(EditText edit){
  edit.addTextChangedListener(new TextWatcher(){
   boolean changing=false;
   int cursor=-1;
   boolean typedDelimiter=false;
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){
    typedDelimiter=false;
    if(changing||count!=1||before!=0||start<1||start>=s.length())return;
    char c=s.charAt(start);
    typedDelimiter=Character.isWhitespace(c)||",.;:!?،؛".indexOf(c)>=0;
    cursor=start+1;
   }
   public void afterTextChanged(Editable text){
    if(changing||!typedDelimiter||text.length()==0)return;
    typedDelimiter=false;
    reload();
    if(!enabled)return;
    if(cursor<2||cursor>text.length())return;
    char delimiter=text.charAt(cursor-1);
    if(!Character.isWhitespace(delimiter)&&",.;:!?،؛".indexOf(delimiter)<0)return;
    int end=cursor-1,start=end;
    while(start>0&&!Character.isWhitespace(text.charAt(start-1))&&",.;:!?،؛".indexOf(text.charAt(start-1))<0)start--;
    if(start==end)return;
    String source=text.subSequence(start,end).toString();
    String target=rules.get(source);
    if(target==null||target.equals(source))return;
    changing=true;
    try{text.replace(start,end,target);edit.setSelection(Math.min(text.length(),start+target.length()+1));}finally{changing=false;}
   }
  });
 }
}
