**Contributions improving the code of this repository are welcome !**

# NCBI Taxonomy SQL/Java Tools

This set of tools aims to create a local SQL copy of the NCBI Taxonomy database and uses Java scripts to query the database.
 
 It aims to answer many common operations when working on systematics or species identification, such as :
  * extract lineage for a species
  * extract all species below a certain taxonomy node
  * extract the lineages of all species below a certain taxonomy node
  * build a Blast "Delimitation File", used when building a blast database index focusing on a particular NCBI Taxonomy clade.
  * ... etc ...
  
This package was initially developed for the following academic projects :

* *The contribution of mitochondrial metagenomics to large-scale data mining and phylogenetic analysis of Coleoptera. Linard B et al. Mol Phylogenet Evol. 2018 Nov;128:1-11.*
* *Lessons from genome skimming of arthropod-preserving ethanol. Linard B. et al.  Mol Ecol Resour. 2016 Nov;16(6):1365-1377.*
* *Metagenome skimming of insect specimen pools: potential for comparative genomics. Linard et al. Genome Biol Evol. 2015 May 14;7(6):1474-89.*

If these sources are of any use in your own project, the authors would greatly appreciate that you cite one of these manuscripts.

## Requirements

* Postgresql server (Java code should be compatible with other SGBDs after adapting COPY statements in `update_taxonomy.sh` and the SQL schema in `taxonomy_schema.sql`)
* ADMIN or COPY rights associated to your SGBD user/role to copy NCBI dumps to your local database.
* Java JDK 1.8


## Installation

The installation process is done in 4 steps:

1. Configure the header of 'update_taxonomy.sh'. Execute itto download the NCBI taxonomy dumps and copy them to your SQL server. (requires ADMIN or COPY rights associated to your SGBD user/role).
2. Create a user granted with SELECT permissions on the created database.
3. Write the database credentials of this user in a database.properties file (see example below).
4. Use the Java package to query your database copy (see example below).


**Step 1: Java compilation**

**Step 2: Java compilation**

**Step 3: Java compilation**

**Step 4: Java compilation**

Install Java JDK and compiler is not already done.
```
#install java DK and gradle for compilation
sudo apt-get update
sudo apt-get install openjdk-8-jdk
sudo apt-get install gradle
```
Compile sources.
``` 
gradle clean && gradle build
```
Rapid test. The command help should appear.
```
java -cp NCBITaxonomy-0.1.0.jar op.TaxidToLineage --help
```

Rapid connection test. If your setup is correct, you should see : 
```
java -Ddatabase.properties=database.properties -cp NCBITaxonomy-0.1.0.jar op.DBConnectionTest

Testing postgres database connection...
connected on: jdbc:postgresql://127.0.0.1:5432/ncbi_taxonomy_2020_03_12
with user: taxonomypublic
```


## NCBI Taxonomy operations

### Calling an operation

```
java -cp NCBITaxonomy.jar op.[operation_name] 
```
For instance:
```
java -cp NCBITaxonomy.jar op.TaxidToLineage --help
```
Will show the usage of this operation:
```
Usage: TaxidToLineage [-hrV] [-f=[1|2]] [-o=<out>] -t=int
Extract NCBI lineage from a NCBI taxid.
  -f, --format=[1|2]   Format used to output ranks:
                       1 = 'Homo[Genus];sapiens[Species]'
                       2 = 'Homo;sapiens' (line 1)
                            'genus;species' (line 2)
  -h, --help           Show this help message and exit.
  -o, --out=<out>      Output results in file instead of stdout.
  -r, --ranks          Add rank names to scientific names.
  -t, --taxid=int      The taxonomic id.
  -V, --version        Print version information and exit.
```

Available operation can be listed by writing the following line in a terminal, followed by 2 pushes on the TAB key when your cursor is just on the right of the last dot :
```
java -cp NCBITaxonomy.jar op.
```
If your system is correctly configured for Java autocompletion, you should see a list of all available operations (op).
```
op.DBConnectionTest
op.GenerateTaxonomyDelimitationForBlast
op.IdentifiersToLineages
[...]
```

### Description of operations:

* IdentifiersToLineages : From a list of NCBI GIs or ACCESSIONs identifiers (written in a file, 1 identifier per line) extract the corresponding NCBI lineages.
* ScientificNamesToLineages : From a list of Scientific Names, (written in a file, 1 identifier per line) extract the corresponding NCBI lineages.
* TaxidToLineage : Extract the lineage from a simple taxonomic id.
* TaxidToSubTreeLeavesLineages : Using a taxonomic id of an internal node of NCBI Taxonomy (for instance, 7041 which is Coleopteran order), extract the lineages of every species belonging to the subtree of this node (with, 7041, extract lineages of every Coleoptaran species.
* 
* etc...

[to complete...]

## Database connection

By default, the Java code will look for database credentials in a database.properties file in the current directory.
This file contains the following lines:
```
jdbc.drivers=org.postgresql.Driver                           #driver
jdbc.url=jdbc:postgresql://ip:port/ncbi_taxonomy_xxxx_xx_x   #taxonomy DB address
jdbc.username=taxonomypublic                                 #login
jdbc.password=taxonomypublic                                 #password
```
If the program cannot locate correctly your credentials (database.properties file), set it manually.
For instance, the following example force to load database credentials from the relative file
`credentials/connection.infos`:
```
java -Ddatabase.properties=credentials/connection.infos -cp NCBITaxonomy.jar op.[operation_name] 
```
Connection to database can be tested with operation `TaxoDBConnection`:
```
java -cp NCBITaxonomy.jar op.DBConnectionTest
```

# License

This code is distributed under the MIT License.
