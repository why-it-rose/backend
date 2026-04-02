CREATE TABLE IF NOT EXISTS market_bottom_bar_items (
    id VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    shcode VARCHAR(20) NOT NULL,
    info_url VARCHAR(500) NOT NULL,
    display_order INT NOT NULL,
    current_price BIGINT NOT NULL DEFAULT 0,
    price_change BIGINT NOT NULL DEFAULT 0,
    change_rate DOUBLE NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_market_bottom_bar_items PRIMARY KEY (id),
    CONSTRAINT uq_market_bottom_bar_items_shcode UNIQUE (shcode)
);

CREATE INDEX idx_market_bottom_bar_items_display_order
    ON market_bottom_bar_items (display_order);

INSERT INTO market_bottom_bar_items (id, label, shcode, info_url, display_order)
VALUES
    ('sol-200tr', 'SOL 200TR', '295040', 'https://www.soletf.co.kr/ko/fund/etf/210734', 1),
    ('sol-kosdaq150', 'SOL 코스닥150', '450910', 'https://www.soletf.co.kr/ko/fund/etf/210961', 2),
    ('sol-ai-sobujang', 'SOL AI반도체소부장', '455850', 'https://www.soletf.co.kr/ko/fund/etf/210980', 3),
    ('sol-korea-dividend', 'SOL 코리아고배당', '0105E0', 'https://www.soletf.co.kr/ko/fund/etf/211097', 4),
    ('sol-smr', 'SOL 한국원자력SMR', '0092B0', 'https://www.soletf.co.kr/ko/fund/etf/211096', 5),
    ('sol-ai-top2plus', 'SOL AI반도체TOP2플러스', '0167A0', 'https://www.soletf.co.kr/ko/fund/etf/211106', 6)
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    shcode = VALUES(shcode),
    info_url = VALUES(info_url),
    display_order = VALUES(display_order),
    updated_at = CURRENT_TIMESTAMP(6);
