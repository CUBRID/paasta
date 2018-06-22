INSERT INTO [docker_service_info] (service_port)
SELECT 40000 + LEVEL FROM DB_ROOT CONNECT BY LEVEL <= 5000;

INSERT INTO [docker_service_info] (service_port)
SELECT 45000 + LEVEL FROM DB_ROOT CONNECT BY LEVEL <= 5000;

INSERT INTO [docker_service_info] (service_port)
SELECT 50000 + LEVEL FROM DB_ROOT CONNECT BY LEVEL <= 5000;

INSERT INTO [docker_service_info] (service_port)
SELECT 55000 + LEVEL FROM DB_ROOT CONNECT BY LEVEL <= 5000;

INSERT INTO [docker_node_info] ([node_ip]) VALUES ('10.96.1.100');
INSERT INTO [docker_node_info] ([node_ip]) VALUES ('10.96.1.101');

INSERT INTO [service_plan_info] ([plan_id], [repository], [tag]) VALUES ('512M', 'local/cubrid', '512M');
INSERT INTO [service_plan_info] ([plan_id], [repository], [tag]) VALUES ('1.5G','local/cubrid', '1.5G');
INSERT INTO [service_plan_info] ([plan_id], [repository], [tag]) VALUES ('2.5G','local/cubrid', '2.5G');
INSERT INTO [service_plan_info] ([plan_id], [repository], [tag]) VALUES ('4.5G','local/cubrid', '4.5G');
