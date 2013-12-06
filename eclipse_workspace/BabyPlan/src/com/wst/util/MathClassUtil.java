package com.wst.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wst.model.MathClass;
import com.wst.model.Student;
import com.wst.util.ModelConfig.TIME_LEVEL;

public class MathClassUtil {
	
	/**
	 * 获取各个时段的总课时数目
	 * @param context   上下文环境
	 */
	public static int getTotalClassCount(Context context,TIME_LEVEL level){
		MathClass[] classes = MathClassUtil.getAllClasses(context,level);
		
		if(classes == null){
			return 0;
		}else{
			return classes.length;
		}
	}
	
	public static int getTotalMoney(Context context, TIME_LEVEL level){
        MathClass[] classes = MathClassUtil.getAllClasses(context,level);
		int sum = 0;
		if(classes == null){
			return 0;
		}else{
			int index = 0;
			for(index = 0; index < classes.length; index++){
				sum += classes[index].getMoney();
			}
			return sum;
		}
	}
	
	/**
	 * 获取课时费用
	 * @param context   上下文环境
	 * @param nextLesson   True:获取下节课的课时费，False:获取当前的课时费
	 */
	public static int getCurrentMoneyPerClass(Context context, boolean nextLesson){
		int classesCount = MathClassUtil.getTotalClassCount(context,TIME_LEVEL.Month);
		
		if(nextLesson){
			classesCount++;
		}
		
		int index = 0;
		int curCount = ModelConfig.deltaCount;
		while(index < ModelConfig.moneys.length && curCount < classesCount){
			index++;
			curCount += ModelConfig.deltaCount;
		}
		
		if(index >= ModelConfig.moneys.length){
			return ModelConfig.moneys[ModelConfig.moneys.length - 1];
		}else{
			return ModelConfig.moneys[index];
		}
	}
	
	/**
	 * 按阶段获取所有课时信息，全局函数
	 */
	@SuppressLint("SimpleDateFormat")
	public static MathClass[] getAllClasses(Context context, TIME_LEVEL level){
		MathClass[] classes;
		DBHelper db = new DBHelper(context,ModelConfig.dbName);
		SQLiteDatabase mDataBase = null;
		mDataBase = db.getWritableDatabase();
		Cursor cursor;
		
		if(level == TIME_LEVEL.Total){
			cursor = mDataBase.rawQuery("select * from mathclass order by startDateTime desc", new String[]{});
		}else{
			
			Calendar calendar = Calendar.getInstance();  
	        calendar.setTimeInMillis(System.currentTimeMillis());
	        int year = calendar.get(Calendar.YEAR);
	        
			int month = calendar.get(Calendar.MONTH);
			month++;
			
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(Calendar.MINUTE);
			int second = calendar.get(Calendar.SECOND);
			int weekday = calendar.get(Calendar.DAY_OF_WEEK);
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			StringBuffer eStr = new StringBuffer();
			long sdl = 0;
			long edl = 0;
			
			eStr.append(String.format("%d-%02d-%02d %02d:%02d:%02d",   
	                year,   
	                month,  
	                day,hour,minute,second));
			try{
				Date ed = df.parse(eStr.toString());
				edl = ed.getTime();
			}catch(Exception e){
				e.printStackTrace();
			} 
			
			try{
				if(level == TIME_LEVEL.Month){
					sdl = edl - 1000*(second+minute*60+hour*3600+(day-1)*24*3600);
				}else if(level == TIME_LEVEL.Today){
					sdl = edl - 1000*(second+minute*60+hour*3600);
				}else{
					//周日
					if(weekday == 1){
						sdl = edl - 1000*(second+minute*60+hour*3600+6*24*3600);
					}else{
						//周一是2
						sdl = edl - 1000*(second+minute*60+hour*3600+(weekday-2)*24*3600);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			} 
			cursor = mDataBase.rawQuery("select * from mathclass where startDateTime >= ? and startDateTime <= ? order by startDateTime desc", new String[]{String.valueOf(sdl),String.valueOf(edl)});
		}
		
		
		if(cursor.getCount() == 0){
			if(!cursor.isClosed()){
				cursor.close();
			}
			mDataBase.close();
			return null;
		}
		classes = new MathClass[cursor.getCount()];
		cursor.moveToFirst();
		int index = 0;
		do{
			String cdate = cursor.getString(cursor.getColumnIndex("startDate"));
			String ctime = cursor.getString(cursor.getColumnIndex("startTime"));
			String cgrade = cursor.getString(cursor.getColumnIndex("grade"));
			String cremark = cursor.getString(cursor.getColumnIndex("remark"));
			String croom = cursor.getString(cursor.getColumnIndex("room"));
			String cstudents = cursor.getString(cursor.getColumnIndex("students"));
			int cmoney = cursor.getInt(cursor.getColumnIndex("money"));
			Student[] cstu;
			if(!cstudents.equals("")){
				String[] stuNames = cstudents.split(ModelConfig.STUDENT_SEP);
				cstu = new Student[stuNames.length];
				int i = 0;
				for(i = 0; i < stuNames.length; i++){
					Student s = Student.getStudentByName(context, stuNames[i]);
					cstu[i] = s;
					if(s != null){
						//s.print();
					}
				}
			}else{
				cstu = null;
			}
			MathClass math = new MathClass(cdate, ctime, cgrade, croom, cmoney,cstu, cremark);
			classes[index++] = math;
		}while(cursor.moveToNext());
		if(!cursor.isClosed()){
			cursor.close();
		}
		mDataBase.close();
		return classes;
	}
}
