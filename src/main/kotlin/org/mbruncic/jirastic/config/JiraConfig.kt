package org.mbruncic.jirastic.config

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.beans.ConstructorProperties
import java.net.URI
import javax.validation.constraints.NotNull

@Configuration
@ConfigurationProperties("jira")
@Validated
class JiraConfig {

    @NotNull
    lateinit var url: String
    var username: String? = null
    var password: String? = null

    @Bean
    fun jira(): JiraRestClient {
        val jiraUri = URI.create(url)
        val client = AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(jiraUri, username, password)
        return client
    }
}