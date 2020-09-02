package op;

import au.com.bytecode.opencsv.CSVWriter;
import constants.Rank;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * use a list of NCBI gi or accession and get the corresponding lineages
 * @author benjamin linard
 */

@CommandLine.Command(
        name = "IdentifiersToLineages",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Build VSearch(sintax)-formatted lineages from a list of taxids."
)

public class TaxidToVSearchSintax implements Callable<Integer> {

    @CommandLine.Option(names = {"-l", "--taxid_list"}, required = true, paramLabel = "file", description = "An input file containing a list of taxids, one per line.")
    private File in=null;

//    @CommandLine.Option(names = {"-f", "--fasta_with_taxid"}, required = true, paramLabel = "file", description = "A input fasta, with pattern 'taxid=[0-9]+' present in the headers.")
//    private File in=null;

    @CommandLine.Option(names = {"-o", "--out"}, paramLabel = "file", description = "Output results to file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new op.TaxidToVSearchSintax()).execute(args);
        System.exit(exitCode);
    }


    public Integer call() throws Exception {

        BufferedReader br = Files.newBufferedReader(in.toPath());
        ArrayList<Integer> taxids = new ArrayList<>();
        String line = null;
        int lineCount =1;
        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.length() < 1) {
                continue;
            }
            try {
                Integer taxid = Integer.parseInt(t);
                taxids.add(taxid);
            } catch (NumberFormatException ex) {
                System.out.println("Line "+lineCount+" cannot be parsed as an integer.");
                System.exit(1);
            }
            lineCount++;
        }
        br.close();

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
        NCBITaxonomyTree taxonomy = new NCBITaxonomyTree(c);
        Statement stat = c.createStatement();

        HashMap<Integer, String> results = new LinkedHashMap<>(); //map(taxid)=completeTaxonomy

        for (int i = 0; i < taxids.size(); i++) {
            int taxid = taxids.get(i);
            if (!results.containsKey(taxid)) {

                LinkedHashMap<String, Integer> rankedLineage = taxonomy.getRankedLineage(taxid);
                if (rankedLineage == null) {
                    System.out.println("taxid="+taxid+" is absent from NCBI Taxonomy.");
                    continue;
                }
                //build sintax string, for ranks={domain, kingdom, phylum, class, order, family, genus, species}
                StringBuilder sb = new StringBuilder(";tax=");
                boolean first = true;
                for (Iterator<String> it = rankedLineage.keySet().iterator(); it.hasNext();) {
                    String scientificName = it.next();
                    int rank = rankedLineage.get(scientificName);
                    String s = null;
                    if (rank == Rank.KINGDOM) { s="k:"+scientificName; }
                    else if (rank == Rank.PHYLUM) { s="p:"+scientificName; }
                    else if (rank == Rank.CLASS) { s="c:"+scientificName; }
                    else if (rank == Rank.ORDER) { s="o:"+scientificName; }
                    else if (rank == Rank.FAMILY) { s="f:"+scientificName; }
                    else if (rank == Rank.GENUS) { s="g:"+scientificName; }
                    else if (rank == Rank.SPECIES) { s="s:"+scientificName.replaceAll(" ","_"); }
                    else { continue; }
                    if (!first) { sb.append(','); }
                    sb.append(s);
                    first = false;
                }
                if (sb.length()==5) {
                    System.out.println("taxid="+taxid+" found in NCBI taxonomy but no rank matching Vsearch(sintax) requirements.");
                    continue;
                }
                results.put(taxid, sb.toString());
            }
        }

        Writer w;
        if (out != null) {
            w = Files.newBufferedWriter(out.toPath());
        } else {
            w = new OutputStreamWriter(System.out);
        }

        CSVWriter cw = new CSVWriter(w, '\t');
        for (Iterator<Integer> iterator = results.keySet().iterator(); iterator.hasNext(); ) {
            Integer currentId = iterator.next();
            String sintaxString = results.get(currentId);
            String[] infos = new String[2];
            infos[0] = currentId.toString();
            infos[1] = results.get(currentId);
            cw.writeNext(infos);
        }
        cw.flush();
        cw.close();

        return 0;
    }

}
