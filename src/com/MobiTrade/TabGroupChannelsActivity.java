package com.MobiTrade;

import android.content.Intent;
import android.os.Bundle;

public class TabGroupChannelsActivity extends TabGroupActivity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startChildActivity("ChannelsTab", new Intent(this, ChannelsTab.class));
        
    }
}
