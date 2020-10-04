package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import groovy.text.*
import org.gradle.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.lang.management.*

val Project.node_modules get() = korgeCacheDir["node_modules"]

private object JavaScriptClass

fun Project.configureJavaScript() {
    gkotlin.apply {
		js(KotlinJsCompilerType.IR) {
            browser {
                binaries.executable()
                testTask {
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }

			this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

			compilations.all {
				it.kotlinOptions.apply {
					languageVersion = "1.4"
					sourceMap = true
					//metaInfo = true
					//moduleKind = "umd"
					suppressWarnings = korge.supressWarnings
				}
			}
		}
	}

    val runJs = project.addTask<Task>(name = "runJs") { task ->
        task.group = GROUP_KORGE_RUN
        task.dependsOn("jsBrowserDevelopmentRun")
    }

    project.tasks.getByName("jsProcessResources").apply {
        //println(this.outputs.files.toList())
        doLast {
            val targetDir = this.outputs.files.first()
            logger.debug("jsProcessResources.targetDir: $targetDir")
            val jsMainCompilation = kotlin.js().compilations["main"]!!
            val jsFile = File(jsMainCompilation.kotlinOptions.outputFile ?: "dummy.js").name
            val resourcesFolders = jsMainCompilation.allKotlinSourceSets
                .flatMap { it.resources.srcDirs } + listOf(File(rootProject.rootDir, "_template"))
            //println("jsFile: $jsFile")
            //println("resourcesFolders: $resourcesFolders")
            fun readTextFile(name: String): String {
                for (folder in resourcesFolders) {
                    val file = File(folder, name)?.takeIf { it.exists() } ?: continue
                    return file.readText()
                }
                return JavaScriptClass::class.java.classLoader.getResourceAsStream(name)?.readBytes()?.toString(Charsets.UTF_8)
                    ?: error("We cannot find suitable '$name'")
            }

            val indexTemplateHtml = readTextFile("index.v2.template.html")
            val customCss = readTextFile("custom-styles.template.css")
            val customHtmlHead = readTextFile("custom-html-head.template.html")
            val customHtmlBody = readTextFile("custom-html-body.template.html")

            //println(File(targetDir, "index.html"))

            File(targetDir, "index.html").writeText(
                groovy.text.SimpleTemplateEngine().createTemplate(indexTemplateHtml).make(
                    mapOf(
                        "OUTPUT" to jsFile,
                        //"TITLE" to korge.name,
                        "TITLE" to "TODO",
                        "CUSTOM_CSS" to customCss,
                        "CUSTOM_HTML_HEAD" to customHtmlHead,
                        "CUSTOM_HTML_BODY" to customHtmlBody
                    )
                ).toString()
            )
        }
    }
}