package org.digitalcampus.oppia.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ngo.lal.oppia.R;
import org.digitalcampus.oppia.activity.PrefsActivity;

public class RemoteApiEndpoint implements ApiEndpoint{

    @Override
    public String getFullURL(Context ctx, String apiPath) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(PrefsActivity.PREF_SERVER, ctx.getString(R.string.prefServerDefault)) + apiPath;
    }
}
