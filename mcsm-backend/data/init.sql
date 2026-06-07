-- MCSM 数据库初始化脚本（SetupCli 专用）
-- 基于真实数据库 schema，保留结构和种子数据

CREATE DATABASE IF NOT EXISTS mcsm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mcsm;

-- ========== 核心表 ==========

CREATE TABLE IF NOT EXISTS `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `money` bigint DEFAULT '0',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `baned` tinyint(1) NOT NULL DEFAULT '0',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `creat_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bind_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ban_publish` tinyint(1) DEFAULT '0' COMMENT '禁止发布：0-允许，1-禁止',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `role` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `permission` (
  `perm_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `user_role` (
  `user_id` int NOT NULL,
  `role_id` int NOT NULL DEFAULT '2'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `role_permission` (
  `role_id` int NOT NULL,
  `perm_id` int NOT NULL,
  PRIMARY KEY (`role_id`,`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ========== 业务表 ==========

CREATE TABLE IF NOT EXISTS `gameplayer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_online` tinyint DEFAULT NULL,
  `money` bigint DEFAULT '0',
  `last_played` bigint DEFAULT NULL,
  `server_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `mcsm_market_goods` (
  `id` varchar(64) NOT NULL COMMENT '交易唯一ID(UUID)',
  `seller_id` varchar(64) NOT NULL COMMENT '卖家玩家UUID',
  `item_key` varchar(128) NOT NULL COMMENT '物品材质',
  `display_name` varchar(100) NOT NULL COMMENT '物品名称',
  `nbt_data` json NOT NULL COMMENT '完整NBT数据',
  `amount` int DEFAULT '1' COMMENT '道具数量',
  `price` int NOT NULL COMMENT '售价',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待售, 1-交易中, 2-已成交, 3-已撤回',
  `creat_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_seller_status` (`seller_id`,`status`),
  FULLTEXT KEY `idx_name_search` (`display_name`) /*!50100 WITH PARSER `ngram` */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCSN个人交易市场表';

CREATE TABLE IF NOT EXISTS `mcsm_order_record` (
  `id` varchar(64) NOT NULL COMMENT '订单流水号',
  `goods_id` varchar(64) NOT NULL COMMENT '关联商品ID',
  `seller_id` varchar(64) NOT NULL COMMENT '卖家UUID',
  `buyer_id` varchar(64) NOT NULL COMMENT '买家UUID',
  `item_key` varchar(128) NOT NULL COMMENT '物品材质',
  `display_name` varchar(100) NOT NULL COMMENT '物品名称',
  `nbt_data` json NOT NULL COMMENT '完整NBT',
  `final_price` int NOT NULL COMMENT '成交价格',
  `fee` int DEFAULT '0' COMMENT '手续费',
  `complete_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成交时间',
  `delivery_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待发货, 1-已发货, 2-发货失败',
  PRIMARY KEY (`id`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_seller` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易订单历史记录表';

CREATE TABLE IF NOT EXISTS `mcsm_pending_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `player_uuid` varchar(36) NOT NULL COMMENT '玩家UUID',
  `item_key` varchar(100) NOT NULL COMMENT '物品ID',
  `amount` int NOT NULL DEFAULT '1' COMMENT '数量',
  `display_name` varchar(100) DEFAULT NULL COMMENT '物品显示名',
  `nbt_data` text COMMENT '完整NBT数据（JSON）',
  `source` varchar(50) DEFAULT NULL COMMENT '来源',
  `source_id` varchar(50) DEFAULT NULL COMMENT '来源ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `claimed` tinyint(1) DEFAULT '0' COMMENT '0-未领取, 1-已领取',
  `claim_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_player_uuid` (`player_uuid`),
  KEY `idx_claimed` (`claimed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='玩家暂存物品表（离线/背包满时使用）';

CREATE TABLE IF NOT EXISTS `mcsm_redeem_code` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL COMMENT '兑换码',
  `amount` int NOT NULL COMMENT '金额',
  `status` tinyint(1) DEFAULT '0' COMMENT '0-未使用, 1-已使用',
  `used_by` bigint DEFAULT NULL COMMENT '使用者用户ID',
  `used_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兑换码表';

CREATE TABLE IF NOT EXISTS `mcsm_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收者用户ID',
  `type` varchar(32) NOT NULL COMMENT '通知类型',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` varchar(500) NOT NULL COMMENT '通知内容',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '0-未读, 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知表';

CREATE TABLE IF NOT EXISTS `mcsm_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `type` tinyint(1) DEFAULT '1' COMMENT '1-网站公告，2-游戏公告，3-双端',
  `is_published` tinyint(1) DEFAULT '0' COMMENT '0-草稿，1-已发布',
  `server_ids` varchar(1000) DEFAULT NULL COMMENT '生效服务器ID列表',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL COMMENT '发布人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告表';

CREATE TABLE IF NOT EXISTS `mcsm_admin_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人账号',
  `operator_ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `action` varchar(50) DEFAULT NULL COMMENT '操作动作',
  `description` text COMMENT '操作描述',
  `method_name` varchar(200) DEFAULT NULL COMMENT '执行方法',
  `request_params` text COMMENT '请求参数',
  `response_result` varchar(500) DEFAULT NULL COMMENT '返回结果',
  `status` tinyint(1) DEFAULT '1' COMMENT '1-成功，0-失败',
  `error_msg` text COMMENT '错误信息',
  `execute_time` bigint DEFAULT NULL COMMENT '执行耗时(ms)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志';

-- ========== Agent 表 ==========

CREATE TABLE IF NOT EXISTS `agent_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `task_state` text,
  `chat_history` longtext,
  `sub_task_states` text,
  `pending_changes` text,
  `confirmed_changes` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `agent_change_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(32) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `server_id` varchar(64) DEFAULT NULL,
  `file_path` varchar(512) DEFAULT NULL,
  `change_key` varchar(256) DEFAULT NULL,
  `old_value` text,
  `new_value` text,
  `reason` text,
  `status` varchar(20) DEFAULT 'SUCCESS',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `agent_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(32) DEFAULT NULL,
  `agent_id` varchar(64) DEFAULT NULL,
  `agent_type` varchar(32) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `operation` varchar(64) DEFAULT NULL,
  `details` text,
  `success` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `agent_error_patterns` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scope` varchar(16) NOT NULL DEFAULT 'GLOBAL',
  `server_id` varchar(64) DEFAULT NULL,
  `plugin` varchar(128) DEFAULT NULL,
  `error_type` varchar(128) DEFAULT NULL,
  `error_message` text,
  `solution` text,
  `summary` varchar(512) DEFAULT NULL,
  `effectiveness` varchar(16) DEFAULT 'PENDING',
  `recurrence_count` int DEFAULT '0',
  `stale` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scope` (`scope`),
  KEY `idx_plugin` (`plugin`),
  KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `agent_ops_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scope` varchar(16) NOT NULL DEFAULT 'GLOBAL',
  `server_id` varchar(64) DEFAULT NULL,
  `category` varchar(64) NOT NULL,
  `subject` varchar(256) DEFAULT NULL,
  `relation` varchar(64) DEFAULT NULL,
  `object` varchar(256) DEFAULT NULL,
  `detail` text,
  `confidence` varchar(16) DEFAULT 'MEDIUM',
  `verified` tinyint(1) DEFAULT '0',
  `stale` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scope` (`scope`),
  KEY `idx_category` (`category`),
  KEY `idx_subject` (`subject`),
  KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `custom_llm_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_id` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `base_url` varchar(512) NOT NULL,
  `models_endpoint` varchar(512) DEFAULT '/models',
  `thinking_field` varchar(64) DEFAULT NULL,
  `content_field` varchar(64) DEFAULT 'content',
  `tool_calls_field` varchar(64) DEFAULT 'tool_calls',
  `delta_content_field` varchar(64) DEFAULT 'content',
  `delta_tool_calls_field` varchar(64) DEFAULT 'tool_calls',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `provider_id` (`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========== 种子数据 ==========

-- 默认角色
INSERT INTO `role` (`role_id`, `role_name`, `role_description`) VALUES
(1, '管理员', ''),
(2, '用户', NULL)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- 权限
INSERT INTO `permission` (`perm_id`, `name`, `description`) VALUES
(1,  'user:market:view',      '查看交易市场'),
(2,  'user:market:buy',       '购买商品'),
(3,  'user:market:sell',      '发布/上架商品'),
(4,  'admin:user',            '用户管理'),
(5,  'admin:role',            '角色管理'),
(6,  'admin:server',          '服务器管理'),
(7,  'admin:player',          '玩家管理'),
(8,  'admin:plugin',          '插件管理'),
(9,  'admin:announcement',    '公告管理'),
(10, 'admin:log',             '操作日志查看'),
(11, 'admin:redeem',          '兑换码管理'),
(12, 'admin:notification',    '通知管理'),
(13, 'admin:market:view',     '查看所有商品'),
(14, 'admin:market:withdraw', '强制下架商品'),
(15, 'admin:market:delete',   '删除商品记录')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 角色-权限关联：管理员拥有全部权限
INSERT INTO `role_permission` (`role_id`, `perm_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),(1,15),
(2,1),(2,2),(2,3)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- 默认管理员账号 (admin / admin123456)
INSERT INTO `user` (`id`, `username`, `password`, `email`, `baned`, `money`) VALUES
(1, 'admin', '$2a$10$QrIqDFhxxXbS4V24BaF3oeDTgMxe2YMa/tCROW9rrJ26nTZYF6gie', '', 0, 0)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- 管理员角色关联
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);
