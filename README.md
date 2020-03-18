**Contributions are welcome !**

# NCBI Taxonomy SQL/Java Tools

This set of tools aims to create a local SQL copy of the NCBI Taxonomy database and uses Java scripts to query the database.
 
 It aims to answer many common operations when working on systematics or species identification, such as :
  * extract a lineage for a species or taxonomic id
  * extract all species below a certain taxonomy node
  * extract every lineages of every species below a certain taxonomy node
  * build a Blast "Delimitation File", intended to be used with commands `blastdb_aliastool -gilist ` to build Blast database indexes focuses on particular NCBI Taxonomy clades.
  * ... etc ...
  
This package was initially developed for the following academic projects :

* *The contribution of mitochondrial metagenomics to large-scale data mining and phylogenetic analysis of Coleoptera. Linard B et al. Mol Phylogenet Evol. 2018 Nov;128:1-11.*
* *Lessons from genome skimming of arthropod-preserving ethanol. Linard B. et al.  Mol Ecol Resour. 2016 Nov;16(6):1365-1377.*
* *Metagenome skimming of insect specimen pools: potential for comparative genomics. Linard et al. Genome Biol Evol. 2015 May 14;7(6):1474-89.*

If these sources are of any use in your own project, the authors would greatly appreciate that you cite one of these.

## Requirements

* Postgresql server (Java code should be compatible with other SGBDs after adapting COPY statements in `update_taxonomy.sh` and the SQL schema in `taxonomy_schema.sql`)
* ADMIN or COPY rights associated to your SGBD user/role to copy NCBI dumps to your local database.
* Java JDK 1.8


## NCBI Taxonomy operations

### Available operations

* **ScientificNamesToLineages** : From a list of Scientific Names, (written in a file, 1 identifier per line) extract the corresponding NCBI lineages.

* **TaxidToLineage** : Extract the lineage from a simple taxonomic id.

