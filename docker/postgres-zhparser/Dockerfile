FROM postgres:15

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    postgresql-server-dev-15 \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Install scws (Simple Chinese Word Segmentation)
RUN wget -q -O /tmp/scws-1.2.3.tar.bz2 https://github.com/hightman/scws/releases/download/v1.2.3/scws-1.2.3.tar.bz2 \
    && tar xjf /tmp/scws-1.2.3.tar.bz2 -C /tmp \
    && cd /tmp/scws-1.2.3 \
    && ./configure \
    && make \
    && make install \
    && rm -rf /tmp/scws-1.2.3*

# Install zhparser
RUN git clone https://github.com/amutu/zhparser.git /tmp/zhparser \
    && cd /tmp/zhparser \
    && SCWS_HOME=/usr/local make \
    && SCWS_HOME=/usr/local make install \
    && rm -rf /tmp/zhparser