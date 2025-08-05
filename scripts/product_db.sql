CREATE USER product_user WITH PASSWORD 'product_user';

CREATE DATABASE product_db OWNER product_user;

CREATE TABLE category (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          name VARCHAR(100) NOT NULL UNIQUE,
                          description TEXT,
                          parent_id UUID,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
);

CREATE TABLE product (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name VARCHAR(200) NOT NULL,
                         description TEXT,
                         price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
                         stock INTEGER NOT NULL CHECK (stock >= 0),
                         category_id UUID NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

CREATE TABLE product_image (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               product_id UUID NOT NULL,
                               image_url TEXT NOT NULL,
                               alt_text TEXT,
                               is_primary BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_product
                                   FOREIGN KEY (product_id)
                                       REFERENCES product(id)
                                       ON DELETE CASCADE
);

CREATE TABLE product_review (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                product_id UUID NOT NULL,
                                user_id UUID NOT NULL,
                                rating INTEGER CHECK (rating >= 1 AND rating <= 5),
                                review TEXT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_product_review_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE product_variant (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 product_id UUID NOT NULL,
                                 name TEXT NOT NULL,        -- e.g., "Size", "Color"
                                 value TEXT NOT NULL,       -- e.g., "M", "Red"
                                 price_adjustment NUMERIC(12,2) DEFAULT 0,
                                 stock INTEGER DEFAULT 0,

                                 CONSTRAINT fk_product_variant_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE tag (
                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                     name TEXT NOT NULL UNIQUE
);

CREATE TABLE product_tag (
                             product_id UUID NOT NULL,
                             tag_id UUID NOT NULL,
                             PRIMARY KEY (product_id, tag_id),
                             FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
                             FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);

CREATE TABLE product_discount (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  product_id UUID NOT NULL,
                                  discount_percentage NUMERIC(5,2) NOT NULL,
                                  start_date TIMESTAMP,
                                  end_date TIMESTAMP,
                                  CONSTRAINT fk_product_discount_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE inventory_log (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               product_id UUID NOT NULL,
                               change_amount INTEGER NOT NULL,
                               reason TEXT,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_inventory_log_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE product_bundle (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                name TEXT NOT NULL,
                                description TEXT,
                                total_price NUMERIC(12,2),
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_bundle_item (
                                     bundle_id UUID NOT NULL,
                                     product_id UUID NOT NULL,
                                     quantity INTEGER DEFAULT 1,

                                     PRIMARY KEY (bundle_id, product_id),
                                     FOREIGN KEY (bundle_id) REFERENCES product_bundle(id) ON DELETE CASCADE,
                                     FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TYPE question_status AS ENUM ('PENDING', 'ANSWERED', 'REJECTED');

CREATE TABLE product_question (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  product_id UUID NOT NULL,
                                  user_id UUID NOT NULL,
                                  question TEXT NOT NULL,
                                  answer TEXT,
                                  status question_status NOT NULL DEFAULT 'PENDING',
                                  asked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  answered_at TIMESTAMP,

                                  CONSTRAINT fk_product_question_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);