CREATE INDEX IF NOT EXISTS industryactivityproducts_activityID_IDX USING BTREE ON evesde.industryactivityproducts (activityID);
CREATE INDEX IF NOT EXISTS invtypes_typeName_IDX USING BTREE ON evesde.invtypes (typeName);
CREATE INDEX IF NOT EXISTS mapsolarsystems_solarSystemName_IDX USING BTREE ON evesde.mapsolarsystems (solarSystemName);

