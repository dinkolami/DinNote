package com.dinkolami.dinnote;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import android.text.*;import android.content.*;import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root,items; android.view.GestureDetector readerDetector; EditText editor,titleEditor; Spinner categoryPicker; String categoryFilter="Semua"; String selected=null; boolean maktabah=false, sectionOpen=false; android.content.SharedPreferences prefs;
 int green=Color.rgb(24,39,35), gold=Color.rgb(185,143,53);
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("notes",0);seedDefaultCategories();home();}
 void applySystemBarInsets(LinearLayout view){
  getWindow().setStatusBarColor(0xff125b42);
  getWindow().setNavigationBarColor(0xfffafaf7);
  if(android.os.Build.VERSION.SDK_INT>=30){
   getWindow().setDecorFitsSystemWindows(false);
  }
  view.setOnApplyWindowInsetsListener((v,insets)->{
   if(android.os.Build.VERSION.SDK_INT>=30){
    android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
    v.setPadding(0,bars.top,0,bars.bottom);
   }else{
    v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());
   }
   return insets;
  });
  view.requestApplyInsets();
 }
 TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(green);t.setPadding(24,20,24,20);return t;}

 void base(String title){
  readerDetector=null;
  root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.WHITE);
  setContentView(root);applySystemBarInsets(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  header.setPadding(dp(8),dp(4),dp(8),dp(4));header.setBackgroundColor(0xfff6f8fa);
  ImageView menu=headerIcon("☰",0xff102b25,"Buka menu DinNote");header.addView(menu);
  menu.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Halaman utama","Nota","Maktabah","Kategori"},(d,i)->{if(i==0)home();else if(i==3)manageCategories();else openSection(i==2);}).show());
  ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.dinnote_header_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Logo DinNote");logo.setClickable(true);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(40),dp(40));lp.leftMargin=dp(7);lp.rightMargin=dp(5);header.addView(logo,lp);
  logo.setOnClickListener(v->home());
  TextView name=text("DinNote",18);name.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.NORMAL);name.setTextColor(0xff123e2b);
  name.setGravity(Gravity.BOTTOM);name.setPadding(dp(3),0,0,dp(7));header.addView(name,new LinearLayout.LayoutParams(0,dp(40),1));
  name.setOnClickListener(v->home());
  ImageView searchIcon=headerIcon("⌕",0xff102b25,"Cari nota");header.addView(searchIcon);
  searchIcon.setOnClickListener(v->searchNotesDialog());
  ImageView inbox=headerIcon("▣",0xff102b25,"Buka nota");header.addView(inbox);
  inbox.setOnClickListener(v->openSection(false));
  ImageView bell=headerIcon("♧",0xff126b43,"Peringatan nota");header.addView(bell);
  bell.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Peringatan").setMessage("Peringatan nota belum tersedia.").setPositiveButton("OK",null).show());
  ImageView avatar=headerIcon("☪",0xff126b43,"Urus kategori");header.addView(avatar);
  avatar.setOnClickListener(v->manageCategories());
  root.addView(header,new LinearLayout.LayoutParams(-1,dp(62)));
  View border=new View(this);border.setBackgroundColor(0xffd0d7de);
  root.addView(border,new LinearLayout.LayoutParams(-1,dp(1)));
 }
 ImageView headerIcon(String symbol,int color,String description){
  int px=dp(34);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(px,px,android.graphics.Bitmap.Config.ARGB_8888);
  android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
  android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  paint.setColor(color);paint.setTextSize(dp(21));paint.setTypeface(android.graphics.Typeface.DEFAULT);
  paint.setTextAlign(android.graphics.Paint.Align.CENTER);
  android.graphics.Paint.FontMetrics fm=paint.getFontMetrics();
  canvas.drawText(symbol,px/2f,px/2f-(fm.ascent+fm.descent)/2f,paint);
  ImageView view=new ImageView(this);view.setImageBitmap(bitmap);view.setContentDescription(description);
  view.setClickable(true);view.setFocusable(true);view.setScaleType(ImageView.ScaleType.FIT_CENTER);
  android.graphics.drawable.GradientDrawable frame=new android.graphics.drawable.GradientDrawable();frame.setColor(0xffffffff);frame.setCornerRadius(dp(10));frame.setStroke(dp(1),0xffaeb9bb);view.setBackground(frame);
  view.setPadding(dp(5),dp(5),dp(5),dp(5));
  LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(34),dp(34));iconParams.leftMargin=dp(3);iconParams.rightMargin=dp(3);view.setLayoutParams(iconParams);
  return view;
 }
 void searchNotesDialog(){
  final EditText field=new EditText(this);field.setSingleLine(true);field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);field.setHint("Cari tajuk atau kandungan nota");field.setContentDescription("Carian tajuk dan kandungan");
  LinearLayout wrapper=new LinearLayout(this);wrapper.setPadding(dp(20),dp(6),dp(20),0);wrapper.addView(field,new LinearLayout.LayoutParams(-1,-2));
  new AlertDialog.Builder(this).setTitle("Carian DinNote").setView(wrapper)
   .setPositiveButton("Cari",(dialog,which)->{
    String query=field.getText().toString().trim();
    if(query.isEmpty())return;
    if(items==null){openSection(false);}
    if(items!=null)list(query);
   }).setNegativeButton("Batal",null).show();
 }
 void notesPage(){editor=null;titleEditor=null;categoryPicker=null;selected=null;base("DinNote");LinearLayout tabs=new LinearLayout(this);tabs.setPadding(12,12,12,8);TextView notes=text("✎  Nota",16);TextView books=text("▤  Maktabah",16);notes.setGravity(Gravity.CENTER);books.setGravity(Gravity.CENTER);notes.setBackgroundColor(maktabah?0xfff6f8fa:0xffddf4ff);books.setBackgroundColor(maktabah?0xffddf4ff:0xfff6f8fa);tabs.addView(notes,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(books,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);notes.setOnClickListener(v->openSection(false));books.setOnClickListener(v->openSection(true));LinearLayout tools=new LinearLayout(this);tools.setPadding(12,4,12,4);Button add=new Button(this);add.setAllCaps(false);add.setText(maktabah?"+ Kitab baharu":"+ Nota baharu");tools.addView(add,new LinearLayout.LayoutParams(0,-2,1));add.setOnClickListener(v->edit(null));Button manage=new Button(this);manage.setAllCaps(false);manage.setText("⚙ Kategori");tools.addView(manage);manage.setOnClickListener(v->manageCategories());root.addView(tools);EditText search=new EditText(this);search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setSelectAllOnFocus(false);search.setTextSize(15);search.setHint(maktabah?"Cari dalam Maktabah":"Cari nota");search.setContentDescription(maktabah?"Carian kitab":"Carian nota");search.setPadding(24,12,24,12);root.addView(search);LinearLayout filters=new LinearLayout(this);filters.setPadding(12,0,12,8);TextView folder=text("▤  "+(maktabah?"Maktabah":"Nota")+"  /",14);filters.addView(folder);Spinner catFilter=new Spinner(this);catFilter.setContentDescription("Tapis mengikut kategori");ArrayList<String> cats=new ArrayList<>();cats.add("Semua");cats.addAll(categories());catFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));int ci=cats.indexOf(categoryFilter);catFilter.setSelection(ci<0?0:ci);filters.addView(catFilter,new LinearLayout.LayoutParams(0,-2,1));root.addView(filters);View line=new View(this);line.setBackgroundColor(0xffd0d7de);root.addView(line,new LinearLayout.LayoutParams(-1,1));ScrollView sc=new ScrollView(this);sc.setOverScrollMode(View.OVER_SCROLL_NEVER);items=new LinearLayout(this);items.setOrientation(1);sc.addView(items);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));catFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){categoryFilter=cats.get(pos);list(search.getText().toString());}});search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){list(s.toString());}public void afterTextChanged(Editable e){}});list("");}

 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 TextView dashboardText(String value,int size,boolean bold){TextView t=text(value,size);t.setPadding(dp(10),dp(8),dp(10),dp(8));if(bold)t.setTypeface(null,1);return t;}
 LinearLayout dashboardCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(14),dp(12),dp(14),dp(12));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(17));bg.setStroke(dp(1),0xffe1e5e4);c.setBackground(bg);c.setElevation(dp(3));return c;}
 void openSection(boolean books){sectionOpen=true;maktabah=books;categoryFilter="Semua";notesPage();}
 void home(){
  items=null;sectionOpen=false;editor=null;titleEditor=null;categoryPicker=null;selected=null;maktabah=false;
  base("DinNote");
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
  scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setSmoothScrollingEnabled(true);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);
  page.setPadding(dp(16),dp(20),dp(16),dp(32));scroll.addView(page);
  TextView recent=dashboardText("Baru dilihat  ▾",15,true);
  recent.setBackground(rounded(0xfff6f8fa,0xffd0d7de,10));
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,-2);rp.bottomMargin=dp(22);
  page.addView(recent,rp);
  recent.setContentDescription("Pilih paparan: baru dilihat, semua nota atau Maktabah");recent.setFocusable(true);
  recent.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Baru dilihat","Semua nota","Maktabah"},(d,i)->{if(i==2)openSection(true);else openSection(false);}).show());
  TextView heading=dashboardText("Baru dilihat",22,true);heading.setPadding(dp(2),dp(8),0,dp(18));page.addView(heading);
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);
  search.setHint("Cari nota atau kitab");search.setContentDescription("Carian nota dan kitab");
  search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
  search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
  search.setPadding(dp(16),dp(10),dp(16),dp(10));
  search.setBackground(rounded(Color.WHITE,0xffd0d7de,11));
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.bottomMargin=dp(20);
  page.addView(search,sp);
  search.setOnEditorActionListener((v,id,event)->{String q=search.getText().toString().trim();if(q.isEmpty()){search.setError("Masukkan kata carian");return true;}openSection(false);list(q);return true;});
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setClipToPadding(false);
  card.setBackground(rounded(Color.WHITE,0xffd0d7de,12));
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);page.addView(card,cp);
  TextView count=dashboardText("Nota dan Maktabah",15,true);
  count.setBackgroundColor(0xfff6f8fa);card.addView(count);
  View divider=new View(this);divider.setBackgroundColor(0xffd0d7de);
  card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
  LinearLayout tiles=new LinearLayout(this);tiles.setPadding(dp(12),dp(18),dp(12),dp(18));card.addView(tiles);
  TextView note=dashboardText("▤  Nota",18,true);
  TextView book=dashboardText("▤  Maktabah",18,true);
  note.setGravity(Gravity.CENTER);book.setGravity(Gravity.CENTER);note.setFocusable(true);book.setFocusable(true);
  note.setBackground(rounded(0xfff6f8fa,0xffd0d7de,10));
  book.setBackground(rounded(0xfff6f8fa,0xffd0d7de,10));
  LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(96),1);a.rightMargin=dp(6);
  LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(96),1);b.leftMargin=dp(6);
  tiles.addView(note,a);tiles.addView(book,b);
  note.setContentDescription("Buka senarai Nota");book.setContentDescription("Buka Maktabah");
  note.setOnClickListener(v->openSection(false));book.setOnClickListener(v->openSection(true));
  LinearLayout recentNotes=new LinearLayout(this);recentNotes.setOrientation(1);
  recentNotes.setPadding(dp(14),dp(14),dp(14),dp(28));card.addView(recentNotes);
  ArrayList<String> keys=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if((k.startsWith("n_")||k.startsWith("m_"))&&prefs.getLong("viewed_"+k,0L)>0L)keys.add(k);
  Collections.sort(keys,(firstKey,secondKey)->Long.compare(prefs.getLong("viewed_"+secondKey,0L),prefs.getLong("viewed_"+firstKey,0L)));
  if(keys.isEmpty()){
   TextView emblem=dashboardText("☪\n▤",36,true);emblem.setTextColor(0xff176b47);
   emblem.setGravity(Gravity.CENTER);recentNotes.addView(emblem);
   TextView empty=dashboardText("Belum ada nota atau kitab dilihat",18,true);
   empty.setGravity(Gravity.CENTER);recentNotes.addView(empty);
   TextView hint=dashboardText("Nota atau kitab yang dibuka akan muncul di sini.",14,false);
   hint.setGravity(Gravity.CENTER);hint.setTextColor(0xff656d76);hint.setTextSize(13);recentNotes.addView(hint);
  }else{
   for(int i=0;i<Math.min(5,keys.size());i++){
    String k=keys.get(i);String t=prefs.getString("title_"+k,"");
    if(t.trim().isEmpty())t="Tanpa tajuk";
    TextView item=dashboardText((k.startsWith("m_")?"▤  ":"✎  ")+t+"   ›",16,true);
    item.setMaxLines(2);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
    item.setContentDescription("Buka "+(k.startsWith("m_")?"kitab ":"nota ")+t);
    recentNotes.addView(item);item.setOnClickListener(v->{maktabah=k.startsWith("m_");read(k);});
   }
  }
  TextView add=dashboardText("＋  Nota baharu",15,true);add.setGravity(Gravity.CENTER);
  add.setBackground(rounded(0xfff6f8fa,0xffd0d7de,10));
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);
  ap.topMargin=dp(16);page.addView(add,ap);
  add.setContentDescription("Cipta nota baharu");add.setFocusable(true);
  add.setOnClickListener(v->{maktabah=false;edit(null);});
 }
 android.graphics.drawable.GradientDrawable rounded(int fill,int stroke,int radius){
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
  d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;
 }

 void showDashboardNotes(LinearLayout page,boolean important){View view=page.findViewWithTag("dashboard_notes");if(view instanceof LinearLayout)populateDashboardNotes((LinearLayout)view,important);}
 void populateDashboardNotes(LinearLayout target,boolean important){
  target.removeAllViews();ArrayList<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("n_"))keys.add(k);Collections.sort(keys,Collections.reverseOrder());
  int count=0;int[] colors={0xfff1f7ec,0xfffff5e5,0xffeaf4f8,0xfff9edf1};
  for(String k:keys){if(important&&!prefs.getString("cat_"+k,"").equalsIgnoreCase("Penting"))continue;if(count>=5)break;
   String title=prefs.getString("title_"+k,"").trim();String content=prefs.getString(k,"");if(title.isEmpty())title=content.isEmpty()?"Tanpa tajuk":content.split("\\n",2)[0];
   LinearLayout card=dashboardCard();android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(colors[count%colors.length]);bg.setCornerRadius(dp(14));card.setBackground(bg);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);target.addView(card,lp);
   TextView heading=dashboardText("▤  "+title,16,true);card.addView(heading);
   String preview=content.replace("\\n"," ");if(preview.length()>100)preview=preview.substring(0,100)+"…";TextView detail=dashboardText(preview,13,false);detail.setTextColor(0xff57635e);card.addView(detail);
   card.setOnClickListener(v->{maktabah=false;read(k);});count++;
  }
  if(count==0)target.addView(dashboardText(important?"Tiada nota dalam kategori Penting.":"Belum ada nota. Tekan + untuk mencipta nota.",14,false));
 }

 void list(String filter){filter=filter.trim();items.removeAllViews();ArrayList<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_"))keys.add(k);Collections.sort(keys,Collections.reverseOrder());int matches=0;for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");if(!(heading+" "+v).toLowerCase(java.util.Locale.ROOT).contains(filter.toLowerCase(java.util.Locale.ROOT)))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;matches++;}if(matches==0){TextView empty=text(keys.isEmpty()?(maktabah?"▤  Belum ada kitab\nTekan + Kitab baharu untuk bermula":"✎  Belum ada nota\nTekan + Nota baharu untuk mula mencatat"):"Tiada hasil carian yang sepadan",17);empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff727985);empty.setPadding(28,80,28,80);items.addView(empty);}for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");String shown=(heading.isEmpty()?(v.isEmpty()?"Tanpa tajuk":v.split("\n",2)[0]):heading);if(shown.length()>90)shown=shown.substring(0,90)+"…";if(!(heading+" "+v).toLowerCase(java.util.Locale.ROOT).contains(filter.toLowerCase(java.util.Locale.ROOT)))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;String preview=v.replace("\n"," ");TextView t=text("✦  "+shown+"\n"+(preview.length()>100?preview.substring(0,100)+"…":preview),17);t.setBackgroundColor(Color.WHITE);t.setPadding(30,28,30,28);items.addView(t);View line=new View(this);line.setBackgroundColor(0xffe5e9e5);items.addView(line,new LinearLayout.LayoutParams(-1,1));t.setOnClickListener(w->read(k));}}

 void cleanBase(){readerDetector=null;root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.WHITE);setContentView(root);applySystemBarInsets(root);}
 Button action(String label,LinearLayout bar,Runnable callback){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);bar.addView(b);b.setOnClickListener(v->callback.run());return b;}
 void returnFromReader(){selected=null;if(sectionOpen)notesPage();else home();}
 void read(String key){
  editor=null;titleEditor=null;selected=key;prefs.edit().putLong("viewed_"+key,System.currentTimeMillis()).apply();cleanBase();
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);
  Button readerBack=action("‹",bar,()->returnFromReader());readerBack.setContentDescription("Kembali ke senarai");
  TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
  Button readerEdit=action("✎ Edit",bar,()->edit(key));readerEdit.setContentDescription("Edit nota ini");
  Button readerOptions=action("⋮",bar,()->new AlertDialog.Builder(this).setItems(new String[]{"Edit","Padam"},(d,i)->{if(i==0)edit(key);else confirmDelete(key);}).show());readerOptions.setContentDescription("Pilihan nota");
  String heading=prefs.getString("title_"+key,"");
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
  LinearLayout body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);
  TextView title=text(heading,22);title.setTextColor(0xff25282d);title.setTextIsSelectable(true);
  if(!heading.isEmpty())body.addView(title);
  TextView content=text(prefs.getString(key,""),16);
  content.setTextColor(0xff30343b);content.setTextIsSelectable(true);content.setLineSpacing(dp(6),1.12f);
  body.addView(content,new LinearLayout.LayoutParams(-1,-2));
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  readerDetector=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){bar.setVisibility(bar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);return true;}
  });
  // Touch events are observed once in dispatchTouchEvent, including selectable text.
  // Long press is a reliable alternative when double tap is intercepted by text selection.
  body.setOnLongClickListener(v->{edit(key);return true;});
  scroll.setOnLongClickListener(v->{edit(key);return true;});
  bar.setVisibility(View.GONE);
 }
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
  if(readerDetector!=null)readerDetector.onTouchEvent(event);
  return super.dispatchTouchEvent(event);
 }
 void confirmDelete(String key){new AlertDialog.Builder(this).setMessage("Padam nota ini?").setPositiveButton("Padam",(d,w)->{prefs.edit().remove(key).remove("title_"+key).remove("cat_"+key).remove("viewed_"+key).apply();selected=null;if(sectionOpen)notesPage();else home();}).setNegativeButton("Batal",null).show();}
 void edit(String key){if(key!=null)maktabah=key.startsWith("m_");editor=null;selected=key;cleanBase();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);action("‹",bar,()->{save();if(selected!=null)read(selected);else if(sectionOpen)notesPage();else home();});TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));action("Selesai",bar,()->{save();if(selected!=null)read(selected);else if(sectionOpen)notesPage();else home();});action("⋮",bar,()->new AlertDialog.Builder(this).setItems(new String[]{"Padam"},(d,i)->{if(selected!=null)confirmDelete(selected);}).show());categoryPicker=new Spinner(this);categoryPicker.setContentDescription("Kategori nota");ArrayList<String> choices=new ArrayList<>();choices.add("Tanpa kategori");choices.addAll(categories());categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));String savedCat=key==null?"Tanpa kategori":prefs.getString("cat_"+key,"Tanpa kategori");int pos=choices.indexOf(savedCat);categoryPicker.setSelection(pos<0?0:pos);root.addView(categoryPicker);titleEditor=new EditText(this);titleEditor.setSingleLine(true);titleEditor.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);titleEditor.setTextSize(21);titleEditor.setHint(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setContentDescription(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setPadding(24,12,24,12);titleEditor.setBackgroundColor(Color.WHITE);root.addView(titleEditor);if(key!=null)titleEditor.setText(prefs.getString("title_"+key,""));editor=new EditText(this);editor.setGravity(Gravity.TOP);editor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);editor.setTextSize(16);editor.setTextColor(0xff30343b);editor.setPadding(24,16,24,16);editor.setLineSpacing(5,1.08f);editor.setHint("Tulis di sini…");editor.setContentDescription("Kandungan nota");editor.setBackgroundColor(Color.WHITE);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));if(key!=null)editor.setText(prefs.getString(key,""));TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save();}public void afterTextChanged(Editable e){}};titleEditor.addTextChangedListener(watcher);editor.addTextChangedListener(watcher);categoryPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){save();}});}

 void save(){if(editor==null||selected==null&&(editor.getText().toString().trim().isEmpty()&&(titleEditor==null||titleEditor.getText().toString().trim().isEmpty())))return;String value=editor.getText().toString();if(selected==null)selected=(maktabah?"m_":"n_")+System.currentTimeMillis();prefs.edit().putString(selected,value).putString("title_"+selected,titleEditor==null?"":titleEditor.getText().toString()).putString("cat_"+selected,categoryPicker==null||categoryPicker.getSelectedItem()==null?"Tanpa kategori":categoryPicker.getSelectedItem().toString()).apply();}
 void seedDefaultCategories(){seedOne("categories_n",new String[]{"Nota Umum","Kuliah","Tazkirah","Catatan Peribadi","Senarai Semak"});seedOne("categories_m",new String[]{"Kalimah Tayyibah","Kalimah Tayyibah › Zikir","Kalimah Tayyibah › Doa","Kalimah Tayyibah › Istighfar","Kalimah Tayyibah › Selawat","Kalam Hikmah","Kalam Hikmah › Hikmah Ulama","Kalam Hikmah › Nasihat","Kalam Hikmah › Tazkiyah","Kalam Hikmah › Tasawuf","Kitab-Kitab","Kitab-Kitab › Tafsir","Kitab-Kitab › Hadis","Kitab-Kitab › Tauhid","Kitab-Kitab › Fiqh","Kitab-Kitab › Tasawuf","Kitab-Kitab › Akhlak","Kitab-Kitab › Tajwid","Kitab-Kitab › Bahasa Arab"});}
 void seedOne(String key,String[] defaults){if(prefs.contains(key))return;prefs.edit().putString(key,android.text.TextUtils.join("|",defaults)).apply();}
 ArrayList<String> categories(){String raw=prefs.getString(maktabah?"categories_m":"categories_n","");ArrayList<String> out=new ArrayList<>();for(String x:raw.split("\\|"))if(!x.trim().isEmpty())out.add(x.trim());return out;}
 void storeCategories(ArrayList<String> c){prefs.edit().putString(maktabah?"categories_m":"categories_n",android.text.TextUtils.join("|",c)).apply();}
 void manageCategories(){ArrayList<String> c=categories();String[] options=new String[c.size()+1];options[0]="+ Tambah kategori";for(int i=0;i<c.size();i++)options[i+1]=c.get(i);new AlertDialog.Builder(this).setTitle("Urus kategori").setItems(options,(d,which)->{if(which==0)categoryDialog(null);else categoryActions(c.get(which-1));}).setNegativeButton("Tutup",null).show();}
 void categoryActions(String old){new AlertDialog.Builder(this).setTitle(old).setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{if(which==0)categoryDialog(old);else new AlertDialog.Builder(this).setMessage("Padam kategori? Nota tidak akan dipadam.").setPositiveButton("Padam",(a,b)->{ArrayList<String> c=categories();c.remove(old);storeCategories(c);for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,"Tanpa kategori").apply();categoryFilter="Semua";home();}).setNegativeButton("Batal",null).show();}).show();}
 void categoryDialog(String old){EditText field=new EditText(this);field.setSingleLine(true);field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);field.setHint("Nama kategori");if(old!=null)field.setText(old);new AlertDialog.Builder(this).setTitle(old==null?"Kategori baharu":"Ubah nama kategori").setView(field).setPositiveButton("Simpan",(d,w)->{String name=field.getText().toString().trim().replace("|","");if(name.isEmpty()||name.equals("Semua")||name.equals("Tanpa kategori"))return;ArrayList<String> c=categories();if(c.contains(name)&&!name.equals(old))return;if(old!=null)c.remove(old);c.add(name);storeCategories(c);if(old!=null){for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,name).apply();}categoryFilter="Semua";home();}).setNegativeButton("Batal",null).show();}
 @Override protected void onPause(){if(editor!=null)save();super.onPause();}
 public void onBackPressed(){if(editor!=null){save();String key=selected;editor=null;if(key!=null)read(key);else home();}else if(selected!=null){returnFromReader();}else if(sectionOpen){home();}else super.onBackPressed();}
}
