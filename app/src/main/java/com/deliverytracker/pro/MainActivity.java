package com.deliverytracker.pro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    ArrayList<String[]> list = new ArrayList<String[]>();
    BaseAdapter adp;
    LinearLayout vTrack, vPerf, vCards;
    ScrollView sPerf;
    Button bT, bP, b1, b2, b3;
    TextView tCnt, tHub;
    String mode = "daily";
    String CSV = "https://docs.google.com/spreadsheets/d/1Dul38iNZ_eNmABVuYVWhrUg9F_xVMvaVvQvLIXlySj4/export?format=csv";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = openOrCreateDatabase("App.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS ord (t TEXT, o TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS prf (name TEXT, mob TEXT, ofd INT, del INT, ofp INT, pik INT, dt TEXT);");
        build();
        count();
    }

    void build() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(Color.parseColor("#0d1117"));

        LinearLayout top = new LinearLayout(this);
        top.setBackgroundColor(Color.parseColor("#161b22"));
        top.setPadding(24, 20, 24, 20);
        TextView h = new TextView(this);
        h.setText("⚡ Delivery Tracker Pro");
        h.setTextColor(Color.parseColor("#00E676"));
        h.setTextSize(18f);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(h, new LinearLayout.LayoutParams(0, -2, 1f));

        Button bAdm = new Button(this);
        bAdm.setText("🔒 Admin");
        bAdm.setTextColor(Color.parseColor("#00E676"));
        bAdm.setBackgroundColor(Color.parseColor("#21262d"));
        bAdm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { adm(); }
        });
        top.addView(bAdm);
        r.addView(top);

        LinearLayout tb = new LinearLayout(this);
        tb.setBackgroundColor(Color.parseColor("#161b22"));
        tb.setPadding(12, 10, 12, 10);
        bT = new Button(this); bT.setText("🔍 Tracker");
        bT.setBackgroundColor(Color.parseColor("#00E676")); bT.setTextColor(Color.BLACK);
        bP = new Button(this); bP.setText("📊 Performance");
        bP.setBackgroundColor(Color.parseColor("#21262d")); bP.setTextColor(Color.parseColor("#8b949e"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(6, 0, 6, 0);
        tb.addView(bT, lp); tb.addView(bP, new LinearLayout.LayoutParams(lp));
        r.addView(tb);

        FrameLayout bdy = new FrameLayout(this);
        bdy.setPadding(20, 20, 20, 20);
        r.addView(bdy, new LinearLayout.LayoutParams(-1, -1));

        vTrack = new LinearLayout(this);
        vTrack.setOrientation(LinearLayout.VERTICAL);
        EditText src = new EditText(this);
        src.setHint("Enter Tracking ID...");
        src.setHintTextColor(Color.parseColor("#8b949e"));
        src.setTextColor(Color.WHITE);
        src.setBackgroundColor(Color.parseColor("#161b22"));
        src.setPadding(24, 24, 24, 24);
        src.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            public void onTextChanged(CharSequence s, int i, int i1, int i2) { find(s.toString().trim()); }
            public void afterTextChanged(Editable s) {}
        });
        vTrack.addView(src);

        tCnt = new TextView(this);
        tCnt.setTextColor(Color.parseColor("#8b949e"));
        tCnt.setPadding(0, 20, 0, 16);
        vTrack.addView(tCnt);

        ListView lv = new ListView(this);
        adp = new BaseAdapter() {
            public int getCount() { return list.size(); }
            public Object getItem(int i) { return list.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup p) {
                LinearLayout l = new LinearLayout(MainActivity.this);
                l.setOrientation(LinearLayout.VERTICAL);
                l.setPadding(20, 16, 20, 16);
                l.setBackgroundColor(Color.parseColor("#161b22"));
                final String[] itm = list.get(i);
                TextView t1 = new TextView(MainActivity.this);
                t1.setText("Track ID: " + itm[0]); t1.setTextColor(Color.parseColor("#8b949e"));
                TextView t2 = new TextView(MainActivity.this);
                t2.setText("Order ID: " + itm[1] + " (Tap to Copy)");
                t2.setTextColor(Color.parseColor("#00E676"));
                t2.setTypeface(Typeface.DEFAULT_BOLD);
                l.addView(t1); l.addView(t2);
                l.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View vw) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("ID", itm[1]));
                        Toast.makeText(MainActivity.this, "Copied: " + itm[1], Toast.LENGTH_SHORT).show();
                    }
                });
                return l;
            }
        };
        lv.setAdapter(adp);
        vTrack.addView(lv, new LinearLayout.LayoutParams(-1, -1));
        bdy.addView(vTrack);

        sPerf = new ScrollView(this);
        vPerf = new LinearLayout(this);
        vPerf.setOrientation(LinearLayout.VERTICAL);
        sPerf.addView(vPerf);
        sPerf.setVisibility(View.GONE);
        bdy.addView(sPerf);

        LinearLayout flt = new LinearLayout(this);
        b1 = mkBtn("📅 Daily", "daily");
        b2 = mkBtn("📆 Weekly", "weekly");
        b3 = mkBtn("🗓️ Monthly", "monthly");
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(0, -2, 1f);
        lpF.setMargins(4, 0, 4, 16);
        flt.addView(b1, lpF); flt.addView(b2, new LinearLayout.LayoutParams(lpF)); flt.addView(b3, new LinearLayout.LayoutParams(lpF));
        vPerf.addView(flt);

        LinearLayout hb = new LinearLayout(this);
        hb.setOrientation(LinearLayout.VERTICAL);
        hb.setBackgroundColor(Color.parseColor("#1c2331"));
        hb.setPadding(24, 24, 24, 24);
        TextView hbT = new TextView(this);
        hbT.setText("🏢 MALBAZARHUB_NJP | 🎯 Target: 92.0%");
        hbT.setTextColor(Color.parseColor("#00E676"));
        hbT.setTypeface(Typeface.DEFAULT_BOLD);
        hb.addView(hbT);
        tHub = new TextView(this);
        tHub.setTextColor(Color.WHITE);
        tHub.setPadding(0, 12, 0, 0);
        hb.addView(tHub);
        vPerf.addView(hb);

        TextView at = new TextView(this);
        at.setText("👥 Delivery Agents Report");
        at.setTextColor(Color.parseColor("#8b949e"));
        at.setPadding(0, 24, 0, 12);
        at.setTypeface(Typeface.DEFAULT_BOLD);
        vPerf.addView(at);

        vCards = new LinearLayout(this);
        vCards.setOrientation(LinearLayout.VERTICAL);
        vPerf.addView(vCards);

        bT.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vTrack.setVisibility(View.VISIBLE); sPerf.setVisibility(View.GONE);
                bT.setBackgroundColor(Color.parseColor("#00E676")); bT.setTextColor(Color.BLACK);
                bP.setBackgroundColor(Color.parseColor("#21262d")); bP.setTextColor(Color.parseColor("#8b949e"));
            }
        });
        bP.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vTrack.setVisibility(View.GONE); sPerf.setVisibility(View.VISIBLE);
                bP.setBackgroundColor(Color.parseColor("#00E676")); bP.setTextColor(Color.BLACK);
                bT.setBackgroundColor(Color.parseColor("#21262d")); bT.setTextColor(Color.parseColor("#8b949e"));
                loadP();
            }
        });

        setContentView(r);
    }

    Button mkBtn(String title, final String m) {
        Button b = new Button(this);
        b.setText(title);
        b.setBackgroundColor(m.equals(mode) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
        b.setTextColor(m.equals(mode) ? Color.WHITE : Color.parseColor("#8b949e"));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode = m;
                b1.setBackgroundColor("daily".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
                b2.setBackgroundColor("weekly".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
                b3.setBackgroundColor("monthly".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
                loadP();
            }
        });
        return b;
    }

    String getDt() {
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) < 9) c.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
    }

    void count() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM ord", null);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        tCnt.setText("📦 Orders List (Active: " + n + ")");
    }

    void find(String q) {
        list.clear();
        if (!q.isEmpty()) {
            Cursor c = db.rawQuery("SELECT t, o FROM ord WHERE t LIKE ? LIMIT 40", new String[]{"%" + q + "%"});
            while (c.moveToNext()) list.add(new String[]{c.getString(0), c.getString(1)});
            c.close();
        }
        adp.notifyDataSetChanged();
    }

    void loadP() {
        vCards.removeAllViews();
        String cond = "daily".equals(mode) ? " WHERE dt = (SELECT MAX(dt) FROM prf) " : ("weekly".equals(mode) ? " WHERE dt >= date('now','localtime','-7 days') " : " WHERE dt >= date('now','localtime','-30 days') ");
        Cursor hc = db.rawQuery("SELECT SUM(ofd), SUM(del), SUM(ofp), SUM(pik) FROM prf" + cond, null);
        if (hc.moveToFirst()) {
            int o = hc.getInt(0), d = hc.getInt(1), op = hc.getInt(2), p = hc.getInt(3);
            int dnp = o + op, dnpc = d + p;
            double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
            tHub.setText("OFD: " + o + " | DEL: " + d + " | OFP: " + op + " | PIK: " + p + "\nDNP: " + dnp + " | DNPC: " + dnpc + " | Actual Conv: " + String.format(Locale.US, "%.1f%%", r));
        } else { tHub.setText("No data synced yet."); }
        hc.close();

        Cursor ac = db.rawQuery("SELECT name, mob, SUM(ofd), SUM(del), SUM(ofp), SUM(pik) FROM prf " + cond + " GROUP BY name, mob", null);
        ArrayList<String[]> agList = new ArrayList<String[]>();
        while (ac.moveToNext()) {
            int o = ac.getInt(2), d = ac.getInt(3), op = ac.getInt(4), p = ac.getInt(5);
            int dnp = o + op, dnpc = d + p;
            double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
            agList.add(new String[]{ac.getString(0), ac.getString(1), String.valueOf(o), String.valueOf(d), String.valueOf(op), String.valueOf(p), String.valueOf(dnp), String.valueOf(dnpc), String.format(Locale.US, "%.1f", r), String.valueOf(r)});
        }
        ac.close();

        Collections.sort(agList, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return Double.compare(Double.parseDouble(a[9]), Double.parseDouble(b[9]));
            }
        });

        for (String[] ag : agList) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setBackgroundColor(Color.parseColor("#161b22"));
            c.setPadding(20, 16, 20, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, 12);
            c.setLayoutParams(lp);

            TextView n = new TextView(this);
            n.setText("👤 " + ag[0] + " (" + ag[1] + ")");
            n.setTextColor(Color.parseColor("#00E676"));
            n.setTypeface(Typeface.DEFAULT_BOLD);
            c.addView(n);

            TextView s = new TextView(this);
            s.setText("OFD: " + ag[2] + " | DEL: " + ag[3] + " | OFP: " + ag[4] + " | PIK: " + ag[5] + "\nDNP: " + ag[6] + " | DNPC: " + ag[7] + " | Conv: " + ag[8] + "%");
            s.setTextColor(Color.WHITE);
            s.setTextSize(12f);
            c.addView(s);
            vCards.addView(c);
        }
    }

    void adm() {
        final EditText in = new EditText(this);
        in.setHint("PIN...");
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("🔐 Admin Login").setView(in)
            .setPositiveButton("Verify", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    if ("9547927698".equals(in.getText().toString().trim())) syncDlg();
                    else Toast.makeText(MainActivity.this, "Wrong PIN!", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    void syncDlg() {
        new AlertDialog.Builder(this).setTitle("⚡ Sync Options")
            .setPositiveButton("Sync Now", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    new Thread(new Runnable() {
                        public void run() { doSync(); }
                    }).start();
                }
            })
            .setNegativeButton("Clear Data", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    db.delete("ord", null, null);
                    db.delete("prf", null, null);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            count(); find("");
                            Toast.makeText(MainActivity.this, "Cleared!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).show();
    }

    void doSync() {
        try {
            runOnUiThread(new Runnable() {
                public void run() { Toast.makeText(MainActivity.this, "Syncing...", Toast.LENGTH_SHORT).show(); }
            });
            String dt = getDt();
            HttpURLConnection conn = (HttpURLConnection) new URL(CSV).openConnection();
            conn.setConnectTimeout(15000);
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            db.beginTransaction();
            int count = 0;
            try {
                db.delete("ord", null, null);
                db.delete("prf", "dt = ?", new String[]{dt});
                String l; boolean hd = true;
                while ((l = r.readLine()) != null) {
                    if (hd) { hd = false; continue; }
                    String[] p = l.split(",", -1);
                    if (p.length < 2) continue;
                    String o = p[0].replace("\"", "").trim();
                    String t = p[1].replace("\"", "").trim();
                    String name = p.length > 2 ? p[2].replace("\"", "").trim() : "";
                    String mob = p.length > 3 ? p[3].replace("\"", "").trim() : "";
                    if (!t.isEmpty()) {
                        ContentValues cv = new ContentValues();
                        cv.put("t", t); cv.put("o", o);
                        db.insert("ord", null, cv);
                        count++;
                    }
                    if (!name.isEmpty() && !name.equalsIgnoreCase("NAME")) {
                        ContentValues cv = new ContentValues();
                        cv.put("name", name); cv.put("mob", mob);
                        cv.put("ofd", p.length > 4 ? pNum(p[4]) : 0);
                        cv.put("del", p.length > 5 ? pNum(p[5]) : 0);
                        cv.put("ofp", p.length > 6 ? pNum(p[6]) : 0);
                        cv.put("pik", p.length > 7 ? pNum(p[7]) : 0);
                        cv.put("dt", dt);
                        db.insert("prf", null, cv);
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            final int finCount = count;
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Synced " + finCount + " items!", Toast.LENGTH_LONG).show();
                    count();
                    if (sPerf.getVisibility() == View.VISIBLE) loadP();
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                public void run() { Toast.makeText(MainActivity.this, "Sync Failed!", Toast.LENGTH_SHORT).show(); }
            });
        }
    }

    int pNum(String s) {
        try { return Integer.parseInt(s.replace("\"", "").trim()); } catch (Exception e) { return 0; }
    }
                      }
