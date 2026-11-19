# Imagem para publicar o site (Railway, Render, Fly.io...). O banco PostgreSQL fica fora da imagem e é
# informado pelas variáveis de ambiente DB_URL, DB_USERNAME e DB_PASSWORD (veja o README).

# Etapa 1: compila o projeto e gera o .jar.
FROM eclipse-temurin:25-jdk AS compilacao
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Remove quebras de linha do Windows (CRLF), caso a pasta tenha vindo de um computador Windows.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
# Os testes rodam no computador de desenvolvimento e no GitHub, não na publicação.
RUN ./mvnw -B -q package -DskipTests

# Etapa 2: só o Java e o .jar, sem o código-fonte nem o Maven.
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 gift4you
COPY --from=compilacao /app/target/gift-4-you-*.jar app.jar
USER gift4you
# Liga as configurações de produção (application-producao.properties).
ENV SPRING_PROFILES_ACTIVE=producao
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
