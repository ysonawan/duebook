package com.duebook.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private Long shopId;
    private String entityType;
    private UUID entityId;
    private String action;
    private Long performedById;
    private String performedByName;
    private String oldValue;
    private String newValue;
    private LocalDateTime performedAt;
}

