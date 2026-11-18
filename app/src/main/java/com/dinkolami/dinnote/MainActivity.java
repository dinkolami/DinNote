package com.dinkolami.dinnote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.*;
import android.text.style.*;
import android.text.Html;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(26,112,85);
    private Store store;
    private LinearLayout root, body, toolbar;
    private EditText titleEdit, contentEdit;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSave;
    private long currentNote = -1, currentNotebook = 1;
    private boolean loading = false;
    private String filter = "";
    private boolean showingEditor = false;
    private boolean focusMode = false;
    private static final int SURFACE = Color.rgb(249,250,247);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(Color.BLACK);
        store = new Store(this);
        store.ensureDefaultNotebook();
        showHome();
    }
    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(Color.rgb(37,49,45));
        v.setPadding(dp(12),dp(12),dp(12),dp(12));
        if(bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private TextView action(String label, Runnable click) {
        TextView v = text(label,15,true);
        v.setTextColor(GREEN);
        v.setOnClickListener(w -> click.run()); return v;
    }
    private void base(String heading) {
        root = column(); root.setBackgroundColor(SURFACE);
        setContentView(root);
        LinearLayout top=row(); top.setBackgroundColor(Color.WHITE);
        TextView name=text(heading,22,true); name.setTextColor(GREEN); name.setLetterSpacing(.02f);
        top.addView(name,new LinearLayout.LayoutParams(0,dp(58),1));
        root.addView(top);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        body=column(); body.setPadding(dp(14),dp(10),dp(14),dp(16));
        scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void showHome() {
        showingEditor=false; currentNote=-1;
        base("DinNote");
        LinearLayout nav=row();
        nav.addView(action("Notebook",this::chooseNotebook));
        nav.addView(action("  + Notebook",this::createNotebook));
        nav.addView(action("  + Nota",()->openNote(-1)));
        body.addView(nav);
        EditText search=new EditText(this); search.setSingleLine(true);
        search.setTextSize(14); search.setHint("Cari semua nota...");
        search.setText(filter); search.setSingleLine(true); body.addView(search);
        LinearLayout results=column(); body.addView(results);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){filter=s.toString(); renderNotes(results);}
            public void afterTextChanged(Editable e){}
        });
        renderNotes(results);
    }
    private void renderNotes(LinearLayout results) {
        results.removeAllViews();
        Cursor c=store.getReadableDatabase().rawQuery(
            "SELECT n.id,n.title,n.content,b.name,n.updated FROM notes n JOIN notebooks b ON b.id=n.notebook_id WHERE n.title LIKE ? OR n.content LIKE ? ORDER BY n.updated DESC",
            new String[]{"%"+filter+"%","%"+filter+"%"});
        try {
            while(c.moveToNext()) {
                final long id=c.getLong(0);
                String title=c.getString(1), content=c.getString(2), notebook=c.getString(3);
                LinearLayout card=column(); card.setPadding(dp(8),dp(6),dp(8),dp(6));
                card.setBackgroundColor(Color.WHITE); card.setElevation(dp(1));
                TextView heading=text(title.isEmpty()?"Nota tanpa tajuk":title,17,true);
                TextView preview=text(notebook+"  ·  "+Html.fromHtml(content,Html.FROM_HTML_MODE_COMPACT).toString().replace('\n',' ').trim(),13,false);
                preview.setMaxLines(2); preview.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(heading); card.addView(preview);
                card.setOnClickListener(v->openNote(id));
                results.addView(card,new LinearLayout.LayoutParams(-1,-2));
                View line=new View(this); line.setBackgroundColor(Color.rgb(230,235,231));
                results.addView(line,new LinearLayout.LayoutParams(-1,dp(6)));
            }
        } finally {c.close();}
    }
    private void chooseNotebook() {
        Cursor c=store.getReadableDatabase().rawQuery("SELECT id,name FROM notebooks ORDER BY name",null);
        ArrayList<Long> ids=new ArrayList<>(); ArrayList<String> names=new ArrayList<>();
        try {while(c.moveToNext()){ids.add(c.getLong(0));names.add(c.getString(1));}}finally{c.close();}
        new AlertDialog.Builder(this).setTitle("Notebook").setItems(names.toArray(new String[0]),(d,which)->{
            currentNotebook=ids.get(which); showNotebook(ids.get(which),names.get(which));
        }).setPositiveButton("+ Baharu",(d,w)->createNotebook()).setNegativeButton("Tutup",null).show();
    }
    private void createNotebook() {
        EditText input=new EditText(this); input.setSingleLine(true); input.setHint("Nama notebook");
        new AlertDialog.Builder(this).setTitle("Notebook baharu").setView(input)
            .setPositiveButton("Simpan",(d,w)->{
                String name=input.getText().toString().trim();
                if(!name.isEmpty()){
                    android.content.ContentValues cv=new android.content.ContentValues(); cv.put("name",name);
                    try {currentNotebook=store.getWritableDatabase().insertOrThrow("notebooks",null,cv);showHome();}
                    catch(Exception ex){Toast.makeText(this,"Nama notebook sudah wujud",Toast.LENGTH_SHORT).show();}
                }
            }).setNegativeButton("Batal",null).show();
    }
    private void showNotebook(long id,String name) {
        base(name);
        body.addView(action("← Semua nota",this::showHome));
        body.addView(action("+ Nota dalam notebook",()->{currentNotebook=id;openNote(-1);}));
        body.addView(action("Namakan semula",()->renameNotebook(id,name)));
        body.addView(action("Padam notebook",()->new AlertDialog.Builder(this).setMessage("Padam notebook dan semua notanya?")
            .setPositiveButton("Padam",(d,w)->{store.getWritableDatabase().delete("notebooks","id=?",new String[]{""+id});currentNotebook=1;showHome();})
            .setNegativeButton("Batal",null).show()));
        Cursor c=store.getReadableDatabase().rawQuery("SELECT id,title FROM notes WHERE notebook_id=? ORDER BY updated DESC",new String[]{""+id});
        try {while(c.moveToNext()){long noteId=c.getLong(0);body.addView(action("✎ "+c.getString(1),()->openNote(noteId)));}}finally{c.close();}
    }
    private void renameNotebook(long id,String old) {
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(old);
        new AlertDialog.Builder(this).setTitle("Namakan semula").setView(input).setPositiveButton("Simpan",(d,w)->{
            android.content.ContentValues cv=new android.content.ContentValues();cv.put("name",input.getText().toString().trim());
            try {store.getWritableDatabase().update("notebooks",cv,"id=?",new String[]{""+id});showNotebook(id,input.getText().toString());}
            catch(Exception ex){Toast.makeText(this,"Nama sudah digunakan",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("Batal",null).show();
    }
    private void openNote(long id) {
        showingEditor=true; loading=true;currentNote=id;
        base("Editor DinNote");
        LinearLayout actions=row();
        actions.addView(action("← Kembali",()->{saveNow();showHome();}));
        actions.addView(action("  Notebook",this::moveNote));
        actions.addView(action("  Padam",this::deleteNote));
        body.addView(actions);
        titleEdit=new EditText(this);titleEdit.setHint("Tajuk nota");titleEdit.setSingleLine(true);
        titleEdit.setTextSize(21);body.addView(titleEdit);
        toolbar=row(); HorizontalScrollView tools=new HorizontalScrollView(this);tools.setHorizontalScrollBarEnabled(false);
        tools.addView(toolbar);body.addView(tools);
        toolbar.addView(action("B",()->toggleStyle(Typeface.BOLD)));
        toolbar.addView(action("I",()->toggleStyle(Typeface.ITALIC)));
        toolbar.addView(action("U̲",()->applySpan(new UnderlineSpan())));
        toolbar.addView(action("Warna",()->applySpan(new ForegroundColorSpan(GREEN))));
        toolbar.addView(action("Highlight",()->applySpan(new BackgroundColorSpan(Color.YELLOW))));
        toolbar.addView(action("☑",()->insertChecklist()));
        toolbar.addView(action("Cari/Ganti",this::findReplace));
        toolbar.addView(action("Baca",this::readingMode));
        toolbar.addView(action("Fokus",this::focusWriting));
        toolbar.addView(action("Kongsi",this::shareNote));
        toolbar.addView(action("A+",()->resizeText(2)));
        toolbar.addView(action("A−",()->resizeText(-2)));
        toolbar.addView(action("Arab ⇄",()->{
            contentEdit.setTextDirection(contentEdit.getTextDirection()==View.TEXT_DIRECTION_RTL?View.TEXT_DIRECTION_LTR:View.TEXT_DIRECTION_RTL);
        }));
        contentEdit=new EditText(this);contentEdit.setGravity(Gravity.TOP);
        contentEdit.setTextSize(17);contentEdit.setHint("Mula menulis nota di sini...");
        contentEdit.setMinLines(14);contentEdit.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        body.addView(contentEdit,new LinearLayout.LayoutParams(-1,-2));
        if(id>0){
            Cursor c=store.getReadableDatabase().rawQuery("SELECT title,content,notebook_id FROM notes WHERE id=?",new String[]{""+id});
            try {if(c.moveToFirst()){
                titleEdit.setText(c.getString(0));
                contentEdit.setText(Html.fromHtml(c.getString(1),Html.FROM_HTML_MODE_COMPACT));
                currentNotebook=c.getLong(2);
            }}finally{c.close();}
        }
        TextWatcher watcher=new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){scheduleSave();}
            public void afterTextChanged(Editable e){}
        };
        titleEdit.addTextChangedListener(watcher);contentEdit.addTextChangedListener(watcher);
        loading=false;
    }
    private void resizeText(int delta) {
        if(contentEdit==null)return;
        contentEdit.setTextSize(Math.max(12,Math.min(32,contentEdit.getTextSize()/getResources().getDisplayMetrics().scaledDensity+delta)));
    }
    private void focusWriting() {
        focusMode=!focusMode;
        toolbar.setVisibility(focusMode?View.GONE:View.VISIBLE);
        Toast.makeText(this,focusMode?"Mod fokus aktif":"Mod fokus tamat",Toast.LENGTH_SHORT).show();
    }
    private void readingMode() {
        saveNow();
        ScrollView scroll=new ScrollView(this);
        TextView reading=text("",19,false);
        reading.setTextIsSelectable(true);
        reading.setText(Html.fromHtml(Html.toHtml(contentEdit.getText(),Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE),Html.FROM_HTML_MODE_COMPACT));
        reading.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        scroll.addView(reading);
        new AlertDialog.Builder(this).setTitle(titleEdit.getText().toString()).setView(scroll)
            .setPositiveButton("Kembali menulis",null).show();
    }
    private void shareNote() {
        saveNow();
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT,titleEdit.getText().toString());
        send.putExtra(Intent.EXTRA_TEXT,titleEdit.getText().toString()+"\\n\\n"+contentEdit.getText().toString());
        startActivity(Intent.createChooser(send,"Kongsi nota"));
    }
    private void findReplace() {
        LinearLayout fields=column();
        EditText find=new EditText(this);find.setSingleLine(true);find.setHint("Cari perkataan");
        EditText replace=new EditText(this);replace.setSingleLine(true);replace.setHint("Ganti dengan");
        fields.addView(find);fields.addView(replace);
        new AlertDialog.Builder(this).setTitle("Cari dan ganti").setView(fields)
            .setPositiveButton("Ganti semua",(d,w)->{
                String needle=find.getText().toString();
                if(needle.isEmpty())return;
                Editable e=contentEdit.getText();
                String value=e.toString();
                int from=0,found=0;
                while((from=value.indexOf(needle,from))>=0){found++;from+=needle.length();}
                if(found>0){
                    for(int i=e.length()-needle.length();i>=0;i--){
                        if(i+needle.length()<=e.length() && e.subSequence(i,i+needle.length()).toString().equals(needle)){
                            e.replace(i,i+needle.length(),replace.getText().toString());
                        }
                    }
                    scheduleSave();
                }
                Toast.makeText(this,found+" padanan diganti",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Batal",null).show();
    }
    private void toggleStyle(int style) {applySpan(new StyleSpan(style));}
    private void applySpan(Object span) {
        if(contentEdit==null)return;
        int a=contentEdit.getSelectionStart(),b=contentEdit.getSelectionEnd();
        if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}
        contentEdit.getText().setSpan(span,Math.min(a,b),Math.max(a,b),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        scheduleSave();
    }
    private void insertChecklist() {
        int pos=Math.max(0,contentEdit.getSelectionStart());
        contentEdit.getText().insert(pos,"☐ ");
    }
    private void moveNote() {
        Cursor c=store.getReadableDatabase().rawQuery("SELECT id,name FROM notebooks ORDER BY name",null);
        ArrayList<Long> ids=new ArrayList<>();ArrayList<String> names=new ArrayList<>();
        try{while(c.moveToNext()){ids.add(c.getLong(0));names.add(c.getString(1));}}finally{c.close();}
        new AlertDialog.Builder(this).setTitle("Pilih notebook").setItems(names.toArray(new String[0]),(d,w)->{
            currentNotebook=ids.get(w);scheduleSave();
        }).show();
    }
    private void deleteNote() {
        if(currentNote<0){showHome();return;}
        new AlertDialog.Builder(this).setMessage("Padam nota ini?")
            .setPositiveButton("Padam",(d,w)->{
                if(pendingSave!=null)handler.removeCallbacks(pendingSave);
                store.getWritableDatabase().delete("notes","id=?",new String[]{""+currentNote});
                currentNote=-1;showHome();
            }).setNegativeButton("Batal",null).show();
    }
    private void scheduleSave() {
        if(loading||!showingEditor)return;
        if(pendingSave!=null)handler.removeCallbacks(pendingSave);
        pendingSave=this::saveNow;handler.postDelayed(pendingSave,500);
    }
    private void saveNow() {
        if(!showingEditor||titleEdit==null||contentEdit==null)return;
        if(pendingSave!=null)handler.removeCallbacks(pendingSave);
        String title=titleEdit.getText().toString();
        String content=Html.toHtml(contentEdit.getText(),Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        if(currentNote<0&&title.trim().isEmpty()&&contentEdit.getText().toString().trim().isEmpty())return;
        android.content.ContentValues cv=new android.content.ContentValues();
        cv.put("title",title);cv.put("content",content);cv.put("notebook_id",currentNotebook);
        cv.put("updated",System.currentTimeMillis());
        if(currentNote<0)currentNote=store.getWritableDatabase().insert("notes",null,cv);
        else store.getWritableDatabase().update("notes",cv,"id=?",new String[]{""+currentNote});
    }
    @Override protected void onPause(){saveNow();super.onPause();}
    @Override public void onBackPressed(){if(showingEditor){saveNow();showHome();}else super.onBackPressed();}
    static class Store extends SQLiteOpenHelper {
        Store(Context c){super(c,"dinnote.db",null,1);}
        @Override public void onCreate(SQLiteDatabase db){
            db.execSQL("CREATE TABLE notebooks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE)");
            db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,notebook_id INTEGER NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE,title TEXT NOT NULL DEFAULT '',content TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL)");
            db.execSQL("INSERT INTO notebooks(id,name) VALUES(1,'Nota Saya')");
        }
        @Override public void onUpgrade(SQLiteDatabase db,int old,int next){}
        void ensureDefaultNotebook(){
            SQLiteDatabase db=getWritableDatabase();
            db.execSQL("PRAGMA foreign_keys=ON");
            db.execSQL("INSERT OR IGNORE INTO notebooks(id,name) VALUES(1,'Nota Saya')");
        }
        @Override public void onConfigure(SQLiteDatabase db){super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);}
    }
}
