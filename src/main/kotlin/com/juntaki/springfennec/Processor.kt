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

import com.juntaki.springfennec.annotation.ApiGroup
import com.juntaki.springfennec.util.PropertyUtil
import io.swagger.annotations.SwaggerDefinition
import io.swagger.models.*
import io.swagger.models.auth.In
import io.swagger.util.BaseReaderUtils
import io.swagger.util.Json
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.juntaki.springfennec.annotation.*")
class Processor : AbstractProcessor() {
    private var checked = false

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Only first round is enough.
        if (checked) return true
        checked = true
        val propertyUtil = PropertyUtil(processingEnv.elementUtils, processingEnv.typeUtils)

        // This function is from swagger-core readSwaggerConfig(), rewrite to Kotlin by juntaki.
        fun createSwaggerByConfig(config: SwaggerDefinition?): Swagger {
            val swagger = Swagger()
            config?.let {
                if (config.basePath.isNotEmpty()) {
                    swagger.basePath = config.basePath
                }

                if (config.host.isNotEmpty()) {
                    swagger.host = config.host
                }
                swagger.info = swagger.info ?: io.swagger.models.Info()

                if (config.info.description.isNotEmpty()) {
                    swagger.info.description = config.info.description
                }

                if (config.info.termsOfService.isNotEmpty()) {
                    swagger.info.termsOfService = config.info.termsOfService
                }

                if (config.info.title.isNotEmpty()) {
                    swagger.info.title = config.info.title
                }

                if (config.info.version.isNotEmpty()) {
                    swagger.info.version = config.info.version
                }

                if (config.info.contact.name.isNotEmpty()) {
                    swagger.info.contact = swagger.info.contact ?: Contact()
                    swagger.info.contact.name = config.info.contact.name
                }
                if (config.info.contact.email.isNotEmpty()) {
                    swagger.info.contact = swagger.info.contact ?: Contact()
                    swagger.info.contact.email = config.info.contact.email
                }

                if (config.info.contact.url.isNotEmpty()) {
                    swagger.info.contact = swagger.info.contact ?: Contact()
                    swagger.info.contact.url = config.info.contact.url
                }

                if (config.info.license.name.isNotEmpty()) {
                    swagger.info.license = swagger.info.license ?: License()

                    swagger.info.license.name = config.info.license.name
                    if ((config.info.license.url.isNotEmpty())) {
                        swagger.info.license.url = config.info.license.url
                    }
                }

                swagger.info.vendorExtensions.putAll(BaseReaderUtils.parseExtensions(config.info.extensions))

                config.consumes.forEach {
                    if (it.isNotEmpty()) {
                        swagger.addConsumes(it)
                    }
                }

                config.produces.forEach {
                    if (it.isNotEmpty()) {
                        swagger.addProduces(it)
                    }
                }
                config.securityDefinition.oAuth2Definitions.forEach {
                    var oAuth2Definition = io.swagger.models.auth.OAuth2Definition()
                    val flow = it.flow

                    oAuth2Definition = when (flow) {
                        io.swagger.annotations.OAuth2Definition.Flow.ACCESS_CODE ->
                            oAuth2Definition.accessCode(it.authorizationUrl, it.tokenUrl)
                        io.swagger.annotations.OAuth2Definition.Flow.APPLICATION ->
                            oAuth2Definition.application(it.tokenUrl)
                        io.swagger.annotations.OAuth2Definition.Flow.IMPLICIT ->
                            oAuth2Definition.implicit(it.authorizationUrl)
                        else ->
                            oAuth2Definition.password(it.tokenUrl)
                    }

                    it.scopes.forEach {
                        oAuth2Definition.addScope(it.name, it.description)
                    }

                    oAuth2Definition.description = it.description
                    swagger.addSecurityDefinition(it.key, oAuth2Definition)
                }

                for (apiKeyAuthConfigs in arrayOf(config.securityDefinition.apiKeyAuthDefintions, config.securityDefinition.apiKeyAuthDefinitions)) {
                    for (apiKeyAuthConfig in apiKeyAuthConfigs) {
                        val apiKeyAuthDefinition = io.swagger.models.auth.ApiKeyAuthDefinition()

                        apiKeyAuthDefinition.name = apiKeyAuthConfig.name
                        apiKeyAuthDefinition.`in` = In.forValue(apiKeyAuthConfig.`in`.toValue())
                        apiKeyAuthDefinition.description = apiKeyAuthConfig.description

                        swagger.addSecurityDefinition(apiKeyAuthConfig.key, apiKeyAuthDefinition)
                    }
                }

                for (basicAuthConfigs in arrayOf(config.securityDefinition.basicAuthDefinions, config.securityDefinition.basicAuthDefinitions)) {
                    for (basicAuthConfig in basicAuthConfigs) {
                        val basicAuthDefinition = io.swagger.models.auth.BasicAuthDefinition()

                        basicAuthDefinition.description = basicAuthConfig.description

                        swagger.addSecurityDefinition(basicAuthConfig.key, basicAuthDefinition)
                    }
                }

                if (config.externalDocs.value.isNotEmpty()) {
                    var externalDocs = swagger.externalDocs
                    if (externalDocs == null) {
                        externalDocs = ExternalDocs()
                        swagger.externalDocs = externalDocs
                    }

                    externalDocs.description = config.externalDocs.value


                    if (!config.externalDocs.url.isEmpty()) {
                        externalDocs.url = config.externalDocs.url
                    }
                }

                config.tags.forEach {
                    if (!it.name.isEmpty()) {
                        val tag = Tag()
                        tag.name = it.name
                        tag.description = it.description

                        if (!it.externalDocs.value.isEmpty()) {
                            tag.externalDocs = ExternalDocs(it.externalDocs.value, it.externalDocs.url)
                        }
                        tag.vendorExtensions.putAll(BaseReaderUtils.parseExtensions(it.extensions))
                        swagger.addTag(tag)
                    }
                }

                config.schemes.forEach {
                    if (it != SwaggerDefinition.Scheme.DEFAULT) {
                        swagger.addScheme(Scheme.forValue(it.name))
                    }
                }
            }

            return swagger
        }

        fun createSpec(specFileName: String, pathRegexString: String, config: SwaggerDefinition?) {
            val swagger = createSwaggerByConfig(config)

            val pathRegex = Regex(pathRegexString)
            roundEnv.rootElements.forEach {
                it.accept(FunctionVisitor(swagger, propertyUtil, pathRegex), null)
            }

            File(specFileName).printWriter().use {
                Json.mapper()!!.writeValue(it, swagger)
            }
        }

        // Check if annotation processor work
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Springfennec is running")

        val apiGroups = roundEnv.getElementsAnnotatedWith(ApiGroup::class.java)

        if (apiGroups == null || apiGroups.isEmpty()) {
            createSpec("spec.json", ".*", null)
        } else {
            apiGroups.forEach {
                it.getAnnotationsByType(ApiGroup::class.java).forEach {
                    for (regexString in it.value) {
                        createSpec("spec_${it.name}.json", regexString, it.apiInfo)
                    }
                }
            }
        }
        return true
    }
}
