 package cn.qingyuyu.phonetemperature;


import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

 public class MainActivity extends AppCompatActivity implements SensorEventListener {
     private TextView infoText;//显示单片机传来信息
     private TextView cpuInfo;//显示手机传感器信息
     // 手机传感器管理器
     private SensorManager mSensorManager;
     private UsbManager usbManager;//USB管理器
     UsbSerialPort port;//串口端口
     UsbSerialDriver driver;//USB设备
     boolean running =true;//用来管理子线程
     private String recData="no data";//用啦储存接收数据
     private static final String ACTION_USB_PERMISSION =
             "com.android.example.USB_PERMISSION";//申请的USB权限



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //实例化变量
        infoText=findViewById( R.id.infoText);
        cpuInfo=findViewById( R.id.cpuInfo);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        //申请权限的广播
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbReceiver, filter);

        //查找设备
        findSerialPortDevice();





    }

     @Override
     protected void onResume() {
         // 为系统的温度传感器注册监听器
         mSensorManager.registerListener(this,
                 mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
                 SensorManager.SENSOR_DELAY_GAME);
         super.onResume();
     }

     private void findSerialPortDevice() {
         //查找所有插入的设备
         List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
         if(!availableDrivers.isEmpty()) {
             driver = availableDrivers.get(0);//偷懒，一般只有一个设备
             requestUserPermission(driver.getDevice());
         }
     }
     //申请权限
     private void requestUserPermission(UsbDevice device) {
         Toast.makeText(this, "request", Toast.LENGTH_SHORT).show();
         PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
         usbManager.requestPermission(device,mPendingIntent);
     }


     //打开设备
     public boolean openPort() {
         // 打开设备，建立通信连接
         UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
         if (connection == null) {
             // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
             return false;
         }


         //打开端口，设置端口参数
          port = driver.getPorts().get(0);//一般只有一个接口，一个端口
         try {
             port.open(connection);
//四个参数分别是：波特率，数据位，停止位，校验位
             port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (running)
                        {
                            try {
                                Thread.sleep(1000);
                                byte buffer[]=new byte[26];
                                int l=port.read(buffer,1000);
                                if(l!=0) {
                                    recData = new String(buffer, 0, l);//数据放入recData
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            infoText.setText("当前温度："+recData);
                                        }
                                    });
                                    if(Float.parseFloat(recData)<5)//温度小于5度
                                    {
                                        port.write("start".getBytes(),"start".length());//让单片机开始工作
                                    }
                                    else if(Float.parseFloat(recData)>25)
                                    {
                                        port.write("close".getBytes(),"close".length());
                                    }
                                }

                            } catch (Exception e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        infoText.setText("打开设备出错"+e.toString());
                                    }
                                });
                            }
                        }
                    }
                }).start();
         } catch (Exception e) {
             Toast.makeText(this,""+e,Toast.LENGTH_SHORT).show();
         }
         return true;
     }
     private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             infoText.setText(action);
             if (ACTION_USB_PERMISSION.equals(action)) {//申请权限广播
                 synchronized (this) {
                     UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                     if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                         if(null != usbDevice){
                           infoText.setText("获得权限");
                           openPort();
                         }
                     }

                 }

             }
             if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {//设备拔出广播
                 UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                 if (usbDevice != null) {
                     running=false;
                     try {
                         port.close();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     infoText.setText("与设备断开连接");
                 }
             }else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                 //当设备插入时执行具体操作
                 infoText.setText("设备接入");
             }
         }
     };
     @Override
     // 当传感器精度改变时回调该方法。
     public void onAccuracyChanged(Sensor sensor, int accuracy)
     {
     }
     @Override
     public void onSensorChanged(SensorEvent event) {
         float[] values = event.values;
         // 获取触发event的传感器类型
         int sensorType = event.sensor.getType();
// 温度传感器
         if(sensorType==Sensor.TYPE_AMBIENT_TEMPERATURE) {
             cpuInfo.setText("当前手机温度："+values[0]);
         }
     }
     @Override
     protected void onDestroy() {
         // 程序退出时取消注册传感器监听器
         mSensorManager.unregisterListener(this);
        running=false;
       unregisterReceiver(mUsbReceiver);//取消广播监听
        if(port!=null) {//如果设备被打开
            try {
                port.close();//关闭设备
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
         super.onDestroy();
     }
 }
