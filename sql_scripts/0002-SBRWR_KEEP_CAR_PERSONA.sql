ALTER TABLE `CAR`
	ADD COLUMN `sold_at` DATETIME NULL;

ALTER TABLE `PERSONA`
	ADD COLUMN `deleted_at` DATETIME NULL;

ALTER TABLE `PERSONA`
	ADD COLUMN `prestige` INT NULL DEFAULT '0';