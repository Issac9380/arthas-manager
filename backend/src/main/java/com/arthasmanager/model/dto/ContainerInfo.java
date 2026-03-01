package com.arthasmanager.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerInfo {
    private String name;
    private String image;
    /** Whether JDK / Arthas has been deployed into this container */
    private boolean arthasDeployed;
}
