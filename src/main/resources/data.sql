INSERT INTO ITEM(id, auction_end, name, description, imperialweight)
VALUES (1, '2099-10-10', 'Some name', 'Java: A Detailed Approach to Practical Coding (Step-By-Step Java Book 2)', 2);

INSERT INTO BID(id, item_id, amount)
VALUES (1, 1, 100);
INSERT INTO BID(id, item_id, amount)
VALUES (2, 1, 200);
INSERT INTO BID(id, item_id, amount)
VALUES (3, 1, 300);

INSERT INTO USERS(id, street, zipcode, city, country)
VALUES (1, 'Wall Street', '44542', 'New York', 'USA');
INSERT INTO USERS(id, street, zipcode, city, country)
VALUES (2, 'Krasnay Street', '65444', 'Moscow', 'Russia');