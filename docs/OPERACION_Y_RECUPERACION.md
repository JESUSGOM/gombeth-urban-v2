# Gombeth Urban — Operación y recuperación

## 1. Configuración privada

Las credenciales no deben almacenarse en Git, en documentación pública
ni dentro del JAR.

La configuración privada se encuentra en:

backend/config/application-local.properties

Debe contener la conexión MySQL y la confirmación expresa:

spring.datasource.url=URL_PRIVADA
spring.datasource.username=USUARIO_PRIVADO
spring.datasource.password=CONTRASENA_PRIVADA
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
gombeth.production.confirmed=true

El archivo está ignorado por Git y nunca debe incluirse en ZIP destinados
a terceros.

## 2. Base de datos compartida

La base sepa_1914 es utilizada también por la aplicación anterior.

Gombeth Urban debe arrancarse con los perfiles:

prod,local

La protección ProductionDatabaseGuard exige:

- perfil prod activo;
- confirmación gombeth.production.confirmed=true;
- spring.jpa.hibernate.ddl-auto=none o validate.

No deben ejecutarse pruebas destructivas, borrados masivos ni scripts SQL
experimentales.

## 3. Construcción del frontend

Desde la carpeta frontend:

npm run build

Angular genera los archivos en:

backend/src/main/resources/static

Los avisos de presupuesto Angular no bloquean la construcción, pero deben
revisarse antes del paquete comercial definitivo.

## 4. Construcción del JAR

Desde la carpeta backend:

mvn clean package

El resultado es:

backend/target/GombethUrban-1.0.0.jar

El JAR debe contener Angular y no debe contener
application-local.properties.

## 5. Arranque

Desde la raíz del proyecto:

iniciar-gombeth-urban.bat

La aplicación se abre en:

http://localhost:8080/

No es necesario ejecutar npm start para utilizar el JAR empaquetado.

## 6. Detención

Desde la raíz del proyecto:

detener-gombeth-urban.bat

El script debe detener solamente los procesos que escuchan en los puertos
8080 y 4200.

## 7. Health check

Endpoint público:

GET /api/health

Respuesta esperada:

{"app":"Gombeth Urban Backend","status":"OK"}

No debe exponer credenciales, URL JDBC, servidor MySQL, perfiles activos,
rutas internas ni datos de usuarios.

## 8. Pruebas automatizadas

Desde backend:

mvn test

Las pruebas utilizan el perfil test y una base H2 en memoria. No deben
conectarse a MySQL.

## 9. Logs

Los logs se guardan por defecto en:

backend/logs

La ruta puede configurarse mediante:

GOMBETH_LOG_PATH

Los archivos rotan por fecha y tamaño, se comprimen y se conservan durante
30 días.

## 10. Copias de seguridad

Antes de una operación importante debe existir una copia de:

1. La base de datos.
2. Los documentos y adjuntos.
3. La configuración privada externa.
4. El JAR desplegado.
5. La versión Git correspondiente.

Las copias deben almacenarse fuera del proyecto y con acceso restringido.

## 11. Recuperación

1. Detener Gombeth Urban.
2. Restaurar una copia verificada de la base de datos.
3. Restaurar documentos y adjuntos.
4. Recuperar el JAR o la versión Git correspondiente.
5. Restaurar backend/config/application-local.properties.
6. Ejecutar mvn test.
7. Arrancar con los perfiles prod,local.
8. Comprobar /api/health.
9. Acceder a la aplicación y realizar una comprobación de solo lectura.

## 12. Restricciones

No incluir application-local.properties en Git, JAR, documentación,
correos o ZIP compartidos.

No modificar sepa_1914 mediante pruebas experimentales.

Las operaciones bancarias deben probarse únicamente con datos controlados
de la Comunidad de Propietarios de Prueba.