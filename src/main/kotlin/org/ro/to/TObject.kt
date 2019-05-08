package org.ro.to

import kotlinx.serialization.Serializable
import org.ro.core.TransferObject

@Serializable
data class TObject(val links: List<Link> = emptyList(),
                   val extensions: Extensions,
                   val title: String = "",
                   val domainType: String = "",
                   val instanceId: String? = null,
                   val members: Map<String, Member> = emptyMap()
) : TransferObject {

    fun getId(): String {
        return "$domainType/$instanceId"
    }

    fun getLayoutLink(): Link? {
        var href: String?
        for (l in links) {
            href = l.href
            //TODO can be "object-layout" >= 1.16
            if (href.isNotEmpty()) {
                if (href.endsWith("layout")) {
                    return l
                }
            }
        }
        return null
    }

    fun getProperties(): MutableList<Member> {
        val result = mutableListOf<Member>()
        for (m in members) {
            if (m.value.memberType == MemberType.PROPERTY.type) {
                result.add(m.value)
            }
        }
        return result
    }

    /**
     * Post-Constructor fun using dynamic nature of class.
     */
    fun addMembersAsProperties() {
        val members: MutableList<Member> = getProperties()
        console.log("[TObject.addMembersAsProperties] $members")
        for (m in members) {
            if (m.memberType == MemberType.PROPERTY.type) {
                addAsProperty(this, m)
            }
        }
    }

    fun addAsProperty(dynObj: TObject, member: Member) {
        val value = "//TODO members of type property can be either null|String|Link"
        if (value == "[object Object]") {
            //FIXME var link = Link(value)
            //here the magic of recursive OA's take place
            //FIXME attribute = ObjectAdapter(link, link.title, "Link")
        }
        val key: String = member.id
        //FIXME dynObj[key] = attribute
    }

}