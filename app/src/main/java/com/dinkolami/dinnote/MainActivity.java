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
  void applyAppearance(){
    // Theme only the app content. Android owns the top status bar and bottom navigation bar.
    background=surface();
  }
  void showSettings(){new AlertDialog.Builder(this).setTitle("Tetapan DinNote").setItems(new String[]{"Penampilan","Backup ke DinPhone","Restore dari fail"},(d,w)->{if(w==0)appearanceSettings();else if(w==1)exportBackup();else importBackup();}).setNegativeButton("Tutup",null).show();}
  static final int REQUEST_EXPORT=7301,REQUEST_IMPORT=7302;
  void exportBackup(){try{flushPendingSave();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"DinPhone-DinNote-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmm",java.util.Locale.US).format(new java.util.Date())+".db");startActivityForResult(i,REQUEST_EXPORT);}catch(Exception e){Toast.makeText(this,"Tidak dapat membuka pilihan backup",Toast.LENGTH_LONG).show();}}
  void importBackup(){new AlertDialog.Builder(this).setTitle("Restore DinNote").setMessage("Restore akan menggantikan semua nota, kategori dan kitab dalam aplikasi. Pastikan backup terkini telah dibuat. Teruskan?").setNegativeButton("Batal",null).setPositiveButton("Pilih fail",(d,w)->{try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,REQUEST_IMPORT);}catch(Exception e){Toast.makeText(this,"Tidak dapat membuka fail",Toast.LENGTH_LONG).show();}}).show();}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;try{android.net.Uri uri=data.getData();if(request==REQUEST_EXPORT){flushPendingSave();db.getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)",null).close();try(java.io.InputStream in=new java.io.FileInputStream(getDatabasePath("dinnote.db"));java.io.OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new java.io.IOException("No output stream");copyBytes(in,out);}Toast.makeText(this,"Backup DinPhone berjaya",Toast.LENGTH_LONG).show();}else if(request==REQUEST_IMPORT){java.io.File temp=new java.io.File(getCacheDir(),"restore-dinnote.db");try(java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.OutputStream out=new java.io.FileOutputStream(temp)){if(in==null)throw new java.io.IOException("No input stream");copyBytes(in,out);}try(SQLiteDatabase check=SQLiteDatabase.openDatabase(temp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)){for(String table:new String[]{"notes","categories","books"}){try(Cursor c=check.rawQuery("SELECT COUNT(*) FROM "+table,null)){if(!c.moveToFirst())throw new java.io.IOException("Invalid backup");}}}flushPendingSave();db.close();java.io.File target=getDatabasePath("dinnote.db");java.io.File parent=target.getParentFile();if(parent!=null)parent.mkdirs();java.io.File old=new java.io.File(target.getAbsolutePath()+".before-restore");if(old.exists())old.delete();if(target.exists()&&!target.renameTo(old))throw new java.io.IOException("Cannot protect existing database");try(java.io.InputStream in=new java.io.FileInputStream(temp);java.io.OutputStream out=new java.io.FileOutputStream(target)){copyBytes(in,out);}db=new DB(this);db.getWritableDatabase();Toast.makeText(this,"Restore berjaya",Toast.LENGTH_LONG).show();showHome();temp.delete();}}catch(Exception e){android.util.Log.e("DinNote","Backup or restore failed",e);Toast.makeText(this,"Operasi gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
  static void copyBytes(java.io.InputStream in,java.io.OutputStream out)throws java.io.IOException{byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);out.flush();}
  void appearanceSettings(){
    new AlertDialog.Builder(this).setTitle("Penampilan")
      .setSingleChoiceItems(new String[]{"Ikut sistem","Cerah","Gelap"},themeMode,(dialog,which)->{
        themeMode=which;getPreferences(MODE_PRIVATE).edit().putInt("appearance",which).apply();
        dialog.dismiss();applyAppearance();showHome();
      }).setNegativeButton("Batal",null).show();
  }
  String noteFilter="";
  int selectedNoteCategory=-1; // -1 means all categories
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
    TextView appearance=text("⚙",22);appearance.setContentDescription("Tetapan");appearance.setFocusable(true);appearance.setOnClickListener(v->showSettings());header.addView(appearance);
    View headerDivider=new View(this);headerDivider.setBackgroundColor(borderColor());LinearLayout.LayoutParams hd=new LinearLayout.LayoutParams(-1,1);hd.setMargins(0,4,0,12);root.addView(headerDivider,hd);
    Button notesMenu=button();notesMenu.setText("📝  Nota");notesMenu.setTypeface(null,Typeface.BOLD);root.addView(notesMenu);
    Button libraryMenu=button();libraryMenu.setText("📚  Maktabah");libraryMenu.setTypeface(null,Typeface.BOLD);root.addView(libraryMenu);
    libraryMenu.setContentDescription("Buka Maktabah");libraryMenu.setOnClickListener(v->showLibrary());
    notesMenu.setContentDescription("Buka Nota");notesMenu.setOnClickListener(v->showHome());
    Button categoriesButton=button();categoriesButton.setText("Kategori Nota  •  Tambah / Ubah / Padam");root.addView(categoriesButton);
    categoriesButton.setOnClickListener(v->showNoteCategories());
    Button sort=button();sort.setText("Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[noteSort]);root.addView(sort);
    sort.setContentDescription("Pilih susunan nota");sort.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Terkini","Terlama","Tajuk A-Z"},(d,w)->{noteSort=w;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",w).apply();sort.setText("Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[w]);refresh(noteFilter);}).show());
    search=field();search.setContentDescription("Cari nota");search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setSelectAllOnFocus(false);search.setHint("Cari nota…");search.setText(noteFilter);search.setSelection(search.length());root.addView(search);
    Button add=button();add.setText("+ Nota baharu");add.setContentDescription("Cipta nota baharu");root.addView(add);add.setOnClickListener(v->edit(-1));
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){noteFilter=s.toString();refresh(noteFilter);}public void afterTextChanged(Editable e){}});
    try{refresh(noteFilter);}catch(Exception e){android.util.Log.e("DinNote","Failed loading notes",e);Toast.makeText(this,"Gagal memuatkan nota",Toast.LENGTH_LONG).show();}
    // In-app navigator sits ABOVE Android's own gesture/button navigation area.
    LinearLayout navigator=new LinearLayout(this);
    navigator.setOrientation(LinearLayout.HORIZONTAL);
    navigator.setGravity(Gravity.CENTER_VERTICAL);
    navigator.setBackgroundColor(cardSurface());
    Button homeNav=button();homeNav.setText("⌂ Utama");
    Button noteNav=button();noteNav.setText("▤ Nota");
    Button bookNav=button();bookNav.setText("▥ Maktabah");
    navigator.addView(homeNav,new LinearLayout.LayoutParams(0,-2,1));
    navigator.addView(noteNav,new LinearLayout.LayoutParams(0,-2,1));
    navigator.addView(bookNav,new LinearLayout.LayoutParams(0,-2,1));
    root.addView(navigator,new LinearLayout.LayoutParams(-1,-2));
    homeNav.setOnClickListener(v->showHome());
    noteNav.setOnClickListener(v->{search.requestFocus();});
    bookNav.setOnClickListener(v->showLibrary());
  }
  String noteCategoryName(int id){
    if(id<0)return "Tanpa kategori";
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT name FROM note_categories WHERE id=?",new String[]{""+id})){if(c.moveToFirst())return c.getString(0);}
    return "Tanpa kategori";
  }
  void showNoteCategories(){
    flushPendingSave();
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← Nota");page.addView(back);back.setOnClickListener(v->showHome());
    page.addView(text("Kategori Nota",22));
    Button all=button();all.setText("Semua nota");page.addView(all);all.setOnClickListener(v->{selectedNoteCategory=-1;showHome();});
    Button add=button();add.setText("+ Tambah kategori");page.addView(add);add.setOnClickListener(v->promptNoteCategory(-1,""));
    ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(1);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name FROM note_categories ORDER BY name COLLATE NOCASE",null)){
      while(c.moveToNext()){
        final int id=c.getInt(0);final String name=c.getString(1);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);items.addView(row);
        Button open=button();open.setText("📁 "+name);row.addView(open,new LinearLayout.LayoutParams(0,-2,1));
        open.setOnClickListener(v->{selectedNoteCategory=id;showHome();});
        Button rename=button();rename.setText("Edit");row.addView(rename,new LinearLayout.LayoutParams(-2,-2));
        rename.setOnClickListener(v->promptNoteCategory(id,name));
        Button delete=button();delete.setText("Del");row.addView(delete,new LinearLayout.LayoutParams(-2,-2));
        delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Padam kategori?")
          .setMessage("Nota dalam kategori ini tidak dipadam. Nota akan dipindahkan ke Tanpa kategori.")
          .setNegativeButton("Batal",null).setPositiveButton("Padam",(d,w)->{
            SQLiteDatabase writable=db.getWritableDatabase();writable.beginTransaction();
            try{ContentValues cv=new ContentValues();cv.put("category_id",-1);writable.update("notes",cv,"category_id=?",new String[]{""+id});writable.delete("note_categories","id=?",new String[]{""+id});writable.setTransactionSuccessful();}finally{writable.endTransaction();}
            if(selectedNoteCategory==id)selectedNoteCategory=-1;showNoteCategories();
          }).show());
      }
    }
  }
  void promptNoteCategory(int id,String existing){
    EditText input=field();input.setSingleLine(true);input.setText(existing);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
    new AlertDialog.Builder(this).setTitle(id<0?"Tambah kategori":"Ubah kategori").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(name.isEmpty()){Toast.makeText(this,"Nama kategori diperlukan",Toast.LENGTH_SHORT).show();return;}
      ContentValues cv=new ContentValues();cv.put("name",name);
      if(id<0)db.getWritableDatabase().insert("note_categories",null,cv);else db.getWritableDatabase().update("note_categories",cv,"id=?",new String[]{""+id});
      showNoteCategories();
    }).show();
  }
  void refresh(String query) {
    if(query==null)query="";
    if(list==null||db==null)return;
    list.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,body,color FROM notes WHERE (title LIKE ? OR body LIKE ?) AND (?=-1 OR category_id=?) ORDER BY "+(noteSort==1?"updated ASC,id ASC":noteSort==2?"title COLLATE NOCASE ASC,id DESC":"updated DESC,id DESC"),new String[]{"%"+query+"%","%"+query+"%",""+selectedNoteCategory,""+selectedNoteCategory})) {
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
    final int[] chosenCategory={-1};
    if(id>=0){try(Cursor cat=db.getReadableDatabase().rawQuery("SELECT category_id FROM notes WHERE id=?",new String[]{""+id})){if(cat.moveToFirst())chosenCategory[0]=cat.getInt(0);}}
    else if(selectedNoteCategory>=0)chosenCategory[0]=selectedNoteCategory;
    final Runnable[] saveActionHolder={null};
    Button categoryPicker=button();categoryPicker.setText("Kategori: "+noteCategoryName(chosenCategory[0]));page.addView(categoryPicker);
    categoryPicker.setOnClickListener(v->{ArrayList<Integer> ids=new ArrayList<>();ArrayList<String> names=new ArrayList<>();ids.add(-1);names.add("Tanpa kategori");try(Cursor cats=db.getReadableDatabase().rawQuery("SELECT id,name FROM note_categories ORDER BY name COLLATE NOCASE",null)){while(cats.moveToNext()){ids.add(cats.getInt(0));names.add(cats.getString(1));}}new AlertDialog.Builder(this).setTitle("Pilih kategori").setItems(names.toArray(new String[0]),(dialog,which)->{chosenCategory[0]=ids.get(which);categoryPicker.setText("Kategori: "+noteCategoryName(chosenCategory[0]));if(saveActionHolder[0]!=null)scheduleSave(saveActionHolder[0]);}).show();});
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
      ContentValues values=new ContentValues();values.put("title",t);values.put("body",b);values.put("updated",System.currentTimeMillis());values.put("color",noteColor[0]);values.put("category_id",chosenCategory[0]);
      try{if(noteId[0]<0){noteId[0]=(int)db.getWritableDatabase().insertOrThrow("notes",null,values);deleteButton[0].setEnabled(true);}else db.getWritableDatabase().update("notes",values,"id=?",new String[]{""+noteId[0]});setSaveStatus("✓ Disimpan");}catch(Exception e){android.util.Log.e("DinNote","Note save failed",e);setSaveStatus("Gagal disimpan");Toast.makeText(this,"Nota gagal disimpan",Toast.LENGTH_LONG).show();}
    };
    saveAction[0]=save;saveActionHolder[0]=save;
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
    DB(Context c){super(c,"dinnote.db",null,5);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL,color INTEGER NOT NULL DEFAULT -1,category_id INTEGER NOT NULL DEFAULT -1)");createLibrary(db);createNoteCategories(db);seedDefaultCategories(db);}
    static void createLibrary(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY AUTOINCREMENT,category_id INTEGER NOT NULL,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL)");}
    static void createNoteCategories(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS note_categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");}
    static void seedDefaultCategories(SQLiteDatabase db){
      // Only seed during database creation or version migration; user changes remain editable.
      for(String name:new String[]{"Al-Quran & Tafsir","Hadis","Akidah & Tauhid","Fiqh","Tasawuf & Akhlak","Sirah & Sejarah Islam","Bahasa Arab","Lain-lain"}){
        db.execSQL("INSERT INTO categories(name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name=?)",new Object[]{name,name});
      }
      db.execSQL("INSERT INTO note_categories(name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM note_categories WHERE name=?)",new Object[]{"Peribadi","Peribadi"});
    }
    @Override public void onOpen(SQLiteDatabase database){super.onOpen(database);createLibrary(database);createNoteCategories(database);}
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2) { try { db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT -1"); } catch (android.database.sqlite.SQLiteException ignored) { /* Column may already exist in older builds. */ } } if(oldVersion<3)createLibrary(db);if(oldVersion<4){createNoteCategories(db);try{db.execSQL("ALTER TABLE notes ADD COLUMN category_id INTEGER NOT NULL DEFAULT -1");}catch(android.database.sqlite.SQLiteException ignored){}} if(oldVersion<5){createLibrary(db);createNoteCategories(db);seedDefaultCategories(db);} }
  }
}
