package app.codewithmanab.geoimage.properties;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GEOUtility {

    /** Check for Map SDK Integration */
    public static boolean isGoogleMapsLinked(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String apiKey = null;
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            android.os.Bundle metaData = applicationInfo.metaData;
            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return apiKey != null;
    }

    /** Check for google map api key (if Map SDK is integrated) */
    public static String getMapKey(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String apiKey = null;
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            android.os.Bundle metaData = applicationInfo.metaData;
            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return apiKey;
    }

    /** To get application name */
    public static String getApplicationName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Unknown";
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    /** Save original image in camera directory */
    public static File generateOriginalFile(FragmentActivity mContext, String IMAGE_EXTENSION) {
        File file = null;
        try {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            file = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + IMAGE_EXTENSION);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (file != null) {
            scanMediaFile(mContext, file);
        }
        return file;
    }

    private static void scanMediaFile(FragmentActivity mContext, File file) {
        MediaScannerConnection.scanFile(mContext, new String[]{file.getAbsolutePath()}, null, (path, uri) -> {});
    }

    public static Bitmap optimizeBitmap(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
    }
}
