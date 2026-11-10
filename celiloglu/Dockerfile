# ==============================================================================
# Mərhələ 1: Layihənin Qurulması (Build Stage)
# ==============================================================================
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Maven Wrapper və layihə konfiqurasiyasını kopyalayırıq
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Windows CRLF xətalarının qarşısını almaq üçün sətir sonluqlarını düzəldirik və icra icazəsi veririk
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Mənbə kodlarını kopyalayırıq
COPY src ./src

# Testləri atlayaraq executable Spring Boot JAR paketini hazırlayırıq (Real DB tələb etmir)
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Mərhələ 2: Tətbiqin İcrası (Runtime Stage)
# ==============================================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Təhlükəsizlik üçün qeyri-root (non-root) istifadəçi və qrup yaradırıq
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Məhsul şəkillərinin saxlanması üçün qovluğu hazırlayırıq və icazələri tənzimləyirik
RUN mkdir -p /app/uploads/products && chown -R spring:spring /app

USER spring:spring

# Qurulmuş executable JAR faylını runtime mərhələsinə kopyalayırıq
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Render və lokal mühit üçün standart port
EXPOSE 8081

# Tətbiqin başladılması
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

