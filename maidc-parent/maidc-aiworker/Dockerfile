FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
# 主依赖（fastapi/uvicorn/celery/pydantic/httpx/numpy/pillow/psycopg-binary）均有官方 wheel，
# 无需 build-essential 编译工具链（debian 源慢时会阻塞构建）
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Install Poetry（使用清华 PyPI 镜像，直连 PyPI 在部分网络不稳定）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple poetry==1.8.2

# Copy dependency files
COPY pyproject.toml poetry.lock* ./

# Install dependencies (no dev, no gpu by default)
# POETRY_REPOSITORIES_PYPI_URL 覆盖隐式 PyPI 源
RUN poetry config virtualenvs.create false && poetry config installer.parallel false && \
    POETRY_REPOSITORIES_PYPI_URL=https://pypi.tuna.tsinghua.edu.cn/simple \
    poetry install --no-interaction --no-ansi --only main

# Copy application code
COPY app/ ./app/

# Create model cache directory
RUN mkdir -p /data/models

EXPOSE 8090

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
