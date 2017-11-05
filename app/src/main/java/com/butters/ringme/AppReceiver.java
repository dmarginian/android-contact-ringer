package com.butters.ringme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AppReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.pref_name), 0);
    boolean enabled = settings.getBoolean(context.getString(R.string.enabled), true);
    if (enabled) {
      try {
        String action = intent.getAction();
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
          String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
          String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
          if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            if (doesContactExistForIncomingCall(context, incomingNumber, settings)) {
              increaseRingerOnContactCallStart(context, audioManager, settings);
            }
          } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            resetRingerOnContactCallEnd(context, audioManager, settings);
          }
        }
      } catch(Exception e) {
        Log.e("App Receiver", e.getMessage(), e);
      }
    }
  }

  /**
   * Checks if a contact exists for the incoming number.
   *
   * @param context
   * @param incomingNumber
   * @return
   */
  private boolean doesContactExistForIncomingCall(Context context, String incomingNumber, SharedPreferences settings) {
    incomingNumber = cleanPhoneNumber(incomingNumber);
    StringBuffer selectionClause = new StringBuffer("substr(" + ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + ",-10, 10) =?");
    List<String> selectionClauseArgs = new ArrayList();
    selectionClauseArgs.add(incomingNumber);
    boolean allContacts = settings.getBoolean(context.getString(R.string.all_contacts), true);
    if (!allContacts) {
      selectionClause.append(" AND starred=?");
      selectionClauseArgs.add("1");
    }
    String[] projections = { ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER };
    Cursor contacts = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projections, selectionClause.toString(), selectionClauseArgs.toArray(new String[selectionClauseArgs.size()]), null);
    boolean contactForIncomingCallExists = contacts.getCount() > 0;
    contacts.close();
    return contactForIncomingCallExists;
  }

  /**
   * Increases the ringer volume and sets the ringer mode to normal.
   * Saves off ringer volume and ringer mode state to SharedPreferences
   * so that they can be restored when the calls ends.
   *
   * @param context
   * @param audioManager
   */
  private void increaseRingerOnContactCallStart(Context context, AudioManager audioManager, SharedPreferences settings) {
    // Store off the ringer state before increasing the volume, this will be used
    // to restore the ringer state after th = e call ends.
    int ringerVolumeBeforeIncrease = audioManager.getStreamVolume(AudioManager.STREAM_RING);
    int ringerModeBeforeIncrease = audioManager.getRingerMode();

    int ringerVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
    int ringerVolumePercent = settings.getInt(context.getString(R.string.ringer_volume_percent), -1);
    if (ringerVolumePercent != -1) {
      ringerVolume = Math.round((new Float(ringerVolumePercent) / 100) * ringerVolume);
    }

    // Increase the ringer volume.
    setRingerModeAndVolume(audioManager, AudioManager.RINGER_MODE_NORMAL, ringerVolume);

    // Save the ringer state via SharedPreferences.
    SharedPreferences.Editor editor = settings.edit();
    editor.putInt(context.getString(R.string.ringer_volume_before_increase), ringerVolumeBeforeIncrease);
    editor.putInt(context.getString(R.string.ringer_mode_before_increase), ringerModeBeforeIncrease);
    editor.commit();
  }

  /**
   * Resets the ringer volume and mode based on the state that was saved when a contact call
   * was received.  If no state was saved the ringer volume defaults to 0 and the ringer mode
   * defaults to silent.
   *
   * @param context
   * @param audioManager
   */
  private void resetRingerOnContactCallEnd(Context context, AudioManager audioManager, SharedPreferences settings) {
    int ringerVolume = settings.getInt(context.getString(R.string.ringer_volume_before_increase), 0);
    int ringerMode = settings.getInt(context.getString(R.string.ringer_mode_before_increase), AudioManager.RINGER_MODE_SILENT);

    setRingerModeAndVolume(audioManager, ringerMode, ringerVolume);
  }

  /**
   * Sets the ringer mode and volume.
   *
   * @param audioManager
   * @param ringerMode
   * @param ringerVolume
   */
  private void setRingerModeAndVolume(AudioManager audioManager, int ringerMode, int ringerVolume) {
    audioManager.setRingerMode(ringerMode);
    audioManager.setStreamVolume(AudioManager.STREAM_RING, ringerVolume, 0);
  }

  /**
   * Removes all special characters from a phone number.
   *
   * @param numberIn
   * @return
   */
  private String cleanPhoneNumber(String numberIn) {
    return numberIn.replaceAll("[\\D]", "");
  }
}
