# ============================================================
# siam-server · 微信云托管部署镜像（多阶段构建）
# 见 04-deploy-cost/mvp-migration/01
# 构建上下文 = siam-server 仓库根目录（.dockerignore 已排除前端/无关模块）
# ============================================================

# ---------- 构建阶段：JDK8 + Maven（阿里云镜像加速） ----------
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build

# 先写 settings.xml 再拷源码，最大化利用层缓存
RUN mkdir -p /root/.m2 && printf '<?xml version="1.0" encoding="UTF-8"?>\n\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">\n\
  <mirrors>\n\
    <mirror>\n\
      <id>aliyun-public</id>\n\
      <mirrorOf>central</mirrorOf>\n\
      <url>https://maven.aliyun.com/repository/public</url>\n\
    </mirror>\n\
  </mirrors>\n\
</settings>\n' > /root/.m2/settings.xml

# 只拷 pom 先下依赖（依赖层缓存）
# 注意：siam-weixin 的两个子模块 pom、siam-monitor 的 pom 都必须拷——
# 父 pom <modules> 声明了它们，缺了 go-offline 缓存层会报 Child module 错（不影响产物但浪费缓存）
COPY pom.xml ./
COPY siam-common/pom.xml ./siam-common/
COPY siam-weixin/pom.xml ./siam-weixin/
COPY siam-weixin/weixin-basic/pom.xml ./siam-weixin/weixin-basic/
COPY siam-weixin/weixin-pay/pom.xml ./siam-weixin/weixin-pay/
COPY siam-system/pom.xml ./siam-system/
COPY siam-system/system-api/pom.xml ./siam-system/system-api/
COPY siam-system/system-provider/pom.xml ./siam-system/system-provider/
COPY siam-monitor/pom.xml ./siam-monitor/
RUN mvn -q -pl siam-system/system-provider -am dependency:go-offline -DskipTests || true

# 再拷源码做全量打包（monitor 只为 pom 聚合存在，-pl 不会构建它）
COPY siam-common ./siam-common
COPY siam-weixin ./siam-weixin
COPY siam-system ./siam-system
COPY siam-monitor ./siam-monitor
RUN mvn -q -pl siam-system/system-provider -am package -DskipTests \
 && cp siam-system/system-provider/target/siam-server.jar /build/app.jar

# ---------- 运行阶段：JRE8 ----------
# 云端拉取失败时的备选：ccr.ccs.tencentyun.com/library/openjdk:8-jre
FROM eclipse-temurin:8-jre
# 桌码图服务端合成（Java2D 绘制桌号文字）依赖 fontconfig + 基础字体；缺失会导致 AWT 画字抛错
RUN apt-get update \
 && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/*
# SPRING_PROFILES_ACTIVE 默认 mvp：容器开箱即用；控制台环境变量可覆盖
# （01:5x 事故复盘：未配该变量时容器以 local profile 启动，数据源指向容器内 127.0.0.1，假健康）
ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=mvp \
    JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+HeapDumpOnOutOfMemoryError -Duser.timezone=Asia/Shanghai -Djava.awt.headless=true"
WORKDIR /app
COPY --from=builder /build/app.jar /app/app.jar
EXPOSE 9200
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
