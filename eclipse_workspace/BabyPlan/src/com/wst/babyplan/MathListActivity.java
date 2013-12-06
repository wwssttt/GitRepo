package com.wst.babyplan;


import com.wst.model.MathClass;
import com.wst.model.Student;
import com.wst.util.MathClassUtil;
import com.wst.util.ModelConfig;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import br.com.dina.ui.widget.UITableView;
import br.com.dina.ui.widget.UITableView.ClickListener;


public class MathListActivity extends Activity implements OnClickListener{

	UITableView tableView;
	MathClass[] maths;
	Button addClassBtn;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        maths = MathClassUtil.getAllClasses(getApplicationContext(), ModelConfig.TIME_LEVEL.Total);
        
        if(maths == null || maths.length == 0){
        	setContentView(R.layout.nomathlist);
        	addClassBtn = (Button)findViewById(R.id.addClassBtn);
        	addClassBtn.setOnClickListener(this);
        	return;
        }
        
        setContentView(R.layout.mathlist);
        
        tableView = (UITableView) findViewById(R.id.mathTableView);
        
        createList();
        
        Log.d("MainActivity", "total items: " + tableView.getCount());
        
        tableView.commit();
    }
    
    @Override
	public void onClick(View v) {
    	if(v == addClassBtn){
    		Intent intent = new Intent(MathListActivity.this, MathClassInfoActivity.class);
            startActivity(intent);
    	}
    }
    
    private void createList() {
    	CustomClickListener listener = new CustomClickListener();
    	tableView.setClickListener(listener);
    	
    	int index = 0;
    	for(index = 0; index < maths.length; index++){
    		MathClass math = maths[index];
    		String date = math.getDate();
    		String time = math.getStartTime();
    		String room = math.getRoom();
    		String grade = math.getGrade();
    		int money = math.getMoney();
    		String remark = math.getRemark();
    		Student[] stus = math.getStudents();
    		String stuStr = "";
    		for(int i = 0; i < stus.length; i++){
    			stuStr += stus[i].getName()+"##";
    		}
    		if(stuStr.endsWith("##")){
    			stuStr = stuStr.substring(0, stuStr.length()-"##".length());
    		}
    		
    		String[] items = date.split("-");
    		int year = Integer.valueOf(items[0]);
    		int month = Integer.valueOf(items[1]);
    		int day = Integer.valueOf(items[2]);
    		
    		String[] itemss = time.split(":");
    		int hour = Integer.valueOf(itemss[0]);
    		int minute = Integer.valueOf(itemss[1]);
    		 
    		StringBuffer datetimeStr = new StringBuffer();
    		
    		datetimeStr.append(String.format("%d-%02d-%02d %02d:%02d:%02d",   
                    year,   
                    month,  
                    day,hour,minute,0));
    		
    		tableView.addBasicItem(R.drawable.lesson_item, datetimeStr.toString(), "薪酬:"+money+"  年级:"+grade+"  学生："+stuStr+"  教室:"+room+"  备注:"+remark);
    		
    		
    	}
    }
    
    private class CustomClickListener implements ClickListener {

		@Override
		public void onClick(int index) {
			
			MathClass math = maths[index];
    		String date = math.getDate();
    		String time = math.getStartTime();
    		String room = math.getRoom();
    		String grade = math.getGrade();
    		int money = math.getMoney();
    		String remark = math.getRemark();
    		Student[] stus = math.getStudents();
    		String stuStr = "";
    		for(int i = 0; i < stus.length; i++){
    			stuStr += stus[i].getName()+"##";
    		}
    		if(stuStr.endsWith("##")){
    			stuStr = stuStr.substring(0, stuStr.length()-"##".length());
    		}
    		
    		Intent editMathIntent = new Intent(MathListActivity.this, MathClassInfoActivity.class);
    		
    		editMathIntent.putExtra("where", "old");
    		editMathIntent.putExtra("date", date);
    		editMathIntent.putExtra("time", time);
    		editMathIntent.putExtra("room", room);
    		editMathIntent.putExtra("grade", grade);
    		editMathIntent.putExtra("money", money);
    		editMathIntent.putExtra("remark", remark);
    		editMathIntent.putExtra("stus", stuStr);
    		
            startActivity(editMathIntent);
    		
		}
    	
    }
}
