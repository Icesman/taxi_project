package ru.ntechs;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Мой заказ подробно
 * @author p.balmasov
 */
public class MyOrderItemActivity extends BalanceActivity implements AsyncTaskCompleteListener<Document> {

    protected static final int REQUEST_EXIT = 0;
    private CountDownTimer timer;
    private TextView counterView;
    private MyCostOrder order;
    private Dialog dialog;
    private Bundle bundle;
    private TextView tv;
    private Date currentTimer;

    private Timer myTimer = new Timer();
    private Integer refreshperiod = null;
    private boolean start = false;

    private String order_index;
    private boolean isDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("My_tag", "create");
        setContentView(R.layout.myorder);
        Bundle bundle = getIntent().getExtras();
        int index = bundle.getInt("index");
        isDialog = bundle.getBoolean("initdialog");

        counterView = (TextView) findViewById(R.id.textView1);
        tv = (TextView) findViewById(R.id.textView2);
        try {
            order = (MyCostOrder) TaxiApplication.getDriver(this).getOrder(index);
            order_index = order.get_index();
            ArrayList<String> orderList = order.toArrayList();

            int arraySize = orderList.size();
            for (int i = 0; i < arraySize; i++) {
                tv.append(orderList.get(i));
                tv.append("\n");
            }
        } catch (Exception e) {
            Log.d("My_tag", "error index");
            order_index = String.valueOf(index);
        }

        // if (order.get_departuretime() != null) {
        // if (order.get_servertime().before(order.get_departuretime())
        // && order.get_invitationtime() == null) {
        // currentTimer = order.get_departuretime();
        // timerInit();
        // }
        // }

