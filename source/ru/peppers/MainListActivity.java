package ru.peppers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Driver;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainListActivity extends Activity {
    private ListView lv;
    public SimpleAdapter simpleAdpt;
    public List<Map<String, String>> itemsList;
    private static final String MY_TAG = "My_tag";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        //Bundle bundle = getIntent().getExtras();
        //int id = bundle.getInt("id");

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("action", "mainlist"));
        //nameValuePairs.add(new BasicNameValuePair("id", String.valueOf(id)));

        Document doc = PhpData.postData(this, nameValuePairs);
        if (doc != null) {
            Node errorNode = doc.getElementsByTagName("error").item(0);

            if (Integer.parseInt(errorNode.getTextContent()) == 1)
                new AlertDialog.Builder(this).setTitle("������")
                        .setMessage("������ �� �������. ������������� ����������.")
                        .setNeutralButton("�������", null).show();
            else {
                parseMainList(doc);
                initMainList();
            }
        } else {
            initMainList();
        }
    }

    private void parseMainList(Document doc) {
        int ordersCount = Integer.valueOf(doc.getElementsByTagName("ordersCount").item(0).getTextContent());
        int carClass = Integer.valueOf(doc.getElementsByTagName("carClass").item(0).getTextContent());
        int status = Integer.valueOf(doc.getElementsByTagName("status").item(0).getTextContent());
        String district = doc.getElementsByTagName("district").item(0).getTextContent();
        String subdistrict = doc.getElementsByTagName("subdistrict").item(0).getTextContent();

        //Bundle bundle = getIntent().getExtras();
        //int id = bundle.getInt("id");
        Driver driver = TaxiApplication.getDriver();
        driver.setStatus(status);
        driver.setClassAuto(carClass);
        driver.setOrdersCount(ordersCount);
        driver.setDistrict(district);
        driver.setSubdistrict(subdistrict);
    }

    private void initMainList() {
        final Driver driver = TaxiApplication.getDriver();
        if (driver != null) {
            itemsList = new ArrayList<Map<String, String>>();
            itemsList.add(createItem("item", "��� ������: " + driver.getOrdersCount()));
            itemsList.add(createItem("item", "������: " + driver.getStatusString()));
            itemsList.add(createItem("item", "��������� ������"));
            if (driver.getStatus() != 1) {
                String rayonString = "";
                if (driver.getDistrict() != "")
                    rayonString = driver.getDistrict() + "," + driver.getSubdistrict();
                else
                    rayonString = "�� ������";

                itemsList.add(createItem("item", "�����: " + rayonString));
            }
            itemsList.add(createItem("item", "������ �� �����"));
            itemsList.add(createItem("item", "���������"));
            itemsList.add(createItem("item", "���������"));
            itemsList.add(createItem("item", "�����"));

            lv = (ListView) findViewById(R.id.mainListView);

            simpleAdpt = new SimpleAdapter(this, itemsList, android.R.layout.simple_list_item_1,
                    new String[] { "item" }, new int[] { android.R.id.text1 });

            lv.setAdapter(simpleAdpt);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long index) {
                    Bundle extras = getIntent().getExtras();
                    ////int id = extras.getInt("id");
                    Intent intent;
                    switch (position) {
                        case 0:
                            intent = new Intent(MainListActivity.this, MyOrderActivity.class);
                            startActivity(intent);
                            break;
                        case 1:
                            if (driver.getStatus() != 3) {
                                intent = new Intent(MainListActivity.this, ReportActivity.class);
                                startActivity(intent);
                            } else {
                                intent = new Intent(MainListActivity.this, MyOrderActivity.class);
                                startActivity(intent);
                            }
                            break;
                        case 2:
                            intent = new Intent(MainListActivity.this, FreeOrderActivity.class);
                            startActivity(intent);
                            break;
                        case 3:
                            if (driver.getStatus() != 1) {
                                intent = new Intent(MainListActivity.this, DistrictActivity.class);
                                return;
                            }
                        default:
                            break;
                    }
                    if (driver.getStatus() != 1)
                        position--;
                    if (position == 3) {
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        nameValuePairs.add(new BasicNameValuePair("action", "calloffice"));
                       // nameValuePairs.add(new BasicNameValuePair("id", String.valueOf(id)));

                        Document doc = PhpData.postData(MainListActivity.this, nameValuePairs);
                        if (doc != null) {
                            Node errorNode = doc.getElementsByTagName("error").item(0);

                            if (Integer.parseInt(errorNode.getTextContent()) == 1)
                                new AlertDialog.Builder(MainListActivity.this).setTitle("������")
                                        .setMessage("������ �� �������. ������������� ����������.")
                                        .setNeutralButton("�������", null).show();
                            else {
                                new AlertDialog.Builder(MainListActivity.this).setTitle("��")
                                        .setMessage("��� ������ ������. �������� ������.")
                                        .setNeutralButton("�������", null).show();
                            }
                        }
                    }
                    if (position == 4) {
                        intent = new Intent(MainListActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }
                    if (position == 5) {
                        intent = new Intent(MainListActivity.this, MessageActivity.class);
                        startActivity(intent);
                    }
                    if (position == 6) {
                        Driver driver = TaxiApplication.getDriver();
                        if (driver.getOrdersCount() != 0) {
                            exitDialog();
                        }
                    }
                }

            });
        }
    }

    private void exitDialog() {
        new AlertDialog.Builder(MainListActivity.this).setTitle("������")
                .setMessage("� ��������� � ��� ���� ���������� ������.")
                .setPositiveButton("�����", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(MainListActivity.this, PhpService.class);
                        stopService(intent);
                        finish();
                    }

                }).setNegativeButton("��������", null).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle the back button
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            // Ask the user if they want to quit
            Driver driver = TaxiApplication.getDriver();
            if (driver.getOrdersCount() != 0) {
                exitDialog();
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }

    public HashMap<String, String> createItem(String key, String name) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put(key, name);

        return item;
    }

}
