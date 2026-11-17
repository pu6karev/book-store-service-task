-- V3: two fields added after the first version

-- clients: a blocked client cannot sign in; existing clients become not blocked
ALTER TABLE clients ADD COLUMN blocked BOOLEAN DEFAULT FALSE NOT NULL;

-- book_items: the price of one copy at the moment of the order.
-- Add the column, fill it for the existing rows with the current price of the book, then forbid NULL.
ALTER TABLE book_items ADD COLUMN unit_price NUMERIC(38, 2);
UPDATE book_items SET unit_price = (SELECT b.price FROM books b WHERE b.id = book_items.book_id);
ALTER TABLE book_items ALTER COLUMN unit_price SET NOT NULL;