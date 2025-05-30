-- Create carts table
CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create cart_items table
CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX ASYNC IF NOT EXISTS idx_carts_user_id ON carts(user_id);
CREATE INDEX ASYNC IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX ASYNC IF NOT EXISTS idx_cart_items_product_id ON cart_items(product_id);

-- Create unique constraint to prevent duplicate products in the same cart
CREATE UNIQUE INDEX ASYNC IF NOT EXISTS idx_cart_items_cart_product ON cart_items(cart_id, product_id);