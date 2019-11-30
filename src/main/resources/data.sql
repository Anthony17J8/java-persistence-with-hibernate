INSERT INTO ITEM(id, auction_end, name, description)
VALUES (1, '2099-10-10', 'Some name', 'Java: A Detailed Approach to Practical Coding (Step-By-Step Java Book 2)');

INSERT INTO BID(id, item_id, amount)
VALUES (1, 1, 100);
INSERT INTO BID(id, item_id, amount)
VALUES (2, 1, 200);
INSERT INTO BID(id, item_id, amount)
VALUES (3, 1, 300);