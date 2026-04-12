-- EVE SDE: invTypeMaterials (reprocessing yields)
-- Populate INSERT rows from your SDE dump (same process as R__import_invTypes.sql)

DROP TABLE IF EXISTS `invTypeMaterials`;
CREATE TABLE `invTypeMaterials` (
  `typeID`         int(11) NOT NULL,
  `materialTypeID` int(11) NOT NULL,
  `quantity`       int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`typeID`, `materialTypeID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- INSERT rows from SDE dump go here
