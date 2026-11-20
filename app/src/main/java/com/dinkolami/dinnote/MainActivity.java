package com.dinkolami.dinnote;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import android.text.*;import android.content.*;import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root,items; android.view.GestureDetector readerDetector; EditText editor,titleEditor; TextView saveIndicator; Spinner categoryPicker; String categoryFilter="Semua"; String sortMode="Terbaru"; String selected=null; boolean maktabah=false, sectionOpen=false; android.content.SharedPreferences prefs;
 int green=Color.rgb(23,107,71), gold=Color.rgb(197,166,107); static final int BACKUP_CREATE=4201, BACKUP_OPEN=4202;
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("notes",0);seedDefaultCategories();seedSampleArticles();home();}
 void applySystemBarInsets(LinearLayout view){
  // Keep Android's real clock, battery and network status bar visible.
  getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
  getWindow().setStatusBarColor(0xfff6f8fa);
  if(android.os.Build.VERSION.SDK_INT>=30){
   android.view.WindowInsetsController controller=getWindow().getInsetsController();
   if(controller!=null){
    controller.show(android.view.WindowInsets.Type.statusBars()|android.view.WindowInsets.Type.navigationBars());
    controller.setSystemBarsAppearance(android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
   }
  }else{
   getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  }
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
  ImageView menu=headerIcon("⌂",0xff102b25,"Halaman utama DinNote");header.addView(menu);
  menu.setOnClickListener(v->home());
  ImageView settingsMenu=headerIcon("☰",0xff102b25,"Menu DinNote dan Tetapan");header.addView(settingsMenu);
  settingsMenu.setOnClickListener(v->showMainMenu());
  ImageView logo=new ImageView(this);logo.setImageBitmap(transparentHeaderLogo());logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Logo DinNote");logo.setClickable(true);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(40),dp(40));lp.leftMargin=dp(7);lp.rightMargin=dp(5);header.addView(logo,lp);
  logo.setOnClickListener(v->home());
  TextView name=text("DinNote",18);name.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.NORMAL);name.setTextColor(0xff123e2b);
  name.setGravity(Gravity.BOTTOM);name.setPadding(dp(3),0,0,dp(7));header.addView(name,new LinearLayout.LayoutParams(0,dp(40),1));
  name.setOnClickListener(v->home());
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
 android.graphics.Bitmap transparentHeaderLogo(){
  android.graphics.Bitmap original=android.graphics.BitmapFactory.decodeResource(getResources(),R.drawable.dinnote_header_logo);
  if(original==null)return null;
  android.graphics.Bitmap result=original.copy(android.graphics.Bitmap.Config.ARGB_8888,true);
  int w=result.getWidth(),h=result.getHeight(),count=w*h;
  int[] pixels=new int[count];result.getPixels(pixels,0,w,0,0,w,h);
  boolean[] seen=new boolean[count];int[] queue=new int[count];int head=0,tail=0;
  for(int x=0;x<w;x++){
   int top=x,bottom=(h-1)*w+x;
   if(!seen[top]&&lightBackground(pixels[top])){seen[top]=true;queue[tail++]=top;}
   if(!seen[bottom]&&lightBackground(pixels[bottom])){seen[bottom]=true;queue[tail++]=bottom;}
  }
  for(int y=0;y<h;y++){
   int left=y*w,right=left+w-1;
   if(!seen[left]&&lightBackground(pixels[left])){seen[left]=true;queue[tail++]=left;}
   if(!seen[right]&&lightBackground(pixels[right])){seen[right]=true;queue[tail++]=right;}
  }
  while(head<tail){
   int pos=queue[head++],x=pos%w,y=pos/w;
   pixels[pos]=0x00000000;
   int[] neighbors={x>0?pos-1:-1,x<w-1?pos+1:-1,y>0?pos-w:-1,y<h-1?pos+w:-1};
   for(int next:neighbors)if(next>=0&&!seen[next]&&lightBackground(pixels[next])){
    seen[next]=true;queue[tail++]=next;
   }
  }
  result.setPixels(pixels,0,w,0,0,w,h);
  return result;
 }
 boolean lightBackground(int color){
  return android.graphics.Color.alpha(color)<20||
   (android.graphics.Color.red(color)>237&&android.graphics.Color.green(color)>237&&android.graphics.Color.blue(color)>237);
 }
 ImageView headerIcon(String symbol,int color,String description){
  int px=dp(34);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(px,px,android.graphics.Bitmap.Config.ARGB_8888);
  android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
  android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  paint.setColor(color);paint.setTextSize(dp(21));paint.setTypeface(android.graphics.Typeface.DEFAULT);
  paint.setTextAlign(android.graphics.Paint.Align.CENTER);
  android.graphics.Paint.FontMetrics fm=paint.getFontMetrics();
  if("⌂".equals(symbol)){
   paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
   android.graphics.Path house=new android.graphics.Path();
   house.moveTo(px*.20f,px*.46f);house.lineTo(px*.50f,px*.20f);house.lineTo(px*.80f,px*.46f);
   house.moveTo(px*.28f,px*.43f);house.lineTo(px*.28f,px*.78f);house.lineTo(px*.72f,px*.78f);house.lineTo(px*.72f,px*.43f);
   house.moveTo(px*.44f,px*.78f);house.lineTo(px*.44f,px*.57f);house.lineTo(px*.56f,px*.57f);house.lineTo(px*.56f,px*.78f);
   canvas.drawPath(house,paint);
  }else{canvas.drawText(symbol,px/2f,px/2f-(fm.ascent+fm.descent)/2f,paint);}
  ImageView view=new ImageView(this);view.setImageBitmap(bitmap);view.setContentDescription(description);
  view.setClickable(true);view.setFocusable(true);view.setScaleType(ImageView.ScaleType.FIT_CENTER);
  android.graphics.drawable.GradientDrawable frame=new android.graphics.drawable.GradientDrawable();frame.setColor(0xffffffff);frame.setCornerRadius(dp(10));frame.setStroke(dp(1),0xffe5e7eb);view.setBackground(frame);
  view.setPadding(dp(5),dp(5),dp(5),dp(5));
  LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(34),dp(34));iconParams.leftMargin=dp(3);iconParams.rightMargin=dp(3);view.setLayoutParams(iconParams);
  return view;
 }
 void showMainMenu(){
  new AlertDialog.Builder(this).setTitle("Menu DinNote")
   .setItems(new String[]{"Halaman utama","Nota","Maktabah","Tetapan"},(dialog,which)->{
    if(which==0)home();
    else if(which==1)openSection(false);
    else if(which==2)openSection(true);
    else showSettings();
   }).show();
 }
 void showSettings(){
  new AlertDialog.Builder(this).setTitle("Tetapan DinNote")
   .setItems(new String[]{"Saiz tulisan bacaan","Saiz tulisan editor","Backup ke fail","Restore daripada fail","Maklumat aplikasi"},(dialog,which)->{
    if(which==0){
     new AlertDialog.Builder(this).setTitle("Saiz tulisan Nota dan Maktabah")
      .setSingleChoiceItems(new String[]{"Kecil","Biasa","Besar","Sangat besar"},
       Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1))),(d,choice)->{
        prefs.edit().putInt("reader_font_level",choice).apply();
        d.dismiss();
        Toast.makeText(this,"Saiz tulisan bacaan dikemas kini",Toast.LENGTH_SHORT).show();
       }).setNegativeButton("Batal",null).show();
    }else if(which==1){
     new AlertDialog.Builder(this).setTitle("Saiz tulisan editor")
      .setSingleChoiceItems(new String[]{"Kecil","Biasa","Besar","Sangat besar"},
       Math.max(0,Math.min(3,prefs.getInt("editor_font_level",1))),(d,choice)->{
        prefs.edit().putInt("editor_font_level",choice).apply();
        d.dismiss();
        if(editor!=null)editor.setTextSize(new int[]{13,15,18,22}[choice]);
        Toast.makeText(this,"Saiz tulisan editor dikemas kini",Toast.LENGTH_SHORT).show();
       }).setNegativeButton("Batal",null).show();
    }else if(which==2)startBackup();
    else if(which==3)startRestore();
    else{
     String version="Tidak diketahui";
     try{
      version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;
     }catch(Exception ignored){}
     new AlertDialog.Builder(this).setTitle("Mengenai DinNote")
      .setMessage("DinNote "+version+"\\nNota dan Maktabah\\nOCR ditangguhkan.")
      .setPositiveButton("Tutup",null).show();
    }
   }).setNegativeButton("Tutup",null).show();
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
 void notesPage(){editor=null;titleEditor=null;categoryPicker=null;selected=null;base("DinNote");LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(12),dp(10),dp(12),dp(8));TextView notes=text("✎  Nota",15);TextView books=text("▤  Maktabah",15);notes.setGravity(Gravity.CENTER);books.setGravity(Gravity.CENTER);notes.setTypeface(null,maktabah?android.graphics.Typeface.NORMAL:android.graphics.Typeface.BOLD);books.setTypeface(null,maktabah?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);notes.setPadding(dp(10),dp(13),dp(10),dp(13));books.setPadding(dp(10),dp(13),dp(10),dp(13));notes.setBackground(rounded(maktabah?0xfff8faf8:0xffe4f3e9,maktabah?0xffe0e6e2:0xffc5a66b,16));notes.setTextColor(maktabah?0xff476456:0xff176b47);notes.setContentDescription(maktabah?"Buka Nota":"Nota, bahagian dipilih");books.setBackground(rounded(maktabah?0xffe4f3e9:0xfff8faf8,maktabah?0xffc5a66b:0xffe0e6e2,16));books.setTextColor(maktabah?0xff176b47:0xff476456);books.setContentDescription(maktabah?"Maktabah, bahagian dipilih":"Buka Maktabah");tabs.addView(notes,new LinearLayout.LayoutParams(0,-2,1));tabs.addView(books,new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);String recentKey=prefs.getString(maktabah?"reading_last_m":"reading_last_n","");if(recentKey.startsWith(maktabah?"m_":"n_")&&prefs.contains(recentKey)){TextView resume=text("▶  Sambung bacaan  ·  "+prefs.getString("title_"+recentKey,"Tanpa tajuk"),13);resume.setTextColor(0xff176b47);resume.setBackground(rounded(0xfff3f9f4,0xffdbe9de,12));resume.setPadding(dp(14),dp(12),dp(14),dp(12));LinearLayout.LayoutParams resumeParams=new LinearLayout.LayoutParams(-1,-2);resumeParams.setMargins(dp(12),dp(2),dp(12),dp(6));root.addView(resume,resumeParams);resume.setOnClickListener(v->read(recentKey));}applyTouchFeedback(notes);applyTouchFeedback(books);notes.setOnClickListener(v->{if(maktabah)openSection(false);});books.setOnClickListener(v->{if(!maktabah)openSection(true);});LinearLayout tools=new LinearLayout(this);tools.setPadding(dp(12),dp(5),dp(12),dp(5));Button add=new Button(this);add.setAllCaps(false);add.setText(maktabah?"+ Kitab baharu":"+ Nota baharu");add.setContentDescription(maktabah?"Tambah kitab baharu":"Tambah nota baharu");tools.addView(add,new LinearLayout.LayoutParams(0,-2,1));add.setOnClickListener(v->edit(null));Button manage=new Button(this);manage.setAllCaps(false);manage.setText("⚙ Kategori");manage.setContentDescription("Urus kategori");tools.addView(manage);manage.setOnClickListener(v->manageCategories());Button backup=new Button(this);backup.setAllCaps(false);backup.setText("⋮");backup.setContentDescription("Pilihan sandaran dan pemulihan data");tools.addView(backup);backup.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Data DinNote").setItems(new String[]{"Backup ke fail","Restore daripada fail"},(d,i)->{if(i==0)startBackup();else startRestore();}).show());root.addView(tools);EditText search=new EditText(this);search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setSelectAllOnFocus(false);search.setMaxLines(1);search.setTextSize(14);search.setHint(maktabah?"Cari tajuk, isi atau kategori kitab":"Cari tajuk, isi atau kategori nota");search.setContentDescription(maktabah?"Carian kitab":"Carian nota");search.setPadding(dp(16),dp(10),dp(16),dp(10));search.setBackground(rounded(0xffffffff,0xffdce7df,12));LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(-1,-2);searchParams.setMargins(dp(12),dp(4),dp(12),dp(6));root.addView(search,searchParams);LinearLayout filters=new LinearLayout(this);filters.setPadding(dp(12),dp(6),dp(12),dp(6));TextView folder=text((maktabah?"▤  Maktabah":"✎  Nota")+"  /",14);folder.setTextColor(0xff35644a);folder.setTypeface(null,android.graphics.Typeface.BOLD);filters.addView(folder);Spinner catFilter=new Spinner(this);catFilter.setContentDescription("Tapis "+(maktabah?"kitab":"nota")+" mengikut kategori");ArrayList<String> cats=new ArrayList<>();cats.add("Semua");cats.addAll(categories());catFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));int ci=cats.indexOf(categoryFilter);catFilter.setSelection(ci<0?0:ci);filters.addView(catFilter,new LinearLayout.LayoutParams(0,-2,1));root.addView(filters);
  TextView sort=text("Susun: "+sortMode+"  ▾",13);sort.setGravity(Gravity.END);sort.setPadding(dp(12),dp(6),dp(16),dp(6));sort.setContentDescription("Susunan semasa: "+sortMode+". Tekan untuk ubah");applyTouchFeedback(sort);root.addView(sort);
  sort.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Susunan").setItems(new String[]{"Terbaru","Terlama","Tajuk A–Z","Kerap dibuka"},(d,i)->{sortMode=new String[]{"Terbaru","Terlama","Tajuk A–Z","Kerap dibuka"}[i];sort.setText("Susun: "+sortMode+"  ▾");sort.setContentDescription("Susunan semasa: "+sortMode+". Tekan untuk ubah");list(search.getText().toString());}).show());
  View line=new View(this);line.setBackgroundColor(0xffd0d7de);root.addView(line,new LinearLayout.LayoutParams(-1,1));ScrollView sc=new ScrollView(this);sc.setOverScrollMode(View.OVER_SCROLL_NEVER);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(true);items=new LinearLayout(this);items.setOrientation(1);items.setPadding(0,dp(4),0,dp(12));sc.addView(items);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));catFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){categoryFilter=cats.get(pos);list(search.getText().toString());}});search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){list(s.toString());}public void afterTextChanged(Editable e){}});list("");}

 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 android.graphics.drawable.GradientDrawable silverGradient(int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xffffffff,0xfffcfcfd,0xfff1f2f4});d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xffe1e4e8);return d;}
 android.graphics.drawable.GradientDrawable subtleNoteBorder(int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(Color.WHITE);d.setCornerRadius(dp(radius));d.setStroke(dp(0.5f),0xffe5e7eb);return d;}
 TextView dashboardText(String value,int size,boolean bold){TextView t=text(value,size);t.setPadding(dp(10),dp(8),dp(10),dp(8));if(bold)t.setTypeface(null,1);return t;}
 LinearLayout dashboardCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(14),dp(12),dp(14),dp(12));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(17));bg.setStroke(dp(1),0xffe1e5e4);c.setBackground(bg);c.setElevation(dp(3));return c;}
 void applyTouchFeedback(View view){
  android.util.TypedValue value=new android.util.TypedValue();
  if(getTheme().resolveAttribute(android.R.attr.selectableItemBackground,value,true)){
   view.setForeground(getDrawable(value.resourceId));
  }
 }
 void openSection(boolean books){sectionOpen=true;maktabah=books;categoryFilter="Semua";notesPage();}
 void home(){
  items=null;sectionOpen=false;editor=null;titleEditor=null;categoryPicker=null;selected=null;maktabah=false;
  base("DinNote");
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
  scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setSmoothScrollingEnabled(true);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);
  page.setPadding(dp(8),dp(6),dp(8),dp(12));scroll.addView(page);
  TextView recent=dashboardText("Baru dilihat  ▾",15,true);
  recent.setBackground(rounded(0xffeaf7eb,0xffc9e8cf,8));
  LinearLayout quickTools=new LinearLayout(this);quickTools.setOrientation(LinearLayout.HORIZONTAL);quickTools.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout.LayoutParams quickParams=new LinearLayout.LayoutParams(-1,-2);quickParams.bottomMargin=dp(4);
  page.addView(quickTools,quickParams);
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,dp(42));rp.rightMargin=dp(6);
  recent.setGravity(Gravity.CENTER_VERTICAL);recent.setPadding(dp(12),0,dp(12),0);
  quickTools.addView(recent,rp);
  recent.setContentDescription("Pilih paparan: baru dilihat, semua nota atau Maktabah");recent.setFocusable(true);
  recent.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Baru dilihat","Semua nota","Maktabah"},(d,i)->{if(i==2)openSection(true);else openSection(false);}).show());
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);
  search.setHint("Cari nota atau kitab");search.setContentDescription("Carian nota dan kitab");
  search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
  search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
  search.setPadding(dp(12),dp(6),dp(12),dp(6));
  search.setBackground(rounded(Color.WHITE,0xffe5e7eb,8));
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(42),1);
  quickTools.addView(search,sp);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){String q=s.toString().trim();if(!q.isEmpty())showHomeSearchResults(q);}public void afterTextChanged(Editable e){}});
  search.setOnEditorActionListener((v,id,event)->{String q=search.getText().toString().trim();if(q.isEmpty()){search.setError("Masukkan kata carian");return true;}openSection(false);list(q);return true;});
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setClipToPadding(false);
  card.setBackground(rounded(Color.WHITE,0xffe5e7eb,8));
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);page.addView(card,cp);
  TextView count=dashboardText("Nota dan Maktabah",15,true);
  count.setGravity(Gravity.CENTER);count.setTextColor(0xff123b2b);count.setBackgroundColor(Color.WHITE);card.addView(count);
  View divider=new View(this);divider.setBackgroundColor(0xffe5e7eb);
  card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
  LinearLayout tiles=new LinearLayout(this);tiles.setPadding(dp(8),dp(5),dp(8),dp(4));card.addView(tiles);
  TextView note=dashboardText("▤\nNota",14,true);
  TextView book=dashboardText("▱\nMaktabah",14,true);
  note.setGravity(Gravity.CENTER);book.setGravity(Gravity.CENTER);note.setFocusable(true);book.setFocusable(true);
  note.setBackground(rounded(0xfff0fbf2,0xffbde6c5,10));
  book.setBackground(rounded(0xfff0fbf2,0xffbde6c5,10));
  LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(78),1);a.rightMargin=dp(4);
  LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(78),1);b.leftMargin=dp(4);
  tiles.addView(note,a);tiles.addView(book,b);
  note.setContentDescription("Buka senarai Nota");book.setContentDescription("Buka Maktabah");
  note.setOnClickListener(v->openSection(false));book.setOnClickListener(v->openSection(true));
  applyTouchFeedback(note);applyTouchFeedback(book);
  // Compact GitHub-style shortcuts, without changing the approved header.
  LinearLayout shortcuts=new LinearLayout(this);shortcuts.setOrientation(1);
  shortcuts.setPadding(dp(2),dp(2),dp(2),dp(1));page.addView(shortcuts);
  dashboardSection(shortcuts,"📌  Nota disemat",dashboardKeys("pinned_",false),true);
  dashboardSection(shortcuts,"◷  Terakhir dikemaskini",dashboardKeys("updated_",false),false);
  String resume=prefs.getString("last_edited_key","");
  if(!resume.isEmpty()&&prefs.contains(resume)){
   TextView resumeButton=dashboardText("✎  Sambung menulis  ›",15,true);
   resumeButton.setBackground(rounded(0xffeffaf1,0xffa6dcb3,10));
   shortcuts.addView(resumeButton);
   resumeButton.setOnClickListener(v->edit(resume));
  }
  LinearLayout recentNotes=new LinearLayout(this);recentNotes.setOrientation(1);
  recentNotes.setPadding(dp(12),dp(2),dp(12),dp(5));card.addView(recentNotes);
  ArrayList<String> keys=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if((k.startsWith("n_")||k.startsWith("m_"))&&prefs.getLong("viewed_"+k,0L)>0L)keys.add(k);
  Collections.sort(keys,(firstKey,secondKey)->Long.compare(prefs.getLong("viewed_"+secondKey,0L),prefs.getLong("viewed_"+firstKey,0L)));
  if(keys.isEmpty()){
   TextView create=dashboardText("＋  Cipta nota pertama",13,true);create.setGravity(Gravity.CENTER);create.setOnClickListener(v->{maktabah=false;edit(null);});recentNotes.addView(create);
   TextView emblem=dashboardText("☪\n▤",36,true);emblem.setTextColor(0xff176b47);
   emblem.setGravity(Gravity.CENTER);recentNotes.addView(emblem);
   TextView empty=dashboardText("Belum ada nota atau kitab dilihat",18,true);
   empty.setGravity(Gravity.CENTER);recentNotes.addView(empty);
   TextView hint=dashboardText("Nota atau kitab yang dibuka akan muncul di sini.",14,false);
   hint.setGravity(Gravity.CENTER);hint.setTextColor(0xff656d76);hint.setTextSize(13);recentNotes.addView(hint);
  }else {
   // Two balanced columns of five on an unfolded Fold6; one column on narrow screens.
   boolean wide=getResources().getConfiguration().screenWidthDp>=500;
   LinearLayout columns=null,left=null,right=null;
   if(wide){
    columns=new LinearLayout(this);columns.setOrientation(LinearLayout.HORIZONTAL);
    left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);
    right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams leftParams=new LinearLayout.LayoutParams(0,-2,1);leftParams.rightMargin=dp(4);
    LinearLayout.LayoutParams rightParams=new LinearLayout.LayoutParams(0,-2,1);rightParams.leftMargin=dp(4);
    columns.addView(left,leftParams);columns.addView(right,rightParams);
    recentNotes.addView(columns,new LinearLayout.LayoutParams(-1,-2));
   }
   for(int i=0;i<(wide?10:Math.min(5,keys.size()));i++){
    if(i>=keys.size()){
     TextView placeholder=dashboardText("＋  Ruang nota "+(i+1),13,false);
     placeholder.setTextColor(0xff8a9290);placeholder.setGravity(Gravity.CENTER_VERTICAL);
     placeholder.setBackground(subtleNoteBorder(8));
     LinearLayout.LayoutParams emptyParams=new LinearLayout.LayoutParams(-1,dp(38));emptyParams.bottomMargin=dp(3);
     (i<5?left:right).addView(placeholder,emptyParams);
     placeholder.setOnClickListener(v->{maktabah=false;edit(null);});
     continue;
    }
    String k=keys.get(i);String t=prefs.getString("title_"+k,"");
    if(t.trim().isEmpty())t="Tanpa tajuk";
    // Light emerald list: slightly larger titles, dates consistently aligned right.
    FrameLayout row=new FrameLayout(this);
    row.setBackground(subtleNoteBorder(8));
    TextView item=dashboardText((k.startsWith("m_")?"▱  ":"▤  ")+t,14,false);
    item.setTextColor(0xff26352e);
    item.setPadding(dp(10),dp(5),dp(88),dp(5));
    item.setSingleLine(true);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
    row.addView(item,new FrameLayout.LayoutParams(-1,-2));
    long when=prefs.getLong("viewed_"+k,0L);
    if(when>0L){
     TextView date=dashboardText(new java.text.SimpleDateFormat("dd MMM",new java.util.Locale("ms","MY")).format(new java.util.Date(when)),12,false);
     date.setTextColor(0xff35644a);date.setGravity(Gravity.CENTER);
     date.setBackground(rounded(0xffeaf7eb,0xffeaf7eb,14));
     date.setPadding(dp(8),dp(3),dp(8),dp(3));
     FrameLayout.LayoutParams dateParams=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.CENTER_VERTICAL);
     dateParams.rightMargin=dp(8);row.addView(date,dateParams);
    }
    LinearLayout.LayoutParams itemParams=new LinearLayout.LayoutParams(-1,-2);
    itemParams.bottomMargin=dp(3);
    if(wide)(i<5?left:right).addView(row,itemParams);else recentNotes.addView(row,itemParams);
    applyTouchFeedback(row);row.setFocusable(true);
    row.setContentDescription("Buka "+(k.startsWith("m_")?"kitab ":"nota ")+t);
    row.setOnClickListener(v->{maktabah=k.startsWith("m_");read(k);});
   }
  }
  TextView add=dashboardText("＋  Nota baharu",15,true);add.setGravity(Gravity.CENTER);
  add.setBackground(silverGradient(8));
  add.setPadding(dp(12),dp(8),dp(12),dp(8));
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);
  ap.topMargin=dp(3);page.addView(add,ap);
  add.setContentDescription("Cipta nota baharu");add.setFocusable(true);
  add.setOnClickListener(v->{maktabah=false;edit(null);});
  applyTouchFeedback(add);applyTouchFeedback(recent);
 }
 void showHomeSearchResults(String query){
  int hits=0;String first="";
  for(String k:prefs.getAll().keySet())if(k.startsWith("n_")||k.startsWith("m_")){
   String title=prefs.getString("title_"+k,""),body=prefs.getString(k,"");
   if((title+" "+body).toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))){hits++;first=title;}
  }
  // Results count is announced without replacing the approved home screen.
  if(hits==1)android.widget.Toast.makeText(this,"Ditemui: "+first,android.widget.Toast.LENGTH_SHORT).show();
 }
 ArrayList<String> dashboardKeys(String field,boolean includeBooks){
  ArrayList<String> out=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if((k.startsWith("n_")||(includeBooks&&k.startsWith("m_")))&&
    (field.equals("pinned_")?prefs.getBoolean(field+k,false):prefs.getLong(field+k,0L)>0))out.add(k);
  Collections.sort(out,(a,b)->field.equals("pinned_")?a.compareTo(b):
   Long.compare(prefs.getLong(field+b,0),prefs.getLong(field+a,0)));
  return out;
 }
 void dashboardSection(LinearLayout target,String heading,ArrayList<String> keys,boolean pin){
  // Compact, consistent GitHub-style outline icons and tighter vertical rhythm.
  TextView title=dashboardText((pin?"⌖  ":"◷  ")+(pin?"Nota disemat":"Terakhir dikemaskini"),15,true);
  title.setPadding(dp(8),dp(5),dp(8),dp(3));target.addView(title);
  if(keys.isEmpty()){
   TextView empty=dashboardText(pin?"Belum ada nota disemat":"Belum ada nota dikemaskini",13,false);
   empty.setTextColor(0xff656d76);empty.setPadding(dp(10),dp(4),dp(10),dp(7));
   if(pin)target.addView(empty);return;
  }
  for(int i=0;i<Math.min(5,keys.size());i++){
   String key=keys.get(i),label=prefs.getString("title_"+key,"").trim();
   TextView entry=dashboardText("▤  "+(label.isEmpty()?"Tanpa tajuk":label),14,false);
   entry.setTextColor(0xff26352e);
   entry.setSingleLine(true);entry.setEllipsize(android.text.TextUtils.TruncateAt.END);
   entry.setPadding(dp(10),dp(6),dp(10),dp(6));
   entry.setBackground(subtleNoteBorder(8));
   LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);
   ep.bottomMargin=dp(3);target.addView(entry,ep);applyTouchFeedback(entry);
   entry.setFocusable(true);
   entry.setContentDescription("Buka nota "+(label.isEmpty()?"Tanpa tajuk":label));
   entry.setOnClickListener(v->{maktabah=key.startsWith("m_");read(key);});
  }
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

 boolean matchesSearch(String content,String[] terms){String normalized=content.replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);for(String term:terms)if(!normalized.contains(term))return false;return true;}
 void list(String filter){filter=filter.trim();String query=filter.replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);String[] terms=query.isEmpty()?new String[0]:query.split(" ");items.removeAllViews();ArrayList<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&prefs.getAll().get(k) instanceof String)keys.add(k);Collections.sort(keys,(a,b)->{
   boolean pinA=prefs.getBoolean("pinned_"+a,false),pinB=prefs.getBoolean("pinned_"+b,false);
   if(pinA!=pinB)return pinA?-1:1;
   if("Tajuk A–Z".equals(sortMode)){int byTitle=prefs.getString("title_"+a,"").trim().compareToIgnoreCase(prefs.getString("title_"+b,"").trim());if(byTitle!=0)return byTitle;}
   if("Kerap dibuka".equals(sortMode)){int byViews=Long.compare(prefs.getLong("viewed_"+b,0),prefs.getLong("viewed_"+a,0));if(byViews!=0)return byViews;}
   long ta=prefs.getLong("updated_"+a,0),tb=prefs.getLong("updated_"+b,0);
   int byDate="Terlama".equals(sortMode)?Long.compare(ta,tb):Long.compare(tb,ta);return byDate!=0?byDate:a.compareTo(b);
  });int matches=0;for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");if(!matchesSearch(heading+" "+v+" "+prefs.getString("cat_"+k,"Tanpa kategori"),terms))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;matches++;}if(matches>0){TextView countLabel=text(matches+" "+(maktabah?"kitab":"nota"),12);countLabel.setTextColor(0xff687078);countLabel.setPadding(dp(16),dp(10),dp(16),dp(6));items.addView(countLabel);}if(matches==0){TextView empty=text(keys.isEmpty()?(maktabah?"▤  Belum ada kitab\nTekan + Kitab baharu untuk bermula":"✎  Belum ada nota\nTekan + Nota baharu untuk mula mencatat"):"Tiada hasil carian yang sepadan",17);empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff727985);empty.setPadding(dp(20),dp(56),dp(20),dp(56));items.addView(empty);if(keys.isEmpty()&&query.isEmpty()){empty.setContentDescription(maktabah?"Tambah kitab baharu":"Tambah nota baharu");applyTouchFeedback(empty);empty.setOnClickListener(v->edit(null));}else{empty.setText(query.isEmpty()?(maktabah?"Tiada kitab dalam kategori ini":"Tiada nota dalam kategori ini"):"Tiada hasil carian yang sepadan");}if(!query.isEmpty())empty.setText("Tiada hasil untuk: "+(filter.length()>60?filter.substring(0,60)+"…":filter));}for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");String shown=(heading.trim().isEmpty()?(v.trim().isEmpty()?"Tanpa tajuk":v.trim().split("\\R",2)[0]):heading.trim());shown=shown.replaceAll("\\s+"," ").trim();if(shown.length()>70)shown=shown.substring(0,70)+"…";if(shown.trim().isEmpty())shown="Tanpa tajuk";if(!matchesSearch(heading+" "+v+" "+prefs.getString("cat_"+k,"Tanpa kategori"),terms))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;String preview=v.replaceAll("\\s+"," ").trim();if(preview.isEmpty())preview="Belum ada kandungan";String cat=prefs.getString("cat_"+k,"Tanpa kategori").trim();if(cat.isEmpty())cat="Tanpa kategori";long stamp=prefs.getLong("updated_"+k,0);String date=stamp>0?new java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(new java.util.Date(stamp)):"Belum dikemas kini";TextView t=text((prefs.getBoolean("pinned_"+k,false)?"📌  ":"")+(maktabah?"▤  ":"✎  ")+shown+"\n"+(preview.length()>80?preview.substring(0,80)+"…":preview)+"\n"+cat+"   "+date,15);t.setMaxLines(3);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setTextColor(0xff30343b);t.setGravity(Gravity.CENTER_VERTICAL);t.setLineSpacing(dp(3),1.05f);t.setBackground(rounded(0xffffffff,0xffe1e5e3,16));t.setElevation(dp(1));t.setPadding(dp(16),dp(14),dp(16),dp(14));LinearLayout.LayoutParams noteCardParams=new LinearLayout.LayoutParams(-1,-2);noteCardParams.setMargins(dp(12),dp(5),dp(12),dp(5));t.setContentDescription((prefs.getBoolean("pinned_"+k,false)?"Disemat. ":"")+(maktabah?"Buka kitab: ":"Buka nota: ")+shown+", kategori "+cat+", "+date);applyTouchFeedback(t);t.setFocusable(true);items.addView(t,noteCardParams);View line=new View(this);line.setBackgroundColor(0xffe5e9e5);--matches;t.setOnClickListener(w->read(k));}}

 void cleanBase(){readerDetector=null;root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.WHITE);setContentView(root);applySystemBarInsets(root);}
 Button action(String label,LinearLayout bar,Runnable callback){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setBackground(rounded(0xfff6f8fa,0xffe1e7e3,12));LinearLayout.LayoutParams buttonParams=new LinearLayout.LayoutParams(-2,dp(42));buttonParams.setMargins(dp(3),dp(5),dp(3),dp(5));bar.addView(b,buttonParams);b.setOnClickListener(v->callback.run());return b;}
 void returnFromReader(){selected=null;if(sectionOpen)notesPage();else home();}


 void showPagedReader(String key){
  final String source=prefs.getString(key,"");
  final boolean spread=getResources().getConfiguration().screenWidthDp>=500&&prefs.getBoolean("reader_two_columns",true);
  final int fontLevel=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  final int fontSize=(key.startsWith("m_")?new int[]{15,18,21,25}:new int[]{13,15,18,22})[fontLevel];
  final int theme=prefs.getInt("reader_theme",0);
  final int paper=theme==0?0xffffffff:theme==2?0xfff2f8f3:0xfffffcf4;
  final String heading=prefs.getString("title_"+key,"");
  TextView headingView=text(heading,17);headingView.setGravity(Gravity.CENTER);headingView.setTypeface(null,android.graphics.Typeface.BOLD);headingView.setMaxLines(2);
  headingView.setEllipsize(android.text.TextUtils.TruncateAt.END);
  headingView.setPadding(dp(16),dp(8),dp(16),dp(8));
  headingView.setBackground(rounded(paper,0xffdde5e1,2));headingView.setElevation(0);
  LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);
  titleParams.setMargins(dp(10),dp(6),dp(10),dp(4));root.addView(headingView,titleParams);
  LinearLayout pageRow=new LinearLayout(this);pageRow.setOrientation(LinearLayout.HORIZONTAL);
  pageRow.setPadding(dp(8),dp(1),dp(8),dp(1));
  FrameLayout readingFrame=new FrameLayout(this);
  root.addView(readingFrame,new LinearLayout.LayoutParams(-1,0,1));
  readingFrame.addView(pageRow,new FrameLayout.LayoutParams(-1,-1));
  // Minimal reader controls: bookmark and progress only; swipe changes pages.
  LinearLayout navigation=new LinearLayout(this);navigation.setGravity(Gravity.CENTER_VERTICAL);
  navigation.setPadding(dp(8),dp(1),dp(8),dp(1));
  Button bookmark=new Button(this);bookmark.setText("♧");bookmark.setContentDescription("Simpan penanda halaman; tekan lama untuk pergi ke penanda");
  TextView indicator=text("",11);indicator.setGravity(Gravity.CENTER);indicator.setPadding(dp(2),0,dp(2),0);
  indicator.setContentDescription("Kemajuan bacaan; tekan untuk pergi ke halaman");
  for(Button control:new Button[]{bookmark}){
   control.setAllCaps(false);control.setTextSize(16);
   control.setMinWidth(0);control.setMinimumWidth(0);control.setMinHeight(0);control.setMinimumHeight(0);
   control.setPadding(0,0,0,0);
   control.setTextColor(0xff176b47);
   control.setBackground(rounded(0xffeff8f0,0xffb8d9c2,9));
  }
  LinearLayout.LayoutParams controlParams;
  // Bookmark remains in the upper reader toolbar; omit duplicate lower control.
  // Progress label sits on the lower reading-frame border, not in the toolbar.
  indicator.setTextColor(0xff356b50);
  indicator.setBackgroundColor(paper);
  indicator.setPadding(dp(9),0,dp(9),0);
  FrameLayout.LayoutParams progressParams=new FrameLayout.LayoutParams(-2,dp(22),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
  progressParams.bottomMargin=dp(1);
  readingFrame.addView(indicator,progressParams);
  indicator.bringToFront();
  LinearLayout.LayoutParams navigationParams=new LinearLayout.LayoutParams(-1,-2);
  navigationParams.bottomMargin=dp(5); // Small gap above Android system navigation.
  root.addView(navigation,navigationParams);
  Button jump=new Button(this); // Dialog action retained without a second navigation row.
  // Paginate after the view has been measured. Avoid repeated layout passes and
  // view-tree changes while Android is measuring the page content.
  // Display saved content immediately, even before pagination is calculated.
  TextView loading=text(source,Math.max(12,fontSize));loading.setTextColor(0xff26372e);loading.setPadding(dp(12),dp(12),dp(12),dp(12));pageRow.addView(loading,new LinearLayout.LayoutParams(-1,-1));
  pageRow.post(()->{
   if(pageRow.getParent()==null||root==null||!key.equals(selected)||!pageRow.isAttachedToWindow())return;
   try{
    // Measure actual rendered lines, not an estimated character count.
    // Keep a small bottom reserve so the final line is never clipped.
    int width=Math.max(dp(80),(pageRow.getWidth()-dp(16)-(spread?dp(8):0))/(spread?2:1));
    int textWidth=Math.max(dp(40),width-dp(24));
    int usableHeight=Math.max(dp(40),pageRow.getHeight()-dp(20)-dp(22));
    // Use identical line spacing in the measuring layout and visible pages.
    final boolean arabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(source).find();
    final int extraLine=arabic?dp(3):dp(1);
    final float lineMultiplier=arabic?1.12f:1.04f;
    TextView probe=text("",fontSize);
    probe.setLineSpacing(extraLine,lineMultiplier);
    probe.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    android.text.TextPaint paint=probe.getPaint();
    ArrayList<String> parts=new ArrayList<>();
     ArrayList<Integer> repeatedPrefixLengths=new ArrayList<>();
     int previousStop=0;
    int cursor=0;
    while(cursor<source.length()){
     // Limit the candidate for speed; a full page cannot contain this many
     // characters at the configured text size.
     int candidateEnd=Math.min(source.length(),cursor+6000);
     CharSequence candidate=source.subSequence(cursor,candidateEnd);
     android.text.StaticLayout layout=android.text.StaticLayout.Builder
      .obtain(candidate,0,candidate.length(),paint,textWidth)
      .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
      .setTextDirection(android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
      .setLineSpacing(extraLine,lineMultiplier)
      .setIncludePad(true).build();
     int lastLine=0;
     for(int line=0;line<layout.getLineCount();line++){
      if(layout.getLineBottom(line)>usableHeight)break;
      lastLine=line+1;
     }
     int chars=lastLine>0?layout.getLineEnd(lastLine-1):Math.min(candidate.length(),1);
      // Reserve the last rendered line as a reading overlap on the next page.
      // Advance by at least one character so pagination always terminates.
      int overlapStart=lastLine>1?layout.getLineStart(lastLine-1):0;
     int stop=Math.min(source.length(),cursor+Math.max(1,chars));
     // Never split a surrogate pair, and preserve every character in order.
     if(stop<source.length()&&stop>cursor&&Character.isHighSurrogate(source.charAt(stop-1))&&Character.isLowSurrogate(source.charAt(stop)))stop--;
     if(stop<=cursor)stop=cursor+1;
     parts.add(source.substring(cursor,stop));
      repeatedPrefixLengths.add(Math.max(0,Math.min(stop-cursor,previousStop-cursor)));
      previousStop=stop;
     cursor=stop;
      if(lastLine>1 && overlapStart>0 && cursor<source.length()){
       int nextCursor=cursor-(chars-overlapStart);
       if(nextCursor>cursor-chars && nextCursor<cursor)cursor=nextCursor;
      }
    }
    if(parts.isEmpty()){parts.add("");repeatedPrefixLengths.add(0);}
    final int total=parts.size();
    final int[] position={Math.min(total-1,Math.max(0,prefs.getInt("reading_page_"+key,0)))};
    if(spread)position[0]=position[0]/2*2;
    final Runnable[] render={null};
    render[0]=()->{
     pageRow.removeAllViews();
     for(int i=0;i<(spread?2:1);i++){
      int pageNumber=position[0]+i;
      TextView page=text(pageNumber<total?parts.get(pageNumber):"",fontSize);
       if(pageNumber<total && repeatedPrefixLengths.get(pageNumber)>0){
        String visible=parts.get(pageNumber);
        int repeated=Math.min(visible.length(),repeatedPrefixLengths.get(pageNumber));
        android.text.SpannableString marked=new android.text.SpannableString(visible);
        marked.setSpan(new android.text.style.BackgroundColorSpan(0xfffff0bd),0,repeated,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        marked.setSpan(new android.text.style.UnderlineSpan(),0,repeated,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        page.setText(marked);
       }
      page.setTextColor(0xff26372e);page.setGravity(Gravity.TOP);
      page.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
      page.setLineSpacing(extraLine,lineMultiplier);
      page.setPadding(dp(12),dp(10),dp(12),dp(10));
      page.setBackground(rounded(paper,0xffdde5e1,2));page.setElevation(0);
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
      if(i>0)lp.leftMargin=dp(8);
      pageRow.addView(page,lp);
      page.setOnTouchListener(new View.OnTouchListener(){
       float x,y;
       public boolean onTouch(View v,android.view.MotionEvent event){
        if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){x=event.getX();y=event.getY();return true;}
        if(event.getAction()==android.view.MotionEvent.ACTION_UP){
         float dx=event.getX()-x;
         if(Math.abs(dx)>dp(65)&&Math.abs(dx)>Math.abs(event.getY()-y)*1.5f){
          position[0]=Math.max(0,Math.min(total-1,position[0]+(dx<0?(spread?2:1):-(spread?2:1))));
          if(spread)position[0]=position[0]/2*2;
          render[0].run();
         }
         return true;
        }
        return true;
       }
      });
     }
     indicator.setText((position[0]+1)+(spread&&position[0]+1<total?"–"+(position[0]+2):"")+" / "+total+" · "+Math.min(100,Math.round((position[0]+(spread?2:1))*100f/total))+"%");
     prefs.edit().putInt("reading_page_"+key,position[0]).apply();
    };
    bookmark.setOnClickListener(v->{prefs.edit().putInt("bookmark_page_"+key,position[0]).apply();Toast.makeText(this,"Penanda disimpan",Toast.LENGTH_SHORT).show();});
    jump.setOnClickListener(v->{
     EditText field=new EditText(this);field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);field.setHint("1 – "+total);
     new AlertDialog.Builder(this).setTitle("Pergi ke halaman").setView(field).setPositiveButton("Pergi",(d,w)->{
      try{int target=Integer.parseInt(field.getText().toString())-1;if(target>=0&&target<total){position[0]=spread?target/2*2:target;render[0].run();}}
      catch(NumberFormatException ignored){}
     }).setNeutralButton("Penanda",(d,w)->{position[0]=Math.max(0,Math.min(total-1,prefs.getInt("bookmark_page_"+key,0)));if(spread)position[0]=position[0]/2*2;render[0].run();}).setNegativeButton("Batal",null).show();
    });
    indicator.setOnClickListener(v->jump.performClick());
    bookmark.setOnLongClickListener(v->{position[0]=Math.max(0,Math.min(total-1,prefs.getInt("bookmark_page_"+key,0)));if(spread)position[0]=position[0]/2*2;render[0].run();return true;});
    render[0].run();
   }catch(Exception error){
    android.util.Log.e("DinNote","Paged reader error",error);
    prefs.edit().putBoolean("reader_safe_scroll_"+key,true).apply();
    if(key.equals(selected))read(key);
   }
  });
 }

 void read(String key){
  editor=null;titleEditor=null;saveIndicator=null;selected=key;prefs.edit().putLong("viewed_"+key,System.currentTimeMillis()).putString(key.startsWith("m_")?"reading_last_m":"reading_last_n",key).apply();cleanBase();
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(3),dp(8),dp(3));bar.setBackground(rounded(0xfff8faf8,0xffe1e7e3,12));root.addView(bar);
  Button readerBack=action("‹",bar,()->returnFromReader());readerBack.setContentDescription(key.startsWith("m_")?"Kembali ke senarai kitab":"Kembali ke senarai nota");
  TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
  Button readerFont=action("Aa",bar,()->{new AlertDialog.Builder(this).setTitle("Tetapan bacaan").setItems(new String[]{"Saiz tulisan","Latar Putih","Latar Ivory","Latar Hijau Lembut","Jarak baris: biasa","Jarak baris: luas"},(dialog,choice)->{if(choice==0)new AlertDialog.Builder(this).setTitle("Saiz tulisan").setSingleChoiceItems(new String[]{"Kecil","Biasa","Besar","Sangat besar"},prefs.getInt("reader_font_level",1),(d,which)->{prefs.edit().putInt("reader_font_level",which).apply();d.dismiss();read(key);}).setNegativeButton("Batal",null).show();else if(choice<=3){prefs.edit().putInt("reader_theme",choice-1).apply();read(key);}else{prefs.edit().putBoolean("reader_wide_spacing",choice==5).apply();read(key);}}).show();});readerFont.setContentDescription("Ubah saiz tulisan Nota dan Maktabah");
  Button bookmarkButton=action("♧",bar,()->{int pos=prefs.getInt("reading_scroll_"+key,0);new AlertDialog.Builder(this).setTitle("Penanda bacaan").setMessage("Kedudukan terakhir: "+pos+" px").setPositiveButton("Simpan penanda",(d,w)->{prefs.edit().putInt("bookmark_scroll_"+key,prefs.getInt("reading_scroll_"+key,0)).apply();Toast.makeText(this,"Penanda disimpan",Toast.LENGTH_SHORT).show();}).setNeutralButton("Pergi ke penanda",(d,w)->{int target=prefs.getInt("bookmark_scroll_"+key,-1);if(target>=0){read(key);root.post(()->{ScrollView currentScroll=(ScrollView)root.getChildAt(root.getChildCount()-2);currentScroll.post(()->currentScroll.scrollTo(0,target));});}else Toast.makeText(this,"Tiada penanda",Toast.LENGTH_SHORT).show();}).setNegativeButton("Tutup",null).show();});bookmarkButton.setContentDescription("Penanda bacaan");Button readerEdit=action("✎ Edit",bar,()->edit(key));readerEdit.setContentDescription(key.startsWith("m_")?"Sunting kandungan kitab":"Sunting kandungan nota");
  Button swipeMode=action(prefs.getBoolean("reader_swipe_mode",false)?"⇆ ✓":"⇆",bar,()->{boolean next=!prefs.getBoolean("reader_swipe_mode",false);prefs.edit().putBoolean("reader_swipe_mode",next).apply();read(key);});swipeMode.setContentDescription("Mod swipe halaman untuk skrin terbuka; ulang baris akhir halaman sebelumnya"); Button layoutButton=action("▥",bar,()->{boolean now=prefs.getBoolean("reader_two_columns",getResources().getConfiguration().screenWidthDp>=600);prefs.edit().putBoolean("reader_two_columns",!now).apply();read(key);});layoutButton.setContentDescription("Tukar paparan satu atau dua kolum");layoutButton.setTextColor(0xff176b47); Button readerOptions=action("⋮",bar,()->new AlertDialog.Builder(this).setItems(new String[]{"Edit","Semat / Buka sematan","Duplikasi","Padam"},(d,i)->{if(i==0)edit(key);else if(i==1){boolean pinned=prefs.getBoolean("pinned_"+key,false);prefs.edit().putBoolean("pinned_"+key,!pinned).apply();android.widget.Toast.makeText(this,pinned?"Sematan dibuang":"Nota disemat",android.widget.Toast.LENGTH_SHORT).show();read(key);}else if(i==2)duplicateNote(key);else confirmDelete(key);}).show());readerOptions.setContentDescription(key.startsWith("m_")?"Menu tindakan kitab":"Menu tindakan nota");
  // Page-by-page reader for long Nota and Maktabah content.
  if(prefs.getString(key,"").length()>=100 && getResources().getConfiguration().screenWidthDp>=500 && prefs.getBoolean("reader_swipe_mode",false)){ // Optional unfolded swipe reader.
   try{showPagedReader(key);return;}
   catch(RuntimeException problem){
    android.util.Log.e("DinNote","Paged reader failed; using safe scrolling",problem);
    // Do not recursively call read(): that can overflow the stack and close the app.
    prefs.edit().putBoolean("reader_safe_scroll_"+key,true).apply();
    cleanBase();
   }
  }
  String heading=prefs.getString("title_"+key,"");
  TextView sectionLabel=text(key.startsWith("m_")?"▤  Maktabah":"✎  Nota",12);sectionLabel.setTextColor(0xff23714b);sectionLabel.setPadding(dp(20),dp(10),dp(20),dp(2));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
  LinearLayout body=new LinearLayout(this);body.setOrientation(1);int readingTheme=prefs.getInt("reader_theme",0);int paperColor=readingTheme==0?0xffffffff:readingTheme==2?0xfff2f8f3:0xfffffcf4;body.setBackground(rounded(paperColor,0xffdde5e1,12));LinearLayout.LayoutParams readingFrame=new LinearLayout.LayoutParams(-1,-2);readingFrame.setMargins(dp(10),dp(8),dp(10),dp(12));scroll.addView(body,readingFrame);body.addView(sectionLabel);
  TextView title=text(heading,20);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setBackgroundColor(paperColor);title.setTextColor(green);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setPadding(dp(20),dp(6),dp(20),dp(8));title.setTextIsSelectable(true);title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setIncludeFontPadding(true);
  if(!heading.isEmpty())body.addView(title);
  String readingCategory=prefs.getString("cat_"+key,"Tanpa kategori");String fullReadingText=prefs.getString(key,"");String firstReadingLine=fullReadingText.split("\\R",2)[0].trim();if(firstReadingLine.matches("(?i)^(bab|الفصل|باب)\\b.*")&&firstReadingLine.length()<110){TextView chapterLabel=text(firstReadingLine,16);chapterLabel.setTextColor(0xff176b47);chapterLabel.setTypeface(null,android.graphics.Typeface.BOLD);chapterLabel.setPadding(dp(20),dp(8),dp(20),dp(8));body.addView(chapterLabel);}TextView categoryLine=text("⌑  "+readingCategory,12);categoryLine.setTextColor(0xff52745e);categoryLine.setPadding(dp(20),0,dp(20),dp(8));body.addView(categoryLine);View chapterRule=new View(this);chapterRule.setBackgroundColor(0xffdfe8df);LinearLayout.LayoutParams chapterRuleParams=new LinearLayout.LayoutParams(-1,dp(1));chapterRuleParams.setMargins(dp(20),dp(2),dp(20),dp(8));body.addView(chapterRule,chapterRuleParams);
  int fontLevel=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  int[] readingSizes=key.startsWith("m_")?new int[]{15,18,21,25}:new int[]{13,15,18,22};
  TextView content=text(prefs.getString(key,""),readingSizes[fontLevel]);boolean containsArabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(fullReadingText).find();if(containsArabic)content.setLineSpacing(dp(7),1.2f);
  content.setTextColor(0xff26372e);content.setGravity(Gravity.TOP);content.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);content.setTextIsSelectable(true);content.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);int readingSide=dp(12);content.setPadding(readingSide,dp(10),readingSide,dp(12));if(prefs.getBoolean("reader_wide_spacing",false))content.setLineSpacing(dp(9),1.3f);else if(!containsArabic)content.setLineSpacing(dp(3),1.12f);content.setIncludeFontPadding(true);
  // Display list rows with subtle mint dividers; preserve the original stored text.
  String[] displayLines=fullReadingText.split("\\n",-1);
  boolean hasLists=false;
  for(String line:displayLines)if(line.trim().equals("---")){hasLists=true;break;}
  for(String line:displayLines)if(line.matches("^\\s*(?:\\d+\\.\\s+|[•☐☑✓]\\s+).*")){hasLists=true;break;}
  boolean twoColumnReading=getResources().getConfiguration().screenWidthDp>=500&&prefs.getBoolean("reader_two_columns",true)&&fullReadingText.length()>=100;
  if(!twoColumnReading&&!hasLists)body.addView(content,new LinearLayout.LayoutParams(-1,-2));
  else if(twoColumnReading){
   // Split at a word boundary for balanced side-by-side reading on unfolded devices.
   int middle=fullReadingText.length()/2;
   int split=fullReadingText.indexOf('\n',middle);
   if(split<0)split=fullReadingText.indexOf(' ',middle);
   if(split<0){int next=middle;while(next<fullReadingText.length()&&!Character.isWhitespace(fullReadingText.charAt(next)))next++;split=next<fullReadingText.length()?next:middle;}
   LinearLayout columns=new LinearLayout(this);columns.setOrientation(LinearLayout.HORIZONTAL);
   columns.setPadding(dp(4),dp(8),dp(4),dp(12));
   // Preserve every original character, including newlines and whitespace at the split.
    String[] halves={fullReadingText.substring(0,split),fullReadingText.substring(split)};
   for(int i=0;i<2;i++){
    TextView page=text(halves[i],readingSizes[fontLevel]);page.setTextColor(0xff26372e);
    page.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);page.setTextIsSelectable(true);
    page.setGravity(Gravity.TOP);page.setPadding(dp(10),dp(8),dp(10),dp(8));
    page.setLineSpacing(dp(containsArabic?7:3),containsArabic?1.2f:1.12f);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
    if(i==1)lp.leftMargin=dp(4);
    columns.addView(page,lp);
    if(i==0){View islamicDivider=new View(this){android.graphics.Paint p=new android.graphics.Paint(3);protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float x=getWidth()/2f,h=getHeight();p.setColor(0xff176b47);p.setStrokeWidth(dp(1));c.drawLine(x,0,x,h,p);p.setColor(0xffc5a66b);float y=h/2f,r=dp(7);android.graphics.Path star=new android.graphics.Path();for(int j=0;j<16;j++){double a=Math.PI*j/8-Math.PI/2;float rad=j%2==0?r:r*.52f;float px=x+(float)Math.cos(a)*rad,py=y+(float)Math.sin(a)*rad;if(j==0)star.moveTo(px,py);else star.lineTo(px,py);}star.close();c.drawPath(star,p);}};columns.addView(islamicDivider,new LinearLayout.LayoutParams(dp(18),-1));}
   }
   body.addView(columns);
  }
  else{
   LinearLayout readingRows=new LinearLayout(this);readingRows.setOrientation(1);
   readingRows.setPadding(readingSide,dp(8),readingSide,dp(12));
   body.addView(readingRows);
   for(String line:displayLines){
    java.util.regex.Matcher numbered=java.util.regex.Pattern.compile("^\\s*(\\d+)\\.\\s+(.*)$").matcher(line);
    java.util.regex.Matcher marked=java.util.regex.Pattern.compile("^\\s*([•☐☑✓])\\s+(.*)$").matcher(line);
    boolean isNumber=numbered.matches(),isMarked=!isNumber&&marked.matches();
    if(line.trim().equals("---")){
     LinearLayout ornament=new LinearLayout(this);ornament.setGravity(Gravity.CENTER_VERTICAL);
     ornament.setPadding(dp(16),dp(12),dp(16),dp(12));
     View leftRule=new View(this);leftRule.setBackgroundColor(0xffc4a45a);
     ornament.addView(leftRule,new LinearLayout.LayoutParams(0,dp(1),1));
     TextView diamond=text("◇",17);diamond.setTextColor(0xffae913d);
     diamond.setPadding(dp(10),0,dp(10),0);ornament.addView(diamond);
     View rightRule=new View(this);rightRule.setBackgroundColor(0xffc4a45a);
     ornament.addView(rightRule,new LinearLayout.LayoutParams(0,dp(1),1));
     readingRows.addView(ornament);
    }else if(isNumber||isMarked){
     LinearLayout itemRow=new LinearLayout(this);itemRow.setOrientation(LinearLayout.HORIZONTAL);
     itemRow.setGravity(Gravity.TOP|Gravity.CENTER_VERTICAL);itemRow.setPadding(dp(6),dp(8),dp(6),dp(8));
     String marker=isNumber?numbered.group(1):marked.group(1);
     String value=isNumber?numbered.group(2):marked.group(2);
     TextView markerView=text(marker,14);markerView.setGravity(Gravity.CENTER);
     markerView.setTextColor(0xff167c42);
     markerView.setBackground(rounded(0xffe4f7e8,0xffc8e9d0,16));
     itemRow.addView(markerView,new LinearLayout.LayoutParams(dp(30),dp(30)));
     TextView valueView=text(value,readingSizes[fontLevel]);valueView.setTextColor(0xff26372e);
     valueView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);valueView.setTextIsSelectable(true);
     valueView.setPadding(dp(10),0,dp(4),0);valueView.setLineSpacing(dp(4),1.12f);
     itemRow.addView(valueView,new LinearLayout.LayoutParams(0,-2,1));
     readingRows.addView(itemRow);
     View fineDivider=new View(this);fineDivider.setBackgroundColor(0xffc5e7cb);
     readingRows.addView(fineDivider,new LinearLayout.LayoutParams(-1,dp(1)));
    }else{
     TextView paragraph=text(line.isEmpty()?" ":line,readingSizes[fontLevel]);
     paragraph.setTextColor(0xff26372e);paragraph.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
     paragraph.setTextIsSelectable(true);paragraph.setPadding(dp(4),dp(4),dp(4),dp(4));
     paragraph.setLineSpacing(dp(5),1.12f);readingRows.addView(paragraph);
    }
   }
  }
  String readingText=prefs.getString(key,"").trim();
  int wordCount=readingText.isEmpty()?0:readingText.split("\\s+").length;
  long readingUpdated=prefs.getLong("updated_"+key,0L);String updatedLabel=readingUpdated>0?new java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(new java.util.Date(readingUpdated)):"—";TextView wordCounter=text(wordCount+" perkataan  ·  Dikemas kini "+updatedLabel,12);
  wordCounter.setTextColor(0xff687078);
  wordCounter.setPadding(dp(20),dp(8),dp(20),dp(20));
  body.addView(wordCounter);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
   // Compact reading progress indicator shared by Nota and Maktabah.
   FrameLayout progressFrame=new FrameLayout(this);
   progressFrame.setBackgroundColor(0xfff8faf8);
   ProgressBar readingProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
   readingProgress.setMax(100);
   readingProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff176b47));
   readingProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffe4f3e9));
   FrameLayout.LayoutParams trackParams=new FrameLayout.LayoutParams(-1,dp(3),Gravity.TOP);
   progressFrame.addView(readingProgress,trackParams);
   TextView readingPercent=text("0%",10);
   readingPercent.setTextColor(0xff52745e);
   readingPercent.setGravity(Gravity.CENTER);
   readingPercent.setPadding(dp(5),0,dp(5),0);
   readingPercent.setBackgroundColor(0xfff8faf8);
   FrameLayout.LayoutParams percentParams=new FrameLayout.LayoutParams(-2,dp(18),Gravity.TOP|Gravity.RIGHT);
   percentParams.rightMargin=dp(8);
   progressFrame.addView(readingPercent,percentParams);
   root.addView(progressFrame,new LinearLayout.LayoutParams(-1,dp(18)));
   Runnable updateReadingProgress=()->{
    if(scroll.getChildCount()==0)return;
    int maxScroll=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());
    int progress=maxScroll==0?100:Math.max(0,Math.min(100,Math.round(scroll.getScrollY()*100f/maxScroll)));
    readingProgress.setProgress(progress);
    readingPercent.setText(progress+"%");
   };
   scroll.getViewTreeObserver().addOnScrollChangedListener(updateReadingProgress::run);
   scroll.post(updateReadingProgress);int rememberedScroll=prefs.getInt("reading_scroll_"+key,0);scroll.post(()->scroll.scrollTo(0,Math.max(0,rememberedScroll)));scroll.setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_UP)scroll.postDelayed(()->prefs.edit().putInt("reading_scroll_"+key,scroll.getScrollY()).apply(),160);return false;});
  readerDetector=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){bar.setVisibility(bar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);return true;}
  });
  // Touch events are observed once in dispatchTouchEvent, including selectable text.
  // Long press is a reliable alternative when double tap is intercepted by text selection.
  body.setOnLongClickListener(v->{edit(key);return true;});
  scroll.setOnLongClickListener(v->{edit(key);return true;});
  bar.setVisibility(View.VISIBLE);
 }
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
  if(readerDetector!=null)readerDetector.onTouchEvent(event);
  return super.dispatchTouchEvent(event);
 }
 void duplicateNote(String sourceKey){
  if(sourceKey==null||!prefs.contains(sourceKey))return;
  String copyKey=(sourceKey.startsWith("m_")?"m_":"n_")+java.util.UUID.randomUUID().toString();
  String sourceTitle=prefs.getString("title_"+sourceKey,"");
  String copyTitle=(sourceTitle.isEmpty()?"Tanpa tajuk":sourceTitle)+" (Salinan)";
  boolean saved=prefs.edit().putString(copyKey,prefs.getString(sourceKey,""))
   .putString("title_"+copyKey,copyTitle)
   .putString("cat_"+copyKey,prefs.getString("cat_"+sourceKey,"Tanpa kategori"))
   .putLong("updated_"+copyKey,System.currentTimeMillis()).commit();
  if(saved){android.widget.Toast.makeText(this,"Salinan berjaya dibuat",android.widget.Toast.LENGTH_SHORT).show();read(copyKey);}
  else android.widget.Toast.makeText(this,"Gagal membuat salinan",android.widget.Toast.LENGTH_LONG).show();
 }
 void confirmDelete(String key){new AlertDialog.Builder(this).setMessage("Padam nota ini?").setPositiveButton("Padam",(d,w)->{android.content.SharedPreferences.Editor deletion=prefs.edit().remove(key).remove("title_"+key).remove("cat_"+key).remove("viewed_"+key).remove("updated_"+key).remove("pinned_"+key);if(key.contains("_sample_"))deletion.putBoolean("deleted_"+key,true);if(key.equals(prefs.getString("last_edited_key","")))deletion.remove("last_edited_key");deletion.apply();editor=null;titleEditor=null;categoryPicker=null;saveIndicator=null;selected=null;if(sectionOpen)notesPage();else home();}).setNegativeButton("Batal",null).show();}
 void finishEditingSafely(){
  if(editor==null)return;
  save();
  final String finishedKey=selected;
  final EditText oldEditor=editor;
  final EditText oldTitle=titleEditor;
  // Disconnect the IME from editable views before replacing the activity content.
  android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  if(imm!=null){android.os.IBinder token=oldEditor.getWindowToken();if(token!=null)imm.hideSoftInputFromWindow(token,0);}
  oldEditor.clearFocus();
  if(oldTitle!=null)oldTitle.clearFocus();
  oldEditor.setOnFocusChangeListener(null);
  readerDetector=null;
  // Let the keyboard and selection controller finish their current event.
  oldEditor.postDelayed(()->{
   if(editor!=oldEditor)return;
   editor=null;titleEditor=null;saveIndicator=null;categoryPicker=null;
   if(finishedKey!=null){prefs.edit().putBoolean("reader_safe_scroll_"+finishedKey,true).apply();read(finishedKey);}
   else if(sectionOpen)notesPage();else home();
  },180);
 }
 void edit(String key){readerDetector=null;if(key!=null){
  if(!prefs.contains(key)){
   Toast.makeText(this,"Nota tidak ditemui atau telah dipadam",Toast.LENGTH_LONG).show();
   selected=null;editor=null;titleEditor=null;categoryPicker=null;
   if(sectionOpen)notesPage();else home();
   return;
  }
  maktabah=key.startsWith("m_");prefs.edit().putString("last_edited_key",key).apply();}editor=null;saveIndicator=null;selected=key;cleanBase();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);action("‹",bar,()->finishEditingSafely());TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));final java.util.ArrayDeque<String> undoHistory=new java.util.ArrayDeque<>();final java.util.ArrayDeque<String> redoHistory=new java.util.ArrayDeque<>();final boolean[] restoring={false};final TextView[] historyButtons=new TextView[2];historyButtons[0]=action("↶",bar,()->{if(editor==null||undoHistory.isEmpty())return;restoring[0]=true;String current=editor.getText().toString();String previous=undoHistory.removeLast();redoHistory.addLast(current);editor.setText(previous);editor.setSelection(previous.length());restoring[0]=false;save();historyButtons[0].setAlpha(undoHistory.isEmpty()?0.35f:1f);historyButtons[1].setAlpha(1f);});historyButtons[1]=action("↷",bar,()->{if(editor==null||redoHistory.isEmpty())return;restoring[0]=true;String current=editor.getText().toString();String next=redoHistory.removeLast();undoHistory.addLast(current);editor.setText(next);editor.setSelection(next.length());restoring[0]=false;save();historyButtons[0].setAlpha(1f);historyButtons[1].setAlpha(redoHistory.isEmpty()?0.35f:1f);});historyButtons[0].setContentDescription("Undo perubahan teks");historyButtons[1].setContentDescription("Redo perubahan teks");for(TextView historyButton:historyButtons){historyButton.setTextSize(23);historyButton.setTextColor(0xff146b45);historyButton.setBackground(rounded(0xfff0f5f2,0xffc6d9cf,12));historyButton.setPadding(dp(9),dp(4),dp(9),dp(4));}historyButtons[0].setAlpha(0.45f);historyButtons[1].setAlpha(0.45f);action("Selesai",bar,()->finishEditingSafely());action("⋮",bar,()->{String[] options=maktabah?new String[]{"Masukkan tarikh","Masukkan masa","Masukkan tarikh dan masa","Padam"}:new String[]{"Masukkan tarikh","Masukkan masa","Masukkan tarikh dan masa","Padam"};new AlertDialog.Builder(this).setItems(options,(d,i)->{if(i<3)insertCurrentDateTime(i);else if(selected!=null)confirmDelete(selected);}).show();});categoryPicker=new Spinner(this);categoryPicker.setContentDescription(maktabah?"Kategori kitab":"Kategori nota");ArrayList<String> choices=new ArrayList<>();choices.add("Tanpa kategori");choices.addAll(categories());categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));String savedCat=key==null?"Tanpa kategori":prefs.getString("cat_"+key,"Tanpa kategori");
  // Preserve an existing note category even if it is missing from the category list.
  if(!choices.contains(savedCat)){
   choices.add(savedCat);
   categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));
  }
  int pos=choices.indexOf(savedCat);categoryPicker.setSelection(pos<0?0:pos);root.addView(categoryPicker);saveIndicator=text("Autosimpan aktif",12);saveIndicator.setTextColor(0xff687078);saveIndicator.setPadding(dp(24),dp(4),dp(24),dp(4));root.addView(saveIndicator);titleEditor=new EditText(this);titleEditor.setSingleLine(true);titleEditor.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);titleEditor.setTextSize(20);titleEditor.setHint(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setContentDescription(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setPadding(dp(24),dp(12),dp(24),dp(12));titleEditor.setBackground(rounded(Color.WHITE,0xffdde5e1,12));titleEditor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);root.addView(titleEditor);if(key!=null)titleEditor.setText(prefs.getString("title_"+key,""));editor=new EditText(this);editor.setGravity(Gravity.TOP);editor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);editor.setTextSize(new int[]{13,15,18,22}[Math.max(0,Math.min(3,prefs.getInt("editor_font_level",1)))]);editor.setTextColor(0xff30343b);editor.setPadding(dp(24),dp(16),dp(24),dp(16));editor.setLineSpacing(dp(5),1.08f);editor.setIncludeFontPadding(true);editor.setHint(maktabah?"Tulis kandungan kitab di sini…":"Tulis nota di sini…");editor.setContentDescription(maktabah?"Kandungan kitab":"Kandungan nota");editor.setBackground(rounded(Color.WHITE,0xffdde5e1,12));editor.setOnFocusChangeListener((v,focused)->editor.setBackground(rounded(Color.WHITE,focused?0xff81c99b:0xffdde5e1,12)));editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);LinearLayout formatBar=new LinearLayout(this);
  formatBar.setGravity(Gravity.CENTER_VERTICAL);
  formatBar.setPadding(dp(5),dp(2),dp(5),dp(2));
  formatBar.setHorizontalScrollBarEnabled(false);
  formatBar.setBackground(rounded(0xfff0faf2,0xffc9e8cf,12));
  HorizontalScrollView formatScroll=new HorizontalScrollView(this);
  formatScroll.setHorizontalScrollBarEnabled(false);
  formatScroll.setFillViewport(false);
  root.addView(formatScroll);
  formatScroll.addView(formatBar);
  Button boldButton=action("B",formatBar,()->wrapEditorSelection("**","**"));
  boldButton.setTypeface(null,android.graphics.Typeface.BOLD);boldButton.setContentDescription("Tebal: tandakan teks terpilih");
  Button italicButton=action("I",formatBar,()->wrapEditorSelection("*","*"));
  italicButton.setTypeface(null,android.graphics.Typeface.ITALIC);italicButton.setContentDescription("Condong: tandakan teks terpilih");
  Button bulletButton=action("☷",formatBar,()->insertEditorText("• "));
  bulletButton.setContentDescription("Masukkan senarai");
  Button numberedButton=action("1.",formatBar,()->insertNumberedList());
  numberedButton.setTextColor(0xff17814a);numberedButton.setContentDescription("Senarai bernombor automatik");
  Button checklistButton=action("☑",formatBar,()->insertEditorText("☐ "));
  checklistButton.setContentDescription("Masukkan kotak semak");
  Button underlineButton=action("U",formatBar,()->wrapEditorSelection("<u>","</u>"));
  underlineButton.setContentDescription("Garis bawah teks terpilih");
  Button dividerButton=action("─",formatBar,()->insertEditorText("\n---\n"));
  dividerButton.setTextColor(0xffb49a43);dividerButton.setContentDescription("Masukkan divider emas");
  Button fontButton=action("Aa",formatBar,()->new AlertDialog.Builder(this).setTitle("Saiz tulisan editor").setItems(new String[]{"Kecil","Biasa","Besar","Sangat besar"},(d,i)->{prefs.edit().putInt("editor_font_level",i).apply();if(editor!=null)editor.setTextSize(new int[]{13,15,18,22}[i]);}).show());
  fontButton.setContentDescription("Saiz tulisan editor");
  root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
  // Keep normal Android double-tap text selection while editing.
  // Never navigate away while EditText is handling touch/selection events.
  editor.setOnTouchListener(null);
  if(key!=null)editor.setText(prefs.getString(key,""));TextWatcher titleWatcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save();}public void afterTextChanged(Editable e){}};titleEditor.addTextChangedListener(titleWatcher);TextWatcher bodyWatcher=new TextWatcher(){String before="";public void beforeTextChanged(CharSequence s,int st,int count,int after){before=s.toString();}public void onTextChanged(CharSequence s,int st,int beforeCount,int count){}public void afterTextChanged(Editable e){if(!restoring[0]&&!before.equals(e.toString())){
   int cursor=editor.getSelectionStart();
   if(cursor>0&&cursor<=e.length()&&e.charAt(cursor-1)=='\n'){
    String prior=e.subSequence(0,cursor-1).toString();
    int start=prior.lastIndexOf('\n')+1;
    String previousLine=prior.substring(start);
    java.util.regex.Matcher numbered=java.util.regex.Pattern.compile("^\\s*(\\d+)\\.\\s*(.*)$").matcher(previousLine);
    if(numbered.matches()){
     int next=1;try{next=Integer.parseInt(numbered.group(1))+1;}catch(NumberFormatException ignored){}
     restoring[0]=true;
     if(numbered.group(2).trim().isEmpty()){e.delete(start,cursor);editor.setSelection(start);}
     else{String prefix=next+". ";e.insert(cursor,prefix);editor.setSelection(cursor+prefix.length());}
     restoring[0]=false;
    }
   }
   undoHistory.addLast(before);if(undoHistory.size()>100)undoHistory.removeFirst();redoHistory.clear();historyButtons[0].setAlpha(1f);historyButtons[1].setAlpha(0.35f);}save();}};editor.addTextChangedListener(bodyWatcher);categoryPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){save();}});}

 void insertNumberedList(){
  if(editor==null)return;
  int cursor=Math.max(0,editor.getSelectionStart());
  String content=editor.getText().toString();
  int lineStart=content.lastIndexOf('\n',Math.max(0,cursor-1))+1;
  String line=content.substring(lineStart,cursor);
  if(line.matches("\\s*\\d+\\.\\s*"))return;
  int number=1;
  java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?m)^\\s*(\\d+)\\.\\s+").matcher(content.substring(0,lineStart));
  while(matcher.find()){try{number=Integer.parseInt(matcher.group(1))+1;}catch(NumberFormatException ignored){}}
  editor.getText().insert(lineStart,number+". ");
  editor.setSelection(cursor+(number+". ").length());
 }
 void insertEditorText(String value){
  if(editor==null)return;
  int start=Math.max(0,editor.getSelectionStart());
  editor.getText().insert(start,value);
  editor.setSelection(start+value.length());
 }
 void wrapEditorSelection(String before,String after){
  if(editor==null)return;
  int a=Math.max(0,Math.min(editor.getSelectionStart(),editor.getSelectionEnd()));
  int b=Math.max(0,Math.max(editor.getSelectionStart(),editor.getSelectionEnd()));
  String selectedText=editor.getText().subSequence(a,b).toString();
  editor.getText().replace(a,b,before+selectedText+after);
  editor.setSelection(a+before.length(),a+before.length()+selectedText.length());
 }
 void insertCurrentDateTime(int mode){
  if(editor==null)return;
  java.util.Date now=new java.util.Date();
  String pattern=mode==0?"dd/MM/yyyy":mode==1?"HH:mm":"dd/MM/yyyy HH:mm";
  java.text.SimpleDateFormat format=new java.text.SimpleDateFormat(pattern,java.util.Locale.getDefault());
  String value=format.format(now);
  int start=Math.max(0,editor.getSelectionStart());
  int end=Math.max(0,editor.getSelectionEnd());
  editor.getText().replace(Math.min(start,end),Math.max(start,end),value);
  save();
 }
 void save(){
  if(editor==null)return;
  String value=editor.getText().toString();
  String title=titleEditor==null?"":titleEditor.getText().toString();
  if(selected==null&&value.trim().isEmpty()&&title.trim().isEmpty())return;
  String category=categoryPicker==null||categoryPicker.getSelectedItem()==null?
   "Tanpa kategori":categoryPicker.getSelectedItem().toString();
  boolean isNew=selected==null;
  if(isNew)selected=(maktabah?"m_":"n_")+java.util.UUID.randomUUID().toString();
  boolean changed=isNew||!prefs.contains(selected)||
   !value.equals(prefs.getString(selected,""))||
   !title.equals(prefs.getString("title_"+selected,""))||
   !category.equals(prefs.getString("cat_"+selected,"Tanpa kategori"));
  if(changed){
   prefs.edit().putString(selected,value).putString("title_"+selected,title)
    .putString("cat_"+selected,category).putLong("updated_"+selected,System.currentTimeMillis())
    .putString("last_edited_key",selected).apply();
  }
  if(saveIndicator!=null)saveIndicator.setText("Disimpan");
 }

 void seedSampleArticles(){
  // Only seed once per category; never overwrite user-created notes.
  String[] noteCats={"Nota Umum","Kuliah","Tazkirah","Catatan Peribadi","Senarai Semak"};
  String[] noteTitles={"Selamat datang ke DinNote","Rangka Kuliah: Adab Menuntut Ilmu","Tazkirah: Mensyukuri Nikmat","Catatan Harian","Senarai Semak Harian"};
  String[] noteBodies={
   "DinNote ialah ruang mencatat idea, ilmu dan perkara penting. Anda boleh mengubah atau memadam artikel contoh ini.",
   "Tajuk: Adab Menuntut Ilmu\\n1. Betulkan niat.\\n2. Hormati guru dan rakan.\\n3. Catat isi penting.\\n4. Amalkan ilmu yang dipelajari.",
   "Syukur bukan sekadar ucapan. Ia turut ditunjukkan dengan menggunakan nikmat pada perkara yang baik.\\nRenungan: Apakah satu nikmat yang patut saya syukuri hari ini?",
   "Tarikh: __________\\nPerkara penting hari ini:\\nPengajaran:\\nPerancangan esok:",
   "☐ Semak tugasan\\n☐ Baca dan ulang kaji\\n☐ Kemas kini nota\\n☐ Buat sandaran data"
  };
  for(int i=0;i<noteCats.length;i++)seedSample(false,noteCats[i],noteTitles[i],noteBodies[i]);
  String[] bookCats={
   "Kalimah Tayyibah","Kalimah Tayyibah › Zikir","Kalimah Tayyibah › Doa","Kalimah Tayyibah › Istighfar","Kalimah Tayyibah › Selawat",
   "Kalam Hikmah","Kalam Hikmah › Hikmah Ulama","Kalam Hikmah › Nasihat","Kalam Hikmah › Tazkiyah","Kalam Hikmah › Tasawuf",
   "Kitab-Kitab","Kitab-Kitab › Tafsir","Kitab-Kitab › Hadis","Kitab-Kitab › Tauhid","Kitab-Kitab › Fiqh","Kitab-Kitab › Tasawuf","Kitab-Kitab › Akhlak","Kitab-Kitab › Tajwid","Kitab-Kitab › Bahasa Arab"};
  String[] bookTitles={
   "Kalimah Tauhid","Zikir: Subhanallah","Doa Memohon Ilmu","Istighfar","Selawat ke Atas Nabi",
   "Hikmah: Nilai Masa","Hikmah Tentang Ilmu","Nasihat: Menjaga Lidah","Muhasabah Diri","Pengenalan Tazkiyah",
   "Panduan Membaca Kitab","Tafsir Surah al-Ikhlas","Hadis Tentang Niat","Tauhid: Allah Maha Esa","Fiqh: Kepentingan Bersuci",
   "Tasawuf: Ikhlas","Akhlak: Amanah","Tajwid: Mad Asli","Bahasa Arab: Isim, Fiil dan Harf"};
  String[] bookBodies={
   "لَا إِلَٰهَ إِلَّا اللَّهُ\\nTiada tuhan yang berhak disembah melainkan Allah.\\nRujukan: Maksud kalimah tauhid.",
   "سُبْحَانَ اللَّهِ\\nMaha Suci Allah.\\nZikir ringkas untuk mengingati Allah.",
   "رَبِّ زِدْنِي عِلْمًا\\nWahai Tuhanku, tambahkanlah ilmuku.\\nRujukan: Surah Taha, 20:114.",
   "أَسْتَغْفِرُ اللَّهَ\\nAku memohon keampunan daripada Allah.",
   "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ\\nYa Allah, limpahkanlah selawat kepada Muhammad.",
   "Masa ialah amanah. Susunlah waktu untuk ilmu, ibadah dan tanggungjawab.\\nNota renungan; bukan petikan khusus daripada kitab.",
   "Ilmu menjadi lebih bermanfaat apabila dipelajari dengan adab dan diamalkan.\\nNota renungan; bukan petikan khusus daripada ulama tertentu.",
   "Berfikir sebelum berkata membantu mengelakkan kata-kata yang menyakiti orang lain.\\nLatihan: Diam seketika sebelum menjawab ketika marah.",
   "Soalan muhasabah:\\n1. Apakah niat saya hari ini?\\n2. Apakah kesilapan yang perlu diperbaiki?\\n3. Apakah kebaikan yang boleh diteruskan?",
   "Tazkiyah al-nafs bermaksud usaha membersihkan jiwa daripada sifat tercela dan memupuk sifat terpuji. Antara latihannya ialah muhasabah, taubat dan ikhlas.",
   "Catat nama kitab, pengarang, bab, halaman dan sumber edisi. Bezakan petikan asal daripada ringkasan sendiri.",
   "قُلْ هُوَ اللَّهُ أَحَدٌ\\nKatakanlah: Dialah Allah Yang Maha Esa.\\nRujukan: Surah al-Ikhlas, 112:1. Ini terjemahan makna ringkas.",
   "إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ\\nSesungguhnya amalan itu bergantung pada niat.\\nRujukan: Sahih al-Bukhari, hadis pertama; Sahih Muslim, hadis 1907.",
   "قُلْ هُوَ اللَّهُ أَحَدٌ\\nAllah Maha Esa.\\nRujukan: Surah al-Ikhlas, 112:1.",
   "Bersuci merupakan antara syarat sah solat. Pelajari cara berwuduk dan mengenali perkara yang membatalkannya melalui rujukan fiqh yang muktabar.",
   "Ikhlas ialah melakukan kebaikan kerana Allah, bukan semata-mata mengharapkan pujian manusia. Renungkan niat sebelum dan selepas beramal.",
   "Amanah bermaksud menjaga kepercayaan dan menunaikan tanggungjawab. Contohnya, memulangkan barang pinjaman dan menepati janji.",
   "Mad asli dibaca sepanjang dua harakat apabila terdapat huruf mad tanpa sebab tambahan seperti hamzah atau sukun. Contoh: قَالَ.",
   "Dalam nahu Arab, perkataan dibahagi kepada tiga: اِسْمٌ (kata nama), فِعْلٌ (kata kerja) dan حَرْفٌ (partikel)."
  };
  for(int i=0;i<bookCats.length;i++)seedSample(true,bookCats[i],bookTitles[i],bookBodies[i]);
  String[] extraTitles=new String[]{"Fiqh: Air Mutlak","Kuliah: Rukun Wuduk","Tazkirah: Solat","Catatan Pengajian","Semak Sebelum Solat","Makna Syahadah","Zikir Tahmid","Doa Kebaikan","Taubat","Selawat Ringkas","Hikmah Istiqamah","Adab Ilmu","Menjaga Lidah","Muhasabah","Sabar dan Syukur","Catatan Kitab","Tafsir al-Asr","Hadis Nasihat","Tauhid Qidam","Fiqh: Syarat Sah Solat","Tasawuf Muraqabah","Akhlak Jiran","Tajwid Nun Sakinah","Nahu Arab"};
  String[] extraBodies=new String[]{"Air mutlak boleh digunakan untuk bersuci, seperti air hujan dan air sungai. Ringkasan pengenalan fiqh Syafi‘i.","Rukun wuduk: niat, basuh muka, basuh tangan hingga siku, sapu sebahagian kepala, basuh kaki hingga buku lali dan tertib.","Menjaga solat lima waktu ialah kewajipan Muslim mukalaf.","Tajuk: ____\\nRujukan kitab: ____\\nCatatan: ____","☐ Masuk waktu\\n☐ Bersuci\\n☐ Menutup aurat\\n☐ Menghadap kiblat","أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ\\nAku bersaksi bahawa tiada tuhan yang berhak disembah melainkan Allah.","الْحَمْدُ لِلَّهِ\\nSegala puji bagi Allah.","رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ\\nRujukan: al-Baqarah 2:201.","Taubat menuntut meninggalkan dosa, menyesal dan berazam tidak mengulanginya.","اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ\\nYa Allah, limpahkanlah selawat kepada Muhammad.","Amalan kecil yang dilakukan berterusan membantu mendidik jiwa. Nota renungan.","Belajar dengan ikhlas, menghormati guru dan menyemak sumber ilmu.","Berfikir sebelum berkata dan elakkan menyakiti orang lain.","Renungkan kebaikan dan kesilapan hari ini serta niat untuk memperbaikinya.","Sabar ketika diuji dan syukur ketika menerima nikmat.","Catat judul, pengarang, jilid, halaman dan edisi bagi setiap petikan.","Surah al-Asr mengingatkan tentang iman, amal soleh, kebenaran dan kesabaran. Rujukan: al-Asr 103:1–3.","الدِّينُ النَّصِيحَةُ\\nAgama itu nasihat. Rujukan: Sahih Muslim, hadis 55.","Qidam bermaksud Allah tidak mempunyai permulaan.","Antara syarat sah solat ialah suci daripada hadas, bersih daripada najis, menutup aurat, masuk waktu dan menghadap kiblat.","Muraqabah ialah kesedaran bahawa Allah sentiasa mengetahui keadaan hamba-Nya.","Jangan mengganggu jiran, bantu ketika perlu dan pelihara pertuturan.","Hukum nun sakinah dan tanwin termasuk izhar, idgham, iqlab dan ikhfa.","الْعِلْمُ نَافِعٌ\\nIlmu itu bermanfaat. Contoh jumlah ismiyyah."};
  for(int i=0;i<extraTitles.length;i++){
   boolean book=i>=noteCats.length;
   String cat=book?bookCats[i-noteCats.length]:noteCats[i];
   String id=(book?"m_":"n_")+"sample_extra_"+Integer.toHexString(cat.hashCode());
   if(!prefs.contains(id)&&!prefs.getBoolean("deleted_"+id,false))prefs.edit().putString(id,extraBodies[i]).putString("title_"+id,extraTitles[i]).putString("cat_"+id,cat).apply();
  }
 }
 void seedSample(boolean book,String category,String title,String body){
  String id=(book?"m_":"n_")+"sample_"+Integer.toHexString(category.hashCode());
  if(prefs.contains(id)||prefs.getBoolean("deleted_"+id,false))return;
  prefs.edit().putString(id,body).putString("title_"+id,title).putString("cat_"+id,category).apply();
 }
 void seedDefaultCategories(){seedOne("categories_n",new String[]{"Nota Umum","Kuliah","Tazkirah","Catatan Peribadi","Senarai Semak"});seedOne("categories_m",new String[]{"Kalimah Tayyibah","Kalimah Tayyibah › Zikir","Kalimah Tayyibah › Doa","Kalimah Tayyibah › Istighfar","Kalimah Tayyibah › Selawat","Kalam Hikmah","Kalam Hikmah › Hikmah Ulama","Kalam Hikmah › Nasihat","Kalam Hikmah › Tazkiyah","Kalam Hikmah › Tasawuf","Kitab-Kitab","Kitab-Kitab › Tafsir","Kitab-Kitab › Hadis","Kitab-Kitab › Tauhid","Kitab-Kitab › Fiqh","Kitab-Kitab › Tasawuf","Kitab-Kitab › Akhlak","Kitab-Kitab › Tajwid","Kitab-Kitab › Bahasa Arab"});}
 void seedOne(String key,String[] defaults){if(prefs.contains(key))return;prefs.edit().putString(key,android.text.TextUtils.join("|",defaults)).apply();}
 ArrayList<String> categories(){String raw=prefs.getString(maktabah?"categories_m":"categories_n","");ArrayList<String> out=new ArrayList<>();for(String x:raw.split("\\|"))if(!x.trim().isEmpty())out.add(x.trim());return out;}
 void storeCategories(ArrayList<String> c){prefs.edit().putString(maktabah?"categories_m":"categories_n",android.text.TextUtils.join("|",c)).apply();}
 void manageCategories(){ArrayList<String> c=categories();String[] options=new String[c.size()+1];options[0]="+ Tambah kategori";for(int i=0;i<c.size();i++)options[i+1]=c.get(i);new AlertDialog.Builder(this).setTitle("Urus kategori").setItems(options,(d,which)->{if(which==0)categoryDialog(null);else categoryActions(c.get(which-1));}).setNegativeButton("Tutup",null).show();}
 void categoryActions(String old){new AlertDialog.Builder(this).setTitle(old).setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{if(which==0)categoryDialog(old);else new AlertDialog.Builder(this).setMessage("Padam kategori? Nota tidak akan dipadam.").setPositiveButton("Padam",(a,b)->{ArrayList<String> c=categories();c.remove(old);storeCategories(c);for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,"Tanpa kategori").apply();categoryFilter="Semua";openSection(maktabah);}).setNegativeButton("Batal",null).show();}).show();}
 void categoryDialog(String old){EditText field=new EditText(this);field.setSingleLine(true);field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);field.setHint("Nama kategori");if(old!=null)field.setText(old);new AlertDialog.Builder(this).setTitle(old==null?"Kategori baharu":"Ubah nama kategori").setView(field).setPositiveButton("Simpan",(d,w)->{String name=field.getText().toString().trim().replace("|","");if(name.isEmpty()||name.equals("Semua")||name.equals("Tanpa kategori"))return;ArrayList<String> c=categories();if(c.contains(name)&&!name.equals(old))return;if(old!=null)c.remove(old);c.add(name);storeCategories(c);if(old!=null){for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,name).apply();}categoryFilter="Semua";home();}).setNegativeButton("Batal",null).show();}

 void startBackup(){
  save();
  // Flush pending asynchronous preference writes before reading a consistent backup snapshot.
  if(!prefs.edit().commit()){
   Toast.makeText(this,"Backup dibatalkan: gagal menyimpan data terkini",Toast.LENGTH_LONG).show();
   return;
  }
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/json");
  intent.putExtra(Intent.EXTRA_TITLE,"DinNote-backup.json");
  startActivityForResult(intent,BACKUP_CREATE);
 }
 void startRestore(){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/json");
  startActivityForResult(intent,BACKUP_OPEN);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  android.net.Uri uri=data.getData();
  if(request==BACKUP_CREATE){
   try{
    org.json.JSONObject obj=new org.json.JSONObject();
    obj.put("format","DinNote-backup-v1");
    // Informational metadata for future SQLite migration; v1 backups stay compatible.
    obj.put("storage","SharedPreferences");
    obj.put("exported_at",System.currentTimeMillis());
    obj.put("app_package",getPackageName());
    org.json.JSONObject values=new org.json.JSONObject();
    for(java.util.Map.Entry<String,?> entry:prefs.getAll().entrySet()){
     Object value=entry.getValue();
     if(value instanceof String||value instanceof Boolean||value instanceof Number)values.put(entry.getKey(),value);
    }
    obj.put("data",values);
    byte[] backupBytes=obj.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if(backupBytes.length>20*1024*1024)throw new java.io.IOException("Sandaran melebihi had 20 MB");
    try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
     if(out==null)throw new java.io.IOException("Tidak dapat membuka fail");
     out.write(backupBytes);
     out.flush();
    }
    Toast.makeText(this,"Backup berjaya disimpan",Toast.LENGTH_LONG).show();
   }catch(Exception e){Toast.makeText(this,"Backup gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }else if(request==BACKUP_OPEN){
   new AlertDialog.Builder(this).setTitle("Pulihkan DinNote?")
    .setMessage("Data sedia ada akan digantikan dengan kandungan fail sandaran. Teruskan?")
    .setNegativeButton("Batal",null).setPositiveButton("Pulihkan",(dialog,which)->restoreBackup(uri)).show();
  }
 }
 void restoreBackup(android.net.Uri uri){
  try{
   java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
   try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
    if(in==null)throw new java.io.IOException("Fail tidak dapat dibaca");
    byte[] chunk=new byte[8192];int n;int total=0;
    while((n=in.read(chunk))!=-1){total+=n;if(total>20*1024*1024)throw new java.io.IOException("Fail terlalu besar");buffer.write(chunk,0,n);}
   }
   org.json.JSONObject backup=new org.json.JSONObject(new String(buffer.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
   if(!"DinNote-backup-v1".equals(backup.optString("format")))throw new java.io.IOException("Format sandaran tidak sah");
   // Backups from a different installed application must not overwrite DinNote data.
   // Validate optional metadata types without rejecting older v1 backups.
   if(backup.has("app_package")&&!(backup.opt("app_package") instanceof String))
    throw new java.io.IOException("Identiti aplikasi dalam sandaran tidak sah");
   if(backup.has("storage")&&!(backup.opt("storage") instanceof String))
    throw new java.io.IOException("Maklumat storan dalam sandaran tidak sah");
   if(backup.has("exported_at")&&!(backup.opt("exported_at") instanceof Number))
    throw new java.io.IOException("Tarikh sandaran tidak sah");
   if(backup.has("exported_at")){
    double exportedAt=((Number)backup.opt("exported_at")).doubleValue();
    if(Double.isNaN(exportedAt)||Double.isInfinite(exportedAt)||exportedAt<0)
     throw new java.io.IOException("Tarikh sandaran tidak sah");
   }
   String backupPackage=backup.optString("app_package","");
   if(!backupPackage.isEmpty()&&!getPackageName().equals(backupPackage))
    throw new java.io.IOException("Sandaran daripada aplikasi lain; data sedia ada tidak diubah");
   org.json.JSONObject values=backup.getJSONObject("data");
   if(values.length()==0)throw new java.io.IOException("Sandaran kosong; data sedia ada tidak diubah");
   // Reject unrelated JSON before clearing the existing DinNote preferences.
   boolean recognized=false;
   java.util.Iterator<String> backupKeys=values.keys();
   while(backupKeys.hasNext()){
    String key=backupKeys.next();
    if(key.equals("categories_n")||key.equals("categories_m")||
      key.startsWith("n_")||key.startsWith("m_")||
      key.startsWith("title_n_")||key.startsWith("title_m_")){
     recognized=true;break;
    }
   }
   if(!recognized)throw new java.io.IOException("Fail tidak mengandungi data DinNote; data sedia ada tidak diubah");
   java.util.Iterator<String> validationKeys=values.keys();
   while(validationKeys.hasNext()){
    String key=validationKeys.next();
    Object value=values.get(key);
    if(key==null||key.trim().isEmpty()||value==null||value==org.json.JSONObject.NULL||
      !(value instanceof String||value instanceof Boolean||value instanceof Number))
     throw new java.io.IOException("Kandungan sandaran tidak disokong; data sedia ada tidak diubah");
    // Notes, titles, and category labels must remain text to avoid crashes after restore.
    if((key.startsWith("n_")||key.startsWith("m_")||
        key.startsWith("title_n_")||key.startsWith("title_m_")||
        key.startsWith("cat_n_")||key.startsWith("cat_m_")||
        key.equals("categories_n")||key.equals("categories_m"))&&!(value instanceof String))
     throw new java.io.IOException("Jenis data nota atau kategori tidak sah; data sedia ada tidak diubah");
    // Validate types of known settings to prevent runtime ClassCastException after restore.
    if((key.startsWith("updated_")||key.startsWith("viewed_"))&&
       (!(value instanceof Number)||((Number)value).doubleValue()%1!=0))
     throw new java.io.IOException("Tarikh nota dalam sandaran tidak sah; data sedia ada tidak diubah");
    if((key.startsWith("pinned_")||key.startsWith("deleted_"))&&!(value instanceof Boolean))
     throw new java.io.IOException("Status nota dalam sandaran tidak sah; data sedia ada tidak diubah");
    // Validate all numeric values before creating the destructive restore transaction.
    if(value instanceof Number&&!(value instanceof Integer)&&!(value instanceof Long)){
     double number=((Number)value).doubleValue();
     if(Double.isNaN(number)||Double.isInfinite(number)||number>Float.MAX_VALUE||number< -Float.MAX_VALUE)
      throw new java.io.IOException("Nombor sandaran di luar julat; data sedia ada tidak diubah");
    }
   }
   android.content.SharedPreferences.Editor tx=prefs.edit().clear();
   java.util.Iterator<String> keys=values.keys();
   while(keys.hasNext()){
    String key=keys.next();Object value=values.get(key);
    if(value instanceof String)tx.putString(key,(String)value);
    else if(value instanceof Boolean)tx.putBoolean(key,(Boolean)value);
    else if(value instanceof Integer)tx.putInt(key,(Integer)value);
    else if(value instanceof Long)tx.putLong(key,(Long)value);
    else if(value instanceof Number){
     // JSON decimals represent SharedPreferences float values; never truncate them to long.
     tx.putFloat(key,((Number)value).floatValue());
    }
   }
   if(!tx.commit())throw new java.io.IOException("Gagal menyimpan data");
   editor=null;selected=null;items=null;sectionOpen=false;
   home();
   Toast.makeText(this,"Data DinNote berjaya dipulihkan",Toast.LENGTH_LONG).show();
  }catch(Exception e){Toast.makeText(this,"Restore gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 @Override public void onConfigurationChanged(android.content.res.Configuration newConfig){
  super.onConfigurationChanged(newConfig);
  if(editor!=null)return; // Preserve in-progress editing and autosave.
  if(selected!=null&&prefs.contains(selected)){
   String key=selected;
   root.post(()->{if(editor==null&&key.equals(selected))read(key);});
  }
 }
 @Override protected void onPause(){if(editor!=null)save();super.onPause();}
 public void onBackPressed(){if(editor!=null){finishEditingSafely();}else if(selected!=null){returnFromReader();}else if(sectionOpen){home();}else super.onBackPressed();}
}
