FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV LOG_PATH=/app/logs

RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app/logs

USER spring

COPY *.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]