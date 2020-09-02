package op;

import constants.Rank;
import database.ConnectionTools;
import graph.NCBITaxonomyTree;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * use a list of NCBI gi or accession and get the corresponding lineages
 * @author benjamin linard
 */

@CommandLine.Command(
        name = "FastaTaxidToVSearchSintaxFasta",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Inject VSearch(sintax)-formatted lineages in a fasta where headers contain taxids matching the pattern [ ;,|:]taxid=[0-9]+[ ;,|:] (examples: ;taxid=1452; or |tqaxid=1452| ...)."
)

public class FastaTaxidToVSearchSintaxFasta implements Callable<Integer> {

    @CommandLine.Option(names = {"-f", "--fasta_with_taxid"}, required = true, paramLabel = "file", description = "A input fasta, with pattern '[ ;,|:]taxid=[0-9]+[ ;,|:]' present in the headers.")
    private File in=null;

    @CommandLine.Option(names = {"-o", "--out"}, paramLabel = "file", description = "Output results to file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new FastaTaxidToVSearchSintaxFasta()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {

        // load db
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
        Pattern pattern = Pattern.compile("[ ;,|:]taxid=([0-9]+)[ ;,|:]");

        // output
        Writer w;
        if (out != null) {
            w = Files.newBufferedWriter(out.toPath());
        } else {
            w = new OutputStreamWriter(System.out);
        }

        // read input
        BufferedReader br = Files.newBufferedReader(in.toPath());
        String line = null;
        int lineCount =1;
        boolean skip = false;
        while ((line = br.readLine()) != null) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() < 1) {
                continue;
            }
            // convert header
            if (trimmedLine.startsWith(">")) {
                skip = false;
                //match pattern
                Matcher matcher = pattern.matcher(trimmedLine);
                String taxidString = null;
                while (matcher.find()) {
                    taxidString = matcher.group(1);
                }
                if (taxidString==null) {
                    System.out.println("Line " + lineCount + " did not match expected pattern [ ;,|:]taxid=[0-9]+[ ;,|:] .");
                    System.exit(1);
                }
                Integer taxid = null;
                try {
                    taxid = Integer.parseInt(taxidString);
                } catch (NumberFormatException ex) {
                    System.out.println("Line " + lineCount + " cannot be parsed as an integer.");
                    System.exit(1);
                }
                //search taxonomy match
                LinkedHashMap<String, Integer> rankedLineage = taxonomy.getRankedLineage(taxid);
                if (rankedLineage == null) {
                    System.out.println("taxid="+taxid+" is absent from NCBI Taxonomy. Fasta skipped: "+line);
                    skip = true;
                    continue;
                }
                StringBuilder sb = new StringBuilder(trimmedLine.trim());
                StringBuilder sb2 = new StringBuilder(" ;tax=");
                //build sintax string, for ranks={domain, kingdom, phylum, class, order, family, genus, species}
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
                    if (!first) { sb2.append(','); }
                    sb2.append(s);
                    first = false;
                }
                if (sb2.length()==5) {
                    System.out.println("taxid="+taxid+" found in NCBI taxonomy but no rank matching Vsearch(sintax) requirements.");
                    continue;
                }
                //avoid consecutive ";" in fasta header
                if (sb.charAt(sb.length()-1)==';') {
                    w.append(sb.substring(0,sb.length()-1));
                } else {
                    w.append(sb.toString());
                }
                w.append(sb2.toString());
                w.append('\n');
            } else {
                // if sequence line just write without modifications
                if (!skip) {
                    w.append(line);
                    w.append('\n');
                }
            }
            lineCount++;
        }

        br.close();
        w.close();
        return 0;
    }

}
