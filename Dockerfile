FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .

RUN javac -d out -sourcepath src $(find src -name '*.java')

EXPOSE 8080

CMD ["java", "-cp", "out", "com.hotel.oop.HotelServerApp"]
