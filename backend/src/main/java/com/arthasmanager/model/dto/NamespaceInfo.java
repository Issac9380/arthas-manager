package com.arthasmanager.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NamespaceInfo {
    private String name;
    private String status;
}
