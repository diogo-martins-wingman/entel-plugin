package pt.wingman.entel.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

import pt.wingman.entel.R;
import pt.wingman.entel.plugin.FingerprintManager;
import pt.wingman.entel.plugin.FingerprintManagerCallback;
import pt.wingman.entel.plugin.definitions.FingerStatus;
import pt.wingman.entel.plugin.definitions.FingerprintStatus;

public class PluginActivity extends AppCompatActivity implements FingerprintManagerCallback {
    private TextView fingerStatus = null;
    private ImageView image = null;
    private TextView percentage = null;
    private Button start = null;
    private int fingerprintStatus = FingerprintStatus.STOPED;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        setupViews();
    }

    private void setupViews() {
        fingerStatus = findViewById(R.id.activity_plugin_finger_position);
        start = findViewById(R.id.activity_plugin_start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (fingerprintStatus) {
                    default:
                    case FingerprintStatus.STARTED:
                        break;
                    case FingerprintStatus.STOPED:
                        FingerprintManager.getInstance().initialize(PluginActivity.this, PluginActivity.this, 1, 0, false);
                        break;
                    case FingerprintStatus.SCANNING:
                        FingerprintManager.getInstance().stop();
                        break;
                }
            }
        });
        image = findViewById(R.id.activity_plugin_image);
        fingerStatus = findViewById(R.id.activity_plugin_finger_position);
        percentage = findViewById(R.id.activity_plugin_percentage);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FingerprintManager.getInstance().stop();
    }

    @Override
    public void onFingerStatusUpdate(final int fingerStatusValue) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String fingerStatus;
                switch (fingerStatusValue) {
                    default:
                        fingerStatus = "unexpected";
                        break;
                    case FingerStatus.NO_FINGER:
                        fingerStatus = "move-no-finger";
                        break;
                    case FingerStatus.MOVE_UP:
                        fingerStatus = "move-finger-up";
                        break;
                    case FingerStatus.MOVE_DOWN:
                        fingerStatus = "move-finger-down";
                        break;
                    case FingerStatus.MOVE_LEFT:
                        fingerStatus = "move-finger-left";
                        break;
                    case FingerStatus.MOVE_RIGHT:
                        fingerStatus = "move-finger-right";
                        break;
                    case FingerStatus.PRESS_HARDER:
                        fingerStatus = "press-harder";
                        break;
                    case FingerStatus.MOVE_LATENT:
                        fingerStatus = "move-latent";
                        break;
                    case FingerStatus.REMOVE_FINGER:
                        fingerStatus = "remove-finger";
                        break;
                    case FingerStatus.FINGER_OK:
                        fingerStatus = "finger-ok";
                }
                PluginActivity.this.fingerStatus.setText(fingerStatus);
            }
        });
    }

    @Override
    public void onBitmapUpdate(final int width, final int height, final String base64String) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
                ByteBuffer buffer = ByteBuffer.wrap(Base64.decode(base64String, Base64.NO_WRAP));
                bitmap.copyPixelsFromBuffer(buffer);
                image.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void onPercentageUpdate(final int percentageValue) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                percentage.setText(percentageValue + "%");
            }
        });
    }

    @Override
    public void onFingerprintStatusUpdate(final int fingerprintStatus) {
        this.fingerprintStatus = fingerprintStatus;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (fingerprintStatus) {
                    case FingerprintStatus.STARTED:
                        start.setEnabled(false);
                        start.setText("starting");
                        break;
                    case FingerprintStatus.SCANNING:
                        start.setEnabled(true);
                        start.setText("stop");
                        break;
                    case FingerprintStatus.STOPED:
                        start.setEnabled(true);
                        start.setText("start");
                        break;
                }
            }
        });
    }

    @Override
    public void onError(final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PluginActivity.this, "FingerPrintManagerError: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSDKError(final String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PluginActivity.this, "SDKError: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
