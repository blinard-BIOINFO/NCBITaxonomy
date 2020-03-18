#!/bin/sh

#############################################
# Script download NCBI taxonomy database from 
# their ftp server
#  -download required files
#  -convert their char encoding
#  -create the database schema (currently postgresql)
#  -dump the retrieved files to the database
#
#  ! must be run from sql server host 
#  ! will ask for sql admin password during the procedure
#
#Author: benjamin linard ; benjamin.linard@gmail.com
##############################################


#################
# SCRIPT CONFIG

#DB user, need rights for creating databases and dumping data via COPY
#(either allowed via postgres engine config, or grant role 'pg_write_server_files' from pg11)
PG_USER=user
PG_HOST=localhost
PG_PORT=5432
WORKDIR=/path/to/NCBI_taxonomy

#set which very large tables should be indexed or not
#ex: accession2taxid contains milliards of lines leading to hundreds of Gb of index
#by default, it is not indexed
index_accession2taxid=0
index_index_gi_taxid_nucl=1
index_index_gi_taxid_prot=1

#TO SKIP SOME STEPS
step_schema=1
step_download=1
step_inflate=1
step_encode=1
step_dump=1
step_index=1
step_clean=1

#create automatically database name, if different than ""
#by default database name is ncbi_taxonomy_$(year_month_day)
dbname=""
##################
##################


if [ $step_schema -eq 1 ] ; then
        #create new database
        echo "creating database"
        d=`date "+%Y_%m_%d"`
        dbname="ncbi_taxonomy_"$d
        echo "psql -h $PG_HOST -U -p $PG_PORT $PG_USER"
        echo "create database "$dbname" ;" | psql -h $PG_HOST -U $PG_USER -p $PG_PORT $PG_USER
        if [ $? -ne 0 ] ; then
                echo "database name creation failed"
                exit 1
        fi
        #create schema
        echo "creating schema"
        #command to dump the schema
        echo "psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname < $WORKDIR/nocomment_taxonomy_schema.sql"
        psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname < $WORKDIR/nocomment_taxonomy_schema.sql
        if [ $? -ne 0 ] ; then
                echo "database schema creation failed"
                exit 1
        fi
fi




cd $WORKDIR
#get files from NCBI
DUMPDIR=$WORKDIR/original_dump


