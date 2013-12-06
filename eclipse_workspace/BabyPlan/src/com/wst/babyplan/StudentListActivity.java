package com.wst.babyplan;

import com.wst.model.Student;
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


public class StudentListActivity extends Activity implements OnClickListener{

	UITableView tableView;
	Student[] stus;
	Button addStuBtn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        stus = Student.getAllStudents(getApplicationContext());
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        if(stus == null || stus.length == 0){
        	setContentView(R.layout.nostulist);
        	addStuBtn = (Button)findViewById(R.id.addStuBtn);
        	addStuBtn.setOnClickListener(this);
        	return;
        }
        
        setContentView(R.layout.stulist);
        
        tableView = (UITableView) findViewById(R.id.stuTableView);
        
        createList();
        
        Log.d("MainActivity", "total items: " + tableView.getCount());
        
        tableView.commit();
    }
    
    @Override
	public void onClick(View v) {
    	if(v == addStuBtn){
    		Intent intent = new Intent(StudentListActivity.this, StudentInfoActivity.class);
            startActivity(intent);
    	}
    }
    
    private void createList() {
    	CustomClickListener listener = new CustomClickListener();
    	tableView.setClickListener(listener);
    	
    	int index = 0;
    	for(index = 0; index < stus.length; index++){
    		Student s = stus[index];
    		String sex = s.getSex();
    		String name = s.getName();
    		String grade = s.getGrade();
    		int age = s.getAge();
    		String remark = s.getRemark();
    		int count = s.getCount(getApplicationContext());
    		if(sex.equals(ModelConfig.sexs[0])){
    			tableView.addBasicItem(R.drawable.boy, name, "总课时:"+count+"  年龄:"+age+"  年级:"+grade+"  备注："+remark);
    		}else if(sex.equals(ModelConfig.sexs[1])){
    			tableView.addBasicItem(R.drawable.girl, name, "总课时:"+count+"  年龄:"+age+"  年级:"+grade+"  备注："+remark);
    		}else{
    			tableView.addBasicItem(R.drawable.unkown, name, "总课时:"+count+"  年龄:"+age+"  年级:"+grade+"  备注："+remark);
    		}
    		
    		
    	}
    }
    
    private class CustomClickListener implements ClickListener {

		@Override
		public void onClick(int index) {
			
			Student s = stus[index];
    		String sex = s.getSex();
    		String name = s.getName();
    		String grade = s.getGrade();
    		int age = s.getAge();
    		String remark = s.getRemark();
    		
    		Intent editStuIntent = new Intent(StudentListActivity.this, StudentInfoActivity.class);
    		
    		editStuIntent.putExtra("where", "old");
    		editStuIntent.putExtra("name",name);
    		editStuIntent.putExtra("sex",sex);
    		editStuIntent.putExtra("grade",grade);
    		editStuIntent.putExtra("age",age);
    		editStuIntent.putExtra("remark",remark);
    		
    		startActivity(editStuIntent);
		}
    	
    }
    
}
