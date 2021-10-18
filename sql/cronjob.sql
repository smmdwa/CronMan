/*
Navicat MySQL Data Transfer

Source Server         : 192.168.213.130
Source Server Version : 80020
Source Host           : 192.168.213.130:3306
Source Database       : cronjob

Target Server Type    : MYSQL
Target Server Version : 80020
File Encoding         : 65001

Date: 2021-10-18 15:23:54
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for job
-- ----------------------------
DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_id` bigint DEFAULT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `pids` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `class_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `method_name` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `parameter_types` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `args` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `cron_expr` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `shard_num` int DEFAULT NULL,
  `transfer` tinyint DEFAULT NULL,
  `restart` tinyint DEFAULT NULL,
  `policy` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `next_start_time` bigint DEFAULT NULL,
  `status` int DEFAULT NULL,
  `exec_times` int DEFAULT NULL,
  `job_type` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `enable` int DEFAULT NULL,
  `shell` mediumtext COLLATE utf8_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
-- Records of job
-- ----------------------------
INSERT INTO `job` VALUES ('1', '11', 'first_job', null, 'com.example.demo.service.test1', 'hello55', 'java.lang.String;java.lang.Integer', 'dyw;18', '10 * * 7-31 * ?', '1', '1', '1', 'random', '2021-09-30 18:30:23', '1634476390000', '1', '411', 'java_normal', '2021-10-17 15:14:21', '1', null);
INSERT INTO `job` VALUES ('2', '12', 'second_job', null, 'com.distribute.executor.service.helloService', 'hello', 'java.lang.String;java.lang.Integer', 'dyw;18', '10 * * 7-31 * ?', '1', '1', '1', 'random', '2021-10-04 15:55:27', '1634476390000', '0', '19', 'java_normal', '2021-10-17 15:10:21', '1', null);
INSERT INTO `job` VALUES ('5', '13', 'third_job', '11;12', 'com.distribute.executor.service.helloService', 'hello_passive', 'java.lang.String;java.lang.Integer', 'dyw;19', '10 * * 7-31 * ?', '1', '1', '1', 'random', '2021-09-29 17:52:04', '1634476390000', '3', '15', 'java_passive', '2021-10-17 15:10:22', '1', null);
INSERT INTO `job` VALUES ('6', '14', 'passive', '0', '2', null, null, null, '10 * * 7-31 * ?', '1', null, null, null, null, '1634474390000', '2', '1', 'shell_passive', null, '1', null);
INSERT INTO `job` VALUES ('7', '15', 'washUp', '13', null, null, null, null, '10 * * 7-31 * ?', '1', '0', '0', null, null, '1634476290000', '2', '0', 'java_normal', null, '1', null);
INSERT INTO `job` VALUES ('8', '16', 'cleanUp', '15;17', null, null, null, null, '15 * * 7-31 * ?', '2', null, null, null, null, '1634476390000', '0', '1', 'java_normal', null, '1', null);
INSERT INTO `job` VALUES ('9', '17', 'backUp', null, null, null, null, null, '10 * * 7-31 * ?', '2', '0', '0', null, null, '0', '0', '0', 'java_normal', null, '1', null);
INSERT INTO `job` VALUES ('10', '18', 'sleep', '17', null, null, null, null, '5 * * 7-31 * ?', '2', null, null, null, null, '1634476360000', '0', '1', 'java_normal', null, '0', null);
INSERT INTO `job` VALUES ('14', '19', 'single', null, null, null, null, null, '5 * * 7-31 * ?', '1', '0', '0', null, null, '1634476360000', '0', '0', 'java_normal', null, '1', null);

-- ----------------------------
-- Table structure for jobDetail
-- ----------------------------
DROP TABLE IF EXISTS `jobDetail`;
CREATE TABLE `jobDetail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_id` bigint DEFAULT NULL,
  `exec_id` int DEFAULT NULL,
  `code` int DEFAULT NULL,
  `executor_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `shard_index` int DEFAULT NULL,
  `shard_total` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13092 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
-- Records of jobDetail
-- ----------------------------
INSERT INTO `jobDetail` VALUES ('1', '1', '0', '300', 'executor_fffffgg', '0', '2');
INSERT INTO `jobDetail` VALUES ('2', '1', '0', '400', 'executor_2', '1', '2');
INSERT INTO `jobDetail` VALUES ('3', '33', '0', '300', 'executor_fffffgg', '0', '1');
INSERT INTO `jobDetail` VALUES ('6', '123', '0', '0', 'executor_dd', '0', '1');
INSERT INTO `jobDetail` VALUES ('7', '124', '0', '0', 'executor_dd', '0', '1');

-- ----------------------------
-- Table structure for jobLock
-- ----------------------------
DROP TABLE IF EXISTS `jobLock`;
CREATE TABLE `jobLock` (
  `mylock` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
-- Records of jobLock
-- ----------------------------
INSERT INTO `jobLock` VALUES ('lock');
