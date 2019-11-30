INSERT INTO ITEM(id, auction_end, name, start_price)
VALUES (1, '2099-10-10', 'Some name', 0);

INSERT INTO BID(id, item_id)
VALUES (1, 1);
INSERT INTO BID(id, item_id)
VALUES (2, 1);
INSERT INTO BID(id, item_id)
VALUES (3, 1);