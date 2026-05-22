ALTER TABLE deck
    ADD COLUMN shards     INT,
    ADD COLUMN difficulty VARCHAR(32),
    ADD COLUMN tags       TEXT[];

WITH cost(rarity, c) AS (
    VALUES ('Common', 60), ('Uncommon', 180), ('Rare', 420),
           ('Epic', 1260), ('Mythic', 1940)
),
per_card AS (
    SELECT dc.deck_id,
           dc.amount * (
               CASE
                   WHEN ocd.card_rarity IS NULL          THEN COALESCE(c_cur.c, 0)
                   WHEN ocd.card_rarity = cd.card_rarity THEN COALESCE(c_cur.c, 0) * 2
                   ELSE COALESCE(c_orig.c, 0) + COALESCE(c_cur.c, 0)
               END
           ) AS sub
    FROM deck_card dc
    JOIN card_detail cd       ON cd.card_id = dc.card_id
    LEFT JOIN card_detail ocd ON ocd.card_id = LOWER(cd.upgraded_from)
    LEFT JOIN cost c_cur      ON c_cur.rarity  = cd.card_rarity
    LEFT JOIN cost c_orig     ON c_orig.rarity = ocd.card_rarity
)
UPDATE deck d
SET shards = COALESCE((SELECT SUM(sub) FROM per_card pc WHERE pc.deck_id = d.deck_id), 0);
