package com.helloitsmeadm.screenshotmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    Spinner logType;
    int logTypeSelected = 0;
    Button deleteLogs, manualAdd, manualDelete;
    LottieAnimationView refresh;
    Handler widgerHandler = new Handler();
    TextView subtitle;

    private FileObserver screenshotsFileObserver;
    public static String screenshotPath = "/storage/emulated/0/DCIM/Screenshots";

    private FileObserver tiktokFileObserver;
    public static String tiktokPath = "/storage/emulated/0/DCIM/Camera";

    private FileObserver screenRecFileObserver;
    public static String screenRecPath = "/storage/emulated/0/DCIM/ScreenRecorder";

    private FileObserver pdfFileObserver;
    public static String pdfPath = "/storage/emulated/0/Download";

    private FileObserver whatsappFileObserver;
    public static String whatsappPath = "/storage/emulated/0/Pictures/Whatsapp";

    /*
     * TODO:
     * - DCMI/Camera waiting list for check
     * - Multi change time
     * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // run AutoDelete
        final Handler autoDeleteHandler = new Handler();
        autoDeleteHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AutoDelete autoDelete = new AutoDelete();
                autoDelete.run(MainActivity.this, MainActivity.this);
                autoDeleteHandler.postDelayed(this, 3000);
            }
        }, 3000);

        // Buttons
        manualDelete = findViewById(R.id.manualDelete);
        manualDelete.setOnClickListener(view -> {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
            builderSingle.setTitle("Select which files you want to delete.");

            //get all pending items
            SharedPreferences sharedPreferences = getSharedPreferences("pending", Context.MODE_PRIVATE);
            Map<String, ?> allEntries = sharedPreferences.getAll();
            final List<String> pendingItems = new ArrayList<>();
            final List<String> whatToDelete = new ArrayList<>();
            AtomicInteger deleteCount = new AtomicInteger();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String[] values = entry.getValue().toString().split("\\|");
                pendingItems.add(values[0]);
            }

            new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, pendingItems);
            builderSingle.setMultiChoiceItems(pendingItems.toArray(new String[0]), null, (dialogInterface, i, b) -> {
                if (b) {
                    whatToDelete.add(pendingItems.get(i));
                    deleteCount.getAndIncrement();
                } else {
                    whatToDelete.remove(pendingItems.get(i));
                    deleteCount.getAndDecrement();
                }
            });
            builderSingle.setPositiveButton("OK", (dialogInterface, i) -> {
                for (String item : whatToDelete) {
                    String[] values = item.split("\\|");
                    deleteTimer(MainActivity.this, values[0], 0, false, this);
                }
                ToastManager.run(this, 3, R.drawable.green_delete, "Deleted " + deleteCount + " files.");
                updateLogWidget();
            });
            builderSingle.setNegativeButton("Cancel", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                pendingItems.clear();
                whatToDelete.clear();
            });
            builderSingle.show();
        });

        refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(view -> {
            updateLogWidget();
            updateWidget(MainActivity.this);
            refresh.playAnimation();
        });

        manualAdd = findViewById(R.id.manualAdd);
        manualAdd.setOnClickListener(view -> {
            // Ask user to select multiple files
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, 1);
        });

        // Setup logType spinner
        logType = findViewById(R.id.logType);
        logType.setSelection(logTypeSelected);
        logType.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                logTypeSelected = i;
                updateLogWidget();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Setup deleteLogs button
        deleteLogs = findViewById(R.id.deleteLogs);
        deleteLogs.setOnClickListener(view -> {
            // Ask user if they want to delete logs using dialog
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SharedPreferences sharedPrefLog = getSharedPreferences("log", Context.MODE_PRIVATE);
                        SharedPreferences sharedPrefPending = getSharedPreferences("pending", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editorLog = sharedPrefLog.edit();
                        SharedPreferences.Editor editorPending = sharedPrefPending.edit();
                        editorLog.clear();
                        editorLog.apply();
                        editorPending.clear();
                        editorPending.apply();
                        updateLogWidget();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.cancel();
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Are you sure you want to delete logs?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        });

        subtitle = findViewById(R.id.subtitle);
        SharedPreferences totalSharedPreferences = getSharedPreferences("totalCount", Context.MODE_PRIVATE);
        subtitle.setText(Html.fromHtml("Total files deleted - <b>" + totalSharedPreferences.getInt("num", 0) + "</b>"));

        // Show log
        updateLogWidget();

        ////////////////////////////////////////////////////////////////////////////////////////////
        // FileObserver for screenshot folder
        ////////////////////////////////////////////////////////////////////////////////////////////
        if (screenshotsFileObserver == null) {
            screenshotsFileObserver = new FileObserver(screenshotPath, FileObserver.CREATE) {
                @Override
                public void onEvent(int event, String path) {
                    if (event == FileObserver.CREATE) {
                        runOnUiThread(() -> {
                            ToastManager.run(MainActivity.this, 3, R.drawable.green_screenshot, "Screenshot detected!\nWaiting for interaction.");
                            new Handler().postDelayed(() -> {
                                LogManager.log(getApplicationContext(), 0, 1, path);
                                updateLogWidget();

                                // Call dialog to notify user of screenshot
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, 16);
                                    } else {
                                        String fullPath = screenshotPath + "/" + path;
                                        DialogManager dialogManager = new DialogManager(MainActivity.this, path, fullPath, "Screenshot detected!", "image");
                                        dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                        dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                        dialogManager.setOnCancelListener(dialogInterface -> {
                                            dialogManager.dismiss();

                                            // Delete after 15 mins
                                            deleteTimer(MainActivity.this, fullPath, 900000, true, MainActivity.this);
                                        });

                                        dialogManager.show();
                                    }
                                }
                            }, 3000);
                        });
                    }
                }
            };
            screenshotsFileObserver.startWatching();
            LogManager.log(getApplicationContext(), 1, 0, screenshotPath);
            ////////////////////////////////////////////////////////////////////////////////////////////
            // FileObserver for tiktok video folder
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (tiktokFileObserver == null) {
                tiktokFileObserver = new FileObserver(tiktokPath, FileObserver.CREATE) {
                    @Override
                    public void onEvent(int event, String path) {
                        if (event == FileObserver.CREATE) {
                            if (path.contains("VID_") || path.contains("IMG_") || path.contains("PANO_")) {
                                runOnUiThread(() -> {
                                    LogManager.log(getApplicationContext(), 0, 5, path);
                                    updateLogWidget();
                                });
                                return;
                            }
                            runOnUiThread(() -> {
                                LogManager.log(getApplicationContext(), 0, 1, path);
                                updateLogWidget();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, 16);
                                    } else {
                                        String fullPath = tiktokPath + "/" + path;
                                        DialogManager dialogManager = new DialogManager(MainActivity.this, path, fullPath, "Tiktok video detected!", "video");
                                        dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                        dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                        dialogManager.setOnCancelListener(dialogInterface -> {
                                            dialogManager.dismiss();

                                            // Delete after 15 mins
                                            deleteTimer(MainActivity.this, fullPath, 900000, true, MainActivity.this);
                                        });

                                        dialogManager.show();
                                    }
                                }
                            });
                        }
                    }
                };
            }
            tiktokFileObserver.startWatching();
            LogManager.log(getApplicationContext(), 1, 0, tiktokPath);
            ////////////////////////////////////////////////////////////////////////////////////////////
            // FileObserver for screen recorder folder
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (screenRecFileObserver == null) {
                screenRecFileObserver = new FileObserver(screenRecPath, FileObserver.MOVED_TO) {
                    @Override
                    public void onEvent(int event, String path) {
                        if (event == FileObserver.MOVED_TO) {
                            runOnUiThread(() -> {
                                LogManager.log(getApplicationContext(), 0, 1, path);
                                updateLogWidget();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, 16);
                                    } else {
                                        String fullPath = screenRecPath + "/" + path;
                                        DialogManager dialogManager = new DialogManager(MainActivity.this, path, fullPath, "Screen record detected!", "video");
                                        dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                        dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                        dialogManager.setOnCancelListener(dialogInterface -> {
                                            dialogManager.dismiss();

                                            // Delete after 15 mins
                                            deleteTimer(MainActivity.this, fullPath, 900000, true, MainActivity.this);
                                        });

                                        dialogManager.show();
                                    }
                                }
                            });
                        }
                    }
                };
            }
            screenRecFileObserver.startWatching();
            LogManager.log(getApplicationContext(), 1, 0, screenRecPath);
            ////////////////////////////////////////////////////////////////////////////////////////////
            // FileObserver for pdf
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (pdfFileObserver == null) {
                pdfFileObserver = new FileObserver(pdfPath, FileObserver.MOVED_TO) {
                    @Override
                    public void onEvent(int event, String path) {
                        if (event == FileObserver.MOVED_TO && path.endsWith(".pdf") || path.endsWith(".doc") || path.endsWith(".docx")) {
                            runOnUiThread(() -> {
                                ToastManager.run(MainActivity.this, 3, R.drawable.green_file, "Downloaded file detected!\nWaiting for interaction.");
                                new Handler().postDelayed(() -> {
                                    LogManager.log(getApplicationContext(), 0, 1, path);
                                    updateLogWidget();

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (!Settings.canDrawOverlays(MainActivity.this)) {
                                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:" + getPackageName()));
                                            startActivityForResult(intent, 16);
                                        } else {
                                            String fullPath = pdfPath + "/" + path;
                                            DialogManager dialogManager = new DialogManager(MainActivity.this, path, fullPath, "Downloaded file detected!", "file");
                                            dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                            dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                            dialogManager.setOnCancelListener(dialogInterface -> {
                                                dialogManager.dismiss();

                                                // Delete after 15 mins
                                                deleteTimer(MainActivity.this, fullPath, 900000, true, MainActivity.this);
                                            });

                                            dialogManager.show();
                                        }
                                    }
                                }, 5000);
                            });
                        }
                    }
                };
            }
            pdfFileObserver.startWatching();
            LogManager.log(getApplicationContext(), 1, 0, pdfPath);
            ////////////////////////////////////////////////////////////////////////////////////////////
            // FileObserver for screenshot folder
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (whatsappFileObserver == null) {
                whatsappFileObserver = new FileObserver(whatsappPath, FileObserver.CREATE) {
                    @Override
                    public void onEvent(int event, String path) {
                        if (event == FileObserver.CREATE) {
                            runOnUiThread(() -> {
                                LogManager.log(getApplicationContext(), 0, 1, path);
                                updateLogWidget();

                                // Call dialog to notify user of screenshot
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, 16);
                                    } else {
                                        String fullPath = whatsappPath + "/" + path;
                                        String filePath;
                                        if (path.toLowerCase().contains("img") || path.toLowerCase().contains("jpg") || path.toLowerCase().contains("jpeg")) {
                                            filePath = "image";
                                        } else if (path.toLowerCase().contains("mp4")) {
                                            filePath = "video";
                                        } else {
                                            filePath = "file";
                                        }
                                        DialogManager dialogManager = new DialogManager(MainActivity.this, path, fullPath, "WhatsApp file detected!", filePath);
                                        dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                        dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                        dialogManager.setOnCancelListener(dialogInterface -> {
                                            dialogManager.dismiss();

                                            // Delete after 15 mins
                                            deleteTimer(MainActivity.this, fullPath, 900000, true, MainActivity.this);
                                        });

                                        dialogManager.show();
                                    }
                                }
                            });
                        }
                    }
                };
            }
            whatsappFileObserver.startWatching();
            LogManager.log(getApplicationContext(), 1, 0, whatsappPath);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////
    // Logging widget
    ////////////////////////////////////////////////////////////////////////////////////////////
    private void updateLogWidget() {
        // Widgets
        LinearLayout logsContainer = findViewById(R.id.logsContainer);
        logsContainer.removeAllViews();
        TextView noLogs = findViewById(R.id.noLogs);
        noLogs.setVisibility(View.VISIBLE);

        // Get what log type to show
        String getLogType;
        String key;
        int newLogTypeSelected;
        if (logTypeSelected == 0) {
            getLogType = "pending";
            newLogTypeSelected = 2;
            key = "pending";
        } else if (logTypeSelected == 1) {
            getLogType = "log";
            newLogTypeSelected = 0;
            key = "log";
        } else {
            getLogType = "system";
            newLogTypeSelected = 1;
            key = "log";
        }

        // Read log from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(key, Context.MODE_PRIVATE);
        String logText = sharedPreferences.getString(getLogType, "");

        if (key.equals("pending")) {
            noLogs.setVisibility(View.INVISIBLE);

            // Create TextView for each log entry
            Map<String, ?> allEntries = sharedPreferences.getAll();
            int items = 0;
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                items++;
                String[] values = entry.getValue().toString().split("\\|");
                String shortedText = values[0];
                if (values[0].length() > 33) {
                    shortedText = values[0].substring(0, 33) + "...";
                }
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

                RelativeLayout relativeLayout = new RelativeLayout(this);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.bottomMargin = 10;
                relativeLayout.setLayoutParams(layoutParams);

                ImageView imageView = new ImageView(this);
                RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(50, 50);
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                imageParams.addRule(RelativeLayout.CENTER_VERTICAL);
                imageParams.setMargins(10, 50, 0, 0);
                imageView.setLayoutParams(imageParams);
                imageView.setImageResource(R.drawable.green_timer);
                relativeLayout.addView(imageView);

                TextView textView = new TextView(this);
                RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                textParams.addRule(RelativeLayout.CENTER_VERTICAL);
                textParams.setMargins(65, 30, 10, -20);
                Typeface typeface = ResourcesCompat.getFont(this, R.font.consolas);
                textView.setTypeface(typeface);
                textView.setLayoutParams(textParams);
                textView.setTextColor(ContextCompat.getColor(this, R.color.green));
                textView.setText(Html.fromHtml(String.format("<b>File:</b> %s<br><b>Time:</b> %s", shortedText, newTime)));
                relativeLayout.addView(textView);

                View bottomLine = new View(this);
                RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, calcDp(this, 2));
                viewParams.setMargins(calcDp(this, 16), 0, calcDp(this, 16), 0);
                bottomLine.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
                bottomLine.setLayoutParams(viewParams);
                relativeLayout.addView(bottomLine);

                relativeLayout.setOnClickListener(v -> {
                    // check if values[2] is a mp4 file
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(MainActivity.this)) {
                            Intent intentOverlay = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intentOverlay, 16);
                        } else {
                            String fileTypeStart;
                            if (values[2].endsWith(".mp4")) {
                                fileTypeStart = "video";
                            } else if (values[2].endsWith(".jpg") || values[2].endsWith(".png") || values[2].endsWith(".jpeg")) {
                                fileTypeStart = "image";
                            } else {
                                fileTypeStart = "file";
                            }
                            DialogManager dialogManager = new DialogManager(MainActivity.this, values[0], values[2], "File options", fileTypeStart);
                            dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                            dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            dialogManager.show();
                        }
                    }
                });

                logsContainer.addView(relativeLayout);
            }
            if (items == 0) {
                noLogs.setVisibility(View.VISIBLE);
            }
        } else {
            if (!logText.isEmpty()) {
                noLogs.setVisibility(View.INVISIBLE);
                // Cut log json object into individual lines
                String[] logLines = logText.split("\n");

                List<JSONObject> jsonArray = new ArrayList<>();
                for (String line : logLines) {
                    try {
                        jsonArray.add(new JSONObject(line));

                        int category = jsonArray.get(jsonArray.size() - 1).getInt("category");
                        int event = jsonArray.get(jsonArray.size() - 1).getInt("event");
                        String time = jsonArray.get(jsonArray.size() - 1).getString("time");
                        String extra = jsonArray.get(jsonArray.size() - 1).getString("extra");

                        RelativeLayout relativeLayout = new RelativeLayout(this);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.bottomMargin = 10;
                        relativeLayout.setLayoutParams(layoutParams);

                        ImageView imageView = new ImageView(this);
                        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(50, 50);
                        imageParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        imageParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        imageParams.setMargins(10, 50, 0, 0);
                        imageView.setLayoutParams(imageParams);
                        relativeLayout.addView(imageView);

                        TextView textView = new TextView(this);
                        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        textParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        textParams.setMargins(65, 30, 10, -20);
                        Typeface typeface = ResourcesCompat.getFont(this, R.font.consolas);
                        textView.setTypeface(typeface);
                        textView.setLayoutParams(textParams);
                        relativeLayout.addView(textView);

                        View bottomLine = new View(this);
                        RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, calcDp(this, 2));
                        viewParams.setMargins(calcDp(this, 16), 0, calcDp(this, 16), 0);
                        bottomLine.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
                        bottomLine.setLayoutParams(viewParams);
                        relativeLayout.addView(bottomLine);

                        if (category == newLogTypeSelected) {
                            if (event == 0) {
                                // Started watching
                                imageView.setImageResource(R.drawable.green_file);

                                textView.setText(String.format("(%s) Started watching %s.", time, extra));
                                textView.setTextColor(ContextCompat.getColor(this, R.color.green));

                            } else if (event == 1) {
                                // Detected new screenshot
                                imageView.setImageResource(R.drawable.green_screenshot);

                                textView.setText(String.format("(%s) Detected new file (%s).", time, extra));
                                textView.setTextColor(ContextCompat.getColor(this, R.color.green));
                            } else if (event == 2) {
                                // App started
                                imageView.setImageResource(R.drawable.green_checkmark);
                                textView.setTextColor(ContextCompat.getColor(this, R.color.green));
                                textView.setText(String.format("(%s) Background service is running!", time));
                            } else if (event == 3) {
                                // Scheduled
                                String[] extraSplit = extra.split("\\|");

                                imageView.setImageResource(R.drawable.green_timer);
                                textView.setTextColor(ContextCompat.getColor(this, R.color.green));
                                textView.setText(String.format("(%s) Scheduled deletion for file %s on %s!", time, extraSplit[0], extraSplit[1]));
                            } else if (event == 4) {
                                // Deleted
                                String[] extraSplit = extra.split("\\|");
                                imageView.setImageResource(R.drawable.green_delete);

                                textView.setText(String.format("(%s) Deleted file in %s tries (%s).", time, extraSplit[0], extraSplit[1]));
                                textView.setTextColor(ContextCompat.getColor(this, R.color.green));
                            } else if (event == 5) {
                                // Detected wrong
                                imageView.setImageResource(R.drawable.orange_error);

                                textView.setText(String.format("(%s) Detected new file but got filtered out. (%s)", time, extra));
                                textView.setTextColor(ContextCompat.getColor(this, R.color.orange));
                            } else if (event == 6) {
                                // Didn't delete
                                imageView.setImageResource(R.drawable.red_bad);

                                textView.setText(String.format("(%s) File failed to delete! (%s)", time, extra));
                                textView.setTextColor(ContextCompat.getColor(this, R.color.red));
                            } else if (event == 7) {
                                // Didn't delete
                                imageView.setImageResource(R.drawable.red_bad);

                                textView.setText(String.format("(%s) File (%s) no longer exists!", time, extra));
                                textView.setTextColor(ContextCompat.getColor(this, R.color.red));
                            }

                            logsContainer.addView(relativeLayout);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        widgerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLogWidget();
                widgerHandler.postDelayed(this, 500);
            }
        }, 500);

        // Update widget
        updateWidget(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        widgerHandler.removeCallbacksAndMessages(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Delete timer
    ////////////////////////////////////////////////////////////////////////////////////////////
    public static void deleteTimer(Context context, String path, int time, boolean showToast, Activity activity) {
        String[] justFile = path.split("/");
        String formattedTime;

        // Format time
        if (time == 0) {
            formattedTime = "immediately.";
        } else if (time <= 3600000) {
            formattedTime = "in " + (time / 1000) / 60 + " minutes.";
        } else if (time <= 86400000) {
            formattedTime = "in " + (time / 1000) / 3600 + " hours.";
        } else {
            formattedTime = "in " + (time / 1000) / 86400 + " days.";
        }

        if (!formattedTime.equals("immediately.") && showToast) {
            ToastManager.run(activity, 3, R.drawable.green_delete, "Deleting file " + formattedTime);
        }

        // Schedule deletion
        long currentTime = System.currentTimeMillis();
        long scheduledTime = currentTime + time;

        // Get date from scheduled time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(scheduledTime);
        String hours = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minutes = String.valueOf(calendar.get(Calendar.MINUTE));
        String seconds = String.valueOf(calendar.get(Calendar.SECOND));
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
        String date = String.format(Locale.getDefault(), "%d.%d.%d at %s:%s:%s",
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR),
                hours, minutes, seconds);

        LogManager.addPending(context, justFile[justFile.length - 1], scheduledTime, path, date);
    }

    public int calcDp(Context context, int pixels) {
        return Math.round(pixels * context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String pathTitle = "";
            String fullPath = "";
            String fileType = "";
            ArrayList<String> paths = new ArrayList<>();

            if (data.getClipData() != null) {
                // Multiple files
                for (int index = 0; index < data.getClipData().getItemCount(); index++) {
                    paths.add(ImageFilePath.getPath(this, data.getClipData().getItemAt(index).getUri()));

                    pathTitle = "Multiple files";
                    fileType = "file";
                }
            } else {
                // One file
                fullPath = ImageFilePath.getPath(this, data.getData());

                String[] justFile = fullPath.split("/");
                pathTitle = justFile[justFile.length - 1];
                fileType = "file";
            }

            if (paths.size() > 0) {
                // Multiple files
                DialogManager dialogManager = new DialogManager(MainActivity.this, pathTitle, paths.get(0), "Manual Add", fileType);
                dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                dialogManager.setOnCancelListener(dialogInterface -> {
                    dialogManager.dismiss();

                    // Delete after 15 mins
                    for (String path : paths) {
                        deleteTimer(MainActivity.this, path, 900000, false, this);
                    }
                });

                dialogManager.show();
                dialogManager.findViewById(R.id.quater).setOnClickListener(v -> {
                    dialogManager.dismiss();
                    for (String path : paths) {
                        deleteTimer(MainActivity.this, path, 900000, false, this);
                    }
                });

                dialogManager.findViewById(R.id.day).setOnClickListener(v -> {
                    dialogManager.dismiss();
                    for (String path : paths) {
                        deleteTimer(MainActivity.this, path, 259200000, false, this);
                    }
                });
                dialogManager.findViewById(R.id.now).setVisibility(View.GONE);
                dialogManager.findViewById(R.id.never).setVisibility(View.GONE);
                TextView previewText = dialogManager.findViewById(R.id.previewText);
                previewText.setText(pathTitle);
                previewText.setVisibility(View.VISIBLE);
            } else {
                File file = new File(fullPath);
                // One file
                if (file.exists()) {
                    DialogManager dialogManager = new DialogManager(MainActivity.this, pathTitle, fullPath, "Manual Add", fileType);
                    dialogManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                    dialogManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                    String finalFullPath = fullPath;
                    dialogManager.setOnCancelListener(dialogInterface -> {
                        dialogManager.dismiss();

                        // Delete after 15 mins
                        deleteTimer(MainActivity.this, finalFullPath, 900000, true, this);
                    });

                    dialogManager.show();
                    dialogManager.findViewById(R.id.now).setVisibility(View.GONE);
                    dialogManager.findViewById(R.id.never).setVisibility(View.GONE);
                } else {
                    ToastManager.run(this, 3, R.drawable.red_error, "File does not exist!");
                }
            }
        }
    }

    private static void updateWidget(Context context) {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, PendingWidget.class));
        PendingWidget pendingWidget = new PendingWidget();
        pendingWidget.onUpdate(context, AppWidgetManager.getInstance(context), ids);
    }
}