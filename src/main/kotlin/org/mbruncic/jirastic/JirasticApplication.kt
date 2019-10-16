package org.mbruncic.jirastic

import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JirasticApplication : ApplicationRunner {

	@Autowired
    lateinit var synchronizer: Synchronizer

    override fun run(args: ApplicationArguments?) {
        synchronizer.sync()
    }
}

fun main(args: Array<String>) {
    runApplication<JirasticApplication>(*args)
}
