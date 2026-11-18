package com.dinkolami.dinnote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
  DB db;
  LinearLayout root, list;
  EditText search;
  int background = Color.rgb(248,249,252);
  @Override public void onCreate(Bundle b) { super.onCreate(b); db=new DB(this); showHome(); }
  TextView text(String value,int size) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.rgb(36,43,57));t.setPadding(18,16,18,16);return t; }
  LinearLayout column() { LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(18,18,18,18);l.setBackgroundColor(background);return l; }
  void showHome() {
    root=column();setContentView(root);
    TextView title=text("DinNote",28);title.setTypeface(null,1);root.addView(title);
    search=new EditText(this);search.setSingleLine(true);search.setHint("Cari nota…");root.addView(search);
    Button add=new Button(this);add.setText("+ Nota baharu");root.addView(add);add.setOnClickListener(v->edit(-1));
    ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){refresh(s.toString());}public void afterTextChanged(Editable e){}});
    refresh("");
  }
  void refresh(String query) {
    list.removeAllViews();
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,body FROM notes WHERE title LIKE ? OR body LIKE ? ORDER BY updated DESC",new String[]{"%"+query+"%","%"+query+"%"})) {
      while(c.moveToNext()){final int id=c.getInt(0);String heading=c.getString(1);String body=c.getString(2);
        TextView item=text((heading.isEmpty()?"Tanpa tajuk":heading)+"\n"+(body.length()>110?body.substring(0,110)+"…":body),17);
        item.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,10,0,10);list.addView(item,p);item.setOnClickListener(v->edit(id));
      }
    }
  }
  void edit(int id) {
    LinearLayout page=column();setContentView(page);
    Button back=new Button(this);back.setText("← Kembali");page.addView(back);
    EditText title=new EditText(this);title.setHint("Tajuk nota");title.setSingleLine(true);title.setTextSize(22);page.addView(title);
    EditText body=new EditText(this);body.setHint("Tulis nota di sini…");body.setTextSize(17);body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    final int[] noteId={id};final boolean[] loading={true};
    if(id>=0){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body FROM notes WHERE id=?",new String[]{""+id})){if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(1));}}}
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),b=body.getText().toString();if(noteId[0]<0&&t.trim().isEmpty()&&b.trim().isEmpty())return;
      ContentValues values=new ContentValues();values.put("title",t);values.put("body",b);values.put("updated",System.currentTimeMillis());
      if(noteId[0]<0)noteId[0]=(int)db.getWritableDatabase().insert("notes",null,values);else db.getWritableDatabase().update("notes",values,"id=?",new String[]{""+noteId[0]});
    };
    TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save.run();}public void afterTextChanged(Editable e){}};
    title.addTextChangedListener(watcher);body.addTextChangedListener(watcher);
    back.setOnClickListener(v->{save.run();showHome();});
    Button delete=new Button(this);delete.setText("Padam nota");page.addView(delete);
    delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Padam nota ini?").setNegativeButton("Batal",null).setPositiveButton("Padam",(d,w)->{if(noteId[0]>=0)db.getWritableDatabase().delete("notes","id=?",new String[]{""+noteId[0]});showHome();}).show());
  }
  @Override public void onBackPressed(){showHome();}
  static class DB extends SQLiteOpenHelper {
    DB(Context c){super(c,"dinnote.db",null,1);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL)");}
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
  }
}
