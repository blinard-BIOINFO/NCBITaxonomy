package database;


import environment.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * open database connection and provide some basic tests
 * @author benjamin linard
 */
public class ConnectionTools {

    /**
     * open connection with parameters set in the file target by the system property 'database.properties'
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static Connection openConnection () throws SQLException, IOException{
        return ConnectionTools.getConnection(null);
    }

    /**
     * open connection with connection parameters read from inputstream (if null attempts to read from a file named 'database.properties' in local directory)
     * @param properties
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static Connection openConnection(InputStream properties) throws SQLException, IOException {
        if (properties == null) {
            File dir = Environment.getExecutablePathWithoutFilename(ConnectionTools.class);
            File prop = new File(dir.getAbsolutePath()+File.separator+"database.properties");
            if ( ! (prop.exists() || prop.canRead() )) {
                System.out.println("Attempted to open DB connection using 'database.properties in local directory, but file does not exist or cannot be read.");
                System.exit(1);
            }
            FileInputStream fis = new FileInputStream(prop);
            return ConnectionTools.getConnection(fis);
        } else {
            return ConnectionTools.getConnection(properties);
        }
    }

    /**
     * close the given connection
     * @param c
     * @throws SQLException
     */
    public static void closeConnection(Connection c) throws SQLException {
        c.close();
    }

    private static Connection getConnection(InputStream is) throws SQLException, IOException {
        Properties props = new Properties();
        if (is==null) {
            if (System.getProperty("database.properties")==null) {
                System.out.println("database.properties is not set !!!");
                System.exit(1);
            }
            InputStream in = new FileInputStream(new File(System.getProperty("database.properties")));
            props.load(in);
            in.close();
        } else {
            props.load(is);
        }

        String drivers = props.getProperty("jdbc.drivers");
        //System.out.println("Java driver loader Class :\n jdbc.drivers="+drivers);
        if (drivers != null) {
            System.setProperty("jdbc.drivers",drivers);
        }
        String url = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");
        /*Enumeration<Driver> drivers1 = DriverManager.getDrivers();
        while (drivers1.hasMoreElements()) {
            Driver d=drivers1.nextElement();
            getInfoDriver(d, url);
        }*/

        Connection c= DriverManager.getConnection(url,username,password);
        c.setAutoCommit(true);
        return c;

    }

    /**
     *Get informations about a driver.
     * @param driver
     * @param url
     */
    public static void getInfoDriver(Driver driver, String url) {
        int majorVersion = driver.getMajorVersion();
        int minorVersion = driver.getMinorVersion();
        DriverPropertyInfo[] props = null;
        try {
            props = driver.getPropertyInfo(url,null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("########################################################################\n"
                + "Driver class = "+driver.getClass()+
           " v"+majorVersion+"."+minorVersion+
           "\n########################################################################\n");

        if (props==null) {
            System.out.println("-->No properties associated to this driver.");
            return;
        }

        for(int i=0 ;i<props.length;i++) {
            DriverPropertyInfo prop = props[i];
            System.out.println("Prop name = "+prop.name);
            System.out.println("Prop description = "+prop.description);
            System.out.println("Prop value = "+prop.value);
            if(prop.choices!=null){
                for(int j=0;j<prop.choices.length;j++) {
                    System.out.println("prop choice "+j+" = "+prop.choices[j]);
                }
            }
        }

    }
}
