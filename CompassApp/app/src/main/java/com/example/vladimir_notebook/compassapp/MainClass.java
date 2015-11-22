package com.example.vladimir_notebook.compassapp;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;

/**
 * Created by vladimir-notebook on 19.11.15.
 */
public class MainClass extends Activity implements SensorEventListener {

    private TextView txtAzimuth;
    private TextView txtPitch;
    private TextView txtRoll;
    private TextView sensorsList;
    private TextView txtSuccess;
   /* private TextView txtSendedAzimuth;
    private TextView txtSendedPitch;
    private TextView txtSendedRoll;*/
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private BusHandler mBusHandler;
    //Чтобы NULL не возвращал куда попало, ноль хоть видно
    private double azimuth = 0;
    private double pitch = 0;
    private double roll = 0;
    private double onlyForTest = 0;

    static {
        System.loadLibrary("alljoyn_java");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        txtAzimuth = (TextView) findViewById(R.id.txtAzimuth);
        txtPitch = (TextView) findViewById(R.id.txtPitch);
        txtRoll = (TextView) findViewById(R.id.txtRoll);
        sensorsList = (TextView) findViewById(R.id.sensorsList);
        txtSuccess = (TextView) findViewById(R.id.txtSucces);
        txtAzimuth.setText("");
        txtPitch.setText("");
        txtRoll.setText("");
        sensorsList.setText("");
        txtSuccess.setText("");
        //txtSendedAzimuth.setText("");
       // txtSendedPitch.setText("");
        //txtSendedRoll.setText("");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> listOfSensord = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor list : listOfSensord) {
            sensorsList.append(list.getName() + "\n");
        }
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }


    float[] mGravity;
    float[] mGeomagnetic;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                txtSuccess.setText("True");
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                txtAzimuth.setText("" + orientation[0]);
                txtPitch.setText("" + orientation[1]);
                txtRoll.setText("" + orientation[2]);

                azimuth = orientation[0];
                pitch = orientation[1];
                roll = orientation[2];
            } else {
                txtSuccess.setText("False");
            }
        }
    }

    public void onClick (View view) {
        azimuth = azimuth + 0.5;
        pitch = pitch +0.4;
        roll = roll + 0.6;
    }
    //Implements methods of Interface
    private class AllJoynCompassSerivice implements BusObject, Compass_service {

        public double getCompassAzimuth() {
            //azimuth = onlyForTest + 1.5;
            //onlyForTest++;
            //txtSendedAzimuth.setText("" + azimuth);
            return azimuth;
        }
        public double getCompassPitch() {
            //pitch = onlyForTest +0.7;
            //onlyForTest++;
            //txtSendedPitch.setText("" + pitch);
            return pitch;
        }
        public double getCompassRoll() {
            //roll = onlyForTest + 0.3;
            //onlyForTest++;
            //txtSendedRoll.setText("" + roll);
            return roll;
        }




    }

    class BusHandler extends Handler {
        private static final String SERVICE_NAME = "com.example.bus.compass";
        private static final short CONTACT_PORT = 42;

        BusAttachment mBus;

        private AllJoynCompassSerivice mService;

        /* These are the messages sent to the BusHandler from the UI. */
        public static final int CONNECT = 1;
        public static final int DISCONNECT = 2;

        public BusHandler(Looper looper) {
            super(looper);

            mService = new AllJoynCompassSerivice();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT: {
                    org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
                    mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);

                    mBus.registerBusListener(new BusListener());

                    Status status = mBus.registerBusObject(mService, "/compass");
                    if (status != Status.OK) {
                        Toast.makeText(getApplicationContext(), "registerBusObject Failed",Toast.LENGTH_SHORT).show();
                    }
                    status = mBus.connect();
                    if (status != Status.OK) {
                        Toast.makeText(getApplicationContext(), "connect Bus Failed",Toast.LENGTH_SHORT).show();
                    }

                    Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);

                    SessionOpts sessionOpts = new SessionOpts();
                    sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
                    sessionOpts.isMultipoint = false;
                    sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
                    sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

                    status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
                        @Override
                        public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                            if (sessionPort == CONTACT_PORT) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });

                    if (status != Status.OK) {
                        finish();
                        return;
                    } else {
                        Toast.makeText(getApplicationContext(),"Binds session port Ok",Toast.LENGTH_SHORT).show();
                    }

                    status = mBus.requestName(SERVICE_NAME, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
                    if (status == Status.OK) {
                        Toast.makeText(getApplicationContext(), "requestName Success",Toast.LENGTH_SHORT).show();
                    }

                    status = mBus.advertiseName(SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
                    if (status == Status.OK) {
                        Toast.makeText(getApplicationContext(), "advertiseName Succes",Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case DISCONNECT: {
                    mBus.unregisterBusObject(mService);
                    mBus.disconnect();
                    mBusHandler.getLooper().quit();
                    break;
                }
                default:
                    break;

            }
        }
    }
}
