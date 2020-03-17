package op;

import au.com.bytecode.opencsv.CSVWriter;
import graph.NCBITaxonomyTree;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * use a list of NCBI gi or accession and
 * @author benjamin linard
 */
public class ScientificNamesToLineages {

 /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        if (args.length<1) {
            System.out.println("####################################################");
            System.out.println("## ARGS: path_to_input_file  ");
            System.out.println("## The file must contain a list of scientific_names,");
            System.out.println("## one per line.");
            System.out.println("## Note that upper/lower cases will be ignored");
            System.out.println("## ('Homo sapiens' == 'homo sapiens)");
            System.out.println("####################################################");
        }
        
        
        try {

            
            FileReader fr = new FileReader(new File(args[0]));
            BufferedReader br =new BufferedReader(fr);
            ArrayList<String> names=new ArrayList<String>();
            String line=null;

            
            //connection, loaded from local database_taxo.properties file
//            File f=null;
//            InputStream in =null;
//            try {
//                f=new File(Environment.getExecutablePathWithoutFilename(GenerateTaxonomyDelimitationForBlast.class).getAbsolutePath()+File.separator+"database_taxo.properties");
//                in = new FileInputStream(f);
//            } catch (FileNotFoundException ex) {
//                System.out.println("database properties file not found, should be in the same directory as the jar");
//                System.exit(1);
//                //p.load(ExtractSequenceHavingBlastHits.class.getResourceAsStream("database_taxo.properties"));
//            }
//            System.out.println("Connection Configuration loaded from '"+f.getAbsolutePath()+"'");
            
            Connection c= DBConnectionTest.connectNCBITaxonomy(null);
            NCBITaxonomyTree taxonomy=new NCBITaxonomyTree(c);
            Statement stat=c.createStatement();
            
            HashMap<String,Integer> queries=new LinkedHashMap<>(); //map()identifier)=taxid
            HashMap<Integer,String> clades=new LinkedHashMap<>(); //map(taxid)=completeTaxonomy
            
            for (int i=0; i<names.size();i++) {
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

            FileWriter fw =new FileWriter(new File("output.tsv"));
            
            CSVWriter cw=new CSVWriter(fw, '\t');
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
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ScientificNamesToLineages.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ScientificNamesToLineages.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ScientificNamesToLineages.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }  
    
    
}
