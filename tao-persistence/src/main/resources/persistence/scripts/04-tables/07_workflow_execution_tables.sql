﻿-------------------------------------------------------------------------------
-- table: workflow_graph_visibility
DROP TABLE IF EXISTS tao.workflow_graph_visibility CASCADE;

CREATE TABLE tao.workflow_graph_visibility
(
	id integer NOT NULL,
	visibility varchar(250) NOT NULL
);

ALTER TABLE tao.workflow_graph_visibility ADD CONSTRAINT PK_workflow_graph_visibility PRIMARY KEY (id);


-------------------------------------------------------------------------------
-- table: workflow_graph
DROP TABLE IF EXISTS tao.workflow_graph CASCADE;

CREATE TABLE tao.workflow_graph
(
	id bigint NOT NULL,
	name varchar(250) NOT NULL,
	created timestamp without time zone NOT NULL,
	user_id integer NOT NULL,
	definition_path varchar(512) NOT NULL,
	visibility_id integer NOT NULL,
	active boolean NOT NULL
);

ALTER TABLE tao.workflow_graph ADD CONSTRAINT PK_workflow PRIMARY KEY (id);

ALTER TABLE tao.workflow_graph ADD CONSTRAINT FK_workflow_graph_visibility
	FOREIGN KEY (visibility_id) REFERENCES tao.workflow_graph_visibility (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE tao.workflow_graph ADD CONSTRAINT FK_workflow_user
	FOREIGN KEY (user_id) REFERENCES tao."user" (id) ON DELETE No Action ON UPDATE No Action;


-------------------------------------------------------------------------------
-- table: graph_node
DROP TABLE IF EXISTS tao.graph_node CASCADE;

CREATE TABLE tao.graph_node
(
	id bigint NOT NULL,
	name varchar(250) NULL,
	workflow_id bigint NOT NULL,
	xCoord real NOT NULL,
	yCoord real NOT NULL,
	origin bigint NULL,
	destination bigint NULL
);

ALTER TABLE tao.graph_node ADD CONSTRAINT PK_graph_node PRIMARY KEY (id);

ALTER TABLE tao.graph_node ADD CONSTRAINT FK_graph_node_workflow_graph
	FOREIGN KEY (workflow_id) REFERENCES tao.workflow_graph (id) ON DELETE No Action ON UPDATE No Action;
	
	
-------------------------------------------------------------------------------
-- table: job_status
DROP TABLE IF EXISTS tao.job_status CASCADE;

CREATE TABLE tao.job_status
(
	id integer NOT NULL,
	status varchar(50) NOT NULL
);

ALTER TABLE tao.job_status ADD CONSTRAINT PK_job_status PRIMARY KEY (id);
	
	
-------------------------------------------------------------------------------
-- table: job
DROP TABLE IF EXISTS tao.job CASCADE;

CREATE TABLE tao.job
(
	id bigint NOT NULL,
	start_time timestamp without time zone NULL,
	end_time timestamp without time zone NULL,
	user_id integer NULL,
	workflow_id bigint NOT NULL,
	job_status_id integer NULL
);

ALTER TABLE tao.job ADD CONSTRAINT PK_job PRIMARY KEY (id);

ALTER TABLE tao.job ADD CONSTRAINT FK_job_job_status
	FOREIGN KEY (job_status_id) REFERENCES tao.job_status (id) ON DELETE No Action ON UPDATE No Action;
	
ALTER TABLE tao.job ADD CONSTRAINT FK_job_user
	FOREIGN KEY (user_id) REFERENCES tao."user" (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE tao.job ADD CONSTRAINT FK_job_workflow
	FOREIGN KEY (workflow_id) REFERENCES tao.workflow_graph (id) ON DELETE No Action ON UPDATE No Action;

		
-------------------------------------------------------------------------------
-- table: execution_node
DROP TABLE IF EXISTS tao.execution_node CASCADE;

CREATE TABLE tao.execution_node
(
	id integer NOT NULL,
	ip_address varchar(50) NOT NULL,
	username varchar(50) NOT NULL,
	password text NOT NULL,
	total_CPU integer NOT NULL,
	total_RAM integer NOT NULL,
	total_HDD integer NOT NULL,
	name varchar(250) NOT NULL,
	description text NULL,
	SSH_key text NULL,
	used_CPU integer NULL,
	used_RAM integer NULL,
	used_HDD integer NULL,
	active boolean NOT NULL
);

ALTER TABLE tao.execution_node ADD CONSTRAINT PK_execution_node PRIMARY KEY (id);


-------------------------------------------------------------------------------
-- task_status
DROP TABLE IF EXISTS tao.task_status CASCADE;

CREATE TABLE tao.task_status
(
	id integer NOT NULL,
	status varchar(50) NOT NULL
);

ALTER TABLE tao.task_status ADD CONSTRAINT PK_task_status PRIMARY KEY (id);


-------------------------------------------------------------------------------
-- table: task
DROP TABLE IF EXISTS tao.task CASCADE;

CREATE TABLE tao.task
(
	id bigint NOT NULL,
	processing_component_id integer NOT NULL,
	graph_node_id bigint NOT NULL,
	start_time timestamp without time zone NULL,
	end_time timestamp without time zone NULL,
	job_id bigint NOT NULL,
	task_status_id integer NOT NULL
);

ALTER TABLE tao.task ADD CONSTRAINT PK_task PRIMARY KEY (id);

ALTER TABLE tao.task ADD CONSTRAINT FK_task_graph_node
	FOREIGN KEY (graph_node_id) REFERENCES tao.graph_node (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE tao.task ADD CONSTRAINT FK_task_job
	FOREIGN KEY (job_id) REFERENCES tao.job (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE tao.task ADD CONSTRAINT FK_task_task_status
	FOREIGN KEY (task_status_id) REFERENCES tao.task_status (id) ON DELETE No Action ON UPDATE No Action;


-------------------------------------------------------------------------------
-- table: task_input
DROP TABLE IF EXISTS tao.task_input CASCADE;

CREATE TABLE tao.task_input
(
	task_id bigint NOT NULL,
	input_name varchar(250) NOT NULL,
	input_value varchar(500) NOT NULL
);

ALTER TABLE tao.task_input ADD CONSTRAINT PK_task_input PRIMARY KEY (task_id, input_name);

ALTER TABLE tao.task_input ADD CONSTRAINT FK_task_input_task
	FOREIGN KEY (task_id) REFERENCES tao.task (id) ON DELETE No Action ON UPDATE No Action;

-------------------------------------------------------------------------------
-- table: task_output
DROP TABLE IF EXISTS tao.task_output CASCADE;

CREATE TABLE tao.task_output
(
	task_id bigint NOT NULL,
	output_name varchar(250) NOT NULL,
	output_value varchar(500) NOT NULL
);

ALTER TABLE tao.task_output ADD CONSTRAINT PK_task_output PRIMARY KEY (task_id, output_name);

ALTER TABLE tao.task_output ADD CONSTRAINT FK_task_output_task
	FOREIGN KEY (task_id) REFERENCES tao.task (id) ON DELETE No Action ON UPDATE No Action;

-------------------------------------------------------------------------------
-- table: task_resources
DROP TABLE IF EXISTS tao.task_resources CASCADE;

CREATE TABLE tao.task_resources
(
	task_id bigint NOT NULL,
	execution_node_id integer NOT NULL,
	used_CPU integer NULL,
	used_RAM integer NULL,
	used_HDD integer NULL
);

ALTER TABLE tao.task_resources ADD CONSTRAINT PK_task_resources PRIMARY KEY (task_id,execution_node_id);

ALTER TABLE tao.task_resources ADD CONSTRAINT FK_task_resources_execution_node
	FOREIGN KEY (execution_node_id) REFERENCES tao.execution_node (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE tao.task_resources ADD CONSTRAINT FK_task_resources_task
	FOREIGN KEY (task_id) REFERENCES tao.task (id) ON DELETE No Action ON UPDATE No Action;

