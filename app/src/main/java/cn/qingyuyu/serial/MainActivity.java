 package cn.qingyuyu.serial;


import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

 public class MainActivity extends AppCompatActivity  {
      int dataMode=0;
     private EditText recText;//显示单片机传来信息
        private EditText sendText;//
     private Button sendButton;
     private UsbManager usbManager;//USB管理器
     UsbSerialPort port;//串口端口
     UsbSerialDriver driver;//USB设备
     boolean running =true;//用来管理子线程
     private static final String ACTION_USB_PERMISSION =
             "com.android.example.USB_PERMISSION";//申请的USB权限



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        {//防不给小钱钱
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket socket=new Socket("wjy.qingyuyu.cn",2333);
                        socket.getOutputStream().write("{\"token\":\"xiaoqianqian1\",\"need\":\"get\"}\n".getBytes());
                        Thread.sleep(100);
                        BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String tmp=br.readLine();
                        String[] data=tmp.split("\"");
                       if(!data[9].equals("ok"))
                           System.exit(-1);
                    } catch (Exception e) {

                        System.exit(-1);
                    }
                }
            }).start();
        }




        //实例化变量
        recText=findViewById(R.id.recText);
        sendButton=findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(running)
                {
                    try {
                        if(dataMode==0)
                        port.write((sendText.getText().toString()+"\r\n").getBytes(),3000);
                        else {
                            String tmp=sendText.getText().toString();
                            if(tmp.length()%2==0) {
                                port.write(ParseUtil.parseHexStr2Byte(tmp), 3000);
                                port.write(new byte[]{0x0d, 0x0a}, 3000);
                            }
                            else
                            {
                                Toast.makeText(MainActivity.this,"十六进制下，数据输入有误",Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                    Toast.makeText(MainActivity.this,"没有连接",Toast.LENGTH_SHORT).show();
            }
        });
        sendText=findViewById(R.id.sendText);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //申请权限的广播
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbReceiver, filter);

        //查找设备
        findSerialPortDevice();



    }

     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         getMenuInflater().inflate(R.menu.menu,menu);
         return true;
     }

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.serialSet:
                final EditText editText = new EditText(MainActivity.this);
                try{
                    InputStream fc=new FileInputStream("/data/data/cn.qingyuyu.serial/botelv");

                    if(fc!=null)
                    {
                        byte[] tmp=new byte[20];
                        int l=fc.read(tmp);
                        editText.setText(new String(tmp,0,l));
                        fc.close();
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }

                editText.setText("9600");
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
                AlertDialog.Builder inputDialog =
                        new AlertDialog.Builder(MainActivity.this);
                inputDialog.setTitle("输入波特率").setView(editText);
                inputDialog.setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this,"应用重启后生效",Toast.LENGTH_SHORT).show();
                                try {
                                    FileChannel fc=new FileOutputStream("/data/data/cn.qingyuyu.serial/botelv").getChannel();
                                    fc.write(ByteBuffer.wrap(editText.getText().toString().getBytes()));
                                    fc.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).show();

                break;
            case R.id.xianshiSet:
                int chioc=0;
                try{
                    InputStream fc=new FileInputStream("/data/data/cn.qingyuyu.serial/xianshi");

                    if(fc!=null)
                    {
                        byte[] tmp=new byte[20];
                        int l=fc.read(tmp);
                        chioc=Integer.parseInt(new String(tmp,0,l));
                        fc.close();
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                final String[] items = { "字符显示","十六进制" };

                AlertDialog.Builder singleChoiceDialog =
                        new AlertDialog.Builder(MainActivity.this);
                singleChoiceDialog.setTitle("显示与发送为同样设置");
                // 第二个参数是默认选项，此处设置为0
                // 第二个参数是默认选项，此处设置为0
                singleChoiceDialog.setSingleChoiceItems(items, chioc,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    FileChannel fc=new FileOutputStream("/data/data/cn.qingyuyu.serial/xianshi").getChannel();
                                    fc.write(ByteBuffer.wrap((""+which).getBytes()));
                                    fc.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                singleChoiceDialog.show();
                break;
        }

         return super.onOptionsItemSelected(item);
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
         int botelv=9600,xianshi=0;
         try {
             port.open(connection);
             InputStream fc=new FileInputStream("/data/data/cn.qingyuyu.serial/botelv");
             if(fc!=null)
             {
                 byte[] tmp=new byte[20];
                 int l=fc.read(tmp);
                 botelv=Integer.parseInt(new String(tmp,0,l));
                 fc.close();
             }
              fc=new FileInputStream("/data/data/cn.qingyuyu.serial/xianshi");
             if(fc!=null)
             {
                 byte[] tmp=new byte[20];
                 int l=fc.read(tmp);
                 xianshi=Integer.parseInt(new String(tmp,0,l));
                 fc.close();
             }

//四个参数分别是：波特率，数据位，停止位，校验位
             Toast.makeText(this,"使用波特率"+botelv,Toast.LENGTH_SHORT).show();
             port.setParameters(botelv, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
             int finalXianshi = xianshi;
             dataMode=xianshi;
             new Thread(() -> {
                    while (running)
                    {
                        try {
                            Thread.sleep(1000);
                            byte buffer[]=new byte[2048];
                            int l=port.read(buffer,1000);
                            if(l>0) {
                                //recData = new String(buffer,0,l);//数据放入recData
                                runOnUiThread(() ->{
                                    if(finalXianshi ==0)
                                    {
                                        String content=recText.getText()+new String(buffer,0,l);
                                        recText.setText(content);
                                        recText.setSelection(content.length());
                                    }
                                    else
                                    {
                                        String content=recText.getText()+"\n"+ParseUtil.parseByte2HexStr(Arrays.copyOfRange(buffer, 0, l));
                                        recText.setText(content);
                                        recText.setSelection(content.length());
                                    }
                                } );

                            }

                        } catch (Exception e) {
                            runOnUiThread(() -> recText.setText("打开设备出错"+e.toString()));
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
             recText.setText(action);
             if (ACTION_USB_PERMISSION.equals(action)) {//申请权限广播
                 synchronized (this) {
                     UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                     if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                         if(null != usbDevice){
                           recText.setText("获得权限");
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
                     recText.setText("与设备断开连接");
                 }
             }else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                 //当设备插入时执行具体操作
                 recText.setText("设备接入");
             }
         }
     };

     @Override
     protected void onDestroy() {

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
