﻿-- Processing components visibility

INSERT INTO tao.component_visibility (visibility) VALUES ('SYSTEM');

INSERT INTO tao.component_visibility (visibility) VALUES ('USER');

INSERT INTO tao.component_visibility (visibility) VALUES ('CONTRIBUTOR');



-- Data formats

INSERT INTO tao.data_format (type) VALUES ('RASTER');

INSERT INTO tao.data_format (type) VALUES ('VECTOR');



-- Data Sources types

INSERT INTO tao.data_source_type (type) VALUES ('LOCAL_DATA_SOURCE');

INSERT INTO tao.data_source_type (type) VALUES ('SCIHUB_SENTINEL_1_DATA_SOURCE');

INSERT INTO tao.data_source_type (type) VALUES ('SCIHUB_SENTINEL_2_DATA_SOURCE');

INSERT INTO tao.data_source_type (type) VALUES ('AWS_SENTINEL_2_DATA_SOURCE');

INSERT INTO tao.data_source_type (type) VALUES ('AWS_LANDSAT_8_DATA_SOURCE');



-- Data types

INSERT INTO tao.data_type (type) VALUES ('String');

INSERT INTO tao.data_type (type) VALUES ('Integer');

INSERT INTO tao.data_type (type) VALUES ('Double');

INSERT INTO tao.data_type (type) VALUES ('Short');

INSERT INTO tao.data_type (type) VALUES ('Date');

INSERT INTO tao.data_type (type) VALUES ('Polygon2D');



-- Operation source

INSERT INTO tao.operation_source (source) VALUES ('USER');

INSERT INTO tao.operation_source (source) VALUES ('SYSTEM');



-- Orbit direction

INSERT INTO tao.orbit_direction (direction) VALUES ('ASCENDING');

INSERT INTO tao.orbit_direction (direction) VALUES ('DESCENDING');



-- Pixel types

INSERT INTO tao.pixel_type (type) VALUES ('UINT8');

INSERT INTO tao.pixel_type (type) VALUES ('INT8');

INSERT INTO tao.pixel_type (type) VALUES ('UINT16');

INSERT INTO tao.pixel_type (type) VALUES ('INT16');

INSERT INTO tao.pixel_type (type) VALUES ('UINT32');

INSERT INTO tao.pixel_type (type) VALUES ('INT32');

INSERT INTO tao.pixel_type (type) VALUES ('FLOAT32');

INSERT INTO tao.pixel_type (type) VALUES ('FLOAT64');


-- Sensor types

INSERT INTO tao.sensor_type (type) VALUES ('OPTICAL');

INSERT INTO tao.sensor_type (type) VALUES ('RADAR');

INSERT INTO tao.sensor_type (type) VALUES ('ALTIMETRIC');

INSERT INTO tao.sensor_type (type) VALUES ('ATMOSPHERIC');


-- Data Query parameters
-- common parameters
INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (1, 'platformName');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (5, 'beginPosition');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (5, 'endPosition');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (6, 'footprint');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (1, 'productType');

-- Sentinel-1 parameters
INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (1, 'polarisationMode');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (1, 'sensorOperationalMode');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (1, 'relativeOrbitNumber');

-- Sentinel-2 parameters
INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (3, 'cloudcoverpercentage');

INSERT INTO tao.query_parameter (data_type_id, name)
VALUES (4, 'relativeOrbitNumber');



-- Workflow graph visibility

INSERT INTO tao.workflow_graph_visibility (visibility) VALUES ('PRIVATE');

INSERT INTO tao.workflow_graph_visibility (visibility) VALUES ('PUBLIC');

