package tbandlxvi.takebreak;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
  private final static String LINE_SEP = System.getProperty("line.separator");

  private int MY_NOTIFICATION_1 = 1;
  
  private Timer theTimer = null;
  private final static int CLOCK_TIMER_INTERVAL = 1000;
  
  private long startTime = 0; // The NanoTime at start of a "continuous time ticking period".  Either a start after a pause, or a start of a new phase.
                              // todo: ovan stämmer inte...
  private long totalDuration = 0; // Total duration (seconds) for current phase.

  private boolean isOn = false;   // The timer is ticking.
  private boolean isWork = true; // Current phase is work phase.
  
  private TextView clockTextView = null;
  
  private int normalTextColor = -1;
  
  private Toast confirmQuitToast = null;
  private long lastBackPressTime = 0;
  
  AppSettings mySettings = new AppSettings();
  boolean inSettingsDialog = false;
  
  boolean notifCreated = false;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    mySettings.readSettings(this);
    
    clockTextView = (TextView) findViewById(R.id.clock_text);

    if (mySettings.keepPhoneAwake)
    {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Prevent phone from sleeping.
    }
  }

  @Override
  public void onDestroy()
  {
    removeNotification();
    timerCleanup(); 
    super.onDestroy();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) 
  {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) 
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    switch (id) 
    {
      case R.id.action_settings:
        this.launchSettingsDialog();
        return true;
      case R.id.action_help:
        this.showHelp();
        return true;        
      default:
        return super.onOptionsItemSelected(item);
    }  
  }
  
  @Override
  protected void onResume()
  {
    super.onResume();

    if (inSettingsDialog)
    {
      inSettingsDialog = false;

      handleUpdatedSettings();
    }
  }

  @Override
  protected void onPause()
  {
    // Main screen is about to loose focus. Maybe app is closing or maybe just a settings dialog is opening.
    // Anyway, make sure any GPS usage is removed.
    
    //    todo ?
    
    super.onPause();
  }
  
  public void onBackPressed()
  {
    if (isOn)
    {
      // Timer-vyn är den aktuella vyn, och timern tickar. Få bekräftat att man verkligen vill stänga ned appen.
      if (lastBackPressTime < System.currentTimeMillis() - 2000) 
      {
        confirmQuitToast = Toast.makeText(this, getResources().getString(R.string.str_confirm_app_close), Toast.LENGTH_SHORT);
        confirmQuitToast.show();
        this.lastBackPressTime = System.currentTimeMillis();
      } 
      else 
      {
        // Ok - man vill verkligen stänga ned appen
        if (confirmQuitToast != null) 
        {
          confirmQuitToast.cancel();
        }
        super.onBackPressed();
        return;
      }        
    }
    else
    {
      super.onBackPressed();
      return;
    }
  }
  
  public void onCheckboxClicked(View view) 
  {
    switch(view.getId()) 
    {
      case R.id.timer_on_chkbox:
        isOn = ((CheckBox) view).isChecked();
        applyTimerOnOffState();
        break;
    }
  }

  public void onButtonClicked(View view) 
  {
    switch(view.getId()) 
    {
      case R.id.phase_btn:
        isWork = !isWork;
        applyPhase();
        break;
    }
  }
   
  private void launchSettingsDialog()
  {
    inSettingsDialog = true;
    
    startActivity(new Intent(this, SettingsActivity.class));
  }
  
  private void handleUpdatedSettings()
  {
    boolean keepPhoneAwakePrevValue = mySettings.keepPhoneAwake;
    
    mySettings.readSettings(this);
    
    if (keepPhoneAwakePrevValue != mySettings.keepPhoneAwake)
    {
      if (mySettings.keepPhoneAwake)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      else
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }
  
  // Creates or updates the one and only notification of the app. 
  // The actual notification sending should not cause any sound/vibration. All sounds/vibrations are issued from this app.
  // Clicking on the notification in the notification drawer should resume this activity - not start a new one.
  private void createNotification(boolean overdue)
  {
    notifCreated = true;

    String contentText = isWork ? getResources().getString(R.string.str_phase_in_work_statustext) : getResources().getString(R.string.str_phase_in_rest_statustext);
    if (overdue)
    {
      contentText += " " + Character.toUpperCase(getResources().getString(R.string.str_overdue).charAt(0)) + getResources().getString(R.string.str_overdue).substring(1);
    }
    
    int defaults = Notification.FLAG_NO_CLEAR; 
    NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher2)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(contentText)
                .setDefaults(defaults);
    Intent intent = new Intent(this, this.getClass());
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

    mBuilder.setContentIntent(pendingIntent);
    NotificationManager mNotificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.notify(MY_NOTIFICATION_1, mBuilder.build());
  }

  
  private void removeNotification()
  {
    if (notifCreated)
    {
      NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.cancel(MY_NOTIFICATION_1);
    }
  }
  
  private void alertPhaseTimesUp()
  {
    if (normalTextColor == -1)
    {
      normalTextColor = clockTextView.getCurrentTextColor();      
    }
    clockTextView.setTextColor(Color.RED);
    
    if (mySettings.alertViaSound)
    {
      Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
      if (r != null)
      {
        r.play();
      }  
    }
    
    if (mySettings.alertViaVibration)
    {
      Vibrator v = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
      v.vibrate(300);
    }
  }
  
  private void revokeAlert()
  {
    if (normalTextColor != -1)
    {
      clockTextView.setTextColor(normalTextColor);
    }
  }
  
  private void applyTimerOnOffState() // Timer has been started or stopped.
  {
    Button btn = (Button) findViewById(R.id.phase_btn);
    if (btn != null)
    {
      btn.setEnabled(isOn);
      if (isOn)
      {
        startTimer();
      }
      else
      {
        updateGuiClock();
        stopTimer();
      }
    }
    if (!notifCreated) // The first start of timer.
    {
      createNotification(false);
    }
  }
  
  private void applyPhase() // Phase has been changed.
  {
    totalDuration = 0;
    startTime = TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    
    revokeAlert();
    
    TextView tv = (TextView) findViewById(R.id.phase_statustext);
    if (tv != null)
    {
      if (isWork)
        tv.setText(getResources().getString(R.string.str_phase_in_work_statustext));
      else
        tv.setText(getResources().getString(R.string.str_phase_in_rest_statustext));
    }
    Button btn = (Button) findViewById(R.id.phase_btn);
    if (btn != null)
    {
      if (isWork)
        btn.setText(getResources().getString(R.string.str_phase_btn_in_work));
      else
        btn.setText(getResources().getString(R.string.str_phase_btn_in_rest));
    }
    
    updateGuiClock();
    
    createNotification(false);
  }
  
  private void updateGuiClock()
  {
    if (clockTextView != null && startTime > 0)
    {
      totalDuration += TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;
      startTime = TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);

      long timeLeft = (isWork ? mySettings.workPhaseLength*60 - totalDuration : mySettings.restPhaseLength*60 - totalDuration);
      
      String overdue = (timeLeft < 0 ? getResources().getString(R.string.str_overdue) : "");
      
      clockTextView.setText(String.format("%02d:%02d %s", Math.abs(timeLeft) / 60, Math.abs(timeLeft) % 60, overdue));
      
      if (timeLeft <= 0)
      {
        if (timeLeft > -mySettings.alertRepetition*2 && (timeLeft % 2 == 0)) // Gör alert varannan sekund.
        {
          alertPhaseTimesUp();
        }
        if (mySettings.autoPhaseChange) // Change automatically, after just one alert.
        {
          isWork = !isWork;
          applyPhase();          
        }
        else if (timeLeft < 0)
        {
          createNotification(true);
        }
      }
    }
  }
  
  private void timerMethod()
  {
    //This method is called directly by the timer
    //and runs in the same thread as the timer.

    //We call the method that will work with the UI
    //through the runOnUiThread method.
    this.runOnUiThread(updateGuiThread);    
  }

  private Runnable updateGuiThread = new Runnable() 
  {
    public void run() 
    {
      //This method runs in the same thread as the UI.               
  
      updateGuiClock();
    }
  };

  private void startTimer()
  {
    timerCleanup();

    startTime = TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);

    theTimer = new Timer();
    theTimer.schedule(new TimerTask() {
      @Override
      public void run() {
          timerMethod();
      }
    }, 100, CLOCK_TIMER_INTERVAL);    
  }
  
  private void stopTimer()
  {
    timerCleanup();
  }  
  
  private void timerCleanup()
  {
    if (theTimer != null)
    {
      theTimer.cancel();
      theTimer.purge();
      theTimer = null;
    }
  }
  
  private void showHelp()
  {
    StringBuilder msg = new StringBuilder();
    
    try
    {
      msg.append(String.format("%s v %s (versionCode %d)", getResources().getString(R.string.app_name),
          getPackageManager().getPackageInfo(getPackageName(), 0).versionName,
          getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
    }
    catch (Exception e) 
    {
      showMessageBox("Error", "Failed retrieving version. " + e.toString());
      e.printStackTrace();
    }
    
    msg.append(LINE_SEP);
    msg.append(LINE_SEP);
    
    msg.append(getResources().getString(R.string.app_name) + " is an app for reminding on when to switch between work and rest.");
    
    showMessageBox("Help", msg.toString());
  }
  
  protected void showMessageBox(String title, String msg)
  {
    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
    
    dlgAlert.setMessage(msg);
    dlgAlert.setTitle(title);
    dlgAlert.setPositiveButton("OK", null);
    dlgAlert.setCancelable(true);
    dlgAlert.create().show();     
  }
  
}
