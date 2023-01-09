package com.helloitsmeadm.screenshotmanager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class DialogManager extends Dialog {
    public Activity activity;
    public Button quater, day, never, now;
    public String path, fullPath, fileType;
    public ImageView preview, play, imageView;
    public TextView previewText, titleWidget;
    public String title;
    boolean alreadyExists;

    public DialogManager(Activity a, String path, String fullPath, String title, String fileType) {
        super(a);
        this.activity = a;
        this.path = path;
        this.fullPath = fullPath;
        this.title = title;
        this.fileType = fileType;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_main);

        previewText = findViewById(R.id.previewText);
        play = findViewById(R.id.play);
        quater = findViewById(R.id.quater);
        day = findViewById(R.id.day);

        imageView = findViewById(R.id.imageView);

        if (title.equals("File options")) {
            alreadyExists = true;
            imageView.setImageResource(R.drawable.green_settings);
            quater.setText("Add 15 minutes");
            day.setText("Add 3 days");
        } else if (title.equals("Manual Add")) {
            imageView.setImageResource(R.drawable.green_manual);
        }

        preview = findViewById(R.id.preview);

        File file = new File(fullPath);
        if (file.exists()) {
            Uri photoURI = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", new File(fullPath));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // video
            if (fileType.equals("video")) {
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(fullPath, MediaStore.Images.Thumbnails.MINI_KIND);
                preview.setImageBitmap(bitmap);
                preview.setOnClickListener(v -> {
                    intent.setDataAndType(photoURI, "video/*");
                    getContext().startActivity(intent);
                    dismiss();
                });
                // If setImageBitmap fails, try again
                //while (preview.getDrawable() == null) {
                previewText.setVisibility(View.VISIBLE);
                preview.setImageBitmap(bitmap);
                //}
                // image
            } else if (fileType.equals("image")) {
                preview.setOnClickListener(v -> {
                    intent.setDataAndType(photoURI, "image/*");
                    getContext().startActivity(intent);
                    dismiss();
                });

                preview.setImageURI(Uri.parse(fullPath));
                // If setImageURI fails, try again
                while (preview.getDrawable() == null) {
                    previewText.setVisibility(View.VISIBLE);
                    preview.setImageURI(Uri.parse(fullPath));
                    System.out.println("wtf");
                }
            } else {
                previewText.setVisibility(View.VISIBLE);
                previewText.setText(path);
                previewText.setTextColor(getContext().getResources().getColor(R.color.white));
            }

            // If set image was successful, set the image to the imageView
            if (preview.getDrawable() != null) {
                previewText.setVisibility(View.INVISIBLE);
                if (fileType.equals("video")) {
                    play.setVisibility(View.VISIBLE);
                }
            }
        }


        titleWidget = findViewById(R.id.title);
        titleWidget.setText(title);

        quater.setOnClickListener(view -> {
            dismiss();
            if (alreadyExists) {
                AutoDelete.addTime(getContext(), path, 900000, "15 minutes", activity);
            } else {
                MainActivity.deleteTimer(getContext(), fullPath, 900000, true, activity);
            }
        });

        day.setOnClickListener(view -> {
            dismiss();
            if (alreadyExists) {
                AutoDelete.addTime(getContext(), path, 259200000, "3 days", activity);
            } else {
                MainActivity.deleteTimer(getContext(), fullPath, 259200000, true, activity);
            }
        });

        never = findViewById(R.id.never);
        never.setOnClickListener(view -> {
            dismiss();
            if (alreadyExists) {
                ToastManager.run(activity, 3, R.drawable.green_list, "File removed from pending list.");
                AutoDelete.removeFromDatabase(getContext(), path);
            } else {
                ToastManager.run(activity, 3, R.drawable.green_checkmark, "File will not be deleted.");
            }
        });

        now = findViewById(R.id.now);
        now.setOnClickListener(view -> {
            dismiss();
            MainActivity.deleteTimer(getContext(), fullPath, 0, false, activity);
        });
    }
}
