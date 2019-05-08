package org.ro.to

import com.github.snabbdom._set
import org.ro.core.event.EventStore
import org.ro.handler.ActionHandler
import org.ro.handler.IntegrationTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinkTest : IntegrationTest() {

    @Test
    // IntegrationTest, assumes a server is running, furthermore expects the fingerPrint (PD94*) to be valid
    fun testInvokeAction() {
        val url = "http://localhost:8080/restful/services/isisApplib.FixtureScriptsDefault/actions/runFixtureScript/invoke"
        val fingerPrint = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPG1lbWVudG8-PHBhdGg-PC9wYXRoPjwvbWVtZW50bz4="
        val body = """{
                "script": {
                    "value": {
                        "href": "http://localhost:8080/restful/objects/domainapp.application.fixture.scenarios.DomainAppDemo/$fingerPrint"}
                        },
                "parameters": {"value": ""}
            }  """
        val href = """"href": "http://localhost:8080/restful/objects/domainapp.application.fixture.scenarios.DomainAppDemo/$fingerPrint""""
        //TODO construct link, invoke and check response
        if (isSimpleAppAvailable()) {
            val action = ActionHandler().parse(ACTIONS_RUN_FIXTURE_SCRIPT.str) as Action

            val link = action.getInvokeLink()
            assertNotNull(link)
            //now pass on body in order to prepare everything to invoke
            val arguments = link.arguments
            val arg = Argument(href)
            arguments._set("script", arg)
            link.invoke()
            val le = EventStore.find(url)
            assertNotNull(le)
            assertTrue(!le.isError())
        }
    }
}