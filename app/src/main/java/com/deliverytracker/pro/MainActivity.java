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
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private DatabaseHelper dbHelper;
    private final ArrayList<String[]> ordersList = new ArrayList<String[]>();
    private BaseAdapter ordersAdapter;
    private LinearLayout secTracker, secPerformance, agentsContainer;
    private ScrollView scrollPerf;
    private Button btnTabTracker, btnTabPerf, btnDaily, btnWeekly, btnMonthly;
    private TextView txtActiveCount, txtHubStats;
    private String currentFilter = "daily";
    private static final String CSV_URL = "https://docs.google.com/spreadsheets/d/1Dul38iNZ_eNmABVuYVWhrUg9F_xVMvaVvQvLIXlySj4/export?format=csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);
        buildUI();
        refreshTotalCount();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0d1117"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#161b22"));
        header.setPadding(24, 20, 24, 20);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("⚡ Delivery Tracker Pro");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnAdmin = new Button(this);
        btnAdmin.setText("🔒 Admin");
        btnAdmin.setTextColor(Color.parseColor("#00E676"));
        btnAdmin.setBackgroundColor(Color.parseColor("#21262d"));
        btnAdmin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAdminDialog(); }
        });
        header.addView(btnAdmin);
        root.addView(header);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(12, 10, 12, 10);
        tabs.setBackgroundColor(Color.parseColor("#161b22"));

        btnTabTracker = new Button(this);
        btnTabTracker.setText("🔍 Tracker");
        btnTabTracker.setTextColor(Color.BLACK);
        btnTabTracker.setBackgroundColor(Color.parseColor("#00E676"));
        btnTabTracker.setTypeface(Typeface.DEFAULT_BOLD);

        btnTabPerf = new Button(this);
        btnTabPerf.setText("📊 Performance");
        btnTabPerf.setTextColor(Color.parseColor("#8b949e"));
        btnTabPerf.setBackgroundColor(Color.parseColor("#21262d"));
        btnTabPerf.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpT.setMargins(6, 0, 6, 0);
        tabs.addView(btnTabTracker, lpT);
        tabs.addView(btnTabPerf, new LinearLayout.LayoutParams(lpT));
        root.addView(tabs);

        FrameLayout body = new FrameLayout(this);
        body.setPadding(20, 20, 20, 20);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        secTracker = new LinearLayout(this);
        secTracker.setOrientation(LinearLayout.VERTICAL);

        EditText searchInput = new EditText(this);
        searchInput.setHint("Enter Tracking ID (FMPC...)...");
        searchInput.setHintTextColor(Color.parseColor("#8b949e"));
        searchInput.setTextColor(Color.WHITE);
        searchInput.setBackgroundColor(Color.parseColor("#161b22"));
        searchInput.setPadding(24, 24, 24, 24);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int b, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int b, int c, int a) { executeSearch(s.toString().trim()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        secTracker.addView(searchInput);

        txtActiveCount = new TextView(this);
        txtActiveCount.setText("📦 Orders List (Active: 0)");
        txtActiveCount.setTextColor(Color.parseColor("#8b949e"));
        txtActiveCount.setPadding(0, 20, 0, 16);
        txtActiveCount.setTypeface(Typeface.DEFAULT_BOLD);
        secTracker.addView(txtActiveCount);

        ListView listView = new ListView(this);
        ordersAdapter = new BaseAdapter() {
            @Override public int getCount() { return ordersList.size(); }
            @Override public Object getItem(int i) { return ordersList.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup p) {
                LinearLayout l = new LinearLayout(MainActivity.this);
                l.setOrientation(LinearLayout.VERTICAL);
                l.setPadding(20, 16, 20, 16);
                l.setBackgroundColor(Color.parseColor("#161b22"));
                final String[] item = ordersList.get(i);
                TextView t1 = new TextView(MainActivity.this);
                t1.setText("Track ID: " + item[0]);
                t1.setTextColor(Color.parseColor("#8b949e"));
                t1.setTextSize(12f);
                TextView t2 = new TextView(MainActivity.this);
                t2.setText("Order ID: " + item[1] + " (Tap to Copy)");
                t2.setTextColor(Color.parseColor("#00E676"));
                t2.setTextSize(14f);
                t2.setTypeface(Typeface.DEFAULT_BOLD);
                l.addView(t1);
                l.addView(t2);
                l.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("Order ID", item[1]));
                        Toast.makeText(MainActivity.this, "Copied: " + item[1], Toast.LENGTH_SHORT).show();
                    }
                });
                return l;
            }
        };
        listView.setAdapter(ordersAdapter);
        secTracker.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        body.addView(secTracker);

        scrollPerf = new ScrollView(this);
        secPerformance = new LinearLayout(this);
        secPerformance.setOrientation(LinearLayout.VERTICAL);
        scrollPerf.addView(secPerformance);
        scrollPerf.setVisibility(View.GONE);
        body.addView(scrollPerf);

        LinearLayout filters = new LinearLayout(this);
        btnDaily = makeFilterBtn("📅 Daily", "daily");
        btnWeekly = makeFilterBtn("📆 Weekly", "weekly");
        btnMonthly = makeFilterBtn("🗓️ Monthly", "monthly");
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lpF.setMargins(4, 0, 4, 16);
        filters.addView(btnDaily, lpF);
        filters.addView(btnWeekly, new LinearLayout.LayoutParams(lpF));
        filters.addView(btnMonthly, new LinearLayout.LayoutParams(lpF));
        secPerformance.addView(filters);

        LinearLayout hubBox = new LinearLayout(this);
        hubBox.setOrientation(LinearLayout.VERTICAL);
        hubBox.setBackgroundColor(Color.parseColor("#1c2331"));
        hubBox.setPadding(24, 24, 24, 24);
        TextView hubTitle = new TextView(this);
        hubTitle.setText("🏢 MALBAZARHUB_NJP  |  🎯 Target: 92.0%");
        hubTitle.setTextColor(Color.parseColor("#00E676"));
        hubTitle.setTypeface(Typeface.DEFAULT_BOLD);
        hubBox.addView(hubTitle);

        txtHubStats = new TextView(this);
        txtHubStats.setTextColor(Color.WHITE);
        txtHubStats.setTextSize(13f);
        txtHubStats.setPadding(0, 12, 0, 0);
        hubBox.addView(txtHubStats);
        secPerformance.addView(hubBox);

        TextView agentTitle = new TextView(this);
        agentTitle.setText("👥 Delivery Agents Report (Low to High)");
        agentTitle.setTextColor(Color.parseColor("#8b949e"));
        agentTitle.setPadding(0, 24, 0, 12);
        agentTitle.setTypeface(Typeface.DEFAULT_BOLD);
        secPerformance.addView(agentTitle);

        agentsContainer = new LinearLayout(this);
        agentsContainer.setOrientation(LinearLayout.VERTICAL);
        secPerformance.addView(agentsContainer);

        btnTabTracker.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                secTracker.setVisibility(View.VISIBLE);
                scrollPerf.setVisibility(View.GONE);
                btnTabTracker.setBackgroundColor(Color.parseColor("#00E676"));
                btnTabTracker.setTextColor(Color.BLACK);
                btnTabPerf.setBackgroundColor(Color.parseColor("#21262d"));
                btnTabPerf.setTextColor(Color.parseColor("#8b949e"));
            }
        });
        btnTabPerf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                secTracker.setVisibility(View.GONE);
                scrollPerf.setVisibility(View.VISIBLE);
                btnTabPerf.setBackgroundColor(Color.parseColor("#00E676"));
                btnTabPerf.setTextColor(Color.BLACK);
                btnTabTracker.setBackgroundColor(Color.parseColor("#21262d"));
                btnTabTracker.setTextColor(Color.parseColor("#8b949e"));
                loadPerformance();
            }
        });

        setContentView(root);
    }

    private Button makeFilterBtn(String title, final String mode) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(mode.equals(currentFilter) ? Color.WHITE : Color.parseColor("#8b949e"));
        b.setBackgroundColor(mode.equals(currentFilter) ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentFilter = mode;
                updateFilterUI();
                loadPerformance();
            }
        });
        return b;
    }

    private void updateFilterUI() {
        btnDaily.setBackgroundColor(currentFilter.equals("daily") ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
        btnDaily.setTextColor(currentFilter.equals("daily") ? Color.WHITE : Color.parseColor("#8b949e"));
        btnWeekly.setBackgroundColor(currentFilter.equals("weekly") ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
        btnWeekly.setTextColor(currentFilter.equals("weekly") ? Color.WHITE : Color.parseColor("#8b949e"));
        btnMonthly.setBackgroundColor(currentFilter.equals("monthly") ? Color.parseColor("#238636") : Color.parseColor("#21262d"));
        btnMonthly.setTextColor(currentFilter.equals("monthly") ? Color.WHITE : Color.parseColor("#8b949e"));
    }

    public static String getShiftCycleDate() {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) < 9) cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    private void refreshTotalCount() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM orders", null);
            int count = c.moveToFirst() ? c.getInt(0) : 0;
            c.close();
            txtActiveCount.setText("📦 Orders List (Active: " + count + ")");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void executeSearch(String q) {
        ordersList.clear();
        if (!q.isEmpty()) {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor c = db.rawQuery("SELECT tracking_id, order_id FROM orders WHERE tracking_id LIKE ? LIMIT 40", new String[]{"%" + q + "%"});
                while (c.moveToNext()) {
                    ordersList.add(new String[]{c.getString(0), c.getString(1)});
                }
                c.close();
            } catch (Exception e) { e.printStackTrace(); }
        }
        ordersAdapter.notifyDataSetChanged();
    }

    private void loadPerformance() {
        agentsContainer.removeAllViews();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String cond = "daily".equals(currentFilter) ? " WHERE entry_date = (SELECT MAX(entry_date) FROM agent_performance) "
                    : ("weekly".equals(currentFilter) ? " WHERE entry_date >= date('now', 'localtime', '-7 days') "
                    : " WHERE entry_date >= date('now', 'localtime', '-30 days') ");

            Cursor hc = db.rawQuery("SELECT SUM(ofd), SUM(del), SUM(ofp), SUM(piked) FROM agent_performance" + cond, null);
            if (hc.moveToFirst()) {
                int tofd = hc.getInt(0), tdel = hc.getInt(1), tofp = hc.getInt(2), tpik = hc.getInt(3);
                int tdnp = tofd + tofp, tdnpc = tdel + tpik;
                double r = tdnp > 0 ? ((double) tdnpc / tdnp) * 100.0 : 0.0;
                txtHubStats.setText("OFD: " + tofd + " | DEL: " + tdel + " | OFP: " + tofp + " | PIKED: " + tpik + "\nDNP: " + tdnp + " | DNPC: " + tdnpc + " | Actual Conv: " + String.format(Locale.US, "%.1f%%", r));
            } else {
                txtHubStats.setText("No data synced yet. Tap Admin -> Live Sync.");
            }
            hc.close();

            Cursor ac = db.rawQuery("SELECT name, mobile, SUM(ofd), SUM(del), SUM(ofp), SUM(piked) FROM agent_performance " + cond + " GROUP BY name, mobile", null);
            ArrayList<AgentModel> list = new ArrayList<AgentModel>();
            while (ac.moveToNext()) {
                list.add(new AgentModel(ac.getString(0), ac.getString(1), ac.getInt(2), ac.getInt(3), ac.getInt(4), ac.getInt(5)));
            }
            ac.close();

            Collections.sort(list, new Comparator<AgentModel>() {
                @Override public int compare(AgentModel a, AgentModel b) { return Double.compare(a.getRate(), b.getRate()); }
            });

            for (AgentModel ag : list) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Color.parseColor("#161b22"));
                card.setPadding(20, 16, 20, 16);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                card.setLayoutParams(lp);

                TextView n = new TextView(this);
                n.setText("👤 " + ag.name + " (" + ag.mobile + ")");
                n.setTextColor(Color.parseColor("#00E676"));
                n.setTypeface(Typeface.DEFAULT_BOLD);
                card.addView(n);

                TextView s = new TextView(this);
                s.setText(String.format(Locale.US, "OFD: %d | DEL: %d | OFP: %d | PIK: %d\nDNP: %d | DNPC: %d | Conv: %.1f%%", ag.ofd, ag.del, ag.ofp, ag.piked, ag.dnp, ag.dnpc, ag.getRate()));
                s.setTextColor(Color.WHITE);
                s.setTextSize(12f);
                s.setPadding(0, 6, 0, 0);
                card.addView(s);
                agentsContainer.addView(card);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAdminDialog() {
        final EditText in = new EditText(this);
        in.setHint("Enter Admin PIN...");
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("🔐 Admin Login")
                .setView(in)
                .setPositiveButton("Verify", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if ("9547927698".equals(in.getText().toString().trim())) openSyncOptions();
                        else Toast.makeText(MainActivity.this, "Wrong PIN!", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void openSyncOptions() {
        new AlertDialog.Builder(this)
                .setTitle("⚡ Google Sheet Sync")
                .setMessage("Sync Google Sheet data live?")
                .setPositiveButton("Sync Now", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { new SyncTask().execute(); }
                })
                .setNegativeButton("Clear All Data", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete("orders", null, null);
                            db.delete("agent_performance", null, null);
                            refreshTotalCount();
                            executeSearch("");
                            Toast.makeText(MainActivity.this, "All Data Cleared!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }).show();
    }

    private class SyncTask extends AsyncTask<Void, Void, Integer> {
        @Override protected void onPreExecute() { Toast.makeText(MainActivity.this, "Syncing Google Sheet...", Toast.LENGTH_SHORT).show(); }

        @Override protected Integer doInBackground(Void... v) {
            int count = 0;
            String cycleDate = getShiftCycleDate();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(CSV_URL).openConnection();
                conn.setConnectTimeout(15000);
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    db.delete("orders", null, null);
                    db.delete("agent_performance", "entry_date = ?", new String[]{cycleDate});
                    String line;
                    boolean isHeader = true;
                    while ((line = r.readLine()) != null) {
                        if (isHeader) { isHeader = false; continue; }
                        String[] p = line.split(",", -1);
                        if (p.length < 2) continue;
                        String oId = p[0].replace("\"", "").trim();
                        String tId = p[1].replace("\"", "").trim();
                        String name = p.length > 2 ? p[2].replace("\"", "").trim() : "";
                        String mob = p.length > 3 ? p[3].replace("\"", "").trim() : "";

                        if (!tId.isEmpty()) {
                            ContentValues cv = new Conten
