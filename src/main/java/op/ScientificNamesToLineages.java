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
 * use a list of scientific names and attempt to match them to a lineage
 * @author benjamin linard
 */



@CommandLine.Command(
        name = "ScientificNamesToLineages",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Extract NCBI lineages from a list of scientific names (e.g., \"Genus species\")."
)

public class ScientificNamesToLineages implements Callable<Integer> {

    @CommandLine.Option(names = {"-i", "--in"}, required = true, paramLabel = "file", description = "A file containing a list of scientific names (e.g., \"Genus species\"), one per line.")
    private File in=null;

    @CommandLine.Option(names = {"-o", "--out"}, paramLabel = "file", description = "Output results to file instead of stdout.")
    private File out=null;

    @CommandLine.Option(names = {"-d", "--db"}, paramLabel = "file", description = "Properties file defining DB connection (if not used, a file named 'database.properties will be searched in local directory).")
    private File db=null;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new ScientificNamesToLineages()).execute(args);
        System.exit(exitCode);
    }


    public Integer call() throws Exception {
        
        try {

            if (in==null) {
                System.out.println("Please provide a list of scientific names with option -i.");
                System.exit(1);
            }

            BufferedReader br = Files.newBufferedReader(in.toPath());
            ArrayList<String> names = new ArrayList<>();
            String line=null;

            while ((line=br.readLine())!=null) {
                String t = line.trim();
                if (t.length() < 1) {
                    continue;
                }
                names.add(t);
            }
            br.close();

            if (names.size() < 1) {
                System.out.println("No names could be parsed from the input file.");
                System.exit(1);
            } else {
                System.out.println(names.size() +" names parsed from input file.");
            }

            Connection c;
            if ( db == null ) {
                c = ConnectionTools.openConnection(null);
            } else {
                if ( ! ( db.exists() || db.canRead() )) {
                    System.out.println("File given via option -d do not exists or cannot be read.");
                    System.exit(1);
                }
                c = ConnectionTools.openConnection(new FileInputStream(db));
            }
            NCBITaxonomyTree taxonomy=new NCBITaxonomyTree(c);
            Statement stat=c.createStatement();
            
            HashMap<String,Integer> queries=new LinkedHashMap<>(); //map()identifier)=taxid
            HashMap<Integer,String> clades=new LinkedHashMap<>(); //map(taxid)=completeTaxonomy
            
            for (int i=0; i<names.size(); i++) {
                String currentName = names.get(i);
                //System.out.println(currentId);
                StringBuilder sb=new StringBuilder( "select taxid from names where ");
                sb.append("name_txt ILIKE '");
                sb.append(currentName);
                sb.append("' and name_class='scientific name'");
                String finalQuery=sb.toString();
                //System.out.println(finalQuery);
                ResultSet rs=stat.executeQuery(finalQuery);
                boolean success=false;
                int taxid=-1;
                while (rs.next()) { taxid=rs.getInt("taxid"); success=true; };
                if (!success) {
                    System.out.println("No taxid found for '"+currentName+"'");
                } else {
                    queries.put(currentName,taxid);
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
            
            CSVWriter cw=new CSVWriter(w, '\t');
            for (Iterator<String> iterator = queries.keySet().iterator(); iterator.hasNext();) {
                String currentId = iterator.next();
                int currentTaxid=queries.get(currentId);
                String[] infos=new String[4];
                infos[0]=currentId;
                infos[1]=Integer.toString(currentTaxid);
                String[] levs=clades.get(currentTaxid).split(";");
                infos[2]=levs[levs.length-1];
                infos[3]=clades.get(currentTaxid);
                cw.writeNext(infos);
            }
            cw.flush();
            cw.close();
            
        } catch (IOException | SQLException ex) {
            Logger.getLogger(ScientificNamesToLineages.class.getName()).log(Level.SEVERE, null, ex);
        }

        return 0;
        
    }  
    
    
}
