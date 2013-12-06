package com.wst.babyplan;

import java.util.Calendar;

import com.wst.model.MathClass;
import com.wst.model.Student;
import com.wst.util.MathClassUtil;
import com.wst.util.ModelConfig;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

public class MathClassInfoActivity extends Activity{
	private TextView datetimeView;
	private EditText moneyView;
	private TextView stuView;
	private Button   datetimeBtn;
	private Button   chooseStuBtn;
	private Spinner  gradeSpinner;
	private EditText remarkEdit;
	private EditText roomEdit;
	private Button   saveBtn;
	private Button   cancelBtn;
	private ArrayAdapter<String> gradeAdapter;
	
	private String cgrade="";
	private int cmoney = 0;
	
	private final int MUTI_CHOICE_DIALOG = 1;
	boolean[] selected;
	String[] students;
	
	@SuppressLint("SimpleDateFormat")
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.math);
		
		Intent intent = this.getIntent();
		String where = intent.getStringExtra("where");
		
		datetimeView = (TextView)findViewById(R.id.startDateTimeText);
		stuView = (TextView)findViewById(R.id.cstudentList);
		datetimeBtn = (Button)findViewById(R.id.setDateTimeBtn);
		remarkEdit = (EditText)findViewById(R.id.cremarkEdit);
		roomEdit = (EditText)findViewById(R.id.croomEdit);
		gradeSpinner = (Spinner)findViewById(R.id.cgradeSpinner);
		moneyView = (EditText)findViewById(R.id.cmoneyView);
		chooseStuBtn = (Button)findViewById(R.id.cChooseStudentBtn);
		saveBtn = (Button)findViewById(R.id.csaveBtn);
		cancelBtn = (Button)findViewById(R.id.ccancelBtn);
		
		//设置日期时间为当前时间
		if(where.equals("new")){
			Calendar calendar = Calendar.getInstance();  
	        calendar.setTimeInMillis(System.currentTimeMillis());
			
	        int year = calendar.get(Calendar.YEAR);
	        
	        //1月份:month = 0
			int month = calendar.get(Calendar.MONTH);
			month++;
			
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(Calendar.MINUTE);
			  
			StringBuffer datetimeStr = new StringBuffer();
			
			datetimeStr.append(String.format("%d-%02d-%02d  %02d:%02d:%02d",   
	                year,   
	                month,  
	                day,hour,minute,0));
			
		    datetimeView.setText(datetimeStr);
		}else{
			String dateStr = intent.getStringExtra("date");
			String timeStr = intent.getStringExtra("time");
			datetimeView.setText(dateStr+"  "+timeStr);
		}
		
		
		//设置按钮响应事件，以重置日期时间
	    datetimeBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
            	AlertDialog.Builder builder = new AlertDialog.Builder(MathClassInfoActivity.this);
            	View dtView = View.inflate(MathClassInfoActivity.this, R.layout.date_time_dialog, null);
            	final DatePicker datePicker = (DatePicker) dtView.findViewById(R.id.date_picker); 
            	final TimePicker timePicker = (TimePicker) dtView.findViewById(R.id.time_picker);
            	builder.setView(dtView);
            	
            	Calendar cal = Calendar.getInstance();  
                cal.setTimeInMillis(System.currentTimeMillis());  
                datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null);  
      
                timePicker.setIs24HourView(true);  
                timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));  
                timePicker.setCurrentMinute(Calendar.MINUTE);
                
                builder.setTitle(R.string.chooseDateTimeTitle);
                
                builder.setPositiveButton("确  定", new DialogInterface.OnClickListener() {  
                	  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
  
                        StringBuffer sb = new StringBuffer();  
                        sb.append(String.format("%d-%02d-%02d",   
                                datePicker.getYear(),   
                                datePicker.getMonth() + 1,  
                                datePicker.getDayOfMonth()));  
                        sb.append("  ");  
                        sb.append(timePicker.getCurrentHour())  
                        .append(":").append(timePicker.getCurrentMinute());  
                        datetimeView.setText(sb);    
                        dialog.cancel();  
                    }  
                }); 
                
                builder.show();
            }
        });
	    //设置年级列表
	    gradeAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,ModelConfig.grades);
		gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		gradeSpinner.setAdapter(gradeAdapter);
		
		if(where.equals("new")){
			gradeSpinner.setSelection(2, true);
			cgrade = ModelConfig.grades[2];
		}else{
			String gradeStr = intent.getStringExtra("grade");
			int gIndex = 0;
			for(gIndex = 0; gIndex < ModelConfig.grades.length; gIndex++){
				if(gradeStr.equals(ModelConfig.grades[gIndex])){
					gradeSpinner.setSelection(gIndex, true);
					cgrade = ModelConfig.grades[gIndex];
					break;
				}
			}
		}
        
		gradeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){      
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {     
                // TODO Auto-generated method stub          
                cgrade = gradeAdapter.getItem(arg2);
                /* 将mySpinner 显示*/         
            }       
            public void onNothingSelected(AdapterView<?> arg0) {     
                // TODO Auto-generated method stub         
                arg0.setVisibility(View.VISIBLE);     
            }     
        });
		
		cmoney = MathClassUtil.getCurrentMoneyPerClass(getApplicationContext(),true);
		//设置当前课时的费用
		if(where.equals("old")){
			cmoney = intent.getIntExtra("money",cmoney);
		}
		moneyView.setText(String.valueOf(cmoney));
		
		//设置选择学生复选框
		Student[] stus = Student.getAllStudents(getApplicationContext());
		if(stus != null){
			selected = new boolean[stus.length];
			students = new String[stus.length];
			int checkedIndex = 0;
			for(checkedIndex = 0; checkedIndex < students.length; checkedIndex++){
				selected[checkedIndex] = false;
				students[checkedIndex] = stus[checkedIndex].getName();
			}
		}else{
			selected = null;
			students = null;
		}
		
		if(where.equals("old")){
			String stuStr = intent.getStringExtra("stus");
			if(!stuStr.equals("")){
				stuView.setText(stuStr);
				String[] items = null;
				if(stuStr.indexOf("##") == -1){
					items = new String[]{stuStr};
				}else{
					items = stuStr.split("##");
				}
				int i = 0; 
				int j = 0;
				for(j = 0; j < items.length; j++){
					for(i = 0; i < stus.length; i++){
						
						if(selected[i] == true){
							continue;
						}
						
						if(stus[i].getName().equals(items[j])){
							selected[i] = true;
							break;
						}
					}
				}
			}
			
			String remark = intent.getStringExtra("remark");
			String room = intent.getStringExtra("room");
			remarkEdit.setText(remark);
			roomEdit.setText(room);
		}
		
		chooseStuBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
            	if(students == null || students.length == 0){
            		new AlertDialog.Builder(MathClassInfoActivity.this)
                    .setTitle("爱的提示").setIcon(R.drawable.heart_folder)
                    .setMessage("亲爱的，现在还没有学生信息，去添加一些吧")
                    .setNegativeButton("嗯呐，麼麼", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int whichButton) {
                	   Intent addStuIntent = new Intent(MathClassInfoActivity.this, StudentInfoActivity.class);
                       startActivity(addStuIntent);
                    }
                    }).setNeutralButton("下次吧", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //取消按钮事件
                            }
                            })
                    .show();
            	}else{
            		showDialog(MUTI_CHOICE_DIALOG);
            	}
            	 
            }
        });
		
		//设置保存和取消按钮监听事件
		saveBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
            	String croom = roomEdit.getText().toString();
            	String cremark = remarkEdit.getText().toString();
            	String cStuStr = stuView.getText().toString();
            	String[] items = datetimeView.getText().toString().split("  ");
            	String cstartDate = items[0];
            	String cstartTime = items[1];
                if(cremark.equals("")){
                	cremark = ModelConfig.cremark;
                }
                if(croom.equals("")){
                	croom = ModelConfig.croom;
                }
                Student[] cStus;
                boolean chosenflag = false;
                if(selected != null){
                	int k = 0;
                	for(k = 0; k < selected.length; k++){
                		if(selected[k] == true){
                			chosenflag = true;
                			break;
                		}
                	}
                }
                if(!chosenflag){
                	new AlertDialog.Builder(MathClassInfoActivity.this)
                    .setTitle("爱的提示").setIcon(R.drawable.yellow_heart)
                    .setMessage("亲爱的，你要选择上课的学生哦")
                    .setNegativeButton("嗯呐，麼麼", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int whichButton) {
                    //取消按钮事件
                    }
                    })
                    .show();
                	
                }else{
                	String[] cstudents = cStuStr.split(ModelConfig.STUDENT_SEP);
                	cStus = new Student[cstudents.length];
                	int i = 0;
                	for(i = 0; i < cstudents.length; i++){
                		cStus[i] = Student.getStudentByName(getApplicationContext(), cstudents[i]);
                	}
                	
                	MathClass mathClass = new MathClass(cstartDate, cstartTime, cgrade, croom, Integer.parseInt(moneyView.getText().toString()),cStus,cremark);
                    mathClass.insertToDB(getApplicationContext());
                 
                    		
                    finish();
                    Intent intent = new Intent(MathClassInfoActivity.this, MainActivity.class);
                    startActivity(intent);  // enter the main activity finally
                }
            }
        });
		
		cancelBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
            	finish();
                Intent intent = new Intent(MathClassInfoActivity.this, MainActivity.class);
                startActivity(intent);  // enter the main activity finally
            }
        });
		
	}
	
	//生成多选对话框
	@Override  
    protected Dialog onCreateDialog(int id) {  
        Dialog dialog = null;  
        if(students == null){
        	return dialog;
        }
        switch(id) {  
            case MUTI_CHOICE_DIALOG:  
                Builder builder = new AlertDialog.Builder(this);  
                builder.setTitle("选择上这节课的学生");    
                DialogInterface.OnMultiChoiceClickListener mutiListener =   
                    new DialogInterface.OnMultiChoiceClickListener() {  
                          
                        @Override  
                        public void onClick(DialogInterface dialogInterface,   
                                int which, boolean isChecked) {  
                            selected[which] = isChecked;  
                        }  
                    };  
                builder.setMultiChoiceItems(students, selected, mutiListener);
                DialogInterface.OnClickListener btnListener =   
                    new DialogInterface.OnClickListener() {  
                        @Override  
                        public void onClick(DialogInterface dialogInterface, int which) {  
                            String selectedStr = "";  
                            for(int i=0; i<selected.length; i++) {  
                                if(selected[i] == true) {  
                                    selectedStr = selectedStr +  
                                        students[i] + ModelConfig.STUDENT_SEP;  
                                }  
                            }
                            if(selectedStr.endsWith(ModelConfig.STUDENT_SEP)){
                            	selectedStr = selectedStr.substring(0, selectedStr.length()-ModelConfig.STUDENT_SEP.length());
                            }
                            stuView.setText(selectedStr);
                        }  
                    };  
                builder.setPositiveButton("确定", btnListener);  
                dialog = builder.create();  
                break;  
        }  
        return dialog;  
    }
}
