# Sample usages of Maven Plugin for Azure Web Apps

#### Table of Content
* V2 configuration
   * Web App on Linux
      * [Tomcat with JRE 8](#Web-App-on-Linux-with-Java-8-Tomcat)
      * [JRE 8](#Web-App-on-Linux-with-Java-8-and-JAR-deploymentConfig)
   * Web App on Windows
      * [Deploy War File to Tomcat](#Web-App-on-Windows-with-Java-8-Tomcat)
   * Web App for Containers
      * [Public Docker Hub](#Web-App-for-Containers-with-public-DockerHub-container-image)
* [Deploy to Existing App Service Plan](#Web-App-deploymentConfig-to-an-existing-App-Service-Plan)
* [Deploy to Web App Deployment Slot](#Deploy-to-Web-App-Deployment-Slot)

<a name="web-app-on-linux-tomcat-v2"></a>
## Web App (on Linux) with Java 8, Tomcat
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8 and Tomcat 8.5
- Deploy a **WAR** file to context path: `/${project.build.finalName}` in your Web App server
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <packaging>war</packaging>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.6.0</version>
               <configuration>
                  <schemaVersion>V2</schemaVersion>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>${AZURE_AUTH}</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>${RESOURCEGROUP_NAME}</resourceGroup>
                  <appName>${WEBAPP_NAME}</appName>
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  <!-- Java Runtime Stack for Web App on Windows-->
                  <runtime>
                    <os>Linux</os>
                      <!-- for now only jre8 is supported for <javaVersion> of linux web app-->
                      <javaVersion>jre8</javaVersion>
                      <webContainer>tomcat 8.5</webContainer>
                    </runtime>
                  <appSettings>
                     <property>
                     <!-- Tell Azure which port you want to use, required for springboot 
                        jar applications -->
                        <name>PORT</name>
                        <value>8081</value>
                     </property>
                     <!--JVM OPTIONS -->
                     <property>
                        <name>JAVA_OPTS</name>
                        <value>-Xmx512m -Xms512m</value>
                     </property>
                  </appSettings>
                  <!-- Deployment settings -->
                  <deploymentConfig>
                    <resources>
                      <resource>
                        <directory>${project.basedir}/target</directory>
                        <targetPath>${project.build.finalName}</targetPath>
                        <includes>
                          <include>*.war</include>
                        </includes>
                      </resource>
                    </resources>
                  </deploymentConfig>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-on-linux-jre8-v2"></a>
## Web App (on Linux) with Java 8 and JAR deploymentConfig
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8
- Deploy an executable jar file to `/site/wwwroot/` directory in your Web App server

   ```xml
   <project>
      ...
      <packaging>jar</packaging>
      ...
      <build>
         <finalName>app</finalName>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.6.0</version>
               <configuration>
                  <schemaVersion>V2</schemaVersion>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>${AZURE_AUTH}</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>${RESOURCEGROUP_NAME}</resourceGroup>
                  <appName>${WEBAPP_NAME}</appName>
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
                  <!-- Java Runtime Stack for Web App on Windows-->
                  <runtime>
                    <os>Linux</os>
                    <javaVersion>jre8</javaVersion>
                  </runtime>
                  <!-- Deployment settings -->
                  <deploymentConfig>
                    <resources>
                      <resource>
                        <directory>${project.basedir}/target</directory>
                        <includes>
                          <include>*.jar</include>
                        </includes>
                      </resource>
                    </resources>
                  </deploymentConfig>
                  
                  <!-- This is to make sure the jar file can be released at the server side -->
                  <stopAppDuringDeployment>true</stopAppDuringDeployment>

               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```   


<a name="windows-tomcat-war-deploymentConfig-v2"></a>
## Web App (on Windows) with Java 8, Tomcat
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
- Use Java 8 and Tomcat 8.5
- Deploy the **WAR** file to context path: `/${project.build.finalName}` in your Web App server
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <packaging>war</packaging>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.6.0</version>
               <configuration>
                  <schemaVersion>V2</schemaVersion>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>${AZURE_AUTH}</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>${RESOURCEGROUP_NAME}</resourceGroup>
                  <appName>${WEBAPP_NAME}</appName>
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>

                  <!-- Java Runtime Stack for Web App on Windows-->
                  <runtime>
                    <os>Windows</os>
                    <javaVersion>1.8</javaVersion>
                    <webContainer>tomcat 8.5</webContainer>
                  </runtime>
                  <!-- Deployment settings -->
                  <deploymentConfig>
                    <resources>
                      <resource>
                        <directory>${project.basedir}/target</directory>
                        <targetPath>${project.build.finalName}</targetPath>
                        <includes>
                          <include>*.war</include>
                        </includes>
                      </resource>
                    </resources>
                  </deploymentConfig>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-for-containers-public-docker-v2"></a>
## Web App for Containers with public DockerHub container image
The following configuration is applicable for below scenario:
- Reference `${azure.auth.filePath}` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Use public DockerHub image `springio/gs-spring-boot-docker:latest` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.6.0</version>
               <configuration>
                  <schemaVersion>V2</schemaVersion>
                  <!-- Reference ${azure.auth.filePath} from Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <file>${azure.auth.filePath}</file>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>${RESOURCEGROUP_NAME}</resourceGroup>
                  <appName>${WEBAPP_NAME}</appName>
                  
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
                  <!-- Runtime Stack specified by Docker container image -->
                  <runtime>
                    <os>Docker</os>
                    <image>springio/gs-spring-boot-docker:latest</image>
                  </runtime>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="existing-app-service-plan"></a>
## Web App deploymentConfig to an existing App Service Plan
The following configuration is applicable for below scenario:
- Web App on Linux
- Use existing App Service Plan
- Use Java 8 and Tomcat 8.5
- Use WAR to deploy **WAR** file to ROOT: `/` in Tomcat

   ```xml
   <project>
      ...
      <packaging>war</packaging>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.6.0</version>
               <configuration>
                  
                  <!-- Web App information -->
                  <resourceGroup>${RESOURCEGROUP_NAME}</resourceGroup>
                  <appName>${WEBAPP_NAME}</appName>

                  <!-- Deploy Web App to the existing App Service Plan -->
                  <appServicePlanResourceGroup>${PLAN_RESOURCEGROUP_NAME}</appServicePlanResourceGroup>
                  <appServicePlanName>${PLAN_NAME}</appServicePlanName>
                  
                  <!-- Java Runtime Stack for Web App on Linux-->
                  <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```
   
<a name = "web-application-to-deploymentConfig-slot"></a>
## Deploy to Web App Deployment Slot
The following configuration is applicable for below scenario:

- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8 and Tomcat 8.5
- Use **WAR** deploymentConfig to deploy war file to context path `/${project.build.finalName}` in your Web App server
- Create a deploymentConfig slot and copy configuration from parent Web App then do the deploy

```xml
<project>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>com.microsoft.azure</groupId>
                <artifactId>azure-webapp-maven-plugin</artifactId>
                <version>1.6.0</version>
                <configuration>
                    <authentication>
                        <serverId>${AZURE_AUTH}</serverId>
                    </authentication>
                    
                    <!-- Web App information -->
                    <resourceGroup>${RESOURCEGROUP_NAME}</resourceGroup>
                    <appName>${WEBAPP_NAME}</appName>

                    <!-- Deployment Slot Setting -->
                    <deploymentSlotSetting>
                        <name>${SLOT_NAME}</name>
                        <configurationSource>parent</configurationSource>
                    </deploymentSlotSetting>
                    
                    <!-- Java Runtime Stack for Web App on Linux-->
                    <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
                    
                    <!-- War Deploy -->
                    <deploymentType>war</deploymentType>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
