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

INSERT INTO test_user (name) VALUES ( $$Mr. T$$);
INSERT INTO test_user (name) VALUES ( $$Mr. ' Quote$$);
INSERT INTO test_user (name) VALUES ( $$'Mr. Semicolon+Linebreak;
another line'$$);