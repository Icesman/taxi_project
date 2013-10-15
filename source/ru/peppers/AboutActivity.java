package ru.peppers;

import java.util.ArrayList;
import java.util.Arrays;

import model.Driver;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AboutActivity extends BalanceActivity {
    private ArrayAdapter<String> simpleAdpt;
    private ArrayList<String> itemsList;
    protected static final String PREFS_NAME = "MyNamePrefs1";
    private static final int REQUEST_GET = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initList();

    }

    public void initList() {
        itemsList = new ArrayList<String>();
        itemsList.add("АБС-Такси: Водитель");
        try {
            itemsList.add("Статус: " + "Версия программы: "
                    + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        ListView lv = (ListView) findViewById(R.id.mainListView);

        simpleAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, itemsList);

        lv.setAdapter(simpleAdpt);

    }
}
