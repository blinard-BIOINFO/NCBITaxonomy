# NCBI Taxonomy Java Tools

The current set of tools aims to create a local SQL copy of the NCBI Taxonomy database and proveds Java classes and 
scripts to query the database.
 
 It aims to answer many common operations when working on systematics or species identification, such as :
  * extract lineage for a species
  * extract all species below a certain taxonomy node
  * extract the lineages of all species below a certain taxonomy node
  * build a Blast "Delimitation File", used when building a blast database index focusing on a particular NCBI Taxonomy clade.
  * ... etc ...
  
This package has been initially developed during the following projects:
* The contribution of mitochondrial metagenomics to large-scale data mining and phylogenetic analysis of Coleoptera. 
Linard B et al. Mol Phylogenet Evol. 2018 Nov;128:1-11.
* Lessons from genome skimming of arthropod-preserving ethanol. Linard B. et al.  Mol Ecol Resour. 2016 
Nov;16(6):1365-1377.
* Metagenome skimming of insect specimen pools: potential for comparative genomics. Linard et al. Genome Biol Evol. 
2015 May 14;7(6):1474-89.

If these sources are of any use in your own projects, the authors would greatly appreciate that you cite one of those.

## Requirements

* Postgresql server (should be compatible with other SGBDs after adapting files `update_taxonomy.sh` and `taxonomy_schema.sql`)
* ADMIN or COPY rights associated to your SGBD user/role.
* Java >1.8


## Overview

The installation process is done in 4 steps:

1. Edit 'update_taxonomy.sh' header and execute to download the NCBI taxonomy dumps and copy them in your SQL server.
   (requires ADMIN or COPY rights associated to your SGBD user/role).
2. Create a user granted with SELECT permissions on the created database.
3. Write corresponding database credentials in a database.properties file (see below).
3. Use the Java package to query your database copy (see below).

## Implemented operations

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

* IdentifiersToLineages
* ScientificNamesToLineages
* TaxidToLineage
* TaxidToSubTreeLeavesLineages
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

# Licence

This code is distributed under the MIT Licence.