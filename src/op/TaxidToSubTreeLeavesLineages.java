package op;

import constants.Rank;
import database.ConnectionTools;
import graph.NCBITaxonomyTree;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * extract lineage from a taxid
 * @author benjamin linard
 */

@Command(
    name = "TaxidToSubTreeLeavesLineages",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "From a taxid, extract leves from subtree then lineages for all these leaves."
)

public class TaxidToSubTreeLeavesLineages implements Callable<Integer> {

    @Option(names = {"-t", "--taxid"}, required = true, paramLabel = "int", description = "The taxonomic id.")
    private int taxid=9606;

    @Option(names = {"-r", "--ranks"}, description = "Add rank names to scientific names (if --format=1).")
    private boolean ranks=false;

    @Option(names = {"-f", "--format"}, paramLabel = "[1|2]", description = "Format used to output ranks:\n1 = include 'no_rank' levels\n2 = only defined ranks")
    private int format=1;

    @Option(names = {"-o", "--out"}, description = "Output results in file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;

    //@Option(names = {"-i", "--identified-ranks"}, description = "Output results in file instead of stdout.")
    //private File identified=null;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new TaxidToSubTreeLeavesLineages()).execute(args);
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
            //check if taxid valid
            if (taxonomy.getScientificName(taxid)==null) {
                System.out.println("taxid not found in database !");
                System.exit(1);
            }
            //retrieve subtree leaves
            System.out.println("Extracting leaves for "+taxid);
            ArrayList<Integer> leavesTaxids = taxonomy.getSubtreeLeavesTaxids(taxid);
            System.out.println("#leaves found: "+leavesTaxids.size());

            //prepare output
            BufferedWriter w = null;
            if (out != null) {
//                if (!out.canWrite()) {
//                    System.out.println("No write permission for " + out.getAbsolutePath());
//                    System.exit(1);
//                }
                w = Files.newBufferedWriter(out.toPath());
            } else {
                w = new BufferedWriter(new OutputStreamWriter(System.out));
            }

            StringBuilder sb = null;

            if (format==2) {

                //header
                sb = new StringBuilder();
                boolean first = true;
                for (String rank : Rank.getRanks()) {
                    if (!first) {
                        sb.append(';');
                    }
                    sb.append(rank);
                    first = false;
                }
                w.append(sb.toString());
                w.newLine();
                //lines
                for (int leafTaxid : leavesTaxids) {
                    sb = new StringBuilder();
                    //create as many column for this line as ranks
                    String[] observed_ranks = new String[Rank.getRanks().length];
                    Arrays.fill(observed_ranks, "");
                    LinkedHashMap<String, Integer> rankedLineage = taxonomy.getRankedLineage(leafTaxid);
                    for (String rank_value : rankedLineage.keySet()) {
                        int rank_id = rankedLineage.get(rank_value);
                        observed_ranks[rank_id] = rank_value;
                    }
                    first = true;
                    for (String elt : observed_ranks) {
                        if (!first) {
                            sb.append(';');
                        }
                        sb.append(elt);
                        first = false;
                    }
                    w.append(sb.toString());
                    w.newLine();
                }
            } else {
                for (int leafTaxid : leavesTaxids) {
                    sb = new StringBuilder();
                    LinkedHashMap<String, Integer> rankedLineage = taxonomy.getRankedLineage(leafTaxid);
                    buildAllRanksLineOutputs(sb, rankedLineage, ranks);
                    w.append(sb);
                    w.newLine();
                }
            }

            //close writer
            if (out!=null) {
                System.out.println("Results in "+out.getAbsolutePath());
            }
            w.close();

            //close connection
            if (c!=null) {
                c.close();
            }
            return 0;
        } catch (Exception ex) {
            Logger.getLogger(TaxidToSubTreeLeavesLineages.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }
        
    }


    private void buildAllRanksLineOutputs(StringBuilder result, LinkedHashMap<String, Integer> rankedLineage, boolean ranks) {
        boolean first = true;
        for (String rank_scientific : rankedLineage.keySet()) {
            int rank_id = rankedLineage.get(rank_scientific);
            String rank_name = Rank.getRankString(rank_id);
            if (!first) {
                result.append(';');
            }
            result.append(rank_scientific);
            if (ranks) { //1 line rank output
                result.append('[');
                result.append(rank_name);
                result.append(']');
            }
            first = false;
        }
    }

}