        Button button = (Button) findViewById(R.id.button1);
        button.setText(this.getString(R.string.choose_action));
        button.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                initActionDialog();
            }

        });
        // getOrder();

    }

    @Override
    public void onTaskComplete(Document doc) {
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

    @SuppressWarnings("unchecked")
    private void getOrder() {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "get"));
        nameValuePairs.add(new BasicNameValuePair("mode", "available"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "order"));
        nameValuePairs.add(new BasicNameValuePair("orderid", order_index));

        ProgressDialog progress = new ProgressDialog(this);
        new MyTask(this, progress, this).execute(nameValuePairs);

    }

    private void initOrder(Document doc) throws DOMException, ParseException {
        tv.setText("");

        String oldComment = order.get_comment();

        Element item = (Element) doc.getElementsByTagName("order").item(0);
        order = Util.parseMyCostOrder(item, this,doc);
        if (order != null)
            //если появился комментарий
            if (oldComment == null && order.get_comment() != null) {
                MediaPlayer mp = MediaPlayer.create(getBaseContext(), (R.raw.sound));
                mp.start();
            }

        //таймер и т.д
        if (currentTimer != null) {
            if (!order.get_departuretime().equals(currentTimer) && order.get_invitationtime() == null) {
                currentTimer = order.get_departuretime();
                timerInit();
            }
        } else {
            if (order.get_departuretime() != null) {
                if (order.get_servertime().before(order.get_departuretime())
                        && order.get_invitationtime() == null) {
                    currentTimer = order.get_departuretime();
                    timerInit();
                }
            }
        }

        if (order.get_invitationtime() != null) {
            if (timer != null) {
                counterView.setText("");
                timer.cancel();
            }
        }
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
                            getOrder();
                        }
                    });
                }
            };

            myTimer.schedule(timerTask, 1000 * refreshperiod, 1000 * refreshperiod);
        }

        if (isDialog){
            isDialog = false;
            initDelayOrInviteDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // myTimer.cancel();
        Log.d("My_tag", "pause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("My_tag", "resume");
        getOrder();
        if (order != null)
            if (order.get_departuretime() != null) {
                if (order.get_servertime() != null)
                    if (order.get_servertime().before(order.get_departuretime())
                            && order.get_invitationtime() == null) {
                        currentTimer = order.get_departuretime();
                        timerInit();
                    }
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myTimer != null)
            myTimer.cancel();
        if (timer != null)
            timer.cancel();
        Log.d("My_tag", "stop");
    }

    private void inviteDialog() {

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", (order.get_invitationtime() == null) ? "invite"
                : "hurry"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "client"));
        nameValuePairs.add(new BasicNameValuePair("orderid", order.get_index()));

        Document doc = PhpData.postData(this, nameValuePairs, PhpData.newURL);
        if (doc != null) {
            Node responseNode = doc.getElementsByTagName("response").item(0);
            Node errorNode = doc.getElementsByTagName("message").item(0);

            if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                PhpData.errorFromServer(MyOrderItemActivity.this, errorNode);
            else {
                new AlertDialog.Builder(MyOrderItemActivity.this)
                        .setTitle(this.getString(R.string.Ok))
                        .setMessage((order.get_invitationtime() == null) ? "Ок. Приглашаем!" : "Ок. Торопим!")
                        .setNeutralButton(this.getString(R.string.close), null).show();
            }
        }
    }

    private void timeDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MyOrderItemActivity.this);
        alert.setTitle(this.getString(R.string.time));
        final String cs[] = new String[] { "3", "5", "7", "10", "15" };// ,
                                                                       // "20",
                                                                       // "25",
                                                                       // "30",
                                                                       // "35"};

        alert.setItems(cs, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // https://www.abs-taxi.ru/fcgi-bin/office/cman.fcgi?module=mobile;object=order;action=delay;time=7
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("action", "delay"));
                nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
                nameValuePairs.add(new BasicNameValuePair("object", "order"));
                nameValuePairs.add(new BasicNameValuePair("minutes", cs[which]));
                nameValuePairs.add(new BasicNameValuePair("orderid", order.get_index()));

                Document doc = PhpData.postData(MyOrderItemActivity.this, nameValuePairs, PhpData.newURL);
                if (doc != null) {
                    Node responseNode = doc.getElementsByTagName("response").item(0);
                    Node errorNode = doc.getElementsByTagName("message").item(0);

                    if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                        PhpData.errorFromServer(MyOrderItemActivity.this, errorNode);
                    else {
                        dialog.dismiss();
                        // final Order order =
                        // TaxiApplication.getDriver().getOrder(index);

                        // if (order.getTimerDate() != null) {
                        // Calendar cal = Calendar.getInstance();
                        // cal.add(Calendar.MINUTE, Integer.valueOf((String)
                        // cs[which]));
                        //
                        // order.setTimerDate(cal.getTime());
                        // timer.cancel();
                        // timerInit(order);
                        // }
                        new AlertDialog.Builder(MyOrderItemActivity.this)
                                .setTitle(MyOrderItemActivity.this.getString(R.string.Ok))
                                .setMessage(MyOrderItemActivity.this.getString(R.string.order_delayed))
                                .setNeutralButton(MyOrderItemActivity.this.getString(R.string.close), null)
                                .show();
                    }
                }
            }

        });
        alert.show();

    }

    private void initActionDialog() {
        ArrayList<String> arrayList = new ArrayList<String>();
        // пригласить закрыть звонок из офиса отложить
        // если driverstate=1 и invitationtime NULL: ОТЛОЖИТЬ, ЗВОНОК ИЗ ОФИСА
        // если driverstate=1 и invitationtime не NULL: ПОТОРОПИТЬ, ЗАКРЫТЬ,
        // ЗВОНОК ИЗ ОФИСА
        // если driverstate=2: ЗВОНОК ИЗ ОФИСА, ОТКАЗАТЬСЯ
        // если driverstate=3: ПРИНЯТЬ, ОТКЛОНИТЬ

        // пункт 1 (если driverstate=1 и invitationtime NULL) теперь следует обрабатывать сложнее: для этого
        // пункта надо сделать ветвление на 2 варианта:
        // если departuretime > текущего времени (которое следует брать из поля time): ОТЛОЖИТЬ, ЗВОНОК ИЗ
        // ОФИСА
        // если departuretime <= текущего времени (которое следует брать из поля time): ОТЛОЖИТЬ, ЗАКРЫТЬ,
        // ЗВОНОК ИЗ ОФИСА
        if (order.get_driverstate() == null)
            return;
        if (order.get_driverstate() == 1) {
            if (order.get_orderedtime() == null) {
                if (order.get_servertime().before(order.get_departuretime())) {
                    if (order.get_invitationtime() == null) {
                        arrayList.add(this.getString(R.string.invite));
                        arrayList.add(this.getString(R.string.delay));// опаздываю
                        arrayList.add(this.getString(R.string.close));
                        arrayList.add(this.getString(R.string.call_office));
                    } else {
                        arrayList.add("Поторопить");
                        arrayList.add(this.getString(R.string.close));
                        arrayList.add(this.getString(R.string.call_office));

                    }
                } else {
                    if (order.get_invitationtime() == null) {
                        arrayList.add(this.getString(R.string.invite));
                        arrayList.add(this.getString(R.string.close));
                        arrayList.add(this.getString(R.string.call_office));
                    } else {
                        arrayList.add("Поторопить");
                        arrayList.add(this.getString(R.string.close));
                        arrayList.add(this.getString(R.string.call_office));

                    }
                }
            } else {
                if (order.get_servertime().before(order.get_departuretime())) {
                    arrayList.add(this.getString(R.string.delay));// опаздываю
                    arrayList.add(this.getString(R.string.call_office));
                } else {
                    arrayList.add("Поторопить");
                    arrayList.add(this.getString(R.string.close));
                    arrayList.add(this.getString(R.string.call_office));
                }
            }
        } else if (order.get_driverstate() == 2) {
            arrayList.add(this.getString(R.string.call_office));
            arrayList.add("Отказаться");
        } else if (order.get_driverstate() == 3) {
            arrayList.add("Принять");
            arrayList.add("Отказаться");
        }

        // if (order.get_invitationtime() == null) {
        //
        // arrayList.add(this.getString(R.string.delay));
        // if (order.get_servertime().after(order.get_departuretime())) {
        // arrayList.add(this.getString(R.string.close));
        // }
        // arrayList.add(this.getString(R.string.call_office));
        //
        // // arrayList.add(this.getString(R.string.invite));
        // // else
        // // arrayList.add(this.getString(R.string.delay));
        // // arrayList.add(this.getString(R.string.call_office));
        // } else {
        // arrayList.add("Поторопить");
        // arrayList.add(this.getString(R.string.close));
        // arrayList.add(this.getString(R.string.call_office));
        //
        // }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getString(R.string.choose_action));
        builder.setItems(arrayList.toArray(new String[arrayList.size()]), onContextMenuItemListener());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private OnClickListener onContextMenuItemListener() {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // пригласить закрыть звонок из офиса отложить
                // если driverstate=1 и invitationtime NULL: ОТЛОЖИТЬ, ЗВОНОК ИЗ
                // ОФИСА
                // если driverstate=1 и invitationtime не NULL: ПОТОРОПИТЬ,
                // ЗАКРЫТЬ, ЗВОНОК ИЗ ОФИСА
                // если driverstate=2: ЗВОНОК ИЗ ОФИСА, ОТКАЗАТЬСЯ
                // если driverstate=3: ПРИНЯТЬ, ОТКЛОНИТЬ
                dialog.dismiss();
                if (order.get_driverstate() == 1) {
                    if (order.get_orderedtime() == null) {
                        if (order.get_servertime().before(order.get_departuretime())) {
                            if (order.get_invitationtime() == null) {
                                switch (item) {
                                    case 0:
                                        inviteDialog();
                                        break;
                                    case 1:
                                        timeDialog();
                                        break;
                                    case 2:
                                        closeDialog();
                                        break;
                                    case 3:
                                        callbackDialog();// ЗВОНОК ИЗ ОФИСА
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                switch (item) {
                                    case 0:
                                        inviteDialog();
                                        break;
                                    case 1:
                                        closeDialog();
                                        break;
                                    case 2:
                                        callbackDialog();// ЗВОНОК ИЗ ОФИСА
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else {
                            if (order.get_invitationtime() == null) { // дублирование
                                switch (item) {
                                    case 0:
                                        inviteDialog();
                                        break;
                                    case 1:
                                        closeDialog();
                                        break;
                                    case 2:
                                        callbackDialog();// ЗВОНОК ИЗ ОФИСА
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                switch (item) {
                                    case 0:
                                        inviteDialog();
                                        break;
                                    case 1:
                                        closeDialog();
                                        break;
                                    case 2:
                                        callbackDialog();// ЗВОНОК ИЗ ОФИСА
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    } else {
                        if (order.get_servertime().before(order.get_departuretime())) {
                            switch (item) {
                                case 0:
                                    timeDialog();
                                    break;
                                case 1:
                                    callbackDialog();// ЗВОНОК ИЗ ОФИСА
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            switch (item) {
                                case 0:
                                    inviteDialog();
                                    break;
                                case 1:
                                    closeDialog();// ЗВОНОК ИЗ ОФИСА
                                    break;
                                case 2:
                                    callbackDialog();// ЗВОНОК ИЗ ОФИСА
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                } else if (order.get_driverstate() == 2)
                    switch (item) {
                        case 0:
                            callbackDialog();// ЗВОНОК ИЗ ОФИСА
                            break;
                        case 1:
                            cancelDialog();
                            break;
                        default:
                            break;
                    }
                else if (order.get_driverstate() == 3)
                    switch (item) {
                        case 0:
                            acceptDialog();
                            break;
                        case 1:
                            cancelDialog();
                            break;
                        default:
                            break;
                    }
            }

        };
    }

    private void acceptDialog() {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "accept"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "order"));
        nameValuePairs.add(new BasicNameValuePair("orderid", String.valueOf(order.get_index())));

        Document doc = PhpData.postData(this, nameValuePairs, PhpData.newURL);
        if (doc != null) {
            Node responseNode = doc.getElementsByTagName("response").item(0);
            Node errorNode = doc.getElementsByTagName("message").item(0);

            if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                PhpData.errorFromServer(this, errorNode);
            else {
                finish();
            }
        }
    }

    private void cancelDialog() {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "refuse"));
        nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
        nameValuePairs.add(new BasicNameValuePair("object", "order"));
        nameValuePairs.add(new BasicNameValuePair("orderid", order.get_index()));

        Document doc = PhpData.postData(MyOrderItemActivity.this, nameValuePairs, PhpData.newURL);
        if (doc != null) {
            Node responseNode = doc.getElementsByTagName("response").item(0);
            Node errorNode = doc.getElementsByTagName("message").item(0);

            if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                PhpData.errorFromServer(MyOrderItemActivity.this, errorNode);
            else {
                finish();
            }
        }
    }

    private void callbackDialog() {
        new AlertDialog.Builder(MyOrderItemActivity.this).setTitle("Звонок")
                .setMessage("Вы действительно хотите заказать обратный звонок?")
                .setNegativeButton("Нет", null).setPositiveButton("Да", onCallbackClickListener()).show();

    }

    private OnClickListener onCallbackClickListener() {
        return new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                nameValuePairs.add(new BasicNameValuePair("action", "callback"));
                nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
                nameValuePairs.add(new BasicNameValuePair("object", "driver"));
                nameValuePairs.add(new BasicNameValuePair("orderid", order.get_index()));

                Document doc = PhpData.postData(MyOrderItemActivity.this, nameValuePairs, PhpData.newURL);
                if (doc != null) {
                    Node responseNode = doc.getElementsByTagName("response").item(0);
                    Node errorNode = doc.getElementsByTagName("message").item(0);

                    if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                        PhpData.errorFromServer(MyOrderItemActivity.this, errorNode);
                    else {
                        new AlertDialog.Builder(MyOrderItemActivity.this).setTitle("Звонок")
                                .setMessage("Ваш запрос принят. Пожалуйста ожидайте звонка")
                                .setNeutralButton("Ок", null).show();
                    }
                }
            }
        };
    }

    private void timerInit() {
        long diffInMs = currentTimer.getTime() - order.get_servertime().getTime();
        if (timer != null)
            timer.cancel();
        timer = new CountDownTimer(diffInMs, 1000) {

            public void onTick(long millisUntilFinished) {
                int seconds = ((int) millisUntilFinished / 1000) % 60;
                int minutes = ((int) millisUntilFinished / 1000) / 60;
                String secondsStr = String.valueOf(seconds);
                if (seconds <= 9)
                    secondsStr = "0" + seconds;

                counterView.setText("Время до приглашения: " + minutes + ":" + secondsStr);
                if (minutes == 1 && seconds == 0) {
                    initDelayOrInviteDialog();
                    MediaPlayer mp = MediaPlayer.create(getBaseContext(), (R.raw.sound));
                    mp.start();
                }
            }

            public void onFinish() {
                counterView.setText("Время до приглашения: "
                        + MyOrderItemActivity.this.getString(R.string.ended_timer));
                // timeDialog();
            }
        }.start();
    }

    private void initDelayOrInviteDialog() {
        new AlertDialog.Builder(MyOrderItemActivity.this).setTitle("Действие")
                .setMessage("Через минуту приглашаем клиентов")
                .setPositiveButton("Отсрочить", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        timeDialog();
                    }
                }).setNegativeButton("Пригласить", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        inviteDialog();
                    }
                }).show();
//        new Handler().postDelayed(new Runnable() {
//
//            public void run() {
//                dialog.dismiss();
//            }
//        }, 15000);
    }

    // private void alertDelay() {
    // AlertDialog.Builder alert = new
    // AlertDialog.Builder(MyOrderItemActivity.this);
    // alert.setTitle(this.getString(R.string.time));
    // final CharSequence cs[];
    //
    // cs = new String[] { "3", "5", "7", "10", "15", "20", "25", "30", "35" };
    //
    // alert.setItems(cs, new OnClickListener() {
    //
    // @Override
    // public void onClick(DialogInterface dialog, int which) {
    // // Bundle extras = getIntent().getExtras();
    // // int id = extras.getInt("id");
    // List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
    // nameValuePairs.add(new BasicNameValuePair("action", "saveminutes"));
    // nameValuePairs.add(new BasicNameValuePair("order_id",
    // String.valueOf(order.get_index())));
    // // nameValuePairs.add(new BasicNameValuePair("id",
    // // String.valueOf(id)));
    // nameValuePairs.add(new BasicNameValuePair("minutes",
    // String.valueOf(cs[which])));
    // Document doc = PhpData.postData(MyOrderItemActivity.this,
    // nameValuePairs);
    // if (doc != null) {
    // Node errorNode = doc.getElementsByTagName("error").item(0);
    // if (Integer.parseInt(errorNode.getTextContent()) == 1)
    // PhpData.errorHandler(MyOrderItemActivity.this, null);
    // else {
    // // Calendar cal = Calendar.getInstance();
    // // cal.setTime(new Date());
    // // cal.add(Calendar.MINUTE, Integer.valueOf((String) cs[which]));
    // // order.setTimerDate(cal.getTime());
    // // timerInit();
    // }
    // }
    // }
    //
    // });
    // alert.show();
    // }

    private void closeDialog() {

        // View view = getLayoutInflater().inflate(R.layout.custom_dialog,
        // null);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int isLightTheme = settings.getInt("theme", 0);
        if (isLightTheme != 0)
            dialog = new Dialog(this, android.R.style.Theme_Light);
        else
            dialog = new Dialog(this, android.R.style.Theme_Black);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setTitle(this.getString(R.string.price));
        dialog.show();

        Button btn = (Button) dialog.findViewById(R.id.button1);
        EditText input = (EditText) dialog.findViewById(R.id.editText1);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        btn.setOnClickListener(onSavePrice(dialog));

        LinearLayout ll = (LinearLayout) dialog.findViewById(R.id.layout1);
        if (order.get_paymenttype() == 1 || (order.getAbonent() != null && order.get_paymenttype() == 0))// безнал
            ll.setVisibility(View.VISIBLE);
        if (order.getAbonent() != null && order.get_paymenttype() == 0)
            ((TextView) dialog.findViewById(R.id.textView2)).setText("Сдача");

//        final CheckBox cb = (CheckBox) dialog.findViewById(R.id.checkBox1);
        final TextView tv = (TextView) dialog.findViewById(R.id.textView3);
        Button btn1 = (Button) dialog.findViewById(R.id.button2);
        Button btn2 = (Button) dialog.findViewById(R.id.button3);
        tv.setText(MyOrderItemActivity.this.getText(R.string.end_point) + " " + order.get_addressarrival());
        btn1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(MyOrderItemActivity.this, DistrictActivity.class);
                intent.putExtra("close", true);
                if (PhpData.isNetworkAvailable(MyOrderItemActivity.this))
                    startActivityForResult(intent, REQUEST_EXIT);
                else {
                    setResult(RESULT_OK);
                    finish();
                }
            }

        });
        btn2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                bundle = null;
                tv.setText(MyOrderItemActivity.this.getText(R.string.end_point) + " "
                        + order.get_addressarrival());
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                bundle = data.getExtras();
                TextView tv = (TextView) dialog.findViewById(R.id.textView3);
                String district = bundle.getString("districtname");
                String subdistrict = bundle.getString("subdistrictname");
                String rayonString = "";
                if (district != null) {
                    rayonString = district;
                    if (subdistrict != null)
                        rayonString += ", " + subdistrict;
                }
                tv.setText("Район: " + rayonString);
            }
        }
    }

    private Button.OnClickListener onSavePrice(final Dialog dialog) {
        return new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText input = (EditText) dialog.findViewById(R.id.editText1);
                EditText cashless = (EditText) dialog.findViewById(R.id.editText2);
                RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.radioGroup1);
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                String state = "1";
                if (checkedRadioButtonId == R.id.radio0) {
                    state = "1";
                } else if (checkedRadioButtonId == R.id.radio1) {
                    state = "0";
                }

                if (input.getText().length() != 0) {
                    String value = input.getText().toString();

                    String cashvalue;
                    if (cashless.getText().length() == 0 && order.get_paymenttype() == 1)
                        cashvalue = value;
                    else {
                        cashvalue = "0";

                        if (order.get_paymenttype() == 1)
                            cashvalue = cashless.getText().toString();
                        else if (order.getAbonent() != null)
                            cashvalue = "-" + cashless.getText().toString();
                    }

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    nameValuePairs.add(new BasicNameValuePair("action", "close"));
                    nameValuePairs.add(new BasicNameValuePair("module", "mobile"));
                    nameValuePairs.add(new BasicNameValuePair("object", "order"));
                    nameValuePairs.add(new BasicNameValuePair("orderid", order.get_index()));
                    // if (order.get_nominalcost() != null)
                    // orderCost = String.valueOf(order.get_nominalcost());
                    nameValuePairs.add(new BasicNameValuePair("cost", value));
                    nameValuePairs.add(new BasicNameValuePair("cashless", cashvalue));
                    nameValuePairs.add(new BasicNameValuePair("state", state));
                    if (bundle != null) {
                        nameValuePairs
                                .add(new BasicNameValuePair("districtid", bundle.getString("district")));
                        // nameValuePairs.add(new
                        // BasicNameValuePair("subdistrictid",
                        // bundle.getString("subdistrict")));
                    }
                    Document doc = PhpData.postData(MyOrderItemActivity.this, nameValuePairs, PhpData.newURL);
                    if (doc != null) {
                        Node responseNode = doc.getElementsByTagName("response").item(0);
                        Node errorNode = doc.getElementsByTagName("message").item(0);

                        if (responseNode.getTextContent().equalsIgnoreCase("failure"))
                            PhpData.errorFromServer(MyOrderItemActivity.this, errorNode);
                        else {
                            dialog.dismiss();
                            new AlertDialog.Builder(MyOrderItemActivity.this)
                                    .setTitle(MyOrderItemActivity.this.getString(R.string.Ok))
                                    .setMessage(MyOrderItemActivity.this.getString(R.string.order_closed))
                                    .setNeutralButton(MyOrderItemActivity.this.getString(R.string.close),
                                            new OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if (timer != null)
                                                        timer.cancel();
                                                    setResult(RESULT_OK);
                                                    finish();
                                                }
                                            }).show();
                        }
                    }
                }
            }
        }

        ;
    }

}
