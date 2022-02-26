package com.mochijump.reviewer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("com.mochijump.reviewer")
@Data
public class ReviewerProperties {
    private String currencyString = "USD";
    //TODO lots we can do here, I want to use config to drive using different kinds of readers
}
