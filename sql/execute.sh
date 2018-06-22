SQL_HOME=/home/cubrid/SQL/

csql -u dba paasta -i $SQL_HOME/drop_all.sql
csql -u dba paasta -i $SQL_HOME/create_all.sql
csql -u dba paasta -i $SQL_HOME/insert_data.sql
