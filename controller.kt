package com.sanfosys.app.controllers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI

@CrossOrigin(origins = ["*"])
@RestController
class KController(val restTemplate: RestTemplate) {

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    @RequestMapping("/**")
    fun proxy(requestEntity: RequestEntity<Any>, @RequestParam params: HashMap<String, String>): ResponseEntity<Any> {
        val remoteService = URI.create("http://localhost:8080")
        val uri = requestEntity.url.run {
            URI(scheme, userInfo, remoteService.host, remoteService.port, path, query, fragment)
        }

        val forward = RequestEntity(
                requestEntity.body, requestEntity.headers,
                requestEntity.method, uri
        )

        return restTemplate.exchange(forward)
    }
}
