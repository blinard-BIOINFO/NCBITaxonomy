package environment;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * contains several tools for system environement variables
 * @author benjamin linard
 */
public class Environment {

    /**
     * print all system environement variables to stdout
     */
    public static void printEnvironementVariables() {
        java.util.Enumeration liste = System.getProperties().propertyNames();
        String cle;
        while( liste.hasMoreElements() ) {
                cle = (String)liste.nextElement();
                System.out.println( cle + " = " + System.getProperty(cle) );
        }
    }

    /**
     * get path of the jar or the class that call this method (name of file included)
     * @param c
     * @return
     * @throws UnsupportedEncodingException 
     */
    public static String getExecutablePath(Class c) throws UnsupportedEncodingException {
        String path = "/" + c.getName().replace('.', '/') + ".class";
        URL url = c.getResource(path);
        path = URLDecoder.decode(url.toString(), "UTF-8");

        // suppression de  la classe ou du jar du path de l'url
        int index = path.lastIndexOf("/");
        path = path.substring(0, index);
        if (path.startsWith("jar:file:")) {
            // suppression de jar:file: de l'url d'un jar
            // ainsi que du path de la classe dans le jar
            index = path.indexOf("!");
            path = path.substring(9, index);
            return path;
        } else {
            // suppresion du file: de l'url si c'est une classe en dehors d'un jar
            // et suppression du path du package si il est présent.
            path = path.substring(5, path.length());
            Package pack = c.getClass().getPackage();
            if (null != pack) {
                String packPath = pack.toString().replace('.', '/');
                if (path.endsWith(packPath)) {
                    path = path.substring(0, (path.length() - packPath.length()));
                }
            }
            return path;
        }
    }
    /**
     * get path of the jar or the class that call this method (only directory)
     * @param c
     * @return
     * @throws UnsupportedEncodingException
     */
    public static File getExecutablePathWithoutFilename(Class c) {
        try {
            String path= Environment.getExecutablePath(c);
            return new File(path).getParentFile();
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * print memory usage in MB format( max memory, free memory and used memory)
     */
    public static void printMemoryUsage() {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        System.out.println("MEMORY: allocated: " + df.format(new Double(Runtime.getRuntime().totalMemory()/1048576)) + "MB of " + df.format(new Double(Runtime.getRuntime().maxMemory()/1048576))+ "MB (" + df.format(new Double(Runtime.getRuntime().freeMemory()/1048576)) +"MB free)");
    }

    /**
     * print memory usage in MB format( max memory, free memory and used memory)
     */
    public static String getMemoryUsage() {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        return "MEMORY: allocated: " + df.format(new Double(Runtime.getRuntime().totalMemory()/1048576)) + "MB of " + df.format(new Double(Runtime.getRuntime().maxMemory()/1048576))+ "MB (" + df.format(new Double(Runtime.getRuntime().freeMemory()/1048576)) +"MB free)";
    }

}
