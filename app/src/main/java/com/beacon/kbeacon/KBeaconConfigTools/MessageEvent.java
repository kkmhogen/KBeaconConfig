package com.beacon.kbeacon.KBeaconConfigTools;

/**
 * Created by qindachang on 2017/3/12.
 */
public class MessageEvent {
    private boolean isDfu;

    public MessageEvent(boolean isDfu) {
        this.isDfu = isDfu;
    }

    public boolean isDfu() {
        return isDfu;
    }

    public void setDfu(boolean dfu) {
        isDfu = dfu;
    }
}
