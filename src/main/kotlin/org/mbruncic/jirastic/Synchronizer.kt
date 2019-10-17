package org.mbruncic.jirastic

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.stream.StreamSupport

@Service
class Synchronizer {

    private val logger = LoggerFactory.getLogger(Synchronizer::class.java)

    @Autowired
    lateinit var jira: JiraRestClient
    @Autowired
    lateinit var elasticsearch: RestHighLevelClient

    @Scheduled(fixedDelay = 5*60*1000)
    fun sync() {
        val response = elasticsearch.get(GetRequest(".jirastic", "config"), RequestOptions.DEFAULT)
        for (config in response.source["configs"] as List<*>) {
            try {
                val hashMap = config as java.util.HashMap<*, *>
                val configName = hashMap["name"]
                val jiraQuery = hashMap["jql"]
                syncConfiguration(configName as String, jiraQuery as String)
            } catch (e: Exception){
                logger.error(e.message)
            }
        }
    }

    private fun syncConfiguration(configName: String, jiraQuery: String) {
        var tasksFetchedFromJira = 0
        do {
            //read
            logger.debug("Fetching data from Jira")
            val searchResult = jira.searchClient.searchJql(jiraQuery, null, tasksFetchedFromJira, null).claim()
            val count = StreamSupport.stream(searchResult.issues.spliterator(), false).count().toInt()
            tasksFetchedFromJira += count
            logger.debug("Fetched $count tasks from Jira")

            //map
            logger.debug("Mapping started")
            val listOfElasticsearchDocuments = searchResult.issues.map { mapToElasticsearchDocument(it) }
            logger.debug("Mapping finished")

            //add additional fields to each document
            listOfElasticsearchDocuments.forEach{
                it["ConfigName"] = configName
            }

            //write
            logger.debug("Elasticsearch indexing started")
            val bulkRequest = createElasticBulkRequest(listOfElasticsearchDocuments)
            elasticsearch.bulk(bulkRequest, RequestOptions.DEFAULT)
            logger.debug("Elasticsearch indexing finished")
        } while (tasksFetchedFromJira < searchResult.total)
    }

    private fun createElasticBulkRequest(listOfElasticsearchDocuments: List<HashMap<String, Any>>): BulkRequest {
        val bulkRequest = BulkRequest("tasks")
        listOfElasticsearchDocuments.forEach {
            val indexRequest = IndexRequest("tasks")
            indexRequest.id(it["Key"] as String?)
            indexRequest.source(it)
            bulkRequest.add(indexRequest)
        }
        return bulkRequest
    }

    private fun mapToElasticsearchDocument(issue: Issue): HashMap<String, Any> {
        val key = issue.key
        logger.trace("Mapping task $key")
        val source = HashMap<String, Any>()
        key?.let { source["Key"] = it }
        issue.status?.name?.let { source["Status"] = it }
        issue.issueType?.name?.let { source["IssueType"] = it }
        issue.project?.key?.let { source["Project"] = it }
        issue.summary?.let { source["Summary"] = it }
        issue.description?.let { source["Description"] = it }
        issue.reporter?.name?.let { source["Reporter"] = it }
        issue.assignee?.name?.let { source["Assignee"] = it }
        issue.creationDate?.millis?.let { source["CreationDate"] = it }
        issue.updateDate?.millis?.let { source["UpdatedDate"] = it }
        issue.priority?.name?.let { source["Priority"] = it }
        issue.votes?.votes?.let { source["Votes"] = it }
        issue.watchers?.numWatchers?.let { source["Watchers"] = it }
        issue.fields.filter { it.id == "customfield_10900" }.getOrNull(0)?.value?.let { source["Backlog"] = ((it as JSONArray).get(0) as JSONObject).get("value") }
        issue.fields.filter { it.id == "customfield_14300" }.getOrNull(0)?.value?.let { source["Categorization"] = (it as JSONObject).get("value") }
        issue.fields.filter { it.id == "aggregatetimespent" }.getOrNull(0)?.value?.let { source["AggregateTimeSpent"] = it }
        issue.resolution?.name?.let{ source["Resolution"] = it}
        return source
    }
}