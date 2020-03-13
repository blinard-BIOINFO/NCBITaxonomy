package op;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import database.ConnectionTools;

/**
 * Opens connection to NCBI sql database
 * @author benjamin linard
 */
public class DBConnectionTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("Testing postgres database connection...");
            Connection connectNCBITaxonomy = connectNCBITaxonomy(null);
            DatabaseMetaData metaData = connectNCBITaxonomy.getMetaData();
            System.out.print("connected on: "+metaData.getURL()+"\n");
            System.out.print("with user: "+metaData.getUserName()+"\n");
            connectNCBITaxonomy.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBConnectionTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Connection attempt failed.");
            System.out.println("Does your machine have a connection to ctag-postgres server ?");
            System.out.println("Is postgres session active on server?");
        } catch (IOException ex) {
            Logger.getLogger(DBConnectionTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Connection attempt failed.");
            System.out.println("Does your machine have a connection to ctag-postgres server ?");
            System.out.println("Is postgres session active on server?");
        }
    }
    
    public static Connection connectNCBITaxonomy(InputStream is) throws SQLException, IOException {
        return ConnectionTools.openConnection(is);
    }
    
}
