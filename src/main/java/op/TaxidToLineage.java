package op;

import constants.Rank;
import database.ConnectionTools;
import graph.NCBITaxonomyTree;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * extract lineage from a taxid
 * @author benjamin linard
 */

@Command(
    name = "TaxidToLineage",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "Extract NCBI lineage from a NCBI taxid."
)

public class TaxidToLineage implements Callable<Integer> {

    @Option(names = {"-t", "--taxid"}, required = true, paramLabel = "int", description = "The taxonomic id.")
    private int taxid=9606;

    @Option(names = {"-r", "--ranks"}, description = "Add rank names to scientific names.")
    private boolean ranks=false;

    @Option(names = {"-f", "--format"}, paramLabel = "[1|2]", description = "Format used to output ranks:\n1 = 'Homo[Genus];sapiens[Species]'\n2 = 'Homo;sapiens' (line 1)\n     'genus;species' (line 2)")
    private int format=1;

    @Option(names = {"-o", "--out"}, description = "Output results in file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new TaxidToLineage()).execute(args);
        System.exit(exitCode);
    }


    public Integer call() throws Exception {
        Connection c=null;
        try {
            //NCBI DB connection
            if ( db == null ) {
                c = ConnectionTools.openConnection(null);
            } else {
                if ( ! ( db.exists() || db.canRead() )) {
                    System.out.println("File given via option -d do not exists or cannot be read.");
                    System.exit(1);
                }
                c = ConnectionTools.openConnection(new FileInputStream(db));
            }
            //Taxo tree
            NCBITaxonomyTree taxonomy=new NCBITaxonomyTree(c);
            //query
            System.out.println("Extracting lineage for "+taxid);
            LinkedHashMap<String, Integer> rankedLineage = taxonomy.getRankedLineage(taxid);
            if (rankedLineage==null) {
                System.out.println("no lineage found, taxid valid ?");
            }

            StringBuilder result = new StringBuilder();
            StringBuilder resultLine2 = null;
            if (format==2) {
                resultLine2 = new StringBuilder();
            }
            boolean first=true;
            for (String rank_scientific :rankedLineage.keySet()) {
                int rank_id=rankedLineage.get(rank_scientific);
                String rank_name= Rank.getRankString(rank_id);
                if (!first) { result.append(';'); }
                result.append(rank_scientific);
                if (ranks && (format==1) ) { //1 line rank output
                    result.append('[');
                    result.append(rank_name);
                    result.append(']');
                } else if (ranks && (format==2) ) {
                    if (!first) { resultLine2.append(';'); }
                    resultLine2.append(rank_name);
                }
                first=false;
            }

            //output to file
            if (out!=null) {
                if (!out.canWrite()) {
                    System.out.println("No write permission for "+out.getAbsolutePath());
                    System.exit(1);
                }
                BufferedWriter w = Files.newBufferedWriter(out.toPath());
                w.append(result.toString());
                if (resultLine2!=null) {
                    w.newLine();
                    w.append(resultLine2.toString());
                }
                w.close();
                System.out.println("Results in "+out.getAbsolutePath());
            //output to stdout
            } else {
                System.out.println(result.toString());
                if (resultLine2!=null) {
                    System.out.println(resultLine2.toString());
                }
            }

            if (c!=null) {
                c.close();
            }
            return 0;
        } catch (Exception ex) {
            Logger.getLogger(TaxidToLineage.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }
        
    }
}
