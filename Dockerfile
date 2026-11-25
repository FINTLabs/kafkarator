FROM gradle:7.5-jdk17@sha256:deb7b59256192aed1f463ab5e698c509cc77ce5d4e421d8b89b5d99eb0b24538 AS builder

USER gradle
WORKDIR /home/gradle
COPY --chown=gradle:gradle . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java17:nonroot@sha256:cbbf140705ea2bd2157840d713ba54fe3435e84223aa450c6a8c2fec69fd8409

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"

WORKDIR /data
COPY --from=builder --chown=0:0 --chmod=0444 \
    /home/gradle/build/libs/*.jar /data/app.jar

CMD ["/data/app.jar"] 
