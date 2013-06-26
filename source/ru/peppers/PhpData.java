package ru.peppers;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

final public class PhpData {
    static boolean withDebug = false;
    static String sessionid = "";

    public static HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    static public Document postData(Activity activity, List<NameValuePair> nameValuePairs, String url) {
        if (isNetworkAvailable(activity)) {

            // Create a new HttpClient and Post Header
            HttpClient httpclient = getNewHttpClient();
            HttpPost httppost = new HttpPost(url);
            // http://sandbox.peppers-studio.ru/dell/accelerometer/index.php
            // http://10.0.2.2/api
            try {
                // Add your data
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                if (sessionid != "" && url == "https://www.abs-taxi.ru/fcgi-bin/office/cman.fcgi")
                    httppost.setHeader("cookie", "cmansid=" + sessionid);
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                Document doc;
                if (withDebug) {
                    String str = EntityUtils.toString(response.getEntity());
                    Log.d("My_tag", str);
                    InputSource is = new InputSource();
                    is.setCharacterStream(new StringReader(str));
                    // response.getEntity().getContent()
                    doc = builder.parse(is);
                } else {
                    doc = builder.parse(response.getEntity().getContent());

                }

                return doc;

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("��������� ������ � ���������� � ��������.")
                        .setNeutralButton("�������", null).show();
            } catch (IOException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("��������� ������ � ���������� � ��������.")
                        .setNeutralButton("�������", null).show();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("������ � ��������� ������ �� �������.")
                        .setNeutralButton("�������", null).show();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("������ � ��������� ������ �� �������.")
                        .setNeutralButton("�������", null).show();
            } catch (SAXException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("������ � ��������� ������ �� �������.")
                        .setNeutralButton("�������", null).show();
            }
        } else {
            new AlertDialog.Builder(activity).setTitle("������")
                    .setMessage("����������� � ��������� �����������.").setNeutralButton("�������", null)
                    .show();
        }
        Log.d("My_tag", "no connection");
        return null;
    }

    static public Document postData(Activity activity, List<NameValuePair> nameValuePairs) {

        return postData(activity, nameValuePairs,
                "http://sandbox.peppers-studio.ru/dell/accelerometer/index.php");

    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Date getFileDate(Activity activity) {
        if (isNetworkAvailable(activity)) {

            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpHead httpphead = new HttpHead(
                    "http://sandbox.peppers-studio.ru/dell/accelerometer/TaxiProject.apk");
            // http://sandbox.peppers-studio.ru/dell/accelerometer/index.php
            // http://10.0.2.2/api
            try {

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httpphead);
                // for(int i = 0; i<response.getAllHeaders().length;i++){
                // Log.d("My_tag",response.getAllHeaders()[i].toString());
                // }
                Log.d("My_tag", response.getFirstHeader("Last-Modified").getValue());

                return new Date(response.getFirstHeader("Last-Modified").getValue());

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("��������� ������ � ���������� � ��������.")
                        .setNeutralButton("�������", null).show();
            } catch (IOException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("��������� ������ � ���������� � ��������.")
                        .setNeutralButton("�������", null).show();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                new AlertDialog.Builder(activity).setTitle("������")
                        .setMessage("������ � ��������� ������ �� �������.")
                        .setNeutralButton("�������", null).show();
            }
        } else {
            new AlertDialog.Builder(activity).setTitle("������")
                    .setMessage("����������� � ��������� �����������.").setNeutralButton("�������", null)
                    .show();
        }
        Log.d("My_tag", "no connection");
        return null;

    }

}