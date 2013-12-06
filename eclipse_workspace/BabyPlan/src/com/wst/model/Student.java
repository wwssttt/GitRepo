package com.wst.model;

import java.lang.String;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.wst.util.Encript;
import com.wst.util.ModelConfig;
import com.wst.util.DBHelper;

public class Student {
	private String name;
	private String sex;
	private String grade;
	private int age;
	private int count;
	private String remark;
	/*
	public Student(){
		name = ModelConfig.sname;
		sex=ModelConfig.ssex;
		age = ModelConfig.sage;
		grade = ModelConfig.sgrade;
		count = 0;
	}**/
	
	/**
	 * 构造函数
	 * @param name   学生姓名
	 * @param sex    学生性别
	 * @param age    学生年龄
	 * @param grade  学生年级
	 * @param count  学生课时总数
	 * @param remark 备注
	 */
	public Student(String name, String sex, int age, String grade,int count, String remark){
		this.name = name;
		this.sex = sex;
		this.age = age;
		this.grade = grade;
        this.count = count;
        this.remark = remark;
	}
	
	/**
	 * 构造函数
	 * @param name   学生姓名
	 * @param sex    学生性别
	 * @param age    学生年龄
	 * @param grade  学生年级
	 * @param remark 备注
	 */
	public Student(String name, String sex, int age, String grade,String remark){
		this.name = name;
		this.sex = sex;
		this.age = age;
		this.grade = grade;
        this.count = 0;
        this.remark = remark;
	}
	
	/**
	 * 构造函数
	 * @param name   学生姓名
	 * @param sex    学生性别
	 * @param age    学生年龄
	 * @param grade  学生年级
	 */
	public Student(String name, String sex, int age, String grade){
		this.name = name;
		this.sex = sex;
		this.age = age;
		this.grade = grade;
        this.count = 0;
        this.remark = ModelConfig.cremark;
	}
	
	/**
	 * 构造函数
	 * @param name   学生姓名
	 */
	public Student(String name){
		this.name = name;
		this.sex = ModelConfig.ssex;
		this.age = ModelConfig.sage;
		this.grade = ModelConfig.sgrade;
        this.count = 0;
        this.remark = ModelConfig.cremark;
	}
	
