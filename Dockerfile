# Copyright (C) 2020 -  Juergen Zimmermann, Hochschule Karlsruhe
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# -----------------------------------------------------------------------------------------------
# D o c k e r   I m a g e s
# -----------------------------------------------------------------------------------------------

# alpine:3.12.0                   6 MB
# debian:buster-slim             25 MB
# openjdk:16-jdk-alpine3.12     190 MB
# openjdk:11-jre-slim-buster     70 MB

# -----------------------------------------------------------------------------------------------
# S e h r   e i n f a c h :
# -----------------------------------------------------------------------------------------------

#FROM openjdk:16-jdk-alpine3.12
# "working directory" fuer die Docker-Kommandos RUN, CMD, ENTRYPOINT, COPY und ADD
#WORKDIR application
#ARG JAR_FILE=build/libs/kunde-*.jar
#COPY ${JAR_FILE} application.jar
#ENTRYPOINT ["java", "-jar", "application.jar"]

# -----------------------------------------------------------------------------------------------
# B e s s e r :   L a y e r e d   I m a g e   m i t   C l o u d   N a t i v e   B u i l d p a c k
# -----------------------------------------------------------------------------------------------

FROM openjdk:11-jdk-slim-buster as builder
WORKDIR source
ARG JAR_FILE=build/libs/labor-*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract
#
FROM openjdk:11-jre-slim-buster
WORKDIR application
COPY --from=builder source/dependencies/ ./
COPY --from=builder source/spring-boot-loader/ ./
COPY --from=builder source/snapshot-dependencies/ ./
COPY --from=builder source/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
