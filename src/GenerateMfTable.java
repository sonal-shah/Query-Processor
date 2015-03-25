package edu.stevens.cs562.queryprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class GenerateMfTable {

	public static Class generateMfTableClass(String className,
			Map<String, Class<?>> properties) throws NotFoundException,CannotCompileException {
		
		ClassPool cpool = ClassPool.getDefault();
		CtClass cc = cpool.makeClass(className);
		
		for(Entry<String,Class<?>> entry : properties.entrySet()){
			CtClass type = resolveClass(entry.getValue());
			CtField newField = new CtField(type,entry.getKey(),cc);
			//newField.setModifiers(Modifier.PUBLIC);
			cc.addField(newField);
			
			//add getter method of the property.
			cc.addMethod(generateGetterMethod(newField, entry.getKey()));
			//add setter method of the property.
			cc.addMethod(generateSetterMethod(newField,entry.getKey()));
			
		}
		
		return cc.toClass();

	}
	
	public static CtMethod generateGetterMethod(CtField field,String argName) throws CannotCompileException{
		String getMethodName = "get" + (argName.substring(0,1).toUpperCase()) + argName.substring(1);
		return CtNewMethod.getter(getMethodName, field);
		
	}
	
	public static CtMethod generateSetterMethod(CtField field,String argName) throws CannotCompileException{
		
		String setMethodName = "set" + (argName.substring(0,1).toUpperCase()) + argName.substring(1);
	
		return CtNewMethod.setter(setMethodName, field);
		
	}
	
	public static CtClass resolveClass(Class c) throws NotFoundException{
		ClassPool pool = ClassPool.getDefault();
		return pool.get(c.getName());
		
	}
	
	public static void main(String[] args){
		
		//This Map will be provided by the module, which is handing user inputs and generates this map.
		Map<String,Class<?>> properties = new HashMap<String,Class<?>>();
		properties.put("customer", String.class);
		properties.put("product",String.class);
		properties.put("1_sum_quant", Integer.class);
				
		
		try{
			Class MfTable = GenerateMfTable.generateMfTableClass("MfTable", properties);
			Object[] object = new Object[500];
			object[0] = MfTable.newInstance();
			MfTable.getMethod("setCustomer",String.class).invoke(object[0], "Sonal");
			
			//print the result of getCustomer() type     
			System.out.println("result from reflection :"+(String)MfTable.getMethod("getCustomer").invoke(object[0]));
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		
  		//Other approach to create mf-structure: create the list of maps, where map holds all fields of mf-table.
		
		//Below Map created by the module handling user input : keys in the maps will be 2+4 user input fields 
		Map<String,Class<?>> map = new HashMap<String,Class<?>>();
		//check the type of field from information_schema only if its name doesnot start with a digit e.g. 1_sum_quant, 
		//if the type is varchar put string.class in the value field otherwise Integer.class 
		
		map.put("cust", String.class);
		map.put("1_sum_quant", Integer.class);
		map.put("1_avg_quant", Integer.class);
		map.put("2_sum_quant", Integer.class);
		map.put("3_sum_quant", Integer.class);
		map.put("3_avg_quant", Integer.class);
		
		//Map keys will be the name of the mf-table columns and value of type object.
		List<Map<String,Object>> mf_structure = new ArrayList<Map<String,Object>>();
		
		Map<String,Object> row1_mftable = new HashMap<String,Object>();
		
		for(Entry<String,Class<?>> entry: map.entrySet()) {
			
			if(entry.getValue().getSimpleName().equals("String"))
				row1_mftable.put(entry.getKey(), new String("Sonal")); // Here new String() statement will be replaced by the resultSet.getString()
			
			else if(entry.getValue().getSimpleName().equals("Integer"))
				row1_mftable.put(entry.getKey(), new Integer(10));	// Here new String() statement will be replaced by the resultSet.getInt()
			
		}
		
		mf_structure.add(row1_mftable);
		
		for(Map<String,Object> m: mf_structure){
			Set<String> keys = m.keySet();
			for(String key:keys)
				System.out.println(key+": "+m.get(key));
		}
		
		
	}

}
