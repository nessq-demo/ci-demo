package es.us.isa.restest.util;


import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

//mysqladmin.exe -u root password NEWPASS


public class DBConnector {
	
	  	  
	  public Connection conn = null;
	  public PreparedStatement stmt = null;
	  public Properties CONFIG=null;
	  public Properties PRODUCT_CONFIG=null;
	  public final String REPORT_SERVER_URL="http://192.168.15.227:8081";


	/** The insert execution status query. */
	String insertEndpointLatest = "INSERT INTO ResTest_API_Test "
			+ "(`product`, `test_endpoint`, `last_update`) "
			+ "VALUES (?,?,NOW())";

	/** The insert execution status query. */
	String insertExecutionResult = "INSERT INTO ResTest_API_Report "
			+ "(`product`,`environment`,`total`, `pass`, `fail`, `pass_percentage`,`test_file`,`report_link`,`last_update`) "
			+ "VALUES (?,?,?,?,?,?,?,?,NOW())";
		

		
		public DBConnector(){
			CONFIG=initConfig();
			PRODUCT_CONFIG=initProductConfig();
		}

	public static void main(String[] args)
	{
		DBConnector db = new DBConnector();
		db.conn= db.initConnection(true);
		//db.insertExecutionStatus("/cns/api/notification");
		db.insertReportTable(18,18,0,"jutiter_release_v1.0");
		db.closeConnection();



	}
		
		public Properties initConfig(){
			if(CONFIG==null){
				CONFIG=new Properties();
			  try{
					// loading properties file
					FileInputStream fs = new FileInputStream("src/main/resources/config.properties");
					CONFIG.load(fs);
					}catch(Exception e){
						System.out.println("Error loading properties");
				}
			}
			return CONFIG;
			//System.out.println(CONFIG.getProperty("environment"));
		}

	public Properties initProductConfig(){
			if(PRODUCT_CONFIG==null){
				PRODUCT_CONFIG=new Properties();
				try{
					// loading properties file
					FileInputStream fs = new FileInputStream("src/test/resources/zycus_new/zycus.properties");
					PRODUCT_CONFIG.load(fs);
				}catch(Exception e){
					System.out.println("Error loading product config properties");
				}
			}
			return PRODUCT_CONFIG;
		}
		
		public boolean dbUpdateFlag(){
			if(CONFIG.get("dbUpdateFlag").equals("true"))
				return true;
			else
				return false;
		}
	  
	  public Connection initConnection(boolean connect){
		  if(connect){
		  String host=CONFIG.getProperty("restest.database.host");
		  String port=CONFIG.getProperty("restest.database.port");
		  String url = "jdbc:mysql://"+host+":"+port+"/";
		  String driver = CONFIG.getProperty("restest.database.driver");
		  String dbName = CONFIG.getProperty("restest.database.name");
		  String username=CONFIG.getProperty("restest.database.username");
		  String password=CONFIG.getProperty("restest.database.password");
		     try {
				Class.forName(driver);
				conn = DriverManager.getConnection(url+dbName,username,password);
				System.out.println(conn.isClosed());
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		  }
			 return conn;
	  }
	  
	  public void closeConnection(){
		  try {
				if((conn!=null) && (!conn.isClosed())){
					  conn.close();
				  }
			} catch (SQLException e) {
				e.printStackTrace();
			}
	  }
	  

	  public void insertExecutionStatus(String endpoint){
			if(CONFIG.getProperty("dbUpdateFlag").equalsIgnoreCase("true")) {
				if (conn != null) {
					System.out.println("Executing insertEndpointLatest: " + endpoint);
					try {
						String product = PRODUCT_CONFIG.getProperty("product.name.in.api.uri");
						stmt = conn.prepareStatement(insertEndpointLatest);
						stmt.setString(1, product);
						stmt.setString(2, endpoint);
						stmt.executeUpdate();
					} catch (SQLException e) {
						e.printStackTrace();
					} finally {
						try {
							stmt.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
	  }

	public void insertReportTable(int total,int pass,int fail,String file){
		if(CONFIG.getProperty("dbUpdateFlag").equalsIgnoreCase("true")) {
			if (conn != null) {
				System.out.println("Executing insertReportTable: " + file);
				try {
					String[] data = getReportLink();
					Float percentage = Float.valueOf((pass*100/total));
					//String product = PRODUCT_CONFIG.getProperty("product.name.in.api.uri");
					stmt = conn.prepareStatement(insertExecutionResult);
					stmt.setString(1, data[0]);
					stmt.setString(2, data[1]);
					stmt.setInt(3, total);
					stmt.setInt(4, pass);
					stmt.setInt(5, fail);
					stmt.setFloat(6, percentage);
					stmt.setString(7, file);
					stmt.setString(8, data[2]);
					stmt.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					try {
						stmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}



	public void updateSessionQueueStatus(String runId,String status){
		if(conn!=null){
		 System.out.println("Executing updateSessionQueueStatus");
		  try {
			String statusUpdateQuery = "update session_queue set status = ? where run_id = ?";
			stmt=conn.prepareStatement(statusUpdateQuery);
			stmt.setString(1, status);
			stmt.setString(2, runId);
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		  finally{
			  try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		  }
		}
	}

	public String[] getReportLink(){
		    String url = "";
		    String product = "";
		    String counter = "";
		    String environment = "";
			String[] returnVal = new String[3];
			String pipelineName = System.getProperty("$GO_PIPELINE_NAME");
			if(pipelineName==null) {
				product = PRODUCT_CONFIG.getProperty("product.name.in.api.uri");
				url = REPORT_SERVER_URL + "/" +product+"/"+product+"-RESTEST-QCVM/"+System.currentTimeMillis()+"/zycus_new/";
				returnVal[0]=product;
				returnVal[1]="QCVM";
				returnVal[2]=url;
			}
			else {
				product = pipelineName.split("RESTEST")[0].replace("-","").trim();
				counter = System.getProperty("$GO_PIPELINE_COUNTER");
				environment = pipelineName.split("RESTEST")[1].replace("-","").trim();
				returnVal[0]=product;
				returnVal[1]=environment;
				url = REPORT_SERVER_URL + "/" + product + "/" + pipelineName + "/" + counter + "/zycus_new/";
				returnVal[2]=url;
			}
			return returnVal;
	}

	  

	
}
