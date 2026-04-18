package com.maidc.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTask {

    private String tableName;
    private String sourceFile;
    private String prefix;           // o3_ or o4_
    private List<String> csvColumns;
    private boolean largeTable;
    private long fileSizeBytes;
}
