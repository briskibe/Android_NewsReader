package com.example.bernard.newsreaderapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase sqLiteDatabase;
    ListView listNews;
    List<String> listNewsTitles;
    ArrayAdapter adapter;

    public class getUrlTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                JSONObject json = new JSONObject(s);
                String title = json.getString("title");
                String url = json.getString("url");
                Log.i("CONTINUE", title);
                sqLiteDatabase.execSQL("INSERT INTO news(title, url) values ('"
                        + title + "', '" + url + "')");
                populateListView();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... urls) {
            return backgroundWork(urls);
        }
    }

    public class GetIDsTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                JSONArray jsonArray = new JSONArray(s);
                for (int i = 0; i < jsonArray.length(); i++) {
                    String url;
                    try {
                        getUrlTask getUrlTask = new getUrlTask();

                        url = getUrlTask.execute("https://hacker-news.firebaseio.com/v0/item/" +
                                jsonArray.getString(i) +
                                ".json?print=pretty").get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... urls) {
            return backgroundWork(urls);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpListView();

        sqLiteDatabase = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS news(id INTEGER PRIMARY KEY, title varchar(100), url varchar(100))");
        Cursor c = sqLiteDatabase.rawQuery("SELECT * FROM news", null);
        c.moveToFirst();
        if (!c.isAfterLast()) {
            // table exists, only make listview
            Log.i("START", "POPULATING LISTVIEW");
            populateListView();
        } else {
            Log.i("START", "POPULATING DATABASE");
            String listIds;
            try {
                GetIDsTask getIDsTask = new GetIDsTask();
                listIds = getIDsTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String backgroundWork(String... urls) {
        try {
            URL url = new URL(urls[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream in = conn.getInputStream();
            InputStreamReader reader = new InputStreamReader(in);
            String retVal = "";
            int data = reader.read();
            while (data != -1) {
                retVal += (char) data;
                data = reader.read();
            }
            return retVal;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void populateListView() {
        listNewsTitles.clear();
        Cursor c = sqLiteDatabase.rawQuery("SELECT title FROM news", null);
        int titleIndex = c.getColumnIndex("title");
        c.moveToFirst();
        Log.i("START", c.getString(titleIndex));
        while (!c.isAfterLast()) {
            Log.i("START", c.getString(titleIndex));
            listNewsTitles.add(c.getString(titleIndex));
            adapter.notifyDataSetChanged();
            c.moveToNext();
        }
    }

    private void setUpListView() {
        listNews = (ListView) findViewById(R.id.listNews);
        listNewsTitles = new ArrayList<String>();
        adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listNewsTitles);
        listNews.setAdapter(adapter);

        listNews.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String title = listNewsTitles.get(position);
                Cursor c = sqLiteDatabase.rawQuery("SELECT url FROM news WHERE title LIKE '" + title + "'", null);
                int urlIndex = c.getColumnIndex("url");
                c.moveToFirst();
                if (!c.isAfterLast()) {
                    Intent i = new Intent(MainActivity.this, WebViewActivity.class);
                    i.putExtra("url", c.getString(urlIndex));
                    startActivity(i);
                }
            }
        });
    }
}
