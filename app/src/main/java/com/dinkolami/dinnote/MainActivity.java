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
  boolean pearlPalette(){return getPreferences(MODE_PRIVATE).getBoolean("pearlTurquoiseGold",true);}
  int surface(){return dark()?0xFF0B1D19:(pearlPalette()?0xFFFAFBF8:0xFFF7FAF6);}
  int foreground(){return dark()?0xFFE7F4EB:(pearlPalette()?0xFF234B49:0xFF123C31);}
  int borderColor(){return dark()?0xFF285044:(pearlPalette()?0xFFD9E6E1:0xFFD6E7DB);}
  int background = Color.rgb(247,250,246);
  void applyAppearance(){
    // Theme only the app content. Android owns the top status bar and bottom navigation bar.
    background=surface();
  }
  void showSettings(){
    flushPendingSave();
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← DinNote");back.setContentDescription("Kembali ke halaman utama");
    page.addView(back);back.setOnClickListener(v->showHome());
    TextView heading=text("⚙  Tetapan DinNote",21);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(foreground());heading.setBackground(rounded(cardSurface(),pearlPalette()?0xFFD5BD83:borderColor(),12));heading.setPadding(16,14,16,14);page.addView(heading);
    TextView subtitle=text("Penampilan • Editor • Nota • Data",13);page.addView(subtitle);
    ScrollView scroll=new ScrollView(this);scroll.setSmoothScrollingEnabled(true);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(true);scroll.setClipToPadding(false);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout options=new LinearLayout(this);options.setOrientation(1);scroll.addView(options);
    TextView displayTitle=text("PAPARAN",12);displayTitle.setTypeface(null,Typeface.BOLD);options.addView(displayTitle);
    Button appearance=button();appearance.setText("◐  Penampilan  ·  "+new String[]{"Ikut sistem","Cerah","Gelap"}[Math.max(0,Math.min(2,themeMode))]);
    options.addView(appearance);appearance.setOnClickListener(v->appearanceSettings());
    Button editorSize=button();editorSize.setText("Aa  Saiz tulisan editor  ·  "+getPreferences(MODE_PRIVATE).getInt("editorFontSize",16)+" sp");
    options.addView(editorSize);
    editorSize.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Saiz tulisan editor")
      .setSingleChoiceItems(new String[]{"Kecil (14)","Sederhana (16)","Besar (18)","Sangat besar (20)"},Math.max(0,Math.min(3,(getPreferences(MODE_PRIVATE).getInt("editorFontSize",16)-14)/2)),(dialog,which)->{
        getPreferences(MODE_PRIVATE).edit().putInt("editorFontSize",14+which*2).apply();dialog.dismiss();showSettings();
      }).setNegativeButton("Batal",null).show());
    Button editorSpacing=button();editorSpacing.setText("↕  Jarak baris editor  ·  "+(getPreferences(MODE_PRIVATE).getBoolean("editorComfortSpacing",true)?"Selesa":"Padat"));
    options.addView(editorSpacing);
    editorSpacing.setOnClickListener(v->{boolean current=getPreferences(MODE_PRIVATE).getBoolean("editorComfortSpacing",true);getPreferences(MODE_PRIVATE).edit().putBoolean("editorComfortSpacing",!current).apply();showSettings();});
    Button palette=button();palette.setText("✦  Palet Pearl • Gold • Turquoise  ·  "+(pearlPalette()?"Aktif":"Asal"));
    options.addView(palette);palette.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().putBoolean("pearlTurquoiseGold",!pearlPalette()).apply();applyAppearance();showSettings();});
    Button smoothHeader=button();smoothHeader.setText("◈  Header lembut ketika skrol  ·  "+(getPreferences(MODE_PRIVATE).getBoolean("smoothHeader",true)?"Aktif":"Mati"));
    options.addView(smoothHeader);smoothHeader.setOnClickListener(v->{boolean enabled=getPreferences(MODE_PRIVATE).getBoolean("smoothHeader",true);getPreferences(MODE_PRIVATE).edit().putBoolean("smoothHeader",!enabled).apply();showSettings();});
    Button reduceMotion=button();reduceMotion.setText("◌  Kurangkan animasi  ·  "+(getPreferences(MODE_PRIVATE).getBoolean("reduceMotion",false)?"Aktif":"Mati"));
    options.addView(reduceMotion);reduceMotion.setOnClickListener(v->{boolean enabled=getPreferences(MODE_PRIVATE).getBoolean("reduceMotion",false);getPreferences(MODE_PRIVATE).edit().putBoolean("reduceMotion",!enabled).apply();showSettings();});
    Button sort=button();sort.setText("↕  Susunan nota  ·  "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[Math.max(0,Math.min(noteSort,2))]);
    options.addView(sort);sort.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Susunan nota")
      .setSingleChoiceItems(new String[]{"Terkini","Terlama","Tajuk A-Z"},Math.max(0,Math.min(2,noteSort)),(dialog,which)->{
        noteSort=which;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",which).apply();
        dialog.dismiss();showSettings();
      }).setNegativeButton("Batal",null).show());
    TextView homeTitle=text("HALAMAN UTAMA",12);homeTitle.setTypeface(null,Typeface.BOLD);options.addView(homeTitle);
    boolean pinnedCollapsed=getPreferences(MODE_PRIVATE).getBoolean("pinnedCollapsed",false);
    Button pinnedVisibility=button();
    pinnedVisibility.setText("📌  Nota dipin  ·  "+(pinnedCollapsed?"Disembunyikan":"Dipaparkan"));
    pinnedVisibility.setContentDescription("Tukar paparan nota dipin pada halaman utama");
    options.addView(pinnedVisibility);
    pinnedVisibility.setOnClickListener(v->{
      boolean current=getPreferences(MODE_PRIVATE).getBoolean("pinnedCollapsed",false);
      getPreferences(MODE_PRIVATE).edit().putBoolean("pinnedCollapsed",!current).apply();
      showSettings();
    });
    Button resetSearch=button();resetSearch.setText("⌕  Kosongkan carian dan penapis Nota");
    resetSearch.setContentDescription("Kosongkan carian dan tunjuk semua kategori nota");
    options.addView(resetSearch);
    resetSearch.setOnClickListener(v->{
      noteFilter="";selectedNoteCategory=-1;
      Toast.makeText(this,"Carian dan penapis dikosongkan",Toast.LENGTH_SHORT).show();
      showSettings();
    });
    Button openNotes=button();openNotes.setText("📝  Urus kategori Nota");
    options.addView(openNotes);openNotes.setOnClickListener(v->showNoteCategories());
    Button openLibrary=button();openLibrary.setText("📚  Urus kategori Maktabah");
    options.addView(openLibrary);openLibrary.setOnClickListener(v->showLibrary());
    TextView accessibilityTitle=text("KEBOLEHCAPAIAN & MAKLUMAT",12);
    accessibilityTitle.setTypeface(null,Typeface.BOLD);options.addView(accessibilityTitle);
    Button fontGuide=button();fontGuide.setText("Aa  Saiz teks  ·  Sederhana");
    fontGuide.setContentDescription("Maklumat saiz teks lalai DinNote");options.addView(fontGuide);
    fontGuide.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Saiz teks")
      .setMessage("DinNote menggunakan saiz teks sederhana untuk pembacaan yang selesa. Tetapan saiz teks telefon boleh membantu mengubah skala paparan.")
      .setPositiveButton("OK",null).show());
    Button languageGuide=button();languageGuide.setText("ع  Bahasa  ·  Arab & Rumi");
    languageGuide.setContentDescription("Maklumat sokongan tulisan Arab dan Rumi");options.addView(languageGuide);
    languageGuide.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Bahasa tulisan")
      .setMessage("Nota dan Maktabah menyokong teks Arab dan Rumi dalam kandungan yang sama.")
      .setPositiveButton("OK",null).show());
    Button storageInfo=button();storageInfo.setText("▣  Lokasi data  ·  Dalam telefon");
    storageInfo.setContentDescription("Maklumat lokasi penyimpanan data");options.addView(storageInfo);
    storageInfo.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Penyimpanan DinNote")
      .setMessage("Nota dan kitab disimpan dalam pangkalan data aplikasi pada telefon ini. Memasang APK pada telefon lain tidak memindahkan data secara automatik; gunakan Backup dan Restore.")
      .setPositiveButton("OK",null).show());
    Button offlineInfo=button();offlineInfo.setText("◌  Mod luar talian");
    offlineInfo.setContentDescription("Maklumat penggunaan tanpa internet");options.addView(offlineInfo);
    offlineInfo.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Mod luar talian")
      .setMessage("Nota dan kandungan Maktabah yang sudah disimpan boleh digunakan tanpa sambungan internet.")
      .setPositiveButton("OK",null).show());
    Button privacyInfo=button();privacyInfo.setText("♙  Privasi nota");
    privacyInfo.setContentDescription("Maklumat privasi data nota");options.addView(privacyInfo);
    privacyInfo.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Privasi")
      .setMessage("Data disimpan secara setempat dalam ruang aplikasi. Simpan salinan backup di lokasi yang anda percayai.")
      .setPositiveButton("OK",null).show());
    Button backupGuide=button();backupGuide.setText("?  Panduan backup");
    backupGuide.setContentDescription("Panduan membuat salinan data");options.addView(backupGuide);
    backupGuide.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Panduan Backup")
      .setMessage("1. Tekan Backup ke DinPhone.\n2. Pilih lokasi penyimpanan fail.\n3. Simpan fail .db dengan selamat.\n4. Gunakan Restore daripada fail untuk memulihkan data.")
      .setPositiveButton("OK",null).show());
    Button restoreGuide=button();restoreGuide.setText("?  Panduan restore");
    restoreGuide.setContentDescription("Panduan memulihkan data");options.addView(restoreGuide);
    restoreGuide.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Panduan Restore")
      .setMessage("Restore menggantikan semua nota, kategori dan kitab semasa. Buat backup terkini dahulu, kemudian pilih fail backup DinNote.")
      .setPositiveButton("OK",null).show());
    Button categoryGuide=button();categoryGuide.setText("▤  Panduan kategori");
    categoryGuide.setContentDescription("Panduan kategori Nota dan Maktabah");options.addView(categoryGuide);
    categoryGuide.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Kategori")
      .setMessage("Kategori Nota diurus dalam menu Nota. Kategori kitab diurus dalam menu Maktabah. Kedua-duanya berasingan.")
      .setPositiveButton("OK",null).show());
    Button pinnedGuide=button();pinnedGuide.setText("📌  Panduan nota dipin");
    pinnedGuide.setContentDescription("Panduan memaparkan nota dipin");options.addView(pinnedGuide);
    pinnedGuide.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Nota dipin")
      .setMessage("Pin nota penting untuk memaparkannya di halaman utama. Gunakan pilihan Nota dipin dalam Tetapan untuk menyembunyikan atau memaparkan senarai.")
      .setPositiveButton("OK",null).show());
    Button systemBarInfo=button();systemBarInfo.setText("▣  Bar sistem Android");
    systemBarInfo.setContentDescription("Maklumat bar status dan navigasi sistem");options.addView(systemBarInfo);
    systemBarInfo.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Bar sistem Android")
      .setMessage("DinNote tidak mengubah status bar jam di atas atau navigation bar Android di bawah.")
      .setPositiveButton("OK",null).show());
    TextView quickTitle=text("PINTASAN & BANTUAN",12);quickTitle.setTypeface(null,Typeface.BOLD);options.addView(quickTitle);
    Button quick1=button();quick1.setText("1. Pintasan Nota");quick1.setContentDescription("Buka semua kategori nota peribadi");options.addView(quick1);
    quick1.setOnClickListener(v->showNoteCategories());
    Button quick2=button();quick2.setText("2. Pintasan Maktabah");quick2.setContentDescription("Buka koleksi kitab dan kategori ilmu Islam");options.addView(quick2);
    quick2.setOnClickListener(v->showLibrary());
    Button quick3=button();quick3.setText("3. Carian Nota");quick3.setContentDescription("Kosongkan carian untuk melihat nota tanpa kata kunci");options.addView(quick3);
    quick3.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Carian Nota").setMessage("Kosongkan carian untuk melihat nota tanpa kata kunci").setPositiveButton("OK",null).show());
    Button quick4=button();quick4.setText("4. Susunan nota");quick4.setContentDescription("Pilih nota terkini, terlama atau tajuk A-Z");options.addView(quick4);
    quick4.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Susunan nota").setMessage("Pilih nota terkini, terlama atau tajuk A-Z").setPositiveButton("OK",null).show());
    Button quick5=button();quick5.setText("5. Nota dipin");quick5.setContentDescription("Paparkan nota penting pada halaman utama");options.addView(quick5);
    quick5.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Nota dipin").setMessage("Paparkan nota penting pada halaman utama").setPositiveButton("OK",null).show());
    Button quick6=button();quick6.setText("6. Tema aplikasi");quick6.setContentDescription("Gunakan mod sistem, cerah atau gelap");options.addView(quick6);
    quick6.setOnClickListener(v->appearanceSettings());
    Button quick7=button();quick7.setText("7. Backup berkala");quick7.setContentDescription("Simpan salinan fail pangkalan data secara berkala");options.addView(quick7);
    quick7.setOnClickListener(v->exportBackup());
    Button quick8=button();quick8.setText("8. Pemulihan data");quick8.setContentDescription("Pilih fail backup untuk memulihkan nota dan kitab");options.addView(quick8);
    quick8.setOnClickListener(v->importBackup());
    Button quick9=button();quick9.setText("9. Penyimpanan setempat");quick9.setContentDescription("Data pengguna kekal dalam ruang storan aplikasi pada telefon");options.addView(quick9);
    quick9.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Penyimpanan setempat").setMessage("Data pengguna kekal dalam ruang storan aplikasi pada telefon").setPositiveButton("OK",null).show());
    Button quick10=button();quick10.setText("10. Navigasi aplikasi");quick10.setContentDescription("Gunakan butang kembali dalam DinNote tanpa mengubah bar Android");options.addView(quick10);
    quick10.setOnClickListener(v->showHome());
    TextView toolsTitle=text("ALAT NOTA",12);toolsTitle.setTypeface(null,Typeface.BOLD);options.addView(toolsTitle);
    Button newest=button();newest.setText("↓  Susun nota terkini");options.addView(newest);
    newest.setOnClickListener(v->{noteSort=0;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",0).apply();showSettings();});
    Button oldest=button();oldest.setText("↑  Susun nota terlama");options.addView(oldest);
    oldest.setOnClickListener(v->{noteSort=1;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",1).apply();showSettings();});
    Button alphabet=button();alphabet.setText("A–Z  Susun nota mengikut tajuk");options.addView(alphabet);
    alphabet.setOnClickListener(v->{noteSort=2;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",2).apply();showSettings();});
    Button systemTheme=button();systemTheme.setText("◐  Tema ikut sistem");options.addView(systemTheme);
    systemTheme.setOnClickListener(v->{themeMode=0;getPreferences(MODE_PRIVATE).edit().putInt("appearance",0).apply();applyAppearance();showSettings();});
    Button lightTheme=button();lightTheme.setText("☀  Tema cerah");options.addView(lightTheme);
    lightTheme.setOnClickListener(v->{themeMode=1;getPreferences(MODE_PRIVATE).edit().putInt("appearance",1).apply();applyAppearance();showSettings();});
    Button darkTheme=button();darkTheme.setText("☾  Tema gelap");options.addView(darkTheme);
    darkTheme.setOnClickListener(v->{themeMode=2;getPreferences(MODE_PRIVATE).edit().putInt("appearance",2).apply();applyAppearance();showSettings();});
    Button showPins=button();showPins.setText("📌  Papar nota dipin");options.addView(showPins);
    showPins.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().putBoolean("pinnedCollapsed",false).apply();showSettings();});
    Button hidePins=button();hidePins.setText("📌  Sembunyikan nota dipin");options.addView(hidePins);
    hidePins.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().putBoolean("pinnedCollapsed",true).apply();showSettings();});
    Button clearSearch=button();clearSearch.setText("⌕  Kosongkan kata carian");options.addView(clearSearch);
    clearSearch.setOnClickListener(v->{noteFilter="";showSettings();});
    Button allCategories=button();allCategories.setText("▤  Pilih semua kategori Nota");options.addView(allCategories);
    allCategories.setOnClickListener(v->{selectedNoteCategory=-1;showSettings();});
    TextView statisticsTitle=text("STATISTIK DATA",12);statisticsTitle.setTypeface(null,Typeface.BOLD);options.addView(statisticsTitle);
    try(Cursor stat0=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes",null)){
      if(stat0.moveToFirst()){
        TextView statLabel0=text("Jumlah Nota: "+stat0.getLong(0),14);
        statLabel0.setContentDescription("Jumlah Nota, "+stat0.getLong(0));
        options.addView(statLabel0);
      }
    }catch(Exception statError0){android.util.Log.w("DinNote","Unable to read statistic 1",statError0);}
    try(Cursor stat1=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes WHERE pinned=1",null)){
      if(stat1.moveToFirst()){
        TextView statLabel1=text("Nota dipin: "+stat1.getLong(0),14);
        statLabel1.setContentDescription("Nota dipin, "+stat1.getLong(0));
        options.addView(statLabel1);
      }
    }catch(Exception statError1){android.util.Log.w("DinNote","Unable to read statistic 2",statError1);}
    try(Cursor stat2=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes WHERE category_id IS NULL OR category_id=0",null)){
      if(stat2.moveToFirst()){
        TextView statLabel2=text("Nota tanpa kategori: "+stat2.getLong(0),14);
        statLabel2.setContentDescription("Nota tanpa kategori, "+stat2.getLong(0));
        options.addView(statLabel2);
      }
    }catch(Exception statError2){android.util.Log.w("DinNote","Unable to read statistic 3",statError2);}
    try(Cursor stat3=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM note_categories",null)){
      if(stat3.moveToFirst()){
        TextView statLabel3=text("Jumlah kategori Nota: "+stat3.getLong(0),14);
        statLabel3.setContentDescription("Jumlah kategori Nota, "+stat3.getLong(0));
        options.addView(statLabel3);
      }
    }catch(Exception statError3){android.util.Log.w("DinNote","Unable to read statistic 4",statError3);}
    try(Cursor stat4=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM categories",null)){
      if(stat4.moveToFirst()){
        TextView statLabel4=text("Jumlah kategori Maktabah: "+stat4.getLong(0),14);
        statLabel4.setContentDescription("Jumlah kategori Maktabah, "+stat4.getLong(0));
        options.addView(statLabel4);
      }
    }catch(Exception statError4){android.util.Log.w("DinNote","Unable to read statistic 5",statError4);}
    try(Cursor stat5=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM books",null)){
      if(stat5.moveToFirst()){
        TextView statLabel5=text("Jumlah kitab: "+stat5.getLong(0),14);
        statLabel5.setContentDescription("Jumlah kitab, "+stat5.getLong(0));
        options.addView(statLabel5);
      }
    }catch(Exception statError5){android.util.Log.w("DinNote","Unable to read statistic 6",statError5);}
    try(Cursor stat6=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes WHERE TRIM(title)<>''",null)){
      if(stat6.moveToFirst()){
        TextView statLabel6=text("Nota dengan tajuk: "+stat6.getLong(0),14);
        statLabel6.setContentDescription("Nota dengan tajuk, "+stat6.getLong(0));
        options.addView(statLabel6);
      }
    }catch(Exception statError6){android.util.Log.w("DinNote","Unable to read statistic 7",statError6);}
    try(Cursor stat7=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes WHERE TRIM(body)<>''",null)){
      if(stat7.moveToFirst()){
        TextView statLabel7=text("Nota dengan kandungan: "+stat7.getLong(0),14);
        statLabel7.setContentDescription("Nota dengan kandungan, "+stat7.getLong(0));
        options.addView(statLabel7);
      }
    }catch(Exception statError7){android.util.Log.w("DinNote","Unable to read statistic 8",statError7);}
    try(Cursor stat8=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes WHERE pinned=0 OR pinned IS NULL",null)){
      if(stat8.moveToFirst()){
        TextView statLabel8=text("Nota belum dipin: "+stat8.getLong(0),14);
        statLabel8.setContentDescription("Nota belum dipin, "+stat8.getLong(0));
        options.addView(statLabel8);
      }
    }catch(Exception statError8){android.util.Log.w("DinNote","Unable to read statistic 9",statError8);}
    try(Cursor stat9=db.getReadableDatabase().rawQuery("SELECT (SELECT COUNT(*) FROM notes)+(SELECT COUNT(*) FROM books)",null)){
      if(stat9.moveToFirst()){
        TextView statLabel9=text("Jumlah kandungan tersimpan: "+stat9.getLong(0),14);
        statLabel9.setContentDescription("Jumlah kandungan tersimpan, "+stat9.getLong(0));
        options.addView(statLabel9);
      }
    }catch(Exception statError9){android.util.Log.w("DinNote","Unable to read statistic 10",statError9);}
    Button advancedStats=button();advancedStats.setText("▦  25 statistik lanjutan");
    advancedStats.setContentDescription("Lihat dua puluh lima statistik Nota dan Maktabah");
    options.addView(advancedStats);advancedStats.setOnClickListener(v->showAdvancedStatistics());
    Button contentInsights=button();contentInsights.setText("▦  27 analisis kandungan");options.addView(contentInsights);
    contentInsights.setOnClickListener(v->showContentInsights());
    Button dataQuality=button();dataQuality.setText("▦  23 semakan kualiti data");options.addView(dataQuality);
    dataQuality.setOnClickListener(v->showDataQualityInsights());
    Button writingInsights=button();writingInsights.setText("▦  23 analisis penulisan");options.addView(writingInsights);
    writingInsights.setOnClickListener(v->showWritingInsights());
    TextView dataTitle=text("DATA & BACKUP",12);dataTitle.setTypeface(null,Typeface.BOLD);options.addView(dataTitle);
    Button backup=button();backup.setText("⇧  Backup ke DinPhone");options.addView(backup);
    backup.setOnClickListener(v->exportBackup());
    Button restore=button();restore.setText("⇩  Restore daripada fail");options.addView(restore);
    restore.setOnClickListener(v->importBackup());
    TextView warning=text("Simpan fail backup di tempat selamat. Restore menggantikan data semasa.",12);
    options.addView(warning);
    TextView aboutTitle=text("TENTANG",12);aboutTitle.setTypeface(null,Typeface.BOLD);options.addView(aboutTitle);
    Button about=button();about.setText("ⓘ  Mengenai DinNote");options.addView(about);
    about.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("DinNote")
      .setMessage("Aplikasi nota dan maktabah luar talian. Kategori Nota dan Maktabah diurus berasingan. Data disimpan pada telefon; gunakan Backup untuk memindahkannya.")
      .setPositiveButton("OK",null).show());
  }
  void showAdvancedStatistics(){
    flushPendingSave();
    String[] labels=new String[]{
      "Nota hari ini",
      "Nota 7 hari",
      "Nota 30 hari",
      "Kitab hari ini",
      "Kitab 7 hari",
      "Kitab 30 hari",
      "Kitab dipin",
      "Kitab belum dipin",
      "Kitab tanpa tajuk",
      "Kitab tanpa isi",
      "Kitab bertajuk",
      "Kitab berisi",
      "Nota tanpa tajuk",
      "Nota tanpa isi",
      "Nota kategori peribadi",
      "Nota kategori tidak ditemui",
      "Kitab kategori tidak ditemui",
      "Kategori Nota kosong",
      "Kategori Maktabah kosong",
      "Kategori Nota digunakan",
      "Kategori Maktabah digunakan",
      "Jumlah aksara nota",
      "Jumlah aksara kitab",
      "Nota panjang (1000+ aksara)",
      "Kitab panjang (1000+ aksara)"
    };
    String[] queries=new String[]{
      "SELECT COUNT(*) FROM notes WHERE updated>=?",
      "SELECT COUNT(*) FROM notes WHERE updated>=?",
      "SELECT COUNT(*) FROM notes WHERE updated>=?",
      "SELECT COUNT(*) FROM books WHERE updated>=?",
      "SELECT COUNT(*) FROM books WHERE updated>=?",
      "SELECT COUNT(*) FROM books WHERE updated>=?",
      "SELECT COUNT(*) FROM books WHERE pinned=1",
      "SELECT COUNT(*) FROM books WHERE pinned=0",
      "SELECT COUNT(*) FROM books WHERE TRIM(title)=''",
      "SELECT COUNT(*) FROM books WHERE TRIM(body)=''",
      "SELECT COUNT(*) FROM books WHERE TRIM(title)<>''",
      "SELECT COUNT(*) FROM books WHERE TRIM(body)<>''",
      "SELECT COUNT(*) FROM notes WHERE TRIM(title)=''",
      "SELECT COUNT(*) FROM notes WHERE TRIM(body)=''",
      "SELECT COUNT(*) FROM notes WHERE category_id IN (SELECT id FROM note_categories)",
      "SELECT COUNT(*) FROM notes WHERE category_id>0 AND category_id NOT IN (SELECT id FROM note_categories)",
      "SELECT COUNT(*) FROM books WHERE category_id NOT IN (SELECT id FROM categories)",
      "SELECT COUNT(*) FROM note_categories c WHERE NOT EXISTS (SELECT 1 FROM notes n WHERE n.category_id=c.id)",
      "SELECT COUNT(*) FROM categories c WHERE NOT EXISTS (SELECT 1 FROM books b WHERE b.category_id=c.id)",
      "SELECT COUNT(*) FROM note_categories c WHERE EXISTS (SELECT 1 FROM notes n WHERE n.category_id=c.id)",
      "SELECT COUNT(*) FROM categories c WHERE EXISTS (SELECT 1 FROM books b WHERE b.category_id=c.id)",
      "SELECT COALESCE(SUM(LENGTH(title)+LENGTH(body)),0) FROM notes",
      "SELECT COALESCE(SUM(LENGTH(title)+LENGTH(body)),0) FROM books",
      "SELECT COUNT(*) FROM notes WHERE LENGTH(body)>=1000",
      "SELECT COUNT(*) FROM books WHERE LENGTH(body)>=1000"
    };
    String[] periods=new String[]{"day",
      "week",
      "month",
      "day",
      "week",
      "month",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      ""};
    long now=System.currentTimeMillis();
    java.util.Calendar midnight=java.util.Calendar.getInstance();
    midnight.set(java.util.Calendar.HOUR_OF_DAY,0);
    midnight.set(java.util.Calendar.MINUTE,0);
    midnight.set(java.util.Calendar.SECOND,0);
    midnight.set(java.util.Calendar.MILLISECOND,0);
    StringBuilder report=new StringBuilder();
    for(int i=0;i<labels.length;i++){
      String[] args=null;
      if(!periods[i].isEmpty()){
        long since=periods[i].equals("day")?midnight.getTimeInMillis():
          now-(periods[i].equals("week")?7L:30L)*24L*60L*60L*1000L;
        args=new String[]{Long.toString(since)};
      }
      try(Cursor result=db.getReadableDatabase().rawQuery(queries[i],args)){
        report.append(i+1).append(". ").append(labels[i]).append(": ");
        report.append(result.moveToFirst()?result.getLong(0):0).append("\n");
      }catch(Exception e){
        android.util.Log.w("DinNote","Advanced statistic "+(i+1)+" unavailable",e);
        report.append(i+1).append(". ").append(labels[i]).append(": Tidak tersedia\n");
      }
    }
    new AlertDialog.Builder(this).setTitle("25 Statistik Lanjutan")
      .setMessage(report.toString()).setPositiveButton("Tutup",null).show();
  }
  void showContentInsights(){
    flushPendingSave();
    String[] labels=new String[]{"Nota 24 jam","Nota 14 hari","Nota 90 hari","Kitab 24 jam","Kitab 14 hari","Kitab 90 hari","Nota sangat panjang (5000+)","Kitab sangat panjang (5000+)","Nota ringkas (1-99 aksara)","Kitab ringkas (1-99 aksara)","Nota sederhana (100-999)","Kitab sederhana (100-999)","Nota 1000-4999 aksara","Kitab 1000-4999 aksara","Nota tajuk panjang (50+)","Kitab tajuk panjang (50+)","Nota dengan teks Arab","Kitab dengan teks Arab","Nota mempunyai baris baharu","Kitab mempunyai baris baharu","Nota tajuk dan isi kosong","Kitab tajuk dan isi kosong","Nota mempunyai tajuk dan isi","Kitab mempunyai tajuk dan isi","Purata aksara isi nota","Purata aksara isi kitab","Jumlah kategori keseluruhan"};
    String[] queries=new String[]{"SELECT COUNT(*) FROM notes WHERE updated>=?","SELECT COUNT(*) FROM notes WHERE updated>=?","SELECT COUNT(*) FROM notes WHERE updated>=?","SELECT COUNT(*) FROM books WHERE updated>=?","SELECT COUNT(*) FROM books WHERE updated>=?","SELECT COUNT(*) FROM books WHERE updated>=?","SELECT COUNT(*) FROM notes WHERE LENGTH(body)>=5000","SELECT COUNT(*) FROM books WHERE LENGTH(body)>=5000","SELECT COUNT(*) FROM notes WHERE LENGTH(body) BETWEEN 1 AND 99","SELECT COUNT(*) FROM books WHERE LENGTH(body) BETWEEN 1 AND 99","SELECT COUNT(*) FROM notes WHERE LENGTH(body) BETWEEN 100 AND 999","SELECT COUNT(*) FROM books WHERE LENGTH(body) BETWEEN 100 AND 999","SELECT COUNT(*) FROM notes WHERE LENGTH(body) BETWEEN 1000 AND 4999","SELECT COUNT(*) FROM books WHERE LENGTH(body) BETWEEN 1000 AND 4999","SELECT COUNT(*) FROM notes WHERE LENGTH(title)>=50","SELECT COUNT(*) FROM books WHERE LENGTH(title)>=50","SELECT COUNT(*) FROM notes WHERE body GLOB '*[ء-ي]*'","SELECT COUNT(*) FROM books WHERE body GLOB '*[ء-ي]*'","SELECT COUNT(*) FROM notes WHERE INSTR(body,CHAR(10))>0","SELECT COUNT(*) FROM books WHERE INSTR(body,CHAR(10))>0","SELECT COUNT(*) FROM notes WHERE TRIM(title)='' AND TRIM(body)=''","SELECT COUNT(*) FROM books WHERE TRIM(title)='' AND TRIM(body)=''","SELECT COUNT(*) FROM notes WHERE TRIM(title)<>'' AND TRIM(body)<>''","SELECT COUNT(*) FROM books WHERE TRIM(title)<>'' AND TRIM(body)<>''","SELECT COALESCE(AVG(LENGTH(body)),0) FROM notes","SELECT COALESCE(AVG(LENGTH(body)),0) FROM books","SELECT (SELECT COUNT(*) FROM note_categories)+(SELECT COUNT(*) FROM categories)"};
    String[] periods=new String[]{"24h","14d","90d","24h","14d","90d","","","","","","","","","","","","","","","","","","","","",""};
    StringBuilder report=new StringBuilder();
    long now=System.currentTimeMillis();
    for(int i=0;i<labels.length;i++){
      String[] args=null;
      if(!periods[i].isEmpty()){
        long days=periods[i].equals("24h")?1L:periods[i].equals("14d")?14L:90L;
        args=new String[]{Long.toString(now-days*86400000L)};
      }
      try(Cursor cursor=db.getReadableDatabase().rawQuery(queries[i],args)){
        report.append(i+1).append(". ").append(labels[i]).append(": ");
        report.append(cursor.moveToFirst()?cursor.getLong(0):0).append("\n");
      }catch(Exception e){
        android.util.Log.w("DinNote","Content insight "+(i+1)+" unavailable",e);
        report.append(i+1).append(". ").append(labels[i]).append(": Tidak tersedia\n");
      }
    }
    new AlertDialog.Builder(this).setTitle("27 Analisis Kandungan")
      .setMessage(report.toString()).setPositiveButton("Tutup",null).show();
  }
  void showDataQualityInsights(){
    flushPendingSave();
    String[] labels=new String[]{"Nota dengan warna lalai","Nota dengan warna khusus","Nota terkini mempunyai isi","Kitab terkini mempunyai isi","Nota dengan ruang di awal tajuk","Kitab dengan ruang di awal tajuk","Nota dengan ruang di hujung tajuk","Kitab dengan ruang di hujung tajuk","Nota tanpa perubahan masa","Kitab tanpa perubahan masa","Nota tajuk 1-20 aksara","Kitab tajuk 1-20 aksara","Nota tajuk 21-49 aksara","Kitab tajuk 21-49 aksara","Nota tajuk 50-99 aksara","Kitab tajuk 50-99 aksara","Nota tajuk 100+ aksara","Kitab tajuk 100+ aksara","Kategori Nota nama kosong","Kategori Maktabah nama kosong","Kategori Nota nama panjang","Kategori Maktabah nama panjang","Jumlah aksara nama kategori"};
    String[] queries=new String[]{"SELECT COUNT(*) FROM notes WHERE color=-1","SELECT COUNT(*) FROM notes WHERE color<>-1","SELECT COUNT(*) FROM notes WHERE updated>0 AND LENGTH(TRIM(body))>0","SELECT COUNT(*) FROM books WHERE updated>0 AND LENGTH(TRIM(body))>0","SELECT COUNT(*) FROM notes WHERE LENGTH(title)>LENGTH(LTRIM(title))","SELECT COUNT(*) FROM books WHERE LENGTH(title)>LENGTH(LTRIM(title))","SELECT COUNT(*) FROM notes WHERE LENGTH(title)>LENGTH(RTRIM(title))","SELECT COUNT(*) FROM books WHERE LENGTH(title)>LENGTH(RTRIM(title))","SELECT COUNT(*) FROM notes WHERE updated<=0","SELECT COUNT(*) FROM books WHERE updated<=0","SELECT COUNT(*) FROM notes WHERE LENGTH(title) BETWEEN 1 AND 20","SELECT COUNT(*) FROM books WHERE LENGTH(title) BETWEEN 1 AND 20","SELECT COUNT(*) FROM notes WHERE LENGTH(title) BETWEEN 21 AND 49","SELECT COUNT(*) FROM books WHERE LENGTH(title) BETWEEN 21 AND 49","SELECT COUNT(*) FROM notes WHERE LENGTH(title) BETWEEN 50 AND 99","SELECT COUNT(*) FROM books WHERE LENGTH(title) BETWEEN 50 AND 99","SELECT COUNT(*) FROM notes WHERE LENGTH(title)>=100","SELECT COUNT(*) FROM books WHERE LENGTH(title)>=100","SELECT COUNT(*) FROM note_categories WHERE LENGTH(TRIM(name))=0","SELECT COUNT(*) FROM categories WHERE LENGTH(TRIM(name))=0","SELECT COUNT(*) FROM note_categories WHERE LENGTH(name)>=40","SELECT COUNT(*) FROM categories WHERE LENGTH(name)>=40","SELECT (SELECT COALESCE(SUM(LENGTH(name)),0) FROM note_categories)+(SELECT COALESCE(SUM(LENGTH(name)),0) FROM categories)"};
    StringBuilder report=new StringBuilder();
    for(int i=0;i<labels.length;i++){
      try(Cursor cursor=db.getReadableDatabase().rawQuery(queries[i],null)){
        report.append(i+1).append(". ").append(labels[i]).append(": ");
        report.append(cursor.moveToFirst()?cursor.getLong(0):0).append("\n");
      }catch(Exception e){
        android.util.Log.w("DinNote","Data quality insight "+(i+1)+" unavailable",e);
        report.append(i+1).append(". ").append(labels[i]).append(": Tidak tersedia\n");
      }
    }
    new AlertDialog.Builder(this).setTitle("23 Semakan Data")
      .setMessage(report.toString()).setPositiveButton("Tutup",null).show();
  }
  void showWritingInsights(){
    flushPendingSave();
    String[] labels=new String[]{"Nota mengandungi angka","Kitab mengandungi angka","Nota mempunyai tanda soal","Kitab mempunyai tanda soal","Nota mempunyai titik bertindih","Kitab mempunyai titik bertindih","Nota mempunyai senarai berbulet","Kitab mempunyai senarai berbulet","Nota mempunyai kurungan","Kitab mempunyai kurungan","Nota mempunyai pautan web","Kitab mempunyai pautan web","Nota mempunyai e-mel","Kitab mempunyai e-mel","Nota mempunyai petikan","Kitab mempunyai petikan","Nota mempunyai tanda seru","Kitab mempunyai tanda seru","Nota mempunyai pemisah garis","Kitab mempunyai pemisah garis","Nota mempunyai tab","Kitab mempunyai tab","Jumlah nota dan kitab dipin"};
    String[] queries=new String[]{"SELECT COUNT(*) FROM notes WHERE body GLOB '*[0-9]*'","SELECT COUNT(*) FROM books WHERE body GLOB '*[0-9]*'","SELECT COUNT(*) FROM notes WHERE INSTR(body,'?')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'?')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,':')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,':')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'•')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'•')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'(')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'(')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'http://')>0 OR INSTR(body,'https://')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'http://')>0 OR INSTR(body,'https://')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'@')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'@')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'\"')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'\"')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'!')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'!')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,'---')>0","SELECT COUNT(*) FROM books WHERE INSTR(body,'---')>0","SELECT COUNT(*) FROM notes WHERE INSTR(body,CHAR(9))>0","SELECT COUNT(*) FROM books WHERE INSTR(body,CHAR(9))>0","SELECT (SELECT COUNT(*) FROM notes WHERE pinned=1)+(SELECT COUNT(*) FROM books WHERE pinned=1)"};
    StringBuilder report=new StringBuilder();
    for(int i=0;i<labels.length;i++){
      try(Cursor cursor=db.getReadableDatabase().rawQuery(queries[i],null)){
        report.append(i+1).append(". ").append(labels[i]).append(": ");
        report.append(cursor.moveToFirst()?cursor.getLong(0):0).append("\n");
      }catch(Exception e){
        android.util.Log.w("DinNote","Writing insight "+(i+1)+" unavailable",e);
        report.append(i+1).append(". ").append(labels[i]).append(": Tidak tersedia\n");
      }
    }
    new AlertDialog.Builder(this).setTitle("23 Analisis Penulisan")
      .setMessage(report.toString()).setPositiveButton("Tutup",null).show();
  }
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
  int accent(){return dark()?0xFF68C7A1:(pearlPalette()?0xFF008F8B:0xFF087C59);}
  android.graphics.drawable.GradientDrawable rounded(int color,int stroke,int radius){
    android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
    d.setColor(color);d.setCornerRadius(radius);d.setStroke(1,stroke);return d;
  }
  Button button(){
    Button b=new Button(this);b.setAllCaps(false);b.setTextSize(14);b.setLetterSpacing(0);
    b.setMinHeight(48);b.setTextColor(foreground());b.setBackground(rounded(cardSurface(),borderColor(),16));
    b.setPadding(16,12,16,12);
    b.setStateListAnimator(null);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,5,0,5);b.setLayoutParams(p);
    return b;
  }
  EditText field(){
    EditText e=new EditText(this);e.setTextColor(foreground());
    e.setHintTextColor(dark()?0xFF8B949E:0xFF656D76);
    e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);e.setTextSize(15);e.setPadding(16,12,16,12);
    e.setBackground(rounded(cardSurface(),borderColor(),12));return e;
  }
  TextView text(String value,int size) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(foreground());t.setPadding(10,9,10,9);return t; }
  // Keep editor actions visible above Samsung Keyboard without altering system bars.
  // Restrained, consistent editor styling shared by Nota and Maktabah.
  void styleEditorPage(LinearLayout page,EditText title,EditText body,TextView status,Button back){
    page.setPadding(18,12,18,14);
    title.setTextSize(20);
    title.setLetterSpacing(0.012f);
    title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    title.setMinHeight(56);
    title.setPadding(18,12,18,12);
    title.setBackground(rounded(cardSurface(),pearlPalette()?0xFFD5BD83:borderColor(),12));
    body.setPadding(20,22,20,28);
    body.setIncludeFontPadding(true);
    body.setTextSize(getPreferences(MODE_PRIVATE).getInt("editorFontSize",16));
    body.setLineSpacing(getPreferences(MODE_PRIVATE).getBoolean("editorComfortSpacing",true)?8f:3f,getPreferences(MODE_PRIVATE).getBoolean("editorComfortSpacing",true)?1.16f:1.04f);
    body.setTextColor(foreground());
    body.setHintTextColor(dark()?0xFF9AB6A7:0xFF72877B);
    body.setBackground(rounded(cardSurface(),pearlPalette()?0xFFD5BD83:borderColor(),12));
    body.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
    body.setVerticalScrollBarEnabled(true);
    body.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
    body.setFadingEdgeLength(12);
    body.setScrollBarFadeDuration(380);body.setSmoothScrollingEnabled(true);
    body.setScrollbarFadingEnabled(true);
    body.setVerticalFadingEdgeEnabled(false);
    body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    status.setTextColor(dark()?0xFF8B949E:0xFF57606A);
    status.setTextSize(12);
    status.setGravity(Gravity.END);
    status.setPadding(0,4,2,8);
    status.setLetterSpacing(0.02f);
    back.setTextColor(foreground());
    back.setBackground(rounded(cardSurface(),borderColor(),10));
    back.setElevation(2f*getResources().getDisplayMetrics().density);
    back.setMinHeight(48);
    back.setContentDescription("Kembali ke senarai; perubahan nota disimpan secara automatik");
  }
  void styleEditorTool(Button control){
    control.setMinHeight(48);
    control.setTextSize(14);
    control.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    control.setPadding(6,10,6,10);
    control.setTextColor(foreground());
    control.setBackground(rounded(cardSurface(),borderColor(),10));
  }
  // Android resizes the editor when Samsung Keyboard appears; system bars stay native.
  void enableKeyboardAwareEditor(LinearLayout page) {
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    // Keep the editor inside the resized application viewport when the IME opens.
    page.setFocusableInTouchMode(true);
    page.setClipToPadding(false);
    page.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    page.setFocusable(false);
    // Let Android retain ownership of clock/status and gesture navigation insets.
    page.setFitsSystemWindows(false);
  }
  LinearLayout column() { LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(18,16,18,16);l.setBackgroundColor(surface());return l; }
  // Compact anchored dropdown like GitHub's repository menu.
  void showHomeDropdown(View anchor){
    android.widget.PopupMenu popup=new android.widget.PopupMenu(this,anchor);
    popup.getMenu().add(0,1,0,"⌂  Utama");
    popup.getMenu().add(0,2,1,"✎  Nota");
    popup.getMenu().add(0,3,2,"▤  Maktabah");
    popup.getMenu().add(0,4,3,"⚙  Tetapan & penampilan");
    popup.getMenu().add(0,5,4,"⌕  Cari kategori Nota");
    popup.getMenu().add(0,6,5,"✦  Tema Pearl Gold & Turquoise");
    popup.getMenu().add(0,7,6,"Aa  Tetapan editor");
    popup.getMenu().add(0,8,7,"◈  Animasi header: "+(getPreferences(MODE_PRIVATE).getBoolean("smoothHeader",true)?"Aktif":"Mati"));
    popup.setOnMenuItemClickListener(item->{
      switch(item.getItemId()){
        case 1:showHome();return true;
        case 2:showNoteCategories();return true;
        case 3:showLibrary();return true;
        case 4:showSettings();return true;
        case 5:showNoteCategories();return true;
        case 6:getPreferences(MODE_PRIVATE).edit().putBoolean("pearlTurquoiseGold",!pearlPalette()).apply();applyAppearance();showHome();return true;
        case 7:showSettings();return true;
        case 8:boolean enabled=getPreferences(MODE_PRIVATE).getBoolean("smoothHeader",true);getPreferences(MODE_PRIVATE).edit().putBoolean("smoothHeader",!enabled).apply();showHome();return true;
        default:return false;
      }
    });
    popup.show();
  }
  void showHome() {
    flushPendingSave();
    editingBook=false;editingNote=false;saveIndicator=null;
    root=column();root.setFocusableInTouchMode(true);root.requestFocus();setContentView(root);
    // Two-tier GitHub-inspired header, entirely below Android system status bar.
    LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);root.addView(header);
    header.setBackground(rounded(cardSurface(),borderColor(),8));header.setPadding(8,5,8,2);header.setClipChildren(false);
    header.setElevation(1f*getResources().getDisplayMetrics().density);header.setFocusable(false);
    LinearLayout topBar=new LinearLayout(this);topBar.setGravity(Gravity.CENTER_VERTICAL);header.addView(topBar);
    TextView menu=text("☰",24);menu.setGravity(Gravity.CENTER);menu.setMinWidth(48);menu.setMinHeight(48);menu.setContentDescription("Buka dropdown navigasi dan tetapan DinNote");menu.setTooltipText("Menu DinNote");menu.setBackground(rounded(cardSurface(),borderColor(),12));menu.setOnClickListener(v->showHomeDropdown(menu));topBar.addView(menu);
    TextView title=text(" DinNote  ▾",18);android.graphics.drawable.Drawable logo=getDrawable(R.drawable.dinnote_launcher);int logoSize=(int)(28*getResources().getDisplayMetrics().density);logo.setBounds(0,0,logoSize,logoSize);title.setCompoundDrawables(logo,null,null,null);title.setCompoundDrawablePadding(8);title.setTypeface(null,Typeface.BOLD);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setContentDescription("DinNote, buka pilihan navigasi");title.setTooltipText("Pilih bahagian DinNote");title.setGravity(Gravity.CENTER_VERTICAL);title.setMinHeight(48);title.setClickable(true);title.setOnClickListener(v->showHomeDropdown(title));topBar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    TextView search=text("⌕",27);search.setGravity(Gravity.CENTER);search.setMinWidth(48);search.setMinHeight(48);search.setContentDescription("Buka kategori dan carian nota");search.setTooltipText("Carian nota");search.setBackground(rounded(cardSurface(),borderColor(),12));search.setOnClickListener(v->showNoteCategories());topBar.addView(search);
    TextView settings=text("⚙",22);settings.setGravity(Gravity.CENTER);settings.setMinWidth(48);settings.setMinHeight(48);settings.setTextColor(accent());settings.setContentDescription("Buka tetapan penampilan dan editor");settings.setTooltipText("Tetapan");settings.setBackground(rounded(cardSurface(),pearlPalette()?0xFFD5BD83:borderColor(),12));settings.setOnClickListener(v->showSettings());topBar.addView(settings);
    HorizontalScrollView tabScroll=new HorizontalScrollView(this);tabScroll.setHorizontalScrollBarEnabled(false);tabScroll.setSmoothScrollingEnabled(true);tabScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);tabScroll.setFillViewport(true);header.addView(tabScroll);
    LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER_VERTICAL);tabScroll.addView(tabs);
    TextView tabHome=text("Utama",14);tabHome.setTypeface(null,Typeface.BOLD);tabHome.setTextColor(pearlPalette()?0xFF0C9B9A:accent());tabHome.setPadding(12,9,12,9);tabHome.setContentDescription("Tab Utama aktif");tabHome.setSelected(true);tabs.addView(tabHome);
    TextView tabNotes=text("Nota",14);tabNotes.setPadding(12,9,12,9);tabNotes.setContentDescription("Buka Nota");tabNotes.setOnClickListener(v->showNoteCategories());tabs.addView(tabNotes);
    TextView tabBooks=text("Maktabah",14);tabBooks.setPadding(12,9,12,9);tabBooks.setContentDescription("Buka Maktabah");tabBooks.setOnClickListener(v->showLibrary());tabs.addView(tabBooks);
    TextView tabMore=text("Lagi ▾",14);tabMore.setPadding(12,9,12,9);tabMore.setContentDescription("Buka dropdown pilihan lain");tabMore.setOnClickListener(v->showHomeDropdown(tabMore));tabs.addView(tabMore);
    View activeRule=new View(this);activeRule.setBackgroundColor(pearlPalette()?0xFF0C9B9A:accent());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(56,3);rp.setMargins(14,0,0,0);header.addView(activeRule,rp);
    View headerDivider=new View(this);headerDivider.setBackgroundColor(borderColor());root.addView(headerDivider,new LinearLayout.LayoutParams(-1,1));
    // Header remains visible while the home content scrolls beneath it.
    ScrollView homeScroll=new ScrollView(this);homeScroll.setFillViewport(false);homeScroll.setScrollbarFadingEnabled(true);homeScroll.setVerticalFadingEdgeEnabled(false);homeScroll.setContentDescription("Kandungan halaman utama DinNote");homeScroll.setFocusable(false);homeScroll.setVerticalScrollBarEnabled(true);homeScroll.setFadingEdgeLength(0);homeScroll.setSmoothScrollingEnabled(true);homeScroll.setDescendantFocusability(android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS);homeScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);homeScroll.setClipToPadding(false);homeScroll.setPadding(0,0,0,2);root.addView(homeScroll,new LinearLayout.LayoutParams(-1,0,1));
    // Animate only the in-app header opacity, never Android system bars.
    final boolean smoothHeaderEnabled=getPreferences(MODE_PRIVATE).getBoolean("smoothHeader",true);
    homeScroll.setOnScrollChangeListener((View.OnScrollChangeListener)(v,scrollX,scrollY,oldX,oldY)->{
      if(smoothHeaderEnabled){
        float target=1f-Math.min(0.10f,Math.max(0,scrollY)/600f);
        if(Math.abs(header.getAlpha()-target)>0.015f){
          header.animate().cancel();
          header.animate().alpha(target).setDuration(getPreferences(MODE_PRIVATE).getBoolean("reduceMotion",false)?0:220).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
      } else if(header.getAlpha()!=1f){header.animate().cancel();header.setAlpha(1f);}
    });
    LinearLayout homeContent=new LinearLayout(this);homeContent.setOrientation(1);homeContent.setPadding(0,0,0,20);homeContent.setFocusableInTouchMode(false);homeContent.setClipToPadding(false);homeScroll.addView(homeContent);
    // GitHub-like home: lightweight heading, no oversized hero banner.
    TextView welcome=text("Nota & Maktabah",21);
    welcome.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    welcome.setTextColor(foreground());
    welcome.setPadding(4,16,4,5);
    welcome.setAccessibilityHeading(true);
    homeContent.addView(welcome);
    TextView intro=text("Semua catatan dan kitab anda, tersusun ringkas.",13);
    intro.setTextColor(dark()?0xFFA4BEB3:0xFF687B75);
    intro.setPadding(4,0,4,14);
    homeContent.addView(intro);
    // Compact collection overview; no additional home-screen menu.
    try(Cursor overview=db.getReadableDatabase().rawQuery(
      "SELECT (SELECT COUNT(*) FROM notes), (SELECT COUNT(*) FROM books)",null)){
      if(overview.moveToFirst()){
        TextView summary=text("✎  "+overview.getLong(0)+" nota    ·    ▤  "+overview.getLong(1)+" kitab",13);
        summary.setTextColor(dark()?0xFFA4C6B4:0xFF58776A);
        summary.setPadding(4,2,4,18);summary.setLetterSpacing(0.03f);summary.setMaxLines(2);summary.setTextDirection(View.TEXT_DIRECTION_LOCALE);summary.setFocusable(false);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        summary.setContentDescription(overview.getLong(0)+" nota dan "+overview.getLong(1)+" kitab disimpan");
        homeContent.addView(summary);
      }
    }catch(Exception overviewError){
      android.util.Log.w("DinNote","Home overview unavailable",overviewError);
    }
    // Two equal-width, minimal home cards; keep Android system bars untouched.
    LinearLayout homeCards=new LinearLayout(this);homeCards.setOrientation(LinearLayout.HORIZONTAL);
    homeCards.setGravity(Gravity.CENTER_VERTICAL);homeCards.setPadding(0,6,0,10);homeCards.setBaselineAligned(false);homeCards.setMinimumHeight(158);homeCards.setClipChildren(false);homeCards.setClipToPadding(false);homeCards.setContentDescription("Menu utama Nota dan Maktabah");homeCards.setFocusable(false);homeContent.addView(homeCards);
    Button notesMenu=button();notesMenu.setText("✎\nNota\nTulis & susun");notesMenu.setTextColor(accent());notesMenu.setTypeface(null,Typeface.BOLD);notesMenu.setTextSize(16);notesMenu.setIncludeFontPadding(false);notesMenu.setLetterSpacing(0.015f);notesMenu.setBackground(rounded(dark()?0xFF15372D:0xFFFFFFFF,borderColor(),24));
    notesMenu.setGravity(Gravity.CENTER);notesMenu.setElevation(1.5f);notesMenu.setStateListAnimator(null);notesMenu.setLineSpacing(6,1f);notesMenu.setMinWidth(0);notesMenu.setMinHeight(152);notesMenu.setHapticFeedbackEnabled(true);notesMenu.setSoundEffectsEnabled(true);notesMenu.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);notesMenu.setMaxLines(4);notesMenu.setEllipsize(android.text.TextUtils.TruncateAt.END);notesMenu.setPadding(12,20,12,20);notesMenu.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    Button libraryMenu=button();libraryMenu.setText("▤\nMaktabah\nKoleksi kitab");libraryMenu.setTextColor(accent());libraryMenu.setTypeface(null,Typeface.BOLD);
    libraryMenu.setTextSize(16);libraryMenu.setIncludeFontPadding(false);libraryMenu.setLetterSpacing(0.015f);libraryMenu.setBackground(rounded(dark()?0xFF173C30:0xFFEAF6E9,borderColor(),24));libraryMenu.setElevation(1.5f);libraryMenu.setStateListAnimator(null);libraryMenu.setGravity(Gravity.CENTER);libraryMenu.setLineSpacing(6,1f);libraryMenu.setMinWidth(0);libraryMenu.setMinHeight(152);libraryMenu.setHapticFeedbackEnabled(true);libraryMenu.setSoundEffectsEnabled(true);libraryMenu.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);libraryMenu.setMaxLines(4);libraryMenu.setEllipsize(android.text.TextUtils.TruncateAt.END);libraryMenu.setPadding(12,20,12,20);libraryMenu.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    LinearLayout.LayoutParams noteCardParams=new LinearLayout.LayoutParams(0,-2,1);noteCardParams.setMargins(0,8,7,18);
    LinearLayout.LayoutParams libraryCardParams=new LinearLayout.LayoutParams(0,-2,1);libraryCardParams.setMargins(7,8,0,18);
    homeCards.addView(notesMenu,noteCardParams);homeCards.addView(libraryMenu,libraryCardParams);
    libraryMenu.setContentDescription("Buka kategori kitab Maktabah");libraryMenu.setTooltipText("Buka Maktabah");libraryMenu.setFocusable(true);libraryMenu.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);libraryMenu.setAllCaps(false);libraryMenu.setMinHeight(140);libraryMenu.setLongClickable(false);libraryMenu.setOnClickListener(v->showLibrary());
    notesMenu.setContentDescription("Buka kategori Nota peribadi");notesMenu.setTooltipText("Buka Nota");notesMenu.setFocusable(true);notesMenu.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);notesMenu.setAllCaps(false);notesMenu.setMinHeight(140);notesMenu.setLongClickable(false);notesMenu.setOnClickListener(v->showNoteCategories());
    // Front-page pinned notes: compact, accessible, and independent of category filtering.
    LinearLayout pinnedSection=new LinearLayout(this);pinnedSection.setOrientation(1);pinnedSection.setContentDescription("Bahagian nota penting dipin pada halaman utama");pinnedSection.setPadding(0,12,0,18);pinnedSection.setClipToPadding(false);
    homeContent.addView(pinnedSection);
    try(Cursor pinnedNotes=db.getReadableDatabase().rawQuery(
      "SELECT n.id,n.title,n.body,n.updated,n.category_id FROM notes n WHERE n.pinned=1 ORDER BY n.updated DESC,n.id DESC",null)){
      if(pinnedNotes.moveToFirst()){
        int pinnedCount=pinnedNotes.getCount();
        LinearLayout pinnedHeader=new LinearLayout(this);
        pinnedHeader.setOrientation(LinearLayout.HORIZONTAL);
        pinnedHeader.setGravity(Gravity.CENTER_VERTICAL);pinnedHeader.setMinimumHeight(48);pinnedHeader.setClipToPadding(false);pinnedHeader.setBaselineAligned(false);
        pinnedSection.addView(pinnedHeader);
        TextView pinnedHeading=text("📌  Nota dipin  ·  "+pinnedCount,16);
        pinnedHeading.setTypeface(null,Typeface.BOLD);pinnedHeading.setLetterSpacing(0.015f);pinnedHeading.setContentDescription("Jumlah nota dipin: "+pinnedCount);pinnedHeading.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);pinnedHeading.setMaxLines(1);pinnedHeading.setAccessibilityHeading(true);pinnedHeading.setIncludeFontPadding(false);pinnedHeading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        pinnedHeader.addView(pinnedHeading,new LinearLayout.LayoutParams(0,-2,1));
        Button togglePinned=button();
        boolean collapsed=getPreferences(MODE_PRIVATE).getBoolean("pinnedCollapsed",false);
        togglePinned.setText(collapsed?"Papar":"Sembunyi");togglePinned.setMinWidth(88);togglePinned.setTooltipText("Papar atau sembunyikan nota dipin");togglePinned.setTextSize(12);togglePinned.setAllCaps(false);togglePinned.setFocusable(true);togglePinned.setMinHeight(48);togglePinned.setHapticFeedbackEnabled(true);togglePinned.setSoundEffectsEnabled(true);togglePinned.setGravity(Gravity.CENTER);togglePinned.setContentDescription(collapsed?"Paparkan nota dipin":"Sembunyikan nota dipin");
        togglePinned.setContentDescription(collapsed?"Paparkan nota dipin":"Sembunyikan nota dipin");
        pinnedHeader.addView(togglePinned,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout pinnedItems=new LinearLayout(this);
        pinnedItems.setOrientation(1);pinnedItems.setPadding(0,4,0,4);pinnedItems.setFocusable(false);pinnedItems.setClipToPadding(false);pinnedItems.setClipChildren(false);pinnedItems.setContentDescription("Senarai nota dipin");pinnedSection.addView(pinnedItems);
        pinnedItems.setVisibility(collapsed?View.GONE:View.VISIBLE);
        togglePinned.setOnClickListener(v->{
          boolean hide=pinnedItems.getVisibility()==View.VISIBLE;
          pinnedItems.setVisibility(hide?View.GONE:View.VISIBLE);
          togglePinned.setText(hide?"Papar":"Sembunyi");
          togglePinned.setContentDescription(hide?"Paparkan nota dipin":"Sembunyikan nota dipin");
          getPreferences(MODE_PRIVATE).edit().putBoolean("pinnedCollapsed",hide).apply();
        });
        do{
          final int pinnedId=pinnedNotes.getInt(0);
          String pinnedTitle=pinnedNotes.getString(1);
          String pinnedBody=pinnedNotes.getString(2);
          if(pinnedTitle==null||pinnedTitle.trim().isEmpty())pinnedTitle="Tanpa tajuk";
          if(pinnedBody==null)pinnedBody="";
          pinnedBody=pinnedBody.replace("\r"," ").replace("\n"," ").replace("\t"," ").trim();
          LinearLayout pinnedRow=new LinearLayout(this);
          pinnedRow.setOrientation(LinearLayout.HORIZONTAL);
          pinnedRow.setGravity(Gravity.CENTER_VERTICAL);pinnedRow.setBaselineAligned(false);
          pinnedItems.addView(pinnedRow);
          LinearLayout pinnedCard=new LinearLayout(this);
          pinnedCard.setOrientation(1);
          pinnedCard.setPadding(18,14,18,14);
          pinnedCard.setBackground(rounded(cardSurface(),borderColor(),18));pinnedCard.setElevation(1f);pinnedCard.setHapticFeedbackEnabled(true);pinnedCard.setSoundEffectsEnabled(true);pinnedCard.setClipToPadding(false);
          LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(0,-2,1);
          cardParams.setMargins(0,6,7,6);pinnedRow.addView(pinnedCard,cardParams);
          TextView pinnedTitleView=text(pinnedTitle,15);
          pinnedTitleView.setTypeface(null,Typeface.BOLD);pinnedTitleView.setTextColor(foreground());
          pinnedTitleView.setMaxLines(2);pinnedTitleView.setMinHeight(24);pinnedTitleView.setIncludeFontPadding(false);pinnedTitleView.setLineSpacing(2,1f);pinnedTitleView.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);pinnedTitleView.setTextIsSelectable(false);pinnedTitleView.setGravity(Gravity.START);pinnedTitleView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
          pinnedTitleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
          pinnedCard.addView(pinnedTitleView);
          if(!pinnedBody.isEmpty()){
            TextView preview=text(pinnedBody.length()>110?pinnedBody.substring(0,110)+"…":pinnedBody,13);
            preview.setMaxLines(2);preview.setMinHeight(20);preview.setLineSpacing(2,1f);preview.setIncludeFontPadding(false);preview.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);preview.setGravity(Gravity.START);preview.setTextColor(foreground());preview.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            pinnedCard.addView(preview);
          }
          String category=noteCategoryName(pinnedNotes.getInt(4));
          java.text.DateFormat dateFormat=android.text.format.DateFormat.getDateFormat(this);
          long updated=pinnedNotes.getLong(3);
          String dateLabel=updated>0?dateFormat.format(new java.util.Date(updated)).toString():"";
          TextView meta=text(category+(dateLabel.isEmpty()?"":" · "+dateLabel),12);
          meta.setTextColor(dark()?0xFF8B949E:0xFF656D76);meta.setGravity(Gravity.START);meta.setMaxLines(1);meta.setLetterSpacing(0.01f);meta.setIncludeFontPadding(false);meta.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
          pinnedCard.addView(meta);
          pinnedCard.setContentDescription("Buka nota dipin "+pinnedTitle+" dalam kategori "+category);
          pinnedCard.setClickable(true);pinnedCard.setFocusable(true);pinnedCard.setMinimumHeight(64);pinnedCard.setFocusableInTouchMode(false);pinnedCard.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
          pinnedCard.setOnClickListener(v->edit(pinnedId));pinnedCard.setTooltipText("Buka nota penting");
          Button unpin=button();unpin.setText("⌁");
          unpin.setContentDescription("Nyahpin "+pinnedTitle+" daripada halaman utama");unpin.setTextSize(17);unpin.setMinHeight(48);unpin.setMinWidth(48);unpin.setHapticFeedbackEnabled(true);unpin.setSoundEffectsEnabled(true);unpin.setFocusable(true);unpin.setSoundEffectsEnabled(true);unpin.setGravity(Gravity.CENTER);unpin.setTooltipText("Buang pin");unpin.setAllCaps(false);
          pinnedRow.addView(unpin,new LinearLayout.LayoutParams(56,-2));
          unpin.setOnClickListener(v->{
            db.getWritableDatabase().execSQL("UPDATE notes SET pinned=0 WHERE id=?",new Object[]{pinnedId});
            Toast.makeText(this,"Nota dinyahpin",Toast.LENGTH_SHORT).show();
            showHome();
          });
        }while(pinnedNotes.moveToNext());
      }
    }catch(Exception e){android.util.Log.w("DinNote","Pinned notes preview unavailable",e);}
    // Category management belongs inside Nota and Maktabah, not on the home page.
    TextView recentHeading=text("NOTA ANDA",11);
    recentHeading.setLetterSpacing(0.15f);recentHeading.setTypeface(null,Typeface.BOLD);
    recentHeading.setTextColor(accent());recentHeading.setPadding(4,18,4,9);recentHeading.setAccessibilityHeading(true);recentHeading.setIncludeFontPadding(false);recentHeading.setTextDirection(View.TEXT_DIRECTION_LOCALE);
    homeContent.addView(recentHeading);
    Button sort=button();sort.setText("↕ Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[Math.max(0,Math.min(2,noteSort))]);homeContent.addView(sort);
    sort.setTextSize(13);sort.setTooltipText("Tukar susunan nota");sort.setFocusable(true);sort.setMinHeight(48);sort.setHapticFeedbackEnabled(true);sort.setSoundEffectsEnabled(true);sort.setAllCaps(false);sort.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);sort.setContentDescription("Pilih susunan nota: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[Math.max(0,Math.min(2,noteSort))]);sort.setOnClickListener(v->new AlertDialog.Builder(this).setItems(new String[]{"Terkini","Terlama","Tajuk A-Z"},(d,w)->{noteSort=w;getPreferences(MODE_PRIVATE).edit().putInt("noteSort",w).apply();sort.setText("↕ Susun: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[w]);sort.setContentDescription("Pilih susunan nota: "+new String[]{"Terkini","Terlama","Tajuk A-Z"}[w]);refresh(noteFilter);}).show());
    search=field();search.setContentDescription("Cari tajuk dan kandungan nota");search.setSingleLine(true);search.setMinHeight(52);search.setMaxLines(1);search.setHintTextColor(dark()?0xFF8B949E:0xFF656D76);search.setTextSize(15);search.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);search.setSelectAllOnFocus(false);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setHint("⌕  Cari tajuk atau kandungan…");search.setText(noteFilter==null?"":noteFilter);search.setSelection(search.length());homeContent.addView(search);
    Button add=button();add.setText("＋  Nota baharu");add.setBackground(rounded(cardSurface(),accent(),16));add.setTextColor(accent());add.setTypeface(null,Typeface.BOLD);add.setTooltipText("Cipta nota baharu");add.setMinHeight(52);add.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);add.setHapticFeedbackEnabled(true);add.setSoundEffectsEnabled(true);add.setAllCaps(false);add.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);add.setContentDescription("Tulis nota peribadi baharu");homeContent.addView(add);add.setOnClickListener(v->edit(-1));
    list=new LinearLayout(this);list.setOrientation(1);list.setContentDescription("Senarai nota");list.setClipToPadding(false);list.setPadding(0,4,0,12);homeContent.addView(list,new LinearLayout.LayoutParams(-1,-2));
    search.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();return true;}return false;});
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){noteFilter=s.toString();refresh(noteFilter);}public void afterTextChanged(Editable e){}});
    try{refresh(noteFilter);}catch(Exception e){android.util.Log.e("DinNote","Failed loading notes",e);Toast.makeText(this,"Gagal memuatkan nota",Toast.LENGTH_LONG).show();}
    // In-app navigator sits ABOVE Android's own gesture/button navigation area.
    LinearLayout navigator=new LinearLayout(this);
    navigator.setOrientation(LinearLayout.HORIZONTAL);
    navigator.setGravity(Gravity.CENTER_VERTICAL);
    navigator.setContentDescription("Navigasi dalaman DinNote: Utama, Nota dan Maktabah");
    navigator.setBackground(rounded(cardSurface(),borderColor(),14));navigator.setPadding(4,8,4,6);
    Button homeNav=button();homeNav.setText("⌂ Utama");homeNav.setTextSize(13);homeNav.setTooltipText("Kembali ke utama");homeNav.setMinHeight(48);homeNav.setContentDescription("Kembali ke halaman utama DinNote");
    Button noteNav=button();noteNav.setText("▤ Nota");noteNav.setTextSize(13);noteNav.setTooltipText("Cari nota");noteNav.setMinHeight(48);noteNav.setOnFocusChangeListener((v,hasFocus)->{if(hasFocus)noteNav.setTextColor(accent());});noteNav.setContentDescription("Fokus carian Nota");noteNav.setTextColor(accent());
    Button bookNav=button();bookNav.setText("▥ Maktabah");bookNav.setTextSize(13);bookNav.setTooltipText("Buka Maktabah");bookNav.setMinHeight(48);bookNav.setContentDescription("Buka halaman Maktabah");bookNav.setTextColor(accent());
    navigator.addView(homeNav,new LinearLayout.LayoutParams(0,-2,1));
    navigator.addView(noteNav,new LinearLayout.LayoutParams(0,-2,1));
    navigator.addView(bookNav,new LinearLayout.LayoutParams(0,-2,1));
    navigator.setMinimumHeight(56);navigator.setElevation(1f);navigator.setFocusable(false);navigator.setClipToPadding(false);navigator.setBaselineAligned(false);root.addView(navigator,new LinearLayout.LayoutParams(-1,-2));
    homeNav.setOnClickListener(v->{search.clearFocus();homeScroll.smoothScrollTo(0,0);});
    noteNav.setOnClickListener(v->{search.requestFocus();homeScroll.post(()->{android.graphics.Rect r=new android.graphics.Rect();search.getDrawingRect(r);homeContent.offsetDescendantRectToMyCoords(search,r);homeScroll.smoothScrollTo(0,r.top);});});
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
    Button back=button();back.setText("← DinNote");back.setContentDescription("Kembali ke halaman utama daripada kategori Nota");back.setMinHeight(48);page.addView(back);back.setOnClickListener(v->showHome());
    TextView heading=text("📁 Kategori Nota",21);heading.setContentDescription("Urus kategori nota peribadi");heading.setTypeface(null,Typeface.BOLD);heading.setPadding(10,12,10,14);page.addView(heading);
    EditText categorySearch=field();categorySearch.setSingleLine(true);categorySearch.setSelectAllOnFocus(false);categorySearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);categorySearch.setHint("⌕ Cari kategori Nota…");categorySearch.setContentDescription("Cari kategori Nota");categorySearch.setTextSize(15);categorySearch.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);categorySearch.setMinHeight(48);page.addView(categorySearch);
    Button all=button();all.setText("▤  Semua nota");all.setContentDescription("Lihat semua nota tanpa penapis kategori");all.setMinHeight(48);all.setAllCaps(false);page.addView(all);all.setOnClickListener(v->{selectedNoteCategory=-1;showHome();});
    Button add=button();add.setText("＋ Tambah kategori");add.setContentDescription("Cipta kategori Nota baharu");add.setTextColor(accent());add.setMinHeight(48);page.addView(add);add.setOnClickListener(v->promptNoteCategory(-1,""));
    ScrollView scroll=new ScrollView(this);LinearLayout items=new LinearLayout(this);items.setOrientation(1);items.setPadding(0,4,0,12);scroll.setFillViewport(true);scroll.setSmoothScrollingEnabled(true);scroll.addView(items);scroll.setContentDescription("Senarai kategori Nota peribadi");page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    categorySearch.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence text,int start,int count,int after){}
      public void onTextChanged(CharSequence text,int start,int before,int count){populateNoteCategories(items,text.toString());}
      public void afterTextChanged(Editable text){}
    });
    populateNoteCategories(items,"");
  }
  void populateNoteCategories(LinearLayout items,String query){
    items.removeAllViews();
    String filter=query==null?"":query.trim();
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name FROM note_categories WHERE name LIKE ? ORDER BY name COLLATE NOCASE",new String[]{"%"+filter+"%"})){
      while(c.moveToNext()){
        final int id=c.getInt(0);final String name=c.getString(1);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(48);row.setPadding(0,4,0,4);items.addView(row);
        Button open=button();open.setText("▤  "+name);open.setContentDescription("Buka kategori Nota "+name);open.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);open.setAllCaps(false);open.setMaxLines(2);open.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(open,new LinearLayout.LayoutParams(0,-2,1));
        open.setOnClickListener(v->{selectedNoteCategory=id;showHome();});
        Button options=button();options.setText("⋮");options.setMinWidth(48);options.setMinHeight(48);options.setContentDescription("Ubah nama atau padam kategori "+name);row.addView(options,new LinearLayout.LayoutParams(-2,-2));
        options.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(name)
          .setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{
            if(which==0){promptNoteCategory(id,name);return;}
            new AlertDialog.Builder(this).setTitle("Padam kategori?")
              .setMessage("Nota dalam kategori ini tidak dipadam. Nota akan dipindahkan ke Tanpa kategori.")
              .setNegativeButton("Batal",null).setPositiveButton("Padam",(dd,w)->{
                SQLiteDatabase writable=db.getWritableDatabase();writable.beginTransaction();
                try{
                  ContentValues cv=new ContentValues();cv.put("category_id",-1);
                  writable.update("notes",cv,"category_id=?",new String[]{String.valueOf(id)});
                  writable.delete("note_categories","id=?",new String[]{String.valueOf(id)});
                  writable.setTransactionSuccessful();
                }finally{writable.endTransaction();}
                if(selectedNoteCategory==id)selectedNoteCategory=-1;
                showNoteCategories();
              }).show();
          }).show());
      }
    }
    if(items.getChildCount()==0)items.addView(text(filter.isEmpty()?"Belum ada kategori Nota. Tekan Tambah kategori untuk mula.":"Tiada kategori sepadan dengan carian.",16));
  }
  void promptNoteCategory(int id,String existing){
    EditText input=field();input.setSingleLine(true);input.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);input.setText(existing);input.setSelectAllOnFocus(id>=0);input.setHint("Nama kategori Nota");input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
    new AlertDialog.Builder(this).setTitle(id<0?"Tambah kategori":"Ubah kategori").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(name.isEmpty()){Toast.makeText(this,"Nama kategori diperlukan",Toast.LENGTH_SHORT).show();return;}
      ContentValues cv=new ContentValues();cv.put("name",name);
      if(id<0)db.getWritableDatabase().insert("note_categories",null,cv);else db.getWritableDatabase().update("note_categories",cv,"id=?",new String[]{""+id});
      showNoteCategories();
    }).show();
  }
  void refresh(String query) {
    if(query==null)query="";
    query=query.trim();
    final String currentQuery=query;
    final int safeSort=Math.max(0,Math.min(2,noteSort));
    if(list==null||db==null)return;
    list.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,body,color,pinned FROM notes WHERE (title LIKE ? OR body LIKE ?) AND (?=-1 OR category_id=?) ORDER BY pinned DESC,"+(safeSort==1?"updated ASC,id ASC":safeSort==2?"title COLLATE NOCASE ASC,id DESC":"updated DESC,id DESC"),new String[]{"%"+query+"%","%"+query+"%",""+selectedNoteCategory,""+selectedNoteCategory})) {
      while(c.moveToNext()){visibleCount++;final int id=c.getInt(0);String heading=c.getString(1);String body=c.getString(2);if(heading==null)heading="";if(body==null)body="";body=body.replace("\r"," ").replace("\n"," ").replace("\t"," ").trim();int noteColor=c.getInt(3);boolean pinned=c.getInt(4)==1;
        TextView item=text((heading.trim().isEmpty()?"Tanpa tajuk":heading)+"\n"+(body.length()>90?body.substring(0,90)+"…":body),15);
        item.setBackground(rounded(noteColor==Color.WHITE?cardSurface():noteColor,borderColor(),16));item.setMaxLines(4);item.setEllipsize(android.text.TextUtils.TruncateAt.END);item.setMinHeight(56);item.setTextColor(noteColor==Color.WHITE?foreground():Color.BLACK);item.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,5,0,5);LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setBaselineAligned(false);list.addView(line,p);line.addView(item,new LinearLayout.LayoutParams(0,-2,1));item.setContentDescription("Buka nota "+(heading.trim().isEmpty()?"Tanpa tajuk":heading)+(pinned?", dipin":""));item.setClickable(true);item.setFocusable(true);item.setPadding(14,12,14,12);item.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);item.setGravity(Gravity.CENTER_VERTICAL);item.setOnClickListener(v->edit(id));Button pin=button();pin.setText(pinned?"📌":"○");pin.setContentDescription((pinned?"Nyahpin nota ":"Pin nota ")+(heading.trim().isEmpty()?"Tanpa tajuk":heading));pin.setMinHeight(48);pin.setAllCaps(false);pin.setTooltipText(pinned?"Nyahpin":"Pin nota");pin.setHapticFeedbackEnabled(true);line.addView(pin,new LinearLayout.LayoutParams(56,-2));pin.setOnClickListener(v->{db.getWritableDatabase().execSQL("UPDATE notes SET pinned=? WHERE id=?",new Object[]{pinned?0:1,id});Toast.makeText(this,pinned?"Nota dinyahpin":"Nota dipin",Toast.LENGTH_SHORT).show();if(search!=null&&search.getParent()!=null)showHome();else refresh(currentQuery);});
      }
    }
    if(visibleCount==0){TextView empty=text(query.trim().isEmpty()?"Belum ada nota. Tekan Nota baharu untuk mula.":"Tiada nota sepadan. Cuba kata kunci lain.",15);empty.setGravity(Gravity.CENTER);empty.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);empty.setPadding(20,32,20,32);empty.setMinHeight(112);empty.setContentDescription("Tiada nota untuk dipaparkan");empty.setTextColor(dark()?0xFF8B949E:0xFF656D76);list.addView(empty);}
  }
  void edit(int id) {
    flushPendingSave();editingNote=true;
    LinearLayout page=column();setContentView(page);enableKeyboardAwareEditor(page);
    Button back=button();back.setText("← Nota");back.setTextSize(14);back.setContentDescription("Kembali ke halaman Nota");back.setMinHeight(48);page.addView(back);
    saveIndicator=text("Disimpan secara automatik",12);saveIndicator.setContentDescription("Status simpanan automatik");page.addView(saveIndicator);
    final int[] chosenCategory={-1};
    if(id>=0){try(Cursor cat=db.getReadableDatabase().rawQuery("SELECT category_id FROM notes WHERE id=?",new String[]{""+id})){if(cat.moveToFirst())chosenCategory[0]=cat.getInt(0);}}
    else if(selectedNoteCategory>=0)chosenCategory[0]=selectedNoteCategory;
    final Runnable[] saveActionHolder={null};
    Button categoryPicker=button();categoryPicker.setText("Kategori: "+noteCategoryName(chosenCategory[0]));page.addView(categoryPicker);
    categoryPicker.setOnClickListener(v->{ArrayList<Integer> ids=new ArrayList<>();ArrayList<String> names=new ArrayList<>();ids.add(-1);names.add("Tanpa kategori");try(Cursor cats=db.getReadableDatabase().rawQuery("SELECT id,name FROM note_categories ORDER BY name COLLATE NOCASE",null)){while(cats.moveToNext()){ids.add(cats.getInt(0));names.add(cats.getString(1));}}new AlertDialog.Builder(this).setTitle("Pilih kategori").setItems(names.toArray(new String[0]),(dialog,which)->{chosenCategory[0]=ids.get(which);categoryPicker.setText("Kategori: "+noteCategoryName(chosenCategory[0]));if(saveActionHolder[0]!=null)scheduleSave(saveActionHolder[0]);}).show();});
    EditText title=field();title.setContentDescription("Tajuk nota");title.setHint("Tajuk nota");title.setSingleLine(true);title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});title.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);title.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);title.setTextSize(17);title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setSelectAllOnFocus(false);title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);page.addView(title);
    EditText body=field();body.setHint("Tulis nota di sini…");body.setTextSize(15);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);body.setVerticalScrollBarEnabled(true);body.setTextColor(foreground());
    body.setMinLines(5);body.setMinHeight(160);body.setSelectAllOnFocus(false);body.setContentDescription("Kandungan nota");body.setHorizontallyScrolling(false);body.setTextIsSelectable(true);body.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    styleEditorPage(page,title,body,saveIndicator,back);
    final Button[] deleteButton={null};
    final int[] noteId={id};final boolean[] loading={true};final int[] noteColor={Color.WHITE};final boolean[] notePinned={false};final Runnable[] saveAction={null};
    int[] colors={Color.WHITE,0xFFFFF4B8,0xFFDDF4DF,0xFFDCEEFF,0xFFFFE1E9,0xFFECE2FF,0xFFFFE7CC,0xFFD9F5F0,0xFFE7EAFB,0xFFF3E7D3};
    LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);page.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
    Button pinButton=button();pinButton.setText("○ Pin");pinButton.setContentDescription("Pin nota ke halaman utama");page.addView(pinButton);pinButton.setOnClickListener(v->{notePinned[0]=!notePinned[0];pinButton.setText(notePinned[0]?"📌 Dipin":"○ Pin");pinButton.setContentDescription(notePinned[0]?"Nyahpin nota":"Pin nota ke halaman utama");if(saveAction[0]!=null)scheduleSave(saveAction[0]);});
    Button colorButton=button();colorButton.setText("● Warna");colorButton.setContentDescription("Pilih warna latar nota");toolbar.addView(colorButton,new LinearLayout.LayoutParams(0,-2,1));
    Button fontColor=button();fontColor.setText("A●");fontColor.setContentDescription("Warna tulisan terpilih");toolbar.addView(fontColor,new LinearLayout.LayoutParams(0,-2,1));fontColor.setOnClickListener(v->chooseTextColor(body,()->{if(saveAction[0]!=null)scheduleSave(saveAction[0]);}));
    Button checkButton=button();checkButton.setText("☐");toolbar.addView(checkButton,new LinearLayout.LayoutParams(0,-2,1));
    Button boldButton=button();boldButton.setText("B");boldButton.setContentDescription("Tebalkan teks dipilih");boldButton.setTypeface(null,Typeface.BOLD);toolbar.addView(boldButton,new LinearLayout.LayoutParams(0,-2,1));
    Button italicButton=button();italicButton.setText("I");italicButton.setContentDescription("Condongkan teks dipilih");italicButton.setTypeface(null,Typeface.ITALIC);toolbar.addView(italicButton,new LinearLayout.LayoutParams(0,-2,1));
    Button underlineButton=button();underlineButton.setText("U");underlineButton.setContentDescription("Gariskan teks dipilih");toolbar.addView(underlineButton,new LinearLayout.LayoutParams(0,-2,1));
    for(int i=0;i<toolbar.getChildCount();i++)styleEditorTool((Button)toolbar.getChildAt(i));
    underlineButton.setOnClickListener(v->underlineSelection(body));
    colorButton.setOnClickListener(v->{
      String[] names={"Putih","Kuning lembut","Hijau lembut","Biru lembut","Merah jambu","Ungu lembut","Oren lembut","Turquoise","Indigo lembut","Krim"};
      LinearLayout palette=new LinearLayout(this);palette.setOrientation(LinearLayout.VERTICAL);palette.setPadding(20,12,20,12);
      AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Warna nota").setView(palette).setNegativeButton("Batal",null).create();
      for(int i=0;i<colors.length;i++){
        final int selected=colors[i];
        Button swatch=button();swatch.setText((selected==noteColor[0]?"✓  ":"●  ")+names[i]);
        swatch.setAllCaps(false);swatch.setTextColor(Color.BLACK);
        swatch.setBackground(rounded(selected,0xFFB9C2CC,14));
        palette.addView(swatch,new LinearLayout.LayoutParams(-1,-2));
        swatch.setOnClickListener(view->{
          noteColor[0]=selected;
          body.setBackground(rounded(selected==Color.WHITE?cardSurface():selected,borderColor(),16));
          body.setTextColor(selected==Color.WHITE?foreground():Color.BLACK);
          colorButton.setText("● Warna");
          if(saveAction[0]!=null)scheduleSave(saveAction[0]);
          dialog.dismiss();
        });
      }
      dialog.show();
    });
    checkButton.setOnClickListener(v->{int start=Math.max(0,body.getSelectionStart());body.getText().insert(start,"☐ ");});
    boldButton.setOnClickListener(v->styleSelection(body,Typeface.BOLD));
    italicButton.setOnClickListener(v->styleSelection(body,Typeface.ITALIC));
    if(id>=0){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body,color,pinned,body_html FROM notes WHERE id=?",new String[]{""+id})){if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(4)!=null&&!c.getString(4).isEmpty()?android.text.Html.fromHtml(c.getString(4),android.text.Html.FROM_HTML_MODE_COMPACT):c.getString(1));noteColor[0]=c.getInt(2);notePinned[0]=c.getInt(3)==1;pinButton.setText(notePinned[0]?"📌 Dipin":"○ Pin");pinButton.setContentDescription(notePinned[0]?"Nyahpin nota":"Pin nota ke halaman utama");body.setBackground(rounded(noteColor[0]==Color.WHITE?cardSurface():noteColor[0],borderColor(),16));body.setTextColor(noteColor[0]==Color.WHITE?foreground():Color.BLACK);}}}
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),b=body.getText().toString();if(noteId[0]<0&&t.trim().isEmpty()&&b.trim().isEmpty()){setSaveStatus("Nota kosong");return;}
      ContentValues values=new ContentValues();values.put("title",t);values.put("body",b);values.put("body_html",android.text.Html.toHtml(body.getText(),android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));values.put("updated",System.currentTimeMillis());values.put("color",noteColor[0]);values.put("category_id",chosenCategory[0]);values.put("pinned",notePinned[0]?1:0);
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
    Button back=button();back.setText("← DinNote");back.setContentDescription("Kembali ke halaman utama DinNote");back.setTextSize(14);page.addView(back);back.setOnClickListener(v->showHome());
    TextView libraryTitle=text("📚 Maktabah",21);libraryTitle.setContentDescription("Maktabah, koleksi kitab dan ilmu Islam");libraryTitle.setTypeface(null,Typeface.BOLD);libraryTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);libraryTitle.setPadding(10,12,10,14);page.addView(libraryTitle);
    EditText categorySearch=field();categorySearch.setSingleLine(true);categorySearch.setSelectAllOnFocus(false);categorySearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);categorySearch.setHint("⌕ Cari kategori Maktabah…");categorySearch.setContentDescription("Cari kategori Maktabah");categorySearch.setTextSize(15);categorySearch.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);page.addView(categorySearch);
    Button add=button();add.setText("＋ Tambah kategori Maktabah");add.setContentDescription("Tambah kategori baharu Maktabah");add.setTextColor(accent());add.setMinHeight(48);page.addView(add);
    add.setOnClickListener(v->promptCategory());
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setSmoothScrollingEnabled(true);LinearLayout items=new LinearLayout(this);items.setOrientation(1);items.setPadding(0,4,0,12);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    categorySearch.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence text,int start,int count,int after){}
      public void onTextChanged(CharSequence text,int start,int before,int count){populateLibraryCategories(items,text.toString());}
      public void afterTextChanged(Editable text){}
    });
    populateLibraryCategories(items,"");
  }
  void populateLibraryCategories(LinearLayout items,String query){
    items.removeAllViews();
    String filter=query==null?"":query.trim();
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name FROM categories WHERE name LIKE ? ORDER BY name COLLATE NOCASE",new String[]{"%"+filter+"%"})){
      while(c.moveToNext()){int categoryId=c.getInt(0);String name=c.getString(1);
        LinearLayout categoryRow=new LinearLayout(this);categoryRow.setOrientation(LinearLayout.HORIZONTAL);categoryRow.setGravity(Gravity.CENTER_VERTICAL);categoryRow.setMinimumHeight(48);categoryRow.setPadding(0,4,0,4);items.addView(categoryRow);
        Button item=button();item.setText("▥  "+name);item.setContentDescription("Buka kategori "+name);item.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        item.setMaxLines(2);item.setEllipsize(android.text.TextUtils.TruncateAt.END);categoryRow.addView(item,new LinearLayout.LayoutParams(0,-2,1));item.setOnClickListener(v->showBooks(categoryId,name));
        Button options=button();options.setText("⋮");options.setMinWidth(48);options.setMinHeight(48);options.setContentDescription("Urus kategori "+name);
        categoryRow.addView(options,new LinearLayout.LayoutParams(-2,-2));
        options.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(name)
          .setItems(new String[]{"Ubah nama","Padam kategori"},(d,which)->{
            if(which==0){promptRenameCategory(categoryId,name);return;}
            new AlertDialog.Builder(this).setTitle("Padam kategori?")
              .setMessage("Kategori dan semua kitab di dalamnya akan dipadam. Tindakan ini tidak boleh dibatalkan. Buat backup terlebih dahulu jika perlu.")
              .setNegativeButton("Batal",null)
              .setPositiveButton("Padam", (dd,w)->{
                SQLiteDatabase writable=db.getWritableDatabase();writable.beginTransaction();
                try{
                  writable.delete("books","category_id=?",new String[]{String.valueOf(categoryId)});
                  writable.delete("categories","id=?",new String[]{String.valueOf(categoryId)});
                  writable.setTransactionSuccessful();
                }finally{writable.endTransaction();}
                showLibrary();
              }).show();
          }).show());
      }
    }
    if(items.getChildCount()==0)items.addView(text(filter.isEmpty()?"Belum ada kategori. Tekan Tambah kategori Maktabah untuk mula.":"Tiada kategori sepadan dengan carian.",16));
  }
  void promptCategory(){
    EditText input=field();input.setHint("Nama kategori");input.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);input.setSingleLine(true);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
    new AlertDialog.Builder(this).setTitle("Kategori baharu").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(!name.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",name);db.getWritableDatabase().insert("categories",null,cv);}else Toast.makeText(this,"Masukkan nama kategori",Toast.LENGTH_SHORT).show();showLibrary();
    }).show();
  }
  void promptRenameCategory(int id,String old){
    EditText input=field();input.setText(old);input.setSelectAllOnFocus(true);input.setSingleLine(true);input.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
    new AlertDialog.Builder(this).setTitle("Ubah nama kategori").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
      String name=input.getText().toString().trim();if(!name.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",name);db.getWritableDatabase().update("categories",cv,"id=?",new String[]{""+id});}else Toast.makeText(this,"Nama kategori tidak boleh kosong",Toast.LENGTH_SHORT).show();showLibrary();
    }).show();
  }
  void showBooks(int categoryId,String categoryName){
    flushPendingSave();editingBook=false;editingNote=false;saveIndicator=null;activeCategoryId=categoryId;activeCategoryName=categoryName;
    LinearLayout page=column();setContentView(page);
    Button back=button();back.setText("← Maktabah");back.setTextSize(14);back.setContentDescription("Kembali ke kategori Maktabah");back.setMinHeight(48);page.addView(back);back.setOnClickListener(v->showLibrary());
    TextView categoryHeading=text(categoryName,20);categoryHeading.setTypeface(null,Typeface.BOLD);categoryHeading.setContentDescription("Kategori Maktabah "+categoryName);categoryHeading.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);categoryHeading.setMaxLines(2);categoryHeading.setPadding(4,8,4,8);categoryHeading.setEllipsize(android.text.TextUtils.TruncateAt.END);page.addView(categoryHeading);page.setFocusableInTouchMode(true);
    Button add=button();add.setText("+ Tambah kitab");add.setTextColor(accent());add.setMinHeight(48);add.setContentDescription("Cipta kitab dalam kategori "+categoryName);page.addView(add);add.setOnClickListener(v->editBook(-1,categoryId,categoryName));
    EditText bookSearch=field();bookSearch.setSingleLine(true);bookSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);bookSearch.setSelectAllOnFocus(false);bookSearch.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);bookSearch.setHint("Cari kitab dalam kategori…");bookSearch.setContentDescription("Cari tajuk atau kandungan kitab");bookSearch.setTextSize(15);bookSearch.setMinHeight(48);page.addView(bookSearch);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setSmoothScrollingEnabled(true);LinearLayout items=new LinearLayout(this);items.setOrientation(1);items.setPadding(0,4,0,12);scroll.addView(items);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    bookSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){populateBooks(items,categoryId,categoryName,s.toString());}public void afterTextChanged(Editable e){}});
    populateBooks(items,categoryId,categoryName,"");
  }
  void populateBooks(LinearLayout items,int categoryId,String categoryName,String query){
    if(query==null)query="";final String[] queryText={query};items.removeAllViews();
    int visibleCount=0;
    try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,pinned FROM books WHERE category_id=? AND (title LIKE ? OR body LIKE ?) ORDER BY pinned DESC,updated DESC,id DESC",
      new String[]{""+categoryId,"%"+query+"%","%"+query+"%"})){
      while(c.moveToNext()){visibleCount++;int bookId=c.getInt(0);String heading=c.getString(1);
        Button item=button();item.setText((heading==null||heading.trim().isEmpty()?"Tanpa tajuk":heading));
        item.setContentDescription("Buka kitab "+(heading==null||heading.trim().isEmpty()?"Tanpa tajuk":heading));item.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);item.setAllCaps(false);item.setMaxLines(2);item.setEllipsize(android.text.TextUtils.TruncateAt.END);item.setMinHeight(48);LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);items.addView(line);line.addView(item,new LinearLayout.LayoutParams(0,-2,1));item.setOnClickListener(v->editBook(bookId,categoryId,categoryName));boolean pinned=c.getInt(2)==1;Button pin=button();pin.setText(pinned?"📌":"○");pin.setContentDescription((pinned?"Nyahpin kitab ":"Pin kitab ")+(heading==null||heading.trim().isEmpty()?"Tanpa tajuk":heading));pin.setMinHeight(48);pin.setAllCaps(false);line.addView(pin,new LinearLayout.LayoutParams(56,-2));pin.setOnClickListener(v->{db.getWritableDatabase().execSQL("UPDATE books SET pinned=? WHERE id=?",new Object[]{pinned?0:1,bookId});populateBooks(items,categoryId,categoryName,queryText[0]);Toast.makeText(this,pinned?"Kitab dinyahpin":"Kitab dipin",Toast.LENGTH_SHORT).show();});
      }
    }
    if(visibleCount==0)items.addView(text(query.trim().isEmpty()?"Belum ada kitab. Tekan + Tambah kitab untuk mula.":"Tiada kitab ditemui. Cuba kata kunci lain.",16));
  }

  void editBook(int id,int categoryId,String categoryName){
    flushPendingSave();editingBook=true;activeCategoryId=categoryId;activeCategoryName=categoryName;
    LinearLayout page=column();setContentView(page);enableKeyboardAwareEditor(page);
    Button back=button();back.setText("← Maktabah");back.setContentDescription("Kembali ke senarai kitab");back.setMinHeight(48);page.addView(back);
    saveIndicator=text("Disimpan secara automatik",12);saveIndicator.setContentDescription("Status simpanan automatik kitab");page.addView(saveIndicator);
    EditText title=field();title.setContentDescription("Tajuk kitab");title.setHint("Tajuk kitab");title.setSingleLine(true);title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});title.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);title.setTextSize(17);title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);title.setSelectAllOnFocus(false);page.addView(title);
    EditText body=field();body.setHint("Tulis kandungan kitab (Arab / Rumi)…");body.setTextSize(15);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    body.setGravity(Gravity.TOP|Gravity.START);body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    body.setMinLines(5);body.setSelectAllOnFocus(false);body.setContentDescription("Kandungan kitab");body.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);body.setHorizontallyScrolling(false);body.setTextIsSelectable(true);body.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    styleEditorPage(page,title,body,saveIndicator,back);
    final Button[] deleteButton={null};
    final int[] bookId={id};final boolean[] loading={true};final Runnable[] bookSaveHolder={null};
    if(id>=0)try(Cursor c=db.getReadableDatabase().rawQuery("SELECT title,body,body_html FROM books WHERE id=?",new String[]{""+id})){
      if(c.moveToFirst()){title.setText(c.getString(0));body.setText(c.getString(2)!=null&&!c.getString(2).isEmpty()?android.text.Html.fromHtml(c.getString(2),android.text.Html.FROM_HTML_MODE_COMPACT):c.getString(1));}
    }
    LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);
    page.addView(toolbar,new LinearLayout.LayoutParams(-1,-2));
    Button fontColor=button();fontColor.setText("A●");fontColor.setContentDescription("Warna tulisan kitab");toolbar.addView(fontColor,new LinearLayout.LayoutParams(0,-2,1));fontColor.setOnClickListener(v->chooseTextColor(body,()->{if(bookSaveHolder[0]!=null)scheduleSave(bookSaveHolder[0]);}));
    Button check=button();check.setText("☐");toolbar.addView(check,new LinearLayout.LayoutParams(0,-2,1));
    Button bold=button();bold.setText("B");bold.setContentDescription("Tebalkan teks dipilih");bold.setTypeface(null,Typeface.BOLD);toolbar.addView(bold,new LinearLayout.LayoutParams(0,-2,1));
    Button italic=button();italic.setText("I");italic.setContentDescription("Condongkan teks dipilih");italic.setTypeface(null,Typeface.ITALIC);toolbar.addView(italic,new LinearLayout.LayoutParams(0,-2,1));
    Button underline=button();underline.setText("U");underline.setContentDescription("Gariskan teks dipilih");toolbar.addView(underline,new LinearLayout.LayoutParams(0,-2,1));
    for(int i=0;i<toolbar.getChildCount();i++)styleEditorTool((Button)toolbar.getChildAt(i));
    underline.setOnClickListener(v->underlineSelection(body));
    check.setOnClickListener(v->body.getText().insert(Math.max(0,body.getSelectionStart()),"☐ "));
    bold.setOnClickListener(v->styleSelection(body,Typeface.BOLD));
    italic.setOnClickListener(v->styleSelection(body,Typeface.ITALIC));
    loading[0]=false;
    Runnable save=()->{if(loading[0])return;String t=title.getText().toString(),content=body.getText().toString();
      if(bookId[0]<0&&t.trim().isEmpty()&&content.trim().isEmpty()){setSaveStatus("Kitab kosong");return;}
      ContentValues cv=new ContentValues();cv.put("title",t);cv.put("body",content);cv.put("body_html",android.text.Html.toHtml(body.getText(),android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
      cv.put("category_id",categoryId);cv.put("updated",System.currentTimeMillis());
      try{if(bookId[0]<0){bookId[0]=(int)db.getWritableDatabase().insertOrThrow("books",null,cv);deleteButton[0].setEnabled(true);}
      else db.getWritableDatabase().update("books",cv,"id=?",new String[]{""+bookId[0]});setSaveStatus("✓ Disimpan");}catch(Exception e){android.util.Log.e("DinNote","Book save failed",e);setSaveStatus("Gagal disimpan");Toast.makeText(this,"Kitab gagal disimpan",Toast.LENGTH_LONG).show();}
    };
    bookSaveHolder[0]=save;
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
  void chooseTextColor(EditText editor,Runnable onChanged){
    int[] colors={0xFF1F2328,0xFFD1242F,0xFFB45309,0xFF15803D,0xFF0969DA,0xFF7C3AED,0xFFBE185D,0xFF0F766E,0xFF475569,0xFFFFFFFF,0xFFEA580C,0xFF4338CA,0xFFB8860B,0xFF800020,0xFF556B2F,0xFF172554,0xFF0284C7,0xFF65A30D,0xFF9333EA,0xFFFB7185};
    String[] names={"Hitam","Merah","Coklat","Hijau","Biru","Ungu","Merah jambu","Turquoise","Kelabu","Putih","Oren","Indigo","Emas","Marun","Zaitun","Navy","Biru langit","Lime","Lavender","Coral"};
    int start=editor.getSelectionStart(),end=editor.getSelectionEnd();
    if(start<0||end<=start){Toast.makeText(this,"Pilih teks yang hendak diwarnakan dahulu",Toast.LENGTH_SHORT).show();return;}
    new AlertDialog.Builder(this).setTitle("Warna tulisan")
      .setItems(names,(dialog,index)->{
        editor.getText().setSpan(new android.text.style.ForegroundColorSpan(colors[index]),start,end,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(onChanged!=null)onChanged.run();
      }).setNegativeButton("Batal",null).show();
  }
  void underlineSelection(EditText edit){int a=edit.getSelectionStart(),b=edit.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}edit.getText().setSpan(new android.text.style.UnderlineSpan(),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}
  void styleSelection(EditText edit,int style){int a=edit.getSelectionStart(),b=edit.getSelectionEnd();if(a<0||b<=a){Toast.makeText(this,"Pilih teks dahulu",Toast.LENGTH_SHORT).show();return;}SpannableStringBuilder t=new SpannableStringBuilder(edit.getText());t.setSpan(new StyleSpan(style),a,b,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);edit.setText(t);edit.setSelection(b);}
  @Override public void onBackPressed(){flushPendingSave();if(editingBook)showBooks(activeCategoryId,activeCategoryName);else showHome();}
  static class DB extends SQLiteOpenHelper {
    DB(Context c){super(c,"dinnote.db",null,7);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL,color INTEGER NOT NULL DEFAULT -1,category_id INTEGER NOT NULL DEFAULT -1,pinned INTEGER NOT NULL DEFAULT 0,body_html TEXT NOT NULL DEFAULT '')");createLibrary(db);createNoteCategories(db);seedDefaultCategories(db);}
    static void createLibrary(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY AUTOINCREMENT,category_id INTEGER NOT NULL,title TEXT NOT NULL DEFAULT '',body TEXT NOT NULL DEFAULT '',updated INTEGER NOT NULL,pinned INTEGER NOT NULL DEFAULT 0,body_html TEXT NOT NULL DEFAULT '')");}
    static void createNoteCategories(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS note_categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");}
    static void seedDefaultCategories(SQLiteDatabase db){
      // Only seed during database creation or version migration; user changes remain editable.
      for(String name:new String[]{"Al-Quran & Tafsir","Hadis","Akidah & Tauhid","Fiqh","Tasawuf & Akhlak","Sirah & Sejarah Islam","Bahasa Arab","Lain-lain"}){
        db.execSQL("INSERT INTO categories(name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name=?)",new Object[]{name,name});
      }
      db.execSQL("INSERT INTO note_categories(name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM note_categories WHERE name=?)",new Object[]{"Peribadi","Peribadi"});
    }
    @Override public void onOpen(SQLiteDatabase database){super.onOpen(database);createLibrary(database);createNoteCategories(database);}
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2) { try { db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT -1"); } catch (android.database.sqlite.SQLiteException ignored) { /* Column may already exist in older builds. */ } } if(oldVersion<3)createLibrary(db);if(oldVersion<4){createNoteCategories(db);try{db.execSQL("ALTER TABLE notes ADD COLUMN category_id INTEGER NOT NULL DEFAULT -1");}catch(android.database.sqlite.SQLiteException ignored){}} if(oldVersion<5){createLibrary(db);createNoteCategories(db);seedDefaultCategories(db);} if(oldVersion<6){try{db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");}catch(android.database.sqlite.SQLiteException ignored){}try{db.execSQL("ALTER TABLE books ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");}catch(android.database.sqlite.SQLiteException ignored){}} if(oldVersion<7){db.execSQL("ALTER TABLE notes ADD COLUMN body_html TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE books ADD COLUMN body_html TEXT NOT NULL DEFAULT ''");} }
  }
}
