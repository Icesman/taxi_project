package ru.peppers;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class BalanceActivity extends Activity {

	public TextView title;
	private TextView balance;

	@Override
    public void onCreate(Bundle savedInstanceState) {
	    setTheme(android.R.style.Theme_Light);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        setContentView(R.layout.main);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);

        title = (TextView) findViewById(R.id.titleView);
        balance = (TextView) findViewById(R.id.balanceView);
        title.setText(this.getTitle());
        updateBalance();
	}

	public void updateBalance(){
        if(TaxiApplication.getDriver()!=null)
        	balance.setText("������: "+ TaxiApplication.getDriver().getBalance());
	}
}
