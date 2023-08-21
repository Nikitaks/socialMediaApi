# Тестовое задание Social Media API
---

Цель проекта: Разработать RESTful API для социальной медиа платформы, позволяющей пользователям регистрироваться, входить в систему, создавать посты, переписываться, подписываться на других пользователей и получать
свою ленту активности.

#### Используемые инструменты:
1. Spring Boot, Spring Security
2. СУБД MySQL
3. Spring Data JPA + Hibernate
4. Swagger
5. Сборщик проектов `maven`
 
#### Особенности проекта
1. Описание базы данных приведено в `documentation/DatabaseSchema.txt`
2. Оффлайн-swagger документация в формате JSON расположена в `documentation/api-docs.json`.
3. Конфигурационный файл подключения к базе данных MySQL `/src/main/resources/META-INF/persistence.xml`
 
#### Как запустить проект

1. Импортировать maven-проект в IDE.
2. Убедиться, что установленная JDK версии не ниже 1.8.0_181.
3. Собрать проект командой `mvn clean install`.
4. Развернуть базу данных, выполнив следующие действия:
   1. Установить сервер MySQL со стандартными параметрами (userName: root, password:root, serverName: localhost, portNumber: 3306).
   2. Создать базу данных путем выполнения скрипта командой 
         ```
         source <путь к файлу src/main/resources/DatabaseSchemaSQL.txt>
         ```
5. Запустить на исполнение приложение SocialMediaApiApplication.java
6. Перейти на страницу с документацией localhost:8080/swagger-ui/index.html
