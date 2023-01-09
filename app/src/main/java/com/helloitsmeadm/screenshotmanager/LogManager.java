package com.helloitsmeadm.screenshotmanager;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class LogManager extends AppCompatActivity {
    public static void log(Context context, int category, int event, String extra) {
        //get current hours, minutes and seconds
        Calendar c = Calendar.getInstance();
        String hours = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        String minutes = String.valueOf(c.get(Calendar.MINUTE));
        String seconds = String.valueOf(c.get(Calendar.SECOND));
        String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        String month = String.valueOf(c.get(Calendar.MONTH));
        String year = String.valueOf(c.get(Calendar.YEAR));

        // Fix zeros
        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        if (seconds.length() == 1) {
            seconds = "0" + seconds;
        }

        // put it in a string
        String time = day + "." + month + "." + year + " " + hours + ":" + minutes + ":" + seconds;

        /*
         * JSON format:
         * CATEGORY (History (0), System (1))
         * EVENT (Started watching (0), Detected (1), Service (2), --- (3), Deleted (4), Filtered (5), Failed to delete (6), No longer exists (7))
         * TIME
         * MESSAGE
         * */

        String logType;
        if (category == 0) {
            logType = "log";
        } else if (category == 1) {
            logType = "system";
        } else {
            logType = "pending";
        }

        // get shared preferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("log", Context.MODE_PRIVATE);
        // get all logs
        String logs = sharedPreferences.getString(logType, "");
        // create new json object
        JSONObject jsonObject = new JSONObject();
        try {
            // put data
            jsonObject.put("category", category);
            jsonObject.put("event", event);
            jsonObject.put("time", time);
            jsonObject.put("extra", extra);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Error while creating JSON object\n" + e.getMessage());
        }
        // add new log to old logs reversed
        logs = jsonObject + "\n" + logs;
        // save logs
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(logType, logs);
        editor.apply();

        updateWidget(context);
    }

    public static void addPending(Context context, String justFile, long scheduledTime, String fullPath, String formattedTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("pending", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(justFile, justFile + "|" + scheduledTime + "|" + fullPath);
        editor.apply();

        if (scheduledTime > 0) {
            LogManager.log(context, 0, 3, justFile + "|" + formattedTime);
        }
        updateWidget(context);
    }

    public static void updateWidget(Context context) {
        // Update widget
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, PendingWidget.class));
        PendingWidget pendingWidget = new PendingWidget();
        pendingWidget.onUpdate(context, AppWidgetManager.getInstance(context), ids);
    }
}
