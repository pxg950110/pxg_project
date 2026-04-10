#!/bin/bash
# MAIDC MinIO 初始化脚本
# 创建 4 个存储桶

set -e

MINIO_ALIAS="maidc"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
MINIO_USER="${MINIO_ROOT_USER:-maidc}"
MINIO_PASS="${MINIO_ROOT_PASSWORD:-maidc12345}"

echo "=== MAIDC MinIO 初始化 ==="
echo "Endpoint: ${MINIO_ENDPOINT}"

# 等待 MinIO 就绪
echo "等待 MinIO 服务启动..."
for i in $(seq 1 30); do
    if curl -sf "${MINIO_ENDPOINT}/minio/health/live" > /dev/null 2>&1; then
        echo "MinIO 已就绪"
        break
    fi
    echo "  等待中... ($i/30)"
    sleep 2
done

# 配置 mc 别名
mc alias set ${MINIO_ALIAS} ${MINIO_ENDPOINT} ${MINIO_USER} ${MINIO_PASS}

# 创建存储桶
echo "创建存储桶..."

mc mb --ignore-existing ${MINIO_ALIAS}/maidc-models
echo "  ✓ maidc-models   (模型文件: {org_id}/{model_code}/{version_no}/model.pt)"

mc mb --ignore-existing ${MINIO_ALIAS}/maidc-dicom
echo "  ✓ maidc-dicom    (DICOM影像: {org_id}/{yyyy}/{MM}/{dd}/{accession_no}.dcm)"

mc mb --ignore-existing ${MINIO_ALIAS}/maidc-datasets
echo "  ✓ maidc-datasets (数据集: {org_id}/{project_id}/{dataset_id}/v{version}.parquet)"

mc mb --ignore-existing ${MINIO_ALIAS}/maidc-docs
echo "  ✓ maidc-docs     (文档材料: {org_id}/{approval_id}/{filename})"

echo ""
echo "=== MinIO 初始化完成 ==="
mc ls ${MINIO_ALIAS}/
