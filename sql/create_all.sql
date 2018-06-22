CREATE USER cubrid PASSWORD 'password';

CALL login('cubrid', 'password') ON CLASS db_user;

CREATE TABLE [service_plan_info] (
    [plan_id]                     VARCHAR   /* PK */
    , [repository]                VARCHAR
    , [tag]                       VARCHAR
    , [updateDateTime]            DATETIME  DEFAULT SYSDATETIME
    
    , CONSTRAINT [pk_service_plan_info_plan_id] PRIMARY KEY([plan_id])
)  REUSE_OID;

CREATE TABLE [service_instance_info] (
    [service_instance_id]         VARCHAR   /* PK */
    , [service_definition_id]     VARCHAR   
    , [plan_id]                   VARCHAR   /* FK */
    , [organization_guid]         VARCHAR
    , [space_guid]                VARCHAR
    , [updateDateTime]            DATETIME  DEFAULT SYSDATETIME
    
    , CONSTRAINT [pk_service_instance_info_service_instance_id] PRIMARY KEY([service_instance_id])
    , CONSTRAINT [fk_service_instance_info_plan_id] FOREIGN KEY (plan_id) REFERENCES [service_plan_info] ([plan_id])
)  REUSE_OID;

CREATE TABLE [service_bind_info] (
    [service_instance_bind_id]    VARCHAR   /* PK */
    , [service_instance_id]       VARCHAR   /* FK */
    , [application_id]            VARCHAR
    , [username]                  VARCHAR
    , [password]                  VARCHAR
    , [updateDateTime]            DATETIME  DEFAULT SYSDATETIME
    
    , CONSTRAINT [pk_service_bind_info_service_instance_bind_id] PRIMARY KEY([service_instance_bind_id])
    , CONSTRAINT [fk_service_bind_info_service_instance_id] FOREIGN KEY (service_instance_id) REFERENCES [service_instance_info] ([service_instance_id])
) REUSE_OID;

CREATE TABLE [docker_node_info] (
    [node_ip]                     VARCHAR   /* PK */
    , [cpu_cores]                 VARCHAR
    , [memory_size]               VARCHAR
    , [disk_size]                 VARCHAR
    , [updateDateTime]            DATETIME  DEFAULT SYSDATETIME
    
    , CONSTRAINT [pk_docker_node_info_node_ip] PRIMARY KEY([node_ip])
) REUSE_OID;

CREATE TABLE [docker_container_info] (
    [container_id]                VARCHAR   /* PK */
    , [container_name]            VARCHAR   /* FK */
    , [container_ip]              VARCHAR
    , [node_ip]                   VARCHAR   /* FK */
    , [database_name]             VARCHAR
    , [updateDateTime]            DATETIME  DEFAULT SYSDATETIME
    
    , CONSTRAINT [pk_docker_container_info_container_id] PRIMARY KEY([container_id])
    , CONSTRAINT [fk_docker_container_info_container_name] FOREIGN KEY (container_name) REFERENCES [service_instance_info] ([service_instance_id])
    , CONSTRAINT [fk_docker_container_info_node_ip] FOREIGN KEY (node_ip) REFERENCES [docker_node_info] ([node_ip])
) REUSE_OID;

CREATE TABLE [docker_service_info] (
    [service_port]                VARCHAR   /* PK */
    , [service_instance_id]       VARCHAR   /* FK */
    , [purpose]                   VARCHAR
    , [updateDateTime]            DATETIME  DEFAULT SYSDATETIME
    
    , CONSTRAINT [pk_docker_service_info_port] PRIMARY KEY([service_port])
    , CONSTRAINT [fk_docker_service_info_service_instance_id] FOREIGN KEY (service_instance_id) REFERENCES [service_instance_info] ([service_instance_id])
) REUSE_OID;

CREATE VIEW [docker_service_info_in_use] AS
SELECT * FROM [docker_service_info] WHERE [service_instance_id] IS NOT NULL;