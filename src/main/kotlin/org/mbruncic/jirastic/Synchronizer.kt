package org.mbruncic.jirastic

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class Synchronizer {

    @Autowired
    lateinit var elasticsearch: RestHighLevelClient

    fun sync() {
        val indexRequest = IndexRequest("tasks")
        indexRequest.id("MATS-1")
        val source = HashMap<String, Any>()
        source.put("name", "Prvi task ikad")
        indexRequest.source(source)
        elasticsearch.index(indexRequest, RequestOptions.DEFAULT)
    }
}