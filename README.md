## Dependencies
### Java
- Install JDK 8 (newer versions are not yet supported).
### Maven
- Install an instance of Maven following the instructions from the Maven website.
### PostgreSQL
- Install a fresh instance of PostgreSQL 9.6 or higher (11 or 12 recommended).
- Install PostGis extension for the respective server instance.
  or
- Set the jbdcUrl in tao-config tao.properties before build to an existing server instance.

### Docker
#### Windows
- Download and install Docker Desktop (latest version)
- Follow the installation instructions (make sure to update the WSL kernel)
- (in you have a Docker registry available) Edit Docker configuration to add
```json
"insecure-registries": ["<registry_host>:port"]
```
#### Linux
- Follow the docker installation instructions for the respective distro
## Build
- Check out sources into the same parent folder (e.g. tao-master)
- Build each of the 3 projects (in the order **tao-core, tao-plugins, tao-services**):
```shell
cd tao-core
mvn clean install -DskipTests=true
cd ../tao-plugins
mvn clean install -DskipTests=true
cd ../tao-services
mvn clean install -DskipTests=true
```
- Now the bundle should be available in:
  `tao-core/tao-core-kit/target/tao-core-x.y.z

### Known issues
For now, make sure to delete the following jars from the **lib** subfolder of the bundle:
- checker-qual-2.0.0.jar
- btf-1.2.jar
- commons-codec-1.11.jar
- commons-codec-1.13.jar
- commons-lang-2.4.jar
- commons-lang3-3.8.1.jar
- commons-lang3-3.9.jar
- commons-io-2.5.jar
- gt-api*.jar
- guava-25.1-jre.jar
- httpclient-4.5.1.jar
- httpclient-4.5.12.jar
- httpcore-4.4.3.jar
- istack-commons-runtime-3.0.7.jar
- jackson-annotations-2.10.5.jar
- jackson-core-2.10.*.jar
- jackson-coreutils-1.*.jar
- jackson-databind-2.10.*.jar
- jackson-jaxrs-base-2.10.*.jar
- jackson-jaxrs-json-provider-2.10.*.jar
- jackson-module-jaxb-annotations-2.12.*.jar
- javax.activation-api-1.2.0.jar
- javax.annotation-api-1.3.1.jar
- jakarta.activation-api-1.2.1.jar
- jakarta.annotation-api-1.3.4.jar
- jakarta.xml.bind-api-2.3.2.jar
- javax.json-1.0.4.jar
- jaxb-api-2.3.1.jar
- jaxb-runtime-2.3.1.jar
- jboss-logging-3.3.2.Final.jar
- jboss-logging-3.4.1.Final.jar
- jboss-jaxb-api_2.3_spec-2.0.0.Final.jar
- jsch-0.1.53.jar
- json-patch-1.9.jar
- json-simple-1.1.jar
- jsr305-2.0.0.jar
- jts-core-1.14.0.jar
- keycloak-*-15*.jar
- mchange-commons-java-0.2.11.jar
- msg-simple-1.1.jar
- reactive-streams-1.0.0.jar
- resteasy-*-3.*.jar
- servlet-api-2.4.jar
- slf4j-api-1.7.7.jar
- slf4j-api-1.7.2*.jar
- slf4j-api-1.7.30.jar
- snakeyaml-1.24.jar
- spring-*-5.2*.jar
- spring-security-core-5.6.0.jar
- spring-security-crypto-5.6.0.jar
- spring-security-web-5.6.0.jar
- txw2-2.3.1.jar
- validation-api-2.0.1.Final.jar
