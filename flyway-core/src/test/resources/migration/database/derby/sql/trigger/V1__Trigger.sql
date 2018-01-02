--
-- Copyright 2010-2018 Boxfuse GmbH
--
-- INTERNAL RELEASE. ALL RIGHTS RESERVED.
--
-- Must
-- be
-- exactly
-- 13 lines
-- to match
-- community
-- edition
-- license
-- length.
--

CREATE TABLE TEST (
ID INTEGER NOT NULL,
NAME VARCHAR(128) NOT NULL,
PRIMARY KEY (ID)
);

CREATE TABLE TEST_REFERENCE (
REF_ID INTEGER NOT NULL,
TEST_ID INTEGER NOT NULL,
PRIMARY KEY (REF_ID),
CONSTRAINT FK_TEST_ID FOREIGN KEY (TEST_ID) REFERENCES TEST (ID)
);

CREATE TRIGGER TRG_Del_Test AFTER DELETE ON TEST_REFERENCE
REFERENCING OLD AS EXISTING
FOR EACH ROW MODE DB2SQL
DELETE FROM TEST
WHERE ID = EXISTING.TEST_ID
;

INSERT INTO TEST VALUES (1, 'Test trigger');
INSERT INTO TEST_REFERENCE VALUES (1, 1);