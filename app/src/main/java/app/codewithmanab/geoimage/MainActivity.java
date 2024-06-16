package app.codewithmanab.geoimage;

import static app.codewithmanab.geoimage.properties.GeoTagImage.PNG;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import app.codewithmanab.geoimage.properties.GEOException;
import app.codewithmanab.geoimage.properties.GEOPermissions;
import app.codewithmanab.geoimage.properties.GEOUtility;
import app.codewithmanab.geoimage.properties.GeoTagImage;
import app.codewithmanab.geoimage.properties.ImageQuality;
import app.codewithmanab.geoimage.properties.PermissionCallback;
import java.io.File;


public class MainActivity extends AppCompatActivity implements PermissionCallback {
    private ImageView ivCamera, ivImage, ivClose;
    private static String originalImgStoragePath, gtiImageStoragePath;
    public static final String IMAGE_EXTENSION = ".png";
    private Uri fileUri;
    private static final int PERMISSION_REQUEST_CODE = 100;

    static FragmentActivity mContext;
    private GeoTagImage geoTagImage;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivCamera = findViewById(R.id.ivCamera);
        ivImage = findViewById(R.id.ivImage);
        ivClose = findViewById(R.id.ivClose);
        progressBar = findViewById(R.id.progressBar);

        mContext = MainActivity.this;

       PermissionCallback permissionCallback = this;

        geoTagImage = new GeoTagImage(mContext, permissionCallback);

        ivCamera.setOnClickListener(click -> {
            if (GEOPermissions.checkCameraLocationPermission(mContext)) {
                openCamera();

            } else {
                GEOPermissions.requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE);
            }
        });
    }
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file;
        file = GEOUtility.generateOriginalFile(mContext, IMAGE_EXTENSION);
        if (file != null) {

            gtiImageStoragePath = file.getPath();
            originalImgStoragePath = file.getPath();
        }

        fileUri = GEOUtility.getFileUri(mContext, file);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        activityResultLauncher.launch(intent);
    }
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {

                    try {
                        progressBar.setVisibility(View.VISIBLE);
                        ivCamera.setVisibility(View.GONE);
                        geoTagImage.createImage(fileUri);
                        geoTagImage.setTextSize(30f);
                        geoTagImage.setBackgroundRadius(5f);
                        geoTagImage.setBackgroundColor(Color.parseColor("#66000000"));
                        geoTagImage.setTextColor(Color.WHITE);
                        geoTagImage.setAuthorName("CodeWithManab");
                        geoTagImage.showAuthorName(false);
                        geoTagImage.showAppName(true);
                        geoTagImage.showGoogleMap(true);
                        geoTagImage.setImageQuality(ImageQuality.HIGH);
                        geoTagImage.setImageExtension(PNG);

                        gtiImageStoragePath = geoTagImage.imagePath();

                        new Handler().postDelayed(this::previewCapturedImage, 3000);


                    } catch (GEOException e) {
                        throw new RuntimeException(e);
                    }



                }
            });
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                geoTagImage.handlePermissionGrantResult();
                Toast.makeText(mContext, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void previewCapturedImage() {
        try {
            Bitmap bitmap = GEOUtility.optimizeBitmap(gtiImageStoragePath);
            ivImage.setImageBitmap(bitmap);

            if (ivImage.getDrawable() != null) {
                ivClose.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
            ivClose.setOnClickListener(v -> {
                ivImage.setImageBitmap(null);
                ivCamera.setVisibility(View.VISIBLE);
                ivClose.setVisibility(View.GONE);
                ivImage.setImageDrawable(null);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPermissionGranted() {
        openCamera();
    }

    @Override
    public void onPermissionDenied() {
        GEOPermissions.requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE);
    }
}