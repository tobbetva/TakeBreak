package tbandlxvi.takebreak;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class AppSettings 
{
  // Note: Changing default values may require change also in preferences.xml.
  private final int DFLT_WORK_PHASE_LENGTH = 50;
  private final int DFLT_REST_PHASE_LENGTH = 10;
  private final boolean DFLT_ALERT_VIA_SOUND = true;
  private final boolean DFLT_ALERT_VIA_VIBRATION = false;
  private final int DFLT_ALERT_REPETITION = 2;
  private final boolean DFLT_AUTO_PHASE_CHANGE = false;
  private final boolean DFLT_KEEP_PHONE_AWAKE = false;

  public int workPhaseLength = DFLT_WORK_PHASE_LENGTH; // Minutes
  public int restPhaseLength = DFLT_REST_PHASE_LENGTH; // Minutes
  public boolean alertViaSound = DFLT_ALERT_VIA_SOUND;
  public boolean alertViaVibration = DFLT_ALERT_VIA_VIBRATION;
  public int alertRepetition = DFLT_ALERT_REPETITION; // Number of repetitions of the alert.
  public boolean autoPhaseChange = DFLT_AUTO_PHASE_CHANGE;
  public boolean keepPhoneAwake = DFLT_KEEP_PHONE_AWAKE;
  

  public void readSettings(Context context)
  {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    
    String numString = ""; // For temp storage of values that could be empty, and hence cause Integer conversion to fail.
                     // Even if "number" is specified in preferences.xml, the user can save an empty value in the settings dialog.
    

    numString = sharedPref.getString(SettingsActivity.PREFKEY_WORK_PHASE_LENGTH, Integer.toString(DFLT_WORK_PHASE_LENGTH));
    workPhaseLength = numString.isEmpty() ? DFLT_WORK_PHASE_LENGTH : Integer.valueOf(numString);

    numString = sharedPref.getString(SettingsActivity.PREFKEY_REST_PHASE_LENGTH, Integer.toString(DFLT_REST_PHASE_LENGTH));
    restPhaseLength = numString.isEmpty() ? DFLT_REST_PHASE_LENGTH : Integer.valueOf(numString);
    
    alertViaSound = sharedPref.getBoolean(SettingsActivity.PREFKEY_ALERT_VIA_SOUND, DFLT_ALERT_VIA_SOUND);
    
    alertViaVibration = sharedPref.getBoolean(SettingsActivity.PREFKEY_ALERT_VIA_VIBRATION, DFLT_ALERT_VIA_VIBRATION);
    
    numString = sharedPref.getString(SettingsActivity.PREFKEY_ALERT_REPETITION, Integer.toString(DFLT_ALERT_REPETITION));
    alertRepetition = numString.isEmpty() ? DFLT_ALERT_REPETITION : Integer.valueOf(numString);
    
    autoPhaseChange = sharedPref.getBoolean(SettingsActivity.PREFKEY_AUTO_PHASE_CHANGE, DFLT_AUTO_PHASE_CHANGE);
    
    keepPhoneAwake = sharedPref.getBoolean(SettingsActivity.PREFKEY_KEEP_PHONE_AWAKE, DFLT_KEEP_PHONE_AWAKE);
  }  
}
