package core.log.test;

import core.log.LoggerHelper;

public class LogTest {

    public static void main(String[] args) {
        LoggerHelper.logger.debug("shuai1 +++++++++++++++++++++++++++++++++++++debug");
        LoggerHelper.logger.warn("shuai2 +++++++++++++++++++++++++++++++++++++warn");
        LoggerHelper.logger.info("shuai3 +++++++++++++++++++++++++++++++++++++info");
        LoggerHelper.logger.error("shuai4 +++++++++++++++++++++++++++++++++++++error");
    }

}