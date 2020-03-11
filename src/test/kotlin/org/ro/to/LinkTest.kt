package org.ro.to

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import org.ro.IntegrationTest
import org.ro.core.aggregator.ActionDispatcher
import org.ro.core.event.EventStore
import org.ro.handler.ActionHandler
import org.ro.core.event.ResourceSpecification
import org.ro.snapshots.simpleapp1_16_0.ACTIONS_RUN_FIXTURE_SCRIPT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@UnstableDefault
class LinkTest : IntegrationTest() {

    @Test
    fun testParse() {
        //given
        val jsonStr = """{
            "rel": "R",
            "href": "H",
            "method": "GET",
            "type": "TY",
            "title": "TI"
        }"""

        // when
        val link = Json.parse(Link.serializer(), jsonStr)

        // then
        assertEquals("R", link.rel)
    }

    @Test
    // IntegrationTest, assumes a server is running, furthermore expects the fingerPrint (PD94*) to be valid
    fun testInvokeAction() {
        val url = "http://localhost:8080/restful/services/isisApplib.FixtureScriptsDefault/actions/runFixtureScript/invoke"
        val fingerPrint = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPG1lbWVudG8-PHBhdGg-PC9wYXRoPjwvbWVtZW50bz4="
/*        val body = """{
                "script": {
                    "value": {
                        "href": "http://localhost:8080/restful/objects/domainapp.application.fixture.scenarios.DomainAppDemo/$fingerPrint"}
                        },
                "parameters": {"value": ""}
            }  """   */
        val href = """"href": "http://localhost:8080/restful/objects/domainapp.application.fixture.scenarios.DomainAppDemo/$fingerPrint""""
        //TODO construct link, invoke and check response
        if (isAppAvailable()) {
            val action = ActionHandler().parse(ACTIONS_RUN_FIXTURE_SCRIPT.str) as Action

            val link = action.getInvokeLink()
            assertNotNull(link)
            //now pass on body in order to prepare everything to invoke
            val arguments = link.arguments as MutableMap
            val arg = Argument(href)
            arguments.put("script", arg)
            ActionDispatcher().invoke(link)
            val urlSpec = ResourceSpecification(url)
            val le = EventStore.find(urlSpec)!!
            assertTrue(!le.isError())
        }
    }

    @Test
    fun testArgumentsCanHaveEmptyKeys() {
        val href = "href"
        val arg = Argument(href)
        val args = mutableMapOf<String, Argument?>()
        args.put("", arg)
        val l = Link(arguments = args, href = href)
        // then
        val arguments = l.argMap()!!
        val a = arguments[""]
        assertEquals("href", a!!.key)
    }

}
