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

	private static final String MF_STRUCTURE_ClASSNAME = "MfStructure";

	private static final String QUERY_PROCESSOR_CLASSNAME = "QP";
	private static final String QUERY_PROCESSOR_FILEPATH = "src/edu/stevens/cs562/queryprocessor/QP.java";
	private static final String QP_MFTABLE_FIELD = "mftable";
	private static final String PACKAGE_NAME = "edu.stevens.cs562.queryprocessor";

	private static final String MF_STRUCTURE_FILEPATH = "src/edu/stevens/cs562/queryprocessor/MfStructure.java";

	public static boolean generateMfStructureClass(
			Map<String, String> mftableDataTypeMap) {

		File file = new File(MF_STRUCTURE_FILEPATH);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			pw.println("package " + PACKAGE_NAME + ";");
			pw.println("public class " + MF_STRUCTURE_ClASSNAME + " {");

			Set<Entry<String, String>> entries = mftableDataTypeMap.entrySet();
			for (Entry<String, String> entry : entries)
				pw.println(" public " + entry.getValue() + " " + entry.getKey()
						+ ";");

			pw.println("}");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pw != null)
				pw.close();
		}

		return true;
	}

	public static boolean generateQPClass(Map<String, String> input,
			Map<String, String> mfDataTypeMap) {
		File file = new File(QUERY_PROCESSOR_FILEPATH);
		PrintWriter pw = null;
		String[] gas = input.get(Constants.GROUPING_ATTRIBUTES).split(",");
		List<String> cols = new ArrayList<String>();
		boolean is_gr_0_aggr_exist = false;

		Set<Entry<String, String>> entries = mfDataTypeMap.entrySet();
		for (Entry<String, String> entry : entries) {
			String k = entry.getKey();
			if (k.startsWith("sum") || k.startsWith("count")
					|| k.startsWith("min") || k.startsWith("max")
					|| k.startsWith("avg")) {
				String str = "";
				int count = Util.countMatches(k, "_");
				System.out.println("count :: " + count);

				if (count == 1) {
					is_gr_0_aggr_exist = true;
					str = k.substring(k.indexOf('_') + 1);
				} else if (count > 1) {
					str = k.substring(k.indexOf('_') + 1, k.lastIndexOf('_'));
				}

				System.out.println("************* " + str);
				if (!cols.contains(str))
					cols.add(str);
			}
		}

		try {
			pw = new PrintWriter(file);
			pw.println("package " + PACKAGE_NAME + ";");
			List<String> importList = new ArrayList<String>();
			importList.add(PACKAGE_NAME + "." + MF_STRUCTURE_ClASSNAME);
			importList.add(PACKAGE_NAME + "." + "DatabaseOperations");
			importList.add("java.sql.ResultSet");
			importList.add("java.sql.SQLException");

			// Add import declaration statement into the class
			addImportDeclaration(pw, importList);
			pw.println("public class " + QUERY_PROCESSOR_CLASSNAME + " {");

			// define array of type MfStructure class with size of 500.
			pw.println("private " + MF_STRUCTURE_ClASSNAME + "[] "
					+ QP_MFTABLE_FIELD + " = new " + MF_STRUCTURE_ClASSNAME
					+ "[500];");
			pw.println("private int counter;");

			// algorithm method
			pw.println(" public void algorithm(){");
			pw.println(" DatabaseOperations dbop = new DatabaseOperations();");
			pw.println(" String query = \"select * from sales\";");
			pw.println(" String newquery=\"\";");
			String whereCond = input.get(Constants.WHERE_CLAUSE);
			if (!whereCond.isEmpty() && whereCond.trim().length() > 0)
				pw.println(" newquery = query +\" where "
						+ input.get(Constants.WHERE_CLAUSE) + "\";");
			else
				pw.println(" newquery = query;");

			pw.println(" if(dbop != null) {");
			pw.println(" ResultSet rs = dbop.executeSaleQuery(newquery);");

			pw.println("try {");

			pw.println("  while( rs.next()) {");
			pw.println("   int index = 0; ");
			pw.println("   boolean isexist = false;");

			String gacondition = "";
			for (int ix = 0; ix < gas.length; ix++) {

				if (mfDataTypeMap.containsKey(gas[ix])) {
					String dataType = mfDataTypeMap.get(gas[ix]);

					if (dataType.equals("String")) {
						pw.println(" String " + gas[ix] + " = rs.getString(\""
								+ gas[ix] + "\");");
						gacondition = gacondition + "mftable[index]." + gas[ix]
								+ ".equals(" + gas[ix] + ")";
					} else if (dataType.equals("int")) {
						pw.println(" int " + gas[ix] + " = rs.getInt(\""
								+ gas[ix] + "\");");
						gacondition = gacondition + "mftable[index]." + gas[ix]
								+ " == " + gas[ix];
					}

					if (ix < gas.length - 1)
						gacondition = gacondition + " && ";
				}

			}

			System.out.println("gacondition ::: " + gacondition);
			if (is_gr_0_aggr_exist) {
				for (String col : cols) {
					if(!Util.isExistInGroupingAttribute(gas,col))
					/*if (mfDataTypeMap.containsKey("sum_" + col)
							|| mfDataTypeMap.containsKey("max_" + col)
							|| mfDataTypeMap.containsKey("min_" + col)
							|| mfDataTypeMap.containsKey("avg_" + col))*/
						pw.println("int " + col + " = rs.getInt(\"" + col
								+ "\");");
				}
			}
			pw.println("while (index < counter) { ");
			pw.println("if (" + gacondition + "){");

			if (is_gr_0_aggr_exist) {
				for (String col : cols) {
					System.out.println("sum_::" + "sum_" + col);
					if (mfDataTypeMap.containsKey("sum_" + col)) {
						pw.println("mftable[index].sum_" + col
								+ " = mftable[index].sum_" + col + " + " + col
								+ ";");
					}
					if (mfDataTypeMap.containsKey("count_" + col)) {
						pw.println("mftable[index].count_" + col
								+ " = mftable[index].count_" + col + " + " + 1
								+ ";");
					}

					if (mfDataTypeMap.containsKey("max_" + col)) {
						pw.println("if (mftable[index].max_" + col + " < "
								+ col + ")");
						pw.println("mftable[index].max_" + col + " = " + col
								+ ";");

					}

					if (mfDataTypeMap.containsKey("min_" + col)) {
						pw.println("if (mftable[index].min_" + col + " > "
								+ col + ")");
						pw.println("mftable[index].min_" + col + " = " + col
								+ ";");

					}

					if (mfDataTypeMap.containsKey("avg_" + col)) {

						pw.println("mftable[index].avg_" + col
								+ " = mftable[index].sum_" + col
								+ "/mftable[index].count_" + col + ";");

					}

				}
			}
			pw.println(" isexist = true ;");
			pw.println("break;");

			pw.println("}"); // end if gacondition
			pw.println("index++;");
			pw.println("}");// end while

			pw.println("if (!isexist) {");
			pw.println(" mftable[counter] = new " + MF_STRUCTURE_ClASSNAME
					+ "();");
			for (int ix = 0; ix < gas.length; ix++) {
				pw.println("mftable[counter]." + gas[ix] + " = " + gas[ix]
						+ ";");
			}

			if (is_gr_0_aggr_exist) {

				for (String col : cols) {
					if (mfDataTypeMap.containsKey("count_" + col))
						pw.println("mftable[index].count_" + col + " = 1;");
					if ((mfDataTypeMap.containsKey("sum_" + col)))
						pw.println("mftable[index].sum_" + col + " = " + col
								+ ";");
					if ((mfDataTypeMap.containsKey("max_" + col)))
						pw.println("mftable[index].max_" + col + " = " + col
								+ ";");
					if ((mfDataTypeMap.containsKey("min_" + col)))
						pw.println("mftable[index].min_" + col + " = " + col
								+ ";");
					if ((mfDataTypeMap.containsKey("avg_" + col)))
						pw.println("mftable[index].avg_" + col
								+ " = mftable[index].sum_" + col
								+ "/mftable[index].count_" + col + ";");
				}
			}
			pw.println("counter++;");

			pw.println("}"); // end if exist

			pw.println("}"); // while rs

			// Start looping for grouping variables

			String str_gv = input.get(Constants.NO_OF_GRP_VAR);
			String[] condition_vect = input.get(Constants.CONDITION_VECT)
					.split(",");

			System.out.println("number of grouping variables :::: "
					+ str_gv.isEmpty() + " " + str_gv.length());

			if (!str_gv.isEmpty() && str_gv.length() > 0) {
				int no_of_gv = Integer.parseInt(str_gv);

				for (int i = 0; i < no_of_gv; i++) {

					pw.println("rs.beforeFirst();");
					pw.println("  while( rs.next()) {");
					pw.println("   int index = 0; ");

					for (int ix = 0; ix < gas.length; ix++) {

						if (mfDataTypeMap.containsKey(gas[ix])) {
							String dataType = mfDataTypeMap.get(gas[ix]);

							if (dataType.equals("String"))
								pw.println(" String " + gas[ix]
										+ " = rs.getString(\"" + gas[ix]
										+ "\");");
							else if (dataType.equals("int"))
								pw.println(" int " + gas[ix]
										+ " = rs.getInt(\"" + gas[ix] + "\");");
						}
					}

					for (String col : cols) {
						/*String col_name = col + "_" + (i + 1);
						if (mfDataTypeMap.containsKey("sum_" + col_name)
								|| mfDataTypeMap.containsKey("max_" + col_name)
								|| mfDataTypeMap.containsKey("min_" + col_name)
								|| mfDataTypeMap.containsKey("avg_" + col_name))*/
						if(!Util.isExistInGroupingAttribute(gas,col))
							pw.println("int " + col + " = rs.getInt(\"" + col
									+ "\");");
					}

					pw.println("while (index < counter) { ");
					pw.println("if (" + condition_vect[i] + "){");

					for (String col : cols) {

						String col_name = col + "_" + (i + 1);

						System.out.println("sum_" + col_name);

						if (mfDataTypeMap.containsKey("sum_" + col_name)) {
							pw.println("mftable[index].sum_" + col_name
									+ " = mftable[index].sum_" + col_name
									+ " + " + col + ";");
						}
						if (mfDataTypeMap.containsKey("count_" + col_name)) {
							pw.println("mftable[index].count_" + col_name
									+ " = mftable[index].count_" + col_name
									+ " + " + 1 + ";");
						}

						if (mfDataTypeMap.containsKey("max_" + col_name)) {
							pw.println("if (mftable[index].max_" + col_name
									+ " < " + col + ")");
							pw.println("mftable[index].max_" + col_name + " = "
									+ col + ";");

						}

						if (mfDataTypeMap.containsKey("min_" + col_name)) {
							pw.println("if (mftable[index].min_" + col_name
									+ " > " + col + ")");
							pw.println("mftable[index].min_" + col_name + " = "
									+ col + ";");

						}

						if (mfDataTypeMap.containsKey("avg_" + col_name)) {

							pw.println("mftable[index].avg_" + col_name
									+ " = mftable[index].sum_" + col_name
									+ "/mftable[index].count_" + col_name + ";");

						}

					}

					pw.println("}"); // end if condition_vect
					pw.println("index++;");
					pw.println("}");// end while

					pw.println("}"); // end while rs

				}

			}

			pw.println("}"); // try end

			pw.println("catch (SQLException e) {");
			pw.println(" e.printStackTrace();");
			pw.println("}"); // catch end

			pw.println("}"); // if end
			pw.println("}"); // method end;

			String[] displayCols = input.get(Constants.SELECTION_ATTRIBUTES)
					.split(",");
			String disStr = "";
			String headerStr = "";
			// Display result function.
			pw.println(" public void displayResult(){ ");

			for (int ix = 0; ix < displayCols.length; ix++) {

				headerStr = headerStr + displayCols[ix];

				/*
				 * if(displayCols[ix].startsWith("avg")){ String subStr =
				 * displayCols[ix].substring(displayCols[ix].indexOf('_')+1);
				 * disStr =
				 * disStr+"mftable[index].sum_"+subStr+"/"+"mftable[index].count_"
				 * +subStr; }else
				 */
				if (displayCols[ix].startsWith("mftable"))
					disStr = disStr + displayCols[ix];
				else
					disStr = disStr + "mftable[index]." + displayCols[ix];

				if (ix < displayCols.length - 1) {
					disStr = disStr + "+\"\t|\t\"+";
					headerStr = headerStr + "\t|\t";
				}

			}

			pw.println("System.out.println(\"" + headerStr + "\");");
			pw.println("System.out.println(\"----------------------------------------------------------------\");");
			pw.println("for (int index = 0; index < counter; index++) {");
			String havingCondition = input.get(Constants.HAVING_CLAUSE);

			if (!havingCondition.isEmpty() && havingCondition.length() > 0) {
				havingCondition = havingCondition.replace("sum",
						"mftable[index].sum");
				havingCondition = havingCondition.replace("count",
						"mftable[index].count");
				havingCondition = havingCondition.replace("max",
						"mftable[index].max");
				havingCondition = havingCondition.replace("min",
						"mftable[index].min");
				havingCondition = havingCondition.replace("avg",
						"mftable[index].avg");
				pw.println("if(" + havingCondition + ") {");
			}

			System.out.println(havingCondition);
			pw.println("System.out.println(" + disStr + ");");

			if (!havingCondition.isEmpty() && havingCondition.length() > 0)
				pw.println("}");

			pw.println("}");
			pw.println("}");
			// Add main method in the QP class
			pw.println(" public static void main(String[] args){");
			pw.println("  System.out.println(\"QP class main()\");");
			pw.println(QUERY_PROCESSOR_CLASSNAME + " qp = new "
					+ QUERY_PROCESSOR_CLASSNAME + "();");
			pw.println("qp.algorithm();");
			pw.println("qp.displayResult();");

			pw.println(" }");
			pw.println("}");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pw != null)
				pw.close();
		}

		return true;
	}

	public static void addImportDeclaration(PrintWriter pw,
			List<String> importList) {
		for (String importStr : importList) {
			pw.println("import " + importStr + ";");
		}

	}

}
