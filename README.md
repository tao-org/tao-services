## Dependencies
### Java
- Install JDK 17 or later (JDK 21 recommended)
### Maven
- Install an instance of Maven for the installed JDK following the instructions from the Maven website.
### PostgreSQL
- Install a fresh instance of PostgreSQL 13 or higher.
- Install PostGis extension for the respective server instance.
  or
- Set the jbdcUrl in tao-config tao.properties before build to an existing server instance.

### Docker
#### Windows
- Download and install Docker Desktop (latest version) or Rancher Desktop (latest version)
- Follow the installation instructions (make sure to update the WSL kernel)
- (in you have a Docker registry available) Edit Docker configuration to add
```json
"insecure-registries": ["<registry_host>:port"]
```
#### Linux
- Follow the docker installation instructions for the respective distro
## Build
- Check out sources into the same parent folder (e.g. tao)
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
When OpenStack and Kubernetes plugins are used together, one of them may fail. This is due to different Jackson configurations applied to the same instance.
In order to use them together, please check out the forked **openstack4j** repository, build this version from sources and use the resulted jars instead of the Maven published ones.
