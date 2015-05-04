package edu.stevens.cs562.queryprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {

	private Map<String, String> input;
	private static final String INPUT_FILE = "resources/input4.properties";
	private Map<String, String> mf_structure_datatype;
	private DatabaseOperations dbop;

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		Main m = new Main();
		m.input = new TreeMap<String, String>();

		// Take user input
		System.out.print("User input from console[Y/N] : ");
		if (sc.hasNext() && sc.nextLine().equalsIgnoreCase("y")) {
			m.getConsoleUserInput(sc);
		} else {
			System.out.println("Enter the input file path: ");
			String fpath = sc.nextLine();
			m.readInputPropertyFile(fpath);
		}

		m.initiateMfStructureDataTypeMap();
		// just for testing
		// m.printMap(m.mf_structure_datatype);
		try {
			m.generateMFStructureSource();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	public void initiateMfStructureDataTypeMap() {
		dbop = new DatabaseOperations();

		mf_structure_datatype = new TreeMap<String, String>();

		String[] gas = input.get(Constants.GROUPING_ATTRIBUTES).split(",");
		for (String gattr : gas) {

			String result = dbop.getColumnDataType(gattr);
			if (result.contains("character"))
				mf_structure_datatype.put(gattr, "String");
			else if (result.contains("integer"))
				mf_structure_datatype.put(gattr, "int");

		}

		String[] fvect_cols = input.get(Constants.F_VECT).split(",");
		for (String col : fvect_cols) {

			String substr = col.substring(0, 3);

			if (substr.equalsIgnoreCase("avg")) {
				String remstr = col.substring(3);

				if (!mf_structure_datatype.containsKey("sum" + remstr))
					mf_structure_datatype.put("sum" + remstr, "int");
				if (!mf_structure_datatype.containsKey("count" + remstr))
					mf_structure_datatype.put("count" + remstr, "int");
				if (!mf_structure_datatype.containsKey("avg" + remstr))
					mf_structure_datatype.put("avg" + remstr, "double");
			}
			else{
				if (!mf_structure_datatype.containsKey(col))
					mf_structure_datatype.put(col, "int");
			}
		}

	}

	public void getConsoleUserInput(Scanner sc) {

		System.out.print("Enter Selection Attributes Seperated by ',' :\n");
		String value = "";
		value = sc.nextLine();
		input.put(Constants.SELECTION_ATTRIBUTES, value);

		System.out.print("Enter Where Condtion (if any)\n");
		value = sc.nextLine();
		input.put(Constants.WHERE_CLAUSE, value);
		
		System.out.print("Enter Grouping Attributes Seperated by ',' :\n");
		value = sc.nextLine();
		input.put(Constants.GROUPING_ATTRIBUTES, value);

		System.out.print("Enter Number Of Grouping Variables:\n");
		value = sc.nextLine();
		input.put(Constants.NO_OF_GRP_VAR, value);

		System.out.print("Enter F Vect Seperated by ',' :\n");
		value = sc.nextLine();
		input.put(Constants.F_VECT, value);

		System.out.print("Enter Condition Vect Seperated by ',' :\n");
		value = sc.nextLine();
		input.put(Constants.CONDITION_VECT, value);

		System.out.print("Enter Having Condtion (if any)\n");
		value = sc.nextLine();
		
		// System.out.println("having contion :::: "+value+
		// " "+value.isEmpty()+" "+value.length());

		input.put(Constants.HAVING_CLAUSE, value);
		

	}

	public void readInputPropertyFile(String fpath) {
		Properties prop = new Properties();
		File file = new File(fpath);
		FileInputStream fi;
		try {
			fi = new FileInputStream(file);

			prop.load(fi);
			fi.close();
			Enumeration<Object> enumKeys = prop.keys();
			while (enumKeys.hasMoreElements()) {
				String key = (String) enumKeys.nextElement();
				String value = prop.getProperty(key);
				// System.out.println("Keys : "+key+" value: "+value);
				// put key value pair into a hashmap.
				input.put(key, value);
			}

		} catch (FileNotFoundException e) {
			System.out.println("Error in reading input file: "+e.getMessage());
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void printMap(Map mp) {
		Iterator it = mp.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			System.out.println(pair.getKey() + " = " + pair.getValue());
			// it.remove(); // avoids a ConcurrentModificationException
		}
	}

	public void generateMFStructureSource() {
		boolean isCreated = false;
		if (mf_structure_datatype != null)
			isCreated = GenerateJavaSource
					.generateMfStructureClass(mf_structure_datatype);
		
		 if(isCreated) GenerateJavaSource.generateQPClass(input,mf_structure_datatype);
		 
		// TODO
		//generateAlgorithm();

	}

/*	public void generateAlgorithm() {
		MfStructure[] mf = new MfStructure[500];
		String query = "select * from sales where ";
		Map<String,String> gamap = new HashMap<String,String>();
		int counter = 0;

		String[] gas = input.get(GROUPING_ATTRIBUTES).split(",");
		
		// for(String cond : cond_vect){
		String newquery = query + input.get(CONDITION_VECT);
		System.out.println(newquery);
		if (dbop != null) {
			ResultSet rs = dbop.executeSaleQuery(newquery);
			try {
				while (rs.next()) {
					int localcounter = 0;
					boolean isexist = false;

					String cust = rs.getString("cust");
					String prod = rs.getString("prod");
					int quant = rs.getInt("quant");

					while (localcounter < counter) {

						if (mf[localcounter].cust.equals(cust)
								&& mf[localcounter].prod.equals(prod)) {
							mf[localcounter].sum_quant = mf[localcounter].sum_quant
									+ quant;
							mf[localcounter].count_quant = mf[localcounter].count_quant + 1;
							if (mf[localcounter].max_quant < quant)
								mf[localcounter].max_quant = quant;
							isexist = true;
							break;
						}
						localcounter++;
					}

					if (!isexist) {
						mf[counter] = new MfStructure();
						mf[counter].cust = cust;
						mf[counter].prod = prod;
						mf[counter].sum_quant = quant;
						mf[counter].count_quant = 1;
						mf[counter].max_quant = quant;
						counter++;
					}

				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// }

		for (int index = 0; index < counter; index++)
			System.out.println(mf[index].cust + " " + mf[index].prod + " "
					+ mf[index].max_quant + " " + mf[index].sum_quant
					/ mf[index].count_quant);

	}*/

}
