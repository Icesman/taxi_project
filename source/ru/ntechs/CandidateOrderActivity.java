package ru.ntechs;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import model.Util;
import myorders.MyCostOrder;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

/**
 * Кандидатский заказ
 * @author p.balmasov
 */
public class CandidateOrderActivity extends BalanceActivity {

    private TextView tv;
    private MyCostOrder order;
    private Timer myTimer = new Timer();
    private Integer refreshperiod = null;
    private boolean start = false;
    private String orderIndex;
    private boolean fromService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.candidateorder);

        Bundle bundle = getIntent().getExtras();
        final String index = bundle.getString("id");
        orderIndex = index;
        tv = (TextView) findViewById(R.id.textView1);
        fromService = bundle.getBoolean("isService");
        Button button = (Button) findViewById(R.id.button1);
        button.setText("Принять");
        button.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (order.get_departuretime() != null)
                    onAccept(index, null);
                else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(CandidateOrderActivity.this);
                    alert.setTitle(CandidateOrderActivity.this.getString(R.string.time));
                    final CharSequence cs[];

                    cs = new String[] { "3", "5", "7", "10", "15", "20", "25", "30", "35" };

                    alert.setItems(cs, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            onAccept(index, (String) cs[which]);
                        }
                    });
                    alert.show();
                }

            }

        });

        Button button1 = (Button) findViewById(R.id.button2);
        button1.setText("Отказаться");
        button1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefuse(index);
            }

        });
        getOrder(index);

        Window window = this.getWindow();
        window.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);

    }

    private void onAccept(String index, String c) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "accept"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "order"));
        nameValuePairs.add(new BasicNameValuePair("orderid", index));
        if (c != null)
            nameValuePairs.add(new BasicNameValuePair("minutes", c));

        Document doc = PhpData.postData(this, nameValuePairs, PhpData.newURL);
        if (doc != null) {
            Node responseNode = doc.getElementsByTagName("response").item(0);
            Node errorNode = doc.getElementsByTagName("message").item(0);

            if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                PhpData.errorFromServer(this, errorNode);
            else {
                try {
                    Bundle bundle = new Bundle();
                    bundle.putString("index", orderIndex);

                    if (fromService) {
                        Intent intent = new Intent(this, MyOrderItemActivity.class);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent();
                        intent.putExtras(bundle);
                        setResult(RESULT_OK, intent);
                    }

                    finish();
                } catch (Exception e) {
                    PhpData.errorHandler(this, e);
                }
            }
        }
    }

    private void onRefuse(String index) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "refuse"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "order"));
        nameValuePairs.add(new BasicNameValuePair("orderid", index));

        Document doc = PhpData.postData(this, nameValuePairs, PhpData.newURL);
        if (doc != null) {
            Node responseNode = doc.getElementsByTagName("response").item(0);
            Node errorNode = doc.getElementsByTagName("message").item(0);

            if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                PhpData.errorFromServer(this, errorNode);
            else {
                try {
                    if (myTimer != null)
                        myTimer.cancel();
                    finish();
                    // initOrder(doc);
                } catch (Exception e) {
                    PhpData.errorHandler(this, e);
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle the back button
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            // Ask the user if they want to quit

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }

    private void getOrder(String index) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "get"));
        nameValuePairs.add(new BasicNameValuePair("mode", "available"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "order"));
        nameValuePairs.add(new BasicNameValuePair("orderid", index));

        Document doc = PhpData.postData(this, nameValuePairs, PhpData.newURL);
        if (doc != null) {
            Node responseNode = doc.getElementsByTagName("response").item(0);
            Node errorNode = doc.getElementsByTagName("message").item(0);

            if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                PhpData.errorFromServer(this, errorNode);
            else {
                try {
                    initOrder(doc);
                } catch (Exception e) {
                    PhpData.errorHandler(this, e);
                }
            }
        }
    }

    private void initOrder(Document doc) throws DOMException, ParseException {
        // nominalcost - рекомендуемая стоимость заказа
        // class - класс автомобля (0 - все равно, 1 - Эконом, 2 - Стандарт,
        // 3 - Базовый)
        // addressdeparture - адрес подачи автомобиля
        // departuretime - время подачи(если есть)
        // paymenttype - форма оплаты (0 - наличные, 1 - безнал)
        // invitationtime - время приглашения (если пригласили)
        // quantity - количество заказов от этого клиента
        // comment - примечание
        // nickname - ник абонента (если есть)
        // registrationtime - время регистрации заказа
        // addressarrival - куда поедут

        Element item = (Element) doc.getElementsByTagName("order").item(0);

        order = Util.parseMyCostOrder(item, this);

        MediaPlayer mp = MediaPlayer.create(getBaseContext(), (R.raw.sound));
        mp.start();
        tv.setText("");
        ArrayList<String> orderList = order.toArrayList();
        int arraySize = orderList.size();
        for (int i = 0; i < arraySize; i++) {
            tv.append(orderList.get(i));
            tv.append("\n");
        }

        Node refreshperiodNode = doc.getElementsByTagName("refreshperiod").item(0);
        Integer newrefreshperiod = null;
        if (!refreshperiodNode.getTextContent().equalsIgnoreCase(""))
            newrefreshperiod = Integer.valueOf(refreshperiodNode.getTextContent());

        boolean update = false;

        Log.d("My_tag", refreshperiod + " " + newrefreshperiod + " " + update);

        if (newrefreshperiod != null) {
            if (refreshperiod != newrefreshperiod) {
                refreshperiod = newrefreshperiod;
                update = true;
            }
        }

        Log.d("My_tag", refreshperiod + " " + newrefreshperiod + " " + update);

        if (update && refreshperiod != null) {
            if (start) {
                myTimer.cancel();
                start = true;
                Log.d("My_tag", "cancel timer");
            }
            final Handler uiHandler = new Handler();

            TimerTask timerTask = new TimerTask() { // Определяем задачу
                @Override
                public void run() {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            getOrder(orderIndex);
                        }
                    });
                }
            };

            myTimer.schedule(timerTask, 1000 * refreshperiod, 1000 * refreshperiod);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myTimer != null)
            myTimer.cancel();
    }
}
