package org.ruan.connection;

import android.content.Context;


import java.io.File;
import java.util.Date;

public abstract class AndroidMavLinkConnection extends MavLinkConnection {

    private static final String TAG = AndroidMavLinkConnection.class.getSimpleName();

    protected final Context mContext;

    public AndroidMavLinkConnection(Context applicationContext) {
        this.mContext = applicationContext;
    }

    @Override
    protected final Logger initLogger() {
        return AndroidLogger.getLogger();
    }
}