	/**
	 * 获取学生姓名
	 */
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name; 
	}
	
	public String getGrade(){
		return grade;
	}
	
	public void setGrade(String grade){
		this.grade = grade; 
	}
	
	public String getSex(){
		return sex;
	}
	
	public void setSex(String sex){
		this.sex = sex;
	}
	
	public int getAge(){
		return age;
	}
	
	public void setAge(int age){
		this.age = age;
	}
	
	public int getCount(Context context){
		this.count = getTotalCount(context);
		return count;
	}
	
	/**
	 * 获取学生课时总数
	 * @param context   上下文环境
	 */
	private int getTotalCount(Context context){
		//TODO:select sth from db
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		String md5 = Encript.md5(this.name);
		Cursor cursor = mDataBase.rawQuery("select * from students where id = ?", new String[]{md5});
		if(cursor.getCount() == 0){
			cursor.close();
			mDataBase.close();
			return count;
		}
		cursor.moveToFirst();
		int scount = cursor.getInt(cursor.getColumnIndex("count"));
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
		return scount;
	}
	
	public void setCount(int count, Context context){
		updateCount(context,count,1);
	}
	
	public String getRemark(){
		return remark;
	}
	
	public void setRemark(String remark){
		this.remark = remark;
	}
	
	public void print(){
	  
	}
	
	/**
	 * 将学生信息插入到数据库中
	 * @param context   上下文环境
	 */
	public void insertToDB(Context context){
		//TODO:save me to DB
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		String md5 = Encript.md5(this.name);
		Cursor cursor = mDataBase.rawQuery("select * from students where id = ?", new String[]{md5});
		if(cursor.getCount() > 0){
			//TODO:upgrade the student
			Toast.makeText(context, "学生插入失败，暂不提供学生信息更新",
		   		     Toast.LENGTH_SHORT).show();
		}else{
			//TODO:insert the student
			ContentValues cv = new ContentValues();
			cv.put("id", md5);
			cv.put("name", name);
			cv.put("sex", sex);
			cv.put("grade", grade);
			cv.put("age", age);
			cv.put("count", count);
			cv.put("remark", remark);
			mDataBase.insert(ModelConfig.studentTable, null, cv);
			Toast.makeText(context, "成功插入一条学生记录",
       		     Toast.LENGTH_SHORT).show();
		}
		print();
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
	}
	
	/**
	 * 更新学生课时总数
	 * @param context   上下文环境
	 * @param step      累加步长，即增加多少节课
	 * @param flag      更新标记，flag=0则累加，否则则直接设置为step
	 */
	public void updateCount(Context context, int step, int flag){
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		String md5 = Encript.md5(this.name);
		Cursor cursor = mDataBase.rawQuery("select * from students where id = ?", new String[]{md5});
		if(cursor.getCount() == 0){
		}
		
		if(flag == 0){
			this.count = getCount(context) + step;
		}else{
			this.count = step;
		}
		
		ContentValues cv = new ContentValues();
		cv.put("count", this.count);
		mDataBase.update(ModelConfig.studentTable, cv, "id = ?", new String[]{md5});
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
	}
	
	/**
	 * 更新学生课时总数
	 * @param context   上下文环境
	 * @param step      累加步长，即增加多少节课
	 * @param flag      更新标记，flag=0则累加，否则则直接设置为step
	 */
	public void updateCount(Context context){
		updateCount(context,1,0);
	}
	
	/**
	 * 按姓名获取学生信息
	 * @param context   上下文环境
	 * @param stuName   学生姓名
	 */
	public static Student getStudentByName(Context context,String stuname){
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		String stuMD5 = Encript.md5(stuname);
		Cursor cursor = mDataBase.rawQuery("select * from students where id = ?", new String[]{stuMD5});
		if(cursor.getCount() == 0){
			if(!cursor.isClosed()){
				cursor.close();
			}
			mDataBase.close();
			return null;
		}
		cursor.moveToFirst();
		String sname = cursor.getString(cursor.getColumnIndex("name"));
		String ssex = cursor.getString(cursor.getColumnIndex("sex"));
		String sgrade = cursor.getString(cursor.getColumnIndex("grade"));
		String sremark = cursor.getString(cursor.getColumnIndex("remark"));
		int sage = cursor.getInt(cursor.getColumnIndex("age"));
		int scount = cursor.getInt(cursor.getColumnIndex("count"));
		Student s = new Student(sname,ssex,sage,sgrade,scount,sremark);
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
		return s;
	}
	
	/**
	 * 获取所有学生信息，全局函数
	 */
	public static Student[] getAllStudents(Context context){
		Student[] students;
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		Cursor cursor = mDataBase.rawQuery("select * from students", new String[]{});
		if(cursor.getCount() == 0){
			if(!cursor.isClosed()){
				cursor.close();
			}
			mDataBase.close();
			return null;
		}
		students = new Student[cursor.getCount()];
		cursor.moveToFirst();
		int index = 0;
		do{
			String sname = cursor.getString(cursor.getColumnIndex("name"));
			String ssex = cursor.getString(cursor.getColumnIndex("sex"));
			String sgrade = cursor.getString(cursor.getColumnIndex("grade"));
			String sremark = cursor.getString(cursor.getColumnIndex("remark"));
			int sage = cursor.getInt(cursor.getColumnIndex("age"));
			int scount = cursor.getInt(cursor.getColumnIndex("count"));
			Student s = new Student(sname,ssex,sage,sgrade,scount,sremark);
			students[index++] = s;
		}while(cursor.moveToNext());
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
		return students;
	}
}
