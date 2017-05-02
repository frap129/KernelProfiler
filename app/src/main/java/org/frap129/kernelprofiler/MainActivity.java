package org.frap129.kernelprofiler;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import eu.chainfire.libsuperuser.Shell;

import static java.lang.System.in;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_FILE = 1;
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Ensure root access
        if (!checkSU())
            return;

        final Switch applyOnBoot = (Switch) findViewById(R.id.boot);
        SharedPreferences boot = getApplication().getSharedPreferences("onBoot", Context.MODE_PRIVATE);

        applyOnBoot.setChecked(boot.getBoolean("onBoot", false));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        startActivityForResult(intent, SELECT_FILE);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        applyOnBoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getApplication().getSharedPreferences("onBoot", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (applyOnBoot.isChecked()) {
                    editor.putBoolean("onBoot", true);
                    editor.apply();
                    Snackbar.make(findViewById(android.R.id.content), "Profile will be applied on boot.", Snackbar.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean("onBoot", false);
                    editor.apply();
                    Snackbar.make(findViewById(android.R.id.content), "Profile won't be applied on boot.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // Method that parses profile file
    public static void setProfile(final String path) {
        new AsyncTask<Object, Object, Void>() {
            @Override
            protected Void doInBackground(Object... params) {
                try {
                    FileInputStream fstream = new FileInputStream(path);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                    String strLine;
                    String exec = "write() { echo -n $2 > $1; };";

                    while ((strLine = br.readLine()) != null) {
                        exec = exec + " write " + strLine + ";";
                    }

                    Shell.SU.run(exec);

                    br.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    // Method that prompts the user for confirmation
    protected void profileDialog(final String path){
        final Dialog pDialog = new Dialog(MainActivity.this);
        pDialog.setTitle("Profile Loader");
        pDialog.setContentView(R.layout.profile_dialog);
        Button pDialogCancel = (Button) pDialog.findViewById(R.id.pDialogCancel);
        pDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pDialog.dismiss();
            }
        });
        Button pDialogConfirm = (Button) pDialog.findViewById(R.id.pDialogConfirm);
        pDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getApplication().getSharedPreferences("onBoot", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("onBoot", false);
                editor.apply();
                setProfile(path);
                SharedPreferences profPath = getApplication().getSharedPreferences("profilePath", Context.MODE_PRIVATE);
                SharedPreferences.Editor paths = profPath.edit();
                SharedPreferences prof = getApplication().getSharedPreferences("profile", Context.MODE_PRIVATE);
                SharedPreferences.Editor peditor = prof.edit();
                paths.putString("profilePath", path);
                paths.apply();
                peditor.putString("profile", "custom");
                peditor.apply();
                pDialog.dismiss();
            }
        });
        pDialog.show();
    }

    // File path methods taken from aFileChooser, thanks to iPaulPro: https://github.com/iPaulPro/aFileChooser
    public static String getPath(final Context context, final Uri uri) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type))
                    return Environment.getExternalStorageDirectory() + "/" + split[1];

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type))
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("video".equals(type))
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                else if ("audio".equals(type))
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme()))
            return getDataColumn(context, uri, null, null);
            // File
        else if ("file".equalsIgnoreCase(uri.getScheme()))
            return uri.getPath();

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    permDialog();
                }
                else{
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    startActivityForResult(intent, SELECT_FILE);
                }
                break;
            }
        }
    }

    //Thanks: http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                Uri selectedFileUri = data.getData();
                String selectedFilePath = getPath(this, selectedFileUri);
                if (selectedFilePath != null) {
                    profileDialog(selectedFilePath);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Invalid file path.", Snackbar.LENGTH_LONG).show();

                }
            }
        }
    }

    // Method to check if the device is rooted
    private boolean checkSU() {
        if (!Shell.SU.available()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material);
            dialog.setTitle("Root access not available");
            dialog.setMessage("Please root your device and/or grant root access to Spectrum.");
            dialog.setCancelable(false);
            AlertDialog root = dialog.create();
            root.show();
            return false;
        } else
            Snackbar.make(findViewById(android.R.id.content), "Root access granted!", Snackbar.LENGTH_SHORT).show();
            return true;
    }

    // Method to request permissions AGAIN
    protected void permDialog(){
        final Dialog permDialog = new Dialog(MainActivity.this);
        permDialog.setContentView(R.layout.perm_dialog);
        permDialog.setCancelable(false);
        Button pDialogConfirm = (Button) permDialog.findViewById(R.id.permDialogConfirm);
        pDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                permDialog.dismiss();
            }
        });
        permDialog.show();
    }
}
