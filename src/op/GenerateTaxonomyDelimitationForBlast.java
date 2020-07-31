package op;

import database.ConnectionTools;
import graph.NCBITaxonomyTree;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * build a taxonomy delimitation file to target blast search to specific clades
 * @author benjamin linard
 */

@CommandLine.Command(
        name = "GenerateTaxonomyDelimitationForBlast",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Build a taxonomy delimitation file to target blast search to specific clades (using blastdb_aliastool utility)."
)

public class GenerateTaxonomyDelimitationForBlast implements Callable<Integer> {

    @CommandLine.Option(names = {"-t", "--taxid"}, required = true, paramLabel = "int", description = "The taxonomic id.")
    private int taxid=9606;

    @CommandLine.Option(names = {"-a", "--amino"}, description = "If used, build the delimitation for a protein database (nucleotides by default).")
    private boolean amino=false;

    @CommandLine.Option(names = {"-o", "--out"}, paramLabel = "file", description = "Output results to file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;


    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new op.GenerateTaxonomyDelimitationForBlast()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        
        try {

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
            NCBITaxonomyTree taxonomy=new NCBITaxonomyTree(c);
            
            ArrayList<Integer> subTaxIds = taxonomy.getSubTreeTaxids(taxid);
            
            System.out.println("Fetching GIs from subtree "+taxid+" ("+taxonomy.getScientificName(taxid)+")...\n(can take >10 minutes for large clades)");
            StringBuilder sb=new StringBuilder( "select gi.gi " +
                                                "from (VALUES ");
            boolean first=true;
            for (int i=0;i<subTaxIds.size();i++) {
                if (first) {
                    sb.append("("+subTaxIds.get(i)+")");
                } else {
                    sb.append(",("+subTaxIds.get(i)+")");
                }
                first=false;
            }
            String type;
            if (amino) {
                type = "nucl";
            } else {
                type = "prot";
            }
            sb.append(") AS t (taxid), gi_taxid_"+type+" as gi "
                    + "where t.taxid=gi.taxid;");
            //System.out.println(sb.toString());
            
            Statement stat = c.createStatement();
            ResultSet res = stat.executeQuery(sb.toString());
            System.out.println("Output list of identifiers...");
            Writer w;
            if (out != null) {
                w = Files.newBufferedWriter(out.toPath());
            } else {
                w = new OutputStreamWriter(System.out);
            }
            while(res.next()) {
                w.append(res.getInt(1)+"\n");
            }
            w.close();
            System.out.println("List created. Use this list as an input of the tool blastdb_aliastool. ");
            res.close();
            stat.close();
            c.close();

            
        } catch (SQLException | IOException ex) {
            Logger.getLogger(GenerateTaxonomyDelimitationForBlast.class.getName()).log(Level.SEVERE, null, ex);
        }

        return 0;

    }
}
