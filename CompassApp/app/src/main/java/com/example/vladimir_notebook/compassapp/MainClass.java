package com.example.vladimir_notebook.compassapp;

//Подключение библиотек
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by vladimir-notebook on 19.11.15.
 */
public class MainClass extends Activity implements SensorEventListener {

    //объявление всех необходимых переменных
    private TextView txtAzimuth;
    private TextView txtPitch;
    private TextView txtRoll;
    private TextView sensorsList;
    private TextView txtSuccess;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private BusHandler mBusHandler;
    //Инициализация значений углов нулём.
    private double azimuth = 0;
    private double pitch = 0;
    private double roll = 0;
    // загрузка библиотеки AllJoyn
    static {
        System.loadLibrary("alljoyn_java");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Привязываем Java файл в layout
        setContentView(R.layout.main_layout);
        //Привязываем компоненты внешнего вида к переменным
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

        //Получение ссылки на сервис управления датчиками
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> listOfSensord = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor list : listOfSensord) {
            sensorsList.append(list.getName() + "\n");
        }
        //Получаем ссылку на акселерометр
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Получаем ссылку на магнитометр
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Поток для работы в сети
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);

    }
    //Метод, который вызывается при возобновлении(и при старте) приложения
    @Override
    protected void onResume() {
        super.onResume();
        //Регистрация слушателей
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    //Метод, который вызывается при остановке приложения
    @Override
    protected void onPause() {
        super.onPause();
        //Сброс регистрации слушателей
        mSensorManager.unregisterListener(this);
    }

    @Override
    //Необходимая строчка. Она есть в интерфейс SensorEventListener и, поэтому нельзя
    //не реализовать метод. Даже если он будет пустой
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    //Массивы, для хранения данных с датчиков
    float[] mGravity;
    float[] mGeomagnetic;
    //Метод, который вызывается, если слушатели заметили изменение параметров
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Если событие произошло на Акселерометре, то получаем с него данные
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        //Если событие произошло на Магнитометре, то получаем с него данные
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        //Для вычисления положения устройства в пространстве необходимы показания с обоих датчиков
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            //Получение матрицы вращения
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            //если успешно, то выводим значения на экран и сохраняем их для передачи
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
    //метод для моделирования изменения сигнала
    public void onClick (View view) {
        azimuth = azimuth + 0.5;
        pitch = pitch +0.4;
        roll = roll + 0.6;
    }
    //метод для кнопки
    public void goToNewActivity (View view) {
        Intent intent = new Intent(this,GsmClass.class);
        startActivity(intent);
    }
    //Сервисная часть приложения. Методы, которые передают данные на другое устройство
    private class AllJoynCompassSerivice implements BusObject, Compass_service {

        public double getCompassAzimuth() {
            return azimuth;
        }
        public double getCompassPitch() {
            return pitch;
        }
        public double getCompassRoll() {
            return roll;
        }




    }
    // Класс, в котором происходит вся сетевая часть
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
