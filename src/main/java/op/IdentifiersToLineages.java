package op;

import au.com.bytecode.opencsv.CSVWriter;
import graph.NCBITaxonomyTree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
public class IdentifiersToLineages {

 /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        if (args.length<1) {
            System.out.println("####################################################");
            System.out.println("## ARGS: path_to_input_file  ");
            System.out.println("## The file must contain a list of gi or accessions");
            System.out.println("## one per line.");
            System.out.println("## Note that versions (XXXXXX.1) are not considered");
            System.out.println("####################################################");
        }
        
        
        try {
            
            //to set if the file contains gi or accession ids
            boolean isGi=false;
            boolean isAccession=false;
            
            
            FileReader fr = new FileReader(new File(args[0]));
            BufferedReader br =new BufferedReader(fr);
            ArrayList<String> ids=new ArrayList<String>();
            String line=null;
            
            while ((line=br.readLine())!=null) {
                
                if (!isGi && !isAccession) {
                    try {
                        Integer.parseInt(line.trim());
                        isGi=true;
                        System.out.println("Ids recognized as gi numbers (integer only).");
                    } catch (NumberFormatException ex) {
                        isAccession=true;
                        System.out.println("Ids recognized as accessions (not only integer).");
                    }
                }
                //note that we don't consider the accession version
                if (line.contains("\\.")) {
                    ids.add(line.trim().substring(0, line.indexOf('.')));
                } else {
                    ids.add(line.trim());
                }
            }
            
            
            
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
            
            for (int i=0; i<ids.size();i++) {
                String currentId = ids.get(i);
                //System.out.println(currentId);
                StringBuilder sb=new StringBuilder( "select taxid from accession2taxid where ");
                if (isGi) {sb.append("gi='");} else {sb.append("accession='");}
                sb.append(currentId+"'");
                String finalQuery=sb.toString();
                //System.out.println(finalQuery);
                ResultSet rs=stat.executeQuery(finalQuery);
                boolean success=false;
                int taxid=-1;
                while (rs.next()) { taxid=rs.getInt("taxid"); success=true; };
                if (!success) {
                    System.out.println("No taxid found for '"+currentId+"'");
                } else {
                    queries.put(currentId,taxid);
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
            Logger.getLogger(IdentifiersToLineages.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(IdentifiersToLineages.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(IdentifiersToLineages.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }  
    
    
}
