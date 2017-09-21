package com.juntaki.springfennec

import io.swagger.annotations.ApiOperation
import io.swagger.models.Operation
import io.swagger.models.Path
import io.swagger.models.Swagger
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import javax.lang.model.element.*
import javax.lang.model.util.ElementScanner8
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class FunctionVisitor(
        private val swagger: Swagger,
        private val elementUtils: Elements,
        private val typeUtils: Types
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
        fun setUniqueOperationId(operation: Operation, operationId:String):Operation {
            definedOperationIds.find { it == operationId }?.let { throw Exception("Duplicate operationId: " + operationId) }
            definedOperationIds.add(operationId)
            operation.operationId = operationId
            return operation
        }

        val requestMapping: RequestMapping? = e!!.getAnnotation(RequestMapping::class.java)
        requestMapping?.let {
            requestMapping.value.forEach {
                // if already defined path, overwrite it.
                var path = swagger.getPath(currentPath + it)?: Path()

                // one method means one operation, but one operationId is per method
                val operation = Operation()

                operation.consumes = requestMapping.consumes.asList()
                operation.produces = requestMapping.produces.asList()

                val apiOperation: ApiOperation? = e.getAnnotation(ApiOperation::class.java)
                apiOperation?.let {
                    operation.summary = it.value
                    operation.tags = it.tags.toList()

                    // set parameters
                    e.accept(ParamVisitor(swagger, operation.parameters, elementUtils, typeUtils), null)

                    // Set operationId in the following manner.
                    // 1. nickname, if it defined (you should not use multiple requestMapping)
                    // 2. function name: findPetsByTags
                    // TODO: 3. (request method) + (1. or 2.): e.g. GETfindPetsByTags, if multiple request method is defined on one function.
                    // TODO: more option
                    val operationId = if(it.nickname.isEmpty()) e.simpleName.toString() else it.nickname

                    requestMapping.method.forEach {
                        when(it) {
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
                }
                swagger.path(currentPath + it ,path)
            }
        }
        return super.visitExecutable(e, p)
    }


}