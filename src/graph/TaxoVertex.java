
package graph;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * simple vertex representation
 * @author benjmain linard
 */
public class TaxoVertex {

    private int taxid=-1;
    private int division=-1;
    
    //map(name_class_id)=array(values)
    /*the key represent the NameClass, one the value contained in @class NameClass
    private HashMap<String,ArrayList<String>> names=null;
    */
    private HashMap<Integer, ArrayList<String>> namesTxt=null;
    private HashMap<Integer, ArrayList<String>> uniqueNames=null;

    /*
     * minimal constructor
     */
    public TaxoVertex( int taxid) {
        this.taxid=taxid;
        namesTxt=new HashMap<>();
        uniqueNames=new HashMap<>();
    }

    /**
     * add a name to this vertex
     * @param nameClassId
     * @param nameTxt
     * @param uniqueName
     */
    public void addName(int nameClassId,String nameTxt,String uniqueName) {
        if (!namesTxt.containsKey(nameClassId)) {
            namesTxt.put(nameClassId, new ArrayList<String>());
            uniqueNames.put(nameClassId,new ArrayList<String>());
        }
        namesTxt.get(nameClassId).add(nameTxt);
        uniqueNames.get(nameClassId).add(uniqueName);
    }
    
    
    public int getTaxId() {
        return this.taxid;
    }
    
    public ArrayList<String> getNamesTxtForClass(int nameClassId) {
        return namesTxt.get(nameClassId); 
    }
    
    public ArrayList<String> getUniqueNamesForClass(int nameClassId) {
        return uniqueNames.get(nameClassId); 
    }   

    @Override
    public String toString() {
        return super.toString()+" taxid:"+this.taxid+" namesTxt:"+this.namesTxt+" uniqueNames:"+this.uniqueNames;
    }
    
    
            
}