#DOWNLOAD THE NCBI FILES
if [ $step_download -eq 1 ] ; then
	mkdir -p $DUMPDIR
	rm -f $DUMPDIR/*
	# gi to taxid files
	wget --directory-prefix=$DUMPDIR ftp://ftp.ncbi.nih.gov/pub/taxonomy/*.dmp.gz
	#database iself
	wget --directory-prefix=$DUMPDIR ftp://ftp.ncbi.nih.gov/pub/taxonomy/taxdump.tar.gz
	#supll infos
	wget --directory-prefix=$DUMPDIR ftp://ftp.ncbi.nih.gov/pub/taxonomy/*.txt
	#accessionsToTaxids
	wget --directory-prefix=$DUMPDIR ftp://ftp.ncbi.nih.gov/pub/taxonomy/accession2taxid/*.gz
fi

if [ $step_inflate -eq 1 ] ; then
	#unzip files
	cd $DUMPDIR
	gunzip -vf *.dmp.gz
	tar -zxvf *.tar.gz
	gunzip -vf *taxid.gz
fi


if [ $step_encode -eq 1 ] ; then 
	#encoding conversion
	echo "changing encoding from ISO-8859-15 to UTF-8"
	for i in `ls *.dmp`
		do
		echo "   $i"
		iconv -c -f UTF-8 -t ISO-8859-15 $i > $i.e
		done
	for i in `ls *2taxid`
		do
		echo "   $i"
		iconv -c -f UTF-8 -t ISO-8859-15 $i > $i.e
		done
	#just change name of gi files
	for i in `ls gi_*.dmp.e`
	        do
	        mv $i $i.2
	        done

	#necessary to remove | that are not column separators
	echo "replacing non-separator | from dumps"
	for i in `ls *.dmp.e`
		do
		echo "   $i"
		sed 's/[^\t]|[^\t]\|[^\t]|\t\|\t|[^\t]/I/g' $i > b_$i
		done

	#removes tab from dumps
	echo "removing tabs from dumps"
	for i in `ls b_*.dmp.e`
		do
		echo "   $i"
		sed 's/\t//g' $i > t$i
		done
	for i in `ls tb_*.dmp.e`
	        do
	        echo "   $i"
	        sed 's/^\(.*\)|$/\1/g' $i > f$i
	        done


	#replace tab with | in gi_taxid dumps
	echo "replacing tabs with | in gi-related dumps"
	for i in `ls *.dmp.e.2`
        	do
		echo "   $i"
	        sed 's/\t/|/g' $i > ftb_$i
	        done
	#rm gi_.dmp.e.2
	#replace tab with | in db2taxid files
        echo "replacing tabs with | in db2taxid files"
        for i in `ls *2taxid.e`
                do
                echo "   $i"
                sed 's/\t/|/g' $i > ftb_$i
                done
	#add 'database' column in db2taxid files and remove header
	echo "adding db column to accession2taxid files"
	for i in `ls ftb_*2taxid.e`
		do
		echo "   $i"
		database="`echo $i | sed 's/^ftb_\([^\.]*\)\..*$/\1/'`"
		pattern="s/$/\|"$database"/"
		sed "$pattern" $i | sed '1d' > $i.ext
		done
	cat ftb_*2taxid.e.ext >> all_ftb_accession2taxid.e.ext
fi



if [ $step_dump -eq 1 ] ; then
	chmod 755 $WORKDIR"/original_dump/*"
	#copy dumps to database
	echo "copy dumps to database..."
	echo "copy citations..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
	-c "copy citations (cit_id,cit_key,pubmed_id,medline_id,url,text,taxid_list) from \
	    '"$WORKDIR"/original_dump/ftb_citations.dmp.e' with delimiter as '|';"
	echo "copy delnodes..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy delnodes (tax_id) from \
            '"$WORKDIR"/original_dump/ftb_delnodes.dmp.e' with delimiter as '|';"
	echo "copy division..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy division (division_id,division_cde,division_name,comments) from \
            '"$WORKDIR"/original_dump/ftb_division.dmp.e' with delimiter as '|';"
	echo "copy gencode..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy gencode (genetic_code_id,abbreviation,name,cde,starts) from \
            '"$WORKDIR"/original_dump/ftb_gencode.dmp.e' with delimiter as '|';"
	echo "copy gi_taxid_nucl..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy gi_taxid_nucl (gi,taxid) from \
            '"$WORKDIR"/original_dump/ftb_gi_taxid_nucl.dmp.e.2' with delimiter as '|';"
	echo "copy gi_taxid_prot..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy gi_taxid_prot (gi,taxid) from \
            '"$WORKDIR"/original_dump/ftb_gi_taxid_prot.dmp.e.2' with delimiter as '|';"
	echo "copy merged..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy merged (old_tax_id,new_tax_id) from \
            '"$WORKDIR"/original_dump/ftb_merged.dmp.e' with delimiter as '|';"
	echo "copy names..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy names (taxid,name_txt,unique_name,name_class) from \
            '"$WORKDIR"/original_dump/ftb_names.dmp.e' with delimiter as '|';"
	echo "copy nodes..."
	psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy nodes (tax_id,parent_tax_id,rank,embl_code,division_id,inherited_div_flag,genetic_code_id,inherited_gc_flag, \
            mitochondrial_genetic_code_id,inherited_mgc_flag,GenBank_hidden_flag,hidden_subtree_root_flag,comments) from \
            '"$WORKDIR"/original_dump/ftb_nodes.dmp.e' with delimiter as '|';"
	echo "copy accession2taxid..."
        psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
        -c "copy accession2taxid (accession,accession_version,taxid,gi,db) from \
            '"$WORKDIR"/original_dump/all_ftb_accession2taxid.e.ext' with delimiter as '|';"
	echo "Copy done! database filled with data."
fi

if [ $step_index -eq 1 ] ; then

	#create indexes AFTER COPY insertion, way more faster than filling indexes
	#before COPY
        echo "indexing names,nodes ..."
        psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
	-c "
	    CREATE INDEX idx_names_name_class ON names USING btree (name_class); \
	    CREATE INDEX idx_names_name_txt ON names USING btree (name_txt); \
	    CREATE INDEX idx_names_taxid ON names USING btree (taxid); \
	    CREATE INDEX idx_nodes_parent_tax_id ON nodes USING btree (parent_tax_id); \
	    CREATE INDEX idx_nodes_tax_id ON nodes USING btree (tax_id); \
	    CREATE INDEX idx_nodes_division_id ON nodes USING btree (division_id); \
	"

	if [ $index_gi_taxid_nucl -eq 1 ] ; then
	echo "indexing taxid_nucl ..."
		psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
	        -c "
	            CREATE INDEX idx_gi_taxid_nucl_gi ON gi_taxid_nucl USING btree (gi); \
	            CREATE INDEX idx_gi_taxid_nucl_taxid ON gi_taxid_nucl USING btree (taxid); \
		"
	fi

        if [ $index_gi_taxid_prot -eq 1 ] ; then
        echo "indexing taxid_prot ..."
                psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
                -c "
                    CREATE INDEX idx_gi_tax_id_prot_gi ON gi_taxid_prot USING btree (gi); \
                    CREATE INDEX idx_gi_tax_id_prot_taxid ON gi_taxid_prot USING btree (taxid); \
                "
        fi
	
	if [ $index_accession2taxid -eq 1 ] ; then
		echo "indexing accession2taxid ..."
		psql -h $PG_HOST -U $PG_USER -p $PG_PORT $dbname \
	        -c " 
		   CREATE INDEX idx_accession_accession2taxid ON accession2taxid USING btree (accession) WITH ( FILLFACTOR = 100); \
		    CREATE INDEX idx_accession_version_accession2taxid ON accession2taxid USING btree (accession_version) WITH ( FILLFACTOR = 100); \
		    CREATE INDEX idx_taxid_accession2taxid ON accession2taxid USING btree (taxid) WITH ( FILLFACTOR = 100); \
		    CREATE INDEX idx_gi_accession2taxid ON accession2taxid USING btree (gi) WITH ( FILLFACTOR = 100); \
		    CREATE INDEX idx_db_accession2taxid ON accession2taxid USING btree (db) WITH ( FILLFACTOR = 100); \
		"
	fi
	echo "Database "$dbname" will be ready for exploitation after granting connection to taxnonomypublic in pg_hba.conf ."
fi

if [ $step_clean -eq 1 ] ; then	
	#delete files
	rm *.dmp.2
	rm *.dmp
fi
	
