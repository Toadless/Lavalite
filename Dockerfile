FROM openjdk:15-jdk

WORKDIR /home/snowball/

COPY --chown=gradle . /home/build/

RUN /home/build/gradlew build --no-daemon

COPY /home/build/build/libs/*.jar /snowball/snowball.jar

RUN rm -r /home/build/

ENTRYPOINT ["java", "-jar", "snowball.jar"]