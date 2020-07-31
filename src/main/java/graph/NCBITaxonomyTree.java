package graph;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import constants.NameClass;
import constants.Rank;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.util.TreeUtils;

/**
 * load the NCBI tree from the Taxonomy database at instanciation and provides an object from which most common
 * operations can be done (querying taxonomy with species names, gi, getting lineages...)
 * @author benjamin linard
 */
public class NCBITaxonomyTree {
    
    HashMap<Integer,TaxoVertex> vertices=null;
    DelegateTree<TaxoVertex,TaxoEdge> tree=null;

    //by default, whole taxonomy is loaded
    //TODO, focus data retrieval on a list of sections, this require to load edges 1st, filter them per division,
    // then load names
    int division = -1;

    /**
     *
     * @param c
     * @throws SQLException
     */
    public NCBITaxonomyTree(Connection c) throws SQLException {
        
        //instanciations
        vertices= new HashMap<>();
        Statement stat = c.createStatement();
        tree= new DelegateTree<>();
        
        System.out.println("Loading NCBI Taxonomy...");
        System.out.println("Loading nodes...");
        
        //load all taxid and names with the name table
        ResultSet res=stat.executeQuery("select * from names order by taxid");
        int previousTaxId=-1;
        String previousNameTxt=null;
        String previousUniqueName=null;
        String previousNameClass=null;
        TaxoVertex currentVertex=null;
        while (res.next()) {
            int currentTaxId=res.getInt("taxid");
            if (currentVertex==null) {
                currentVertex=new TaxoVertex(currentTaxId);
            } //very first iteration
            if ( (currentTaxId!=previousTaxId) && (previousTaxId!=-1) ) {
                vertices.put(previousTaxId, currentVertex);
                //start the new vertex with current line
                currentVertex=new TaxoVertex(currentTaxId);
                currentVertex.addName(  NameClass.getClass(res.getString("name_class")),
                                        res.getString("name_txt"),
                                        res.getString("unique_name"));
            } else {
                currentVertex.addName(  NameClass.getClass(res.getString("name_class")),
                                        res.getString("name_txt"),
                                        res.getString("unique_name"));
            }
            
            previousTaxId=currentTaxId;
            previousNameTxt=res.getString("name_txt");
            previousUniqueName=res.getString("unique_name");
            previousNameClass=res.getString("name_class");
        }
        //last entry
        vertices.put(previousTaxId, currentVertex);
        res.close();
        
        //load edges with the nodes table and create tree
        System.out.println("Loading edges and ranks...");
        HashMap<Integer,ArrayList<Integer>> edges=new HashMap<>();
        HashMap<Integer,ArrayList<Integer>> edgesRank=new HashMap<>();
        HashMap<Integer,ArrayList<Integer>> edgesDivision=new HashMap<>();
        res=stat.executeQuery("select tax_id,parent_tax_id,rank,division_id from nodes");
        while (res.next()) {
            //root case
            if ( (res.getInt("tax_id")==1) && (res.getInt("tax_id")==res.getInt("parent_tax_id")) ) {
                tree.setRoot(vertices.get(1));
                continue;
            }
            //other nodes
            int child=res.getInt("tax_id");
            int parent=res.getInt("parent_tax_id");
            int division=res.getInt("division_id");
            int rank=Rank.getRank(res.getString("rank"));
            if (!edges.containsKey(parent))
                edges.put(parent, new ArrayList<Integer>());
            if (!edgesRank.containsKey(parent))
                edgesRank.put(parent, new ArrayList<Integer>());
            if (!edgesDivision.containsKey(parent))
                edgesDivision.put(parent, new ArrayList<Integer>());
            edges.get(parent).add(child);
            edgesRank.get(parent).add(rank);
            edgesDivision.get(parent).add(division);
        }
        System.out.println("Taxonomy data loaded!\n # nodes:"+vertices.size()+"\n # edges:"+edges.size());
        res.close();
        stat.close();
        System.gc();
        
        System.out.println("Building taxonomy tree...");
        int currentDepth=0;
        ArrayList<Integer> taxidsFromPreviousDepth=new ArrayList<>(); //contains the axids branched at a specific depth
        ArrayList<Integer> taxidsAddedInCurrentDepth=new ArrayList<>();
        taxidsFromPreviousDepth.add(1);
        while (true) {
            
            for (int i=0;i<taxidsFromPreviousDepth.size();i++) { //for all nodes of previous depth
                int currentParent=taxidsFromPreviousDepth.get(i);
                //System.out.println("currentparent "+currentParent);
                //System.out.println("children "+edges.get(currentParent));
                if (edges.get(currentParent)==null) { continue;} //we arrive on a leaf
                for (int j=0;j<edges.get(currentParent).size();j++) { //branch their nodes in the current depth
                    //System.out.println("add edge: "+currentParent+" -> "+edges.get(currentParent).get(j));
                    tree.addEdge(   new TaxoEdge(edgesRank.get(currentParent).get(j), edgesDivision.get(currentParent).get(j)),
                                    vertices.get(currentParent),
                                    vertices.get(edges.get(currentParent).get(j))
                    );
//                    if (edges.get(currentParent).get(j)==9606) {
//                        System.out.print("    Humans are there...\n");
//                    } else if (edges.get(currentParent).get(j)==7041) {
//                        System.out.print("    Here are the beetles!\n");
//                    }
                    taxidsAddedInCurrentDepth.add(edges.get(currentParent).get(j));

                }
                //free some memory at each tree level
                edges.remove(currentParent);
                //vertices.remove(currentParent);
            }
            currentDepth++;
            taxidsFromPreviousDepth.clear();
            taxidsFromPreviousDepth.addAll(taxidsAddedInCurrentDepth);
            taxidsAddedInCurrentDepth.clear();
            //System.out.println(" Depth level = "+currentDepth+" ("+taxidsFromPreviousDepth.size()+" nodes at this level)");
            
            if (taxidsFromPreviousDepth.size()<1) //to quit the loop, there is no more nodes at this depth
                break;
        }
        edges=null;
        edgesRank=null;
        edgesDivision=null;
        System.gc();

        System.out.println("Taxonomy tree loaded!");
    }
    
