package com.wst.babyplan;

import com.wst.model.Student;
import com.wst.util.ModelConfig;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class StudentInfoActivity extends Activity{
	private Spinner ageSpinner;
	private Spinner gradeSpinner;
	private Spinner sexSpinner;
	private Button  saveBtn;
	private Button  cancelBtn;
	private EditText nameEdit;
	private EditText remarkEdit;
	private ArrayAdapter<String> ageAdapter;
	private ArrayAdapter<String> gradeAdapter;
	private ArrayAdapter<String> sexAdapter;
	
	private String sname = "";
	private int sage;
	private String sgrade = "";
	private String ssex = "";
	private String sremark = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.student);
		
		Intent intent = this.getIntent();
		String where = intent.getStringExtra("where");
		
		ageSpinner = (Spinner)findViewById(R.id.sageSpinner);
		gradeSpinner = (Spinner)findViewById(R.id.sgradeSpinner);
		sexSpinner = (Spinner)findViewById(R.id.ssexSpinner);
		saveBtn = (Button)findViewById(R.id.ssaveBtn);
		cancelBtn = (Button)findViewById(R.id.scancelBtn);
		nameEdit = (EditText)findViewById(R.id.snameEdit);
		remarkEdit = (EditText)findViewById(R.id.sremarkEdit);
		
		saveBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
            	sname = nameEdit.getText().toString();
            	sremark = remarkEdit.getText().toString();
                if(sname.equals("")){
                	new AlertDialog.Builder(StudentInfoActivity.this)
                    .setTitle("爱的提示").setIcon(R.drawable.red_heart)
                    .setMessage("亲爱的，你要输入学生姓名哦")
                    .setNegativeButton("嗯呐，麼麼", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int whichButton) {
                    //取消按钮事件
                    }
                    })
                    .show(); 
                }else{
                	if(sremark.equals("")){
                		sremark = ModelConfig.cremark;
                	}
                	Student s = new Student(sname, ssex, sage, sgrade,0, sremark);
                	s.insertToDB(StudentInfoActivity.this);
                	
                	finish();
                    Intent intent = new Intent(StudentInfoActivity.this, MainActivity.class);
                    startActivity(intent);  // enter the main activity finally
                }
            }
        });
		
		cancelBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
            	finish();
                Intent intent = new Intent(StudentInfoActivity.this, MainActivity.class);
                startActivity(intent);  // enter the main activity finally
            }
        });
		
		ageAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,ModelConfig.ages);
		ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ageSpinner.setAdapter(ageAdapter);
		
		if(where.equals("new")){
			ageSpinner.setSelection(8, true);
	        sage = Integer.parseInt(ModelConfig.ages[8]);
		}else{
			int age = intent.getIntExtra("age",18);
			int aIndex = 0;
			for(aIndex = 0; aIndex < ModelConfig.ages.length; aIndex++){
				if(String.valueOf(age).equals(ModelConfig.ages[aIndex])){
					ageSpinner.setSelection(aIndex, true);
			        sage = Integer.parseInt(ModelConfig.ages[aIndex]);
			        break;
				}
			}
		}
        
		
		ageSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){      
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {     
                // TODO Auto-generated method stub          
                sage = Integer.parseInt(ageAdapter.getItem(arg2));
                /* 将mySpinner 显示*/         
            }       
            public void onNothingSelected(AdapterView<?> arg0) {     
                // TODO Auto-generated method stub         
                arg0.setVisibility(View.VISIBLE);     
            }     
        });
		
		gradeAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,ModelConfig.grades);
		gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		gradeSpinner.setAdapter(gradeAdapter);
        
		
		if(where.equals("new")){
			gradeSpinner.setSelection(2, true);
			sgrade = ModelConfig.grades[2];
		}else{
			String gradeStr = intent.getStringExtra("grade");
			int gIndex = 0;
			for(gIndex = 0; gIndex < ModelConfig.grades.length; gIndex++){
				if(gradeStr.equals(ModelConfig.grades[gIndex])){
					gradeSpinner.setSelection(gIndex, true);
			        sgrade = ModelConfig.grades[gIndex];
			        break;
				}
			}
		}
        
		gradeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){      
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {     
                // TODO Auto-generated method stub          
                sgrade = gradeAdapter.getItem(arg2);
                /* 将mySpinner 显示*/         
            }       
            public void onNothingSelected(AdapterView<?> arg0) {     
                // TODO Auto-generated method stub         
                arg0.setVisibility(View.VISIBLE);     
            }     
        });
		
		sexAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,ModelConfig.sexs);
		sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sexSpinner.setAdapter(sexAdapter);
		
		if(where.equals("new")){
			sexSpinner.setSelection(0, true);
			ssex = ModelConfig.sexs[0];
		}else{
			String sexStr = intent.getStringExtra("sex");
			int sIndex = 0;
			for(sIndex = 0; sIndex < ModelConfig.sexs.length; sIndex++){
				if(sexStr.equals(ModelConfig.sexs[sIndex])){
					sexSpinner.setSelection(sIndex, true);
			        ssex = ModelConfig.sexs[sIndex];
			        break;
				}
			}
		}

		sexSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){      
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {     
                // TODO Auto-generated method stub          
                ssex = sexAdapter.getItem(arg2);
                /* 将mySpinner 显示*/         
            }       
            public void onNothingSelected(AdapterView<?> arg0) {     
                // TODO Auto-generated method stub         
                arg0.setVisibility(View.VISIBLE);     
            }     
        });
		
		if(where.equals("old")){
			String remarkStr = intent.getStringExtra("remark");
			remarkEdit.setText(remarkStr);
			String nameStr = intent.getStringExtra("name");
			nameEdit.setText(nameStr);
			nameEdit.setEnabled(false);
		}
	}
}
