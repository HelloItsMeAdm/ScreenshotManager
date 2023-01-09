package com.helloitsmeadm.screenshotmanager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class AutoDelete {
    public void run(Context context, Activity activity) {
        // get shared preferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("pending", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        // auto delete logs
        checkIfLogsAreFull(context);

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String[] values = entry.getValue().toString().split("\\|");
            // get current time
            long currentTime = System.currentTimeMillis();

            File file = new File(values[2]);
            if (!file.exists()) {
                removeFromDatabase(context, entry.getKey());
                //ToastManager.run(activity, 3, R.drawable.red_error, "File " + values[2] + " no longer exists!");
                LogManager.log(context, 0, 7, values[2]);
                addCount(context);
            }

            // check if values[1] is past the current time
            if (Long.parseLong(values[1]) < currentTime) {
                // delete the file
                int count = 0;
                while (file.exists()) {
                    file.delete();
                    count++;
                }

                if (count != 0) {
                    //ToastManager.run(activity, 3, R.drawable.green_delete, "File " + values[0] + " deleted!");
                    LogManager.log(context, 0, 4, count + "|" + values[0]);
                    addCount(context);
                }
                // delete the log entry
                sharedPreferences.edit().remove(entry.getKey()).apply();
            }
        }
    }

    public static void addTime(Context context, String key, long addTime, String formattedTime, Activity activity) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("pending", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        String value = allEntries.get(key).toString();
        String[] values = value.split("\\|");

        long newTime = Long.parseLong(values[1]) + addTime;
        String newTimeString = String.valueOf(newTime);
        value = values[0] + "|" + newTimeString + "|" + values[2];

        sharedPreferences.edit().putString(key, value).apply();
        ToastManager.run(activity, 3, R.drawable.green_checkmark, "Added " + formattedTime + " to " + key + ".");
    }

    public static void removeFromDatabase(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("pending", Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(key).apply();
    }

    private static void checkIfLogsAreFull(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("log", Context.MODE_PRIVATE);

        String[] logLength = sharedPreferences.getString("log", "").split("\n");
        String[] sysLength = sharedPreferences.getString("system", "").split("\n");

        if (logLength.length > 50 || sysLength.length > 50) {
            sharedPreferences.edit().clear().apply();
        }
    }

    private static void addCount(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("totalCount", Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt("num", sharedPreferences.getInt("num", 0) + 1).apply();
    }
}