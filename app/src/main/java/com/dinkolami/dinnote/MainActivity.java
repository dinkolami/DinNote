package com.dinkolami.dinnote;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import android.text.*;import android.content.*;import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root,items; EditText editor,titleEditor; Spinner categoryPicker; String categoryFilter="Semua"; String selected=null; boolean maktabah=false, sectionOpen=false; android.content.SharedPreferences prefs;
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

 void base(String title){root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xfffafaf7);setContentView(root);applySystemBarInsets(root);LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(8,0,8,0);header.setMinimumHeight((int)(56*getResources().getDisplayMetrics().density));header.setBackgroundColor(0xff075b43);TextView menu=text("☰",23);menu.setTextColor(Color.WHITE);menu.setPadding(14,10,14,10);header.addView(menu);menu.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Nota","Maktabah","Kategori"},(d,i)->{if(i==2)manageCategories();else openSection(i==1);}).show());TextView name=text("📖 DinNote",20);name.setTextColor(Color.WHITE);name.setPadding(8,10,8,10);header.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView searchIcon=text("⌕",26);searchIcon.setTextColor(Color.WHITE);searchIcon.setPadding(10,10,10,10);header.addView(searchIcon);TextView bell=text("♧",21);bell.setTextColor(Color.WHITE);bell.setPadding(8,10,8,10);bell.setContentDescription("Pemberitahuan");header.addView(bell);searchIcon.setOnClickListener(v->{if(items!=null)home();});TextView options=text("⋮",23);options.setTextColor(Color.WHITE);options.setPadding(12,10,12,10);header.addView(options);options.setOnClickListener(v->manageCategories());root.addView(header,new LinearLayout.LayoutParams(-1,(int)(56*getResources().getDisplayMetrics().density)));View border=new View(this);border.setBackgroundColor(0xffd0d7de);root.addView(border,new LinearLayout.LayoutParams(-1,1));}
 void notesPage(){editor=null;titleEditor=null;categoryPicker=null;selected=null;base("DinNote");LinearLayout tabs=new LinearLayout(this);tabs.setPadding(12,12,12,8);TextView notes=text("✎  Nota",16);TextView books=text("▤  Maktabah",16);notes.setGravity(Gravity.CENTER);books.setGravity(Gravity.CENTER);notes.setBackgroundColor(maktabah?0xfff6f8fa:0xffddf4ff);books.setBackgroundColor(maktabah?0xffddf4ff:0xfff6f8fa);tabs.addView(notes,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(books,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);notes.setOnClickListener(v->openSection(false));books.setOnClickListener(v->openSection(true));LinearLayout tools=new LinearLayout(this);tools.setPadding(12,4,12,4);Button add=new Button(this);add.setAllCaps(false);add.setText(maktabah?"+ Kitab baharu":"+ Nota baharu");tools.addView(add,new LinearLayout.LayoutParams(0,-2,1));add.setOnClickListener(v->edit(null));Button manage=new Button(this);manage.setAllCaps(false);manage.setText("⚙ Kategori");tools.addView(manage);manage.setOnClickListener(v->manageCategories());root.addView(tools);EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setHint(maktabah?"Cari dalam Maktabah":"Cari nota");search.setPadding(24,12,24,12);root.addView(search);LinearLayout filters=new LinearLayout(this);filters.setPadding(12,0,12,8);TextView folder=text("▤  "+(maktabah?"Maktabah":"Nota")+"  /",14);filters.addView(folder);Spinner catFilter=new Spinner(this);ArrayList<String> cats=new ArrayList<>();cats.add("Semua");cats.addAll(categories());catFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));int ci=cats.indexOf(categoryFilter);catFilter.setSelection(ci<0?0:ci);filters.addView(catFilter,new LinearLayout.LayoutParams(0,-2,1));root.addView(filters);View line=new View(this);line.setBackgroundColor(0xffd0d7de);root.addView(line,new LinearLayout.LayoutParams(-1,1));ScrollView sc=new ScrollView(this);items=new LinearLayout(this);items.setOrientation(1);sc.addView(items);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));catFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){categoryFilter=cats.get(pos);list(search.getText().toString());}});search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){list(s.toString());}public void afterTextChanged(Editable e){}});list("");}

 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 TextView dashboardText(String value,int size,boolean bold){TextView t=text(value,size);t.setPadding(dp(10),dp(8),dp(10),dp(8));if(bold)t.setTypeface(null,1);return t;}
 LinearLayout dashboardCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(14),dp(12),dp(14),dp(12));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(17));bg.setStroke(dp(1),0xffe1e5e4);c.setBackground(bg);c.setElevation(dp(3));return c;}
 void openSection(boolean books){sectionOpen=true;maktabah=books;categoryFilter="Semua";notesPage();}
 void home(){
  sectionOpen=false;editor=null;titleEditor=null;categoryPicker=null;selected=null;maktabah=false;
  base("DinNote");
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(14),dp(14),dp(14),dp(28));scroll.addView(page);
  
  
  LinearLayout tiles=new LinearLayout(this);tiles.setPadding(0,dp(6),0,dp(18));page.addView(tiles);
  LinearLayout noteTile=dashboardCard(),bookTile=dashboardCard();
  LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(150),1);np.setMargins(0,0,dp(7),0);tiles.addView(noteTile,np);
  LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(150),1);bp.setMargins(dp(7),0,0,0);tiles.addView(bookTile,bp);
  TextView ni=dashboardText("▤",45,true);ni.setTextColor(0xff075d45);ni.setGravity(Gravity.CENTER);noteTile.addView(ni);
  TextView nt=dashboardText("Nota",21,true);nt.setGravity(Gravity.CENTER);noteTile.addView(nt);
  TextView nd=dashboardText("Catatan peribadi",14,false);nd.setGravity(Gravity.CENTER);noteTile.addView(nd);
  TextView bi=dashboardText("▰",45,true);bi.setTextColor(0xff075d45);bi.setGravity(Gravity.CENTER);bookTile.addView(bi);
  TextView bt=dashboardText("Maktabah",21,true);bt.setGravity(Gravity.CENTER);bookTile.addView(bt);
  TextView bd=dashboardText("Kitab dan rujukan",14,false);bd.setGravity(Gravity.CENTER);bookTile.addView(bd);
  noteTile.setOnClickListener(v->openSection(false));bookTile.setOnClickListener(v->openSection(true));

  TextView section=dashboardText("Nota Saya",19,true);page.addView(section);
  LinearLayout filters=new LinearLayout(this);filters.setGravity(Gravity.CENTER_VERTICAL);page.addView(filters);
  String[] labels={"Semua","Penting","Terbaru","Kategori"};
  for(String label:labels){TextView chip=dashboardText(label,12,false);chip.setGravity(Gravity.CENTER);chip.setTextColor(0xff075b43);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(0xffe9f2ed);bg.setCornerRadius(dp(20));chip.setBackground(bg);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(38),1);cp.setMargins(dp(2),dp(5),dp(2),dp(12));filters.addView(chip,cp);chip.setOnClickListener(v->{if(label.equals("Kategori")){maktabah=false;manageCategories();}else if(label.equals("Penting"))showDashboardNotes(page,true);else if(label.equals("Terbaru"))showDashboardNotes(page,false);else showDashboardNotes(page,false);});}
  LinearLayout noteList=new LinearLayout(this);noteList.setOrientation(1);noteList.setTag("dashboard_notes");page.addView(noteList);populateDashboardNotes(noteList,false);
  TextView libraryTitle=dashboardText("Maktabah                         Lihat semua  →",18,true);page.addView(libraryTitle);libraryTitle.setOnClickListener(v->openSection(true));
  LinearLayout libraryCard=dashboardCard();page.addView(libraryCard);
  ArrayList<String> mk=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("m_"))mk.add(k);Collections.sort(mk,Collections.reverseOrder());
  if(mk.isEmpty())libraryCard.addView(dashboardText("Belum ada bacaan. Tekan Maktabah untuk bermula.",14,false));
  else{String k=mk.get(0);String title=prefs.getString("title_"+k,"Kitab");TextView item=dashboardText("▤  "+title,16,true);libraryCard.addView(item);item.setOnClickListener(v->{maktabah=true;read(k);});}
  LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.RIGHT);page.addView(bottom);
  Button plus=new Button(this);plus.setText("+");plus.setTextSize(29);plus.setTextColor(Color.WHITE);plus.setAllCaps(false);
  android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setColor(0xff075b43);circle.setCornerRadius(dp(60));plus.setBackground(circle);
  LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(62),dp(62));pp.setMargins(0,dp(20),dp(8),0);bottom.addView(plus,pp);plus.setOnClickListener(v->{maktabah=false;edit(null);});

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

 void list(String filter){items.removeAllViews();ArrayList<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_"))keys.add(k);Collections.sort(keys,Collections.reverseOrder());if(keys.isEmpty()){TextView empty=text(maktabah?"▤  Belum ada kitab\nTekan + Kitab baharu untuk bermula":"✎  Belum ada nota\nTekan + Nota baharu untuk mula mencatat",17);empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff727985);empty.setPadding(28,80,28,80);items.addView(empty);}for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");String shown=(heading.isEmpty()?(v.isEmpty()?"Tanpa tajuk":v.split("\n",2)[0]):heading);if(!(heading+" "+v).toLowerCase().contains(filter.toLowerCase()))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;String preview=v.replace("\n"," ");TextView t=text("✦  "+shown+"\n"+(preview.length()>100?preview.substring(0,100)+"…":preview),17);t.setBackgroundColor(Color.WHITE);t.setPadding(30,28,30,28);items.addView(t);View line=new View(this);line.setBackgroundColor(0xffe5e9e5);items.addView(line,new LinearLayout.LayoutParams(-1,1));t.setOnClickListener(w->read(k));}}

 void cleanBase(){root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.WHITE);setContentView(root);applySystemBarInsets(root);}
 Button action(String label,LinearLayout bar,Runnable callback){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);bar.addView(b);b.setOnClickListener(v->callback.run());return b;}
 void returnFromReader(){selected=null;if(sectionOpen)notesPage();else home();}
 void read(String key){
  editor=null;titleEditor=null;selected=key;cleanBase();
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);
  action("‹",bar,()->returnFromReader());
  TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
  action("✎ Edit",bar,()->edit(key));
  action("⋮",bar,()->new AlertDialog.Builder(this).setItems(new String[]{"Edit","Padam"},(d,i)->{if(i==0)edit(key);else confirmDelete(key);}).show());
  String heading=prefs.getString("title_"+key,"");
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  LinearLayout body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);
  TextView title=text(heading,22);title.setTextColor(0xff25282d);
  if(!heading.isEmpty())body.addView(title);
  TextView content=text(prefs.getString(key,""),16);
  content.setTextColor(0xff30343b);content.setLineSpacing(dp(6),1.12f);
  body.addView(content,new LinearLayout.LayoutParams(-1,-2));
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  android.view.GestureDetector detector=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){bar.setVisibility(bar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);return true;}
  });
  // Observe taps without consuming touch events: scrolling and text selection must still work.
  scroll.setOnTouchListener((v,e)->{detector.onTouchEvent(e);return false;});
  bar.setVisibility(View.GONE);
 }
 void confirmDelete(String key){new AlertDialog.Builder(this).setMessage("Padam nota ini?").setPositiveButton("Padam",(d,w)->{prefs.edit().remove(key).remove("title_"+key).remove("cat_"+key).apply();selected=null;home();}).setNegativeButton("Batal",null).show();}
 void edit(String key){editor=null;selected=key;cleanBase();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);action("‹",bar,()->{save();if(selected!=null)read(selected);else home();});TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));action("Selesai",bar,()->{save();if(selected!=null)read(selected);else home();});action("⋮",bar,()->new AlertDialog.Builder(this).setItems(new String[]{"Padam"},(d,i)->{if(selected!=null)confirmDelete(selected);}).show());categoryPicker=new Spinner(this);ArrayList<String> choices=new ArrayList<>();choices.add("Tanpa kategori");choices.addAll(categories());categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));String savedCat=key==null?"Tanpa kategori":prefs.getString("cat_"+key,"Tanpa kategori");int pos=choices.indexOf(savedCat);categoryPicker.setSelection(pos<0?0:pos);root.addView(categoryPicker);titleEditor=new EditText(this);titleEditor.setSingleLine(true);titleEditor.setTextSize(21);titleEditor.setHint(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setPadding(24,12,24,12);titleEditor.setBackgroundColor(Color.WHITE);root.addView(titleEditor);if(key!=null)titleEditor.setText(prefs.getString("title_"+key,""));editor=new EditText(this);editor.setGravity(Gravity.TOP);editor.setTextSize(16);editor.setTextColor(0xff30343b);editor.setPadding(24,16,24,16);editor.setLineSpacing(5,1.08f);editor.setHint("Tulis di sini…");editor.setBackgroundColor(Color.WHITE);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));if(key!=null)editor.setText(prefs.getString(key,""));TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save();}public void afterTextChanged(Editable e){}};titleEditor.addTextChangedListener(watcher);editor.addTextChangedListener(watcher);categoryPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){save();}});}

 void save(){if(editor==null||selected==null&&(editor.getText().toString().trim().isEmpty()&&(titleEditor==null||titleEditor.getText().toString().trim().isEmpty())))return;String value=editor.getText().toString();if(selected==null)selected=(maktabah?"m_":"n_")+System.currentTimeMillis();prefs.edit().putString(selected,value).putString("title_"+selected,titleEditor==null?"":titleEditor.getText().toString()).putString("cat_"+selected,categoryPicker==null?"Tanpa kategori":categoryPicker.getSelectedItem().toString()).apply();}
 void seedDefaultCategories(){seedOne("categories_n",new String[]{"Nota Umum","Kuliah","Tazkirah","Catatan Peribadi","Senarai Semak"});seedOne("categories_m",new String[]{"Kalimah Tayyibah","Kalimah Tayyibah › Zikir","Kalimah Tayyibah › Doa","Kalimah Tayyibah › Istighfar","Kalimah Tayyibah › Selawat","Kalam Hikmah","Kalam Hikmah › Hikmah Ulama","Kalam Hikmah › Nasihat","Kalam Hikmah › Tazkiyah","Kalam Hikmah › Tasawuf","Kitab-Kitab","Kitab-Kitab › Tafsir","Kitab-Kitab › Hadis","Kitab-Kitab › Tauhid","Kitab-Kitab › Fiqh","Kitab-Kitab › Tasawuf","Kitab-Kitab › Akhlak","Kitab-Kitab › Tajwid","Kitab-Kitab › Bahasa Arab"});}
 void seedOne(String key,String[] defaults){if(prefs.contains(key))return;prefs.edit().putString(key,android.text.TextUtils.join("|",defaults)).apply();}
 ArrayList<String> categories(){String raw=prefs.getString(maktabah?"categories_m":"categories_n","");ArrayList<String> out=new ArrayList<>();for(String x:raw.split("\\|"))if(!x.trim().isEmpty())out.add(x.trim());return out;}
 void storeCategories(ArrayList<String> c){prefs.edit().putString(maktabah?"categories_m":"categories_n",android.text.TextUtils.join("|",c)).apply();}
 void manageCategories(){ArrayList<String> c=categories();String[] options=new String[c.size()+1];options[0]="+ Tambah kategori";for(int i=0;i<c.size();i++)options[i+1]=c.get(i);new AlertDialog.Builder(this).setTitle("Urus kategori").setItems(options,(d,which)->{if(which==0)categoryDialog(null);else categoryActions(c.get(which-1));}).setNegativeButton("Tutup",null).show();}
 void categoryActions(String old){new AlertDialog.Builder(this).setTitle(old).setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{if(which==0)categoryDialog(old);else new AlertDialog.Builder(this).setMessage("Padam kategori? Nota tidak akan dipadam.").setPositiveButton("Padam",(a,b)->{ArrayList<String> c=categories();c.remove(old);storeCategories(c);for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,"Tanpa kategori").apply();categoryFilter="Semua";home();}).setNegativeButton("Batal",null).show();}).show();}
 void categoryDialog(String old){EditText field=new EditText(this);field.setSingleLine(true);field.setHint("Nama kategori");if(old!=null)field.setText(old);new AlertDialog.Builder(this).setTitle(old==null?"Kategori baharu":"Ubah nama kategori").setView(field).setPositiveButton("Simpan",(d,w)->{String name=field.getText().toString().trim().replace("|","");if(name.isEmpty()||name.equals("Semua")||name.equals("Tanpa kategori"))return;ArrayList<String> c=categories();if(c.contains(name)&&!name.equals(old))return;if(old!=null)c.remove(old);c.add(name);storeCategories(c);if(old!=null){for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,name).apply();}categoryFilter="Semua";home();}).setNegativeButton("Batal",null).show();}
 public void onBackPressed(){if(editor!=null){save();String key=selected;editor=null;if(key!=null)read(key);else home();}else if(selected!=null){returnFromReader();}else if(sectionOpen){home();}else super.onBackPressed();}
}