    /**
     * retrieve the taxid of an organism directly from its name
     * very basic search, the name should be exactly as in the NCBI
     * i.e taking into account genus/species/subspecies
     * however upper/lower cases have no influence
     * @param organismName
     * @return 
     */
    public int getTaxId(String organismName) {
        int taxId=-1;
        //searching if a node contains this name
        Collection<TaxoVertex> children = tree.getVertices();
        for (Iterator<TaxoVertex> it = children.iterator(); it.hasNext();) {
            TaxoVertex taxoVertex = it.next();
            ArrayList<String> namesForClass = taxoVertex.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME);
            for (Iterator<String> it1 = namesForClass.iterator(); it1.hasNext();) {
                String string = it1.next();
                if (string.equalsIgnoreCase(organismName.trim())) {
                    taxId=taxoVertex.getTaxId();
                    break;
                }
            }
            if (taxId>-1) {
                break;
            }
        }
        return taxId;
    }


    /**
     * return the complete lineage of an organism selected by its taxid
     * lineage is a scientific name concatenation with levels separated by ';'
     * return null if no match is found
     * @param taxid
     * @return
     */
    public String getStringLineage(int taxid) {
        List<TaxoVertex> l=tree.getPath(vertices.get(taxid));
        if (l==null) {return null;}
        StringBuilder sb=new StringBuilder();
        boolean first=true;
        for (Iterator<TaxoVertex> it=l.iterator();it.hasNext();) {
            TaxoVertex v=it.next();
            if (first) {
                sb.append(v.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME));
            } else {
                sb.append(";"+v.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0));
            }
            first=false;
        }
        return sb.toString();
            
    }

    /**
     * return the complete lineage of an organism selected by its taxid
     * as a sortedMap where key=rank_scientific_name, val=rank_nature (kingdom, no_rank, family, genus...)
     * return null if no match is found
     * @param taxid
     * @return
     */
    public LinkedHashMap<String,Integer> getRankedLineage(int taxid) throws Exception {
        List<TaxoVertex> l=tree.getPath(vertices.get(taxid));
        if (l==null) {return null;}
        LinkedHashMap<String,Integer> map=new LinkedHashMap(l.size());
        for (TaxoVertex v : l) {
            //root case, no parents
            if (v.getTaxId()==1) {
                map.put(
                        v.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0),
                        Rank.NO_RANK
                );
            } else {
                map.put(
                        v.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0),
                        tree.getParentEdge(v).getRank()
                );
            }
        }
        return map;
    }

    /**
     * return the complete taxid lineage of an organism selected by its taxid
     * return null if no match is found
     * @param taxid
     * @return
     */
    public ArrayList<Integer> getTaxIdLineageArray(int taxid) {
        List<TaxoVertex> l=tree.getPath(vertices.get(taxid));
        if (l==null) {return null;}
        StringBuilder sb=new StringBuilder();
        ArrayList<Integer> res=new ArrayList<>();
        for (Iterator<TaxoVertex> it=l.iterator();it.hasNext();) {
            res.add(it.next().getTaxId());
        }
        return res;        
    }


    /**
     * return the complete lineage of an organism selected by its scientific name
     * case is ignore and argument String can be a regular java regexp
     * first match, starting from the tree root, is retained
     * lineage is a scientific name concatenation with levels separated by ';'
     * return null if no match is found
     * @param lineage
     * @return
     */
    public String getStringLineage(String lineage) {
        int taxid=-1;
        for (Iterator<Integer> it=vertices.keySet().iterator();it.hasNext();) {
            taxid=it.next();
            if (vertices.get(taxid).getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0).matches(lineage)) {
                break;
            }
        }
        if (taxid==-1) {return null;}
        List<TaxoVertex> l=tree.getPath(vertices.get(taxid));
        StringBuilder sb=new StringBuilder();
        boolean first=true;
        for (Iterator<TaxoVertex> it=l.iterator();it.hasNext();) {
            TaxoVertex v=it.next();
            if (first) {
                sb.append(v.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0));
            } else {
                sb.append(";"+v.getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0));
            }
            first=false;
        }
        return sb.toString();
            
    }

    /**
     * return the complete lineage of an organism selected by its scientific name
     * case is ignore and argument String can be a regular java regexp
     * first match, starting from the tree root, is retained
     * lineage is a scientific name concatenation with levels separated by ';'
     * return null if no match is found
     * @param lineage
     * @return
     */
    public String getTaxIdLineage(String lineage) {
        int taxid=-1;
        for (Iterator<Integer> it=vertices.keySet().iterator();it.hasNext();) {
            taxid=it.next();
            if (vertices.get(taxid).getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0).matches(lineage)) {
                break;
            }
        }
        if (taxid==-1) {return null;}
        List<TaxoVertex> l=tree.getPath(vertices.get(taxid));
        StringBuilder sb=new StringBuilder();
        boolean first=true;
        for (Iterator<TaxoVertex> it=l.iterator();it.hasNext();) {
            TaxoVertex v=it.next();
            if (first) {
                sb.append(v.getTaxId());
            } else {
                sb.append(";"+v.getTaxId());
            }
            first=false;
        }
        return sb.toString();
            
    }

    /**
     * return the complete lineage of an organism selected by its staxid
     * first match, starting from the tree root, is retained
     * lineage is a scientific name concatenation with levels separated by ';'
     * return null if no match is found
     * @param taxid
     * @return
     */
    
    public String getTaxIdLineage(int taxid) {
        if (taxid<-1) {return null;}
        List<TaxoVertex> l=tree.getPath(vertices.get(taxid));
        StringBuilder sb=new StringBuilder();
        boolean first=true;
        for (Iterator<TaxoVertex> it=l.iterator();it.hasNext();) {
            TaxoVertex v=it.next();
            if (first) {
                sb.append(v.getTaxId());
            } else {
                sb.append(";"+v.getTaxId());
            }
            first=false;
        }
        return sb.toString();
            
    }

    /**
     * return a list of all the taxid that belong to the subtree of a particular node (including internal nodes)
     * @param taxid
     * @return
     */
    public ArrayList<Integer> getSubTreeTaxids(int taxid) {
        try {
            Collection<TaxoVertex> subvertices = TreeUtils.getSubTree(tree, vertices.get(taxid)).getVertices();
            if (subvertices==null) {return null;}
            if (subvertices.size()<1) {return null;}
            ArrayList<Integer> taxids=new ArrayList<>();
            for (Iterator<TaxoVertex> it=subvertices.iterator();it.hasNext();) {
                TaxoVertex v = it.next();
                taxids.add(v.getTaxId());
            }
            return taxids;
            
        } catch (InstantiationException ex) {
            Logger.getLogger(NCBITaxonomyTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(NCBITaxonomyTree.class.getName()).log(Level.SEVERE, null, ex);
        }    
        return null;
    }

    /**
     * return a list of all the taxid that belong to the subtree of a particular node (including internal nodes)
     * and for a particular division
     * @param taxid
     * @param division
     * @return
     */
    public ArrayList<Integer> getSubTreeTaxids(int taxid, int division) {
        try {
            Collection<TaxoVertex> subvertices = TreeUtils.getSubTree(tree, vertices.get(taxid)).getVertices();
            if (subvertices==null) {return null;}
            if (subvertices.size()<1) {return null;}
            ArrayList<Integer> taxids=new ArrayList<>();
            for (Iterator<TaxoVertex> it=subvertices.iterator();it.hasNext();) {
                TaxoVertex v = it.next();

                taxids.add(v.getTaxId());
            }
            return taxids;

        } catch (InstantiationException ex) {
            Logger.getLogger(NCBITaxonomyTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(NCBITaxonomyTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * return a list of all the taxid that belong to the subtree of a particular node (only leaves)
     * @param taxid
     * @return
     */
    public ArrayList<Integer> getSubtreeLeavesTaxids(int taxid) {
        try {
            Collection<TaxoVertex> subvertices = TreeUtils.getSubTree(tree, vertices.get(taxid)).getVertices();
            if (subvertices==null) {return null;}
            if (subvertices.size()<1) {return null;}
            ArrayList<Integer> taxids=new ArrayList<>();
            for (Iterator<TaxoVertex> it=subvertices.iterator();it.hasNext();) {
                TaxoVertex v = it.next();
                //add its son if is a leaf
                if (tree.getChildCount(v)==0) {
                    taxids.add(v.getTaxId());
                }
            }
            return taxids;

        } catch (InstantiationException ex) {
            Logger.getLogger(NCBITaxonomyTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(NCBITaxonomyTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * get scientific name, null if taxid invalid
     * @param taxid
     * @return
     */
    public String getScientificName(int taxid) {
        if (!vertices.containsKey(taxid)) {
            return null;
        }
        return vertices.get(taxid).getNamesTxtForClass(NameClass.SCIENTIFIC_NAME).get(0);
    }
    
    
}