* **TaxidToSubTreeLeavesLineages** : Using a taxonomic id of an internal node of NCBI Taxonomy (for instance, 7041 which is Coleopteran order), extract the lineages of every species belonging to the subtree of this node (with, 7041, extract lineages of every Coleoptaran species.

* **IdentifiersToLineages** : From a list of NCBI GIs or ACCESSIONs identifiers (written in a file, 1 identifier per line) extract the corresponding NCBI lineages. WARNING: queries will be fast only IF `index_accession2taxid` was set to 1 (default is 0) during installation. If not, you can index later the column of this table (corresponding SQL lines are in `database_schema.sql`).

* **GenerateTaxonomyDelimitationForBlast** : One can require a copy of the NCBI Blast database focused on a particular clade. For instance, you may download the Nematodes Blast database but you are actually only interested by C. elegans sequences. A command `blastdb_aliastool -db nematode_mrna -gilist c_elegans_mrna.gi` can help you to build the corresponding Blast index (see NCBI documentation) BUT the annoying part is to build the `gilist` which targets every single sequence of C. elegans. The present operation does exactly that, from a taxonomic id, it will extract every gi numbers associated to the subtree so that you can build later a Blst database focused on a particular clade and accelerate you Blast searches. WARNING: queries will be fast only IF `index_index_gi_taxid_nucl` or `index_index_gi_taxid_prot` were set to 1 during installation (default is 1). If not, you can index later the column of this table (corresponding SQL lines are in `database_schema.sql`).

* More to come ...


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

Available operations can be listed by writing the following line in a terminal, followed by 2 pushes of the TAB key when your cursor is just on the right of the last dot :
```
java -cp NCBITaxonomy.jar op.
```
If your system is correctly configured for Java autocompletion, you should see a list of all available operations (op).
```
op.DBConnectionTest
op.GenerateTaxonomyDelimitationForBlast
op.IdentifiersToLineages
op.ScientificNamesToLineages
op.TaxidToLineage
op.TaxidToSubTreeLeavesLineages
```

## Installation

The installation process is done in 4 steps:

1. Configure the header of 'update_taxonomy.sh'. Execute to download the NCBI taxonomy dumps and copy them to a new database in your SQL server. (requires ADMIN or COPY rights associated to your SGBD user/role).
2. Optionnal: Create a user granted with SELECT permissions on the created database.
3. Write the database credentials of this user in a database.properties file (see below).
4. Use the Java package to query your new NCBI Taxonomy database via different operations (see below).


**Step 1: update_taxonomy.sh**

Just edit the `### SCRIPT CONFIG` section. Some important points:

* The database user set in this file MUST have COPY rights to dump data to the database. In most recent version of Postgres, this can be done by granting `pg_write_server_files` privileges to this user.
* `index_*` options are intended to avoid the create of giant indexes that are not necessarily useful to your applications. In particular, the table `index_accession2taxid=0` will avoid to index the corresponding table wich contains todays ~ 2 milliards of lines (march 2020), leading to a >100 Gb index while the database itself is less than this size !
* `step_*` options are there only if one of the step fails and you need to relaunch the script. For instance, to avoid downloading again the NACBI taxonomy dumps when the script failed in a later step.
* By default the create database name will follow the pattern `ncbi_taxoniomy_YYYY_MM_DD` where YYYY_MM_DD are year-month-date in numerical caracters. This can be changed by changing `dbname=""` with a non-empty string.

**Step 2: (Optionnal) NCBI taxonomy user**

It can be useful to create a user dedicated to this new datbase, in particular when someone not intended to modify your SQL datbases just want to interogate NCBI taxonomy. In the database prompt, and a role holding 'CREATE USER' rights, do the following :

```
CREATE USER taxonomypublic WITH PASSWORD 'taxonomypublic' ;
GRANT CONNECT ON DATABASE ncbi_taxonomy_YYYY_MM_DD TO taxonomypublic ;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO taxonomypublic ;
```

**Step 3: database.properties file**

Edit the file `database.properties` to set valid database credentials. The Java code will require this file to connect to the database.  You can either use your own database user or a dedicated-user as sdhown in step 2. 
By default, the Java code will look for database credentials in a database.properties file in the current directory.
```
jdbc.drivers=org.postgresql.Driver                           #driver
jdbc.url=jdbc:postgresql://ip:port/ncbi_taxonomy_xxxx_xx_x   #taxonomy DB address
jdbc.username=taxonomypublic                                 #login
jdbc.password=taxonomypublic                                 #password
```
Moreover, if you plan to use something else than Postgresl (MySQL, Oracle ...) do not forget to change the driver accordingly. 

**Step 4: Java compilation**

Install Java JDK and compiler is not already done.
```
#install java DK and gradle for compilation
sudo apt-get update
sudo apt-get install openjdk-8-jdk
sudo apt-get install gradle
```
Compile sources. To use another JDBC driver (MySQL, Oracle ...) edit `gradle.build` to add the corresponding driver in the dependancies. 

``` 
git clone https://github.com/blinard-BIOINFO/NCBITaxonomy.git
cd ./NCBITaxonomy
gradle build && gradle clean
```
Rapid test. The command help should appear.
```
java -cp NCBITaxonomy-0.1.0.jar op.TaxidToLineage --help
```

Rapid connection test. If your setup is correct, you should see : 
```
java -cp NCBITaxonomy-0.1.0.jar op.DBConnectionTest

Testing postgres database connection...
connected on: jdbc:postgresql://127.0.0.1:5432/ncbi_taxonomy_2020_03_12
with user: taxonomypublic
```

By default the java code will look for the file `database.properties` in the directory where `NCBITaxonomy-x.x.x.jar` is saved. Alternatively, you can target this file by setting a Java System Property:
```
java -Ddatabase.properties=/path/to/database.properties -cp NCBITaxonomy-0.1.0.jar op.DBConnectionTest
```


# License

This code is distributed under the MIT License.
