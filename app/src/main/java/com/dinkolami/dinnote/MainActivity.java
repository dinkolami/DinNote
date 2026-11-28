package com.dinkolami.dinnote;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import android.text.*;import android.content.*;import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root,items,compactSummary; ScrollView listScroll,readerScroll; int savedNotesScroll=0,savedBooksScroll=0; String lastOpenedCard=null; android.view.GestureDetector readerDetector; Runnable lectureBackReveal; EditText editor,titleEditor; TextView saveIndicator; Spinner categoryPicker; String categoryFilter="Semua"; String sortMode="Terbaru"; String selected=null; boolean maktabah=false, sectionOpen=false; android.content.SharedPreferences prefs;
 int green=Color.rgb(23,107,71), gold=Color.rgb(197,166,107); static final int BACKUP_CREATE=4201, BACKUP_OPEN=4202, CONTENT_IMPORT=4203, BOOK_TEXT_IMPORT=4204, PDF_EXPORT_CREATE=4205; String pdfExportTitle="",pdfExportBody="";
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("notes",0);if(!prefs.getBoolean("font_defaults_13_15_16_18_applied",false)){prefs.edit().putInt("reader_font_level",2).putInt("editor_font_level",2).putBoolean("font_defaults_13_15_16_18_applied",true).apply();}if(!prefs.getBoolean("fasa4_default_besar_applied",false)){android.content.SharedPreferences.Editor fontUpgrade=prefs.edit();if(prefs.getInt("reader_font_level",1)==1)fontUpgrade.putInt("reader_font_level",2);if(prefs.getInt("editor_font_level",1)==1)fontUpgrade.putInt("editor_font_level",2);fontUpgrade.putBoolean("fasa4_default_besar_applied",true).apply();}if(prefs.contains("security_pin_hash"))promptUnlock();else {openHomeSafely();receiveBookShare();}} // User content and categories are local; import them explicitly.}
 void openHomeSafely(){
  try{installMindmapDemoOnce();home();}
  catch(Exception problem){
   android.util.Log.e("DinNote","Home startup failure; entering data-safe recovery",problem);
   showDataSafeRecovery(problem);
  }
 }
 void installMindmapDemoOnce(){
  final String flag="demo_ikhlas_v1_installed";
  if(prefs.getBoolean(flag,false))return;
  final String key="m_demo_kitab_keikhlasan_v1";
  if(prefs.contains(key)){prefs.edit().putBoolean(flag,true).apply();return;}
  String content="Bab 1: Keutamaan Ikhlas\n"
   +"Fasal 1: Niat dalam Amalan\n"
   +"1. Amalan bergantung pada niat\n"
   +"إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ\n"
   +"Maksud ringkas: Nilai sesuatu amalan berkait dengan niat pelakunya.\n\n"
   +"Fasal 2: Mengharap Keredaan Allah\n"
   +"1. Memperbetulkan tujuan ibadah\n"
   +"Lakukan kebaikan dengan tujuan mencari keredaan Allah.\n\n"
   +"Bab 2: Tanda-tanda Ikhlas\n"
   +"Fasal 1: Menjaga Niat\n"
   +"1. Muhasabah sebelum beramal\n"
   +"1. Muhasabah selepas beramal\n\n"
   +"Bab 3: Memelihara Keikhlasan\n"
   +"Fasal 1: Menghindari Riya\n"
   +"1. Tidak mengejar pujian manusia\n"
   +"1. Memohon pertolongan Allah\n\n"
   +"Nota: Kandungan ini ialah contoh demonstrasi Petaminda dan boleh disunting atau dipadam.";
  boolean saved=prefs.edit().putString(key,content)
   .putString("title_"+key,"Kitab Keikhlasan (Contoh Petaminda)")
   .putString("cat_"+key,"Kitab-Kitab")
   .putLong("updated_"+key,System.currentTimeMillis())
   .putBoolean(flag,true).commit();
  if(!saved)android.util.Log.w("DinNote","Demo Petaminda tidak berjaya disimpan");
 }
 void showDataSafeRecovery(Exception problem){
  editor=null;titleEditor=null;saveIndicator=null;selected=null;sectionOpen=false;
  LinearLayout safe=new LinearLayout(this);safe.setOrientation(1);safe.setPadding(dp(20),dp(35),dp(20),dp(20));safe.setBackgroundColor(0xfffafbf7);
  setContentView(safe);
  TextView heading=text("DinNote · Mod Pemulihan",20);heading.setTextColor(0xff176b47);safe.addView(heading);
  TextView warning=text("Paparan utama menghadapi masalah. Nota dan kitab tidak dipadam. Buat backup dahulu sebelum mencuba pembaikan.",15);warning.setPadding(0,dp(12),0,dp(18));safe.addView(warning);
  Button backup=new Button(this);backup.setText("Simpan backup data sekarang");safe.addView(backup);backup.setOnClickListener(v->startBackup());
  Button retry=new Button(this);retry.setText("Cuba buka halaman utama");safe.addView(retry);retry.setOnClickListener(v->openHomeSafely());
  TextView detail=text("Ralat: "+problem.getClass().getSimpleName()+": "+String.valueOf(problem.getMessage()),12);detail.setTextIsSelectable(true);safe.addView(detail);
 }
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
  root=new LinearLayout(this);root.setOrientation(1);root.setBackground(pageShade());
  setContentView(root);applySystemBarInsets(root);
  // Subtle content fade on navigation; keep system bars and all controls unchanged.
  root.setAlpha(0.94f);
  root.animate().alpha(1f).setDuration(140L).start();
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  header.setPadding(dp(8),dp(3),dp(8),dp(3));header.setBackgroundColor(Color.TRANSPARENT);
  ImageView menu=headerIcon("⌂",0xff9daeb9,"Halaman utama DinNote");header.addView(menu);
  menu.setOnClickListener(v->home());
  ImageView settingsMenu=headerIcon("☰",0xff9daeb9,"Menu DinNote dan Tetapan");header.addView(settingsMenu);
  settingsMenu.setOnClickListener(v->showMainMenu());
  ImageView logo=new ImageView(this);logo.setImageBitmap(transparentHeaderLogo());logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Logo DinNote");logo.setClickable(true);logo.setPadding(dp(3),dp(3),dp(3),dp(3));logo.setBackground(rounded(0xfffdfefe,0xffb89543,50));logo.setClipToOutline(true);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(34));lp.leftMargin=dp(6);lp.rightMargin=dp(5);header.addView(logo,lp);
  logo.setOnClickListener(v->home());
  TextView name=text("DinNote",16);name.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.ITALIC));name.setTextColor(0xff075033);
  name.setGravity(Gravity.CENTER_VERTICAL);name.setPadding(dp(3),0,0,0);name.setSingleLine(true);name.setAutoSizeTextTypeUniformWithConfiguration(10,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);header.addView(name,new LinearLayout.LayoutParams(0,dp(40),1));
  name.setOnClickListener(v->home());
  ImageView inbox=headerIcon("▣",0xff9daeb9,"Buka nota");// Nota is accessible through the category tile and main menu.
  inbox.setOnClickListener(v->openSection(false));
  ImageView bell=headerIcon("▦",0xff216e4e,"Board & Sub-board");header.addView(bell);
  bell.setOnClickListener(v->{if(editor!=null){save();if(selected==null)return;}manageCategories();});
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
  LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(30),dp(30));iconParams.leftMargin=dp(3);iconParams.rightMargin=dp(3);view.setLayoutParams(iconParams);
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
   if(verifyPin(input.getText().toString())){dialog.dismiss();openHomeSafely();receiveBookShare();}
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
 
 void appendQurtubiLecture(){
 if(true){String id="m_qurtubi_fatihah_1";String existing=prefs.getString(id,"");if(!prefs.getBoolean("deleted_"+id,false)&&!existing.trim().isEmpty()&&!existing.contains("الْفَوَائِدُ — Pengajaran")){prefs.edit().putString(id,existing+"\n\n---TERJEMAHAN---\nTerjemahan Melayu: Menurut ulama yang dinukilkan, basmalah merupakan sumpah daripada Tuhan kita yang diturunkan pada permulaan setiap surah, menegaskan kebenaran kandungannya serta pemenuhan janji, kelembutan dan kebaikan-Nya. Basmalah dikurniakan kepada umat ini selepas Nabi Sulaiman عليه السلام. Sebahagian ulama menyatakan bahawa basmalah merangkumi seluruh syariat kerana menunjukkan kepada Zat dan sifat Allah; al-Qurthubi membenarkan pandangan ini.\n---SYARAHAN---\n1. Basmalah dan keyakinan: Tafsiran sumpah ini ialah pandangan ulama yang dinukilkan oleh al-Qurthubi, bukan lafaz hadis qudsi.\n2. Janji Allah: Lafaz وَعْدِي وَلُطْفِي وَبِرِّي mengingatkan kita kepada janji, kelembutan dan kebaikan Allah. Pertolongan-Nya tidak semestinya datang dalam bentuk yang kita jangkakan.\n3. Basmalah menghimpunkan asas syariat: Nama Allah menunjukkan kepada Zat-Nya, manakala al-Rahman dan al-Rahim menunjukkan sifat rahmat-Nya. Ini bukan bermaksud semua hukum terperinci terkandung secara lafaz dalam basmalah.\n---DALIL---\nوَمَنْ أَصْدَقُ مِنَ اللَّهِ حَدِيثًا\nSiapakah yang lebih benar perkataannya daripada Allah? (al-Nisa’: 87)\nإِنَّهُ مِنْ سُلَيْمَانَ وَإِنَّهُ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nSesungguhnya surat itu daripada Sulaiman dan sesungguhnya ia dengan nama Allah Yang Maha Pemurah lagi Maha Penyayang. (al-Naml: 30)\n---IKTIBAR---\nالْفَوَائِدُ — Pengajaran\n1. Biasakan menyebut nama Allah sebelum melakukan pekerjaan yang baik.\n2. Jangan berputus asa daripada rahmat Allah ketika menghadapi kesusahan.\n3. Yakin kepada janji Allah walaupun hasil usaha belum kelihatan.\n4. Didik keluarga mengenal Allah melalui rahmat dan kebaikan-Nya.\n5. Usaha bersungguh-sungguh, tetapi jangan bergantung kepada usaha semata-mata.\n\nالْعِبَرُ — Iktibar\n1. Kita sering menyebut Bismillah pada lidah. Adakah hati turut mengharapkan pertolongan Allah?\n2. Ketika urusan menjadi mudah, adakah kita mengingati Allah sebagaimana ketika menghadapi kesusahan?\n3. Jangan mengukur kasih sayang Allah hanya melalui kesenangan dunia. Kesukaran juga boleh menjadi jalan mendidik jiwa.\n4. Sebelum memulakan sesuatu, tanyalah diri: adakah pekerjaan ini diredai Allah?\n---KHULASAH---\nMulakan pekerjaan dengan nama Allah, laksanakan kerana Allah dan sandarkan hasilnya kepada Allah. Jangan sekadar menyebut nama-Nya pada lidah; hadirkan pergantungan kepada-Nya dalam hati.\n---TAMAT---").apply();}}
 }
 void seedQurtubiPosts(){
 if(!prefs.getBoolean("qurtubi_basmalah_pilot_v1",false)){String id="m_qurtubi_fatihah_1";String old="بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nالنَّصُّ الْعَرَبِيُّ\nTeks tafsir asal belum diimport.\n\nTerjemahan Indonesia\nPadanan terjemahan belum diimport.";android.content.SharedPreferences.Editor p=prefs.edit();if(!prefs.getBoolean("deleted_"+id,false)&&((!prefs.contains(id)&&!prefs.getBoolean("qurtubi_posts_v1",false))||old.equals(prefs.getString(id,""))))p.putString(id,"بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nالْأُولَى: قَالَ الْعُلَمَاءُ: «بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ» قَسَمٌ مِنْ رَبِّنَا أَنْزَلَهُ عِنْدَ رَأْسِ كُلِّ سُورَةٍ، يُقْسِمُ لِعِبَادِهِ إِنَّ هَذَا الَّذِي وَضَعْتُ لَكُمْ يَا عِبَادِي فِي هَذِهِ السُّورَةِ حَقٌّ، وَإِنِّي أُوفِي لَكُمْ بِجَمِيعِ مَا ضَمَّنْتُ فِي هَذِهِ السُّورَةِ مِنْ وَعْدِي وَلُطْفِي وَبِرِّي. وَ«بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ» مِمَّا أَنْزَلَهُ اللَّهُ تَعَالَى فِي كِتَابِنَا وَعَلَى هَذِهِ الْأُمَّةِ خُصُوصًا بَعْدَ سُلَيْمَانَ عَلَيْهِ السَّلَامُ. وَقَالَ بَعْضُ الْعُلَمَاءِ: إِنَّ «بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ» تَضَمَّنَتْ جَمِيعَ الشَّرْعِ، لِأَنَّهَا تَدُلُّ عَلَى الذَّاتِ وَعَلَى الصِّفَاتِ، وَهَذَا صَحِيحٌ. || Pertama: Ulama berkata, lafaz Bismillahirrahmanirrahim adalah sumpah Tuhan kita yang Dia turunkan di awal setiap surah. Dia bersumpah kepada hamba-hamba-Nya: Wahai hamba-hamba-Ku, sesungguhnya yang Aku letakkan untuk kalian dalam surah ini adalah suatu kebenaran, dan Aku akan memenuhi semua yang Aku jamin dalam surah ini, yaitu janji, kelembutan-Ku dan kebaikan-Ku. Bismillahirrahmanirrahim adalah sesuatu yang Allah turunkan dalam kitab kita dan diberikan kepada umat ini, khususnya setelah diberikan kepada Sulaiman. Sebagian ulama berkata bahwa lafaz ini mengandung seluruh syariat, sebab ia menunjukkan kepada Zat dan sifat Allah. Ini adalah pendapat yang benar.\n\nRujukan: al-Qurthubi 1/91, https://shamela.ws/book/20855/97 ; edisi Indonesia hlm. 237 (teks dinormalisasi daripada OCR).").putString("title_"+id,"Tafsir al-Qurthubi — al-Fatihah, Ayat 1 (Masalah 1)").putString("cat_"+id,"Kitab-Kitab › Tafsir");p.putBoolean("qurtubi_basmalah_pilot_v1",true).apply();}

 appendQurtubiLecture();
 if(!prefs.getBoolean("qurtubi_intro_pilot_v1",false)){String id="m_qurtubi_fatihah_intro_1";android.content.SharedPreferences.Editor pilot=prefs.edit();if(!prefs.contains(id)&&!prefs.getBoolean("deleted_"+id,false))pilot.putString(id,"فَضَائِلُ الْفَاتِحَةِ وَأَسْمَاؤُهَا\n\nPermasalahan pertama — petikan percubaan\n\nAt-Tirmidzi meriwayatkan dari Ubay bin Ka'ab, dia berkata: Rasulullah SAW bersabda:\n\nمَا أَنْزَلَ اللَّهُ فِي التَّوْرَاةِ وَلَا فِي الْإِنْجِيلِ مِثْلَ أُمِّ الْقُرْآنِ، وَهِيَ السَّبْعُ الْمَثَانِي، وَهِيَ مَقْسُومَةٌ بَيْنِي وَبَيْنَ عَبْدِي، وَلِعَبْدِي مَا سَأَلَ.\n\nTerjemahan\nAllah tidak menurunkan seperti Ummul Qur'an (Al Faatihah) di dalam Taurat dan tidak (pula) di dalam Injil. Ia adalah As-Sab'u Al Matsani (tujuh ayat yang diulang), dia terbagi di antara Aku dan hamba-Ku, dan bagi hamba-Ku apa yang dia minta.\n\nRujukan: Tafsir al-Qurthubi, jilid 1, hlm. 280; At-Tirmidzi no. 3125 (menurut edisi terjemahan). Teks Arab belum disemak dengan naskhah asal.").putString("title_"+id,"Tafsir al-Qurthubi — Keutamaan al-Fatihah (Percubaan)").putString("cat_"+id,"Kitab-Kitab › Tafsir");pilot.putBoolean("qurtubi_intro_pilot_v1",true).apply();}

 if(prefs.getBoolean("qurtubi_posts_v1",false))return;
 String[] verses={"بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ","الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ","الرَّحْمَٰنِ الرَّحِيمِ","مَالِكِ يَوْمِ الدِّينِ","إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ","اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ","صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"};
 android.content.SharedPreferences.Editor e=prefs.edit();
 for(int i=0;i<verses.length;i++){String id="m_qurtubi_fatihah_"+(i+1);if(!prefs.contains(id)&&!prefs.getBoolean("deleted_"+id,false))e.putString(id,verses[i]+"\n\nالنَّصُّ الْعَرَبِيُّ\nTeks tafsir asal belum diimport.\n\nTerjemahan Indonesia\nPadanan terjemahan belum diimport.").putString("title_"+id,"Tafsir al-Qurthubi — al-Fatihah, Ayat "+(i+1)).putString("cat_"+id,"Kitab-Kitab › Tafsir");}
 e.putBoolean("qurtubi_posts_v1",true).apply();
 }
 void addBoardTrailLink(LinearLayout trail,String label,String target,boolean active){
  TextView link=text(label,13);link.setTextColor(active?0xff243d32:0xff216e4e);link.setGravity(Gravity.CENTER_VERTICAL);link.setPadding(dp(3),0,dp(3),0);link.setSingleLine(true);
  if(active)link.setTypeface(null,android.graphics.Typeface.BOLD);
  trail.addView(link,new LinearLayout.LayoutParams(-2,-1));
  if(!active){link.setContentDescription("Buka "+label);link.setOnClickListener(v->{categoryFilter=target;prefs.edit().putString(maktabah?"board_last_m":"board_last_n",target).apply();if(maktabah)savedBooksScroll=0;else savedNotesScroll=0;notesPage(true);});}
 }
 void notesPage(){notesPage(false);}
 void seedMuqaddimahCollection(){
  if(prefs.getBoolean("muqaddimah_collection_v2",false))return;
  String board="MUQADDIMAH", id="m_muqaddimah_collection_v2";
  java.util.LinkedHashSet<String> cats=new java.util.LinkedHashSet<>();
  for(String c:prefs.getString("categories_m","").split("\\|"))if(!c.trim().isEmpty())cats.add(c.trim());
  android.content.SharedPreferences.Editor p=prefs.edit();
  String[] defaults={"⁣Kesyukuran\nالْحَمْدُ لِلَّهِ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ، كَمَا يُحِبُّ رَبُّنَا وَيَرْضَى.\n\nTerjemahan: Segala puji bagi Allah dengan pujian yang banyak, baik dan diberkati, sebagaimana disukai dan diredai Tuhan kita.\nSumber: Lafaz pujian umum.","⁣Selawat ringkas\nاللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى سَيِّدِنَا مُحَمَّدٍ، وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ.\n\nTerjemahan: Ya Allah, limpahkan selawat dan salam kepada Nabi Muhammad, keluarga dan seluruh sahabatnya.\nSumber: Susunan selawat umum.","⁣Pembukaan khutbah hajat (petikan)\nإِنَّ الْحَمْدَ لِلَّهِ، نَسْتَعِينُهُ وَنَسْتَغْفِرُهُ، وَنَعُوذُ بِاللَّهِ مِنْ شُرُورِ أَنْفُسِنَا، مَنْ يَهْدِهِ اللَّهُ فَلَا مُضِلَّ لَهُ، وَمَنْ يُضْلِلْ فَلَا هَادِيَ لَهُ.\n\nTerjemahan: Sesungguhnya segala puji bagi Allah. Kita memohon pertolongan dan keampunan-Nya serta berlindung kepada-Nya daripada kejahatan diri. Sesiapa yang Allah beri petunjuk, tiada yang dapat menyesatkannya; sesiapa yang disesatkan-Nya, tiada yang dapat memberinya petunjuk.\nSumber: Petikan khutbah hajat; riwayat Abu Dawud, 2118.","⁣Majlis ilmu\nالْحَمْدُ لِلَّهِ الَّذِي وَفَّقَنَا لِطَلَبِ الْعِلْمِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى سَيِّدِنَا مُحَمَّدٍ، وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ.\n\nTerjemahan: Segala puji bagi Allah yang memberi kita taufik untuk menuntut ilmu. Selawat dan salam ke atas Nabi Muhammad, keluarga dan seluruh sahabatnya.\nSumber: Susunan umum, bukan petikan hadis.\n\n⁣Nikmat iman dan Islam\nالْحَمْدُ لِلَّهِ الَّذِي أَنْعَمَ عَلَيْنَا بِنِعْمَةِ الْإِيمَانِ وَالْإِسْلَامِ، وَهَدَانَا إِلَى صِرَاطِهِ الْمُسْتَقِيمِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى سَيِّدِنَا مُحَمَّدٍ، وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ.\n\nTerjemahan: Segala puji bagi Allah yang mengurniakan nikmat iman dan Islam serta membimbing kita ke jalan-Nya yang lurus. Selawat dan salam ke atas Nabi Muhammad, keluarga dan sahabatnya.\nSumber: Susunan umum.","⁣Doa memohon kelapangan\nرَبِّ اشْرَحْ لِي صَدْرِي، وَيَسِّرْ لِي أَمْرِي، وَاحْلُلْ عُقْدَةً مِنْ لِسَانِي، يَفْقَهُوا قَوْلِي.\n\nTerjemahan: Wahai Tuhanku, lapangkan dadaku, mudahkan urusanku dan lepaskan kekakuan lidahku agar mereka memahami perkataanku.\nSumber: Surah Taha 20:25–28.","⁣Doa kebaikan dunia dan akhirat\nرَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ.\n\nTerjemahan: Wahai Tuhan kami, berikanlah kami kebaikan di dunia dan akhirat serta peliharalah kami daripada azab neraka.\nSumber: Surah al-Baqarah 2:201."};
  StringBuilder body=new StringBuilder();
  for(int i=0;i<defaults.length;i++){
   String oldId="m_muqaddimah_v1_"+i;
   if(prefs.getBoolean("deleted_"+oldId,false))continue;
   String value=prefs.getString(oldId,defaults[i]);
   if(body.length()>0)body.append("\n\n");
   body.append(value);
   if(prefs.contains(oldId)){
    p.remove(oldId).remove("title_"+oldId).remove("cat_"+oldId);
   }
  }
  for(String c:new java.util.ArrayList<String>(cats)){
   if(!c.equals("Muqaddimah & Doa")&&!c.startsWith("Muqaddimah & Doa › "))continue;
   boolean used=false;
   for(java.util.Map.Entry<String,?> e:prefs.getAll().entrySet()){
    if(!e.getKey().startsWith("cat_m_")||!c.equals(e.getValue()))continue;
    String post=e.getKey().substring(4);
    if(!post.matches("m_muqaddimah_v1_[0-5]")&&prefs.contains(post)){used=true;break;}
   }
   if(!used)cats.remove(c);
  }
  cats.add(board);
  if(!prefs.contains(id)&&!prefs.getBoolean("deleted_"+id,false)&&body.length()>0)
   p.putString(id,body.toString()).putString("title_"+id,"Koleksi Muqaddimah Kuliah").putString("cat_"+id,board);
  p.putString("categories_m",android.text.TextUtils.join("|",cats)).putBoolean("muqaddimah_collection_v2",true).putBoolean("muqaddimah_collection_v1",true).apply();
 }
 void notesPage(boolean keepBoards){if(maktabah){seedQurtubiPosts();seedMuqaddimahCollection();}editor=null;titleEditor=null;categoryPicker=null;selected=null;
  LinearLayout existing=keepBoards&&root!=null?root.findViewWithTag("board_list_pane"):null;
  if(existing==null)base("DinNote");
  LinearLayout listHost=root;
  if(existing!=null){listHost=existing;listHost.removeAllViews();refreshBoardSelection(root);}
  else if(homeWindowWidthDp()>=440){
   LinearLayout split=new LinearLayout(this);root.addView(split,new LinearLayout.LayoutParams(-1,0,1));
   ScrollView boards=new ScrollView(this);boards.setTag("board_split_left");split.addView(boards,new LinearLayout.LayoutParams(0,-1,1));
   LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(8),dp(8),dp(8),dp(8));boards.addView(panel);
   renderHomeBoards(panel,false);renderHomeBoards(panel,true);
   rememberBoardScroll(boards,"board_scroll_split");
   android.widget.FrameLayout divider=new android.widget.FrameLayout(this);divider.setContentDescription("Tarik untuk laras kelebaran panel");divider.setTag("board_split_divider");
   View goldLine=new View(this);goldLine.setBackgroundColor(0xffd5bb72);divider.addView(goldLine,new android.widget.FrameLayout.LayoutParams(dp(1),-1,Gravity.CENTER));
   split.addView(divider,new LinearLayout.LayoutParams(dp(12),-1));
   listHost=new LinearLayout(this);listHost.setOrientation(1);listHost.setTag("board_list_pane");split.addView(listHost,new LinearLayout.LayoutParams(0,-1,1));
   final LinearLayout rightPane=listHost;
   float ratio=Math.max(0.30f,Math.min(0.65f,prefs.getFloat("board_split_ratio",0.5f)));
   boards.setLayoutParams(new LinearLayout.LayoutParams(0,-1,ratio));
   rightPane.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f-ratio));
   if(prefs.getBoolean("board_split_hidden",false)){boards.setVisibility(View.GONE);divider.setVisibility(View.GONE);}

   divider.setOnTouchListener(new View.OnTouchListener(){
    float startX,startRatio,currentRatio;
    public boolean onTouch(View v,android.view.MotionEvent e){
     int width=split.getWidth()-divider.getWidth();if(width<=0)return false;
     switch(e.getActionMasked()){
      case android.view.MotionEvent.ACTION_DOWN:
       startX=e.getRawX();startRatio=boards.getWidth()/(float)width;currentRatio=startRatio;v.getParent().requestDisallowInterceptTouchEvent(true);return true;
      case android.view.MotionEvent.ACTION_MOVE:
       float min=Math.min(0.5f,Math.max(0.30f,dp(145)/(float)width));
       float max=Math.max(min,Math.min(0.65f,1f-dp(220)/(float)width));
       currentRatio=Math.max(min,Math.min(max,startRatio+(e.getRawX()-startX)/width));
       boards.setLayoutParams(new LinearLayout.LayoutParams(0,-1,currentRatio));
       rightPane.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f-currentRatio));return true;
      case android.view.MotionEvent.ACTION_UP:
      case android.view.MotionEvent.ACTION_CANCEL:
       prefs.edit().putFloat("board_split_ratio",currentRatio).apply();v.getParent().requestDisallowInterceptTouchEvent(false);return true;
     }return true;
    }
   });
  }
  LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(12),dp(7),dp(12),dp(5));tabs.setGravity(Gravity.CENTER_VERTICAL);int toolbarWidth=homeWindowWidthDp()>=440?(int)(homeWindowWidthDp()*(1f-Math.max(0.30f,Math.min(0.65f,prefs.getFloat("board_split_ratio",0.5f))))):getResources().getConfiguration().screenWidthDp;boolean narrowToolbar=toolbarWidth<370;int toolbarIconWidth=narrowToolbar?36:43;TextView notes=text(toolbarWidth<300?"✎":"✎ Nota",narrowToolbar?12:13);TextView books=text(toolbarWidth<300?"▤":"▤ Maktabah",narrowToolbar?12:13);notes.setSingleLine(true);books.setSingleLine(true);notes.setAutoSizeTextTypeUniformWithConfiguration(9,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);books.setAutoSizeTextTypeUniformWithConfiguration(9,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);notes.setGravity(Gravity.CENTER);books.setGravity(Gravity.CENTER);notes.setTypeface(null,maktabah?android.graphics.Typeface.NORMAL:android.graphics.Typeface.BOLD);books.setTypeface(null,maktabah?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);notes.setPadding(dp(3),0,dp(3),0);books.setPadding(dp(3),0,dp(3),0);notes.setBackground(rounded(maktabah?0xfff8faf8:0xfff0f3f6,maktabah?0xffe0e6e2:0xffb7c1c9,16));notes.setTextColor(maktabah?0xff65777b:0xff566a70);notes.setContentDescription(maktabah?"Buka Nota":"Nota, bahagian dipilih");books.setBackground(rounded(maktabah?0xfff0f3f6:0xfff8faf8,maktabah?0xffb7c1c9:0xffe0e6e2,16));books.setTextColor(maktabah?0xff566a70:0xff65777b);books.setContentDescription(maktabah?"Maktabah, bahagian dipilih":"Buka Maktabah");LinearLayout.LayoutParams noteTabParams=new LinearLayout.LayoutParams(0,dp(44),1f);noteTabParams.rightMargin=dp(4);tabs.addView(notes,noteTabParams);
 LinearLayout.LayoutParams bookTabParams=new LinearLayout.LayoutParams(0,dp(44),1.25f);bookTabParams.rightMargin=dp(7);tabs.addView(books,bookTabParams);
 TextView quickSearch=text("⌕",23);quickSearch.setGravity(Gravity.CENTER);quickSearch.setTextColor(0xff566a70);quickSearch.setBackground(rounded(0xfff8faf8,0xffdce5df,12));quickSearch.setContentDescription("Cari tajuk, isi atau kategori");applyTouchFeedback(quickSearch);LinearLayout.LayoutParams quickSearchParams=new LinearLayout.LayoutParams(dp(toolbarIconWidth),dp(44));quickSearchParams.rightMargin=dp(5);tabs.addView(quickSearch,quickSearchParams);
 TextView quickAdd=text("✚",20);quickAdd.setGravity(Gravity.CENTER);quickAdd.setTextColor(0xff566a70);quickAdd.setBackground(rounded(0xffeaf4ee,0xffc9dfd0,12));quickAdd.setContentDescription(maktabah?"Kitab baharu":"Nota baharu");quickAdd.setOnClickListener(v->{if(maktabah)showBookAddMenu();else showNoteCreationMenu();});applyTouchFeedback(quickAdd);
 LinearLayout.LayoutParams quickParams=new LinearLayout.LayoutParams(dp(toolbarIconWidth),dp(44));quickParams.rightMargin=dp(5);tabs.addView(quickAdd,quickParams);
 TextView quickCategory=text("⚙",19);quickCategory.setGravity(Gravity.CENTER);quickCategory.setTextColor(0xff596e75);quickCategory.setBackground(rounded(0xfff8faf8,0xffdce5df,12));quickCategory.setContentDescription("Urus kategori");quickCategory.setOnClickListener(v->manageCategories());applyTouchFeedback(quickCategory);
 LinearLayout.LayoutParams categoryButtonParams=new LinearLayout.LayoutParams(dp(toolbarIconWidth),dp(44));categoryButtonParams.rightMargin=dp(5);tabs.addView(quickCategory,categoryButtonParams);
 TextView sort=text("↕",20);sort.setGravity(Gravity.CENTER);sort.setTextColor(0xff566a70);sort.setPadding(dp(4),0,dp(4),0);sort.setBackground(rounded(0xfff8faf8,0xffdce5df,12));sort.setContentDescription("Susun: "+sortMode+". Tekan untuk ubah susunan");applyTouchFeedback(sort);tabs.addView(sort,new LinearLayout.LayoutParams(dp(toolbarIconWidth),dp(44)));listHost.addView(tabs);applyTouchFeedback(notes);applyTouchFeedback(books);notes.setOnClickListener(v->{if(maktabah)openSection(false);});books.setOnClickListener(v->{if(!maktabah)openSection(true);});EditText search=new EditText(this);search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setSelectAllOnFocus(false);search.setMaxLines(1);search.setTextSize(14);search.setHint(maktabah?"Cari tajuk, isi atau kategori kitab":"Cari tajuk, isi atau kategori nota");search.setContentDescription(maktabah?"Carian kitab":"Carian nota");search.setPadding(dp(16),dp(10),dp(16),dp(10));search.setBackground(rounded(0xffffffff,0xffdce7df,12));LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(-1,-2);searchParams.setMargins(dp(12),dp(4),dp(12),dp(2));quickSearch.setOnClickListener(v->{
  if(maktabah){showGlobalBookSearch();return;}
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
 });LinearLayout filters=new LinearLayout(this);filters.setPadding(dp(14),0,dp(14),0);TextView folder=text((maktabah?"▤  Maktabah":"✎  Nota")+"  /",14);folder.setTextColor(0xff60757b);folder.setTypeface(null,android.graphics.Typeface.BOLD);filters.addView(folder);
  if(listHost!=root){
   View boardPane=root.findViewWithTag("board_split_left"),splitDivider=root.findViewWithTag("board_split_divider");
   if(boardPane!=null&&splitDivider!=null){
    TextView toggle=text(boardPane.getVisibility()==View.VISIBLE?"◀":"▶",16);toggle.setGravity(Gravity.CENTER);toggle.setTextColor(0xff216e4e);toggle.setBackground(rounded(0xfff8faf8,0xffdce5df,8));
    toggle.setContentDescription(boardPane.getVisibility()==View.VISIBLE?"Sembunyikan Board":"Munculkan Board");
    LinearLayout.LayoutParams toggleParams=new LinearLayout.LayoutParams(dp(32),dp(36));toggleParams.rightMargin=dp(6);filters.addView(toggle,0,toggleParams);
    applyTouchFeedback(toggle);toggle.setOnClickListener(v->{
     boolean hide=boardPane.getVisibility()==View.VISIBLE;
     boardPane.setVisibility(hide?View.GONE:View.VISIBLE);splitDivider.setVisibility(hide?View.GONE:View.VISIBLE);
     prefs.edit().putBoolean("board_split_hidden",hide).apply();
     toggle.setText(hide?"▶":"◀");toggle.setContentDescription(hide?"Munculkan Board":"Sembunyikan Board");
    });
   }
  }
  Spinner catFilter=new Spinner(this);catFilter.setContentDescription("Tapis "+(maktabah?"kitab":"nota")+" mengikut kategori");ArrayList<String> cats=new ArrayList<>();cats.add("Semua");cats.addAll(categories());if(!cats.contains(categoryFilter))cats.add(categoryFilter);catFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));int ci=cats.indexOf(categoryFilter);catFilter.setSelection(ci<0?0:ci);filters.addView(catFilter,new LinearLayout.LayoutParams(0,-2,1));
  compactSummary=null;
  if(homeWindowWidthDp()<440&&getResources().getConfiguration().screenWidthDp>=600){
   LinearLayout.LayoutParams categoryParams=(LinearLayout.LayoutParams)catFilter.getLayoutParams();
   categoryParams.width=0;categoryParams.weight=1f;catFilter.setLayoutParams(categoryParams);
   compactSummary=new LinearLayout(this);compactSummary.setGravity(Gravity.CENTER_VERTICAL);
   filters.addView(compactSummary,new LinearLayout.LayoutParams(0,-2,2f));
  }
  listHost.addView(filters);
  if(!categoryFilter.equals("Semua")&&!categoryFilter.equals("Tanpa kategori")){
   HorizontalScrollView trailScroll=new HorizontalScrollView(this);trailScroll.setHorizontalScrollBarEnabled(false);
   LinearLayout trail=new LinearLayout(this);trail.setGravity(Gravity.CENTER_VERTICAL);trail.setPadding(dp(14),0,dp(14),0);trailScroll.addView(trail);
   ArrayList<String> chain=new ArrayList<>();for(String n=categoryFilter;!n.isEmpty();n=boardParent(n))chain.add(0,n);
   addBoardTrailLink(trail,maktabah?"Maktabah":"Nota","Semua",false);
   for(String node:chain){TextView sep=text(" › ",13);sep.setTextColor(0xff87958c);trail.addView(sep);addBoardTrailLink(trail,boardLeaf(node),node,node.equals(categoryFilter));}
   listHost.addView(trailScroll,new LinearLayout.LayoutParams(-1,dp(32)));trailScroll.post(()->trailScroll.fullScroll(View.FOCUS_RIGHT));
  }
  sort.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Susunan").setItems(new String[]{"Terbaru","Terlama","Tajuk A–Z","Kerap dibuka"},(d,i)->{sortMode=new String[]{"Terbaru","Terlama","Tajuk A–Z","Kerap dibuka"}[i];sort.setContentDescription("Susun: "+sortMode+". Tekan untuk ubah susunan");list(search.getText().toString());}).show());
  View line=new View(this);line.setBackgroundColor(0xffd0d7de);listHost.addView(line,new LinearLayout.LayoutParams(-1,1));ScrollView sc=new ScrollView(this);listScroll=sc;sc.setOverScrollMode(View.OVER_SCROLL_NEVER);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(true);items=new LinearLayout(this);items.setOrientation(1);items.setPadding(0,0,0,dp(12));sc.addView(items);listHost.addView(sc,new LinearLayout.LayoutParams(-1,0,1));catFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){categoryFilter=cats.get(pos);prefs.edit().putString(maktabah?"board_last_m":"board_last_n",categoryFilter).apply();list(search.getText().toString());}});search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){list(s.toString());}public void afterTextChanged(Editable e){}});list("");
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
 void openSection(boolean books){sectionOpen=true;maktabah=books;categoryFilter=prefs.getString(books?"board_last_m":"board_last_n","Semua");if(!categoryFilter.equals("Semua")&&!categories().contains(categoryFilter)){boolean found=false;for(String c:categories())for(String p=boardParent(c);!p.isEmpty();p=boardParent(p))if(p.equals(categoryFilter))found=true;if(!found)categoryFilter="Semua";}notesPage();}
 String smartPasteText(String value){
  // Conservative cleanup: preserve Arabic harakat, headings, lists and paragraph breaks.
  String clean=value.replace("\r\n","\n").replace('\r','\n').replace('\u00a0',' ');
  clean=clean.replaceAll("[ \t]+(?=\n)","").replaceAll("\n{4,}","\n\n\n");
  return clean;
 }
 // One editable Maktabah posting supplies all home wisdom entries.
 // Each ---HIKMAH--- begins a separate short entry; no extra posts required.
 java.util.ArrayList<String> homeWisdomEntries(){
  final String key="m_hikmah_ringkas";
  if(!prefs.contains(key)&&!prefs.getBoolean("deleted_"+key,false)){
   prefs.edit().putString(key,
    "---HIKMAH---\\nإِنَّ مَعَ الْعُسْرِ يُسْرًا\\nسورة الشرح: ٦\\n\\n"+
    "---HIKMAH---\\nرَبِّ زِدْنِي عِلْمًا\\nسورة طه: ١١٤")
    .putString("title_"+key,"Mutiara Hikmah").putString("cat_"+key,"Kalam Hikmah").apply();
  }
  java.util.ArrayList<String> entries=new java.util.ArrayList<>();
  String source=prefs.getString(key,"");
  for(String section:source.split("---HIKMAH---")){
   String clean=section.replaceAll("(?s)<[^>]*>","").trim();
   if(clean.length()>0&&clean.length()<=280)entries.add(clean);
  }
  if(entries.isEmpty())entries.add("Tambah teks Arab melalui posting Mutiara Hikmah dalam Maktabah.");
  return entries;
 }
 void setHomeWisdomText(TextView target,String entry){
  // Home preview shows Arabic only. Keep translation and source intact in the editable Maktabah post.
  StringBuilder arabic=new StringBuilder();
  for(String line:entry.split("\\r?\\n")){
   String clean=line.trim();
   if(clean.isEmpty())continue;
   // Stop at the first non-Arabic line (Malay translation or source metadata).
   if(!clean.matches(".*[\u0600-\u06FF].*"))break;
   if(arabic.length()>0)arabic.append('\n');
   arabic.append(clean);
  }
  target.setText(arabic.length()>0?arabic.toString():entry.split("\\r?\\n",2)[0].trim());
 }
 class WisdomGlowFrame extends android.graphics.drawable.Drawable {
  final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  @Override public void draw(android.graphics.Canvas canvas){
   android.graphics.Rect b=getBounds();if(b.width()<2||b.height()<2)return;
   float radius=dp(11);android.graphics.RectF rect=new android.graphics.RectF(b.left+1,b.top+1,b.right-1,b.bottom-1);
   paint.setShader(null);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(0x55fdfcf7);canvas.drawRoundRect(rect,radius,radius,paint);
   paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(1f);paint.setColor(0xffd8c69c);canvas.drawRoundRect(rect,radius,radius,paint);
   float phase=(android.os.SystemClock.uptimeMillis()%8500L)/8500f;
   android.graphics.Path path=new android.graphics.Path();path.addRoundRect(rect,radius,radius,android.graphics.Path.Direction.CW);
   android.graphics.PathMeasure measure=new android.graphics.PathMeasure(path,true);float length=measure.getLength();float[] pos=new float[2];
   float glowRadius=dp(62);
   for(int i=0;i<4;i++){
    float offset=((phase+i*0.25f)%1f)*length;measure.getPosTan(offset,pos,null);
    int[] colors=i%2==0?new int[]{0xff07864d,0xcc35b77b,0x0035b77b}:new int[]{0xffc58b0b,0xeef3c342,0x00f3c342};
    paint.setShader(new android.graphics.RadialGradient(pos[0],pos[1],glowRadius,colors,new float[]{0f,0.42f,1f},android.graphics.Shader.TileMode.CLAMP));
    paint.setStrokeWidth(Math.max(1.5f,dp(1)*1.15f));canvas.drawPath(path,paint);
   }
   paint.setShader(null);
   if(isVisible()&&getCallback()!=null)scheduleSelf(tick,android.os.SystemClock.uptimeMillis()+50L);
  }
  final Runnable tick=()->invalidateSelf();
  @Override public boolean setVisible(boolean visible,boolean restart){if(!visible)unscheduleSelf(tick);boolean changed=super.setVisible(visible,restart);if(visible)invalidateSelf();return changed;}
  @Override public void setAlpha(int a){paint.setAlpha(a);}
  @Override public void setColorFilter(android.graphics.ColorFilter f){paint.setColorFilter(f);}
  @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
 class PostingOutline extends android.graphics.drawable.Drawable {
  private final boolean book;
  private final android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  PostingOutline(boolean book){this.book=book;p.setColor(0xff536a5c);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(1.35f);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setStrokeJoin(android.graphics.Paint.Join.ROUND);}
  @Override public void draw(android.graphics.Canvas c){android.graphics.Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);android.graphics.Path q=new android.graphics.Path();
   if(book){q.moveTo(12,5);q.cubicTo(8,2,4,3,2,4);q.lineTo(2,19);q.cubicTo(6,17,9,18,12,20);q.cubicTo(15,18,18,17,22,19);q.lineTo(22,4);q.cubicTo(18,3,15,2,12,5);q.close();c.drawPath(q,p);c.drawLine(12,5,12,20,p);}
   else{q.moveTo(5,3);q.lineTo(14,3);q.lineTo(19,8);q.lineTo(19,12);q.moveTo(19,8);q.lineTo(14,8);q.lineTo(14,3);q.moveTo(5,3);q.lineTo(5,21);q.lineTo(12,21);c.drawPath(q,p);c.drawLine(8,10,12,10,p);c.drawLine(8,14,12,14,p);q.reset();q.moveTo(13,19);q.lineTo(14,15);q.lineTo(20,9);q.lineTo(23,12);q.lineTo(17,18);q.close();c.drawPath(q,p);}
   c.restore();}
  @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}
  @Override public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);invalidateSelf();}
  @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
 class LivingGradient extends android.graphics.drawable.Drawable {
  float phase=0f;final android.graphics.Paint basePaint=new android.graphics.Paint(); {basePaint.setColor(0xfff7f8f4);} final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  @Override public void draw(android.graphics.Canvas canvas){
   android.graphics.Rect b=getBounds();float w=b.width(),h=b.height();if(w<=0||h<=0)return;
   canvas.drawRect(b,basePaint);
   // V2: visible, slow-moving emerald and gold orbs; content remains above the background.
   float radius=Math.max(w*0.82f,h*0.48f);
   float theta=phase*(float)Math.PI*2f;
   float x=w*(0.45f+0.48f*(float)Math.sin(theta));
   float y=h*(0.28f+0.24f*(float)Math.cos(theta));
   paint.setColor(0xffffffff);paint.setShader(new android.graphics.RadialGradient(x,y,radius,new int[]{0xd973cba2,0x7875cda9,0x2275cda9,0x0075cda9},new float[]{0f,0.30f,0.68f,1f},android.graphics.Shader.TileMode.CLAMP));canvas.drawRect(b,paint);
   float goldX=w*(0.55f-0.45f*(float)Math.sin(theta));
   float goldY=h*(0.64f+0.24f*(float)Math.cos(theta));
   paint.setShader(new android.graphics.RadialGradient(goldX,goldY,radius*0.85f,new int[]{0xd9ffe59a,0x78fff2c8,0x00fff2c8},null,android.graphics.Shader.TileMode.CLAMP));canvas.drawRect(b,paint);paint.setShader(null);
   // Static, subtle mosque silhouette in the shared lower background.
   canvas.save();canvas.translate(w*0.72f,h*0.99f);float u=Math.min(w,h)*0.12f;paint.setColor(0x23619b80);canvas.drawRect(-u,-u*0.55f,u,0,paint);android.graphics.Path dome=new android.graphics.Path();dome.moveTo(-u,-u*0.55f);dome.cubicTo(-u,-u*1.12f,-u*0.32f,-u*1.12f,0,-u*1.45f);dome.cubicTo(u*0.32f,-u*1.12f,u,-u*1.12f,u,-u*0.55f);dome.close();canvas.drawPath(dome,paint);for(int side:new int[]{-1,1}){float cx=side*u*1.45f;canvas.drawRect(cx-u*0.12f,-u*1.4f,cx+u*0.12f,0,paint);android.graphics.Path tip=new android.graphics.Path();tip.moveTo(cx-u*0.17f,-u*1.4f);tip.lineTo(cx,-u*1.75f);tip.lineTo(cx+u*0.17f,-u*1.4f);tip.close();canvas.drawPath(tip,paint);}canvas.restore();
  }
  @Override public void setAlpha(int alpha){} @Override public void setColorFilter(android.graphics.ColorFilter filter){} @Override public int getOpacity(){return android.graphics.PixelFormat.OPAQUE;}
 }
 void home(){
  items=null;sectionOpen=false;editor=null;titleEditor=null;categoryPicker=null;selected=null;maktabah=false;
  base("DinNote");
  // V4: extend the animated emerald aura behind the home header only.
  final View homeHeader=root.getChildAt(0);
  final LivingGradient homeBackground=new LivingGradient();
  root.setBackground(homeBackground);
  homeHeader.setBackgroundColor(Color.TRANSPARENT);
  root.getChildAt(1).setBackgroundColor(Color.TRANSPARENT);
  final boolean splitHome=homeWindowWidthDp()>=440;
  LinearLayout dashboardHost=root;
  if(splitHome){
   LinearLayout split=new LinearLayout(this);root.addView(split,new LinearLayout.LayoutParams(-1,0,1));
   ScrollView boardScroll=new ScrollView(this);split.addView(boardScroll,new LinearLayout.LayoutParams(0,-1,1));
   LinearLayout boardPanel=new LinearLayout(this);boardPanel.setOrientation(1);boardPanel.setPadding(dp(8),dp(8),dp(1),dp(8));boardScroll.addView(boardPanel);
   renderHomeBoards(boardPanel,false);renderHomeBoards(boardPanel,true);
   View separator=new View(this);separator.setBackgroundColor(Color.TRANSPARENT);split.addView(separator,new LinearLayout.LayoutParams(dp(1),-1));
   dashboardHost=new LinearLayout(this);dashboardHost.setOrientation(1);split.addView(dashboardHost,new LinearLayout.LayoutParams(0,-1,1));
  }
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
  scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setSmoothScrollingEnabled(true);
  dashboardHost.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);
  page.setPadding(dp(splitHome?2:10),dp(6),dp(10),dp(3));page.setBackgroundColor(Color.TRANSPARENT);page.setTag("dinnote_home");
  android.animation.ValueAnimator homeGlow=android.animation.ValueAnimator.ofFloat(0f,1f);homeGlow.setDuration(4500L);homeGlow.setRepeatCount(android.animation.ValueAnimator.INFINITE);homeGlow.setRepeatMode(android.animation.ValueAnimator.RESTART);homeGlow.setInterpolator(new android.view.animation.LinearInterpolator());homeGlow.addUpdateListener(a->{if(page.isAttachedToWindow()){float phase=(float)a.getAnimatedValue();homeBackground.phase=phase;homeBackground.invalidateSelf();}});page.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){if(!homeGlow.isStarted())homeGlow.start();}public void onViewDetachedFromWindow(View v){homeGlow.cancel();}});
  // Use the actual measured dashboard height. Avoid scaleY, synthetic spacer
  // and fixed-height wrapper: all three created a scrollable empty tail.
  scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
// Rebuild Home when the measured window crosses the Fold6 column breakpoint.
   final boolean builtWide=homeWindowWidthDp()>=440;
   root.addOnLayoutChangeListener((view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
    if(view!=root||root.findViewWithTag("dinnote_home")==null||isFinishing()||isDestroyed())return;
    int measuredDp=Math.round((right-left)/getResources().getDisplayMetrics().density);
    if(measuredDp<=0||(measuredDp>=440)==builtWide)return;
    final LinearLayout currentRoot=root;
    currentRoot.post(()->{
     if(root!=currentRoot||root.findViewWithTag("dinnote_home")==null||isFinishing()||isDestroyed())return;
     int actualDp=Math.round(root.getWidth()/getResources().getDisplayMetrics().density);
     if(actualDp>0&&(actualDp>=440)!=builtWide)home();
    });
   });
  // The frontpage category tiles now begin directly beneath the search controls.
  int noteCount=0,bookCount=0;
  for(String postingKey:prefs.getAll().keySet()){
   if(!(prefs.getAll().get(postingKey) instanceof String)||prefs.getBoolean("deleted_"+postingKey,false))continue;
   if(postingKey.startsWith("n_"))noteCount++;
   else if(postingKey.startsWith("m_"))bookCount++;
  }
  LinearLayout tiles=new LinearLayout(this);tiles.setGravity(Gravity.CENTER_VERTICAL);tiles.setPadding(dp(splitHome?2:10),dp(2),dp(10),dp(3));tiles.setBackgroundColor(Color.TRANSPARENT);dashboardHost.addView(tiles,splitHome?0:1,new LinearLayout.LayoutParams(-1,dp(48)));
  for(int j=0;j<2;j++){
   final boolean isBook=j==1;
   LinearLayout tile=new LinearLayout(this);tile.setGravity(Gravity.CENTER_VERTICAL);
   tile.setPadding(dp(5),0,dp(5),0);
   tile.setBackground(homeGlossy(isBook?1:0,12));tile.setElevation(dp(1));
   TextView symbol=dashboardText(isBook?"▤":"✎",20,false);
   symbol.setTextColor(0xff174d37);symbol.setTextSize(15);symbol.setPadding(0,0,dp(3),0);tile.addView(symbol);
   TextView label=dashboardText(isBook?"Maktabah":"Nota",isBook?11:12,true);
   label.setTextColor(0xff174d37);label.setSingleLine(true);label.setAutoSizeTextTypeUniformWithConfiguration(8,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);label.setPadding(0,0,0,0);
   tile.addView(label,new LinearLayout.LayoutParams(0,-2,1));
   TextView count=dashboardText(String.valueOf(isBook?bookCount:noteCount),14,true);
   count.setTextColor(0xff52725e);count.setGravity(Gravity.CENTER);count.setPadding(dp(3),0,dp(3),0);
   count.setBackground(rounded(isBook?0xffe0e5e1:0xffd0e0d5,isBook?0xffd7ded9:0xffcbdcd0,8));
   tile.addView(count,new LinearLayout.LayoutParams(-2,dp(24)));
   LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(38),1);
   tp.rightMargin=dp(4);
   tiles.addView(tile,tp);tile.setFocusable(true);
   tile.setContentDescription((isBook?"Maktabah, ":"Nota, ")+(isBook?bookCount:noteCount)+" topik");
   tile.setOnClickListener(v->openSection(isBook));applyTouchFeedback(tile);
  }
  LinearLayout utilityRow=tiles;
  // Home utilities on a separate row, preserving both actions.
  TextView recentIcon=dashboardText("◷",19,true);
  recentIcon.setGravity(Gravity.CENTER);recentIcon.setTextColor(0xff174d37);
  recentIcon.setBackground(homeGlossy(2,12));
  recentIcon.setContentDescription("Baru dilihat: pilih paparan");
  LinearLayout.LayoutParams recentIconParams=new LinearLayout.LayoutParams(dp(36),dp(38));
  recentIconParams.leftMargin=dp(0);utilityRow.addView(recentIcon,recentIconParams);
  recentIcon.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Baru dilihat","Semua nota","Maktabah"},(d,i)->{if(i==2)openSection(true);else openSection(false);}).show());
  applyTouchFeedback(recentIcon);
  TextView searchIcon=dashboardText("⌕",22,true);
  searchIcon.setGravity(Gravity.CENTER);searchIcon.setTextColor(0xff174d37);
  searchIcon.setBackground(homeGlossy(3,12));
  searchIcon.setContentDescription("Cari nota atau kitab");
  LinearLayout.LayoutParams searchIconParams=new LinearLayout.LayoutParams(dp(36),dp(38));
  searchIconParams.leftMargin=dp(4);utilityRow.addView(searchIcon,searchIconParams);
  searchIcon.setOnClickListener(v->{
   EditText queryInput=new EditText(this);
   queryInput.setSingleLine(true);queryInput.setTextSize(15);queryInput.setHint("Cari nota atau kitab");
   queryInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
   LinearLayout searchContainer=new LinearLayout(this);searchContainer.setPadding(dp(16),dp(4),dp(16),0);
   searchContainer.addView(queryInput,new LinearLayout.LayoutParams(-1,-2));
   new AlertDialog.Builder(this).setTitle("Carian").setView(searchContainer)
    .setPositiveButton("Cari",(dialog,which)->{
     String q=queryInput.getText().toString().trim();
     if(!q.isEmpty())showAllSearchResults(q);
    }).setNegativeButton("Batal",null).show();
  });
  applyTouchFeedback(searchIcon);
  // Compact GitHub-style shortcuts, without changing the approved header.
  LinearLayout shortcuts=new LinearLayout(this);shortcuts.setOrientation(1);
  shortcuts.setPadding(0,0,0,dp(1));page.addView(shortcuts);
  if(false){
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
   dashboardSection(pinnedColumn,"Nota Disemat",dashboardKeys("pinned_",true),true);
   dashboardSection(updatedColumn,"Kemaskini Terakhir",dashboardKeys("updated_",false),false);
  }else{
   dashboardSection(shortcuts,"Nota Disemat",dashboardKeys("pinned_",true),true);
   dashboardSection(shortcuts,"Kemaskini Terakhir",dashboardKeys("updated_",false),false);
  }
  String resume=prefs.getString("last_edited_key","");
  // Resume editing is available as a compact icon between the home actions.

  // Match the neutral "Terakhir dikemaskini" section rather than the darker pinned style.
  LinearLayout latestHeader=new LinearLayout(this);
  latestHeader.setGravity(Gravity.CENTER_VERTICAL);
  android.graphics.drawable.GradientDrawable latestFill=new android.graphics.drawable.GradientDrawable(
   android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
   new int[]{0xfffafffb,0xffddece2});
  latestFill.setCornerRadius(dp(8));
  latestHeader.setBackground(latestFill);
  View latestAccent=new View(this);latestAccent.setBackgroundColor(0xffc6ad70);
  LinearLayout.LayoutParams latestAccentParams=new LinearLayout.LayoutParams(dp(1),dp(15));
  latestAccentParams.leftMargin=dp(5);latestHeader.addView(latestAccent,latestAccentParams);
  boolean latestCollapsed=prefs.getBoolean("home_latest_collapsed",false);
  TextView latestHeading=dashboardText("Terkini Dimasukkan  "+(latestCollapsed?"▸":"▾"),15,true);
  latestHeading.setTextColor(0xff365c48);
  latestHeading.setIncludeFontPadding(false);
  latestHeading.setPadding(dp(8),dp(6),dp(8),dp(homeWindowWidthDp()<440?6:8));
  ImageView latestIcon=new ImageView(this);latestIcon.setImageResource(R.drawable.home_icon_history);
  LinearLayout.LayoutParams latestIconParams=new LinearLayout.LayoutParams(dp(16),dp(16));latestIconParams.leftMargin=dp(9);latestIconParams.rightMargin=dp(2);latestHeader.addView(latestIcon,latestIconParams);
  latestHeader.addView(latestHeading,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout.LayoutParams latestHeadingParams=new LinearLayout.LayoutParams(-1,-2);
  latestHeadingParams.topMargin=dp(2);latestHeadingParams.bottomMargin=dp(2);
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setClipToPadding(false);
  card.setBackground(homeInsetSurface(0xfffdfefe,0xfffdfefe,0xffc1ccc8,10));
  card.setPadding(dp(3),0,dp(3),0);
  page.addView(card,new LinearLayout.LayoutParams(-1,-2));
  card.addView(latestHeader,latestHeadingParams);
  latestHeader.setContentDescription("Terkini Dimasukkan, ketik untuk buka atau lipat");
  latestHeader.setFocusable(true);applyTouchFeedback(latestHeader);
  latestHeader.setOnClickListener(v->{prefs.edit().putBoolean("home_latest_collapsed",!latestCollapsed).apply();home();});
  LinearLayout recentNotes=new LinearLayout(this);recentNotes.setOrientation(1);
  recentNotes.setPadding(0,0,0,0);card.addView(recentNotes);
  recentNotes.setVisibility(latestCollapsed?View.GONE:View.VISIBLE);
  ArrayList<String> keys=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if((k.startsWith("n_")||k.startsWith("m_"))&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&prefs.getLong("viewed_"+k,0L)>0L)keys.add(k);
  Collections.sort(keys,(firstKey,secondKey)->{int order=Long.compare(prefs.getLong("viewed_"+secondKey,0L),prefs.getLong("viewed_"+firstKey,0L));return order!=0?order:firstKey.compareTo(secondKey);});
  TextView latestCount=dashboardText(Math.min(5,keys.size())+" Topik",10,true);
  latestCount.setTextColor(0xff416653);latestCount.setGravity(Gravity.CENTER);
  latestCount.setPadding(dp(5),dp(3),dp(5),dp(3));
  latestCount.setBackground(homeGlossy(0,20));
  LinearLayout.LayoutParams latestCountParams=new LinearLayout.LayoutParams(-2,-2);
  latestCountParams.rightMargin=dp(5);
  latestHeader.addView(latestCount,latestCountParams);
  latestCount.setOnClickListener(v->latestHeader.performClick());
  if(keys.isEmpty()){
   TextView create=dashboardText("＋  Cipta nota pertama",13,true);create.setGravity(Gravity.CENTER);create.setMinHeight(dp(48));create.setContentDescription("Cipta nota baharu");applyTouchFeedback(create);create.setOnClickListener(v->{maktabah=false;edit(null);});recentNotes.addView(create);
   TextView emblem=dashboardText("☪\n▤",36,true);emblem.setTextColor(0xff566a70);
   emblem.setGravity(Gravity.CENTER);recentNotes.addView(emblem);
   TextView empty=dashboardText("Belum ada nota atau kitab dilihat",18,true);
   empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff4e6269);empty.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);empty.setPadding(dp(8),dp(8),dp(8),dp(4));recentNotes.addView(empty);
   TextView hint=dashboardText("Nota atau kitab yang dibuka akan muncul di sini.",14,false);
   hint.setGravity(Gravity.CENTER);hint.setTextColor(0xff656d76);hint.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);hint.setTextSize(13);hint.setPadding(dp(12),dp(6),dp(12),dp(12));recentNotes.addView(hint);
  }else {
   // Two balanced columns of ten on an unfolded Fold6; one column of five on narrow screens.
   boolean wide=false;
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
   for(int i=0;i<Math.min(wide?20:5,keys.size());i++){
    String k=keys.get(i);String t=prefs.getString("title_"+k,"").trim();
    if(t.trim().isEmpty())t="Tanpa tajuk";
    // Light emerald list: slightly larger titles, dates consistently aligned right.
    FrameLayout row=new FrameLayout(this);
    row.setBackground(homeInsetSurface(0xffffffff,0xffffffff,0xffd4ddd7,10));row.setMinimumHeight(dp(42));
    TextView item=dashboardText(t,14,false);
    item.setTextColor(0xff303b42);item.setGravity(Gravity.CENTER_VERTICAL);
    item.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    item.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    item.setPadding(dp(10),dp(4),dp(prefs.getLong("viewed_"+k,0L)>0L?88:10),dp(4));item.setIncludeFontPadding(false);
    item.setMinHeight(dp(42));
    item.setTooltipText(t);item.setContentDescription(t);
    item.setSingleLine(true);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
    item.setPadding(dp(33),dp(4),dp(prefs.getLong("viewed_"+k,0L)>0L?88:10),dp(4));
    row.addView(item,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL));
    android.widget.ImageView dashboardRowIcon=new android.widget.ImageView(this);
    dashboardRowIcon.setImageDrawable(new PostingOutline(k.startsWith("m_")));
    dashboardRowIcon.setColorFilter(0xff536a5c);
    dashboardRowIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
    FrameLayout.LayoutParams dashboardRowIconParams=new FrameLayout.LayoutParams(dp(17),dp(17),Gravity.LEFT|Gravity.CENTER_VERTICAL);
    dashboardRowIconParams.leftMargin=dp(7);row.addView(dashboardRowIcon,dashboardRowIconParams);
    item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    long when=prefs.getLong("viewed_"+k,0L);
    if(when>0L){
     TextView date=dashboardText(new java.text.SimpleDateFormat("dd MMM",new java.util.Locale("ms","MY")).format(new java.util.Date(when)),9,false);
     date.setTextColor(0xff416653);date.setGravity(Gravity.CENTER);date.setSingleLine(true);date.setTextDirection(View.TEXT_DIRECTION_LTR);
     android.graphics.drawable.GradientDrawable dateFrame=homeGlossy(new int[]{0,1,3}[i%3],20);dateFrame.setStroke(Math.max(1,Math.round(getResources().getDisplayMetrics().density*0.7f)),0xffc5ae76);date.setBackground(dateFrame);date.setTextColor(0xff174d37);date.setIncludeFontPadding(false);date.setMaxLines(1);date.setEllipsize(android.text.TextUtils.TruncateAt.END);
     date.setPadding(dp(4),dp(1),dp(4),dp(1));
     FrameLayout.LayoutParams dateParams=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.CENTER_VERTICAL);
     dateParams.rightMargin=dp(8);row.addView(date,dateParams);date.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);date.setContentDescription("Dilihat pada "+new java.text.SimpleDateFormat("dd MMMM",new java.util.Locale("ms","MY")).format(new java.util.Date(when)));
    }
    LinearLayout.LayoutParams itemParams=new LinearLayout.LayoutParams(-1,-2);
    itemParams.bottomMargin=dp(2);
    itemParams.leftMargin=0;itemParams.rightMargin=0;
    if(wide)(i<10?left:right).addView(row,itemParams);else recentNotes.addView(row,itemParams);
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
  add.setBackground(homeGlossy(0,12));add.setTextColor(0xff174d37);
  add.setPadding(dp(12),dp(8),dp(12),dp(8));
  // Fixed home footer: keep the primary action visible above Android navigation,
  // independent of dashboard scrolling and window size.
  LinearLayout footer=new LinearLayout(this);
  footer.setOrientation(LinearLayout.VERTICAL);
  footer.setPadding(dp(splitHome?2:10),dp(3),dp(10),dp(4));
  footer.setBackgroundColor(Color.TRANSPARENT);
  footer.setElevation(0);
  dashboardHost.addView(footer,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(42));
  LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
  // Compact Hikmah Ringkas preview: fixed above the home actions on the folded screen only.
  // Use existing sample wisdom text for layout testing; do not touch user notes.
  if(true){
   final java.util.ArrayList<String> sampleWisdom=homeWisdomEntries();
   LinearLayout wisdomCard=new LinearLayout(this);wisdomCard.setOrientation(LinearLayout.VERTICAL);
   wisdomCard.setPadding(dp(12),dp(1),dp(12),dp(4));
   final int[] wisdomBorders={0xffd8c69c,0xffb7d2be,0xffd4c5df};
   final int wisdomStyle=new java.util.Random().nextInt(wisdomBorders.length);
   wisdomCard.setBackground(new WisdomGlowFrame());
   wisdomCard.setClipToOutline(true);
   wisdomCard.setClipChildren(true);
   LinearLayout.LayoutParams wisdomParams=new LinearLayout.LayoutParams(-1,dp(126));
   wisdomParams.bottomMargin=dp(5);footer.addView(wisdomCard,wisdomParams);
   TextView wisdomHeading=dashboardText("❀  Mutiara Hikmah  ❀",14,true);
   wisdomHeading.setTextColor(0xff47745d);wisdomHeading.setGravity(Gravity.CENTER);wisdomHeading.setIncludeFontPadding(false);
   wisdomCard.addView(wisdomHeading);
   TextView wisdomBody=dashboardText("",15,false);wisdomBody.setTextColor(0xff35453c);wisdomBody.setPadding(0,0,0,0);wisdomBody.setLineSpacing(0,1.0f);
   // Rotate available Android Arabic typeface styles until dedicated font assets are bundled.
   String[] wisdomFamilies={"serif","sans-serif"};
   String family=wisdomFamilies[new java.util.Random().nextInt(wisdomFamilies.length)];
   wisdomBody.setTypeface(android.graphics.Typeface.create(family,android.graphics.Typeface.NORMAL));
   wisdomBody.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);wisdomBody.setIncludeFontPadding(false);wisdomBody.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   ScrollView wisdomScroll=new ScrollView(this);
   wisdomScroll.setFillViewport(false);
   wisdomScroll.setVerticalScrollBarEnabled(false);
   wisdomScroll.setNestedScrollingEnabled(true);
   LinearLayout.LayoutParams wisdomTextParams=new LinearLayout.LayoutParams(-1,0,1);
   wisdomTextParams.topMargin=dp(0);wisdomCard.addView(wisdomScroll,wisdomTextParams);
   wisdomScroll.addView(wisdomBody,new ScrollView.LayoutParams(-1,-2));
   final int[] wisdomIndex={new java.util.Random().nextInt(sampleWisdom.size())};
   final long[] wisdomLastTap={0L};
   View.OnClickListener wisdomTap=v->{
    long now=android.os.SystemClock.uptimeMillis();
    if(now-wisdomLastTap[0]<350L){
     wisdomLastTap[0]=0L;
     maktabah=true;read("m_hikmah_ringkas");
    }else{
     wisdomLastTap[0]=now;
     wisdomIndex[0]=(wisdomIndex[0]+1)%sampleWisdom.size();
     setHomeWisdomText(wisdomBody,sampleWisdom.get(wisdomIndex[0]));
    }
   };
   android.view.GestureDetector wisdomGestures=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
    @Override public boolean onDown(android.view.MotionEvent e){return true;}
    @Override public boolean onSingleTapConfirmed(android.view.MotionEvent e){
     wisdomIndex[0]=(wisdomIndex[0]+1)%sampleWisdom.size();
     setHomeWisdomText(wisdomBody,sampleWisdom.get(wisdomIndex[0]));
     wisdomScroll.scrollTo(0,0);
     return true;
    }
    @Override public boolean onDoubleTap(android.view.MotionEvent e){maktabah=true;read("m_hikmah_ringkas");return true;}
    @Override public void onLongPress(android.view.MotionEvent e){maktabah=true;edit("m_hikmah_ringkas");}
   });
   wisdomScroll.setOnTouchListener((v,event)->{
    if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)
     v.getParent().requestDisallowInterceptTouchEvent(true);
    wisdomGestures.onTouchEvent(event);
    if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP||event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)
     v.getParent().requestDisallowInterceptTouchEvent(false);
    return false;
   });
   setHomeWisdomText(wisdomBody,sampleWisdom.get(wisdomIndex[0]));
   wisdomCard.setContentDescription("Mutiara Hikmah. Ketik untuk petikan lain. Ketik dua kali untuk buka topik. Tekan lama untuk edit posting");
   wisdomCard.setOnClickListener(wisdomTap);
   wisdomCard.setOnLongClickListener(v->{maktabah=true;edit("m_hikmah_ringkas");return true;});
   applyTouchFeedback(wisdomCard);
  }
  footer.addView(actions,new LinearLayout.LayoutParams(-1,dp(38)));
  LinearLayout.LayoutParams noteActionParams=new LinearLayout.LayoutParams(0,-1,1);noteActionParams.rightMargin=dp(4);
  actions.addView(add,noteActionParams);
  String resumeKey=prefs.getString("last_edited_key","");
  if(!resumeKey.isEmpty()&&prefs.contains(resumeKey)&&!prefs.getBoolean("deleted_"+resumeKey,false)){
   TextView resumeIcon=dashboardText("✎",19,true);
   resumeIcon.setGravity(Gravity.CENTER);
   resumeIcon.setTextColor(0xff47745d);
   resumeIcon.setBackground(homeGlossy(0,12));
   LinearLayout.LayoutParams resumeIconParams=new LinearLayout.LayoutParams(dp(38),-1);
   resumeIconParams.leftMargin=dp(3);resumeIconParams.rightMargin=dp(3);
   actions.addView(resumeIcon,resumeIconParams);
   resumeIcon.setContentDescription("Sambung menulis");
   resumeIcon.setTooltipText("Sambung menulis");
   resumeIcon.setOnClickListener(v->{maktabah=resumeKey.startsWith("m_");edit(resumeKey);});
   applyTouchFeedback(resumeIcon);
  }
  TextView addBook=dashboardText("＋  Kitab baharu",15,true);
  addBook.setGravity(Gravity.CENTER);addBook.setTextColor(0xff365c48);
  addBook.setBackground(homeGlossy(0,12));
  LinearLayout.LayoutParams bookActionParams=new LinearLayout.LayoutParams(0,-1,1);bookActionParams.leftMargin=dp(4);
  actions.addView(addBook,bookActionParams);
  addBook.setContentDescription("Cipta kitab baharu");addBook.setOnClickListener(v->{maktabah=true;edit(null);});applyTouchFeedback(addBook);
  add.setContentDescription("Cipta nota baharu");add.setFocusable(true);
  add.setOnClickListener(v->{maktabah=false;showNoteCreationMenu();});
  applyTouchFeedback(add);
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
    else if(i==1){prefs.edit().putBoolean("pinned_"+key,!pinned).putLong("pinned_at_"+key,!pinned?System.currentTimeMillis():0L).apply();home();}
    else if(i==2)duplicateNote(key);
    else confirmDelete(key);
   }).show();
 }
 void showAllSearchResults(String query){
  String needle=normalizeCompareText(query);if(needle.isEmpty())return;
  ArrayList<String> keys=new ArrayList<>(),labels=new ArrayList<>();
  for(java.util.Map.Entry<String,?> entry:prefs.getAll().entrySet()){String k=entry.getKey();if(!(k.startsWith("n_")||k.startsWith("m_"))||!(entry.getValue() instanceof String)||prefs.getBoolean("deleted_"+k,false))continue;String title=prefs.getString("title_"+k,"Tanpa tajuk"),body=(String)entry.getValue();if(!normalizeCompareText(title+" "+body).contains(needle))continue;keys.add(k);labels.add((k.startsWith("m_")?"▤ ":"✎ ")+title);}
  if(keys.isEmpty()){new AlertDialog.Builder(this).setTitle("Hasil carian").setMessage("Tiada padanan").setPositiveButton("Tutup",null).show();return;}
  new AlertDialog.Builder(this).setTitle("Hasil carian ("+keys.size()+")").setItems(labels.toArray(new String[0]),(d,i)->{String k=keys.get(i);maktabah=k.startsWith("m_");if(normalizeCompareText(prefs.getString(k,"")).contains(needle))searchInsideReader(k,query);else read(k);}).setNegativeButton("Tutup",null).show();
 }
 void showHomeSearchResults(String query){
  int hits=0;String first="";
  for(String k:prefs.getAll().keySet())if(k.startsWith("n_")||k.startsWith("m_")){
   String title=prefs.getString("title_"+k,""),body=prefs.getString(k,"");
   if(normalizeCompareText(title+" "+body).contains(normalizeCompareText(query))){hits++;first=title;}
  }
  // Results count is announced without replacing the approved home screen.
  if(hits==1)android.widget.Toast.makeText(this,"Ditemui: "+first,android.widget.Toast.LENGTH_SHORT).show();
 }
 long pinOrderTime(String key){
  long pinnedAt=prefs.getLong("pinned_at_"+key,0L);
  if(pinnedAt>0L)return pinnedAt;
  // Existing pinned notes predate pin timestamps: use last edit time as fallback.
  return prefs.getLong("updated_"+key,0L);
 }
 ArrayList<String> dashboardKeys(String field,boolean includeBooks){
  ArrayList<String> out=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if((k.startsWith("n_")||(includeBooks&&k.startsWith("m_")))&&
    prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&
    (field.equals("pinned_")?prefs.getBoolean(field+k,false):prefs.getLong(field+k,0L)>0))out.add(k);
  Collections.sort(out,(a,b)->{
   int comparison=field.equals("pinned_")?
    Long.compare(pinOrderTime(b),pinOrderTime(a)):
    Long.compare(prefs.getLong(field+b,0),prefs.getLong(field+a,0));
   return comparison!=0?comparison:a.compareTo(b);
  });
  return out;
 }
 android.graphics.drawable.Drawable homeInsetSurface(int top,int bottom,int border,int radius){
  android.graphics.drawable.GradientDrawable surface=isGoldBorder(border)?new GoldGlowDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom},radius):new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
  surface.setCornerRadius(dp(radius));
  surface.setStroke(Math.max(1,Math.round(0.5f*getResources().getDisplayMetrics().density)),border);
  android.graphics.drawable.GradientDrawable inner=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x08000000,0x00000000,0x08ffffff});
  inner.setCornerRadius(dp(radius));
  android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{surface,inner});
  return layers;
 }
 android.graphics.drawable.Drawable dashboardTileSurface(boolean pearl){
  return homeInsetSurface(0xfffdfefe,0xfffdfefe,0xffc1ccc8,10);
 }
 int homeWindowWidthDp(){
  // Configuration width tracks the current Android multi-window bounds.
  // root/decor measured widths can still describe the previous full-screen
  // layout while onConfigurationChanged is rebuilding the dashboard.
  int configuredWidthDp=getResources().getConfiguration().screenWidthDp;
  if(configuredWidthDp>0)return configuredWidthDp;
  int widthPx=root!=null&&root.getWidth()>0?root.getWidth():getWindow().getDecorView().getWidth();
  return widthPx>0?Math.round(widthPx/getResources().getDisplayMetrics().density):0;
 }
 void dashboardSection(LinearLayout parent,String heading,ArrayList<String> keys,boolean pin){
  LinearLayout target=new LinearLayout(this);target.setOrientation(LinearLayout.VERTICAL);
  target.setBackground(homeInsetSurface(0xfffdfefe,0xfffdfefe,0xffc1ccc8,10));
  target.setPadding(dp(3),0,dp(3),0);
  LinearLayout.LayoutParams outerParams=new LinearLayout.LayoutParams(-1,-2);
  outerParams.topMargin=dp(2);outerParams.bottomMargin=dp(1);
  parent.addView(target,outerParams);
  // Keep folded rows compact; unfolded columns retain comfortable touch targets.
  boolean folded=homeWindowWidthDp()<440;
  boolean collapsed=prefs.getBoolean(pin?"home_pinned_collapsed":"home_updated_collapsed",false);
  TextView title=dashboardText(heading+"  "+(collapsed?"▸":"▾"),15,true);
  title.setTextColor(0xff365c48);
  title.setElevation(0);title.setPadding(dp(8),dp(folded?6:8),0,dp(folded?6:8));title.setIncludeFontPadding(false);title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setContentDescription(heading+", ketik untuk buka atau lipat");title.setTooltipText(heading);title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-1,-2);titleParams.topMargin=dp(2);titleParams.bottomMargin=dp(2);
  LinearLayout headingRow=new LinearLayout(this);headingRow.setGravity(Gravity.CENTER_VERTICAL);
  android.graphics.drawable.GradientDrawable headingFill=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xfffafffb,0xffddece2});
  headingFill.setCornerRadius(dp(8));headingRow.setBackground(headingFill);headingRow.setPadding(0,0,dp(5),0);
  View goldAccent=new View(this);goldAccent.setBackgroundColor(0xffc6ad70);
  LinearLayout.LayoutParams accentParams=new LinearLayout.LayoutParams(dp(1),dp(15));accentParams.leftMargin=dp(5);headingRow.addView(goldAccent,accentParams);
  ImageView sectionIcon=new ImageView(this);sectionIcon.setImageResource(pin?R.drawable.home_icon_pin:R.drawable.home_icon_clock);
  LinearLayout.LayoutParams sectionIconParams=new LinearLayout.LayoutParams(dp(16),dp(16));sectionIconParams.leftMargin=dp(9);sectionIconParams.rightMargin=dp(2);headingRow.addView(sectionIcon,sectionIconParams);
  headingRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  TextView topicCount=dashboardText(Math.min(3,keys.size())+" Topik",10,true);
  topicCount.setTextColor(0xff416653);topicCount.setGravity(Gravity.CENTER);topicCount.setPadding(dp(5),dp(3),dp(5),dp(3));
  topicCount.setBackground(homeGlossy(0,20));
  headingRow.addView(topicCount,new LinearLayout.LayoutParams(-2,-2));target.addView(headingRow,titleParams);
  headingRow.setOnClickListener(v->title.performClick());topicCount.setOnClickListener(v->title.performClick());
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
  for(int i=0;i<Math.min(3,keys.size());i++){
   String key=keys.get(i),label=prefs.getString("title_"+key,"").trim();
   if(label.isEmpty()){
    String body=prefs.getString(key,"").trim();
    if(!body.isEmpty())label=body.split("\\n",2)[0].trim();
   }
   String entryTitle=label.isEmpty()?"Tanpa tajuk":label;
   TextView entry=dashboardText(entryTitle,14,false);
   entry.setTextColor(0xff303b42);entry.setGravity(Gravity.CENTER_VERTICAL);
   entry.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   entry.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
   entry.setSingleLine(true);entry.setEllipsize(android.text.TextUtils.TruncateAt.END);
   entry.setIncludeFontPadding(false);entry.setPadding(0,0,0,0);entry.setMinWidth(0);
   LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);line.setGravity(Gravity.CENTER_VERTICAL);
   line.setPadding(dp(7),dp(3),dp(7),dp(3));line.setBackground(homeInsetSurface(0xffffffff,0xffffffff,0xffd4ddd7,10));line.setMinimumHeight(dp(folded?38:42));
   android.widget.ImageView entryIcon=new android.widget.ImageView(this);
   entryIcon.setImageDrawable(new PostingOutline(key.startsWith("m_")));
   entryIcon.setColorFilter(pin?0xff8b6925:0xff536a5c);
   entryIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
   LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(17),dp(17));iconParams.rightMargin=dp(9);
   line.addView(entryIcon,iconParams);
   if(pin)entry.setTextColor(0xff8b6925);
   line.addView(entry,new LinearLayout.LayoutParams(0,-2,1));
   Object rawStamp=prefs.getAll().get("updated_"+key);
   long stamp=rawStamp instanceof Number?((Number)rawStamp).longValue():0L;
   if(stamp>0L){
    java.util.Calendar cal=java.util.Calendar.getInstance();cal.setTimeInMillis(stamp);
    String[] months={"Jan","Feb","Mac","Apr","Mei","Jun","Jul","Ogo","Sep","Okt","Nov","Dis"};
    String date=cal.get(java.util.Calendar.DAY_OF_MONTH)+" "+months[cal.get(java.util.Calendar.MONTH)];
    if(!pin)date+=" · "+new java.text.SimpleDateFormat("HHmm",java.util.Locale.ROOT).format(new java.util.Date(stamp));
    TextView time=dashboardText(date,9,false);time.setTextColor(0xff68756c);time.setSingleLine(true);time.setTextDirection(View.TEXT_DIRECTION_LTR);time.setIncludeFontPadding(false);time.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);time.setPadding(dp(5),0,0,0);
    android.graphics.drawable.GradientDrawable timeFrame=homeGlossy(new int[]{0,1,3}[i%3],20);timeFrame.setStroke(Math.max(1,Math.round(getResources().getDisplayMetrics().density*0.7f)),0xffc5ae76);time.setBackground(timeFrame);
    time.setTextColor(0xff174d37);time.setTypeface(android.graphics.Typeface.DEFAULT);time.setGravity(Gravity.CENTER);time.setPadding(dp(4),dp(1),dp(4),dp(1));
    LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(-2,-2);dateParams.leftMargin=dp(4);
    line.addView(time,dateParams);
   }
   LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(folded?38:42));ep.bottomMargin=dp(3);target.addView(line,ep);applyTouchFeedback(line);
   line.setFocusable(true);line.setContentDescription("Buka "+(key.startsWith("m_")?"kitab ":"nota ")+(label.isEmpty()?"Tanpa tajuk":label));line.setTooltipText(label.isEmpty()?"Tanpa tajuk":label);
   line.setOnClickListener(v->{maktabah=key.startsWith("m_");read(key);});
   line.setOnLongClickListener(v->{showDashboardActions(key);return true;});
  }
 }
 boolean isGoldBorder(int color){
  int r=Color.red(color),g=Color.green(color),b=Color.blue(color);
  return Color.alpha(color)>180&&r>=145&&r>g+5&&g>b+12;
 }
 class GoldGlowDrawable extends android.graphics.drawable.GradientDrawable {
  final int corner;final android.graphics.Paint glow=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  final android.graphics.Path path=new android.graphics.Path();final android.graphics.PathMeasure measure=new android.graphics.PathMeasure();final float[] pos=new float[2];
  final Runnable tick=()->invalidateSelf();
  GoldGlowDrawable(int radius){super();corner=radius;}
  GoldGlowDrawable(Orientation orientation,int[] colors,int radius){super(orientation,colors);corner=radius;}
  @Override public void draw(android.graphics.Canvas canvas){
   super.draw(canvas);android.graphics.Rect b=getBounds();if(b.width()<4||b.height()<4)return;
   float inset=Math.max(1f,dp(1)/2f),radius=Math.min(dp(corner),Math.min(b.width(),b.height())/2f);
   path.reset();android.graphics.RectF frame=new android.graphics.RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);if(getShape()==android.graphics.drawable.GradientDrawable.OVAL)path.addOval(frame,android.graphics.Path.Direction.CW);else path.addRoundRect(frame,radius,radius,android.graphics.Path.Direction.CW);measure.setPath(path,true);
   float length=measure.getLength(),phase=(android.os.SystemClock.uptimeMillis()%8500L)/8500f;
   float reach=Math.max(dp(4),Math.min(dp(62),length*0.12f));
   glow.setStyle(android.graphics.Paint.Style.STROKE);glow.setStrokeWidth(Math.max(1f,dp(1)*(length>dp(240)?1.15f:0.85f)));
   for(int i=0;i<4;i++){measure.getPosTan(((phase+i*0.25f)%1f)*length,pos,null);
    int[] colors=i%2==0?new int[]{0xff07864d,0xcc35b77b,0x0035b77b}:new int[]{0xffc58b0b,0xeef3c342,0x00f3c342};
    glow.setShader(new android.graphics.RadialGradient(pos[0],pos[1],reach,colors,new float[]{0f,0.42f,1f},android.graphics.Shader.TileMode.CLAMP));canvas.drawPath(path,glow);
   }
   glow.setShader(null);if(isVisible()&&getCallback()!=null)scheduleSelf(tick,android.os.SystemClock.uptimeMillis()+80L);
  }
  @Override public boolean setVisible(boolean visible,boolean restart){if(!visible)unscheduleSelf(tick);boolean changed=super.setVisible(visible,restart);if(visible)invalidateSelf();return changed;}
 }
 android.graphics.drawable.GradientDrawable homeGlossy(int index,int radius){
  android.graphics.drawable.GradientDrawable d=new GoldGlowDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xfffffefa,0xfff3faf2,0xffdcefd9},radius);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xffc99a20);return d;
 }
 android.graphics.drawable.GradientDrawable rounded(int fill,int stroke,int radius){
  android.graphics.drawable.GradientDrawable d=isGoldBorder(stroke)?new GoldGlowDrawable(radius):new android.graphics.drawable.GradientDrawable();
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
   
   TextView detail=dashboardText(preview,13,false);detail.setTextColor(0xff57635e);
   detail.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);detail.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
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

 boolean matchesSearch(String content,String[] terms){String normalized=normalizeCompareText(content.replaceAll("\\s+"," "));for(String term:terms)if(!normalized.contains(term))return false;return true;}
 String cleanCardPreview(String raw){
  if(raw==null)return "";
  // Some imported samples contain escaped newline sequences rather than line breaks.
  String cleaned=raw.replace("\\r\\n","\n").replace("\\n","\n").replace("\\r","\n").replace("\\t"," ");
  // Remove entire subtopic heading lines before flattening the card preview.
  cleaned=lectureHeadingPattern().matcher(cleaned).replaceAll("");
  cleaned=cleaned.replaceAll("(?m)^[\\t ]*#+[^\\r\\n]*","");
  cleaned=cleaned.replaceAll("(?i)---(?:MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN|TAMAT|FLOWER|GEOMETRIC|CLASSIC|KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT))---"," ");
  cleaned=cleaned.replaceAll("(?i)</?[biu]>","");
  cleaned=formattedReadingText(cleaned).toString();
  cleaned=cleaned.replaceAll("(?<!\\S)---(?!\\S)"," ").replaceAll("\\s+"," ").trim();
  // Keep logical text order; Android resolves mixed-direction text as a whole.
  return cleaned;
 }
 void chooseBulkPostings(){
  final boolean books=maktabah;
  final String prefix=books?"m_":"n_";
  ArrayList<String> keys=new ArrayList<>();
  java.util.Map<String,?> snapshot=prefs.getAll();
  for(String k:snapshot.keySet())if(k.startsWith(prefix)&&snapshot.get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&(categoryFilter.equals("Semua")||categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori"))))keys.add(k);
  Collections.sort(keys,(a,b)->prefs.getString("title_"+a,"").compareToIgnoreCase(prefs.getString("title_"+b,"")));
  if(keys.isEmpty())return;
  String[] labels=new String[keys.size()];boolean[] checked=new boolean[keys.size()];
  for(int i=0;i<keys.size();i++){String k=keys.get(i);labels[i]=prefs.getString("title_"+k,"Tanpa tajuk")+" — "+prefs.getString("cat_"+k,"Tanpa kategori");}
  AlertDialog picker=new AlertDialog.Builder(this).setTitle("Pilih posting").setMultiChoiceItems(labels,checked,(d,i,on)->checked[i]=on).setNeutralButton("Pilih semua",null).setNegativeButton("Batal",null).setPositiveButton("Seterusnya",(d,w)->{
   ArrayList<String> chosen=new ArrayList<>();for(int i=0;i<keys.size();i++)if(checked[i])chosen.add(keys.get(i));
   if(chosen.isEmpty()){Toast.makeText(this,"Tiada posting dipilih",Toast.LENGTH_SHORT).show();return;}
   new AlertDialog.Builder(this).setTitle(chosen.size()+" posting dipilih").setItems(new String[]{"Pindah","Padam"},(action,which)->{
    if(which==1){
     new AlertDialog.Builder(this).setTitle("Alih ke Tong Sampah").setMessage("Alih "+chosen.size()+" posting ke Tong Sampah? Boleh dipulihkan melalui Tetapan > Tong sampah.").setNegativeButton("Batal",null).setPositiveButton("Padam",(confirm,button)->{
      if(maktabah!=books)return;
      android.content.SharedPreferences.Editor changes=prefs.edit();int removed=0;long now=System.currentTimeMillis();
      for(String k:chosen)if(k.startsWith(prefix)&&prefs.contains(k)&&!prefs.getBoolean("deleted_"+k,false)){changes.putBoolean("deleted_"+k,true).putLong("trashed_at_"+k,now);removed++;}
      if(!changes.commit()){Toast.makeText(this,"Gagal mengalihkan posting",Toast.LENGTH_LONG).show();return;}
      Toast.makeText(this,removed+" posting dialihkan ke Tong Sampah",Toast.LENGTH_SHORT).show();notesPage();
     }).show();return;
    }
   ArrayList<String> destinations=new ArrayList<>();destinations.add("Tanpa kategori");
   for(String cat:categories())if(!destinations.contains(cat)&&!cat.equals("Semua"))destinations.add(cat);
   new AlertDialog.Builder(this).setTitle("Pindah ke Board").setItems(destinations.toArray(new String[0]),(a,i)->{
    String target=destinations.get(i);
    new AlertDialog.Builder(this).setMessage("Pindah "+chosen.size()+" posting ke "+target+"?").setNegativeButton("Batal",null).setPositiveButton("Pindah",(b,j)->{
     if(maktabah!=books||(!target.equals("Tanpa kategori")&&!categories().contains(target)))return;
     android.content.SharedPreferences.Editor changes=prefs.edit();int moved=0;
     for(String k:chosen)if(k.startsWith(prefix)&&prefs.contains(k)&&!prefs.getBoolean("deleted_"+k,false)){changes.putString("cat_"+k,target);moved++;}
     if(!changes.commit()){Toast.makeText(this,"Gagal menyimpan pemindahan",Toast.LENGTH_LONG).show();return;}
     Toast.makeText(this,moved+" posting dipindahkan",Toast.LENGTH_SHORT).show();notesPage();
    }).show();
   }).setNegativeButton("Batal",null).show();
   }).setNegativeButton("Batal",null).show();
  }).create();
  picker.setOnShowListener(d->picker.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{boolean all=true;for(boolean on:checked)if(!on){all=false;break;}for(int i=0;i<checked.length;i++){checked[i]=!all;picker.getListView().setItemChecked(i,!all);}}));
  picker.show();
 }
 void list(String filter){filter=filter.trim();String query=normalizeCompareText(filter.replaceAll("\\s+"," "));String[] terms=query.isEmpty()?new String[0]:query.split(" ");items.removeAllViews();ArrayList<String> keys=new ArrayList<>();java.util.Map<String,?> listSnapshot=prefs.getAll();for(String k:listSnapshot.keySet())if(k.startsWith(maktabah?"m_":"n_")&&listSnapshot.get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false))keys.add(k);Collections.sort(keys,(a,b)->{
   boolean pinA=prefs.getBoolean("pinned_"+a,false),pinB=prefs.getBoolean("pinned_"+b,false);
   if(pinA!=pinB)return pinA?-1:1;
   if(maktabah&&prefs.getBoolean("book_manual_order",false)){int rank=Integer.compare(prefs.getInt("book_rank_"+a,Integer.MAX_VALUE),prefs.getInt("book_rank_"+b,Integer.MAX_VALUE));if(rank!=0)return rank;}
   if("Tajuk A–Z".equals(sortMode)){int byTitle=prefs.getString("title_"+a,"").trim().compareToIgnoreCase(prefs.getString("title_"+b,"").trim());if(byTitle!=0)return byTitle;}
   if("Kerap dibuka".equals(sortMode)){int byViews=Long.compare(prefs.getLong("viewed_"+b,0),prefs.getLong("viewed_"+a,0));if(byViews!=0)return byViews;}
   long ta=prefs.getLong("updated_"+a,0),tb=prefs.getLong("updated_"+b,0);
   int byDate="Terlama".equals(sortMode)?Long.compare(ta,tb):Long.compare(tb,ta);return byDate!=0?byDate:a.compareTo(b);
  });int matches=0;for(String k:keys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");if(!matchesSearch(heading+" "+v+" "+prefs.getString("cat_"+k,"Tanpa kategori"),terms))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;matches++;}if(matches>0){
 LinearLayout countRow=new LinearLayout(this);countRow.setOrientation(LinearLayout.HORIZONTAL);countRow.setGravity(Gravity.CENTER_VERTICAL);countRow.setPadding(dp(16),dp(3),dp(12),dp(3));
 TextView countLabel=text(matches+" "+(maktabah?"kitab":"nota"),12);countLabel.setTextColor(0xff687078);countRow.addView(countLabel,new LinearLayout.LayoutParams(0,-2,1));
 TextView bulk=text("☑",19);bulk.setTextColor(0xff216e4e);bulk.setGravity(Gravity.CENTER);bulk.setContentDescription("Pilih posting untuk pindah atau padam");countRow.addView(bulk,new LinearLayout.LayoutParams(dp(36),dp(36)));bulk.setOnClickListener(v->chooseBulkPostings());
 String resumeKey=prefs.getString(maktabah?"reading_last_m":"reading_last_n","");
 if(query.isEmpty()&&resumeKey.startsWith(maktabah?"m_":"n_")&&prefs.contains(resumeKey)&&!prefs.getBoolean("deleted_"+resumeKey,false)){
  String resumeTitle=prefs.getString("title_"+resumeKey,"Tanpa tajuk").trim();
  TextView resume=text("Sambung: "+resumeTitle+"  →",12);
  resume.setTypeface(null,android.graphics.Typeface.BOLD);resume.setTextColor(0xff566a70);
  resume.setSingleLine(true);resume.setEllipsize(android.text.TextUtils.TruncateAt.END);
  resume.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
  resume.setContentDescription("Sambung bacaan "+resumeTitle);resume.setPadding(dp(6),dp(5),0,dp(5));
  applyTouchFeedback(resume);resume.setOnClickListener(v->resumeReading(resumeKey));
  countRow.addView(resume,new LinearLayout.LayoutParams(0,-2,2));
 }
 if(compactSummary!=null){compactSummary.removeAllViews();compactSummary.addView(countRow,new LinearLayout.LayoutParams(-1,-2));}else items.addView(countRow);
}else if(compactSummary!=null)compactSummary.removeAllViews();if(matches==0){TextView empty=text(keys.isEmpty()?(maktabah?"▤  Belum ada kitab\nTekan + Kitab baharu untuk bermula":"✎  Belum ada nota\nTekan + Nota baharu untuk mula mencatat"):"Tiada hasil carian yang sepadan",17);empty.setGravity(Gravity.CENTER);empty.setTextColor(0xff727985);empty.setPadding(dp(20),dp(56),dp(20),dp(56));items.addView(empty);if(keys.isEmpty()&&query.isEmpty()){empty.setContentDescription(maktabah?"Tambah kitab baharu":"Tambah nota baharu");applyTouchFeedback(empty);empty.setOnClickListener(v->edit(null));}else{empty.setText(query.isEmpty()?(maktabah?"Tiada kitab dalam kategori ini":"Tiada nota dalam kategori ini"):"Tiada hasil carian yang sepadan");}if(!query.isEmpty())empty.setText("Tiada hasil untuk: "+(filter.length()>60?filter.substring(0,60)+"…":filter));}boolean twoColumnList=root.findViewWithTag("board_list_pane")==null&&getResources().getConfiguration().screenWidthDp>=600;
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
 for(String k:displayKeys){String v=prefs.getString(k,"");String heading=prefs.getString("title_"+k,"");String shown=(heading.trim().isEmpty()?(cleanCardPreview(v).isEmpty()?"Tanpa tajuk":cleanCardPreview(v)):heading.trim());shown=shown.replaceAll("\\s+"," ").trim();if(shown.length()>70)shown=shown.substring(0,70)+"…";if(shown.trim().isEmpty())shown="Tanpa tajuk";if(!matchesSearch(heading+" "+v+" "+prefs.getString("cat_"+k,"Tanpa kategori"),terms))continue;if(!categoryFilter.equals("Semua")&&!categoryFilter.equals(prefs.getString("cat_"+k,"Tanpa kategori")))continue;String preview=cleanCardPreview(v);if(preview.isEmpty())preview="Belum ada kandungan";String cat=prefs.getString("cat_"+k,"Tanpa kategori").trim();if(cat.isEmpty())cat="Tanpa kategori";long stamp=prefs.getLong("updated_"+k,0);String date=stamp>0?new java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(new java.util.Date(stamp)):"Belum dikemas kini";String previewText=preview;
 String cardTitle=(prefs.getBoolean("pinned_"+k,false)?"📌  ":"")+(maktabah?"▤  ":"✎  ")+shown;
 LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
 card.setBackground(homeInsetSurface(k.equals(lastOpenedCard)?0xfff2faf5:0xffffffff,k.equals(lastOpenedCard)?0xfff2faf5:0xffffffff,k.equals(lastOpenedCard)?0xffb7d8c2:0xffd4ddd7,16));
 card.setElevation(0);card.setPadding(dp(twoColumnList?12:16),dp(3),dp(twoColumnList?12:16),dp(3));
 TextView headingView=text(cardTitle,15);headingView.setTypeface(null,android.graphics.Typeface.BOLD);
 headingView.setIncludeFontPadding(false);headingView.setSingleLine(true);headingView.setEllipsize(android.text.TextUtils.TruncateAt.END);headingView.setTextColor(0xff30343b);
 LinearLayout.LayoutParams compactTitleParams=new LinearLayout.LayoutParams(-1,-2);compactTitleParams.bottomMargin=-dp(8);card.addView(headingView,compactTitleParams);
 TextView previewView=text(previewText,15);previewView.setTextColor(0xff30343b);
 previewView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);previewView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
 previewView.setIncludeFontPadding(false);previewView.setMaxLines(2);previewView.setMinLines(twoColumnList?2:1);previewView.setEllipsize(android.text.TextUtils.TruncateAt.END);
 previewView.setLineSpacing(dp(1),1.0f);card.addView(previewView,new LinearLayout.LayoutParams(-1,-2));
 LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);
 TextView categoryView=text(cat,12);categoryView.setIncludeFontPadding(false);categoryView.setTextColor(0xff687078);categoryView.setSingleLine(true);
 categoryView.setEllipsize(android.text.TextUtils.TruncateAt.END);
 meta.addView(categoryView,new LinearLayout.LayoutParams(0,-2,1f));
 TextView dateView=text(date,12);dateView.setIncludeFontPadding(false);dateView.setTextColor(0xff687078);dateView.setGravity(Gravity.END);
 dateView.setSingleLine(true);dateView.setEllipsize(android.text.TextUtils.TruncateAt.END);
 meta.addView(dateView,new LinearLayout.LayoutParams(-2,-2));
 LinearLayout.LayoutParams compactMetaParams=new LinearLayout.LayoutParams(-1,-2);compactMetaParams.topMargin=-dp(8);card.addView(meta,compactMetaParams);
 LinearLayout.LayoutParams noteCardParams=new LinearLayout.LayoutParams(twoColumnList?0:-1,-2,twoColumnList?1f:0f);noteCardParams.setMargins(dp(twoColumnList?4:12),dp(2),dp(twoColumnList?4:12),dp(2));card.setContentDescription((prefs.getBoolean("pinned_"+k,false)?"Disemat. ":"")+(maktabah?"Buka kitab: ":"Buka nota: ")+shown+", kategori "+cat+", "+date);applyTouchFeedback(card);card.setFocusable(true);
 if(twoColumnList){
  if(visibleCards%2==0){cardRow=new LinearLayout(this);cardRow.setOrientation(LinearLayout.HORIZONTAL);cardRow.setPadding(dp(8),0,dp(8),0);items.addView(cardRow,new LinearLayout.LayoutParams(-1,-2));}
  cardRow.addView(card,noteCardParams);
 }else items.addView(card,noteCardParams);
 visibleCards++;View line=new View(this);line.setBackgroundColor(0xffe5e9e5);--matches;card.setOnClickListener(w->{
  if(listScroll!=null){if(maktabah)savedBooksScroll=listScroll.getScrollY();else savedNotesScroll=listScroll.getScrollY();}
  lastOpenedCard=k;read(k);
 });}}

 android.view.ScaleGestureDetector lectureScale;
 View lectureHighlighted;
 int readerBuiltWidthPx=0;
 void enableLectureZoom(String key){
  final View activeRoot=root;
  lectureScale=new android.view.ScaleGestureDetector(this,new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener(){
   float scale=1f;
   @Override public boolean onScaleBegin(android.view.ScaleGestureDetector d){scale=1f;return true;}
   @Override public boolean onScale(android.view.ScaleGestureDetector d){scale*=d.getScaleFactor();return true;}
   @Override public void onScaleEnd(android.view.ScaleGestureDetector d){
    int old=prefs.getInt("reader_font_level",1);
    int next=Math.max(0,Math.min(3,old+(scale>1.12f?1:scale<0.89f?-1:0)));
    if(next!=old)activeRoot.post(()->{if(root==activeRoot&&key.equals(selected)){saveReaderPosition(key);prefs.edit().putInt("reader_font_level",next).apply();read(key);}});
   }
  });
 }
 void markLectureParagraph(View paragraph){
  if(lectureHighlighted!=null)lectureHighlighted.setBackgroundColor(lectureDark()?0xff202824:Color.TRANSPARENT);
  if(lectureHighlighted==paragraph){lectureHighlighted=null;return;}
  lectureHighlighted=paragraph;paragraph.setBackgroundColor(lectureDark()?0xff365746:0xffe3f2e7);
 }

 // Shared quiet pearl shade for secondary screens; home retains its animated aura.
 android.graphics.drawable.GradientDrawable pageShade(){
  return new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xffe6f2e9,0xfffdfdf9,0xfffff5d9});
 }
 void cleanBase(){
  readerDetector=null;lectureScale=null;lectureHighlighted=null;root=new LinearLayout(this);root.setOrientation(1);root.setBackground(pageShade());
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
 Button action(String label,LinearLayout bar,Runnable callback){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setBackground(rounded(0xfff6f8fa,0xffe1e7e3,12));LinearLayout.LayoutParams buttonParams=new LinearLayout.LayoutParams(-2,dp("reader_toolbar".equals(bar.getTag())?34:"editor_format_toolbar".equals(bar.getTag())||"editor_top_toolbar".equals(bar.getTag())?34:42));buttonParams.setMargins(dp(3),dp("reader_toolbar".equals(bar.getTag())||"editor_format_toolbar".equals(bar.getTag())||"editor_top_toolbar".equals(bar.getTag())?3:5),dp(3),dp("reader_toolbar".equals(bar.getTag())||"editor_format_toolbar".equals(bar.getTag())||"editor_top_toolbar".equals(bar.getTag())?3:5));bar.addView(b,buttonParams);if("editor_format_toolbar".equals(bar.getTag())){b.setBackground(rounded(0xfff6f8fa,0xffd0d7de,7));b.setElevation(0);b.setTextColor(0xff344054);b.setPadding(dp(10),dp(3),dp(10),dp(3));}if("editor_top_toolbar".equals(bar.getTag())){android.graphics.drawable.GradientDrawable pearl=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xffffffff,0xffedf7f0});pearl.setCornerRadius(dp(16));pearl.setStroke(dp(1),0xffb6cdbd);b.setBackground(rounded(0xfff6f8fa,0xffd0d7de,8));b.setTextColor(0xff344054);b.setElevation(0);b.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.NORMAL);b.setPadding(dp(9),dp(3),dp(9),dp(3));}if("reader_toolbar".equals(bar.getTag())){android.graphics.drawable.GradientDrawable emerald=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xffffffff,0xffedf7f0});emerald.setCornerRadius(dp(7));emerald.setStroke(dp(1),0xffb6cdbd);b.setBackground(emerald);b.setGravity(Gravity.CENTER);b.setIncludeFontPadding(false);b.setPadding(0,0,0,0);b.setTextColor(0xff216e4e);b.setElevation(0);b.setStateListAnimator(null);b.setSingleLine(true);buttonParams.width=dp(30);buttonParams.height=dp(30);buttonParams.setMargins(dp(2),dp(2),dp(2),dp(2));b.setLayoutParams(buttonParams);b.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);}if("editor_format_toolbar".equals(bar.getTag())||"editor_top_toolbar".equals(bar.getTag())){b.setBackground(rounded(0xffffffff,0xffd7dedb,9));b.setElevation(0);b.setTextColor(0xff3c5051);b.setPadding(dp(8),dp(2),dp(8),dp(2));}b.setOnClickListener(v->callback.run());return b;}
 void returnFromReader(){stopAutoReading();lectureStarted=0;lectureTimerKey=null;if(selected!=null)saveReaderPosition(selected);getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);prefs.edit().putBoolean("reader_clean_mode",false).apply();selected=null;if(sectionOpen)notesPage();else home();}


 long lectureStarted=0;
 String lectureTimerKey=null;
 void startLectureTimer(String key){
  if(!key.equals(lectureTimerKey)||lectureStarted==0){lectureTimerKey=key;lectureStarted=android.os.SystemClock.elapsedRealtime();}
 }
 TextView lectureClock(){
  TextView t=text("",13);t.setGravity(Gravity.CENTER);t.setTextColor(0xff254c39);t.setBackground(rounded(0xfff3f7f3,0xffbed2c5,20));t.setElevation(dp(5));return t;
 }
 void revealLectureClock(FrameLayout frame,TextView clock){
  long sec=Math.max(0,(android.os.SystemClock.elapsedRealtime()-lectureStarted)/1000);
  clock.setText(String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60));
  if(clock.getParent()==null){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(100),dp(32),Gravity.CENTER);p.bottomMargin=dp(130);frame.addView(clock,p);}
 }
 int lecturePaper(){return new int[]{0xffffffff,0xfffffefd,0xfff8eed8,0xff202824}[Math.max(0,Math.min(3,prefs.getInt("lecture_paper",0)))];}
 boolean lectureDark(){return prefs.getInt("lecture_paper",0)==3;}
 void applyLectureDark(View v){
  if(v instanceof TextView){TextView t=(TextView)v;t.setTextColor(0xffe5ece7);t.setBackgroundColor(0xff202824);if(t instanceof EditText)t.setHintTextColor(0xffaebdb3);}
  else if(v instanceof android.view.ViewGroup){v.setBackgroundColor(0xff202824);android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)applyLectureDark(g.getChildAt(i));}
 }
 void chooseLecturePaper(String key){
  new AlertDialog.Builder(this).setTitle("Warna kertas").setSingleChoiceItems(new String[]{"Putih","Mutiara","Krim","Gelap"},Math.max(0,Math.min(3,prefs.getInt("lecture_paper",0))),(d,w)->{saveReaderPosition(key);prefs.edit().putInt("lecture_paper",w).apply();d.dismiss();read(key);}).setNegativeButton("Tutup",null).show();
 }
 Button lectureSettingsButton(){
  Button b=new Button(this);b.setText("⚙");b.setTextSize(22);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinHeight(0);b.setBackground(rounded(0xfff3f7f3,0xffbed2c5,50));b.setElevation(dp(5));b.setContentDescription("Kawalan kuliah");return b;
 }
 FrameLayout.LayoutParams lectureSettingsParams(){
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER);p.topMargin=dp(140);return p;
 }
 void switchLectureView(String key,int mode){
  prefs.edit().putInt("lecture_view_"+key,mode).apply();
  try{readContentSafely(key);}catch(Exception failure){showReaderRecovery(key,failure);}
 }
 void addLectureModeBar(LinearLayout parent,String key,int mode,boolean editing){
  LinearLayout menu=new LinearLayout(this);menu.setGravity(Gravity.CENTER_VERTICAL);menu.setBaselineAligned(false);parent.addView(menu,new LinearLayout.LayoutParams(-1,-2));menu.setPadding(dp(4),0,dp(4),0);
  String[] labels={"☷ Expand","▤ Paparan Siap","✎ Edit Penuh","⌘ Peta Minda"};
  for(int i=0;i<labels.length;i++){
   final int choice=i;TextView item=text(labels[i],12);item.setGravity(Gravity.CENTER);item.setSingleLine(true);item.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);item.setTextColor(0xff216e4e);item.setTypeface(null,android.graphics.Typeface.BOLD);item.setBackground(rounded((editing?i==2:(mode==3?i==3:mode==2?i==1:i==0))?0xffedf4eb:0xfffffefd,0xff9db9ad,10));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(36),1);lp.setMargins(dp(2),dp(2),dp(2),dp(3));menu.addView(item,lp);
   item.setOnClickListener(v->{if(editing){if(choice==2)return;finishEditingSafely(choice==0?0:choice==1?2:3);return;}if(choice==0)switchLectureView(key,0);else if(choice==1)switchLectureView(key,2);else if(choice==2)edit(key);else switchLectureView(key,3);});
  }
 }
 void showPagedReader(String key){
  enableLectureZoom(key);startLectureTimer(key);
  final String rawPagedText=selectedReadingText(key);
  if(isLectureTemplate(rawPagedText)&&!prefs.getBoolean("reader_clean_mode",false))addLectureModeBar(root,key,2,false);
  final android.text.SpannableStringBuilder source=readerLabelText(rawPagedText);
  if(isLectureTemplate(rawPagedText)){
   java.util.regex.Matcher headings=lectureHeadingPattern().matcher(source.toString());
   while(headings.find()){
    int start=headings.start(),end=headings.end();
    source.setSpan(new PagedLectureHeadingSpan(headings.group(1).trim()),start,end,33);
    source.setSpan(new android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),start,end,33);
   }
  }
  final int availableWidthDp=Math.round(readerBuiltWidthPx/getResources().getDisplayMetrics().density);
  final boolean spread=availableWidthDp>=500&&prefs.getBoolean("reader_two_columns",true);
  final int fontLevel=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  final int fontSize=new int[]{13,15,16,18}[fontLevel];
  final int paper=lecturePaper();
  final Runnable[] pageIndex={()->Toast.makeText(this,"Indeks sedang disediakan",Toast.LENGTH_SHORT).show()};
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
    new int[]{0xffdce7e0,0xffa4bfb0,0xffcbdcd2,0xffa4bfb0,0xffdce7e0});
   line.setCornerRadius(dp(1));
   View centralLine=new View(this);centralLine.setBackground(line);
   fixedDivider.addView(centralLine,new FrameLayout.LayoutParams(dp(1),-1,Gravity.CENTER));
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
  final boolean lecturePages=prefs.getBoolean("reader_clean_mode",false);
  Button pageBack=new Button(this);
  pageBack.setText("‹");pageBack.setTextSize(48);pageBack.setAllCaps(false);
  pageBack.setPadding(0,0,0,0);pageBack.setMinWidth(0);pageBack.setMinHeight(0);
  pageBack.setBackground(rounded(0xfff3f7f3,0xffbed2c5,50));
  pageBack.setAlpha(0.95f);pageBack.setElevation(dp(5));
  pageBack.setContentDescription("Kembali ke paparan bacaan biasa");
  pageBack.setOnClickListener(v->{
   if(lecturePages){prefs.edit().putBoolean("reader_clean_mode",false).apply();read(key);}
   else returnFromReader();
  });
  Button pageSettings=lectureSettingsButton();
  pageSettings.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Kawalan kuliah").setItems(new String[]{"Bacaan scroll dan syarahan","Saiz tulisan","Warna kertas","Indeks kuliah"},(d,w)->{
   if(w==3){pageIndex[0].run();return;}
   if(w==2){chooseLecturePaper(key);return;}
   if(w==0){prefs.edit().putBoolean("reader_swipe_mode",false).putBoolean("reader_clean_mode",true).apply();read(key);}
   else new AlertDialog.Builder(this).setTitle("Saiz tulisan").setItems(new String[]{"Kecil","Biasa","Besar","Lebih besar"},(dialog,size)->{prefs.edit().putInt("reader_font_level",size).apply();read(key);}).show();
  }).show());
  final TextView pageClock=lectureClock();
  final Runnable hidePageBack=()->{readingFrame.removeView(pageBack);readingFrame.removeView(pageSettings);readingFrame.removeView(pageClock);};
  readerDetector=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){
    readingFrame.removeCallbacks(hidePageBack);
    if(pageBack.getParent()==null)readingFrame.addView(pageBack,new FrameLayout.LayoutParams(dp(76),dp(76),Gravity.CENTER));
    pageBack.bringToFront();
    if(pageSettings.getParent()==null)readingFrame.addView(pageSettings,lectureSettingsParams());
    revealLectureClock(readingFrame,pageClock);
    readingFrame.postDelayed(hidePageBack,3000);
    return true;
   }
  });

  LinearLayout.LayoutParams navigationParams=new LinearLayout.LayoutParams(-1,-2);
  navigationParams.bottomMargin=dp(5); // Small gap above Android system navigation.
  root.addView(navigation,navigationParams);
  Button jump=new Button(this); // Dialog action retained without a second navigation row.
  // Paginate after the view has been measured. Avoid repeated layout passes and
  // view-tree changes while Android is measuring the page content.
  // Display saved content immediately, even before pagination is calculated.
  TextView loading=text("",Math.max(12,fontSize));loading.setText(source);loading.setTextColor(0xff303c43);loading.setPadding(dp(12),dp(12),dp(12),dp(12));pageRow.addView(loading,new LinearLayout.LayoutParams(-1,-1));
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
    ArrayList<CharSequence> parts=new ArrayList<>();
    ArrayList<Integer> starts=new ArrayList<>();
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
     starts.add(cursor);parts.add(source.subSequence(cursor,stop));
      repeatedPrefixLengths.add(Math.max(0,Math.min(stop-cursor,previousStop-cursor)));
      previousStop=stop;
     cursor=stop;
      if(lastLine>1 && overlapStart>0 && cursor<source.length()){
       int nextCursor=cursor-(chars-overlapStart);
       if(nextCursor>cursor-chars && nextCursor<cursor)cursor=nextCursor;
      }
    }
    if(parts.isEmpty()){starts.add(0);parts.add("");repeatedPrefixLengths.add(0);}
    final int total=parts.size();
    final int[] position={Math.min(total-1,Math.max(0,prefs.getInt("reading_page_"+key,0)))};
    int savedOffset=prefs.getInt("reading_page_offset_"+key,-1);
    if(savedOffset>=0){for(int j=0;j<starts.size();j++){if(starts.get(j)>savedOffset)break;position[0]=j;}}
    if(spread)position[0]=position[0]/2*2;
    final Runnable[] render={null};
    render[0]=()->{
     pageRow.removeAllViews();
     for(int i=0;i<(spread?2:1);i++){
      int pageNumber=position[0]+i;
      TextView page=text("",fontSize);page.setText(pageNumber<total?parts.get(pageNumber):"");
       if(pageNumber<total && repeatedPrefixLengths.get(pageNumber)>0){
        CharSequence visible=parts.get(pageNumber);
        int repeated=Math.min(visible.length(),repeatedPrefixLengths.get(pageNumber));
        android.text.SpannableString marked=new android.text.SpannableString(visible);
        marked.setSpan(new android.text.style.BackgroundColorSpan(lectureDark()?0xff596044:0xfffff0bd),0,repeated,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        marked.setSpan(new android.text.style.UnderlineSpan(),0,repeated,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        page.setText(marked);
       }
      page.setTextColor(lectureDark()?0xffe5ece7:0xff303c43);page.setGravity(Gravity.TOP);
      page.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
      if(arabic&&prefs.getBoolean("reader_arabic_clear_font",false))page.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));
      page.setLineSpacing(extraLine,lineMultiplier);
      page.setPadding(dp(12),dp(10),dp(12),dp(10));
      page.setBackground(rounded(paper,lectureDark()?0xff53665a:0xffd3c197,5));page.setElevation(0);
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
     prefs.edit().putInt("reading_page_"+key,position[0]).putInt("reading_page_offset_"+key,starts.get(position[0])).apply();
    };
    pageIndex[0]=()->{
     ArrayList<String> titles=new ArrayList<>();
     ArrayList<Integer> targets=new ArrayList<>();
     java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?m)^[ \\t]*(?:[0-9٠-٩۰-۹]+[.)،:–-]|[0-9]\\uFE0F?\\u20E3|[①-⑳]|(?:Bab|Fasal|باب|فصل)\\s+)[^\\r\\n]*").matcher(source);
     while(m.find()){
      int target=0;
      for(int j=0;j<starts.size();j++){if(starts.get(j)>m.start())break;target=j;}
      titles.add(m.group().trim());targets.add(target);
     }
     if(titles.isEmpty()){Toast.makeText(this,"Tiada tajuk bernombor",Toast.LENGTH_SHORT).show();return;}
     new AlertDialog.Builder(this).setTitle("Indeks kuliah").setItems(titles.toArray(new String[0]),(d,w)->{int target=targets.get(w);position[0]=spread?target/2*2:target;render[0].run();}).setNegativeButton("Tutup",null).show();
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
    String needle=normalizeCompareText(term);if(needle.isEmpty())return;
    StringBuilder haystack=new StringBuilder();int[] offsets=new int[source.length()];int count=0;
    for(int j=0;j<source.length();j++){String c=normalizeCompareText(source.substring(j,j+1));if(c.isEmpty())continue;haystack.append(c.charAt(0));offsets[count++]=j;}
    java.util.ArrayList<int[]> matches=new java.util.ArrayList<>();
    int index=0;
    while((index=haystack.indexOf(needle,index))>=0){int end=offsets[index+needle.length()-1]+1;while(end<source.length()&&normalizeCompareText(source.substring(end,end+1)).isEmpty())end++;matches.add(new int[]{offsets[index],end});index+=needle.length();}
    if(matches.isEmpty()){Toast.makeText(this,"Tiada padanan",Toast.LENGTH_SHORT).show();return;}
    showReaderSearchResult(source,term,matches,0);
   }).setNegativeButton("Batal",null).show();
 }
 void showReaderSearchResult(String source,String term,java.util.ArrayList<int[]> matches,int selectedMatch){
  int index=matches.get(selectedMatch)[0],end=matches.get(selectedMatch)[1],from=Math.max(0,index-90),to=Math.min(source.length(),end+120);
  String excerpt=(from>0?"…":"")+source.substring(from,to)+(to<source.length()?"…":"");
  int prefix=from>0?1:0;
  android.text.SpannableString marked=new android.text.SpannableString(excerpt);
  int begin=index-from+prefix;
  marked.setSpan(new android.text.style.BackgroundColorSpan(0xffffe6a0),begin,Math.min(marked.length(),begin+end-index),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  marked.setSpan(new android.text.style.UnderlineSpan(),begin,Math.min(marked.length(),begin+end-index),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
 class SectionIndentSpan extends android.text.style.LeadingMarginSpan.Standard {
  SectionIndentSpan(){super(dp(24),0);}
 }
 class SectionGapSpan implements android.text.style.LineHeightSpan {
  @Override public void chooseHeight(CharSequence text,int start,int end,int spanstartv,int v,android.graphics.Paint.FontMetricsInt fm){
   if(start>=end)return;
   for(int i=start;i<end&&i<text.length();i++)if(!Character.isWhitespace(text.charAt(i)))return;
   if(text instanceof android.text.Spanned){android.text.Spanned sp=(android.text.Spanned)text;if(start<sp.getSpanStart(this)||end>sp.getSpanEnd(this))return;}
   fm.ascent=-dp(3);fm.descent=dp(3);
   fm.top=fm.ascent;fm.bottom=fm.descent;
  }
 }
 void applySectionIndent(android.text.Spannable value){
  for(SectionIndentSpan span:value.getSpans(0,value.length(),SectionIndentSpan.class))value.removeSpan(span);
  for(SectionGapSpan span:value.getSpans(0,value.length(),SectionGapSpan.class))value.removeSpan(span);
  String raw=value.toString();boolean active=false,box=false,paragraphStart=false;
  for(int start=0;start<raw.length();){
   int end=raw.indexOf('\n',start);if(end<0)end=raw.length();
   String line=raw.substring(start,end),trim=line.trim();
   if(trim.matches("---(?:MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN)---")){active=true;paragraphStart=true;}
   else if(trim.equals("---TAMAT---")){active=false;paragraphStart=false;}
   else if(trim.equals("---KOTAKTAMAT---"))box=false;
   else if(trim.startsWith("---KOTAK"))box=true;
   else if(trim.isEmpty()){paragraphStart=true;}
   else if(active&&paragraphStart&&!box&&!trim.isEmpty()&&!Character.isWhitespace(line.charAt(0))&&!Character.isSpaceChar(line.charAt(0))&&!trim.startsWith("---")&&!trim.matches("^(?:\\p{Nd}+[.)]|[-•●◦▪☐☑✓*#]|@#).*")){
    value.setSpan(new SectionIndentSpan(),start,end<raw.length()?end+1:end,android.text.Spanned.SPAN_PARAGRAPH);
   }
   if(!trim.isEmpty()&&!trim.startsWith("---"))paragraphStart=false;
   start=end+1;
  }
 }
 class CompactLatinReadingSpan implements android.text.style.LineHeightSpan {
  @Override public void chooseHeight(CharSequence text,int start,int end,int spanstartv,int v,android.graphics.Paint.FontMetricsInt fm){
   boolean latin=false;
   for(int i=start;i<end&&i<text.length();i++){
    char c=text.charAt(i);
    if((c>=0x0600&&c<=0x08ff)||c=='-')return;
    if((c>='A'&&c<='Z')||(c>='a'&&c<='z'))latin=true;
   }
   if(!latin)return;
   int reduction=Math.round((fm.descent-fm.ascent)*0.10f);
   fm.descent=Math.max(1,fm.descent-reduction);
  }
 }
 android.text.SpannableStringBuilder readerLabelText(String source){
  android.text.SpannableStringBuilder out=formattedReadingText(source);
  java.util.regex.Matcher pages=java.util.regex.Pattern.compile("(?m)^---MSASAL:([0-9]{1,7})---[ \\t]*$").matcher(out.toString());
  while(pages.find()){
   out.setSpan(new OriginalPageSpan("❖ Muka Surat "+pages.group(1)+" ❖"),pages.start(),pages.end(),33);
   out.setSpan(new android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),pages.start(),pages.end(),33);
  }
  java.util.regex.Matcher marks=java.util.regex.Pattern.compile("(?m)^---(MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN|TAMAT)---[ \\t]*$").matcher(out.toString());
  while(marks.find()){
   String k=marks.group(1);
   String label=k.equals("MATAN")?"المتن · Matan":k.equals("SYARAHAN")?"الشرح · Syarahan":k.equals("DALIL")?"الدليل · Dalil":k.equals("TERJEMAHAN")?"الترجمة · Terjemahan":k.equals("SYAFII")?"الشَّافِعِيُّ · Syafi‘i":k.equals("HANAFI")?"الْحَنَفِيُّ · Hanafi":k.equals("MALIKI")?"الْمَالِكِيُّ · Maliki":k.equals("HANBALI")?"الْحَنْبَلِيُّ · Hanbali":k.equals("MUQARAN")?"الْفِقْهُ الْمُقَارَنُ · Fiqh Muqaran":k.equals("KHULASAH")?"الخُلَاصَةُ · Khulasah":"الدُّرُوسُ وَالْعِبَرُ · Pengajaran & Iktibar";
   out.setSpan(new BoxMarkerSpan(k.equals("TAMAT")?"":label,0xff176b47,0xffedf5ee),marks.start(),marks.end(),33);
  }
  // Use a visible ornament in text-only readers; do not depend on a replacement span
  // surviving pagination, line-height adjustments or selection.
  java.util.regex.Matcher dividers=java.util.regex.Pattern.compile("(?m)^[ \\t]*---(FLOWER|GEOMETRIC|CLASSIC)---[ \\t]*$").matcher(out.toString());
  java.util.ArrayList<int[]> dividerRanges=new java.util.ArrayList<>();
  while(dividers.find())dividerRanges.add(new int[]{dividers.start(),dividers.end(),dividers.group(1).equals("FLOWER")?0:dividers.group(1).equals("GEOMETRIC")?1:2});
  for(int i=dividerRanges.size()-1;i>=0;i--){
   int[] range=dividerRanges.get(i);
   String ornament="──────  "+new String[]{"✿","❖","❧"}[range[2]]+"  ──────";
   out.replace(range[0],range[1],ornament);
   int finish=range[0]+ornament.length();
   out.setSpan(new android.text.style.ForegroundColorSpan(0xff176b47),range[0],finish,33);
   out.setSpan(new ReaderOrnamentSpan(new String[]{"FLOWER","GEOMETRIC","CLASSIC"}[range[2]]),range[0],finish,33);
   out.setSpan(new android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),range[0],finish,33);
  }
  // Closing markers are structural only; remove their entire line from the display copy.
  java.util.regex.Matcher closingLines=java.util.regex.Pattern.compile("(?m)^---TAMAT---[ \\t]*(?:\\n|$)").matcher(out.toString());
  java.util.ArrayList<int[]> closingRanges=new java.util.ArrayList<>();
  while(closingLines.find())closingRanges.add(new int[]{closingLines.start(),closingLines.end()});
  applySectionIndent(out);
  for(int i=closingRanges.size()-1;i>=0;i--){int[] range=closingRanges.get(i);out.delete(range[0],range[1]);}
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?m)^---KOTAK(DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---[ \\t]*$").matcher(out.toString());
  int body=-1,bg=0,ink=0,first=-1;
  while(m.find()){
   String kind=m.group(1);
   if(kind.equals("TAMAT")){
    if(first>=0){out.setSpan(new BoxBodySpan(bg,ink,first,m.end()),first,m.end(),33);if(body<m.start())out.setSpan(new BoxInsetSpan(),body,m.start(),33);}
    out.setSpan(new BoxMarkerSpan("",ink,bg),m.start(),m.end(),33);body=-1;first=-1;continue;
   }
   boolean gold=kind.equals("SABDA"),gray=kind.equals("CATATAN"),wisdom=kind.equals("HIKMAH"),dalil=kind.equals("DALIL"),sahabat=kind.equals("SAHABAT"),tabii=kind.equals("TABII"),ulama=kind.equals("ULAMA");
   bg=kind.equals("FIRMAN")?0xfffaf8f0:gold?0xfff0f8f2:0xfff3f3f2;
   ink=kind.equals("FIRMAN")?0xff96783e:gold?0xff176b47:0xff666b68;
   String title=kind.equals("FIRMAN")?"Firman Allah SWT:":kind.equals("SABDA")?"Sabda Nabi SAW:":dalil?"Dalil":wisdom?"Kalam Hikmah":sahabat?"Kata Sahabat Rdh.":tabii?"Kata Tabi\u0027in Rhm.":ulama?"Kata Ulama\u0027 Rhm.":"Catatan Penting";
   first=m.start();out.setSpan(new BoxMarkerSpan(title,ink,bg),m.start(),m.end(),33);body=Math.min(out.length(),m.end()+1);
  }
  if(out.length()>0&&!prefs.getBoolean("reader_wide_spacing",false))out.setSpan(new CompactLatinReadingSpan(),0,out.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  return out;
 }
 int richMask(android.text.Spanned value,int a,int b){
  int mask=0;
  for(android.text.style.StyleSpan span:value.getSpans(a,b,android.text.style.StyleSpan.class))if(value.getSpanStart(span)<=a&&value.getSpanEnd(span)>=b)mask|=span.getStyle()&3;
  for(android.text.style.UnderlineSpan span:value.getSpans(a,b,android.text.style.UnderlineSpan.class))if(value.getSpanStart(span)<=a&&value.getSpanEnd(span)>=b)mask|=4;
  return mask;
 }
 String encodedEditorText(){
  if(editor==null)return "";
  return encodeLectureEditable(editor.getText());
 }
 String encodeLectureEditable(android.text.Editable e){StringBuilder out=new StringBuilder();int i=0;
  while(i<e.length()){
   if(e.charAt(i)=='\n'||e.charAt(i)=='\u2063'){out.append(e.charAt(i));i++;continue;}
   int mask=richMask(e,i,i+1),end=i+1;
   while(end<e.length()&&e.charAt(end)!='\n'&&e.charAt(end)!='\u2063'&&richMask(e,end,end+1)==mask)end++;
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
 void showAutomaticContents(String key){
  String[] codes={"MATAN","SYARAHAN","DALIL","TERJEMAHAN","KHULASAH","IKTIBAR","SYAFII","HANAFI","MALIKI","HANBALI","MUQARAN"};
  String[] names={"Matan","Syarahan","Dalil","Terjemahan","Khulasah","Pengajaran & Iktibar","Syafi‘i","Hanafi","Maliki","Hanbali","Fiqh Muqaran"};
  java.util.ArrayList<String> labels=new java.util.ArrayList<>(),terms=new java.util.ArrayList<>();
  java.util.HashMap<String,Integer> counts=new java.util.HashMap<>();
  for(String raw:prefs.getString(key,"").split("\\n")){
   String line=raw.trim();
   for(int i=0;i<codes.length;i++)if(line.equals("---"+codes[i]+"---")){
    int n=counts.containsKey(line)?counts.get(line)+1:1;counts.put(line,n);
    labels.add(names[i]+(n>1?" ("+n+")":""));terms.add(line);break;
   }
  }
  if(labels.isEmpty()){new AlertDialog.Builder(this).setTitle("Isi Kandungan").setMessage("Belum ada label bahagian dalam posting ini.").setPositiveButton("Tutup",null).show();return;}
  ScrollView list=new ScrollView(this);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(12),dp(4),dp(12),dp(6));list.addView(panel);
  int[] colors={0xff386b55,0xff42698b,0xff92732f,0xff795a90,0xff976b42,0xffa2556c,0xff386b55,0xff42698b,0xff92732f,0xff795a90,0xff386b55};
  final AlertDialog[] dialog=new AlertDialog[1];
  for(int j=0;j<labels.size();j++){
   final int which=j;int color=0xff386b55;
   for(int k=0;k<codes.length;k++)if(terms.get(j).equals("---"+codes[k]+"---")){color=colors[k];break;}
   int bg=android.graphics.Color.rgb((android.graphics.Color.red(color)+255*9)/10,(android.graphics.Color.green(color)+255*9)/10,(android.graphics.Color.blue(color)+255*9)/10);
   int edge=android.graphics.Color.rgb((android.graphics.Color.red(color)+255*2)/3,(android.graphics.Color.green(color)+255*2)/3,(android.graphics.Color.blue(color)+255*2)/3);
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(7),dp(10),dp(7));row.setBackground(rounded(bg,edge,10));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);panel.addView(row,lp);
   TextView icon=text("◎",16);icon.setTextColor(color);row.addView(icon,new LinearLayout.LayoutParams(dp(28),-2));
   TextView label=text(labels.get(j),14);label.setTextColor(color);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
   TextView arrow=text("›",20);arrow.setTextColor(color);row.addView(arrow);
   applyTouchFeedback(row);row.setOnClickListener(v->{dialog[0].dismiss();
   String term=terms.get(which);int occurrence=0;for(int i=0;i<which;i++)if(terms.get(i).equals(term))occurrence++;
   final int wanted=occurrence;
   if(readerScroll==null){Toast.makeText(this,"Gunakan mod bacaan skrol",Toast.LENGTH_SHORT).show();return;}
   final ScrollView scroll=readerScroll;
   scroll.post(()->{if(!jumpToContents(scroll,scroll,term,wanted,new int[]{0}))Toast.makeText(this,"Bahagian tidak ditemui dalam paparan ini",Toast.LENGTH_SHORT).show();});
   });
  }
  dialog[0]=new AlertDialog.Builder(this).setTitle("Isi Kandungan").setView(list).setNegativeButton("Tutup",null).create();dialog[0].show();
 }
 boolean jumpToContents(ScrollView scroll,View node,String term,int wanted,int[] seen){
  if(node instanceof TextView){
   TextView tv=(TextView)node;String raw=tv.getText().toString();int offset=0;
   while((offset=raw.indexOf(term,offset))>=0){
    if(seen[0]++==wanted&&tv.getLayout()!=null){
     int y=tv.getTotalPaddingTop()+tv.getLayout().getLineTop(tv.getLayout().getLineForOffset(offset));
     android.graphics.Rect rect=new android.graphics.Rect(0,y,1,y+1);
     scroll.offsetDescendantRectToMyCoords(tv,rect);scroll.smoothScrollTo(0,Math.max(0,rect.top-dp(16)));return true;
    }offset+=term.length();
   }
  }
  if(node instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)node;for(int i=0;i<group.getChildCount();i++)if(jumpToContents(scroll,group.getChildAt(i),term,wanted,seen))return true;}
  return false;
 }
 void showBookIndex(String key){
  String content=prefs.getString(key,"");
  String[] lines=content.split("\\n");
  java.util.ArrayList<String> headings=new java.util.ArrayList<>();
  java.util.ArrayList<String> terms=new java.util.ArrayList<>();
  for(String raw:lines){
   String line=raw.trim();
   if(line.isEmpty()||line.length()>110)continue;
   String lower=line.toLowerCase(java.util.Locale.ROOT);
   boolean chapter=lower.matches("^(bab|fasal|chapter|section|juz)(\\s+.*)?")||line.matches("^(باب|فصل|جزء)(\\s+.*)?");
   boolean marked=line.matches("^@#\\s+.+");
   boolean numbered=line.matches("^[0-9]{1,3}[.)]\\s+.{3,100}");
   if(chapter||marked||numbered){
    String label=marked?line.substring(2).trim():line;
    boolean isBab=lower.matches("^(bab|chapter|juz)(\\s+.*)?")||line.matches("^(باب|جزء)(\\s+.*)?");
    boolean isFasal=lower.matches("^(fasal|section)(\\s+.*)?")||line.matches("^فصل(\\s+.*)?");
    headings.add((isBab?"":isFasal?"    ├  ":"        •  ")+label);
    terms.add(line);
    if(headings.size()>=100)break;
   }
  }
  if(headings.isEmpty()){
   new AlertDialog.Builder(this).setTitle("Indeks Kitab")
    .setMessage("Belum ada subtopik. Gunakan Bab, Fasal, باب, فصل, @# Tajuk atau 1. Tajuk.")
    .setPositiveButton("Tutup",null).show();return;
  }
  new AlertDialog.Builder(this).setTitle("Indeks Kitab ("+headings.size()+")")
   .setItems(headings.toArray(new String[0]),(d,which)->{
    if(readerScroll==null){Toast.makeText(this,"Buka kitab dalam mod bacaan skrol untuk melompat ke tajuk",Toast.LENGTH_LONG).show();return;}
    String chosen=terms.get(which);
    int occurrence=0;
    for(int i=0;i<which;i++)if(terms.get(i).equals(chosen))occurrence++;
    final int targetOccurrence=occurrence;
    readerScroll.post(()->{
     View target=findIndexHeadingView(readerScroll,chosen,targetOccurrence,new int[]{0});
     if(target==null){Toast.makeText(this,"Tajuk tidak ditemui pada paparan bacaan",Toast.LENGTH_SHORT).show();return;}
     android.graphics.Rect rect=new android.graphics.Rect(0,0,target.getWidth(),target.getHeight());
     readerScroll.offsetDescendantRectToMyCoords(target,rect);
     readerScroll.smoothScrollTo(0,Math.max(0,readerScroll.getScrollY()+rect.top-dp(48)));
     int original=0xff303c43;
     if(target instanceof TextView){
      TextView label=(TextView)target;
      original=label.getCurrentTextColor();
      label.setTextColor(0xff176b47);
      final int oldColor=original;
      label.postDelayed(()->{if(label.getParent()!=null)label.setTextColor(oldColor);},1400);
     }
    });
   }).setNeutralButton("Petaminda", (d,w)->showBookMindmap(key,headings))
    .setNegativeButton("Tutup",null).show();
 }
 void showBookMindmap(String key,java.util.ArrayList<String> headings){
  String title=prefs.getString("title_"+key,"Kitab");
  ScrollView scroll=new ScrollView(this);
  LinearLayout tree=new LinearLayout(this);tree.setOrientation(LinearLayout.VERTICAL);
  tree.setPadding(dp(12),dp(12),dp(12),dp(14));scroll.addView(tree);
  TextView rootNode=text(title.trim().isEmpty()?"Kitab":title,16);
  rootNode.setTypeface(null,android.graphics.Typeface.BOLD);
  rootNode.setTextColor(0xff16543b);rootNode.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
  rootNode.setPadding(dp(13),dp(11),dp(13),dp(11));
  rootNode.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_agenda,0,0,0);
  rootNode.setCompoundDrawablePadding(dp(10));
  rootNode.setBackground(rounded(0xffe6f2e9,0xffb6d3bf,12));
  tree.addView(rootNode,new LinearLayout.LayoutParams(-1,-2));
  final java.util.ArrayList<LinearLayout> branchRows=new java.util.ArrayList<>();
  final java.util.ArrayList<Integer> branchDepths=new java.util.ArrayList<>();
  final java.util.ArrayList<Boolean> expanded=new java.util.ArrayList<>();
  final java.util.ArrayList<TextView> branchNodes=new java.util.ArrayList<>();
  final java.util.ArrayList<String> branchLabels=new java.util.ArrayList<>();
  final java.util.ArrayList<Boolean> branchHasChildren=new java.util.ArrayList<>();
  for(String heading:headings){
   int depth=heading.startsWith("        ")?2:heading.startsWith("    ")?1:0;
   String label=heading.trim().replaceFirst("^[├•]\\s*","").trim();
   LinearLayout branch=new LinearLayout(this);branch.setOrientation(LinearLayout.HORIZONTAL);
   branch.setGravity(Gravity.CENTER_VERTICAL);
   LinearLayout.LayoutParams branchParams=new LinearLayout.LayoutParams(-1,-2);
   branchParams.topMargin=dp(depth==0?9:5);branchParams.leftMargin=dp(9+depth*14);
   tree.addView(branch,branchParams);
   TextView connector=text(depth==0?"▣":depth==1?"◆":"•",13);
   connector.setTextColor(0xff8a9f8d);connector.setGravity(Gravity.CENTER);
   branch.addView(connector,new LinearLayout.LayoutParams(dp(22),-2));
   TextView node=text(label,14);
   node.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
   node.setTextColor(depth==0?0xff205b42:0xff384a41);
   node.setPadding(dp(11),dp(depth==0?10:8),dp(11),dp(depth==0?10:8));
   node.setBackground(rounded(depth==0?0xffedf6ee:depth==1?0xfff5f8f3:0xfffdfcf8,0xffd3dfd4,9));
   if(depth==0)node.setTypeface(null,android.graphics.Typeface.BOLD);
   branch.addView(node,new LinearLayout.LayoutParams(0,-2,1));
   final int branchIndex=branchRows.size();
   branchRows.add(branch);branchDepths.add(depth);expanded.add(true);branchNodes.add(node);branchLabels.add(label);
   boolean hasChildren=branchIndex+1<headings.size()&&(headings.get(branchIndex+1).startsWith("        ")?2:headings.get(branchIndex+1).startsWith("    ")?1:0)>depth;
   branchHasChildren.add(hasChildren);
   if(hasChildren){
    node.setText("▾  "+label);
    node.setContentDescription("Buka atau tutup cabang "+label);
    node.setOnClickListener(v->{
     expanded.set(branchIndex,!expanded.get(branchIndex));
     branchNodes.get(branchIndex).setText((expanded.get(branchIndex)?"▾  ":"▸  ")+label);
     refreshMindmapBranches(branchRows,branchDepths,expanded,branchNodes,branchLabels,branchHasChildren);
    });
   }
  }
  rootNode.setContentDescription("Tekan untuk buka atau tutup semua cabang");
  rootNode.setOnClickListener(v->{
   boolean anyCollapsed=false;
   for(int i=0;i<expanded.size();i++)if(branchHasChildren.get(i)&&!expanded.get(i)){anyCollapsed=true;break;}
   for(int i=0;i<expanded.size();i++)if(branchHasChildren.get(i))expanded.set(i,anyCollapsed);
   refreshMindmapBranches(branchRows,branchDepths,expanded,branchNodes,branchLabels,branchHasChildren);
  });
  new AlertDialog.Builder(this).setTitle("Petaminda Subtopik ("+headings.size()+")")
   .setView(scroll).setPositiveButton("Tutup",null).show();
 }
 void refreshMindmapBranches(java.util.ArrayList<LinearLayout> rows,java.util.ArrayList<Integer> depths,
  java.util.ArrayList<Boolean> expanded,java.util.ArrayList<TextView> nodes,
  java.util.ArrayList<String> labels,java.util.ArrayList<Boolean> hasChildren){
  for(int i=0;i<rows.size();i++){
   int depth=depths.get(i);
   boolean visible=true;
   for(int j=i-1;j>=0;j--){
    if(depths.get(j)<depth){
     if(!expanded.get(j)){visible=false;break;}
     depth=depths.get(j);
     if(depth==0)break;
    }
   }
   rows.get(i).setVisibility(visible?View.VISIBLE:View.GONE);
   if(hasChildren.get(i))nodes.get(i).setText((expanded.get(i)?"▾  ":"▸  ")+labels.get(i));
  }
 }
 View findIndexHeadingView(View node,String term,int occurrence,int[] seen){
  if(node instanceof TextView){
   String shown=((TextView)node).getText().toString().trim();
   String target=term.replaceFirst("^@#\\s*","").replaceFirst("^[0-9]{1,3}[.)]\\s+","").trim();
   if(shown.equals(term)||shown.equals(target)){
    if(seen[0]++==occurrence)return node;
   }
  }
  if(node instanceof android.view.ViewGroup){
   android.view.ViewGroup group=(android.view.ViewGroup)node;
   for(int i=0;i<group.getChildCount();i++){
    View match=findIndexHeadingView(group.getChildAt(i),term,occurrence,seen);
    if(match!=null)return match;
   }
  }
  return null;
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
 org.json.JSONArray loadBookFootnotes(String key){
  try{return new org.json.JSONArray(prefs.getString("footnotes_"+key,"[]"));}catch(Exception e){return new org.json.JSONArray();}
 }
 void showBookFootnotes(String key){
  org.json.JSONArray notes=loadBookFootnotes(key);
  String[] rows=new String[notes.length()+1];rows[0]="+ Tambah catatan kaki";
  for(int i=0;i<notes.length();i++){org.json.JSONObject item=notes.optJSONObject(i);String anchor=item==null?"":item.optString("anchor");rows[i+1]=(i+1)+". "+anchor.substring(0,Math.min(100,anchor.length()));}
  new AlertDialog.Builder(this).setTitle("الحواشي · "+prefs.getString("title_"+key,"Kitab")).setItems(rows,(d,i)->editBookFootnote(key,i-1)).setNegativeButton("Tutup",null).show();
 }
 void editBookFootnote(String key,int index){
  org.json.JSONArray notes=loadBookFootnotes(key);
  org.json.JSONObject old=index<0?null:notes.optJSONObject(index);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  EditText anchor=new EditText(this);anchor.setHint("Petikan teks asal / halaman");anchor.setMaxLines(3);panel.addView(anchor);
  EditText detail=new EditText(this);detail.setHint("Huraian / hasyiah");detail.setMinLines(3);detail.setMaxLines(6);panel.addView(detail);
  EditText ref=new EditText(this);ref.setHint("Nama kitab, jilid, halaman");ref.setMaxLines(2);panel.addView(ref);
  if(old!=null){anchor.setText(old.optString("anchor"));detail.setText(old.optString("detail"));ref.setText(old.optString("reference"));}
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);
  AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(index<0?"Tambah catatan kaki":"Catatan kaki "+(index+1)).setView(scroll).setNegativeButton("Batal",(d,w)->showBookFootnotes(key)).setPositiveButton("Simpan",null);
  if(index>=0)builder.setNeutralButton("Padam",(d,w)->new AlertDialog.Builder(this).setMessage("Padam catatan kaki ini sahaja?").setNegativeButton("Batal",(dd,ww)->showBookFootnotes(key)).setPositiveButton("Padam",(dd,ww)->{notes.remove(index);prefs.edit().putString("footnotes_"+key,notes.toString()).apply();showBookFootnotes(key);}).show());
  AlertDialog dialog=builder.create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
   String a=anchor.getText().toString().trim(),b=detail.getText().toString().trim();
   if(a.isEmpty()){anchor.setError("Masukkan petikan atau halaman");return;}if(b.isEmpty()){detail.setError("Masukkan catatan");return;}
   try{
    org.json.JSONObject item=new org.json.JSONObject();item.put("anchor",a);item.put("detail",b);item.put("reference",ref.getText().toString().trim());
    if(index<0)notes.put(item);else notes.put(index,item);
    if(!prefs.edit().putString("footnotes_"+key,notes.toString()).commit())throw new java.io.IOException();
    dialog.dismiss();showBookFootnotes(key);
   }catch(Exception e){Toast.makeText(this,"Simpan gagal",Toast.LENGTH_SHORT).show();}
  }));dialog.show();
 }
 void insertReadingTag(String code){
  if(editor==null)return;
  int start=Math.min(editor.getSelectionStart(),editor.getSelectionEnd()),end=Math.max(editor.getSelectionStart(),editor.getSelectionEnd());
  if(start<0||end>editor.length())return;
  android.text.Editable value=editor.getText();
  if(start!=end&&value.subSequence(start,end).toString().matches("(?s).*---(?:MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN|TAMAT)---.*")){Toast.makeText(this,"Pilih teks tanpa tanda bacaan sedia ada",Toast.LENGTH_SHORT).show();return;}
  // At a collapsed caret before existing prose, label that paragraph instead of inserting an empty section.
  if(start==end&&start<value.length()&&(start==0||value.charAt(start-1)=='\n')){
   int p=start;
   while(p<value.length()&&(value.charAt(p)=='\n'||value.charAt(p)==' '||value.charAt(p)=='\t'))p++;
   if(p<value.length()&&!value.subSequence(p,value.length()).toString().startsWith("---")){
    int stop=p;
    while(stop<value.length()){
     if(value.charAt(stop)=='\n'&&(stop+1==value.length()||value.charAt(stop+1)=='\n'||value.subSequence(stop+1,value.length()).toString().startsWith("---")))break;
     stop++;
    }
    String prefix="---"+code+"---\n";
    android.text.SpannableStringBuilder section=new android.text.SpannableStringBuilder(prefix);
    section.append(value.subSequence(p,stop));section.append("\n---TAMAT---");
    if(stop==value.length())section.append("\n");
    value.replace(start,stop,section);
    editor.setSelection(Math.min(value.length(),start+prefix.length()));
    return;
   }
  }
  String opening=(start>0&&value.charAt(start-1)!='\n'?"\n":"")+"---"+code+"---\n";
  String closing="\n---TAMAT---\n";
  android.text.SpannableStringBuilder block=new android.text.SpannableStringBuilder(opening);
  block.append(value.subSequence(start,end));block.append(closing);
  value.replace(start,end,block);
  editor.setSelection(Math.min(value.length(),start+opening.length()+(end-start)));
 }
 String selectedReadingText(String key,int mode){return selectedReadingText(key,mode,false);}
 String selectedReadingText(String key,int mode,boolean labels){
  String raw=prefs.getString(key,"");
  StringBuilder out=new StringBuilder();int type=0;
  for(String line:raw.split("\\r?\\n",-1)){
   String marker=line.trim();
   if(marker.equals("---MATAN---")){type=1;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---SYARAHAN---")){type=2;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---DALIL---")){type=3;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---TERJEMAHAN---")){type=4;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---KHULASAH---")){type=5;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---IKTIBAR---")){type=6;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---SYAFII---")){type=7;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---HANAFI---")){type=8;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---MALIKI---")){type=9;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---HANBALI---")){type=10;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---MUQARAN---")){type=11;if(labels&&(mode==0||mode==type))out.append(marker).append('\n');continue;}
   if(marker.equals("---TAMAT---")){if(labels&&type!=0&&(mode==0||mode==type))out.append(marker).append('\n');type=0;continue;}
   if(marker.equals("---KOTAKDALIL---")||marker.equals("---KOTAKFIRMAN---")||marker.equals("---KOTAKSABDA---"))type=3;
   if(mode==0||type==mode)out.append(line).append('\n');
   if(marker.equals("---KOTAKTAMAT---"))type=0;
  }
  return out.toString().trim();
 }
 String selectedReadingText(String key){
  int mode=prefs.getInt("reading_filter_"+key,0);
  String filtered=selectedReadingText(key,mode);
  if(mode!=0&&filtered.isEmpty()){prefs.edit().remove("reading_filter_"+key).apply();return selectedReadingText(key,0,true);}
  return selectedReadingText(key,mode,true);
 }
 void showSelectedReading(String key){
  String[] names={"Teks lengkap","Matan sahaja","Syarahan sahaja","Dalil sahaja","Terjemahan sahaja","Khulasah sahaja","Pengajaran & Iktibar sahaja","Syafi‘i sahaja","Hanafi sahaja","Maliki sahaja","Hanbali sahaja","Fiqh Muqaran sahaja"};
  new AlertDialog.Builder(this).setTitle("Mod Bacaan Terpilih").setSingleChoiceItems(names,Math.max(0,Math.min(11,prefs.getInt("reading_filter_"+key,0))),(d,i)->{
   if(i>0&&selectedReadingText(key,i).isEmpty()){Toast.makeText(this,"Tiada teks bertanda untuk pilihan ini",Toast.LENGTH_LONG).show();return;}
   prefs.edit().putInt("reading_filter_"+key,i).apply();d.dismiss();read(key);
  }).setNeutralButton("Panduan tanda",(d,w)->new AlertDialog.Builder(this).setTitle("Penandaan dalam editor").setMessage("Letakkan tanda pada baris berasingan sebelum teks: ---MATAN---, ---SYARAHAN---, ---DALIL--- atau ---TERJEMAHAN--- atau ---KHULASAH--- atau ---IKTIBAR---. Akhiri bahagian dengan ---TAMAT---. Teks tanpa tanda kekal dalam mod lengkap.").setPositiveButton("OK",null).show()).setNegativeButton("Batal",null).show();
 }
 void showLayeredCommentary(String key){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(8),dp(12),dp(12));
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);
  String raw=prefs.getString(key,"");
  java.util.regex.Matcher blocks=java.util.regex.Pattern.compile("(?m)^---(MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN|TAMAT)---[ \\t]*$").matcher(raw);
  int start=0;String kind="";LinearLayout group=panel;int count=0;
  while(blocks.find()){
   String chunk=raw.substring(start,blocks.start()).trim();
   if(!chunk.isEmpty())group=appendLayeredBlock(panel,group,kind,chunk);
   kind=blocks.group(1);if(kind.equals("MATAN"))count++;
   start=blocks.end();
  }
  String tail=raw.substring(start).trim();if(!tail.isEmpty())appendLayeredBlock(panel,group,kind,tail);
  if(count==0){Toast.makeText(this,"Tandakan matan dan syarahan dalam editor untuk bacaan bertingkat",Toast.LENGTH_LONG).show();return;}
  if(key.startsWith("m_")){Button foot=new Button(this);foot.setText("الحواشي · Catatan kaki kitab");panel.addView(foot);foot.setOnClickListener(v->showBookFootnotes(key));}
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("الشرح والحاشية · "+prefs.getString("title_"+key,"")).setView(scroll).setNegativeButton("Tutup",null).create();dialog.show();
  if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.85f));
 }
 LinearLayout appendLayeredBlock(LinearLayout panel,LinearLayout group,String kind,String chunk){
  TextView value=text("",15);value.setText(readerLabelText(chunk));applySharedTextMetrics(value);value.setTextIsSelectable(true);value.setPadding(dp(10),dp(8),dp(10),dp(8));value.setTextColor(0xff303c43);
  if(kind.equals("MATAN")){
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(5),dp(5),dp(5),dp(5));card.setBackground(rounded(0xfff9fcf8,0xffc6b582,10));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);panel.addView(card,lp);
   value.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(value);return card;
  }
  if(kind.equals("SYARAHAN")){
   TextView toggle=text("▸  الشرح · Buka syarahan",13);toggle.setTextColor(0xff176b47);toggle.setPadding(dp(10),dp(12),dp(10),dp(12));group.addView(toggle);group.addView(value);value.setVisibility(View.GONE);
   toggle.setOnClickListener(v->{boolean open=value.getVisibility()!=View.VISIBLE;value.setVisibility(open?View.VISIBLE:View.GONE);toggle.setText(open?"▾  الشرح · Tutup syarahan":"▸  الشرح · Buka syarahan");});
  }else group.addView(value);
  return group;
 }
 int autoReadingToken=0;
 boolean autoReadingActive=false;
 void stopAutoReading(){autoReadingActive=false;autoReadingToken++;}
 void showAutoReading(String key){
  if(readerScroll==null){Toast.makeText(this,"Auto scroll tersedia dalam mod bacaan scroll",Toast.LENGTH_LONG).show();return;}
  if(autoReadingActive){stopAutoReading();Toast.makeText(this,"Auto scroll dihentikan",Toast.LENGTH_SHORT).show();return;}
  String[] speeds={"Perlahan","Sederhana","Laju"};
  new AlertDialog.Builder(this).setTitle("Auto Scroll Pintar").setSingleChoiceItems(speeds,Math.max(0,Math.min(2,prefs.getInt("auto_read_speed",0))),(d,i)->{
   prefs.edit().putInt("auto_read_speed",i).apply();d.dismiss();startAutoReading(key,i);
  }).setNegativeButton("Batal",null).show();
 }
 void startAutoReading(String key,int speed){
  stopAutoReading();final int token=autoReadingToken;final ScrollView target=readerScroll;
  if(target==null)return;autoReadingActive=true;
  Toast.makeText(this,"Auto scroll bermula · buka menu untuk berhenti",Toast.LENGTH_LONG).show();
  target.postDelayed(new Runnable(){public void run(){
   if(token!=autoReadingToken)return;
   if(!key.equals(selected)||readerScroll!=target||!target.isAttachedToWindow()||editor!=null||!hasWindowFocus()){stopAutoReading();return;}
   if(!target.canScrollVertically(1)){stopAutoReading();return;}
   target.scrollBy(0,Math.max(1,dp(speed+1)));target.postDelayed(this,40);
  }},500);
 }
 void showNamedStops(String key){
  stopAutoReading();
  final ScrollView target=readerScroll;
  if(target==null||!target.isAttachedToWindow()){Toast.makeText(this,"Gunakan mod bacaan scroll",Toast.LENGTH_SHORT).show();return;}
  final String storage="named_stops_"+key;
  org.json.JSONArray loaded;
  try{loaded=new org.json.JSONArray(prefs.getString(storage,"[]"));}catch(Exception ex){loaded=new org.json.JSONArray();}
  final org.json.JSONArray marks=loaded;
  String[] names=new String[marks.length()+1];names[0]="+ Simpan tempat semasa";
  for(int i=0;i<marks.length();i++){org.json.JSONObject m=marks.optJSONObject(i);names[i+1]=m==null?"Penanda":m.optString("name");}
  new AlertDialog.Builder(this).setTitle("علامة الوقف · Penanda kitab").setItems(names,(dlg,index)->{
   if(index==0){
    if(marks.length()>=100){Toast.makeText(this,"Had 100 penanda setiap kitab",Toast.LENGTH_SHORT).show();return;}
    final int y=target.getScrollY(),range=Math.max(1,target.getChildAt(0).getHeight()-target.getHeight());
    EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Contoh: Kuliah hari Ahad");
    new AlertDialog.Builder(this).setTitle("Nama penanda").setView(name).setPositiveButton("Simpan",(nd,w)->{
     String label=name.getText().toString().trim();if(label.isEmpty())return;
     try{org.json.JSONObject m=new org.json.JSONObject();m.put("name",label);m.put("fraction",Math.min(1.0,(double)y/range));m.put("filter",prefs.getInt("reading_filter_"+key,0));marks.put(m);prefs.edit().putString(storage,marks.toString()).apply();Toast.makeText(this,"Penanda disimpan",Toast.LENGTH_SHORT).show();}catch(Exception ex){Toast.makeText(this,"Penanda gagal disimpan",Toast.LENGTH_SHORT).show();}
    }).setNegativeButton("Batal",null).show();return;
   }
   int pos=index-1;org.json.JSONObject mark=marks.optJSONObject(pos);if(mark==null)return;
   new AlertDialog.Builder(this).setTitle(mark.optString("name")).setItems(new String[]{"Pergi ke penanda","Padam penanda"},(md,choice)->{
    if(choice==0){
     prefs.edit().putInt("reading_filter_"+key,mark.optInt("filter",0)).apply();read(key);
     ScrollView next=readerScroll;if(next!=null)next.post(()->{if(next.isAttachedToWindow()&&key.equals(selected)&&next.getChildCount()>0)next.scrollTo(0,(int)(Math.max(0,next.getChildAt(0).getHeight()-next.getHeight())*mark.optDouble("fraction",0)));});
    }else new AlertDialog.Builder(this).setTitle("Padam penanda ini?").setPositiveButton("Padam",(dd,w)->{marks.remove(pos);prefs.edit().putString(storage,marks.toString()).apply();}).setNegativeButton("Batal",null).show();
   }).show();
  }).setNegativeButton("Tutup",null).show();
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
  if(book){
   int n=labels.length;
   labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Catatan Kaki (الحواشي)";icons[n-1]="¹";descriptions[n-1]="Catatan bernombor dan rujukan kitab";
  }
  {
   int n=labels.length;
   labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Mod Bacaan Terpilih";icons[n-1]="▤";descriptions[n-1]="Lengkap, matan, syarahan, dalil atau terjemahan";
  }
  {
   int n=labels.length;
   labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Syarahan Bertingkat";icons[n-1]="▾";descriptions[n-1]="Buka atau tutup syarahan dan hasyiah";
  }
  {
   int n=labels.length;
   labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Auto Scroll Pintar";icons[n-1]="↓";descriptions[n-1]="Mula, berhenti dan laraskan kelajuan bacaan";
  }
  if(book){
   int n=labels.length;labels=java.util.Arrays.copyOf(labels,n+1);icons=java.util.Arrays.copyOf(icons,n+1);descriptions=java.util.Arrays.copyOf(descriptions,n+1);
   labels[n]=labels[n-1];icons[n]=icons[n-1];descriptions[n]=descriptions[n-1];
   labels[n-1]="Penanda Tempat Berhenti";icons[n-1]="♧";descriptions[n-1]="Simpan beberapa lokasi bacaan bernama";
  }
  int beforePdf=labels.length;
  labels=java.util.Arrays.copyOf(labels,beforePdf+1);icons=java.util.Arrays.copyOf(icons,beforePdf+1);descriptions=java.util.Arrays.copyOf(descriptions,beforePdf+1);
  labels[beforePdf]=labels[beforePdf-1];icons[beforePdf]=icons[beforePdf-1];descriptions[beforePdf]=descriptions[beforePdf-1];
  labels[beforePdf-1]="Eksport PDF";icons[beforePdf-1]="▤";descriptions[beforePdf-1]="Cetak atau simpan sebagai PDF melalui Android";
  int contentsIndex=labels.length-1;
  labels=java.util.Arrays.copyOf(labels,labels.length+1);icons=java.util.Arrays.copyOf(icons,icons.length+1);descriptions=java.util.Arrays.copyOf(descriptions,descriptions.length+1);
  labels[contentsIndex+1]=labels[contentsIndex];icons[contentsIndex+1]=icons[contentsIndex];descriptions[contentsIndex+1]=descriptions[contentsIndex];
  labels[contentsIndex]="Isi Kandungan";icons[contentsIndex]="☷";descriptions[contentsIndex]="Lompat ke label bahagian dalam posting";
  final int restartIndex=labels.length-1;
  labels=java.util.Arrays.copyOf(labels,labels.length+1);icons=java.util.Arrays.copyOf(icons,icons.length+1);descriptions=java.util.Arrays.copyOf(descriptions,descriptions.length+1);
  labels[restartIndex+1]=labels[restartIndex];icons[restartIndex+1]=icons[restartIndex];descriptions[restartIndex+1]=descriptions[restartIndex];
  labels[restartIndex]="Kembali ke awal bacaan";icons[restartIndex]="↑";descriptions[restartIndex]="Mulakan semula bacaan nota atau kitab ini";
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(12),dp(8),dp(12),dp(10));scroll.addView(panel);
  TextView header=text(book?"▤  Tindakan Maktabah":"▤  Tindakan Nota",18);
  header.setTypeface(null,android.graphics.Typeface.BOLD);header.setTextColor(0xff566a70);
  header.setPadding(dp(12),dp(10),dp(12),dp(14));panel.addView(header);
  final AlertDialog[] dialog=new AlertDialog[1];
  // Display Padam immediately after Edit; retain original action indexes and confirmation.
  for(int i=0;i<labels.length;i++){
   final int option=i==0?0:(i==1?labels.length-1:i-1);
   boolean danger=option==labels.length-1;
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
   row.setPadding(dp(12),dp(5),dp(12),dp(5));
   row.setBackground(rounded(danger?0xfffff1f0:0xfff9fbf9,danger?0xffe8bcb8:0xffcbdcd2,10));
   LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,dp(54));
   rowParams.bottomMargin=dp(4);panel.addView(row,rowParams);
   TextView symbol=text(icons[option],21);symbol.setGravity(Gravity.CENTER);
   symbol.setTextColor(danger?0xffa42e2e:0xff566a70);
   symbol.setBackground(rounded(danger?0xffffdfdc:0xffe2f1e7,danger?0xffffdfdc:0xffe2f1e7,9));
   row.addView(symbol,new LinearLayout.LayoutParams(dp(38),dp(38)));
   LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
   words.setPadding(dp(12),0,dp(4),0);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
   TextView label=text(labels[option],14);label.setTypeface(null,android.graphics.Typeface.BOLD);
   label.setTextColor(danger?0xffa42e2e:0xff25372d);words.addView(label);
   TextView detail=text(descriptions[option],11);detail.setTextColor(danger?0xffa65a56:0xff68766d);words.addView(detail);
   TextView arrow=text("›",22);arrow.setTextColor(danger?0xffa42e2e:0xff566a70);row.addView(arrow);
   applyTouchFeedback(row);
   row.setOnClickListener(v->{
    dialog[0].dismiss();
    if(option==restartIndex){stopAutoReading();if(readerScroll!=null&&key.equals(selected)){readerScroll.scrollTo(0,0);prefs.edit().putInt("reading_scroll_"+key,0).putInt("reading_progress_permille_"+key,0).apply();}}
    else if(option==contentsIndex)showAutomaticContents(key);
    else if(option==0)edit(key);
    else if(option==1){boolean pinned=prefs.getBoolean("pinned_"+key,false);prefs.edit().putBoolean("pinned_"+key,!pinned).putLong("pinned_at_"+key,!pinned?System.currentTimeMillis():0L).apply();Toast.makeText(this,pinned?"Sematan dibuang":"Disemat",Toast.LENGTH_SHORT).show();read(key);}
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
    else if(book&&option==(compact?10:8))showBookFootnotes(key);
    else if(option==(book?(compact?11:9):(compact?7:5)))showSelectedReading(key);
    else if(option==(book?(compact?12:10):(compact?8:6)))showLayeredCommentary(key);
    else if(option==(book?(compact?13:11):(compact?9:7)))showAutoReading(key);
    else if(book&&option==(compact?14:12))showNamedStops(key);
    else if(option==(book?(compact?15:13):(compact?10:8)))printReaderAsPdf(key);
    else confirmDelete(key);
   });
  }
  dialog[0]=new AlertDialog.Builder(this).setView(scroll).setNegativeButton("Tutup",null).create();
  dialog[0].show();
  android.view.Window window=dialog[0].getWindow();
  if(window!=null){
   window.setBackgroundDrawable(rounded(0xffffffff,0xffc5d9ce,12));
   window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(430)),-2);
  }
 }
 void applySharedTextMetrics(android.widget.TextView view){
  // GitHub-like reading typography: lightweight sans-serif for prose, with Android font fallback for Arabic.
  view.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
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
 String cleanSelectableReadingText(String raw){
  return raw.replaceAll("(?im)^[ \\t]*(?:---)?(?:MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN|TAMAT|FLOWER|GEOMETRIC|CLASSIC|KOTAK[A-Z]+)---[ \\t]*(?:\\r?\\n|$)","").replaceAll("(?m)^[ \\t]*---[ \\t]*(?:\\r?\\n|$)","").replace("\u2063","");
 }
 void selectLongReadingText(String key){
  String raw=prefs.getString(key,"");
  // Hide decorative control markers in the continuous selection view and clipboard.
  String cleanText=cleanSelectableReadingText(raw);
  ScrollView selectionScroll=new ScrollView(this);
  selectionScroll.setFillViewport(false);
  selectionScroll.setPadding(dp(12),dp(8),dp(12),dp(8));
  TextView selectable=text("",Math.max(12,new int[]{13,15,16,18}[Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)))]));
  selectable.setText(formattedReadingText(cleanText));
  applySharedTextMetrics(selectable);
  selectable.setTextColor(0xff303c43);
  selectable.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
  selectable.setTextIsSelectable(true);
  selectable.setPadding(dp(8),dp(8),dp(8),dp(24));
  selectionScroll.addView(selectable,new ScrollView.LayoutParams(-1,-2));
  AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle("Pilih teks panjang")
   .setView(selectionScroll)
   .setNeutralButton("Salin semua",(d,w)->{
    android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DinNote",selectable.getText().toString()));
    Toast.makeText(this,"Semua teks disalin",Toast.LENGTH_SHORT).show();
   })
   .setNegativeButton("Tutup",null).create();
  dialog.show();
  if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*0.82f));
 }
 boolean resumeNextReading=false;
 void resumeReading(String key){resumeNextReading=true;read(key);}
 void read(String key){
  try{if(key!=null&&prefs.contains(key)&&isLectureTemplate(prefs.getString(key,"")))prefs.edit().putInt("lecture_view_"+key,2).apply();readContentSafely(key);}
  catch(Exception failure){
   android.util.Log.e("DinNote","Reader failed for "+key,failure);
   showReaderRecovery(key,failure);
  }
 }
 void showReaderRecovery(String key,Exception failure){
  editor=null;titleEditor=null;readerScroll=null;selected=null;
  LinearLayout recovery=new LinearLayout(this);recovery.setOrientation(1);recovery.setPadding(dp(20),dp(24),dp(20),dp(16));
  setContentView(recovery);recovery.setBackgroundColor(0xfffafbf7);
  TextView heading=text("DinNote · Pemulihan Bacaan",20);heading.setTextColor(0xff176b47);recovery.addView(heading);
  TextView msg=text("Posting tidak dapat dipaparkan. Kandungan asal tidak dipadam. Sila buat backup sebelum mengubah data.",15);recovery.addView(msg);
  Button backup=new Button(this);backup.setText("Backup data");recovery.addView(backup);backup.setOnClickListener(v->startBackup());
  Button back=new Button(this);back.setText("Kembali ke halaman depan");recovery.addView(back);back.setOnClickListener(v->openHomeSafely());
  TextView details=text("Ralat: "+failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage()),12);details.setTextIsSelectable(true);recovery.addView(details);
 }
 boolean isLectureTemplate(String source){return lectureHeadingPattern().matcher(source).find();}
 java.util.regex.Pattern lectureHeadingPattern(){return java.util.regex.Pattern.compile("(?m)^(?=\\u2063|(?:Muqaddimah|Pra Topik|Isi Pertama|Isi Kedua|Isi Ketiga|Pengajaran|Iktibar|Khulasah|Penutup)[ \\t]*$)\\u2063?([^\\r\\n]+)$");}
 void moveLectureSection(String key,int index,int delta){
  String raw=prefs.getString(key,"");java.util.regex.Matcher m=lectureHeadingPattern().matcher(raw);ArrayList<Integer> starts=new ArrayList<>();while(m.find())starts.add(m.start());
  int other=index+delta;if(index<0||index>=starts.size()||other<0||other>=starts.size())return;
  int low=Math.min(index,other),high=low+1;int a=starts.get(low),b=starts.get(high),c=high+1<starts.size()?starts.get(high+1):raw.length();
  String first=raw.substring(a,b),second=raw.substring(b,c);if(!second.endsWith("\n"))second+="\n";
  String updated=raw.substring(0,a)+second+first+raw.substring(c);
  snapshotBeforeAutosave(key,raw,prefs.getString("title_"+key,""),prefs.getString("cat_"+key,"Tanpa kategori"));
  prefs.edit().putString(key,updated).putLong("updated_"+key,System.currentTimeMillis()).apply();
  try{readContentSafely(key);}catch(Exception failure){showReaderRecovery(key,failure);}
 }
 TextView copyLectureColumnText(TextView original,CharSequence value){
  TextView copy=text("",15);
  copy.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,original.getTextSize());
  copy.setTypeface(original.getTypeface());copy.setTextColor(original.getCurrentTextColor());
  copy.setText(value);copy.setTextDirection(original.getTextDirection());copy.setGravity(original.getGravity());
  copy.setTextAlignment(original.getTextAlignment());copy.setTextIsSelectable(true);
  copy.setIncludeFontPadding(original.getIncludeFontPadding());
  copy.setLineSpacing(original.getLineSpacingExtra(),original.getLineSpacingMultiplier());
  copy.setPadding(original.getPaddingLeft(),original.getPaddingTop(),original.getPaddingRight(),original.getPaddingBottom());
  if(original.getLayoutParams() instanceof LinearLayout.LayoutParams)
   copy.setLayoutParams(new LinearLayout.LayoutParams((LinearLayout.LayoutParams)original.getLayoutParams()));
  return copy;
 }
 void showLectureMindMap(String key,int size){switchLectureView(key,3);}
 void renderLectureMindMap(String key,int size){
  String raw=prefs.getString(key,"");ArrayList<String> names=new ArrayList<>();ArrayList<Integer> starts=new ArrayList<>(),ends=new ArrayList<>();
  java.util.regex.Matcher m=lectureHeadingPattern().matcher(raw);while(m.find()){names.add(m.group(1));starts.add(m.start());ends.add(m.end());}
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(8),dp(4),dp(8),dp(3));
  TextView title=text(prefs.getString("title_"+key,"Peta Minda"),16);title.setGravity(Gravity.CENTER);title.setPadding(dp(6),dp(3),dp(6),dp(3));title.setBackground(homeGlossy(0,12));root.addView(title);
  LinearLayout columns=new LinearLayout(this);boolean wide=getResources().getConfiguration().screenWidthDp>=600;columns.setOrientation(wide?0:1);LinearLayout.LayoutParams columnsLp=new LinearLayout.LayoutParams(-1,0,1);columnsLp.topMargin=dp(4);columnsLp.bottomMargin=dp(4);root.addView(columns,columnsLp);
  ScrollView treeScroll=new ScrollView(this),contentScroll=new ScrollView(this);
  columns.addView(treeScroll,wide?new LinearLayout.LayoutParams(0,-1,0.8f):new LinearLayout.LayoutParams(-1,0,1));
  if(wide){View divider=new View(this);divider.setBackgroundColor(0xffc99a20);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(dp(1),-1);line.setMargins(dp(6),dp(5),dp(6),dp(5));columns.addView(divider,line);}
  columns.addView(contentScroll,wide?new LinearLayout.LayoutParams(0,-1,2):new LinearLayout.LayoutParams(-1,0,2));
  LinearLayout tree=new LinearLayout(this),detail=new LinearLayout(this);tree.setOrientation(1);detail.setOrientation(1);detail.setPadding(dp(8),dp(8),dp(8),dp(8));treeScroll.addView(tree);contentScroll.addView(detail);
  TextView hint=text("Pilih subtopik untuk membaca kandungan.",14);detail.addView(hint);
  final int[] zoom={size};final int[] chosen={-1};
  Runnable refresh=()->{if(chosen[0]<0)return;int i=chosen[0];detail.removeAllViews();TextView h=text(names.get(i),zoom[0]+2);h.setTextColor(0xff174d37);detail.addView(h);renderBoxReading(detail,raw.substring(ends.get(i),i+1<starts.size()?starts.get(i+1):raw.length()).trim(),zoom[0],key);contentScroll.post(()->contentScroll.scrollTo(0,0));};
  ArrayList<TextView> nodes=new ArrayList<>();
  for(int i=0;i<names.size();i++){final int index=i;View connector=new View(this);connector.setBackgroundColor(0xffb58b29);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(2),dp(4));cp.gravity=Gravity.CENTER_HORIZONTAL;tree.addView(connector,cp);TextView node=text(names.get(i),13);node.setGravity(Gravity.CENTER);node.setTextColor(0xff174d37);node.setIncludeFontPadding(false);node.setPadding(dp(5),dp(3),dp(5),dp(3));node.setMinHeight(dp(26));node.setBackground(homeGlossy(0,9));tree.addView(node);nodes.add(node);node.setOnClickListener(v->{chosen[0]=index;for(TextView n:nodes){boolean active=n==node;n.setBackground(active?rounded(0xff176b47,0xffc99a20,9):homeGlossy(0,9));n.setTextColor(active?Color.WHITE:0xff174d37);}refresh.run();});}
  LinearLayout controls=new LinearLayout(this);root.addView(controls);for(String label:new String[]{"A−","A+","⇅"}){TextView b=text(label,14);b.setGravity(Gravity.CENTER);b.setBackground(homeGlossy(0,10));controls.addView(b,new LinearLayout.LayoutParams(0,dp(32),1));b.setContentDescription(label.equals("⇅")?"Lipat atau kembang subtopik":label);b.setOnClickListener(v->{if(label.equals("A−")){zoom[0]=Math.max(12,zoom[0]-1);refresh.run();}else if(label.equals("A+")){zoom[0]=Math.min(32,zoom[0]+1);refresh.run();}else treeScroll.setVisibility(treeScroll.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);});}
  this.root.addView(root,new LinearLayout.LayoutParams(-1,0,1));
 }
 int lectureGroup(String name){
  switch(name){
   case "Muqaddimah":
   case "Maqasid Kuliah":
   case "Pra Topik": return 0;
   case "Ayat al-Quran":
   case "Hadis Nabawi":
   case "Kupasan Kalimah":
   case "Nukilan Ulama": return 1;
   case "Kalam Hikmah":
   case "Kisah Renungan": return 2;
   case "Perbahasan Hukum":
   case "Kaedah Fiqh":
   case "Realiti Semasa": return 3;
   case "Persoalan Muhasabah":
   case "Tuntutan Amalan":
   case "Khulasah":
   case "Penutup dan Doa": return 4;
   default:return -1;
  }
 }
 boolean groupedLecture(java.util.List<String> names){for(String n:names)if(n.equals("Maqasid Kuliah")||n.equals("Hadis Nabawi")||n.equals("Tuntutan Amalan"))return true;return false;}
 int lectureGroupColor(int g){return new int[]{0xff397e58,0xff5279ad,0xff98702f,0xff8963ad,0xff43877e}[g];}
 void lectureGroupLabel(LinearLayout parent,int g){
  LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);View stripe=new View(this);stripe.setBackgroundColor(lectureGroupColor(g));line.addView(stripe,new LinearLayout.LayoutParams(dp(2),dp(17)));TextView label=text(new String[]{"Pembukaan","Pengisian Ilmiah","Pendidikan Hati","Kefaqihan","Penghayatan"}[g],13);label.setTextColor(lectureGroupColor(g));label.setPadding(dp(8),dp(5),0,dp(5));line.addView(label);parent.addView(line);
 }
 void copyLectureSectionToNote(String key,int index){
  String raw=prefs.getString(key,"");java.util.regex.Matcher m=lectureHeadingPattern().matcher(raw);
  ArrayList<Integer> offsets=new ArrayList<>();while(m.find())offsets.add(m.start());
  if(index<0||index>=offsets.size())return;
  final String section=raw.substring(offsets.get(index),index+1<offsets.size()?offsets.get(index+1):raw.length());
  ArrayList<String> keys=new ArrayList<>();for(java.util.Map.Entry<String,?> e:prefs.getAll().entrySet()){String k=e.getKey();if(k.startsWith("n_")&&!k.equals(key)&&e.getValue() instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&lectureHeadingPattern().matcher((String)e.getValue()).find())keys.add(k);}
  keys.sort((a,b)->prefs.getString("title_"+a,"").compareToIgnoreCase(prefs.getString("title_"+b,"")));
  if(keys.isEmpty()){Toast.makeText(this,"Tiada nota kuliah lain",Toast.LENGTH_SHORT).show();return;}
  String[] labels=new String[keys.size()];for(int i=0;i<keys.size();i++)labels[i]=prefs.getString("title_"+keys.get(i),"Tanpa tajuk");
  new AlertDialog.Builder(this).setTitle("Salin subtopik ke nota").setItems(labels,(d,i)->{
   String dest=keys.get(i);
    if(prefs.getBoolean("deleted_"+dest,false)||!prefs.contains(dest))return;
    String before=prefs.getString(dest,"");snapshotBeforeAutosave(dest,before,prefs.getString("title_"+dest,""),prefs.getString("cat_"+dest,"Tanpa kategori"));
    prefs.edit().putString(dest,before+(before.endsWith("\n")?"\n":"\n\n")+section).putLong("updated_"+dest,System.currentTimeMillis()).apply();
    Toast.makeText(this,"Subtopik disalin",Toast.LENGTH_SHORT).show();
    maktabah=false;read(dest);
  }).setNegativeButton("Batal",null).show();
 }
 void changeLectureSection(String key,int index,String added){
  String raw=prefs.getString(key,"");java.util.regex.Matcher m=lectureHeadingPattern().matcher(raw);ArrayList<Integer> offsets=new ArrayList<>();while(m.find())offsets.add(m.start());if(index<0||index>=offsets.size())return;int start=offsets.get(index),end=index+1<offsets.size()?offsets.get(index+1):raw.length();String updated=added==null?raw.substring(0,start)+raw.substring(end):raw.substring(0,end)+(end>0&&raw.charAt(end-1)!='\n'?"\n":"")+"\u2063"+added+"\n\n\n"+raw.substring(end);snapshotBeforeAutosave(key,raw,prefs.getString("title_"+key,""),prefs.getString("cat_"+key,"Tanpa kategori"));prefs.edit().putString(key,updated).putLong("updated_"+key,System.currentTimeMillis()).apply();try{readContentSafely(key);}catch(Exception failure){showReaderRecovery(key,failure);}
 }
 void renderLectureTemplate(LinearLayout body,String source,int size,String key){
  java.util.regex.Matcher m=lectureHeadingPattern().matcher(source);
  ArrayList<String> names=new ArrayList<>();ArrayList<Integer> starts=new ArrayList<>();ArrayList<Integer> ends=new ArrayList<>();
  while(m.find()){names.add(m.group(1));starts.add(m.start());ends.add(m.end());}
  if(starts.isEmpty())return;
  if(prefs.getInt("lecture_view_"+key,0)!=2&&starts.get(0)>0){TextView prefix=text("",size);prefix.setText(readerLabelText(source.substring(0,starts.get(0))));body.addView(prefix);}
  ArrayList<View> panels=new ArrayList<>();ArrayList<TextView> headers=new ArrayList<>();
  int mode=prefs.getInt("lecture_view_"+key,0);

  if(mode==2){
   // Render each section independently to prevent box leakage.
   String prefix=source.substring(0,starts.get(0)).trim();
   if(!prefix.isEmpty())renderBoxReading(body,prefix,size,key);
   boolean hasContent=!prefix.isEmpty();int pendingBlank=0;
   for(int i=0;i<starts.size();i++){
    String section=source.substring(ends.get(i),i+1<starts.size()?starts.get(i+1):source.length());
    String content=section.trim();
    int leading=0,trailing=0;
    for(int p=0;p<section.length()&&Character.isWhitespace(section.charAt(p));p++)if(section.charAt(p)=='\n')leading++;
    if(content.isEmpty()){pendingBlank+=Math.max(0,leading-1);continue;}
    for(int p=section.length()-1;p>=0&&Character.isWhitespace(section.charAt(p));p--)if(section.charAt(p)=='\n')trailing++;
    int extra=pendingBlank+Math.max(0,leading-1);
    if(hasContent||extra>0){View gap=new View(this);body.addView(gap,new LinearLayout.LayoutParams(-1,dp((hasContent?16:0)+extra*size)));}
    LinearLayout headingBand=new LinearLayout(this);headingBand.setGravity(Gravity.CENTER);headingBand.setPadding(0,dp(1),0,dp(3));
    TextView sectionTitle=text(names.get(i).trim().toUpperCase(java.util.Locale.ROOT),13);sectionTitle.setGravity(Gravity.CENTER);sectionTitle.setTypeface(null,android.graphics.Typeface.BOLD);sectionTitle.setTextColor(0xff216e4e);sectionTitle.setIncludeFontPadding(false);sectionTitle.setPadding(dp(9),dp(2),dp(9),dp(2));android.graphics.drawable.GradientDrawable sectionFrame=rounded(0xffedf4eb,0xffc5ae76,8);sectionFrame.setStroke(Math.max(1,Math.round(getResources().getDisplayMetrics().density*0.7f)),0xffc5ae76);sectionTitle.setBackground(sectionFrame);headingBand.addView(sectionTitle,new LinearLayout.LayoutParams(-2,-2));body.addView(headingBand,new LinearLayout.LayoutParams(-1,-2));
    renderBoxReading(body,content,size,key);hasContent=true;pendingBlank=Math.max(0,trailing-1);
   }
   return;
  }
  final Runnable[] finishActive={null};
  ArrayList<String> groupNames=new ArrayList<>(java.util.Arrays.asList("Pembukaan","Pengisian Ilmiah","Pendidikan Hati","Kefaqihan","Penghayatan")); boolean grouped=groupedLecture(names)||prefs.getBoolean("custom_groups_"+key,false);int previousGroup=-1;LinearLayout groupBody=body;final boolean[] manage={false};ArrayList<View> management=new ArrayList<>();if(grouped){TextView manageButton=text("☷  Urus subtopik",12);manageButton.setTextColor(0xff397e58);body.addView(manageButton);TextView saveTemplate=text("◇ Simpan Sebagai Template",12);saveTemplate.setTextColor(0xff397e58);body.addView(saveTemplate);management.add(saveTemplate);saveTemplate.setVisibility(View.GONE);saveTemplate.setOnClickListener(v->{if(finishActive[0]!=null)finishActive[0].run();saveLectureTemplate(key,new ArrayList<>(names));});TextView addCategory=text("＋ Tambah Kategori",12);addCategory.setTextColor(0xff397e58);body.addView(addCategory);management.add(addCategory);addCategory.setVisibility(View.GONE);addCategory.setOnClickListener(v->{if(finishActive[0]!=null)finishActive[0].run();LinearLayout fields=new LinearLayout(this);fields.setOrientation(1);EditText category=new EditText(this);category.setHint("Nama kategori");category.setSingleLine(true);fields.addView(category);EditText topic=new EditText(this);topic.setHint("Nama subtopik pertama");topic.setSingleLine(true);fields.addView(topic);new AlertDialog.Builder(this).setTitle("Tambah Kategori").setView(fields).setNegativeButton("Batal",null).setPositiveButton("Tambah",(d,w)->{String cat=category.getText().toString().trim(),sub=topic.getText().toString().trim();if(cat.isEmpty()||sub.isEmpty()||names.contains(sub)){Toast.makeText(this,"Isi nama kategori dan subtopik baharu",Toast.LENGTH_SHORT).show();return;}prefs.edit().putBoolean("custom_groups_"+key,true).putString("lecture_group_"+key+"_"+sub,cat).apply();changeLectureSection(key,names.size()-1,sub);}).show();});manageButton.setOnClickListener(v->{manage[0]=!manage[0];for(View control:management)control.setVisibility(manage[0]?View.VISIBLE:View.GONE);});}
  for(int i=0;i<names.size();i++){
   int sectionGroup=grouped?lectureGroup(names.get(i)):-1;
   String customGroup=prefs.getString("lecture_group_"+key+"_"+names.get(i),"");if(grouped&&!customGroup.isEmpty()){if(!groupNames.contains(customGroup))groupNames.add(customGroup);sectionGroup=groupNames.indexOf(customGroup);}
   if(grouped&&sectionGroup<0)sectionGroup=previousGroup>=0?previousGroup:0;
   if(grouped&&sectionGroup!=previousGroup||grouped&&i==0){groupBody=body;if(sectionGroup>=0){LinearLayout band=new LinearLayout(this);band.setGravity(Gravity.TOP);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,dp(6),0,dp(6));body.addView(band,bp);View stripe=new View(this);stripe.setBackground(rounded(lectureGroupColor(sectionGroup%5),lectureGroupColor(sectionGroup%5),3));band.addView(stripe,new LinearLayout.LayoutParams(dp(3),-1));groupBody=new LinearLayout(this);groupBody.setOrientation(1);groupBody.setPadding(dp(8),0,0,0);band.addView(groupBody,new LinearLayout.LayoutParams(0,-2,1));TextView label=text(groupNames.get(sectionGroup),13);label.setTextColor(lectureGroupColor(sectionGroup%5));groupBody.addView(label);}}
   previousGroup=sectionGroup;
   String value=source.substring(ends.get(i),i+1<starts.size()?starts.get(i+1):source.length()).trim();
   if(value.isEmpty()&&mode==2)continue;
   final String name=names.get(i);
   LinearLayout group=new LinearLayout(this);group.setOrientation(1);
   if(grouped){groupBody.addView(group,new LinearLayout.LayoutParams(-1,-2));}else if(mode!=2){group.setBackground(rounded(0xfffffefd,0xffdce5df,8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(sectionGroup>=0?2:3),0,dp(sectionGroup>=0?2:3));body.addView(group,lp);}else body.addView(group);
   TextView h=text("",Math.max(18,size));h.setText(formattedReadingText(name));h.setTextColor(0xff216e4e);h.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));h.setPadding(dp(12),dp(12),dp(12),dp(12));h.setMinHeight(dp(52));if(sectionGroup>=0){h.setTextColor(0xff303633);h.setTypeface(android.graphics.Typeface.DEFAULT);h.setTextSize(Math.max(14,size-1));h.setPadding(dp(10),dp(4),dp(10),dp(4));h.setMinHeight(dp(34));}LinearLayout headerRow=new LinearLayout(this);headerRow.setGravity(Gravity.CENTER_VERTICAL);group.addView(headerRow);TextView arrow=text(mode==1?"▾":(grouped?"›":"▾"),sectionGroup>=0?14:18);arrow.setTextColor(0xff216e4e);arrow.setPadding(dp(8),0,dp(12),0);final int moveIndex=i;if(grouped)headerRow.addView(arrow);headerRow.addView(h,new LinearLayout.LayoutParams(0,-2,1));
   for(int direction:new int[]{-1,1}){
    final int delta=direction;TextView move=text(delta<0?"↑":"↓",sectionGroup>=0?14:17);move.setGravity(Gravity.CENTER);move.setTextColor(0xff216e4e);headerRow.addView(move,new LinearLayout.LayoutParams(dp(sectionGroup>=0?26:30),dp(sectionGroup>=0?36:44)));if(grouped){management.add(move);move.setVisibility(View.GONE);}move.setEnabled(moveIndex+delta>=0&&moveIndex+delta<names.size());move.setAlpha(move.isEnabled()?1f:0.3f);move.setContentDescription(delta<0?"Pindah ke atas":"Pindah ke bawah");move.setOnClickListener(v->{if(finishActive[0]!=null)finishActive[0].run();moveLectureSection(key,moveIndex,delta);});
   }
   if(!grouped)headerRow.addView(arrow);else{for(String symbol:new String[]{"+","⧉","×"}){TextView control=text(symbol,14);control.setGravity(Gravity.CENTER);control.setTextColor(0xff397e58);headerRow.addView(control,new LinearLayout.LayoutParams(dp(26),dp(36)));management.add(control);control.setVisibility(View.GONE);control.setContentDescription(symbol.equals("+")?"Tambah subtopik":symbol.equals("⧉")?"Salin subtopik ke nota lain":"Buang subtopik");control.setOnClickListener(v->{if(finishActive[0]!=null)finishActive[0].run();if(symbol.equals("⧉")){copyLectureSectionToNote(key,moveIndex);return;}if(symbol.equals("+")){EditText input=new EditText(this);input.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Tambah subtopik selepas "+name).setView(input).setNegativeButton("Batal",null).setPositiveButton("Tambah",(d,w)->{String title=input.getText().toString().trim();if(!title.isEmpty())changeLectureSection(key,moveIndex,title);}).show();}else new AlertDialog.Builder(this).setTitle("Buang "+name+"?").setMessage("Kandungan subtopik ini juga akan dibuang.").setNegativeButton("Batal",null).setPositiveButton("Buang",(d,w)->changeLectureSection(key,moveIndex,null)).show();});}}
   LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(8),0,dp(8),dp(6));group.addView(panel);
   if(value.isEmpty()){
    TextView empty=text("Tiada kandungan",Math.max(13,size-2));empty.setTextColor(0xff718078);empty.setPadding(dp(4),dp(8),dp(4),dp(8));panel.addView(empty);
   }else renderBoxReading(panel,value,size,key);
   final int sectionIndex=i;
   TextView quick=text("✎  Edit pantas",13);quick.setTextColor(0xff216e4e);quick.setPadding(dp(4),dp(8),dp(4),dp(4));panel.addView(quick);
   quick.setOnClickListener(v->{
    if(finishActive[0]!=null)finishActive[0].run();
    for(int j=0;j<panels.size();j++){
     if(panels.get(j)==panel)continue;
     panels.get(j).setVisibility(View.GONE);
     headers.get(j).setText("▾");
    }
    panel.setVisibility(View.VISIBLE);
    arrow.setText("▴");
    String raw=prefs.getString(key,"");
    java.util.regex.Matcher matcher=lectureHeadingPattern().matcher(raw);
    ArrayList<Integer> ss=new ArrayList<>(),ee=new ArrayList<>();
    while(matcher.find()){ss.add(matcher.start());ee.add(matcher.end());}
    if(sectionIndex>=ee.size())return;
    int start=ee.get(sectionIndex),end=sectionIndex+1<ss.size()?ss.get(sectionIndex+1):raw.length();
    EditText input=new EditText(this);input.setTextSize(size);input.setGravity(Gravity.TOP);input.setMinLines(3);
    input.setText(formattedReadingText(raw.substring(start,end).trim()));
    panel.removeAllViews();panel.addView(input);
    refreshEditorBoxes(input.getText());
    final String[] expected={raw};
    final int[] boundary={end};
    final String[] last={encodeLectureEditable(input.getText())};
    final boolean[] backedUp={false};
    Runnable persist=()->{
     String edited=encodeLectureEditable(input.getText());
     if(edited.equals(last[0]))return;
     if(!expected[0].equals(prefs.getString(key,""))){Toast.makeText(this,"Nota telah berubah. Buka semula.",Toast.LENGTH_SHORT).show();return;}
     String replacement="\n"+edited+"\n";
     String updated=expected[0].substring(0,start)+replacement+expected[0].substring(boundary[0]);
     if(!backedUp[0]){
      snapshotBeforeAutosave(key,raw,prefs.getString("title_"+key,""),prefs.getString("cat_"+key,"Tanpa kategori"));backedUp[0]=true;
     }
     prefs.edit().putString(key,updated).putLong("updated_"+key,System.currentTimeMillis()).apply();
     expected[0]=updated;boundary[0]=start+replacement.length();last[0]=edited;
    };
    input.addTextChangedListener(new TextWatcher(){
     public void beforeTextChanged(CharSequence t,int st,int c,int after){}
     public void onTextChanged(CharSequence t,int st,int before,int c){}
     public void afterTextChanged(Editable e){
      persist.run();
      if(android.view.inputmethod.BaseInputConnection.getComposingSpanStart(e)<0)refreshEditorBoxes(e);
     }
    });
    Runnable finish=()->{
     persist.run();finishActive[0]=null;
     panel.removeAllViews();renderBoxReading(panel,encodeLectureEditable(input.getText()),size,key);panel.addView(quick);
     panel.setVisibility(View.GONE);arrow.setText(grouped?"›":"▾");
    };
    finishActive[0]=finish;
    input.requestFocus();
   });
   panels.add(panel);headers.add(arrow);
   panel.setVisibility(mode==0?View.GONE:View.VISIBLE);
   if(mode!=2)headerRow.setOnClickListener(v->{boolean open=panel.getVisibility()!=View.VISIBLE;if(finishActive[0]!=null)finishActive[0].run();panel.setVisibility(open?View.VISIBLE:View.GONE);arrow.setText(open?"▾":(grouped?"›":"▾"));});
  }
 }
 void readContentSafely(String key){
  final boolean restoreReading=resumeNextReading||(key!=null&&key.equals(selected)&&readerScroll!=null&&editor==null);resumeNextReading=false;
  if("m_qurtubi_fatihah_1".equals(key))appendQurtubiLecture();
  if(key!=null&&prefs.contains("pdf_original_"+key)){showOriginalPdf(key);return;}
  stopAutoReading();
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
  lectureBackReveal=null;
  editor=null;titleEditor=null;saveIndicator=null;selected=key;prefs.edit().putLong("viewed_"+key,System.currentTimeMillis()).putString(key.startsWith("m_")?"reading_last_m":"reading_last_n",key).apply();cleanBase();
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(5),dp(1),dp(5),dp(1));bar.setBackground(rounded(0xfffffefd,0xffcbd8cf,9));bar.setTag("reader_toolbar");bar.setBaselineAligned(false);root.addView(bar);
  boolean cleanReading=prefs.getBoolean("reader_clean_mode",false);
  if(cleanReading)enableLectureZoom(key);
  final ArrayList<View> lecturePanels=new ArrayList<>();
  final ArrayList<TextView> lectureArrows=new ArrayList<>();
  final ArrayList<View> lectureHeadings=new ArrayList<>();
  final ArrayList<CharSequence> lectureTitles=new ArrayList<>();

  Button readerBack=action("‹",bar,()->{
   if(cleanReading){saveReaderPosition(key);prefs.edit().putBoolean("reader_clean_mode",false).apply();read(key);}
   else returnFromReader();
  });
  readerBack.setContentDescription(cleanReading?"Keluar mod kuliah":(key.startsWith("m_")?"Kembali ke senarai kitab":"Kembali ke senarai nota"));
  TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
  if(cleanReading){
   // The Back control is placed as an overlay on the reading surface below.
   root.removeView(bar);
   // Lecture mode: the normal back control is the only visible toolbar action.
   // Keep the same ScrollView and its touch scrolling, including Fold6 reflow.
   if(root.getChildCount()>0)root.getChildAt(0).setVisibility(View.GONE);
   getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   if(android.os.Build.VERSION.SDK_INT>=30){
    android.view.WindowInsetsController controller=getWindow().getInsetsController();
    if(controller!=null)controller.hide(android.view.WindowInsets.Type.statusBars()|android.view.WindowInsets.Type.navigationBars());
   }else getWindow().getDecorView().setSystemUiVisibility(5894);
  }else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  if(!cleanReading){
  Button selectTextButton=action("▤",bar,()->selectLongReadingText(key));
  selectTextButton.setContentDescription("Pilih dan salin teks merentasi perenggan");
  Button cleanToggle=action(cleanReading?"▣":"▢",bar,()->{saveReaderPosition(key);prefs.edit().putBoolean("reader_clean_mode",true).apply();read(key);});
  cleanToggle.setContentDescription("Aktifkan mod kuliah skrin penuh");
  } // End normal reading toolbar; lecture mode retains Back only.
  if(!cleanReading){
  
  if(isLectureTemplate(selectedReadingText(key))){
   Button lectureMode=action("☷",bar,()->new AlertDialog.Builder(this).setTitle("Paparan Nota Kuliah").setItems(new String[]{"Collapse All","Expand All","Normal View"},(d,i)->{prefs.edit().putInt("lecture_view_"+key,i).apply();try{readContentSafely(key);}catch(Exception failure){showReaderRecovery(key,failure);}}).show());
   lectureMode.setContentDescription("Paparan Nota Kuliah");
  }
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
   verticalScroll.setTextColor(0xff216e4e);
  }
  }
  if(!compactReaderToolbar){
  Button layoutButton=action("▥",bar,()->{boolean now=prefs.getBoolean("reader_two_columns",availableReaderWidthDp>=600);prefs.edit().putBoolean("reader_two_columns",!now).apply();read(key);});layoutButton.setContentDescription("Tukar paparan satu atau dua kolum");layoutButton.setTextColor(0xff216e4e); Button readerOptions=action("⋮",bar,()->showReaderActions(key,compactReaderToolbar));readerOptions.setContentDescription(key.startsWith("m_")?"Menu tindakan kitab":"Menu tindakan nota");
  }
  }
  if(!cleanReading&&isLectureTemplate(selectedReadingText(key))&&prefs.getInt("lecture_view_"+key,0)==3){addLectureModeBar(root,key,3,false);renderLectureMindMap(key,15);return;}
  // Page-by-page reader for long Nota and Maktabah content.
  if(selectedReadingText(key).length()>=100 && availableReaderWidthDp>=500 && prefs.getBoolean("reader_swipe_mode",false) && (!isLectureTemplate(selectedReadingText(key)) || prefs.getInt("lecture_view_"+key,0)==2)){ // Optional unfolded swipe reader.
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
  String fullReadingText=selectedReadingText(key);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
  readerScroll=scroll;
  LinearLayout body=new LinearLayout(this);body.setOrientation(1);
  int paperColor=0xfffffefd;body.setBackground(rounded(paperColor,0xffb8c2ca,12));
  body.setPadding(dp(8),dp(2),dp(8),dp(8));body.setClipChildren(true);body.setClipToPadding(true);
  LinearLayout.LayoutParams readingFrame=new LinearLayout.LayoutParams(-1,-2);
  readingFrame.setMargins(dp(4),dp(4),dp(4),dp(2));scroll.addView(body,readingFrame);
  LinearLayout metaRow=new LinearLayout(this);metaRow.setOrientation(LinearLayout.HORIZONTAL);
  metaRow.setGravity(Gravity.CENTER_VERTICAL);metaRow.setPadding(dp(16),dp(1),dp(16),dp(4));
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
  // Lift the two metadata labels visibly without changing the title or reading boxes.
  sectionLabel.setTranslationY(-dp(7));
  categoryLine.setTranslationY(-dp(7));
  if(!cleanReading){if(isLectureTemplate(fullReadingText)&&!fullReadingText.contains(" || "))addLectureModeBar(root,key,prefs.getInt("lecture_view_"+key,0),false);else body.addView(metaRow,new LinearLayout.LayoutParams(-1,-2));}
  TextView title=text(heading,20);title.setMaxLines(Integer.MAX_VALUE);
  // Preserve complete long titles, including multi-line Arabic, without clipping.
  title.setEllipsize(null);title.setLineSpacing(dp(2),1.05f);
  // Minimal GitHub-inspired heading: no surrounding box, just a slim emerald rule.
  title.setBackgroundColor(Color.TRANSPARENT);
  title.setTextColor(0xff344054);title.setTypeface(null,android.graphics.Typeface.BOLD);
  title.setPadding(dp(12),dp(5),dp(8),dp(5));
  title.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);title.setTextIsSelectable(true);
  title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setIncludeFontPadding(true);
  LinearLayout deferredLectureTitle=null;
  if(!cleanReading&&!heading.isEmpty()){
   LinearLayout titleRow=new LinearLayout(this);titleRow.setOrientation(LinearLayout.HORIZONTAL);
   titleRow.setGravity(Gravity.CENTER_VERTICAL);
   titleRow.setPadding(dp(12),dp(3),dp(8),dp(5));
   View emeraldRule=new View(this);emeraldRule.setBackground(rounded(0xff216e4e,0xff216e4e,2));
   LinearLayout.LayoutParams ruleParams=new LinearLayout.LayoutParams(dp(3),dp(30));
   titleRow.addView(emeraldRule,ruleParams);
   titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
   if(isLectureTemplate(fullReadingText)&&!fullReadingText.contains(" || ")){deferredLectureTitle=titleRow;titleRow.setPadding(dp(12),0,dp(8),dp(2));title.setPadding(dp(12),dp(2),dp(8),dp(2));}else body.addView(titleRow);
  }
  String firstReadingLine=fullReadingText.split("\\R",2)[0].trim();
  if(!cleanReading&&firstReadingLine.matches("(?i)^(bab|الفصل|باب)\\b.*")&&firstReadingLine.length()<110){
   TextView chapterLabel=text(firstReadingLine,16);
   chapterLabel.setTextColor(0xff566a70);chapterLabel.setTypeface(null,android.graphics.Typeface.BOLD);
   chapterLabel.setPadding(dp(20),dp(8),dp(20),dp(8));body.addView(chapterLabel);
  }
  int fontLevel=Math.max(0,Math.min(3,prefs.getInt("reader_font_level",1)));
  int[] readingSizes=new int[]{13,15,16,18}; // Match editor for both Nota and Maktabah.
  TextView content=text("",readingSizes[fontLevel]);content.setText(readerLabelText(fullReadingText));boolean containsArabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(fullReadingText).find();if(containsArabic){if(prefs.getBoolean("reader_arabic_clear_font",false))content.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));}
  content.setTextColor(0xff303c43);content.setGravity(Gravity.TOP);content.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);content.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);content.setTextIsSelectable(true);content.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);int readingSide=dp(4); // 12dp frame + 8dp body + 4dp content = 24dp editor inset.
  content.setPadding(readingSide,dp(10),readingSide,dp(12));if(prefs.getBoolean("reader_wide_spacing",false))content.setLineSpacing(dp(5),1.15f);else content.setLineSpacing(dp(1),1.0f);content.setIncludeFontPadding(true);applySharedTextMetrics(content);
  // Display list rows with subtle mint dividers; preserve the original stored text.
  String[] displayLines=fullReadingText.split("\\n",-1);
  boolean hasBoxes=fullReadingText.matches("(?s).*---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---.*");
  boolean hasLists=hasBoxes;
  for(String line:displayLines)if(line.trim().matches("---(?:FLOWER|GEOMETRIC|CLASSIC)?---|---")){hasLists=true;break;}
  for(String line:displayLines)if(line.matches("^\\s*(?:\\d+\\)\\s+|[•☐☑✓]\\s+).*")){hasLists=true;break;}
  // Optional aligned bilingual reading. Each paired line uses Arabic || Malay.
  boolean qurtubiPair=key.equals("m_qurtubi_fatihah_1")&&fullReadingText.contains(" || ");
  boolean bilingualReading=qurtubiPair||(key.startsWith("m_")&&availableReaderWidthDp>=500
   &&prefs.getBoolean("reader_bilingual_columns",false)&&fullReadingText.contains(" || "));
  if(bilingualReading){
   LinearLayout bilingual=new LinearLayout(this);bilingual.setOrientation(LinearLayout.VERTICAL);
   for(String line:displayLines){
    int divider=line.indexOf(" || ");
    if(divider<0){if(qurtubiPair&&line.trim().equals("---TAMAT---"))continue;TextView plain=text(line,readingSizes[fontLevel]);plain.setText(readerLabelText(line));plain.setTextColor(0xff303c43);bilingual.addView(plain);continue;}
    LinearLayout pair=new LinearLayout(this);pair.setOrientation(LinearLayout.HORIZONTAL);
    TextView arabic=text(line.substring(0,divider),readingSizes[fontLevel]);
    arabic.setTextDirection(View.TEXT_DIRECTION_RTL);arabic.setGravity(Gravity.RIGHT|Gravity.TOP);
    arabic.setTextIsSelectable(true);arabic.setPadding(dp(8),dp(6),dp(8),dp(6));
    TextView malay=text(line.substring(divider+4),readingSizes[fontLevel]);
    malay.setTextDirection(View.TEXT_DIRECTION_LTR);malay.setGravity(Gravity.LEFT|Gravity.TOP);
    malay.setTextIsSelectable(true);malay.setPadding(dp(8),dp(6),dp(8),dp(6));
    if(qurtubiPair){pair.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);if(availableReaderWidthDp<500){pair.setOrientation(LinearLayout.VERTICAL);pair.addView(arabic,new LinearLayout.LayoutParams(-1,-2));pair.addView(malay,new LinearLayout.LayoutParams(-1,-2));}else{pair.addView(malay,new LinearLayout.LayoutParams(0,-2,1));View separator=new View(this);separator.setBackgroundColor(0xffb7c1c9);pair.addView(separator,new LinearLayout.LayoutParams(dp(1),-1));pair.addView(arabic,new LinearLayout.LayoutParams(0,-2,1));}bilingual.addView(pair);continue;}
    pair.addView(arabic,new LinearLayout.LayoutParams(0,-2,1));
    View rule=new View(this);rule.setBackgroundColor(0xffb7c1c9);
    pair.addView(rule,new LinearLayout.LayoutParams(dp(1),-1));
    pair.addView(malay,new LinearLayout.LayoutParams(0,-2,1));
    bilingual.addView(pair);
   }
   body.addView(bilingual);
  }
  boolean twoColumnReading=availableReaderWidthDp>=500&&prefs.getBoolean("reader_two_columns",true)&&fullReadingText.length()>=100;
  if(isLectureTemplate(fullReadingText)&&!bilingualReading){
   int lectureStart=body.getChildCount();
   renderLectureTemplate(body,fullReadingText,readingSizes[fontLevel],key);
   if(deferredLectureTitle!=null){
    int menuIndex=lectureStart;
    java.util.regex.Matcher headingMatcher=lectureHeadingPattern().matcher(fullReadingText);
    if(prefs.getInt("lecture_view_"+key,0)!=2&&headingMatcher.find()&&headingMatcher.start()>0)menuIndex++;
    body.addView(deferredLectureTitle,menuIndex);lectureStart++;
   }
   if(twoColumnReading){
    int first=lectureStart;
    if(prefs.getInt("lecture_view_"+key,0)!=2 && lectureHeadingPattern().matcher(fullReadingText).find()){
     java.util.regex.Matcher firstHeading=lectureHeadingPattern().matcher(fullReadingText);
     if(firstHeading.find()&&firstHeading.start()>0)first++;
    }
    // Lecture controls now live above the scrolling content.
    int count=body.getChildCount()-first;
    if(count>1){
     LinearLayout columns=new LinearLayout(this);columns.setOrientation(LinearLayout.HORIZONTAL);
     LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);
     LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);
     columns.addView(left,new LinearLayout.LayoutParams(0,-2,1));
     android.widget.ImageView ornament=new android.widget.ImageView(this);
     ornament.setImageResource(R.drawable.reader_center_ornament);
     ornament.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
     FrameLayout lectureDivider=new FrameLayout(this);
     View dividerLine=new View(this);dividerLine.setBackgroundColor(0xffd4e2da);
     lectureDivider.addView(dividerLine,new FrameLayout.LayoutParams(dp(1),-1,Gravity.CENTER));
     lectureDivider.addView(ornament,new FrameLayout.LayoutParams(dp(16),dp(40),Gravity.CENTER));
     columns.addView(lectureDivider,new LinearLayout.LayoutParams(dp(24),-1));
     columns.addView(right,new LinearLayout.LayoutParams(0,-2,1));
     // Measure rendered blocks at column width, preserving rich spans and whole boxes.
     ArrayList<View> blocks=new ArrayList<>();
     for(int i=0;i<count;i++){
      View child=body.getChildAt(first);body.removeViewAt(first);
      if(prefs.getInt("lecture_view_"+key,0)==2 && child instanceof TextView){
       TextView original=(TextView)child;
       CharSequence rich=original.getText();
       java.util.regex.Matcher breaks=java.util.regex.Pattern.compile("\\n[ \\t]*\\n+").matcher(rich);
       int start=0;
       while(breaks.find()){
        int end=breaks.end();
        blocks.add(copyLectureColumnText(original,rich.subSequence(start,breaks.start())));start=end;
       }
       if(start<rich.length())blocks.add(copyLectureColumnText(original,rich.subSequence(start,rich.length())));
      }else blocks.add(child);
     }
     int columnWidth=Math.max(dp(80),(readerBuiltWidthPx-dp(72))/2);
     int totalHeight=0;
     int[] heights=new int[blocks.size()];
     for(int i=0;i<blocks.size();i++){
      View child=blocks.get(i);
      ViewGroup.LayoutParams params=child.getLayoutParams();
      int horizontal=0,vertical=0;
      if(params instanceof ViewGroup.MarginLayoutParams){
       ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)params;
       horizontal=margins.leftMargin+margins.rightMargin;vertical=margins.topMargin+margins.bottomMargin;
      }
      child.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,columnWidth-horizontal),View.MeasureSpec.EXACTLY),
       params!=null&&params.height>=0?View.MeasureSpec.makeMeasureSpec(params.height,View.MeasureSpec.EXACTLY):View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
      heights[i]=child.getMeasuredHeight()+vertical;totalHeight+=heights[i];
     }
     int midpoint=blocks.size(),used=0,best=Integer.MAX_VALUE;
     for(int i=1;i<blocks.size();i++){
      used+=heights[i-1];int difference=Math.abs(totalHeight-2*used);
      if(difference<best){best=difference;midpoint=i;}
     }
     // Allow a prose paragraph to continue across the gutter at a measured line boundary.
     int prefixHeight=0;
     for(int i=0;i<blocks.size();i++){
      View child=blocks.get(i);
      if(prefs.getInt("lecture_view_"+key,0)==2 && child instanceof TextView
       && prefixHeight<totalHeight/2 && prefixHeight+heights[i]>totalHeight/2){
       TextView paragraph=(TextView)child;
       android.text.Layout layout=paragraph.getLayout();
       if(layout!=null && layout.getLineCount()>1){
        int target=totalHeight/2-prefixHeight-paragraph.getPaddingTop();
        int line=0;
        while(line+1<layout.getLineCount() && layout.getLineBottom(line)<=target)line++;
        int offset=layout.getLineStart(line);
        CharSequence rich=paragraph.getText();
        if(offset>0 && offset<rich.length()){
         left.addView(copyLectureColumnText(paragraph,rich.subSequence(0,offset)));
         right.addView(copyLectureColumnText(paragraph,rich.subSequence(offset,rich.length())));
         for(int j=i+1;j<blocks.size();j++)right.addView(blocks.get(j));
         break;
        }
       }
      }
      (i<midpoint?left:right).addView(child);
      prefixHeight+=heights[i];
     }
     body.addView(columns);
    }
   }
  }
  else if(hasBoxes){
   if(bilingualReading){body.removeViewAt(body.getChildCount()-1);}
   renderBoxReading(body,fullReadingText,readingSizes[fontLevel],key);
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
    android.text.SpannableStringBuilder formatted=readerLabelText(fullReadingText);
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
     // Multiple source blank lines before a numbered heading should not create
     // oversized gaps in reading mode. Preserve one small visual pause only.
     if(after<displayLines.length&&displayLines[after].matches("^\\s*\\d+\\)\\s+.*$")){
      if(rowIndex==before+1){
       View headingGap=new View(this);
       readingRows.addView(headingGap,new LinearLayout.LayoutParams(-1,dp(4)));
      }
      continue;
     }
    }
    java.util.regex.Matcher numbered=java.util.regex.Pattern.compile("^\\s*(\\d+)\\)\\s+(.*)$").matcher(line);
    java.util.regex.Matcher marked=java.util.regex.Pattern.compile("^\\s*([•☐☑✓])\\s+(.*)$").matcher(line);
    boolean isNumber=numbered.matches(),isMarked=!isNumber&&marked.matches();
    java.util.regex.Matcher iconHeading=java.util.regex.Pattern.compile("^\\s*@#(?:\\s+(.+))?$").matcher(line);
    boolean isIconHeading=!isNumber&&iconHeading.matches();
    if(line.trim().equals("---")||line.trim().equals("---FLOWER---")||line.trim().equals("---GEOMETRIC---")||line.trim().equals("---CLASSIC---")){
     readingRows.addView(islamicOrnamentView(line.trim().equals("---")?"GEOMETRIC":line.trim().replace("---","")),new LinearLayout.LayoutParams(-1,dp(34)));
    }else if(isNumber||isMarked||isIconHeading){
     LinearLayout itemRow=new LinearLayout(this);itemRow.setOrientation(LinearLayout.HORIZONTAL);
     itemRow.setGravity(Gravity.CENTER_VERTICAL);itemRow.setPadding(dp(6),dp(2),dp(6),dp(2));
     String marker=isNumber?numbered.group(1):(isIconHeading?"❖":marked.group(1));
     String value=isNumber?numbered.group(2):(isIconHeading?(iconHeading.group(1)==null?"Huraian":iconHeading.group(1)):marked.group(2));
     boolean arabicHeading=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(value).find();
     // Pearl-and-gold medallion; Arabic headings put the number on the right.
     String displayMarker=marker;
     if(isNumber&&arabicHeading){
      StringBuilder digits=new StringBuilder();
      for(int j=0;j<marker.length();j++){
       char digit=marker.charAt(j);
       digits.append(digit>='0'&&digit<='9'?(char)('٠'+digit-'0'):digit);
      }
      displayMarker=digits.toString();
     }
     TextView markerView=isIconHeading?centeredDiamondMarker(17):text(displayMarker,17);
     markerView.setGravity(Gravity.CENTER);
     markerView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
     markerView.setIncludeFontPadding(false);
     markerView.setPadding(0,0,0,0);
     markerView.setSingleLine(true);
     markerView.setTextColor(marker.equals("•")?0xffc9b478:0xff176b47);if(marker.equals("•")){markerView.setText("●");markerView.setTextSize(18); }
     // Arabic-Indic digits sit optically low in some Android fonts.
     // Centre the glyph within the two rings without changing the 17sp size.
     if(arabicHeading&&isNumber){
      markerView.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));
      markerView.setTranslationY(-dp(1));
     }
     android.graphics.drawable.GradientDrawable medal=new GoldGlowDrawable(0);
     medal.setShape(android.graphics.drawable.GradientDrawable.OVAL);
     medal.setColor(0xfffcf8ed);
     medal.setStroke(dp(1),0xffc3a35a);
     // Two concentric circles: a subtle outer gold ring and a brighter pearl centre.
     FrameLayout medallion=new FrameLayout(this);
     android.graphics.drawable.GradientDrawable outerCircle=new GoldGlowDrawable(0);
     outerCircle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
     outerCircle.setColor(0xfff3ecd9);
     outerCircle.setStroke(dp(1),0xffc3a35a);
     medallion.setBackground(outerCircle);
     medal.setColor(0xfffffff9);
     medal.setStroke(dp(1),0xffdfc98d);
     markerView.setBackground(medal);
     FrameLayout.LayoutParams innerCircleParams=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);
     innerCircleParams.setMargins(dp(2),dp(2),dp(2),dp(2));
     medallion.addView(markerView,innerCircleParams);
     LinearLayout.LayoutParams medalParams=new LinearLayout.LayoutParams(dp(25),dp(25));
     if(arabicHeading)medalParams.leftMargin=dp(2);else medalParams.rightMargin=dp(1);
     if(!arabicHeading)itemRow.addView(medallion,medalParams);
     TextView valueView=text("",readingSizes[fontLevel]);valueView.setText(readerLabelText(value));applySharedTextMetrics(valueView);valueView.setTextColor(0xff303c43);
     valueView.setTextDirection(arabicHeading?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_FIRST_STRONG);valueView.setTextIsSelectable(true);
     valueView.setGravity((arabicHeading?Gravity.RIGHT:Gravity.LEFT)|Gravity.CENTER_VERTICAL);
     valueView.setPadding(dp(1),0,dp(1),0);
     valueView.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);
     itemRow.addView(valueView,new LinearLayout.LayoutParams(0,-2,1));
     if(arabicHeading)itemRow.addView(medallion,medalParams);
     // Expandable commentary stored separately from the source note.
     if(isIconHeading||isNumber){
      final String commentaryKey="syarahan_"+key+"_"+rowIndex;
      LinearLayout commentaryPanel=new LinearLayout(this);
      commentaryPanel.setOrientation(LinearLayout.VERTICAL);
      commentaryPanel.setPadding(dp(10),dp(6),dp(10),dp(8));
      commentaryPanel.setBackground(rounded(0xfff8faf5,0xffd9e4d6,10));
      TextView commentaryLabel=text("Syarahan / Huraian",13);
      commentaryLabel.setTextColor(0xff176b47);
      commentaryLabel.setPadding(dp(4),0,dp(4),dp(5));
      commentaryPanel.addView(commentaryLabel);
      EditText commentaryEditor=new EditText(this);
      commentaryEditor.setText(prefs.getString(commentaryKey,""));
      commentaryEditor.setHint("Tulis syarahan di sini…");
      commentaryEditor.setTextSize(15);
      commentaryEditor.setTextColor(0xff303c43);
      // Compact empty commentary fields; keep populated commentary at its original size.
      if(commentaryEditor.length()==0){commentaryEditor.setMinLines(1);commentaryEditor.setPadding(dp(4),0,dp(4),dp(2));}
      else commentaryEditor.setMinLines(2);
      commentaryEditor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
      // Commentary may mix Arabic and Rumi in one line: keep the paragraph
      // anchored to the left while Android still shapes Arabic glyphs correctly.
      commentaryEditor.setGravity(Gravity.TOP|Gravity.LEFT);
      commentaryEditor.setTextDirection(View.TEXT_DIRECTION_LTR);
      // Arabic-only paragraphs align right; Latin and mixed paragraphs align left.
      final boolean[] adjustingCommentary={false};
      TextWatcher commentaryAlignment=new TextWatcher(){
       public void beforeTextChanged(CharSequence t,int st,int count,int after){}
       public void onTextChanged(CharSequence t,int st,int before,int count){}
       public void afterTextChanged(Editable e){
        if(adjustingCommentary[0])return;
        adjustingCommentary[0]=true;
        try{
         android.text.style.AlignmentSpan[] spans=e.getSpans(0,e.length(),android.text.style.AlignmentSpan.class);
         for(android.text.style.AlignmentSpan span:spans)e.removeSpan(span);
         String content=e.toString();
         int start=0;
         while(start<content.length()){
          int end=content.indexOf('\n',start);
          if(end<0)end=content.length();
          String paragraph=content.substring(start,end);
          boolean arabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(paragraph).find();
          boolean latin=java.util.regex.Pattern.compile("[A-Za-z]").matcher(paragraph).find();
          if(arabic&&!latin&&end>start)e.setSpan(new android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_OPPOSITE),start,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          start=end+1;
         }
        }finally{adjustingCommentary[0]=false;}
       }
      };
      commentaryEditor.addTextChangedListener(commentaryAlignment);
      commentaryAlignment.afterTextChanged(commentaryEditor.getText());
      commentaryEditor.addTextChangedListener(new TextWatcher(){
       public void beforeTextChanged(CharSequence t,int st,int count,int after){}
       public void onTextChanged(CharSequence t,int st,int before,int count){
        prefs.edit().putString(commentaryKey,t.toString()).apply();
       }
       public void afterTextChanged(Editable e){}
      });
      commentaryPanel.addView(commentaryEditor,new LinearLayout.LayoutParams(-1,-2));
      LinearLayout.LayoutParams commentaryParams=new LinearLayout.LayoutParams(-1,-2);
      commentaryParams.setMargins(dp(5),dp(2),dp(5),dp(5));
      commentaryPanel.setVisibility(isIconHeading?View.GONE:(commentaryEditor.length()>0?View.VISIBLE:View.GONE));
      View.OnClickListener toggleCommentary=v->{
       boolean opening=commentaryPanel.getVisibility()!=View.VISIBLE;
       commentaryPanel.setVisibility(opening?View.VISIBLE:View.GONE);

      };
      if(isIconHeading){lecturePanels.add(commentaryPanel);lectureArrows.add(markerView);
      lectureHeadings.add(itemRow);lectureTitles.add(readerLabelText(value));
      medallion.setOnClickListener(toggleCommentary);
      medallion.setContentDescription(isIconHeading?"Buka atau tutup huraian":("Buka atau tutup syarahan nombor "+marker));
      }
      readingRows.addView(itemRow);
      if(isIconHeading||commentaryEditor.length()>0)readingRows.addView(commentaryPanel,commentaryParams);
     }else readingRows.addView(itemRow);
     View fineDivider=new View(this);fineDivider.setBackgroundColor(0xffc5e7cb);
     readingRows.addView(fineDivider,new LinearLayout.LayoutParams(-1,dp(1)));
    }else{
     if(line.trim().isEmpty()){View paragraphGap=new View(this);readingRows.addView(paragraphGap,new LinearLayout.LayoutParams(-1,dp(8)));continue;}
     TextView paragraph=text("",readingSizes[fontLevel]);paragraph.setText(readerLabelText(line));applySharedTextMetrics(paragraph);
     paragraph.setTextColor(0xff303c43);paragraph.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
     paragraph.setTextIsSelectable(!cleanReading);if(cleanReading)paragraph.setOnClickListener(v->markLectureParagraph(paragraph));paragraph.setPadding(dp(4),dp(1),dp(4),dp(1));
     paragraph.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);readingRows.addView(paragraph);
    }
   }
  }
  String readingText=prefs.getString(key,"").trim();
  String countable=normalizeStructuralMarkers(readingText).replaceAll("(?m)^---(?:KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)|FLOWER|GEOMETRIC|CLASSIC)---[ \\t]*$","").replaceAll("</?[biu]>","").trim();
  int wordCount=0;java.util.regex.Matcher words=java.util.regex.Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}]*").matcher(countable);while(words.find())wordCount++;
  long readingUpdated=prefs.getLong("updated_"+key,0L);String updatedLabel=readingUpdated>0?new java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(new java.util.Date(readingUpdated)):"—";TextView wordCounter=text(wordCount+" perkataan  ·  Dikemas kini "+updatedLabel,12);
  wordCounter.setTextColor(0xff687078);
  wordCounter.setGravity(Gravity.LEFT|Gravity.BOTTOM);
  wordCounter.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
  wordCounter.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
  wordCounter.setPadding(dp(4),dp(10),dp(4),0);
  if(!cleanReading){if(isLectureTemplate(fullReadingText)&&!fullReadingText.contains(" || ")){sectionLabel.setTranslationY(0);categoryLine.setTranslationY(0);metaRow.setPadding(dp(4),dp(8),dp(4),0);body.addView(metaRow,new LinearLayout.LayoutParams(-1,-2));}body.addView(wordCounter);}
  if(cleanReading){
   startLectureTimer(key);
   body.setBackgroundColor(lecturePaper());
   if(lectureDark())applyLectureDark(body);
   body.setPadding(dp(3),dp(4),dp(3),dp(3));
   readingFrame.setMargins(0,0,0,0);
   FrameLayout lectureSurface=new FrameLayout(this);
   lectureSurface.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
   Button floatingBack=new Button(this);
   floatingBack.setText("‹");floatingBack.setTextSize(48);floatingBack.setAllCaps(false);
   floatingBack.setPadding(0,0,0,0);floatingBack.setMinWidth(0);floatingBack.setMinHeight(0);
   floatingBack.setBackground(rounded(0xfff3f7f3,0xffbed2c5,50));
   floatingBack.setContentDescription("Kembali ke paparan bacaan biasa");
   floatingBack.setElevation(dp(5));
   floatingBack.setOnClickListener(v->{saveReaderPosition(key);prefs.edit().putBoolean("reader_clean_mode",false).apply();read(key);});
   floatingBack.setAlpha(0.95f);
   FrameLayout.LayoutParams floatingParams=new FrameLayout.LayoutParams(dp(76),dp(76),Gravity.CENTER);
   floatingBack.setOnLongClickListener(v->{
    new AlertDialog.Builder(this).setTitle("Kawalan kuliah").setItems(new String[]{"Buka semua syarahan","Tutup semua syarahan","Indeks kuliah","Warna kertas"},(d,which)->{
     if(which==3){chooseLecturePaper(key);return;}
     if(which<2){
      for(int j=0;j<lecturePanels.size();j++){
       lecturePanels.get(j).setVisibility(which==0?View.VISIBLE:View.GONE);

      }
     }else if(lectureTitles.isEmpty())Toast.makeText(this,"Tiada tajuk bernombor",Toast.LENGTH_SHORT).show();
     else new AlertDialog.Builder(this).setTitle("Indeks kuliah").setItems(lectureTitles.toArray(new CharSequence[0]),(dialog,index)->{
      android.graphics.Rect rect=new android.graphics.Rect();
      lectureHeadings.get(index).getDrawingRect(rect);
      scroll.offsetDescendantRectToMyCoords(lectureHeadings.get(index),rect);
      scroll.smoothScrollTo(0,rect.top);
     }).show();
    }).show();
    return true;
   });
   // Do not attach the Back control until a deliberate double tap.
   Button settings=lectureSettingsButton();
   settings.setOnClickListener(v->floatingBack.performLongClick());
   final TextView scrollClock=lectureClock();
   final Runnable hideLectureBack=()->{lectureSurface.removeView(floatingBack);lectureSurface.removeView(settings);lectureSurface.removeView(scrollClock);};
   lectureBackReveal=()->{
    lectureSurface.removeCallbacks(hideLectureBack);
    if(floatingBack.getParent()==null)lectureSurface.addView(floatingBack,floatingParams);
    if(settings.getParent()==null)lectureSurface.addView(settings,lectureSettingsParams());
    revealLectureClock(lectureSurface,scrollClock);
    lectureSurface.postDelayed(hideLectureBack,3000);
   };
   root.addView(lectureSurface,new LinearLayout.LayoutParams(-1,0,1));
  }else root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
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
   if(!cleanReading)root.addView(progressFrame,new LinearLayout.LayoutParams(-1,dp(18)));
   Runnable updateReadingProgress=()->{
    if(scroll.getChildCount()==0)return;
    int maxScroll=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());
    int progress=maxScroll==0?100:Math.max(0,Math.min(100,Math.round(scroll.getScrollY()*100f/maxScroll)));
    readingProgress.setProgress(progress);
    readingPercent.setText(progress+"%");
   };
   final boolean[] positionRestored={false};
   scroll.getViewTreeObserver().addOnScrollChangedListener(()->{updateReadingProgress.run();if(positionRestored[0]&&readerScroll==scroll&&key.equals(selected)&&editor==null)saveReaderPosition(key);});
   scroll.post(()->{
    int maxScroll=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());
    int savedFraction=prefs.getInt("reading_progress_permille_"+key,-1);
    int target=!restoreReading?0:(savedFraction>=0?Math.round(maxScroll*Math.max(0,Math.min(1000,savedFraction))/1000f):prefs.getInt("reading_scroll_"+key,0));
    scroll.scrollTo(0,Math.max(0,Math.min(maxScroll,target)));
    positionRestored[0]=true;
    updateReadingProgress.run();
   });
   scroll.setOnTouchListener((v,e)->{
    if(e.getAction()==android.view.MotionEvent.ACTION_UP||e.getAction()==android.view.MotionEvent.ACTION_CANCEL)
     scroll.postDelayed(()->{
      if(selected==null||!selected.equals(key)||readerScroll!=scroll||!positionRestored[0]||scroll.getChildCount()==0)return;
      int maxScroll=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());
      int permille=maxScroll==0?0:Math.max(0,Math.min(1000,Math.round(scroll.getScrollY()*1000f/maxScroll)));
      prefs.edit().putInt("reading_scroll_"+key,scroll.getScrollY()).putInt("reading_progress_permille_"+key,permille).apply();
     },200);
    return false;
   });
  readerDetector=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){if(cleanReading){if(lectureBackReveal!=null)lectureBackReveal.run();}else bar.setVisibility(bar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);return true;}
  });
  // Touch events are observed once in dispatchTouchEvent, including selectable text.
  // Long press is a reliable alternative when double tap is intercepted by text selection.
  body.setOnLongClickListener(v->{edit(key);return true;});
  scroll.setOnLongClickListener(v->{edit(key);return true;});
  bar.setVisibility(View.VISIBLE);
 }
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
  if(lectureScale!=null){lectureScale.onTouchEvent(event);if(lectureScale.isInProgress()||event.getPointerCount()>1)return true;}
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
         .remove("trashed_at_"+k).remove("pdf_original_"+k).remove("pdf_page_"+k);
        if(!k.contains("_sample_"))tx.remove("deleted_"+k);
        if(tx.commit())Toast.makeText(this,"Dipadam kekal",Toast.LENGTH_SHORT).show();
        else Toast.makeText(this,"Gagal memadam",Toast.LENGTH_LONG).show();
        showTrash();
       }).setNegativeButton("Batal",null).show();
     }
    }).show();
  }).setNegativeButton("Tutup",null).show();
 }
 void finishEditingSafely(){finishEditingSafely(2);}
 void finishEditingSafely(int targetMode){
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
   if(finishedKey!=null){prefs.edit().putBoolean("reader_safe_scroll_"+finishedKey,true).apply();if(isLectureTemplate(prefs.getString(finishedKey,""))){switchLectureView(finishedKey,targetMode==0?0:targetMode==3?3:2);}else read(finishedKey);}
   else if(sectionOpen)notesPage();else home();
  },180);
 }
 void editNumberedCommentaries(){
  if(editor==null)return;
  save();final String key=selected;
  if(key==null)return;
  String[] lines=encodedEditorText().split("\\n",-1);
  ArrayList<Integer> positions=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
  for(int i=0;i<lines.length;i++){
   java.util.regex.Matcher m=java.util.regex.Pattern.compile("^\\s*(\\d+)\\)\\s+(.*)$").matcher(lines[i]);
   if(m.matches()){positions.add(i);labels.add(m.group(1)+") "+m.group(2));}
  }
  if(positions.isEmpty()){new AlertDialog.Builder(this).setTitle("Huraian bernombor").setMessage("Tulis senarai seperti 1) Pertama, 2) Kedua dan seterusnya terlebih dahulu.").setPositiveButton("Faham",null).show();return;}
  new AlertDialog.Builder(this).setTitle("Huraian bernombor").setItems(labels.toArray(new String[0]),(d,which)->{
   String commentaryKey="syarahan_"+key+"_"+positions.get(which);
   EditText detail=new EditText(this);detail.setMinLines(4);detail.setMaxLines(12);detail.setGravity(Gravity.TOP);detail.setHint("Tulis huraian…");detail.setText(prefs.getString(commentaryKey,""));
   new AlertDialog.Builder(this).setTitle(labels.get(which)).setView(detail).setPositiveButton("Simpan",(dd,w)->{prefs.edit().putString(commentaryKey,detail.getText().toString()).apply();Toast.makeText(this,"Huraian disimpan",Toast.LENGTH_SHORT).show();}).setNegativeButton("Batal",null).show();
  }).setNegativeButton("Tutup",null).show();
 }
 void showNoteCreationMenu(){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(14),dp(12),dp(14),dp(8));
  TextView heading=text("✎  Nota Baharu",18);heading.setTextColor(0xff216e4e);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setPadding(dp(4),dp(2),dp(4),dp(12));panel.addView(heading);
  AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).create();
  String[] labels={"Nota Umum","Nota Kuliah"};
  int[] icons={android.R.drawable.ic_menu_edit,android.R.drawable.ic_menu_agenda};
  for(int i=0;i<labels.length;i++){
   final int option=i;
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(7),dp(12),dp(7));row.setMinimumHeight(dp(48));row.setBackground(rounded(0xfffffefd,0xffc4d8cd,10));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);panel.addView(row,lp);
   android.widget.ImageView icon=new android.widget.ImageView(this);icon.setImageResource(icons[i]);icon.setColorFilter(0xff216e4e);row.addView(icon,new LinearLayout.LayoutParams(dp(20),dp(20)));
   TextView label=text(labels[i],16);label.setTextColor(0xff216e4e);label.setPadding(dp(10),0,0,0);row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
   TextView arrow=text("›",20);arrow.setTextColor(0xff719384);row.addView(arrow);
   row.setOnClickListener(v->{dialog.dismiss();if(option==0)edit(null);else createLectureTemplate();});
  }
  TextView cancel=text("Batal",14);cancel.setTextColor(0xff216e4e);cancel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);cancel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.addView(cancel);cancel.setOnClickListener(v->dialog.dismiss());
  dialog.show();android.view.Window window=dialog.getWindow();
  if(window!=null){window.setBackgroundDrawable(rounded(0xfff8faf7,0xffc5d9ce,14));window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(340)),-2);}
 }
 void saveLectureTemplate(String key,ArrayList<String> names){
  EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Nama template");
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Simpan Sebagai Template").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
   String name=input.getText().toString().trim();if(name.isEmpty()){input.setError("Masukkan nama template");return;}
   StringBuilder structure=new StringBuilder();org.json.JSONObject groups=new org.json.JSONObject();
   for(String sub:names){structure.append("\u2063").append(sub).append("\n\n\n");String cat=prefs.getString("lecture_group_"+key+"_"+sub,"");if(!cat.isEmpty())try{groups.put(sub,cat);}catch(org.json.JSONException ignored){}}
   if(structure.length()==0){input.setError("Tiada subtopik untuk disimpan");return;}
   boolean saved=prefs.edit().putString("lecture_template_body_"+name,structure.toString()).putString("lecture_template_groups_"+name,groups.toString()).commit();
   if(saved){dialog.dismiss();Toast.makeText(this,"Template disimpan: "+name,Toast.LENGTH_LONG).show();}else input.setError("Simpan gagal. Cuba lagi.");
  }));dialog.show();
 }
 void createLectureTemplate(){
  new AlertDialog.Builder(this).setTitle("Template Nota Kuliah").setItems(new String[]{"Template Asal","Template Saya","Template Kosong"},(d,i)->{
   if(i==0)createLectureTemplateWith(null,false);
   else if(i==2)createLectureTemplateWith(null,true);
   else{ArrayList<String> templates=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("lecture_template_body_"))templates.add(k.substring("lecture_template_body_".length()));java.util.Collections.sort(templates);if(templates.isEmpty()){Toast.makeText(this,"Simpan template melalui Urus Subtopik dahulu",Toast.LENGTH_LONG).show();return;}showPersonalTemplates(templates);}
  }).setNegativeButton("Batal",null).show();
 }
 void showPersonalTemplates(ArrayList<String> templates){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(14),dp(12),dp(14),dp(8));
  TextView heading=text("◇  Template Saya",17);heading.setTextColor(0xff216e4e);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setPadding(dp(4),0,0,dp(10));panel.addView(heading);
  AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).create();
  ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);panel.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(320),dp(48)*templates.size())));
  for(String name:templates){
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(5),dp(8),dp(5));row.setBackground(rounded(0xfffffefd,0xffd7c69d,9));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42));lp.bottomMargin=dp(6);list.addView(row,lp);
   TextView mark=text("❖",15);mark.setTextColor(0xffb59a55);row.addView(mark);
   TextView label=text(name,15);label.setTextColor(0xff216e4e);label.setPadding(dp(9),0,0,0);row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
   TextView arrow=text("›",19);arrow.setTextColor(0xff719384);row.addView(arrow);row.setOnClickListener(v->{dialog.dismiss();createLectureTemplateWith(name,false);});
  }
  TextView cancel=text("Batal",14);cancel.setTextColor(0xff216e4e);cancel.setGravity(Gravity.RIGHT);cancel.setPadding(dp(8),dp(8),dp(4),dp(6));panel.addView(cancel);cancel.setOnClickListener(v->dialog.dismiss());
  dialog.show();android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(rounded(0xfff8faf7,0xffc5d9ce,14));window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(340)),-2);}
 }
 void createLectureTemplateWith(String template,boolean blank){
  EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Tajuk kuliah");
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Nota Kuliah").setView(title).setNegativeButton("Batal",null).setPositiveButton("Cipta",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{
   String name=title.getText().toString().trim();if(name.isEmpty()){title.setError("Masukkan tajuk");return;}
   String key="n_"+java.util.UUID.randomUUID().toString();
   String body="\u2063Muqaddimah\n\n\n\u2063Maqasid Kuliah\n\n\n\u2063Pra Topik\n\n\n\u2063Ayat al-Quran\n\n\n\u2063Hadis Nabawi\n\n\n\u2063Kupasan Kalimah\n\n\n\u2063Nukilan Ulama\n\n\n\u2063Kalam Hikmah\n\n\n\u2063Kisah Renungan\n\n\n\u2063Perbahasan Hukum\n\n\n\u2063Kaedah Fiqh\n\n\n\u2063Realiti Semasa\n\n\n\u2063Persoalan Muhasabah\n\n\n\u2063Tuntutan Amalan\n\n\n\u2063Khulasah\n\n\n\u2063Penutup dan Doa\n\n\n";
   if(blank)body="\u2063Subtopik 1\n\n\n";else if(template!=null)body=prefs.getString("lecture_template_body_"+template,body);
   android.content.SharedPreferences.Editor changes=prefs.edit();
   if(blank||template!=null)changes.putBoolean("custom_groups_"+key,true);
   if(template!=null)try{org.json.JSONObject groups=new org.json.JSONObject(prefs.getString("lecture_template_groups_"+template,"{}"));java.util.Iterator<String> it=groups.keys();while(it.hasNext()){String sub=it.next();changes.putString("lecture_group_"+key+"_"+sub,groups.getString(sub));}}catch(org.json.JSONException ignored){}
   boolean ok=changes.putString(key,body).putString("title_"+key,name).putString("cat_"+key,"Tanpa kategori").putLong("updated_"+key,System.currentTimeMillis()).commit();
   if(ok){dialog.dismiss();maktabah=false;try{readContentSafely(key);}catch(Exception failure){showReaderRecovery(key,failure);}}else Toast.makeText(this,"Simpan gagal",Toast.LENGTH_SHORT).show();
  }));dialog.show();
 }
 void edit(String key){
  try{editContentSafely(key);}
  catch(Exception failure){
   android.util.Log.e("DinNote","Editor failed to open "+key,failure);
   // Do not delete or rewrite a damaged note. Offer recovery and backup.
   showReaderRecovery(key,failure);
  }
 }
 String markLectureHeadings(String source){
  java.util.regex.Matcher m=lectureHeadingPattern().matcher(source);
  StringBuffer out=new StringBuffer();
  while(m.find())m.appendReplacement(out,java.util.regex.Matcher.quoteReplacement("\u2063"+m.group(1)));
  m.appendTail(out);return out.toString();
 }
 void preserveLectureHeading(EditText active){
  android.text.InputFilter guard=(source,start,end,dest,dstart,dend)->{
   if(dstart>=dend)return null;
   String removed=dest.subSequence(dstart,dend).toString();
   if(removed.indexOf('\u2063')<0)return null;
   String replacement=source.subSequence(start,end).toString();
   if(replacement.indexOf('\u2063')>=0)return null;
   if(replacement.isEmpty())return null;
   if(removed.indexOf('\n')>=0)return null;
   return "\u2063"+replacement;
  };
  android.text.InputFilter[] old=active.getFilters();
  android.text.InputFilter[] next=java.util.Arrays.copyOf(old,old.length+1);next[old.length]=guard;active.setFilters(next);
 }
 void compactFormatButtons(LinearLayout formatBar,boolean compact){
  formatBar.setPadding(dp(compact?1:5),dp(2),dp(compact?1:5),dp(2));
  for(int i=0;i<formatBar.getChildCount();i++){
   View child=formatBar.getChildAt(i);
   if(!(child instanceof Button)||!(child.getLayoutParams() instanceof LinearLayout.LayoutParams))continue;
   LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)child.getLayoutParams();
   p.setMargins(dp(compact?2:3),dp(3),dp(compact?2:3),dp(3));
   child.setLayoutParams(p);child.setPadding(dp(compact?7:8),dp(2),dp(compact?7:8),dp(2));
  }
 }
 void chooseStudyReference(){
  if(editor==null)return;
  if(homeWindowWidthDp()<600){Toast.makeText(this,"Buka skrin Unfold untuk Dual View",Toast.LENGTH_SHORT).show();return;}
  if(editor.getParent() instanceof LinearLayout && "study_split".equals(((View)editor.getParent()).getTag())){closeStudyReference();return;}
  ArrayList<String> keys=new ArrayList<>(),labels=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if(k.startsWith("m_")&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)){keys.add(k);labels.add(prefs.getString("title_"+k,"Tanpa tajuk"));}
  if(keys.isEmpty()){Toast.makeText(this,"Tiada kitab dalam Maktabah",Toast.LENGTH_SHORT).show();return;}
  final EditText active=editor;
  new AlertDialog.Builder(this).setTitle("Pilih kitab rujukan").setItems(labels.toArray(new String[0]),(d,i)->{if(editor==active)openStudyReference(keys.get(i));}).setNegativeButton("Batal",null).show();
 }
 void closeStudyReference(){
  if(editor==null||!(editor.getParent() instanceof LinearLayout))return;
  LinearLayout split=(LinearLayout)editor.getParent();
  if(!"study_split".equals(split.getTag())||split.getParent()!=root)return;
  int index=root.indexOfChild(split);split.removeView(editor);root.removeView(split);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.setMargins(dp(4),0,dp(4),0);root.addView(editor,index,p);
  final EditText active=editor;active.post(()->revealEditorCursor(active));
 }
 void openStudyReference(String key){
  if(editor==null||editor.getParent()!=root||homeWindowWidthDp()<600)return;
  final EditText active=editor;int index=root.indexOfChild(active);
  LinearLayout split=new LinearLayout(this);split.setOrientation(LinearLayout.HORIZONTAL);split.setTag("study_split");
  LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);left.setBackgroundColor(0xfff5faf6);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);left.addView(header);
  TextView title=text(prefs.getString("title_"+key,"Kitab"),14);title.setTextColor(0xff216e4e);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  action("×",header,()->closeStudyReference());
  ScrollView scroll=new ScrollView(this);TextView body=text("",15);body.setText(formattedReadingText(cleanSelectableReadingText(prefs.getString(key,""))));body.setTextIsSelectable(true);body.setTextColor(0xff30343b);body.setGravity(Gravity.TOP|Gravity.START);body.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);body.setPadding(dp(10),dp(8),dp(10),dp(12));scroll.addView(body);left.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  View handle=new View(this);handle.setBackgroundColor(0xffc6ad70);
  root.removeView(active);root.addView(split,index,new LinearLayout.LayoutParams(-1,0,1));
  split.addView(left,new LinearLayout.LayoutParams(0,-1,1));split.addView(handle,new LinearLayout.LayoutParams(dp(3),-1));split.addView(active,new LinearLayout.LayoutParams(0,-1,1));
  handle.setOnTouchListener((v,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)return true;if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE){int[] xy=new int[2];split.getLocationOnScreen(xy);float f=Math.max(.30f,Math.min(.70f,(event.getRawX()-xy[0])/Math.max(1,split.getWidth())));left.setLayoutParams(new LinearLayout.LayoutParams(0,-1,f));active.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1-f));return true;}return true;});
  active.post(()->revealEditorCursor(active));
 }
 int countLectureHeadingsBefore(CharSequence value,int offset){int count=0;java.util.regex.Matcher m=lectureHeadingPattern().matcher(value);while(m.find()&&m.start()<offset)count++;return count;}
 int[] lectureQuickBounds(CharSequence value,int index){java.util.regex.Matcher m=lectureHeadingPattern().matcher(value);int i=0,start=-1;while(m.find()){if(i==index){start=m.end();if(start<value.length()&&value.charAt(start)=='\n')start++;}else if(i==index+1)return new int[]{start,m.start()};i++;}return start<0?null:new int[]{start,value.length()};}
 void attachLectureEditorPanel(final EditText active){
  if(selected!=null){LinearLayout modes=new LinearLayout(this);modes.setOrientation(1);root.addView(modes,Math.min(1,root.getChildCount()));addLectureModeBar(modes,selected,0,true);}
  active.setTransformationMethod(new LectureHeadingTransformation());
  // Subtopic management is centralized in Expand; keep the full editor visible.
  active.setVisibility(View.VISIBLE);
 }
 void showLectureEditorIndex(){
  if(editor==null)return;
  final EditText active=editor;
  java.util.regex.Matcher m=lectureHeadingPattern().matcher(active.getText());
  ArrayList<String> names=new ArrayList<>();ArrayList<Integer> offsets=new ArrayList<>();
  while(m.find()){names.add(m.group(1));offsets.add(m.end());}
  new AlertDialog.Builder(this).setTitle("Indeks Nota Kuliah").setItems(names.toArray(new String[0]),(d,i)->{
   if(editor!=active)return;
   int p=Math.min(offsets.get(i),active.length());
   if(p<active.length()&&active.getText().charAt(p)=='\n')p++;
   active.requestFocus();active.setSelection(p);active.post(()->revealEditorCursor(active));
  }).setNegativeButton("Tutup",null).show();
 }
 boolean restoringEditorCursor=false;
 void editContentSafely(String key){readerDetector=null;restoringEditorCursor=false;final int savedCursor=key==null?0:prefs.getInt("edit_cursor_"+key,0);if(key!=null){
  if(!prefs.contains(key)){
   Toast.makeText(this,"Nota tidak ditemui atau telah dipadam",Toast.LENGTH_LONG).show();
   selected=null;editor=null;titleEditor=null;categoryPicker=null;
   if(sectionOpen)notesPage();else home();
   return;
  }
  maktabah=key.startsWith("m_");prefs.edit().putString("last_edited_key",key).apply();}editor=null;saveIndicator=null;selected=key;cleanBase();LinearLayout bar=new LinearLayout(this);bar.setTag("editor_top_toolbar");bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar);Button editorBack=action("←",bar,()->finishEditingSafely());editorBack.setTextSize(23);editorBack.setTextColor(0xff176b47);editorBack.setGravity(Gravity.CENTER);editorBack.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);editorBack.setIncludeFontPadding(false);editorBack.setPadding(0,0,0,0);editorBack.setMinWidth(dp(34));editorBack.setTranslationY(-dp(1));editorBack.setContentDescription("Kembali dan simpan");saveIndicator=text("✓ Disimpan",11);saveIndicator.setTextColor(0xff566a70);saveIndicator.setTypeface(null,android.graphics.Typeface.BOLD);saveIndicator.setGravity(Gravity.CENTER);saveIndicator.setPadding(dp(9),dp(3),dp(9),dp(3));saveIndicator.setBackground(rounded(0xfff6f8fa,0xffd0d7de,8));saveIndicator.setMinHeight(dp(34));saveIndicator.setElevation(0);LinearLayout.LayoutParams savedBadgeParams=new LinearLayout.LayoutParams(-2,-2);savedBadgeParams.leftMargin=dp(5);bar.addView(saveIndicator,savedBadgeParams);TextView spacer=text("",12);bar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));final java.util.ArrayDeque<EditorSnapshot> undoHistory=new java.util.ArrayDeque<>();final java.util.ArrayDeque<EditorSnapshot> redoHistory=new java.util.ArrayDeque<>();final boolean[] restoring={false};final TextView[] historyButtons=new TextView[2];historyButtons[0]=action("↶",bar,()->{if(editor==null||undoHistory.isEmpty())return;restoring[0]=true;EditorSnapshot current=new EditorSnapshot(editor);EditorSnapshot previous=undoHistory.removeLast();redoHistory.addLast(current);previous.restore(editor);revealEditorCursor(editor);restoring[0]=false;save();historyButtons[0].setAlpha(undoHistory.isEmpty()?0.35f:1f);historyButtons[1].setAlpha(1f);});historyButtons[1]=action("↷",bar,()->{if(editor==null||redoHistory.isEmpty())return;restoring[0]=true;EditorSnapshot current=new EditorSnapshot(editor);EditorSnapshot next=redoHistory.removeLast();undoHistory.addLast(current);next.restore(editor);revealEditorCursor(editor);restoring[0]=false;save();historyButtons[0].setAlpha(1f);historyButtons[1].setAlpha(redoHistory.isEmpty()?0.35f:1f);});historyButtons[0].setContentDescription("Undo perubahan teks");historyButtons[1].setContentDescription("Redo perubahan teks");for(TextView historyButton:historyButtons){historyButton.setTextSize(23);historyButton.setTextColor(0xff146b45);historyButton.setBackground(rounded(0xfff6f8fa,0xffd0d7de,8));historyButton.setPadding(dp(9),dp(4),dp(9),dp(4));}historyButtons[0].setAlpha(0.45f);historyButtons[1].setAlpha(0.45f);Button doneButton=action("✓ Selesai",bar,()->finishEditingSafely());doneButton.setTextColor(Color.WHITE);doneButton.setTypeface(null,android.graphics.Typeface.BOLD);doneButton.setBackground(rounded(0xff216e4e,0xff216e4e,8));doneButton.setContentDescription("Selesai menyunting dan simpan nota");action("⋮",bar,()->{String[] options=new String[]{"Masukkan tarikh","Masukkan masa","Masukkan tarikh dan masa","Simpan versi","Sejarah suntingan","Huraian bernombor","Eksport PDF","Edit Tajuk Topik","Padam"};new AlertDialog.Builder(this).setItems(options,(d,i)->{if(i<3)insertCurrentDateTime(i);else if(i==3)saveHistoryVersion();else if(i==4)showHistoryVersions();else if(i==5)editNumberedCommentaries();else if(i==6)startPdfExport();else if(i==7){if(titleEditor==null)return;final EditText field=new EditText(this);field.setSingleLine(true);field.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);field.setText(titleEditor.getText());field.setSelection(field.length());new AlertDialog.Builder(this).setTitle("Edit Tajuk Topik").setView(field).setNegativeButton("Batal",null).setPositiveButton("Simpan",(dialog,which)->{if(titleEditor!=null){titleEditor.setText(field.getText().toString());save();}}).show();}else if(selected!=null)confirmDelete(selected);}).show();});categoryPicker=new Spinner(this);categoryPicker.setContentDescription(maktabah?"Kategori kitab":"Kategori nota");ArrayList<String> choices=new ArrayList<>();choices.add("Tanpa kategori");choices.addAll(categories());categoryPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));String savedCat=key==null?(sectionOpen&&homeWindowWidthDp()>=440&&choices.contains(categoryFilter)?categoryFilter:"Tanpa kategori"):prefs.getString("cat_"+key,"Tanpa kategori");
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
  });final Runnable[] formatRefresh={null};final android.widget.OverScroller editorMomentum=new android.widget.OverScroller(this);
  // A little more friction keeps editor flings smooth without excessive travel.
  editorMomentum.setFriction(android.view.ViewConfiguration.getScrollFriction()*1.1f);
  editor=new EditText(this){
   @Override public void computeScroll(){super.computeScroll();if(editorMomentum.computeScrollOffset()){scrollTo(getScrollX(),editorMomentum.getCurrY());postInvalidateOnAnimation();}}
   @Override public boolean onTextContextMenuItem(int id){
    if(id==android.R.id.copy&&getSelectionStart()>=0&&getSelectionEnd()>=0&&getSelectionStart()!=getSelectionEnd()){
     int a=Math.min(getSelectionStart(),getSelectionEnd()),b=Math.max(getSelectionStart(),getSelectionEnd());
     String selected=getText().subSequence(a,b).toString();
     String clean=selected.replaceAll("(?m)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---[ \\t]*(?:\\r?\\n)?","");
     android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
     if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DinNote",clean));return true;}
    }
    if(id==android.R.id.paste||id==android.R.id.pasteAsPlainText){
     android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
     if(clipboard!=null&&clipboard.hasPrimaryClip()){
      android.content.ClipData clip=clipboard.getPrimaryClip();
      if(clip!=null&&clip.getItemCount()>0){
       CharSequence source=clip.getItemAt(0).coerceToText(MainActivity.this);
       if(source!=null){
        String clean=id==android.R.id.pasteAsPlainText?source.toString():smartPasteText(source.toString());
        int start=Math.max(0,Math.min(getSelectionStart(),getSelectionEnd()));
        int end=Math.max(start,Math.max(getSelectionStart(),getSelectionEnd()));
        start=Math.min(start,length());end=Math.min(end,length());
        getText().replace(start,end,clean);
        setSelection(Math.max(0,Math.min(length(),start+clean.length())));
        return true;
       }
      }
     }
    }
    return super.onTextContextMenuItem(id);
   }

   // Keep the native Samsung insertion handle, but draw a visible caret above text.
   // Foreground drawing avoids the text/background clipping that hid prior attempts.
   @Override public void onDrawForeground(android.graphics.Canvas canvas){
    super.onDrawForeground(canvas);
    if(!hasFocus()||getSelectionStart()<0||getSelectionStart()!=getSelectionEnd())return;
    android.text.Layout layout=getLayout();if(layout==null)return;
    int offset=Math.min(getSelectionStart(),length());
    int line=layout.getLineForOffset(offset);
    // onDrawForeground's canvas is already in the view's scrolled content coordinates.
    // Subtracting scroll offsets here displaced the caret from Samsung's insertion handle.
    float x=getTotalPaddingLeft()+layout.getPrimaryHorizontal(offset);
    float top=getTotalPaddingTop()+layout.getLineTop(line);
    float bottom=getTotalPaddingTop()+layout.getLineBottom(line);
    int sx=getScrollX(),sy=getScrollY();
    if(x<sx||x>sx+getWidth()||bottom<sy||top>sy+getHeight())return;
    top=Math.max(sy,top);bottom=Math.min(sy+getHeight(),bottom);
    if(bottom-top<dp(3))return;
    android.graphics.Paint caret=new android.graphics.Paint(3);
    caret.setColor(0xff087b4c);caret.setStrokeWidth(dp(3));
    canvas.drawLine(x,top+dp(1),x,bottom-dp(1),caret);
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
  // Preserve the system caret drawable; custom GradientDrawable cursors were invisible on device.
  editor.setPadding(dp(10),dp(10),dp(10),dp(12));editor.setLineSpacing(prefs.getBoolean("reader_wide_spacing",false)?dp(5):dp(1),prefs.getBoolean("reader_wide_spacing",false)?1.15f:1.0f);editor.setIncludeFontPadding(true);applySharedTextMetrics(editor);editor.setHint(maktabah?"Tulis kandungan kitab di sini…":"Tulis nota di sini…");editor.setContentDescription(maktabah?"Kandungan kitab":"Kandungan nota");editor.setBackground(rounded(0xfffffefb,0xffb8d5c2,12));editor.setOnFocusChangeListener((v,focused)->v.setBackground(rounded(0xfffffefb,focused?0xff168254:0xffb8d5c2,12)));editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);LinearLayout formatBar=new LinearLayout(this);
  formatBar.setTag("editor_format_toolbar");
  formatBar.setGravity(Gravity.CENTER_VERTICAL);
  formatBar.setPadding(dp(5),dp(2),dp(5),dp(2));
  formatBar.setHorizontalScrollBarEnabled(false);
  formatBar.setBackground(rounded(0xffffffff,0xffd0d7de,8));
  HorizontalScrollView formatScroll=new HorizontalScrollView(this);
  formatScroll.setHorizontalScrollBarEnabled(false);
  formatScroll.setFillViewport(false);
  root.addView(formatScroll);
  formatScroll.addView(formatBar);
  final LinearLayout editorRoot=root;

  if(key!=null&&isLectureTemplate(prefs.getString(key,""))){
   Button lectureIndex=action("☷",formatBar,()->showLectureEditorIndex());
   lectureIndex.setContentDescription("Indeks subtopik Nota Kuliah");
  }
  Button boldButton=action("B",formatBar,()->wrapEditorSelection("**","**"));
  boldButton.setTypeface(null,android.graphics.Typeface.BOLD);boldButton.setContentDescription("Tebal: tandakan teks terpilih");
  Button italicButton=action("I",formatBar,()->wrapEditorSelection("*","*"));
  italicButton.setTypeface(null,android.graphics.Typeface.ITALIC);italicButton.setContentDescription("Condong: tandakan teks terpilih");
  // Keep underline next to bold and italic, visible before horizontally scrolled list tools.
  Button underlineButton=action("U̲",formatBar,()->applyLiveStyle(4));
  Button bulletButton=action("☷",formatBar,()->new AlertDialog.Builder(this).setTitle("List").setItems(new String[]{"●  Bullet Pearl Gold","1.  Nombor biasa","1)  Nombor bulat","❖  Lambang dropdown"},(d,i)->{if(i==0)insertEditorText("• ");else if(i==1)insertEditorText("1. ");else if(i==2)insertNumberedList();else insertEditorText("@# ");}).show());
  bulletButton.setContentDescription("Masukkan senarai");
  Button numberedButton=action("1)",formatBar,()->insertNumberedList());
  numberedButton.setTextColor(0xff17814a);numberedButton.setContentDescription("Senarai bernombor automatik");
  Button checklistButton=action("☑",formatBar,()->insertEditorText("☐ "));
  checklistButton.setContentDescription("Masukkan kotak semak");
    underlineButton.setContentDescription("Garis bawah teks terpilih");
  // Compact reading-section picker shares the formatting row; no extra toolbar row.
  final String[] tagLabels={"Matan · المتن","Syarahan · الشرح","Dalil · الدليل","Terjemahan · الترجمة","Khulasah · الخُلَاصَةُ","Pengajaran & Iktibar · الدُّرُوسُ وَالْعِبَرُ","Mazhab · المَذَاهِبُ"};
  final String[] tagCodes={"MATAN","SYARAHAN","DALIL","TERJEMAHAN","KHULASAH","IKTIBAR"};
  Button readingTagPicker=action("§",formatBar,()->new AlertDialog.Builder(this)
   .setTitle("Bahagian Teks")
   .setItems(tagLabels,(dialog,index)->{if(index<tagCodes.length)insertReadingTag(tagCodes[index]);else new AlertDialog.Builder(this).setTitle("Mazhab · المَذَاهِبُ").setItems(new String[]{"Syafi‘i · الشَّافِعِيُّ","Hanafi · الْحَنَفِيُّ","Maliki · الْمَالِكِيُّ","Hanbali · الْحَنْبَلِيُّ","Fiqh Muqaran · الْفِقْهُ الْمُقَارَنُ"},(d,j)->insertReadingTag(new String[]{"SYAFII","HANAFI","MALIKI","HANBALI","MUQARAN"}[j])).show();})
   .show());
  readingTagPicker.setTextColor(0xff176b47);
  readingTagPicker.setContentDescription("Bahagian Teks: Matan, Syarahan, Dalil, Terjemahan, Khulasah, Pengajaran & Iktibar");
  Button pageMarkerButton=action("▤",formatBar,()->insertOriginalPageMarker());pageMarkerButton.setContentDescription("Muka Surat Asal Kitab");
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
  Button referenceButton=action("▥",formatBar,()->chooseStudyReference());referenceButton.setContentDescription("Dual View Maktabah");
  editorRoot.addOnLayoutChangeListener((v,l,t,rr,b,ol,ot,orr,ob)->{boolean wide=rr-l>=dp(600);if(wide&&formatScroll.getParent()==editorRoot){formatBar.removeView(toolsButton);bar.addView(toolsButton,bar.indexOfChild(historyButtons[0]));compactFormatButtons(formatBar,true);editorRoot.removeView(formatScroll);spacer.setVisibility(View.GONE);bar.addView(formatScroll,bar.indexOfChild(spacer),new LinearLayout.LayoutParams(0,-2,1));formatScroll.post(()->formatScroll.scrollTo(0,0));}else if(!wide&&formatScroll.getParent()==bar){bar.removeView(toolsButton);formatBar.addView(toolsButton,formatBar.indexOfChild(referenceButton));compactFormatButtons(formatBar,false);bar.removeView(formatScroll);spacer.setVisibility(View.VISIBLE);editorRoot.addView(formatScroll,editorRoot.indexOfChild(metadataTitle)+1,new LinearLayout.LayoutParams(-1,-2));}});

  LinearLayout.LayoutParams editBodyFrame=new LinearLayout.LayoutParams(-1,0,1);editBodyFrame.setMargins(dp(4),0,dp(4),0);root.addView(editor,editBodyFrame);


  // Keep normal Android double-tap text selection while editing.
  // Never navigate away while EditText is handling touch/selection events.
  // Let EditText handle selection and cursor gestures; observe swipes for inertial scrolling.
  final android.view.GestureDetector editorSwipe=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){editorMomentum.forceFinished(true);return false;}
   @Override public boolean onFling(android.view.MotionEvent start,android.view.MotionEvent end,float vx,float vy){
    if(start==null||end==null||Math.abs(end.getY()-start.getY())<dp(28)||Math.abs(vy)<dp(400)||Math.abs(vy)<Math.abs(vx))return false;
    android.text.Layout layout=editor.getLayout();if(layout==null)return false;
    int viewport=editor.getHeight()-editor.getTotalPaddingTop()-editor.getTotalPaddingBottom();
    int maxY=Math.max(0,layout.getHeight()-Math.max(1,viewport));if(maxY==0)return false;
    editorMomentum.fling(0,editor.getScrollY(),0,-Math.round(vy*0.35f),0,0,0,maxY);
    editor.postInvalidateOnAnimation();return false;
   }
  });
  editor.setOnTouchListener((v,event)->{editorSwipe.onTouchEvent(event);return false;});
  restoringEditorCursor=true;
  if(key!=null){int remembered=savedCursor;editor.setText(formattedReadingText(repairBoxStructure(normalizeStructuralMarkers(isLectureTemplate(prefs.getString(key,""))?markLectureHeadings(prefs.getString(key,"")):prefs.getString(key,"")))));editor.setSelection(Math.max(0,Math.min(remembered,editor.length())));}
  if(key!=null&&isLectureTemplate(prefs.getString(key,""))){attachLectureEditorPanel(editor);preserveLectureHeading(editor);}
  final EditText activeBody=editor;
  final int resumeCursor=Math.max(0,Math.min(savedCursor,activeBody.length()));
  activeBody.post(()->{if(editor!=activeBody||activeBody.getParent()==null)return;activeBody.requestFocus();activeBody.setSelection(Math.min(resumeCursor,activeBody.length()));restoringEditorCursor=false;persistEditorPosition();revealEditorCursor(activeBody);});
  // Match the reader's optional Arabic serif typeface in the editor as well.
  // Without this, Arabic glyph widths and line wrapping differ after Selesai.
  if(prefs.getBoolean("reader_arabic_clear_font",false)&&
     java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(editor.getText()).find())
   editor.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));
  TextWatcher titleWatcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){save();}public void afterTextChanged(Editable e){}};titleEditor.addTextChangedListener(titleWatcher);TextWatcher bodyWatcher=new TextWatcher(){String before="";EditorSnapshot snapshot;public void beforeTextChanged(CharSequence s,int st,int count,int after){if(!restoring[0]){before=s.toString();snapshot=new EditorSnapshot(editor);}}public void onTextChanged(CharSequence s,int st,int beforeCount,int count){}public void afterTextChanged(Editable e){
   // Samsung IME may still be composing Arabic or Malay text; never insert
   // numbering or repair structural markers until the composing span ends.
   boolean imeComposing=android.view.inputmethod.BaseInputConnection.getComposingSpanStart(e)>=0;
   if(!restoring[0]&&!imeComposing&&!before.equals(e.toString())){
   int cursor=editor.getSelectionStart();
   // Do not automatically continue numbered headings after Enter.
   // Samsung keyboard whitespace/composition could create a duplicate, hard-to-delete
   // next number. Users can explicitly type the next heading when needed.
   if(snapshot!=null)undoHistory.addLast(snapshot);if(undoHistory.size()>100)undoHistory.removeFirst();redoHistory.clear();historyButtons[0].setAlpha(1f);historyButtons[1].setAlpha(0.35f);}
   // Recheck Arabic when text is typed or pasted into an initially Rumi-only note.
   // Typeface updates do not modify Editable content or its autosave state.
   if(editor!=null&&prefs.getBoolean("reader_arabic_clear_font",false)){
    boolean arabic=java.util.regex.Pattern.compile("[\\u0600-\\u06FF]").matcher(e).find();
    android.graphics.Typeface desired=android.graphics.Typeface.create(arabic?"serif":"sans-serif",android.graphics.Typeface.NORMAL);
    if(!desired.equals(editor.getTypeface()))editor.setTypeface(desired);
   }
   if(!restoring[0]&&!imeComposing&&repairTypedBoxMarkers(e,restoring))return;
   // Do not rebuild ReplacementSpans while the keyboard is composing a character:
   // doing so can leave the caret visually over the newly inserted glyph.
   final EditText active=editor;
   int composingStart=android.view.inputmethod.BaseInputConnection.getComposingSpanStart(e);
   int composingEnd=android.view.inputmethod.BaseInputConnection.getComposingSpanEnd(e);
   boolean composing=composingStart>=0&&composingEnd>=composingStart;
   if(!composing)refreshEditorBoxes(e);
   save();
   if(active!=null)active.post(()->{
    if(active!=editor)return;
    // Spans were already refreshed above; avoid scanning the full note twice per edit.
     revealEditorCursor(active);
   });}};editor.addTextChangedListener(bodyWatcher);refreshEditorBoxes(editor.getText());editor.addOnLayoutChangeListener((v,l,t,rr,bb,ol,ot,orr,ob)->{if(rr-l!=orr-ol&&v==editor)refreshEditorBoxes(editor.getText());});categoryPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){save();}});}

 
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
 class PagedLectureHeadingSpan extends android.text.style.ReplacementSpan {
  final String title;
  PagedLectureHeadingSpan(String title){this.title=title;}
  android.graphics.Paint headingPaint(android.graphics.Paint p){android.graphics.Paint q=new android.graphics.Paint(p);q.setAntiAlias(true);q.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);q.setTextSize(dp(13));return q;}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){
   android.graphics.Paint q=headingPaint(p);
   if(fm!=null){fm.ascent=(int)Math.floor(q.ascent())-dp(4);fm.descent=(int)Math.ceil(q.descent())+dp(4);fm.top=fm.ascent;fm.bottom=fm.descent;}
   return (int)Math.ceil(q.measureText(title))+dp(18);
  }
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){
   android.graphics.Paint q=headingPaint(p);float right=x+getSize(p,t,a,b,null)-dp(1);
   float upper=y+q.ascent()-dp(2),lower=y+q.descent()+dp(2);
   q.setColor(0xffedf4eb);c.drawRoundRect(x+dp(1),upper,right,lower,dp(10),dp(10),q);
   q.setColor(0xffc5ae76);q.setStyle(android.graphics.Paint.Style.STROKE);q.setStrokeWidth(dp(1)*0.7f);c.drawRoundRect(x+dp(1),upper,right,lower,dp(10),dp(10),q);
   q.setStyle(android.graphics.Paint.Style.FILL);q.setColor(0xff216e4e);q.setTextAlign(android.graphics.Paint.Align.CENTER);c.drawText(title,(x+right)/2,y,q);
  }
 }
 class BoxMarkerSpan extends android.text.style.ReplacementSpan {
  final String title;final int ink,bg;
  BoxMarkerSpan(String t,int i,int b){title=t;bg=b;ink=t.contains(" · Syafi‘i")?0xff246447:t.contains(" · Hanafi")?0xff345d8c:t.contains(" · Maliki")?0xff94701d:t.contains(" · Hanbali")?0xff78558f:t.contains(" · Fiqh Muqaran")?0xff287e80:t.contains(" · Matan")?0xff246447:t.contains(" · Syarahan")?0xff345d8c:t.contains(" · Dalil")?0xff94701d:t.contains(" · Terjemahan")?0xff78558f:t.contains(" · Khulasah")?0xffa15e29:t.contains(" · Pengajaran & Iktibar")?0xff9d4960:i;}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){
   if(fm!=null){int h=dp(title.isEmpty()?5:22);fm.ascent=-h;fm.top=-h;fm.descent=dp(title.startsWith("Firman")||title.startsWith("Sabda")?4:2);fm.bottom=fm.descent;}
   android.graphics.Paint q=new android.graphics.Paint(p);q.setTypeface(android.graphics.Typeface.DEFAULT);q.setTextSize(dp(13));
   return title.isEmpty()?1:Math.round(q.measureText(title))+dp(38);
  }
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){
   if(title.isEmpty())return;
   if(title.startsWith("Firman")||title.startsWith("Sabda")){android.graphics.Paint q=new android.graphics.Paint(p);q.setColor(ink);q.setTextSize(dp(11));q.setTypeface(android.graphics.Typeface.DEFAULT);q.setTextAlign(android.graphics.Paint.Align.LEFT);c.drawText("❖ "+title,x+dp(9),(top+bottom)/2f-(q.ascent()+q.descent())/2,q);return;}
   boolean section=title.contains(" · Matan")||title.contains(" · Syarahan")||title.contains(" · Dalil")||title.contains(" · Terjemahan")||title.contains(" · Khulasah")||title.contains(" · Pengajaran & Iktibar")||title.contains(" · Syafi‘i")||title.contains(" · Hanafi")||title.contains(" · Maliki")||title.contains(" · Hanbali")||title.contains(" · Fiqh Muqaran");
   float left=x+dp(section?1:7),right=x+getSize(p,t,a,b,null)-dp(4),cy=(top+bottom)/2f;
   float capsuleTop=section?cy-dp(11):top+dp(4),capsuleBottom=section?cy+dp(11):bottom-dp(3);
   android.graphics.Paint q=new android.graphics.Paint(p);q.setAntiAlias(true);q.setColor(ink);q.setAlpha(22);c.drawRoundRect(left,capsuleTop,right,capsuleBottom,dp(12),dp(12),q);if(title.contains(" · Matan")||title.contains(" · Syarahan")||title.contains(" · Dalil")||title.contains(" · Terjemahan")||title.contains(" · Khulasah")||title.contains(" · Pengajaran & Iktibar")||title.contains(" · Syafi‘i")||title.contains(" · Hanafi")||title.contains(" · Maliki")||title.contains(" · Hanbali")||title.contains(" · Fiqh Muqaran")){q.setStyle(android.graphics.Paint.Style.STROKE);q.setStrokeWidth(getResources().getDisplayMetrics().density*0.65f);q.setAlpha(100);c.drawRoundRect(left,capsuleTop,right,capsuleBottom,dp(12),dp(12),q);q.setStyle(android.graphics.Paint.Style.FILL);}q.setAlpha(45);c.drawCircle(x+dp(13),cy,dp(section?9:10),q);
   q.setAlpha(255);q.setColor(ink);q.setTypeface(android.graphics.Typeface.DEFAULT);q.setTextSize(dp(13));
   String mark="❖";
   q.setTextAlign(android.graphics.Paint.Align.CENTER);c.drawText(mark,x+dp(13),(top+bottom)/2f-(q.ascent()+q.descent())/2,q);
   if(title.startsWith("Firman")||title.startsWith("Sabda")){boolean book=title.startsWith("Firman");float size=dp(book?24:19);c.save();c.translate(x+dp(13)-size/2f,(top+bottom-size)/2f);c.scale(size/32f,size/32f);drawSacredBoxIcon(c,book,ink);c.restore();}
   q.setTextAlign(android.graphics.Paint.Align.LEFT);c.drawText(title,x+dp(28),(top+bottom)/2f-(q.ascent()+q.descent())/2,q);
  }
 }
 class BoxBodySpan implements android.text.style.LineBackgroundSpan {
  final int bg,ink,first,last;final android.graphics.Paint reusablePaint=new android.graphics.Paint(3);final android.graphics.Path reusableShape=new android.graphics.Path();final android.graphics.Path reusableEdge=new android.graphics.Path();BoxBodySpan(int b,int i,int a,int z){bg=b;ink=i;first=a;last=z;}
  @Override public void drawBackground(android.graphics.Canvas c,android.graphics.Paint p,int left,int right,int top,int baseline,int bottom,CharSequence t,int start,int end,int line){
   boolean head=start<=first,tail=end>=last;
   if(head)top+=dp(2);if(tail)bottom-=dp(2);if(bottom<=top)return;
   if(ink==0xff176b47||ink==0xff96783e||bg==0xfff3f3f2){
    android.graphics.Paint q=reusablePaint;q.reset();q.setAntiAlias(true);q.setColor(bg);c.drawRect(left+dp(1),top,right-dp(1),bottom,q);float phase=(android.os.SystemClock.uptimeMillis()%8500L)/8500f;float period=dp(360);float center=phase*period;int dark=bg==0xfff3f3f2?0xff737976:ink==0xff176b47?0xff438b65:0xffb49a5d,light=bg==0xfff3f3f2?0xfff5f6f5:ink==0xff176b47?0xffa5f5bb:0xffffe49a;q.setShader(new android.graphics.LinearGradient(0,center-period/2,0,center+period/2,new int[]{dark,light,dark},null,android.graphics.Shader.TileMode.REPEAT));c.drawRect(left+dp(1),top,left+dp(4),bottom,q);q.setShader(null);if(root!=null&&root.isShown())root.postInvalidateDelayed(80);return;
   }
   float l=left+dp(1),r=right-dp(1),rad=dp(8);
   float[] corners={head?rad:0,head?rad:0,head?rad:0,head?rad:0,tail?rad:0,tail?rad:0,tail?rad:0,tail?rad:0};
   android.graphics.Paint q=reusablePaint;q.reset();q.setAntiAlias(true);q.setStyle(android.graphics.Paint.Style.FILL);q.setColor(bg);
   android.graphics.Path shape=reusableShape;shape.rewind();shape.addRoundRect(new android.graphics.RectF(l,top,r,bottom),corners,android.graphics.Path.Direction.CW);c.drawPath(shape,q);
   q.setColor(ink);q.setAlpha(75);q.setStyle(android.graphics.Paint.Style.STROKE);q.setStrokeWidth(dp(1));
   android.graphics.Path edge=reusableEdge;edge.rewind();
   edge.moveTo(l,tail?bottom-rad:bottom);edge.lineTo(l,head?top+rad:top);
   if(head){edge.quadTo(l,top,l+rad,top);edge.lineTo(r-rad,top);edge.quadTo(r,top,r,top+rad);}
   else edge.moveTo(r,top);
   edge.lineTo(r,tail?bottom-rad:bottom);
   if(tail){edge.quadTo(r,bottom,r-rad,bottom);edge.lineTo(l+rad,bottom);edge.quadTo(l,bottom,l,bottom-rad);}
   if(bg==0xfff3f3f2){q.setStyle(android.graphics.Paint.Style.FILL);q.setColor(0xffb5bab7);c.drawRect(l,top,l+dp(2),bottom,q);return;}
   c.drawPath(edge,q);
   if(head){q.setAlpha(65);for(int j=0;j<3;j++){float d=dp(6+j*5);android.graphics.Path motif=new android.graphics.Path();motif.moveTo(r-d-dp(5),top+dp(2));motif.lineTo(r-d,top+dp(5));motif.lineTo(r-d,top+dp(8));motif.lineTo(r-dp(5),top+d);motif.lineTo(r-dp(2),top+d+dp(5));c.drawPath(motif,q);}}
  }
 }
 class BoxInsetSpan extends android.text.style.LeadingMarginSpan.Standard {
  BoxInsetSpan(){super(dp(9));}
 }
 View islamicOrnamentView(final String kind){
  View v=new View(this){
   @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float w=getWidth()*0.90f;if(w>0)drawIslamicOrnament(c,(getWidth()-w)/2f,getHeight()/2f,w,kind);}
  };v.setContentDescription("Pemisah "+kind);return v;
 }
 void drawIslamicOrnament(android.graphics.Canvas c,float x,float y,float w,String kind){
  if(w<=0)return;c.save();c.translate(x+w/2f,y);
  float scale=Math.min(w/270f,(float)dp(1));c.scale(scale,scale);
  android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(0xffb3a16a);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(0.8f);
  c.drawLine(-135,0,-15,0,p);c.drawLine(15,0,135,0,p);
  for(int d:new int[]{8,4}){android.graphics.Path diamond=new android.graphics.Path();diamond.moveTo(0,-d);diamond.lineTo(d,0);diamond.lineTo(0,d);diamond.lineTo(-d,0);diamond.close();c.drawPath(diamond,p);}
  c.restore();
 }
 class ReaderOrnamentSpan extends android.text.style.ReplacementSpan {
  final String kind;ReaderOrnamentSpan(String k){kind=k;}
  int width(android.graphics.Paint p){return Math.max(dp(60),Math.round(p.measureText("────────────────────────")));}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){if(fm!=null){fm.ascent=-dp(10);fm.top=fm.ascent;fm.descent=dp(10);fm.bottom=fm.descent;}return width(p);}
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){drawIslamicOrnament(c,x,y,width(p),kind);}
 }
 class EditorDividerSpan extends android.text.style.ReplacementSpan {
  final String symbol;EditorDividerSpan(String kind){symbol=kind.equals("FLOWER")?"✿":kind.equals("GEOMETRIC")?"❖":"❧";}
  int width(){return editor==null?dp(120):Math.max(dp(24),editor.getWidth()-editor.getCompoundPaddingLeft()-editor.getCompoundPaddingRight()-dp(22));}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){
   if(fm!=null){fm.ascent=-dp(17);fm.top=fm.ascent;fm.descent=dp(17);fm.bottom=fm.descent;}return width();
  }
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){
   drawIslamicOrnament(c,x,(top+bottom)/2f,width(),symbol.equals("✿")?"FLOWER":symbol.equals("❖")?"GEOMETRIC":"CLASSIC");
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
 // Length-preserving display only: editing and persistence keep the original characters.
 static class LectureHeadingDisplay implements android.text.Spanned, android.text.GetChars {
  final android.text.Spanned source;
  LectureHeadingDisplay(android.text.Spanned source){this.source=source;}
  public int length(){return source.length();}
  public char charAt(int i){
   char c=source.charAt(i);
   if(c=='\n'||c=='\r')return c;
   int start=i;while(start>0&&source.charAt(start-1)!='\n'&&source.charAt(start-1)!='\r')start--;
   if(source.charAt(start)!='\u2063')return c;
   return i==start?'#':Character.toUpperCase(c);
  }
  public void getChars(int start,int end,char[] dest,int off){for(int i=start;i<end;i++)dest[off+i-start]=charAt(i);}
  public CharSequence subSequence(int start,int end){
   char[] chars=new char[end-start];getChars(start,end,chars,0);
   android.text.SpannableString result=new android.text.SpannableString(new String(chars));
   android.text.TextUtils.copySpansFrom(source,start,end,Object.class,result,0);return result;
  }
  public String toString(){char[] chars=new char[length()];getChars(0,length(),chars,0);return new String(chars);}
  public <T>T[] getSpans(int a,int b,Class<T> type){return source.getSpans(a,b,type);}
  public int getSpanStart(Object o){return source.getSpanStart(o);}
  public int getSpanEnd(Object o){return source.getSpanEnd(o);}
  public int getSpanFlags(Object o){return source.getSpanFlags(o);}
  public int nextSpanTransition(int a,int b,Class type){return source.nextSpanTransition(a,b,type);}
 }
 static class LectureHeadingTransformation implements android.text.method.TransformationMethod {
  public CharSequence getTransformation(CharSequence source,View view){return source instanceof android.text.Spanned?new LectureHeadingDisplay((android.text.Spanned)source):source;}
  public void onFocusChanged(View view,CharSequence source,boolean focused,int direction,android.graphics.Rect previous){}
 }
 static class LectureHeadingVisualSpan extends android.text.style.MetricAffectingSpan {
  private void apply(android.text.TextPaint p){android.graphics.Typeface old=p.getTypeface();int style=old==null?0:old.getStyle();p.setTypeface(android.graphics.Typeface.create(old,style|android.graphics.Typeface.BOLD));}
  @Override public void updateDrawState(android.text.TextPaint p){apply(p);}
  @Override public void updateMeasureState(android.text.TextPaint p){apply(p);}
 }
 void refreshEditorBoxes(android.text.Editable e){
  for(LectureHeadingVisualSpan span:e.getSpans(0,e.length(),LectureHeadingVisualSpan.class))e.removeSpan(span);
  java.util.regex.Matcher heading=lectureHeadingPattern().matcher(e.toString());
  while(heading.find())if(heading.start()<heading.end()&&e.charAt(heading.start())=='\u2063')e.setSpan(new LectureHeadingVisualSpan(),heading.start(),heading.end(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  for(OriginalPageSpan span:e.getSpans(0,e.length(),OriginalPageSpan.class))e.removeSpan(span);
  java.util.regex.Matcher pageMarks=java.util.regex.Pattern.compile("(?m)^---MSASAL:([0-9]{1,7})---[ \\t]*$").matcher(e.toString());
  while(pageMarks.find())e.setSpan(new OriginalPageSpan("❖ Muka Surat "+pageMarks.group(1)+" ❖"),pageMarks.start(),pageMarks.end(),33);
  applySectionIndent(e);
  for(EditorDividerSpan span:e.getSpans(0,e.length(),EditorDividerSpan.class))e.removeSpan(span);
  java.util.regex.Matcher div=java.util.regex.Pattern.compile("(?m)^---(FLOWER|GEOMETRIC|CLASSIC)---$").matcher(e.toString());
  while(div.find())e.setSpan(new EditorDividerSpan(div.group(1)),div.start(),div.end(),33);

  for(BoxMarkerSpan span:e.getSpans(0,e.length(),BoxMarkerSpan.class))e.removeSpan(span);
  java.util.regex.Matcher readingMarks=java.util.regex.Pattern.compile("(?m)^---(MATAN|SYARAHAN|DALIL|TERJEMAHAN|KHULASAH|IKTIBAR|SYAFII|HANAFI|MALIKI|HANBALI|MUQARAN|TAMAT)---$").matcher(e.toString());
  while(readingMarks.find()){
   String k=readingMarks.group(1);
   String label=k.equals("MATAN")?"المتن · Matan":k.equals("SYARAHAN")?"الشرح · Syarahan":k.equals("DALIL")?"الدليل · Dalil":k.equals("TERJEMAHAN")?"الترجمة · Terjemahan":k.equals("SYAFII")?"الشَّافِعِيُّ · Syafi‘i":k.equals("HANAFI")?"الْحَنَفِيُّ · Hanafi":k.equals("MALIKI")?"الْمَالِكِيُّ · Maliki":k.equals("HANBALI")?"الْحَنْبَلِيُّ · Hanbali":k.equals("MUQARAN")?"الْفِقْهُ الْمُقَارَنُ · Fiqh Muqaran":k.equals("KHULASAH")?"الخُلَاصَةُ · Khulasah":k.equals("IKTIBAR")?"الدُّرُوسُ وَالْعِبَرُ · Pengajaran & Iktibar":"";
   e.setSpan(new BoxMarkerSpan(label,0xff176b47,0xffedf5ee),readingMarks.start(),readingMarks.end(),33);
  }
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
   bg=kind.equals("FIRMAN")?0xfffaf8f0:gold?0xfff0f8f2:0xfff3f3f2;
   ink=kind.equals("FIRMAN")?0xff96783e:gold?0xff176b47:0xff666b68;
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
 TextView centeredDiamondMarker(int size){
  TextView marker=text("❖",size);
  marker.setGravity(Gravity.CENTER);
  marker.setIncludeFontPadding(false);
  marker.setTextColor(0xff176b47);
  return marker;
 }
 void insertNumberedList(){
  if(editor==null)return;
  int cursor=Math.max(0,Math.min(editor.getSelectionStart(),editor.getSelectionEnd()));
  int selectionEnd=Math.max(cursor,Math.max(editor.getSelectionStart(),editor.getSelectionEnd()));
  String content=editor.getText().toString();
  int lineStart=content.lastIndexOf('\n',cursor-1)+1;
  int lineEnd=content.indexOf('\n',lineStart);if(lineEnd<0)lineEnd=content.length();
  if(content.substring(lineStart,lineEnd).matches("\\s*\\d+\\)\\s*.*"))return;
  int number=1;
  if(lineStart>0){
   int previousStart=content.lastIndexOf('\n',lineStart-2)+1;
   java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("^\\s*(\\d+)\\)\\s+.*$").matcher(content.substring(previousStart,lineStart-1));
   if(matcher.matches()){try{number=Integer.parseInt(matcher.group(1))+1;}catch(NumberFormatException ignored){}}
  }
  String prefix=number+") ";
  editor.getText().insert(lineStart,prefix);
  editor.setSelection(cursor+prefix.length(),selectionEnd+prefix.length());
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
 void renderBoxReading(LinearLayout body,String source,int fontSize,String key){
  source=repairBoxStructure(normalizeStructuralMarkers(source));
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?ms)^---KOTAK(DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---[ \\t]*\\n(.*?)^---KOTAKTAMAT---[ \\t]*(?:\\n|$)").matcher(source);
  int previous=0;
  while(m.find()){
   addBoxParagraph(body,source.substring(previous,m.start()),fontSize,key,source.substring(0,previous).split("\\n",-1).length-1);
   String type=m.group(1);
   final boolean sacred=type.equals("FIRMAN")||type.equals("SABDA");
   LinearLayout panel=new LinearLayout(this){
    @Override protected void dispatchDraw(android.graphics.Canvas c){
     super.dispatchDraw(c);
     if(sacred||!sacred){
      float h=getHeight();if(h<=0)return;
      boolean green=type.equals("SABDA");
      int dark=!sacred?0xff737976:green?0xff438b65:0xffb49a5d,light=!sacred?0xfff5f6f5:green?0xffa5f5bb:0xffffe49a;
      float phase=(android.os.SystemClock.uptimeMillis()%8500L)/8500f;
      float center=(phase*1.8f-0.4f)*h;
      android.graphics.Paint glow=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
      glow.setShader(new android.graphics.LinearGradient(0,center-h*0.4f,0,center+h*0.4f,new int[]{dark,light,dark},null,android.graphics.Shader.TileMode.CLAMP));
      c.drawRect(0,0,dp(3),h,glow);
      if(isShown())postInvalidateDelayed(80);
      return;
     }
     if(!sacred)return;
     android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(0xff789184);p.setAlpha(65);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(1));
     float right=getWidth()-dp(4),top=dp(4);
     for(int n=0;n<3;n++){float d=dp(7+n*5);android.graphics.Path path=new android.graphics.Path();path.moveTo(right-d-dp(6),top);path.lineTo(right-d,top+dp(3));path.lineTo(right-d,top+dp(6));path.lineTo(right-dp(3),top+d);path.lineTo(right,top+d+dp(5));c.drawPath(path,p);}
    }
   };panel.setOrientation(LinearLayout.VERTICAL);
   int bg=type.equals("FIRMAN")?0xfffaf8f0:type.equals("SABDA")?0xfff0f8f2:0xfff3f3f2;
   int border=type.equals("FIRMAN")?0xffd6c7a4:type.equals("SABDA")?0xffa5c3aa:0xffc7cac8;
   border=Color.rgb((Color.red(border)+Color.red(bg))/2,(Color.green(border)+Color.green(bg))/2,(Color.blue(border)+Color.blue(bg))/2);
   if(sacred){
    android.graphics.drawable.GradientDrawable base=new android.graphics.drawable.GradientDrawable();base.setColor(bg);base.setCornerRadius(dp(2));
    android.graphics.drawable.GradientDrawable stripe=new android.graphics.drawable.GradientDrawable();stripe.setColor(type.equals("FIRMAN")?0xffbba66e:0xff83a98e);
    android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{stripe,base});layers.setLayerInset(1,dp(3),0,0,0);panel.setBackground(layers);panel.setPadding(dp(9),dp(3),dp(6),dp(3));
   }else{android.graphics.drawable.GradientDrawable base=new android.graphics.drawable.GradientDrawable();base.setColor(bg);android.graphics.drawable.GradientDrawable stripe=new android.graphics.drawable.GradientDrawable();stripe.setColor(0xff737976);android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{stripe,base});layers.setLayerInset(1,dp(3),0,0,0);panel.setBackground(layers);panel.setPadding(dp(9),dp(3),dp(6),dp(3));}
   TextView label=text(type.equals("FIRMAN")?"Firman Allah SWT:":type.equals("SABDA")?"Sabda Nabi SAW:":type.equals("DALIL")?"❖  Dalil":type.equals("HIKMAH")?"❖  Kalam Hikmah":type.equals("SAHABAT")?"❖  Kata Sahabat Rdh.":type.equals("TABII")?"❖  Kata Tabi\u0027in Rhm.":type.equals("ULAMA")?"❖  Kata Ulama\u0027 Rhm.":"❖  Catatan Penting",13);
   label.setTextColor(type.equals("FIRMAN")?0xff96783e:type.equals("SABDA")?0xff176b47:0xff666b68);
   label.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));label.setIncludeFontPadding(false);
   label.setPadding(dp(5),dp(1),dp(7),dp(1));
   label.setBackground(null);
   if(sacred){label.setText("❖ "+label.getText());label.setTextSize(11);label.setBackground(null);label.setPadding(0,0,0,dp(4));panel.addView(label,new LinearLayout.LayoutParams(-2,-2));}
   else {label.setTextSize(11);label.setPadding(0,0,0,dp(4));panel.addView(label,new LinearLayout.LayoutParams(-2,-2));}
   TextView value=text("",fontSize);String boxContent=m.group(2).replaceFirst("^\\n","").replaceFirst("\\n$","");value.setText(readerLabelText(boxContent));applySharedTextMetrics(value);value.setTextColor(0xff303c43);
   value.setPadding(0,0,0,0);value.setIncludeFontPadding(false);value.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);value.setTextIsSelectable(true);value.setGravity(Gravity.START);value.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);if(!boxContent.trim().isEmpty())panel.addView(value);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
   // Add a small breathing gap whenever two box blocks meet, regardless of their types.
   boolean adjacentBox=previous>0&&source.substring(previous,m.start()).trim().isEmpty();
   // Give prose before and after a box a little more breathing room.
   // Adjacent boxes retain a compact separation rather than doubling the gap.
   lp.setMargins(dp(2),dp(3),dp(2),dp(3));body.addView(panel,lp);
   previous=m.end();
  }
  addBoxParagraph(body,source.substring(previous),fontSize,key,source.substring(0,previous).split("\\n",-1).length-1);
 }
 void addBoxParagraph(LinearLayout body,String value,int fontSize,String key,int baseLine){
  java.util.regex.Matcher ornaments=java.util.regex.Pattern.compile("(?m)^[ \\t]*\\*{0,2}---(FLOWER|GEOMETRIC|CLASSIC)---\\*{0,2}[ \\t]*$").matcher(value);
  int start=0;
  while(ornaments.find()){
   addPlainBoxParagraph(body,value.substring(start,ornaments.start()),fontSize,key,baseLine+value.substring(0,start).split("\\n",-1).length-1);
   body.addView(islamicOrnamentView(ornaments.group(1)),new LinearLayout.LayoutParams(-1,dp(20)));
   start=ornaments.end();
  }
  addPlainBoxParagraph(body,value.substring(start),fontSize,key,baseLine+value.substring(0,start).split("\\n",-1).length-1);
 }
 void addPlainBoxParagraph(LinearLayout body,String value,int fontSize,String key,int baseLine){
  // Mixed numbered headings and decorative boxes must retain expandable
  // per-heading commentary, rather than flattening every paragraph to a TextView.
  String[] lines=value.split("\\n",-1);StringBuilder paragraph=new StringBuilder();
  for(int i=0;i<lines.length;i++){
   String line=lines[i];
   java.util.regex.Matcher numbered=java.util.regex.Pattern.compile("^\\s*(\\d+)\\)\\s+(.*)$").matcher(line);
   java.util.regex.Matcher iconHeading=java.util.regex.Pattern.compile("^\\s*@#(?:\\s+(.+))?$").matcher(line);
   boolean isIconHeading=!numbered.matches()&&iconHeading.matches();
   boolean isBullet=line.trim().startsWith("• ");
   if(numbered.matches()||isIconHeading||isBullet){
    addPlainBoxText(body,paragraph.toString(),fontSize);paragraph.setLength(0);
    final String commentaryKey="syarahan_"+key+"_"+(baseLine+i);
    LinearLayout wrapper=new LinearLayout(this);wrapper.setOrientation(1);
    LinearLayout heading=new LinearLayout(this);heading.setOrientation(0);heading.setGravity(Gravity.CENTER_VERTICAL);
    // Match the original pearl-and-gold numbered medallion in the normal reader.
    FrameLayout medallion=new FrameLayout(this);
    android.graphics.drawable.GradientDrawable ring=new GoldGlowDrawable(0);
    ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);ring.setColor(0xfffcf8ed);ring.setStroke(dp(1),0xffc3a35a);
    medallion.setBackground(ring);
    TextView numeral=isIconHeading?centeredDiamondMarker(13):new TextView(this);numeral.setText(isBullet?"●":isIconHeading?"❖":numbered.group(1));numeral.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,isBullet?18:13);numeral.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));numeral.setGravity(Gravity.CENTER);numeral.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);numeral.setTextColor(isBullet?0xffc9b478:0xff176b47);numeral.setIncludeFontPadding(false);numeral.setPadding(0,0,0,0);numeral.setSingleLine(true);numeral.setMinHeight(0);numeral.setMinimumHeight(0);numeral.setMinWidth(0);numeral.setMinimumWidth(0);
    FrameLayout.LayoutParams numeralParams=new FrameLayout.LayoutParams(-1,-1);medallion.addView(numeral,numeralParams);
    LinearLayout.LayoutParams medalParams=new LinearLayout.LayoutParams(dp(23),dp(23));medalParams.setMargins(dp(4),0,dp(1),0);heading.addView(medallion,medalParams);
    TextView title=text("",fontSize);title.setText(formattedReadingText(isBullet?line.trim().substring(2):isIconHeading?(iconHeading.group(1)==null?"Huraian":iconHeading.group(1)):numbered.group(2)));title.setTextColor(0xff303c43);title.setPadding(0,dp(2),dp(3),dp(2));heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    if(isIconHeading)medallion.setContentDescription("Buka atau tutup huraian");
    wrapper.addView(heading);
    LinearLayout details=new LinearLayout(this);details.setOrientation(1);details.setPadding(dp(10),dp(4),dp(10),dp(6));details.setBackground(rounded(0xfff8faf5,0xffd9e4d6,10));details.setVisibility(View.GONE);
    TextView label=text("Syarahan / Huraian",13);label.setTextColor(0xff176b47);details.addView(label);
    EditText note=new EditText(this);note.setTextSize(15);note.setHint("Tulis huraian di sini…");note.setMinLines(2);note.setText(prefs.getString(commentaryKey,""));details.addView(note);
    note.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int a,int c,int f){}public void onTextChanged(CharSequence t,int a,int b,int c){prefs.edit().putString(commentaryKey,t.toString()).apply();}public void afterTextChanged(Editable e){}});
    if(isIconHeading||(!isBullet&&note.length()>0)){wrapper.addView(details);if(!isIconHeading)details.setVisibility(View.VISIBLE);}
    View.OnClickListener toggle=v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);};
    if(isIconHeading){heading.setOnClickListener(toggle);medallion.setOnClickListener(toggle);}
    body.addView(wrapper);View divider=new View(this);divider.setBackgroundColor(0xffc5e7cb);body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
   }else{paragraph.append(line);if(i<lines.length-1)paragraph.append('\n');}
  }
  addPlainBoxText(body,paragraph.toString(),fontSize);
 }
 void addPlainBoxText(LinearLayout body,String value,int fontSize){
  value=value.replaceAll("(?m)^[ \\t]*\\*{1,2}[ \\t]*$","").trim();
  if(value.isEmpty())return;
  TextView t=text("",fontSize);
  android.text.SpannableStringBuilder compact=readerLabelText(value);
  // Preserve user-entered blank lines in both reading column modes.
  for(SectionGapSpan gap:compact.getSpans(0,compact.length(),SectionGapSpan.class))compact.removeSpan(gap);
  t.setText(compact);applySharedTextMetrics(t);
  // Give mixed Arabic/Malay paragraphs comfortable baseline clearance.
  if(value.codePoints().anyMatch(cp->cp>=0x0600&&cp<=0x06ff))t.setLineSpacing(dp(3),1.08f);t.setTextColor(0xff24292f);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);t.setTextIsSelectable(true);t.setPadding(dp(2),dp(2),dp(2),dp(2));
  LinearLayout.LayoutParams paragraphParams=new LinearLayout.LayoutParams(-1,-2);
  // Keep Arabic paragraphs spacious; tighten Malay-only prose slightly.
  boolean containsArabic=value.codePoints().anyMatch(cp->cp>=0x0600&&cp<=0x06ff);
  paragraphParams.setMargins(0,dp(containsArabic?3:2),0,dp(containsArabic?3:2));body.addView(t,paragraphParams);
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
  // Always open the insertion picker on a normal tap, even when the caret is inside an existing box.
  // Managing an existing box remains available via long press.
  final EditText target=editor;
  final int startSelection=target.getSelectionStart(),endSelection=target.getSelectionEnd();
  final int a=startSelection<0?target.length():Math.max(0,Math.min(startSelection,endSelection));
  final int b=endSelection<0?a:Math.max(a,Math.max(startSelection,endSelection));
  final String[] types={"DALIL","HIKMAH","CATATAN","FIRMAN","SABDA","SAHABAT","TABII","ULAMA"};
  new AlertDialog.Builder(this).setTitle("Kotak teks")
   .setItems(new String[]{"Dalil","Kalam Hikmah","Catatan Penting","Firman Allah SWT:","Sabda Nabi SAW:","Kata Sahabat Rdh.","Kata Tabi\u0027in Rhm.","Kata Ulama\u0027 Rhm.","Buang kotak terpilih","Urus kotak sedia ada"},(d,i)->{
    if(i==9){manageCurrentBox(true);return;}
    if(editor!=target)return;
    // Clamp the saved selection after dialog focus changes or repeated insertions.
    int safeA=Math.min(a,editor.length()),safeB=Math.min(b,editor.length());
    String chosen=editor.getText().subSequence(safeA,safeB).toString();
    if(i==8){editor.getText().replace(safeA,safeB,chosen.replaceAll("(?m)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA|TAMAT)---\\n?",""));return;}
    String prefix="\n---KOTAK"+types[i]+"---\n";
    int insertA=safeA,insertB=safeB;
    String existing=editor.getText().toString();
    java.util.regex.Matcher boxes=java.util.regex.Pattern.compile("(?ms)^---KOTAK(?:DALIL|HIKMAH|CATATAN|FIRMAN|SABDA|SAHABAT|TABII|ULAMA)---\\n.*?^---KOTAKTAMAT---").matcher(existing);
    while(boxes.find()){if(safeA>=boxes.start()&&safeA<boxes.end()){insertA=insertB=boxes.end();chosen="";break;}}
    if(chosen.contains("---KOTAK")){insertA=insertB=safeB;chosen="";}
    editor.getText().replace(insertA,insertB,prefix+chosen+"\n---KOTAKTAMAT---\n");
    editor.setSelection(insertA+prefix.length(),insertA+prefix.length()+chosen.length());
   }).show();
 }
 void showDividerPicker(){
  final String[] labels={"Bunga","Simpulan","Hiasan"};
  final String[] kinds={"FLOWER","GEOMETRIC","CLASSIC"};
  LinearLayout choices=new LinearLayout(this);choices.setOrientation(1);choices.setPadding(dp(16),dp(8),dp(16),dp(8));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Pemisah Perenggan (Divider)").setView(choices).setNegativeButton("Batal",null).create();
  for(int i=0;i<kinds.length;i++){
   final String kind=kinds[i];LinearLayout item=new LinearLayout(this);item.setOrientation(1);
   TextView label=text(labels[i],14);label.setGravity(Gravity.CENTER);item.addView(label);
   item.addView(islamicOrnamentView(kind),new LinearLayout.LayoutParams(-1,dp(42)));
   choices.addView(item,new LinearLayout.LayoutParams(-1,-2));
   item.setOnClickListener(v->{dialog.dismiss();insertEditorText("\n---"+kind+"---\n");});
  }
  dialog.show();
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
 void insertOriginalPageMarker(){
  if(editor==null)return;
  final EditText target=editor;final int cursor=Math.max(0,target.getSelectionStart());
  final EditText number=new EditText(this);number.setSingleLine(true);number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);number.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(7)});number.setHint("Contoh: 25");
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Muka Surat Asal Kitab").setView(number).setNegativeButton("Batal",null).setPositiveButton("Masukkan",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
   String n=number.getText().toString().trim();if(!n.matches("[0-9]{1,7}")||Long.parseLong(n)==0){number.setError("Masukkan nombor muka surat");return;}
   if(editor!=target){dialog.dismiss();return;}
   int at=Math.min(cursor,target.length());String marker=(at>0&&target.getText().charAt(at-1)!='\n'?"\n":"")+"---MSASAL:"+n+"---"+(at<target.length()&&target.getText().charAt(at)=='\n'?"":"\n");
   target.getText().insert(at,marker);target.setSelection(at+marker.length());save();dialog.dismiss();
  }));dialog.show();
 }
 class OriginalPageSpan extends android.text.style.ReplacementSpan {
  final String label;OriginalPageSpan(String label){this.label=label;}
  android.graphics.Paint pagePaint(android.graphics.Paint p){android.graphics.Paint q=new android.graphics.Paint(p);q.setAntiAlias(true);q.setTextSize(dp(12));q.setTypeface(android.graphics.Typeface.DEFAULT);return q;}
  @Override public int getSize(android.graphics.Paint p,CharSequence t,int a,int b,android.graphics.Paint.FontMetricsInt fm){if(fm!=null){fm.ascent=-dp(19);fm.top=fm.ascent;fm.descent=dp(7);fm.bottom=fm.descent;}return (int)Math.ceil(pagePaint(p).measureText(label))+dp(20);}
  @Override public void draw(android.graphics.Canvas c,CharSequence t,int a,int b,float x,int top,int y,int bottom,android.graphics.Paint p){android.graphics.Paint q=pagePaint(p);float right=x+getSize(p,t,a,b,null);float upper=y-dp(15),lower=y+dp(5);q.setColor(0xffedf4eb);c.drawRoundRect(x,upper,right,lower,dp(8),dp(8),q);q.setColor(0xffc5ae76);q.setStyle(android.graphics.Paint.Style.STROKE);q.setStrokeWidth(dp(1)*0.7f);c.drawRoundRect(x,upper,right,lower,dp(8),dp(8),q);q.setStyle(android.graphics.Paint.Style.FILL);q.setColor(0xff216e4e);q.setTextAlign(android.graphics.Paint.Align.CENTER);c.drawText(label,(x+right)/2,y,q);}
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
   entry.put("source","manual");
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
    new AlertDialog.Builder(this).setTitle("Sejarah suntingan").setMessage("Belum ada versi terdahulu. DinNote menyimpan sandaran automatik sebelum perubahan, dan anda juga boleh memilih Simpan versi.")
     .setPositiveButton("OK",null).show();return;
   }
   String[] labels=new String[versions.length()];
   for(int i=0;i<versions.length();i++){
    org.json.JSONObject item=versions.getJSONObject(versions.length()-1-i);
    labels[i]=(item.optString("source","manual").equals("auto")?"⏱ Automatik  ·  ":"✎ Disimpan  ·  ")+new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(item.optLong("time",0)))+" — "+item.optString("title","Tanpa tajuk");
   }
   new AlertDialog.Builder(this).setTitle("Sejarah suntingan").setItems(labels,(d,index)->{
    try{
     org.json.JSONObject item=versions.getJSONObject(versions.length()-1-index);
     String oldBody=item.optString("body","");
     String snippet=oldBody.replaceAll("(?m)^---[A-Z][A-Z0-9_ -]*---\\s*$","").trim();
     if(snippet.length()>350)snippet=snippet.substring(0,350)+"…";
     String versionTime=new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(item.optLong("time",0)));
     String versionType="auto".equals(item.optString("source","manual"))?"Automatik":"Disimpan";
     String preview="Jenis: "+versionType+"\nTarikh: "+versionTime+"\nTajuk: "+item.optString("title","Tanpa tajuk")+"\nKategori: "+item.optString("category","Tanpa kategori")+"\n\n"+(snippet.isEmpty()?"(Tiada kandungan)":snippet)+"\n\nMemulihkan versi ini akan menggantikan kandungan editor semasa. Versi semasa akan disandarkan terlebih dahulu.";
     new AlertDialog.Builder(this).setTitle("Pratonton versi terdahulu")
      .setMessage(preview)
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
        int categoryPosition=-1;
        for(int i=0;i<categoryPicker.getCount();i++)
         if(category.equals(categoryPicker.getItemAtPosition(i).toString())){categoryPosition=i;break;}
        if(categoryPosition<0&&categoryPicker.getAdapter() instanceof android.widget.ArrayAdapter){
         @SuppressWarnings("unchecked") android.widget.ArrayAdapter<String> adapter=(android.widget.ArrayAdapter<String>)categoryPicker.getAdapter();
         adapter.add(category);
         categoryPosition=adapter.getPosition(category);
        }
        if(categoryPosition>=0)categoryPicker.setSelection(categoryPosition);
       }
       save();
       Toast.makeText(this,"Versi dipulihkan",Toast.LENGTH_SHORT).show();
      }).setNegativeButton("Batal",null).show();
    }catch(Exception e){Toast.makeText(this,"Gagal membaca versi",Toast.LENGTH_LONG).show();}
   }).setNegativeButton("Tutup",null).show();
  }catch(Exception e){Toast.makeText(this,"Sejarah tidak dapat dibuka",Toast.LENGTH_LONG).show();}
 }
 void persistEditorPosition(){
  if(restoringEditorCursor||editor==null||selected==null||editor.getSelectionEnd()<0)return;
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
 void snapshotBeforeAutosave(String key,String oldBody,String oldTitle,String oldCategory){
  // Preserve the previous content once per editing window, never on every keystroke.
  try{
   if(oldBody.isEmpty()&&oldTitle.isEmpty())return;
   long now=System.currentTimeMillis();
   long last=prefs.getLong("auto_history_at_"+key,0L);
   if(last>0&&now-last<30L*60L*1000L)return;
   org.json.JSONArray versions=new org.json.JSONArray(prefs.getString("history_"+key,"[]"));
   if(versions.length()>0){
    org.json.JSONObject previous=versions.getJSONObject(versions.length()-1);
    if(oldBody.equals(previous.optString("body"))&&oldTitle.equals(previous.optString("title"))&&oldCategory.equals(previous.optString("category"))){
     prefs.edit().putLong("auto_history_at_"+key,now).apply();return;
    }
   }
   org.json.JSONObject entry=new org.json.JSONObject();
   entry.put("time",now);entry.put("title",oldTitle);entry.put("body",oldBody);entry.put("category",oldCategory);entry.put("source","auto");
   org.json.JSONArray kept=new org.json.JSONArray();
   for(int i=Math.max(0,versions.length()-9);i<versions.length();i++)kept.put(versions.getJSONObject(i));
   kept.put(entry);
   if(!prefs.edit().putString("history_"+key,kept.toString()).putLong("auto_history_at_"+key,now).commit())
    android.util.Log.w("DinNote","Gagal menyimpan sandaran automatik sebelum suntingan");
  }catch(Exception e){android.util.Log.e("DinNote","Sandaran sejarah automatik gagal",e);}
 }
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
   if(!isNew&&prefs.contains(selected))snapshotBeforeAutosave(selected,prefs.getString(selected,""),prefs.getString("title_"+selected,""),prefs.getString("cat_"+selected,"Tanpa kategori"));
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
 java.util.Map<String,Integer> boardPostingCounts(boolean books){
  java.util.Map<String,Integer> counts=new java.util.HashMap<>();
  java.util.Map<String,?> snapshot=prefs.getAll();
  for(String key:snapshot.keySet()){
   if(!key.startsWith(books?"m_":"n_")||!(snapshot.get(key) instanceof String)||prefs.getBoolean("deleted_"+key,false))continue;
   String category=prefs.getString("cat_"+key,"Tanpa kategori");
   counts.put(category,counts.getOrDefault(category,0)+1);
  }
  return counts;
 }
 void addBoardPostingCount(LinearLayout row,int count){
  TextView number=text(String.valueOf(count),12);number.setTextColor(0xff687b70);number.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);number.setPadding(dp(4),0,dp(8),0);number.setContentDescription(count+" posting");row.addView(number,new LinearLayout.LayoutParams(-2,dp(38)));
 }
 void rememberBoardScroll(ScrollView scroll,String key){
  final int saved=prefs.getInt(key,0);
  scroll.post(()->{scroll.scrollTo(0,saved);scroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->prefs.edit().putInt(key,y).apply());});
 }
 void restoreBoardBranch(TextView arrow,LinearLayout nested,String key){
  nested.setTag("board_branch_children");
  arrow.setTag("board_branch_arrow");
  nested.setTag(0x7f0f00fe,key);
  boolean expanded=prefs.getBoolean(key,true);
  nested.setVisibility(expanded?View.VISIBLE:View.GONE);arrow.setText(expanded?"▾":"▸");
 }
 void saveBoardBranch(View nested,boolean expanded){
  Object key=nested.getTag(0x7f0f00fe);
  if(key instanceof String)prefs.edit().putBoolean((String)key,expanded).apply();
 }
 void setAllBoardBranches(View view,boolean expand){
  if(view instanceof TextView&&"board_branch_arrow".equals(view.getTag())){
   View parent=(View)view.getParent();
   if(parent instanceof android.view.ViewGroup){
    android.view.ViewGroup group=(android.view.ViewGroup)parent.getParent();
    int index=group.indexOfChild(parent);
    for(int i=index+1;i<group.getChildCount();i++){
     View child=group.getChildAt(i);
     if("board_branch_children".equals(child.getTag())){
      child.setVisibility(expand?View.VISIBLE:View.GONE);saveBoardBranch(child,expand);
      ((TextView)view).setText(expand?"▾":"▸");break;
     }
     if(child instanceof LinearLayout)break;
    }
   }
  }
  if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)setAllBoardBranches(group.getChildAt(i),expand);}
 }
 void addBoardExpandControls(LinearLayout panel){
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.END);panel.addView(controls);
  for(boolean expand:new boolean[]{true,false}){
   TextView action=dashboardText(expand?"▾ Kembang semua":"▸ Lipat semua",11,false);
   action.setTextColor(0xff216e4e);action.setGravity(Gravity.CENTER);action.setPadding(dp(2),dp(5),dp(2),dp(5));
   controls.addView(action,new LinearLayout.LayoutParams(0,-2,1));action.setOnClickListener(v->setAllBoardBranches(panel,expand));
  }
 }
 void showBoardSearch(boolean books){
  LinkedHashSet<String> nodes=new LinkedHashSet<>();
  for(String name:prefs.getString(books?"categories_m":"categories_n","").split("\\|")){ArrayList<String> chain=new ArrayList<>();for(String n=name.trim();!n.isEmpty();n=boardParent(n))chain.add(0,n);nodes.addAll(chain);}
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(12),dp(6),dp(12),0);
  EditText field=new EditText(this);field.setSingleLine(true);field.setHint("Cari Board atau Sub-board");content.addView(field);
  android.widget.ListView results=new android.widget.ListView(this);content.addView(results,new LinearLayout.LayoutParams(-1,dp(280)));
  ArrayList<String> matches=new ArrayList<>();
  ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,matches);results.setAdapter(adapter);
  Runnable refresh=()->{matches.clear();String q=field.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);for(String n:nodes)if(n.toLowerCase(java.util.Locale.ROOT).contains(q))matches.add(n);adapter.notifyDataSetChanged();};
  field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh.run();}public void afterTextChanged(Editable e){}});
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Cari Board "+(books?"Maktabah":"Nota")).setView(content).setNegativeButton("Tutup",null).create();
  results.setOnItemClickListener((p,v,i,id)->{
   String target=matches.get(i);dialog.dismiss();sectionOpen=true;maktabah=books;categoryFilter=target;
   android.content.SharedPreferences.Editor changes=prefs.edit();changes.putString(books?"board_last_m":"board_last_n",target);
   for(String parent=boardParent(target);!parent.isEmpty();parent=boardParent(parent))changes.putBoolean("board_expand_"+(books?"m:":"n:")+parent,true);
   changes.apply();if(books)savedBooksScroll=0;else savedNotesScroll=0;notesPage();
   View row=root.findViewWithTag("board_row:"+(books?"m:":"n:")+target);
   if(row!=null)row.post(()->row.requestRectangleOnScreen(new android.graphics.Rect(0,0,row.getWidth(),row.getHeight()),true));
  });refresh.run();dialog.show();
 }
 void renderHomeBoards(LinearLayout outer,boolean books){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(2),dp(2),dp(2),dp(2));panel.setBackground(rounded(0xfffdfefe,0xffd8c68a,8));
  LinearLayout.LayoutParams boxParams=new LinearLayout.LayoutParams(-1,-2);boxParams.bottomMargin=dp(8);outer.addView(panel,boxParams);
  TextView heading=dashboardText(books?"▤  Maktabah":"✎  Nota",16,true);heading.setTextColor(0xff216e4e);heading.setPadding(dp(8),dp(7),dp(5),dp(7));heading.setBackground(rounded(0xffe3eee7,0xffdce7df,5));LinearLayout boardHeader=new LinearLayout(this);boardHeader.setGravity(Gravity.CENTER_VERTICAL);boardHeader.setBackground(rounded(0xffe3eee7,0xffdce7df,5));panel.addView(boardHeader);boardHeader.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  TextView boardSearch=text("⌕",22);boardSearch.setGravity(Gravity.CENTER);boardSearch.setTextColor(0xff216e4e);boardSearch.setContentDescription("Cari Board "+(books?"Maktabah":"Nota"));boardHeader.addView(boardSearch,new LinearLayout.LayoutParams(dp(36),dp(38)));boardSearch.setOnClickListener(v->showBoardSearch(books));
  heading.setOnClickListener(v->openSection(books));
  addBoardExpandControls(panel);
  LinkedHashSet<String> nodes=new LinkedHashSet<>();
  for(String name:prefs.getString(books?"categories_m":"categories_n","").split("\\|")){ArrayList<String> chain=new ArrayList<>();for(String n=name.trim();!n.isEmpty();n=boardParent(n))chain.add(0,n);nodes.addAll(chain);}
  renderHomeBoardRows(panel,nodes,"",0,books,boardPostingCounts(books));
  TextView manage=dashboardText("+ Tambah / Urus Board",13,true);manage.setTextColor(0xff216e4e);manage.setPadding(dp(5),dp(8),dp(5),dp(8));panel.addView(manage);manage.setOnClickListener(v->{maktabah=books;manageCategories();});
 }
 void refreshBoardSelection(View view){
  Object tag=view.getTag();
  if(tag instanceof String&&((String)tag).startsWith("board_row:")){
   boolean active=sectionOpen&&tag.equals("board_row:"+(maktabah?"m:":"n:")+categoryFilter);
   view.setBackground(active?rounded(0xffe3eee7,0xffc9dfd0,5):null);
  }
  if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)refreshBoardSelection(group.getChildAt(i));}
 }
 void renderHomeBoardRows(LinearLayout panel,LinkedHashSet<String> nodes,String parent,int depth,boolean books,java.util.Map<String,Integer> counts){
  for(String name:nodes){if(!boardParent(name).equals(parent))continue;
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(Math.min(depth,8)*14),0,0,0);panel.addView(row);
   boolean children=false;for(String n:nodes)if(boardParent(n).equals(name)){children=true;break;}
   TextView arrow=dashboardText(children?"▾":" ",15,true);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(dp(24),dp(38)));
   TextView label=dashboardText((depth==0?"▣  ":"↳  ")+boardLeaf(name),14,depth==0);label.setTextColor(0xff365c48);label.setGravity(Gravity.CENTER_VERTICAL);row.addView(label,new LinearLayout.LayoutParams(0,dp(38),1));
   addBoardPostingCount(row,counts.getOrDefault(name,0));
   row.setTag("board_row:"+(books?"m:":"n:")+name);
   if(sectionOpen&&maktabah==books&&name.equals(categoryFilter))row.setBackground(rounded(0xffe3eee7,0xffc9dfd0,5));
   label.setOnClickListener(v->{sectionOpen=true;maktabah=books;categoryFilter=name;prefs.edit().putString(books?"board_last_m":"board_last_n",name).apply();if(books)savedBooksScroll=0;else savedNotesScroll=0;notesPage(true);});
   View rule=new View(this);rule.setBackgroundColor(0xffe5eae6);panel.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));
   if(children){LinearLayout nested=new LinearLayout(this);nested.setOrientation(1);restoreBoardBranch(arrow,nested,"board_expand_"+(books?"m:":"n:")+name);panel.addView(nested);renderHomeBoardRows(nested,nodes,name,depth+1,books,counts);arrow.setOnClickListener(v->{boolean show=nested.getVisibility()!=View.VISIBLE;nested.setVisibility(show?View.VISIBLE:View.GONE);saveBoardBranch(nested,show);arrow.setText(show?"▾":"▸");});}
  }
 }
 String boardParent(String name){int i=Math.max(name.lastIndexOf(" / "),name.lastIndexOf(" › "));return i<0?"":name.substring(0,i);}
 String boardLeaf(String name){int i=Math.max(name.lastIndexOf(" / "),name.lastIndexOf(" › "));return i<0?name:name.substring(i+3);}
 void manageCategories(){
  ArrayList<String> all=categories();LinkedHashSet<String> nodes=new LinkedHashSet<>();
  for(String name:all){ArrayList<String> chain=new ArrayList<>();for(String n=name;!n.isEmpty();n=boardParent(n))chain.add(0,n);nodes.addAll(chain);}
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(8),dp(12),dp(8));
  editor=null;titleEditor=null;categoryPicker=null;selected=null;sectionOpen=true;
  base("DinNote");root.setTag("board_management");
  LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER_VERTICAL);tabs.setPadding(dp(8),dp(6),dp(8),dp(4));root.addView(tabs);
  
  Button notes=new Button(this);notes.setText("Nota");styleBoardButton(notes,!maktabah);tabs.addView(notes,new LinearLayout.LayoutParams(0,-2,1));notes.setEnabled(maktabah);notes.setOnClickListener(v->{maktabah=false;manageCategories();});
  Button books=new Button(this);books.setText("Maktabah");styleBoardButton(books,maktabah);tabs.addView(books,new LinearLayout.LayoutParams(0,-2,1));books.setEnabled(!maktabah);books.setOnClickListener(v->{maktabah=true;manageCategories();});
  Button back=new Button(this);back.setText("›");styleBoardButton(back,false);tabs.addView(back);back.setOnClickListener(v->openSection(maktabah));
  TextView heading=text("Board Tree",16);heading.setTextColor(0xff216e4e);heading.setPadding(dp(12),0,dp(4),0);heading.setSingleLine(true);
  LinearLayout boardTitle=new LinearLayout(this);boardTitle.setGravity(Gravity.CENTER_VERTICAL);boardTitle.setPadding(0,dp(4),dp(8),dp(4));root.addView(boardTitle);boardTitle.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  addBoardToolbarIcon(boardTitle,0,"Cari Board",()->showBoardSearch(maktabah));
  addBoardToolbarIcon(boardTitle,1,"Tambah Board",()->categoryDialog(null));
  addBoardToolbarIcon(boardTitle,2,"Lipat semua",()->setAllBoardBranches(panel,false));
  addBoardToolbarIcon(boardTitle,3,"Kembang semua",()->setAllBoardBranches(panel,true));
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  renderBoardRows(panel,null,nodes,"",0,boardPostingCounts(maktabah));
  rememberBoardScroll(scroll,maktabah?"board_scroll_manage_m":"board_scroll_manage_n");
 }
 void addBoardToolbarIcon(LinearLayout row,int kind,String label,Runnable action){
  View icon=new View(this){
   @Override protected void onDraw(android.graphics.Canvas c){
    super.onDraw(c);c.save();c.translate(getWidth()/2f,getHeight()/2f);float scale=dp(20)/20f;c.scale(scale,scale);
    android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);p.setColor(0xff216e4e);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setStrokeJoin(android.graphics.Paint.Join.ROUND);
    if(kind==0){c.drawCircle(-2,-2,5.5f,p);c.drawLine(2,2,7,7,p);}
    else if(kind==1){c.drawLine(-6,0,6,0,p);c.drawLine(0,-6,0,6,p);}
    else {float y=kind==2?1:-1; c.drawLine(-5,3*y,0,-3*y,p);c.drawLine(0,-3*y,5,3*y,p);}
    c.restore();
   }
  };
  icon.setContentDescription(label);icon.setFocusable(true);icon.setBackground(rounded(0xfff8faf8,0xffdce5df,8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(36),dp(36));lp.leftMargin=dp(4);row.addView(icon,lp);applyTouchFeedback(icon);icon.setOnClickListener(v->action.run());
 }
 void appendOrderedBoardCategories(LinkedHashSet<String> nodes,java.util.Set<String> actual,String parent,ArrayList<String> ordered){
  for(String node:nodes)if(boardParent(node).equals(parent)){
   if(actual.contains(node))ordered.add(node);
   appendOrderedBoardCategories(nodes,actual,node,ordered);
  }
 }
 void moveBoardWithinLevel(String source,String target,boolean after){
  if(source.equals(target)||!boardParent(source).equals(boardParent(target)))return;
  ArrayList<String> original=categories();
  LinkedHashSet<String> nodes=new LinkedHashSet<>();
  for(String name:original){ArrayList<String> chain=new ArrayList<>();for(String n=name;!n.isEmpty();n=boardParent(n))chain.add(0,n);nodes.addAll(chain);}
  if(!nodes.contains(source)||!nodes.contains(target))return;
  LinkedHashSet<String> reordered=new LinkedHashSet<>();
  for(String node:nodes){
   if(node.equals(source))continue;
   if(node.equals(target)&&!after)reordered.add(source);
   reordered.add(node);
   if(node.equals(target)&&after)reordered.add(source);
  }
  ArrayList<String> ordered=new ArrayList<>();
  appendOrderedBoardCategories(reordered,new LinkedHashSet<>(original),"",ordered);
  if(ordered.size()!=original.size()||!new LinkedHashSet<>(ordered).equals(new LinkedHashSet<>(original)))return;
  storeCategories(ordered);manageCategories();
 }
 void renderBoardRows(LinearLayout panel,AlertDialog dialog,LinkedHashSet<String> nodes,String parent,int depth,java.util.Map<String,Integer> counts){
  for(String name:nodes){if(!boardParent(name).equals(parent))continue;
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(Math.min(depth,8)*14),dp(2),dp(3),dp(2));row.setBackground(rounded(0xfffdfefe,0xffe0e7e2,7));panel.addView(row);
   boolean children=false;for(String n:nodes)if(boardParent(n).equals(name)){children=true;break;}
   final boolean hasChildren=children;
   TextView arrow=text(children?"▾":"",15);arrow.setGravity(Gravity.CENTER);arrow.setTextColor(0xff216e4e);row.addView(arrow,new LinearLayout.LayoutParams(dp(28),dp(40)));TextView label=text((depth==0?"▣  ":"↳  ")+boardLeaf(name),15);label.setTextColor(0xff216e4e);label.setMinHeight(dp(44));label.setGravity(Gravity.CENTER_VERTICAL);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
   addBoardPostingCount(row,counts.getOrDefault(name,0));
   TextView grip=text("⠿",20);grip.setGravity(Gravity.CENTER);grip.setTextColor(0xff687b70);grip.setContentDescription("Tekan lama untuk susun "+boardLeaf(name));row.addView(grip,new LinearLayout.LayoutParams(dp(28),dp(44)));
   grip.setOnLongClickListener(v->v.startDragAndDrop(android.content.ClipData.newPlainText("board",name),new View.DragShadowBuilder(row),name,0));
   row.setOnDragListener((v,event)->{
    Object local=event.getLocalState();
    if(!(local instanceof String)||local.equals(name)||!boardParent((String)local).equals(parent))return false;
    switch(event.getAction()){
     case android.view.DragEvent.ACTION_DRAG_STARTED:return true;
     case android.view.DragEvent.ACTION_DRAG_ENTERED:v.setBackground(rounded(0xffe3eee7,0xff216e4e,7));return true;
     case android.view.DragEvent.ACTION_DRAG_EXITED:
     case android.view.DragEvent.ACTION_DRAG_ENDED:v.setBackground(rounded(0xfffdfefe,0xffe0e7e2,7));return true;
     case android.view.DragEvent.ACTION_DROP:moveBoardWithinLevel((String)local,name,event.getY()>v.getHeight()/2f);return true;
     default:return true;
    }
   });
   Button more=new Button(this);more.setText("⋮");styleBoardButton(more,false);row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(44)));
   more.setOnClickListener(v->{if(dialog!=null)dialog.dismiss();new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Tambah Sub-board","Pindah Board","Urus kategori"},(d,w)->{if(w==0)addBoardChild(name);else if(w==1)chooseBoardDestination(name);else categoryActions(name);}).setNegativeButton("Batal",null).show();});
   LinearLayout nested=new LinearLayout(this);nested.setOrientation(1);panel.addView(nested);
   label.setOnClickListener(v->{if(dialog!=null)dialog.dismiss();sectionOpen=true;categoryFilter=name;prefs.edit().putString(maktabah?"board_last_m":"board_last_n",name).apply();if(maktabah)savedBooksScroll=0;else savedNotesScroll=0;notesPage();});
   if(hasChildren){restoreBoardBranch(arrow,nested,"board_expand_"+(maktabah?"m:":"n:")+name);renderBoardRows(nested,dialog,nodes,name,depth+1,counts);arrow.setContentDescription("Kembang atau lipat "+boardLeaf(name));arrow.setOnClickListener(v->{boolean show=nested.getVisibility()!=View.VISIBLE;nested.setVisibility(show?View.VISIBLE:View.GONE);saveBoardBranch(nested,show);arrow.setText(show?"▾":"▸");});}
  }
 }
 void styleBoardButton(Button button,boolean active){button.setAllCaps(false);button.setTextColor(0xff216e4e);button.setTextSize(14);button.setElevation(0);button.setStateListAnimator(null);button.setBackground(rounded(active?0xffe3eee7:0xfff8faf8,0xffd4dfd7,8));button.setPadding(dp(8),dp(4),dp(8),dp(4));}
 void addBookSubcategory(){manageCategories();}
 boolean isBoardBelow(String name,String parent){
  return name.startsWith(parent+" / ")||name.startsWith(parent+" › ");
 }
 void chooseBoardDestination(String source){
  ArrayList<String> destinations=new ArrayList<>();destinations.add("");
  LinkedHashSet<String> nodes=new LinkedHashSet<>();
  for(String name:categories()){ArrayList<String> chain=new ArrayList<>();for(String n=name;!n.isEmpty();n=boardParent(n))chain.add(0,n);nodes.addAll(chain);}
  for(String node:nodes)if(!node.equals(source)&&!isBoardBelow(node,source))destinations.add(node);
  String[] labels=new String[destinations.size()];
  for(int i=0;i<labels.length;i++)labels[i]=i==0?"Peringkat utama":destinations.get(i);
  new AlertDialog.Builder(this).setTitle("Pindah "+boardLeaf(source)+" ke").setItems(labels,(d,w)->{
   String target=destinations.get(w);
   new AlertDialog.Builder(this).setMessage("Pindah Board bersama semua posting dan Sub-board ke "+labels[w]+"?").setPositiveButton("Pindah",(a,b)->moveBoardToParent(source,target)).setNegativeButton("Batal",null).show();
  }).setNegativeButton("Batal",null).show();
 }
 void moveBoardToParent(String source,String parent){
  if(source.equals(parent)||isBoardBelow(parent,source)||boardParent(source).equals(parent))return;
  String target=parent.isEmpty()?boardLeaf(source):parent+" / "+boardLeaf(source);
  ArrayList<String> original=categories(),updated=new ArrayList<>();
  LinkedHashSet<String> existing=new LinkedHashSet<>();
  for(String cat:original)for(String n=cat;!n.isEmpty();n=boardParent(n))existing.add(n);
  for(String n:existing)if(!n.equals(source)&&!isBoardBelow(n,source)&&boardParent(n).equals(parent)&&boardLeaf(n).equals(boardLeaf(source))){
   Toast.makeText(this,"Nama Board sudah wujud",Toast.LENGTH_LONG).show();return;
  }
  for(String cat:original){
   String next=cat.equals(source)?target:isBoardBelow(cat,source)?target+cat.substring(source.length()):cat;
   if(updated.contains(next)){Toast.makeText(this,"Nama Board bertindih",Toast.LENGTH_LONG).show();return;}
   updated.add(next);
  }
  android.content.SharedPreferences.Editor changes=prefs.edit();
  String prefix=maktabah?"m_":"n_";
  for(String key:prefs.getAll().keySet())if(key.startsWith(prefix)&&prefs.getAll().get(key) instanceof String){
   String cat=prefs.getString("cat_"+key,"");
   if(cat.equals(source)||isBoardBelow(cat,source))changes.putString("cat_"+key,target+cat.substring(source.length()));
  }
  String lastKey=maktabah?"board_last_m":"board_last_n";
  String last=prefs.getString(lastKey,"Semua");
  if(last.equals(source)||isBoardBelow(last,source))changes.putString(lastKey,target+last.substring(source.length()));
  String statePrefix="board_expand_"+(maktabah?"m:":"n:");
  for(String key:prefs.getAll().keySet())if(key.startsWith(statePrefix)){
   String cat=key.substring(statePrefix.length());
   if(cat.equals(source)||isBoardBelow(cat,source)){changes.putBoolean(statePrefix+target+cat.substring(source.length()),prefs.getBoolean(key,true));changes.remove(key);}
  }
  changes.putString(maktabah?"categories_m":"categories_n",android.text.TextUtils.join("|",updated));
  if(!changes.commit()){Toast.makeText(this,"Gagal menyimpan pemindahan",Toast.LENGTH_LONG).show();return;}
  if(categoryFilter.equals(source)||isBoardBelow(categoryFilter,source))categoryFilter=target+categoryFilter.substring(source.length());
  manageCategories();
 }
 void addBoardChild(String parent){
  EditText field=new EditText(this);field.setSingleLine(true);field.setHint("Nama Sub-board");
  new AlertDialog.Builder(this).setTitle("Sub-board dalam "+parent).setView(field).setPositiveButton("Simpan",(d,w)->{
   String child=field.getText().toString().trim().replace("|","").replace("/","").trim();
   if(child.isEmpty()||child.equals("Semua")||child.equals("Tanpa kategori"))return;
   ArrayList<String> all=categories();String full=parent+" / "+child;
   if(!all.contains(parent))all.add(parent);
   if(!all.contains(full)){all.add(full);storeCategories(all);}
   categoryFilter="Semua";openSection(maktabah);manageCategories();
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
      String next=cat.equals(old)?name:((cat.startsWith(old+" / ")||cat.startsWith(old+" › "))?name+cat.substring(old.length()):cat);
      if(updated.contains(next)){Toast.makeText(this,"Nama kategori bertindih",Toast.LENGTH_LONG).show();return;}
      updated.add(next);
     }
     c=updated;
     android.content.SharedPreferences.Editor changes=prefs.edit();
     for(String k:prefs.getAll().keySet()){
      if(!k.startsWith(maktabah?"m_":"n_"))continue;
      String cat=prefs.getString("cat_"+k,"");
      if(cat.equals(old))changes.putString("cat_"+k,name);
      else if((cat.startsWith(old+" / ")||cat.startsWith(old+" › ")))changes.putString("cat_"+k,name+cat.substring(old.length()));
     }
     if(!changes.commit()){Toast.makeText(this,"Gagal mengemas kini kategori",Toast.LENGTH_LONG).show();return;}
    }else if(old==null)c.add(name);
    storeCategories(c);categoryFilter="Semua";if("board_management".equals(root.getTag()))manageCategories();else openSection(maktabah);
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
 void receiveBookShare(){
  Intent incoming=getIntent();
  if(incoming==null||!Intent.ACTION_SEND.equals(incoming.getAction()))return;
  CharSequence shared=incoming.getCharSequenceExtra(Intent.EXTRA_TEXT);
  String subject=incoming.getStringExtra(Intent.EXTRA_SUBJECT);
  incoming.setAction(Intent.ACTION_MAIN);incoming.removeExtra(Intent.EXTRA_TEXT);
  if(shared!=null)showSharedBookImport(shared.toString(),subject);
 }
 void showSharedBookImport(String content,String subject){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Nama kitab");if(subject!=null)title.setText(subject);panel.addView(title);
  EditText source=new EditText(this);source.setSingleLine(true);source.setHint("Sumber / URL / jilid / halaman");panel.addView(source);
  EditText body=new EditText(this);body.setHint("Tampal teks kitab daripada Syamilah");body.setGravity(Gravity.TOP);body.setMinLines(5);body.setMaxLines(9);body.setText(content);panel.addView(body);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Import teks kitab").setView(panel).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
   String value=body.getText().toString().trim(),name=title.getText().toString().trim(),ref=source.getText().toString().trim();
   if(name.isEmpty()){title.setError("Masukkan nama kitab");return;}
   if(value.isEmpty()||value.length()>500000||value.matches("https?://\\\\S+")){body.setError("Masukkan teks kitab, bukan pautan sahaja (maksimum 500,000 aksara)");return;}
   String key="m_"+java.util.UUID.randomUUID().toString();
   if(!ref.isEmpty())value+="\n\nSumber: "+ref;
   boolean ok=prefs.edit().putString(key,value).putString("title_"+key,name).putString("cat_"+key,"Tanpa kategori").putLong("updated_"+key,System.currentTimeMillis()).commit();
   if(ok){dialog.dismiss();openSection(true);}else Toast.makeText(this,"Simpan gagal",Toast.LENGTH_LONG).show();
  }));dialog.show();
 }
 ArrayList<String> managedBooks(){
  ArrayList<String> books=new ArrayList<>();java.util.Map<String,?> snapshot=prefs.getAll();
  for(String k:snapshot.keySet())if(k.startsWith("m_")&&snapshot.get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false))books.add(k);
  java.util.Collections.sort(books,(a,b)->{int rank=Integer.compare(prefs.getInt("book_rank_"+a,Integer.MAX_VALUE),prefs.getInt("book_rank_"+b,Integer.MAX_VALUE));return rank!=0?rank:prefs.getString("title_"+a,"").compareToIgnoreCase(prefs.getString("title_"+b,""));});return books;
 }
 void showBookManagement(){
  ArrayList<String> books=managedBooks();String[] names=new String[books.size()];for(int i=0;i<names.length;i++)names[i]=prefs.getString("title_"+books.get(i),"Tanpa tajuk");
  new AlertDialog.Builder(this).setTitle("Urus kitab & tag").setItems(names,(dg,i)->editBookDetails(books.get(i))).setNeutralButton("Cari tag",(dg,w)->searchBookTags()).setNegativeButton("Tutup",null).show();
 }
 void editBookDetails(String key){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  String[] fields={"Pengarang","Edisi / penerbit","Jilid / rujukan","Tag (pisahkan dengan koma)"};
  String[] prefixes={"book_author_","book_edition_","book_reference_","book_tags_"};
  EditText[] inputs=new EditText[fields.length];for(int i=0;i<fields.length;i++){inputs[i]=new EditText(this);inputs[i].setSingleLine(true);inputs[i].setHint(fields[i]);inputs[i].setText(prefs.getString(prefixes[i]+key,""));panel.addView(inputs[i]);}
  int words=0;boolean inWord=false;String source=selectedReadingText(key,0);for(int i=0;i<source.length();i++){boolean letter=Character.isLetterOrDigit(source.charAt(i));if(letter&&!inWord)words++;if(letter)inWord=true;else if(Character.getType(source.charAt(i))!=Character.NON_SPACING_MARK)inWord=false;}
  panel.addView(text("Anggaran bacaan: "+Math.max(1,(words+119)/120)+" minit (120 perkataan/minit)",13));
  new AlertDialog.Builder(this).setTitle(prefs.getString("title_"+key,"Kitab")).setView(panel).setPositiveButton("Simpan",(dg,w)->{android.content.SharedPreferences.Editor tx=prefs.edit();for(int i=0;i<inputs.length;i++)tx.putString(prefixes[i]+key,inputs[i].getText().toString().trim());tx.apply();}).setNegativeButton("Batal",null).show();
 }
 void searchBookTags(){
  java.util.TreeMap<String,ArrayList<String>> tags=new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  for(String key:managedBooks())for(String raw:prefs.getString("book_tags_"+key,"").split("[,،]")){String tag=raw.trim();if(tag.isEmpty())continue;if(!tags.containsKey(tag))tags.put(tag,new ArrayList<>());if(!tags.get(tag).contains(key))tags.get(tag).add(key);}
  String[] names=tags.keySet().toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Tag kitab").setItems(names,(dg,i)->{ArrayList<String> keys=tags.get(names[i]);String[] titles=new String[keys.size()];for(int j=0;j<titles.length;j++)titles[j]=prefs.getString("title_"+keys.get(j),"Kitab");new AlertDialog.Builder(this).setTitle(names[i]).setItems(titles,(dd,j)->read(keys.get(j))).setNegativeButton("Tutup",null).show();}).setNegativeButton("Tutup",null).show();
 }
 void showManualBookOrder(){
  ArrayList<String> books=managedBooks();String[] names=new String[books.size()];for(int i=0;i<names.length;i++)names[i]=(i+1)+". "+prefs.getString("title_"+books.get(i),"Kitab");
  new AlertDialog.Builder(this).setTitle("Susun kitab manual").setItems(names,(dg,i)->new AlertDialog.Builder(this).setTitle(names[i]).setItems(new String[]{"Naik satu","Turun satu","Pindah ke teratas"},(dd,w)->{
   int dest=w==0?Math.max(0,i-1):w==1?Math.min(books.size()-1,i+1):0;books.add(dest,books.remove(i));android.content.SharedPreferences.Editor tx=prefs.edit();for(int j=0;j<books.size();j++)tx.putInt("book_rank_"+books.get(j),j);tx.putBoolean("book_manual_order",true).apply();showManualBookOrder();
  }).show()).setNeutralButton("Susunan asal",(dg,w)->{prefs.edit().putBoolean("book_manual_order",false).apply();notesPage();}).setNegativeButton("Selesai",(dg,w)->notesPage()).show();
 }
 void showBookAddMenu(){
  new AlertDialog.Builder(this).setTitle("Tambah kitab").setItems(new String[]{"Tulis / tampal teks","Import TXT / HTML / DOCX / EPUB / PDF","Import DinNote JSON","Buka Maktabah Syamilah","Banding dua kitab","Tampal petikan Syamilah","Koleksi petikan ulama","Penyusun Kuliah Pintar","Indeks Tematik Pintar","Urus kitab & tag","Susun kitab manual"},(d,i)->{
   if(i==0)edit(null);
   else if(i==1){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/plain","text/html","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/epub+zip","application/pdf","application/zip","application/octet-stream"});startActivityForResult(intent,BOOK_TEXT_IMPORT);}
   else if(i==2)startContentImport();
   else if(i==3){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://shamela.ws/")));}catch(Exception e){Toast.makeText(this,"Pelayar tidak tersedia",Toast.LENGTH_SHORT).show();}}
   else if(i==4)chooseComparison(null);
   else if(i==5)showSharedBookImport("",null);
   else if(i==6)showQuoteCollection();
   else if(i==7)showLectureComposer();
   else if(i==8)showThematicIndex();
   else if(i==9)showBookManagement();
   else showManualBookOrder();
  }).show();
 }
 String bookImportName(android.net.Uri uri){
  try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
   if(c!=null&&c.moveToFirst())return c.getString(0);
  }catch(Exception ignored){}
  return "";
 }
 java.util.Map<String,byte[]> readBookZip(byte[] raw)throws Exception{
  java.util.Map<String,byte[]> entries=new java.util.LinkedHashMap<>();int total=0,count=0;
  try(java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(raw))){
   java.util.zip.ZipEntry entry;byte[] buf=new byte[8192];
   while((entry=zip.getNextEntry())!=null){
    if(++count>3000)throw new java.io.IOException("Terlalu banyak fail");
    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();int n;
    while((n=zip.read(buf))!=-1){total+=n;if(total>20*1024*1024)throw new java.io.IOException("Kandungan melebihi 20 MB");out.write(buf,0,n);}
    if(!entry.isDirectory())entries.put(entry.getName(),out.toByteArray());zip.closeEntry();
   }
  }return entries;
 }
 org.xmlpull.v1.XmlPullParser bookXml(byte[] data)throws Exception{
  if(data==null)throw new java.io.IOException("Struktur kitab tidak lengkap");
  org.xmlpull.v1.XmlPullParser p=android.util.Xml.newPullParser();p.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);p.setInput(new java.io.ByteArrayInputStream(data),null);return p;
 }
 String extractDocx(java.util.Map<String,byte[]> entries)throws Exception{
  org.xmlpull.v1.XmlPullParser p=bookXml(entries.get("word/document.xml"));StringBuilder out=new StringBuilder();
  for(int e=p.getEventType();e!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT;e=p.next()){
   if(e==org.xmlpull.v1.XmlPullParser.START_TAG&&"t".equals(p.getName()))out.append(p.nextText());
   else if(e==org.xmlpull.v1.XmlPullParser.START_TAG&&"tab".equals(p.getName()))out.append('\t');
   else if(e==org.xmlpull.v1.XmlPullParser.START_TAG&&"br".equals(p.getName()))out.append('\n');
   else if(e==org.xmlpull.v1.XmlPullParser.END_TAG&&"p".equals(p.getName()))out.append('\n');
  }return out.toString();
 }
 String extractEpub(java.util.Map<String,byte[]> entries)throws Exception{
  org.xmlpull.v1.XmlPullParser p=bookXml(entries.get("META-INF/container.xml"));String opf=null;
  for(int e=p.getEventType();e!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT;e=p.next())if(e==org.xmlpull.v1.XmlPullParser.START_TAG&&"rootfile".equals(p.getName())){opf=p.getAttributeValue(null,"full-path");break;}
  p=bookXml(entries.get(opf));java.util.Map<String,String> manifest=new java.util.HashMap<>();ArrayList<String> spine=new ArrayList<>();
  for(int e=p.getEventType();e!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT;e=p.next())if(e==org.xmlpull.v1.XmlPullParser.START_TAG){
   if("item".equals(p.getName()))manifest.put(p.getAttributeValue(null,"id"),p.getAttributeValue(null,"href"));
   if("itemref".equals(p.getName()))spine.add(p.getAttributeValue(null,"idref"));
  }
  StringBuilder out=new StringBuilder();
  for(String id:spine){String href=manifest.get(id);if(href==null)throw new java.io.IOException("Bab EPUB tidak lengkap");
   String resolved=new java.net.URI(null,null,"/"+opf,null).resolve(href).normalize().getPath();
   byte[] chapter=entries.get(resolved.substring(1));if(chapter==null)throw new java.io.IOException("Bab EPUB tidak dapat dibaca");
   out.append(android.text.Html.fromHtml(new String(chapter,java.nio.charset.StandardCharsets.UTF_8),0).toString()).append("\n\n");
   if(out.length()>500000)throw new java.io.IOException("Pecahkan kitab kepada jilid lebih kecil");
  }return out.toString();
 }
 void saveOriginalPdf(byte[] bytes,String filename){
  EditText name=new EditText(this);name.setSingleLine(true);name.setText(filename.replaceFirst("\\.[^.]+$",""));
  new AlertDialog.Builder(this).setTitle("Simpan PDF asal").setView(name).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
   String key="m_"+java.util.UUID.randomUUID().toString();
   boolean ok=prefs.edit().putString(key,"Dokumen PDF asal · Bacaan offline").putString("pdf_original_"+key,android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)).putString("title_"+key,name.getText().toString().trim()).putString("cat_"+key,"Tanpa kategori").putLong("updated_"+key,System.currentTimeMillis()).commit();
   if(ok)openSection(true);else Toast.makeText(this,"Simpan gagal",Toast.LENGTH_LONG).show();
  }).show();
 }
 void showOriginalPdf(String key){
  java.io.File tmp=null;
  android.os.ParcelFileDescriptor fd=null;
  android.graphics.pdf.PdfRenderer renderer=null;
  try{
   tmp=java.io.File.createTempFile("dinnote_pdf_",".pdf",getCacheDir());
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(tmp)){out.write(android.util.Base64.decode(prefs.getString("pdf_original_"+key,""),android.util.Base64.NO_WRAP));}
   fd=android.os.ParcelFileDescriptor.open(tmp,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
   renderer=new android.graphics.pdf.PdfRenderer(fd);fd=null;
   final android.graphics.pdf.PdfRenderer pdf=renderer;final java.io.File cache=tmp;
   if(pdf.getPageCount()==0)throw new java.io.IOException("PDF kosong");
   LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);
   LinearLayout controls=new LinearLayout(this);
   Button prev=new Button(this),next=new Button(this);prev.setText("‹");next.setText("›");
   TextView pageLabel=text("",13);pageLabel.setGravity(Gravity.CENTER);
   controls.addView(prev);controls.addView(pageLabel,new LinearLayout.LayoutParams(0,-2,1));controls.addView(next);panel.addView(controls);
   ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
   ScrollView scroll=new ScrollView(this);scroll.addView(image,new ScrollView.LayoutParams(-1,-2));panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(480)));
   int[] index={Math.max(0,Math.min(pdf.getPageCount()-1,prefs.getInt("pdf_page_"+key,0)))};
   android.graphics.Bitmap[] current={null};
   Runnable draw=()->{
    try(android.graphics.pdf.PdfRenderer.Page page=pdf.openPage(index[0])){
     // Bound bitmap memory on large scanned pages and narrow Fold screens.
     float scale=Math.min(2.5f,1800f/Math.max(page.getWidth(),page.getHeight()));
     double pixels=(double)page.getWidth()*page.getHeight()*scale*scale;
     if(pixels>3000000d)scale*=(float)Math.sqrt(3000000d/pixels);
     android.graphics.Bitmap bmp=android.graphics.Bitmap.createBitmap(Math.max(1,Math.round(page.getWidth()*scale)),Math.max(1,Math.round(page.getHeight()*scale)),android.graphics.Bitmap.Config.ARGB_8888);
     bmp.eraseColor(android.graphics.Color.WHITE);page.render(bmp,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
     image.setImageBitmap(bmp);if(current[0]!=null)current[0].recycle();current[0]=bmp;
     pageLabel.setText((index[0]+1)+" / "+pdf.getPageCount());prev.setEnabled(index[0]>0);next.setEnabled(index[0]+1<pdf.getPageCount());scroll.scrollTo(0,0);
     prefs.edit().putInt("pdf_page_"+key,index[0]).apply();
    }catch(Exception e){Toast.makeText(this,"Halaman gagal dibaca",Toast.LENGTH_SHORT).show();}
   };
   prev.setOnClickListener(v->{if(index[0]>0){index[0]--;draw.run();}});
   next.setOnClickListener(v->{if(index[0]+1<pdf.getPageCount()){index[0]++;draw.run();}});
   stopAutoReading();editor=null;titleEditor=null;readerScroll=null;sectionOpen=true;maktabah=true;selected=key;
   base("DinNote");
   LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
   Button back=new Button(this),menu=new Button(this);back.setText("‹");menu.setText("⋮");
   TextView title=text(prefs.getString("title_"+key,"PDF asal"),16);title.setTypeface(null,android.graphics.Typeface.BOLD);
   bar.addView(back);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));bar.addView(menu);root.addView(bar);
   back.setOnClickListener(v->returnFromReader());
   menu.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Urus PDF").setItems(new String[]{"Ubah tajuk","Ubah kategori","Alih ke tong sampah"},(d,w)->{
    if(w==0){EditText input=new EditText(this);input.setSingleLine(true);input.setText(prefs.getString("title_"+key,""));new AlertDialog.Builder(this).setTitle("Ubah tajuk").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(x,y)->{prefs.edit().putString("title_"+key,input.getText().toString().trim()).apply();title.setText(input.getText().toString().trim());}).show();}
    else if(w==1){ArrayList<String> cats=new ArrayList<>(categories());if(!cats.contains("Tanpa kategori"))cats.add(0,"Tanpa kategori");new AlertDialog.Builder(this).setTitle("Ubah kategori").setItems(cats.toArray(new String[0]),(x,y)->prefs.edit().putString("cat_"+key,cats.get(y)).apply()).show();}
    else confirmDelete(key);
   }).show());
   scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
   root.addView(panel,new LinearLayout.LayoutParams(-1,0,1));
   panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
    public void onViewAttachedToWindow(View v){}
    public void onViewDetachedFromWindow(View v){image.setImageDrawable(null);if(current[0]!=null){current[0].recycle();current[0]=null;}pdf.close();cache.delete();v.removeOnAttachStateChangeListener(this);}
   });
   draw.run();
  }catch(Exception e){if(renderer!=null)renderer.close();if(fd!=null)try{fd.close();}catch(Exception ignored){}if(tmp!=null)tmp.delete();Toast.makeText(this,"PDF gagal dibuka: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 void importBookText(android.net.Uri uri){importBookText(uri,false);}
 void importBookText(android.net.Uri uri,boolean extractPdf){
  try{
   java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
   try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
    if(in==null)throw new java.io.IOException("Fail tidak dapat dibaca");
    byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>5*1024*1024)throw new java.io.IOException("Had fail 5 MB");out.write(buf,0,n);}
   }
   byte[] bytes=out.toByteArray();
   if(bytes.length==0)throw new java.io.IOException("Fail kosong; pilih dokumen yang mengandungi teks");
   String filename=bookImportName(uri);
   String mime=getContentResolver().getType(uri);
   final String body;
   int pdfPages=0,pdfSparsePages=0;
   if(bytes.length>=5&&bytes[0]==37&&bytes[1]==80&&bytes[2]==68&&bytes[3]==70&&bytes[4]==45){
    if(!extractPdf){
     new AlertDialog.Builder(this).setTitle("Cara import PDF").setItems(new String[]{"PDF asal · Kekalkan halaman dan tulisan Arab","Ekstrak teks · Boleh disunting"},(d,w)->{if(w==0)saveOriginalPdf(bytes,filename);else importBookText(uri,true);}).setNegativeButton("Batal",null).show();return;
    }
    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(getApplicationContext());
    try(com.tom_roush.pdfbox.pdmodel.PDDocument doc=com.tom_roush.pdfbox.pdmodel.PDDocument.load(bytes)){
     if(!doc.getCurrentAccessPermission().canExtractContent())throw new java.io.IOException("PDF tidak membenarkan pengekstrakan teks");
     if(doc.getNumberOfPages()==0)throw new java.io.IOException("PDF tidak mempunyai halaman");
     if(doc.getNumberOfPages()>300)throw new java.io.IOException("Had 300 halaman; pecahkan PDF dahulu");
     com.tom_roush.pdfbox.text.PDFTextStripper stripper=new com.tom_roush.pdfbox.text.PDFTextStripper();
     stripper.setSortByPosition(true);
     StringBuilder extracted=new StringBuilder();
     pdfPages=doc.getNumberOfPages();
     for(int page=1;page<=pdfPages;page++){
      stripper.setStartPage(page);stripper.setEndPage(page);
      String pageText=stripper.getText(doc);
      if(pageText.trim().length()<25)pdfSparsePages++;
      extracted.append(pageText).append("\n\n");
      if(extracted.length()>500000)throw new java.io.IOException("Pecahkan kitab kepada jilid lebih kecil");
     }
     body=extracted.toString().trim();
     if(body.isEmpty())throw new java.io.IOException("Tiada teks PDF; fail imbasan memerlukan OCR");
    }
   }else if(bytes.length>=4&&bytes[0]==80&&bytes[1]==75){
    java.util.Map<String,byte[]> entries=readBookZip(bytes);
    if(entries.containsKey("word/document.xml"))body=extractDocx(entries);
    else if(entries.containsKey("META-INF/container.xml"))body=extractEpub(entries);
    else throw new java.io.IOException("Fail ZIP bukan DOCX atau EPUB");
   }else{
    String raw=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
    if(raw.indexOf(0)>=0||raw.indexOf('\uFFFD')>=0||raw.startsWith("%PDF"))throw new java.io.IOException("Format tidak disokong");
    body="text/html".equals(mime)||filename.toLowerCase(java.util.Locale.ROOT).endsWith(".html")?android.text.Html.fromHtml(raw,0).toString():raw;
   }
   if(filename.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")&&looksLikeQuranVerses(body)){
    offerQuranSurahImport(body);return;
   }
   if(body.length()>500000)throw new java.io.IOException("Pecahkan kitab kepada jilid lebih kecil");
   if(body.trim().isEmpty())throw new java.io.IOException("Teks kosong");
   EditText name=new EditText(this);name.setHint("Nama kitab");name.setSingleLine(true);name.setText(filename.replaceFirst("\\.[^.]+$",""));
   LinearLayout importPanel=new LinearLayout(this);importPanel.setOrientation(1);importPanel.addView(name);
   if(extractPdf){String previewText=body.substring(0,Math.min(5000,body.length()));previewText=previewText.replace("\\r\\n","\n").replace("\\n","\n");TextView sample=text("Pratonton "+Math.min(5000,body.length())+" daripada "+body.length()+" aksara. Semak tulisan Arab/Jawi sebelum simpan:\n\n"+previewText,15);sample.setTextIsSelectable(true);sample.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);sample.setPadding(dp(12),dp(8),dp(12),dp(8));ScrollView preview=new ScrollView(this);preview.addView(sample);importPanel.addView(preview,new LinearLayout.LayoutParams(-1,dp(155)));TextView warning=text((pdfSparsePages>0?"Amaran: "+pdfSparsePages+" daripada "+pdfPages+" halaman mengandungi sangat sedikit teks yang dapat diekstrak. ":"")+"Jika Arab/Jawi rosak atau tidak lengkap, pilih Simpan PDF asal. Jangan simpan teks yang rosak.",12);warning.setTextColor(0xff875b25);warning.setPadding(dp(12),dp(6),dp(12),dp(8));importPanel.addView(warning);}
   AlertDialog.Builder importDialog=new AlertDialog.Builder(this).setTitle(extractPdf?"Semak ekstrak PDF":"Import kitab").setView(importPanel).setNegativeButton("Batal",null).setPositiveButton("Simpan teks",(d,w)->{
    String key="m_"+java.util.UUID.randomUUID().toString();
    boolean ok=prefs.edit().putString(key,body).putString("title_"+key,name.getText().toString().trim().isEmpty()?"Kitab import":name.getText().toString().trim()).putString("cat_"+key,"Tanpa kategori").putLong("updated_"+key,System.currentTimeMillis()).commit();
    if(ok)openSection(true);else Toast.makeText(this,"Simpan gagal",Toast.LENGTH_LONG).show();
   });
   if(extractPdf){
    LinearLayout choices=new LinearLayout(this);choices.setOrientation(LinearLayout.HORIZONTAL);
    choices.setPadding(dp(8),dp(4),dp(8),dp(4));
    Button original=new Button(this);original.setText("Simpan PDF asal");original.setAllCaps(false);original.setTextSize(12);
    original.setTextColor(0xff176b47);original.setBackground(rounded(0xffeaf4ee,0xffc5d9ce,9));
    LinearLayout.LayoutParams originalParams=new LinearLayout.LayoutParams(0,dp(42),1);originalParams.rightMargin=dp(8);
    choices.addView(original,originalParams);
    Button cancel=new Button(this);cancel.setText("Batal");cancel.setAllCaps(false);cancel.setTextSize(12);
    cancel.setBackground(rounded(0xfff6f8f7,0xffd9e5dd,9));
    choices.addView(cancel,new LinearLayout.LayoutParams(0,dp(42),1));
    importPanel.addView(choices);
    importDialog.setNegativeButton(null,null);
    AlertDialog previewDialog=importDialog.create();
    original.setOnClickListener(v->{previewDialog.dismiss();saveOriginalPdf(bytes,filename);});
    cancel.setOnClickListener(v->previewDialog.dismiss());
    previewDialog.show();
    // Explicit confirmation prevents accidental saving of broken Arabic/Jawi extraction.
    previewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
     new AlertDialog.Builder(this).setTitle("Pastikan teks PDF betul")
      .setMessage("Pratonton hanya menunjukkan sebahagian teks PDF. Adakah teks yang diperiksa jelas dan betul? Jika ragu-ragu, pilih Simpan PDF asal supaya kandungan tidak hilang.")
      .setNegativeButton("Semak semula",null)
      .setPositiveButton("Ya, simpan teks",(confirm,which)->{
       String key="m_"+java.util.UUID.randomUUID().toString();
       boolean ok=prefs.edit().putString(key,body).putString("title_"+key,name.getText().toString().trim().isEmpty()?"Kitab import":name.getText().toString().trim()).putString("cat_"+key,"Tanpa kategori").putLong("updated_"+key,System.currentTimeMillis()).commit();
       if(ok){previewDialog.dismiss();openSection(true);}else Toast.makeText(this,"Simpan gagal",Toast.LENGTH_LONG).show();
      }).show();
    });
   }else importDialog.show();
  }catch(Exception e){Toast.makeText(this,"Import gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 // Tanzil plain-text Quran import: 114 independent surahs; original verse text is never normalized.
 boolean looksLikeQuranVerses(String raw){
  String[] lines=raw.split("\\r?\\n");int found=0;
  for(String line:lines){
   if(line.startsWith("#")||line.trim().isEmpty())continue;
   if(!line.matches("[0-9]{1,3}\\|[0-9]{1,3}\\|.+"))return false;
   found++;
  }
  return found==6236;
 }
 void offerQuranSurahImport(String raw){
  final String[] surahNames={"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};
  final String[] lines=raw.split("\\r?\\n");
  final StringBuilder[] chapters=new StringBuilder[114];
  final int[] counts=new int[114];
  final StringBuilder credits=new StringBuilder();
  int total=0;boolean valid=true;
  for(String line:lines){
   if(line.startsWith("#")){credits.append(line).append('\n');continue;}
   if(line.trim().isEmpty())continue;
   int p=line.indexOf('|'),q=p<0?-1:line.indexOf('|',p+1);
   try{
    int chapter=Integer.parseInt(line.substring(0,p)),verse=Integer.parseInt(line.substring(p+1,q));
    if(chapter<1||chapter>114||verse!=counts[chapter-1]+1||q+1>=line.length())throw new IllegalArgumentException();
    if(chapters[chapter-1]==null)chapters[chapter-1]=new StringBuilder();
    chapters[chapter-1].append(verse).append("  ").append(line.substring(q+1)).append('\n');
    counts[chapter-1]++;total++;
   }catch(Exception e){valid=false;break;}
  }
  if(!valid||total!=6236||credits.indexOf("Tanzil")<0){
   new AlertDialog.Builder(this).setTitle("Import al-Quran dihentikan").setMessage("Format, turutan ayat atau maklumat sumber tidak lengkap. Tiada kandungan disimpan.").setPositiveButton("OK",null).show();return;
  }
  for(int i=0;i<114;i++)if(chapters[i]==null){Toast.makeText(this,"Surah tidak lengkap",Toast.LENGTH_LONG).show();return;}
  new AlertDialog.Builder(this).setTitle("Import al-Quran · 114 surah")
   .setMessage("6,236 ayat disahkan mengikut turutan fail. Setiap surah akan menjadi topik Maktabah. Teks ayat asal dikekalkan, bersama atribusi Tanzil. Surah sedia ada daripada import ini akan dikemas kini tanpa pendua.")
   .setNegativeButton("Batal",null)
   .setPositiveButton("Import 114 surah",(dialog,which)->{
    android.content.SharedPreferences.Editor batch=prefs.edit();
    long now=System.currentTimeMillis();
    for(int i=0;i<114;i++){
     String key=String.format(java.util.Locale.ROOT,"m_quran_tanzil_simple_%03d",i+1);
     String title=String.format(java.util.Locale.ROOT,"%03d · سورة %s",i+1,surahNames[i]);
     String content=chapters[i].toString().trim()+"\\n\\n— Sumber: Tanzil Quran Text (Simple). Lesen CC BY 3.0. https://tanzil.net\\n";
     batch.putString(key,content).putString("title_"+key,title).putString("cat_"+key,"Al-Quran")
       .putLong("updated_"+key,now).putBoolean("deleted_"+key,false);
    }
    batch.putString("quran_tanzil_source_notice",credits.toString());
    if(batch.commit()){Toast.makeText(this,"114 surah berjaya diimport",Toast.LENGTH_LONG).show();openSection(true);}
    else new AlertDialog.Builder(this).setTitle("Import gagal").setMessage("Tidak dapat menyimpan koleksi al-Quran. Cuba lagi selepas ruang storan dikosongkan.").setPositiveButton("OK",null).show();
   }).show();
 }
 void chooseComparison(String first){
  ArrayList<String> keys=new ArrayList<>(),names=new ArrayList<>();
  for(String k:prefs.getAll().keySet())if(k.startsWith("m_")&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&!k.equals(first))keys.add(k);
  java.util.Collections.sort(keys,(a,b)->prefs.getString("title_"+a,"").compareToIgnoreCase(prefs.getString("title_"+b,"")));
  if(keys.isEmpty()||(first==null&&keys.size()<2)){Toast.makeText(this,"Perlu sekurang-kurangnya dua kitab",Toast.LENGTH_SHORT).show();return;}
  for(String k:keys)names.add(prefs.getString("title_"+k,"Tanpa tajuk"));
  new AlertDialog.Builder(this).setTitle(first==null?"Pilih kitab pertama":"Pilih kitab kedua").setItems(names.toArray(new String[0]),(d,i)->{if(first==null)chooseComparison(keys.get(i));else showComparison(first,keys.get(i));}).show();
 }
 void showGlobalBookSearch(){
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Perkataan Arab atau Melayu");
  new AlertDialog.Builder(this).setTitle("Carian seluruh Maktabah").setView(query).setNegativeButton("Batal",null).setPositiveButton("Cari",(d,w)->{
   final String term=query.getText().toString().trim(),needle=normalizeCompareText(term);
   if(needle.isEmpty())return;
   final java.util.Map<String,?> snapshot=prefs.getAll();
   final LinearLayout currentRoot=root;
   final AlertDialog progress=new AlertDialog.Builder(this).setMessage("Mencari dalam semua kitab…").setCancelable(true).create();progress.show();
   new Thread(()->{
    ArrayList<String> keys=new ArrayList<>(),labels=new ArrayList<>();
    ArrayList<Integer> offsets=new ArrayList<>();
    ArrayList<String> sorted=new ArrayList<>(snapshot.keySet());java.util.Collections.sort(sorted);
    for(String k:sorted){
     if(!k.startsWith("m_")||!(snapshot.get(k) instanceof String)||Boolean.TRUE.equals(snapshot.get("deleted_"+k)))continue;
     String source=formattedReadingText((String)snapshot.get(k)).toString();
     StringBuilder normal=new StringBuilder();int[] map=new int[source.length()];int n=0;
     for(int j=0;j<source.length();j++){
      char c=source.charAt(j);if(c>=0x64b&&c<=0x65f||c==0x670||c==0x640)continue;
      if(c=='أ'||c=='إ'||c=='آ'||c=='ٱ')c='ا';
      normal.append(Character.toLowerCase(c));map[n++]=j;
     }
     String title=String.valueOf(snapshot.containsKey("title_"+k)?snapshot.get("title_"+k):"Tanpa tajuk");
     int at=normal.indexOf(needle);
     if(at<0){if(normalizeCompareText(title).contains(needle)){keys.add(k);offsets.add(-1);labels.add(title);}}
     else{
      int count=0;while(at>=0&&count<20&&keys.size()<500){
       int pos=map[at];keys.add(k);offsets.add(pos);labels.add(title+"\n"+source.substring(Math.max(0,pos-25),Math.min(source.length(),pos+90)).replace('\n',' '));count++;at=normal.indexOf(needle,at+needle.length());
      }
     }
     if(keys.size()>=500)break;
    }
    runOnUiThread(()->{
     if(!progress.isShowing()||isFinishing()||isDestroyed()||root!=currentRoot)return;
     progress.dismiss();
     if(keys.isEmpty()){Toast.makeText(this,"Tiada padanan",Toast.LENGTH_SHORT).show();return;}
     new AlertDialog.Builder(this).setTitle("Hasil Maktabah ("+keys.size()+")").setItems(labels.toArray(new String[0]),(resultDialog,i)->{
      String key=keys.get(i);if(!prefs.contains(key)||prefs.getBoolean("deleted_"+key,false))return;
      if(offsets.get(i)<0){read(key);return;}
      showGlobalBookExcerpt(key,offsets.get(i));
     }).setNegativeButton("Tutup",null).show();
    });
   }).start();
  }).show();
 }
 void showGlobalBookExcerpt(String key,int offset){
  CharSequence source=formattedReadingText(prefs.getString(key,""));
  int pos=Math.min(offset,source.length());
  new AlertDialog.Builder(this).setTitle(prefs.getString("title_"+key,"Kitab")).setMessage(source.subSequence(Math.max(0,pos-100),Math.min(source.length(),pos+250))).setNegativeButton("Tutup",null).setNeutralButton("Simpan petikan",(d,w)->saveBookQuote(key,source.subSequence(Math.max(0,pos-100),Math.min(source.length(),pos+250)).toString())).setPositiveButton("Buka kitab",(d,w)->read(key)).show();
 }
 void saveBookQuote(String key,String excerpt){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  EditText topic=new EditText(this);topic.setSingleLine(true);topic.setHint("Tajuk pengajian");panel.addView(topic);
  EditText ref=new EditText(this);ref.setSingleLine(true);ref.setHint("Jilid / halaman / rujukan");panel.addView(ref);
  EditText quote=new EditText(this);quote.setMinLines(4);quote.setMaxLines(8);quote.setText(excerpt);panel.addView(quote);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Simpan petikan ulama").setView(panel).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
   if(quote.getText().toString().trim().isEmpty()){quote.setError("Masukkan petikan");return;}
   try{
    org.json.JSONObject item=new org.json.JSONObject();
    item.put("book",key);item.put("title",prefs.getString("title_"+key,"Kitab"));item.put("topic",topic.getText().toString().trim());item.put("reference",ref.getText().toString().trim());item.put("text",quote.getText().toString());item.put("created",System.currentTimeMillis());
    if(!prefs.edit().putString("quote_"+java.util.UUID.randomUUID(),item.toString()).commit())throw new java.io.IOException();
    dialog.dismiss();Toast.makeText(this,"Petikan disimpan",Toast.LENGTH_SHORT).show();
   }catch(Exception e){Toast.makeText(this,"Simpan gagal",Toast.LENGTH_SHORT).show();}
  }));dialog.show();
 }
 void showLectureComposer(){
  ArrayList<org.json.JSONObject> items=new ArrayList<>();
  for(String key:prefs.getAll().keySet())if(key.startsWith("quote_"))try{items.add(new org.json.JSONObject(prefs.getString(key,"")));}catch(Exception ignored){}
  if(items.isEmpty()){new AlertDialog.Builder(this).setTitle("Penyusun Kuliah Pintar").setMessage("Simpan petikan ulama dahulu untuk membina nota kuliah.").setPositiveButton("Koleksi petikan",(d,w)->showQuoteCollection()).setNegativeButton("Tutup",null).show();return;}
  java.util.Collections.sort(items,(a,b)->Long.compare(a.optLong("created"),b.optLong("created")));
  String[] labels=new String[items.size()];boolean[] checked=new boolean[items.size()];ArrayList<Integer> order=new ArrayList<>();
  for(int i=0;i<items.size();i++){org.json.JSONObject item=items.get(i);String body=item.optString("text");labels[i]=item.optString("topic")+" · "+item.optString("title")+"\n"+body.substring(0,Math.min(90,body.length()));}
  new AlertDialog.Builder(this).setTitle("Pilih petikan mengikut urutan kuliah").setMultiChoiceItems(labels,checked,(d,i,on)->{if(on)order.add(i);else order.remove(Integer.valueOf(i));}).setNegativeButton("Batal",null).setPositiveButton("Susun",(d,w)->{
   if(order.isEmpty()){Toast.makeText(this,"Pilih sekurang-kurangnya satu petikan",Toast.LENGTH_SHORT).show();return;}
   StringBuilder draft=new StringBuilder();
   for(int j=0;j<order.size();j++){
    org.json.JSONObject item=items.get(order.get(j));
    if(j>0)draft.append("\n\n---CLASSIC---\n\n");
    draft.append(j+1).append(". ").append(item.optString("topic")).append("\n\n").append(item.optString("text")).append("\n\nSumber: ").append(item.optString("title"));
    if(!item.optString("reference").isEmpty())draft.append(" — ").append(item.optString("reference"));
    draft.append("\n\nHuraian:\n");
   }
   EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Tajuk kuliah");
   AlertDialog confirm=new AlertDialog.Builder(this).setTitle("Cipta nota kuliah").setView(title).setNegativeButton("Batal",null).setPositiveButton("Cipta",null).create();
   confirm.setOnShowListener(v->confirm.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{
    String name=title.getText().toString().trim();if(name.isEmpty()){title.setError("Masukkan tajuk");return;}
    String key="n_"+java.util.UUID.randomUUID().toString();
    boolean ok=prefs.edit().putString(key,draft.toString()).putString("title_"+key,name).putString("cat_"+key,"Tanpa kategori").putLong("updated_"+key,System.currentTimeMillis()).commit();
    if(ok){confirm.dismiss();openSection(false);edit(key);}else Toast.makeText(this,"Simpan gagal",Toast.LENGTH_SHORT).show();
   }));confirm.show();
  }).show();
 }
 void showThematicIndex(){
  java.util.TreeMap<String,ArrayList<String>> topics=new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  for(String id:prefs.getAll().keySet())if(id.startsWith("quote_"))try{
   org.json.JSONObject q=new org.json.JSONObject(prefs.getString(id,""));String topic=q.optString("topic").trim();if(topic.isEmpty())topic="Tanpa tajuk";
   if(!topics.containsKey(topic))topics.put(topic,new ArrayList<>());topics.get(topic).add(id);
  }catch(Exception ignored){}
  if(topics.isEmpty()){new AlertDialog.Builder(this).setTitle("الفهرس الموضوعي").setMessage("Simpan petikan bertopik daripada carian Maktabah terlebih dahulu.").setPositiveButton("Cari petikan",(dg,w)->showGlobalBookSearch()).setNegativeButton("Tutup",null).show();return;}
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),dp(8),dp(16),dp(8));
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Cari topik Arab atau Melayu");panel.addView(query);
  ScrollView scroll=new ScrollView(this);LinearLayout rows=new LinearLayout(this);rows.setOrientation(1);scroll.addView(rows);panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(330)));
  final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("الفهرس الموضوعي · Indeks Tematik").setView(panel).setNegativeButton("Tutup",null).create();
  Runnable render=()->{rows.removeAllViews();String filter=normalizeCompareText(query.getText().toString().trim());
   for(String topic:topics.keySet()){if(!normalizeCompareText(topic).contains(filter))continue;
    ArrayList<String> ids=topics.get(topic);java.util.HashSet<String> books=new java.util.HashSet<>();for(String id:ids)try{books.add(new org.json.JSONObject(prefs.getString(id,"")).optString("book"));}catch(Exception ignored){}
    TextView row=text(topic+"   ·   "+ids.size()+" petikan / "+books.size()+" kitab",15);row.setPadding(dp(12),dp(14),dp(12),dp(14));row.setTextColor(0xff176b47);rows.addView(row);row.setOnClickListener(v->{dialog.dismiss();showThematicTopic(topic,ids);});
   }
  };
  query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence c,int a,int n,int f){}public void onTextChanged(CharSequence c,int a,int b,int n){render.run();}public void afterTextChanged(android.text.Editable e){}});
  render.run();dialog.show();
 }
 void showThematicTopic(String topic,ArrayList<String> ids){
  ArrayList<org.json.JSONObject> quotes=new ArrayList<>();ArrayList<String> valid=new ArrayList<>();
  for(String id:ids)try{quotes.add(new org.json.JSONObject(prefs.getString(id,"")));valid.add(id);}catch(Exception ignored){}
  String[] labels=new String[quotes.size()];for(int i=0;i<labels.length;i++){org.json.JSONObject q=quotes.get(i);String body=q.optString("text");labels[i]=q.optString("title")+" · "+q.optString("reference")+"\n"+body.substring(0,Math.min(120,body.length()));}
  new AlertDialog.Builder(this).setTitle(topic).setItems(labels,(dg,i)->showSavedQuote(valid.get(i),quotes.get(i))).setNegativeButton("Kembali",(dg,w)->showThematicIndex()).show();
 }
 void showQuoteCollection(){
  ArrayList<String> ids=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("quote_"))ids.add(k);
  ArrayList<org.json.JSONObject> items=new ArrayList<>();ArrayList<String> valid=new ArrayList<>();
  for(String id:ids)try{items.add(new org.json.JSONObject(prefs.getString(id,"")));valid.add(id);}catch(Exception ignored){}
  if(items.isEmpty()){new AlertDialog.Builder(this).setTitle("Koleksi petikan ulama").setMessage("Cari teks dalam Maktabah, buka petikan hasil carian dan pilih Simpan petikan.").setPositiveButton("Cari",(d,w)->showGlobalBookSearch()).setNegativeButton("Tutup",null).show();return;}
  java.util.TreeSet<String> topics=new java.util.TreeSet<>();
  for(org.json.JSONObject item:items)topics.add(item.optString("topic").isEmpty()?"Tanpa tajuk":item.optString("topic"));
  ArrayList<String> labels=new ArrayList<>(topics);labels.add(0,"Semua petikan");
  new AlertDialog.Builder(this).setTitle("Koleksi petikan ulama").setItems(labels.toArray(new String[0]),(d,w)->{
   ArrayList<Integer> positions=new ArrayList<>();ArrayList<String> rows=new ArrayList<>();
   for(int i=0;i<items.size();i++){org.json.JSONObject item=items.get(i);String t=item.optString("topic");if(w!=0&&!labels.get(w).equals(t.isEmpty()?"Tanpa tajuk":t))continue;
    String excerpt=item.optString("text");positions.add(i);rows.add(item.optString("title")+"\n"+excerpt.substring(0,Math.min(100,excerpt.length())));
   }
   new AlertDialog.Builder(this).setTitle(labels.get(w)).setItems(rows.toArray(new String[0]),(dd,i)->showSavedQuote(valid.get(positions.get(i)),items.get(positions.get(i)))).setNegativeButton("Kembali",(dd,i)->showQuoteCollection()).show();
  }).setNegativeButton("Tutup",null).show();
 }
 void showSavedQuote(String id,org.json.JSONObject item){
  String key=item.optString("book"),body=item.optString("text"),title=item.optString("title");
  new AlertDialog.Builder(this).setTitle(title).setMessage(body+"\n\n"+item.optString("reference")).setPositiveButton("Tindakan",(outer,action)->new AlertDialog.Builder(this).setTitle("Tindakan petikan").setItems(new String[]{"Buka kitab asal","Banding dengan kitab lain","Salin petikan dan sumber","Padam petikan"},(d,w)->{
   if(w<2){if(!prefs.contains(key)||prefs.getBoolean("deleted_"+key,false)){Toast.makeText(this,"Kitab asal tidak tersedia",Toast.LENGTH_SHORT).show();return;}if(w==0)read(key);else chooseComparison(key);}
   else if(w==2){android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cb!=null)cb.setPrimaryClip(android.content.ClipData.newPlainText(title,body+"\n\n"+title+" "+item.optString("reference")));}
   else new AlertDialog.Builder(this).setMessage("Padam petikan ini sahaja?").setNegativeButton("Batal",null).setPositiveButton("Padam",(dd,i)->{prefs.edit().remove(id).apply();showQuoteCollection();}).show();
  }).show()).setNegativeButton("Kembali",(d,w)->showQuoteCollection()).show();
 }
 String normalizeCompareText(String value){
  return value.replaceAll("[\\u064B-\\u065F\\u0670\\u0640]","").replaceAll("[أإآٱ]","ا").toLowerCase(java.util.Locale.ROOT);
 }
 void showComparison(String a,String b){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.HORIZONTAL);panel.setBackgroundColor(0xfff8f7f0);
  final String[] active={a,b};
  for(int side=0;side<2;side++){
   final int index=side;
   LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(6),dp(6),dp(6),dp(6));panel.addView(column,new LinearLayout.LayoutParams(0,-1,1));
   TextView title=text("",15);title.setTextColor(0xff176b47);title.setGravity(Gravity.CENTER);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(2);column.addView(title);
   LinearLayout tools=new LinearLayout(this);column.addView(tools);
   Button change=new Button(this);change.setText("Tukar");tools.addView(change,new LinearLayout.LayoutParams(0,-2,1));
   Button find=new Button(this);find.setText("Cari");tools.addView(find,new LinearLayout.LayoutParams(0,-2,1));
   ScrollView scroll=new ScrollView(this);column.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
   TextView content=text("",15);applySharedTextMetrics(content);content.setTextColor(0xff26352d);content.setGravity(Gravity.START);content.setTextIsSelectable(true);scroll.addView(content);
   final boolean[] restoring={false};
   Runnable load=()->{
    restoring[0]=true;
    String key=active[index];int saved=prefs.getInt("compare_y_"+key,0);
    title.setText(prefs.getString("title_"+key,"Tanpa tajuk"));content.setText(formattedReadingText(prefs.getString(key,"")));
    scroll.post(()->{if(key.equals(active[index])){scroll.scrollTo(0,saved);restoring[0]=false;}});
   };
   scroll.setOnScrollChangeListener((v,x,y,ox,oy)->{if(!restoring[0])prefs.edit().putInt("compare_y_"+active[index],y).apply();});
   change.setOnClickListener(v->{
    ArrayList<String> keys=new ArrayList<>(),names=new ArrayList<>();
    for(String k:prefs.getAll().keySet())if(k.startsWith("m_")&&prefs.getAll().get(k) instanceof String&&!prefs.getBoolean("deleted_"+k,false)&&!k.equals(active[1-index]))keys.add(k);
    java.util.Collections.sort(keys,(x,y)->prefs.getString("title_"+x,"").compareToIgnoreCase(prefs.getString("title_"+y,"")));
    for(String k:keys)names.add(prefs.getString("title_"+k,"Tanpa tajuk"));
    new AlertDialog.Builder(this).setTitle("Tukar kitab").setItems(names.toArray(new String[0]),(d,i)->{active[index]=keys.get(i);load.run();}).show();
   });
   find.setOnClickListener(v->{
    EditText term=new EditText(this);term.setSingleLine(true);term.setHint("Arab atau Melayu");
    new AlertDialog.Builder(this).setTitle("Cari dalam kitab ini").setView(term).setNegativeButton("Batal",null).setPositiveButton("Cari",(d,w)->{
     String q=normalizeCompareText(term.getText().toString().trim());if(q.isEmpty())return;
     CharSequence original=formattedReadingText(prefs.getString(active[index],""));
     StringBuilder normalized=new StringBuilder();ArrayList<Integer> offsets=new ArrayList<>();
     for(int j=0;j<original.length();j++){String ch=normalizeCompareText(original.subSequence(j,j+1).toString());for(int z=0;z<ch.length();z++){normalized.append(ch.charAt(z));offsets.add(j);}}
     android.text.SpannableString marked=new android.text.SpannableString(original);ArrayList<Integer> hits=new ArrayList<>();int at=0;
     while((at=normalized.indexOf(q,at))>=0&&hits.size()<1000){int first=offsets.get(at),last=offsets.get(at+q.length()-1)+1;hits.add(first);marked.setSpan(new android.text.style.BackgroundColorSpan(0xffffe39b),first,last,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);at+=q.length();}
     if(hits.isEmpty()){Toast.makeText(this,"Tiada padanan",Toast.LENGTH_SHORT).show();return;}
     content.setText(marked);String[] results=new String[hits.size()];
     for(int j=0;j<hits.size();j++){int offset=hits.get(j);results[j]=original.subSequence(Math.max(0,offset-20),Math.min(original.length(),offset+70)).toString().replace('\n',' ');}
     new AlertDialog.Builder(this).setTitle("Padanan: "+hits.size()).setItems(results,(dialog,i)->content.post(()->{android.text.Layout layout=content.getLayout();if(layout!=null)scroll.scrollTo(0,layout.getLineTop(layout.getLineForOffset(hits.get(i)))+content.getTotalPaddingTop());})).show();
    }).show();
   });
   load.run();
   if(side==0){View divider=new View(this);divider.setBackgroundColor(0xffc5a66b);panel.addView(divider,new LinearLayout.LayoutParams(dp(1),-1));}
  }
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Perbandingan dua kitab").setView(panel).setPositiveButton("Tutup",null).create();
  dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);
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

 void startPdfExport(){
  if(editor==null)return;
  save();
  pdfExportTitle=titleEditor==null?"":titleEditor.getText().toString().trim();
  if(pdfExportTitle.isEmpty())pdfExportTitle=maktabah?"Kitab DinNote":"Nota DinNote";
  pdfExportBody=editor.getText().toString();
  if(pdfExportBody.trim().isEmpty()){Toast.makeText(this,"Tiada teks untuk dieksport",Toast.LENGTH_LONG).show();return;}
  String name=pdfExportTitle.replaceAll("[^\\p{L}\\p{N} ._-]","_");
  if(name.length()>70)name=name.substring(0,70);
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
  intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");
  intent.putExtra(Intent.EXTRA_TITLE,name+".pdf");
  startActivityForResult(intent,PDF_EXPORT_CREATE);
 }
 void writePdfExport(android.net.Uri uri) throws java.io.IOException{
  final int width=595,height=842,margin=48,contentWidth=width-2*margin;
  android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();
  android.text.TextPaint bodyPaint=new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  bodyPaint.setColor(0xff263e34);bodyPaint.setTextSize(12.5f);
  bodyPaint.setTypeface(android.graphics.Typeface.create("sans-serif",0));
  android.text.TextPaint titlePaint=new android.text.TextPaint(bodyPaint);
  titlePaint.setTextSize(19);titlePaint.setColor(0xff176b47);
  titlePaint.setTypeface(android.graphics.Typeface.create("sans-serif",1));
  android.graphics.pdf.PdfDocument.Page sheet=null;
  int page=0,y=0;
  try{
   String[] blocks=(pdfExportTitle+"\n\n"+pdfExportBody).split("\n",-1);
   for(int i=0;i<blocks.length;i++){
    String line=blocks[i].isEmpty()?" ":blocks[i];
    boolean arabicLine=line.matches(".*[\\u0600-\\u06FF].*"); android.text.TextPaint paint=i==0?titlePaint:(arabicLine?new android.text.TextPaint(bodyPaint):bodyPaint); if(i!=0&&arabicLine)paint.setTextSize(17f);
    android.text.StaticLayout layout=android.text.StaticLayout.Builder.obtain(line,0,line.length(),paint,contentWidth)
      .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
      .setTextDirection(android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
      .setIncludePad(false).build();
    int first=0;
    while(first<layout.getLineCount()){
     if(sheet==null){
      sheet=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(width,height,++page).create());
      android.graphics.Paint accent=new android.graphics.Paint();
      accent.setColor(0xff176b47);sheet.getCanvas().drawRect(margin,28,width-margin,31,accent);
      y=margin+10;
     }
     int last=first,remaining=height-margin-40-y;
     while(last<layout.getLineCount()&&layout.getLineBottom(last)-layout.getLineTop(first)<=remaining)last++;
     if(last==first){finishPdfExportPage(pdf,sheet,page,width,height,margin);sheet=null;continue;}
     android.graphics.Canvas canvas=sheet.getCanvas();
     canvas.save();canvas.translate(margin,y-layout.getLineTop(first));
     canvas.clipRect(0,layout.getLineTop(first),contentWidth,layout.getLineBottom(last-1));
     layout.draw(canvas);canvas.restore();
     y+=layout.getLineBottom(last-1)-layout.getLineTop(first);
     first=last;
     if(first<layout.getLineCount()){finishPdfExportPage(pdf,sheet,page,width,height,margin);sheet=null;}
    }
    y+=i==0?16:8;
    if(sheet!=null&&y>height-margin-55){finishPdfExportPage(pdf,sheet,page,width,height,margin);sheet=null;}
   }
   if(sheet!=null){finishPdfExportPage(pdf,sheet,page,width,height,margin);sheet=null;}
   try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){
    if(out==null)throw new java.io.IOException("Tidak dapat membuka fail destinasi");
    pdf.writeTo(out);
   }
  }finally{if(sheet!=null)pdf.finishPage(sheet);pdf.close();}
 }
 void finishPdfExportPage(android.graphics.pdf.PdfDocument pdf,android.graphics.pdf.PdfDocument.Page sheet,int number,int width,int height,int margin){
  android.graphics.Paint footer=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  footer.setColor(0xff75877e);footer.setTextSize(9);
  sheet.getCanvas().drawText("DinNote - Emerald Pearl",margin,height-27,footer);
  String numberText=String.valueOf(number);
  sheet.getCanvas().drawText(numberText,width-margin-footer.measureText(numberText),height-27,footer);
  pdf.finishPage(sheet);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  android.net.Uri uri=data.getData();
  if(request==PDF_EXPORT_CREATE){try{writePdfExport(uri);Toast.makeText(this,"PDF berjaya disimpan",Toast.LENGTH_LONG).show();}catch(Exception e){android.util.Log.e("DinNote","PDF export failed",e);new AlertDialog.Builder(this).setTitle("Eksport PDF gagal").setMessage(String.valueOf(e.getMessage())).setPositiveButton("OK",null).show();}finally{pdfExportTitle="";pdfExportBody="";}return;} if(request==BOOK_TEXT_IMPORT){importBookText(uri);return;}
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
    if((key.startsWith("book_author_")||key.startsWith("book_edition_")||key.startsWith("book_reference_")||key.startsWith("book_tags_")||key.startsWith("named_stops_")||key.startsWith("footnotes_")||key.startsWith("quote_"))&&!(value instanceof String))throw new java.io.IOException("Metadata Maktabah tidak sah");
    if(key.equals("book_manual_order")&&!(value instanceof Boolean))throw new java.io.IOException("Susunan kitab tidak sah");
    if((key.startsWith("book_rank_")||key.startsWith("reading_filter_")||key.equals("auto_read_speed"))&&(!(value instanceof Number)||((Number)value).doubleValue()%1!=0||((Number)value).doubleValue()<0||((Number)value).doubleValue()>Integer.MAX_VALUE))throw new java.io.IOException("Tetapan Maktabah tidak sah");
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
  }catch(Exception e){
   String reason=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
   new AlertDialog.Builder(this).setTitle("Restore DinNote gagal")
    .setMessage("Punca: "+reason+"\\n\\nData sedia ada tidak diubah jika pemeriksaan gagal sebelum pemulihan. Jangan padam fail backup asal.")
    .setPositiveButton("Faham",null).show();
  }
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
   if(homeWindowWidthDp()<600)closeStudyReference();
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
 @Override protected void onRestart(){
  super.onRestart();
  // Clipboard and other temporary system activities can stop this Activity.
  // Preserve the live screen; a fresh Activity still opens Home in onCreate.
 }
 @Override protected void onStop(){
  // Checkpoint the latest editor text before flushing when Android backgrounds the app.
  if(editor!=null){
   save();
   if(!prefs.edit().commit()){
    android.util.Log.e("DinNote","Failed to flush editor data on stop");
    if(saveIndicator!=null)saveIndicator.setText("⚠ Simpan gagal");
   }
  }else if(selected!=null){
   saveReaderPosition(selected);
  }
  super.onStop();
 }
 @Override public boolean onKeyDown(int keyCode,android.view.KeyEvent event){
  if(editor==null&&selected!=null&&readerScroll!=null&&readerScroll.isAttachedToWindow()&&prefs.getBoolean("reader_clean_mode",false)&&(keyCode==KeyEvent.KEYCODE_VOLUME_UP||keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)){
   readerScroll.smoothScrollBy(0,(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN?1:-1)*Math.max(dp(80),readerScroll.getHeight()*2/3));
   return true;
  }
  return super.onKeyDown(keyCode,event);
 }
 public void onBackPressed(){if(root!=null&&"board_management".equals(root.getTag())){openSection(maktabah);return;}if(editor!=null){finishEditingSafely();}else if(selected!=null){returnFromReader();}else if(sectionOpen){home();}else super.onBackPressed();}
}
