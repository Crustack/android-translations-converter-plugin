package io.github.crustack.android.translations.converter.excel.imports

import io.github.crustack.android.translations.converter.AndroidTranslation
import io.github.crustack.android.translations.converter.AndroidTranslations
import io.github.crustack.android.translations.converter.PLURALS_QUANTITIES
import org.gradle.internal.logging.progress.ProgressLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.w3c.dom.Element
import java.io.File
import java.lang.reflect.Proxy
import javax.xml.parsers.DocumentBuilderFactory

class TranslationsXmlImporterTest {

    @TempDir
    lateinit var tempDir: File

    private val dummyProgressLogger: ProgressLogger = Proxy.newProxyInstance(
        ProgressLogger::class.java.classLoader,
        arrayOf(ProgressLogger::class.java)
    ) { proxy, method, _ ->
        if (method.returnType.isAssignableFrom(proxy.javaClass)) proxy else null
    } as ProgressLogger

    @Test
    fun testPluralsOrderedByPluralsQuantities() {
        val importer = TranslationsXmlImporter(dummyProgressLogger)

        val translationsMap = mutableMapOf<String, AndroidTranslation>(
            "zebra_string" to AndroidTranslation(mutableMapOf("values" to "Zebra"), true),
            "apple_string" to AndroidTranslation(mutableMapOf("values" to "Apple"), true),
            // Plural items in reverse or random order
            "banana_plural_PLURALS_other" to AndroidTranslation(mutableMapOf("values" to "%d bananas"), true),
            "banana_plural_PLURALS_many" to AndroidTranslation(mutableMapOf("values" to "%d many bananas"), true),
            "banana_plural_PLURALS_few" to AndroidTranslation(mutableMapOf("values" to "%d few bananas"), true),
            "banana_plural_PLURALS_two" to AndroidTranslation(mutableMapOf("values" to "%d two bananas"), true),
            "banana_plural_PLURALS_one" to AndroidTranslation(mutableMapOf("values" to "%d banana"), true),
            "banana_plural_PLURALS_zero" to AndroidTranslation(mutableMapOf("values" to "No bananas"), true),
        )

        val androidTranslations = AndroidTranslations(translationsMap, mutableSetOf("values"))
        importer.import(androidTranslations, tempDir)

        val stringsXmlFile = File(tempDir, "values/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsXmlFile)
        val root = doc.documentElement

        val childElements = (0 until root.childNodes.length)
            .map { root.childNodes.item(it) }
            .filterIsInstance<Element>()

        // Check top-level elements order by name: apple_string, banana_plural, zebra_string
        assertEquals(3, childElements.size)
        assertEquals("string", childElements[0].nodeName)
        assertEquals("apple_string", childElements[0].getAttribute("name"))

        assertEquals("plurals", childElements[1].nodeName)
        assertEquals("banana_plural", childElements[1].getAttribute("name"))

        assertEquals("string", childElements[2].nodeName)
        assertEquals("zebra_string", childElements[2].getAttribute("name"))

        // Check <item> quantities order inside banana_plural
        val pluralItems = (0 until childElements[1].childNodes.length)
            .map { childElements[1].childNodes.item(it) }
            .filterIsInstance<Element>()

        val quantities = pluralItems.map { it.getAttribute("quantity") }
        assertEquals(PLURALS_QUANTITIES, quantities)
    }

    @Test
    fun testPluralsPartialQuantitiesOrderedCorrectly() {
        val importer = TranslationsXmlImporter(dummyProgressLogger)

        val translationsMap = mutableMapOf<String, AndroidTranslation>(
            "test_plural_PLURALS_other" to AndroidTranslation(mutableMapOf("values" to "%d tests"), true),
            "test_plural_PLURALS_few" to AndroidTranslation(mutableMapOf("values" to "%d few tests"), true),
            "test_plural_PLURALS_one" to AndroidTranslation(mutableMapOf("values" to "%d test"), true),
        )

        val androidTranslations = AndroidTranslations(translationsMap, mutableSetOf("values"))
        importer.import(androidTranslations, tempDir)

        val stringsXmlFile = File(tempDir, "values/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsXmlFile)
        val root = doc.documentElement

        val pluralElem = root.getElementsByTagName("plurals").item(0) as Element
        val pluralItems = (0 until pluralElem.childNodes.length)
            .map { pluralElem.childNodes.item(it) }
            .filterIsInstance<Element>()

        val quantities = pluralItems.map { it.getAttribute("quantity") }
        assertEquals(listOf("one", "few", "other"), quantities)
    }
}
