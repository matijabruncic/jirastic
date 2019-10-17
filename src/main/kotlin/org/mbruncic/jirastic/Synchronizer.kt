package org.mbruncic.jirastic

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.StreamSupport

@Service
class Synchronizer {

    private val logger = LoggerFactory.getLogger(Synchronizer::class.java)

    @Autowired
    lateinit var jira: JiraRestClient
    @Autowired
    lateinit var elasticsearch: RestHighLevelClient

    fun sync() {
        var tasksFetchedFromJira = 0
        do {
            //read
            logger.debug("Fetching data from Jira")
            val searchResult = jira.searchClient.searchJql("filter = 21271", null, tasksFetchedFromJira, null).claim()
            val count = StreamSupport.stream(searchResult.issues.spliterator(), false).count().toInt()
            tasksFetchedFromJira += count
            logger.debug("Fetched $count tasks from Jira")

            //map
            logger.debug("Mapping started")
            val listOfElasticsearchDocuments = searchResult.issues.map { mapToElasticsearchDocument(it) }
            logger.debug("Mapping finished")

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

    private fun mapToElasticsearchDocument(next: Issue): HashMap<String, Any> {
        val key = next.key
        logger.trace("Mapping task $key")
        val source = HashMap<String, Any>()
        key?.let { source["Key"] = it }
        next.status?.name?.let { source["Status"] = it }
        next.issueType?.name?.let { source["IssueType"] = it }
        next.project?.key?.let { source["Project"] = it }
        next.summary?.let { source["Summary"] = it }
        next.description?.let { source["Description"] = it }
        next.reporter?.name?.let { source["Reporter"] = it }
        next.assignee?.name?.let { source["Assignee"] = it }
        next.creationDate?.millis?.let { source["CreationDate"] = it }
        next.updateDate?.millis?.let { source["UpdatedDate"] = it }
        next.priority?.name?.let { source["Priority"] = it }
        next.votes?.votes?.let { source["Votes"] = it }
        next.watchers?.numWatchers?.let { source["Watchers"] = it }
        next.fields.filter { it.id == "customfield_10900" }.getOrNull(0)?.value?.let { source["Backlog"] = ((it as JSONArray).get(0) as JSONObject).get("value") }
        next.fields.filter { it.id == "customfield_14300" }.getOrNull(0)?.value?.let { source["Categorization"] = (it as JSONObject).get("value") }
        next.fields.filter { it.id == "aggregatetimespent" }.getOrNull(0)?.value?.let { source["AggregateTimeSpent"] = it }
        return source
    }
}