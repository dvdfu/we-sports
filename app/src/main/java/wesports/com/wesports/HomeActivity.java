package wesports.com.wesports;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.appyvet.rangebar.RangeBar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class HomeActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {

  protected GoogleApiClient mGoogleApiClient;
  protected Location mLastLocation;

  private PrivateChannel channel;

  protected TextView mLatitudeText;
  protected TextView mLongitudeText;

  private double latitude;
  private double longitude;

  private Button mLocationButton;
  private Button mDateButton;
  private Button mTimeButton;
  private TextView mRangeText;
  private RangeBar mRangeBar;
  private Spinner spinner;

  private int leftRange;
  private int rightRange;

  protected static final String TAG = "HomeActivity";
  private static final int PLACE_PICKER_REQUEST = 1;

  private Place place;
  Calendar cal;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    mLatitudeText = (TextView) findViewById(R.id.latitude);
    mLongitudeText = (TextView) findViewById(R.id.longtitude);
    buildGoogleApiClient();

    mDateButton = (Button) findViewById(R.id.date_button);
    mTimeButton = (Button) findViewById(R.id.time_button);
    mLocationButton = (Button) findViewById(R.id.location_button);

    // set date and time text
    cal = Calendar.getInstance();
    Date date = cal.getTime();
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
    mDateButton.setText(dateFormat.format(date));
    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa");
    mTimeButton.setText(timeFormat.format(date));

    // set range bar values
    mRangeBar = (RangeBar) findViewById(R.id.rangebar);
    mRangeText = (TextView) findViewById(R.id.rangetext);
    mRangeText.setText(6 + " - " + 10);
    leftRange = 6;
    rightRange = 10;
    mRangeBar.setRangePinsByIndices(6, 10);
    mRangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
      @Override
      public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                        int rightPinIndex,
                                        String leftPinValue, String rightPinValue) {
        if (leftPinValue == rightPinValue) {
          mRangeText.setText(leftPinValue);
        } else {
          mRangeText.setText(leftPinValue + " - " + rightPinValue);
        }
        leftRange = leftPinIndex;
        rightRange = rightPinIndex;
      }
    });

    // set spinner values
    spinner = (Spinner) findViewById(R.id.game_spinner);
    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.games_array, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    spinner.setAdapter(adapter);

    setUpPusher();
  }

  void setUpPusher() {
    // Create a new Pusher instance
    HttpAuthorizer authorizer = new HttpAuthorizer("http://we-sports.herokuapp.com/pusher/auth");
    PusherOptions options = new PusherOptions().setAuthorizer(authorizer);
    Pusher pusher = new Pusher(BuildConfig.PUSHER_KEY, options);

    pusher.connect(new ConnectionEventListener() {
      @Override
      public void onConnectionStateChange(ConnectionStateChange change) {
      }

      @Override
      public void onError(String message, String code, Exception e) {
      }
    }, ConnectionState.ALL);

    // Subscribe to a channel
    channel = pusher.subscribePrivate("private-notifications",
            new PrivateChannelEventListener() {
              @Override
              public void onAuthenticationFailure(String message, Exception e) {
              }

              @Override
              public void onSubscriptionSucceeded(String channelName) {
              }

              @Override
              public void onEvent(String channelName, String eventName, String data) {
              }
            });
  }

  protected synchronized void buildGoogleApiClient() {
    mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.home, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onConnected(Bundle bundle) {
    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    if (mLastLocation != null) {
      mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
      mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
      latitude = mLastLocation.getLatitude();
      longitude = mLastLocation.getLongitude();
    } else {
      Toast.makeText(this, "No location detected", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.i(TAG, "Connection suspended");
    mGoogleApiClient.connect();
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
  }

  public void locationSelect(View view) {
    try {
      PlacePicker.IntentBuilder intentBuilder =
              new PlacePicker.IntentBuilder();
      // intentBuilder.setLatLngBounds(BOUNDS_MOUNTAIN_VIEW);
      Intent intent = intentBuilder.build(this);
      startActivityForResult(intent, PLACE_PICKER_REQUEST);

    } catch (GooglePlayServicesRepairableException e) {
      e.printStackTrace();
    } catch (GooglePlayServicesNotAvailableException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onActivityResult(int requestCode,
                                  int resultCode, Intent data) {

    if (requestCode == PLACE_PICKER_REQUEST
            && resultCode == Activity.RESULT_OK) {

      place = PlacePicker.getPlace(data, this);
      final CharSequence name = place.getName();
      mLocationButton.setText(name);

    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  public void setDate(View view) {
    DatePickerDialog dpd = DatePickerDialog.newInstance(
            HomeActivity.this,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
    );
    dpd.setThemeDark(true);
    dpd.show(getFragmentManager(), "Datepickerdialog");
  }

  public void setTime(View view) {
    TimePickerDialog tpd = TimePickerDialog.newInstance(
            HomeActivity.this,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
    );
    tpd.setThemeDark(true);
    tpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialogInterface) {
        Log.d("TimePicker", "Dialog was cancelled");
      }
    });
    tpd.show(getFragmentManager(), "Timepickerdialog");
  }

  public void createGame(View view) {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          HttpClient httpclient = new DefaultHttpClient();
          HttpPost httppost = new HttpPost("http://we-sports.herokuapp.com/create");

          JSONObject message = new JSONObject();

          message.put("type", "basketball");
          message.put("name", "b-ball");
          message.put("desc", "5pm");
          message.put("date", 1437279239);
          message.put("bet", 799);

          JSONObject people = new JSONObject();
          people.put("min", leftRange);
          people.put("max", rightRange);
          message.put("people", people);


          JSONObject place = new JSONObject();
          people.put("long", longitude);
          people.put("lat", latitude);
          message.put("place", place);

          JSONObject contact = new JSONObject();
          contact.put("phone", "2o312u");
          contact.put("email", "qbwlkjdbdaf");
          contact.put("name", "Andy");
          message.put("contact", contact);

          httppost.setEntity(new StringEntity(message.toString(), "UTF-8"));
          HttpResponse response = httpclient.execute(httppost);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    thread.start();
  }

  @Override
  public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
    cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
    cal.set(Calendar.MINUTE, minute);
    Date date = cal.getTime();
    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa");
    mTimeButton.setText(timeFormat.format(date));
  }

  @Override
  public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, monthOfYear);
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    Date date = cal.getTime();
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
    mDateButton.setText(dateFormat.format(date));
    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa");
    mTimeButton.setText(timeFormat.format(date));
  }
}
