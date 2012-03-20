package com.MobiTrade;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class TabGroupDashboardActivity extends TabGroupActivity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startChildActivity("DashboardTab", new Intent(this, DashboardTab.class));
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	
        	if(getNumberOfChildActivities() > 1)
        		return true;
        	else finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
