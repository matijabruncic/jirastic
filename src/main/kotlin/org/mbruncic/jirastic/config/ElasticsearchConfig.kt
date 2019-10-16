package org.mbruncic.jirastic.config

import mu.KotlinLogging
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotNull

@Configuration
@ConfigurationProperties("elasticsearch")
@Validated
class ElasticsearchConfig {

    private val logger = KotlinLogging.logger {}

    @NotNull
    lateinit var url: String
    var username: String? = null
    var password: String? = null

    @Bean(destroyMethod="close")
    fun elasticsearch(): RestHighLevelClient {
        val httpHost = HttpHost.create(url)
        val restClientBuilder = RestClient.builder(httpHost)
        username?.let {
            logger.debug { "Configuring elasticsearch lib to use basic authentication" }
            restClientBuilder.setHttpClientConfigCallback {
                val basicCredentialsProvider = BasicCredentialsProvider()
                basicCredentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
                it.setDefaultCredentialsProvider(basicCredentialsProvider)
            }
        }
        val restHighLevelClient = RestHighLevelClient(restClientBuilder)
        val health = restHighLevelClient.cluster().health(ClusterHealthRequest(), RequestOptions.DEFAULT)
        if (health.status === ClusterHealthStatus.RED) {
            throw IllegalStateException("Elasticsearch cluster in ${health.status} status")
        } else {
            logger.debug { "Elasticsearch cluster in ${health.status} status" }
        }
        return restHighLevelClient
    }
}