package op;

import database.ConnectionTools;
import environment.Environment;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static database.ConnectionTools.getInfoDriver;

/**
 * Opens connection to NCBI sql database
 * @author benjamin linard
 */

@CommandLine.Command(
        name = "GenerateTaxonomyDelimitationForBlast",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Build a taxonomy delimitation file to target blast search to specific clades (using blastdb_aliastool utility)."
)

public class DBConnectionTest implements Callable<Integer> {

    @CommandLine.Option(names = {"-e", "--extended"}, description = "Output extended information related to the SQL driver and the established connection.")
    private boolean extended=false;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;


    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new op.DBConnectionTest()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        try {
            System.out.println("Testing postgres database connection...");
            Connection c;
            if (db == null) {
                c = ConnectionTools.openConnection(null);
            } else {
                if (!(db.exists() || db.canRead())) {
                    System.out.println("File given via option -d do not exists or cannot be read.");
                    System.exit(1);
                }
                c = ConnectionTools.openConnection(new FileInputStream(db));
            }
            DatabaseMetaData metaData = c.getMetaData();
            System.out.print("connected on: "+metaData.getURL()+"\n");
            System.out.print("with user: "+metaData.getUserName()+"\n");
            if (extended) {
                FileInputStream is;
                if (db == null) {
                    File dir = Environment.getExecutablePathWithoutFilename(ConnectionTools.class);
                    File prop = new File(dir.getAbsolutePath()+File.separator+"database.properties");
                    if ( ! (prop.exists() || prop.canRead() )) {
                        System.out.println("Attempted to open DB connection using 'database.properties in local directory, but file does not exist or cannot be read.");
                        System.exit(1);
                    }
                    is = new FileInputStream(prop);
                } else {
                    is = new FileInputStream(db);
                }
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
                String url = props.getProperty("jdbc.url");
                Enumeration<Driver> drivers1 = DriverManager.getDrivers();
                while (drivers1.hasMoreElements()) {
                    Driver d=drivers1.nextElement();
                    ConnectionTools.getInfoDriver(d, url);
                }
            }
            c.close();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(DBConnectionTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Connection attempt failed.");
            System.out.println("Does your machine have a connection to ctag-postgres server ?");
            System.out.println("Is postgres session active on server?");
            return 1;
        }

        return 0;
    }
    
}
