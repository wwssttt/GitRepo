import java.io.*;
import java.util.*;

public class FileManager {
	public static ArrayList<String> searchDir(String dirPath){
		System.out.println("I am searching "+dirPath);
		ArrayList<String> fileArr = new ArrayList<String>();
		LinkedList<File> list = new LinkedList<File>();
		File dir;
		try{
			dir = new File(dirPath);
		}catch(Exception e){
			e.printStackTrace();
			System.out.println(dirPath + "is not found");
			return fileArr;
		}
        File file[] = dir.listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory())
              list.add(file[i]);
            else
              fileArr.add(file[i].getAbsolutePath());
        }
        File tmp;
        while (!list.isEmpty()) {
            tmp = (File) list.removeFirst();
            if (tmp.isDirectory()) {
                file = tmp.listFiles();
                if (file == null)
                    continue;
                for (int i = 0; i < file.length; i++) {
                    if (file[i].isDirectory())
                        list.add(file[i]);
                    else
                    	fileArr.add(file[i].getAbsolutePath());
                }
            } else {
            	fileArr.add(tmp.getAbsolutePath());
            }
        }
        return fileArr;
	}
	
    public static void delDir(String filepath){  
    	try{
    		File f = new File(filepath);//定义文件路径         
    	       if(f.exists() && f.isDirectory()){//判断是文件还是目录  
    	          if(f.listFiles().length == 0){//若目录下没有文件则直接删除  
    	             f.delete();  
    	           }else{//若有则把文件放进数组，并判断是否有下级目录  
    	              File delFile[] = f.listFiles();  
    	              int len = f.listFiles().length;  
    	              for(int j = 0; j < len; j++){  
    	                  if(delFile[j].isDirectory()){  
    	                     delDir(delFile[j].getAbsolutePath());//递归调用del方法并取得子目录路径  
    	                  }
 	                	  delFile[j].delete();//删除文件
    	              }  
    	           }  
    	       }     
    	}catch(Exception e){
    		e.printStackTrace();
    	} 
     }  
}
