FROM eclipse-temurin:21
RUN mkdir -p /experiment /data
VOLUME "/data"
ENV DATA_DIR=/data
ENV OWNER=1000:1000
WORKDIR /experiment
COPY effects effects
COPY gradle gradle
COPY src src
COPY docker docker
COPY *.kts ./
COPY *.properties ./
COPY gradlew* ./
RUN sed -i '/alias(libs.plugins.gitSemVer)/d' build.gradle.kts
RUN CI=true ./gradlew runGossipBatch
RUN rm -rf data
RUN ./gradlew --stop
CMD export OUTPUT_DIR=$DATA_DIR/$(date +%Y-%m-%d-%H-%M-%S)-$(hostname) && \
    mkdir -p $OUTPUT_DIR && \
    ./gradlew runGossipBatch | tee $OUTPUT_DIR/output.log && \
    chown -R $OWNER $DATA_DIR