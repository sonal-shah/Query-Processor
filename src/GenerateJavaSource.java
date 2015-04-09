package edu.stevens.cs562.queryprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;



public class GenerateJavaSource {
	
	private static final String MF_STRUCTURE_ClASSNAME= "MfStructure";
	
	private static final String QUERY_PROCESSOR_CLASSNAME= "QP";
	private static final String QUERY_PROCESSOR_FILEPATH= "src/edu/stevens/cs562/queryprocessor/QP.java";
	private static final String QP_MFTABLE_FIELD = "mftable";
	private static final String PACKAGE_NAME  = "edu.stevens.cs562.queryprocessor";
	
	private static final String MF_STRUCTURE_FILEPATH= "src/edu/stevens/cs562/queryprocessor/MfStructure.java";
	
	
	public static boolean generateMfStructureClass(Map<String,String> mftableDataTypeMap){
		
		File file = new File(MF_STRUCTURE_FILEPATH);
		PrintWriter pw = null;
		try {
			pw =  new PrintWriter(file);
			pw.println("package "+PACKAGE_NAME+";");
			pw.println("public class "+MF_STRUCTURE_ClASSNAME+" {");
				
			Set<Entry<String,String>> entries = mftableDataTypeMap.entrySet();
			for (Entry<String,String> entry : entries) 
				pw.println(" public "+entry.getValue()+" "+entry.getKey()+";");
			
			pw.println("}");
			
		
		}catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}finally{
			if(pw!=null)
				pw.close();
		}
		
		return true;
	}
	
	
	public static boolean generateQPClass(Map<String,String> input){
		File file = new File(QUERY_PROCESSOR_FILEPATH);
		PrintWriter pw = null;
		try {
			pw =  new PrintWriter(file);
			pw.println("package "+PACKAGE_NAME+";");
			List<String> importList =new ArrayList<String>();
			importList.add(PACKAGE_NAME+"."+MF_STRUCTURE_ClASSNAME);
			
			//Add import declaration statement into the class
			addImportDeclaration(pw,importList);
			pw.println("public class "+QUERY_PROCESSOR_CLASSNAME+" {");
			
			//define array of type MfStructure class with size of 500.
			pw.println("private "+MF_STRUCTURE_ClASSNAME+"[] "+QP_MFTABLE_FIELD+" = new "+MF_STRUCTURE_ClASSNAME+"[500];");
			
			//Add main method in the QP class
			pw.println(" public static void main(String[] args){");
			pw.println("  System.out.println(\"QP class main()\");");
			
			pw.println(" }");
			pw.println("}");
			
		}catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}finally{
			if(pw!=null)
				pw.close();
		}
		
		return true;
	}
	
	public static void addImportDeclaration(PrintWriter pw, List<String> importList){
		for(String importStr : importList){
			pw.println("import "+importStr+";");
		}
		
	}
	


}
