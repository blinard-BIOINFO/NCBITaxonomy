package op;

import environment.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import graph.NCBITaxonomyTree;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * build a taxonomy delimitation file to target blast search to specific clades
 * @author benjamin linard
 */
public class GenerateTaxonomyDelimitationForBlast {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        if (args.length<1) {
            System.out.println("####################################################");
            System.out.println("## ARGS: taxonomic_id (integer) 'nucl'/'prot' (string)");
            System.out.println("####################################################");
        }
        
        try {
            if (args.length!=2) {
                System.out.println("Only two argument are authorized taxonomic_id \\(integer\\) 'nucl'/'prot' \\(string\\)");
                System.exit(1);
            }
            int taxid=Integer.parseInt(args[0]);
            
            //connection, loaded from local database_taxo.properties file
                File f=null;
                InputStream in =null;
                try {
                    f=new File(Environment.getExecutablePathWithoutFilename(GenerateTaxonomyDelimitationForBlast.class).getAbsolutePath()+File.separator+"database_taxo.properties");
                    in = new FileInputStream(f);
                } catch (FileNotFoundException ex) {
                    System.out.println("database properties file not found, should be in the same directory as the jar");
                    System.exit(1);
                    //p.load(ExtractSequenceHavingBlastHits.class.getResourceAsStream("database_taxo.properties"));
                }
                System.out.println("Connection Configuration loaded from '"+f.getAbsolutePath()+"'");
                
            
            
            Connection c= DBConnectionTest.connectNCBITaxonomy(in);
            NCBITaxonomyTree taxonomy=new NCBITaxonomyTree(c);
            
            //System.out.println(taxonomy.getOrganismLineage(7041));
            //System.out.println(taxonomy.getOrganismLineage(9606));
            
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
            sb.append(") AS t (taxid), gi_taxid_"+args[1]+" as gi "
                    + "where t.taxid=gi.taxid;");
            //System.out.println(sb.toString());
            
            Statement stat = c.createStatement();
            ResultSet res = stat.executeQuery(sb.toString());
            System.out.println("Writing GIs to temporary file...");
            File fTemp=new File("blast_GIs.temp");
            System.out.println("Cache file: "+fTemp.getAbsolutePath());
            Writer w=new BufferedWriter(new FileWriter(fTemp));
            while(res.next()) {
                w.append(res.getInt(1)+"\n");
            }
            w.close();
            System.out.println("File created. Use this list with blastdb_aliastool. ");
            res.close();
            stat.close();
            c.close();
            
        
            

            
            
        } catch (SQLException ex) {
            Logger.getLogger(GenerateTaxonomyDelimitationForBlast.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GenerateTaxonomyDelimitationForBlast.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
