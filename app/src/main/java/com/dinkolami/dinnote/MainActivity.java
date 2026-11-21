package com.dinkolami.dinnote;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import android.text.*;import android.content.*;import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root,items,compactSummary; ScrollView listScroll,readerScroll; int savedNotesScroll=0,savedBooksScroll=0; String lastOpenedCard=null; android.view.GestureDetector readerDetector; EditText editor,titleEditor; TextView saveIndicator; Spinner categoryPicker; String categoryFilter="Semua"; String sortMode="Terbaru"; String selected=null; boolean maktabah=false, sectionOpen=false; android.content.SharedPreferences prefs;
 int green=Color.rgb(23,107,71), gold=Color.rgb(197,166,107); static final int BACKUP_CREATE=4201, BACKUP_OPEN=4202, CONTENT_IMPORT=4203;
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("notes",0);if(!prefs.getBoolean("font_defaults_13_15_16_18_applied",false)){prefs.edit().putInt("reader_font_level",1).putInt("editor_font_level",1).putBoolean("font_defaults_13_15_16_18_applied",true).apply();}if(prefs.contains("security_pin_hash"))promptUnlock();else home();} // User content and categories are local; import them explicitly.}
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
    int bottom=Math.max(bars.bottom,insets.getInsets(android.view.WindowInsets.Type.ime()).bottom);
    v.setPadding(0,bars.top,0,bottom);
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
  // Subtle content fade on navigation; keep system bars and all controls unchanged.
  root.setAlpha(0.94f);
  root.animate().alpha(1f).setDuration(140L).start();
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  header.setPadding(dp(8),dp(3),dp(8),dp(3));header.setBackgroundColor(0xfffdfefe);
  ImageView menu=headerIcon("⌂",0xff9daeb9,"Halaman utama DinNote");header.addView(menu);
  menu.setOnClickListener(v->home());
  ImageView settingsMenu=headerIcon("☰",0xff9daeb9,"Menu DinNote dan Tetapan");header.addView(settingsMenu);
  settingsMenu.setOnClickListener(v->showMainMenu());
  ImageView logo=new ImageView(this);logo.setImageBitmap(transparentHeaderLogo());logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Logo DinNote");logo.setClickable(true);logo.setPadding(dp(3),dp(3),dp(3),dp(3));logo.setBackground(rounded(0xfffdfefe,0xffb89543,50));logo.setClipToOutline(true);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(34));lp.leftMargin=dp(6);lp.rightMargin=dp(5);header.addView(logo,lp);
  logo.setOnClickListener(v->home());
  TextView name=text("DinNote",16);name.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.ITALIC));name.setTextColor(0xff075033);
  name.setGravity(Gravity.CENTER_VERTICAL);name.setPadding(dp(3),0,0,0);header.addView(name,new LinearLayout.LayoutParams(0,dp(40),1));
  name.setOnClickListener(v->home());
  ImageView inbox=headerIcon("▣",0xff9daeb9,"Buka nota");header.addView(inbox);
  inbox.setOnClickListener(v->openSection(false));
  ImageView bell=headerIcon("♧",0xff9daeb9,"Peringatan nota");header.addView(bell);
  bell.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Peringatan").setMessage("Peringatan nota belum tersedia.").setPositiveButton("OK",null).show());
  ImageView avatar=headerIcon("☪",0xff9daeb9,"Urus kategori");header.addView(avatar);
  avatar.setOnClickListener(v->manageCategories());
  root.addView(header,new LinearLayout.LayoutParams(-1,dp(54)));
  View border=new View(this);border.setBackgroundColor(0xffd5bb72);
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
  android.graphics.drawable.GradientDrawable frame=new android.graphics.drawable.GradientDrawable();frame.setColor(0xfffdf9eb);frame.setCornerRadius(dp(10));frame.setStroke(dp(1),0xffb8c2ca);view.setBackground(frame);
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
 String hashPin(String pin,String salt) throws Exception{
  byte[] saltBytes=android.util.Base64.decode(salt,android.util.Base64.NO_WRAP);
  javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(pin.toCharArray(),saltBytes,120000,256);
  try{
   byte[] hash=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
   return android.util.Base64.encodeToString(hash,android.util.Base64.NO_WRAP);
  }finally{spec.clearPassword();}
 }
 EditText pinField(){
  EditText input=new EditText(this);
  input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
  input.setSingleLine(true);input.setHint("PIN 4 hingga 8 digit");
  input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8)});
  return input;
 }
 boolean validPin(String pin){return pin.matches("[0-9]{4,8}");}
 boolean verifyPin(String pin){
  try{
   String actual=hashPin(pin,prefs.getString("security_pin_salt",""));
   byte[] a=android.util.Base64.decode(actual,android.util.Base64.NO_WRAP);
   byte[] b=android.util.Base64.decode(prefs.getString("security_pin_hash",""),android.util.Base64.NO_WRAP);
   return java.security.MessageDigest.isEqual(a,b);
  }catch(Exception e){return false;}
 }
 void promptUnlock(){
  EditText input=pinField();
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("DinNote dikunci")
   .setMessage("Masukkan PIN untuk membuka Nota dan Maktabah.")
   .setView(input).setCancelable(false)
   .setPositiveButton("Buka",null)
   .setNegativeButton("Keluar",(d,w)->finish()).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
   if(verifyPin(input.getText().toString())){dialog.dismiss();home();}
   else{input.setText("");input.setError("PIN tidak tepat");}
  }));
  dialog.show();
 }
 void configurePin(){
  boolean enabled=prefs.contains("security_pin_hash");
  EditText input=pinField();
  new AlertDialog.Builder(this).setTitle(enabled?"Matikan kunci PIN":"Aktifkan kunci PIN")
   .setMessage(enabled?"Masukkan PIN semasa untuk mematikan kunci.":"Tetapkan PIN 4–8 digit. Simpan PIN dengan selamat; tiada pemulihan PIN automatik.")
   .setView(input).setNegativeButton("Batal",null)
   .setPositiveButton(enabled?"Matikan":"Set PIN",(d,w)->{
    String pin=input.getText().toString();
    if(enabled){
     if(!verifyPin(pin)){Toast.makeText(this,"PIN salah; kunci masih aktif",Toast.LENGTH_LONG).show();return;}
     if(prefs.edit().remove("security_pin_hash").remove("security_pin_salt").commit())
      Toast.makeText(this,"Kunci PIN dimatikan",Toast.LENGTH_SHORT).show();
    }else{
     if(!validPin(pin)){Toast.makeText(this,"PIN mesti 4 hingga 8 digit",Toast.LENGTH_LONG).show();return;}
     try{
      byte[] salt=new byte[16];new java.security.SecureRandom().nextBytes(salt);
      String encoded=android.util.Base64.encodeToString(salt,android.util.Base64.NO_WRAP);
      if(!prefs.edit().putString("security_pin_salt",encoded).putString("security_pin_hash",hashPin(pin,encoded)).commit())
       throw new java.io.IOException("Gagal menyimpan PIN");
      Toast.makeText(this,"Kunci PIN diaktifkan",Toast.LENGTH_LONG).show();
     }catch(Exception e){Toast.makeText(this,"Gagal mengaktifkan PIN",Toast.LENGTH_LONG).show();}
    }
   }).show();
 }
 void showSettings(){
  new AlertDialog.Builder(this).setTitle("Tetapan DinNote")
   .setItems(new String[]{"Saiz tulisan bacaan","Saiz tulisan editor","Backup ke fail","Restore daripada fail","Import kandungan JSON","Tong sampah","Kunci PIN aplikasi","Maklumat aplikasi"},(dialog,which)->{
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
       Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1))),(d,choice)->{
        prefs.edit().putInt("reader_font_level",choice).putInt("editor_font_level",choice).apply();
        d.dismiss();
        if(editor!=null)editor.setTextSize(new int[]{13,15,16,18}[choice]);
        Toast.makeText(this,"Saiz tulisan editor dikemas kini",Toast.LENGTH_SHORT).show();
       }).setNegativeButton("Batal",null).show();
    }else if(which==2)startBackup();
    else if(which==3)startRestore();
    else if(which==4)startContentImport();
    else if(which==5)showTrash();
    else if(which==6)configurePin();
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
 void notesPage(){editor=null;titleEditor=null;categoryPicker=null;selected=null;base("DinNote");LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(12),dp(7),dp(12),dp(5));tabs.setGravity(Gravity.CENTER_VERTICAL);TextView notes=text("✎ Nota",13);TextView books=text("▤ Maktabah",13);notes.setGravity(Gravity.CENTER);books.setGravity(Gravity.CENTER);notes.setTypeface(null,maktabah?android.graphics.Typeface.NORMAL:android.graphics.Typeface.BOLD);books.setTypeface(null,maktabah?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);notes.setPadding(dp(5),dp(10),dp(5),dp(10));books.setPadding(dp(5),dp(10),dp(5),dp(10));notes.setBackground(rounded(maktabah?0xfff8faf8:0xfff0f3f6,maktabah?0xffe0e6e2:0xffb7c1c9,16));notes.setTextColor(maktabah?0xff65777b:0xff566a70);notes.setContentDescription(maktabah?"Buka Nota":"Nota, bahagian dipilih");books.setBackground(rounded(maktabah?0xfff0f3f6:0xfff8faf8,maktabah?0xffb7c1c9:0xffe0e6e2,16));books.setTextColor(maktabah?0xff566a70:0xff65777b);books.setContentDescription(maktabah?"Maktabah, bahagian dipilih":"Buka Maktabah");LinearLayout.LayoutParams noteTabParams=new LinearLayout.LayoutParams(0,dp(44),1f);noteTabParams.rightMargin=dp(4);tabs.addView(notes,noteTabParams);
 LinearLayout.LayoutParams bookTabParams=new LinearLayout.LayoutParams(0,dp(44),1.25f);bookTabParams.rightMargin=dp(7);tabs.addView(books,bookTabParams);
 TextView quickSearch=text("⌕",23);quickSearch.setGravity(Gravity.CENTER);quickSearch.setTextColor(0xff566a70);quickSearch.setBackground(rounded(0xfff8faf8,0xffdce5df,12));quickSearch.setContentDescription("Cari tajuk, isi atau kategori");applyTouchFeedback(quickSearch);LinearLayout.LayoutParams quickSearchParams=new LinearLayout.LayoutParams(dp(43),dp(44));quickSearchParams.rightMargin=dp(5);tabs.addView(quickSearch,quickSearchParams);
 TextView quickAdd=text("✚",20);quickAdd.setGravity(Gravity.CENTER);quickAdd.setTextColor(0xff566a70);quickAdd.setBackground(rounded(0xffeaf4ee,0xffc9dfd0,12));quickAdd.setContentDescription(maktabah?"Kitab baharu":"Nota baharu");quickAdd.setOnClickListener(v->edit(null));applyTouchFeedback(quickAdd);
 LinearLayout.LayoutParams quickParams=new LinearLayout.LayoutParams(dp(43),dp(44));quickParams.rightMargin=dp(5);tabs.addView(quickAdd,quickParams);
 TextView quickCategory=text("⚙",19);quickCategory.setGravity(Gravity.CENTER);quickCategory.setTextColor(0xff596e75);quickCategory.setBackground(rounded(0xfff8faf8,0xffdce5df,12));quickCategory.setContentDescription("Urus kategori");quickCategory.setOnClickListener(v->manageCategories());applyTouchFeedback(quickCategory);
 LinearLayout.LayoutParams categoryButtonParams=new LinearLayout.LayoutParams(dp(43),dp(44));categoryButtonParams.rightMargin=dp(5);tabs.addView(quickCategory,categoryButtonParams);
 TextView sort=text("↕",20);sort.setGravity(Gravity.CENTER);sort.setTextColor(0xff566a70);sort.setPadding(dp(4),0,dp(4),0);sort.setBackground(rounded(0xfff8faf8,0xffdce5df,12));sort.setContentDescription("Susun: "+sortMode+". Tekan untuk ubah susunan");applyTouchFeedback(sort);tabs.addView(sort,new LinearLayout.LayoutParams(dp(43),dp(44)));root.addView(tabs);applyTouchFeedback(notes);applyTouchFeedback(books);notes.setOnClickListener(v->{if(maktabah)openSection(false);});books.setOnClickListener(v->{if(!maktabah)openSection(true);});EditText search=new EditText(this);search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setSelectAllOnFocus(false);search.setMaxLines(1);search.setTextSize(14);search.setHint(maktabah?"Cari tajuk, isi atau kategori kitab":"Cari tajuk, isi atau kategori nota");search.setContentDescription(maktabah?"Carian kitab":"Carian nota");search.setPadding(dp(16),dp(10),dp(16),dp(10));search.setBackground(rounded(0xffffffff,0xffdce7df,12));LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(-1,-2);searchParams.setMargins(dp(12),dp(4),dp(12),dp(2));quickSearch.setOnClickListener(v->{
  EditText popupSearch=new EditText(this);popupSearch.setSingleLine(true);popupSearch.setTextSize(16);
  popupSearch.setHint(maktabah?"Cari tajuk, isi atau kategori kitab":"Cari tajuk, isi atau kategori nota");
  popupSearch.setText(search.getText());popupSearch.setSelection(popupSearch.length());
  popupSearch.setPadding(dp(16),dp(12),dp(16),dp(12));
  popupSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void onTextChanged(CharSequence text,int start,int before,int count){search.setText(text);}public void afterTextChanged(Editable text){}});
  LinearLayout popup=new LinearLayout(this);popup.setOrientation(LinearLayout.VERTICAL);popup.setPadding(dp(16),dp(6),dp(16),0);popup.addView(popupSearch);
  new AlertDialog.Builder(this).setTitle("Carian "+(maktabah?"Maktabah":"Nota")).setView(popup)
   .setNeutralButton("Padam carian",(dialog,which)->search.setText(""))
   .setPositiveButton("Selesai",(dialog,which)->{})
   .show();
 });LinearLayout filters=new LinearLayout(this);filters.setPadding(dp(14),0,dp(14),0);TextView folder=text((maktabah?"▤  Maktabah":"✎  Nota")+"  /",14);folder.setTextColor(0xff60757b);folder.setTypeface(null,android.graphics.Typeface.BOLD);filters.addView(folder);Spinner catFilter=new Spinner(this);catFilter.setContentDescription("Tapis "+(maktabah?"kitab":"nota")+" mengikut kategori");ArrayList<String> cats=new ArrayList<>();cats.add("Semua");cats.addAll(categories());catFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));int ci=cats.indexOf(categoryFilter);catFilter.setSelection(ci<0?0:ci);filters.addView(catFilter,new LinearLayout.LayoutParams(0,-2,1));
  compactSummary=null;
  if(getResources().getConfiguration().screenWidthDp>=600){
   LinearLayout.LayoutParams categoryParams=(LinearLayout.LayoutParams)catFilter.getLayoutParams();
   categoryParams.width=0;categoryParams.weight=1f;catFilter.setLayoutParams(categoryParams);
   compactSummary=new LinearLayout(this);compactSummary.setGravity(Gravity.CENTER_VERTICAL);
   filters.addView(compactSummary,new LinearLayout.LayoutParams(0,-2,2f));
  }
  root.addView(filters);
  sort.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Susunan").setItems(new String[]{"Terbaru","Terlama","Tajuk A–Z","Kerap dibuka"},(d,i)->{sortMode=new String[]{"Terbaru","Terlama","Tajuk A–Z","Kerap dibuka"}[i];sort.setContentDescription("Susun: "+sortMode+". Tekan untuk ubah susunan");list(search.getText().toString());}).show());
  View line=new View(this);line.setBackgroundColor(0xffd0d7de);root.addView(line,new LinearLayout.LayoutParams(-1,1));ScrollView sc=new ScrollView(this);listScroll=sc;sc.setOverScrollMode(View.OVER_SCROLL_NEVER);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(true);items=new LinearLayout(this);items.setOrientation(1);items.setPadding(0,0,0,dp(12));sc.addView(items);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));catFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){categoryFilter=cats.get(pos);list(search.getText().toString());}});search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){list(s.toString());}public void afterTextChanged(Editable e){}});list("");
 int restoreY=maktabah?savedBooksScroll:savedNotesScroll;
 sc.post(()->sc.scrollTo(0,Math.max(0,restoreY)));
 }

 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 android.graphics.drawable.GradientDrawable silverGradient(int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xffffffff,0xfffcfcfd,0xfff1f2f4});d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xffe1e4e8);return d;}
 android.graphics.drawable.GradientDrawable subtleNoteBorder(int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(Color.WHITE);d.setCornerRadius(dp(radius));d.setStroke(dp(0.5f),0xffe5e7eb);return d;}
 TextView dashboardText(String value,int size,boolean bold){TextView t=text(value,size);t.setPadding(dp(10),dp(8),dp(10),dp(8));if(bold)t.setTypeface(null,1);return t;}
 LinearLayout dashboardCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(14),dp(12),dp(14),dp(12));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(17));bg.setStroke(dp(1),0xffe1e5e4);c.setBackground(bg);c.setElevation(dp(3));return c;}
 void applyTouchFeedback(View view){
  view.setClickable(true);
  view.setFocusable(true);
  android.util.TypedValue value=new android.util.TypedValue();
  if(getTheme().resolveAttribute(android.R.attr.selectableItemBackground,value,true)
     && value.resourceId!=0){
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
  page.setPadding(dp(10),dp(6),dp(10),dp(22));page.setBackgroundColor(0xfffffefd);page.setTag("dinnote_home");
  // Frontpage content below search: fit minor overflow and never leave a
  // scrollable blank tail caused by the wrapper retaining its old height.
  FrameLayout homeFitFrame=new FrameLayout(this);
  homeFitFrame.setClipChildren(false);homeFitFrame.setClipToPadding(false);
  scroll.addView(homeFitFrame,new ScrollView.LayoutParams(-1,-2));
  homeFitFrame.addView(page,new FrameLayout.LayoutParams(-1,-2));
  final boolean[] homeFits={false};
  final View[] homeFlexibleSpace={null};
  // Never intercept dashboard scrolling: in split-screen the visible height is smaller.
  // Android must be allowed to scroll all real content, including the bottom rows.
  scroll.getViewTreeObserver().addOnGlobalLayoutListener(()->{
   int available=scroll.getHeight()-scroll.getPaddingTop()-scroll.getPaddingBottom();
   int width=homeFitFrame.getWidth();
   if(available<=0||width<=0)return;
   page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
     View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
   int natural=page.getMeasuredHeight();
   // Subtract the previously allocated flexible gap to avoid layout oscillation.
   if(homeFlexibleSpace[0]!=null)natural-=homeFlexibleSpace[0].getLayoutParams().height;
   if(natural<=0)return;
   float ratio=(float)available/natural;
   // Fit only when the dashboard genuinely fits; do not shrink or clip a tall
   // dashboard to a short split-screen window.
   // On folded phones, compress dashboard row heights moderately to keep
   // the bottom action visible; on narrow multi-window panes retain scrolling
   // rather than squeezing the text and touch targets excessively.
   int homeWindowDp=Math.round(scroll.getWidth()/getResources().getDisplayMetrics().density);
   float minimumFit=homeWindowDp<500?0.82f:0.93f;
   // Fill surplus vertical space too: folded and unfolded dashboards
   // should end near the bottom rather than leaving a blank strip.
   // Bound the stretch to preserve readable labels and touch targets.
   float fit=ratio<1f&&ratio>=minimumFit?ratio:1f;
   int extra=fit==1f?Math.max(0,available-natural):0;
   extra=Math.min(extra,dp(260));
   if(homeFlexibleSpace[0]!=null&&homeFlexibleSpace[0].getLayoutParams().height!=extra){
    android.view.ViewGroup.LayoutParams gapParams=homeFlexibleSpace[0].getLayoutParams();
    gapParams.height=extra;homeFlexibleSpace[0].setLayoutParams(gapParams);
   }
   int desired=Math.round((natural+extra)*fit);
   boolean fits=desired<=available;
   homeFits[0]=fits;
   // Clamp the wrapper to the actual visual content height; the ScrollView
   // must never be able to scroll through leftover wrapper space.
   if(Math.abs(page.getScaleY()-fit)>=0.001f||homeFitFrame.getLayoutParams().height!=desired){
    page.setPivotX(0f);page.setPivotY(0f);page.setScaleY(fit);
    android.view.ViewGroup.LayoutParams params=homeFitFrame.getLayoutParams();
    params.height=desired;homeFitFrame.setLayoutParams(params);
   }
   if(fits&&scroll.getScrollY()!=0)scroll.scrollTo(0,0);
  });
  TextView recent=dashboardText("Baru dilihat  ▾",15,true);
  recent.setBackground(rounded(0xfffdfefe,0xffcdd6df,9));recent.setTextColor(0xff566a70);recent.setIncludeFontPadding(false);
  LinearLayout quickTools=new LinearLayout(this);quickTools.setOrientation(LinearLayout.HORIZONTAL);quickTools.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout.LayoutParams quickParams=new LinearLayout.LayoutParams(-1,-2);quickParams.bottomMargin=dp(1);
  quickTools.setPadding(dp(10),dp(4),dp(10),dp(6));quickTools.setBackgroundColor(0xfffffefd);quickTools.setElevation(dp(1));
  // Keep dashboard search and view selector visible while the note list scrolls.
  root.addView(quickTools,2,quickParams);
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,dp(42));rp.rightMargin=dp(6);
  recent.setGravity(Gravity.CENTER_VERTICAL);recent.setPadding(dp(12),0,dp(12),0);
  quickTools.addView(recent,rp);
  recent.setContentDescription("Pilih paparan: baru dilihat, semua nota atau Maktabah");recent.setFocusable(true);applyTouchFeedback(recent);
  recent.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Baru dilihat","Semua nota","Maktabah"},(d,i)->{if(i==2)openSection(true);else openSection(false);}).show());
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);
  search.setHint("Cari nota atau kitab");search.setContentDescription("Carian nota dan kitab");
  search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
  search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
  search.setPadding(dp(12),dp(6),dp(12),dp(6));
  search.setBackground(rounded(0xffffffff,0xffcdd6df,9));
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(42),1);
  quickTools.addView(search,sp);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){String q=s.toString().trim();if(!q.isEmpty())showHomeSearchResults(q);}public void afterTextChanged(Editable e){}});
  TextView clearSearch=dashboardText("×",22,true);clearSearch.setGravity(Gravity.CENTER);clearSearch.setTextColor(0xff587463);clearSearch.setContentDescription("Padam carian");clearSearch.setVisibility(View.GONE);quickTools.addView(clearSearch,new LinearLayout.LayoutParams(dp(34),dp(42)));clearSearch.setOnClickListener(v->search.setText(""));applyTouchFeedback(clearSearch);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int c,int a){}public void onTextChanged(CharSequence t,int st,int before,int count){clearSearch.setVisibility(t.length()>0?View.VISIBLE:View.GONE);}public void afterTextChanged(Editable e){}});
  search.setOnEditorActionListener((v,id,event)->{String q=search.getText().toString().trim();if(q.isEmpty()){search.setError("Masukkan kata carian");return true;}openSection(false);list(q);return true;});
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setClipToPadding(false);
  card.setBackground(rounded(0xfffdfefe,0xffd0d8df,10));
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);page.addView(card,cp);
  // The frontpage category tiles now begin directly beneath the search controls.
  int noteCount=0,bookCount=0;
  for(String postingKey:prefs.getAll().keySet()){
   if(!(prefs.getAll().get(postingKey) instanceof String)||prefs.getBoolean("deleted_"+postingKey,false))continue;
   if(postingKey.startsWith("n_"))noteCount++;
   else if(postingKey.startsWith("m_"))bookCount++;
  }
  LinearLayout tiles=new LinearLayout(this);tiles.setPadding(dp(8),dp(3),dp(8),dp(5));card.addView(tiles);
  for(int j=0;j<2;j++){
   final boolean isBook=j==1;
   LinearLayout tile=new LinearLayout(this);tile.setGravity(Gravity.CENTER_VERTICAL);
   tile.setPadding(dp(8),0,dp(8),0);
   tile.setBackground(dashboardTileSurface(isBook));tile.setElevation(dp(1));
   TextView symbol=dashboardText(isBook?"▤":"✎",20,false);
   symbol.setTextColor(0xff528069);symbol.setPadding(0,0,dp(5),0);tile.addView(symbol);
   TextView label=dashboardText(isBook?"Maktabah":"Nota",12,true);
   label.setTextColor(0xff365c48);label.setSingleLine(true);label.setPadding(0,0,0,0);
   tile.addView(label,new LinearLayout.LayoutParams(0,-2,1));
   TextView count=dashboardText(String.valueOf(isBook?bookCount:noteCount),14,true);
   count.setTextColor(0xff52725e);count.setGravity(Gravity.CENTER);count.setPadding(dp(5),0,dp(5),0);
   count.setBackground(rounded(isBook?0xffe0e5e1:0xffd0e0d5,isBook?0xffd7ded9:0xffcbdcd0,8));
   tile.addView(count,new LinearLayout.LayoutParams(-2,dp(24)));
   LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(54),1);
   if(isBook)tp.leftMargin=dp(4);else tp.rightMargin=dp(4);
   tiles.addView(tile,tp);tile.setFocusable(true);
   tile.setContentDescription((isBook?"Maktabah, ":"Nota, ")+(isBook?bookCount:noteCount)+" topik");
   tile.setOnClickListener(v->openSection(isBook));applyTouchFeedback(tile);
  }
  // Compact GitHub-style shortcuts, without changing the approved header.
  LinearLayout shortcuts=new LinearLayout(this);shortcuts.setOrientation(1);
  shortcuts.setPadding(dp(2),dp(2),dp(2),dp(1));page.addView(shortcuts);
  if(getResources().getConfiguration().screenWidthDp>=500){
   LinearLayout sectionColumns=new LinearLayout(this);
   sectionColumns.setOrientation(LinearLayout.HORIZONTAL);
   LinearLayout pinnedColumn=new LinearLayout(this);pinnedColumn.setOrientation(LinearLayout.VERTICAL);
   LinearLayout updatedColumn=new LinearLayout(this);updatedColumn.setOrientation(LinearLayout.VERTICAL);
   LinearLayout.LayoutParams pinnedParams=new LinearLayout.LayoutParams(0,-2,1);
   pinnedParams.rightMargin=dp(5);
   LinearLayout.LayoutParams updatedParams=new LinearLayout.LayoutParams(0,-2,1);
   updatedParams.leftMargin=dp(5);
   sectionColumns.addView(pinnedColumn,pinnedParams);
   sectionColumns.addView(updatedColumn,updatedParams);
   shortcuts.addView(sectionColumns,new LinearLayout.LayoutParams(-1,-2));
   dashboardSection(pinnedColumn,"Nota disemat",dashboardKeys("pinned_",false),true);
   dashboardSection(updatedColumn,"Terakhir dikemaskini",dashboardKeys("updated_",false),false);
  }else{
   dashboardSection(shortcuts,"Nota disemat",dashboardKeys("pinned_",false),true);
   dashboardSection(shortcuts,"Terakhir dikemaskini",dashboardKeys("updated_",false),false);
  }
  View flexibleSpace=new View(this);
  shortcuts.addView(flexibleSpace,new LinearLayout.LayoutParams(-1,0));
  homeFlexibleSpace[0]=flexibleSpace;
  String resume=prefs.getString("last_edited_key","");
  if(!resume.isEmpty()&&prefs.contains(resume)){
   TextView resumeButton=dashboardText("✎  Sambung menulis  ›",15,true);
   resumeButton.setBackground(rounded(0xfff5f7f9,0xffd7bd79,8));resumeButton.setTextColor(0xff566a70);
   shortcuts.addView(resumeButton);
   resumeButton.setContentDescription("Sambung menulis nota terakhir");resumeButton.setFocusable(true);applyTouchFeedback(resumeButton);resumeButton.setOnClickListener(v->edit(resume));
  }
  LinearLayout recentNotes=new LinearLayout(this);recentNotes.setOrientation(1);
  recentNotes.setPadding(dp(10),dp(4),dp(10),dp(7));card.addView(recentNotes);
  ArrayList<String> keys=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if((k.startsWith("n_")||k.startsWith("m_"))&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&prefs.getLong("viewed_"+k,0L)>0L)keys.add(k);
  Collections.sort(keys,(firstKey,secondKey)->{int order=Long.compare(prefs.getLong("viewed_"+secondKey,0L),prefs.getLong("viewed_"+firstKey,0L));return order!=0?order:firstKey.compareTo(secondKey);});
  if(keys.isEmpty()){
   TextView create=dashboardText("＋  Cipta nota pertama",13,true);create.setGravity(Gravity.CENTER);create.setMinHeight(dp(48));create.setContentDescription("Cipta nota baharu");applyTouchFeedback(create);create.setOnClickListener(v->{maktabah=false;edit(null);});recentNotes.addView(create);
   TextView emblem=dashboardText("☪\n▤",36,true);emblem.setTextColor(0xff566a70);
   emblem.setGravity(Gravity.CENTER);recentNotes.addView(emblem);
   TextView empty=dashboardText("Belum ada nota atau kitab dilihat",18,true);
   empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff4e6269);empty.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);empty.setPadding(dp(8),dp(8),dp(8),dp(4));recentNotes.addView(empty);
   TextView hint=dashboardText("Nota atau kitab yang dibuka akan muncul di sini.",14,false);
   hint.setGravity(Gravity.CENTER);hint.setTextColor(0xff656d76);hint.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);hint.setTextSize(13);hint.setPadding(dp(12),dp(6),dp(12),dp(12));recentNotes.addView(hint);
  }else {
   // Two balanced columns of five on an unfolded Fold6; one column on narrow screens.
   boolean wide=getResources().getConfiguration().screenWidthDp>=500;
   LinearLayout columns=null,left=null,right=null;
   if(wide){
    columns=new LinearLayout(this);columns.setOrientation(LinearLayout.HORIZONTAL);
    left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);
    right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams leftParams=new LinearLayout.LayoutParams(0,-2,1);leftParams.rightMargin=dp(5);
    LinearLayout.LayoutParams rightParams=new LinearLayout.LayoutParams(0,-2,1);rightParams.leftMargin=dp(5);
    columns.addView(left,leftParams);columns.addView(right,rightParams);
    recentNotes.addView(columns,new LinearLayout.LayoutParams(-1,-2));
   }
   for(int i=0;i<(wide?10:Math.min(5,keys.size()));i++){
    if(i>=keys.size()){
     TextView placeholder=dashboardText("＋  Ruang nota "+(i+1),13,false);
     placeholder.setTextColor(0xff7a897f);placeholder.setGravity(Gravity.CENTER_VERTICAL);placeholder.setSingleLine(true);placeholder.setEllipsize(android.text.TextUtils.TruncateAt.END);placeholder.setPadding(dp(10),0,dp(8),0);
     placeholder.setBackground(subtleNoteBorder(8));
     LinearLayout.LayoutParams emptyParams=new LinearLayout.LayoutParams(-1,dp(38));emptyParams.bottomMargin=dp(4);
     (i<5?left:right).addView(placeholder,emptyParams);
     placeholder.setContentDescription("Cipta nota baharu di ruang "+(i+1));applyTouchFeedback(placeholder);placeholder.setOnClickListener(v->{maktabah=false;edit(null);});
     continue;
    }
    String k=keys.get(i);String t=prefs.getString("title_"+k,"").trim();
    if(t.trim().isEmpty())t="Tanpa tajuk";
    // Light emerald list: slightly larger titles, dates consistently aligned right.
    FrameLayout row=new FrameLayout(this);
    row.setBackground(subtleNoteBorder(9));row.setMinimumHeight(dp(42));
    TextView item=dashboardText((k.startsWith("m_")?"▱  ":"▤  ")+t,14,false);
    item.setTextColor(0xff303b42);item.setGravity(Gravity.CENTER_VERTICAL);
    item.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    item.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    item.setPadding(dp(10),dp(4),dp(prefs.getLong("viewed_"+k,0L)>0L?88:10),dp(4));item.setIncludeFontPadding(false);
    item.setMinHeight(dp(42));
    item.setTooltipText(t);item.setContentDescription(t);
    item.setSingleLine(true);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
    row.addView(item,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL));
    item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    long when=prefs.getLong("viewed_"+k,0L);
    if(when>0L){
     TextView date=dashboardText(new java.text.SimpleDateFormat("dd MMM",new java.util.Locale("ms","MY")).format(new java.util.Date(when)),12,false);
     date.setTextColor(0xff60757b);date.setGravity(Gravity.CENTER);date.setSingleLine(true);date.setTextDirection(View.TEXT_DIRECTION_LTR);
     date.setBackground(rounded(0xfffaf7ee,0xffe8d9b6,14));date.setIncludeFontPadding(false);date.setMaxLines(1);date.setEllipsize(android.text.TextUtils.TruncateAt.END);
     date.setPadding(dp(7),dp(3),dp(7),dp(3));
     FrameLayout.LayoutParams dateParams=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.CENTER_VERTICAL);
     dateParams.rightMargin=dp(8);row.addView(date,dateParams);date.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);date.setContentDescription("Dilihat pada "+new java.text.SimpleDateFormat("dd MMMM",new java.util.Locale("ms","MY")).format(new java.util.Date(when)));
    }
    LinearLayout.LayoutParams itemParams=new LinearLayout.LayoutParams(-1,-2);
    itemParams.bottomMargin=dp(5);
    if(wide)(i<5?left:right).addView(row,itemParams);else recentNotes.addView(row,itemParams);
    applyTouchFeedback(row);row.setFocusable(true);
    row.setContentDescription("Buka "+(k.startsWith("m_")?"kitab ":"nota ")+t);
    row.setTooltipText(t);
    View.OnClickListener openRecent=v->{maktabah=k.startsWith("m_");read(k);};
    row.setOnClickListener(openRecent);
    row.setOnLongClickListener(v->{showDashboardActions(k);return true;});
    item.setOnClickListener(openRecent);
    for(int child=0;child<row.getChildCount();child++){
     View childView=row.getChildAt(child);
     childView.setOnClickListener(openRecent);
     childView.setOnLongClickListener(v->{showDashboardActions(k);return true;});
    }
   }
  }
  TextView add=dashboardText("＋  Nota baharu",15,true);add.setGravity(Gravity.CENTER);
  add.setBackground(rounded(0xfff5f7f9,0xffd0d8df,9));add.setTextColor(0xff566a70);
  add.setPadding(dp(12),dp(8),dp(12),dp(8));
  // Fixed home footer: keep the primary action visible above Android navigation,
  // independent of dashboard scrolling and window size.
  LinearLayout footer=new LinearLayout(this);
  footer.setOrientation(LinearLayout.VERTICAL);
  footer.setPadding(dp(10),dp(5),dp(10),dp(7));
  footer.setBackgroundColor(0xfffdfefe);
  footer.setElevation(dp(3));
  root.addView(footer,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(42));
  LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
  footer.addView(actions,new LinearLayout.LayoutParams(-1,dp(42)));
  LinearLayout.LayoutParams noteActionParams=new LinearLayout.LayoutParams(0,-1,1);noteActionParams.rightMargin=dp(4);
  actions.addView(add,noteActionParams);
  TextView addBook=dashboardText("＋  Kitab baharu",15,true);
  addBook.setGravity(Gravity.CENTER);addBook.setTextColor(0xff365c48);
  addBook.setBackground(rounded(0xffeef5ef,0xffc4d8c8,9));
  LinearLayout.LayoutParams bookActionParams=new LinearLayout.LayoutParams(0,-1,1);bookActionParams.leftMargin=dp(4);
  actions.addView(addBook,bookActionParams);
  addBook.setContentDescription("Cipta kitab baharu");addBook.setOnClickListener(v->{maktabah=true;edit(null);});applyTouchFeedback(addBook);
  add.setContentDescription("Cipta nota baharu");add.setFocusable(true);
  add.setOnClickListener(v->{maktabah=false;edit(null);});
  applyTouchFeedback(add);applyTouchFeedback(recent);
 }
 void showDashboardActions(String key){
  if(!prefs.contains(key)||prefs.getBoolean("deleted_"+key,false))return;
  boolean pinned=prefs.getBoolean("pinned_"+key,false);
  String label=prefs.getString("title_"+key,"").trim();
  String[] options={"Edit",pinned?"Buka sematan":"Semat","Duplikasi","Alih ke Tong Sampah"};
  int[] icons={android.R.drawable.ic_menu_edit,android.R.drawable.ic_menu_agenda,android.R.drawable.ic_menu_add,android.R.drawable.ic_menu_delete};
  android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,options){
   @Override public View getView(int position,View convertView,android.view.ViewGroup parent){
    TextView row=(TextView)super.getView(position,convertView,parent);
    android.graphics.drawable.Drawable icon=getResources().getDrawable(icons[position],getTheme()).mutate();
    icon.setTint(position==3?0xffb64545:0xff398264);
    icon.setBounds(0,0,dp(22),dp(22));
    row.setCompoundDrawablesRelative(icon,null,null,null);
    row.setCompoundDrawablePadding(dp(14));row.setPadding(dp(20),dp(10),dp(16),dp(10));
    return row;
   }
  };
  new AlertDialog.Builder(this).setTitle(label.isEmpty()?"Tanpa tajuk":label)
   .setAdapter(adapter,(d,i)->{
    maktabah=key.startsWith("m_");
    if(i==0)edit(key);
    else if(i==1){prefs.edit().putBoolean("pinned_"+key,!pinned).apply();home();}
    else if(i==2)duplicateNote(key);
    else confirmDelete(key);
   }).show();
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
    prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&
    (field.equals("pinned_")?prefs.getBoolean(field+k,false):prefs.getLong(field+k,0L)>0))out.add(k);
  Collections.sort(out,(a,b)->{
   int comparison=field.equals("pinned_")?
    prefs.getString("title_"+a,"").trim().compareToIgnoreCase(prefs.getString("title_"+b,"").trim()):
    Long.compare(prefs.getLong(field+b,0),prefs.getLong(field+a,0));
   return comparison!=0?comparison:a.compareTo(b);
  });
  return out;
 }
 android.graphics.drawable.GradientDrawable dashboardTileSurface(boolean pearl){
  android.graphics.drawable.GradientDrawable surface=new android.graphics.drawable.GradientDrawable(
   android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
   pearl?new int[]{0xffffffff,0xffe9edeb}:new int[]{0xfffafffb,0xffddece2});
  surface.setCornerRadius(dp(12));
  surface.setStroke(Math.max(1,Math.round(getResources().getDisplayMetrics().density*0.6f)),pearl?0xffd2d9d5:0xffc3d3c8);
  return surface;
 }
 void dashboardSection(LinearLayout target,String heading,ArrayList<String> keys,boolean pin){
  // Keep folded rows compact; unfolded columns retain comfortable touch targets.
  boolean folded=getResources().getConfiguration().screenWidthDp<500;
  boolean collapsed=prefs.getBoolean(pin?"home_pinned_collapsed":"home_updated_collapsed",false);
  TextView title=dashboardText((pin?"◆  ":"◷  ")+heading+"    "+Math.min(pin?5:3,keys.size())+" Topik  "+(collapsed?"▸":"▾"),15,true);
  title.setTextColor(0xff365c48);
  title.setBackground(dashboardTileSurface(!pin));title.setElevation(dp(1));title.setPadding(dp(10),dp(folded?6:8),dp(10),dp(folded?6:8));title.setIncludeFontPadding(false);title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setContentDescription(heading+", ketik untuk buka atau lipat");title.setTooltipText(heading);title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);titleParams.topMargin=dp(5);titleParams.bottomMargin=dp(5);target.addView(title,titleParams);
  title.setFocusable(true);applyTouchFeedback(title);
  title.setOnClickListener(v->{prefs.edit().putBoolean(pin?"home_pinned_collapsed":"home_updated_collapsed",!collapsed).apply();home();});
  if(collapsed)return;
  if(keys.isEmpty()){
   TextView empty=dashboardText(pin?"Belum ada nota disemat":"Belum ada nota dikemaskini",13,false);
   empty.setTextColor(0xff68756c);empty.setPadding(dp(10),dp(6),dp(10),dp(8));
   empty.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   empty.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
   empty.setIncludeFontPadding(false);
   empty.setMaxLines(2);empty.setEllipsize(android.text.TextUtils.TruncateAt.END);
   empty.setMinHeight(dp(36));empty.setGravity(Gravity.CENTER_VERTICAL);
   empty.setContentDescription(pin?"Tiada nota disemat":"Tiada nota yang dikemaskini");
   target.addView(empty);return;
  }
  for(int i=0;i<Math.min(pin?5:3,keys.size());i++){
   String key=keys.get(i),label=prefs.getString("title_"+key,"").trim();
   if(label.isEmpty()){
    String body=prefs.getString(key,"").trim();
    if(!body.isEmpty())label=body.split("\\n",2)[0].trim();
   }
   TextView entry=dashboardText((key.startsWith("m_")?"▱  ":"▤  ")+(label.isEmpty()?"Tanpa tajuk":label),14,false);
   entry.setTextColor(0xff303b42);entry.setGravity(Gravity.CENTER_VERTICAL);
   entry.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   entry.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
   entry.setSingleLine(true);entry.setEllipsize(android.text.TextUtils.TruncateAt.END);
   entry.setIncludeFontPadding(false);entry.setPadding(0,0,0,0);entry.setMinWidth(0);
   LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);line.setGravity(Gravity.CENTER_VERTICAL);
   line.setPadding(dp(7),dp(folded?3:7),dp(7),dp(folded?3:7));line.setBackground(subtleNoteBorder(9));line.setMinimumHeight(dp(folded?38:48));
   if(pin){entry.setCompoundDrawablePadding(dp(5));entry.setTextColor(0xff8b6925);}
   line.addView(entry,new LinearLayout.LayoutParams(0,-2,1));
   Object rawStamp=prefs.getAll().get("updated_"+key);
   long stamp=rawStamp instanceof Number?((Number)rawStamp).longValue():0L;
   if(stamp>0L){
    java.util.Calendar cal=java.util.Calendar.getInstance();cal.setTimeInMillis(stamp);
    String[] months={"Jan","Feb","Mac","Apr","Mei","Jun","Jul","Ogo","Sep","Okt","Nov","Dis"};
    String date=cal.get(java.util.Calendar.DAY_OF_MONTH)+" "+months[cal.get(java.util.Calendar.MONTH)];
    if(!pin)date+=" · "+new java.text.SimpleDateFormat("HHmm",java.util.Locale.ROOT).format(new java.util.Date(stamp));
    TextView time=dashboardText(date,10,true);time.setTextColor(0xff68756c);time.setSingleLine(true);time.setTextDirection(View.TEXT_DIRECTION_LTR);time.setIncludeFontPadding(false);time.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);time.setPadding(dp(5),0,0,0);
    time.setBackground(rounded(pin?0xffb8d8c5:0xffd1e4d8,pin?0xff81ae93:0xff9bbba7,8));
    time.setTextColor(0xff174d37);time.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));time.setGravity(Gravity.CENTER);time.setPadding(dp(5),0,dp(5),0);
    LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(-2,dp(24));dateParams.leftMargin=dp(4);
    line.addView(time,dateParams);
   }
   LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.bottomMargin=dp(folded?3:5);target.addView(line,ep);applyTouchFeedback(line);
   line.setFocusable(true);line.setContentDescription("Buka "+(key.startsWith("m_")?"kitab ":"nota ")+(label.isEmpty()?"Tanpa tajuk":label));line.setTooltipText(label.isEmpty()?"Tanpa tajuk":label);
   line.setOnClickListener(v->{maktabah=key.startsWith("m_");read(key);});
   line.setOnLongClickListener(v->{showDashboardActions(key);return true;});
  }
 }
 android.graphics.drawable.GradientDrawable rounded(int fill,int stroke,int radius){
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
  d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;
 }

 void showDashboardNotes(LinearLayout page,boolean important){View view=page.findViewWithTag("dashboard_notes");if(view instanceof LinearLayout)populateDashboardNotes((LinearLayout)view,important);}
 void populateDashboardNotes(LinearLayout target,boolean important){
  target.removeAllViews();
  ArrayList<String> keys=new ArrayList<>();
  for(String k:prefs.getAll().keySet())
   if(k.startsWith("n_")&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false))keys.add(k);
  Collections.sort(keys,(a,b)->{
   int byDate=Long.compare(prefs.getLong("updated_"+b,0L),prefs.getLong("updated_"+a,0L));
   return byDate!=0?byDate:a.compareTo(b);
  });
  int count=0;int[] colors={0xfff5f7f9,0xfff6f7f9,0xffeef3f7,0xfff7f7fa};
  for(String k:keys){
   if(important&&!prefs.getString("cat_"+k,"").equalsIgnoreCase("Penting"))continue;
   if(count>=5)break;
   String title=prefs.getString("title_"+k,"").trim();
   String content=prefs.getString(k,"");
   if(title.isEmpty())title=content.trim().isEmpty()?"Tanpa tajuk":content.split("\\n",2)[0].trim();
   if(title.isEmpty())title="Tanpa tajuk";
   LinearLayout card=dashboardCard();
   android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
   bg.setColor(colors[count%colors.length]);bg.setCornerRadius(dp(14));card.setBackground(bg);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);target.addView(card,lp);
   TextView heading=dashboardText("▤  "+title,16,true);
   heading.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   heading.setSingleLine(true);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
   heading.setTooltipText(title);card.addView(heading);
   String preview=cleanCardPreview(content);
   if(preview.length()>100)preview=preview.substring(0,100)+"…";
   TextView detail=dashboardText(preview,13,false);detail.setTextColor(0xff57635e);
   detail.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   detail.setMaxLines(2);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
   card.addView(detail);
   card.setMinimumHeight(dp(48));applyTouchFeedback(card);
   card.setContentDescription("Buka nota "+title);
   card.setOnClickListener(v->{maktabah=false;read(k);});count++;
  }
  if(count==0){
   TextView empty=dashboardText(important?"Tiada nota dalam kategori Penting.":"Belum ada nota. Tekan + untuk mencipta nota.",14,false);
   empty.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);target.addView(empty);
  }
 }

 boolean matchesSearch(String content,String[] terms){String normalized=content.replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);for(String term:terms)if(!normalized.contains(term))return false;return true;}
 String cleanCardPreview(String raw){
  if(raw==null)return "";
  // Some imported samples contain escaped newline sequences rather than line breaks.
  String cleaned=raw.replace("\\r\\n"," ").replace("\\n"," ").replace("\\r"," ").replace("\\t"," ");
  cleaned=cleaned.replaceAll("\\s+"," ").trim();
  // Wrap Arabic runs individually to preserve natural RTL order within Malay sentences.
  java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("[\\p{InArabic}]+(?:\\s+[\\p{InArabic}]+)*").matcher(cleaned);
  StringBuffer result=new StringBuffer();
  android.text.BidiFormatter formatter=android.text.BidiFormatter.getInstance(false);
  while(matcher.find()){
   String run=matcher.group();
   if(!run.matches(".*[\\p{InArabic}].*"))continue;
   matcher.appendReplacement(result,java.util.regex.Matcher.quoteReplacement(formatter.unicodeWrap(run.trim(),android.text.TextDirectionHeuristics.RTL)));
  }
  matcher.appendTail(result);
  return result.toString();
 }
 void list(String filter){filter=filter.trim();String query=filter.replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);String[] terms=query.isEmpty()?new String[0]:query.split(" ");items.removeAllViews();ArrayList<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false))keys.add(k);Collections.sort(keys,(a,b)->{
   boolean pinA=prefs.getBoolean("pinned_"+a,false),pinB=prefs.getBoolean("pinned_"+b,false);
   if(pinA!=pinB)return pinA?-1:1;
   if("Tajuk A–Z".equals(sortMode)){int byTitle=prefs.getString("title_"+a,"").trim().compareToIgnoreCase(prefs.getString("title_"+b,"").trim());if(byTitle!=0)return byTitle;}
   if("Kerap dibuka".equals(sortMode)){int byViews=Long.compare(prefs.getLong("viewed_"+b,0),prefs.getLong("viewed_"+a,0));if(byViews!=0)return byViews;}
   long ta=prefs.getLong("updated_"+a,0),tb=prefs.getLong("updated_"+b,0);
   int byDate="Terlama".equals(sortMode)?Long.compare(ta,tb):Long.compare(tb,ta);return byDate!=0?byDate:a.compareTo(b);
  });int matches=0;for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");if(!matchesSearch(heading+" "+v+" "+prefs.getString("cat_"+k,"Tanpa kategori"),terms))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;matches++;}if(matches>0){
 LinearLayout countRow=new LinearLayout(this);countRow.setOrientation(LinearLayout.HORIZONTAL);countRow.setGravity(Gravity.CENTER_VERTICAL);countRow.setPadding(dp(16),dp(3),dp(12),dp(3));
 TextView countLabel=text(matches+" "+(maktabah?"kitab":"nota"),12);countLabel.setTextColor(0xff687078);countRow.addView(countLabel,new LinearLayout.LayoutParams(0,-2,1));
 String resumeKey=prefs.getString(maktabah?"reading_last_m":"reading_last_n","");
 if(resumeKey.startsWith(maktabah?"m_":"n_")&&prefs.contains(resumeKey)&&!prefs.getBoolean("deleted_"+resumeKey,false)){
  String resumeTitle=prefs.getString("title_"+resumeKey,"Tanpa tajuk").trim();
  TextView resume=text("Sambung: "+resumeTitle+"  →",12);
  resume.setTypeface(null,android.graphics.Typeface.BOLD);resume.setTextColor(0xff566a70);
  resume.setSingleLine(true);resume.setEllipsize(android.text.TextUtils.TruncateAt.END);
  resume.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
  resume.setContentDescription("Sambung bacaan "+resumeTitle);resume.setPadding(dp(6),dp(5),0,dp(5));
  applyTouchFeedback(resume);resume.setOnClickListener(v->read(resumeKey));
  countRow.addView(resume,new LinearLayout.LayoutParams(0,-2,2));
 }
 if(compactSummary!=null){compactSummary.removeAllViews();compactSummary.addView(countRow,new LinearLayout.LayoutParams(-1,-2));}else items.addView(countRow);
}else if(compactSummary!=null)compactSummary.removeAllViews();if(matches==0){TextView empty=text(keys.isEmpty()?(maktabah?"▤  Belum ada kitab\nTekan + Kitab baharu untuk bermula":"✎  Belum ada nota\nTekan + Nota baharu untuk mula mencatat"):"Tiada hasil carian yang sepadan",17);empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff727985);empty.setPadding(dp(20),dp(56),dp(20),dp(56));items.addView(empty);if(keys.isEmpty()&&query.isEmpty()){empty.setContentDescription(maktabah?"Tambah kitab baharu":"Tambah nota baharu");applyTouchFeedback(empty);empty.setOnClickListener(v->edit(null));}else{empty.setText(query.isEmpty()?(maktabah?"Tiada kitab dalam kategori ini":"Tiada nota dalam kategori ini"):"Tiada hasil carian yang sepadan");}if(!query.isEmpty())empty.setText("Tiada hasil untuk: "+(filter.length()>60?filter.substring(0,60)+"…":filter));}boolean twoColumnList=getResources().getConfiguration().screenWidthDp>=600;
 LinearLayout cardRow=null;int visibleCards=0;
 // In unfolded mode show each group of ten as five cards per column.
 ArrayList<String> visibleKeys=new ArrayList<>();
 for(String candidate:keys){
  String body=prefs.getString(candidate,"");
  String heading=prefs.getString("title_"+candidate,"");
  if(!matchesSearch(heading+" "+body+" "+prefs.getString("cat_"+candidate,"Tanpa kategori"),terms))continue;
  if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+candidate,"Tanpa kategori")))continue;
  visibleKeys.add(candidate);
 }
 ArrayList<String> displayKeys=new ArrayList<>();
 if(twoColumnList){
  for(int start=0;start<visibleKeys.size();start+=10){
   int groupSize=Math.min(10,visibleKeys.size()-start);
   int leftCount=Math.min(5,groupSize);
   for(int row=0;row<leftCount;row++){
    displayKeys.add(visibleKeys.get(start+row));
    if(row+leftCount<groupSize)displayKeys.add(visibleKeys.get(start+row+leftCount));
   }
  }
 }else displayKeys.addAll(visibleKeys);
 for(String k:displayKeys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");String shown=(heading.trim().isEmpty()?(v.trim().isEmpty()?"Tanpa tajuk":v.trim().split("\\R",2)[0]):heading.trim());shown=shown.replaceAll("\\s+"," ").trim();if(shown.length()>70)shown=shown.substring(0,70)+"…";if(shown.trim().isEmpty())shown="Tanpa tajuk";if(!matchesSearch(heading+" "+v+" "+prefs.getString("cat_"+k,"Tanpa kategori"),terms))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;String preview=cleanCardPreview(v);if(preview.isEmpty())preview="Belum ada kandungan";String cat=prefs.getString("cat_"+k,"Tanpa kategori").trim();if(cat.isEmpty())cat="Tanpa kategori";long stamp=prefs.getLong("updated_"+k,0);String date=stamp>0?new java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(new java.util.Date(stamp)):"Belum dikemas kini";String previewText=preview.length()>80?preview.substring(0,80)+"…":preview;
 String cardTitle=(prefs.getBoolean("pinned_"+k,false)?"📌  ":"")+(maktabah?"▤  ":"✎  ")+shown;
 LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
 card.setBackground(rounded(k.equals(lastOpenedCard)?0xfff2faf5:0xffffffff,k.equals(lastOpenedCard)?0xffb7d8c2:0xffe1e5e3,16));
 card.setElevation(dp(1));card.setPadding(dp(twoColumnList?12:16),dp(7),dp(twoColumnList?12:16),dp(7));
 TextView headingView=text(cardTitle,15);headingView.setTypeface(null,android.graphics.Typeface.BOLD);
 headingView.setSingleLine(true);headingView.setEllipsize(android.text.TextUtils.TruncateAt.END);headingView.setTextColor(0xff30343b);
 card.addView(headingView,new LinearLayout.LayoutParams(-1,-2));
 TextView previewView=text(previewText,15);previewView.setTextColor(0xff30343b);
 previewView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);previewView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
 previewView.setMaxLines(2);previewView.setMinLines(twoColumnList?2:1);previewView.setEllipsize(android.text.TextUtils.TruncateAt.END);
 previewView.setLineSpacing(dp(3),1.05f);card.addView(previewView,new LinearLayout.LayoutParams(-1,-2));
 LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);
 TextView categoryView=text(cat,12);categoryView.setTextColor(0xff687078);categoryView.setSingleLine(true);
 categoryView.setEllipsize(android.text.TextUtils.TruncateAt.END);
 meta.addView(categoryView,new LinearLayout.LayoutParams(0,-2,1f));
 TextView dateView=text(date,12);dateView.setTextColor(0xff687078);dateView.setGravity(Gravity.END);
 dateView.setSingleLine(true);dateView.setEllipsize(android.text.TextUtils.TruncateAt.END);
 meta.addView(dateView,new LinearLayout.LayoutParams(-2,-2));
 card.addView(meta,new LinearLayout.LayoutParams(-1,-2));
 LinearLayout.LayoutParams noteCardParams=new LinearLayout.LayoutParams(twoColumnList?0:-1,-2,twoColumnList?1f:0f);noteCardParams.setMargins(dp(twoColumnList?4:12),dp(4),dp(twoColumnList?4:12),dp(4));card.setContentDescription((prefs.getBoolean("pinned_"+k,false)?"Disemat. ":"")+(maktabah?"Buka kitab: ":"Buka nota: ")+shown+", kategori "+cat+", "+date);applyTouchFeedback(card);card.setFocusable(true);
 if(twoColumnList){
  if(visibleCards%2==0){cardRow=new LinearLayout(this);cardRow.setOrientation(LinearLayout.HORIZONTAL);cardRow.setPadding(dp(8),0,dp(8),0);items.addView(cardRow,new LinearLayout.LayoutParams(-1,-2));}
  cardRow.addView(card,noteCardParams);
 }else items.addView(card,noteCardParams);
 visibleCards++;View line=new View(this);line.setBackgroundColor(0xffe5e9e5);--matches;card.setOnClickListener(w->{
  if(listScroll!=null){if(maktabah)savedBooksScroll=listScroll.getScrollY();else savedNotesScroll=listScroll.getScrollY();}
  lastOpenedCard=k;read(k);
 });}}

 int readerBuiltWidthPx=0;
 void cleanBase(){
  readerDetector=null;root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.WHITE);
  final LinearLayout activeRoot=root;
  final int[] lastWidth={0};
  activeRoot.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
   int width=right-left;
   if(width<=0)return;
   int previous=lastWidth[0];lastWidth[0]=width;
   // Split-screen resizing may change window width without folding or recreating Activity.
   if(previous>0&&Math.abs(width-previous)>=dp(16)&&editor!=null){
    // Reflow the active editor in place when the split-screen divider moves.
    // Avoid rebuilding the editor: that would disrupt cursor, undo and autosave.
    final EditText activeEditor=editor;
    activeRoot.post(()->{
     if(root==activeRoot&&editor==activeEditor&&activeEditor.getParent()!=null){
      activeEditor.requestLayout();
      activeEditor.invalidate();
      if(titleEditor!=null){titleEditor.requestLayout();titleEditor.invalidate();}
     }
    });
   }
   // First layout can reveal a different width than the previous window/decor
   // measurement used to build the reader (common on Fold6 multi-window).
   boolean firstReaderWidthMismatch=previous==0&&editor==null&&selected!=null
     &&readerBuiltWidthPx>0&&Math.abs(width-readerBuiltWidthPx)>=dp(16);
   if((firstReaderWidthMismatch||(previous>0&&Math.abs(width-previous)>=dp(16)))&&editor==null&&selected!=null){
    final String readingKey=selected;
    activeRoot.post(()->{
     if(root==activeRoot&&!isFinishing()&&!isDestroyed()&&editor==null
       &&readingKey.equals(selected)&&prefs.contains(readingKey)){
      saveReaderPosition(readingKey);
      read(readingKey);
     }
    });
   }
  });
  setContentView(root);applySystemBarInsets(root);
 }
 Button action(String label,LinearLayout bar,Runnable callback){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setBackground(rounded(0xfff6f8fa,0xffe1e7e3,12));LinearLayout.LayoutParams buttonParams=new LinearLayout.LayoutParams(-2,dp("reader_toolbar".equals(bar.getTag())?34:42));buttonParams.setMargins(dp(3),dp("reader_toolbar".equals(bar.getTag())?3:5),dp(3),dp("reader_toolbar".equals(bar.getTag())?3:5));bar.addView(b,buttonParams);if("reader_toolbar".equals(bar.getTag())){android.graphics.drawable.GradientDrawable emerald=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xffffffff,0xffedf7f0});emerald.setCornerRadius(dp(13));emerald.setStroke(dp(1),0xffb6cdbd);b.setBackground(emerald);b.setTextColor(0xff566a70);b.setElevation(dp(2));b.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);}b.setOnClickListener(v->callback.run());return b;}
 void returnFromReader(){selected=null;if(sectionOpen)notesPage();else home();}


 void showPagedReader(String key){
  final String source=prefs.getString(key,"");
  final int availableWidthDp=Math.round(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density);
  final boolean spread=availableWidthDp>=500&&prefs.getBoolean("reader_two_columns",true);
  final int fontLevel=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  final int fontSize=new int[]{13,15,16,18}[fontLevel];
  final int paper=0xfffffefd;
  final String heading=prefs.getString("title_"+key,"");
  TextView headingView=text(heading,17);headingView.setGravity(Gravity.CENTER);headingView.setTypeface(null,android.graphics.Typeface.BOLD);headingView.setMaxLines(2);
  headingView.setEllipsize(android.text.TextUtils.TruncateAt.END);
  headingView.setPadding(dp(16),dp(8),dp(16),dp(8));
  headingView.setTextColor(0xff4d6068);headingView.setBackground(rounded(0xfffffefd,0xffb8c2ca,12));headingView.setElevation(dp(1));
  LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);
  titleParams.setMargins(dp(10),dp(6),dp(10),dp(4));root.addView(headingView,titleParams);
  LinearLayout pageRow=new LinearLayout(this);pageRow.setOrientation(LinearLayout.HORIZONTAL);
  pageRow.setPadding(dp(8),dp(1),dp(8),dp(1));
  FrameLayout readingFrame=new FrameLayout(this);
  root.addView(readingFrame,new LinearLayout.LayoutParams(-1,0,1));
  readingFrame.addView(pageRow,new FrameLayout.LayoutParams(-1,-1));
  // The divider belongs to the fixed reading frame, never to the changing page views.
  if(spread){
   FrameLayout fixedDivider=new FrameLayout(this);
   fixedDivider.setClickable(false);
   android.graphics.drawable.GradientDrawable line=new android.graphics.drawable.GradientDrawable(
    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
    new int[]{0xffc5d0d9,0xff566d75,0xffb8c2ca,0xff566d75,0xffc5d0d9});
   line.setCornerRadius(dp(1));
   View centralLine=new View(this);centralLine.setBackground(line);
   fixedDivider.addView(centralLine,new FrameLayout.LayoutParams(dp(2),-1,Gravity.CENTER));
   android.widget.ImageView motif=new android.widget.ImageView(this);
   motif.setImageResource(com.dinkolami.dinnote.R.drawable.reader_center_ornament);
   motif.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
   fixedDivider.addView(motif,new FrameLayout.LayoutParams(dp(28),dp(64),Gravity.CENTER));
   FrameLayout.LayoutParams dividerParams=new FrameLayout.LayoutParams(dp(24),-1,Gravity.CENTER_HORIZONTAL|Gravity.TOP);
   readingFrame.addView(fixedDivider,dividerParams);
  }
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
   control.setTextColor(0xff566a70);
   control.setBackground(rounded(0xffeff8f0,0xffb8d9c2,9));
  }
  LinearLayout.LayoutParams controlParams;
  // Bookmark remains in the upper reader toolbar; omit duplicate lower control.
  // Progress label sits on the lower reading-frame border, not in the toolbar.
  indicator.setTextColor(0xff566a70);
  indicator.setBackground(rounded(0xfffffefd,0xffb8c2ca,10));
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
  TextView loading=text(source,Math.max(12,fontSize));loading.setTextColor(0xff303c43);loading.setPadding(dp(12),dp(12),dp(12),dp(12));pageRow.addView(loading,new LinearLayout.LayoutParams(-1,-1));
  pageRow.post(()->{
   if(isFinishing()||isDestroyed()||pageRow.getParent()==null||root==null||!key.equals(selected)||!pageRow.isAttachedToWindow())return;
   try{
    // Measure actual rendered lines, not an estimated character count.
    // Keep a small bottom reserve so the final line is never clipped.
    int width=Math.max(dp(80),(pageRow.getWidth()-dp(16)-(spread?dp(8):0))/(spread?2:1));
    int textWidth=Math.max(dp(40),width-dp(32)); // Measure narrower than the actual page to avoid unexpected wraps.
    int usableHeight=Math.max(dp(40),pageRow.getHeight()-dp(20)-dp(72)); // Reserve an extra full text line and font-descent safety margin.
    // Use identical line spacing in the measuring layout and visible pages.
    final boolean arabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(source).find();
    final int extraLine=prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1);
    final float lineMultiplier=prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f;
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
      int overlapStart=lastLine>1?layout.getLineStart(lastLine-1):0; // Repeat only a completely measured line, never a clipped fragment.
     int stop=Math.min(source.length(),cursor+Math.max(1,chars));
     // Do not append a partly visible line: the following page will render it in full.
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
      page.setTextColor(0xff303c43);page.setGravity(Gravity.TOP);
      page.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
      if(arabic&&prefs.getBoolean("reader_arabic_clear_font",false))page.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));
      page.setLineSpacing(extraLine,lineMultiplier);
      page.setPadding(dp(12),dp(10),dp(12),dp(10));
      page.setBackground(rounded(paper,0xffd3c197,5));page.setElevation(0);
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

 void searchInsideReader(String key){searchInsideReader(key,"");}
 void searchInsideReader(String key,String preset){
  EditText field=new EditText(this);field.setSingleLine(true);field.setHint("Cari perkataan Arab atau Rumi");field.setText(preset);
  new AlertDialog.Builder(this).setTitle("Cari dalam nota / kitab").setView(field)
   .setPositiveButton("Cari",(d,w)->{
    String term=field.getText().toString().trim();
    if(term.isEmpty())return;
    String source=prefs.getString(key,"");
    String haystack=source.toLowerCase(java.util.Locale.ROOT),needle=term.toLowerCase(java.util.Locale.ROOT);
    java.util.ArrayList<Integer> matches=new java.util.ArrayList<>();
    int index=0;
    while((index=haystack.indexOf(needle,index))>=0){matches.add(index);index+=Math.max(1,needle.length());}
    if(matches.isEmpty()){Toast.makeText(this,"Tiada padanan",Toast.LENGTH_SHORT).show();return;}
    showReaderSearchResult(source,term,matches,0);
   }).setNegativeButton("Batal",null).show();
 }
 void showReaderSearchResult(String source,String term,java.util.ArrayList<Integer> matches,int selectedMatch){
  int index=matches.get(selectedMatch),from=Math.max(0,index-90),to=Math.min(source.length(),index+term.length()+120);
  String excerpt=(from>0?"…":"")+source.substring(from,to)+(to<source.length()?"…":"");
  int prefix=from>0?1:0;
  android.text.SpannableString marked=new android.text.SpannableString(excerpt);
  int begin=index-from+prefix;
  marked.setSpan(new android.text.style.BackgroundColorSpan(0xffffe6a0),begin,Math.min(marked.length(),begin+term.length()),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  marked.setSpan(new android.text.style.UnderlineSpan(),begin,Math.min(marked.length(),begin+term.length()),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  new AlertDialog.Builder(this).setTitle("Padanan "+(selectedMatch+1)+" / "+matches.size())
   .setMessage(marked)
   .setNegativeButton("Sebelumnya",(d,w)->showReaderSearchResult(source,term,matches,(selectedMatch+matches.size()-1)%matches.size()))
   .setNeutralButton("Tutup",null)
   .setPositiveButton("Seterusnya",(d,w)->showReaderSearchResult(source,term,matches,(selectedMatch+1)%matches.size())).show();
 }
 void copyReaderWithReference(String key){
  String source=prefs.getString(key,"");
  String clean=source.replaceAll("(?m)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---[ \\t]*(?:\\r?\\n)?","");
  clean=clean.replaceAll("(?m)^---(?:FLOWER|GEOMETRIC|CLASSIC)---[ \\t]*$","").trim();
  android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  if(clipboard!=null){
   clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DinNote",clean));
   Toast.makeText(this,"Semua kandungan disalin tanpa tajuk kotak",Toast.LENGTH_SHORT).show();
  }
 }
 android.text.SpannableStringBuilder formattedReadingText(String source){
  android.text.SpannableStringBuilder out=new android.text.SpannableStringBuilder(source==null?"":source);
  String[] patterns={"\\*\\*(.+?)\\*\\*","(?<!\\*)\\*(?!\\*)([^*]+?)\\*(?!\\*)","<u>(.*?)</u>","<b>(.*?)</b>","<i>(.*?)</i>"};
  for(int k=0;k<patterns.length;k++){
   java.util.regex.Matcher m=java.util.regex.Pattern.compile(patterns[k],java.util.regex.Pattern.DOTALL).matcher(out.toString());
   java.util.ArrayList<int[]> ranges=new java.util.ArrayList<>();
   while(m.find())ranges.add(new int[]{m.start(),m.start(1),m.end(1),m.end()});
   for(int i=ranges.size()-1;i>=0;i--){
    int[] p=ranges.get(i);out.delete(p[2],p[3]);out.delete(p[0],p[1]);int end=p[0]+p[2]-p[1];
    if(end>p[0])out.setSpan(k==2?new android.text.style.UnderlineSpan():new android.text.style.StyleSpan(k==0||k==3?1:2),p[0],end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   }
  }return out;
 }
 int richMask(android.text.Spanned value,int a,int b){
  int mask=0;
  for(android.text.style.StyleSpan span:value.getSpans(a,b,android.text.style.StyleSpan.class))if(value.getSpanStart(span)<=a&&value.getSpanEnd(span)>=b)mask|=span.getStyle()&3;
  for(android.text.style.UnderlineSpan span:value.getSpans(a,b,android.text.style.UnderlineSpan.class))if(value.getSpanStart(span)<=a&&value.getSpanEnd(span)>=b)mask|=4;
  return mask;
 }
 String encodedEditorText(){
  if(editor==null)return "";
  android.text.Editable e=editor.getText();StringBuilder out=new StringBuilder();int i=0;
  while(i<e.length()){
   if(e.charAt(i)=='\n'){out.append('\n');i++;continue;}
   int mask=richMask(e,i,i+1),end=i+1;
   while(end<e.length()&&e.charAt(end)!='\n'&&richMask(e,end,end+1)==mask)end++;
   if((mask&4)!=0)out.append("<u>");if((mask&1)!=0)out.append("<b>");if((mask&2)!=0)out.append("<i>");
   out.append(e.subSequence(i,end));
   if((mask&2)!=0)out.append("</i>");if((mask&1)!=0)out.append("</b>");if((mask&4)!=0)out.append("</u>");i=end;
  }return normalizeStructuralMarkers(out.toString());
 }
 void applyLiveStyle(int kind){
  if(editor==null)return;int a=Math.min(editor.getSelectionStart(),editor.getSelectionEnd()),b=Math.max(editor.getSelectionStart(),editor.getSelectionEnd());
  if(a<0)return;android.text.Editable e=editor.getText();
  boolean remove=(richMask(e,a,b)&kind)!=0;
  if(a==b){
   if(remove){
    for(android.text.style.CharacterStyle span:e.getSpans(a,a,android.text.style.CharacterStyle.class)){
     boolean match=kind==4?span instanceof android.text.style.UnderlineSpan:span instanceof android.text.style.StyleSpan&&((android.text.style.StyleSpan)span).getStyle()==kind;
     if(!match)continue;
     int x=e.getSpanStart(span),y=e.getSpanEnd(span);e.removeSpan(span);
     if(x<a)e.setSpan(copyLiveStyle(span),x,a,33);
     if(y>a)e.setSpan(copyLiveStyle(span),a,y,33);
    }
    save();return;
   }
   if(kind==4)e.setSpan(new android.text.style.UnderlineSpan(),a,b,android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE);
   else e.setSpan(new android.text.style.StyleSpan(kind),a,b,android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE);
  }else{
   if(remove){
    for(android.text.style.CharacterStyle span:e.getSpans(a,b,android.text.style.CharacterStyle.class)){
     boolean match=kind==4?span instanceof android.text.style.UnderlineSpan:span instanceof android.text.style.StyleSpan&&((android.text.style.StyleSpan)span).getStyle()==kind;
     if(!match)continue;int x=e.getSpanStart(span),y=e.getSpanEnd(span);e.removeSpan(span);
     if(x<a)e.setSpan(kind==4?new android.text.style.UnderlineSpan():new android.text.style.StyleSpan(kind),x,a,33);
     if(y>b)e.setSpan(kind==4?new android.text.style.UnderlineSpan():new android.text.style.StyleSpan(kind),b,y,33);
    }
   }else e.setSpan(kind==4?new android.text.style.UnderlineSpan():new android.text.style.StyleSpan(kind),a,b,33);
  }save();
 }
 void showReaderSettings(String key){
  ScrollView scroll=new ScrollView(this);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(16),dp(6),dp(16),dp(12));scroll.addView(panel);
  final AlertDialog[] popup=new AlertDialog[1];
  String[] headings={"SAIZ TULISAN","JARAK BARIS","TULISAN ARAB"};
  String[][] options={{"Kecil","Biasa","Besar","Sangat besar"},{"Biasa","Luas"},{"Biasa","Lebih jelas"}};
  int font=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  int[] selected={font,prefs.getBoolean("reader_wide_spacing",false)?1:0,prefs.getBoolean("reader_arabic_clear_font",false)?1:0};
  for(int group=0;group<3;group++){
   final int g=group;
   TextView heading=text(headings[group],12);heading.setTypeface(null,android.graphics.Typeface.BOLD);
   heading.setTextColor(0xff566a70);heading.setPadding(dp(4),dp(group==0?10:16),dp(4),dp(8));panel.addView(heading);
   LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
   row.setPadding(dp(3),dp(3),dp(3),dp(3));
   row.setBackground(rounded(0xfff7faf8,0xffdce5df,12));panel.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));
   for(int index=0;index<options[group].length;index++){
    final int choice=index;boolean active=selected[group]==index;
    TextView chip=text(options[group][index],group==0?12:13);
    chip.setGravity(Gravity.CENTER);chip.setSingleLine(true);
    chip.setTextColor(active?0xff566a70:0xff56665d);
    if(active)chip.setTypeface(null,android.graphics.Typeface.BOLD);
    chip.setBackground(rounded(active?0xffe0f2e8:0xfff7faf8,active?0xffb5d7c2:0xfff7faf8,9));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);lp.setMargins(dp(2),0,dp(2),0);
    row.addView(chip,lp);applyTouchFeedback(chip);
    chip.setContentDescription(options[group][index]+(active?", dipilih":""));
    chip.setOnClickListener(v->{
     if(g==0)prefs.edit().putInt("reader_font_level",choice).apply();
     else if(g==1)prefs.edit().putBoolean("reader_wide_spacing",choice==1).apply();
     else prefs.edit().putBoolean("reader_arabic_clear_font",choice==1).apply();
     popup[0].dismiss();read(key);
    });
   }
  }
  popup[0]=new AlertDialog.Builder(this).setTitle("Tetapan bacaan")
   .setView(scroll).setNegativeButton("Tutup",null).create();
  popup[0].show();
 }
 void showBookIndex(String key){
  String content=prefs.getString(key,"");
  String[] lines=content.split("\\n");
  java.util.ArrayList<String> headings=new java.util.ArrayList<>();
  java.util.ArrayList<String> terms=new java.util.ArrayList<>();
  for(String raw:lines){
   String line=raw.trim();
   if(line.length()<3||line.length()>110)continue;
   String lower=line.toLowerCase(java.util.Locale.ROOT);
   if(lower.matches("^(bab|fasal|chapter|section|juz)\\s+.*")||line.startsWith("باب ")||line.startsWith("فصل ")||line.startsWith("جزء ")){
    headings.add(line);terms.add(line);
    if(headings.size()>=100)break;
   }
  }
  if(headings.isEmpty()){
   new AlertDialog.Builder(this).setTitle("Indeks Kitab")
    .setMessage("Belum ada tajuk bab atau fasal yang dikenal pasti. Gunakan baris bermula dengan Bab, Fasal, باب atau فصل.")
    .setPositiveButton("Tutup",null).show();return;
  }
  new AlertDialog.Builder(this).setTitle("Indeks Kitab ("+headings.size()+")")
   .setItems(headings.toArray(new String[0]),(d,which)->{
    String target=terms.get(which);
    String source=prefs.getString(key,"");
    String haystack=source.toLowerCase(java.util.Locale.ROOT);
    String needle=target.toLowerCase(java.util.Locale.ROOT);
    java.util.ArrayList<Integer> matches=new java.util.ArrayList<>();
    int from=0;
    while((from=haystack.indexOf(needle,from))>=0){
     matches.add(from);from+=Math.max(1,needle.length());
    }
    if(matches.isEmpty())searchInsideReader(key,target);
    else showReaderSearchResult(source,target,matches,0);
   }).setNegativeButton("Tutup",null).show();
 }
 void showBookMarginNote(String key){
  if(!key.startsWith("m_"))return;
  EditText input=new EditText(this);
  input.setMinLines(4);input.setGravity(Gravity.TOP);
  input.setHint("Catatan peribadi untuk kitab ini");
  input.setText(prefs.getString("margin_note_"+key,""));
  LinearLayout wrapper=new LinearLayout(this);
  wrapper.setPadding(dp(20),dp(8),dp(20),dp(4));
  wrapper.addView(input,new LinearLayout.LayoutParams(-1,-2));
  new AlertDialog.Builder(this).setTitle("Nota Tepi Kitab")
   .setMessage("Catatan disimpan berasingan dan tidak mengubah teks asal kitab.")
   .setView(wrapper).setNegativeButton("Batal",null)
   .setPositiveButton("Simpan",(d,w)->{
    boolean ok=prefs.edit().putString("margin_note_"+key,input.getText().toString()).commit();
    Toast.makeText(this,ok?"Nota tepi disimpan":"Nota tepi gagal disimpan",Toast.LENGTH_SHORT).show();
   }).show();
 }
 void printReaderAsPdf(String key){
  String title=prefs.getString("title_"+key,"DinNote");
  String content=prefs.getString(key,"");
  String safeTitle=android.text.TextUtils.htmlEncode(title);
  String safeBody=android.text.TextUtils.htmlEncode(content).replace("\\n","<br>");
  String html="<!doctype html><html><head><meta charset='utf-8'><style>"+
   "@page{margin:18mm}body{font-family:serif;color:#26372e;font-size:13pt;line-height:1.65;overflow-wrap:anywhere}"+
   "h1{color:#176b47;font-size:19pt;border-bottom:1px solid #c5a66b;padding-bottom:8px}"+
   "</style></head><body><h1>"+safeTitle+"</h1><div dir='auto'>"+safeBody+"</div></body></html>";
  android.webkit.WebView web=new android.webkit.WebView(this);
  web.getSettings().setDefaultTextEncodingName("UTF-8");
  web.setWebViewClient(new android.webkit.WebViewClient(){
   boolean started=false;
   @Override public void onPageFinished(android.webkit.WebView view,String url){
    if(started)return;started=true;
    android.print.PrintManager manager=(android.print.PrintManager)getSystemService(PRINT_SERVICE);
    if(manager==null){Toast.makeText(MainActivity.this,"Perkhidmatan cetak tidak tersedia",Toast.LENGTH_LONG).show();return;}
    manager.print("DinNote - "+title,view.createPrintDocumentAdapter("DinNote"),
     new android.print.PrintAttributes.Builder().setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).build());
   }
  });
  web.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
 }
 void showReaderActions(String key,boolean compact){
  boolean book=key.startsWith("m_");
  String noun=book?"kitab":"nota";
  String[] labels=compact?new String[]{"Edit","Semat / Buka sematan","Duplikasi","Cari dalam teks","Salin semua teks","Mod swipe halaman","Tukar satu / dua kolum","Padam"}:new String[]{"Edit","Semat / Buka sematan","Duplikasi","Cari dalam teks","Salin semua teks","Padam"};
  String[] icons=compact?new String[]{"✎","♧","▢","⌕","▤","⇆","▥","⌫"}:new String[]{"✎","♧","▢","⌕","▤","⌫"};
  String[] descriptions=compact?new String[]{"Sunting kandungan","Kekalkan di bahagian atas","Buat salinan "+noun,"Cari perkataan","Salin kandungan semua kotak tanpa tajuk hiasan","Ubah cara menukar halaman","Pilih susunan bacaan","Padam "+noun+" ini"}:new String[]{"Sunting kandungan","Kekalkan di bahagian atas","Buat salinan "+noun,"Cari perkataan","Salin kandungan semua kotak tanpa tajuk hiasan","Padam "+noun+" ini"};
  if(book){
   int n=labels.length;
   labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Nota Tepi Kitab";icons[n-1]="✎";descriptions[n-1]="Catatan peribadi tanpa mengubah teks kitab";
   labels=java.util.Arrays.copyOf(labels,n+2);icons=java.util.Arrays.copyOf(icons,n+2);descriptions=java.util.Arrays.copyOf(descriptions,n+2);
   labels[n+1]=labels[n];icons[n+1]=icons[n];descriptions[n+1]=descriptions[n];
   labels[n]="Indeks Kitab";icons[n]="☷";descriptions[n]="Senarai bab dan fasal yang dikesan";
  }
  if(book){
   int n=labels.length;
   labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Arab–Melayu bersebelahan";icons[n-1]="▥";
   descriptions[n-1]="Baris Arab || Melayu pada skrin terbuka";
  }
  int beforePdf=labels.length;
  labels=java.util.Arrays.copyOf(labels,beforePdf+1);icons=java.util.Arrays.copyOf(icons,beforePdf+1);descriptions=java.util.Arrays.copyOf(descriptions,beforePdf+1);
  labels[beforePdf]=labels[beforePdf-1];icons[beforePdf]=icons[beforePdf-1];descriptions[beforePdf]=descriptions[beforePdf-1];
  labels[beforePdf-1]="Eksport PDF";icons[beforePdf-1]="▤";descriptions[beforePdf-1]="Cetak atau simpan sebagai PDF melalui Android";
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(12),dp(8),dp(12),dp(10));scroll.addView(panel);
  TextView header=text(book?"▤  Tindakan Maktabah":"▤  Tindakan Nota",18);
  header.setTypeface(null,android.graphics.Typeface.BOLD);header.setTextColor(0xff566a70);
  header.setPadding(dp(12),dp(10),dp(12),dp(14));panel.addView(header);
  final AlertDialog[] dialog=new AlertDialog[1];
  for(int i=0;i<labels.length;i++){
   final int option=i;boolean danger=i==labels.length-1;
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
   row.setPadding(dp(12),dp(7),dp(12),dp(7));
   row.setBackground(rounded(danger?0xfffff1f0:0xfff9fbf9,danger?0xfff1d4d2:0xffe2eae4,10));
   LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,dp(62));
   rowParams.bottomMargin=dp(5);panel.addView(row,rowParams);
   TextView symbol=text(icons[i],21);symbol.setGravity(Gravity.CENTER);
   symbol.setTextColor(danger?0xffa42e2e:0xff566a70);
   symbol.setBackground(rounded(danger?0xffffdfdc:0xffe2f1e7,danger?0xffffdfdc:0xffe2f1e7,9));
   row.addView(symbol,new LinearLayout.LayoutParams(dp(42),dp(42)));
   LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
   words.setPadding(dp(12),0,dp(4),0);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
   TextView label=text(labels[i],14);label.setTypeface(null,android.graphics.Typeface.BOLD);
   label.setTextColor(danger?0xffa42e2e:0xff25372d);words.addView(label);
   TextView detail=text(descriptions[i],11);detail.setTextColor(danger?0xffa65a56:0xff68766d);words.addView(detail);
   TextView arrow=text("›",22);arrow.setTextColor(danger?0xffa42e2e:0xff566a70);row.addView(arrow);
   applyTouchFeedback(row);
   row.setOnClickListener(v->{
    dialog[0].dismiss();
    if(option==0)edit(key);
    else if(option==1){boolean pinned=prefs.getBoolean("pinned_"+key,false);prefs.edit().putBoolean("pinned_"+key,!pinned).apply();Toast.makeText(this,pinned?"Sematan dibuang":"Disemat",Toast.LENGTH_SHORT).show();read(key);}
    else if(option==2)duplicateNote(key);
    else if(option==3)searchInsideReader(key);
    else if(option==4)copyReaderWithReference(key);
    else if(compact&&option==5){boolean next=!prefs.getBoolean("reader_swipe_mode",false);prefs.edit().putBoolean("reader_swipe_mode",next).apply();read(key);}
    else if(compact&&option==6){boolean now=prefs.getBoolean("reader_two_columns",true);prefs.edit().putBoolean("reader_two_columns",!now).apply();read(key);}
    else if(book&&option==(compact?7:5))showBookMarginNote(key);
    else if(book&&option==(compact?8:6))showBookIndex(key);
    else if(book&&option==(compact?9:7)){
     boolean next=!prefs.getBoolean("reader_bilingual_columns",false);
     prefs.edit().putBoolean("reader_bilingual_columns",next).apply();
     Toast.makeText(this,next?"Paparan Arab–Melayu diaktifkan":"Paparan Arab–Melayu dimatikan",Toast.LENGTH_SHORT).show();read(key);
    }
    else if(option==(book?(compact?10:8):(compact?7:5)))printReaderAsPdf(key);
    else confirmDelete(key);
   });
  }
  dialog[0]=new AlertDialog.Builder(this).setView(scroll).setNegativeButton("Tutup",null).create();
  dialog[0].show();
  android.view.Window window=dialog[0].getWindow();
  if(window!=null)window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(430)),-2);
 }
 void applySharedTextMetrics(android.widget.TextView view){
  int level=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  view.setTextSize(new int[]{13,15,16,18}[level]);
  view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
  view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
  view.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
  view.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
  view.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),
   prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);
  view.setIncludeFontPadding(true);
 }
 void read(String key){
  // Use the actual DinNote window width: Fold6 split-screen may be narrower than the display.
  int windowWidthPx=root!=null&&root.getWidth()>0?root.getWidth():getWindow().getDecorView().getWidth();
  final int availableReaderWidthDp=windowWidthPx>0?Math.round(windowWidthPx/getResources().getDisplayMetrics().density):getResources().getConfiguration().screenWidthDp;
  readerBuiltWidthPx=windowWidthPx;
  if(key==null||!prefs.contains(key)){
   selected=null;readerScroll=null;editor=null;titleEditor=null;
   Toast.makeText(this,"Nota atau kitab tidak ditemui",Toast.LENGTH_LONG).show();
   if(sectionOpen)notesPage();else home();
   return;
  }
  readerScroll=null;
  editor=null;titleEditor=null;saveIndicator=null;selected=key;prefs.edit().putLong("viewed_"+key,System.currentTimeMillis()).putString(key.startsWith("m_")?"reading_last_m":"reading_last_n",key).apply();cleanBase();
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(2),dp(8),dp(2));bar.setBackground(rounded(0xfffffefd,0xffb8c2ca,12));bar.setTag("reader_toolbar");root.addView(bar);
  Button readerBack=action("‹",bar,()->returnFromReader());readerBack.setContentDescription(key.startsWith("m_")?"Kembali ke senarai kitab":"Kembali ke senarai nota");
  TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
  boolean cleanReading=prefs.getBoolean("reader_clean_mode",false);
  Button cleanToggle=action(cleanReading?"▣":"▢",bar,()->{prefs.edit().putBoolean("reader_clean_mode",!cleanReading).apply();read(key);});
  cleanToggle.setContentDescription(cleanReading?"Paparkan semula toolbar bacaan":"Aktifkan mod bacaan bersih");
  if(!cleanReading){
  
  Button readerFont=action("Aa",bar,()->showReaderSettings(key));readerFont.setContentDescription("Ubah saiz tulisan Nota dan Maktabah");
  Button bookmarkButton=action("♧",bar,()->{int pos=prefs.getInt("reading_scroll_"+key,0);new AlertDialog.Builder(this).setTitle("Penanda bacaan").setMessage("Kedudukan terakhir: "+pos+" px").setPositiveButton("Simpan penanda",(d,w)->{prefs.edit().putInt("bookmark_scroll_"+key,prefs.getInt("reading_scroll_"+key,0)).apply();Toast.makeText(this,"Penanda disimpan",Toast.LENGTH_SHORT).show();}).setNeutralButton("Pergi ke penanda",(d,w)->{int target=prefs.getInt("bookmark_scroll_"+key,-1);if(target>=0){read(key);root.post(()->{ScrollView currentScroll=(ScrollView)root.getChildAt(root.getChildCount()-2);currentScroll.post(()->currentScroll.scrollTo(0,target));});}else Toast.makeText(this,"Tiada penanda",Toast.LENGTH_SHORT).show();}).setNegativeButton("Tutup",null).show();});bookmarkButton.setContentDescription("Penanda bacaan");Button readerEdit=action("✎",bar,()->edit(key));readerEdit.setContentDescription(key.startsWith("m_")?"Sunting kandungan kitab":"Sunting kandungan nota");
  boolean compactReaderToolbar=availableReaderWidthDp<500;
  if(compactReaderToolbar){bookmarkButton.setText("⋮");bookmarkButton.setContentDescription("Menu tindakan Nota dan Maktabah, termasuk Salin Semua Teks");bookmarkButton.setOnClickListener(v->showReaderActions(key,true));bookmarkButton.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Penanda bacaan").setItems(new String[]{"Simpan penanda","Pergi ke penanda"},(d,which)->{if(which==0){prefs.edit().putInt("bookmark_scroll_"+key,prefs.getInt("reading_scroll_"+key,0)).apply();Toast.makeText(this,"Penanda disimpan",Toast.LENGTH_SHORT).show();}else{int target=prefs.getInt("bookmark_scroll_"+key,-1);if(target>=0&&readerScroll!=null)readerScroll.post(()->readerScroll.scrollTo(0,target));else Toast.makeText(this,"Tiada penanda",Toast.LENGTH_SHORT).show();}}).show();return true;});}

  if(!compactReaderToolbar){
  Button swipeMode=action(prefs.getBoolean("reader_swipe_mode",false)?"⇆ ✓":"⇆",bar,()->{boolean next=!prefs.getBoolean("reader_swipe_mode",false);prefs.edit().putBoolean("reader_swipe_mode",next).apply();read(key);});swipeMode.setContentDescription("Mod swipe halaman untuk skrin terbuka; ulang baris akhir halaman sebelumnya");
  if(availableReaderWidthDp>=500){
   Button verticalScroll=action(prefs.getBoolean("reader_swipe_mode",false)?"↕":"↕ ✓",bar,()->{
    prefs.edit().putBoolean("reader_swipe_mode",false).apply();
    read(key);
   });
   verticalScroll.setContentDescription("Mod skrol menegak untuk Fold6 dibuka: baca dua kolum dengan skrol atas bawah");
   verticalScroll.setTextColor(0xff566a70);
  }
  }
  if(!compactReaderToolbar){
  Button layoutButton=action("▥",bar,()->{boolean now=prefs.getBoolean("reader_two_columns",availableReaderWidthDp>=600);prefs.edit().putBoolean("reader_two_columns",!now).apply();read(key);});layoutButton.setContentDescription("Tukar paparan satu atau dua kolum");layoutButton.setTextColor(0xff566a70); Button readerOptions=action("⋮",bar,()->showReaderActions(key,compactReaderToolbar));readerOptions.setContentDescription(key.startsWith("m_")?"Menu tindakan kitab":"Menu tindakan nota");
  }
  }
  // Page-by-page reader for long Nota and Maktabah content.
  if(prefs.getString(key,"").length()>=100 && availableReaderWidthDp>=500 && prefs.getBoolean("reader_swipe_mode",false)){ // Optional unfolded swipe reader.
   try{showPagedReader(key);return;}
   catch(RuntimeException problem){
    android.util.Log.e("DinNote","Paged reader failed; using safe scrolling",problem);
    // Do not recursively call read(): that can overflow the stack and close the app.
    prefs.edit().putBoolean("reader_safe_scroll_"+key,true).apply();
    cleanBase();
   }
  }
  String heading=prefs.getString("title_"+key,"");
  String readingCategory=prefs.getString("cat_"+key,"Tanpa kategori");
  if(readingCategory.trim().isEmpty())readingCategory="Tanpa kategori";
  String fullReadingText=prefs.getString(key,"");
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
  readerScroll=scroll;
  LinearLayout body=new LinearLayout(this);body.setOrientation(1);
  int paperColor=0xfffffefd;body.setBackground(rounded(paperColor,0xffb8c2ca,12));
  body.setPadding(dp(8),dp(6),dp(8),dp(8));body.setClipChildren(true);body.setClipToPadding(true);
  LinearLayout.LayoutParams readingFrame=new LinearLayout.LayoutParams(-1,-2);
  readingFrame.setMargins(dp(4),dp(8),dp(4),dp(12));scroll.addView(body,readingFrame);
  LinearLayout metaRow=new LinearLayout(this);metaRow.setOrientation(LinearLayout.HORIZONTAL);
  metaRow.setGravity(Gravity.CENTER_VERTICAL);metaRow.setPadding(dp(16),dp(4),dp(16),dp(4));
  TextView sectionLabel=text(key.startsWith("m_")?"▤  Maktabah":"✎  Nota",12);
  sectionLabel.setTextColor(0xff5b7077);sectionLabel.setSingleLine(true);
  sectionLabel.setIncludeFontPadding(false);
  metaRow.addView(sectionLabel,new LinearLayout.LayoutParams(0,-2,1));
  TextView categoryLine=text("⌑  "+readingCategory,12);
  categoryLine.setTextColor(0xff73838a);categoryLine.setSingleLine(true);
  categoryLine.setEllipsize(android.text.TextUtils.TruncateAt.END);
  categoryLine.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
  categoryLine.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
  categoryLine.setIncludeFontPadding(false);
  metaRow.addView(categoryLine,new LinearLayout.LayoutParams(0,-2,1));
  body.addView(metaRow,new LinearLayout.LayoutParams(-1,-2));
  TextView title=text(heading,20);title.setMaxLines(3);
  title.setEllipsize(android.text.TextUtils.TruncateAt.END);
  title.setBackground(rounded(0xfffffefd,0xffb8c2ca,12));
  title.setTextColor(0xff4d6068);title.setTypeface(null,android.graphics.Typeface.BOLD);
  title.setPadding(dp(24),dp(8),dp(24),dp(8));
  title.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);title.setTextIsSelectable(true);
  title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setIncludeFontPadding(true);
  if(!heading.isEmpty())body.addView(title);
  String firstReadingLine=fullReadingText.split("\\R",2)[0].trim();
  if(firstReadingLine.matches("(?i)^(bab|الفصل|باب)\\b.*")&&firstReadingLine.length()<110){
   TextView chapterLabel=text(firstReadingLine,16);
   chapterLabel.setTextColor(0xff566a70);chapterLabel.setTypeface(null,android.graphics.Typeface.BOLD);
   chapterLabel.setPadding(dp(20),dp(8),dp(20),dp(8));body.addView(chapterLabel);
  }
  int fontLevel=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  int[] readingSizes=new int[]{13,15,16,18}; // Match editor for both Nota and Maktabah.
  TextView content=text("",readingSizes[fontLevel]);content.setText(formattedReadingText(prefs.getString(key,"")));boolean containsArabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(fullReadingText).find();if(containsArabic){if(prefs.getBoolean("reader_arabic_clear_font",false))content.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));}
  content.setTextColor(0xff303c43);content.setGravity(Gravity.TOP);content.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);content.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);content.setTextIsSelectable(true);content.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);int readingSide=dp(4); // 12dp frame + 8dp body + 4dp content = 24dp editor inset.
  content.setPadding(readingSide,dp(10),readingSide,dp(12));if(prefs.getBoolean("reader_wide_spacing",false))content.setLineSpacing(dp(5),1.15f);else content.setLineSpacing(dp(1),1.0f);content.setIncludeFontPadding(true);applySharedTextMetrics(content);
  // Display list rows with subtle mint dividers; preserve the original stored text.
  String[] displayLines=fullReadingText.split("\\n",-1);
  boolean hasBoxes=fullReadingText.matches("(?s).*---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---.*");
  boolean hasLists=hasBoxes;
  for(String line:displayLines)if(line.trim().matches("---(?:FLOWER|GEOMETRIC|CLASSIC)?---|---")){hasLists=true;break;}
  for(String line:displayLines)if(line.matches("^\\s*(?:\\d+\\.\\s+|[•☐☑✓]\\s+).*")){hasLists=true;break;}
  // Optional aligned bilingual reading. Each paired line uses Arabic || Malay.
  boolean bilingualReading=key.startsWith("m_")&&availableReaderWidthDp>=500
   &&prefs.getBoolean("reader_bilingual_columns",false)&&fullReadingText.contains(" || ");
  if(bilingualReading){
   LinearLayout bilingual=new LinearLayout(this);bilingual.setOrientation(LinearLayout.VERTICAL);
   for(String line:displayLines){
    int divider=line.indexOf(" || ");
    if(divider<0){TextView plain=text(line,readingSizes[fontLevel]);plain.setTextColor(0xff303c43);bilingual.addView(plain);continue;}
    LinearLayout pair=new LinearLayout(this);pair.setOrientation(LinearLayout.HORIZONTAL);
    TextView arabic=text(line.substring(0,divider),readingSizes[fontLevel]);
    arabic.setTextDirection(View.TEXT_DIRECTION_RTL);arabic.setGravity(Gravity.RIGHT|Gravity.TOP);
    arabic.setTextIsSelectable(true);arabic.setPadding(dp(8),dp(6),dp(8),dp(6));
    TextView malay=text(line.substring(divider+4),readingSizes[fontLevel]);
    malay.setTextDirection(View.TEXT_DIRECTION_LTR);malay.setGravity(Gravity.LEFT|Gravity.TOP);
    malay.setTextIsSelectable(true);malay.setPadding(dp(8),dp(6),dp(8),dp(6));
    pair.addView(arabic,new LinearLayout.LayoutParams(0,-2,1));
    View rule=new View(this);rule.setBackgroundColor(0xffb7c1c9);
    pair.addView(rule,new LinearLayout.LayoutParams(dp(1),-1));
    pair.addView(malay,new LinearLayout.LayoutParams(0,-2,1));
    bilingual.addView(pair);
   }
   body.addView(bilingual);
  }
  boolean twoColumnReading=availableReaderWidthDp>=500&&prefs.getBoolean("reader_two_columns",true)&&fullReadingText.length()>=100;
  if(hasBoxes){
   if(bilingualReading){body.removeViewAt(body.getChildCount()-1);}
   renderBoxReading(body,fullReadingText,readingSizes[fontLevel]);
  }
  else if(bilingualReading){}
  else if(!twoColumnReading&&!hasLists)body.addView(content,new LinearLayout.LayoutParams(-1,-2));
  else if(twoColumnReading){
   // Split at a word boundary for balanced side-by-side reading on unfolded devices.
   int middle=fullReadingText.length()/2;
   int split=fullReadingText.indexOf('\n',middle);
   if(split<0)split=fullReadingText.indexOf(' ',middle);
   if(split<0){int next=middle;while(next<fullReadingText.length()&&!Character.isWhitespace(fullReadingText.charAt(next)))next++;split=next<fullReadingText.length()?next:middle;}
   LinearLayout columns=new LinearLayout(this);columns.setOrientation(LinearLayout.HORIZONTAL);
   columns.setPadding(dp(4),dp(8),dp(4),dp(12));
   // Preserve every original character, including newlines and whitespace at the split.
    android.text.SpannableStringBuilder formatted=formattedReadingText(fullReadingText);
    int visibleMiddle=formatted.length()/2;
    int visibleSplit=formatted.toString().indexOf('\n',visibleMiddle);
    if(visibleSplit<0)visibleSplit=formatted.toString().indexOf(' ',visibleMiddle);
    if(visibleSplit<0)visibleSplit=visibleMiddle;
    if(visibleSplit>0&&visibleSplit<formatted.length()&&Character.isLowSurrogate(formatted.charAt(visibleSplit)))visibleSplit--;
    CharSequence[] halves={formatted.subSequence(0,visibleSplit),formatted.subSequence(visibleSplit,formatted.length())};
   for(int i=0;i<2;i++){
    TextView page=text("",readingSizes[fontLevel]);page.setText(halves[i]);applySharedTextMetrics(page);page.setTextColor(0xff303c43);
    page.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);page.setTextIsSelectable(true);
    page.setGravity(Gravity.TOP);page.setPadding(dp(10),dp(8),dp(10),dp(8));
    page.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
    if(i==1)lp.leftMargin=dp(4);
    columns.addView(page,lp);
    if(i==0){android.widget.ImageView islamicDivider=new android.widget.ImageView(this);islamicDivider.setImageResource(R.drawable.reader_center_ornament);islamicDivider.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);islamicDivider.setPadding(dp(1),dp(8),dp(1),dp(8));columns.addView(islamicDivider,new LinearLayout.LayoutParams(dp(24),-1));}
   }
   body.addView(columns);
  }
  else{
   LinearLayout readingRows=new LinearLayout(this);readingRows.setOrientation(1);
   readingRows.setPadding(readingSide,dp(8),readingSide,dp(12));
   body.addView(readingRows);
   for(int rowIndex=0;rowIndex<displayLines.length;rowIndex++){
    String line=displayLines[rowIndex];
    if(line.trim().isEmpty()){
     int before=rowIndex-1,after=rowIndex+1;
     while(before>=0&&displayLines[before].trim().isEmpty())before--;
     while(after<displayLines.length&&displayLines[after].trim().isEmpty())after++;
     if((before>=0&&displayLines[before].trim().matches("---(?:FLOWER|GEOMETRIC|CLASSIC)?---"))||
        (after<displayLines.length&&displayLines[after].trim().matches("---(?:FLOWER|GEOMETRIC|CLASSIC)?---")))continue;
    }
    java.util.regex.Matcher numbered=java.util.regex.Pattern.compile("^\\s*(\\d+)\\.\\s+(.*)$").matcher(line);
    java.util.regex.Matcher marked=java.util.regex.Pattern.compile("^\\s*([•☐☑✓])\\s+(.*)$").matcher(line);
    boolean isNumber=numbered.matches(),isMarked=!isNumber&&marked.matches();
    if(line.trim().equals("---")||line.trim().equals("---FLOWER---")||line.trim().equals("---GEOMETRIC---")||line.trim().equals("---CLASSIC---")){
     LinearLayout ornament=new LinearLayout(this);ornament.setGravity(Gravity.CENTER_VERTICAL);
     ornament.setOrientation(LinearLayout.HORIZONTAL);ornament.setPadding(dp(12),dp(2),dp(12),dp(2));
     boolean floral=line.trim().equals("---FLOWER---");
     boolean geometric=line.trim().equals("---GEOMETRIC---");
     boolean classic=line.trim().equals("---CLASSIC---");
     int ornamentColor=(floral||geometric||classic)?0xff176b47:0xffc4a45a;
     View leftRule=new View(this);leftRule.setBackgroundColor(ornamentColor);
     ornament.addView(leftRule,new LinearLayout.LayoutParams(0,dp(1),1));
     TextView diamond=text(floral?"✿":geometric?"❖":classic?"❧":"◇",floral?24:19);diamond.setTextColor((floral||geometric||classic)?0xff176b47:0xffae913d);
     diamond.setSingleLine(true);diamond.setMaxLines(1);diamond.setGravity(Gravity.CENTER);diamond.setIncludeFontPadding(false);diamond.setPadding(dp(6),0,dp(6),0);ornament.addView(diamond,new LinearLayout.LayoutParams(dp(42),-2));
     View rightRule=new View(this);rightRule.setBackgroundColor(ornamentColor);
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
     TextView valueView=text("",readingSizes[fontLevel]);valueView.setText(formattedReadingText(value));applySharedTextMetrics(valueView);valueView.setTextColor(0xff303c43);
     valueView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);valueView.setTextIsSelectable(true);
     valueView.setPadding(dp(10),0,dp(4),0);valueView.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);
     itemRow.addView(valueView,new LinearLayout.LayoutParams(0,-2,1));
     readingRows.addView(itemRow);
     View fineDivider=new View(this);fineDivider.setBackgroundColor(0xffc5e7cb);
     readingRows.addView(fineDivider,new LinearLayout.LayoutParams(-1,dp(1)));
    }else{
     TextView paragraph=text("",readingSizes[fontLevel]);paragraph.setText(formattedReadingText(line.isEmpty()?" ":line));applySharedTextMetrics(paragraph);
     paragraph.setTextColor(0xff303c43);paragraph.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
     paragraph.setTextIsSelectable(true);paragraph.setPadding(dp(4),dp(1),dp(4),dp(1));
     paragraph.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);readingRows.addView(paragraph);
    }
   }
  }
  String readingText=prefs.getString(key,"").trim();
  String countable=normalizeStructuralMarkers(readingText).replaceAll("(?m)^---(?:KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)|FLOWER|GEOMETRIC|CLASSIC)---[ \\t]*$","").replaceAll("</?[biu]>","").trim();
  int wordCount=0;java.util.regex.Matcher words=java.util.regex.Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}]*").matcher(countable);while(words.find())wordCount++;
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
   readingProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff566a70));
   readingProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xfff0f3f6));
   FrameLayout.LayoutParams trackParams=new FrameLayout.LayoutParams(-1,dp(3),Gravity.TOP);
   progressFrame.addView(readingProgress,trackParams);
   TextView readingPercent=text("0%",10);
   readingPercent.setTextColor(0xff73838a);
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
   scroll.post(()->{
    int maxScroll=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());
    int savedFraction=prefs.getInt("reading_progress_permille_"+key,-1);
    int target=savedFraction>=0?Math.round(maxScroll*Math.max(0,Math.min(1000,savedFraction))/1000f):prefs.getInt("reading_scroll_"+key,0);
    scroll.scrollTo(0,Math.max(0,Math.min(maxScroll,target)));
    updateReadingProgress.run();
   });
   scroll.setOnTouchListener((v,e)->{
    if(e.getAction()==android.view.MotionEvent.ACTION_UP||e.getAction()==android.view.MotionEvent.ACTION_CANCEL)
     scroll.postDelayed(()->{
      if(selected==null||!selected.equals(key)||scroll.getChildCount()==0)return;
      int maxScroll=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());
      int permille=maxScroll==0?0:Math.max(0,Math.min(1000,Math.round(scroll.getScrollY()*1000f/maxScroll)));
      prefs.edit().putInt("reading_scroll_"+key,scroll.getScrollY()).putInt("reading_progress_permille_"+key,permille).apply();
     },200);
    return false;
   });
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
 void confirmDelete(String key){
  new AlertDialog.Builder(this).setTitle("Alih ke Tong Sampah")
   .setMessage("Nota atau kitab ini boleh dipulihkan semula melalui Tetapan > Tong sampah.")
   .setPositiveButton("Alih",(d,w)->{
    if(!prefs.edit().putBoolean("deleted_"+key,true).putLong("trashed_at_"+key,System.currentTimeMillis()).commit()){
     Toast.makeText(this,"Gagal mengalihkan nota",Toast.LENGTH_LONG).show();return;
    }
    editor=null;titleEditor=null;categoryPicker=null;saveIndicator=null;selected=null;
    if(sectionOpen)notesPage();else home();
   }).setNegativeButton("Batal",null).show();
 }
 void showTrash(){
  ArrayList<String> trash=new ArrayList<>();
  for(String k:prefs.getAll().keySet())
   if((k.startsWith("n_")||k.startsWith("m_"))&&prefs.getAll().get(k) instanceof String&&prefs.getBoolean("deleted_"+k,false))
    trash.add(k);
  Collections.sort(trash,(a,b)->Long.compare(prefs.getLong("trashed_at_"+b,0),prefs.getLong("trashed_at_"+a,0)));
  if(trash.isEmpty()){
   new AlertDialog.Builder(this).setTitle("Tong sampah").setMessage("Tiada nota atau kitab dipadam.")
    .setPositiveButton("Tutup",null).show();return;
  }
  String[] labels=new String[trash.size()];
  for(int i=0;i<trash.size();i++){
   String k=trash.get(i),title=prefs.getString("title_"+k,"").trim();
   labels[i]=(k.startsWith("m_")?"Kitab: ":"Nota: ")+(title.isEmpty()?"Tanpa tajuk":title);
  }
  new AlertDialog.Builder(this).setTitle("Tong sampah").setItems(labels,(dialog,index)->{
   String k=trash.get(index);
   new AlertDialog.Builder(this).setTitle(labels[index])
    .setItems(new String[]{"Pulihkan","Padam kekal"},(d,choice)->{
     if(choice==0){
      if(prefs.edit().remove("deleted_"+k).remove("trashed_at_"+k).commit()){
       Toast.makeText(this,"Nota berjaya dipulihkan",Toast.LENGTH_SHORT).show();
       if(sectionOpen)notesPage();
      }else Toast.makeText(this,"Pemulihan gagal",Toast.LENGTH_LONG).show();
      showTrash();
     }else{
      new AlertDialog.Builder(this).setTitle("Padam kekal?")
       .setMessage("Tindakan ini tidak boleh dibatalkan.")
       .setPositiveButton("Padam kekal",(a,b)->{
        android.content.SharedPreferences.Editor tx=prefs.edit().remove(k).remove("title_"+k)
         .remove("cat_"+k).remove("viewed_"+k).remove("updated_"+k).remove("pinned_"+k)
         .remove("trashed_at_"+k);
        if(!k.contains("_sample_"))tx.remove("deleted_"+k);
        if(tx.commit())Toast.makeText(this,"Dipadam kekal",Toast.LENGTH_SHORT).show();
        else Toast.makeText(this,"Gagal memadam",Toast.LENGTH_LONG).show();
        showTrash();
       }).setNegativeButton("Batal",null).show();
     }
    }).show();
  }).setNegativeButton("Tutup",null).show();
 }
 void finishEditingSafely(){
  if(editor==null)return;
  save();
  // Include the latest history snapshot in the same durable flush as the note.
  if(!saveHistoryVersion(false)){
   if(saveIndicator!=null)saveIndicator.setText("⚠ Sejarah gagal");
   Toast.makeText(this,"Sejarah suntingan gagal disimpan. Editor dikekalkan.",Toast.LENGTH_LONG).show();
   return;
  }
  // Persist queued autosave writes before leaving the editor.
  // If storage fails, retain the editor instead of presenting an unsaved reader.
  if(!prefs.edit().commit()){
   android.util.Log.e("DinNote","Failed to persist note before leaving editor");
   if(saveIndicator!=null)saveIndicator.setText("⚠ Simpan gagal");
   Toast.makeText(this,"Simpanan gagal. Cuba tekan Selesai sekali lagi.",Toast.LENGTH_LONG).show();
   return;
  }
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
   if(isFinishing()||isDestroyed()||editor!=oldEditor)return;
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
  maktabah=key.startsWith("m_");prefs.edit().putString("last_edited_key",key).apply();}editor=null;saveIndicator=null;selected=key;cleanBase();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);Button editorBack=action("←",bar,()->finishEditingSafely());editorBack.setTextSize(23);editorBack.setTextColor(0xff176b47);editorBack.setContentDescription("Kembali dan simpan");saveIndicator=text("✓ Disimpan",11);saveIndicator.setTextColor(0xff566a70);saveIndicator.setTypeface(null,android.graphics.Typeface.BOLD);saveIndicator.setGravity(Gravity.CENTER);saveIndicator.setPadding(dp(9),dp(5),dp(9),dp(5));saveIndicator.setBackground(rounded(0xffe5f5eb,0xffc9e8cf,12));LinearLayout.LayoutParams savedBadgeParams=new LinearLayout.LayoutParams(-2,-2);savedBadgeParams.leftMargin=dp(5);bar.addView(saveIndicator,savedBadgeParams);TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));final java.util.ArrayDeque<EditorSnapshot> undoHistory=new java.util.ArrayDeque<>();final java.util.ArrayDeque<EditorSnapshot> redoHistory=new java.util.ArrayDeque<>();final boolean[] restoring={false};final TextView[] historyButtons=new TextView[2];historyButtons[0]=action("↶",bar,()->{if(editor==null||undoHistory.isEmpty())return;restoring[0]=true;EditorSnapshot current=new EditorSnapshot(editor);EditorSnapshot previous=undoHistory.removeLast();redoHistory.addLast(current);previous.restore(editor);revealEditorCursor(editor);restoring[0]=false;save();historyButtons[0].setAlpha(undoHistory.isEmpty()?0.35f:1f);historyButtons[1].setAlpha(1f);});historyButtons[1]=action("↷",bar,()->{if(editor==null||redoHistory.isEmpty())return;restoring[0]=true;EditorSnapshot current=new EditorSnapshot(editor);EditorSnapshot next=redoHistory.removeLast();undoHistory.addLast(current);next.restore(editor);revealEditorCursor(editor);restoring[0]=false;save();historyButtons[0].setAlpha(1f);historyButtons[1].setAlpha(redoHistory.isEmpty()?0.35f:1f);});historyButtons[0].setContentDescription("Undo perubahan teks");historyButtons[1].setContentDescription("Redo perubahan teks");for(TextView historyButton:historyButtons){historyButton.setTextSize(23);historyButton.setTextColor(0xff146b45);historyButton.setBackground(rounded(0xfff0f5f2,0xffc6d9cf,12));historyButton.setPadding(dp(9),dp(4),dp(9),dp(4));}historyButtons[0].setAlpha(0.45f);historyButtons[1].setAlpha(0.45f);Button doneButton=action("✓ Selesai",bar,()->finishEditingSafely());doneButton.setTextColor(Color.WHITE);doneButton.setTypeface(null,android.graphics.Typeface.BOLD);doneButton.setBackground(rounded(0xff516970,0xffadbcc5,12));doneButton.setContentDescription("Selesai menyunting dan simpan nota");action("⋮",bar,()->{String[] options=new String[]{"Masukkan tarikh","Masukkan masa","Masukkan tarikh dan masa","Simpan versi","Sejarah suntingan","Padam"};new AlertDialog.Builder(this).setItems(options,(d,i)->{if(i<3)insertCurrentDateTime(i);else if(i==3)saveHistoryVersion();else if(i==4)showHistoryVersions();else if(selected!=null)confirmDelete(selected);}).show();});categoryPicker=new Spinner(this);categoryPicker.setContentDescription(maktabah?"Kategori kitab":"Kategori nota");ArrayList<String> choices=new ArrayList<>();choices.add("Tanpa kategori");choices.addAll(categories());categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));String savedCat=key==null?"Tanpa kategori":prefs.getString("cat_"+key,"Tanpa kategori");
  // Preserve an existing note category even if it is missing from the category list.
  if(!choices.contains(savedCat)){
   choices.add(savedCat);
   categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));
  }
  int pos=choices.indexOf(savedCat);categoryPicker.setSelection(pos<0?0:pos);root.addView(categoryPicker);titleEditor=new EditText(this);titleEditor.setSingleLine(true);titleEditor.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);titleEditor.setTextSize(20);titleEditor.setHint(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setContentDescription(maktabah?"Tajuk kitab":"Tajuk nota");titleEditor.setPadding(dp(24),dp(12),dp(24),dp(12));titleEditor.setBackground(rounded(0xfffdfefe,0xffd8c68a,12));titleEditor.setTextColor(0xff154f36);titleEditor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);LinearLayout.LayoutParams editTitleFrame=new LinearLayout.LayoutParams(-1,-2);editTitleFrame.setMargins(dp(4),0,dp(4),0);root.addView(titleEditor,editTitleFrame);if(key!=null)titleEditor.setText(prefs.getString("title_"+key,""));final Spinner metadataCategory=categoryPicker;final EditText metadataTitle=titleEditor;
  final boolean[] metadataExpanded={key==null};
  final Button metadataToggle=action(metadataExpanded[0]?"⌃":"⌄",bar,()->{});
  metadataToggle.setContentDescription(metadataExpanded[0]?"Sembunyikan kategori dan tajuk":"Buka kategori dan tajuk");
  metadataCategory.setVisibility(metadataExpanded[0]?View.VISIBLE:View.GONE);metadataTitle.setVisibility(metadataExpanded[0]?View.VISIBLE:View.GONE);
  metadataToggle.setOnClickListener(v->{
   metadataExpanded[0]=!metadataExpanded[0];
   if(!metadataExpanded[0]&&metadataTitle.hasFocus()&&editor!=null)editor.requestFocus();
   metadataCategory.setVisibility(metadataExpanded[0]?View.VISIBLE:View.GONE);
   metadataTitle.setVisibility(metadataExpanded[0]?View.VISIBLE:View.GONE);
   metadataToggle.setText(metadataExpanded[0]?"⌃":"⌄");
   metadataToggle.setContentDescription(metadataExpanded[0]?"Sembunyikan kategori dan tajuk":"Buka kategori dan tajuk");
  });final Runnable[] formatRefresh={null};editor=new EditText(this){
   @Override public boolean onTextContextMenuItem(int id){
    if(id==android.R.id.copy&&getSelectionStart()>=0&&getSelectionEnd()>=0&&getSelectionStart()!=getSelectionEnd()){
     int a=Math.min(getSelectionStart(),getSelectionEnd()),b=Math.max(getSelectionStart(),getSelectionEnd());
     String selected=getText().subSequence(a,b).toString();
     String clean=selected.replaceAll("(?m)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---[ \\t]*(?:\\r?\\n)?","");
     android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
     if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DinNote",clean));return true;}
    }
    return super.onTextContextMenuItem(id);
   }

   @Override protected void onDraw(android.graphics.Canvas canvas){
    super.onDraw(canvas);
    if(!hasFocus()||!isCursorVisible()||getSelectionStart()<0||getSelectionStart()!=getSelectionEnd())return;
    android.text.Layout layout=getLayout();if(layout==null)return;
    int offset=Math.min(getSelectionStart(),length());
    int line=layout.getLineForOffset(offset);
    float x=getCompoundPaddingLeft()+layout.getPrimaryHorizontal(offset)-getScrollX();
    float y=getExtendedPaddingTop()+layout.getLineTop(line)-getScrollY();
    float bottom=getExtendedPaddingTop()+layout.getLineBottom(line)-getScrollY();
    float left=getCompoundPaddingLeft()+dp(2),right=getWidth()-getCompoundPaddingRight()-dp(2);
    if(right<=left||bottom<0||y>getHeight())return;
    x=Math.max(left,Math.min(right,x));
    y=Math.max(getExtendedPaddingTop(),y);bottom=Math.min(getHeight()-getExtendedPaddingBottom(),bottom);
    if(bottom-y<dp(4))return;
    long phase=android.os.SystemClock.uptimeMillis()%1000;
    if(phase<550){android.graphics.Paint caret=new android.graphics.Paint(3);caret.setColor(0xff176b47);caret.setStrokeWidth(dp(2));canvas.drawLine(x,y+dp(1),x,bottom-dp(1),caret);}
    postInvalidateDelayed(phase<550?550-phase:1000-phase);
   }
   @Override protected void onSelectionChanged(int start,int end){super.onSelectionChanged(start,end);if(editor==this&&start>=0&&end>=0){persistEditorPosition();if(hasFocus())post(()->revealEditorCursor(this));if(formatRefresh[0]!=null)post(formatRefresh[0]);}}
   @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(editor==this)post(()->revealEditorCursor(this));}
  };final int[] boxTouchOffset={-1};
  editor.setOnTouchListener((v,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){boxTouchOffset[0]=((EditText)v).getOffsetForPosition(event.getX(),event.getY());}return false;});
  editor.setOnLongClickListener(v->{
   int offset=boxTouchOffset[0];if(offset<0||editor!=v)return false;
   java.util.regex.Matcher headers=java.util.regex.Pattern.compile("(?m)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---$").matcher(editor.getText());
   while(headers.find()){if(offset>=headers.start()&&offset<=headers.end()){editor.setSelection(headers.end());return manageCurrentBox();}}
   return false;
  });
  editor.setGravity(Gravity.TOP);editor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);editor.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);editor.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);editor.setTextSize(new int[]{13,15,16,18}[Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)))]);editor.setTextColor(0xff30343b);editor.setCursorVisible(true);
  if(android.os.Build.VERSION.SDK_INT>=29){
   android.graphics.drawable.GradientDrawable caretDrawable=new android.graphics.drawable.GradientDrawable();
   caretDrawable.setColor(0xff176b47);
   caretDrawable.setSize(dp(2),dp(24));
   editor.setTextCursorDrawable(caretDrawable);
  }editor.setPadding(dp(24),dp(10),dp(24),dp(12));editor.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);editor.setIncludeFontPadding(true);applySharedTextMetrics(editor);editor.setHint(maktabah?"Tulis kandungan kitab di sini…":"Tulis nota di sini…");editor.setContentDescription(maktabah?"Kandungan kitab":"Kandungan nota");editor.setBackground(rounded(0xfffffefb,0xffb8d5c2,12));editor.setOnFocusChangeListener((v,focused)->editor.setBackground(rounded(0xfffffefb,focused?0xff168254:0xffb8d5c2,12)));editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);LinearLayout formatBar=new LinearLayout(this);
  formatBar.setGravity(Gravity.CENTER_VERTICAL);
  formatBar.setPadding(dp(5),dp(2),dp(5),dp(2));
  formatBar.setHorizontalScrollBarEnabled(false);
  formatBar.setBackground(rounded(0xffeff8f1,0xffadbcc5,12));
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
  Button dividerButton=action("✿",formatBar,()->showDividerPicker());
  dividerButton.setTextColor(0xff176b47);dividerButton.setContentDescription("Pilih pemisah perenggan Islamik");
  Button boxButton=action("▣",formatBar,()->showTextBoxPicker());
  boxButton.setOnLongClickListener(v->{if(!manageCurrentBox(true))showTextBoxPicker();return true;});boxButton.setTextColor(0xff176b47);boxButton.setContentDescription("Kotak Dalil, Kalam Hikmah dan Catatan Penting");
  Button fontButton=action("Aa",formatBar,()->new AlertDialog.Builder(this).setTitle("Saiz tulisan editor").setItems(new String[]{"Kecil","Biasa","Besar","Sangat besar"},(d,i)->{prefs.edit().putInt("reader_font_level",i).putInt("editor_font_level",i).apply();if(editor!=null)applySharedTextMetrics(editor);}).show());
  fontButton.setContentDescription("Saiz tulisan editor");
  // Uniform compact emerald buttons; formatting is highlighted while the caret is inside markup.
  // One shared editor toolbar for Nota and Maktabah: identical actions, order and sizing.
  Button[] formatButtons={boldButton,italicButton,bulletButton,numberedButton,checklistButton,underlineButton,dividerButton,fontButton};
  for(Button button:formatButtons){
   button.setTextColor(0xff566a70);
   button.setBackground(rounded(0xfffbfdfb,0xffd2dbe2,10));
  }
  final Runnable refreshFormatting=()->{
   if(editor==null)return;
   int a=Math.min(editor.getSelectionStart(),editor.getSelectionEnd()),b=Math.max(editor.getSelectionStart(),editor.getSelectionEnd());
   int mask=a<0?0:richMask(editor.getText(),a,b);
   Button[] buttons={boldButton,italicButton,underlineButton};
   for(int i=0;i<buttons.length;i++){
    boolean active=(mask&(1<<i))!=0;
    buttons[i].setBackground(rounded(active?0xff176b47:0xfffbfdfb,active?0xffb8d5c2:0xffd2dbe2,10));
    buttons[i].setTextColor(active?Color.WHITE:0xff566a70);
    buttons[i].setSelected(active);
   }
  };
  formatRefresh[0]=refreshFormatting;
  boldButton.setOnClickListener(v->{applyLiveStyle(1);refreshFormatting.run();});
  italicButton.setOnClickListener(v->{applyLiveStyle(2);refreshFormatting.run();});
  underlineButton.setOnClickListener(v->{applyLiveStyle(4);refreshFormatting.run();});
  editor.setOnClickListener(v->refreshFormatting.run());
  Button toolsButton=action("⌕",formatBar,()->new AlertDialog.Builder(this).setTitle("Alat penyuntingan")
   .setItems(new String[]{"Cari dan ganti","Buang format terpilih","Preset format"},(d,i)->{
    if(i==0)showEditorFindReplace();else if(i==1){clearSelectedFormatting();refreshFormatting.run();}else showEditorFormatPresets();
   }).show());
  toolsButton.setContentDescription("Alat penyuntingan tambahan");

  LinearLayout.LayoutParams editBodyFrame=new LinearLayout.LayoutParams(-1,0,1);editBodyFrame.setMargins(dp(4),0,dp(4),0);root.addView(editor,editBodyFrame);
  // Keep normal Android double-tap text selection while editing.
  // Never navigate away while EditText is handling touch/selection events.
  editor.setOnTouchListener(null);
  if(key!=null){int remembered=prefs.getInt("edit_cursor_"+key,0);editor.setText(formattedReadingText(repairBoxStructure(normalizeStructuralMarkers(prefs.getString(key,"")))));editor.setSelection(Math.max(0,Math.min(remembered,editor.length())));}
  final EditText activeBody=editor;
  activeBody.post(()->revealEditorCursor(activeBody));
  // Match the reader's optional Arabic serif typeface in the editor as well.
  // Without this, Arabic glyph widths and line wrapping differ after Selesai.
  if(prefs.getBoolean("reader_arabic_clear_font",false)&&
     java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(editor.getText()).find())
   editor.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));
  TextWatcher titleWatcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save();}public void afterTextChanged(Editable e){}};titleEditor.addTextChangedListener(titleWatcher);TextWatcher bodyWatcher=new TextWatcher(){String before="";EditorSnapshot snapshot;public void beforeTextChanged(CharSequence s,int st,int count,int after){if(!restoring[0]){before=s.toString();snapshot=new EditorSnapshot(editor);}}public void onTextChanged(CharSequence s,int st,int beforeCount,int count){}public void afterTextChanged(Editable e){if(!restoring[0]&&!before.equals(e.toString())){
   int cursor=editor.getSelectionStart();
   if(cursor>0&&cursor<=e.length()&&e.charAt(cursor-1)=='\n'){
    String prior=e.subSequence(0,cursor-1).toString();
    int start=prior.lastIndexOf('\n')+1;
    String previousLine=prior.substring(start);
    java.util.regex.Matcher numbered=java.util.regex.Pattern.compile("^\\s*(\\d+)\\.\\s*(.*)$").matcher(previousLine);
    if(numbered.matches()){
     int next=1;try{int current=Integer.parseInt(numbered.group(1));next=current<Integer.MAX_VALUE?current+1:1;}catch(NumberFormatException ignored){}
     restoring[0]=true;
     if(numbered.group(2).trim().isEmpty()){e.delete(start,cursor);editor.setSelection(start);}
     else{String prefix=next+". ";e.insert(cursor,prefix);editor.setSelection(cursor+prefix.length());}
     restoring[0]=false;
    }
   }
   if(snapshot!=null)undoHistory.addLast(snapshot);if(undoHistory.size()>100)undoHistory.removeFirst();redoHistory.clear();historyButtons[0].setAlpha(1f);historyButtons[1].setAlpha(0.35f);}
   // Recheck Arabic when text is typed or pasted into an initially Rumi-only note.
   // Typeface updates do not modify Editable content or its autosave state.
   if(editor!=null&&prefs.getBoolean("reader_arabic_clear_font",false)){
    boolean arabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(e).find();
    android.graphics.Typeface desired=android.graphics.Typeface.create(arabic?"serif":"sans-serif",android.graphics.Typeface.NORMAL);
    if(!desired.equals(editor.getTypeface()))editor.setTypeface(desired);
   }
   if(!restoring[0]&&repairTypedBoxMarkers(e,restoring))return;
   refreshEditorBoxes(e);save();final EditText active=editor;if(active!=null)active.post(()->revealEditorCursor(active));}};editor.addTextChangedListener(bodyWatcher);refreshEditorBoxes(editor.getText());editor.addOnLayoutChangeListener((v,l,t,rr,bb,ol,ot,orr,ob)->{if(rr-l!=orr-ol&&v==editor)refreshEditorBoxes(editor.getText());});categoryPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){save();}});}

 
 void drawSacredBoxIcon(android.graphics.Canvas canvas,boolean book,int ink){
      android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(ink);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setStrokeJoin(android.graphics.Paint.Join.ROUND);
      android.graphics.Path trace=new android.graphics.Path();
      if(book){
       trace.moveTo(16,8);trace.quadTo(9,3,2,6);trace.lineTo(2,25);trace.quadTo(9,22,16,27);trace.quadTo(23,22,30,25);trace.lineTo(30,6);trace.quadTo(23,3,16,8);trace.lineTo(16,27);
       for(int y=10;y<=18;y+=4){trace.moveTo(5,y);trace.quadTo(10,y-1,13,y+2);trace.moveTo(19,y+2);trace.quadTo(23,y-1,27,y);}
      }else{
       trace.moveTo(7,4);trace.lineTo(25,4);trace.quadTo(29,4,29,8);trace.lineTo(29,21);trace.quadTo(29,25,25,25);trace.lineTo(13,25);trace.lineTo(13,29);trace.quadTo(9,28,7,25);trace.quadTo(3,25,3,21);trace.lineTo(3,8);trace.quadTo(3,4,7,4);trace.close();
       trace.moveTo(8,11);trace.lineTo(24,11);trace.moveTo(8,15);trace.lineTo(24,15);trace.moveTo(8,19);trace.lineTo(19,19);
      }
      canvas.drawPath(trace,p);
 }
 class BoxMarkerSpan extends android.text.style.ReplacementSpan {
  final String title;final int ink,bg;
  BoxMarkerSpan(String t,int i,int b){title=t;ink=i;bg=b;}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){
   if(fm!=null){int h=dp(title.isEmpty()?5:22);fm.ascent=-h;fm.top=-h;fm.descent=dp(2);fm.bottom=fm.descent;}
   android.graphics.Paint q=new android.graphics.Paint(p);q.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);q.setTextSize(dp(13));
   return title.isEmpty()?1:Math.round(q.measureText(title))+dp(38);
  }
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){
   if(title.isEmpty())return;
   android.graphics.Paint q=new android.graphics.Paint(p);q.setAntiAlias(true);q.setColor(ink);q.setAlpha(22);c.drawRoundRect(x+dp(3),top+dp(2),x+getSize(p,t,a,b,null),bottom-dp(1),dp(12),dp(12),q);q.setAlpha(45);c.drawCircle(x+dp(13),(top+bottom)/2f,dp(10),q);
   q.setAlpha(255);q.setColor(ink);q.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);q.setTextSize(dp(13));
   String mark=title.startsWith("Firman")?"":title.startsWith("Sabda")?"":title.startsWith("Kalam")?"✒":title.startsWith("Dalil")?"⚖":"◎";
   q.setTextAlign(android.graphics.Paint.Align.CENTER);c.drawText(mark,x+dp(13),(top+bottom)/2f-(q.ascent()+q.descent())/2,q);
   if(title.startsWith("Firman")||title.startsWith("Sabda")){boolean book=title.startsWith("Firman");float size=dp(book?24:19);c.save();c.translate(x+dp(13)-size/2f,(top+bottom-size)/2f);c.scale(size/32f,size/32f);drawSacredBoxIcon(c,book,ink);c.restore();}
   q.setTextAlign(android.graphics.Paint.Align.LEFT);c.drawText(title,x+dp(28),(top+bottom)/2f-(q.ascent()+q.descent())/2,q);
  }
 }
 class BoxBodySpan implements android.text.style.LineBackgroundSpan {
  final int bg,ink,first,last;BoxBodySpan(int b,int i,int a,int z){bg=b;ink=i;first=a;last=z;}
  @Override public void drawBackground(android.graphics.Canvas c,android.graphics.Paint p,int left,int right,int top,int baseline,int bottom,CharSequence t,int start,int end,int line){
   boolean head=start<=first,tail=end>=last;
   if(head)top+=dp(2);if(tail)bottom-=dp(2);if(bottom<=top)return;
   float l=left+dp(1),r=right-dp(1),rad=dp(8);
   float[] corners={head?rad:0,head?rad:0,head?rad:0,head?rad:0,tail?rad:0,tail?rad:0,tail?rad:0,tail?rad:0};
   android.graphics.Paint q=new android.graphics.Paint(p);q.setAntiAlias(true);q.setStyle(android.graphics.Paint.Style.FILL);q.setColor(bg);
   android.graphics.Path shape=new android.graphics.Path();shape.addRoundRect(new android.graphics.RectF(l,top,r,bottom),corners,android.graphics.Path.Direction.CW);c.drawPath(shape,q);
   q.setColor(ink);q.setAlpha(135);q.setStyle(android.graphics.Paint.Style.STROKE);q.setStrokeWidth(dp(1));
   android.graphics.Path edge=new android.graphics.Path();
   edge.moveTo(l,tail?bottom-rad:bottom);edge.lineTo(l,head?top+rad:top);
   if(head){edge.quadTo(l,top,l+rad,top);edge.lineTo(r-rad,top);edge.quadTo(r,top,r,top+rad);}
   else edge.moveTo(r,top);
   edge.lineTo(r,tail?bottom-rad:bottom);
   if(tail){edge.quadTo(r,bottom,r-rad,bottom);edge.lineTo(l+rad,bottom);edge.quadTo(l,bottom,l,bottom-rad);}
   c.drawPath(edge,q);
   if(head){q.setAlpha(65);for(int j=0;j<3;j++){float d=dp(6+j*5);android.graphics.Path motif=new android.graphics.Path();motif.moveTo(r-d-dp(5),top+dp(2));motif.lineTo(r-d,top+dp(5));motif.lineTo(r-d,top+dp(8));motif.lineTo(r-dp(5),top+d);motif.lineTo(r-dp(2),top+d+dp(5));c.drawPath(motif,q);}}
  }
 }
 class BoxInsetSpan extends android.text.style.LeadingMarginSpan.Standard {
  BoxInsetSpan(){super(dp(9));}
 }
 class EditorDividerSpan extends android.text.style.ReplacementSpan {
  final String symbol;EditorDividerSpan(String kind){symbol=kind.equals("FLOWER")?"✿":kind.equals("GEOMETRIC")?"❖":"❧";}
  int width(){return editor==null?dp(120):Math.max(dp(24),editor.getWidth()-editor.getCompoundPaddingLeft()-editor.getCompoundPaddingRight()-dp(22));}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){
   if(fm!=null){fm.ascent=-dp(15);fm.top=fm.ascent;fm.descent=dp(5);fm.bottom=fm.descent;}return width();
  }
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){
   android.graphics.Paint q=new android.graphics.Paint(p);q.setAntiAlias(true);q.setColor(0xff176b47);q.setStrokeWidth(dp(1));float mid=x+width()/2f,cy=(top+bottom)/2f;
   q.setTypeface(android.graphics.Typeface.DEFAULT);q.setTextSize(dp(19));q.setTextAlign(android.graphics.Paint.Align.CENTER);float gap=q.measureText(symbol)/2+dp(8);
   if(mid-gap>x)c.drawLine(x,cy,mid-gap,cy,q);if(mid+gap<x+width())c.drawLine(mid+gap,cy,x+width(),cy,q);
   c.drawText(symbol,mid,cy-(q.ascent()+q.descent())/2,q);
  }
 }
 // Keep user text out of structural marker lines: a broken marker would expose raw tags.
 boolean repairTypedBoxMarkers(android.text.Editable e,boolean[] restoring){
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?m)^(---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---)([^\\r\\n]+)").matcher(e.toString());
  if(!m.find())return false;
  int oldCursor=editor.getSelectionStart();
  int insertAt=m.start(2);
  String suffix=m.group(2);
  restoring[0]=true;
  try{
   e.replace(insertAt,m.end(2),"\n"+suffix);
   int newCursor=oldCursor>=insertAt?oldCursor+1:oldCursor;
   editor.setSelection(Math.max(0,Math.min(newCursor,e.length())));
  }finally{restoring[0]=false;}
  refreshEditorBoxes(e);save();
  editor.post(()->revealEditorCursor(editor));
  return true;
 }
 void refreshEditorBoxes(android.text.Editable e){
  for(EditorDividerSpan span:e.getSpans(0,e.length(),EditorDividerSpan.class))e.removeSpan(span);
  java.util.regex.Matcher div=java.util.regex.Pattern.compile("(?m)^---(FLOWER|GEOMETRIC|CLASSIC)---$").matcher(e.toString());
  while(div.find())e.setSpan(new EditorDividerSpan(div.group(1)),div.start(),div.end(),33);

  for(BoxMarkerSpan span:e.getSpans(0,e.length(),BoxMarkerSpan.class))e.removeSpan(span);
  for(BoxBodySpan span:e.getSpans(0,e.length(),BoxBodySpan.class))e.removeSpan(span);
  for(BoxInsetSpan span:e.getSpans(0,e.length(),BoxInsetSpan.class))e.removeSpan(span);
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?m)^---KOTAK(DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---$").matcher(e.toString());
  int body=-1,bg=0,ink=0,first=-1;
  while(m.find()){
   String kind=m.group(1);
   if(kind.equals("TAMAT")){
    if(first>=0){e.setSpan(new BoxBodySpan(bg,ink,first,m.end()),first,m.end(),33);if(body<m.start())e.setSpan(new BoxInsetSpan(),body,m.start(),33);}
    e.setSpan(new BoxMarkerSpan("",ink,bg),m.start(),m.end(),33);body=-1;first=-1;continue;
   }
   boolean gold=kind.equals("SABDA"),gray=kind.equals("CATATAN"),wisdom=kind.equals("HIKMAH"),dalil=kind.equals("DALIL"),sahabat=kind.equals("SAHABAT"),tabii=kind.equals("TABII"),ulama=kind.equals("ULAMA");
   bg=sahabat?0xfff1f6ec:tabii?0xfff1f4fa:ulama?0xfffaf1ed:gold?0xfffaf5e8:gray?0xfffcf0f2:wisdom?0xfff5eff9:dalil?0xffedf4fa:0xffedf5ee;
   ink=sahabat?0xff607d42:tabii?0xff536f9b:ulama?0xff9b654b:gold?0xff96783e:gray?0xff946176:wisdom?0xff79549b:dalil?0xff426e96:0xff176b47;
   String title=kind.equals("FIRMAN")?"Firman Allah SWT:":kind.equals("SABDA")?"Sabda Nabi SAW:":dalil?"Dalil":wisdom?"Kalam Hikmah":sahabat?"Kata Sahabat Rdh.":tabii?"Kata Tabi\u0027in Rhm.":ulama?"Kata Ulama\u0027 Rhm.":"Catatan Penting";
   first=m.start();e.setSpan(new BoxMarkerSpan(title,ink,bg),m.start(),m.end(),33);body=Math.min(e.length(),m.end()+1);
  }
 }
 void clearSelectedFormatting(){
  if(editor==null)return;
  int a=Math.min(editor.getSelectionStart(),editor.getSelectionEnd()),b=Math.max(editor.getSelectionStart(),editor.getSelectionEnd());
  if(a<0||a==b){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}
  android.text.Editable e=editor.getText();
  for(android.text.style.CharacterStyle span:e.getSpans(a,b,android.text.style.CharacterStyle.class)){
   if(!(span instanceof android.text.style.StyleSpan)&&!(span instanceof android.text.style.UnderlineSpan))continue;
   int x=e.getSpanStart(span),y=e.getSpanEnd(span);
   if(x>=b||y<=a)continue;
   e.removeSpan(span);
   if(x<a)e.setSpan(copyLiveStyle(span),x,a,33);
   if(y>b)e.setSpan(copyLiveStyle(span),b,y,33);
  }
  save();
 }
 android.text.style.CharacterStyle copyLiveStyle(android.text.style.CharacterStyle span){
  return span instanceof android.text.style.UnderlineSpan?new android.text.style.UnderlineSpan():new android.text.style.StyleSpan(((android.text.style.StyleSpan)span).getStyle());
 }
 org.json.JSONArray personalFormatPresets(){
  try{return new org.json.JSONArray(prefs.getString("personal_format_presets","[]"));}catch(Exception e){return new org.json.JSONArray();}
 }
 void showEditorFormatPresets(){
  if(editor==null)return;
  final String[] names={"Tajuk kitab","Dalil al-Quran","Kalam ulama","Fasal dan subtopik","Catatan penting","Tebal","Condong","Garis bawah"};
  final org.json.JSONArray own=personalFormatPresets();
  java.util.ArrayList<String> labels=new java.util.ArrayList<>(java.util.Arrays.asList(names));
  for(int i=0;i<own.length();i++){org.json.JSONObject p=own.optJSONObject(i);labels.add(p==null?"Preset":p.optString("name","Preset"));}
  labels.add("+ Cipta preset sendiri");labels.add("Urus preset sendiri");
  new AlertDialog.Builder(this).setTitle("Preset Format 2.0").setItems(labels.toArray(new String[0]),(d,i)->{
   if(i==labels.size()-2){editPersonalFormatPreset(-1);return;}
   if(i==labels.size()-1){managePersonalFormatPresets();return;}
   if(i<names.length){previewFormatPreset(names[i],i);return;}
   org.json.JSONObject p=own.optJSONObject(i-names.length);if(p!=null)previewFormatPreset(p.optString("name"),p.optInt("style",0));
  }).show();
 }
 void previewFormatPreset(String name,int style){
  final int kind=Math.max(0,Math.min(7,style));
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(20),dp(12),dp(20),dp(12));
  String[] examples={"كِتَابُ الطَّهَارَةِ","إِنَّ اللَّهَ مَعَ الصَّابِرِينَ","الْعِلْمُ نُورٌ","فَصْلٌ — Fasal Pertama","Perkara ini perlu difahami","Contoh tulisan tebal","Contoh tulisan condong","Contoh garis bawah"};
  int start=Math.min(editor.getSelectionStart(),editor.getSelectionEnd()),end=Math.max(editor.getSelectionStart(),editor.getSelectionEnd());
  String chosen=start>=0&&end>start?editor.getText().subSequence(start,end).toString():examples[kind];
  TextView sample=text(chosen,kind<=2?20:16);
  sample.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
  sample.setTextColor(kind==1?0xff927337:0xff176b47);sample.setGravity(kind<=2?Gravity.CENTER:Gravity.START);
  sample.setTypeface(null,kind==6?android.graphics.Typeface.ITALIC:kind==7?android.graphics.Typeface.NORMAL:android.graphics.Typeface.BOLD);
  if(kind==7)sample.setPaintFlags(sample.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);
  sample.setPadding(dp(12),dp(12),dp(12),dp(12));
  sample.setBackground(rounded(kind==1||kind==4?0xfffff9e9:0xffeff6f0,kind==1||kind==4?0xffd8c68a:0xffb8cdbd,10));panel.addView(sample);
  if(kind==3){TextView line=text("────── ✿ ──────",14);line.setGravity(Gravity.CENTER);line.setTextColor(0xff927337);panel.addView(line);}
  TextView info=text("Pratonton gaya • "+(start>=0&&end>start?"Teks terpilih":"Contoh tulisan"),12);info.setPadding(0,dp(8),0,0);panel.addView(info);
  new AlertDialog.Builder(this).setTitle(name).setView(panel).setPositiveButton("Gunakan",(d,w)->applyFormatPreset(kind)).setNegativeButton("Batal",null).show();
 }
 void applyFormatPreset(int kind){
  if(editor==null)return;
  if(kind==1||kind==2||kind==4){
   String type=kind==1?"DALIL":kind==2?"HIKMAH":"CATATAN";
   wrapEditorSelection("\n---KOTAK"+type+"---\n","\n---KOTAKTAMAT---\n");
  }else if(kind==3)wrapEditorSelection("**","**\n---FLOWER---\n");
  else if(kind==6)wrapEditorSelection("*","*");
  else if(kind==7)wrapEditorSelection("<u>","</u>");
  else wrapEditorSelection("**","**");
 }
 void editPersonalFormatPreset(int index){
  org.json.JSONArray items=personalFormatPresets();org.json.JSONObject old=index<0?null:items.optJSONObject(index);
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(20),dp(10),dp(20),dp(10));
  EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Nama preset");if(old!=null)name.setText(old.optString("name"));fields.addView(name);
  Spinner styles=new Spinner(this);styles.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Tajuk kitab","Dalil al-Quran","Kalam ulama","Fasal dan subtopik","Catatan penting","Tebal","Condong","Garis bawah"}));if(old!=null)styles.setSelection(Math.max(0,Math.min(7,old.optInt("style"))));fields.addView(styles);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(index<0?"Cipta preset":"Edit preset").setView(fields).setPositiveButton("Simpan",null).setNegativeButton("Batal",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   String label=name.getText().toString().trim();if(label.isEmpty()){name.setError("Masukkan nama");return;}
   try{org.json.JSONObject p=new org.json.JSONObject();p.put("name",label);p.put("style",styles.getSelectedItemPosition());if(index<0)items.put(p);else items.put(index,p);prefs.edit().putString("personal_format_presets",items.toString()).apply();dialog.dismiss();showEditorFormatPresets();}catch(Exception e){Toast.makeText(this,"Preset tidak dapat disimpan",Toast.LENGTH_SHORT).show();}
  }));dialog.show();
 }
 void managePersonalFormatPresets(){
  org.json.JSONArray items=personalFormatPresets();if(items.length()==0){Toast.makeText(this,"Belum ada preset sendiri",Toast.LENGTH_SHORT).show();return;}
  String[] names=new String[items.length()];for(int i=0;i<names.length;i++){org.json.JSONObject p=items.optJSONObject(i);names[i]=p==null?"Preset":p.optString("name");}
  new AlertDialog.Builder(this).setTitle("Urus preset").setItems(names,(d,i)->new AlertDialog.Builder(this).setTitle(names[i]).setItems(new String[]{"Edit nama dan gaya","Padam"},(x,j)->{
   if(j==0)editPersonalFormatPreset(i);else new AlertDialog.Builder(this).setMessage("Padam preset ini?").setNegativeButton("Batal",null).setPositiveButton("Padam",(y,z)->{items.remove(i);prefs.edit().putString("personal_format_presets",items.toString()).apply();}).show();
  }).show()).show();
 }
 void showEditorFindReplace(){
  if(editor==null)return;
  final EditText active=editor;
  LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(20),dp(8),dp(20),dp(8));
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Cari perkataan");fields.addView(query);
  EditText replacement=new EditText(this);replacement.setSingleLine(true);replacement.setHint("Ganti dengan");fields.addView(replacement);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Cari dan ganti").setView(fields)
   .setPositiveButton("Ganti semua",null).setNeutralButton("Cari seterusnya",null).setNegativeButton("Tutup",null).create();
  dialog.setOnShowListener(v->{
   dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(w->{
    if(editor!=active)return;String q=query.getText().toString();if(q.isEmpty()){query.setError("Masukkan carian");return;}
    String content=active.getText().toString();int at=content.indexOf(q,Math.max(0,active.getSelectionEnd()));if(at<0)at=content.indexOf(q);
    if(at<0){query.setError("Tidak dijumpai");return;}active.setSelection(at,at+q.length());dialog.dismiss();active.requestFocus();revealEditorCursor(active);
   });
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
    if(editor!=active)return;String q=query.getText().toString();if(q.isEmpty()){query.setError("Masukkan carian");return;}
    String content=active.getText().toString();if(!content.contains(q)){query.setError("Tidak dijumpai");return;}
    String result=content.replace(q,replacement.getText().toString());
    active.getText().replace(0,active.length(),result);dialog.dismiss();
   });
  });dialog.show();
 }
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
 String normalizeStructuralMarkers(String source){
  String[] lines=source.split("\\n",-1);
  for(int i=0;i<lines.length;i++){
   String plain=lines[i].replaceAll("</?[biu]>|\\*","").trim();
   if(plain.matches("---(?:KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)|FLOWER|GEOMETRIC|CLASSIC)---"))lines[i]=plain;
  }
  return android.text.TextUtils.join("\n",lines);
 }
 String repairBoxStructure(String source){
  StringBuilder out=new StringBuilder();boolean open=false;
  for(String line:source.split("\\n",-1)){
   if(line.matches("---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---")){
    if(open)out.append("---KOTAKTAMAT---\n");open=true;
   }else if(line.equals("---KOTAKTAMAT---")){
    if(!open)continue;open=false;
   }
   out.append(line).append('\n');
  }
  if(open)out.append("---KOTAKTAMAT---\n");
  if(out.length()>0)out.setLength(out.length()-1);
  return out.toString();
 }
 void renderBoxReading(LinearLayout body,String source,int fontSize){
  source=repairBoxStructure(normalizeStructuralMarkers(source));
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?ms)^---KOTAK(DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---[ \\t]*\\n(.*?)^---KOTAKTAMAT---[ \\t]*(?:\\n|$)").matcher(source);
  int previous=0;
  while(m.find()){
   addBoxParagraph(body,source.substring(previous,m.start()),fontSize);
   String type=m.group(1);
   LinearLayout panel=new LinearLayout(this){
    @Override protected void dispatchDraw(android.graphics.Canvas c){
     super.dispatchDraw(c);
     android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(0xff789184);p.setAlpha(65);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(1));
     float right=getWidth()-dp(4),top=dp(4);
     for(int n=0;n<3;n++){float d=dp(7+n*5);android.graphics.Path path=new android.graphics.Path();path.moveTo(right-d-dp(6),top);path.lineTo(right-d,top+dp(3));path.lineTo(right-d,top+dp(6));path.lineTo(right-dp(3),top+d);path.lineTo(right,top+d+dp(5));c.drawPath(path,p);}
    }
   };panel.setOrientation(LinearLayout.VERTICAL);
   int bg=type.equals("FIRMAN")?0xffedf5ee:type.equals("SABDA")?0xfffaf5e8:type.equals("DALIL")?0xffedf4fa:type.equals("HIKMAH")?0xfff5eff9:type.equals("SAHABAT")?0xfff1f6ec:type.equals("TABII")?0xfff1f4fa:type.equals("ULAMA")?0xfffaf1ed:0xfffcf0f2;
   int border=type.equals("FIRMAN")?0xffa5c3aa:type.equals("SABDA")?0xffd6c7a4:type.equals("DALIL")?0xff8eb4d2:type.equals("HIKMAH")?0xffbca5d0:type.equals("SAHABAT")?0xffb6c9a3:type.equals("TABII")?0xffb0bfd9:type.equals("ULAMA")?0xffd6b7a5:0xffd7a0b2;
   panel.setBackground(rounded(bg,border,8));panel.setPadding(dp(10),dp(3),dp(10),dp(5));
   TextView label=text(type.equals("FIRMAN")?"Firman Allah SWT:":type.equals("SABDA")?"Sabda Nabi SAW:":type.equals("DALIL")?"⚖  Dalil":type.equals("HIKMAH")?"✒  Kalam Hikmah":type.equals("SAHABAT")?"✦  Kata Sahabat Rdh.":type.equals("TABII")?"❖  Kata Tabi\u0027in Rhm.":type.equals("ULAMA")?"✧  Kata Ulama\u0027 Rhm.":"◎  Catatan Penting",13);
   label.setTextColor(type.equals("FIRMAN")?0xff176b47:type.equals("SABDA")?0xff96783e:type.equals("DALIL")?0xff426e96:type.equals("HIKMAH")?0xff79549b:type.equals("SAHABAT")?0xff607d42:type.equals("TABII")?0xff536f9b:type.equals("ULAMA")?0xff9b654b:0xff946176);
   label.setTypeface(null,android.graphics.Typeface.BOLD);label.setIncludeFontPadding(false);
   label.setPadding(dp(6),dp(2),dp(8),dp(2));
   label.setBackground(rounded(bg,border,16));
   if(type.equals("FIRMAN")||type.equals("SABDA")){
    final boolean book=type.equals("FIRMAN");
    final int ink=label.getCurrentTextColor();
    LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
    View symbol=new View(this){
     @Override protected void onDraw(android.graphics.Canvas canvas){
      super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/32f,getHeight()/32f);
      drawSacredBoxIcon(canvas,book,ink);canvas.restore();
     }
    };
    heading.setBackground(rounded(bg,border,16));heading.setPadding(dp(6),dp(1),dp(8),dp(1));label.setBackground(null);label.setPadding(0,0,0,0);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(book?24:19),dp(book?24:19));iconParams.setMarginEnd(dp(4));heading.addView(symbol,iconParams);heading.addView(label);panel.addView(heading,new LinearLayout.LayoutParams(-2,-2));
   }else panel.addView(label,new LinearLayout.LayoutParams(-2,-2));
   TextView value=text("",fontSize);String boxContent=m.group(2).replaceFirst("^\\n","").replaceFirst("\\n$","");value.setText(formattedReadingText(boxContent));applySharedTextMetrics(value);value.setTextColor(0xff303c43);
   value.setPadding(0,0,0,0);value.setIncludeFontPadding(true);value.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);value.setTextIsSelectable(true);value.setGravity(Gravity.START);value.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);panel.addView(value);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(8),dp(3),dp(8),dp(3));body.addView(panel,lp);
   previous=m.end();
  }
  addBoxParagraph(body,source.substring(previous),fontSize);
 }
 void addBoxParagraph(LinearLayout body,String value,int fontSize){
  java.util.regex.Matcher ornaments=java.util.regex.Pattern.compile("(?m)^[ \\t]*\\*{0,2}---(FLOWER|GEOMETRIC|CLASSIC)---\\*{0,2}[ \\t]*$").matcher(value);
  int start=0;
  while(ornaments.find()){
   addPlainBoxParagraph(body,value.substring(start,ornaments.start()),fontSize);
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),0,dp(12),0);
   View left=new View(this);left.setBackgroundColor(0xff176b47);row.addView(left,new LinearLayout.LayoutParams(0,dp(1),1));
   TextView symbol=text(ornaments.group(1).equals("FLOWER")?"✿":ornaments.group(1).equals("GEOMETRIC")?"❖":"❧",19);
   symbol.setGravity(Gravity.CENTER);symbol.setIncludeFontPadding(false);symbol.setSingleLine(true);symbol.setTextColor(0xff176b47);symbol.setMinHeight(0);symbol.setMinimumHeight(0);symbol.setPadding(0,0,0,0);symbol.setLineSpacing(0,1f);row.addView(symbol,new LinearLayout.LayoutParams(dp(42),-2));
   View right=new View(this);right.setBackgroundColor(0xff176b47);row.addView(right,new LinearLayout.LayoutParams(0,dp(1),1));body.addView(row);
   start=ornaments.end();
  }
  addPlainBoxParagraph(body,value.substring(start),fontSize);
 }
 void addPlainBoxParagraph(LinearLayout body,String value,int fontSize){
  value=value.replaceAll("(?m)^[ \\t]*\\*{1,2}[ \\t]*$","").trim();
  if(value.isEmpty())return;
  TextView t=text("",fontSize);t.setText(formattedReadingText(value));applySharedTextMetrics(t);t.setTextColor(0xff303c43);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);t.setTextIsSelectable(true);t.setPadding(dp(8),dp(1),dp(8),dp(1));body.addView(t);
 }
 boolean manageCurrentBox(){return manageCurrentBox(false);}
 boolean manageCurrentBox(boolean choose){
  if(editor==null)return false;
  final EditText target=editor;
  int cursor=target.getSelectionStart();
  java.util.regex.Matcher boxes=java.util.regex.Pattern.compile("(?ms)^---KOTAK(DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---\\n(.*?)\\n^---KOTAKTAMAT---").matcher(target.getText().toString());
  java.util.ArrayList<Integer> positions=new java.util.ArrayList<>();java.util.ArrayList<String> labels=new java.util.ArrayList<>();
  while(boxes.find()){
   positions.add(boxes.start());labels.add(boxes.group(1).replace("HIKMAH","Kalam Hikmah").replace("CATATAN","Catatan Penting").replace("FIRMAN","Firman Allah SWT").replace("SABDA","Sabda Nabi SAW").replace("DALIL","Dalil").replace("SAHABAT","Kata Sahabat Rdh.").replace("TABII","Kata Tabi\u0027in Rhm.").replace("ULAMA","Kata Ulama\u0027 Rhm."));
   if(choose)continue;
   if(cursor<boxes.start()||cursor>boxes.end())continue;
   final int start=boxes.start(),end=boxes.end(),headEnd=boxes.end(1),bodyStart=boxes.start(2),bodyEnd=boxes.end(2);
   final String original=boxes.group();
   final String[] types={"DALIL","HIKMAH","CATATAN","FIRMAN","SABDA","SAHABAT","TABII","ULAMA"};
   final String[] names={"Dalil","Kalam Hikmah","Catatan Penting","Firman Allah SWT:","Sabda Nabi SAW:","Kata Sahabat Rdh.","Kata Tabi\u0027in Rhm.","Kata Ulama\u0027 Rhm."};
   new AlertDialog.Builder(this).setTitle("Urus kotak").setItems(new String[]{"Tukar jenis kotak","Buang border, kekalkan teks","Gandakan kotak","Tambah kotak di atas","Tambah kotak di bawah"},(d,choice)->{
    if(editor!=target||end>target.length()||!target.getText().subSequence(start,end).toString().equals(original))return;
    if(choice==1){android.text.Editable e=target.getText();android.text.SpannableString inner=new android.text.SpannableString(e.subSequence(bodyStart,bodyEnd));e.replace(start,end,inner);target.setSelection(start+inner.length());return;}
    if(choice==2){android.text.SpannableStringBuilder duplicate=new android.text.SpannableStringBuilder("\n");duplicate.append(target.getText().subSequence(start,end));target.getText().insert(end,duplicate);target.setSelection(end+1);return;}
    new AlertDialog.Builder(this).setTitle("Pilih jenis kotak").setItems(names,(dialog,i)->{
     if(editor!=target||end>target.length()||!target.getText().subSequence(start,end).toString().equals(original))return;
     if(choice==0){target.getText().replace(start,headEnd,"---KOTAK"+types[i]);target.setSelection(Math.min(target.length(),start+("---KOTAK"+types[i]+"---\n").length()));}
     else{int at=choice==3?start:end;String prefix="---KOTAK"+types[i]+"---\n";String block=prefix+"\n---KOTAKTAMAT---";target.getText().insert(at,choice==3?block+"\n":"\n"+block);target.setSelection(at+(choice==3?0:1)+prefix.length());}
    }).show();
   }).show();return true;
  }
  if(choose&&!positions.isEmpty()){new AlertDialog.Builder(this).setTitle("Pilih kotak untuk diurus").setItems(labels.toArray(new String[0]),(d,i)->{if(editor==target){target.setSelection(Math.min(positions.get(i),target.length()));manageCurrentBox(false);}}).show();return true;}
  return false;
 }
 void showTextBoxPicker(){
  if(editor==null)return;
  if(manageCurrentBox())return;
  final int a=Math.max(0,Math.min(editor.getSelectionStart(),editor.getSelectionEnd()));
  final int b=Math.max(a,Math.max(editor.getSelectionStart(),editor.getSelectionEnd()));
  final String[] types={"DALIL","HIKMAH","CATATAN","FIRMAN","SABDA","SAHABAT","TABII","ULAMA"};
  new AlertDialog.Builder(this).setTitle("Kotak teks")
   .setItems(new String[]{"Dalil","Kalam Hikmah","Catatan Penting","Firman Allah SWT:","Sabda Nabi SAW:","Kata Sahabat Rdh.","Kata Tabi\u0027in Rhm.","Kata Ulama\u0027 Rhm.","Buang kotak terpilih","Urus kotak sedia ada"},(d,i)->{
    if(i==9){manageCurrentBox(true);return;}
    if(editor==null||b>editor.length())return;
    String chosen=editor.getText().subSequence(a,b).toString();
    if(i==8){editor.getText().replace(a,b,chosen.replaceAll("(?m)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---\\n?",""));return;}
    String prefix="\n---KOTAK"+types[i]+"---\n";
    int insertA=a,insertB=b;
    String existing=editor.getText().toString();
    java.util.regex.Matcher boxes=java.util.regex.Pattern.compile("(?ms)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---\\n.*?^---KOTAKTAMAT---").matcher(existing);
    while(boxes.find()){if(a>=boxes.start()&&a<boxes.end()){insertA=insertB=boxes.end();chosen="";break;}}
    if(chosen.contains("---KOTAK")){insertA=insertB=b;chosen="";}
    editor.getText().replace(insertA,insertB,prefix+chosen+"\n---KOTAKTAMAT---\n");
    editor.setSelection(insertA+prefix.length(),insertA+prefix.length()+chosen.length());
   }).show();
 }
 void showDividerPicker(){
  final String[] labels={"✿  Bunga Islamik (lalai)","◇  Simpulan geometri","❧  Hiasan klasik"};
  final String[] markers={"---FLOWER---","---GEOMETRIC---","---CLASSIC---"};
  new AlertDialog.Builder(this).setTitle("Pemisah Perenggan (Divider)")
   .setItems(labels,(dialog,choice)->insertEditorText("\n"+markers[choice]+"\n"))
   .show();
 }
 void insertEditorText(String value){
  if(editor==null)return;
  int start=Math.max(0,editor.getSelectionStart());
  editor.getText().insert(start,value);
  editor.setSelection(start+value.length());
 }
 void wrapEditorSelection(String before,String after){
  if(editor==null)return;
  if(before.equals("**")&&after.equals("**")){applyLiveStyle(1);return;}
  if(before.equals("*")&&after.equals("*")){applyLiveStyle(2);return;}
  if(before.equals("<u>")&&after.equals("</u>")){applyLiveStyle(4);return;}
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
 void saveHistoryVersion(){saveHistoryVersion(true);}
 boolean saveHistoryVersion(boolean notify){
  if(editor==null)return false;
  save();
  // An untouched new note has no persistent key or history to save.
  // Allow Selesai to return safely without falsely reporting a storage failure.
  if(selected==null)return true;
  try{
   org.json.JSONArray versions=new org.json.JSONArray(prefs.getString("history_"+selected,"[]"));
   org.json.JSONObject entry=new org.json.JSONObject();
   entry.put("time",System.currentTimeMillis());
   entry.put("title",titleEditor==null?"":titleEditor.getText().toString());
   entry.put("body",encodedEditorText());
   String category=categoryPicker==null||categoryPicker.getSelectedItem()==null?"Tanpa kategori":categoryPicker.getSelectedItem().toString();
   entry.put("category",category);
   if(versions.length()>0){
    org.json.JSONObject last=versions.getJSONObject(versions.length()-1);
    if(entry.getString("title").equals(last.optString("title"))&&entry.getString("body").equals(last.optString("body"))&&entry.getString("category").equals(last.optString("category"))){
     if(notify)Toast.makeText(this,"Versi ini sudah disimpan",Toast.LENGTH_SHORT).show();return true;
    }
   }
   org.json.JSONArray kept=new org.json.JSONArray();
   for(int i=Math.max(0,versions.length()-9);i<versions.length();i++)kept.put(versions.getJSONObject(i));
   kept.put(entry);
   if(!prefs.edit().putString("history_"+selected,kept.toString()).commit())
    throw new java.io.IOException("Gagal menyimpan sejarah");
   if(notify)Toast.makeText(this,"Versi disimpan dalam sejarah",Toast.LENGTH_SHORT).show();
   return true;
  }catch(Exception e){android.util.Log.e("DinNote","Sejarah gagal disimpan",e);if(notify)Toast.makeText(this,"Sejarah gagal disimpan: "+e.getMessage(),Toast.LENGTH_LONG).show();return false;}
 }
 void showHistoryVersions(){
  if(selected==null)return;
  try{
   org.json.JSONArray versions=new org.json.JSONArray(prefs.getString("history_"+selected,"[]"));
   if(versions.length()==0){
    new AlertDialog.Builder(this).setTitle("Sejarah suntingan").setMessage("Belum ada versi tersimpan. Pilih Simpan versi dalam menu editor.")
     .setPositiveButton("OK",null).show();return;
   }
   String[] labels=new String[versions.length()];
   for(int i=0;i<versions.length();i++){
    org.json.JSONObject item=versions.getJSONObject(versions.length()-1-i);
    labels[i]=new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(item.optLong("time",0)))+" — "+item.optString("title","Tanpa tajuk");
   }
   new AlertDialog.Builder(this).setTitle("Sejarah suntingan").setItems(labels,(d,index)->{
    try{
     org.json.JSONObject item=versions.getJSONObject(versions.length()-1-index);
     new AlertDialog.Builder(this).setTitle("Pulihkan versi?")
      .setMessage("Kandungan editor semasa akan digantikan. Simpan versi semasa dahulu jika mahu menyimpannya.")
      .setPositiveButton("Pulihkan",(a,b)->{
       if(editor==null||titleEditor==null)return;
       if(!saveHistoryVersion(false)){
        Toast.makeText(this,"Gagal menyimpan versi semasa; pemulihan dibatalkan",Toast.LENGTH_LONG).show();
        return;
       }
       titleEditor.setText(item.optString("title",""));
       editor.setText(item.optString("body",""));
       if(categoryPicker!=null){
        String category=item.optString("category","Tanpa kategori");
        for(int i=0;i<categoryPicker.getCount();i++)
         if(category.equals(categoryPicker.getItemAtPosition(i).toString())){categoryPicker.setSelection(i);break;}
       }
       save();
       Toast.makeText(this,"Versi dipulihkan",Toast.LENGTH_SHORT).show();
      }).setNegativeButton("Batal",null).show();
    }catch(Exception e){Toast.makeText(this,"Gagal membaca versi",Toast.LENGTH_LONG).show();}
   }).setNegativeButton("Tutup",null).show();
  }catch(Exception e){Toast.makeText(this,"Sejarah tidak dapat dibuka",Toast.LENGTH_LONG).show();}
 }
 void persistEditorPosition(){
  if(editor==null||selected==null||editor.getSelectionEnd()<0)return;
  prefs.edit().putInt("edit_cursor_"+selected,editor.getSelectionEnd()).apply();
 }
 static class EditorSnapshot{
 final CharSequence value;final int start,end;
 EditorSnapshot(EditText e){value=new android.text.SpannableString(e.getText());start=e.getSelectionStart();end=e.getSelectionEnd();}
 void restore(EditText e){e.setText(value);e.setSelection(Math.max(0,Math.min(start,e.length())),Math.max(0,Math.min(end,e.length())));}
 }
 void revealEditorCursor(EditText active){
  if(active==null||active!=editor||active.getLayout()==null||active.getHeight()<=0)return;
  int caret=active.getSelectionEnd();if(caret<0)return;
  active.bringPointIntoView(Math.min(caret,active.length()));
  android.text.Layout layout=active.getLayout();int line=layout.getLineForOffset(Math.min(caret,active.length()));
  int viewport=active.getHeight()-active.getTotalPaddingTop()-active.getTotalPaddingBottom();if(viewport<=0)return;
  int y=active.getScrollY(),top=layout.getLineTop(line),bottom=layout.getLineBottom(line);
  if(top<y)y=top;else if(bottom>y+viewport)y=bottom-viewport;
  y=Math.max(0,Math.min(y,Math.max(0,layout.getHeight()-viewport)));
  if(y!=active.getScrollY())active.scrollTo(active.getScrollX(),y);
 }
 private static final java.util.concurrent.ExecutorService autosaveFlush=java.util.concurrent.Executors.newSingleThreadExecutor();
 void save(){
  if(editor==null)return;
  String value=encodedEditorText();
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
  persistEditorPosition();
  if(saveIndicator!=null){
   final TextView indicator=saveIndicator;
   indicator.setText("Menyimpan…");indicator.setTextColor(0xff68756c);
   final android.content.SharedPreferences store=prefs;
   final String savedKey=selected;
   final String savedValue=value;
   final String savedTitle=title;
   final String savedCategory=category;
   autosaveFlush.execute(()->{
    boolean ok=store.edit().commit();
    runOnUiThread(()->{
     if(saveIndicator!=indicator||editor==null||!savedKey.equals(selected)||!savedValue.equals(encodedEditorText())||!savedTitle.equals(titleEditor==null?"":titleEditor.getText().toString())||!savedCategory.equals(categoryPicker==null||categoryPicker.getSelectedItem()==null?"Tanpa kategori":categoryPicker.getSelectedItem().toString()))return;
     indicator.setText(ok?"✓ Disimpan":"⚠ Simpan gagal");
     indicator.setTextColor(ok?0xff246b43:0xffb64545);
    });
   });
  }
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
 void manageCategories(){ArrayList<String> c=categories();String[] options=new String[c.size()+(maktabah?2:1)];options[0]="+ Tambah kategori";int offset=1;if(maktabah){options[1]="+ Tambah subkategori";offset=2;}for(int i=0;i<c.size();i++)options[i+offset]=c.get(i).replace(" / ","  ›  ");final int first=offset;new AlertDialog.Builder(this).setTitle(maktabah?"Urus kategori & subkategori Maktabah":"Urus kategori").setItems(options,(d,which)->{if(which==0)categoryDialog(null);else if(maktabah&&which==1)addBookSubcategory();else categoryActions(c.get(which-first));}).setNegativeButton("Tutup",null).show();}
 void addBookSubcategory(){
  ArrayList<String> all=categories(),parents=new ArrayList<>();
  for(String name:all)if(!name.contains(" / "))parents.add(name);
  if(parents.isEmpty()){new AlertDialog.Builder(this).setMessage("Cipta kategori utama terlebih dahulu.").setPositiveButton("Tambah kategori",(d,w)->categoryDialog(null)).setNegativeButton("Batal",null).show();return;}
  new AlertDialog.Builder(this).setTitle("Pilih kategori utama").setItems(parents.toArray(new String[0]),(d,which)->{
   String parent=parents.get(which);
   EditText field=new EditText(this);field.setSingleLine(true);field.setHint("Nama subkategori");
   new AlertDialog.Builder(this).setTitle("Subkategori dalam "+parent).setView(field).setPositiveButton("Simpan",(dialog,w)->{
    String child=field.getText().toString().trim().replace("|","").replace("/","").trim();
    if(child.isEmpty()||child.equals("Semua")||child.equals("Tanpa kategori")){Toast.makeText(this,"Nama subkategori tidak sah",Toast.LENGTH_SHORT).show();return;}
    String full=parent+" / "+child;ArrayList<String> existing=categories();
    if(existing.contains(full)){Toast.makeText(this,"Subkategori sudah wujud",Toast.LENGTH_SHORT).show();return;}
    existing.add(full);storeCategories(existing);categoryFilter="Semua";openSection(true);
   }).setNegativeButton("Batal",null).show();
  }).setNegativeButton("Batal",null).show();
 }
 void categoryActions(String old){new AlertDialog.Builder(this).setTitle(old).setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{if(which==0)categoryDialog(old);else new AlertDialog.Builder(this).setMessage("Padam kategori? Nota tidak akan dipadam.").setPositiveButton("Padam",(a,b)->{ArrayList<String> c=categories();c.remove(old);storeCategories(c);for(String k:prefs.getAll().keySet())if(k.startsWith(maktabah?"m_":"n_")&&old.equals(prefs.getString("cat_"+k,"")))prefs.edit().putString("cat_"+k,"Tanpa kategori").apply();categoryFilter="Semua";openSection(maktabah);}).setNegativeButton("Batal",null).show();}).show();}
 void categoryDialog(String old){
  EditText field=new EditText(this);field.setSingleLine(true);
  field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
  field.setHint("Nama kategori");if(old!=null)field.setText(old);
  new AlertDialog.Builder(this).setTitle(old==null?"Kategori baharu":"Ubah nama kategori")
   .setView(field).setPositiveButton("Simpan",(d,w)->{
    String name=field.getText().toString().trim().replace("|","");
    if(name.isEmpty()||name.equals("Semua")||name.equals("Tanpa kategori"))return;
    ArrayList<String> c=categories();
    if(c.contains(name)&&!name.equals(old))return;
    if(old!=null&&!old.equals(name)){
     ArrayList<String> updated=new ArrayList<>();
     for(String cat:c){
      String next=cat.equals(old)?name:(maktabah&&cat.startsWith(old+" / ")?name+cat.substring(old.length()):cat);
      if(updated.contains(next)){Toast.makeText(this,"Nama kategori bertindih",Toast.LENGTH_LONG).show();return;}
      updated.add(next);
     }
     c=updated;
     android.content.SharedPreferences.Editor changes=prefs.edit();
     for(String k:prefs.getAll().keySet()){
      if(!k.startsWith(maktabah?"m_":"n_"))continue;
      String cat=prefs.getString("cat_"+k,"");
      if(cat.equals(old))changes.putString("cat_"+k,name);
      else if(maktabah&&cat.startsWith(old+" / "))changes.putString("cat_"+k,name+cat.substring(old.length()));
     }
     if(!changes.commit()){Toast.makeText(this,"Gagal mengemas kini kategori",Toast.LENGTH_LONG).show();return;}
    }else if(old==null)c.add(name);
    storeCategories(c);categoryFilter="Semua";openSection(maktabah);
   }).setNegativeButton("Batal",null).show();
 }

 String backupDigest(org.json.JSONObject values) throws Exception{
  java.util.ArrayList<String> keys=new java.util.ArrayList<>();
  java.util.Iterator<String> iterator=values.keys();
  while(iterator.hasNext())keys.add(iterator.next());
  java.util.Collections.sort(keys);
  StringBuilder canonical=new StringBuilder();
  for(String key:keys){org.json.JSONArray pair=new org.json.JSONArray();pair.put(key);pair.put(values.get(key));canonical.append(pair.toString()).append(';');}
  return sha256Hex(canonical.toString());
 }
 String sha256Hex(String value) throws java.security.NoSuchAlgorithmException{
  byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  StringBuilder result=new StringBuilder();
  for(byte b:digest)result.append(String.format(java.util.Locale.ROOT,"%02x",b&0xff));
  return result.toString();
 }
 void startBackup(){
  save();
  if(editor!=null&&selected==null&&editor.getText().length()>0){
   Toast.makeText(this,"Sila simpan nota dahulu sebelum membuat sandaran",Toast.LENGTH_LONG).show();
   return;
  }
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
 void startContentImport(){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("application/json");
  startActivityForResult(intent,CONTENT_IMPORT);
 }
 void importContent(android.net.Uri uri){
  try{
   java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
   try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
    if(in==null)throw new java.io.IOException("Fail tidak dapat dibaca");
    byte[] chunk=new byte[8192];int n,total=0;
    while((n=in.read(chunk))!=-1){total+=n;if(total>5*1024*1024)throw new java.io.IOException("Fail terlalu besar");buffer.write(chunk,0,n);}
   }
   org.json.JSONObject doc=new org.json.JSONObject(new String(buffer.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
   if(!"DinNote-content-v1".equals(doc.optString("format")))throw new java.io.IOException("Format JSON tidak disokong");
   org.json.JSONArray items=doc.getJSONArray("items");
   if(items.length()<1||items.length()>1000)throw new java.io.IOException("Had 1 hingga 1000 nota");
   java.util.ArrayList<org.json.JSONObject> checked=new java.util.ArrayList<>();
   for(int i=0;i<items.length();i++){
    org.json.JSONObject item=items.getJSONObject(i);
    String section=item.getString("section"),title=item.getString("title"),content=item.getString("content");
    if(!(section.equals("nota")||section.equals("maktabah"))||title.trim().isEmpty()||title.length()>300||content.trim().isEmpty()||content.length()>200000)throw new java.io.IOException("Item "+(i+1)+" tidak sah");
    checked.add(item);
   }
   android.content.SharedPreferences.Editor edit=prefs.edit();
   for(org.json.JSONObject item:checked){
    boolean book=item.getString("section").equals("maktabah");
    String id=(book?"m_":"n_")+java.util.UUID.randomUUID().toString();
    edit.putString(id,item.getString("content"));
    edit.putString("title_"+id,item.getString("title"));
    edit.putString("cat_"+id,item.optString("category","Tanpa kategori"));
   }
   if(!edit.commit())throw new java.io.IOException("Simpanan gagal");
   Toast.makeText(this,checked.size()+" nota berjaya diimport",Toast.LENGTH_LONG).show();
   notesPage();
  }catch(Exception e){android.util.Log.e("DinNote","Import gagal",e);new AlertDialog.Builder(this).setTitle("Import gagal").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
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
    if(values.length()==0)throw new java.io.IOException("Tiada data untuk disandarkan");
    obj.put("data",values);
    // Integrity digest covers the canonical JSON representation of the stored values.
    obj.put("data_sha256",backupDigest(values));
    byte[] backupBytes=obj.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if(backupBytes.length>20*1024*1024)throw new java.io.IOException("Sandaran melebihi had 20 MB");
    try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
     if(out==null)throw new java.io.IOException("Tidak dapat membuka fail");
     out.write(backupBytes);
     out.flush();
    }
    // Verify the saved document can be reopened and its bytes match the exported snapshot.
    java.security.MessageDigest verifier=java.security.MessageDigest.getInstance("SHA-256");
    java.security.MessageDigest expected=java.security.MessageDigest.getInstance("SHA-256");
    expected.update(backupBytes);
    int verifiedBytes=0;
    try(java.io.InputStream check=getContentResolver().openInputStream(uri)){
     if(check==null)throw new java.io.IOException("Fail sandaran tidak boleh dibaca semula");
     byte[] chunk=new byte[8192];int count;
     while((count=check.read(chunk))!=-1){
      verifiedBytes+=count;
      if(verifiedBytes>20*1024*1024)throw new java.io.IOException("Fail sandaran terlalu besar");
      verifier.update(chunk,0,count);
     }
    }
    if(verifiedBytes!=backupBytes.length||
       !java.util.Arrays.equals(expected.digest(),verifier.digest()))
     throw new java.io.IOException("Pengesahan fail sandaran gagal; sila buat sandaran semula");
    Toast.makeText(this,"Backup berjaya disimpan dan disahkan",Toast.LENGTH_LONG).show();
   }catch(Exception e){Toast.makeText(this,"Backup gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }else if(request==CONTENT_IMPORT){
   importContent(uri);
  }else if(request==BACKUP_OPEN){
   previewRestoreBackup(uri);
  }
 }
 void previewRestoreBackup(android.net.Uri uri){
  try{
   java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
   try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
    if(in==null)throw new java.io.IOException("Fail tidak dapat dibaca");
    byte[] chunk=new byte[8192];int n,total=0;
    while((n=in.read(chunk))!=-1){total+=n;if(total>20*1024*1024)throw new java.io.IOException("Fail terlalu besar");buffer.write(chunk,0,n);}
   }
   if(buffer.size()==0)throw new java.io.IOException("Fail sandaran kosong; data sedia ada tidak diubah");
   org.json.JSONObject backup=new org.json.JSONObject(new String(buffer.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
   if(!"DinNote-backup-v1".equals(backup.optString("format")))throw new java.io.IOException("Format sandaran tidak sah");
   if(backup.has("data_sha256")&&!(backup.opt("data_sha256") instanceof String))
    throw new java.io.IOException("Jenis checksum sandaran tidak sah; data sedia ada tidak diubah");
   if(backup.has("data_sha256")&&!backup.optString("data_sha256","").matches("[0-9a-fA-F]{64}"))
    throw new java.io.IOException("Format checksum sandaran tidak sah; data sedia ada tidak diubah");
   if(backup.has("app_package")&&(! (backup.opt("app_package") instanceof String)||!getPackageName().equals(backup.optString("app_package"))))
    throw new java.io.IOException("Sandaran bukan daripada DinNote");
   if(!backup.has("data")||!(backup.opt("data") instanceof org.json.JSONObject))
    throw new java.io.IOException("Kandungan sandaran tidak sah; data sedia ada tidak diubah");
   org.json.JSONObject values=backup.getJSONObject("data");
   if(backup.has("data_sha256")&&!backupDigest(values).equalsIgnoreCase(backup.getString("data_sha256")))
    throw new java.io.IOException("Integriti fail sandaran gagal");
   int notes=0,books=0;
   java.util.Iterator<String> keys=values.keys();
   while(keys.hasNext()){
    String key=keys.next();
    Object value=values.get(key);
    if(key.startsWith("n_")&&value instanceof String)notes++;
    if(key.startsWith("m_")&&value instanceof String)books++;
   }
   if(notes+books==0)throw new java.io.IOException("Tiada nota atau kitab dalam sandaran");
   // Validate essential types before presenting a restore confirmation.
   java.util.Iterator<String> previewKeys=values.keys();
   while(previewKeys.hasNext()){
    String itemKey=previewKeys.next();
    Object itemValue=values.get(itemKey);
    if(itemValue==null||itemValue==org.json.JSONObject.NULL||
       !(itemValue instanceof String||itemValue instanceof Boolean||itemValue instanceof Number))
     throw new java.io.IOException("Jenis data sandaran tidak disokong");
    if((itemKey.startsWith("title_n_")||itemKey.startsWith("title_m_")||
        itemKey.startsWith("cat_n_")||itemKey.startsWith("cat_m_")||
        itemKey.equals("categories_n")||itemKey.equals("categories_m"))&&!(itemValue instanceof String))
     throw new java.io.IOException("Tajuk atau kategori dalam sandaran tidak sah");
   }
   String filename="Fail sandaran DinNote";
   if(uri.getLastPathSegment()!=null)filename=uri.getLastPathSegment();
   LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(12),dp(22),dp(12));
   TextView title=text("✓  Fail sandaran dikenal pasti",17);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setTextColor(0xff566a70);panel.addView(title);
   TextView details=text(filename+"\\n\\n"+notes+" nota    •    "+books+" kitab\\n\\n✓ Format DinNote disokong\\n✓ Identiti aplikasi sepadan\\n✓ Kandungan asas ditemui",14);
   details.setTextColor(0xff596e75);details.setPadding(dp(12),dp(12),dp(12),dp(12));details.setBackground(rounded(0xfff2f8f3,0xffdbe9de,12));panel.addView(details);
   TextView warning=text("Perhatian: Pemulihan akan menggantikan semua data sedia ada. Pemeriksaan terperinci dibuat sekali lagi sebelum data diganti.",13);
   warning.setTextColor(0xff75572d);warning.setPadding(dp(8),dp(16),dp(8),dp(8));panel.addView(warning);
   new AlertDialog.Builder(this).setTitle("Pulihkan Data DinNote").setView(panel)
    .setNegativeButton("Batal",null)
    .setPositiveButton("Pulihkan Sekarang",(dialog,which)->restoreBackup(uri)).show();
  }catch(Exception e){
   new AlertDialog.Builder(this).setTitle("Sandaran tidak dapat dipulihkan")
    .setMessage(e.getMessage()+"\\n\\nData sedia ada tidak diubah.")
    .setPositiveButton("Tutup",null).show();
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
   if(backup.has("data_sha256")&&!backupDigest(values).equalsIgnoreCase(backup.getString("data_sha256")))
    throw new java.io.IOException("Integriti fail sandaran gagal; data sedia ada tidak diubah");
   if(values.length()==0)throw new java.io.IOException("Sandaran kosong; data sedia ada tidak diubah");
   if(values.length()>100000)throw new java.io.IOException("Terlalu banyak rekod sandaran; data sedia ada tidak diubah");
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
    // Font levels are integer preferences; reject malformed backups before clearing local data.
    if((key.equals("reader_font_level")||key.equals("editor_font_level"))&&
       (!(value instanceof Number)||((Number)value).doubleValue()%1!=0||
        ((Number)value).doubleValue()<0||((Number)value).doubleValue()>3))
     throw new java.io.IOException("Saiz font dalam sandaran tidak sah; data sedia ada tidak diubah");
    if((key.equals("reader_two_columns")||key.equals("reader_wide_spacing")||
        key.equals("reader_clean_mode")||key.equals("reader_swipe_mode"))&&!(value instanceof Boolean))
     throw new java.io.IOException("Tetapan bacaan dalam sandaran tidak sah; data sedia ada tidak diubah");
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
   // Discard references to the old editor and reader after replacing all stored records.
   readerScroll=null;readerDetector=null;editor=null;titleEditor=null;saveIndicator=null;categoryPicker=null;
   selected=null;items=null;sectionOpen=false;
   home();
   Toast.makeText(this,"Data DinNote berjaya dipulihkan",Toast.LENGTH_LONG).show();
  }catch(Exception e){Toast.makeText(this,"Restore gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 @Override public void onConfigurationChanged(android.content.res.Configuration newConfig){
  super.onConfigurationChanged(newConfig);
  // Rebuild only the home dashboard on Fold6 fold/unfold; retain active editor and reader.
  if(root!=null && root.findViewWithTag("dinnote_home")!=null){
   // Preserve proportional home scroll position when the Fold6 changes width.
   ScrollView oldScroll=null;
   for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){oldScroll=(ScrollView)root.getChildAt(i);break;}
   float scrollFraction=0f;
   if(oldScroll!=null && oldScroll.getChildCount()>0){
    int oldRange=Math.max(1,oldScroll.getChildAt(0).getHeight()-oldScroll.getHeight());
    scrollFraction=Math.min(1f,Math.max(0f,(float)oldScroll.getScrollY()/oldRange));
   }
   final float restoreFraction=scrollFraction;
   home();
   for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){
    final ScrollView newScroll=(ScrollView)root.getChildAt(i);
    newScroll.post(()->{
     if(!isFinishing()&&!isDestroyed()&&newScroll.getParent()!=null&&newScroll.getChildCount()>0){
      int range=Math.max(0,newScroll.getChildAt(0).getHeight()-newScroll.getHeight());
      newScroll.scrollTo(0,Math.round(restoreFraction*range));
     }
    });
    break;
   }
   return;
  }
  if(editor!=null){
   // Keep the active editor and unsaved typing intact across fold/unfold.
   // Reflow text using Android's new width without recreating the editor.
   final EditText activeEditor=editor;
   final int selectionStart=activeEditor.getSelectionStart();
   final int selectionEnd=activeEditor.getSelectionEnd();
   activeEditor.requestLayout();
   activeEditor.invalidate();
   if(titleEditor!=null){titleEditor.requestLayout();titleEditor.invalidate();}
   activeEditor.post(()->{
    // Do not steal focus or move the cursor if the user has resumed typing.
    if(editor==activeEditor&&activeEditor.getParent()!=null&&activeEditor.hasFocus()
      &&activeEditor.getSelectionStart()==selectionStart&&activeEditor.getSelectionEnd()==selectionEnd){
     int length=activeEditor.length();
     if(selectionStart>=0&&selectionEnd>=0)activeEditor.setSelection(Math.min(selectionStart,length),Math.min(selectionEnd,length));
    }
   });
   return;
  }
  if(sectionOpen&&selected==null){
   if(listScroll!=null){
    int y=listScroll.getScrollY();
    android.view.View listContent=listScroll.getChildAt(0);
    int extent=Math.max(1,(listContent==null?0:listContent.getHeight())-listScroll.getHeight());
    float fraction=Math.min(1f,Math.max(0f,(float)y/extent));
    if(maktabah)savedBooksScroll=y;else savedNotesScroll=y;
    final boolean currentSection=maktabah;
    root.post(()->{
     if(editor!=null||selected!=null||!sectionOpen||maktabah!=currentSection)return;
     notesPage();
     ScrollView target=listScroll;
     if(target!=null)target.post(()->{
      android.view.View targetContent=target.getChildAt(0);
      int max=Math.max(0,(targetContent==null?0:targetContent.getHeight())-target.getHeight());
      int restored=Math.round(fraction*max);
      target.scrollTo(0,restored);
      if(maktabah)savedBooksScroll=restored;else savedNotesScroll=restored;
     });
    });
   }else root.post(()->{if(editor==null&&selected==null&&sectionOpen)notesPage();});
   return;
  }
  if(selected!=null&&!prefs.contains(selected)){
   selected=null;readerScroll=null;
   root.post(()->{if(!isFinishing()&&!isDestroyed()&&editor==null){if(sectionOpen)notesPage();else home();}});
   return;
  }
  if(selected!=null&&prefs.contains(selected)){
   String key=selected;
   saveReaderPosition(key);
   root.post(()->{if(!isFinishing()&&!isDestroyed()&&editor==null&&key.equals(selected)&&prefs.contains(key))read(key);});
  }
 }
 // Durable checkpoint when Android backgrounds the editor; protects recent autosaved text
 // if the process is later killed before asynchronous preference writes finish.
 void saveReaderPosition(String key){
  if(readerScroll==null||key==null||readerScroll.getChildCount()==0)return;
  int max=Math.max(0,readerScroll.getChildAt(0).getHeight()-readerScroll.getHeight());
  int y=Math.max(0,Math.min(max,readerScroll.getScrollY()));
  int permille=max==0?0:Math.max(0,Math.min(1000,Math.round(y*1000f/max)));
  prefs.edit().putInt("reading_scroll_"+key,y).putInt("reading_progress_permille_"+key,permille).apply();
 }
 @Override protected void onPause(){
  if(editor==null&&selected!=null)saveReaderPosition(selected);
  if(editor!=null){
   save();
   if(!prefs.edit().commit()){
    android.util.Log.e("DinNote","Failed to flush editor data on pause");
    if(saveIndicator!=null)saveIndicator.setText("⚠ Simpan gagal");
   }
  }
  super.onPause();
 }
 @Override protected void onStop(){
  if(editor!=null&&!prefs.edit().commit())
   android.util.Log.e("DinNote","Failed to flush editor data on stop");
  super.onStop();
 }
 public void onBackPressed(){if(editor!=null){finishEditingSafely();}else if(selected!=null){returnFromReader();}else if(sectionOpen){home();}else super.onBackPressed();}
}
