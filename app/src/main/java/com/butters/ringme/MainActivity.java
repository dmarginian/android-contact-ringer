package com.butters.ringme;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final int ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS_REQUEST = 1;

  private SeekBar ringerVolumeSeekBar;
  private Switch enabledSwitch;
  private RadioButton allContactsRadio;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    checkPermissions();
    setUi();
  }

  private void setUi() {
    final SharedPreferences settings = getSharedPreferences(getString(R.string.pref_name), 0);
    setRingerVolumeSeekBar(settings);
    setEnabledSwitch(settings);
    setRadioButtons(settings);
  }

  private void setRingerVolumeSeekBar(final SharedPreferences settings) {
    this.ringerVolumeSeekBar = (SeekBar) findViewById(R.id.ringerVolume);
    int ringerVolume = settings.getInt(getString(R.string.ringer_volume_percent), 0);
    ringerVolumeSeekBar.setProgress(ringerVolume);

    // perform seek bar change listener event used for getting the progress value
    ringerVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      int ringerVolumePercent = 0;

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ringerVolumePercent = progress;
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(getString(R.string.ringer_volume_percent), ringerVolumePercent);
        editor.commit();
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        // Not implemented.
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        Toast.makeText(MainActivity.this, "Ringer volume set to: " + ringerVolumePercent + "%",
            Toast.LENGTH_SHORT).show();
      }

    });
  }

  private void setEnabledSwitch(final SharedPreferences settings) {
    this.enabledSwitch = (Switch) findViewById(R.id.enabled);

    boolean enabled = settings.getBoolean(getString(R.string.enabled), true);
    enabledSwitch.setChecked(enabled);

    enabledSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getString(R.string.enabled), b);
        editor.commit();
      }

    });
  }

  private void setRadioButtons(final SharedPreferences settings) {
    this.allContactsRadio = (RadioButton) findViewById(R.id.radio_all_contacts);
    boolean allContacts = settings.getBoolean(getString(R.string.all_contacts), true);
    allContactsRadio.setChecked(allContacts);

  }

  public void onRadioButtonClicked(View view) {
    // Is the button now checked?
    boolean checked = ((RadioButton) view).isChecked();

    final SharedPreferences settings = getSharedPreferences(getString(R.string.pref_name), 0);
    SharedPreferences.Editor editor = settings.edit();

    // Check which radio button was clicked
    switch(view.getId()) {
      case R.id.radio_all_contacts:
        if (checked)
          editor.putBoolean(getString(R.string.all_contacts), true);
          editor.commit();
          break;
      case R.id.radio_favorites_only:
        if (checked)
          editor.putBoolean(getString(R.string.all_contacts), false);
          editor.commit();
          break;
    }
  }

  private void checkPermissions() {
    boolean hasPermissionToReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    boolean hasPermissionToReadPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    boolean hasPermissionToModifyAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED;

    // https://stackoverflow.com/questions/39151453/in-android-7-api-level-24-my-app-is-not-allowed-to-mute-phone-set-ringer-mode
    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
    boolean hasPermissionToAccessNotificationPolicy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && notificationManager.isNotificationPolicyAccessGranted();

    if (!hasPermissionToReadContacts || !hasPermissionToReadPhoneState || !hasPermissionToModifyAudio || !hasPermissionToAccessNotificationPolicy) {
      // we don't have permission to read contacts...so ask user to grant permission
      List<String> permissionsToRequest = new ArrayList();
      if (!hasPermissionToReadContacts) {
        permissionsToRequest.add(Manifest.permission.READ_CONTACTS);
      }
      if (!hasPermissionToReadPhoneState) {
        permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE);
      }
      if (!hasPermissionToModifyAudio) {
        permissionsToRequest.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
      }
      if (!hasPermissionToAccessNotificationPolicy) {
        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivityForResult(intent, ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS_REQUEST);
      } else {
        ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[permissionsToRequest.size()]), 7);
      }
    }
  }

}
