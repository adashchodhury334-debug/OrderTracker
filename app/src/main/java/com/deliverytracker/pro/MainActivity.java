package com.deliverytracker.pro;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private SQLiteDatabase db;
    private final ArrayList<String[]> orders = new ArrayList<String[]>();
    private BaseAdapter adapter;
    private LinearLayout secTracker, secPerf, perfCards;
    private ScrollView scrollPerf;
    private Button bTrack, bPerf, bD, bW, bM;
    private TextView txtCount, txtHub;
    private String mode = "daily";
    private static final String CSV = "https://docs.google.com/spreadsheets/d/1Dul38iNZ_eNmABVuYVWhrUg9F_xVMvaVvQvLIXlySj4/export?format=csv";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = openOrCreateDatabase("TrackerDB.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS orders (t TEXT, o TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS perf (name TEXT, mob TEXT, ofd INT, del INT, ofp INT, pik INT, dt TEXT);");
        initUI();
        updateCount();
    }

    private void initUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0d1117"));

        LinearLayout top = new LinearLayout(this);
        top.setBackgroundColor(Color.parseColor("#161b22"));
        top.setPadding(24, 20, 24, 20);
        TextView title = new TextView(this);
        title.setText("⚡ Delivery Tracker Pro");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnAdmin = new Button(this);
        btnAdmin.setText("🔒 Admin");
        btnAdmin.setTextColor(Color.parseColor("#00E676"));
        btnAdmin.setBackgroundColor(Color.parseColor("#21262d"));
        btnAdmin.setOnClickListener(v -> showAdmin());
        top.addView(btnAdmin);
        root.addView(top);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackgroundColor(Color.parseColor("#161b22"));
        tabs.setPadding(12, 10, 12, 10);
        bTrack = new Button(this); bTrack.setText("🔍 Tracker");
        bTrack.setBackgroundColor(Color.parseColor("#00E676")); bTrack.setTextColor(Color.BLACK);
        bPerf = new Button(this); bPerf.setText("📊 Performance");
        bPerf.setBackgroundColor(Color.parseColor("#21262d")); bPerf.setTextColor(Color.parseColor("#8b949e"));
        LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(0, -2, 1f);
        lpT.setMargins(6, 0, 6, 0);
        tabs.addView(bTrack, lpT);
        tabs.addView(bPerf, new LinearLayout.LayoutParams(lpT));
        root.addView(tabs);

        FrameLayout body = new FrameLayout(this);
        body.setPadding(20, 20, 20, 20);
        root.addView(body, new LinearLayout.LayoutParams(-1, -1));

        secTracker = new LinearLayout(this);
        secTracker.setOrientation(LinearLayout.VERTICAL);
        EditText search = new EditText(this);
        search.setHint("Enter Tracking ID (FMPC...)...");
        search.setHintTextColor(Color.parseColor("#8b949e"));
        search.setTextColor(Color.WHITE);
        search.setBackgroundColor(Color.parseColor("#161b22"));
        search.setPadding(24, 24, 24, 24);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int b, int c, int a) {}
            public void onTextChanged(CharSequence s, int b, int c, int a) { searchOrders(s.toString().trim()); }
            public void afterTextChanged(Editable s) {}
        });
        secTracker.addView(search);

        txtCount = new TextView(this);
        txtCount.setTextColor(Color.parseColor("#8b949e"));
        txtCount.setPadding(0, 20, 0, 16);
        secTracker.addView(txtCount);

        ListView list = new ListView(this);
        adapter = new BaseAdapter() {
            public int getCount() { return orders.size(); }
            public Object getItem(int i) { return orders.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup p) {
                LinearLayout l = new LinearLayout(MainActivity.this);
                l.setOrientation(LinearLayout.VERTICAL);
                l.setPadding(20, 16, 20, 16);
                l.setBackgroundColor(Color.parseColor("#161b22"));
                String[] item = orders.get(i);
                TextView t1 = new TextView(MainActivity.this);
                t1.setText("Track ID: " + item[0]); t1.setTextColor(Color.parseColor("#8b949e"));
                TextView t2 = new TextView(MainActivity.this);
                t2.setText("Order ID: " + item[1] + " (Tap to Copy)");
                t2.setTextColor(Color.parseColor("#00E676"));
                t2.setTypeface(Typeface.DEFAULT_BOLD);
                l.addView(t1); l.addView(t2);
                l.setOnClickListener(vw -> {
                    ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cb.setPrimaryClip(ClipData.newPlainText("ID", item[1]));
                    Toast.makeText(MainActivity.this, "Copied: " + item[1], Toast.LENGTH_SHORT).show();
                });
                return l;
            }
        };
        list.setAdapter(adapter);
        secTracker.addView(list, new LinearLayout.LayoutParams(-1, -1));
        body.addView(secTracker);

        scrollPerf = new ScrollView(this);
        secPerf = new LinearLayout(this);
        secPerf.setOrientation(LinearLayout.VERTICAL);
        scrollPerf.addView(secPerf);
        scrollPerf.setVisibility(View.GONE);
        body.addView(scrollPerf);

        LinearLayout f = new LinearLayout(this);
        bD = makeFilter("📅 Daily", "daily");
        bW = makeFilter("📆 Weekly", "weekly");
        bM = makeFilter("🗓️ Monthly", "monthly");
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(0, -2, 1f);
        lpF.setMargins(4, 0, 4, 16);
        f.addView(bD, lpF); f.addView(bW, new LinearLayout.LayoutParams(lpF)); f.addView(bM, new LinearLayout.LayoutParams(lpF));
        secPerf.addView(f);

        LinearLayout hub = new LinearLayout(this);
        hub.setOrientation(LinearLayout.VERTICAL);
        hub.setBackgroundColor(Color.parseColor("#1c2331"));
        hub.setPadding(24, 24, 24, 24);
        TextView ht = new TextView(this);
        ht.setText("🏢 MALBAZARHUB_NJP | 🎯 Target: 92.0%");
        ht.setTextColor(Color.parseColor("#00E676"));
        ht.setTypeface(Typeface.DEFAULT_BOLD);
        hub.addView(ht);
        txtHub = new TextView(this);
        txtHub.setTextColor(Color.WHITE);
        txtHub.setPadding(0, 12, 0, 0);
        hub.addView(txtHub);
        secPerf.addView(hub);

        TextView at = new TextView(this);
        at.setText("👥 Delivery Agents Report (Low to High)");
        at.setTextColor(Color.parseColor("#8b949e"));
        at.setPadding(0, 24, 0, 12);
        at.setTypeface(Typeface.DEFAULT_BOLD);
        secPerf.addView(at);

        perfCards = new LinearLayout(this);
        perfCards.setOrientation(LinearLayout.VERTICAL);
        secPerf.addView(perfCards);

        bTrack.setOnClickListener(v -> {
            secTracker.setVisibility(View.VISIBLE); scrollPerf.setVisibility(View.GONE);
            bTrack.setBackgroundColor(Color.parseColor("#00E676")); bTrack.setTextColor(Color.BLACK);
            bPerf.setBackgroundColor(Color.parseColor("#21262d")); bPerf.setTextColor(Color.parseColor("#8b949e"));
        });
        bPerf.setOnClickListener(v -> {
            secTracker.setVisibility(View.GONE); scrollPerf.setVisibility(View.VISIBLE);
            bPerf.setBackgroundColor(Color.parseColor("#00E676")); bPerf.setTextColor(Color.BLACK);
            bTrack.setBackgroundColor(Color.parseColor("#21262d")); bTrack.setTextColor(Color.parseColor("#8b949e"));
            loadPerf();
        });

        setContentView(root);
    }

    private Button makeFilter(String txt, String m) {
        Button b = new Button(this);
        b.setText(txt);
        b.setBackgroundColor(m.equals(mode) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
        b.setTextColor(m.equals(mode) ? Color.WHITE : Color.parseColor("#8b949e"));
        b.setOnClickListener(v -> {
            mode = m;
            bD.setBackgroundColor("daily".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
            bW.setBackgroundColor("weekly".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
            bM.setBackgroundColor("monthly".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
            loadPerf();
        });
        return b;
    }

    private String getCycleDate() {
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) < 9) c.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
    }

    private void updateCount() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM orders", null);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        txtCount.setText("📦 Orders List (Active: " + n + ")");
    }

    private void searchOrders(String q) {
        orders.clear();
        if (!q.isEmpty()) {
            Cursor c = db.rawQuery("SELECT t, o FROM orders WHERE t LIKE ? LIMIT 40", new String[]{"%" + q + "%"});
            while (c.moveToNext()) orders.add(new String[]{c.getString(0), c.getString(1)});
            c.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void loadPerf() {
        perfCards.removeAllViews();
        String cond = "daily".equals(mode) ? " WHERE dt = (SELECT MAX(dt) FROM perf) " : ("weekly".equals(mode) ? " WHERE dt >= date('now','localtime','-7 days') " : " WHERE dt >= date('now','localtime','-30 days') ");
        Cursor hc = db.rawQuery("SELECT SUM(ofd), SUM(del), SUM(ofp), SUM(pik) FROM perf" + cond, null);
        if (hc.moveToFirst()) {
            int o = hc.getInt(0), d = hc.getInt(1), op = hc.getInt(2), p = hc.getInt(3);
            int dnp = o + op, dnpc = d + p;
            double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
            txtHub.setText("OFD: " + o + " | DEL: " + d + " | OFP: " + op + " | PIK: " + p + "\nDNP: " + dnp + " | DNPC: " + dnpc + " | Actual Conv: " + String.format(Locale.US, "%.1f%%", r));
        } else { txtHub.setText("No data synced yet."); }
        hc.close();

        Cursor ac = db.rawQuery("SELECT name, mob, SUM(ofd), SUM(del), SUM(ofp), SUM(pik) FROM perf " + cond + " GROUP BY name, mob", null);
        ArrayList<String[]> list = new ArrayList<String[]>();
        while (ac.moveToNext()) {
            int o = ac.getInt(2), d = ac.getInt(3), op = ac.getInt(4), p = ac.getInt(5);
            int dnp = o + op, dnpc = d + p;
            double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
            list.add(new String[]{ac.getString(0), ac.getString(1), String.valueOf(o), String.valueOf(d), String.valueOf(op), String.valueOf(p), String.valueOf(dnp), String.valueOf(dnpc), String.format(Locale.US, "%.1f", r), String.valueOf(r)});
        }
        ac.close();

        Collections.sort(list, (a, b) -> Double.compare(Double.parseDouble(a[9]), Double.parseDouble(b[9])));

        for (String[] ag : list) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#161b22"));
            card.setPadding(20, 16, 20, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, 12);
            card.setLayoutParams(lp);

            TextView n = new TextView(this);
            n.setText("👤 " + ag[0] + " (" + ag[1] + ")");
            n.setTextColor(Color.parseColor("#00E676"));
            n.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(n);

            TextView s = new TextView(this);
            s.setText("OFD: " + ag[2] + " | DEL: " + ag[3] + " | OFP: " + ag[4] + " | PIK: " + ag[5] + "\nDNP: " + ag[6] + " | DNPC: " + ag[7] + " | Conv: " + ag[8] + "%");
            s.setTextColor(Color.WHITE);
            s.setTextSize(12f);
            card.addView(s);
            perfCards.addView(card);
        }
    }

    private void showAdmin() {
        EditText in = new EditText(this);
        in.setHint("PIN...");
        in.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("🔐 Admin Login").setView(in)
            .setPositiveButton("Verify", (d, w) -> {
                if ("9547927698".equals(in.getText().toString().trim())) openSync();
                else Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void openSync() {
        new AlertDialog.Builder(this).setTitle("⚡ Sync Options")
            .setPositiveButton("Sync Now", (d, w) -> new Thread(() -> syncSheet()).start())
            .setNegativeButton("Clear Data", (d, w) -> {
                db.delete("orders", null, null);
                db.delete("perf", null, null);
                runOnUiThread(() -> { updateCount(); searchOrders(""); Toast.makeText(this, "Cleared!", Toast.LENGTH_SHORT).show(); });
            }).show();
    }

    private void syncSheet() {
        try {
            runOnUiThread(() -> Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show());
            String dt = getCycleDate();
            HttpURLConnection conn = (HttpURLConnection) new URL(CSV).openConnection();
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            db.beginTransaction();
            int count = 0;
            try {
                db.delete("orders", null, null);
                db.delete("perf", "dt = ?", new String[]{dt});
                String l; boolean head = true;
                while ((l = r.readLine()) != null) {
                    if (head) { head = false; continue; }
                    String[] p = l.split(",", -1);
                    if (p.length < 2) continue;
                    String o = p[0].replace("\"", "").trim(), t = p[1].replace("\"", "").trim();
                    String name = p.length > 2 ? p[2].replace("\"", "").trim() : "";
                    String mob = p.length > 3 ? p[3].replace("\"", "").trim() : "";
                    if (!t.isEmpty()) {
                        ContentValues cv = new ContentValues();
                        cv.put("t", t); cv.put("o", o);
                        db.insert("orders", null, cv);
                        count++;
                    }
                    if (!name.isEmpty() && !name.equalsIgnoreCase("NAME")) {
                        ContentValues cv = new ContentValues();
                        cv.put("name", name); cv.put("mob", mob);
                        cv.put("ofd", p.length > 4 ? pInt(p[4]) : 0);
                        cv.put("del", p.length > 5 ? pInt(p[5]) : 0);
                        cv.put("ofp", p.length > 6 ? pInt(p[6]) : 0);
                        cv.put("pik", p.length > 7 ? pInt(p[7]) : 0);
                        cv.put("dt", dt);
                        db.insert("perf", null, cv);
                    }
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            int finalCount = count;
            runOnUiThread(() -> {
                Toast.makeText(this, "Synced " + finalCount + " orders!", Toast.LENGTH_LONG).show();
                updateCount();
                if (scrollPerf.getVisibility() == View.VISIBLE) loadPerf();
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Sync Failed!", Toast.LENGTH_SHORT).show());
        }
    }

    private int pInt(String s) {
        try { return Integer.parseInt(s.replace("\"", "").trim()); } catch (Exception e) { return 0; }
    }
}
