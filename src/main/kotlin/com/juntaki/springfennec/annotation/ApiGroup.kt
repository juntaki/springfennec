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

package com.juntaki.springfennec.annotation

import io.swagger.annotations.SwaggerDefinition
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ApiGroup (
        // regex for path not include basePath
        val value: Array<String> = arrayOf(".*"),
        val name: String = "",
        val apiInfo : SwaggerDefinition = SwaggerDefinition()
)
