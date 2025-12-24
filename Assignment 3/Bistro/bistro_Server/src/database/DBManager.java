package database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBManager {
	private static HikariDataSource dataSource;
	private static String db_Url;
	private static String db_User;
	private static String db_Pass;
	private DBManager() {}
	
	
	/**
	 * initializes the connection pool with configuration loaded from dbDetails.properties
	 * METHOD MUST BE CALLED ONCE
	 */
	public static void init() {
		if (dataSource != null) return;
		
		dbLoadDetails();
		
		HikariConfig config = new HikariConfig();		
		
		config.setJdbcUrl(db_Url);
		config.setUsername(db_User);
		config.setPassword(db_Pass);
		
		config.setMaximumPoolSize(10);
		config.setMinimumIdle(2);
		
		config.setConnectionTimeout(10_000);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(30 * 60_000);

        config.setPoolName("BistroPool");
	     
		dataSource = new HikariDataSource(config);
	}
	
	/**
	 * method to retrieve a JDBC connection from the connection pool
	 * THE CONNECTION MUST BE CLOSED AFTER USE TO RETURN IT TO THE POOL
	 * 
	 * @return a pooled connection ready for database use
	 * @throws SQLException when database access error occurs
	 * @throws IllegalStateException if the connection pool is not initialized 
	 */
	public static Connection getConnection() throws SQLException{
		if(dataSource ==null) 
			throw new IllegalStateException("DBManager not initialized");
		return dataSource.getConnection();			
	}
	
	
	/**
	 * method to shut down all data base connection pool
	 */
	public static void dbShutDown() {
		if(dataSource!=null) {
			dataSource.close();
			dataSource=null;
		}
	}
	
	/**
	 * method to load database url user and password from external file dbDetails.properties
	 */
	private static void dbLoadDetails() {
		Properties props = new Properties();
		boolean loaded = false;
		try {
			
			//try to read from external file
			try(InputStream external = new FileInputStream("dbDetails.properties")){
				props.load(external);
                System.out.println("Loaded EXTERNAL details file.");
                loaded = true;
			}catch(Exception e) {
				System.out.println("External file not found. Trying INTERNAL file...");
			}
			
			//try to read from a classpath file
			if(!loaded) {
				try(InputStream internal = DBManager.class.getClassLoader().getResourceAsStream("dbDetails.properties")){
					if(internal !=null) {
						props.load(internal);
						System.out.println("Loaded INTERNAL details file.");
						loaded=true;
					}
					
				}
			}
			
			//read values from file
			db_Url = props.getProperty("db_Url").trim();
            db_User = props.getProperty("db_User").trim();
            db_Pass = props.getProperty("db_Pass").trim();
            
            //validate that got the details from file
            if (db_Url == null || db_Url.isBlank())
                throw new RuntimeException("Missing property: db_Url");
            if (db_User == null || db_User.isBlank())
                throw new RuntimeException("Missing property: db_User");
            if (db_Pass == null)
                throw new RuntimeException("Missing property: db_Pass");

            //debugging 
            System.out.println(db_Url);
            System.out.println(db_User);                                    						
				
		}catch(Exception e) {
			throw new RuntimeException("Failed to load database configuration.", e);
		}
	}
	
	
}
