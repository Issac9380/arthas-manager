package com.arthasmanager.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PodInfo {
    private String name;
    private String namespace;
    private String status;
    private String podIP;
    private String nodeName;
    private List<ContainerInfo> containers;
}
