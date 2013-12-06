package com.wst.model;

import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.wst.util.Encript;
import com.wst.util.ModelConfig;
import com.wst.util.DBHelper;

public class MathClass {
	private String date;
	private String startTime;
	private String grade;
	private int money;
	private String room;
	private Student[] students;
	private String remark;
	
	public MathClass(String date, String startTime, String grade, String room, int money,Student[] students, String remark){
		this.date = date;
		this.startTime = startTime;
		this.grade = grade;
		this.money = money;
		this.room = room;
		this.students = students;
		this.remark = remark;
	}
	
	public MathClass(String date, String startTime, String grade, String room, int money,Student[] students){
		this.date = date;
		this.startTime = startTime;
		this.grade = grade;
		this.money = money;
		this.room = room;
		this.students = students;
		this.remark = ModelConfig.cremark;
	}
	
	public MathClass(String date, String startTime, String grade, int money,Student[] students){
		this.date = date;
		this.startTime = startTime;
		this.grade = grade;
		this.money = money;
		this.room = ModelConfig.croom;
		this.students = students;
		this.remark = ModelConfig.cremark;
	}
	
	public String getDate(){
		return date;
	}
	
	public void setDate(String date){
		this.date = date;
	}
	
	public String getStartTime(){
		return startTime;
	}
	
	public void setStartTime(String startTime){
		this.startTime = startTime;
	}
	
	public String getGrade(){
		return grade;
	}
	
	public void setGrade(String grade){
		this.grade = grade;
	}
	
	public String getRoom(){
		return room;
	}
	
	public void setRoom(String room){
		this.room = room;
	}
	
	public int getMoney(){
		return money;
	}
	
	public void setMoney(int money){
		this.money = money;
	}
	
	public Student[] getStudents(){
		return students;
	}
	
	public void setStudents(Student[] students){
		this.students = students;
	}
	
	public String getRemark(){
		return remark;
	}
	
	public void setRemark(String remark){
		this.remark = remark;
	}
	
	@SuppressLint("SimpleDateFormat")
	public long getStartDateTime(){
		String datetimeStr = this.date+" "+this.startTime;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long dl = 0;
		try{
			Date d = df.parse(datetimeStr);
			dl = d.getTime();
		}catch(Exception e){
			e.printStackTrace();
		}
		return dl;
	}
	
	public void print(){
		int index = 0;
		if(students == null){
			return;
		}
		for(index = 0; index < students.length; index++){
			Student s = students[index];
			s.print();
		}
	}
	
	/**
	 * 将课时信息插入到数据库中
	 * @param context   上下文环境
	 */
	@SuppressLint("SimpleDateFormat")
	public void insertToDB(Context context){
		//TODO:save me to DB
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		String md5 = Encript.md5(this.date+this.startTime);
		
		String[] items = this.date.split("-");
		int year = Integer.valueOf(items[0]);
		int month = Integer.valueOf(items[1]);
		int day = Integer.valueOf(items[2]);
		
		String[] itemss = this.startTime.split(":");
		int hour = Integer.valueOf(itemss[0]);
		int minute = Integer.valueOf(itemss[1]);
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		StringBuffer datetimeStr = new StringBuffer();
		
		datetimeStr.append(String.format("%d-%02d-%02d %02d:%02d:%02d",   
                year,   
                month,  
                day,hour,minute,0));
		
		long dl = 0;
		try{
			Date d = df.parse(datetimeStr.toString());
			dl = d.getTime();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		Cursor cursor = mDataBase.rawQuery("select * from mathclass where id = ?", new String[]{md5});
		if(cursor.getCount() > 0){
			Toast.makeText(context, "课时插入失败，同一时间只能上一节课哦亲",
		   		     Toast.LENGTH_SHORT).show();
		}else{
			//TODO:insert the class
			ContentValues cv = new ContentValues();
			String stuStr = "";
			if(students != null){
				int index = 0;
				for(index = 0; index < students.length; index++){
					Student s = students[index];
					stuStr = stuStr + s.getName() + ModelConfig.STUDENT_SEP;
					s.updateCount(context);
				}
				if(stuStr.endsWith(ModelConfig.STUDENT_SEP)){
					stuStr = stuStr.substring(0, stuStr.length()-ModelConfig.STUDENT_SEP.length());
				}
			}
			
			cv.put("id", md5);
			cv.put("startDate", date);
			cv.put("startTime", startTime);
			cv.put("grade", grade);
			cv.put("room", room);
			cv.put("money", money);
			cv.put("remark", remark);
			cv.put("students", stuStr);
			cv.put("startDateTime",dl);
			mDataBase.insert(ModelConfig.classTable, null, cv);
			
			Toast.makeText(context, "成功插入一条课时记录",
       		     Toast.LENGTH_SHORT).show();
		}
		print();
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
	}
}
