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
  TextView saveIndicator;
  void setSaveStatus(String message){if(saveIndicator!=null)saveIndicator.setText(message);}
  boolean editingBook=false;
  boolean editingNote=false;
  int activeCategoryId=-1;
  String activeCategoryName="";
  void flushPendingSave(){if(pendingSave!=null){Runnable action=pendingSave;pendingSave=null;saveHandler.removeCallbacksAndMessages(null);action.run();}}
  void scheduleSave(Runnable action){if(pendingSave!=null&&pendingSave!=action)flushPendingSave();saveHandler.removeCallbacksAndMessages(null);pendingSave=action;setSaveStatus("Belum disimpan…");saveHandler.postDelayed(()->{if(pendingSave==action){pendingSave=null;action.run();}},450);}
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
    new AlertDialog.Builder(this).setTitle("Penampilan")
      .setSingleChoiceItems(new String[]{"Ikut sistem","Cerah","Gelap"},themeMode,(dialog,which)->{
        themeMode=which;getPreferences(MODE_PRIVATE).edit().putInt("appearance",which).apply();
        dialog.dismiss();applyAppearance();showHome();
      }).setNegativeButton("Batal",null).show();
  }
  String noteFilter="";
  int noteSort=0;
  final Handler saveHandler=new Handler(Looper.getMainLooper());
  @Override public void onCreate(Bundle b) { super.onCreate(b); try { themeMode=getPreferences(MODE_PRIVATE).getInt("appearance",0);if(themeMode<0||themeMode>2)themeMode=0;noteSort=getPreferences(MODE_PRIVATE).getInt("noteSort",0);if(noteSort<0||noteSort>2)noteSort=0;applyAppearance();db=new DB(this); db.getWritableDatabase(); showHome(); } catch (Exception e) { showStartupError(e); } }
  void showStartupError(Exception e) {
    android.util.Log.e("DinNote", "Startup failed", e);
    LinearLayout page=column();
    setContentView(page);
    TextView message=text("DinNote gagal dibuka. Data nota tidak dipadam.\n\nRalat: "+e.getClass().getSimpleName()+": "+e.getMessage(),16);
    page.addView(message);
    Button retry=button();retry.setText("Cuba lagi");page.addView(retry);
    retry.setOnClickListener(v->{try {db=new DB(this);db.getWritableDatabase();showHome();}catch(Exception error){showStartupError(error);}});
  }
  int cardSurface(){return dark()?0xFF161B22:0xFFFFFFFF;}
  int accent(){return dark()?0xFF58A6FF:0xFF0969DA;}
  android.graphics.drawable.GradientDrawable rounded(int color,int stroke,int radius){
    android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
    d.setColor(color);d.setCornerRadius(radius);d.setStroke(1,stroke);return d;
  }
  Button button(){
    Button b=new Button(this);b.setAllCaps(false);b.setTextSize(14);b.setLetterSpacing(0);
    b.setMinHeight(44);b.setTextColor(foreground());b.setBackground(rounded(cardSurface(),borderColor(),12));
    b.setPadding(14,9,14,9);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,5,0,5);b.setLayoutParams(p);
    return b;
  }
  EditText field(){
    EditText e=new EditText(this);e.setTextColor(foreground());
    e.setHintTextColor(dark()?0xFF8B949E:0xFF656D76);
    e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);e.setTextSize(15);e.setPadding(14,12,14,12);
    e.setBackground(rounded(cardSurface(),borderColor(),12));return e;
  }
  TextView text(String value,int size) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(foreground());t.setPadding(10,9,10,9);return t; }
  LinearLayout column() { LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(14,14,14,14);l.setBackgroundColor(surface());return l; }
  void showHome() {
    flushPendingSave();
    editingBook=false;editingNote=false;saveIndicator=null;
    root=column();root.setFocusableInTouchMode(true);setContentView(root);
    LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
    TextView title=text("▤  DinNote",22);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    TextView appearance=text("⚙",22);appearance.setContentDescription("Tetapan penampilan");appearance.setFocusable(true);appearance.setOnClickListener(v->appearanceSettings());header.addView(appearance);
    View headerDivider=new View(this);headerDivider.setBackgroundColor(borderColor());LinearLayout.LayoutParams hd=new LinearLayout.LayoutParams(-1,1);hd.setMargins(0,4,0,12);root.addView(headerDivider,hd);
    Button notesMenu=button();notesMenu.setText("📝  Nota");notesMenu.setTypeface(null,Typeface.BOLD);root.addView(notesMenu);
    Button libraryMenu=button();libraryMenu.setText("📚  Maktabah");libraryMenu.setTypeface(null,Typeface.BOLD);root.addView(libraryMenu);
    libraryMenu.setContentDescription("Buka Maktabah");libraryMenu.setOnClickListener(v->showLibrary());
    notesMenu.setContentDescription("Buka Nota");notesMenu.setOnClickListener(v->showHome());
    Button sort=button();sort.setText("Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[noteSort]);root.addView(sort);
    sort.setContentDescription("Pilih susunan nota");sort.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Terkini","Terlama","Tajuk A-Z"},(d,w)->{noteSort=w;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",w).apply();sort.setText("Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[w]);refresh(noteFilter);}).show());
    search=field();search.setContentDescription("Cari nota");search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setSelectAllOnFocus(false);search.setHint("Cari nota…");search.setText(noteFilter);search.setSelection(search.length());root.addView(search);
    Button add=button();add.setText("+ Nota baharu");add.setContentDescription("Cipta nota baharu");root.addView(add);add.setOnClickListener(v->edit(-1));
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){noteFilter=s.toString();refresh(noteFilter);}public void afterTextChanged(Editable e){}});
    try{refresh(noteFilter);}catch(Exception e){android.util.Log.e("DinNote","Failed loading notes",e);Toast.makeText(this,"Gagal memuatkan nota",Toast.LENGTH_LONG).show();}
  }
  void refresh(String query) {
    if(query==null)query="";
    if(list==null||db==null)return;
    list.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,body,color FROM notes WHERE title LIKE ? OR body LIKE ? ORDER BY "+(noteSort==1?"updated ASC,id ASC":noteSort==2?"title COLLATE NOCASE ASC,id DESC":"updated DESC,id DESC"),new String[]{"%"+query+"%","%"+query+"%"})) {
      while(c.moveToNext()){visibleCount++;final int id=c.getInt(0);String heading=c.getString(1);String body=c.getString(2);if(heading==null)heading="";if(body==null)body="";body=body.replace("\r"," ").replace("\n"," ").trim();int noteColor=c.getInt(3);
        TextView item=text((heading.trim().isEmpty()?"Tanpa tajuk":heading)+"\n"+(body.length()>90?body.substring(0,90)+"…":body),15);
        item.setBackground(rounded(noteColor==Color.WHITE?cardSurface():noteColor,borderColor(),16));item.setTextColor(noteColor==Color.WHITE?foreground():Color.BLACK);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,5,0,5);list.addView(item,p);item.setOnClickListener(v->edit(id));
      }
    }
    if(visibleCount==0)list.addView(text(query.trim().isEmpty()?"Belum ada nota. Tekan + Nota baharu untuk menulis.":"Tiada nota ditemui.",16));
  }
  void edit(int id) {
    flushPendingSave();editingNote=true;
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← Nota");back.setTextSize(14);page.addView(back);
    saveIndicator=text("Disimpan secara automatik",12);page.addView(saveIndicator);
    EditText title=field();title.setContentDescription("Tajuk nota");title.setHint("Tajuk nota");title.setSingleLine(true);title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});title.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);title.setTextSize(17);page.addView(title);
    EditText body=field();body.setHint("Tulis nota di sini…");body.setTextSize(15);body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);body.setTextColor(foreground());
    body.setMinLines(5);body.setSelectAllOnFocus(false);body.setContentDescription("Kandungan nota");page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    final Button[] deleteButton={null};
    final int[] noteId={id};final boolean[] loading={true};final int[] noteColor={Color.WHITE};final Runnable[] saveAction={null};
    int[] colors={Color.WHITE,0xFFFFF4B8,0xFFDDF4DF,0xFFDCEEFF,0xFFFFE1E9,0xFFECE2FF};
    LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);page.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
    Button colorButton=button();colorButton.setText("Warna");toolbar.addView(colorButton,new LinearLayout.LayoutParams(0,-2,1));
    Button checkButton=button();checkButton.setText("☐");toolbar.addView(checkButton,new LinearLayout.LayoutParams(0,-2,1));
    Button boldButton=button();boldButton.setText("B");boldButton.setContentDescription("Tebalkan teks dipilih");boldButton.setTypeface(null,Typeface.BOLD);toolbar.addView(boldButton,new LinearLayout.LayoutParams(0,-2,1));
    Button italicButton=button();italicButton.setText("I");italicButton.setContentDescription("Condongkan teks dipilih");italicButton.setTypeface(null,Typeface.ITALIC);toolbar.addView(italicButton,new LinearLayout.LayoutParams(0,-2,1));
    Button underlineButton=button();underlineButton.setText("U");underlineButton.setContentDescription("Gariskan teks dipilih");toolbar.addView(underlineButton,new LinearLayout.LayoutParams(0,-2,1));
    underlineButton.setOnClickListener(v->underlineSelection(body));
    colorButton.setOnClickListener(v->{int index=0;for(int i=0;i<colors.length;i++)if(colors[i]==noteColor[0])index=i;noteColor[0]=colors[(index+1)%colors.length];body.setBackground(rounded(noteColor[0]==Color.WHITE?cardSurface():noteColor[0],borderColor(),16));body.setTextColor(noteColor[0]==Color.WHITE?foreground():Color.BLACK);scheduleSave(saveAction[0]);});
    checkButton.setOnClickListener(v->{int start=Math.max(0,body.getSelectionStart());body.getText().insert(start,"☐ ");});
    boldButton.setOnClickListener(v->styleSelection(body,Typeface.BOLD));
    italicButton.setOnClickListener(v->styleSelection(body,Typeface.ITALIC));
    if(id>=0){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body,color FROM notes WHERE id=?",new String[]{""+id})){if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(1));noteColor[0]=c.getInt(2);body.setBackground(rounded(noteColor[0]==Color.WHITE?cardSurface():noteColor[0],borderColor(),16));body.setTextColor(noteColor[0]==Color.WHITE?foreground():Color.BLACK);}}}
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),b=body.getText().toString();if(noteId[0]<0&&t.trim().isEmpty()&&b.trim().isEmpty()){setSaveStatus("Nota kosong");return;}
      ContentValues values=new ContentValues();values.put("title",t);values.put("body",b);values.put("updated",System.currentTimeMillis());values.put("color",noteColor[0]);
      try{if(noteId[0]<0){noteId[0]=(int)db.getWritableDatabase().insertOrThrow("notes",null,values);deleteButton[0].setEnabled(true);}else db.getWritableDatabase().update("notes",values,"id=?",new String[]{""+noteId[0]});setSaveStatus("✓ Disimpan");}catch(Exception e){android.util.Log.e("DinNote","Note save failed",e);setSaveStatus("Gagal disimpan");Toast.makeText(this,"Nota gagal disimpan",Toast.LENGTH_LONG).show();}
    };
    saveAction[0]=save;
    TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){scheduleSave(save);}public void afterTextChanged(Editable e){}};
    title.addTextChangedListener(watcher);body.addTextChangedListener(watcher);
    pendingSave=null;
    back.setOnClickListener(v->{flushPendingSave();showHome();});
    title.setContentDescription("Tajuk nota");body.setContentDescription("Kandungan nota Arab dan Rumi");body.setTextColor(noteColor[0]==Color.WHITE?foreground():Color.BLACK);
    Button delete=button();deleteButton[0]=delete;delete.setText("Padam nota");delete.setEnabled(id>=0);delete.setContentDescription(id>=0?"Padam nota":"Simpan nota dahulu sebelum memadam");page.addView(delete);
    delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Padam nota ini?").setNegativeButton("Batal",null).setPositiveButton("Padam",(d,w)->{saveHandler.removeCallbacksAndMessages(null);pendingSave=null;if(noteId[0]>=0)db.getWritableDatabase().delete("notes","id=?",new String[]{""+noteId[0]});showHome();}).show());
  }
  void showLibrary() {
    flushPendingSave();editingBook=false;editingNote=false;saveIndicator=null;
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← DinNote");back.setTextSize(14);page.addView(back);back.setOnClickListener(v->showHome());
    page.addView(text("📚  Maktabah",23));
    Button add=button();add.setText("+ Kategori baharu");page.addView(add);
    add.setOnClickListener(v->promptCategory());
    ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(1);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name FROM categories ORDER BY name COLLATE NOCASE",null)){
      while(c.moveToNext()){int categoryId=c.getInt(0);String name=c.getString(1);
        Button item=button();item.setText("📁  "+name);item.setContentDescription("Buka kategori "+name);items.addView(item);item.setOnClickListener(v->showBooks(categoryId,name));
        item.setOnLongClickListener(v->{new AlertDialog.Builder(this).setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{
          if(which==0)promptRenameCategory(categoryId,name);else new AlertDialog.Builder(this).setMessage("Padam kategori dan semua kitab di dalamnya?").setNegativeButton("Batal",null).setPositiveButton("Padam",(dd,w)->{db.getWritableDatabase().delete("books","category_id=?",new String[]{""+categoryId});db.getWritableDatabase().delete("categories","id=?",new String[]{""+categoryId});showLibrary();}).show();
        }).show();return true;});
      }
    }
    if(items.getChildCount()==0)items.addView(text("Belum ada kategori. Tekan + Kategori baharu untuk mula.",15));
  }
  void promptCategory(){
    EditText input=field();input.setHint("Nama kategori");input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
    new AlertDialog.Builder(this).setTitle("Kategori baharu").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(!name.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",name);db.getWritableDatabase().insert("categories",null,cv);}else Toast.makeText(this,"Masukkan nama kategori",Toast.LENGTH_SHORT).show();showLibrary();
    }).show();
  }
  void promptRenameCategory(int id,String old){
    EditText input=field();input.setText(old);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
    new AlertDialog.Builder(this).setTitle("Ubah nama kategori").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(!name.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",name);db.getWritableDatabase().update("categories",cv,"id=?",new String[]{""+id});}else Toast.makeText(this,"Nama kategori tidak boleh kosong",Toast.LENGTH_SHORT).show();showLibrary();
    }).show();
  }
  void showBooks(int categoryId,String categoryName){
    flushPendingSave();editingBook=false;editingNote=false;saveIndicator=null;activeCategoryId=categoryId;activeCategoryName=categoryName;
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← Maktabah");back.setTextSize(14);page.addView(back);back.setOnClickListener(v->showLibrary());
    page.addView(text("📁  "+categoryName,21));page.setFocusableInTouchMode(true);
    Button add=button();add.setText("+ Tambah kitab");add.setContentDescription("Cipta kitab dalam kategori "+categoryName);page.addView(add);add.setOnClickListener(v->editBook(-1,categoryId,categoryName));
    EditText bookSearch=field();bookSearch.setSingleLine(true);bookSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);bookSearch.setSelectAllOnFocus(false);bookSearch.setHint("Cari kitab dalam kategori…");page.addView(bookSearch);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout items=new LinearLayout(this);items.setOrientation(1);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    bookSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){populateBooks(items,categoryId,categoryName,s.toString());}public void afterTextChanged(Editable e){}});
    populateBooks(items,categoryId,categoryName,"");
  }
  void populateBooks(LinearLayout items,int categoryId,String categoryName,String query){
    if(query==null)query="";items.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title FROM books WHERE category_id=? AND (title LIKE ? OR body LIKE ?) ORDER BY updated DESC,id DESC",
      new String[]{""+categoryId,"%"+query+"%","%"+query+"%"})){
      while(c.moveToNext()){visibleCount++;int bookId=c.getInt(0);String heading=c.getString(1);
        Button item=button();item.setText("📖  "+(heading==null||heading.trim().isEmpty()?"Tanpa tajuk":heading));
        item.setContentDescription("Buka kitab "+(heading==null?"Tanpa tajuk":heading));items.addView(item);item.setOnClickListener(v->editBook(bookId,categoryId,categoryName));
      }
    }
    if(visibleCount==0)items.addView(text(query.trim().isEmpty()?"Belum ada kitab. Tekan + Tambah kitab untuk mula.":"Tiada kitab ditemui.",16));
  }

  void editBook(int id,int categoryId,String categoryName){
    flushPendingSave();editingBook=true;activeCategoryId=categoryId;activeCategoryName=categoryName;
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← Maktabah");page.addView(back);
    saveIndicator=text("Disimpan secara automatik",12);page.addView(saveIndicator);
    EditText title=field();title.setContentDescription("Tajuk kitab");title.setHint("Tajuk kitab");title.setSingleLine(true);title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});title.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);title.setTextSize(18);page.addView(title);
    EditText body=field();body.setHint("Tulis kandungan kitab (Arab / Rumi)…");body.setTextSize(15);
    body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    body.setMinLines(5);body.setSelectAllOnFocus(false);body.setContentDescription("Kandungan kitab");page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    final Button[] deleteButton={null};
    final int[] bookId={id};final boolean[] loading={true};
    if(id>=0)try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body FROM books WHERE id=?",new String[]{""+id})){
      if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(1));}
    }
    LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);
    page.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
    Button check=button();check.setText("☐");toolbar.addView(check,new LinearLayout.LayoutParams(0,-2,1));
    Button bold=button();bold.setText("B");bold.setContentDescription("Tebalkan teks dipilih");bold.setTypeface(null,Typeface.BOLD);toolbar.addView(bold,new LinearLayout.LayoutParams(0,-2,1));
    Button italic=button();italic.setText("I");italic.setContentDescription("Condongkan teks dipilih");italic.setTypeface(null,Typeface.ITALIC);toolbar.addView(italic,new LinearLayout.LayoutParams(0,-2,1));
    Button underline=button();underline.setText("U");underline.setContentDescription("Gariskan teks dipilih");toolbar.addView(underline,new LinearLayout.LayoutParams(0,-2,1));
    underline.setOnClickListener(v->underlineSelection(body));
    check.setOnClickListener(v->body.getText().insert(Math.max(0,body.getSelectionStart()),"☐ "));
    bold.setOnClickListener(v->styleSelection(body,Typeface.BOLD));
    italic.setOnClickListener(v->styleSelection(body,Typeface.ITALIC));
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),content=body.getText().toString();
      if(bookId[0]<0&&t.trim().isEmpty()&&content.trim().isEmpty()){setSaveStatus("Kitab kosong");return;}
      ContentValues cv=new ContentValues();cv.put("title",t);cv.put("body",content);
      cv.put("category_id",categoryId);cv.put("updated",System.currentTimeMillis());
      try{if(bookId[0]<0){bookId[0]=(int)db.getWritableDatabase().insertOrThrow("books",null,cv);deleteButton[0].setEnabled(true);}
      else db.getWritableDatabase().update("books",cv,"id=?",new String[]{""+bookId[0]});setSaveStatus("✓ Disimpan");}catch(Exception e){android.util.Log.e("DinNote","Book save failed",e);setSaveStatus("Gagal disimpan");Toast.makeText(this,"Kitab gagal disimpan",Toast.LENGTH_LONG).show();}
    };
    TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}
      public void onTextChanged(CharSequence s,int st,int before,int count){scheduleSave(save);}
      public void afterTextChanged(Editable e){}};
    title.addTextChangedListener(watcher);body.addTextChangedListener(watcher);
    pendingSave=null;
    back.setOnClickListener(v->{flushPendingSave();showBooks(categoryId,categoryName);});
    Button delete=button();deleteButton[0]=delete;delete.setText("Padam kitab");delete.setEnabled(id>=0);delete.setContentDescription(id>=0?"Padam kitab":"Simpan kitab dahulu sebelum memadam");page.addView(delete);
    delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Padam kitab ini?")
      .setNegativeButton("Batal",null).setPositiveButton("Padam",(d,w)->{
        saveHandler.removeCallbacksAndMessages(null);pendingSave=null;if(bookId[0]>=0)db.getWritableDatabase().delete("books","id=?",new String[]{""+bookId[0]});
        showBooks(categoryId,categoryName);
      }).show());
  }
  void saveColor(int id,int color){if(id<0)return;ContentValues v=new ContentValues();v.put("color",color);db.getWritableDatabase().update("notes",v,"id=?",new String[]{""+id});}
  void underlineSelection(EditText edit){int a=edit.getSelectionStart(),b=edit.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}edit.getText().setSpan(new android.text.style.UnderlineSpan(),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}
  void styleSelection(EditText edit,int style){int a=edit.getSelectionStart(),b=edit.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}SpannableStringBuilder t=new SpannableStringBuilder(edit.getText());t.setSpan(new StyleSpan(style),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);edit.setText(t);edit.setSelection(b);}
  @Override public void onBackPressed(){flushPendingSave();if(editingBook)showBooks(activeCategoryId,activeCategoryName);else showHome();}
  static class DB extends SQLiteOpenHelper {
    DB(Context c){super(c,"dinnote.db",null,3);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL,color INTEGER NOT NULL DEFAULT -1)");createLibrary(db);}
    static void createLibrary(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY AUTOINCREMENT,category_id INTEGER NOT NULL,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL)");}
    @Override public void onOpen(SQLiteDatabase database){super.onOpen(database);createLibrary(database);}
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2) { try { db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT -1"); } catch (android.database.sqlite.SQLiteException ignored) { /* Column may already exist in older builds. */ } } if(oldVersion<3)createLibrary(db); }
  }
}
