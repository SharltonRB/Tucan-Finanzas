# Imagen de la API de Tucan, en dos etapas.
#
# La primera etapa necesita Maven, el JDK completo y el codigo fuente. Nada de
# eso hace falta para ejecutar, y todo eso pesa. Por eso la segunda etapa parte
# de una imagen limpia con solo el JRE y copia unicamente el .jar: lo que no se
# copia, no existe en la imagen final. La diferencia son unos 800 MB contra
# unos 200 MB, y menos cosas instaladas es tambien menos superficie de ataque.

# ----------------------------------------------------------------------------
# Etapa 1: construir el .jar
# ----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS construccion

WORKDIR /build

# El pom viaja solo y ANTES que el codigo, y no es un capricho de orden: Docker
# cachea cada instruccion y descarta la cache desde la primera que cambie. El
# codigo cambia todos los dias y las dependencias casi nunca, asi que con este
# orden un build normal reaprovecha la descarga entera. Al reves, cada cambio de
# una linea de Java volveria a bajar medio Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

# Los tests ya corrieron fuera del contenedor. Repetirlos aqui solo alargaria
# cada build.
RUN mvn -B clean package -DskipTests

# ----------------------------------------------------------------------------
# Etapa 2: armar un Java a medida
# ----------------------------------------------------------------------------
# El JRE oficial de Temurin pesa 162 MB y arrastra otros 38 MB de paquetes que
# esta API no usa nunca: tipografias, gnupg, coreutils. Con el puesto, la imagen
# final daba 336 MB y el limite del ticket son 250 MB.
#
# jlink arma un runtime con los modulos que se le pidan y nada mas. Los de abajo
# no son una lista copiada: cada uno esta porque algo concreto lo necesita, y si
# falta uno la aplicacion no avisa al construir, revienta en caliente la primera
# vez que pasa por ese camino.
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS runtime

RUN "$JAVA_HOME/bin/jlink" \
      --add-modules \
java.base,\
java.logging,\
java.naming,\
java.management,\
java.instrument,\
java.desktop,\
java.sql,\
java.xml,\
java.xml.crypto,\
java.net.http,\
java.prefs,\
java.rmi,\
java.scripting,\
java.security.jgss,\
java.security.sasl,\
jdk.crypto.ec,\
jdk.crypto.cryptoki,\
jdk.unsupported,\
jdk.jfr,\
jdk.management,\
jdk.naming.dns,\
jdk.zipfs \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=zip-6 \
      --output /javaruntime

# ----------------------------------------------------------------------------
# Etapa 3: ejecutar
# ----------------------------------------------------------------------------
# Alpine pelado. Lo unico que se le agrega es el runtime de la etapa anterior y
# el .jar de la primera.
FROM alpine:3.21 AS ejecucion

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=runtime /javaruntime ${JAVA_HOME}

WORKDIR /app

# Sin esto el proceso corre como root dentro del contenedor. No hay ninguna
# razon para que la API pueda escribir en el sistema de archivos de la imagen.
RUN addgroup -S tucan && adduser -S tucan -G tucan

COPY --from=construccion --chown=tucan:tucan /build/target/*.jar app.jar

USER tucan

# Documental: EXPOSE no publica nada, lo hace el -p de docker run. Sirve para
# que quien lea el archivo sepa por donde escucha.
EXPOSE 8080

# ENTRYPOINT y no CMD porque esto no es un valor por omision que convenga
# reemplazar: el contenedor ES esta aplicacion. Con CMD, un argumento suelto en
# docker run reemplazaria el comando entero y arrancaria otra cosa.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
