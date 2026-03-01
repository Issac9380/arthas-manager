package com.arthasmanager.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JavaProcessInfo {
    private int pid;
    private String mainClass;
    private String args;
}
