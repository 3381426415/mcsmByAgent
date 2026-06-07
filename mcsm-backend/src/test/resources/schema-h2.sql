-- H2 测试数据库 Schema

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    baned BOOLEAN DEFAULT FALSE,
    creat_time VARCHAR(255),
    email VARCHAR(255),
    bind_id VARCHAR(255),
    money BIGINT DEFAULT 0,
    ban_publish BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(255),
    role_description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS permission (
    perm_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT,
    role_id BIGINT
);

CREATE TABLE IF NOT EXISTS role_permission (
    role_id BIGINT,
    perm_id INT
);

CREATE TABLE IF NOT EXISTS mcsm_announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    content TEXT,
    type INT,
    is_published INT DEFAULT 0,
    server_ids VARCHAR(1000),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

CREATE TABLE IF NOT EXISTS mcsm_admin_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_name VARCHAR(255),
    operator_ip VARCHAR(50),
    module VARCHAR(255),
    action VARCHAR(255),
    description TEXT,
    method_name VARCHAR(255),
    request_params TEXT,
    response_result TEXT,
    status INT DEFAULT 1,
    error_msg TEXT,
    execute_time BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mcsm_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    type VARCHAR(50),
    title VARCHAR(255),
    content TEXT,
    is_read INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mcsm_market_goods (
    id VARCHAR(255) PRIMARY KEY,
    seller_id VARCHAR(255),
    item_key VARCHAR(255),
    display_name VARCHAR(255),
    nbt_data TEXT,
    price INT,
    amount INT DEFAULT 1,
    status INT DEFAULT 0,
    creat_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mcsm_order_record (
    id VARCHAR(255) PRIMARY KEY,
    goods_id VARCHAR(255),
    seller_id VARCHAR(255),
    buyer_id VARCHAR(255),
    item_key VARCHAR(255),
    display_name VARCHAR(255),
    nbt_data TEXT,
    final_price INT,
    fee INT DEFAULT 0,
    delivery_status INT DEFAULT 0,
    complete_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mcsm_redeem_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) UNIQUE,
    amount INT,
    status INT DEFAULT 0,
    used_by BIGINT,
    used_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gameplayer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(255),
    nickname VARCHAR(255),
    is_online INT DEFAULT 0,
    money BIGINT DEFAULT 0,
    last_played BIGINT DEFAULT 0,
    server_id VARCHAR(255)
);
