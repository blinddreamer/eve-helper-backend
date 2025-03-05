package com.example.pandatribe.feign;

import feign.Logger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EveHelperLogger extends Logger {


    @Override
    protected void log(String configKey, String format, Object... params) {
        log.info("[Feign Log] " + String.format(methodTag(configKey) + format, params));
    }
}
