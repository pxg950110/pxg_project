package com.maidc.model.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.result.PageResult;
import com.maidc.model.dto.ModelCreateDTO;
import com.maidc.model.service.ModelService;
import com.maidc.model.vo.ModelDetailVO;
import com.maidc.model.vo.ModelVO;
import com.maidc.model.vo.VersionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ModelControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ModelService modelService;

    @InjectMocks
    private ModelController modelController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(modelController).build();
    }

    @Test
    @DisplayName("POST /api/v1/models - valid input returns 200")
    void createModel_validInput_returns200() throws Exception {
        // Arrange
        ModelCreateDTO dto = ModelCreateDTO.builder()
                .modelCode("LLM-001")
                .modelName("Test Model")
                .description("A test model")
                .modelType("LLM")
                .taskType("TEXT_GENERATION")
                .framework("PyTorch")
                .build();

        ModelVO vo = ModelVO.builder()
                .id(1L)
                .modelCode("LLM-001")
                .modelName("Test Model")
                .modelType("LLM")
                .taskType("TEXT_GENERATION")
                .framework("PyTorch")
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(modelService.createModel(any(ModelCreateDTO.class))).thenReturn(vo);

        // Act & Assert
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.modelCode").value("LLM-001"))
                .andExpect(jsonPath("$.data.modelName").value("Test Model"))
                .andExpect(jsonPath("$.data.modelType").value("LLM"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/v1/models - returns page result")
    void listModels_returnsPageResult() throws Exception {
        // Arrange
        ModelVO vo1 = ModelVO.builder()
                .id(1L)
                .modelCode("LLM-001")
                .modelName("Model One")
                .modelType("LLM")
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();
        ModelVO vo2 = ModelVO.builder()
                .id(2L)
                .modelCode("CV-001")
                .modelName("Model Two")
                .modelType("CV")
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .build();

        List<ModelVO> items = List.of(vo1, vo2);
        Page<ModelVO> page = new PageImpl<>(items, PageRequest.of(0, 20), 2);
        PageResult<ModelVO> pageResult = PageResult.of(page);

        when(modelService.listModels(eq(1), eq(20), eq(0L), isNull(), isNull(), isNull()))
                .thenReturn(pageResult);

        // Act & Assert
        mockMvc.perform(get("/api/v1/models")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("orgId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/models/{id} - existing id returns detail")
    void getModel_existingId_returnsDetail() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        VersionVO latestVersion = VersionVO.builder()
                .id(10L)
                .modelId(1L)
                .versionNo("v1.0.0")
                .status("PUBLISHED")
                .createdAt(now)
                .build();

        ModelDetailVO detail = ModelDetailVO.builder()
                .id(1L)
                .modelCode("LLM-001")
                .modelName("Test Model")
                .description("Detailed model description")
                .modelType("LLM")
                .framework("PyTorch")
                .status("PUBLISHED")
                .versionCount(3)
                .latestVersion(latestVersion)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(modelService.getModelDetail(1L)).thenReturn(detail);

        // Act & Assert
        mockMvc.perform(get("/api/v1/models/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.modelCode").value("LLM-001"))
                .andExpect(jsonPath("$.data.modelName").value("Test Model"))
                .andExpect(jsonPath("$.data.description").value("Detailed model description"))
                .andExpect(jsonPath("$.data.modelType").value("LLM"))
                .andExpect(jsonPath("$.data.framework").value("PyTorch"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.versionCount").value(3))
                .andExpect(jsonPath("$.data.latestVersion.versionNo").value("v1.0.0"));
    }

    @Test
    @DisplayName("POST /api/v1/models - missing required fields returns 400")
    void createModel_missingFields_returns400() throws Exception {
        // Send empty body - @Valid should trigger validation errors
        String emptyJson = "{}";

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/models/{id} - non-numeric id returns 400")
    void getModel_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/models/abc"))
                .andExpect(status().isBadRequest());
    }
}
