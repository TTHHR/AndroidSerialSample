 package cn.qingyuyu.phonetemperature;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.SerialInputStream;
import com.felhr.usbserial.SerialOutputStream;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

 public class MainActivity extends AppCompatActivity {
private TextView infoText;
private ListView listView;
     private UsbDevice device;
     private UsbManager usbManager;
     private UsbDeviceConnection connection;
     private UsbSerialDevice serialPort;
     boolean running =true;
     private String recData="";
     private SerialOutputStream serialOutputStream;
     private SerialInputStream serialInputStream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoText=findViewById( R.id.infoText);
        listView=findViewById(R.id.listView);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
         findSerialPortDevice();

    }
     private void findSerialPortDevice() {
         // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
         HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
         if (!usbDevices.isEmpty()) {
             boolean keep = true;
             for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                 device = entry.getValue();
                 int deviceVID = device.getVendorId();
                 int devicePID = device.getProductId();

                 if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) && deviceVID != 0x5c6 && devicePID != 0x904c) {

                     requestUserPermission();
                     keep = false;
                 }
                 if (!keep)
                     break;
             }
         }
     }
     private void requestUserPermission() {
         Toast.makeText(this,"request",Toast.LENGTH_SHORT).show();
         PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.android.example.USB_PERMISSION"), 0);
         usbManager.requestPermission(device, mPendingIntent);
     }

     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         if (grantResults[0] == PackageManager.PERMISSION_GRANTED)// 获取到权限，作相应处理
         {
             Toast.makeText(this,"打开串口",Toast.LENGTH_SHORT).show();
             connection = usbManager.openDevice(device);
             serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
             serialPort.syncOpen();
             serialPort.setBaudRate(9600);
             serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
             serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
             serialPort.setParity(UsbSerialInterface.PARITY_NONE);
             serialInputStream = serialPort.getInputStream();
              serialOutputStream = serialPort.getOutputStream();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (running)
                    {
                        try {
                            int l=serialInputStream.available();
                            final byte [] data=new byte[l];
                            serialInputStream.read(data);
                           recData=new String(data);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();
         }
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
     }

     @Override
     protected void onDestroy() {
        running=false;
        if(serialPort!=null)
        serialPort.close();
         super.onDestroy();
     }
 }
