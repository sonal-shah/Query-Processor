package edu.stevens.cs562.queryprocessor;

public class Util {
	
	public static int countMatches(String str1,String str2){
		int count = str1.length() - str1.replace(str2, "").length();
		return count;
	}

	public static boolean isExistInGroupingAttribute(String[] ga_arr, String col){
		
		for(String ga : ga_arr){
			if(ga.equalsIgnoreCase(col))
				return true;
		}
		return false;
	}
}
