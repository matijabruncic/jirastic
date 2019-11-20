# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/maven-plugin/)

### Docker build
To successfully build and deploy docker image you should login to docker.io and then execute `jib:build`

If you need to tag docker image with some different tag and push to some other docker registry use: `-Djib.to.image=customDockerImage:customTag` 

### Project build
Execute `mvn clean install`

### Running
`docker run -e JIRA_URL=${jira_url} -e JIRA_USERNAME=${jira_username} -e JIRA_PASSWORD=${jira_password} -e ELASTICSEARCH_URL=${elasticsearch_url} --network host mats990/jirastic:latest`