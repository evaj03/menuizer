ALTER TABLE recipes
    ADD COLUMN recipe_type VARCHAR(10) DEFAULT 'VEGETABLE';

UPDATE recipes
SET recipe_type = 'VEGETABLE'
WHERE recipe_type IS NULL;

ALTER TABLE recipes
    ALTER COLUMN recipe_type SET NOT NULL,
    ALTER COLUMN recipe_type DROP DEFAULT,
    ADD CONSTRAINT recipes_recipe_type_allowed
        CHECK (recipe_type IN ('FISH', 'MEAT', 'VEGETABLE'));