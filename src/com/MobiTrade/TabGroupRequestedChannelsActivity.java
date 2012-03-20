package com.MobiTrade;

import android.content.Intent;
import android.os.Bundle;

public class TabGroupRequestedChannelsActivity extends TabGroupActivity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startChildActivity("RequestedChannelsTab", new Intent(this, RequestedChannelsTab.class));
    }
}
