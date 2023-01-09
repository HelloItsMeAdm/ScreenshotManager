package com.helloitsmeadm.screenshotmanager;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Map;

public class PendingWidget extends AppWidgetProvider {
    Spanned values1, values2, values3, values4;
    Spanned time1, time2, time3, time4;
    Uri uri1, uri2, uri3, uri4;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pending_widget);

            // Clear items
            views.removeAllViews(R.id.logsContainer);

            // last updated
            lastUpdated(views);

            // Setonclicklistener
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.main, pendingIntent);

            // Read log from shared preferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("pending", Context.MODE_PRIVATE);

            // Create TextView for each log entry
            Map<String, ?> allEntries = sharedPreferences.getAll();

            int extraItems = 0;
            int items = 0;
            int totalItems = allEntries.size();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                items++;
                if (items < 5) {
                    String[] values = entry.getValue().toString().split("\\|");
                    long time = Long.parseLong(values[1]);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(time);
                    String hours = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
                    String minutes = String.valueOf(calendar.get(Calendar.MINUTE));
                    String seconds = String.valueOf(calendar.get(Calendar.SECOND));
                    if (hours.length() == 1) {
                        hours = "0" + hours;
                    }
                    if (minutes.length() == 1) {
                        minutes = "0" + minutes;
                    }
                    if (seconds.length() == 1) {
                        seconds = "0" + seconds;
                    }
                    String newTime = calendar.get(Calendar.DAY_OF_MONTH) + "." + calendar.get(Calendar.MONTH) + "." + calendar.get(Calendar.YEAR) + " at " + hours + ":" + minutes + ":" + seconds;

                    String shortedText = values[0];
                    if (values[0].length() > 33) {
                        shortedText = values[0].substring(0, 33) + "...";
                    }

                    if (items == 1) {
                        //1 item
                        values1 = Html.fromHtml("<b>File</b>: " + shortedText);
                        time1 = Html.fromHtml("<b>Time</b>: " + newTime);
                        uri1 = Uri.parse(values[2]);
                    } else if (items == 2) {
                        //2 items
                        values2 = Html.fromHtml("<b>File</b>: " + shortedText);
                        time2 = Html.fromHtml("<b>Time</b>: " + newTime);
                        uri2 = Uri.parse(values[2]);
                    } else if (items == 3) {
                        //3 items
                        values3 = Html.fromHtml("<b>File</b>: " + shortedText);
                        time3 = Html.fromHtml("<b>Time</b>: " + newTime);
                        uri3 = Uri.parse(values[2]);
                    } else if (items == 4) {
                        //4 items
                        values4 = Html.fromHtml("<b>File</b>: " + shortedText);
                        time4 = Html.fromHtml("<b>Time</b>: " + newTime);
                        uri4 = Uri.parse(values[2]);
                    } else {
                        //5+ items
                        break;
                    }
                } else {
                    extraItems++;
                }
            }
            views.setViewVisibility(R.id.noLogs, View.INVISIBLE);
            views.setViewVisibility(R.id.extra, View.INVISIBLE);

            if (totalItems == 0) {
                views.setViewVisibility(R.id.noLogs, View.VISIBLE);

                views.setViewVisibility(R.id.one, View.INVISIBLE);
                views.setViewVisibility(R.id.line1, View.INVISIBLE);

                views.setViewVisibility(R.id.two, View.INVISIBLE);
                views.setViewVisibility(R.id.line2, View.INVISIBLE);

                views.setViewVisibility(R.id.three, View.INVISIBLE);
                views.setViewVisibility(R.id.line3, View.INVISIBLE);

                views.setViewVisibility(R.id.four, View.INVISIBLE);
                views.setViewVisibility(R.id.line4, View.INVISIBLE);
            } else if (totalItems == 1) {
                //1 item
                views.setViewVisibility(R.id.one, View.VISIBLE);
                views.setTextViewText(R.id.upperText1, values1);
                views.setTextViewText(R.id.lowerText1, time1);
                views.setImageViewUri(R.id.leftImage1, uri1);
                views.setViewVisibility(R.id.line1, View.INVISIBLE);

                views.setViewVisibility(R.id.two, View.INVISIBLE);
                views.setViewVisibility(R.id.line2, View.INVISIBLE);

                views.setViewVisibility(R.id.three, View.INVISIBLE);
                views.setViewVisibility(R.id.line3, View.INVISIBLE);

                views.setViewVisibility(R.id.four, View.INVISIBLE);
                views.setViewVisibility(R.id.line4, View.INVISIBLE);
            } else if (totalItems == 2) {
                //2 items
                views.setViewVisibility(R.id.one, View.VISIBLE);
                views.setTextViewText(R.id.upperText1, values1);
                views.setTextViewText(R.id.lowerText1, time1);
                views.setImageViewUri(R.id.leftImage1, uri1);
                views.setViewVisibility(R.id.line1, View.VISIBLE);

                views.setViewVisibility(R.id.two, View.VISIBLE);
                views.setTextViewText(R.id.upperText2, values2);
                views.setTextViewText(R.id.lowerText2, time2);
                views.setImageViewUri(R.id.leftImage2, uri2);
                views.setViewVisibility(R.id.line2, View.INVISIBLE);

                views.setViewVisibility(R.id.three, View.INVISIBLE);
                views.setViewVisibility(R.id.line3, View.INVISIBLE);

                views.setViewVisibility(R.id.four, View.INVISIBLE);
                views.setViewVisibility(R.id.line4, View.INVISIBLE);
            } else if (totalItems == 3) {
                //3 items
                views.setViewVisibility(R.id.one, View.VISIBLE);
                views.setTextViewText(R.id.upperText1, values1);
                views.setTextViewText(R.id.lowerText1, time1);
                views.setImageViewUri(R.id.leftImage1, uri1);
                views.setViewVisibility(R.id.line1, View.VISIBLE);

                views.setViewVisibility(R.id.two, View.VISIBLE);
                views.setTextViewText(R.id.upperText2, values2);
                views.setTextViewText(R.id.lowerText2, time2);
                views.setImageViewUri(R.id.leftImage2, uri2);
                views.setViewVisibility(R.id.line2, View.VISIBLE);

                views.setViewVisibility(R.id.three, View.VISIBLE);
                views.setTextViewText(R.id.upperText3, values3);
                views.setTextViewText(R.id.lowerText3, time3);
                views.setImageViewUri(R.id.leftImage3, uri3);
                views.setViewVisibility(R.id.line3, View.INVISIBLE);

                views.setViewVisibility(R.id.four, View.INVISIBLE);
                views.setViewVisibility(R.id.line4, View.INVISIBLE);
            } else if (totalItems == 4) {
                //4 items
                views.setViewVisibility(R.id.one, View.VISIBLE);
                views.setTextViewText(R.id.upperText1, values1);
                views.setTextViewText(R.id.lowerText1, time1);
                views.setImageViewUri(R.id.leftImage1, uri1);
                views.setViewVisibility(R.id.line1, View.VISIBLE);

                views.setViewVisibility(R.id.two, View.VISIBLE);
                views.setTextViewText(R.id.upperText2, values2);
                views.setTextViewText(R.id.lowerText2, time2);
                views.setImageViewUri(R.id.leftImage2, uri2);
                views.setViewVisibility(R.id.line2, View.VISIBLE);

                views.setViewVisibility(R.id.three, View.VISIBLE);
                views.setTextViewText(R.id.upperText3, values3);
                views.setTextViewText(R.id.lowerText3, time3);
                views.setImageViewUri(R.id.leftImage3, uri3);
                views.setViewVisibility(R.id.line3, View.VISIBLE);

                views.setViewVisibility(R.id.four, View.VISIBLE);
                views.setTextViewText(R.id.upperText4, values4);
                views.setTextViewText(R.id.lowerText4, time4);
                views.setImageViewUri(R.id.leftImage4, uri4);
                views.setViewVisibility(R.id.line4, View.INVISIBLE);
            } else {
                //5+ items
                views.setViewVisibility(R.id.one, View.VISIBLE);
                views.setTextViewText(R.id.upperText1, values1);
                views.setTextViewText(R.id.lowerText1, time1);
                views.setImageViewUri(R.id.leftImage1, uri1);
                views.setViewVisibility(R.id.line1, View.VISIBLE);

                views.setViewVisibility(R.id.two, View.VISIBLE);
                views.setTextViewText(R.id.upperText2, values2);
                views.setTextViewText(R.id.lowerText2, time2);
                views.setImageViewUri(R.id.leftImage2, uri2);
                views.setViewVisibility(R.id.line2, View.VISIBLE);

                views.setViewVisibility(R.id.three, View.VISIBLE);
                views.setTextViewText(R.id.upperText3, values3);
                views.setTextViewText(R.id.lowerText3, time3);
                views.setImageViewUri(R.id.leftImage3, uri3);
                views.setViewVisibility(R.id.line3, View.VISIBLE);

                views.setViewVisibility(R.id.four, View.VISIBLE);
                views.setTextViewText(R.id.upperText4, values4);
                views.setTextViewText(R.id.lowerText4, time4);
                views.setImageViewUri(R.id.leftImage4, uri4);
                views.setViewVisibility(R.id.line4, View.VISIBLE);

                views.setViewVisibility(R.id.extra, View.VISIBLE);
                views.setTextViewText(R.id.extra, Html.fromHtml("Show <b>" + extraItems + "</b> more items"));
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void lastUpdated(RemoteViews views) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String hours = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minutes = String.valueOf(calendar.get(Calendar.MINUTE));
        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        String newTime = hours + ":" + minutes;

        views.setTextViewText(R.id.lastUpdate, Html.fromHtml("Last updated at <b>" + newTime + "</b>."));
    }
}