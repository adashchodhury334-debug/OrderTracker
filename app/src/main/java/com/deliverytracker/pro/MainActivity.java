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
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
    ArrayList<String[]> orderList = new ArrayList<String[]>();
    BaseAdapter adapter;
    LinearLayout secTracker, secPerf, cardsContainer;
    ScrollView scrollPerf;
    Button btnTracker, btnPerf, btnDaily, btnWeekly, btnMonthly, btnSortToggle;
    TextView txtCount, txtHubStats;
    String currentFilter = "daily";
    boolean sortTopFirst = true;
    static final String CSV_URL = "https://docs.google.com/spreadsheets/d/1Dul38iNZ_eNmABVuYVWhrUg9F_xVMvaVvQvLIXlySj4/export?format=csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        db = openOrCreateDatabase("DeliveryPro.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS orders (tracking_id TEXT, order_id TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS performance (name TEXT, mobile TEXT, ofd INT, del INT, ofp INT, pik INT, entry_date TEXT);");
        buildUI();
        refreshCount();
    }

    GradientDrawable makeBox(int bgColor, int strokeColor, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(radius);
        if (strokeColor != 0) gd.setStroke(2, strokeColor);
        return gd;
    }

    void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0e17"));

        // Header Top Bar
        LinearLayout top = new LinearLayout(this);
        top.setBackgroundColor(Color.parseColor("#131c2e"));
        top.setPadding(28, 24, 28, 24);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("⚡ Delivery Tracker Pro");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnAdmin = new Button(this);
        btnAdmin.setText("🔒 Admin");
        btnAdmin.setTextColor(Color.parseColor("#00E676"));
        btnAdmin.setBackground(makeBox(Color.parseColor("#1f2d47"), Color.parseColor("#00E676"), 12));
        btnAdmin.setPadding(24, 10, 24, 10);
        btnAdmin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAdminDialog(); }
        });
        top.addView(btnAdmin);
        root.addView(top);

        // Navigation Tabs
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackgroundColor(Color.parseColor("#101726"));
        tabs.setPadding(16, 12, 16, 12);

        btnTracker = new Button(this);
        btnTracker.setText("🔍 Tracker");
        btnTracker.setBackground(makeBox(Color.parseColor("#00E676"), 0, 14));
        btnTracker.setTextColor(Color.BLACK);
        btnTracker.setTypeface(Typeface.DEFAULT_BOLD);

        btnPerf = new Button(this);
        btnPerf.setText("📊 Performance");
        btnPerf.setBackground(makeBox(Color.parseColor("#1a2333"), 0, 14));
        btnPerf.setTextColor(Color.parseColor("#8fa0bc"));
        btnPerf.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams lpTab = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpTab.setMargins(8, 0, 8, 0);
        tabs.addView(btnTracker, lpTab);
        tabs.addView(btnPerf, new LinearLayout.LayoutParams(lpTab));
        root.addView(tabs);

        FrameLayout body = new FrameLayout(this);
        body.setPadding(24, 20, 24, 20);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Tracker Screen
        secTracker = new LinearLayout(this);
        secTracker.setOrientation(LinearLayout.VERTICAL);

        EditText search = new EditText(this);
        search.setHint("Search Tracking ID / Order ID (eg: 848, FMPC...)");
        search.setHintTextColor(Color.parseColor("#657795"));
        search.setTextColor(Color.WHITE);
        search.setBackground(makeBox(Color.parseColor("#141d2d"), Color.parseColor("#2a3b5c"), 16));
        search.setPadding(28, 24, 28, 24);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            public void onTextChanged(CharSequence s, int i, int i1, int i2) { searchOrders(s.toString().trim()); }
            public void afterTextChanged(Editable s) {}
        });
        secTracker.addView(search);

        txtCount = new TextView(this);
        txtCount.setTextColor(Color.parseColor("#8fa0bc"));
        txtCount.setPadding(4, 20, 4, 16);
        txtCount.setTypeface(Typeface.DEFAULT_BOLD);
        secTracker.addView(txtCount);

        ListView lv = new ListView(this);
        lv.setDividerHeight(12);
        lv.setDivider(null);
        adapter = new BaseAdapter() {
            public int getCount() { return orderList.size(); }
            public Object getItem(int i) { return orderList.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup p) {
                LinearLayout card = new LinearLayout(MainActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(24, 20, 24, 20);
                card.setBackground(makeBox(Color.parseColor("#141d2d"), Color.parseColor("#23334d"), 16));

                final String[] itm = orderList.get(i);
                final String tId = itm[0];
                final String oId = itm[1];

                TextView t1 = new TextView(MainActivity.this);
                t1.setText("📦 Tracking ID: " + tId);
                t1.setTextColor(Color.parseColor("#00E676"));
                t1.setTypeface(Typeface.DEFAULT_BOLD);
                t1.setTextSize(15f);

                TextView t2 = new TextView(MainActivity.this);
                t2.setText("🛒 Order ID: " + oId + "  📋 (Tap to Copy)");
                t2.setTextColor(Color.parseColor("#64B5F6"));
                t2.setTextSize(13f);
                t2.setPadding(0, 8, 0, 0);

                card.addView(t1);
                card.addView(t2);

                card.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View vw) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("Order ID", oId));
                        Toast.makeText(MainActivity.this, "✅ Order ID Copied: " + oId, Toast.LENGTH_SHORT).show();
                    }
                });
                return card;
            }
        };
        lv.setAdapter(adapter);
        secTracker.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        body.addView(secTracker);

        // 2. Performance Screen
        scrollPerf = new ScrollView(this);
        secPerf = new LinearLayout(this);
        secPerf.setOrientation(LinearLayout.VERTICAL);
        scrollPerf.addView(secPerf);
        scrollPerf.setVisibility(View.GONE);
        body.addView(scrollPerf);

        // Filters Bar
        LinearLayout filterBar = new LinearLayout(this);
        btnDaily = makeFilterBtn("📅 Daily", "daily");
        btnWeekly = makeFilterBtn("📆 Weekly", "weekly");
        btnMonthly = makeFilterBtn("🗓️ Monthly", "monthly");
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpF.setMargins(4, 0, 4, 16);
        filterBar.addView(btnDaily, lpF);
        filterBar.addView(btnWeekly, new LinearLayout.LayoutParams(lpF));
        filterBar.addView(btnMonthly, new LinearLayout.LayoutParams(lpF));
        secPerf.addView(filterBar);

        // Hub Performance Box
        LinearLayout hubBox = new LinearLayout(this);
        hubBox.setOrientation(LinearLayout.VERTICAL);
        hubBox.setBackground(makeBox(Color.parseColor("#132338"), Color.parseColor("#00E676"), 16));
        hubBox.setPadding(24, 20, 24, 20);

        TextView hubTitle = new TextView(this);
        hubTitle.setText("🏢 MALBAZARHUB_NJP  |  🎯 Target: 92.0%");
        hubTitle.setTextColor(Color.parseColor("#00E676"));
        hubTitle.setTypeface(Typeface.DEFAULT_BOLD);
        hubTitle.setTextSize(15f);
        hubBox.addView(hubTitle);

        txtHubStats = new TextView(this);
        txtHubStats.setTextColor(Color.WHITE);
        txtHubStats.setTextSize(13f);
        txtHubStats.setPadding(0, 10, 0, 0);
        hubBox.addView(txtHubStats);
        secPerf.addView(hubBox);

        // Sorting Toggle Bar
        LinearLayout sortBar = new LinearLayout(this);
        sortBar.setOrientation(LinearLayout.HORIZONTAL);
        sortBar.setGravity(Gravity.CENTER_VERTICAL);
        sortBar.setPadding(0, 24, 0, 12);

        TextView agentTitle = new TextView(this);
        agentTitle.setText("👥 Delivery Agents Report");
        agentTitle.setTextColor(Color.parseColor("#8fa0bc"));
        agentTitle.setTypeface(Typeface.DEFAULT_BOLD);
        agentTitle.setTextSize(14f);
        sortBar.addView(agentTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        btnSortToggle = new Button(this);
        btnSortToggle.setText("🏆 Top First (High)");
        btnSortToggle.setTextSize(11f);
        btnSortToggle.setTextColor(Color.parseColor("#00E676"));
        btnSortToggle.setBackground(makeBox(Color.parseColor("#1f2d47"), Color.parseColor("#00E676"), 12));
        btnSortToggle.setPadding(20, 8, 20, 8);
        btnSortToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sortTopFirst = !sortTopFirst;
                btnSortToggle.setText(sortTopFirst ? "🏆 Top First (High)" : "⚠️ Low First (Need Help)");
                loadPerformance();
            }
        });
        sortBar.addView(btnSortToggle);
        secPerf.addView(sortBar);

        cardsContainer = new LinearLayout(this);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        secPerf.addView(cardsContainer);

        // Tab Switching
        btnTracker.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                secTracker.setVisibility(View.VISIBLE);
                scrollPerf.setVisibility(View.GONE);
                btnTracker.setBackground(makeBox(Color.parseColor("#00E676"), 0, 14));
                btnTracker.setTextColor(Color.BLACK);
                btnPerf.setBackground(makeBox(Color.parseColor("#1a2333"), 0, 14));
                btnPerf.setTextColor(Color.parseColor("#8fa0bc"));
            }
        });

        btnPerf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                secTracker.setVisibility(View.GONE);
                scrollPerf.setVisibility(View.VISIBLE);
                btnPerf.setBackground(makeBox(Color.parseColor("#00E676"), 0, 14));
                btnPerf.setTextColor(Color.BLACK);
                btnTracker.setBackground(makeBox(Color.parseColor("#1a2333"), 0, 14));
                btnTracker.setTextColor(Color.parseColor("#8fa0bc"));
                loadPerformance();
            }
        });

        setContentView(root);
    }

    Button makeFilterBtn(String title, final String m) {
        Button b = new Button(this);
        b.setText(title);
        b.setBackground(makeBox(m.equals(currentFilter) ? Color.parseColor("#238636") : Color.parseColor("#1a2333"), 0, 12));
        b.setTextColor(m.equals(currentFilter) ? Color.WHITE : Color.parseColor("#8fa0bc"));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentFilter = m;
                btnDaily.setBackground(makeBox("daily".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#1a2333"), 0, 12));
                btnDaily.setTextColor("daily".equals(m) ? Color.WHITE : Color.parseColor("#8fa0bc"));
                btnWeekly.setBackground(makeBox("weekly".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#1a2333"), 0, 12));
                btnWeekly.setTextColor("weekly".equals(m) ? Color.WHITE : Color.parseColor("#8fa0bc"));
                btnMonthly.setBackground(makeBox("monthly".equals(m) ? Color.parseColor("#238636") : Color.parseColor("#1a2333"), 0, 12));
                btnMonthly.setTextColor("monthly".equals(m) ? Color.WHITE : Color.parseColor("#8fa0bc"));
                loadPerformance();
            }
        });
        return b;
    }

    String getCycleDate() {
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) < 9) c.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
    }

    void refreshCount() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM orders", null);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        txtCount.setText("📦 Total Active Orders: " + n);
    }

    void searchOrders(String q) {
        orderList.clear();
        if (!q.isEmpty()) {
            Cursor c = db.rawQuery("SELECT tracking_id, order_id FROM orders WHERE tracking_id LIKE ? OR order_id LIKE ? LIMIT 50",
                    new String[]{"%" + q + "%", "%" + q + "%"});
            while (c.moveToNext()) {
                orderList.add(new String[]{c.getString(0), c.getString(1)});
            }
            c.close();
        }
        adapter.notifyDataSetChanged();
    }

    void loadPerformance() {
        cardsContainer.removeAllViews();
        String cond = "daily".equals(currentFilter) ? " WHERE entry_date = (SELECT MAX(entry_date) FROM performance) "
                : ("weekly".equals(currentFilter) ? " WHERE entry_date >= date('now','localtime','-7 days') "
                : " WHERE entry_date >= date('now','localtime','-30 days') ");

        Cursor hc = db.rawQuery("SELECT SUM(ofd), SUM(del), SUM(ofp), SUM(pik) FROM performance" + cond, null);
        if (hc.moveToFirst()) {
            int o = hc.getInt(0), d = hc.getInt(1), op = hc.getInt(2), p = hc.getInt(3);
            int dnp = o + op, dnpc = d + p;
            double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
            txtHubStats.setText("OFD: " + o + " | DEL: " + d + " | OFP: " + op + " | PIK: " + p +
                    "\nDNP: " + dnp + " | DNPC: " + dnpc + " | Hub Actual Conv: " + String.format(Locale.US, "%.1f%%", r));
        } else {
            txtHubStats.setText("No data synced yet. Tap Admin -> Sync.");
        }
        hc.close();

        Cursor ac = db.rawQuery("SELECT name, mobile, SUM(ofd), SUM(del), SUM(ofp), SUM(pik) FROM performance " + cond + " GROUP BY name, mobile", null);
        ArrayList<String[]> agList = new ArrayList<String[]>();
        while (ac.moveToNext()) {
            int o = ac.getInt(2), d = ac.getInt(3), op = ac.getInt(4), p = ac.getInt(5);
            int dnp = o + op, dnpc = d + p;
            double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
            agList.add(new String[]{ac.getString(0), ac.getString(1), String.valueOf(o), String.valueOf(d),
                    String.valueOf(op), String.valueOf(p), String.valueOf(dnp), String.valueOf(dnpc),
                    String.format(Locale.US, "%.1f", r), String.valueOf(r)});
        }
        ac.close();

        Collections.sort(agList, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                double r1 = Double.parseDouble(a[9]);
                double r2 = Double.parseDouble(b[9]);
                return sortTopFirst ? Double.compare(r2, r1) : Double.compare(r1, r2);
            }
        });

        int rank = 1;
        for (String[] ag : agList) {
            double rate = Double.parseDouble(ag[9]);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 20, 24, 20);

            int badgeColor = (rate >= 92.0) ? Color.parseColor("#00E676") : ((rate >= 85.0) ? Color.parseColor("#FFB300") : Color.parseColor("#FF5252"));
            card.setBackground(makeBox(Color.parseColor("#141d2d"), badgeColor, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 14);
            card.setLayoutParams(lp);

            String medal = (sortTopFirst && rank == 1) ? "👑 🥇 #" + rank + " " : ((sortTopFirst && rank == 2) ? "🥈 #" + rank + " " : ((sortTopFirst && rank == 3) ? "🥉 #" + rank + " " : "👤 "));

            TextView n = new TextView(this);
            n.setText(medal + ag[0] + " (" + ag[1] + ")");
            n.setTextColor(badgeColor);
            n.setTypeface(Typeface.DEFAULT_BOLD);
            n.setTextSize(14f);
            card.addView(n);

            TextView s = new TextView(this);
            s.setText("OFD: " + ag[2] + " | DEL: " + ag[3] + " | OFP: " + ag[4] + " | PIK: " + ag[5] +
                    "\nDNP: " + ag[6] + " | DNPC: " + ag[7] + " | Conv Rate: " + ag[8] + "%");
            s.setTextColor(Color.WHITE);
            s.setTextSize(12f);
            s.setPadding(0, 6, 0, 0);
            card.addView(s);

            cardsContainer.addView(card);
            rank++;
        }
    }

    void showAdminDialog() {
        final EditText in = new EditText(this);
        in.setHint("PIN...");
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("🔐 Admin Login").setView(in)
            .setPositiveButton("Verify", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    if ("9547927698".equals(in.getText().toString().trim())) openSyncDialog();
                    else Toast.makeText(MainActivity.this, "Wrong PIN!", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    void openSyncDialog() {
        new AlertDialog.Builder(this).setTitle("⚡ Sync Options")
            .setPositiveButton("Sync Live Sheet", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    new Thread(new Runnable() {
                        public void run() { syncSheetData(); }
                    }).start();
                }
      
