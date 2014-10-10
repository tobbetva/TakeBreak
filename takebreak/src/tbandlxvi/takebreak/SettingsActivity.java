package tbandlxvi.takebreak;

import tbandlxvi.takebreak.R;
//import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
//public class SettingsActivity extends Activity
{
  public static final String PREFKEY_WORK_PHASE_LENGTH = "prefkey_work_phase_length";
  public static final String PREFKEY_REST_PHASE_LENGTH = "prefkey_rest_phase_length";
  public static final String PREFKEY_ALERT_VIA_SOUND = "prefkey_alert_via_sound";
  public static final String PREFKEY_ALERT_VIA_VIBRATION = "prefkey_alert_via_vibration";
  public static final String PREFKEY_ALERT_REPETITION = "prefkey_alert_repetition";
  public static final String PREFKEY_AUTO_PHASE_CHANGE = "prefkey_auto_phase_change";
  public static final String PREFKEY_KEEP_PHONE_AWAKE = "prefkey_keep_phone_awake";

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences); // Must be used for Android 2.3.3 support.
    
    // Display the fragment as the main content.
    //getFragmentManager().beginTransaction()
    //        .replace(android.R.id.content, new SettingsFragment())
    //        .commit();
    
  }  
}
