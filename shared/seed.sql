create database ribbontek;
begin;
create user grpcshared with password 'grpcshared';
grant connect on database ribbontek to grpcshared;
grant all privileges on database ribbontek to postgres;
commit;