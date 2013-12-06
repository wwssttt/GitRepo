package com.wst.babyplan;

import java.util.ArrayList;

import br.com.dina.ui.model.BasicItem;
import br.com.dina.ui.widget.UITableView;
import br.com.dina.ui.widget.UITableView.ClickListener;

import com.wst.util.MathClassUtil;
import com.wst.util.ModelConfig;
import com.wst.util.ModelConfig.TIME_LEVEL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.app.PendingIntent;

public class MainActivity extends Activity{
    
	UITableView tableView;
	
	/*
	Button student;
	Button classes;
	Button viewStu;
	Button viewMath;
	Button sms;
	*/
	
	TextView todayCountView;
	TextView todayMoneyView;
	TextView weekCountView;
	TextView weekMoneyView;
	TextView monthCountView;
	TextView monthMoneyView;
	TextView totalCountView;
	TextView totalMoneyView;
	
	private boolean flag = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_main);
        
        /*
        student = (Button)findViewById(R.id.student);
        classes = (Button)findViewById(R.id.classes);
        viewStu = (Button)findViewById(R.id.viewStudent);
        viewMath = (Button)findViewById(R.id.viewClasses);
        sms = (Button)findViewById(R.id.sms);
        */
        
        todayCountView = (TextView)findViewById(R.id.todayCount);
        todayCountView.setText(String.valueOf(MathClassUtil.getTotalClassCount(getApplicationContext(),TIME_LEVEL.Today)));
        todayMoneyView = (TextView)findViewById(R.id.todayMoney);
        todayMoneyView.setText(String.valueOf(MathClassUtil.getTotalMoney(getApplicationContext(),TIME_LEVEL.Today)));
        weekCountView = (TextView)findViewById(R.id.weekCount);
        weekCountView.setText(String.valueOf(MathClassUtil.getTotalClassCount(getApplicationContext(),TIME_LEVEL.Week)));
        weekMoneyView = (TextView)findViewById(R.id.weekMoney);
        weekMoneyView.setText(String.valueOf(MathClassUtil.getTotalMoney(getApplicationContext(),TIME_LEVEL.Week)));
        monthCountView = (TextView)findViewById(R.id.monthCount);
        monthCountView.setText(String.valueOf(MathClassUtil.getTotalClassCount(getApplicationContext(),TIME_LEVEL.Month)));
        monthMoneyView = (TextView)findViewById(R.id.monthMoney);
        monthMoneyView.setText(String.valueOf(MathClassUtil.getTotalMoney(getApplicationContext(),TIME_LEVEL.Month)));
        totalCountView = (TextView)findViewById(R.id.totalCount);
        totalCountView.setText(String.valueOf(MathClassUtil.getTotalClassCount(getApplicationContext(),TIME_LEVEL.Total)));
        totalMoneyView = (TextView)findViewById(R.id.totalMoney);
        totalMoneyView.setText(String.valueOf(MathClassUtil.getTotalMoney(getApplicationContext(),TIME_LEVEL.Total)));
        
        /*
        student.setOnClickListener(this);
        classes.setOnClickListener(this);
        viewStu.setOnClickListener(this);
        viewMath.setOnClickListener(this);
        sms.setOnClickListener(this);
        */
        
        tableView = (UITableView) findViewById(R.id.menuTableView);
        
        createList();
        
        Log.d("MainActivity", "total items: " + tableView.getCount());
        
        tableView.commit();
    }

    /*
	@Override
	public void onClick(View v) {
		if (v == student) {
			  // enter the main activity finally
		}else if (v == classes) {
			  // enter the main activity finally
		}else if (v == viewStu) {
			  // enter the main activity finally
		}else if (v == viewMath) {
			  // enter the main activity finally
		}else if(v == sms){
			sendSMS("5556","Hello World");
		}
		
	}
	*/
    
    //按后退退出程序，返回主屏幕
	@Override
	public void onBackPressed() {
		if(flag == false){
			Toast.makeText(getApplicationContext(), "哎呀，不要嘛，再退人家就退出啦",
	      		     Toast.LENGTH_SHORT).show();
			flag = true;
		}else{
			   Intent home = new Intent(Intent.ACTION_MAIN);  
			   home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
			   home.addCategory(Intent.CATEGORY_HOME);  
			   startActivity(home); 
		}
		
		//System.exit(0);
    }
	

	@Override
    protected void onStop() {
        showNotification();
        super.onStop();
    }
     
    @Override
    protected void onStart() {
        clearNotification();
        super.onStart();
    }
     
    /**
     * 在状态栏显示通知
     */
    private void showNotification(){
        // 创建一个NotificationManager的引用  
        NotificationManager notificationManager = (NotificationManager)   
            this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);  
         
        // 定义Notification的各种属性  
        Notification notification =new Notification(R.drawable.icon,  
                "宝贝工资", System.currentTimeMillis());
        //FLAG_AUTO_CANCEL   该通知能被状态栏的清除按钮给清除掉
        //FLAG_NO_CLEAR      该通知不能被状态栏的清除按钮给清除掉
        //FLAG_ONGOING_EVENT 通知放置在正在运行
        //FLAG_INSISTENT     是否一直进行，比如音乐一直播放，知道用户响应
        notification.flags |= Notification.FLAG_ONGOING_EVENT; // 将此通知放到通知栏的"Ongoing"即"正在运行"组中  
        notification.flags |= Notification.FLAG_NO_CLEAR; // 表明在点击了通知栏中的"清除通知"后，此通知不清除，经常与FLAG_ONGOING_EVENT一起使用  
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;  
        //DEFAULT_ALL     使用所有默认值，比如声音，震动，闪屏等等
        //DEFAULT_LIGHTS  使用默认闪光提示
        //DEFAULT_SOUNDS  使用默认提示声音
        //DEFAULT_VIBRATE 使用默认手机震动，需加上<uses-permission android:name="android.permission.VIBRATE" />权限
        //叠加效果常量
        //notification.defaults=Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND;
        notification.defaults=Notification.DEFAULT_LIGHTS;
        notification.ledARGB = Color.CYAN;  
        notification.ledOnMS =5000; //闪光时间，毫秒
         
        // 设置通知的事件消息  
        CharSequence contentTitle ="宝贝工资"; // 通知栏标题  
        // 通知栏内容
        String contentText = "";
        contentText += getResources().getString(R.string.today)+
		                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Today)+
		                "/"+
		                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Today);
        contentText += "  "+getResources().getString(R.string.week)+
                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Week)+
                "/"+
                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Week);
        contentText += "  "+getResources().getString(R.string.month)+
                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Month)+
                "/"+
                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Month);
        contentText += "  "+getResources().getString(R.string.total)+
                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Total)+
                "/"+
                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Total); 
        Intent notificationIntent =new Intent(MainActivity.this, MainActivity.class); // 点击该通知后要跳转的Activity  
        PendingIntent contentItent = PendingIntent.getActivity(this, 0, notificationIntent, 0);  
        notification.setLatestEventInfo(this, contentTitle, contentText, contentItent);  
         
        // 把Notification传递给NotificationManager  
        notificationManager.notify(2012111, notification);  
    }
    //删除通知   
    private void clearNotification(){
        // 启动后删除之前我们定义的通知  
        NotificationManager notificationManager = (NotificationManager) this 
                .getSystemService(NOTIFICATION_SERVICE);  
        notificationManager.cancel(2012111); 
 
    }


  private void sendSMS(String phoneNumber, String message) {
	// ---sends an SMS message to another device---
	SmsManager sms = SmsManager.getDefault();
	PendingIntent pi = PendingIntent.getActivity(this, 0, 

               new Intent(this,MainActivity.class), 0);
	//if message's length more than 70 ,
	//then call divideMessage to dive message into several part 

        //and call sendTextMessage()
	//else direct call sendTextMessage()
	if (message.length() > 140) {
		ArrayList<String> msgs = sms.divideMessage(message);
		for (String msg : msgs) {
			sms.sendTextMessage(phoneNumber, null, msg, pi, null);
		}
	} else {
		sms.sendTextMessage(phoneNumber, null, message, pi, null);
	}
	Toast.makeText(MainActivity.this, "短信发送完成", Toast.LENGTH_LONG).show();
  }

  private void createList() {
  	CustomClickListener listener = new CustomClickListener();
  	tableView.setClickListener(listener);
  	
  	BasicItem addStuItem = new BasicItem(getResources().getString(R.string.addStudent));
  	addStuItem.setDrawable(R.drawable.student);
  	BasicItem addLessonItem = new BasicItem(getResources().getString(R.string.addClass));
  	addLessonItem.setDrawable(R.drawable.lesson);
  	BasicItem viewStuItem = new BasicItem(getResources().getString(R.string.viewStudent));
  	viewStuItem.setDrawable(R.drawable.viewstudent);
  	BasicItem viewLessonItem = new BasicItem(getResources().getString(R.string.viewClass));
  	viewLessonItem.setDrawable(R.drawable.viewlesson);
  	BasicItem smsItem = new BasicItem(getResources().getString(R.string.sms));
  	smsItem.setDrawable(R.drawable.sms);
  	tableView.addBasicItem(addStuItem);
  	tableView.addBasicItem(addLessonItem);
  	tableView.addBasicItem(viewStuItem);
  	tableView.addBasicItem(viewLessonItem);
  	tableView.addBasicItem(smsItem);
  }
  
  private class CustomClickListener implements ClickListener {

		@Override
		public void onClick(int index) {
			
			flag = false;
			
			switch(index){
			case 0:
				Intent addStuIntent = new Intent(MainActivity.this, StudentInfoActivity.class);
				addStuIntent.putExtra("where", "new");
                startActivity(addStuIntent);
                break;
			case 1:
				Intent addLessonIntent = new Intent(MainActivity.this, MathClassInfoActivity.class);
				addLessonIntent.putExtra("where", "new");
	            startActivity(addLessonIntent);
	            break;
			case 2:
				Intent viewStuIntent = new Intent(MainActivity.this, StudentListActivity.class);
	            startActivity(viewStuIntent);
	            break;
			case 3:
				Intent viewLessonIntent = new Intent(MainActivity.this, MathListActivity.class);
	            startActivity(viewLessonIntent);
	            break;
			case 4:
				new AlertDialog.Builder(MainActivity.this)
                .setTitle("爱的提示").setIcon(R.drawable.yellow_heart)
                .setMessage("亲爱的，你当真要跟小猪猪得瑟一下嘛")
                .setNegativeButton("嗯呐，麼麼", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int whichButton) {
                String smsStr = "";
				smsStr += getResources().getString(R.string.today)+getResources().getString(R.string.done)+
				                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Today)+
				                getResources().getString(R.string.earn)+
				                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Today)+
				                getResources().getString(R.string.yuan);
				smsStr += getResources().getString(R.string.week)+getResources().getString(R.string.done)+
		                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Week)+
		                getResources().getString(R.string.earn)+
		                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Week)+
		                getResources().getString(R.string.yuan);
				smsStr += getResources().getString(R.string.month)+getResources().getString(R.string.done)+
		                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Month)+
		                getResources().getString(R.string.earn)+
		                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Month)+
		                getResources().getString(R.string.yuan);
				smsStr += getResources().getString(R.string.total)+getResources().getString(R.string.done)+
		                MathClassUtil.getTotalClassCount(getApplicationContext(), ModelConfig.TIME_LEVEL.Total)+
		                getResources().getString(R.string.earn)+
		                MathClassUtil.getTotalMoney(getApplicationContext(), ModelConfig.TIME_LEVEL.Total)+
		                getResources().getString(R.string.yuan);
				sendSMS("18260087687",smsStr);
                }
                }).setNeutralButton("下次吧", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int whichButton) {
                }
                })
                .show();
				break;
			}
		}
  	
  }

}
