/*
 *    Copyright 2017 juntaki
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.juntaki.springfennec

import com.juntaki.springfennec.util.PropertyUtil
import io.swagger.annotations.ApiOperation
import io.swagger.models.Operation
import io.swagger.models.Path
import io.swagger.models.Response
import io.swagger.models.Swagger
import org.springframework.web.bind.annotation.*
import java.nio.file.Paths
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementScanner8

class FunctionVisitor(
        private val swagger: Swagger,
        private val propertyUtil: PropertyUtil,
        private val pathRegex: Regex
) : ElementScanner8<Unit?, Unit?>() {
    var currentPath = ""
    var definedOperationIds = mutableListOf<String>()

    // Class
    override fun visitType(e: TypeElement?, p: Unit?): Unit? {
        val requestMapping: RequestMapping? = e!!.getAnnotation(RequestMapping::class.java)
        if (requestMapping != null) {
            requestMapping.value.forEach {
                currentPath = it
                super.visitType(e, p)
            }
        } else {
            super.visitType(e, p)
        }
        return Unit
    }

    // Function
    override fun visitExecutable(e: ExecutableElement?, p: Unit?): Unit? {
        e ?: throw NullPointerException()

        class RequestMappingData(
                val name: String,
                val path: Array<String>,
                val method: Array<RequestMethod>,
                val params: Array<String>,
                val headers: Array<String>,
                val consumes: Array<String>,
                val produces: Array<String>
        ) {
            constructor(mapping: RequestMapping) : this(
                    method = mapping.method,
                    name = mapping.name,
                    path = if (mapping.value.isNotEmpty()) mapping.value else mapping.path,
                    params = mapping.params,
                    headers = mapping.headers,
                    consumes = mapping.consumes,
                    produces = mapping.produces
            )

            constructor(mapping: GetMapping) : this(
                    method = arrayOf(RequestMethod.GET),
                    name = mapping.name,
                    path = if (mapping.value.isNotEmpty()) mapping.value else mapping.path,
                    params = mapping.params,
                    headers = mapping.headers,
                    consumes = mapping.consumes,
                    produces = mapping.produces
            )

            constructor(mapping: PostMapping) : this(
                    method = arrayOf(RequestMethod.POST),
                    name = mapping.name,
                    path = if (mapping.value.isNotEmpty()) mapping.value else mapping.path,
                    params = mapping.params,
                    headers = mapping.headers,
                    consumes = mapping.consumes,
                    produces = mapping.produces
            )

            constructor(mapping: PutMapping) : this(
                    method = arrayOf(RequestMethod.PUT),
                    name = mapping.name,
                    path = if (mapping.value.isNotEmpty()) mapping.value else mapping.path,
                    params = mapping.params,
                    headers = mapping.headers,
                    consumes = mapping.consumes,
                    produces = mapping.produces
            )

            constructor(mapping: PatchMapping) : this(
                    method = arrayOf(RequestMethod.PATCH),
                    name = mapping.name,
                    path = if (mapping.value.isNotEmpty()) mapping.value else mapping.path,
                    params = mapping.params,
                    headers = mapping.headers,
                    consumes = mapping.consumes,
                    produces = mapping.produces
            )

            constructor(mapping: DeleteMapping) : this(
                    method = arrayOf(RequestMethod.DELETE),
                    name = mapping.name,
                    path = if (mapping.value.isNotEmpty()) mapping.value else mapping.path,
                    params = mapping.params,
                    headers = mapping.headers,
                    consumes = mapping.consumes,
                    produces = mapping.produces
            )

            private fun setUniqueOperationId(operation: Operation, operationId: String): Operation {
                definedOperationIds.find { it == operationId }?.let { throw Exception("Duplicate operationId: " + operationId) }
                definedOperationIds.add(operationId)
                operation.operationId = operationId
                return operation
            }

            fun setOperation(operation: Operation, operationNickname: String?) {
                var pathValue = this.path
                if (pathValue.isEmpty()) pathValue = arrayOf("")
                if (this.consumes.isNotEmpty()) operation.consumes = this.consumes.asList()
                if (this.produces.isNotEmpty()) operation.produces = this.produces.asList()
                val method = this.method
                // it may just one...
                pathValue.forEach {
                    val absPath = Paths.get(currentPath, it).toString()
                    // Ignore path if not match
                    if (!pathRegex.containsMatchIn(absPath)) return@forEach

                    // If path is already defined, use it.
                    // Even if the same request methods was defined, build will be error by spring.
                    val path = swagger.getPath(absPath) ?: Path()
                    val operationId = operationNickname ?: e.simpleName.toString()

                    // set parameters
                    e.accept(ParamVisitor(swagger, operation.parameters, propertyUtil), null)

                    method.forEach {
                        when (it) {
                            RequestMethod.DELETE -> path.delete = setUniqueOperationId(operation, operationId)
                            RequestMethod.GET -> path.get = setUniqueOperationId(operation, operationId)
                            RequestMethod.OPTIONS -> path.options = setUniqueOperationId(operation, operationId)
                            RequestMethod.PATCH -> path.patch = setUniqueOperationId(operation, operationId)
                            RequestMethod.POST -> path.post = setUniqueOperationId(operation, operationId)
                            RequestMethod.PUT -> path.put = setUniqueOperationId(operation, operationId)
                            RequestMethod.HEAD -> path.head = setUniqueOperationId(operation, operationId)
                            RequestMethod.TRACE -> TODO("Not implemented")
                        }
                    }
                    swagger.path(absPath, path)
                }
            }
        }

        // One method means one operation, but an operationId is needed per method.
        val operation = Operation()

        val res = Response()
        // res is org.springframework.http.ResponseEntity<T>, use T. or Void
        val responseRegex = Regex("""^org.springframework.http.ResponseEntity""")
        if (responseRegex.containsMatchIn(e.returnType.toString())) {
            val schema = propertyUtil.getProperty((e.returnType as DeclaredType).typeArguments[0])
            // schema may be Void
            schema?.let {
                res.schema = schema
                res.description = "OK"
                operation.addResponse("200", res)
            }
        }

        var operationNickname: String? = null
        val apiOperation: ApiOperation? = e.getAnnotation(ApiOperation::class.java)
        apiOperation?.let {
            operation.summary = it.value
            operation.tags = it.tags.toList()
            operation.description = it.notes

            // Set operationId in the following manner.
            // 1. nickname, if it defined (you should not use multiple requestMapping)
            // 2. function name: findPetsByTags
            // TODO: 3. (request method) + (1. or 2.): e.g. GETfindPetsByTags, if multiple request method is defined on one function.
            // TODO: more option
            if (it.nickname.isNotEmpty()) operationNickname = it.nickname
        }

        e.getAnnotation(RequestMapping::class.java)?.let { RequestMappingData(it) }?.let { it.setOperation(operation, operationNickname) }
        e.getAnnotation(GetMapping::class.java)?.let { RequestMappingData(it) }?.let { it.setOperation(operation, operationNickname) }
        e.getAnnotation(PostMapping::class.java)?.let { RequestMappingData(it) }?.let { it.setOperation(operation, operationNickname) }
        e.getAnnotation(PutMapping::class.java)?.let { RequestMappingData(it) }?.let { it.setOperation(operation, operationNickname) }
        e.getAnnotation(PatchMapping::class.java)?.let { RequestMappingData(it) }?.let { it.setOperation(operation, operationNickname) }
        e.getAnnotation(DeleteMapping::class.java)?.let { RequestMappingData(it) }?.let { it.setOperation(operation, operationNickname) }

        return super.visitExecutable(e, p)
    }


}