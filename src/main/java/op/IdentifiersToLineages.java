package op;

import au.com.bytecode.opencsv.CSVWriter;
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
        description = "Extract NCBI lineages from a list of identifiers (gi or accession)."
)

public class IdentifiersToLineages implements Callable<Integer> {

    @CommandLine.Option(names = {"-i", "--in"}, required = true, paramLabel = "file", description = "A file containing a list of NCBI gi or accession identifiers, one per line. Note that accession versions (e.g. XXXXXX.1) are ignored.")
    private File in=null;

    @CommandLine.Option(names = {"-o", "--out"}, paramLabel = "file", description = "Output results to file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;


    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new op.IdentifiersToLineages()).execute(args);
        System.exit(exitCode);
    }


    public Integer call() throws Exception {

        //to set if the file contains gi or accession ids
        boolean isGi = false;
        boolean isAccession = false;


        BufferedReader br = Files.newBufferedReader(in.toPath());
        ArrayList<String> ids = new ArrayList<>();
        String line = null;

        while ((line = br.readLine()) != null) {

            String t = line.trim();
            if (t.length() < 1) {
                continue;
            }
            if (!isGi && !isAccession) {
                try {
                    Integer.parseInt(t);
                    isGi = true;
                    System.out.println("Ids recognized as gi numbers (integer only).");
                } catch (NumberFormatException ex) {
                    isAccession = true;
                    System.out.println("Ids recognized as accessions (not only integer).");
                }
            }
            //note that we don't consider the accession version
            if (t.contains("\\.")) {
                ids.add(t.substring(0, t.indexOf('.')));
            } else {
                ids.add(t);
            }
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

        HashMap<String, Integer> queries = new LinkedHashMap<>(); //map()identifier)=taxid
        HashMap<Integer, String> clades = new LinkedHashMap<>(); //map(taxid)=completeTaxonomy

        for (int i = 0; i < ids.size(); i++) {
            String currentId = ids.get(i);
            //System.out.println(currentId);
            StringBuilder sb = new StringBuilder("select taxid from accession2taxid where ");
            if (isGi) {
                sb.append("gi='");
            } else {
                sb.append("accession='");
            }
            sb.append(currentId + "'");
            String finalQuery = sb.toString();
            //System.out.println(finalQuery);
            ResultSet rs = stat.executeQuery(finalQuery);
            boolean success = false;
            int taxid = -1;
            while (rs.next()) {
                taxid = rs.getInt("taxid");
                success = true;
            }
            ;
            if (!success) {
                System.out.println("No taxid found for '" + currentId + "'");
            } else {
                queries.put(currentId, taxid);
                if (!clades.containsKey(taxid)) {
                    clades.put(taxid, taxonomy.getStringLineage(taxid));
                }
            }

        }

        Writer w;
        if (out != null) {
            w = Files.newBufferedWriter(out.toPath());
        } else {
            w = new OutputStreamWriter(System.out);
        }

        CSVWriter cw = new CSVWriter(w, '\t');
        for (Iterator<String> iterator = queries.keySet().iterator(); iterator.hasNext(); ) {
            String currentId = iterator.next();
            int currentTaxid = queries.get(currentId);
            String[] infos = new String[4];
            infos[0] = currentId;
            infos[1] = Integer.toString(currentTaxid);
            String[] levs = clades.get(currentTaxid).split(";");
            infos[2] = levs[levs.length - 1];
            infos[3] = clades.get(currentTaxid);
            cw.writeNext(infos);
        }
        cw.flush();
        cw.close();

        return 0;
    }

}
