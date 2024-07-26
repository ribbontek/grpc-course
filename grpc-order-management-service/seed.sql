create database ribbontek;
begin;
create user grpccourse with password 'grpccourse';
grant connect on database ribbontek to grpccourse;
grant all privileges on database ribbontek to postgres;
commit;