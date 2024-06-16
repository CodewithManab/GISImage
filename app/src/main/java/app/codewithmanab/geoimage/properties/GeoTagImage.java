package app.codewithmanab.geoimage.properties;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeoTagImage {
    private String place = "";
    private String road = "";
    private String Lat= "";
    private String Long = "";
    private String date = "";
    private int originalImageHeight = 0;
    private int originalImageWidth = 0;
    private final Context context;
    private File returnFile = null;
    private Bitmap bitmap = null;
    private Bitmap mapBitmap = null;
    private List<Address> addresses = null;
    private String IMAGE_EXTENSION = ".png";
    private Uri fileUri = null;
    private FusedLocationProviderClient fusedLocationProviderClient = null;
    private Geocoder geocoder = null;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private float textSize = 0f;
    private float textTopMargin = 0f;
    private Typeface typeface = null;
    private float radius = 0f;
    private int backgroundColor = 0;
    private int textColor = 0;
    private float backgroundHeight = 0f;
    private float backgroundLeft = 0f;
    private String authorName = null;
    private boolean showAuthorName = false;
    private boolean showAppName = false;
    private boolean showLat = false;
    private boolean showLong = false;
    private boolean showDate = false;
    private boolean showGoogleMap = false;
    private final ArrayList<String> elementsList = new ArrayList<>();
    private int mapHeight = 0;
    private int mapWidth = 0;
    private int bitmapWidth = 0;
    private int bitmapHeight = 0;
    private String apiKey = null;
    private String center = null;
    private String imageUrl = null;
    private String dimension = null;
    private String markerUrl = null;
    private String imageQuality = null;
    private final PermissionCallback permissionCallback;
//    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Executor executorService = Executors.newSingleThreadExecutor();
    private final String TAG = GeoTagImage.class.getSimpleName();

    public GeoTagImage(Context context, PermissionCallback callback) {
        this.context = context;
        this.permissionCallback = callback;
    }

    public void createImage(Uri fileUri) throws GEOException {
        if (fileUri == null) {
            throw new GEOException("Uri cannot be null");
        }
        this.fileUri = fileUri;

        // set default values here.
        textSize = 25f;
        typeface = Typeface.DEFAULT;
        radius = dpToPx(6f);
        backgroundColor = Color.parseColor("#66000000");
        textColor = context.getColor(android.R.color.white);
        backgroundHeight = 150f;
        authorName = "";
        showAuthorName = false;
        showAppName = false;
        showGoogleMap = true;
        showLat = true;
        showLong = true;
        showDate = true;
        mapHeight = (int) backgroundHeight;
        mapWidth = 120;
        imageQuality = null;
        initialization();
    }

    private void initialization() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        if (imageQuality == null) {
            bitmapWidth = 960 * 2;
            bitmapHeight = 1280 * 2;
            backgroundHeight = (backgroundHeight * 2);
            mapWidth = 120 * 2;
            mapHeight = (int) backgroundHeight;
            textSize *= 2;
            textTopMargin = 50 * 2;
            radius *= 2;
        }
        deviceLocation();
    }

    private void deviceLocation() {
        if (GEOPermissions.checkCameraLocationPermission(context)) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            geocoder = new Geocoder(context, Locale.getDefault());
                            try {
                                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                if (GEOUtility.isGoogleMapsLinked(context)) {
                                    if (GEOUtility.getMapKey(context) == null) {
                                        Bitmap bitmap = createBitmap();
                                        storeBitmapInternally(bitmap);
                                        throw new GEOException("API key not found for this project");
                                    } else {
                                        apiKey = GEOUtility.getMapKey(context);
                                        center = latitude + "," + longitude;
                                        dimension = mapWidth + "x" + mapHeight;
                                        markerUrl = String.format(Locale.getDefault(), "%s%s%s", "markers=color:red%7C", center, "&");
                                        imageUrl = String.format(Locale.getDefault(), "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s", center, 15, dimension, markerUrl, "satellite", apiKey);
                                        executorService.execute(new LoadImageTask(imageUrl));
                                    }
                                } else if (!GEOUtility.isGoogleMapsLinked(context)) {
                                    Bitmap bitmap = createBitmap();
                                    storeBitmapInternally(bitmap);
                                    throw new GEOException("Project is not linked with google map sdk");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("error ", e.getMessage());
                            }
                        }
                    });
        }
    }


    private class LoadImageTask implements Runnable {
        private final String imageUrl;

        public LoadImageTask(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        @Override
        public void run() {
            try {
                Bitmap bitmap = loadImageFromUrl(imageUrl);
                if (bitmap != null) {
                    mapBitmap = bitmap;
                    Bitmap newBitmap = createBitmap();
                    storeBitmapInternally(newBitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap loadImageFromUrl(String imageUrl) {
        try {
            java.io.InputStream inputStream = new URL(imageUrl).openStream();
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public void dimension() throws GEOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(fileUri), null, options);
            originalImageHeight = options.outHeight;
            originalImageWidth = options.outWidth;
            Log.d(TAG, originalImageHeight + " & " + originalImageWidth);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new GEOException("File Not Found : " + e.getMessage());
        }
    }

    private Bitmap createBitmap() {
        Bitmap b = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawARGB(0, 255, 255, 255);
        canvas.drawRGB(255, 255, 255);
        copyTheImage(canvas);
        return b;
    }

    private void copyTheImage(Canvas canvas) {
        try {
            java.io.InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Paint design = new Paint();
        Bitmap scaledbmp = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
        canvas.drawBitmap(scaledbmp, 0f, 0f, design);
        Paint rectPaint = new Paint();
        rectPaint.setColor(backgroundColor);
        rectPaint.setStyle(Paint.Style.FILL);
        if (showAuthorName) {
            backgroundHeight += textTopMargin;
        }
        if (showDate) {
            backgroundHeight += textTopMargin;
        }
        if (showLat) {
            backgroundHeight += textTopMargin;
        }

        if (showLong) {
            backgroundHeight += textTopMargin;
        }
        if (GEOUtility.isGoogleMapsLinked(context)) {
            if (mapBitmap != null) {
                if (showGoogleMap) {
                    float mapLeft = 10f;
                    backgroundLeft = mapBitmap.getWidth() + 20f;
                    canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10f, canvas.getHeight() - 10f, dpToPx(radius), dpToPx(radius), rectPaint);
                    Bitmap scaledbmp2 = Bitmap.createScaledBitmap(mapBitmap, mapWidth, mapHeight, false);
                    canvas.drawBitmap(scaledbmp2, mapLeft, canvas.getHeight() - backgroundHeight + (backgroundHeight - mapBitmap.getHeight()) / 2, design);
                    float textX = backgroundLeft + 10;
                    float textY = canvas.getHeight() - (backgroundHeight - textTopMargin);
                    drawText(textX, textY, canvas);
                } else {
                    backgroundLeft = 10f;
                    canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10f, canvas.getHeight() - 10f, dpToPx(radius), dpToPx(radius), rectPaint);
                    float textX = backgroundLeft + 10;
                    float textY = canvas.getHeight() - (backgroundHeight - textTopMargin);
                    drawText(textX, textY, canvas);
                }
            }
        } else {
            backgroundLeft = 10f;
            canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10f, canvas.getHeight() - 10f, dpToPx(radius), dpToPx(radius), rectPaint);
            float textX = backgroundLeft + 10;
            float textY = canvas.getHeight() - (backgroundHeight - textTopMargin);
            drawText(textX, textY, canvas);
        }
    }

    private void drawText(float textX, float textYDir, Canvas canvas) {
        float textY = textYDir;
        if (imageQuality == null) {
            textSize *= 2;
            textTopMargin = 50 * 2;
        }
        elementsList.clear();
        Paint textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSize);
        if (addresses != null) {
            place = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
            road = addresses.get(0).getAddressLine(0);
            elementsList.add(place);
            elementsList.add(road);
            if(showLat){
                Lat = "Latitude : " + latitude;
                elementsList.add(Lat);
            }
            if (showLong) {
                Long = "Longitude : "  + longitude;
                elementsList.add(Long);
            }
        }
        if (showDate) {
            date = new SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(new Date());
            elementsList.add(date);
        }
        if (showAuthorName) {
            authorName = "Clicked by : " + authorName;
            elementsList.add(authorName);
        }
        for (String item : elementsList) {
            canvas.drawText(item, textX, textY, textPaint);
            textY += textTopMargin;
        }
        if (showAppName) {
            String appName = GEOUtility.getApplicationName(context);
            if (imageQuality != null) {
                switch (imageQuality) {
                    case ImageQuality.LOW:
                        textTopMargin = 50f;
                        textPaint.setTextSize(textSize / 2);
                        textY = canvas.getHeight() - 20f;
                        canvas.drawText(appName, canvas.getWidth() - 10 - 10 - textPaint.measureText(appName), textY, textPaint);
                        break;
                    case ImageQuality.HIGH:
                        textSize = textSize / 2;
                        textTopMargin = 50 * 3.6f;
                        textPaint.setTextSize(textSize);
                        textY = canvas.getHeight() - 40f;
                        canvas.drawText(appName, canvas.getWidth() - 10 - 20 - textPaint.measureText(appName), textY, textPaint);
                        break;
                }
            } else {
                textSize = textSize / 2;
                textTopMargin = 50 * 2;
                textY = canvas.getHeight() - 20f;
                textPaint.setTextSize(textSize);
                canvas.drawText(appName, canvas.getWidth() - 10 - 10 - textPaint.measureText(appName), textY, textPaint);
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    private void storeBitmapInternally(Bitmap b) {
        File pictureFile = outputMediaFile();
        returnFile = pictureFile;
        if (pictureFile == null) {
            Log.e(TAG, "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] compressedImageData = outputStream.toByteArray();
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
            fileOutputStream.write(compressedImageData);
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private File outputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String mImageName = "IMG_" + timeStamp + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File mediaFile = new File(imagePath);
        MediaScannerConnection.scanFile(context, new String[]{imagePath}, null, (path, uri) -> {});
        return mediaFile;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setCustomFont(Typeface typeface) {
        this.typeface = typeface;
    }

    public void setBackgroundRadius(float radius) {
        this.radius = radius;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void showAuthorName(boolean showAuthorName) {
        this.showAuthorName = showAuthorName;
    }

    public void showAppName(boolean showAppName) {
        this.showAppName = showAppName;
    }

    public void showLat(boolean showLat) {
        this.showLat = showLat;
    }

    public void showLong(boolean showLong) {
        this.showLong = showLong;
    }

    public void showDate(boolean showDate) {
        this.showDate = showDate;
    }

    public void showGoogleMap(boolean showGoogleMap) {
        this.showGoogleMap = showGoogleMap;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setImageQuality(String imageQuality) {
        this.imageQuality = imageQuality;
        switch (imageQuality) {
            case ImageQuality.LOW:
                bitmapWidth = 960;
                bitmapHeight = 1280;
                textTopMargin = 50f;
                backgroundHeight = 150f;
                mapWidth = 120;
                mapHeight = (int) backgroundHeight;
                break;
            case ImageQuality.HIGH:
                bitmapWidth = (int) (960 * 3.6);
                bitmapHeight = (int) (1280 * 3.6);
                backgroundHeight = backgroundHeight * 1.5f;
                textSize = textSize * 3.6f;
                textTopMargin = 50 * 3.6f;
                radius = radius * 3.6f;
                mapWidth = mapWidth * 2;
                mapHeight = (int) (backgroundHeight * 1.5);
                break;
        }
    }

    public void setImageExtension(String imgExtension) {
        IMAGE_EXTENSION = imgExtension;
        switch (imgExtension) {
            case ".jpg":
                IMAGE_EXTENSION = ".jpg";
                break;
            case ".png":
                IMAGE_EXTENSION = ".png";
                break;
            case ".jpeg":
                IMAGE_EXTENSION = ".jpeg";
                break;
        }
    }

    public String imagePath() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String mImageName = "IMG_" + timeStamp + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File media = new File(imagePath);
        MediaScannerConnection.scanFile(context, new String[]{media.getAbsolutePath()}, null, (path, uri) -> {});
        return imagePath;
    }

    public Uri imageUri() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String mImageName = "IMG_" + timeStamp + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File media = new File(imagePath);
        return Uri.fromFile(media);
    }

    public String getImagePath() {
        return returnFile != null ? returnFile.getAbsolutePath() : null;
    }

    public void handlePermissionGrantResult() {
        permissionCallback.onPermissionGranted();
    }


    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
}