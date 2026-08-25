-- ---------------------------------------------------------------------------
-- V3: product reviews. A customer may leave at most one review per product,
-- enforced by uk_reviews_product_customer. That unique key also indexes
-- product_id as its leftmost column, so the per-product listing needs no
-- separate index.
-- ---------------------------------------------------------------------------

-- reviews
CREATE TABLE reviews (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    product_id  BIGINT   NOT NULL,
    customer_id BIGINT   NOT NULL,
    rating      INT      NOT NULL,
    comment     TEXT,
    created_at  DATETIME,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_product_customer (product_id, customer_id),
    CONSTRAINT fk_reviews_product  FOREIGN KEY (product_id)  REFERENCES products  (id),
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

-- indexes
CREATE INDEX idx_reviews_customer ON reviews (customer_id);
