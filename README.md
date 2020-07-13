# zookeeper-study
A project to study how to use zookeeper.

## How to build
The root pom.xml will use ${JAVA_X_HOME} in maven-compiler-plugin, so you need enable profile settings
in your settings.xml like the following example:

```xml
<settings>
  <profiles>
    <profile>
      <id>compiler</id>
        <properties>
          <JAVA_1_4_HOME>C:\Program Files\Java\j2sdk1.4.2_09</JAVA_1_4_HOME>
          <JAVA_1_6_HOME>C:\Program Files\Java\j2sdk1.6.0_18</JAVA_1_6_HOME>
        </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>compiler</activeProfile>
  </activeProfiles>
</settings>
``` 