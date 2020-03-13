SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;
SET default_tablespace = '';
SET default_with_oids = false;


CREATE TABLE citations (
    cit_id integer,
    cit_key text,
    pubmed_id integer,
    medline_id integer,
    url text,
    text text,
    taxid_list text
);



CREATE TABLE delnodes (
    tax_id integer
);



CREATE TABLE division (
    division_id integer,
    division_cde character(3),
    division_name text,
    comments text
);



CREATE TABLE gc (
);



CREATE TABLE gencode (
    genetic_code_id integer,
    abbreviation text,
    name text,
    cde text,
    starts text
);



CREATE TABLE gi_taxid_nucl (
    gi integer,
    taxid integer
);



CREATE TABLE gi_taxid_prot (
    gi integer,
    taxid integer
);



CREATE TABLE merged (
    old_tax_id integer,
    new_tax_id integer
);



CREATE TABLE names (
    taxid integer,
    name_txt text,
    unique_name text,
    name_class text
);



CREATE TABLE nodes (
    tax_id integer,
    parent_tax_id integer,
    rank character varying(25),
    embl_code character varying(5),
    division_id integer,
    inherited_div_flag boolean,
    genetic_code_id integer,
    inherited_gc_flag boolean,
    mitochondrial_genetic_code_id integer,
    inherited_mgc_flag boolean,
    genBank_hidden_flag boolean,
    hidden_subtree_root_flag boolean,
    comments text
);


CREATE TABLE accession2taxid (
    accession text,
    accession_version text,
    taxid integer,
    gi integer,
    db text
);


--Indexes are now generated in the update_taxonomy.sh script for speed improvements
--CREATE INDEX idx_gi_tax_id_prot_gi ON gi_taxid_prot USING btree (gi);
--CREATE INDEX idx_gi_tax_id_prot_taxid ON gi_taxid_prot USING btree (taxid);
--CREATE INDEX idx_gi_taxid_nucl_gi ON gi_taxid_nucl USING btree (gi);
--CREATE INDEX idx_gi_taxid_nucl_taxid ON gi_taxid_nucl USING btree (taxid);
--CREATE INDEX idx_names_name_class ON names USING btree (name_class);
--CREATE INDEX idx_names_name_txt ON names USING btree (name_txt);
--CREATE INDEX idx_names_taxid ON names USING btree (taxid);
--CREATE INDEX idx_nodes_parent_tax_id ON nodes USING btree (parent_tax_id);
--CREATE INDEX idx_nodes_tax_id ON nodes USING btree (tax_id);

--CREATE INDEX idx_accession_accession2taxid ON accession2taxid USING btree (accession);
--CREATE INDEX idx_accession_version_accession2taxid ON accession2taxid USING btree (accession_version);
--CREATE INDEX idx_taxid_accession2taxid ON accession2taxid USING btree (taxid);
--CREATE INDEX idx_gi_accession2taxid ON accession2taxid USING btree (gi);
--CREATE INDEX idx_db_accession2taxid ON accession2taxid USING btree (db);


grant select on citations to taxonomypublic;
grant select on delnodes to taxonomypublic;
grant select on division to taxonomypublic;
grant select on gc to taxonomypublic;
grant select on gencode to taxonomypublic;
grant select on gi_taxid_nucl to taxonomypublic;
grant select on gi_taxid_prot to taxonomypublic;
grant select on merged to taxonomypublic;
grant select on names to taxonomypublic;
grant select on nodes to taxonomypublic;
grant select on accession2taxid to taxonomypublic;
