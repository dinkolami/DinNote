package com.dinkolami.dinnote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import java.util.*;
import android.text.style.StyleSpan;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
  DB db;
  Runnable pendingSave;
  void flushPendingSave(){if(pendingSave!=null){Runnable action=pendingSave;pendingSave=null;action.run();}}
  @Override protected void onPause(){flushPendingSave();super.onPause();}
  LinearLayout root, list;
  EditText search;
  int themeMode=0;
  boolean dark(){
    if(themeMode==1)return false;
    if(themeMode==2)return true;
    return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
  }
  int surface(){return dark()?0xFF0D1117:0xFFF6F8FA;}
  int foreground(){return dark()?0xFFE6EDF3:0xFF1F2328;}
  int borderColor(){return dark()?0xFF30363D:0xFFD0D7DE;}
  int background = Color.rgb(246,248,250);
  void applyAppearance(){background=surface();getWindow().setStatusBarColor(dark()?0xFF0D1117:0xFFF6F8FA);getWindow().setNavigationBarColor(background);
    getWindow().getDecorView().setSystemUiVisibility(dark()?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }
  void appearanceSettings(){
    new AlertDialog.Builder(this).setTitle("Appearance")
      .setSingleChoiceItems(new String[]{"System Default","Light","Dark"},themeMode,(dialog,which)->{
        themeMode=which;getPreferences(MODE_PRIVATE).edit().putInt("appearance",which).apply();
        dialog.dismiss();applyAppearance();showHome();
      }).setNegativeButton("Batal",null).show();
  }
  String noteFilter="";
  int noteSort=0;
  @Override public void onCreate(Bundle b) { super.onCreate(b); try { themeMode=getPreferences(MODE_PRIVATE).getInt("appearance",0);applyAppearance();db=new DB(this); db.getWritableDatabase(); showHome(); } catch (Exception e) { showStartupError(e); } }
  void showStartupError(Exception e) {
    android.util.Log.e("DinNote", "Startup failed", e);
    LinearLayout page=column();
    setContentView(page);
    TextView message=text("DinNote gagal dibuka. Data nota tidak dipadam.\\n\\nRalat: "+e.getClass().getSimpleName()+": "+e.getMessage(),16);
    page.addView(message);
    Button retry=new Button(this);retry.setText("Cuba lagi");page.addView(retry);
    retry.setOnClickListener(v->{try {db=new DB(this);db.getWritableDatabase();showHome();}catch(Exception error){showStartupError(error);}});
  }
  TextView text(String value,int size) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(foreground());t.setPadding(18,16,18,16);return t; }
  LinearLayout column() { LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(18,18,18,18);l.setBackgroundColor(surface());return l; }
  void showHome() {
    flushPendingSave();
    root=column();setContentView(root);
    TextView title=text("▤  DinNote",28);title.setTypeface(null,2);root.addView(title);
    Button appearance=new Button(this);appearance.setText("⚙ Appearance");root.addView(appearance);appearance.setOnClickListener(v->appearanceSettings());
    Button notesMenu=new Button(this);notesMenu.setText("📝  Nota");root.addView(notesMenu);
    Button libraryMenu=new Button(this);libraryMenu.setText("📚  Maktabah");root.addView(libraryMenu);
    libraryMenu.setOnClickListener(v->showLibrary());
    notesMenu.setOnClickListener(v->showHome());
    Button sort=new Button(this);sort.setText("Susun: Terkini");root.addView(sort);
    sort.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Terkini","Terlama","Tajuk A-Z"},(d,w)->{noteSort=w;sort.setText("Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[w]);refresh(noteFilter);}).show());
    search=new EditText(this);search.setSingleLine(true);search.setHint("Cari tajuk atau kandungan nota…");root.addView(search);
    Button add=new Button(this);add.setText("+ Nota baharu");root.addView(add);add.setOnClickListener(v->edit(-1));
    ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){noteFilter=s.toString();refresh(noteFilter);}public void afterTextChanged(Editable e){}});
    refresh(noteFilter);
  }
  void refresh(String query) {
    if(list==null)return;
    list.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,body,color FROM notes WHERE title LIKE ? OR body LIKE ? ORDER BY "+(noteSort==1?"updated ASC":noteSort==2?"title COLLATE NOCASE ASC":"updated DESC"),new String[]{"%"+query+"%","%"+query+"%"})) {
      while(c.moveToNext()){visibleCount++;final int id=c.getInt(0);String heading=c.getString(1);String body=c.getString(2);if(heading==null)heading="";if(body==null)body="";int noteColor=c.getInt(3);
        TextView item=text((heading.isEmpty()?"Tanpa tajuk":heading)+"\n"+(body.length()>110?body.substring(0,110)+"…":body),17);
        item.setBackgroundColor(noteColor==Color.WHITE&&dark()?0xFF161B22:noteColor);if(dark()&&noteColor==Color.WHITE)item.setTextColor(foreground());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,10,0,10);list.addView(item,p);item.setOnClickListener(v->edit(id));
      }
    }
    if(visibleCount==0)list.addView(text(query.trim().isEmpty()?"Belum ada nota. Tekan + Nota baharu untuk mula.":"Tiada nota ditemui.",16));
  }
  void edit(int id) {
    flushPendingSave();
    LinearLayout page=column();setContentView(page);
    Button back=new Button(this);back.setText("← Kembali");page.addView(back);
    EditText title=new EditText(this);title.setHint("Tajuk nota");title.setSingleLine(true);title.setTextSize(20);page.addView(title);
    EditText body=new EditText(this);body.setHint("Tulis nota di sini…");body.setTextSize(16);body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    final int[] noteId={id};final boolean[] loading={true};final int[] noteColor={Color.WHITE};
    int[] colors={Color.WHITE,0xFFFFF4B8,0xFFDDF4DF,0xFFDCEEFF,0xFFFFE1E9,0xFFECE2FF};
    LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);page.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
    Button colorButton=new Button(this);colorButton.setText("Warna");toolbar.addView(colorButton,new LinearLayout.LayoutParams(0,-2,1));
    Button checkButton=new Button(this);checkButton.setText("☐");toolbar.addView(checkButton,new LinearLayout.LayoutParams(0,-2,1));
    Button boldButton=new Button(this);boldButton.setText("B");boldButton.setTypeface(null,Typeface.BOLD);toolbar.addView(boldButton,new LinearLayout.LayoutParams(0,-2,1));
    Button italicButton=new Button(this);italicButton.setText("I");italicButton.setTypeface(null,Typeface.ITALIC);toolbar.addView(italicButton,new LinearLayout.LayoutParams(0,-2,1));
    Button underlineButton=new Button(this);underlineButton.setText("U");toolbar.addView(underlineButton,new LinearLayout.LayoutParams(0,-2,1));
    underlineButton.setOnClickListener(v->underlineSelection(body));
    colorButton.setOnClickListener(v->{int index=0;for(int i=0;i<colors.length;i++)if(colors[i]==noteColor[0])index=i;noteColor[0]=colors[(index+1)%colors.length];body.setBackgroundColor(noteColor[0]);saveColor(noteId[0],noteColor[0]);});
    checkButton.setOnClickListener(v->{int start=Math.max(0,body.getSelectionStart());body.getText().insert(start,"☐ ");});
    boldButton.setOnClickListener(v->styleSelection(body,Typeface.BOLD));
    italicButton.setOnClickListener(v->styleSelection(body,Typeface.ITALIC));
    if(id>=0){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body,color FROM notes WHERE id=?",new String[]{""+id})){if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(1));noteColor[0]=c.getInt(2);body.setBackgroundColor(noteColor[0]);}}}
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),b=body.getText().toString();if(noteId[0]<0&&t.trim().isEmpty()&&b.trim().isEmpty())return;
      ContentValues values=new ContentValues();values.put("title",t);values.put("body",b);values.put("updated",System.currentTimeMillis());values.put("color",noteColor[0]);
      if(noteId[0]<0)noteId[0]=(int)db.getWritableDatabase().insertOrThrow("notes",null,values);else db.getWritableDatabase().update("notes",values,"id=?",new String[]{""+noteId[0]});
    };
    TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save.run();}public void afterTextChanged(Editable e){}};
    title.addTextChangedListener(watcher);body.addTextChangedListener(watcher);
    pendingSave=save;
    back.setOnClickListener(v->{flushPendingSave();showHome();});
    title.setContentDescription("Tajuk nota");body.setContentDescription("Kandungan nota Arab dan Rumi");
    Button delete=new Button(this);delete.setText("Padam nota");page.addView(delete);
    delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Padam nota ini?").setNegativeButton("Batal",null).setPositiveButton("Padam",(d,w)->{pendingSave=null;if(noteId[0]>=0)db.getWritableDatabase().delete("notes","id=?",new String[]{""+noteId[0]});showHome();}).show());
  }
  void showLibrary() {
    flushPendingSave();
    LinearLayout page=column();setContentView(page);
    Button back=new Button(this);back.setText("← DinNote");page.addView(back);back.setOnClickListener(v->showHome());
    page.addView(text("📚  Maktabah",26));
    Button add=new Button(this);add.setText("+ Kategori baharu");page.addView(add);
    add.setOnClickListener(v->promptCategory());
    ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(1);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name FROM categories ORDER BY name COLLATE NOCASE",null)){
      while(c.moveToNext()){int categoryId=c.getInt(0);String name=c.getString(1);
        Button item=new Button(this);item.setText("📁  "+name);items.addView(item);item.setOnClickListener(v->showBooks(categoryId,name));
        item.setOnLongClickListener(v->{new AlertDialog.Builder(this).setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{
          if(which==0)promptRenameCategory(categoryId,name);else new AlertDialog.Builder(this).setMessage("Padam kategori dan semua kitab di dalamnya?").setNegativeButton("Batal",null).setPositiveButton("Padam",(dd,w)->{db.getWritableDatabase().delete("books","category_id=?",new String[]{""+categoryId});db.getWritableDatabase().delete("categories","id=?",new String[]{""+categoryId});showLibrary();}).show();
        }).show();return true;});
      }
    }
  }
  void promptCategory(){
    EditText input=new EditText(this);input.setHint("Nama kategori");
    new AlertDialog.Builder(this).setTitle("Kategori baharu").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(!name.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",name);db.getWritableDatabase().insert("categories",null,cv);}showLibrary();
    }).show();
  }
  void promptRenameCategory(int id,String old){
    EditText input=new EditText(this);input.setText(old);
    new AlertDialog.Builder(this).setTitle("Ubah nama kategori").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(!name.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",name);db.getWritableDatabase().update("categories",cv,"id=?",new String[]{""+id});}showLibrary();
    }).show();
  }
  void showBooks(int categoryId,String categoryName){
    flushPendingSave();
    LinearLayout page=column();setContentView(page);
    Button back=new Button(this);back.setText("← Maktabah");page.addView(back);back.setOnClickListener(v->showLibrary());
    page.addView(text("📁  "+categoryName,24));
    Button add=new Button(this);add.setText("+ Tambah kitab");page.addView(add);add.setOnClickListener(v->editBook(-1,categoryId,categoryName));
    EditText bookSearch=new EditText(this);bookSearch.setSingleLine(true);bookSearch.setHint("Cari kitab dalam kategori…");page.addView(bookSearch);
    ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(1);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    bookSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){populateBooks(items,categoryId,categoryName,s.toString());}public void afterTextChanged(Editable e){}});
    populateBooks(items,categoryId,categoryName,"");
  }
  void populateBooks(LinearLayout items,int categoryId,String categoryName,String query){
    items.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title FROM books WHERE category_id=? AND (title LIKE ? OR body LIKE ?) ORDER BY updated DESC",
      new String[]{""+categoryId,"%"+query+"%","%"+query+"%"})){
      while(c.moveToNext()){visibleCount++;int bookId=c.getInt(0);String heading=c.getString(1);
        Button item=new Button(this);item.setText("📖  "+(heading==null||heading.isEmpty()?"Tanpa tajuk":heading));
        items.addView(item);item.setOnClickListener(v->editBook(bookId,categoryId,categoryName));
      }
    }
    if(visibleCount==0)items.addView(text(query.trim().isEmpty()?"Kategori ini belum mempunyai kitab.":"Tiada kitab ditemui.",16));
  }

  void editBook(int id,int categoryId,String categoryName){
    flushPendingSave();
    LinearLayout page=column();setContentView(page);
    Button back=new Button(this);back.setText("← Maktabah");page.addView(back);
    EditText title=new EditText(this);title.setHint("Tajuk kitab");title.setSingleLine(true);title.setTextSize(20);page.addView(title);
    EditText body=new EditText(this);body.setHint("Tulis kandungan kitab (Arab / Rumi)…");body.setTextSize(16);
    body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    final int[] bookId={id};final boolean[] loading={true};
    if(id>=0)try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body FROM books WHERE id=?",new String[]{""+id})){
      if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(1));}
    }
    LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);
    page.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
    Button check=new Button(this);check.setText("☐");toolbar.addView(check,new LinearLayout.LayoutParams(0,-2,1));
    Button bold=new Button(this);bold.setText("B");bold.setTypeface(null,Typeface.BOLD);toolbar.addView(bold,new LinearLayout.LayoutParams(0,-2,1));
    Button italic=new Button(this);italic.setText("I");italic.setTypeface(null,Typeface.ITALIC);toolbar.addView(italic,new LinearLayout.LayoutParams(0,-2,1));
    Button underline=new Button(this);underline.setText("U");toolbar.addView(underline,new LinearLayout.LayoutParams(0,-2,1));
    underline.setOnClickListener(v->underlineSelection(body));
    check.setOnClickListener(v->body.getText().insert(Math.max(0,body.getSelectionStart()),"☐ "));
    bold.setOnClickListener(v->styleSelection(body,Typeface.BOLD));
    italic.setOnClickListener(v->styleSelection(body,Typeface.ITALIC));
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),content=body.getText().toString();
      if(bookId[0]<0&&t.trim().isEmpty()&&content.trim().isEmpty())return;
      ContentValues cv=new ContentValues();cv.put("title",t);cv.put("body",content);
      cv.put("category_id",categoryId);cv.put("updated",System.currentTimeMillis());
      if(bookId[0]<0)bookId[0]=(int)db.getWritableDatabase().insertOrThrow("books",null,cv);
      else db.getWritableDatabase().update("books",cv,"id=?",new String[]{""+bookId[0]});
    };
    TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}
      public void onTextChanged(CharSequence s,int st,int before,int count){save.run();}
      public void afterTextChanged(Editable e){}};
    title.addTextChangedListener(watcher);body.addTextChangedListener(watcher);
    pendingSave=save;
    back.setOnClickListener(v->{flushPendingSave();showBooks(categoryId,categoryName);});
    Button delete=new Button(this);delete.setText("Padam kitab");page.addView(delete);
    delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Padam kitab ini?")
      .setNegativeButton("Batal",null).setPositiveButton("Padam",(d,w)->{
        pendingSave=null;if(bookId[0]>=0)db.getWritableDatabase().delete("books","id=?",new String[]{""+bookId[0]});
        showBooks(categoryId,categoryName);
      }).show());
  }
  void saveColor(int id,int color){if(id<0)return;ContentValues v=new ContentValues();v.put("color",color);db.getWritableDatabase().update("notes",v,"id=?",new String[]{""+id});}
  void underlineSelection(EditText edit){int a=edit.getSelectionStart(),b=edit.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}edit.getText().setSpan(new android.text.style.UnderlineSpan(),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}
  void styleSelection(EditText edit,int style){int a=edit.getSelectionStart(),b=edit.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}SpannableStringBuilder t=new SpannableStringBuilder(edit.getText());t.setSpan(new StyleSpan(style),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);edit.setText(t);edit.setSelection(b);}
  @Override public void onBackPressed(){showHome();}
  static class DB extends SQLiteOpenHelper {
    DB(Context c){super(c,"dinnote.db",null,3);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL,color INTEGER NOT NULL DEFAULT -1)");createLibrary(db);}
    static void createLibrary(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY AUTOINCREMENT,category_id INTEGER NOT NULL,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL)");}
    @Override public void onOpen(SQLiteDatabase database){super.onOpen(database);createLibrary(database);}
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2) { try { db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT -1"); } catch (android.database.sqlite.SQLiteException ignored) { /* Column may already exist in older builds. */ } } if(oldVersion<3)createLibrary(db); }
  }
}
